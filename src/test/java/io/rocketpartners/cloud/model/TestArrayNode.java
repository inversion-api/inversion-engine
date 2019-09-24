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
      assertTrue(new JsonArray().isEmpty());
      assertFalse(new JsonArray("a", "b").isEmpty());

      JsonArray a = new JsonArray();
      a.add("1");
      assertFalse(a.isEmpty());
   }

   @Test
   public void testClearAndIsEmpty()
   {
      JsonArray a = new JsonArray();
      a.add("1");
      a.clear();
      assertTrue(a.isEmpty());
   }
}
