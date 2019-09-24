package io.rocketpartners.cloud.action.sql;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import io.rocketpartners.cloud.model.JsonMap;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.service.Engine;
import io.rocketpartners.cloud.utils.Rows;
import io.rocketpartners.cloud.utils.Rows.Row;
import io.rocketpartners.cloud.utils.SqlUtils;
import junit.framework.TestCase;

@RunWith(Parameterized.class)
public class TestSqlPostAction extends TestCase
{
   @Parameterized.Parameters
   public static Collection input()
   {
      return SqlEngineFactory.CONFIG_DBS_TO_TEST;
   }

   String db = null;

   protected String url(String path)
   {
      if (path.startsWith("http"))
         return path;

      String cp = collectionPath();

      if (cp.length() == 0)
         return path;

      if (!cp.endsWith("/"))
         cp += "/";

      while (path.startsWith("/"))
         path = path.substring(1, path.length());

      //      if (path.indexOf(cp) > -1 || path.startsWith("http"))
      //         return path;

      return "http://localhost/" + cp + path;
   }

   protected String collectionPath()
   {
      return "northwind/" + db + "/";
   }

   protected Engine service() throws Exception
   {
      return SqlEngineFactory.service();
   }

   public TestSqlPostAction(String db) throws Exception
   {
      this.db = db;
   }

   @Before
   public void before() throws Exception
   {
      SqlEngineFactory.prepData(db);
   }

   @Test
   public void testUpsert() throws Exception
   {
      Engine engine = service();
      Response res = null;

      SqlDb mysql = (SqlDb) engine.getApi("northwind").getDb(db);

      if (mysql.isType("mysql"))
      {
         Connection conn = mysql.getConnection();
         try
         {
            Rows rows = SqlUtils.selectRows(conn, "SELECT * FROM Orders WHERE OrderID in(10257, 10395, 10476, 10486)");

            for (Row row : rows)
            {
               row.put("shipaddress", "testing_upsert");
            }

            Map clone1 = new HashMap(rows.get(0));
            clone1.remove("OrderID");

            Map clone2 = new HashMap(rows.get(0));
            clone2.put("OrderID", 1);

            List<Map<String, Object>> toUpsert = new ArrayList(rows);
            toUpsert.add(clone1);
            toUpsert.add(clone2);
            List generatedKeys = SqlUtils.mysqlUpsert(conn, "Orders", toUpsert);

            //[10257, 10395, 10476, 10486, 222001, 1]

            assertEquals("11078", generatedKeys.get(4));//should be next auto increment key
            assertEquals("1", generatedKeys.get(5));
         }
         finally
         {
            conn.close();
         }

      }

   }

   @Test
   public void testAddOneRecord() throws Exception
   {
      Response res = null;
      Engine engine = service();

      //the bootstrap process copies 25 orders into the orders table, they are not sequential
      res = engine.get(url("orders?limit=100&sort=orderid"));
      System.out.println(res.getDebug());
      assertEquals(25, res.find("meta.foundRows")); //25 rows are copied by the bootstrap process, 11058 is last one

      //post one new bogus order
      res = engine.post(url("orders"), new JsonMap("orderid", 100, "shipaddress", "somewhere in atlanta", "shipcity", "atlanta").toString());
      res.dump();
      assertEquals(res.find("data.0.href"), url("orders/100"));

      //check the values we sent are the values we got back
      res = engine.get(res.findString("data.0.href"));
      assertEquals("somewhere in atlanta", res.find("data.0.shipaddress"));
      assertEquals("atlanta", res.find("data.0.shipcity"));

      //check total records
      res = engine.get(url("orders?limit=25&sort=orderid"));
      assertEquals(26, res.find("meta.foundRows"));
      assertEquals(res.find("data.0.href"), url("orders/100"));
   }

   public void testAddUpdateMultipleRecords() throws Exception
   {
      Response res = null;
      Engine engine = service();

      //some of the selected records are already in the target db (from the pre test config)
      //and some are not.  This will cause some records to insert and some to update.
      res = engine.get("http://localhost/northwind/source/orders?limit=25&sort=orderid&page=2&excludes=href");
      assertEquals(10273, res.find("data.0.orderid"));
      assertEquals(10297, res.find("data.24.orderid"));

      //now dump what we just selected into the new db (some will insert some will update)
      String putVal = res.data().toString();
      res = engine.post(url("orders"), putVal);
      assertTrue(res.findString("data.0.href").endsWith("10273"));
      assertTrue(res.findString("data.24.href").endsWith("10297"));

      String location = res.getHeader("Location");

      assertEquals(url("orders/10273,10274,10275,10276,10277,10278,10279,10280,10281,10282,10283,10284,10285,10286,10287,10288,10289,10290,10291,10292,10293,10294,10295,10296,10297"), //
            location);

      assertEquals(51, engine.get(url("orders?limit=1")).find("meta.foundRows"));

      //correcting for the differencees in the hrefs, we should be able to get the inserted
      //rows from the source and dest and they should match
      String srcLocation = location.replace("/" + db + "/", "/source/");
      String src = engine.get(srcLocation).data().toString();
      String copy = engine.get(location).data().toString();
      src = src.replace("/source/", "/" + db + "/");
      assertEquals(src, copy);

      //we did not insert any order details
      res = engine.get("http://localhost/northwind/h2/orderdetails");
      res.dump();
      assertEquals(0, res.findInt("meta.foundRows"));

      //now we are going to reselect the copied rows and expand the orderdetails relationship
      //and then post those
      srcLocation += "?expands=orderdetails&excludes=orderdetails.href,orderdetails.employees";

      res = engine.get(srcLocation);
      assertNull(res.findString("data.0.orderdetails.0.href"));
      assertNull(res.findString("data.0.orderdetails.0.employees"));
      assertTrue(res.findString("data.0.orderdetails.0.order").endsWith("/orders/10273"));
      assertTrue(res.findString("data.0.orderdetails.0.product").endsWith("/products/10"));

      putVal = res.data().toString().replace("/source/", "/h2/");

      res = engine.post(url("orders"), putVal);
      location = res.getHeader("Location");

      assertEquals(51, engine.get(url("orders?limit=1")).find("meta.foundRows"));

      res = engine.get(url("orderdetails"));
      res.dump();
      assertEquals(0, res.findInt("meta.foundRows"));

      res.dump();

      //    res = get("http://localhost/northwind/source/orders?limit=25&sort=orderid&page=4&excludes=href,orderid,shipname,orderdetails,customer,employee,shipvia");

   }

}
