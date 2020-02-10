/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
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
package io.inversion.cloud.jdbc;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import io.inversion.cloud.action.rest.RestAction;
import io.inversion.cloud.jdbc.db.JdbcDb;
import io.inversion.cloud.jdbc.utils.JdbcUtils;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.Collection;
import io.inversion.cloud.model.Endpoint;
import io.inversion.cloud.model.EngineListener;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.service.Engine;
import io.inversion.cloud.service.spring.InversionApp;
import io.inversion.cloud.utils.Rows;
import io.inversion.cloud.utils.Utils;

public class JdbcDbApiFactory
{
   //public static final List<Object[]> CONFIG_DBS_TO_TEST   = Arrays.asList(new Object[][]{{"h2"}, {"mysql"}});
   //public static final List<Object[]> CONFIG_DBS_TO_TEST   = Arrays.asList(new Object[][]{{"mysql"}});
   public static final List<Object[]> CONFIG_DBS_TO_TEST   = Arrays.asList(new Object[][]{{"h2"}});

   public static final boolean        CONFIG_REBUILD_MYSQL = false;

   protected static Engine            engine               = null;

   protected static boolean shouldLoad(String db)
   {
      for (Object[] args : CONFIG_DBS_TO_TEST)
      {
         if (args[0].equals(db))
            return true;
      }
      return false;
   }

   public static void main(String[] args) throws Exception
   {
      Engine e = JdbcDbApiFactory.engine();
      e.withEngineListener(new EngineListener()
         {
            @Override
            public void beforeFinally(Engine engine, Api api, Endpoint endpoint, Chain chain, Request req, Response res)
            {
               if (Chain.getDepth() <= 1)
               {
                  res.dump();
               }
            }
         });

      //      e.startup();
      //      JdbcDb db = (JdbcDb) e.getApi("northwind").getDb("source");
      //      Connection conn = db.getConnection();
      //      System.out.println(JdbcUtils.selectRows(conn, "SELECT COUNT(\"orders\".\"shipCountry\") FROM \"orders\" LIMIT 100 OFFSET"));

      InversionApp.run(e);
   }

   public static void resetAll() throws Exception
   {
      engine = null;
   }

   public static synchronized Engine engine() throws Exception
   {
      return JdbcDbApiFactory.service(false, true);
   }

