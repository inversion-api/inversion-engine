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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.rocketpartners.cloud.model.ApiException;
import io.rocketpartners.cloud.model.Attribute;
import io.rocketpartners.cloud.model.Collection;
import io.rocketpartners.cloud.model.Column;
import io.rocketpartners.cloud.model.Db;
import io.rocketpartners.cloud.model.Entity;
import io.rocketpartners.cloud.model.Relationship;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.model.Table;
import io.rocketpartners.cloud.utils.AutoWire.Ignore;
import io.rocketpartners.cloud.utils.English;

public class SqlDb extends Db
{
   static Map<String, DataSource> pools                    = new HashMap();

   public static final int        MIN_POOL_SIZE            = 3;
   public static final int        MAX_POOL_SIZE            = 10;

   @Ignore
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

   public Connection getConnection() throws ApiException
   {
      try
      {
         Connection conn = ConnectionLocal.getConnection(this);
         if (conn == null && !shutdown)
         {
            DataSource pool = pools.get(getName());

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

                     pools.put(getName(), pool);
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

   public void bootstrapApi() throws Exception
   {
      reflectDb();
      configApi();
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

      if (!rs.next())
         rs = dbmd.getTables(null, null, "%", new String[]{"TABLE", "VIEW"});
      if (rs.next())
         do
         {
            String tableCat = rs.getString("TABLE_CAT");
            String tableSchem = rs.getString("TABLE_SCHEM");
            String tableName = rs.getString("TABLE_NAME");
            //String tableType = rs.getString("TABLE_TYPE");

            //System.out.println(tableName);

            Table table = new Table(this, tableName);
            addTable(table);

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
               table.addColumn(column);

               //               if (DELETED_FLAGS.contains(colName.toLowerCase()))
               //               {
               //                  table.setDeletedFlag(column);
               //               }
            }
            colsRs.close();

            ResultSet indexMd = dbmd.getIndexInfo(conn.getCatalog(), null, tableName, true, false);
            while (indexMd.next())
            {
               String colName = indexMd.getString("COLUMN_NAME");
               Column col = getColumn(tableName, colName);
               col.setUnique(true);
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
      while (rs.next())
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
            fk.setPk(pk);

            //log.info(fkTableName + "." + fkColumnName + " -> " + pkTableName + "." + pkColumnName);
         }
         keyMd.close();
      }
      rs.close();

      //-- if a table has two columns and both are foreign keys
      //-- then it is a relationship table for MANY_TO_MANY relationships
      for (Table table : getTables())
      {
         List<Column> cols = table.getColumns();
         if (cols.size() == 2 && cols.get(0).isFk() && cols.get(1).isFk())
         {
            table.setLinkTbl(true);
         }
      }
   }

   protected String beautifyCollectionName(String inName)
   {
      String collectionName = inName;

      if (collectionName.toUpperCase().equals(collectionName))//crappy oracle style all uppercase name
         collectionName = collectionName.toLowerCase();

      collectionName = Character.toLowerCase(collectionName.charAt(0)) + collectionName.substring(1, collectionName.length());

      if (!(collectionName.endsWith("s") || collectionName.endsWith("S")))
         collectionName = English.plural(collectionName);

      if ("null".equals(collectionName + ""))
         System.out.println("asdfs");

      return collectionName;
   }

   protected String beautifyAttributeName(String inName)
   {
      if (inName.toUpperCase().equals(inName))
         inName = inName.toLowerCase();

      return inName;
   }

   public void configApi() throws Exception
   {
      for (Table t : getTables())
      {
         List<Column> cols = t.getColumns();

         Collection collection = new Collection();
         String collectionName = beautifyCollectionName(t.getName());

         //         collectionName = Character.toLowerCase(collectionName.charAt(0)) + collectionName.substring(1, collectionName.length());
         //
         //         if (!collectionName.endsWith("s"))
         //            collectionName = English.plural(collectionName);

         collection.setName(collectionName);

         Entity entity = new Entity();
         entity.withTable(t);
         entity.setHint(t.getName());

         entity.withCollection(collection);
         collection.setEntity(entity);

         for (Column col : cols)
         {
            if (col.getPk() == null)
            {
               Attribute attr = new Attribute();
               attr.setEntity(entity);
               attr.setName(beautifyAttributeName(col.getName()));
               attr.setColumn(col);
               attr.setHint(col.getTable().getName() + "." + col.getName());
               attr.setType(col.getType());

               entity.withAttribute(attr);

               if (col.isUnique() && entity.getKey() == null)
               {
                  entity.withKey(attr);
               }
            }
         }

         api.withCollection(collection);
         collection.setApi(api);

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
               pkEntity.addRelationship(r);
               r.setEntity(pkEntity);
               Entity related = api.getEntity(fkCol2.getPk().getTable());
               r.setRelated(related);

               String hint = "MANY_TO_MANY - ";
               hint += fkCol1.getPk().getTable().getName() + "." + fkCol1.getPk().getName();
               hint += " <- " + fkCol1.getTable().getName() + "." + fkCol1.getName() + ":" + fkCol2.getName();
               hint += " -> " + fkCol2.getPk().getTable().getName() + "." + fkCol2.getPk().getName();

               r.setHint(hint);
               r.setType(Relationship.REL_MANY_TO_MANY);
               r.setFkCol1(fkCol1);
               r.setFkCol2(fkCol2);

               //Collection related = api.getCollection(fkCol2.getTbl());
               r.setName(makeRelationshipName(r));
            }

            //MANY_TO_MANY the other way
            {
               Entity pkEntity = api.getEntity(fkCol2.getPk().getTable());
               Relationship r = new Relationship();
               pkEntity.addRelationship(r);
               r.setEntity(pkEntity);

               Entity related = api.getEntity(fkCol1.getPk().getTable());
               r.setRelated(related);

               //r.setRelated(api.getEntity(fkCol1.getTable()));

               String hint = "MANY_TO_MANY - ";
               hint += fkCol2.getPk().getTable().getName() + "." + fkCol2.getPk().getName();
               hint += " <- " + fkCol2.getTable().getName() + "." + fkCol2.getName() + ":" + fkCol1.getName();
               hint += " -> " + fkCol1.getPk().getTable().getName() + "." + fkCol1.getPk().getName();

               r.setHint(hint);
               r.setType(Relationship.REL_MANY_TO_MANY);
               r.setFkCol1(fkCol2);
               r.setFkCol2(fkCol1);

               r.setName(makeRelationshipName(r));
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
                     r.setHint("MANY_TO_ONE - " + pkTbl.getName() + "." + pkCol.getName() + " <- " + fkTbl.getName() + "." + fkCol.getName());
                     r.setType(Relationship.REL_MANY_TO_ONE);
                     r.setFkCol1(fkCol);
                     r.setEntity(pkEntity);
                     r.setRelated(fkEntity);
                     r.setName(makeRelationshipName(r));
                     pkEntity.addRelationship(r);
                  }

                  //MANY_TO_ONE
                  {
                     Relationship r = new Relationship();
                     r.setHint("ONE_TO_MANY - " + fkTbl.getName() + "." + fkCol.getName() + " -> " + pkTbl.getName() + "." + pkCol.getName());
                     r.setType(Relationship.REL_ONE_TO_MANY);
                     r.setFkCol1(fkCol);
                     r.setEntity(fkEntity);
                     r.setRelated(pkEntity);
                     r.setName(makeRelationshipName(r));
                     fkEntity.addRelationship(r);
                  }
               }
            }
         }
      }
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

}
