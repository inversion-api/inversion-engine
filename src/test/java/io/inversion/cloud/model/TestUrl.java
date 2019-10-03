/**
 * 
 */
package io.inversion.cloud.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author tc-rocket
 *
 */
public class TestUrl
{
   @Test
   public void testUrlWithParams()
   {
      assertEquals("http://test.com/api?a=b&c=d", new Url("http://test.com/api").withParams("a", "b", "c", "d").toString());
      assertEquals("http://test.com/api?a=b&c=d&e", new Url("http://test.com/api").withParams("a", "b", "c", "d", "e").toString());
   }
}
