package io.rocketpartners.cloud.action.rest;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import io.rocketpartners.cloud.model.ArrayNode;
import io.rocketpartners.cloud.model.ObjectNode;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.Utils;
import junit.framework.TestCase;

public abstract class TestRestGetActions extends TestCase
{
   protected abstract String collectionPath();

   protected abstract Service service() throws Exception;

   /**
    * Returns the last response handled by the service.
    * This is needed for subclasses to decorate test methods
    * @return
    */
   protected Response response() throws Exception
   {
      return service().response();
   }

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

   @Test
   public void testLimit01() throws Exception
   {
      Service service = service();
      Response res = null;
      ObjectNode json = null;

      res = service.get(url("orders?limit=5")).statusOk();
      json = res.getJson();
      assertEquals(5, json.find("meta.pageSize"));
      assertEquals(5, res.data().length());
   }

   @Test
   public void testSort01() throws Exception
   {
      Service service = service();
      Response res = null;

      String url = url("orders?limit=2&sort=orderid");

      res = service.get(url);
      res.dump();

      assertEquals(2, res.data().length());
      String href = res.findString("data.0.href");

      assertTrue(href.endsWith("/orders/10248"));

      res = service.get(url("orders?limit=2&sort=-orderid"));
      res.dump();

      assertEquals(2, res.data().length());
      href = res.findString("data.0.href");
      assertTrue(href.endsWith("/orders/11077"));

   }

   @Test
   public void testPagination01() throws Exception
   {
      Service service = service();
      Response res = null;

      int total = 0;
      int pages = 0;
      String start = url("orders?limit=5&sort=orderId");
      String next = start;
      do
      {
         res = service.get(next);
         res.dump();

         if (res.data().size() == 0)
            break;

         total += res.data().length();
         pages += 1;

         next = res.next();

         assertEquals(5, res.data().length());
         assertEquals(5, res.find("meta.pageSize"));
      }
      while (pages < 200 && next != null);

      assertEquals(166, pages);
      assertEquals(830, total);
   }

   @Test
   public void testEw01() throws Exception
   {
      Service service = service();
      Response res = null;
      res = service.get(url("orders?ew(shipname,Chevalier)"));
      ArrayNode data = res.data();
      assertTrue(data.size() > 0);
      for (Object o : data)
      {
         assertTrue(((ObjectNode) o).getString("shipname").endsWith("Chevalier"));
      }
   }

   @Test
   public void testLt01() throws Exception
   {
      Service service = service();
      Response res = null;
      res = service.get(url("orders?limit=5&lt(freight,2)"));
      ArrayNode data = res.data();
      assertTrue(data.size() > 0);
      for (Object o : data)
      {
         assertTrue(Float.parseFloat(((ObjectNode) o).getString("freight")) < 2);
      }
   }

   public void testEmp01() throws Exception
   {
      Service service = service();
      Response res = null;

      res = service.get(url("orders?limit=5&emp(shipregion)"));
      List<ObjectNode> list = res.data().asList();
      for (ObjectNode result : list)
      {
         String shipregion = result.getString("shipregion");
         assertTrue("shipregion was supposed to be empty but was: '" + shipregion + "'", Utils.empty(shipregion));
      }
   }

   public void testN01() throws Exception
   {
      Service service = service();
      Response res = null;
      ObjectNode json = null;
      ArrayNode data = null;

      res = service.get(url("orders?limit=5&n(shipRegion)")).statusOk();
      json = res.getJson();
      data = json.getArray("data");
      assertTrue(data.length() > 0);
      for (Object o : data)
      {
         assertNull(((ObjectNode) o).getString("shipregion"));
      }
   }

   @Test
   public void testLe01() throws Exception
   {
      Service service = service();
      Response res = null;
      res = service.get(url("orders?limit=5&le(freight,2)"));
      ArrayNode data = res.data();
      assertTrue(data.size() > 0);
      for (Object o : data)
      {
         assertTrue(Float.parseFloat(((ObjectNode) o).getString("freight")) <= 2);
      }
   }

   @Test
   public void testIn01() throws Exception
   {
      Service service = service();
      Response res = null;
      res = service.get(url("orders?in(orderid,10249,10258,10252)"));
      ArrayNode data = res.data();
      List<String> list = Arrays.asList("10249", "10258", "10252");
      assertEquals(3, data.length());
      for (Object obj : data)
      {
         assertTrue(list.contains(((ObjectNode) obj).getString("orderId")));
      }
   }

