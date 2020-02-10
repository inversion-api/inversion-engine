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
package io.inversion.cloud.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A simple tokenizing parser initially designed to facilitate 
 * json path parsing in JSNode.
 * 
 * @author wells
 */
public class SimpleTokenizer
{
   char[]       chars           = null;
   int          head            = 0;

   char         escapeChar      = '\\';
   boolean      escaped         = false;
   boolean      quoted          = false;

   StringBuffer next            = new StringBuffer();

   Set          openQuotes      = null;
   Set          closeQuotes     = null;
   Set          breakIncluded   = null;
   Set          breakExcluded   = null;
   Set          unquotedIgnored = null;
   Set          leadingIgnored  = null;

   public SimpleTokenizer(String openQuoteChars, String closeQuoteChars, String breakIncludedChars, String breakExcludedChars, String unquotedIgnoredChars, String leadingIgnoredChars)
   {
      this(openQuoteChars, closeQuoteChars, breakIncludedChars, breakExcludedChars, unquotedIgnoredChars, leadingIgnoredChars, null);
   }

   public SimpleTokenizer(String openQuoteChars, String closeQuoteChars, String breakIncludedChars, String breakExcludedChars, String unquotedIgnoredChars, String leadingIgnoredChars, String chars)
   {
      openQuotes = toSet(openQuoteChars);
      closeQuotes = toSet(closeQuoteChars);
      breakIncluded = toSet(breakIncludedChars);
      breakExcluded = toSet(breakExcludedChars);
      unquotedIgnored = toSet(unquotedIgnoredChars);
      leadingIgnored = toSet(leadingIgnoredChars);

      withChars(chars);
   }

   /**
    * Resets any ongoing tokenization to tokenize this new string;
    * @param chars
    */
   public SimpleTokenizer withChars(String chars)
   {
      if (chars != null)
      {
         this.chars = chars.toCharArray();
      }
      head = 0;
      next = new StringBuffer();
      escaped = false;
      quoted = false;

      return this;
   }

   public List<String> asList()
   {
      List<String> list = new ArrayList();
      String next = null;
      while ((next = next()) != null)
         list.add(next);

      return list;
   }

   Set toSet(String string)
   {
      Set resultSet = new HashSet();
      for (int i = 0; i < string.length(); i++)
         resultSet.add(new Character(string.charAt(i)));

      return resultSet;
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

         //System.out.println("c = '" + c + "'");

         if (next.length() == 0 && leadingIgnored.contains(c))
            continue;

         if (c == escapeChar)
         {
            if (escaped)
               append(c);

            escaped = !escaped;
            continue;
         }

         if (!quoted && unquotedIgnored.contains(c))
         {
            continue;
         }

         if (!quoted && !escaped && openQuotes.contains(c))
         {
            quoted = true;
         }
         else if (quoted && !escaped && closeQuotes.contains(c))
         {
            quoted = false;
         }

         if (!quoted && breakExcluded.contains(c) && next.length() > 0)
         {
            head--;
            break;
         }

         if (!quoted && breakIncluded.contains(c))
         {
            append(c);
            break;
         }

         append(c);
      }

      if (quoted)
         throw new RuntimeException("Unable to parse unterminated quoted string: \"" + String.valueOf(chars) + "\": -> '" + new String(chars) + "'");

      if (escaped)
         throw new RuntimeException("Unable to parse hanging escape character: \"" + String.valueOf(chars) + "\": -> '" + new String(chars) + "'");

      String str = next.toString().trim();
      next = new StringBuffer();

      if (str.length() == 0)
         str = null;

      return str;
   }

   protected void append(char c)
   {
      next.append(c);
   }

}