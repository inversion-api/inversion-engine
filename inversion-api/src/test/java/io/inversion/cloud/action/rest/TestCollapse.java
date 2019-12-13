package io.inversion.cloud.action.rest;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

import io.inversion.cloud.model.JSArray;
import io.inversion.cloud.model.JSNode;
import junit.framework.TestCase;

public class TestCollapse extends TestCase
{
   @Test
   public void testCollapses1()
   {
      JSNode parent = new JSNode();
      parent.put("name", "testing");

      JSNode child1 = new JSNode();
      parent.put("child1", child1);
      child1.put("href", "http://child1");
      child1.put("name", "child1");

      JSNode child2 = new JSNode();
      parent.put("child2", child2);

      child2.put("href", "http://child2");
      child2.put("name", "child2");

      JSNode collapsed = JSNode.parseJsonNode(parent.toString());

      RestPostAction.collapse(collapsed, false, new HashSet(Arrays.asList("child2")), "");

      JSNode benchmark = JSNode.parseJsonNode(parent.toString());
      benchmark = JSNode.parseJsonNode(parent.toString());
      benchmark.remove("child2");
      benchmark.put("child2", new JSNode("href", "http://child2"));

      assertTrue(benchmark.toString().equals(collapsed.toString()));

   }

   @Test
   public void testCollapses2()
   {
      JSNode parent = new JSNode();
      parent.put("name", "testing");

      JSNode child1 = new JSNode();
      parent.put("child1", child1);
      child1.put("href", "http://child1");
      child1.put("name", "child1");

      JSArray arrChildren = new JSArray();
      for (int i = 0; i < 5; i++)
      {
         arrChildren.add(new JSNode("href", "href://child" + i, "name", "child" + i));
      }

      parent.put("arrChildren", arrChildren);

      JSNode collapsed = JSNode.parseJsonNode(parent.toString());

      RestPostAction.collapse(collapsed, false, new HashSet(Arrays.asList("arrChildren")), "");

      JSNode benchmark = JSNode.parseJsonNode(parent.toString());
      benchmark = JSNode.parseJsonNode(parent.toString());
      benchmark.remove("arrChildren");
      arrChildren = new JSArray();
      for (int i = 0; i < 5; i++)
      {
         arrChildren.add(new JSNode("href", "href://child" + i));
      }
      benchmark.put("arrChildren", arrChildren);

      assertTrue(benchmark.toString().equals(collapsed.toString()));

   }

   @Test
   public void testCollapses3()
   {
      JSNode parent = new JSNode();
      parent.put("name", "testing");

      JSNode child1 = new JSNode();
      parent.put("child1", child1);
      child1.put("href", "http://child1");
      child1.put("name", "child1");

      JSNode child2 = new JSNode();
      parent.put("child2", child2);
      child2.put("href", "http://child2");
      child2.put("name", "child2");

      JSNode child3 = new JSNode();
      child2.put("child3", child3);
      child3.put("href", "http://child3");
      child3.put("name", "child3");

      JSNode collapsed = JSNode.parseJsonNode(parent.toString());

      RestPostAction.collapse(collapsed, false, new HashSet(Arrays.asList("child2.child3")), "");

      JSNode benchmark = JSNode.parseJsonNode(parent.toString());
      benchmark = JSNode.parseJsonNode(parent.toString());
      benchmark.getNode("child2").getNode("child3").remove("name");

      assertTrue(benchmark.toString().equals(collapsed.toString()));

   }

}
