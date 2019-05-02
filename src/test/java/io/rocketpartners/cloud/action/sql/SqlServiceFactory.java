package io.rocketpartners.cloud.action.sql;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.List;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClientBuilder;
import com.amazonaws.services.rds.model.CreateDBInstanceRequest;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBInstanceAlreadyExistsException;
import com.amazonaws.services.rds.model.DeleteDBInstanceRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;

import io.rocketpartners.cloud.action.rest.RestAction;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.Rows;
import io.rocketpartners.cloud.utils.SqlUtils;
import io.rocketpartners.cloud.utils.SqlUtils.SqlListener;
import io.rocketpartners.cloud.utils.Utils;

public class SqlServiceFactory
{
   public static final List DBS     = Arrays.asList(new Object[][]{{"h2"}, {"mysql"}});
   //public static final List DBS     = Arrays.asList(new Object[][]{{"mysql"}});

   protected static Service service = null;

   static
   {
      //delete old h2 dbs
      Utils.delete(new File("./.h2"));

      SqlUtils.addSqlListener(new SqlListener()
         {

            @Override
            public void onError(String method, String sql, Object args, Exception ex)
            {
               // TODO Auto-generated method stub

            }

            @Override
            public void beforeStmt(String method, String sql, Object args)
            {
               //System.out.println("SQL---> " + sql.replace("\r", "").replace("\n", " ").trim().replaceAll(" +", " "));
            }

            @Override
            public void afterStmt(String method, String sql, Object args, Exception ex, Object result)
            {
            }
         });
   }