   public static synchronized Engine service(boolean startup, boolean newCopy) throws Exception
   {
      if (!newCopy && engine != null)
         return engine;

      try
      {
         engine = new Engine()
            {
               @Override
               public void startup0()
               {
                  try
                  {
                     JdbcDb sourceDb = createDb("source", JdbcDb.class.getResource("northwind-h2.ddl").toString(), "org.h2.Driver", "jdbc:h2:mem:" + UUID.randomUUID().toString() + ";DB_CLOSE_DELAY=-1", "sa", "", "source/");

                     engine.withApi("northwind")//
                           .withEndpoint("GET,PUT,POST,DELETE", "source/*", new RestAction())//
                           .withDb(sourceDb)//
                           .withEngineListener(new EngineListener()
                              {
                                 public void onStartup(Engine engine, Api api)
                                 {
                                    for (Collection col : api.getCollections())
                                    {
                                       col.withAlias("aliased_" + col.getCollectionName());
                                    }
                                 }
                              });

                     //Connection conn = sourceDb.getConnection();
                     //System.out.println(SqlUtils.selectInt(conn,  "SELECT count(*) from Orders"));

                     if (shouldLoad("h2"))
                     {
                        JdbcDb h2Db = createDb("h2", JdbcDb.class.getResource("northwind-h2.ddl").toString(), "org.h2.Driver", "jdbc:h2:mem:" + UUID.randomUUID().toString() + ";DB_CLOSE_DELAY=-1", "sa", "", "h2/");

                        engine.getApi("northwind")//
                              .withEndpoint("GET,PUT,POST,DELETE", "h2/*", new RestAction())//
                              .withDb(h2Db);

                        //Connection conn2 = h2Db.getConnection();
                        //System.out.println(SqlUtils.selectInt(conn2,  "SELECT count(*) from Orders OFFSET 0 LIMIT 100"));
                        //System.out.println(SqlUtils.selectInt(conn2,  "SELECT count(*) from Orders LIMIT 0 OFFSET 100"));
                        //System.out.println(SqlUtils.selectRows(conn2,  "SELECT \"EMPLOYEES\".* FROM \"EMPLOYEES\" WHERE \"EMPLOYEES\".\"EMPLOYEEID\" = ? ORDER BY \"EMPLOYEES\".\"EMPLOYEEID\" ASC  OFFSET 0 LIMIT 100", 5));
                        //conn2.close();

                     }

                     if (shouldLoad("mysql"))
                     {
                        JdbcDb mysqlDb = null;
                        {
                           String mysqlUrl = "jdbc:mysql://testnorthwind.cb4bo9agap0y.us-east-1.rds.amazonaws.com:3306";

                           Class.forName("com.mysql.jdbc.Driver").newInstance();
                           Connection mysqlConn = null;
                           boolean sourceLoaded = false;
                           try
                           {
                              mysqlConn = DriverManager.getConnection(mysqlUrl, "testcase", "password");
                              JdbcUtils.execute(mysqlConn, "USE northwindsource");
                              sourceLoaded = true;
                           }
                           catch (Exception ex)
                           {
                              ex.printStackTrace();
                           }
                           if (!sourceLoaded || CONFIG_REBUILD_MYSQL)
                           {
                              JdbcUtils.runDdl(mysqlConn, JdbcDbApiFactory.class.getResourceAsStream("northwind-mysql-source.ddl"));
                           }

                           try
                           {
                              JdbcUtils.runDdl(mysqlConn, JdbcDbApiFactory.class.getResourceAsStream("northwind-mysql-copy.ddl"));
                           }
                           catch (Exception ex)
                           {
                              ex.printStackTrace();
                              throw ex;
                           }
                           mysqlUrl += "/northwindcopy";
                           mysqlDb = createDb("mysql", null, "com.mysql.jdbc.Driver", mysqlUrl, "testcase", "password", "mysql/");

                           engine.getApi("northwind")//
                                 .withEndpoint("GET,PUT,POST,DELETE", "mysql/*", new RestAction())//
                                 .withDb(mysqlDb);
                        }
                     }
                  }
                  catch (Exception ex)
                  {
                     ex.printStackTrace();
                     Utils.rethrow(ex);
                  }
               }

               public Chain service(Request req, Response res)
               {
                  Chain chain = super.service(req, res);

                  String debug = res.getDebug();
                  debug = debug.substring(0, debug.indexOf("<< response"));

                  System.out.print(debug);
                  return chain;
               }

               public Response service(String method, String url, String body)
               {
                  Response res = super.service(method, url, body);

                  if (Chain.size() == 1)
                  {
                     if (res.getChain().getRequest().isGet())
                     {
                        if (res.find("meta.foundRows") == null)
                        {
                           System.out.println(res.getChain().getRequest().getUrl());
                           System.out.println(res.meta());
                        }
                     }

                     if (!res.isSuccess())
                     {
                        System.out.println(res.getDebug());

                        if (res.getStatusCode() == 500)
                           System.exit(1);

                        if (res.getError() != null)
                           Utils.rethrow(res.getError());

                        throw new RuntimeException(res.getStatusMesg());
                     }
                  }

                  return res;
               }
            };

         engine.withApi("northwind");

         if (startup)
         {
            engine.startup();
         }
      }
      catch (Exception ex)
      {
         ex.printStackTrace();
         throw ex;
      }
      return engine;
   }

