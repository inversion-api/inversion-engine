/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.cloud.jdbc.db;

import java.lang.reflect.Field;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import ch.qos.logback.classic.Level;
import io.inversion.cloud.jdbc.rql.SqlQuery;
import io.inversion.cloud.jdbc.utils.JdbcUtils;
import io.inversion.cloud.jdbc.utils.JdbcUtils.SqlListener;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.ApiException;
import io.inversion.cloud.model.ApiListener;
import io.inversion.cloud.model.Collection;
import io.inversion.cloud.model.Db;
import io.inversion.cloud.model.Property;
import io.inversion.cloud.model.Relationship;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.model.Results;
import io.inversion.cloud.model.Rows.Row;
import io.inversion.cloud.rql.Term;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.utils.Utils;

public class JdbcDb extends Db<JdbcDb>
{
   protected char                 stringQuote              = '\'';
   protected char                 columnQuote              = '"';

   protected transient DataSource pool                     = null;

   public static final int        MIN_POOL_SIZE            = 3;
   public static final int        MAX_POOL_SIZE            = 10;

   protected String               driver                   = null;
   protected String               url                      = null;
   protected String               user                     = null;
   protected String               pass                     = null;
   protected int                  poolMin                  = 3;
   protected int                  poolMax                  = 10;
   protected int                  idleConnectionTestPeriod = 3600;           // in seconds
   protected boolean              autoCommit               = false;

   // set this to false to turn off SQL_CALC_FOUND_ROWS and SELECT FOUND_ROWS()
   // Only impacts 'mysql' types
   protected boolean              calcRowsFound            = true;

   protected int                  relatedMax               = 500;

   protected List<String>         ddlUrls                  = new ArrayList();

   static
   {
      ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("ROOT");
      logger.setLevel(Level.WARN);

      //      ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("com.zaxxer.hikari.pool.PoolBase");
      //      logger.setLevel(Level.INFO);

      JdbcUtils.addSqlListener(new SqlListener()
         {
            @Override
            public void onError(String method, String sql, Object args, Exception ex)
            {
               if (method != null && method.equals("selectRows"))
               {
                  logger.error("SQL error in '" + method + "' [" + sql.replace("\r\n", "") + "] " + ex.getMessage());
               }
               else
               {
                  ex.printStackTrace();
               }
            }

            @Override
            public void beforeStmt(String method, String sql, Object args)
            {
            }

            @Override
            public void afterStmt(String method, String sql, Object args, Exception ex, Object result)
            {
               String debugPrefix = "SqlDb: ";

               String debugType = "unknown";

               if (Chain.peek() != null)
               {
                  Collection coll = Chain.peek().getRequest().getCollection();
                  if (coll != null && coll.getDb() != null)
                  {
                     Db db = coll.getDb();
                     debugType = db.getType().toLowerCase();
                  }
               }
               debugPrefix += debugType;

               args = (args != null && args.getClass().isArray() ? Arrays.asList((Object[]) args) : args);

               sql = sql.replaceAll("\r", "");
               sql = sql.replaceAll("\n", " ");
               sql = sql.trim().replaceAll(" +", " ");
               StringBuffer buff = new StringBuffer("");
               buff.append(debugPrefix).append(" -> '").append(sql).append("'").append(" args=").append(args).append(" error='").append(ex != null ? ex.getMessage() : "").append("'");
               String msg = buff.toString();
               Chain.debug(msg);
            }
         });
   }

   public JdbcDb()
   {
      //System.out.println("SqlDb() <init>");
   }

   public JdbcDb(String name)
   {
      withName(name);
   }

   public JdbcDb(String name, String driver, String url, String user, String pass, String... ddlUrls)
   {
      withName(name);
      withDriver(driver);
      withUrl(url);
      withUser(user);
      withPass(pass);
      withDdlUrl(ddlUrls);
   }

   public JdbcDb(String url, String user, String pass)
   {
      withUrl(url);
      withUser(user);
      withPass(pass);
   }

