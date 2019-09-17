package io.rocketpartners.cloud.action.dynamo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import io.rocketpartners.cloud.model.ObjectNode;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.SqlUtils;
import io.rocketpartners.cloud.utils.Utils;
import junit.framework.TestCase;

public class TestDynamoDbPostActions extends TestCase
{
   protected Service service() throws Exception
   {
      return DynamoServiceFactory.service();
   }

   protected String collectionPath()
   {
      return "northwind/dynamodb/";
   }

   /**
    * Pulls 5 orders out of dynamo, deletes those orders, pulls a fresh copy 
    * from h2 and inserts them back into dynamo verifying both the delete and insert
    * were successful
    * 
    * @throws Exception
    */
   @Test
   public void testBatchWriteToDynamoDB() throws Exception
   {
      Service service = service();
      Response res = null;
      ObjectNode json = null;

      //-- find 5 orders that are in dynamo that we can delete and repost
      res = service.get("northwind/dynamodb/orders?type=ORDER&limit=5");

      List<String> hrefs = new ArrayList();
      List orderIds = new ArrayList();

      res.data().forEach(o -> orderIds.add(((ObjectNode) o).get("orderid")));
      res.data().forEach(o -> hrefs.add(((ObjectNode) o).getString("href")));

      //-- make sure the orders were deleted
      for (String href : hrefs)
      {
         service.delete(href);
         res = service.get(href);
         assertTrue(res.data().size() == 0);
      }

      //-- pull the orders again fresh from h2
      String url = "northwind/h2/orders?in(orderId, " + SqlUtils.getQuotedInClauseStr(orderIds).replaceAll("\"", "") + ")";
      res = service.get(url);
      res.dump();
      assertTrue(res.data().size() == 5);

      //-- reconfigure the h2 json for posting to dynamo
      List<Map<String, Object>> mapList = new ArrayList();
      List<String> posted = new ArrayList();
      for (Object obj : res.data())
      {
         ObjectNode node = (ObjectNode) obj;

         url = "northwind/dynamodb/orders?type=ORDER&orderid=" + node.get("orderid");
         posted.add(url);

         service.delete(url);
         res = service.get(url);//make sure was deleted
         assertTrue(res.data().size() == 0);

         node.remove("href");
         node.put("type", "ORDER");
         node.asMap();
         mapList.add(node);
      }

      res = service.post("northwind/dynamodb/orders", mapList);
      Utils.assertEq(5, res.findArray("data").length(), "Confirm 25 documents posted to DynamoDB");

      //-- make sure the response url is correctly formed, should look like: 
      //-- http://localhost/northwind/dynamodb/orders/102680~ORDER
      for (Object o : res.data())
      {
         String key = ((ObjectNode) o).getString("href");
         key = key.substring(key.lastIndexOf("/") + 1, key.length());
         List<String> parts = Utils.explode("~", key);
         assertTrue(parts.size() == 2);
         assertTrue(parts.get(0).matches("[0-9]+"));
         assertEquals("ORDER", parts.get(1));
      }

      //-- finally double check query that each posted item exists
      for (String postedUrl : posted)
      {
         res = service.get(postedUrl);
         assertTrue(res.data().size() == 1);
      }
   }

   /**
    * Test that a put/post does a full replace not a patch.
    * Even fields that are not sent should be replaced.
    * 
    * @throws Exception
    */
   @Test
   public void testFullOverwriteDynamoDB() throws Exception
   {
      Service service = service();
      Response res = null;
      res = service.get("northwind/dynamodb/orders?type=ORDER&limit=1");

      ObjectNode orig = res.findNode("data.0");

      ObjectNode clone = Utils.parseObjectNode(orig.toString());

      clone.put("gs2hk", "testing");
      res = service.put(clone.getString("href"), clone);
      res = service.get(clone.getString("href"));
      assertEquals("testing", res.find("data.0.gs2hk"));

      clone.put("gs2hk", null);
      res = service.put(clone.getString("href"), clone);
      res = service.get(clone.getString("href"));
      assertNull(res.find("data.0.gs2hk"));

      res = service.put(clone.getString("href"), orig);

   }

}
