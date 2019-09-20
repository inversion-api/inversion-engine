package io.rocketpartners.cloud.action.dynamo;

import java.util.Collection;

import org.apache.commons.collections4.CollectionUtils;
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

      //get a random record, 
      res = service.get("northwind/dynamodb/orders?limit=1");

      String href = res.findString("data.0.href");
      assertNotNull(href);

      //delete that random record
      ObjectNode record = res.findNode("data.0");
      service.delete(href).hasStatus(204);

      //make sure it was deleted
      service.get(href).assertStatus(404);

      //now put the recored back and lets make sure
      //it looks the same as it did when we pulled it the first time
      record.remove("href");
      res = service.post("northwind/dynamodb/orders", record);
      res.assertStatus(201);
      res.dump();

      //set this back for the comaprison below
      record.put("href", href);

      //makes sure the write back worked
      res = service.get(href);
      assertEquals(1, res.data().length());

      //makes sure the orig and the written back record match
      ObjectNode updatedRecord = res.findNode("data.0");
      for (String key : record.keySet())
      {
         assertTrue(Utils.equal(record.get(key), updatedRecord.get(key)));
      }

      Collection disjunction = CollectionUtils.disjunction(record.keySet(), updatedRecord.keySet());
      if (disjunction.size() > 0)
      {
         //there is an extra or missing column somewhere
         System.err.println("DISJUNCTION: " + disjunction);
         fail();
      }
   }
}
