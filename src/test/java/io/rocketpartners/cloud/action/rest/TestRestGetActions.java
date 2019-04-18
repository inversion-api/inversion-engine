package io.rocketpartners.cloud.action.rest;

import org.junit.Test;

import io.rocketpartners.cloud.model.ObjectNode;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.service.Service;
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
      String cp = collectionPath();

      if (cp.length() == 0)
         return path;

      if (path.indexOf(cp) > -1 || path.startsWith("http"))
         return path;

      if (!cp.endsWith("/"))
         cp += "/";

      while (path.startsWith("/"))
         path = path.substring(1, path.length());

      return cp + path;
   }

   @Test
   public void testLimit0() throws Exception
   {
      Service service = service();
      Response res = null;
      ObjectNode json = null;

      res = service.get(url("orders?limit=5"));
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

      String url = url("orders?limit=2&sort=orderid&type=ORDER");

      res = service.get(url);
      System.out.println(res.getDebug());

      assertEquals(2, res.findArray("data").length());
      String href = res.findString("data.0.href");
      assertTrue(href.endsWith("/orders/10257"));

      res = service.get(url("orders?limit=2&sort=-orderid"));

      assertEquals(2, res.findArray("data").length());
      href = res.findString("data.0.href");
      assertTrue(href.endsWith("/orders/11058"));
   }

   @Test
   public void testPagination0() throws Exception
   {
      Service service = service();
      Response res = null;

      int total = 0;
      int pages = 0;
      String next = url("orders?limit=5&sort=orderId");
      do
      {
         res = service.get(next);

         System.out.println(res.getDebug());

         if (res.data().size() == 0)
            break;

         total += res.data().length();
         pages += 1;

         next = res.next();

         assertEquals(5, res.findArray("data").length());
         assertEquals(5, res.find("meta.pageSize"));
      }
      while (pages < 20 && next != null);

      assertEquals(5, pages);
      assertEquals(25, total);
   }

}
