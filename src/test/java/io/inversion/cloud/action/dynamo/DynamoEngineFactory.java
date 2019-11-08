package io.inversion.cloud.action.dynamo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.LocalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.Projection;
import com.amazonaws.services.dynamodbv2.model.ProjectionType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.TableDescription;

import io.inversion.cloud.action.rest.RestAction;
import io.inversion.cloud.action.sql.SqlEngineFactory;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.JSArray;
import io.inversion.cloud.model.Collection;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.service.Engine;
import io.inversion.cloud.utils.Utils;

public class DynamoEngineFactory
{
   public static String                 northwind    = "test-northwind";
   public static boolean                relaodDynamo = false;
   protected static Map<String, Engine> engines      = new HashMap();

   public static synchronized Engine service() throws Exception
   {
      return service("northwind", "northwind");
   }

   public static synchronized Engine service(String apiName, final String ddl) throws Exception
   {
      Engine engine = engines.get(apiName.toLowerCase());
      if (engine != null)
         return engine;

      engine = buildEngine(apiName, ddl, "test-" + ddl);

      engines.put(apiName, engine);

      return engine;
   }

   protected static Engine buildEngine(String apiCode, final String ddl, String dynamoTbl) throws Exception
   {
      buildTables();

      Engine engine = SqlEngineFactory.service(true, true);

      final DynamoDb dynamoDb = new DynamoDb("dynamo", dynamoTbl);
      Table table = new DynamoDB(dynamoDb.getDynamoClient()).getTable(dynamoTbl);
      //      //--
      //      //--
      //      //--
      //
      //      //DynamoDb  ScanSpec maxPageSize=500 scanIndexForward=true nameMap={#var1=shipRegion} valueMap={} keyConditionExpression='' filterExpression='attribute_not_exists(#var1)' projectionExpression=''
      //
      //      Map nameMap = new HashMap();
      //      nameMap.put("#var1", "shipregion");
      //
      //      Map valueMap = new HashMap();
      //      valueMap.put(":val1", null);
      //
      //      ScanSpec scanSpec = new ScanSpec();
      //      scanSpec.withMaxPageSize(1000);
      //      scanSpec.withMaxResultSize(1000);
      //      //scanSpec.withFilterExpression("attribute_not_exists(#var1)");
      //      scanSpec.withFilterExpression("(#var1 = :val1)");
      //      scanSpec.withNameMap(nameMap);
      //      scanSpec.withValueMap(valueMap);
      //
      //      
      //
      //      ItemCollection<ScanOutcome> scanResult = table.scan(scanSpec);
      //      int num = 0;
      //      for (Item item : scanResult)
      //      {
      //         num += 1;
      //         String val = item.getString("shipRegion");
      //         if (val == null)
      //            val = item.getString("shipregion");
      //         val += "";
      //
      //         System.out.println(val + " - " + item.asMap());
      //         if (!"null".equalsIgnoreCase(val))
      //         {
      //            System.out.println("should be null: '" + StringEscapeUtils.escapeJava(val) + "'");
      //            throw new RuntimeException("WRONG!!!");
      //         }
      //      }
      //
      //      if (num == 0)
      //         throw new RuntimeException("WRONG!!!");
      //
      //      System.out.println("done");

      //--
      //--
      //--

      final Api api = engine.getApi(apiCode);
      api.withDb(dynamoDb);
      api.withEndpoint("GET,PUT,POST,DELETE", "dynamodb/*", new RestAction());

      dynamoDb.startup();

      //      service.withListener(new EngineListener()
      //         {
      //            @Override
      //            public void onStartup(Engine service)
      //            {
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

      Response res = null;

      //10248 - 11077
      //      relaodDynamo = relaodDynamo || !"10248".equals(service.get("northwind/dynamodb/orders?limit=1&type=ORDER&sort=orderid").findString("data.0.orderid"));
      //      //relaodDynamo = relaodDynamo || !"11077".equals(service.get("northwind/dynamodb/orders?limit=1&type=ORDER&sort=-orderid&includes=href").findString("data.0.orderid"));
      //      relaodDynamo = relaodDynamo || !"11077".equals(service.get("northwind/dynamodb/orders?orderid=11077&limit=1&type=ORDER").findString("data.0.orderid"));

      if (relaodDynamo)
      {
         System.out.print("CLEARING DYNAMO...");

         ItemCollection<ScanOutcome> deleteoutcome = table.scan();
         Iterator<Item> iterator = deleteoutcome.iterator();

         int deletedCount = 0;
         while (iterator.hasNext())
         {
            deletedCount += 1;
            if (deletedCount % 100 == 0)
               System.out.print(deletedCount + " ");

            Item item = iterator.next();
            Object hk = item.get("hk");
            Object sk = item.get("sk");
            table.deleteItem("hk", hk, "sk", sk);
         }

         //--confirm all deleted
         res = engine.get("northwind/dynamodb/orders");
         res.statusOk();
         Utils.assertEq(0, res.findArray("data").length());//confirm nothing in dynamo

         System.out.println("");
         System.out.println("RELOADING DYNAMO...");

         int pages = 0;
         int total = 0;
         String start = "northwind/source/orders?pageSize=100&sort=orderid";
         String next = start;
         do
         {
            JSArray toPost = new JSArray();

            res = engine.get(next);
            if (res.data().size() == 0)
               break;

            pages += 1;
            next = res.next();

            //-- now post to DynamoDb
            for (Object o : res.data())
            {
               total += 1;
               JSNode js = (JSNode) o;

               js.remove("href");
               js.put("type", "ORDER");

               for (String key : js.keySet())
               {
                  String value = js.getString(key);
                  if (value != null && (value.startsWith("http://") || value.startsWith("https://")))
                  {
                     value = value.substring(value.lastIndexOf("/") + 1, value.length());
                     js.remove(key);

                     if (!key.toLowerCase().endsWith("id"))
                        key = key + "Id";

                     js.put(key, value);
                  }
               }
               toPost.add(js);
            }

            res = engine.post("northwind/dynamodb/orders", toPost);
            Utils.assertEq(201, res.getStatusCode());
            System.out.println("DYNAMO LOADED: " + total);// + " - " + js.getString("orderid"));
         }
         while (pages < 200 && next != null);

         Utils.assertEq(9, pages);
         Utils.assertEq(830, total);
      }

      return engine;
   }

