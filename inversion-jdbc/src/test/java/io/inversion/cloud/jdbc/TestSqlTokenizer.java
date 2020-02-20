/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.cloud.jdbc;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.inversion.cloud.jdbc.rql.SqlTokenizer;
import io.inversion.cloud.utils.Utils;

public class TestSqlTokenizer
{
   @Test
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

         if (!Utils.testCompare(expected.toString(), actual.toString()))
            fail();
      }
   }

   @Test
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

         if (!Utils.testCompare(expected.toString(), actual.toString()))
            fail();
      }
   }
}
