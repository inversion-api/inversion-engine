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

import org.junit.Test;

import junit.framework.TestCase;

public class TestSimpleTokenizer extends TestCase
{
   @Test
   public void testJsonPathTokenizers1()
   {
      SimpleTokenizer pathTok = new SimpleTokenizer(//
                                                    "['\"", //openQuoteStr
                                                    "]'\"", //closeQuoteStr
                                                    "]", //breakIncludedChars
                                                    ".", //breakExcludedChars
                                                    "", //unquuotedIgnoredChars
                                                    ". \t" //leadingIgoredChars
      );

      assertEquals(pathTok, "asdf.1234.[939.9393]", "[asdf, 1234, [939.9393]]");

      SimpleTokenizer exprTok = new SimpleTokenizer(//
                                                    "'\"", //openQuoteStr
                                                    "'\"", //closeQuoteStr
                                                    "?", //breakIncludedChars
                                                    "] ", //breakExcludedChars
                                                    "[()", //unquuotedIgnoredChars
                                                    ". \t" //leadingIgoredChars
      );

      assertEquals(exprTok, "[?(@.author = 'Herman Melville')]", "[?, @.author, =, 'Herman Melville']");
      
      assertEquals(exprTok, "[?(@_price > 8.99)]", "[?, @_price, >, 8.99]");
      assertEquals(exprTok, "[?(@_price >= 8.99)]", "[?, @_price, >=, 8.99]");


   }

   void assertEquals(SimpleTokenizer tokenizer, String input, String output)
   {
      String tokenized = tokenizer.withChars(input).asList().toString();
      System.out.println(tokenized);
      assertEquals(tokenized, output);
   }
}
