package io.rocketpartners.cloud.action.dynamo;

import java.util.ArrayList;
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

public class TestDynamoDb extends TestCase
{
   @Test
   public void testEntityKey()
   {
      check("asdf", "1234", "asdf~1234");
      check("as\\df", "12\\34", "as\\\\df~12\\\\34");
      check("asdf\\", "\\1234", "asdf\\\\~\\\\1234");
      check("as~df", "12~34", "as\\~df~12\\~34");
      check("as\\~df", "12~34", "as\\\\\\~df~12\\~34");
   }

   void check(String hashKey, String sortKey, String entityKey)
   {
      String key = DynamoDb.toEntityKey(hashKey, sortKey);
      assertEquals(entityKey, key);

      String[] parsed = DynamoDb.fromEntityKey(entityKey);
      assertEquals(hashKey, parsed[0]);

      if (sortKey == null)
         assertNull(parsed[1]);
      else
         assertEquals(sortKey, parsed[1]);
   }

   //------------------------------------------------------------------------------------------
   //------------------------------------------------------------------------------------------
   //Test Utils

   public static void assertDebug(Response resp, String... matches)
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

   static Map<String, Service> services = new HashMap();

   // public static void
   //
   
   public static synchronized Service service()
   {
      return service("northwind", "northwind", "test-northwind");
   }
   
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

               Response res = service.service("GET", "northwind/sql/orders?or(eq(shipname, 'Blauer See Delikatessen'),eq(customerid,HILAA))&pageSize=100&sort=-orderid");
               ObjectNode json = res.getJson();
               System.out.println(json);

//                     res = service.rest("GET","northwind/sql/orders").pageSize(100).order("orderid").go();
//                     json = res.getJson();
//                     System.out.println(json);
               assertEquals(json.find("meta.pageSize"), 100);
               assertEquals(json.find("meta.rowCount"), 25);
               assertEquals(json.find("data.0.orderid"), 11058);

               for (Object o : json.getArray("data"))
               {
                  ObjectNode js = (ObjectNode) o;
                  js.put("type", "ORDER");
                  if (service.post("northwind/dynamodb/orders", js).getStatusCode() != 200)
                     fail();
               }
            }

         });

      services.put(apiCode, service);

      return service;
   }

   public static List<String> split(String string, char splitOn, char... quoteChars)
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