   public static synchronized Service service() throws Exception
   {
      if (service != null)
         return service;

      try
      {

         SqlDb sourceDb = createDb("source", "northwind-h2.ddl", "org.h2.Driver", "jdbc:h2:./.h2/northwind-source" + "-" + Utils.time(), "sa", "", "source/");
         SqlDb h2Db = createDb("h2", "northwind-h2.ddl", "org.h2.Driver", "jdbc:h2:./.h2/northwind-h2" + "-" + Utils.time(), "sa", "", "h2/");

         SqlDb mysqlDb = null;
         {
            String mysqlHost = createMySqlRDS("mysql", "testnorthwind", "testcase", "password");
            String mysqlUrl = "jdbc:mysql://" + mysqlHost + ":3306";

            Class.forName("com.mysql.jdbc.Driver").newInstance();
            Connection mysqlConn = null;
            boolean sourceLoaded = false;
            try
            {
               mysqlConn = DriverManager.getConnection(mysqlUrl, "testcase", "password");
               SqlUtils.execute(mysqlConn, "USE northwindsource");
               sourceLoaded = true;
            }
            catch (Exception ex)
            {
               ex.printStackTrace();
            }
            if (!sourceLoaded)
            {
               SqlUtils.runDdl(mysqlConn, SqlServiceFactory.class.getResourceAsStream("northwind-mysql-source.ddl"));
            }

            try
            {
               SqlUtils.runDdl(mysqlConn, SqlServiceFactory.class.getResourceAsStream("northwind-mysql-copy.ddl"));
            }
            catch (Exception ex)
            {
               ex.printStackTrace();
               throw ex;
            }
            mysqlUrl += "/northwindcopy";
            mysqlDb = createDb("mysql", null, "com.mysql.jdbc.Driver", mysqlUrl, "testcase", "password", "mysql/");
         }

         service = new Service()
            {
               public Response service(String method, String url, String body)
               {
                  Response res = super.service(method, url, body);

                  if (Chain.size() == 1)
                  {
                     if (res.getChain().request().isGet())
                     {
                        if (res.find("meta.foundRows") == null)
                        {
                           System.out.println(res.getChain().request().getUrl());
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

         //
         service.withApi("northwind")//
                .withEndpoint("GET,PUT,POST,DELETE", "source/", "*").withAction(new RestAction()).getApi()//
                .withDb(sourceDb).getApi()//
                .withEndpoint("GET,PUT,POST,DELETE", "h2/", "*").withAction(new RestAction()).getApi()//
                .withDb(h2Db).getApi()//
                .withEndpoint("GET,PUT,POST,DELETE", "mysql/", "*").withAction(new RestAction()).getApi()//
                .withDb(mysqlDb).getApi()//
         ;

         service.startup();
      }
      catch (Exception ex)
      {
         ex.printStackTrace();
         throw ex;
      }
      return service;
   }

   public static void prepData(String db, String collectionUrl) throws Exception
   {

      //this first part deletes all orders from the DB and then posts back selected 
      //recoreds from the "source" db.

      Service service = service();
      SqlDb destDb = ((SqlDb) service.getApi("northwind").getDb(db));
      Connection destCon = destDb.getConnection();

      String orderDetailsTbl = destDb.getTable("OrderDetails").getName();
      String orderTbl = destDb.getTable("Orders").getName();

      SqlUtils.execute(destCon, "DELETE FROM " + destDb.quoteCol(orderDetailsTbl));
      SqlUtils.execute(destCon, "DELETE FROM " + destDb.quoteCol(orderTbl));

      int rows = SqlUtils.selectInt(destCon, "SELECT count(*) FROM " + destDb.quoteCol(destDb.getTable("Orders").getName()));
      Utils.assertEq(0, rows);

      SqlDb sourceDb = ((SqlDb) service.getApi("northwind").getDb("source"));
      Connection sourceConn = sourceDb.getConnection();

      Rows orders = SqlUtils.selectRows(sourceConn, "SELECT * FROM \"ORDERS\" WHERE \"SHIPNAME\" = ? OR \"CUSTOMERID\" = ?", "Blauer See Delikatessen", "HILAA");
      assertEquals(25, orders.size());

      SqlUtils.insertMaps(destCon, orderTbl, orders);
      assertEquals(25, SqlUtils.selectInt(destCon, "SELECT count(*) FROM " + destDb.quoteCol(orderTbl)));

      Rows orderDetails = SqlUtils.selectRows(sourceConn, "SELECT * FROM \"ORDERDETAILS\" WHERE \"ORDERID\" IN ( SELECT \"ORDERID\" FROM \"ORDERS\" WHERE \"SHIPNAME\" = ? OR \"CUSTOMERID\" = ?)", "Blauer See Delikatessen", "HILAA");
      assertEquals(59, orderDetails.size());

      SqlUtils.insertMaps(destCon, orderDetailsTbl, orderDetails);
      assertEquals(59, SqlUtils.selectInt(destCon, "SELECT count(*) FROM " + destDb.quoteCol(orderDetailsTbl)));

      //      Response res = service.service("GET", "northwind/source/orders?or(eq(shipname, 'Blauer See Delikatessen'),eq(customerid,HILAA))&pageSize=100&sort=-orderid&expands=orderdetails");
      //      res.dump();
      //
      //      Utils.assertEq(25, res.findArray("data").length());
      //      Utils.assertEq(100, res.find("meta.pageSize"));
      //      Utils.assertEq(25, res.find("meta.foundRows"));
      //      Utils.assertEq(11058, res.find("data.0.orderid"));
      //
      //      for (Object o : res.getJson().getArray("data"))
      //      {
      //         ObjectNode js = (ObjectNode) o;
      //         js.remove("href");
      //         res = service.post(collectionUrl, js);
      //
      //         //System.out.println(res.getDebug());
      //         Utils.assertEq(201, res.getStatusCode());//claims it was created
      //
      //         String href = res.findString("data.0.href");
      //         res = service.get(href);
      //
      //         Utils.assertEq(href, res.find("data.0.href"));//check that it actually was created
      //      }
      //
      //      res = service.get(collectionUrl);
      //      Utils.assertEq(25, res.find("meta.foundRows"));
      //
      //      conn = sqldb.getConnection();
      //      rows = SqlUtils.selectInt(conn, "SELECT count(*) FROM " + sqldb.quoteCol(sqldb.getTable("Orders").getName()));
      //      Utils.assertEq(25, rows);

   }

   public static SqlDb createDb(String name, String ddl, String driver, String url, String user, String pass, String collectionPath)
   {
      SqlDb db = new SqlDb();
      db.withName(name);
      db.withDriver(driver);
      db.withUrl(url);
      db.withUser(user);
      db.withPass(pass);
      db.withCollectionPath(collectionPath);

      System.out.println("INITIALIZING DB: " + name + " - " + ddl + " - " + url);

      if (ddl != null)
      {
         try
         {
            Connection conn = db.getConnection();
            SqlUtils.runDdl(conn, SqlServiceFactory.class.getResourceAsStream(ddl));
         }
         catch (Exception ex)
         {
            System.out.println("error initializing " + ddl);
            ex.printStackTrace();
            Utils.rethrow(ex);
         }
      }

      return db;
   }

   //Utility provisions micro MySQL RDS instance if it doesn't already exist, and returns the public URL
   public static String createMySqlRDS(String type, String instanceName, String username, String password)
   {
      AmazonRDS client = AmazonRDSClientBuilder.defaultClient();

      CreateDBInstanceRequest dbRequest = null;

      if ("mysql".equalsIgnoreCase(type))
      {
         dbRequest = new CreateDBInstanceRequest(instanceName, 20, "db.t2.micro", "mysql", username, password);
         dbRequest.setEngineVersion("5.6.40");
      }
      else
      {
         throw new RuntimeException("Unsupported RDS type: " + type);
      }

      dbRequest.setDBName(instanceName);

      String url = null;
      try
      {
         DBInstance db = client.createDBInstance(dbRequest);
         System.out.println("creating DB, need to wait for endpoint…");
         while (db.getDBInstanceStatus().equalsIgnoreCase("creating"))
         {
            System.out.println("waiting…");
            Thread.sleep(5000);
         }
         System.out.println("DB created, retrieving endpoint");
         url = db.getEndpoint().toString();
      }
      catch (DBInstanceAlreadyExistsException e)
      {
         DescribeDBInstancesResult dbs = client.describeDBInstances();
         for (DBInstance db : dbs.getDBInstances())
         {
            if (db.getDBInstanceIdentifier().equalsIgnoreCase(instanceName))
            {
               System.out.println("found existing DB, waiting for endpoint…");
               while (db.getDBInstanceStatus().equalsIgnoreCase("creating"))
               {
                  System.out.println("waiting…");
                  try
                  {
                     Thread.sleep(5000);
                  }
                  catch (InterruptedException e1)
                  {
                     e1.printStackTrace();
                  }
               }
               System.out.println("endpoint available");
               url = db.getEndpoint().getAddress();
               break;
            }
         }
      }
      catch (NullPointerException e)
      {
         System.out.println("Null Pointer Exception!");
         e.printStackTrace();
         url = null;
      }
      catch (Exception e)
      {
         System.out.println("Error creating RDS Instance: " + e.getMessage());
         e.printStackTrace();
         url = null;
      }
      return url;
   }

   public static boolean deleteMySqlRDS(String instanceName)
   {
      boolean result = false;
      try
      {
         AmazonRDS client = AmazonRDSClientBuilder.defaultClient();
         DeleteDBInstanceRequest deleteRequest = new DeleteDBInstanceRequest(instanceName).withSkipFinalSnapshot(true);
         client.deleteDBInstance(deleteRequest);
         result = true;
      }
      catch (Exception e)
      {
         System.out.println(e);
         e.printStackTrace();

      }
      return result;
   }
}
