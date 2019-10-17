package io.inversion.cloud.model;

import java.util.List;

import org.junit.Test;

import io.inversion.cloud.utils.Utils;
import junit.framework.TestCase;

public class TestJSNode extends TestCase
{
   //   @Test
   //   public void testCollectNodes1()
   //   {
   //      List found = null;
   //      JSNode doc = JSNode.parseJsonNode(Utils.read(getClass().getResourceAsStream("testCollectNodes1.json")));
   //
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
   //      assertTrue(found.size() == 3);
   //
   //      found = doc.collect("**.lineItems.*.code");
   //      assertTrue(found.size() == 3);
   //
   //      System.out.println(found);
   //   }
   //
   //   @Test
   //   public void testDiff1()
   //   {
   //      List found = null;
   //      JSNode doc1 = JSNode.parseJsonNode(Utils.read(getClass().getResourceAsStream("testDiff1.1.json")));
   //      JSNode doc2 = JSNode.parseJsonNode(Utils.read(getClass().getResourceAsStream("testDiff1.2.json")));
   //
   //      JSArray patches = doc2.diff(doc1);
   //
   //      doc1.patch(patches);
   //
   //      assertTrue(doc1.toString().equals(doc2.toString()));
   //
   //      System.out.println(found);
   //   }
   //
   //   @Test
   //   public void testDiff2()
   //   {
   //
   //      JSNode doc1 = JSNode.parseJsonNode(Utils.read(getClass().getResourceAsStream("testDiff2.1.json")));
   //      JSArray patches = JSNode.parseJsonArray(Utils.read(getClass().getResourceAsStream("testDiff2.2.json")));
   //
   //      doc1.patch(patches);
   //      System.out.println(doc1);
   //      assertEquals("028000003647", doc1.findString("0.basket.lineItems.1.code"));
   //   }

   /**
    * This test was developed for an error in diff/patch that could result in the same JSNode
    * appearing multiple times in the object graph and causing serialization problems.
    * 
    * The fix was to copy the patches before applying or after computing inside to the JSNode methods
    */
   @Test
   public void testDiff3()
   {
      JSNode stateDoc = JSNode.parseJsonNode(Utils.read(getClass().getResourceAsStream("testDiff3.1.json")));
      JSNode apiEvent = JSNode.parseJsonNode(Utils.read(getClass().getResourceAsStream("testDiff3.2.json")));

      JSNode meta = stateDoc.getNode("meta");
      meta.put("events", new JSArray(apiEvent));

      JSNode body = apiEvent.getNode("body");

      if (!body.isArray())
      {
         body = new JSArray(body);
      }

      JSArray patches = body.diff(stateDoc.getNode("data"));
      if (patches.size() > 0)
      {
         apiEvent.put("patches", patches);
         stateDoc.getNode("data").patch(patches);
      }

      stateDoc = JSNode.parseJsonNode(stateDoc.toString());

      assertEquals("028000003647", stateDoc.findString("data.0.basket.lineItems.1.code"));
      assertEquals("028000003647", stateDoc.findString("meta.events.0.body.basket.lineItems.1.code"));

   }

}
