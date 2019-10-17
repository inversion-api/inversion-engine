package io.inversion.cloud.model;

import java.util.List;

import org.junit.Test;

import io.inversion.cloud.utils.Utils;
import junit.framework.TestCase;

public class TestJSNode extends TestCase
{
   @Test
   public void testCollectNodes1()
   {
      List found = null;
      JSNode doc = JSNode.parseJsonNode(Utils.read(getClass().getResourceAsStream("testCollectNodes1.json")));

      //      found = doc.collect("data.*.basket.lineItems.code");
      //      assertTrue(found.size() == 0);
      //
      //      found = doc.collect("data.*.basket.lineItems.*.code");
      //      assertTrue(found.size() == 2);
      //
      //      found = doc.collect("lineItems.*.code");
      //      assertTrue(found.size() == 0);
      //
      //      found = doc.collect("lineItems.code");
      //      assertTrue(found.size() == 1);
      //
      //      found = doc.collect("data.*.basket.*");
      //      assertTrue(found.size() == 4);

      found = doc.collect("**.lineItems.*.code");
      assertTrue(found.size() == 3);

      System.out.println(found);
   }
}
