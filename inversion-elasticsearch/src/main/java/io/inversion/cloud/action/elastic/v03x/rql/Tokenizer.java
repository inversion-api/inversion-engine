/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.cloud.action.elastic.v03x.rql;

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