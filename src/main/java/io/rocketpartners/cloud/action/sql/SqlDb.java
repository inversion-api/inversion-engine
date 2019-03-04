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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import ch.qos.logback.classic.Level;
import io.rocketpartners.cloud.model.ApiException;
import io.rocketpartners.cloud.model.ArrayNode;
import io.rocketpartners.cloud.model.Attribute;
import io.rocketpartners.cloud.model.Collection;
import io.rocketpartners.cloud.model.Column;
import io.rocketpartners.cloud.model.Db;
import io.rocketpartners.cloud.model.Entity;
import io.rocketpartners.cloud.model.ObjectNode;
import io.rocketpartners.cloud.model.Relationship;
import io.rocketpartners.cloud.model.Request;
import io.rocketpartners.cloud.model.Results;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.model.Table;
import io.rocketpartners.cloud.rql.Term;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.utils.Rows;
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

   transient boolean              shutdown                 = false;

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
               ex.printStackTrace();
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

   @Override
   public String getType()
   {
      if (type != null)
         return type;

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

   public void shutdown()
   {
      shutdown = true;

      synchronized (this)
      {
         //pool.close();
      }
   }

   public Rows selectRelatedEntityKeys(Relationship rel, List<ObjectNode> parentObjs) throws Exception
   {
      if (rel.isManyToOne())
      {
         return selectManyToOneRelatedEntityKeys(rel, parentObjs);
      }
      else if (rel.isManyToMany())
      {
         return selectManyToManyRelatedEntityKeys(rel, parentObjs);
      }
      else
      {
         throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "There is no reason to call this method with a ONE_TO_MANY relationship, you already have the keys of the related entities.");
      }
   }

   //MANY_TO_ONE - Location.id <- Player.locationId
   protected Rows selectManyToOneRelatedEntityKeys(Relationship rel, List<ObjectNode> parentObjs) throws Exception
   {
      Collection childCollection = api.getCollection(rel.getRelated().getTable());
      if (rel.isManyToMany())
         childCollection = api.getCollection(rel.getFkCol2().getPk().getTable());

      String relTbl = childCollection.getTable().getName();

      String parentPkCol = rel.getFkCol1().getPk().getName();
      String childFkCol = rel.getFkCol1().getName();
      String childPkCol = childCollection.getEntity().getKey().getColumn().getName();

      List parentIds = new ArrayList();
      for (ObjectNode parentObj : parentObjs)
      {
         parentIds.add(parentObj.get(parentPkCol));
         if (!(parentObj.get(rel.getName()) instanceof ArrayNode))
            parentObj.put(rel.getName(), new ArrayNode());
      }

      String sql = "";
      sql += " SELECT " + quoteCol(childFkCol) + ", " + quoteCol(childPkCol) + " FROM " + quoteCol(relTbl);
      sql += " WHERE " + quoteCol(childFkCol) + " IN (" + SqlUtils.getQuestionMarkStr(parentIds.size()) + ")";

      if (Chain.getRequest().isDebug())
      {
         Chain.getResponse().debug(sql);
         if (parentIds.size() > 0)
         {
            Chain.getResponse().debug(parentIds);
         }
      }

      Rows relatedEntityKeys = SqlUtils.selectRows(getConnection(), sql, parentIds);
      return relatedEntityKeys;
   }

   protected Rows selectManyToManyRelatedEntityKeys(Relationship rel, List<ObjectNode> parentObjs) throws Exception
   {
      //ex going from Category(id)->CategoryBooks(categoryId, bookId)->Book(id)
      String parentPkCol = rel.getFkCol1().getPk().getName();
      String linkTbl = rel.getFkCol1().getTable().getName();
      String linkTblParentFkCol = rel.getFkCol1().getName();
      String linkTblChildFkCol = rel.getFkCol2().getName();

      List parentIds = new ArrayList();
      for (ObjectNode parentObj : parentObjs)
      {
         parentIds.add(parentObj.get(parentPkCol));

         if (!(parentObj.get(rel.getName()) instanceof ArrayNode))
            parentObj.put(rel.getName(), new ArrayNode());
      }

      String sql = " SELECT " + quoteCol(linkTblParentFkCol) + ", " + quoteCol(linkTblChildFkCol) + //
            " FROM " + quoteCol(linkTbl) + //
            " WHERE " + quoteCol(linkTblChildFkCol) + " IS NOT NULL " + //
            " AND " + quoteCol(linkTblParentFkCol) + " IN(" + SqlUtils.getQuestionMarkStr(parentIds.size()) + ") ";

      if (Chain.getRequest().isDebug())
      {
         Chain.getResponse().debug(sql);
         if (parentIds.size() > 0)
         {
            Chain.getResponse().debug(parentIds);
         }
      }

      Rows relatedEntityKeys = SqlUtils.selectRows(getConnection(), sql, parentIds);
      return relatedEntityKeys;
   }

   public Rows select(Request request, Table table, Column toMatch, Column toRetrieve, List<Object> matchValues) throws Exception
   {
      String sql = "";
      sql += " SELECT " + quoteCol(toMatch.getName()) + ", " + quoteCol(toRetrieve.getName());
      sql += " FROM " + quoteCol(table.getName());
      sql += " WHERE " + quoteCol(toMatch.getName()) + " IN (" + SqlUtils.getQuestionMarkStr(matchValues.size()) + ")";
      return SqlUtils.selectRows(getConnection(), sql, matchValues);
      //      return SqlUtils.selectRows(getConnection(), sql);
   }

   @Override
   public Results<Map<String, Object>> select(Request req, Table table, List<Term> columnMappedTerms) throws Exception
   {
      Collection collection = null;
      try
      {
         collection = req.getCollection();
      }
      catch (ApiException e)
      {
         // need to try catch this because getCollection throws an exception if the collection isn't found
         // but not having a collection isn't always an error in this handler because a previous handler 
         // like the SqlSuggestHandler or ScriptHandler may have set the "sql" chain param. 
      }

      String dbname = (String) req.getChain().get("db");
      SqlDb db = (SqlDb) (collection != null ? collection.getDb() : api.getDb(dbname));
      if (db == null)
      {
         throw new ApiException(SC.SC_404_NOT_FOUND, "Unable to map request to a db table or query. Please check your endpoint.");
      }

      Entity entity = collection != null ? collection.getEntity() : null;
      Table tbl = entity != null ? entity.getTable() : null;

      String sql = (String) req.getChain().remove("select");
      if (Utils.empty(sql))
         sql = " SELECT * FROM " + tbl.getName();

      SqlQuery query = new SqlQuery(table, columnMappedTerms);
      query.withSelectSql(sql);
      query.withDb(db);

      return query.doSelect();

   }

   @Override
   public String upsert(Request request, Table table, Map<String, Object> row) throws Exception
   {
      if (isType("mysql"))
      {
         return mysqlUpsert(request, table, row);
      }
      else if (isType("h2"))
      {
         return h2Upsert(request, table, row);
      }
      else
      {
         throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Need to implement SqlDb.upsert for db type '" + getType() + "'");
      }
   }

   public String mysqlUpsert(Request request, Table table, Map<String, Object> row) throws Exception
   {
      return null;
   }

   public String h2Upsert(Request request, Table table, Map<String, Object> row) throws Exception
   {
      String keyCol = table.getKeyName();
      Object key = row.get(keyCol);

      SqlUtils.upsert(getConnection(), table.getName(), keyCol, row);
      Object scopeIdentity = SqlUtils.selectInt(getConnection(), "SELECT SCOPE_IDENTITY()");

      key = key == null && scopeIdentity != null ? scopeIdentity : key;

      return key != null ? key.toString() : null;
   }

   public void delete(Request request, Table table, String entityKey) throws Exception
   {
      String key = table.getKeyName();
      if (key == null)
         throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "TODO: add support for multi part key deletions.");

      SqlUtils.deleteRow(getConnection(), table.getName(), key, entityKey);
   }

   public Connection getConnection() throws ApiException
   {
      try
      {
         Connection conn = ConnectionLocal.getConnection(this);
         if (conn == null && !shutdown)
         {
            String dsKey = getName() + getUrl() + getUser() + getPass();

            DataSource pool = pools.get(dsKey);

            if (pool == null)
            {
               synchronized (pools)
               {
                  pool = pools.get(getName());

                  if (pool == null && !shutdown)
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
               Connection conn = conns.get(db);

               if (!conn.getAutoCommit())
               {
                  try
                  {
                     conn.commit();
                  }
                  catch (Exception ex)
                  {
                     if (toThrow != null)
                        toThrow = ex;
                  }
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

   public void bootstrapApi()
   {
      try
      {
         reflectDb();
         configApi();
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

               table.withIndex(column, idxName, idxType, unique);

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
               String fkTableName = keyMd.getString("FKTABLE_NAME");
               String fkColumnName = keyMd.getString("FKCOLUMN_NAME");
               String pkTableName = keyMd.getString("PKTABLE_NAME");
               String pkColumnName = keyMd.getString("PKCOLUMN_NAME");

               Column fk = getColumn(fkTableName, fkColumnName);
               Column pk = getColumn(pkTableName, pkColumnName);
               fk.withPk(pk);

               log.info(fkTableName + "." + fkColumnName + " -> " + pkTableName + "." + pkColumnName);
            }
            keyMd.close();
         }
         while (rs.next());

      rs.close();

      //-- if a table has two columns and both are foreign keys
      //-- then it is a relationship table for MANY_TO_MANY relationships
      for (Table table : getTables())
      {
         List<Column> cols = table.getColumns();
         if (cols.size() == 2 && cols.get(0).isFk() && cols.get(1).isFk())
         {
            table.withLinkTbl(true);
         }
      }
   }

   public void configApi() throws Exception
   {
      for (Table table : getTables())
      {
         List<Column> cols = table.getColumns();
         String name = beautifyCollectionName(table.getName());

         Collection collection = api.withCollection(table, name);
         if (getCollectionPath() != null)
            collection.withIncludePath(getCollectionPath());

         Entity entity = collection.getEntity();

         for (Attribute attr : entity.getAttributes())
         {
            attr.withName(beautifyAttributeName(attr.getName()));
         }

         String debug = getCollectionPath();
         debug = (debug == null ? "" : (debug + "/")) + collection;
         System.out.println("CREATING COLLECTION: " + debug);
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
            Column fkCol1 = t.getColumns().get(0);
            Column fkCol2 = t.getColumns().get(1);

            //MANY_TO_MANY one way
            {
               Entity pkEntity = api.getEntity(fkCol1.getPk().getTable());
               Relationship r = new Relationship();
               pkEntity.withRelationship(r);
               r.withEntity(pkEntity);
               Entity related = api.getEntity(fkCol2.getPk().getTable());
               r.withRelated(related);

               String hint = "MANY_TO_MANY - ";
               hint += fkCol1.getPk().getTable().getName() + "." + fkCol1.getPk().getName();
               hint += " <- " + fkCol1.getTable().getName() + "." + fkCol1.getName() + ":" + fkCol2.getName();
               hint += " -> " + fkCol2.getPk().getTable().getName() + "." + fkCol2.getPk().getName();

               r.withHint(hint);
               r.withType(Relationship.REL_MANY_TO_MANY);
               r.withFkCol1(fkCol1);
               r.withFkCol2(fkCol2);

               //Collection related = api.getCollection(fkCol2.getTbl());
               String name = makeRelationshipName(r);
               r.withName(name);
            }

            //MANY_TO_MANY the other way
            {
               Entity pkEntity = api.getEntity(fkCol2.getPk().getTable());
               Relationship r = new Relationship();
               pkEntity.withRelationship(r);
               r.withEntity(pkEntity);

               Entity related = api.getEntity(fkCol1.getPk().getTable());
               r.withRelated(related);

               //r.setRelated(api.getEntity(fkCol1.getTable()));

               String hint = "MANY_TO_MANY - ";
               hint += fkCol2.getPk().getTable().getName() + "." + fkCol2.getPk().getName();
               hint += " <- " + fkCol2.getTable().getName() + "." + fkCol2.getName() + ":" + fkCol1.getName();
               hint += " -> " + fkCol1.getPk().getTable().getName() + "." + fkCol1.getPk().getName();

               r.withHint(hint);
               r.withType(Relationship.REL_MANY_TO_MANY);
               r.withFkCol1(fkCol2);
               r.withFkCol2(fkCol1);

               String name = makeRelationshipName(r);
               r.withName(name);
            }
         }
         else
         {
            for (Column col : t.getColumns())
            {
               if (col.isFk())
               {
                  Column fkCol = col;
                  Table fkTbl = fkCol.getTable();
                  Entity fkEntity = api.getEntity(fkTbl);

                  Column pkCol = col.getPk();
                  Table pkTbl = pkCol.getTable();
                  Entity pkEntity = api.getEntity(pkTbl);

                  if (pkEntity == null)
                  {
                     System.err.println("Unknown Entity for table: " + pkTbl);
                     continue;
                  }

                  //ONE_TO_MANY
                  {
                     Relationship r = new Relationship();

                     //TODO:this name may not be specific enough or certain types
                     //of relationships. For example where an entity is related
                     //to another entity twice
                     r.withHint("MANY_TO_ONE - " + pkTbl.getName() + "." + pkCol.getName() + " <- " + fkTbl.getName() + "." + fkCol.getName());
                     r.withType(Relationship.REL_MANY_TO_ONE);
                     r.withFkCol1(fkCol);
                     r.withEntity(pkEntity);
                     r.withRelated(fkEntity);
                     r.withName(makeRelationshipName(r));
                     pkEntity.withRelationship(r);
                  }

                  //MANY_TO_ONE
                  {
                     Relationship r = new Relationship();
                     r.withHint("ONE_TO_MANY - " + fkTbl.getName() + "." + fkCol.getName() + " -> " + pkTbl.getName() + "." + pkCol.getName());
                     r.withType(Relationship.REL_ONE_TO_MANY);
                     r.withFkCol1(fkCol);
                     r.withEntity(fkEntity);
                     r.withRelated(pkEntity);
                     r.withName(makeRelationshipName(r));
                     fkEntity.withRelationship(r);
                  }
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

   public String getDriver()
   {
      return driver;
   }

   public SqlDb withConfig(String driver, String url, String user, String pass)
   {
      withDriver(driver);
      withUrl(url);
      withUser(user);
      withPass(pass);
      return this;
   }

   public SqlDb withDriver(String driver)
   {
      this.driver = driver;
      return this;
   }

   public String getUrl()
   {
      return url;
   }

   public SqlDb withUrl(String url)
   {
      this.url = url;
      return this;
   }

   public String getUser()
   {
      return user;
   }

   public SqlDb withUser(String user)
   {
      this.user = user;
      return this;
   }

   public String getPass()
   {
      return pass;
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

   public void withStringQuote(char stringQuote)
   {
      this.stringQuote = stringQuote;
   }

   public void withColumnQuote(char columnQuote)
   {
      this.columnQuote = columnQuote;
   }

   public String quoteCol(String columnName)
   {
      return columnQuote + columnName + columnQuote;
   }

   public String quoteStr(String string)
   {
      return stringQuote + string + stringQuote;
   }

}
