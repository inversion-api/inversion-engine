package io.rocketpartners.cloud.action.dynamo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import io.rocketpartners.cloud.action.rest.TestRestGetActions;
import io.rocketpartners.cloud.model.ObjectNode;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.Utils;

public class TestDynamoDbPostActions extends TestRestGetActions
{
   @Override
   protected Service service() throws Exception
   {
      return DynamoServiceFactory.service();
   }

   protected String collectionPath()
   {
      return "northwind/dynamodb/";
   }

   @Test
   public void testBatchWriteToDynamoDB() throws Exception
   {
      Service service = service();
      Response res = null;
      ObjectNode json = null;

      String next = "northwind/dynamodb/orders?limit=3";
      do
      {
         res = service.get(next);
         res.statusOk();
         next = res.findString("meta.next");

         for (Object obj : res.getJson().getArray("data"))
         {
            String href = ((ObjectNode) obj).getString("href");
            res = service.get(href);
            res.statusOk();
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
      Utils.assertEq(0, res.findArray("data").length(), "Confirm nothing in dynamo");

      res = service.get("northwind/h2/orders?limit=25");
      json = res.getJson();
      Utils.assertEq(25, res.findArray("data").length(), "Confirm 25 documents obtained from sql");

      List<Map<String, Object>> mapList = new ArrayList();
      for (Object obj : json.getArray("data"))
      {
         ObjectNode node = (ObjectNode) obj;
         node.remove("href");
         node.put("type", "ORDER");
         node.asMap();
         mapList.add(node);
      }

      res = service.post("northwind/dynamodb/orders", mapList);
      Utils.assertEq(25, res.findArray("data").length(), "Confirm 25 documents posted to DynamoDB");
   }

}
