package io.rocketpartners.cloud.action.sql;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;

import io.rocketpartners.cloud.action.rest.RestAction;
import io.rocketpartners.cloud.model.ObjectNode;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.Rows;
import io.rocketpartners.cloud.utils.Rows.Row;
import io.rocketpartners.cloud.utils.SqlUtils;
import io.rocketpartners.cloud.utils.SqlUtils.SqlListener;
import io.rocketpartners.cloud.utils.Utils;

public class SqlServiceFactory
{
   protected static Service service = null;

   static
   {
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

   public static void main(String[] args) throws Exception
   {
      Class.forName("org.h2.Driver");

      Utils.delete(new File("./.h2"));

      Connection full1 = DriverManager.getConnection("jdbc:h2:./.h2/northwind-source" + "-" + Utils.time());
      SqlUtils.runDdl(full1, SqlServiceFactory.class.getResourceAsStream("northwind-empty.ddl"));

      Connection full2 = DriverManager.getConnection("jdbc:h2:./.h2/northwind-empty" + "-" + Utils.time());
      SqlUtils.runDdl(full2, SqlServiceFactory.class.getResourceAsStream("northwind-source.ddl"));

      System.out.println("OK");

   }

   public static synchronized Service service() throws Exception
   {
      if (service != null)
         return service;

      try
      {
         SqlDb source = createDb("northwind-source", "org.h2.Driver", "jdbc:h2:./.h2/northwind-source" + "-" + Utils.time(), "sa", "", "source/");

         Connection conn = source.getConnection();
         Rows rows = SqlUtils.selectRows(conn, "SELECT * FROM \"ORDERS\" WHERE (\"SHIPNAME\" = 'Blauer See Delikatessen' OR \"CUSTOMERID\" = 'HILAA') ORDER BY \"ORDERID\" DESC  LIMIT 100");
         Utils.assertEq(25, rows.size());
         
//         rows = SqlUtils.selectRows(conn,  "SELECT o.EmployeeID, od.OrderId, od.ProductId FROM \"Order\" o JOIN \"OrderDetails\" od ON o.OrderId = od.OrderId"); 
//         for(Row row : rows)
//         {
//            SqlUtils.insertMap(conn,  "EmployeeOrderDetails", row);
//         }

         SqlDb partial = createDb("northwind-empty", "org.h2.Driver", "jdbc:h2:./.h2/northwind-empty" + "-" + Utils.time(), "sa", "", "sql/");

         conn = partial.getConnection();
         rows = SqlUtils.selectRows(conn, "SELECT * FROM \"ORDERS\"");
         Utils.assertEq(0, rows.size());

         service = new Service()
            {
               public Response service(String method, String url, String body)
               {
                  Response res = super.service(method, url, body);

                  if (!res.isSuccess())
                  {
                     System.out.println(res.getDebug());
                     
                     if (res.getError() != null)
                        Utils.rethrow(res.getError());

                     throw new RuntimeException(res.getStatusMesg());
                  }

                  return res;
               }
            };

         //
         service.withApi("northwind")//
                .withEndpoint("GET,PUT,POST,DELETE", "source/", "*").withAction(new RestAction()).getApi()//
                .withDb(source).getApi()//
                .withEndpoint("GET,PUT,POST,DELETE", "sql/", "*").withAction(new RestAction()).getApi()//
                .withDb(partial).getApi()//
                .getService();

         Response res = null;

         //      res = service.get("northwind/sql/orders");
         //      Utils.assertEq(200, res.getStatusCode());
         //      Utils.assertEq(0, res.findArray("data").length());

         res = service.service("GET", "northwind/source/orders?or(eq(shipname, 'Blauer See Delikatessen'),eq(customerid,HILAA))&pageSize=100&sort=-orderid");
         //System.out.println(res.getJson());
         Utils.assertEq(25, res.findArray("data").length());
         Utils.assertEq(100, res.find("meta.pageSize"));
         Utils.assertEq(25, res.find("meta.rowCount"));
         Utils.assertEq(11058, res.find("data.0.orderid"));

         int inserted = 0;
         for (Object o : res.getJson().getArray("data"))
         {
            ObjectNode js = (ObjectNode) o;
            js.remove("href");
            res = service.post("northwind/sql/orders", js);
            inserted += 1;

            //System.out.println(res.getDebug());
            Utils.assertEq(201, res.getStatusCode());//claims it was created

            String href = res.findString("data.0.href");
            res = service.get(href);

            Utils.assertEq(href, res.find("data.0.href"));//check that it actually was created
         }

         res = service.get("northwind/sql/orders");
         Utils.assertEq(25, res.find("meta.rowCount"));

      }
      catch (Exception ex)
      {
         ex.printStackTrace();
         throw ex;
      }
      return service;
   }

   public static SqlDb createDb(String ddl, String driver, String url, String user, String pass, String collectionPath)
   {
      SqlDb db = new SqlDb();
      db.withName(ddl);
      db.withDriver(driver);
      db.withUrl(url);
      db.withUser(user);
      db.withPass(pass);
      db.withCollectionPath(collectionPath);

      System.out.println("INITIALIZING DB: " + ddl + " - " + url);

      try
      {
         File dir = new File("./.h2");
         dir.mkdir();

         File[] dbfiles = dir.listFiles();
         for (int i = 0; dbfiles != null && i < dbfiles.length; i++)
         {
            if (dbfiles[i].getName().startsWith(ddl))
               dbfiles[i].delete();
         }

         Connection conn = db.getConnection();
         SqlUtils.runDdl(conn, SqlServiceFactory.class.getResourceAsStream(ddl + ".ddl"));
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
