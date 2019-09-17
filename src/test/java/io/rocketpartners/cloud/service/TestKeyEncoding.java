package io.rocketpartners.cloud.service;

import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import io.rocketpartners.cloud.model.Table;
import junit.framework.TestCase;

public class TestKeyEncoding extends TestCase
{
   @Test
   public void test1() throws Exception
   {
      String string = new String("kdksks*/sl&(2)".getBytes(), "UTF-8");

      System.out.println("string   : '" + string + "'");

      String encoded = Table.encodeStr(string);
      System.out.println("encoded  : '" + encoded + "'");

      String decoded = Table.decodeStr(encoded);
      System.out.println("decoded  : '" + decoded + "'");

      String reincoded = Table.encodeStr(decoded);
      System.out.println("reincoded: '" + reincoded + "'");

      assertEquals(string, decoded);
      assertNotEquals(string, encoded);
   }
}
