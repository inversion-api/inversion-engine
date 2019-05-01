package io.rocketpartners.cloud.action.sql;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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

         Connection conn = sourceDb.getConnection();
         Rows rows = SqlUtils.selectRows(conn, "SELECT * FROM \"ORDERS\" WHERE (\"SHIPNAME\" = 'Blauer See Delikatessen' OR \"CUSTOMERID\" = 'HILAA') ORDER BY \"ORDERID\" DESC  LIMIT 100");
         Utils.assertEq(25, rows.size());

         //         rows = SqlUtils.selectRows(conn,  "SELECT o.EmployeeID, od.OrderId, od.ProductId FROM \"Order\" o JOIN \"OrderDetails\" od ON o.OrderId = od.OrderId"); 
         //         for(Row row : rows)
         //         {
         //            SqlUtils.insertMap(conn,  "EmployeeOrderDetails", row);
         //         }

         SqlDb h2Db = createDb("h2", "northwind-h2.ddl", "org.h2.Driver", "jdbc:h2:./.h2/northwind-h2" + "-" + Utils.time(), "sa", "", "h2/");

         conn = h2Db.getConnection();

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
      assertEquals(25, SqlUtils.selectInt(destCon, "SELECT count(*) FROM " + orderTbl));
      

      Rows orderDetails = SqlUtils.selectRows(sourceConn, "SELECT * FROM \"ORDERDETAILS\" WHERE \"ORDERID\" IN ( SELECT \"ORDERID\" FROM \"ORDERS\" WHERE \"SHIPNAME\" = ? OR \"CUSTOMERID\" = ?)", "Blauer See Delikatessen", "HILAA");
      assertEquals(59, orderDetails.size());
      

      SqlUtils.insertMaps(destCon, orderDetailsTbl, orderDetails);
      assertEquals(59, SqlUtils.selectInt(destCon, "SELECT count(*) FROM " + orderDetailsTbl));

      
      

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

      try
      {
         //         File dir = new File("./.h2");
         //         dir.mkdir();
         //
         //         File[] dbfiles = dir.listFiles();
         //         for (int i = 0; dbfiles != null && i < dbfiles.length; i++)
         //         {
         //            if (dbfiles[i].getName().startsWith(ddl))
         //               dbfiles[i].delete();
         //         }

         Connection conn = db.getConnection();
         SqlUtils.runDdl(conn, SqlServiceFactory.class.getResourceAsStream(ddl));
      }
      catch (Exception ex)
      {
         System.out.println("error initializing " + ddl);
         ex.printStackTrace();
         Utils.rethrow(ex);
      }

      return db;
   }

   //   public static void main(String[] args) throws Exception
   //   {
   //      new File("./northwind.db").delete();
   //      new File("./northwind.mv.db").delete();
   //      new File("./northwind.trace.db").delete();
   //      new File("./northwind.lock.db").delete();
   //
   //      Class.forName("org.h2.Driver").newInstance();
   //      Connection conn = DriverManager.getConnection("jdbc:h2:./northwind", "sa", "");
   //
   //      SqlUtils.runDdl(conn, CreateNorthwindsH2Db.class.getResourceAsStream("Northwind.H2.sql"));
   //
   //      DatabaseMetaData dbmd = conn.getMetaData();
   //      ResultSet rs = dbmd.getTables(null, null, "%", new String[]{"TABLE", "VIEW"});
   //      while (rs.next())
   //      {
   //         String tableCat = rs.getString("TABLE_CAT");
   //         String tableSchem = rs.getString("TABLE_SCHEM");
   //         String tableName = rs.getString("TABLE_NAME");
   //
   //         System.out.println(tableName);
   //      }
   //
   //      Rows rows = SqlUtils.selectRows(conn, "SELECT * FROM PRODUCTS");
   //      for (Row row : rows)
   //      {
   //         System.out.println(row);
   //      }
   //
   //      conn.close();
   //
   //   }
}
