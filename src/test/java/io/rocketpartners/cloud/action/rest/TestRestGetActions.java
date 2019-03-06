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
      String next = url("orders?limit=5");
      do
      {
         res = service.get(next);

         System.out.println(res.meta());

         if (res.data().size() == 0)
            break;

         total += res.data().length();
         pages += 1;

         next = res.findString("meta.next");

         assertEquals(5, res.findArray("data").length());
         assertEquals(5, res.find("meta.pageSize"));
      }
      while (pages < 20 && next != null);

      assertEquals(5, pages);
      assertEquals(25, total);
   }

   public void testExpandsOneToMany01() throws Exception
   {
      Service service = service();
      Response res = null;

      res = service.get(url("orders/10395?expands=customer,employee,employee.reportsto"));
      System.out.println(res.getDebug());
      assertTrue(res.findString("data.0.customer.href").endsWith("/customers/HILAA"));
      assertTrue(res.findString("data.0.employee.href").endsWith("/employees/6"));
      assertTrue(res.findString("data.0.employee.reportsto.href").endsWith("/employees/5"));
   }

   public void testExpandsOneToMany02() throws Exception
   {
      Service service = service();
      Response res = null;

      res = service.get(url("orders/10395?expands=employee.reportsto.employees"));
      System.out.println(res.getDebug());

      assertTrue(res.findString("data.0.employee.href").endsWith("/employees/6"));
      assertTrue(res.findString("data.0.employee.reportsto.href").endsWith("/employees/5"));
      assertTrue(res.findString("data.0.employee.reportsto.employees.0.href").endsWith("/employees/6"));

      assertTrue(res.getJson().toString().indexOf("\"@link\" : \"http://localhost/northwind/sql/employees/6\"") > 0);
   }

   public void testExpandsManyToOne01() throws Exception
   {
      Service service = service();
      Response res = null;

      res = service.get("http://localhost/northwind/source/employees/5?expands=employees");
      System.out.println(res.getDebug());

      assertEquals(3, res.findArray("data.0.employees").length());
      assertNotNull(res.find("data.0.employees.0.lastname"));
   }

   public void testExpandsManyToMany01() throws Exception
   {
      Service service = service();
      Response res = null;

      res = service.get("http://localhost/northwind/sql/employees/6?expands=territories");
      System.out.println(res.getDebug());

      assertEquals(5, res.findArray("data.0.territories").length());
      assertNotNull(res.find("data.0.territories.0.territorydescription"));
   }
   
   public void testIncludes01() throws Exception
   {
      Service service = service();
      Response res = null;

      //res = service.get(url("orders/10395?expands=employee.reportsto.employees&includes=shipname"));
      //res = service.get(url("orders/10395?includes=shipname"));
      res = service.get("http://localhost/northwind/source/orders/10395?includes=shipname");
      System.out.println(res.getDebug());

      assertEquals("HILARION-Abastos", res.findString("data.0.shipname"));
      assertEquals(1, res.findNode("data.0").size());
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
