/**
 * 
 */
package io.rocketpartners.cloud.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @author tc-rocket
 *
 */
public class TestArrayNode
{
   @Test
   public void testIsEmpty()
   {
      assertTrue(new ArrayNode().isEmpty());
      assertFalse(new ArrayNode("a", "b").isEmpty());

      ArrayNode a = new ArrayNode();
      a.add("1");
      assertFalse(a.isEmpty());
   }

   @Test
   public void testClearAndIsEmpty()
   {
      ArrayNode a = new ArrayNode();
      a.add("1");
      a.clear();
      assertTrue(a.isEmpty());
   }
}
