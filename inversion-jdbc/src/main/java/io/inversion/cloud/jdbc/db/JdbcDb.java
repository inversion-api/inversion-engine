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
import io.inversion.cloud.model.Property;
import io.inversion.cloud.model.Db;
import io.inversion.cloud.model.Endpoint;
import io.inversion.cloud.model.EngineListener;
import io.inversion.cloud.model.Index;
import io.inversion.cloud.model.Relationship;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.model.Results;
import io.inversion.cloud.model.SC;
import io.inversion.cloud.model.Collection;
import io.inversion.cloud.rql.Term;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.service.Engine;
import io.inversion.cloud.utils.Rows.Row;
import io.inversion.cloud.utils.Utils;

public class JdbcDb extends Db<JdbcDb>
{
   protected char                 stringQuote              = '\'';
   protected char                 columnQuote              = '"';

   static Map<String, DataSource> pools                    = new HashMap();

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

   @Override
   protected void doStartup()
   {
      try
      {
         api.withEngineListener(new EngineListener()
            {

               public void afterRequest(Engine engine, Api api, Endpoint endpoint, Chain chain, Request req, Response res)
               {
                  try
                  {
                     ConnectionLocal.commit();
                  }
                  catch (Exception ex)
                  {
                     throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Error comitting transaction.", ex);
                  }
               }

               public void beforeError(Engine engine, Api api, Endpoint endpoint, Chain chain, Request req, Response res)
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

               public void onFinally(Engine engine, Api api, Endpoint endpoint, Chain chain, Request req, Response res)
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

         if (isType("mysql"))
            withColumnQuote('`');

         super.doStartup();
      }
      catch (Exception ex)
      {
         ex.printStackTrace();
         Utils.rethrow(ex);
      }
   }

   protected void doShutdown()
   {
      //pool.close();
   }

   @Override
   public String getType()
   {
      if (type != null)
         return type;

      String driver = getDriver();
      if (driver != null)
      {
         if (driver.indexOf("mysql") >= 0)
            return "mysql";

         if (driver.indexOf("postgres") >= 0)
            return "postgres";

         if (driver.indexOf("redshift") >= 0)
            return "redshift";

         if (driver.indexOf("h2") >= 0)
            return "h2";
      }

      return null;
   }

   @Override
   public Results<Row> select(Collection table, List<Term> columnMappedTerms) throws Exception
   {
      JdbcDb db = null;
      if (table == null)
      {
         db = this;
      }
      else
      {
         db = (JdbcDb) table.getDb();
      }

      String selectKey = (table != null ? table.getTableName() + "." : "") + "select";

      String selectSql = (String) Chain.peek().remove(selectKey);

      SqlQuery query = new SqlQuery(table, columnMappedTerms);
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
      if (isType("h2"))
      {
         List keys = new ArrayList();
         for (Map<String, Object> row : rows)
         {
            keys.add(h2Upsert(table, row));
         }
         return keys;
      }
      else if (isType("mysql"))
      {
         return mysqlUpsert(table, rows);
      }
      else
      {
         throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Need to implement SqlDb.upsert for db type '" + getType() + "'");
      }
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

   public List<String> mysqlUpsert(Collection table, List<Map<String, Object>> rows) throws Exception
   {
      return JdbcUtils.mysqlUpsert(getConnection(), table.getTableName(), rows);
   }

   public String h2Upsert(Collection table, Map<String, Object> row) throws Exception
   {
      Object key = table.encodeKey(row);

      if (key == null)//this must be an insert
      {
         JdbcUtils.insertMap(getConnection(), table.getTableName(), row);
      }
      else
      {
         //String keyCol = table.getKeyName();
         JdbcUtils.h2Upsert(getConnection(), table.getTableName(), table.getPrimaryIndex(), row);
      }

      if (key == null)
      {
         key = JdbcUtils.selectInt(getConnection(), "SELECT SCOPE_IDENTITY()");
      }

      if (key == null)
         throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Unable to determine key of upserted row: " + row);

      return key.toString();
   }


   public Connection getConnection() throws ApiException
   {
      try
      {
         Connection conn = ConnectionLocal.getConnection(this);
         if (conn == null && !isShutdown())
         {
            String dsKey = "name=" + getName() + ", url=" + getUrl() + ", user=" + getUser();

            DataSource pool = pools.get(dsKey);

            if (pool == null)
            {
               synchronized (pools)
               {
                  //System.out.println("CREATING CONNECTION POOL: " + dsKey);

                  pool = pools.get(getName());

                  if (pool == null && !isShutdown())
                  {
                     pool = createConnectionPool();
                     pools.put(dsKey, pool);
                  }
               }
            }

            conn = pool.getConnection();
            conn.setAutoCommit(isAutoCommit());

            ConnectionLocal.putConnection(this, conn);
         }

         return conn;
      }
      catch (Exception ex)
      {
         log.error("Unable to get DB connection", ex);
         throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Unable to get DB connection", ex);
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

         Class.forName(getDriver());
         Connection conn = null;
         try
         {
            conn = DriverManager.getConnection(getUrl(), getUser(), getPass());
            conn.setAutoCommit(false);
            for (String ddlUrl : ddlUrls)
            {
               JdbcUtils.runDdl(conn, new URL(ddlUrl).openStream());
            }
            conn.commit();
         }
         catch (Exception ex)
         {
            conn.rollback();
            log.warn("Error initializing db with supplied ddl.", ex);
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
      DataSource pool = new HikariDataSource(config);

      return pool;
   }

   public static class ConnectionLocal
   {
      static ThreadLocal<Map<Db, Connection>> connections = new ThreadLocal();

      public static Map<Db, Connection> getConnections()
      {
         return connections.get();
      }

      public static Connection getConnection(Db db)
      {
         Map<Db, Connection> conns = connections.get();
         if (conns == null)
         {
            conns = new HashMap();
            connections.set(conns);
         }

         return conns.get(db);
      }

      public static void putConnection(Db db, Connection connection)
      {
         Map<Db, Connection> conns = connections.get();
         if (conns == null)
         {
            conns = new HashMap();
            connections.set(conns);
         }
         conns.put(db, connection);
      }

      public static void commit() throws Exception
      {
         Exception toThrow = null;
         Map<Db, Connection> conns = connections.get();
         if (conns != null)
         {
            for (Db db : (List<Db>) new ArrayList(conns.keySet()))
            {
               try
               {
                  Connection conn = conns.get(db);
                  if (!conn.isClosed() && !conn.getAutoCommit())
                  {
                     conn.commit();
                  }
               }
               catch (Exception ex)
               {
                  String msg = (ex.getMessage() + "").toLowerCase();
                  if (msg.indexOf("connection is closed") > -1)
                     continue;

                  if (toThrow != null)
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
         Map<Db, Connection> conns = connections.get();
         if (conns != null)
         {
            for (Db db : (List<Db>) new ArrayList(conns.keySet()))
            {
               Connection conn = conns.get(db);
               try
               {
                  conn.rollback();
               }
               catch (Exception ex)
               {
                  if (toThrow != null)
                     toThrow = ex;
               }
            }
         }

         if (toThrow != null)
            throw toThrow;
      }

      public static void close() throws Exception
      {
         Exception toThrow = null;
         Map<Db, Connection> conns = connections.get();
         if (conns != null)
         {
            for (Db db : (List<Db>) new ArrayList(conns.keySet()))
            {
               Connection conn = conns.get(db);
               try
               {
                  conn.close();
               }
               catch (Exception ex)
               {
                  if (toThrow != null)
                     toThrow = ex;
               }
            }
         }

         connections.remove();

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
      ResultSet rs = dbmd.getTables(conn.getCatalog(), "public", "%", new String[]{"TABLE", "VIEW"});
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

            int columnNumber = 0;
            while (colsRs.next())
            {
               columnNumber += 1;
               String colName = colsRs.getString("COLUMN_NAME");
               Object type = colsRs.getString("DATA_TYPE");
               String colType = types.get(type);

               boolean nullable = colsRs.getInt("NULLABLE") == DatabaseMetaData.columnNullable;

               Property column = new Property(colName, colType, nullable);
               table.withProperties(column);

               //               if (DELETED_FLAGS.contains(colName.toLowerCase()))
               //               {
               //                  table.setDeletedFlag(column);
               //               }
            }
            colsRs.close();

            ResultSet indexMd = dbmd.getIndexInfo(conn.getCatalog(), null, tableName, true, false);
            while (indexMd.next())
            {
               String idxName = indexMd.getString("INDEX_NAME");
               String idxType = "Other";
               String colName = indexMd.getString("COLUMN_NAME");

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
               String pkName = keyMd.getString("PK_NAME");
               String fkName = keyMd.getString("FK_NAME");

               String fkTableName = keyMd.getString("FKTABLE_NAME");
               String fkColumnName = keyMd.getString("FKCOLUMN_NAME");
               String pkTableName = keyMd.getString("PKTABLE_NAME");
               String pkColumnName = keyMd.getString("PKCOLUMN_NAME");

               Property fk = getProperty(fkTableName, fkColumnName);
               Property pk = getProperty(pkTableName, pkColumnName);
               fk.withPk(pk);

               Collection table = getCollection(fkTableName);
               if (table != null)
               {
                  //System.out.println("FOREIGN_KEY: " + tableName + " - " + pkName + " - " + fkName + "- " + fkTableName + "." + fkColumnName + " -> " + pkTableName + "." + pkColumnName);
                  table.withIndex(fkName, "FOREIGN_KEY", false, fk.getColumnName());
               }

            }
            keyMd.close();
         }
         while (rs.next());

      rs.close();

      //2019-02-11 WB - moved below code into Table.isLinkTable
      //      
      //      -- if a table has two columns and both are foreign keys
      //      -- then it is a relationship table for MANY_TO_MANY relationships
      //            for (Table table : getTables())
      //            {
      //               List<Column> cols = table.getColumns();
      //               if (cols.size() == 2 && cols.get(0).isFk() && cols.get(1).isFk())
      //               {
      //                  table.withLinkTbl(true);
      //               }
      //            }

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

               if (attr == null)
                  break;
               //throw new ApiException("Unable to identify related column for dotted attribute name: '" + token + "'");

               name += attr.getColumnName();
               break;
            }
            else
            {
               Relationship rel = collection.getRelationship(part);

               if (rel == null)
                  break;

               //               if (rel == null)
               //                  throw new ApiException("Unable to identify relationship for dotted attribute name: '" + token + "'");

               String aliasPrefix = "_join_" + rel.getEntity().getCollectionName() + "_" + part + "_";

               Term join = null;
               for (int j = 0; j < 2; j++)
               {
                  String relatedTable = rel.getRelated().getTableName();

                  if (rel.isManyToMany() && j == 0)
                     relatedTable = rel.getFk1Col1().getTable().getTableName();

                  String tableAlias = aliasPrefix + (j + 1);

                  List joinTerms = new ArrayList();
                  joinTerms.add(relatedTable);
                  joinTerms.add(tableAlias);

                  Index idx = j == 0 ? rel.getFkIndex1() : rel.getFkIndex2();
                  if (idx == null)
                     break;//will NOT be null only for M2M relationships

                  name = tableAlias + ".";

                  if (rel.isOneToMany())
                  {
                     for (int k = 0; k < idx.size(); k++)
                     {
                        Property col = idx.getColumn(k);
                        joinTerms.add(col.getTable().getTableName());
                        joinTerms.add(col.getColumnName());
                        joinTerms.add(tableAlias);
                        joinTerms.add(col.getPk().getColumnName());
                     }
                  }
                  else
                  {
                     if (j == 0)
                     {
                        for (int k = 0; k < idx.size(); k++)
                        {
                           Property col = idx.getColumn(k);
                           joinTerms.add(col.getPk().getTable().getTableName());
                           joinTerms.add(col.getPk().getColumnName());
                           joinTerms.add(tableAlias);
                           joinTerms.add(col.getColumnName());
                        }
                     }
                     else//second time through on M2M
                     {
                        for (int k = 0; k < idx.size(); k++)
                        {
                           Property col = idx.getColumn(k);
                           String m2mTbl = join.getToken(1);

                           joinTerms.add(m2mTbl);
                           joinTerms.add(col.getColumnName());
                           joinTerms.add(tableAlias);
                           joinTerms.add(col.getPk().getColumnName());
                        }
                     }
                  }

                  join = Term.term(null, "join", joinTerms);
                  terms.add(join);
               }

               collection = rel.getRelated();

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
      return Utils.findSysEnvPropStr(getName() + ".driver", driver);
   }

   public JdbcDb withDriver(String driver)
   {
      this.driver = driver;
      return this;
   }

   public String getUrl()
   {
      return Utils.findSysEnvPropStr(getName() + ".url", url);
   }

   public JdbcDb withUrl(String url)
   {
      this.url = url;
      return this;
   }

   public String getUser()
   {
      return Utils.findSysEnvPropStr(getName() + ".user", user);
   }

   public JdbcDb withUser(String user)
   {
      this.user = user;
      return this;
   }

   public String getPass()
   {
      return Utils.findSysEnvPropStr(getName() + ".pass", pass);
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
