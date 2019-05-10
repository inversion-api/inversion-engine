package io.rocketpartners.cloud.action.rest;

import java.util.List;

import org.junit.Test;

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
      assertEquals(5, json.getArray("data").length());
   }

   @Test
   public void testSort01() throws Exception
   {
      Service service = service();
      Response res = null;
      ObjectNode json = null;

      String url = url("orders?limit=2&sort=orderid");

      res = service.get(url);
      res.dump();

      assertEquals(2, res.findArray("data").length());
      String href = res.findString("data.0.href");

      assertTrue(href.endsWith("/orders/10248"));

      res = service.get(url("orders?limit=2&sort=-orderid"));
      res.dump();

      assertEquals(2, res.findArray("data").length());
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

         assertEquals(5, res.findArray("data").length());
         assertEquals(5, res.find("meta.pageSize"));
      }
      while (pages < 200 && next != null);

      assertEquals(166, pages);
      assertEquals(830, total);
   }

   @Test
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

   @Test
   public void testNemp01() throws Exception
   {
      Service service = service();
      Response res = null;
      res = service.get(url("orders?limit=5&nemp(shipregion)"));
      List<ObjectNode> list = res.data().asList();
      for (ObjectNode result : list)
      {
         String shipregion = result.getString("shipregion");
         assertFalse(Utils.empty(shipregion));
      }
   }
}
