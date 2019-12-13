package io.inversion.cloud.model;

import junit.framework.TestCase;

public class TestDb extends TestCase
{
   public void testAttributeBeautification()
   {
      Db db = new Db() {};
      
      assertEquals("somecolumn",  db.beautifyAttributeName("SOMECOLUMN"));
      assertEquals("someColumn",  db.beautifyAttributeName("SomeColumn"));
      assertEquals("sOMEcolumn",  db.beautifyAttributeName("SOMEcolumn"));
      assertEquals("someColumn",  db.beautifyAttributeName("SOME_COLUMN"));
      assertEquals("someColumn",  db.beautifyAttributeName("_SOME_COLUMN_"));
      assertEquals("someColumn",  db.beautifyAttributeName("_SOME_  ____COLUMN _ "));
      assertEquals("someColumn",  db.beautifyAttributeName("_some_column_"));
      
      assertEquals("someColumn",  db.beautifyAttributeName(" SOME COLUMN "));
      assertEquals("someColumn",  db.beautifyAttributeName("    SOME    COLUMN   "));
      assertEquals("someColumn",  db.beautifyAttributeName(" some column "));
      assertEquals("someColumn",  db.beautifyAttributeName("        some   column   "));
      
      assertEquals("x23SomeColumn",  db.beautifyAttributeName("123        some   column   "));
      assertEquals("$someColumn",  db.beautifyAttributeName("$some_column"));
      assertEquals("orderDetails",  db.beautifyAttributeName("Order Details"));
      
      
   }
}
