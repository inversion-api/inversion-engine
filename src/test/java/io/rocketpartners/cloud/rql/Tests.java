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

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import junit.framework.TestSuite;

@RunWith(Suite.class)
@Suite.SuiteClasses({TestTokenizer.class, TestParser.class, TestSqlRql.class, TestQuery.class})
public class Tests extends TestSuite
{

   public static boolean compare(String str1, String str2)
   {
      str1 = str1.replaceAll("\\s+", " ").trim();
      str2 = str2.replaceAll("\\s+", " ").trim();

      if (!str1.equals(str2))
      {
         if (!str1.equals(str2))
         {
            System.out.println("\r\n");
            System.out.println("\r\n");
            System.out.println(str1);
            System.out.println(str2);

            for (int i = 0; i < str1.length() && i < str2.length(); i++)
            {
               if (str1.charAt(i) == str2.charAt(i))
               {
                  System.out.print(" ");
               }
               else
               {
                  System.out.println("X");
                  break;
               }
            }
            System.out.println(" ");

            String err = "failed test: " + str1 + " != " + str2;
            return false;
         }
      }
      return true;
   }
}
