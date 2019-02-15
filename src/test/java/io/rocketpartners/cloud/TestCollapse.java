package io.rocketpartners.cloud;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

import io.rocketpartners.cloud.action.sql.SqlPostAction;
import io.rocketpartners.cloud.utils.JSArray;
import io.rocketpartners.cloud.utils.JSObject;
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
      JSObject parent = new JSObject();
      parent.put("name", "testing");

      JSObject child1 = new JSObject();
      parent.put("child1", child1);
      child1.put("href", "http://child1");
      child1.put("name", "child1");

      JSObject child2 = new JSObject();
      parent.put("child2", child2);

      child2.put("href", "http://child2");
      child2.put("name", "child2");

      JSObject collapsed = Utils.parseJsonObject(parent.toString());

      SqlPostAction.collapse(collapsed, false, new HashSet(Arrays.asList("child2")), "");

      JSObject benchmark = Utils.parseJsonObject(parent.toString());
      benchmark = Utils.parseJsonObject(parent.toString());
      benchmark.remove("child2");
      benchmark.put("child2", new JSObject("href", "http://child2"));

      assertTrue(benchmark.toString().equals(collapsed.toString()));

   }

   @Test
   public void testCollapses2()
   {
      JSObject parent = new JSObject();
      parent.put("name", "testing");

      JSObject child1 = new JSObject();
      parent.put("child1", child1);
      child1.put("href", "http://child1");
      child1.put("name", "child1");

      JSArray arrChildren = new JSArray();
      for (int i = 0; i < 5; i++)
      {
         arrChildren.add(new JSObject("href", "href://child" + i, "name", "child" + i));
      }

      parent.put("arrChildren", arrChildren);

      JSObject collapsed = Utils.parseJsonObject(parent.toString());

      SqlPostAction.collapse(collapsed, false, new HashSet(Arrays.asList("arrChildren")), "");

      JSObject benchmark = Utils.parseJsonObject(parent.toString());
      benchmark = Utils.parseJsonObject(parent.toString());
      benchmark.remove("arrChildren");
      arrChildren = new JSArray();
      for (int i = 0; i < 5; i++)
      {
         arrChildren.add(new JSObject("href", "href://child" + i));
      }
      benchmark.put("arrChildren", arrChildren);

      assertTrue(benchmark.toString().equals(collapsed.toString()));

   }

   @Test
   public void testCollapses3()
   {
      JSObject parent = new JSObject();
      parent.put("name", "testing");

      JSObject child1 = new JSObject();
      parent.put("child1", child1);
      child1.put("href", "http://child1");
      child1.put("name", "child1");

      JSObject child2 = new JSObject();
      parent.put("child2", child2);
      child2.put("href", "http://child2");
      child2.put("name", "child2");

      JSObject child3 = new JSObject();
      child2.put("child3", child3);
      child3.put("href", "http://child3");
      child3.put("name", "child3");

      JSObject collapsed = Utils.parseJsonObject(parent.toString());

      SqlPostAction.collapse(collapsed, false, new HashSet(Arrays.asList("child2.child3")), "");

      JSObject benchmark = Utils.parseJsonObject(parent.toString());
      benchmark = Utils.parseJsonObject(parent.toString());
      benchmark.getObject("child2").getObject("child3").remove("name");

      assertTrue(benchmark.toString().equals(collapsed.toString()));

   }

}
