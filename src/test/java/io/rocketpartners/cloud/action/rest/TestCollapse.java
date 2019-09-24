package io.rocketpartners.cloud.action.rest;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

import io.rocketpartners.cloud.model.JsonArray;
import io.rocketpartners.cloud.model.JsonMap;
import io.rocketpartners.cloud.utils.Utils;
import junit.framework.TestCase;

public class TestCollapse extends TestCase
{
   @Test
   public void testCollapses1()
   {
      JsonMap parent = new JsonMap();
      parent.put("name", "testing");

      JsonMap child1 = new JsonMap();
      parent.put("child1", child1);
      child1.put("href", "http://child1");
      child1.put("name", "child1");

      JsonMap child2 = new JsonMap();
      parent.put("child2", child2);

      child2.put("href", "http://child2");
      child2.put("name", "child2");

      JsonMap collapsed = Utils.parseJsonMap(parent.toString());

      RestPostAction.collapse(collapsed, false, new HashSet(Arrays.asList("child2")), "");

      JsonMap benchmark = Utils.parseJsonMap(parent.toString());
      benchmark = Utils.parseJsonMap(parent.toString());
      benchmark.remove("child2");
      benchmark.put("child2", new JsonMap("href", "http://child2"));

      assertTrue(benchmark.toString().equals(collapsed.toString()));

   }

   @Test
   public void testCollapses2()
   {
      JsonMap parent = new JsonMap();
      parent.put("name", "testing");

      JsonMap child1 = new JsonMap();
      parent.put("child1", child1);
      child1.put("href", "http://child1");
      child1.put("name", "child1");

      JsonArray arrChildren = new JsonArray();
      for (int i = 0; i < 5; i++)
      {
         arrChildren.add(new JsonMap("href", "href://child" + i, "name", "child" + i));
      }

      parent.put("arrChildren", arrChildren);

      JsonMap collapsed = Utils.parseJsonMap(parent.toString());

      RestPostAction.collapse(collapsed, false, new HashSet(Arrays.asList("arrChildren")), "");

      JsonMap benchmark = Utils.parseJsonMap(parent.toString());
      benchmark = Utils.parseJsonMap(parent.toString());
      benchmark.remove("arrChildren");
      arrChildren = new JsonArray();
      for (int i = 0; i < 5; i++)
      {
         arrChildren.add(new JsonMap("href", "href://child" + i));
      }
      benchmark.put("arrChildren", arrChildren);

      assertTrue(benchmark.toString().equals(collapsed.toString()));

   }

   @Test
   public void testCollapses3()
   {
      JsonMap parent = new JsonMap();
      parent.put("name", "testing");

      JsonMap child1 = new JsonMap();
      parent.put("child1", child1);
      child1.put("href", "http://child1");
      child1.put("name", "child1");

      JsonMap child2 = new JsonMap();
      parent.put("child2", child2);
      child2.put("href", "http://child2");
      child2.put("name", "child2");

      JsonMap child3 = new JsonMap();
      child2.put("child3", child3);
      child3.put("href", "http://child3");
      child3.put("name", "child3");

      JsonMap collapsed = Utils.parseJsonMap(parent.toString());

      RestPostAction.collapse(collapsed, false, new HashSet(Arrays.asList("child2.child3")), "");

      JsonMap benchmark = Utils.parseJsonMap(parent.toString());
      benchmark = Utils.parseJsonMap(parent.toString());
      benchmark.getMap("child2").getMap("child3").remove("name");

      assertTrue(benchmark.toString().equals(collapsed.toString()));

   }

}