   @Override
   protected void doStartup(Api api)
   {

      if (isType("mysql"))
         withColumnQuote('`');

      super.doStartup(api);

      api.withApiListener(new ApiListener()
         {

            @Override
            public void onStartup(Api api)
            {
            }

            @Override
            public void onShutdown(Api api)
            {
            }

            @Override
            public void afterRequest(Request req, Response res)
            {
               try
               {
                  ConnectionLocal.commit();
               }
               catch (Exception ex)
               {
                  ApiException.throw500InternalServerError(ex, "Error committing tansaction");
               }
            }

            @Override
            public void afterError(Request req, Response res)
            {

               try
               {
                  ConnectionLocal.rollback();
               }
               catch (Throwable t)
               {
                  log.warn("Error rollowing back transaction.", t);
               }

            }

            @Override
            public void beforeFinally(Request req, Response res)
            {
               try
               {
                  ConnectionLocal.close();
               }
               catch (Throwable t)
               {
                  log.warn("Error closing connections.", t);
               }
            }

         });

   }

   protected void doShutdown()
   {
      if (pool != null)
      {
         ((HikariDataSource) pool).close();
      }
   }

   @Override
   public String getType()
   {
      if (type != null)
         return type;

      String driver = getDriver();
      driver = driver != null ? driver : getUrl();

      if (driver != null)
      {
         if (driver.indexOf("mysql") >= 0)
            return "mysql";

         if (driver.indexOf("postgres") >= 0)
            return "postgres";

         if (driver.indexOf("redshift") >= 0)
            return "redshift";

         if (driver.indexOf("sqlserver") >= 0)
            return "sqlserver";

         if (driver.indexOf("h2") >= 0)
            return "h2";
      }

      return null;
   }

   @Override
   public Results<Row> select(Collection coll, List<Term> columnMappedTerms) throws Exception
   {
      JdbcDb db = null;
      if (coll == null)
      {
         db = this;
      }
      else
      {
         db = (JdbcDb) coll.getDb();
      }

      String selectKey = (coll != null ? coll.getTableName() + "." : "") + "select";
      String selectSql = (String) Chain.peek().remove(selectKey);

      SqlQuery query = new SqlQuery(coll, columnMappedTerms);
      query.withDb(db);
      if (selectSql != null)
      {
         query.withSelectSql(selectSql);
      }

      return query.doSelect();
   }

   @Override
   public List<String> upsert(Collection table, List<Map<String, Object>> rows) throws Exception
   {
      for (Map<String, Object> row : rows)
      {
         for (String key : (Set<String>) new HashSet(row.keySet()))
         {
            if (table.getPropertyByColumnName(key) == null)
               row.remove(key);
         }
      }

      List upserted = JdbcUtils.upsert(getConnection(), table.getTableName(), table.getPrimaryIndex().getColumnNames(), rows);

      for (int i = 0; i < upserted.size(); i++)
      {
         String entityKey = table.encodeKey((Row) upserted.get(i));
         upserted.set(i, entityKey);
      }

      return upserted;
   }

   @Override
   public List<Integer> patch(Collection table, List<Map<String, Object>> rows) throws Exception
   {
      for (Map<String, Object> row : rows)
      {
         for (String key : (Set<String>) new HashSet(row.keySet()))
         {
            if (table.getPropertyByColumnName(key) == null)
               row.remove(key);
         }
      }

      return JdbcUtils.update(getConnection(), table.getTableName(), table.getPrimaryIndex().getColumnNames(), rows);
   }

   @Override
   public void delete(Collection table, List<Map<String, Object>> columnMappedIndexValues) throws Exception
   {
      if (columnMappedIndexValues.size() == 0)
         return;

      Map<String, Object> firstRow = columnMappedIndexValues.get(0);

      if (firstRow.size() == 1)
      {
         String keyCol = firstRow.keySet().iterator().next();

         List values = new ArrayList();
         for (Map entityKey : columnMappedIndexValues)
         {
            values.add(entityKey.values().iterator().next());
         }

         String sql = "";
         sql += " DELETE FROM " + quoteCol(table.getTableName());
         sql += " WHERE " + quoteCol(keyCol) + " IN (" + JdbcUtils.getQuestionMarkStr(columnMappedIndexValues.size()) + ")";
         JdbcUtils.execute(getConnection(), sql, values.toArray());
      }
      else
      {
         String sql = "";
         sql += " DELETE FROM " + quoteCol(table.getTableName());
         sql += " WHERE ";

         List values = new ArrayList();
         for (Map<String, Object> entityKey : columnMappedIndexValues)
         {
            if (values.size() > 0)
               sql += " OR ";
            sql += "(";

            int i = 0;
            for (String key : entityKey.keySet())
            {
               i++;
               if (i > 1)
                  sql += "AND ";
               sql += quoteCol(key) + " = ? ";
               values.add(entityKey.get(key));
            }
            sql += ")";
         }
         JdbcUtils.execute(getConnection(), sql, values.toArray());
      }

   }