   public static void main(String[] args) throws Exception
   {
      buildTables();
   }

   public static void buildTables() throws Exception
   {
      if (!tableExists(northwind))
         createNorthwind();
   }

   public static boolean tableExists(String tableName) throws Exception
   {
      AmazonDynamoDB client = DynamoDb.buildDynamoClient(tableName);
      DynamoDB dynamoDB = new DynamoDB(client);

      return dynamoDB.getTable(tableName) != null;
   }

   public static void deleteTable(String tableName) throws Exception
   {
      AmazonDynamoDB client = DynamoDb.buildDynamoClient(tableName);
      DeleteTableRequest dtr = new DeleteTableRequest().withTableName(tableName);
      client.deleteTable(dtr);
   }

   public static Table createNorthwind() throws Exception
   {
      List<AttributeDefinition> attrs = new ArrayList<>();

      attrs.add(new AttributeDefinition().withAttributeName("hk").withAttributeType("N"));
      attrs.add(new AttributeDefinition().withAttributeName("sk").withAttributeType("S"));

      attrs.add(new AttributeDefinition().withAttributeName("gs1hk").withAttributeType("N"));
      attrs.add(new AttributeDefinition().withAttributeName("gs1sk").withAttributeType("S"));

      attrs.add(new AttributeDefinition().withAttributeName("gs2hk").withAttributeType("S"));
      //attrs.add(new AttributeDefinition().withAttributeName("gs2sk").withAttributeType("S"));

      attrs.add(new AttributeDefinition().withAttributeName("ls1").withAttributeType("S"));
      attrs.add(new AttributeDefinition().withAttributeName("ls2").withAttributeType("S"));
      attrs.add(new AttributeDefinition().withAttributeName("ls3").withAttributeType("S"));

      List<KeySchemaElement> keys = new ArrayList<>();
      keys.add(new KeySchemaElement().withAttributeName("hk").withKeyType(KeyType.HASH));
      keys.add(new KeySchemaElement().withAttributeName("sk").withKeyType(KeyType.RANGE));

      List<LocalSecondaryIndex> lsxs = new ArrayList();
      lsxs.add(new LocalSecondaryIndex().withIndexName("ls1").withKeySchema(new KeySchemaElement().withAttributeName("hk").withKeyType(KeyType.HASH)//
            , new KeySchemaElement().withAttributeName("ls1").withKeyType(KeyType.RANGE)));

      lsxs.add(new LocalSecondaryIndex().withIndexName("ls2").withKeySchema(new KeySchemaElement().withAttributeName("hk").withKeyType(KeyType.HASH)//
            , new KeySchemaElement().withAttributeName("ls2").withKeyType(KeyType.RANGE)));

      lsxs.add(new LocalSecondaryIndex().withIndexName("ls3").withKeySchema(new KeySchemaElement().withAttributeName("hk").withKeyType(KeyType.HASH)//
            , new KeySchemaElement().withAttributeName("ls3").withKeyType(KeyType.RANGE)));

      List<GlobalSecondaryIndex> gsxs = new ArrayList();
      gsxs.add(new GlobalSecondaryIndex().withIndexName("gs1").withKeySchema(new KeySchemaElement().withAttributeName("gs1hk").withKeyType(KeyType.HASH), new KeySchemaElement().withAttributeName("gs1sk").withKeyType(KeyType.RANGE)));
      gsxs.add(new GlobalSecondaryIndex().withIndexName("gs2").withKeySchema(new KeySchemaElement().withAttributeName("gs2hk").withKeyType(KeyType.HASH), new KeySchemaElement().withAttributeName("ls3").withKeyType(KeyType.RANGE)));
      gsxs.add(new GlobalSecondaryIndex().withIndexName("gs3").withKeySchema(new KeySchemaElement().withAttributeName("sk").withKeyType(KeyType.HASH), new KeySchemaElement().withAttributeName("hk").withKeyType(KeyType.RANGE)));

      for (LocalSecondaryIndex lsx : lsxs)
      {
         lsx.setProjection(new Projection().withProjectionType(ProjectionType.ALL));
      }

      for (GlobalSecondaryIndex gsx : gsxs)
      {
         gsx.setProjection(new Projection().withProjectionType(ProjectionType.ALL));
         gsx.withProvisionedThroughput(new ProvisionedThroughput()//
                                                                  .withReadCapacityUnits(5L)//
                                                                  .withWriteCapacityUnits(5L));
      }

      AmazonDynamoDB client = DynamoDb.buildDynamoClient("northwind");
      DynamoDB dynamoDB = new DynamoDB(client);

      CreateTableRequest request = new CreateTableRequest()//
                                                           .withGlobalSecondaryIndexes(gsxs)//
                                                           .withLocalSecondaryIndexes(lsxs).withTableName("test-northwind")//
                                                           .withKeySchema(keys)//
                                                           .withAttributeDefinitions(attrs)//
                                                           .withProvisionedThroughput(new ProvisionedThroughput()//
                                                                                                                 .withReadCapacityUnits(5L)//
                                                                                                                 .withWriteCapacityUnits(5L));

      Table table = dynamoDB.createTable(request);

      try
      {
         table.waitForActive();
      }
      catch (Exception ex)
      {
         table.waitForActive();
      }

      return table;
   }

}
