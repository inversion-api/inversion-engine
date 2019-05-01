package io.rocketpartners.cloud.action.dynamo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
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

import io.rocketpartners.cloud.action.rest.RestAction;
import io.rocketpartners.cloud.action.sql.SqlServiceFactory;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.Collection;
import io.rocketpartners.cloud.model.ObjectNode;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.Utils;

public class DynamoServiceFactory
{
   public static String                  northwind     = "test-northwind";
   public static final boolean           RELOAD_DYNAMO = true;
   protected static Map<String, Service> services      = new HashMap();

   public static synchronized Service service() throws Exception
   {
      return service("northwind", "northwind");
   }

   public static synchronized Service service(String apiName, final String ddl) throws Exception
   {
      Service service = services.get(apiName.toLowerCase());
      if (service != null)
         return service;

      service = buildService(apiName, ddl, "test-" + ddl);

      services.put(apiName, service);

      return service;
   }

   protected static Service buildService(String apiCode, final String ddl, String dynamoTbl) throws Exception
   {
      buildTables();

      Service service = SqlServiceFactory.service();

      final DynamoDb dynamoDb = new DynamoDb("dynamo", dynamoTbl);
      final Api api = service.getApi(apiCode);
      api.withDb(dynamoDb);
      api.withEndpoint("GET,PUT,POST,DELETE", "dynamodb", "*").withAction(new RestAction());

      dynamoDb.startup();

      //      service.withListener(new ServiceListener()
      //         {
      //            @Override
      //            public void onStartup(Service service)
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
      res = service.get("northwind/dynamodb/orders?limit=3");
      res.statusOk();

      if (RELOAD_DYNAMO)
      {
         String next = "northwind/dynamodb/orders?limit=3";
         do
         {
            res = service.get(next);
            System.out.println(res);
            res.statusOk();
            next = res.next();

            for (Object obj : res.getJson().getArray("data"))
            {
               String href = ((ObjectNode) obj).getString("href");
               System.out.println(href);
               res = service.get(href);
               res.statusOk();
               System.out.println(res);
               Utils.assertEq(1, res.data().length());

               res = service.delete(href);
               res.dump();
               res.statusEq(204);
               res = service.get(href);
               res.statusEq(404);
            }
         }
         while (next != null);

         res = service.get("northwind/dynamodb/orders");
         res.statusOk();
         Utils.assertEq(0, res.findArray("data").length());//confirm nothing in dynamo

         res = service.service("GET", "northwind/source/orders?or(eq(shipname, 'Blauer See Delikatessen'),eq(customerid,HILAA))&pageSize=100&sort=-orderid");
         ObjectNode json = res.getJson();
         System.out.println(json);

         //               res = service.request("GET", "northwind/sql/orders", null).pageSize(100).order("orderid").go();
         //               json = res.getJson();
         //               System.out.println(json);
         Utils.assertEq(json.find("meta.pageSize"), 100);
         Utils.assertEq(json.find("meta.foundRows"), 25);
         Utils.assertEq(json.find("data.0.orderid"), 11058);

         for (Object o : json.getArray("data"))
         {
            ObjectNode js = (ObjectNode) o;

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

            res = service.post("northwind/dynamodb/orders", js);
            Utils.assertEq(201, res.getStatusCode());

            res = service.get(res.findString("data.0.href"));

            System.out.println(js);
            System.out.println(res.find("data.0"));

         }
      }
      //            }
      //
      //         });
      return service;
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