   /**
    * Shortcut for getConnection(true);
    * 
    */
   public Connection getConnection() throws ApiException
   {
      Connection conn = getConnection(true);
      return conn;
   }

   /**
    * Returns a JDBC connection that is shared on the ThreadLocal
    * with autoCommit managed by this Db and an EngineListener.
    * 
    * @param managed
    * @return
    * @throws ApiException
    */
   public Connection getConnection(boolean managed) throws ApiException
   {
      return getConnection0(managed);
   }

   protected Connection getConnection0(boolean managed) throws ApiException
   {
      try
      {
         Connection conn = !managed ? null : ConnectionLocal.getConnection(this);
         if (conn == null && !isShutdown())
         {
            if (pool == null)
            {
               synchronized (this)
               {
                  if (pool == null)
                  {
                     pool = createConnectionPool();
                  }
               }
            }

            conn = pool.getConnection();

            if (managed)
            {
               conn.setAutoCommit(isAutoCommit());
               ConnectionLocal.putConnection(this, conn);
            }
         }

         return conn;
      }
      catch (Exception ex)
      {
         ApiException.throw500InternalServerError(ex, "Unable to get DB connection");
         return null;
      }
   }

   protected DataSource createConnectionPool() throws Exception
   {
      if (ddlUrls.size() > 0)
      {
         //createConnectionPool() should only be called once per DB
         //ddlUrls are used to initialize the db...this is really 
         //useful for things like embedded H2 db that are used for
         //unit tests.  It could also be used for db upgrade scripts etc.
         //
         //it might seem logical to create the pool and then use the
         //connection from the pool and close it but in practice that 
         //was found to potentially introduce unintended closed 
         //connection errors for some dbs...specifically h2 in testing
         //so the initialization does not use the pool.

         String driver = getDriver();
         if (driver != null)
            Class.forName(driver);

         Connection conn = null;
         try
         {
            String url = getUrl();
            conn = DriverManager.getConnection(url, getUser(), getPass());
            conn.setAutoCommit(false);
            for (String ddlUrl : ddlUrls)
            {
               JdbcUtils.runSql(conn, new URL(ddlUrl).openStream());
            }
            conn.commit();
         }
         catch (Exception ex)
         {
            log.warn("Error initializing db with supplied ddl.", ex);
            if (conn != null)
               conn.rollback();
            throw ex;
         }
         finally
         {
            JdbcUtils.close(conn);
         }
      }

      HikariConfig config = new HikariConfig();
      String driver = getDriver();
      config.setDriverClassName(driver);
      config.setJdbcUrl(getUrl());
      config.setUsername(getUser());
      config.setPassword(getPass());
      config.setMaximumPoolSize(Math.min(getPoolMax(), MAX_POOL_SIZE));

      if (isType("mysql"))
      {
         //-- this is required to remove STRICT_TRANS_TABLES which prevents upserting
         //-- existing rows without supplying the value of required columns
         //-- hikari seemed to be overriding 'sessionVariables' set on the jdbc url
         //-- so this was done to force the config
         config.setConnectionInitSql("SET @@SESSION.sql_mode= 'NO_ENGINE_SUBSTITUTION'");
      }
      else if (isType("sqlserver"))
      {
         //-- upserts won't work if you can't upsert an identity field
         //-- https://stackoverflow.com/questions/10116759/set-identity-insert-off-for-all-tables
         config.setConnectionInitSql("EXEC sp_MSforeachtable @command1=\"PRINT '?'; SET IDENTITY_INSERT ? ON\", @whereand = ' AND EXISTS (SELECT 1 FROM sys.columns WHERE object_id = o.id  AND is_identity = 1) and o.type = ''U'''");

      }

      DataSource pool = new HikariDataSource(config);

      return pool;
   }

   public static class ConnectionLocal
   {
      static Map<Db, Map<Thread, Connection>> dbToThreadMap = new Hashtable();
      static Map<Thread, Map<Db, Connection>> threadToDbMap = new Hashtable();

      public static void closeAll()
      {
         for (Thread thread : threadToDbMap.keySet())
         {
            try
            {
               close(thread);
            }
            catch (Exception ex)
            {
               ex.printStackTrace();
            }
         }

         //System.out.println(dbToThreadMap);
         //System.out.println(threadToDbMap);
      }

      public static Connection getConnection(Db db)
      {
         return getConnection(db, Thread.currentThread());
      }

