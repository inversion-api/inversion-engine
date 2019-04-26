package io.rocketpartners.cloud.action.dynamo;

import org.junit.Test;

import io.rocketpartners.cloud.model.ObjectNode;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.service.Service;
import junit.framework.TestCase;

public class TestDynamoDbDeleteActions extends TestCase
{
   @Test
   public void test1()throws Exception
   {
      Service service = DynamoServiceFactory.service();
      Response res = null;
      ObjectNode json = null;

      res = service.get("northwind/dynamodb/orders?limit=100&sort=orderid");
      res.dump();
      json = res.getJson();
      assertEquals(25, json.getArray("data").length());
      assertTrue(json.findString("data.0.href").endsWith("/dynamodb/orders/10257~ORDER"));
      assertEquals(1, service.get("northwind/dynamodb/orders/10257~ORDER").getJson().getArray("data").length());

      service.delete("northwind/dynamodb/orders/10257~ORDER").hasStatus(204);//throws an error if not OK

      service.get("northwind/dynamodb/orders/10257~ORDER").statusEq(404);

      res = service.get("northwind/dynamodb/orders?limit=100&sort=orderid");
      json = res.getJson();
      assertEquals(24, json.getArray("data").length());
   }
}
