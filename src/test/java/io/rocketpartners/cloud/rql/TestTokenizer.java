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

import io.rocketpartners.cloud.rql.Tokenizer;
import io.rocketpartners.cloud.utils.Utils;
import junit.framework.TestCase;

public class TestTokenizer extends TestCase
{
   public static void add(List tests, Object... vals)
   {
      tests.add(Arrays.asList(vals));
   }

   @Test
   public void test1() throws Throwable
   {
      List<List> tests = new ArrayList();

      add(tests, "this is a test", "this is a test");
      add(tests, "  this is a test    ", "this is a test");
      add(tests, " ' this is a test    '", "' this is a test    '");
      add(tests, " ' this \"is a test    '", "' this \"is a test    '");
      add(tests, " \" this 'is a test    \"", "\" this 'is a test    \"");
      add(tests, "func(arg, arg, arg with spaces)", "func(", "arg", "arg", "arg with spaces", ")");
      add(tests, "f(a, b)", "f(", "a", "b", ")");
      add(tests, "f(,,,,a, , , , b)", "f(", "a", "b", ")");
      add(tests, "f(a, '   ', b)", "f(", "a", "'   '", "b", ")");
      add(tests, "f(a, \'   \', b)", "f(", "a", "\'   \'", "b", ")");
      add(tests, "f(a, \'   \', b)", "f(", "a", "\'   \'", "b", ")");
      add(tests, "a=b", "a", "=", "b");
      add(tests, "'a=b'", "'a=b'");
      add(tests, "\"a=b\"", "\"a=b\"");
      add(tests, "fun\\(ction(a,b,c)", "fun(ction(", "a", "b", "c", ")");
      add(tests, "fun\\\"ction(a,b,c)", "fun\"ction(", "a", "b", "c", ")");
      add(tests, "fun\\\"ction(a,b,c)", "fun\"ction(", "a", "b", "c", ")");
      add(tests, "\"fun\\\\\"ction(a,b,c)", "\"fun\\\"", "ction(", "a", "b", "c", ")");
      add(tests, "asdf\\asdf", "asdfasdf");
      add(tests, "as''asdf", "as''asdf");
      add(tests, "as''a,sdf", "as''a", "sdf");
      add(tests, "'ab\\'cde'", "'ab'cde'");
      add(tests, "\"ab\\'cde\"", "\"ab'cde\"");
      add(tests, "'ab'cde", "'ab'", "cde");
      add(tests, "a'b", "a'b");
      add(tests, "a'\"\"\"''b", "a'\"\"\"''b");

      add(tests, "'a,b,c'", "'a,b,c'");
      add(tests, "a,b,c", "a", "b", "c");
      add(tests, "a,b\\,c", "a", "b,c");
      add(tests, "(abc)", "(", "abc", ")");
      add(tests, "(abc())", "(", "abc(", ")", ")");

      for (int i = tests.size() - 1; i >= 0; i--)
      {
         List<String> test = tests.get(i);
         String toParse = test.get(0).toString();
         List<String> tokens = null;

         try
         {
            tokens = new Tokenizer(toParse).asList();

            assertTrue("Token count wrong: " + (test.size() - 1) + " != " + tokens.size() + "'" + toParse + "' != " + tokens, tokens.size() == test.size() - 1);

            for (int j = 0; j < tokens.size(); j++)
            {
               String token = tokens.get(j);
               String check = test.get(j + 1);
               assertTrue(Utils.testCompare(check, token));
            }
         }
         catch (Throwable ex)
         {
            System.err.println("Error in test " + i + ": '" + toParse + "' != " + tokens);
            throw ex;
         }
      }

      List<String> fails = new ArrayList();
      fails.add("'hanging quote");
      fails.add("\"hanging quote");
      fails.add("hanging escape\\");

      fails.add("func(");
      fails.add("func(abc");

      fails.add(")");
      fails.add("asdfasdf)");

      for (int i = fails.size() - 1; i >= 0; i--)
      {
         boolean failed = false;
         String test = fails.get(i);

         try
         {
            new Tokenizer(test).asList();
         }
         catch (Exception ex)
         {
            failed = true;
         }
         assertTrue("String should have thrown a parsing error: " + (i + 1) + " '" + test + "'", failed);
      }

   }

}
