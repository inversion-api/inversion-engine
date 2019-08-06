/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
 * http://rocketpartners.io
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package io.rocketpartners.cloud.action.sql;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import ch.qos.logback.classic.Level;
import io.rocketpartners.cloud.model.ApiException;
import io.rocketpartners.cloud.model.Attribute;
import io.rocketpartners.cloud.model.Collection;
import io.rocketpartners.cloud.model.Column;
import io.rocketpartners.cloud.model.Db;
import io.rocketpartners.cloud.model.Entity;
import io.rocketpartners.cloud.model.Index;
import io.rocketpartners.cloud.model.Relationship;
import io.rocketpartners.cloud.model.Results;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.model.Table;
import io.rocketpartners.cloud.rql.Term;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.utils.Rows.Row;
import io.rocketpartners.cloud.utils.SqlUtils;
import io.rocketpartners.cloud.utils.SqlUtils.SqlListener;
import io.rocketpartners.cloud.utils.Utils;

public class SqlDb extends Db<SqlDb>
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
   protected int                  idleConnectionTestPeriod = 3600;         // in seconds

   // set this to false to turn off SQL_CALC_FOUND_ROWS and SELECT FOUND_ROWS()
   // Only impacts 'mysql' types
   protected boolean              calcRowsFound            = true;

   protected int                  relatedMax               = 500;

   static
   {
      ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("ROOT");
      logger.setLevel(Level.WARN);
      //      ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("com.zaxxer.hikari.pool.PoolBase");
      //      logger.setLevel(Level.INFO);

      SqlUtils.addSqlListener(new SqlListener()
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
               args = (args != null && args.getClass().isArray() ? Arrays.asList((Object[]) args) : args);

               sql = sql.replaceAll("\r", "");
               sql = sql.replaceAll("\n", " ");
               sql = sql.trim().replaceAll(" +", " ");
               StringBuffer buff = new StringBuffer("");
               buff.append("SQL -> '").append(sql).append("'").append(" args=").append(args).append(" error='").append(ex != null ? ex.getMessage() : "").append("'");
               String msg = buff.toString();
               Chain.debug(msg);
            }
         });
   }

   public SqlDb()
   {

   }

   public SqlDb(String name)
   {
      withName(name);
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

   protected void shutdown0()
   {
      //pool.close();
   }

   @Override
   public Results<Row> select(Table table, List<Term> columnMappedTerms) throws Exception
   {
      SqlDb db = null;
      if (table == null)
      {
         db = this;
      }
      else
      {
         db = (SqlDb) table.getDb();
      }

      String selectKey = (table != null ? table.getKeyName() + "." : "") + "select";

      String sql = (String) Chain.peek().remove(selectKey);
      if (Utils.empty(sql))
      {
         if (table == null)
            throw new ApiException(SC.SC_400_BAD_REQUEST, "Table missing");
         sql = " SELECT * FROM " + quoteCol(table.getName());
      }

      SqlQuery query = new SqlQuery(table, columnMappedTerms);
      query.withSelectSql(sql);
      query.withDb(db);

      return query.doSelect();
   }

   @Override
   public String upsert(Table table, Map<String, Object> row) throws Exception
   {
      if (isType("h2"))
      {
         return h2Upsert(table, row);
      }
      else if (isType("mysql"))
      {
         return StringUtils.join(mysqlUpsert(table, row), ',');
      }
      else
      {
         throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Need to implement SqlDb.upsert for db type '" + getType() + "'");
      }
   }

   public List<String> mysqlUpsert(Table table, Map<String, Object> row) throws Exception
   {
      return mysqlUpsert(table, Arrays.asList(row));
   }

   public List<String> mysqlUpsert(Table table, List<Map<String, Object>> rows) throws Exception
   {
      return SqlUtils.mysqlUpsert(getConnection(), table.getName(), rows);
   }

   public String h2Upsert(Table table, Map<String, Object> row) throws Exception
   {
      Object key = table.encodeKey(row);

      if (key == null)//this must be an insert
      {
         SqlUtils.insertMap(getConnection(), table.getName(), row);
      }
      else
      {
         String keyCol = table.getKeyName();
         SqlUtils.upsert(getConnection(), table.getName(), keyCol, row);
      }

      if (key == null)
      {
         key = SqlUtils.selectInt(getConnection(), "SELECT SCOPE_IDENTITY()");
      }

      if (key == null)
         throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Unable to determine key of upserted row: " + row);

      return key.toString();
   }

   public void delete(Table table, List<String> entityKeys) throws Exception
   {
      Index pk = table.getPrimaryIndex();

      if (pk.getColumns().size() == 1)
      {
         List castKeys = new ArrayList();
         for (String key : entityKeys)
            castKeys.add(cast(pk.getColumn(0), key));

         String sql = "";
         sql += " DELETE FROM " + quoteCol(table.getName());
         sql += " WHERE " + quoteCol(pk.getColumn(0).getName()) + " IN (" + SqlUtils.getQuestionMarkStr(entityKeys.size()) + ")";
         SqlUtils.execute(getConnection(), sql, castKeys.toArray());
      }
      else
      {
         String sql = "";
         sql += " DELETE FROM " + quoteCol(table.getName());
         sql += " WHERE ";

         List values = new ArrayList();
         for (String entityKey : entityKeys)
         {
            if (values.size() > 0)
               sql += " OR ";
            sql += "(";
            Row row = table.decodeKey(entityKey);
            int i = 0;
            for (String key : row.keySet())
            {
               i++;
               if (i > 1)
                  sql += "AND ";
               sql += quoteCol(key) + " = ? ";
               values.add(row.get(key));
            }
            sql += ")";
         }
         SqlUtils.execute(getConnection(), sql, values.toArray());
      }
   }

   @Override
   public void delete(Table table, String entityKey) throws Exception
   {
      delete(table, Arrays.asList(entityKey));
   }

   public Connection getConnection() throws ApiException
   {
      try
      {
         Connection conn = ConnectionLocal.getConnection(this);
         if (conn == null && !isShutdown())
         {
            String dsKey = getName() + getUrl() + getUser() + getPass();

            DataSource pool = pools.get(dsKey);

            if (pool == null)
            {
               synchronized (pools)
               {
                  pool = pools.get(getName());

                  if (pool == null && !isShutdown())
                  {
                     //pool = JdbcConnectionPool.create("jdbc:h2:./northwind", "sa", "");

                     HikariConfig config = new HikariConfig();
                     config.setDriverClassName(getDriver());
                     config.setJdbcUrl(getUrl());
                     config.setUsername(getUser());
                     config.setPassword(getPass());
                     config.setMaximumPoolSize(Math.min(getPoolMax(), MAX_POOL_SIZE));
                     pool = new HikariDataSource(config);

                     pools.put(dsKey, pool);
                  }
               }
            }

            conn = pool.getConnection();
            conn.setAutoCommit(false);

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
   protected void startup0()
   {
      try
      {
         if (isType("mysql"))
            withColumnQuote('`');

         if (isBootstrap() && getTables().size() == 0)
         {
            reflectDb();
            configApi();
         }
      }
      catch (Exception ex)
      {
         Utils.rethrow(ex);
      }
   }

   public void reflectDb() throws Exception
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
      ResultSet rs = dbmd.getTables(null, "public", "%", new String[]{"TABLE", "VIEW"});
      boolean hasNext = rs.next();
      if (!hasNext)
      {
         rs = dbmd.getTables(null, null, "%", new String[]{"TABLE", "VIEW"});
         hasNext = rs.next();
      }
      if (hasNext)
         do
         {
            String tableCat = rs.getString("TABLE_CAT");
            String tableSchem = rs.getString("TABLE_SCHEM");
            String tableName = rs.getString("TABLE_NAME");
            //String tableType = rs.getString("TABLE_TYPE");

            //System.out.println(tableName);

            Table table = new Table(this, tableName);
            withTable(table);

            ResultSet colsRs = dbmd.getColumns(tableCat, tableSchem, tableName, "%");

            int columnNumber = 0;
            while (colsRs.next())
            {
               columnNumber += 1;
               String colName = colsRs.getString("COLUMN_NAME");
               Object type = colsRs.getString("DATA_TYPE");
               String colType = types.get(type);

               boolean nullable = colsRs.getInt("NULLABLE") == DatabaseMetaData.columnNullable;

               Column column = new Column(table, columnNumber, colName, colType, nullable);
               table.withColumn(column);

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

               Object nonUnique = indexMd.getObject("NON_UNIQUE") + "";
               boolean unique = !(nonUnique.equals("true") || nonUnique.equals("1"));

               Column column = table.getColumn(colName);

               if (unique)
               {
                  column.withUnique(unique);
               }

               table.makeIndex(column, idxName, idxType, unique);

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
      rs = dbmd.getTables(null, "public", "%", new String[]{"TABLE"});
      hasNext = rs.next();
      if (!hasNext)
      {
         rs = dbmd.getTables(null, null, "%", new String[]{"TABLE"});
         hasNext = rs.next();
      }
      if (hasNext)
         do
         {
            String tableName = rs.getString("TABLE_NAME");
            ResultSet keyMd = dbmd.getImportedKeys(conn.getCatalog(), null, tableName);
            while (keyMd.next())
            {
               String pkName = keyMd.getString("PK_NAME");
               String fkName = keyMd.getString("FK_NAME");

               String fkTableName = keyMd.getString("FKTABLE_NAME");
               String fkColumnName = keyMd.getString("FKCOLUMN_NAME");
               String pkTableName = keyMd.getString("PKTABLE_NAME");
               String pkColumnName = keyMd.getString("PKCOLUMN_NAME");

               Column fk = getColumn(fkTableName, fkColumnName);
               Column pk = getColumn(pkTableName, pkColumnName);
               fk.withPk(pk);

               getTable(fkTableName).makeIndex(fk, fkName, "FOREIGN_KEY", false);

               //System.out.println("FOREIGN_KEY: " + tableName + " - " + pkName + " - " + fkName + "- " + fkTableName + "." + fkColumnName + " -> " + pkTableName + "." + pkColumnName);
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

   public void configApi() throws Exception
   {
      List<String> relationshipStrs = new ArrayList();

      for (Table table : getTables())
      {
         if (table.isLinkTbl())
            continue;

         List<Column> cols = table.getColumns();
         String name = beautifyCollectionName(table.getName());

         Collection collection = api.makeCollection(table, name);
         if (getCollectionPath() != null)
            collection.withIncludePaths(getCollectionPath());

         Entity entity = collection.getEntity();

         for (Attribute attr : entity.getAttributes())
         {
            attr.withName(beautifyAttributeName(attr.getName()));
         }

         String debug = getCollectionPath();
         debug = (debug == null ? "" : (debug + collection));
         //System.out.println("CREATING COLLECTION: " + debug);
      }

      //-- Now go back through and create relationships for all foreign keys
      //-- two relationships objects are created for every relationship type
      //-- representing both sides of the relationship...ONE_TO_MANY also
      //-- creates a MANY_TO_ONE and there are always two for a MANY_TO_MANY.
      //-- API designers may want to represent one or both directions of the
      //-- relationship in their API and/or the names of the JSON properties
      //-- for the relationships will probably be different
      for (Table t : getTables())
      {
         if (t.isLinkTbl())
         {
            //create reciprocal pairs for of MANY_TO_MANY relationships
            //for each pair combination in the link table.
            List<Index> indexes = t.getIndexes();
            for (int i = 0; i < indexes.size(); i++)
            {
               for (int j = 0; j < indexes.size(); j++)
               {
                  Index idx1 = indexes.get(i);
                  Index idx2 = indexes.get(j);

                  if (i == j || !idx1.getType().equals("FOREIGN_KEY") || !idx2.getType().equals("FOREIGN_KEY"))
                     continue;

                  Entity entity1 = api.getEntity(idx1.getColumn(0).getPk().getTable());
                  Entity entity2 = api.getEntity(idx2.getColumn(0).getPk().getTable());

                  Relationship r = new Relationship();
                  r.withType(Relationship.REL_MANY_TO_MANY);

                  r.withRelated(entity2);
                  r.withFkIndex1(idx1);
                  r.withFkIndex2(idx2);
                  r.withName(makeRelationshipName(entity1, r));
                  r.withEntity(entity1);
                  relationshipStrs.add(r.toString());
               }
            }
         }
         else
         {
            for (Index fkIdx : t.getIndexes())
            {
               try
               {
                  if (!fkIdx.getType().equals("FOREIGN_KEY"))
                     continue;

                  Entity pkEntity = api.getEntity(fkIdx.getColumn(0).getPk().getTable());
                  Entity fkEntity = api.getEntity(fkIdx.getColumn(0).getTable());

                  //ONE_TO_MANY
                  {
                     Relationship r = new Relationship();
                     //TODO:this name may not be specific enough or certain types
                     //of relationships. For example where an entity is related
                     //to another entity twice
                     r.withType(Relationship.REL_MANY_TO_ONE);
                     r.withFkIndex1(fkIdx);
                     r.withRelated(fkEntity);
                     r.withName(makeRelationshipName(pkEntity, r));
                     r.withEntity(pkEntity);
                     relationshipStrs.add(r.toString());
                  }

                  //MANY_TO_ONE
                  {
                     Relationship r = new Relationship();
                     r.withType(Relationship.REL_ONE_TO_MANY);
                     r.withFkIndex1(fkIdx);
                     r.withRelated(pkEntity);
                     r.withName(makeRelationshipName(fkEntity, r));
                     r.withEntity(fkEntity);
                     relationshipStrs.add(r.toString());
                  }
               }
               catch (Exception ex)
               {
                  throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Error creating relationship for index: " + fkIdx, ex);
               }
            }
         }
      }
   }

   public SqlDb withType(String type)
   {
      if ("mysql".equals(type))
         withStringQuote('`');

      return super.withType(type);
   }

   public SqlDb withConfig(String driver, String url, String user, String pass)
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

   public SqlDb withDriver(String driver)
   {
      this.driver = driver;
      return this;
   }

   public String getUrl()
   {
      return Utils.findSysEnvPropStr(getName() + ".url", url);
   }

   public SqlDb withUrl(String url)
   {
      this.url = url;
      return this;
   }

   public String getUser()
   {
      return Utils.findSysEnvPropStr(getName() + ".user", user);
   }

   public SqlDb withUser(String user)
   {
      this.user = user;
      return this;
   }

   public String getPass()
   {
      return Utils.findSysEnvPropStr(getName() + ".pass", pass);
   }

   public SqlDb withPass(String pass)
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

   public SqlDb withStringQuote(char stringQuote)
   {
      this.stringQuote = stringQuote;
      return this;
   }

   public SqlDb withColumnQuote(char columnQuote)
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

   public SqlDb withRelatedMax(int relatedMax)
   {
      this.relatedMax = relatedMax;
      return this;
   }

}
