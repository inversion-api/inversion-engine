package io.rocketpartners.cloud.action.dynamo;

import static io.rocketpartners.cloud.utils.Utils.assertDebug;

import org.junit.Test;

import io.rocketpartners.cloud.action.rest.TestRestGetActions;
import io.rocketpartners.cloud.model.ObjectNode;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.service.Service;

/**
 * @see README.md
 */
public class TestDynamoDbGetActions extends TestRestGetActions
{

   protected String collectionPath()
   {
      return "northwind/dynamodb/";
   }

   @Override
   protected Service service() throws Exception
   {
      return DynamoServiceFactory.service();
   }

   @Test
   public void testPagination0() throws Exception
   {
      Service service = service();
      Response res = null;

      int total = 0;
      int pages = 0;
      String next = url("orders?type=ORDER&limit=5&sort=orderId");
      do
      {
         res = service.get(next);

         System.out.println(res.getDebug());

         assertDebug(res, "QuerySpec:'gs3'");

         if (pages == 0)
         {
            assertTrue(res.findString("data.0.href").endsWith("/orders/10257~ORDER"));
         }

         if (pages == 4)
         {
            assertTrue(res.findString("data.0.href").endsWith("/orders/10957~ORDER"));
         }

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

   @Test
   public void testSort01() throws Exception
   {
      Service service = service();
      Response res = null;
      ObjectNode json = null;
      String href = null;

      String url = url("orders?limit=2&type=ORDER");

      res = service.get(url);

      assertDebug(res, "DynamoDbQuery: QuerySpec:'gs3' maxPageSize=2 scanIndexForward=true nameMap={#var1=sk} valueMap={:val1=ORDER} keyConditionExpression='(#var1 = :val1)' filterExpression='' projectionExpression=''");
      assertEquals(2, res.findArray("data").length());
      assertTrue(res.findString("data.0.href").endsWith("northwind/dynamodb/orders/10257~ORDER"));

      res = service.get(url("orders?limit=2&type=ORDER&sort=-orderid"));
      assertDebug(res, "DynamoDbQuery: QuerySpec:'gs3' maxPageSize=2 scanIndexForward=false nameMap={#var1=sk} valueMap={:val1=ORDER} keyConditionExpression='(#var1 = :val1)' filterExpression='' projectionExpression=''");
      assertEquals(2, res.findArray("data").length());
      assertTrue(res.findString("data.0.href").endsWith("northwind/dynamodb/orders/11058~ORDER"));
   }

   @Test
   public void test0() throws Exception
   {
      Service service = service();
      Response res = null;
      ObjectNode json = null;

      res = service.get("northwind/dynamodb/orders?limit=5").statusOk();
      json = res.getJson();

      assertEquals(5, json.getArray("data").length());
      assertDebug(res, "DynamoDbQuery: ScanSpec maxPageSize=5 scanIndexForward=true nameMap={} valueMap={} keyConditionExpression='' filterExpression='' projectionExpression=''");

      res = service.get("northwind/dynamodb/orders?limit=100&sort=orderid");
      json = res.getJson();
      assertEquals(25, json.getArray("data").length());
      assertTrue(json.findString("data.0.href").endsWith("/dynamodb/orders/10641~ORDER"));
      assertEquals(1, service.get("northwind/dynamodb/orders/10641~ORDER").getJson().getArray("data").length());

   }

   @Test
   public void testA() throws Exception
   {
      Service service = service();
      Response res = null;
      ObjectNode json = null;

      res = service.get("northwind/dynamodb/orders?shipname=Blauer See Delikatessen").statusOk();
      json = res.getJson();
      assertEquals(7, json.getArray("data").length());
      assertDebug(res, "DynamoDbQuery: ScanSpec maxPageSize=100 scanIndexForward=true nameMap={#var1=ls2} valueMap={:val1=Blauer See Delikatessen} keyConditionExpression='' filterExpression='(#var1 = :val1)' projectionExpression=''");
   }

   @Test
   public void testC() throws Exception
   {
      Service service = service();
      Response res = null;
      ObjectNode json = null;

      res = service.service("GET", "northwind/dynamodb/orders?orderid=11058").statusOk();
      json = res.getJson();

      assertEquals(json.getArray("data").length(), 1);
      assertDebug(res, "DynamoDbQuery: QuerySpec:'Primary Index' maxPageSize=100 scanIndexForward=true nameMap={#var1=hk} valueMap={:val1=11058} keyConditionExpression='(#var1 = :val1)' filterExpression='' projectionExpression=''");
   }

   @Test
   public void testD() throws Exception
   {
      Service service = service();
      Response res = null;
      ObjectNode json = null;

      res = service.get("northwind/dynamodb/orders?orderid=11058&type=ORDER").statusOk();
      json = res.getJson();

      assertEquals(json.getArray("data").length(), 1);
      assertDebug(res, "DynamoDbQuery: GetItemSpec partKeyCol=hk partKeyVal=11058 sortKeyCol=sk sortKeyVal=ORDER");

      res = service.get("northwind/dynamodb/orders/11058~ORDER");
      json = res.getJson();

      assertEquals(json.getArray("data").length(), 1);
      assertDebug(res, "DynamoDbQuery: GetItemSpec partKeyCol=hk partKeyVal=11058 sortKeyCol=sk sortKeyVal=ORDER");
   }

   @Test
   public void testE() throws Exception
   {
      Service service = service();
      Response res = null;
      ObjectNode json = null;

      res = service.get("northwind/dynamodb/orders?eq(OrderId, 11058)&gt(type, 'AAAAA')").statusOk();
      json = res.getJson();

      assertEquals(json.getArray("data").length(), 1);
      assertDebug(res, "DynamoDbQuery: QuerySpec:'Primary Index' maxPageSize=100 scanIndexForward=true nameMap={#var1=hk, #var2=sk} valueMap={:val1=11058, :val2=AAAAA} keyConditionExpression='(#var1 = :val1) and (#var2 > :val2)' filterExpression='' projectionExpression=''");
   }

   @Test
   public void testF() throws Exception
   {
      Service service = service();
      Response res = null;
      ObjectNode json = null;

      res = service.get("northwind/dynamodb/orders?eq(OrderId, 12345)&gt(type, 'AAAAA')&gt(ShipCity,A)").statusOk();
      json = res.getJson();

      assertDebug(res, "DynamoDbQuery: QuerySpec:'Primary Index' maxPageSize=100 scanIndexForward=true nameMap={#var1=hk, #var2=sk, #var3=ls1} valueMap={:val1=12345, :val2=AAAAA, :val3=A} keyConditionExpression='(#var1 = :val1) and (#var2 > :val2)' filterExpression='(#var3 > :val3)' projectionExpression=''");
   }

   @Test
   public void testG() throws Exception
   {
      Service service = service();
      Response res = null;
      ObjectNode json = null;

      res = service.get("northwind/dynamodb/orders?eq(OrderId, 11058)&sw(type, 'ORD')");
      json = res.getJson();
      //System.out.println(res.getDebug());

      assertEquals(json.getArray("data").length(), 1);
      assertDebug(res, "DynamoDbQuery: QuerySpec:'Primary Index' maxPageSize=100 scanIndexForward=true nameMap={#var1=hk} valueMap={:val1=11058, :val2=ORD} keyConditionExpression='(#var1 = :val1) and begins_with(sk,:val2)' filterExpression='' projectionExpression=''");
   }

   @Test
   public void testH() throws Exception
   {
      Service service = service();
      Response res = null;
      ObjectNode json = null;

      res = service.get("northwind/dynamodb/orders?eq(OrderId, 11058)&sw(type, 'ORD')&eq(shipcity,Mannheim)").statusOk();
      json = res.getJson();

      assertEquals(json.getArray("data").length(), 1);
      assertDebug(res, "DynamoDbQuery: QuerySpec:'ls1' maxPageSize=100 scanIndexForward=true nameMap={#var1=hk, #var2=ls1} valueMap={:val1=11058, :val2=Mannheim, :val3=ORD} keyConditionExpression='(#var1 = :val1) and (#var2 = :val2)' filterExpression='begins_with(sk,:val3)' projectionExpression=''");
   }

   @Test
   public void testI() throws Exception
   {
      Service service = service();
      Response res = null;
      ObjectNode json = null;

      res = service.get("northwind/dynamodb/orders?eq(OrderId, 11058)&sw(type, 'ORD')&eq(EmployeeId,9)&eq(OrderDate,'2014-10-29T00:00-0400')").statusOk();
      json = res.getJson();

      assertEquals(json.getArray("data").length(), 1);
      assertDebug(res, "DynamoDbQuery: QuerySpec:'gs1' maxPageSize=100 scanIndexForward=true nameMap={#var1=gs1hk, #var2=gs1sk, #var3=hk} valueMap={:val1=9, :val2=2014-10-29T00:00-0400, :val3=ORD, :val4=11058} keyConditionExpression='(#var1 = :val1) and (#var2 = :val2)' filterExpression='begins_with(sk,:val3) and (#var3 = :val4)' projectionExpression=''");
   }

   @Test
   public void testK() throws Exception
   {
      Service service = service();
      Response res = null;

      res = service.get("northwind/dynamodb/orders?gt(OrderId, 1)&eq(type, ORDER)").statusOk();
      assertDebug(res, "DynamoDbQuery: QuerySpec:'gs3' maxPageSize=100 scanIndexForward=true nameMap={#var1=sk, #var2=hk} valueMap={:val1=ORDER, :val2=1} keyConditionExpression='(#var1 = :val1) and (#var2 > :val2)' filterExpression='' projectionExpression=''");
   }

   @Test
   public void testAA() throws Exception
   {
      Service service = service();
      Response res = null;

      res = service.get("northwind/dynamodb/orders?eq(type, ORDER)&or(eq(shipname, 'Blauer See Delikatessen'),eq(customerid,HILAA))");
      res.statusOk();
   }
}
