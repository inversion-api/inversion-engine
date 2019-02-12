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
import java.util.List;

public class Tokenizer
{
   char[]       chars    = null;
   int          head     = 0;

   boolean      escaped  = false;
   char         quote    = 0;
   int          function = 0;

   StringBuffer next     = new StringBuffer();

   public Tokenizer(String chars)
   {
      this.chars = chars.toCharArray();
   }

   public List<String> asList()
   {
      List<String> list = new ArrayList();
      String next = null;
      while ((next = next()) != null)
         list.add(next);

      return list;
   }

   public String next()
   {
      if (head >= chars.length)
      {
         return null;
      }

      while (head < chars.length)
      {
         char c = chars[head];
         head += 1;

         if ((c == ' ' || c == '\t') && next.length() == 0)
            continue; //ignore leading whitespace

         if (isEscaped())
         {
            append(c);
            escaped = false;
            continue;
         }

         if (c == '\\')
         {
            escaped = !escaped;
            continue;
         }

         if (c == '"' || c == '\'')
         {
            append(c);
            if (c == quote)
            {
               //this has to be the closing quote
               quote = 0;
               break;
            }
            else if (next.length() == 1)
            {
               //this has to be the starting quote
               quote = c;
               continue;
            }
            else
            {
               continue;
            }
         }

         if (!inQuotes() && c == ',')
         {
            if (next.length() > 0)
               break;

            continue;
         }

         if (!inQuotes() && c == '(')
         {
            append(c);
            function += 1;
            break;
         }

         if (!inQuotes() && ((!inFunction() && c == '=') || (inFunction() && c == ')')))
         {
            if (next.length() == 0)
            {
               append(c);
               if (c == ')')
                  function -= 1;
            }
            else
            {
               head--;
            }

            break;
         }

         if (!inQuotes() && c == ')' && !inFunction())
            error("Found ')' as the start of a token but no starting '(' was ever parsed: -> '" + new String(chars) + "'");

         append(c);
      }

      if (inQuotes())
         error("Unable to parse unterminated quoted string: \"" + String.valueOf(chars) + "\": -> '" + new String(chars) + "'");

      if (isEscaped())
         error("Unable to parse hanging escape character: \"" + String.valueOf(chars) + "\": -> '" + new String(chars) + "'");

      if (inFunction() && head == chars.length)
         error("Looks like you are missing a closing ')'");

      String str = next.toString().trim();
      next = new StringBuffer();
      return str;
   }

   protected void error(String msg)
   {
      throw new RuntimeException(msg);
   }

   protected boolean inQuotes()
   {
      return quote != 0;
   }

   protected boolean isEscaped()
   {
      return escaped;
   }

   protected boolean inFunction()
   {
      return function > 0;
   }

   protected void append(char c)
   {
      if (!inQuotes() && next.length() == 0 && c == ' ')
         return;
      next.append(c);
   }
}