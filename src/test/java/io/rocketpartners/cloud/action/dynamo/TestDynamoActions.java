package io.rocketpartners.cloud.action.dynamo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import io.rocketpartners.cloud.action.sql.TestSqlActions;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.Collection;
import io.rocketpartners.cloud.model.ObjectNode;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.service.Service.ServiceListener;
import junit.framework.TestCase;

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
public class TestDynamoActions extends TestCase
{

   //   public static void main(String[] args) throws Exception
   //   {
   //      TestDynamoActions tests = new TestDynamoActions();
   //      tests.test1();
   //   }

   static Map<String, Service> services = new HashMap();

   // public static void
   //
   public static synchronized Service service(String apiCode, String ddl, String dynamoTbl)
   {
      Service service = services.get(apiCode);
      if (service != null)
         return service;

      service = TestSqlActions.service(apiCode, ddl);

      final Api api = service.getApi(apiCode);
      final DynamoDb dynamoDb = new DynamoDb("dynamo", dynamoTbl);
      api.withDb(dynamoDb);

      service.withListener(new ServiceListener()
         {
            @Override
            public void onStartup(Service service)
            {
               Collection orders = api.getCollection(dynamoTbl + "s");//new Collection(dynamoDb.getTable(dynamoTbl));
               orders.withName("orders");

               orders.getAttribute("hk").withName("orderId"); //get orders by id 
               orders.getAttribute("sk").withName("type");

               orders.getAttribute("gs1hk").withName("employeeId"); //get orders by customer sorted by date
               orders.getAttribute("gs1sk").withName("orderDate");

               orders.getAttribute("ls1").withName("shipCity");
               orders.getAttribute("ls2").withName("shipName");
               orders.getAttribute("ls3").withName("requireDate");

               //orders.getAttribute("gs2hk").setName("customerId"); //get orders by customer sorted by date
               //orders.getAttribute("gs2sk").setName("orderDate");//will be "order"

               orders.withIncludePaths("dynamodb/*");

               api.withCollection(orders);
               api.withEndpoint("GET,PUT,POST,DELETE", "dynamodb", "*").withAction(new DynamoDbRestAction<>());

               //uncomment below to populate db

               //               Response res = service.service("GET", "northwind/sql/orders?or(eq(shipname, 'Blauer See Delikatessen'),eq(customerid,HILAA))&pageSize=100&sort=-orderid");
               //               Node json = res.getJson();
               //               System.out.println(json);
               //
               //               //      res = service.get("northwind/sql/orders").pageSize(100).order("orderid").go();
               //               //      json = res.getJson();
               //               //      System.out.println(json);
               //               assertEquals(json.find("meta.pageSize"), 100);
               //               assertEquals(json.find("meta.rowCount"), 25);
               //               assertEquals(json.find("data.0.orderid"), 11058);
               //
               //               for (Object o : json.getArray("data"))
               //               {
               //                  Node js = (Node) o;
               //                  js.put("type", "ORDER");
               //                  if (service.post("northwind/dynamodb/orders", js).getStatusCode() != 200)
               //                     fail();
               //               }
            }

         });

      services.put(apiCode, service);

      return service;
   }

   @Test
   public void testA() throws Exception
   {
      Service service = service("northwind", "northwind", "test-northwind");
      Response res = null;
      ObjectNode json = null;

      res = service.get("northwind/dynamodb/orders?shipname=Blauer See Delikatessen");
      json = res.getJson();
      //System.out.println(res.getDebug());

      assertEquals(7, json.getArray("data").length());
      assertDebug(res, "DynamoDbQuery: ScanSpec maxPageSize=100 scanIndexForward=true nameMap={#var1=ls2} valueMap={:val1=Blauer See Delikatessen} keyConditionExpression='' filterExpression='#var1 = :val1' projectionExpression=''");
   }