      static Connection getConnection(Db db, Thread thread)
      {
         Map<Thread, Connection> threadToConnMap = dbToThreadMap.get(db);
         if (threadToConnMap == null)
            return null;

         return threadToConnMap.get(thread);
      }

      public static void putConnection(Db db, Connection connection)
      {
         putConnection(db, Thread.currentThread(), connection);
      }

      static void putConnection(Db db, Thread thread, Connection connection)
      {
         Map<Thread, Connection> threadToConnMap = dbToThreadMap.get(db);
         if (threadToConnMap == null)
         {
            threadToConnMap = new Hashtable();
            dbToThreadMap.put(db, threadToConnMap);
         }
         threadToConnMap.put(thread, connection);

         Map<Db, Connection> dbToConnMap = threadToDbMap.get(thread);
         if (dbToConnMap == null)
         {
            dbToConnMap = new Hashtable();
            threadToDbMap.put(thread, dbToConnMap);
         }
         dbToConnMap.put(db, connection);

      }

      public static void commit() throws Exception
      {
         Exception toThrow = null;

         Map<Db, Connection> dbToConnMap = threadToDbMap.get(Thread.currentThread());
         if (dbToConnMap != null)
         {
            java.util.Collection<Connection> connections = dbToConnMap.values();
            for (Connection conn : connections)
            {
               try
               {
                  if (!(conn.isClosed() || conn.getAutoCommit()))
                  {
                     conn.commit();
                  }
               }
               catch (Exception ex)
               {
                  if (toThrow == null)
                     toThrow = ex;
               }
            }
         }

         if (toThrow != null)
            throw toThrow;
      }

      public static void rollback() throws Exception
      {
         Exception toThrow = null;

         Map<Db, Connection> dbToConnMap = threadToDbMap.get(Thread.currentThread());
         if (dbToConnMap != null)
         {
            for (Connection conn : dbToConnMap.values())
            {
               try
               {
                  if (!(conn.isClosed() || conn.getAutoCommit()))
                  {
                     conn.rollback();
                  }
               }
               catch (Exception ex)
               {
                  if (toThrow == null)
                     toThrow = ex;
               }
            }
         }

         if (toThrow != null)
            throw toThrow;
      }

      public static void close() throws Exception
      {
         close(Thread.currentThread());
      }

      static void close(Thread thread) throws Exception
      {
         Exception toThrow = null;

         Map<Db, Connection> dbToConnMap = threadToDbMap.remove(thread);

         if (dbToConnMap != null)
         {
            List<Db> dbs = new ArrayList(dbToConnMap.keySet());

            for (Db db : dbs)//Connection conn : dbToConnMap.values())
            {
               //--
               //-- cleanup the reverse mapping first
               Map<Thread, Connection> threadToConnMap = dbToThreadMap.get(db);
               threadToConnMap.remove(thread);

               if (threadToConnMap.size() == 0)
                  dbToThreadMap.remove(db);
               //--
               //--

               try
               {
                  Connection conn = dbToConnMap.get(db);
                  if (!conn.isClosed())
                  {
                     conn.close();
                  }
               }
               catch (Exception ex)
               {
                  if (toThrow == null)
                     toThrow = ex;
               }
            }

            if (dbToConnMap.size() == 0)
               threadToDbMap.remove(thread);
         }

         if (toThrow != null)
            throw toThrow;
      }
   }

