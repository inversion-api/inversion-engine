package io.rocketpartners.cloud.action.dynamo;

import java.util.HashMap;
import java.util.Map;

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
   public static final boolean RELOAD_DYNAMO = false;
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
            next = res.findString("meta.next");

            for (Object obj : res.getJson().getArray("data"))
            {
               String href = ((ObjectNode) obj).getString("href");
               System.out.println(href);
               res = service.get(href);
               res.statusOk();
               System.out.println(res);
               Utils.assertEq(1, res.data().length());
               res = service.delete(href);
               res.statusEq(204);
               res = service.get(href);
               res.statusEq(404);
            }
         }
         while (next != null);

         res = service.get("northwind/dynamodb/orders");
         res.statusOk();
         Utils.assertEq(0, res.findArray("data").length());//confirm nothing in dynamo

         res = service.service("GET", "northwind/sql/orders?or(eq(shipname, 'Blauer See Delikatessen'),eq(customerid,HILAA))&pageSize=100&sort=-orderid");
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
}
