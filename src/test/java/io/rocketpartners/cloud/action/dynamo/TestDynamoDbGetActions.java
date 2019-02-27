package io.rocketpartners.cloud.action.dynamo;

import org.junit.Test;

import io.rocketpartners.cloud.action.rest.TestRestGetActions;
import io.rocketpartners.cloud.model.ObjectNode;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.Utils;

/**
 * TODO - need to test against a table that does not have a primary sort key
 * 
 * 
 * Patterns we want to test
 * - system should select the right primary or global based on supplied params
 * - have primary and global secondary that share a hash key with different sort keys
 * - have primary and global secondary with different hash keys and same sort key
 * - primary hash + local secondary sort vs global with same hash and different sort
 * - primary hash + local secondary sort vs global with same hash and sort (might be a table design mistake, but what should the system do?)
 * 
 * - can't GetItem on a GSI - https://stackoverflow.com/questions/43732835/getitem-from-secondary-index-with-dynamodb
 * 
 * EQ | NE | LE | LT | GE | GT | NOT_NULL | NULL | CONTAINS | NOT_CONTAINS | BEGINS_WITH | IN | BETWEEN
 * 
 * X = ANY condition
 * 
 * SCENARIO            primary                 gs1                     gs2       
 *                   HK      SK          GS1-HK      GS1-SK      GS2-HK   GS2-SK   LS1   LS2   LS3    FIELD-N        COOOSE
 *   A                                                                                                   X            Scan                need to try to scan on a non indexed field
 *   B                                                                               X                                Scan
 *   C                =                                                                                               Query - PRIMARY
 *   D                =       =                                                                                       GetItem - PRIMARY
 *   E                =       >                                                                                       Query - PRIMARY
 *   F                =       >                                                      >                                Query - PRIMARY
 *   G                =       sw                                                                                      Query - PRIMARY
 *   H                =       sw                                                     =                                Query - LS1
 *   I                =       sw          =               =                                                           Query - GS1
 *   J                =       sw          =               sw         =        =                                       Query - GS2
 *   K                gt      =                                                                                       Scan - Primary
 *   L                gt      sw          =                                                                           ????                                             
 * 
 * 
 * 
 * Access Patters for Order Table
 * 
 *   `OrderID` INTEGER NOT NULL AUTO_INCREMENT,
 *   `CustomerID` VARCHAR(5),
 *   `EmployeeID` INTEGER,
 *   `OrderDate` DATETIME,
 *   `RequiredDate` DATETIME,
 *   `ShippedDate` DATETIME,
 *   `ShipVia` INTEGER,
 *   `Freight` DECIMAL(10,4) DEFAULT 0,
 *   `ShipName` VARCHAR(40),
 *   `ShipAddress` VARCHAR(60),
 *   `ShipCity` VARCHAR(15),
 *   `ShipRegion` VARCHAR(15),
 *   `ShipPostalCode` VARCHAR(10),
 *   `ShipCountry` VARCHAR(15),
 * 
 * LS1 - ShipCity
 * 
 * ORDERS
 * Find an order by id                           HK: OrderID           SK: type       ----  12345   | 'ORDER'
 * Find all orders for a given customer       GS1HK: CustomerID     GS1SK: OrderDate  ----  99999   | '2013-01-08'
 * UNUSED - List orders by date -                HK: type              SK: OrderDate  ----  'ORDER' | '2013-01-08'
 * UNUSED - List orders by employee              HK: employeeId        SK: 
 * 
 * SCENARIO
 *   A - eq(ShipPostalCode, 30305) 
 *   B - 
 *   C - eq(OrderId, 12345)
 *   D - eq(OrderId, 12345)&eq(type, 'ORDER')
 *   E - eq(OrderId, 12345)&gt(type, 'AAAAA')
 *   F - eq(OrderId, 12345)&gt(type, 'AAAAA')&eq(ShipCity,Atlanta)
 *   G - eq(OrderId, 12345)&sw(type, 'ORD')
 *   H - eq(OrderId, 12345)&sw(type, 'ORD')&eq(ShipCity,Atlanta)
 *   I - eq(OrderId, 12345)&sw(type, 'ORD')&eq(CustomerId,9999)&eq(OrderDate,'2013-01-08')
 *   J - ????
 *   K - gt(OrderId, 12345)&eq(type, 'ORDER")
 *   L - ???
 *   
 *   
 *   TODO - need to get projections into indexes so you don't return empty attributes that look null
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
   public void test0() throws Exception
   {
      Service service = service();
      Response res = null;
      ObjectNode json = null;

      res = service.get("northwind/dynamodb/orders?limit=5").statusOk();
      json = res.getJson();
      System.out.println(res.getDebug());

      assertEquals(5, json.getArray("data").length());
      Utils.assertDebug(res, "DynamoDbQuery: ScanSpec maxPageSize=5 scanIndexForward=true nameMap={} valueMap={} keyConditionExpression='' filterExpression='' projectionExpression=''");

      res = service.get("northwind/dynamodb/orders?limit=100&sort=orderid");
      json = res.getJson();
      assertEquals(25, json.getArray("data").length());
      assertTrue(json.findString("data.0.href").endsWith("/dynamodb/orders/10641~ORDER"));
      assertEquals(1, service.get("northwind/dynamodb/orders/10641~ORDER").getJson().getArray("data").length());

   }

   @Test
   public void testPagination0() throws Exception
   {
      Service service = service();
      Response res = null;

      int total = 0;
      int iterations = 0;
      String next = url("orders?limit=5");
      do
      {
         res = service.get(next);
         total += res.data().length();
         iterations += 1;
         next = res.findString("meta.next");

         System.out.println(res);

         assertEquals(5, res.findArray("data").length());
         assertEquals(5, res.findArray("meta.pageSize"));
         //assertEquals(iterations, res.find("meta.pageNum"));
      }
      while (iterations < 20 && next != null);

      assertEquals(5, iterations);
      assertEquals(25, total);

   }

   @Test
   public void testA() throws Exception
   {
      Service service = service();
      Response res = null;
      ObjectNode json = null;

      res = service.get("northwind/dynamodb/orders?shipname=Blauer See Delikatessen").statusOk();
      json = res.getJson();
      //System.out.println(res.getDebug());

      assertEquals(7, json.getArray("data").length());
      Utils.assertDebug(res, "DynamoDbQuery: ScanSpec maxPageSize=100 scanIndexForward=true nameMap={#var1=ls2} valueMap={:val1=Blauer See Delikatessen} keyConditionExpression='' filterExpression='#var1 = :val1' projectionExpression=''");
   }

   @Test
   public void testC() throws Exception
   {
      Service service = service();
      Response res = null;
      ObjectNode json = null;

      res = service.service("GET", "northwind/dynamodb/orders?orderid=11058").statusOk();
      json = res.getJson();
      System.out.println(res.getDebug());

      assertEquals(json.getArray("data").length(), 1);
      Utils.assertDebug(res, "DynamoDbQuery: QuerySpec:'Primary Index' maxPageSize=100 scanIndexForward=true nameMap={#var1=hk} valueMap={:val1=11058} keyConditionExpression='#var1 = :val1' filterExpression='' projectionExpression=''");
   }

   @Test
   public void testD() throws Exception
   {
      Service service = service();
      Response res = null;
      ObjectNode json = null;

      res = service.get("northwind/dynamodb/orders?orderid=11058&type=ORDER").statusOk();
      json = res.getJson();
      //System.out.println(res.getDebug());

      assertEquals(json.getArray("data").length(), 1);
      Utils.assertDebug(res, "DynamoDbQuery: GetItemSpec partKeyCol=hk partKeyVal=11058 sortKeyCol=sk sortKeyVal=ORDER");

      res = service.get("northwind/dynamodb/orders/11058~ORDER");
      json = res.getJson();
      System.out.println(res.getDebug());

      assertEquals(json.getArray("data").length(), 1);
      Utils.assertDebug(res, "DynamoDbQuery: GetItemSpec partKeyCol=hk partKeyVal=11058 sortKeyCol=sk sortKeyVal=ORDER");
   }

   @Test
   public void testE() throws Exception
   {
      Service service = service();
      Response res = null;
      ObjectNode json = null;

      res = service.get("northwind/dynamodb/orders?eq(OrderId, 11058)&gt(type, 'AAAAA')").statusOk();
      json = res.getJson();
      //System.out.println(res.getDebug());

      assertEquals(json.getArray("data").length(), 1);
      Utils.assertDebug(res, "DynamoDbQuery: QuerySpec:'Primary Index' maxPageSize=100 scanIndexForward=true nameMap={#var1=hk, #var2=sk} valueMap={:val1=11058, :val2=AAAAA} keyConditionExpression='#var1 = :val1 and #var2 > :val2' filterExpression='' projectionExpression=''");
   }

   @Test
   public void testF() throws Exception
   {
      Service service = service();
      Response res = null;
      ObjectNode json = null;

      res = service.get("northwind/dynamodb/orders?eq(OrderId, 12345)&gt(type, 'AAAAA')&gt(ShipCity,A)").statusOk();
      json = res.getJson();
      //System.out.println(res.getDebug());

      Utils.assertDebug(res, "DynamoDbQuery: QuerySpec:'Primary Index' maxPageSize=100 scanIndexForward=true nameMap={#var1=hk, #var2=sk, #var3=ls1} valueMap={:val1=12345, :val2=AAAAA, :val3=A} keyConditionExpression='#var1 = :val1 and #var2 > :val2' filterExpression='#var3 > :val3' projectionExpression=''");
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
      Utils.assertDebug(res, "DynamoDbQuery: QuerySpec:'Primary Index' maxPageSize=100 scanIndexForward=true nameMap={#var1=hk} valueMap={:val1=11058, :val2=ORD} keyConditionExpression='#var1 = :val1 and begins_with(sk,:val2)' filterExpression='' projectionExpression=''");
   }

   @Test
   public void testH() throws Exception
   {
      Service service = service();
      Response res = null;
      ObjectNode json = null;

      res = service.get("northwind/dynamodb/orders?eq(OrderId, 11058)&sw(type, 'ORD')&eq(shipcity,Mannheim)").statusOk();
      //res = service.get("northwind/dynamodb/orders?eq(OrderId, 11058)&eq(shipcity,Mannheim)");
      //res = service.get("northwind/dynamodb/orders?eq(shipcity,Mannheim)");
      json = res.getJson();
      //System.out.println(res.getDebug());

      assertEquals(json.getArray("data").length(), 1);
      Utils.assertDebug(res, "DynamoDbQuery: QuerySpec:'ls1' maxPageSize=100 scanIndexForward=true nameMap={#var1=hk, #var2=ls1} valueMap={:val1=11058, :val2=Mannheim, :val3=ORD} keyConditionExpression='#var1 = :val1 and #var2 = :val2' filterExpression='begins_with(sk,:val3)' projectionExpression=''");
   }

   @Test
   public void testI() throws Exception
   {
      Service service = service();
      Response res = null;
      ObjectNode json = null;

      res = service.get("northwind/dynamodb/orders?eq(OrderId, 11058)&sw(type, 'ORD')&eq(EmployeeId,9)&eq(OrderDate,'2014-10-29T00:00-0400')").statusOk();
      json = res.getJson();
      //System.out.println(res.getDebug());

      assertEquals(json.getArray("data").length(), 1);
      Utils.assertDebug(res, "DynamoDbQuery: QuerySpec:'gs1' maxPageSize=100 scanIndexForward=true nameMap={#var1=gs1hk, #var2=gs1sk, #var3=hk} valueMap={:val1=9, :val2=2014-10-29T00:00-0400, :val3=ORD, :val4=11058} keyConditionExpression='#var1 = :val1 and #var2 = :val2' filterExpression='begins_with(sk,:val3) and #var3 = :val4' projectionExpression=''");
   }

   @Test
   public void testK() throws Exception
   {
      Service service = service();
      Response res = null;
      ObjectNode json = null;

      res = service.get("northwind/dynamodb/orders?gt(OrderId, 1)&eq(type, ORDER)").statusOk();
      json = res.getJson();
      //System.out.println(res.getDebug());

      //assertEquals(json.getArray("data").length(), 1);
      Utils.assertDebug(res, "DynamoDbQuery: ScanSpec maxPageSize=100 scanIndexForward=true nameMap={#var1=sk, #var2=hk} valueMap={:val1=ORDER, :val2=1} keyConditionExpression='' filterExpression='#var1 = :val1 and #var2 > :val2' projectionExpression=''");
   }

   //   @Test
   //   @Override
   //   public void test0A() throws Exception
   //   {
   //      super.test0A();
   //      Utils.assertDebug(response(), "DynamoDbQuery: ScanSpec maxPageSize=100 scanIndexForward=true nameMap={#var1=ls2} valueMap={:val1=Blauer See Delikatessen} keyConditionExpression='' filterExpression='#var1 = :val1' projectionExpression=''");
   //   }
   //
   //   @Test
   //   @Override
   //   public void test0C() throws Exception
   //   {
   //      super.test0C();
   //      Utils.assertDebug(response(), "DynamoDbQuery: QuerySpec:'Primary Index' maxPageSize=100 scanIndexForward=true nameMap={#var1=hk} valueMap={:val1=11058} keyConditionExpression='#var1 = :val1' filterExpression='' projectionExpression=''");
   //   }
   //
   //   @Test
   //   @Override
   //   public void test0D() throws Exception
   //   {
   //      super.test0D();
   //      Utils.assertDebug(response(), "DynamoDbQuery: GetItemSpec partKeyCol=hk partKeyVal=11058 sortKeyCol=sk sortKeyVal=ORDER");
   //   }
   //
   //   @Test
   //   @Override
   //   public void test0E() throws Exception
   //   {
   //      super.test0E();
   //      Utils.assertDebug(response(), "DynamoDbQuery: QuerySpec:'Primary Index' maxPageSize=100 scanIndexForward=true nameMap={#var1=hk, #var2=sk} valueMap={:val1=11058, :val2=AAAAA} keyConditionExpression='#var1 = :val1 and #var2 > :val2' filterExpression='' projectionExpression=''");
   //   }
   //
   //   @Test
   //   @Override
   //   public void test0F() throws Exception
   //   {
   //      super.test0F();
   //      Utils.assertDebug(response(), "DynamoDbQuery: QuerySpec:'Primary Index' maxPageSize=100 scanIndexForward=true nameMap={#var1=hk, #var2=sk, #var3=ls1} valueMap={:val1=12345, :val2=AAAAA, :val3=A} keyConditionExpression='#var1 = :val1 and #var2 > :val2' filterExpression='#var3 > :val3' projectionExpression=''");
   //   }
   //
   //   @Test
   //   @Override
   //   public void test0G() throws Exception
   //   {
   //      super.test0G();
   //      Utils.assertDebug(response(), "DynamoDbQuery: QuerySpec:'Primary Index' maxPageSize=100 scanIndexForward=true nameMap={#var1=hk} valueMap={:val1=11058, :val2=ORD} keyConditionExpression='#var1 = :val1 and begins_with(sk,:val2)' filterExpression='' projectionExpression=''");
   //   }
   //
   //   @Test
   //   @Override
   //   public void test0H() throws Exception
   //   {
   //      super.test0H();
   //      Utils.assertDebug(response(), "DynamoDbQuery: QuerySpec:'ls1' maxPageSize=100 scanIndexForward=true nameMap={#var1=hk, #var2=ls1} valueMap={:val1=11058, :val2=Mannheim, :val3=ORD} keyConditionExpression='#var1 = :val1 and #var2 = :val2' filterExpression='begins_with(sk,:val3)' projectionExpression=''");
   //   }
   //
   //   @Test
   //   @Override
   //   public void test0I() throws Exception
   //   {
   //      super.test0I();
   //      Utils.assertDebug(response(), "DynamoDbQuery: QuerySpec:'gs1' maxPageSize=100 scanIndexForward=true nameMap={#var1=gs1hk, #var2=gs1sk, #var3=hk} valueMap={:val1=9, :val2=2014-10-29T00:00-0400, :val3=ORD, :val4=11058} keyConditionExpression='#var1 = :val1 and #var2 = :val2' filterExpression='begins_with(sk,:val3) and #var3 = :val4' projectionExpression=''");
   //   }
   //
   //   @Test
   //   @Override
   //   public void test0K() throws Exception
   //   {
   //      super.test0K();
   //      Utils.assertDebug(response(), "DynamoDbQuery: ScanSpec maxPageSize=100 scanIndexForward=true nameMap={#var1=sk, #var2=hk} valueMap={:val1=ORDER, :val2=1} keyConditionExpression='' filterExpression='#var1 = :val1 and #var2 > :val2' projectionExpression=''");
   //   }

}
