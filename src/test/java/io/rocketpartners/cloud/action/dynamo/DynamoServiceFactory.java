package io.rocketpartners.cloud.action.dynamo;

import java.util.HashMap;
import java.util.Map;

import io.rocketpartners.cloud.action.sql.SqlServiceFactory;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.Collection;
import io.rocketpartners.cloud.model.ObjectNode;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.service.Service.ServiceListener;
import io.rocketpartners.cloud.utils.Utils;

public class DynamoServiceFactory
{
   protected static Map<String, Service> services = new HashMap();

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
      Service service = SqlServiceFactory.service();

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

               //delete everything from dynamo
               Response res = service.get("northwind/dynamodb/orders");
               for (Object obj : res.getJson().getArray("data"))
               {
                  ObjectNode node = (ObjectNode) obj;
                  String href = node.getString("href");
                  service.delete(href).statusOk();
               }
               service.get("northwind/dynamodb/orders").statusEq(404);//confirm nothing in dynamo

               res = service.service("GET", "northwind/sql/orders?or(eq(shipname, 'Blauer See Delikatessen'),eq(customerid,HILAA))&pageSize=100&sort=-orderid");
               ObjectNode json = res.getJson();
               System.out.println(json);

               //               res = service.request("GET", "northwind/sql/orders", null).pageSize(100).order("orderid").go();
               //               json = res.getJson();
               //               System.out.println(json);
               Utils.assertEq(json.find("meta.pageSize"), 100);
               Utils.assertEq(json.find("meta.rowCount"), 25);
               Utils.assertEq(json.find("data.0.orderid"), 11058);

               for (Object o : json.getArray("data"))
               {
                  ObjectNode js = (ObjectNode) o;
                  js.put("type", "ORDER");
                  Utils.assertEq(200, service.post("northwind/dynamodb/orders", js).getStatusCode());
               }
            }

         });
      return service;
   }
}