   @Test
   public void testC() throws Exception
   {
      Service service = service("northwind", "northwind", "test-northwind");
      Response res = null;
      ObjectNode json = null;

      res = service.service("GET", "northwind/dynamodb/orders?orderid=11058");
      json = res.getJson();
      System.out.println(res.getDebug());

      assertEquals(json.getArray("data").length(), 1);
      assertDebug(res, "DynamoDbQuery: QuerySpec:'Primary Index' maxPageSize=100 scanIndexForward=true nameMap={#var1=hk} valueMap={:val1=11058} keyConditionExpression='#var1 = :val1' filterExpression='' projectionExpression=''");
   }

   @Test
   public void testD() throws Exception
   {
      Service service = service("northwind", "northwind", "test-northwind");
      Response res = null;
      ObjectNode json = null;

      res = service.get("northwind/dynamodb/orders?orderid=11058&type=ORDER");
      json = res.getJson();
      //System.out.println(res.getDebug());

      assertEquals(json.getArray("data").length(), 1);
      assertDebug(res, "DynamoDbQuery: GetItemSpec partKeyCol=hk partKeyVal=11058 sortKeyCol=sk sortKeyVal=ORDER");

      res = service.get("northwind/dynamodb/orders/11058~ORDER");
      json = res.getJson();
      System.out.println(res.getDebug());

      assertEquals(json.getArray("data").length(), 1);
      assertDebug(res, "DynamoDbQuery: GetItemSpec partKeyCol=hk partKeyVal=11058 sortKeyCol=sk sortKeyVal=ORDER");
   }

   @Test
   public void testE() throws Exception
   {
      Service service = service("northwind", "northwind", "test-northwind");
      Response res = null;
      ObjectNode json = null;

      res = service.get("northwind/dynamodb/orders?eq(OrderId, 11058)&gt(type, 'AAAAA')");
      json = res.getJson();
      //System.out.println(res.getDebug());

      assertEquals(json.getArray("data").length(), 1);
      assertDebug(res, "DynamoDbQuery: QuerySpec:'Primary Index' maxPageSize=100 scanIndexForward=true nameMap={#var1=hk, #var2=sk} valueMap={:val1=11058, :val2=AAAAA} keyConditionExpression='#var1 = :val1 and #var2 > :val2' filterExpression='' projectionExpression=''");
   }

   @Test
   public void testF() throws Exception
   {
      Service service = service("northwind", "northwind", "test-northwind");
      Response res = null;
      ObjectNode json = null;

      res = service.get("northwind/dynamodb/orders?eq(OrderId, 12345)&gt(type, 'AAAAA')&gt(ShipCity,A)");
      json = res.getJson();
      //System.out.println(res.getDebug());

      assertDebug(res, "DynamoDbQuery: QuerySpec:'Primary Index' maxPageSize=100 scanIndexForward=true nameMap={#var1=hk, #var2=sk, #var3=ls1} valueMap={:val1=12345, :val2=AAAAA, :val3=A} keyConditionExpression='#var1 = :val1 and #var2 > :val2' filterExpression='#var3 > :val3' projectionExpression=''");
   }

   @Test
   public void testG() throws Exception
   {
      Service service = service("northwind", "northwind", "test-northwind");
      Response res = null;
      ObjectNode json = null;

      res = service.get("northwind/dynamodb/orders?eq(OrderId, 11058)&sw(type, 'ORD')");
      json = res.getJson();
      //System.out.println(res.getDebug());

      assertEquals(json.getArray("data").length(), 1);
      assertDebug(res, "DynamoDbQuery: QuerySpec:'Primary Index' maxPageSize=100 scanIndexForward=true nameMap={#var1=hk} valueMap={:val1=11058, :val2=ORD} keyConditionExpression='#var1 = :val1 and begins_with(sk,:val2)' filterExpression='' projectionExpression=''");
   }

