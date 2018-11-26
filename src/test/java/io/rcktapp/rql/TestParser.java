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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.rcktapp.rql;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class TestParser
{
   public static void main(String[] args) throws Exception
   {
      Rql rql = new Rql("postgres");
      Parser parser = new Parser(rql);
      LinkedHashMap<String, String> preds = new LinkedHashMap();

      preds.put("col=val", "eq(\"col\",'val')");
      preds.put("col=eq=val", "eq(\"col\",'val')");
      preds.put("col=ne=val", "ne(\"col\",'val')");
      preds.put("col=ge=val", "ge(\"col\",'val')");
      preds.put("or(eq(col1,val1),eq(col2,val2))", "or(eq(\"col1\",'val1'),eq(\"col2\",'val2'))");
      //FAILS - preds.put("or(col1=val1, col2=val2)", "or(eq(\"col1\",'val1'),eq(\"col2\",'val2'))");
      //preds.put("func(col,val1,1234,false)", "func(\"col\",'val1',1234,0)");
      preds.put("in(firstname,fred,george,john)", "in(\"firstname\",'fred','george','john')");
      preds.put("firstname=in=fred,george,john", "in(\"firstname\",'fred','george','john')");
      preds.put("eq('string','string')","eq('string','string')"); 
      
      preds.put("eq('string','string')","eq('string','string')");
      

      //

      List<String> keys = new ArrayList(preds.keySet());

      for (int i = keys.size() - 1; i >= 0; i--)
      {
         String src = keys.get(i);
         String value = preds.get(src);

         Predicate pred = parser.parse(src);

         if (!value.equals(pred.toString()))
            throw new Exception("Test case: " + (i + 1) + " failed. src = " + src + " - pred = " + pred.toString());
      }

      System.out.println("PASSED");
   }
}
