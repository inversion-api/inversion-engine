package io.rocketpartners.cloud.action.dynamo;

import org.junit.Test;

import io.rocketpartners.cloud.model.ObjectNode;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.Utils;
import junit.framework.TestCase;

public class TestDynamoDbDeleteActions extends TestCase
{
   @Test
   public void test1() throws Exception
   {
      Service service = DynamoServiceFactory.service();
      Response res = null;

      res = service.get("northwind/dynamodb/orders?limit=1");
      
      String href = res.findString("data.0.href");
      assertNotNull(href);
      
      Object orderId = res.find("data.0.orderid");
      
      Utils.assertDebug(res,  "DynamoDb", "GetItemSpec partKeyCol=hk partKeyVal=" + orderId + " sortKeyCol=sk sortKeyVal=ORDER");

      ObjectNode record = res.findNode("data.0");

      service.delete(href).hasStatus(204);
      service.get(href).statusEq(404);

      record.remove("href");
      service.post("northwind/dynamodb/orders", record).statusEq(404);

      res = service.get(href);
      assertEquals(1, res.data().length());

      ObjectNode updatedRecord = res.findNode("data.0");
      assertEquals(record.toString(), updatedRecord.toString());
   }
}
