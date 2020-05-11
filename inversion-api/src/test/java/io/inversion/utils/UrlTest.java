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
package io.inversion.utils;

import org.junit.jupiter.api.Test;

import io.inversion.utils.Url;
import io.inversion.utils.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * 
 *
 */
public class UrlTest
{
   @Test
   public void testUrlWithParams()
   {
      assertEquals("http://test.com/api?a=b&c=d", new Url("http://test.com/api").withParams("a", "b", "c", "d").toString());
      assertEquals("http://test.com/api?a=b&c=d&e", new Url("http://test.com/api").withParams("a", "b", "c", "d", "e").toString());
   }

   @Test
   public void test_preserve_param_order()
   {
      String[] tests = new String[]{"http://host.com?zzz=zzz&aaa=aaa&111=111&333=333"};

      for (String test : tests)
      {
         Url url = new Url(test);
         String output = url.toString();

         if (!test.equals(output))
         {
            System.out.println(Utils.parseQueryString(Utils.substringAfter(test, "?")));
            System.out.println(url.getParams());

            System.out.println("EXPECTED: " + test);
            System.out.println("FOUND   : " + output);
            fail();
         }
      }
      //      assertEquals("http://test.com/api?a=b&c=d", new Url("http://test.com/api").withParams("a", "b", "c", "d").toString());
      //      assertEquals("http://test.com/api?a=b&c=d&e", new Url("http://test.com/api").withParams("a", "b", "c", "d", "e").toString());
   }

}
