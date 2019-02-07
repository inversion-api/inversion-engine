/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
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
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.Test;

public class TestTokenizer
{
   public static void main(String[] args) throws Exception
   {
      new TestTokenizer().test1();
   }

   @Test
   public void test1() throws Exception

   {
      LinkedHashMap<String, List<String>> tokensTests = new LinkedHashMap();

      tokensTests.put("\"string\"", lst("\"string\""));
      tokensTests.put("'string'", lst("'string'"));
      tokensTests.put("'str\"ing'", lst("'str\"ing'"));
      tokensTests.put("'str\\'ing'", lst("'str'ing'"));
      tokensTests.put("\"str\\\"ing\"", lst("\"str\"ing\""));
      tokensTests.put("\"str\"ing", lst("\"str\"", "ing"));

      tokensTests.put("abc,def,'gh i'", lst("abc", "def", "'gh i'"));

      tokensTests.put("eq(col,val)", lst("eq(", "col", "val", ")"));
      tokensTests.put("eq(col,'string with spaces')", lst("eq(", "col", "'string with spaces'", ")"));
      tokensTests.put("eq(col,\"string with spaces\")", lst("eq(", "col", "\"string with spaces\"", ")"));

      tokensTests.put("firstname=in=fred,george,john", lst("firstname", "=", "in", "=", "fred", "george", "john"));

      List<String> keys = new ArrayList(tokensTests.keySet());

      for (int i = keys.size() - 1; i >= 0; i--)
      {
         String src = keys.get(i);
         List<String> tokens = tokensTests.get(src);

         Tokenizer tokenizer = new Tokenizer(src);

         List toked = new ArrayList();
         String tok = null;

         while ((tok = tokenizer.next()) != null)
         {
            toked.add(tok);
         }

         boolean failed = tokens.size() != toked.size();
         for (int j = 0; !failed && j < tokens.size(); j++)
         {
            if (!tokens.get(j).equals(toked.get(j)))
               failed = true;
         }

         if (failed)
            throw new Exception("Test case: " + (i + 1) + " failed. src = " + src + " - tokens = " + toked);
      }

      System.out.println("PASSED");
   }

   public static List<String> lst(String... args)
   {
      return Arrays.asList(args);
   }
}