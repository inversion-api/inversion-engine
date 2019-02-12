/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * http://rocketpartners.io
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package io.rocketpartners.cloud.rql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import io.rocketpartners.cloud.rql.Parser;

public class TestParser
{
   void add(List tests, Object... vals)
   {
      tests.add(Arrays.asList(vals));
   }

   @Test
   public void test1() throws Exception
   {
      Parser parser = new Parser();

      List<List> tests = new ArrayList();

      add(tests, "eq(column,String)", "column=String");
      add(tests, "eq(column,string)", "column=    string");
      add(tests, "eq(column,string)", "column=    string   ");
      add(tests, "eq(column,'string')", "column='string'");
      add(tests, "eq(\"column\",string)", "\"column\"=string");
      add(tests, "eq('column','str ing')", "'column'='str ing'");

      add(tests, "eq(column,'str\"ing')", "column='str\"ing'");
      add(tests, "eq(column,\"column\")", "column=\"column\"");

      //      add(tests, "eq('true', 'string')", "eq(true,string)");
      //      add(tests, "eq('123', 'string')", "eq(123,string)");
      //      add(tests, "eq('NULL', 'string')", "eq(null,string)");
      //
      add(tests, "eq(column with spaces,string with spaces)", "eq(column with spaces, string with spaces)");
      add(tests, "eq(a'bc)", "eq(a'bc)");
      add(tests, "func(a,b)", "a=func=b");
      add(tests, "func(a,b)", "func(a  , b   )");

      //add(test, "includes=a,b,c", "includes(a,b,c)")

      //      //
      //      add(tests, "eq(\"col'umn\", 'str\"ing')", "eq(\"col'umn\",'str\"ing')");
      //      //
      //      add(tests, "eq(\"column\", 'string')", "eq(\"column\",'string')");
      //      add(tests, "eq('string','string')", "eq('string','string')");
      //      add(tests, "eq(\"column\", \"column\")", "eq(\"column\",\"column\")");
      //      add(tests, "eq('string', \"column\")", "eq('string',\"column\")");
      //      //
      //add(tests, "eq(col,val)", "col=val", new Query().where("col=val"), new Query().where().eq("col", "val").getQuery());
      //      add(tests, "in(\"firstname\",'fred','george','john')", "in(firstname,fred,george,john)");
      //      add(tests, "in(\"firstname\",'fred','george','john')", "in(firstname,fred,george,john)");

      for (int i = tests.size() - 1; i >= 0; i--)
      {
         List test = tests.get(i);

         String reference = test.get(0).toString();
         for (int j = 1; j < test.size(); j++)
         {
            Object query = test.get(j).toString();
            if (query instanceof String)
               query = parser.parse(query.toString()).toString();
            else
               query = query.toString();

            if (!Tests.compare(reference, (String) query))
               throw new Exception("Test case: " + (i + 1) + "[" + j + "] failed. src = " + reference + " - term = " + query);
         }
      }

      System.out.println("PASSED");
   }
}