   @Override
   public void configDb() throws Exception
   {
      if (!isBootstrap())
      {
         return;
      }

      Connection conn = getConnection();

      DatabaseMetaData dbmd = conn.getMetaData();

      //-- only here to map jdbc type integer codes to strings ex "4" to "BIGINT" or whatever it is
      Map<String, String> types = new HashMap<String, String>();
      for (Field field : Types.class.getFields())
      {
         types.put(field.get(null) + "", field.getName());
      }
      //--

      //-- the first loop through is going to construct all of the
      //-- Tbl and Col objects.  There will be a second loop through
      //-- that caputres all of the foreign key relationships.  You
      //-- have to do the fk loop second becuase the reference pk
      //-- object needs to exist so that it can be set on the fk Col
      ResultSet rs = null;
      if (isType("sqlserver"))
         rs = dbmd.getTables(conn.getCatalog(), "dbo", "%", new String[]{"TABLE", "VIEW"});
      else
         rs = dbmd.getTables(conn.getCatalog(), "public", "%", new String[]{"TABLE", "VIEW"});
      //ResultSet rs = dbmd.getTables(null, "public", "%", new String[]{"TABLE", "VIEW"});
      boolean hasNext = rs.next();
      if (!hasNext)
      {
         rs = dbmd.getTables(conn.getCatalog(), null, "%", new String[]{"TABLE", "VIEW"});
         hasNext = rs.next();
      }
      if (hasNext)
         do
         {
            String tableCat = rs.getString("TABLE_CAT");
            String tableSchem = rs.getString("TABLE_SCHEM");
            String tableName = rs.getString("TABLE_NAME");
            //String tableType = rs.getString("TABLE_TYPE");

            if (includeTables.size() > 0 && !includeTables.containsKey(tableName))
               continue;

            Collection table = new Collection(tableName);
            withCollection(table);

            ResultSet colsRs = dbmd.getColumns(tableCat, tableSchem, tableName, "%");

            while (colsRs.next())
            {
               String colName = colsRs.getString("COLUMN_NAME");
               Object type = colsRs.getString("DATA_TYPE");
               String colType = types.get(type);

               boolean nullable = colsRs.getInt("NULLABLE") == DatabaseMetaData.columnNullable;

               Property column = new Property(colName, colType, nullable);
               table.withProperties(column);
            }
            colsRs.close();

            ResultSet indexMd = dbmd.getIndexInfo(conn.getCatalog(), null, tableName, true, false);
            while (indexMd.next())
            {
               String idxName = indexMd.getString("INDEX_NAME");
               String idxType = "Other";
               String colName = indexMd.getString("COLUMN_NAME");

               if (idxName == null || colName == null)
               {
                  //WDB 2020-02-14 this was put in because SqlServer was 
                  //found to be returning indexes without names.
                  continue;
               }

               switch (indexMd.getInt("TYPE"))
               {
                  case DatabaseMetaData.tableIndexClustered:
                     idxType = "Clustered";
                  case DatabaseMetaData.tableIndexHashed:
                     idxType = "Hashed";
                  case DatabaseMetaData.tableIndexOther:
                     idxType = "Other";
                  case DatabaseMetaData.tableIndexStatistic:
                     idxType = "Statistic";
               }

               Property column = table.getProperty(colName);
               Object nonUnique = indexMd.getObject("NON_UNIQUE") + "";
               boolean unique = !(nonUnique.equals("true") || nonUnique.equals("1"));

               //this looks like it only supports single column indexes but if
               //an index with this name already exists, that means this is another
               //column in that index.
               table.withIndex(idxName, idxType, unique, column.getColumnName());

            }
            indexMd.close();

         }
         while (rs.next());
      rs.close();

      //-- now link all of the fks to pks
      //-- this is done after the first loop
      //-- so that all of the tbls/cols are
      //-- created first and are there to
      //-- be connected
      rs = dbmd.getTables(conn.getCatalog(), "public", "%", new String[]{"TABLE"});
      hasNext = rs.next();
      if (!hasNext)
      {
         rs = dbmd.getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"});
         hasNext = rs.next();
      }
      if (hasNext)
         do
         {
            String tableName = rs.getString("TABLE_NAME");

            //            System.out.println(tableName);
            //            
            //            ResultSetMetaData rsmd = rs.getMetaData();
            //            for(int i = 1; i<= rsmd.getColumnCount(); i++)
            //            {
            //               String name = rsmd.getColumnName(i);
            //               System.out.println(name + " = " + rs.getObject(name)); 
            //               
            //            }

            ResultSet keyMd = dbmd.getImportedKeys(conn.getCatalog(), null, tableName);
            while (keyMd.next())
            {
               //String pkName = keyMd.getString("PK_NAME");
               String fkName = keyMd.getString("FK_NAME");

               String fkTableName = keyMd.getString("FKTABLE_NAME");
               String fkColumnName = keyMd.getString("FKCOLUMN_NAME");
               String pkTableName = keyMd.getString("PKTABLE_NAME");
               String pkColumnName = keyMd.getString("PKCOLUMN_NAME");

               Property fk = getProperty(fkTableName, fkColumnName);
               Property pk = getProperty(pkTableName, pkColumnName);
               fk.withPk(pk);

               Collection coll = getCollectionByTableName(fkTableName);
               if (coll != null)
               {
                  //System.out.println("FOREIGN_KEY: " + tableName + " - " + pkName + " - " + fkName + "- " + fkTableName + "." + fkColumnName + " -> " + pkTableName + "." + pkColumnName);
                  coll.withIndex(fkName, "FOREIGN_KEY", false, fk.getColumnName());
               }

            }
            keyMd.close();
         }
         while (rs.next());

      rs.close();
   }

