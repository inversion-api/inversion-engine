package io.rocketpartners.cloud.action.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

public class TestSqlTokenizer extends TestCase
{
   public void testTokens() throws Exception
   {
      String[][] tests = new String[][]{//
            {//
                  "SELECT * FROM (SELECT *, 'Threatened' as 'Victim' FROM Entry WHERE threatened = true UNION SELECT *, 'Tortured' as 'Victim' FROM Entry WHERE tortured = true  UNION SELECT *, 'Taken Captive' as 'Victim' FROM Entry WHERE captive = true )as t", //
                  "SELECT| |*| |FROM| |(SELECT *, 'Threatened' as 'Victim' FROM Entry WHERE threatened = true UNION SELECT *, 'Tortured' as 'Victim' FROM Entry WHERE tortured = true  UNION SELECT *, 'Taken Captive' as 'Victim' FROM Entry WHERE captive = true )|as| |t"//
            }//
      };

      for (String[] test : tests)
      {
         String input = test[0];
         List<String> expected = Arrays.asList(test[1].split("\\|"));

         String next = null;
         List<String> actual = new ArrayList();
         SqlTokenizer tok = new SqlTokenizer(input);

         while ((next = tok.next()) != null)
         {
            //System.out.println(next);
            actual.add(next);
         }

         if (!TestSqlQuery.compare(expected.toString(), actual.toString()))
            fail();
      }
   }

   public void testClauses() throws Exception
   {
      String[][] tests = new String[][]{//
            {//
                  "select * from `order details` where \"id\" in (select * from some other table) and    x >= 500", //
                  "select * |from `order details` |where \"id\" in (select * from some other table) and    x >= 500"//
            }, //
            {//
                  "SELECT * FROM (SELECT *, 'Threatened' as 'Victim' FROM Entry WHERE threatened = true UNION SELECT *, 'Tortured' as 'Victim' FROM Entry WHERE tortured = true  UNION SELECT *, 'Taken Captive' as 'Victim' FROM Entry WHERE captive = true )as t", //
                  "SELECT * |FROM (SELECT *, 'Threatened' as 'Victim' FROM Entry WHERE threatened = true UNION SELECT *, 'Tortured' as 'Victim' FROM Entry WHERE tortured = true  UNION SELECT *, 'Taken Captive' as 'Victim' FROM Entry WHERE captive = true )as t"//
            }//
      };

      for (String[] test : tests)
      {
         String input = test[0];
         List<String> expected = Arrays.asList(test[1].split("\\|"));

         String next = null;
         List<String> actual = new ArrayList();
         SqlTokenizer tok = new SqlTokenizer(input);

         while ((next = tok.nextClause()) != null)
         {
            actual.add(next);
         }

         if (!TestSqlQuery.compare(expected.toString(), actual.toString()))
            fail();
      }
   }
}
