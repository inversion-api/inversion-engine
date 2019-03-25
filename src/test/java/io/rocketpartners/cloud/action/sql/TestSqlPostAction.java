package io.rocketpartners.cloud.action.sql;

import io.rocketpartners.cloud.action.rest.TestRestGetActions;
import io.rocketpartners.cloud.model.ObjectNode;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.service.Service;

public class TestSqlPostAction extends TestRestGetActions
{

   protected String collectionPath()
   {
      return "northwind/sql/";
   }

   @Override
   protected Service service() throws Exception
   {
      return SqlServiceFactory.service();
   }

   public void testPost1() throws Exception
   {
      Response res = null;
      Service service = service();

      //the bootstrap process copies 25 orders into the orders table, they are not sequential
      res = service.get("http://localhost/northwind/sql/orders?limit=100&sort=orderid");
      System.out.println(res.getDebug());
      assertEquals(25, res.find("meta.foundRows")); //25 rows are copied by the bootstrap process, 11058 is last one

      //post one new bogus order
      res = service.post("northwind/sql/orders", new ObjectNode("orderid", 222000, "shipaddress", "somewhere in atlanta", "shipcity", "atlanta").toString());
      assertEquals(res.find("data.0.href"), "http://localhost/northwind/sql/orders/222000");

      //check the values we sent are the values we got back
      res = service.get(res.findString("data.0.href"));
      assertEquals("somewhere in atlanta", res.find("data.0.shipaddress"));
      assertEquals("atlanta", res.find("data.0.shipcity"));

      //check total records and that pagination is working as expected
      res = service.get("http://localhost/northwind/sql/orders?limit=25&sort=orderid&page=2");
      assertEquals(26, res.find("meta.foundRows"));
      assertEquals(res.find("data.0.href"), "http://localhost/northwind/sql/orders/222000");

      //some of these may not be in the "sql" db
      res = service.get("http://localhost/northwind/source/orders?limit=25&sort=orderid&page=2&excludes=href");
      assertEquals(10273, res.find("data.0.orderid"));
      assertEquals(10297, res.find("data.24.orderid"));

      //now dump what we just selected into the new db
      String putVal = res.data().toString();
      res = service.post("http://localhost/northwind/sql/orders", putVal);
      assertTrue(res.findString("data.0.href").endsWith("10273"));
      assertTrue(res.findString("data.24.href").endsWith("10297"));
      
      String location = res.getHeader("Location");

      assertEquals("http://localhost/northwind/sql/orders/10273,10274,10275,10276,10277,10278,10279,10280,10281,10282,10283,10284,10285,10286,10287,10288,10289,10290,10291,10292,10293,10294,10295,10296,10297", //
            location);

      assertEquals(51, service.get("http://localhost/northwind/sql/orders?limit=1").find("meta.foundRows"));
      
      
      //correcting for the differencees in the hrefs, we should be able to get the inserted
      //rows from the source and dest and they should match
      
      String srcLocation = location.replace("/sql/", "/source/");
      String src = service.get(srcLocation).data().toString();
      String copy = service.get(location).data().toString();
      src = src.replace("/source/",  "/sql/"); 
      assertEquals(src, copy);
      
      
      //we did not insert any order details
      res = service.get("http://localhost/northwind/sql/orderdetails");
      System.out.println(res.getDebug());
      assertEquals(0, res.findInt("meta.foundRows"));
      
      //now we are going to reselect the copied rows and expand the orderdetails relationship
      //and then post those
      srcLocation += "?expands=orderdetails&excludes=orderdetails.href,orderdetails.employees";
      
      res = service.get(srcLocation);
      assertNull(res.findString("data.0.orderdetails.0.href"));
      assertNull(res.findString("data.0.orderdetails.0.employees"));
      assertTrue(res.findString("data.0.orderdetails.0.order").endsWith("/orders/10273"));
      assertTrue(res.findString("data.0.orderdetails.0.product").endsWith("/products/10"));
      
      putVal = res.data().toString().replace("/source/", "/sql/");

      res = service.post("http://localhost/northwind/sql/orders", putVal);
      location = res.getHeader("Location");
      
      assertEquals(51, service.get("http://localhost/northwind/sql/orders?limit=1").find("meta.foundRows"));
      
      res = service.get("http://localhost/northwind/sql/orderdetails");
      res.dump();
      assertEquals(0, res.findInt("meta.foundRows"));
      
      res.dump();
      
      //    res = service.get("http://localhost/northwind/source/orders?limit=25&sort=orderid&page=4&excludes=href,orderid,shipname,orderdetails,customer,employee,shipvia");

   }

}