   @Test
   public void testH() throws Exception
   {
      Service service = service("northwind", "northwind", "test-northwind");
      Response res = null;
      ObjectNode json = null;

      res = service.get("northwind/dynamodb/orders?eq(OrderId, 11058)&sw(type, 'ORD')&eq(shipcity,Mannheim)");
      //res = service.get("northwind/dynamodb/orders?eq(OrderId, 11058)&eq(shipcity,Mannheim)");
      //res = service.get("northwind/dynamodb/orders?eq(shipcity,Mannheim)");
      json = res.getJson();
      //System.out.println(res.getDebug());

      assertEquals(json.getArray("data").length(), 1);
      assertDebug(res, "DynamoDbQuery: QuerySpec:'ls1' maxPageSize=100 scanIndexForward=true nameMap={#var1=hk, #var2=ls1} valueMap={:val1=11058, :val2=Mannheim, :val3=ORD} keyConditionExpression='#var1 = :val1 and #var2 = :val2' filterExpression='begins_with(sk,:val3)' projectionExpression=''");
   }

   @Test
   public void testI() throws Exception
   {
      Service service = service("northwind", "northwind", "test-northwind");
      Response res = null;
      ObjectNode json = null;

      res = service.get("northwind/dynamodb/orders?eq(OrderId, 11058)&sw(type, 'ORD')&eq(EmployeeId,9)&eq(OrderDate,'2014-10-29T00:00-0400')");
      json = res.getJson();
      //System.out.println(res.getDebug());

      assertEquals(json.getArray("data").length(), 1);
      assertDebug(res, "DynamoDbQuery: QuerySpec:'gs1' maxPageSize=100 scanIndexForward=true nameMap={#var1=gs1hk, #var2=gs1sk, #var3=hk} valueMap={:val1=9, :val2=2014-10-29T00:00-0400, :val3=ORD, :val4=11058} keyConditionExpression='#var1 = :val1 and #var2 = :val2' filterExpression='begins_with(sk,:val3) and #var3 = :val4' projectionExpression=''");
   }

   @Test
   public void testK() throws Exception
   {
      Service service = service("northwind", "northwind", "test-northwind");
      Response res = null;
      ObjectNode json = null;

      res = service.get("northwind/dynamodb/orders?gt(OrderId, 1)&eq(type, ORDER)");
      json = res.getJson();
      //System.out.println(res.getDebug());

      //assertEquals(json.getArray("data").length(), 1);
      assertDebug(res, "DynamoDbQuery: ScanSpec maxPageSize=100 scanIndexForward=true nameMap={#var1=sk, #var2=hk} valueMap={:val1=ORDER, :val2=1} keyConditionExpression='' filterExpression='#var1 = :val1 and #var2 > :val2' projectionExpression=''");
   }

   void assertDebug(Response resp, String... matches)
   {
      String debug = resp.getDebug();

      int idx = debug.indexOf("DynamoDbQuery");
      String debugLine = debug.substring(idx, debug.indexOf("\n", idx)).trim();

      for (String match : matches)
      {
         List<String> matchTokens = split(match, ' ', '\'', '"', '{', '}');
         for (String matchToken : matchTokens)
         {
            if (debugLine.indexOf(matchToken) < 0)
               fail("missing debug match: '" + match + "' in debug line: " + debugLine);
         }
      }

   }

   List<String> split(String string, char splitOn, char... quoteChars)
   {
      List<String> strings = new ArrayList();
      Set quotes = new HashSet();
      for (char c : quoteChars)
         quotes.add(c);

      boolean quoted = false;
      StringBuffer buff = new StringBuffer("");
      for (int i = 0; i < string.length(); i++)
      {
         char c = string.charAt(i);

         if (c == splitOn && !quoted)
         {
            if (buff.length() > 0)
            {
               strings.add(buff.toString());
               buff = new StringBuffer("");
            }
            continue;
         }
         else if (quotes.contains(c))
         {
            quoted = !quoted;
         }

         buff.append(c);
      }
      if (buff.length() > 0)
         strings.add(buff.toString());

      return strings;
   }

}
