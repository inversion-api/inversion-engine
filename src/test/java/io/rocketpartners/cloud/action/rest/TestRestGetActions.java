package io.rocketpartners.cloud.action.rest;

import org.junit.Test;

import io.rocketpartners.cloud.action.dynamo.TestDynamoDb;
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

      res = service.get(url("orders?limit=2&sort=orderid"));
      json = res.getJson();

      assertEquals(2, json.getArray("data").length());
      String href = json.findString("data.0.href");
      assertTrue(href.endsWith("/orders/10257"));

      res = service.get(url("orders?limit=2&sort=-orderid"));
      json = res.getJson();

      assertEquals(2, json.getArray("data").length());
      href = res.findString("data.0.href");
      assertTrue(href.endsWith("/orders/11058"));
   }

   @Test
   public void testPagination0() throws Exception
   {
      Service service = service();
      Response res = null;
      ObjectNode json = null;

      json = service.get(url("orders?limit=5")).getJson();
      int total = json.getArray("data").length();;
      int iterations = 1;

      int i = 0;
      String next = null;
      while (i < 20 && (next = json.findString("meta.next")) != null)
      {
         i++;
         System.out.println(json.get("meta"));

         assertEquals(5, json.getArray("data").length());
         assertEquals(5, json.find("meta.pageSize"));
         assertEquals(iterations, json.find("meta.pageNum"));

         res = service.get(next);
         json = res.getJson();

         total += json.getArray("data").length();
         iterations += 1;
      }

      assertEquals(5, iterations);
      assertEquals(25, total);

   }

   //   @Test
   //   public void test0A() throws Exception
   //   {
   //      Service service = service();
   //      Response res = null;
   //      ObjectNode json = null;
   //
   //      res = service.get(url("orders?shipname=Blauer See Delikatessen"));
   //      json = res.getJson();
   //      assertEquals(7, json.getArray("data").length());
   //
   //   }
   //
   //   @Test
   //   public void test0C() throws Exception
   //   {
   //      Service service = service();
   //      Response res = null;
   //      ObjectNode json = null;
   //
   //      res = service.service("GET", url("orders?orderid=11058"));
   //      json = res.getJson();
   //      System.out.println(res.getDebug());
   //
   //      assertEquals(json.getArray("data").length(), 1);
   //
   //   }
   //
   //   @Test
   //   public void test0D() throws Exception
   //   {
   //      Service service = service();
   //      Response res = null;
   //      ObjectNode json = null;
   //
   //      res = service.get(url("orders?orderid=11058&type=ORDER"));
   //      json = res.getJson();
   //      assertEquals(json.getArray("data").length(), 1);
   //
   //   }
   //
   //   @Test
   //   public void test0D2() throws Exception
   //   {
   //      Service service = service();
   //      Response res = null;
   //      ObjectNode json = null;
   //
   //      //res = service.get(url("orders/11058~ORDER"));
   //      //res = service.get(url("orders/11058?type=ORDER"));
   //      res = service.get(url("orders/11058"));
   //
   //      json = res.getJson();
   //      System.out.println(res.getDebug());
   //
   //      assertEquals(1, res.findInt("meta.rowCount"));
   //      assertEquals(1, json.getArray("data").length());
   //
   //   }
   //
   //   @Test
   //   public void test0E() throws Exception
   //   {
   //      Service service = service();
   //      Response res = null;
   //      ObjectNode json = null;
   //
   //      res = service.get(url("orders?eq(OrderId, 11058)&gt(type, 'AAAAA')"));
   //      json = res.getJson();
   //
   //      assertEquals(1, json.getArray("data").length());
   //
   //   }
   //
   //   @Test
   //   public void test0F() throws Exception
   //   {
   //      throw new RuntimeException("implement me for base not just dynamo");
   //      //      
   //      //      Service service = service();
   //      //      Response res = null;
   //      //      ObjectNode json = null;
   //      //
   //      //      res = service.get(url("orders?eq(OrderId, 12345)&gt(type, 'AAAAA')&gt(ShipCity,A)"));
   //      //      json = res.getJson();
   //      //      assertDebug(res, "DynamoDbQuery: QuerySpec:'Primary Index' maxPageSize=100 scanIndexForward=true nameMap={#var1=hk, #var2=sk, #var3=ls1} valueMap={:val1=12345, :val2=AAAAA, :val3=A} keyConditionExpression='#var1 = :val1 and #var2 > :val2' filterExpression='#var3 > :val3' projectionExpression=''");
   //      //      
   //      //      
   //   }
   //
   //   @Test
   //   public void test0G() throws Exception
   //   {
   //      throw new RuntimeException("implement me for base not just dynamo");
   //      //      Service service = service();
   //      //      Response res = null;
   //      //      ObjectNode json = null;
   //      //
   //      //      res = service.get(url("orders?eq(OrderId, 11058)&sw(type, 'ORD')"));
   //      //      json = res.getJson();
   //      //      //System.out.println(res.getDebug());
   //      //
   //      //      assertEquals(json.getArray("data").length(), 1);
   //      //      assertDebug(res, "DynamoDbQuery: QuerySpec:'Primary Index' maxPageSize=100 scanIndexForward=true nameMap={#var1=hk} valueMap={:val1=11058, :val2=ORD} keyConditionExpression='#var1 = :val1 and begins_with(sk,:val2)' filterExpression='' projectionExpression=''");
   //      //      
   //      //      
   //   }
   //
   //   @Test
   //   public void test0H() throws Exception
   //   {
   //      Service service = service();
   //      Response res = null;
   //      ObjectNode json = null;
   //
   //      res = service.get(url("orders?eq(OrderId, 11058)&sw(type, 'ORD')&eq(shipcity,Mannheim)"));
   //      json = res.getJson();
   //      assertEquals(1, json.getArray("data").length());
   //
   //   }
   //
   //   @Test
   //   public void test0I() throws Exception
   //   {
   //      Service service = service();
   //      Response res = null;
   //      ObjectNode json = null;
   //
   //      res = service.get(url("orders?eq(OrderId, 11058)&sw(type, 'ORD')&eq(EmployeeId,9)&eq(OrderDate,'2014-10-29T00:00-0400')"));
   //      json = res.getJson();
   //      assertEquals(1, json.getArray("data").length());
   //
   //   }
   //
   //   @Test
   //   public void test0K() throws Exception
   //   {
   //      throw new RuntimeException("implement me for base not just dynamo");
   //      //      
   //      //      Service service = service();
   //      //      Response res = null;
   //      //      ObjectNode json = null;
   //      //
   //      //      res = service.get(url("orders?gt(OrderId, 1)&eq(type, ORDER)"));
   //      //      json = res.getJson();
   //      //      //System.out.println(res.getDebug());
   //      //
   //      //      //assertEquals(json.getArray("data").length(), 1);
   //      //      assertDebug(res, "DynamoDbQuery: ScanSpec maxPageSize=100 scanIndexForward=true nameMap={#var1=sk, #var2=hk} valueMap={:val1=ORDER, :val2=1} keyConditionExpression='' filterExpression='#var1 = :val1 and #var2 > :val2' projectionExpression=''");
   //   }
}
