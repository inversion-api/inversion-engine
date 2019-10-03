/**
 * 
 */
package io.inversion.cloud.model;

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
      assertTrue(new JSArray().isEmpty());
      assertFalse(new JSArray("a", "b").isEmpty());

      JSArray a = new JSArray();
      a.add("1");
      assertFalse(a.isEmpty());
   }

   @Test
   public void testClearAndIsEmpty()
   {
      JSArray a = new JSArray();
      a.add("1");
      a.clear();
      assertTrue(a.isEmpty());
   }
}
