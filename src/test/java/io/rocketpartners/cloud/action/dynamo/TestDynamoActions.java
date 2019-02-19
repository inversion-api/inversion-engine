package io.rocketpartners.cloud.action.dynamo;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import io.rocketpartners.cloud.action.sql.TestSqlActions;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.Collection;
import io.rocketpartners.cloud.model.Node;
import io.rocketpartners.cloud.service.Response;
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

   public static void main(String[] args) throws Exception
   {
      TestDynamoActions tests = new TestDynamoActions();
      tests.test1();
   }

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
            public void onInit(Service service)
            {
               dynamoDb.bootstrapApi();

               //Api api = service.getApi(apiCode).withDb(dynamoDb).getApi();
               //      Db dynamoDb = .withTable(dynamoTbl)//
               //                                           .withColumn("hk", "S")//
               //                                           .withColumn("sk", "S")//
               //
               //                                           .withColumn("gs1hk", "N")//
               //                                           .withColumn("gs1sk", "N")//
               //                                           .withColumn("gs2hk", "N")//
               //                                           .withColumn("gs2sk", "S")//
               //                                           .withColumn("ls1", "S")//
               //                                           .withColumn("ls2", "N")//
               //                                           .withColumn("ls3", "B")//
               //                                           .withColumn("field1", "S")//
               //                                           .withColumn("field2", "S")//
               //                                           .withColumn("field3", "S")//
               //                                           .withColumn("field4", "S")//
               //                                           .withColumn("field5", "S")//
               //                                           .withColumn("field6", "S")//
               //                                           .getDb();

               Collection orders = api.getCollection(dynamoTbl + "s");//new Collection(dynamoDb.getTable(dynamoTbl));
               orders.withName("orders");

               orders.getAttribute("hk").withName("orderId"); //get orders by id 
               orders.getAttribute("sk").withName("type");

               orders.getAttribute("gs1hk").withName("employeeId"); //get orders by customer sorted by date
               orders.getAttribute("gs1sk").withName("orderDate");

               //orders.getAttribute("gs2hk").setName("customerId"); //get orders by customer sorted by date
               //orders.getAttribute("gs2sk").setName("orderDate");//will be "order"

               //      orders.getAttribute("field1").withName("shipName");
               //      orders.getAttribute("field2").withName("shipAddress");
               //      orders.getAttribute("ls1").withName("shipCity");
               //      orders.getAttribute("field4").withName("shipRegion");
               //      orders.getAttribute("field5").withName("shipPostalCode");
               //      orders.getAttribute("field6").withName("shipCountry");

               orders.withIncludePaths("dynamodb/*");

               api.withCollection(orders);
               api.withEndpoint("GET,PUT,POST,DELETE", "dynamodb", "*").withAction(new DynamoDbRestAction<>());

            }

         });

      services.put(apiCode, service);

      return service;
   }

   @Test
   public void test1() throws Exception
   {
      Service service = service("northwind", "northwind", "test-northwind");
      Response res = null;
      Node json = null;

      res = service.service("GET", "northwind/sql/orders?or(eq(shipname, 'Blauer See Delikatessen'),eq(customerid,HILAA))&pageSize=100&sort=-orderid");
      json = res.getJson();
      System.out.println(json);

      //      res = service.get("northwind/sql/orders").pageSize(100).order("orderid").go();
      //      json = res.getJson();
      //      System.out.println(json);
      assertEquals(json.find("meta.pageSize"), 100);
      assertEquals(json.find("meta.rowCount"), 25);
      assertEquals(json.find("data.0.orderid"), 11058);

      for (Object o : json.getArray("data"))
      {
         Node js = (Node) o;
         js.put("type", "ORDER");
         if (service.post("northwind/dynamodb/orders", js).getStatusCode() != 200)
            fail();
      }

      res = service.service("GET", "northwind/dynamodb/orders?eq(shipname, 'Blauer See Delikatessen')&pageSize=100");
      json = res.getJson();
      System.out.println(json);
      assertEquals(json.getArray("data").length(), 7);

   }

}
