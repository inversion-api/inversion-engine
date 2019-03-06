package io.rocketpartners.cloud.action.rest;

import junit.framework.TestCase;

public class TestStripTerms extends TestCase
{
   public void testStripTerms()
   {
      String[][] tests = {{"http://asdf?offset=5&AAA=BBB&offset = 5&notOffset=123&eq(offset,22)&CCC=DDD&OFFSET=345", "offset", "http://asdf?AAA=BBB&offset = 5&notOffset=123&CCC=DDD"}};
      for (String[] test : tests)
      {
         String stripped = RestGetAction.stripTerms(test[0], test[1]);
         System.out.println(stripped);
         assertEquals(test[2], stripped);
      }
   }
}