   public void testNemp01() throws Exception
   {
      Service service = service();
      Response res = null;
      res = service.get(url("orders?limit=5&nemp(shipregion)"));
      List<ObjectNode> list = res.data().asList();
      for (ObjectNode result : list)
      {
         String shipregion = result.getString("shipregion");
         assertFalse("shipregion was not supposed to be empty but was: '" + shipregion + "'", Utils.empty(shipregion));
      }
   }

   public void testN02() throws Exception
   {
      Service service = service();
      Response res = null;
      ObjectNode json = null;

      res = service.get(url("orders?limit=5&n(shipCountry)")).statusOk();
      json = res.getJson();
      assertTrue(json.getArray("data").length() == 0);
   }

   @Test
   public void testNn01() throws Exception
   {
      Service service = service();
      Response res = null;
      ObjectNode json = null;
      ArrayNode data = null;

      res = service.get(url("orders?limit=5&nn(shipRegion)")).statusOk();
      json = res.getJson();
      data = json.getArray("data");
      assertTrue(data.length() > 0);
      for (Object o : data)
      {
         assertNotNull(((ObjectNode) o).getString("shipregion"));
      }
   }

   @Test
   public void testOut01() throws Exception
   {
      Service service = service();
      Response res = null;
      res = service.get(url("employees?out(employeeid,1,2)")).statusOk();
      List<String> employeeIDs = Arrays.asList("1", "2");
      assertEquals(7, res.data().length());
      for (Object obj : res.data())
      {
         assertFalse(employeeIDs.contains(((ObjectNode) obj).getString("orderId")));
      }
   }

   public void testSw01() throws Exception
   {
      Service service = service();
      Response res = null;
      ObjectNode json = null;
      ArrayNode data = null;

      res = service.get(url("orders?limit=5&sw(customerId,VI)")).statusOk();
      json = res.getJson();
      data = json.getArray("data");
      assertTrue(data.length() > 0);
      for (Object o : data)
      {
         assertTrue(((ObjectNode) o).getString("customerid").startsWith("VI"));
      }
   }

   @Test
   public void testW01() throws Exception
   {
      Service service = service();
      Response res = null;
      res = service.get(url("employees?w(city,ondon)")).statusOk();
      assertEquals(4, res.data().length());
      for (Object obj : res.data())
      {
         assertTrue(((ObjectNode) obj).getString("city").contains("ondon"));
      }
   }

   public void testSw02() throws Exception
   {
      Service service = service();
      Response res = null;
      ObjectNode json = null;

      res = service.get(url("orders?limit=5&sw(customerId,Z)")).statusOk();
      json = res.getJson();
      assertTrue(json.getArray("data").length() == 0);
   }

   @Test
   public void testLike01() throws Exception
   {
      Service service = service();
      Response res = null;
      ObjectNode json = null;
      ArrayNode data = null;

      res = service.get(url("orders?limit=5&like(customerId,*VI*)")).statusOk();
      json = res.getJson();
      data = json.getArray("data");
      assertTrue(data.length() > 0);
      for (Object o : data)
      {
         assertTrue(((ObjectNode) o).getString("customerid").contains("VI"));
      }
   }

   @Test
   public void testLike02() throws Exception
   {
      Service service = service();
      Response res = null;
      ObjectNode json = null;

      res = service.get(url("orders?limit=5&like(customerId,*ZZ*)")).statusOk();
      json = res.getJson();
      assertTrue(json.getArray("data").length() == 0);
   }
   
   @Test
   public void testEq01() throws Exception
   {
      Service service = service();
      Response res = null;
      ObjectNode json = null;
      res = service.get(url("orders?eq(orderid,10248)"));
      json = res.getJson();
      assertEquals(1, json.find("meta.foundRows"));
      assertTrue(res.findString("data.0.orderid").equals("10248"));
   }
 
   @Test
   public void testWo01() throws Exception
   {
      Service service = service();
      Response res = null;
      ObjectNode json = null;
      ArrayNode data = null;

      res = service.get(url("orders?eq(employeeid,5)"));
      json = res.getJson();
      assertEquals(42, json.find("meta.foundRows"));

      res = service.get(url("orders?eq(employeeid,5)&wo(shipcountry,witzer)"));
      json = res.getJson();
      data = json.getArray("data");
      assertEquals(41, json.find("meta.foundRows"));
      for (Object o : data)
      {
         assertFalse(((ObjectNode) o).getString("shipcountry").contains("witzer"));
      }
   }
}