   /**
    * This restores select tables to a default state so test case writers
    * can modify data and then have it restored before the next test case runs.
    * 
    * Tables restored include Orders, OrderDetails, IndexLog  
    *   
    * @param db
    * @throws Exception
    */
   public static void prepData(String db) throws Exception
   {
      Engine engine = service(true, false);
      JdbcDb destDb = ((JdbcDb) engine.getApi("northwind").getDb(db));
      Connection destCon = destDb.getConnection();

      //-- Clear and restore the Order and OrderDetails tables.  OrderDetails must be
      //-- cleared first and restored second because of foreign key dependencies

      String orderDetailsTbl = destDb.getCollection("OrderDetails").getTableName();
      String orderTbl = destDb.getCollection("Orders").getTableName();

      JdbcUtils.execute(destCon, "DELETE FROM " + destDb.quoteCol(orderDetailsTbl));
      JdbcUtils.execute(destCon, "DELETE FROM " + destDb.quoteCol(orderTbl));

      int rows = JdbcUtils.selectInt(destCon, "SELECT count(*) FROM " + destDb.quoteCol(destDb.getCollection("Orders").getTableName()));
      Utils.assertEq(0, rows);

      JdbcDb sourceDb = ((JdbcDb) engine.getApi("northwind").getDb("source"));
      Connection sourceConn = sourceDb.getConnection();

      Rows orders = JdbcUtils.selectRows(sourceConn, "SELECT * FROM \"ORDERS\" WHERE \"SHIPNAME\" = ? OR \"CUSTOMERID\" = ?", "Blauer See Delikatessen", "HILAA");
      assertEquals(25, orders.size());

      JdbcUtils.insertMaps(destCon, orderTbl, orders);
      assertEquals(25, JdbcUtils.selectInt(destCon, "SELECT count(*) FROM " + destDb.quoteCol(orderTbl)));

      Rows orderDetails = JdbcUtils.selectRows(sourceConn, "SELECT * FROM \"ORDERDETAILS\" WHERE \"ORDERID\" IN ( SELECT \"ORDERID\" FROM \"ORDERS\" WHERE \"SHIPNAME\" = ? OR \"CUSTOMERID\" = ?)", "Blauer See Delikatessen", "HILAA");
      assertEquals(59, orderDetails.size());

      JdbcUtils.insertMaps(destCon, orderDetailsTbl, orderDetails);
      assertEquals(59, JdbcUtils.selectInt(destCon, "SELECT count(*) FROM " + destDb.quoteCol(orderDetailsTbl)));

      //-- Restore the IndexLog table from source
      String indexLogTbl = destDb.getCollection("IndexLog").getTableName();
      JdbcUtils.execute(destCon, "DELETE FROM " + destDb.quoteCol(indexLogTbl));

      Rows indexLogs = JdbcUtils.selectRows(sourceConn, "SELECT * FROM \"INDEXLOG\"");
      JdbcUtils.insertMaps(destCon, indexLogTbl, indexLogs);

   }

   public static JdbcDb createDb(String name, String ddlUrl, String driver, String url, String user, String pass, String collectionPath)
   {
      JdbcDb db = new JdbcDb();
      db.withName(name);
      db.withDriver(driver);
      db.withUrl(url);
      db.withUser(user);
      db.withPass(pass);
      db.withCollectionPath(collectionPath);

      if (ddlUrl.indexOf(":") < 0)
         ddlUrl = new File(ddlUrl).toURI().toString();
      db.withDdlUrl(ddlUrl);

      //      if (ddl != null)
      //      {
      //         try
      //         {
      //            Connection conn = db.getConnection();
      //            JdbcUtils.runDdl(conn, JdbcDbApiFactory.class.getResourceAsStream(ddl));
      //            conn.commit();
      //
      //            //System.out.println("INITIALIZING DB: " + name + " - " + ddl + " - " + url + " - " + SqlUtils.selectRows(conn, "SHOW TABLES") + " - " + SqlUtils.selectRows(conn, "SELECT CUSTOMERID FROM CUSTOMERS LIMIT 1"));
      //         }
      //         catch (Exception ex)
      //         {
      //            System.out.println("error initializing " + ddl);
      //            ex.printStackTrace();
      //            Utils.rethrow(ex);
      //         }
      //      }

      return db;
   }

}
