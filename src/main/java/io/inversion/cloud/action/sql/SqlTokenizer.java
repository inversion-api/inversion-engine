/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
 * https://github.com/inversion-api
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
package io.inversion.cloud.action.sql;

import java.util.HashSet;
import java.util.Set;

import io.inversion.cloud.utils.Utils;

public class SqlTokenizer
{
   
   public String select = "";
   public String from   = "";
   public String where  = "";
   public String group  = "";
   public String order  = "";
   public String limit  = "";
   
   static Set keywords = new HashSet(Utils.explode(",", "insert,into,update,delete,select,from,where,group,order,limit"));

   char[]       chars       = null;
   int          head        = 0;

   StringBuffer clause      = new StringBuffer("");

   StringBuffer token       = new StringBuffer("");

   boolean      escape      = false;
   boolean      doubleQuote = false;
   boolean      singleQuote = false;
   boolean      backQuote   = false;

   public SqlTokenizer(String chars)
   {
      this.chars = chars.toCharArray();
   }

   boolean quoted()
   {
      return doubleQuote || singleQuote || backQuote;
   }

   boolean escaped()
   {
      return escape;
   }

   boolean isAlphaNum(char c)
   {
      return Character.isAlphabetic(c) || Character.isDigit(c);
   }

   public String nextClause()
   {
      String toReturn = null;

      String nextToken = null;
      while ((nextToken = next()) != null)
      {
         if (keywords.contains(nextToken.toLowerCase()))
         {
            if (clause.length() > 0)
            {
               toReturn = clause.toString();
               clause = new StringBuffer(nextToken);
               return toReturn;
            }
         }
         clause.append(nextToken);
      }

      if (clause.length() > 0)
      {
         toReturn = clause.toString();
         clause = new StringBuffer("");
      }

      return toReturn;
   }

   public String next()
   {
      if (head >= chars.length)
         return null;

      doubleQuote = false;
      singleQuote = false;
      backQuote = false;

      escape = false;

      boolean done = false;
      int parens = 0;

      for (; head < chars.length && !done; head++)
      {
         char c = chars[head];
         switch (c)
         {
            case '\\':
               token.append(c);
               escape = !escape;
               continue;
            case '(':
               if (!escaped() && !quoted())
               {
                  if (parens == 0 && token.length() > 0)
                  {
                     head--;
                     done = true;
                     break;
                  }

                  parens += 1;
               }
               token.append(c);
               continue;
            case ')':
               if (!escaped() && !quoted())
               {
                  parens -= 1;

                  if (parens == 0)
                  {
                     token.append(c);
                     done = true;
                     break;
                  }
               }
               token.append(c);
               continue;
            case '\"':
               if (!(escape || singleQuote || backQuote || parens > 0))
               {
                  if (!doubleQuote && token.length() > 0)
                  {
                     head--;
                     done = true;
                     break;
                  }

                  doubleQuote = !doubleQuote;

                  if (!doubleQuote)
                  {
                     token.append(c);
                     done = true;
                     break;
                  }
               }
               token.append(c);
               continue;

            case '\'':
               if (!(escape || doubleQuote || backQuote || parens > 0))
               {
                  if (!singleQuote && token.length() > 0)
                  {
                     head--;
                     done = true;
                     break;
                  }

                  singleQuote = !singleQuote;

                  if (!singleQuote)
                  {
                     token.append(c);
                     done = true;
                     break;
                  }
               }
               token.append(c);
               continue;

            case '`':
               if (!(escape || doubleQuote || singleQuote || parens > 0))
               {
                  if (!backQuote && token.length() > 0)
                  {
                     head--;
                     done = true;
                     break;
                  }

                  backQuote = !backQuote;

                  if (!backQuote)
                  {
                     token.append(c);
                     done = true;
                     break;
                  }
               }
               token.append(c);
               continue;

            default :
               escape = false;

               if (quoted() || parens > 0)
               {
                  token.append(c);
                  continue;
               }

               if (token.length() > 0)
               {

                  char previousC = token.charAt(token.length() - 1);

                  if (!isAlphaNum(previousC))
                  {
                     head--;
                     done = true;
                     break;
                  }
                  else if (isAlphaNum(previousC) && !isAlphaNum(c))
                  {
                     head--;
                     done = true;
                     break;
                  }
               }

               token.append(c);
         }
      }
      String str = token.toString();
      token = new StringBuffer();
      return str;
   }

}