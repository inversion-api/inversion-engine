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
package io.inversion.jdbc;

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

import org.apache.commons.configuration2.Configuration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.inversion.Api;
import io.inversion.Api.ApiListener;
import io.inversion.ApiException;
import io.inversion.Chain;
import io.inversion.Collection;
import io.inversion.Db;
import io.inversion.Property;
import io.inversion.Request;
import io.inversion.Response;
import io.inversion.Results;
import io.inversion.jdbc.JdbcUtils.SqlListener;
import io.inversion.rql.Term;
import io.inversion.utils.Config;
import io.inversion.utils.Rows.Row;
import io.inversion.utils.Utils;

/**
 * Exposes the tables of a JDBC data source as REST <code>Collections</code>.
 */
public class JdbcDb extends Db<JdbcDb>
{
   static Map<String, String> DEFAULT_DRIVERS = new HashMap();

   static
   {
      DEFAULT_DRIVERS.put("h2", "org.h2.Driver");
      DEFAULT_DRIVERS.put("mysql", "com.mysql.cj.jdbc.Driver");
      DEFAULT_DRIVERS.put("postgres", "org.postgresql.Driver");
      DEFAULT_DRIVERS.put("sqlserver", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
   }

   static Map<Db, DataSource> pools                    = new Hashtable();

   protected char             stringQuote              = '\'';
   protected char             columnQuote              = '"';

   /**
    * The JDBC driver class name.
    */
   protected String           driver                   = null;

   /**
    * The JDBC url.  
    * <p>
    * Generally you will want to have this value dependency inject this at runtime by setting "${name}.url=${MY_DB_URL}" somewhere
    * where it can be discovered by <code>Config</code>...for example as an environment variable or in an inversion.properties file.
    * 
    * @see Config
    * @see Configuration
    */
   protected String           url                      = null;

   /**
    * The JDBC username.  
    * <p>
    * Generally you will want to have this value dependency inject this at runtime by setting "${name}.user=${MY_DB_USER_NAME}" somewhere
    * where it can be discovered by <code>Config</code>...for example as an environment variable or in an inversion.properties file.
    * 
    * @see Config
    * @see Configuration
    */
   protected String           user                     = null;

   /**
    * The JDBC password.  
    * <p>
    * Generally you will want to have this value dependency inject this at runtime by setting "${name}.user=${MY_DB_USER_NAME}" somewhere
    * where it can be discovered by <code>Config</code>...for example as an environment variable or in an inversion.properties file.
    * 
    * @see Config
    * @see Configuration
    */
   protected String           pass                     = null;

   /**
    * The maximum number of connections in the JDBC Connection pool, defaults to 50.
    */
   protected int              poolMax                  = 50;

   protected int              idleConnectionTestPeriod = 3600;           // in seconds

   /**
    * Should the JDBC connection be set to autoCommit.
    * <p>
    * By default, autoCommit is false because the system is setup to execute all sql statements for a single Request
    * inside of one transaction that commits just before the root Response is returned to the caller.
    * 
    */
   protected boolean          autoCommit               = false;

   /**
    * For MySQL only, set this to false to turn off SQL_CALC_FOUND_ROWS and SELECT FOUND_ROWS()
    */
   protected boolean          calcRowsFound            = true;

   /**
    * Urls to DDL files that should be executed on startup of this Db.
    */
   protected List<String>     ddlUrls                  = new ArrayList();

   static
   {
      JdbcUtils.addSqlListener(new SqlListener()
         {
            ch.qos.logback.classic.Logger log = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(JdbcDb.class);

            @Override
            public void onError(String method, String sql, Object args, Exception ex)
            {
               if (method != null && method.equals("selectRows"))
               {
                  log.error("SQL error in '" + method + "' [" + sql.replace("\r\n", "") + "] " + ex.getMessage());
               }
               else
               {
                  log.warn(ex.getMessage(), ex);
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
                  JdbcConnectionLocal.commit();
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
                  JdbcConnectionLocal.rollback();
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
                  JdbcConnectionLocal.close();
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
      if (isType("h2"))
      {
         try
         {
            String url = getUrl().toUpperCase();
            if (url.indexOf(":MEM:") > 0 && url.indexOf("DB_CLOSE_DELAY=-1") > 0)
            {
               JdbcUtils.execute(getConnection(), "SHUTDOWN");
            }
         }
         catch (Exception ex)
         {
            ex.printStackTrace();
         }
      }

      DataSource pool = pools.get(this);
      if (pool != null)
      {
         System.out.println("CLOSING CONNECTION POOL : " + getUrl());
         ((HikariDataSource) pool).close();
      }
   }

   @Override
   public String getType()
   {
      if (type != null)
         return type;

      String url = getUrl();
      url = url != null ? url : getUrl();

      if (url != null)
      {
         if (url.indexOf("mysql") >= 0)
            return "mysql";

         if (url.indexOf("postgres") >= 0)
            return "postgres";

         if (url.indexOf("redshift") >= 0)
            return "redshift";

         if (url.indexOf("sqlserver") >= 0)
            return "sqlserver";

         if (url.indexOf("h2") >= 0)
            return "h2";
      }

      return null;
   }

   @Override
   public Results doSelect(Collection coll, List<Term> columnMappedTerms) throws ApiException
   {
      SqlQuery query = new SqlQuery(this, coll, columnMappedTerms);
      return query.doSelect();
   }

   @Override
   public List<String> doUpsert(Collection table, List<Map<String, Object>> rows) throws ApiException
   {
      try
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
            String resourceKey = table.encodeResourceKey((Row) upserted.get(i));
            upserted.set(i, resourceKey);
         }

         return upserted;
      }
      catch (Exception ex)
      {
         ApiException.throw500InternalServerError(ex);
      }
      return null;
   }

   @Override
   public void doPatch(Collection table, List<Map<String, Object>> rows) throws ApiException
   {
      try
      {
         for (Map<String, Object> row : rows)
         {
            for (String key : (Set<String>) new HashSet(row.keySet()))
            {
               if (table.getPropertyByColumnName(key) == null)
                  row.remove(key);
            }
         }

         JdbcUtils.update(getConnection(), table.getTableName(), table.getPrimaryIndex().getColumnNames(), rows);
      }
      catch (Exception ex)
      {
         ApiException.throw500InternalServerError(ex);
      }
   }

   @Override
   public void delete(Collection table, List<Map<String, Object>> columnMappedIndexValues) throws ApiException
   {
      try
      {
         if (columnMappedIndexValues.size() == 0)
            return;

         Map<String, Object> firstRow = columnMappedIndexValues.get(0);

         if (firstRow.size() == 1)
         {
            String keyCol = firstRow.keySet().iterator().next();

            List values = new ArrayList();
            for (Map resourceKey : columnMappedIndexValues)
            {
               values.add(resourceKey.values().iterator().next());
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
            for (Map<String, Object> resourceKey : columnMappedIndexValues)
            {
               if (values.size() > 0)
                  sql += " OR ";
               sql += "(";

               int i = 0;
               for (String key : resourceKey.keySet())
               {
                  i++;
                  if (i > 1)
                     sql += "AND ";
                  sql += quoteCol(key) + " = ? ";
                  values.add(resourceKey.get(key));
               }
               sql += ")";
            }
            JdbcUtils.execute(getConnection(), sql, values.toArray());
         }
      }
      catch (Exception ex)
      {
         ApiException.throw500InternalServerError(ex);
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
         Connection conn = !managed ? null : JdbcConnectionLocal.getConnection(this);
         if (conn == null)
         {
            DataSource pool = pools.get(this);

            if (pool == null)
            {
               synchronized (this)
               {
                  pool = pools.get(this);
                  if (pool == null)
                  {
                     pool = createConnectionPool();
                  }
                  pools.put(this, pool);
               }
            }

            conn = pool.getConnection();

            if (managed)
            {
               conn.setAutoCommit(isAutoCommit());
               JdbcConnectionLocal.putConnection(this, conn);
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
               if (Utils.empty(ddlUrl))
                  continue;

               if (ddlUrl.indexOf(":/") < 0)
                  ddlUrl = getClass().getClassLoader().getResource(ddlUrl).toString();

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

      System.out.println("CREATING CONNECTION POOL: " + getUser() + "@" + getUrl());

      HikariConfig config = new HikariConfig();
      String driver = getDriver();
      config.setDriverClassName(driver);
      config.setJdbcUrl(getUrl());
      config.setUsername(getUser());
      config.setPassword(getPass());
      config.setMaximumPoolSize(getPoolMax());

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
         //-- upserts won't work if you can't upsert an idresource field
         //-- https://stackoverflow.com/questions/10116759/set-idresource-insert-off-for-all-tables
         config.setConnectionInitSql("EXEC sp_MSforeachtable @command1=\"PRINT '?'; SET IDENTITY_INSERT ? ON\", @whereand = ' AND EXISTS (SELECT 1 FROM sys.columns WHERE object_id = o.id  AND is_idresource = 1) and o.type = ''U'''");

      }

      DataSource pool = new HikariDataSource(config);

      return pool;
   }

   @Override
   public void buildCollections() throws ApiException
   {
      ResultSet rs = null;

      try
      {

         if (!isBootstrap())
         {
            return;
         }

         //this conn is managed by the JdbcConnectionLocal, this looks like a connection leak but is not
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

         if (isType("sqlserver"))
         {
            String schema = getUrl();
            int idx = schema.toLowerCase().indexOf("databasename=");
            if (idx > 0)
            {
               schema = schema.substring(idx);
               schema = Utils.substringBefore(schema, ";");
               schema = Utils.substringBefore(schema, "&");
            }
            else
            {
               schema = "dbo";
            }
            rs = dbmd.getTables(conn.getCatalog(), schema, "%", new String[]{"TABLE", "VIEW"});
         }
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
      catch (Exception ex)
      {
         ApiException.throw500InternalServerError(ex);
      }
      finally
      {
         Utils.close(rs);
      }

      //-- causes collection and property names to be beautified
      super.buildCollections();
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
      if (driver == null && url != null)
         return DEFAULT_DRIVERS.get(getType());

      return driver;
   }

   public JdbcDb withDriver(String driver)
   {
      this.driver = driver;
      return this;
   }

   public String getUrl()
   {
      return url;
   }

   public JdbcDb withUrl(String url)
   {
      this.url = url;
      return this;
   }

   public String getUser()
   {
      return user;
   }

   public JdbcDb withUser(String user)
   {
      this.user = user;
      return this;
   }

   public String getPass()
   {
      return pass;
   }

   public JdbcDb withPass(String pass)
   {
      this.pass = pass;
      return this;
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