   @Override
   public Set<Term> mapToColumns(Collection collection, Term term)
   {
      Set terms = new HashSet();

      if (term.getParent() == null)
         terms.add(term);

      if (collection == null)
         return terms;

      if (term.isLeaf() && !term.isQuoted())
      {
         String token = term.getToken();

         while (token.startsWith("-") || token.startsWith("+"))
            token = token.substring(1, token.length());

         String name = "";
         String[] parts = token.split("\\.");

         //         if (parts.length > 2)//this could be a literal
         //            throw new ApiException("You can only specify a single level of relationship in dotted attributes: '" + token + "'");

         for (int i = 0; i < parts.length; i++)
         {
            String part = parts[i];

            if (i == parts.length - 1)
            {
               Property attr = collection.findProperty(parts[i]);

               if (attr != null)
                  name += attr.getColumnName();
               else
                  name += parts[i];
            }
            else
            {
               Relationship rel = collection.getRelationship(part);

               if (rel != null)
               {
                  name += rel.getName() + ".";
                  collection = rel.getRelated();
               }
               else
               {
                  name += parts[i] + ".";
               }
            }
         }

         if (!Utils.empty(name))
         {
            if (term.getToken().startsWith("-"))
               name = "-" + name;
            term.withToken(name);
         }
      }
      else
      {
         for (Term child : term.getTerms())
         {
            terms.addAll(mapToColumns(collection, child));
         }
      }

      return terms;
   }

   public JdbcDb withType(String type)
   {
      if ("mysql".equals(type))
         withStringQuote('`');

      return super.withType(type);
   }

   public JdbcDb withConfig(String driver, String url, String user, String pass)
   {
      withDriver(driver);
      withUrl(url);
      withUser(user);
      withPass(pass);
      return this;
   }

   public String getDriver()
   {
      return Utils.getSysEnvPropStr(getName() + ".driver", driver);
   }

   public JdbcDb withDriver(String driver)
   {
      this.driver = driver;
      return this;
   }

   public String getUrl()
   {
      return Utils.getSysEnvPropStr(getName() + ".url", url);
   }

   public JdbcDb withUrl(String url)
   {
      this.url = url;
      return this;
   }

   public String getUser()
   {
      return Utils.getSysEnvPropStr(getName() + ".user", user);
   }

   public JdbcDb withUser(String user)
   {
      this.user = user;
      return this;
   }

   public String getPass()
   {
      return Utils.getSysEnvPropStr(getName() + ".pass", pass);
   }

   public JdbcDb withPass(String pass)
   {
      this.pass = pass;
      return this;
   }

   public int getPoolMin()
   {
      return poolMin;
   }

   public void setPoolMin(int poolMin)
   {
      this.poolMin = poolMin;
   }

   public int getPoolMax()
   {
      return poolMax;
   }

   public void setPoolMax(int poolMax)
   {
      this.poolMax = poolMax;
   }

   public int getIdleConnectionTestPeriod()
   {
      return idleConnectionTestPeriod;
   }

   public void setIdleConnectionTestPeriod(int idleConnectionTestPeriod)
   {
      this.idleConnectionTestPeriod = idleConnectionTestPeriod;
   }

   public JdbcDb withStringQuote(char stringQuote)
   {
      this.stringQuote = stringQuote;
      return this;
   }

   public JdbcDb withColumnQuote(char columnQuote)
   {
      this.columnQuote = columnQuote;
      return this;
   }

   public String quoteCol(String columnName)
   {
      return columnQuote + columnName + columnQuote;
   }

   public String quoteStr(String string)
   {
      return stringQuote + string + stringQuote;
   }

   public int getRelatedMax()
   {
      return relatedMax;
   }

   public JdbcDb withRelatedMax(int relatedMax)
   {
      this.relatedMax = relatedMax;
      return this;
   }

   public JdbcDb withDdlUrl(String... ddlUrl)
   {
      for (int i = 0; ddlUrl != null && i < ddlUrl.length; i++)
      {
         ddlUrls.add(ddlUrl[i]);
      }

      return this;
   }

   public boolean isAutoCommit()
   {
      return autoCommit;
   }

   public JdbcDb withAutoCommit(boolean autoCommit)
   {
      this.autoCommit = autoCommit;
      return this;
   }

}
