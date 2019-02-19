package io.rocketpartners.cloud;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

import io.rocketpartners.cloud.action.sql.SqlPostAction;
import io.rocketpartners.cloud.model.ArrayNode;
import io.rocketpartners.cloud.model.Node;
import io.rocketpartners.cloud.utils.Utils;
import junit.framework.TestCase;

public class TestCollapse extends TestCase
{
   public static void main(String[] args)
   {
      TestCollapse test = new TestCollapse();
      test.testCollapses1();
      test.testCollapses2();
      test.testCollapses3();
   }

   @Test
   public void testCollapses1()
   {
      Node parent = new Node();
      parent.put("name", "testing");

      Node child1 = new Node();
      parent.put("child1", child1);
      child1.put("href", "http://child1");
      child1.put("name", "child1");

      Node child2 = new Node();
      parent.put("child2", child2);

      child2.put("href", "http://child2");
      child2.put("name", "child2");

      Node collapsed = Utils.parseJsonObject(parent.toString());

      SqlPostAction.collapse(collapsed, false, new HashSet(Arrays.asList("child2")), "");

      Node benchmark = Utils.parseJsonObject(parent.toString());
      benchmark = Utils.parseJsonObject(parent.toString());
      benchmark.remove("child2");
      benchmark.put("child2", new Node("href", "http://child2"));

      assertTrue(benchmark.toString().equals(collapsed.toString()));

   }

   @Test
   public void testCollapses2()
   {
      Node parent = new Node();
      parent.put("name", "testing");

      Node child1 = new Node();
      parent.put("child1", child1);
      child1.put("href", "http://child1");
      child1.put("name", "child1");

      ArrayNode arrChildren = new ArrayNode();
      for (int i = 0; i < 5; i++)
      {
         arrChildren.add(new Node("href", "href://child" + i, "name", "child" + i));
      }

      parent.put("arrChildren", arrChildren);

      Node collapsed = Utils.parseJsonObject(parent.toString());

      SqlPostAction.collapse(collapsed, false, new HashSet(Arrays.asList("arrChildren")), "");

      Node benchmark = Utils.parseJsonObject(parent.toString());
      benchmark = Utils.parseJsonObject(parent.toString());
      benchmark.remove("arrChildren");
      arrChildren = new ArrayNode();
      for (int i = 0; i < 5; i++)
      {
         arrChildren.add(new Node("href", "href://child" + i));
      }
      benchmark.put("arrChildren", arrChildren);

      assertTrue(benchmark.toString().equals(collapsed.toString()));

   }

   @Test
   public void testCollapses3()
   {
      Node parent = new Node();
      parent.put("name", "testing");

      Node child1 = new Node();
      parent.put("child1", child1);
      child1.put("href", "http://child1");
      child1.put("name", "child1");

      Node child2 = new Node();
      parent.put("child2", child2);
      child2.put("href", "http://child2");
      child2.put("name", "child2");

      Node child3 = new Node();
      child2.put("child3", child3);
      child3.put("href", "http://child3");
      child3.put("name", "child3");

      Node collapsed = Utils.parseJsonObject(parent.toString());

      SqlPostAction.collapse(collapsed, false, new HashSet(Arrays.asList("child2.child3")), "");

      Node benchmark = Utils.parseJsonObject(parent.toString());
      benchmark = Utils.parseJsonObject(parent.toString());
      benchmark.getNode("child2").getNode("child3").remove("name");

      assertTrue(benchmark.toString().equals(collapsed.toString()));

   }

}
