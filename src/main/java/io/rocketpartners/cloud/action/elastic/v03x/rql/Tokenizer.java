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
package io.rocketpartners.cloud.action.elastic.v03x.rql;

public class Tokenizer
{
   char[]       chars = null;
   int          head  = 0;

   StringBuffer next  = new StringBuffer();

   public Tokenizer(String chars)
   {
      this.chars = chars.toCharArray();
   }

   public String next()
   {
      if (head >= chars.length)
         return null;
      boolean escape = false;
      boolean doubleQuote = false;
      boolean singleQuote = false;
      boolean done = false;
      
      String wrap = "";

      for (; head < chars.length && !done; head++)
      {
         char c = chars[head];
         switch (c)
         {
            case '(':
               next.append(c);
               if (escape || doubleQuote || singleQuote)
               {
                  continue;
               }
               done = true;
               break;
            case ',':
            case ' ':
               if (escape || doubleQuote || singleQuote)
               {
                  next.append(c);
                  continue;
               }
               if (next.length() == 0)
               {
                  continue;
               }
               else
               {
                  done = true;
                  break;
               }
            case '=':
            case ')':
               if (escape)
               {
                  next.append(c);
                  continue;
               }

               if (next.length() == 0)
               {
                  next.append(c);
               }
               else
               {
                  head--;
               }
               done = true;
               break;

            case '\"':
               if (escape || singleQuote)
               {
                  next.append(c);
                  continue;
               }

               wrap = "\"";
               doubleQuote = !doubleQuote;

               if (!doubleQuote)
               {
                  done = true;
                  break;
               }
               continue;

            case '\'':
               if (escape || doubleQuote)
               {
                  next.append(c);
                  escape = false;
                  continue;
               }

               wrap = "'";
               singleQuote = !singleQuote;

               if (!singleQuote)
               {
                  done = true;
                  break;
               }
               continue;

            case '\\':
               if (escape)
               {
                  next.append('\\');
               }
               escape = !escape;
               continue;

            default :
               escape = false;
               next.append(c);
         }
      }

      if (doubleQuote || escape)
         throw new RuntimeException("Unable to parse string: \"" + String.valueOf(chars) + "\"");

      String str = wrap + next.toString() + wrap;
      next = new StringBuffer();
      return str;
   }
}