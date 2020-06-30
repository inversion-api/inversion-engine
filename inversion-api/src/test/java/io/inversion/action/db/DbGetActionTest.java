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
package io.inversion.action.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

public class DbGetActionTest {
    @Test
    public void testStripTerms1() {
        //arg format {resultMatchString, inputString, tostrip...)

        String[][] tests = {//
                {//
                        "http://asdf?AAA=BBB&notOffset=123&CCC=DDD", // 
                        "http://asdf?offset=5&AAA=BBB&offset = 5&notOffset=123&eq(offset,22)&CCC=DDD&OFFSET=345", // 
                        "offset" //
                }, //
                {//
                        "http://localhost/northwind/source/orders?limit=5&sort=orderId", // 
                        "http://localhost/northwind/source/orders?limit=5&sort=orderId&pageNum=2", // 
                        "offest", "page", "pageNum" //
                }, //
                {//
                        "http://localhost/northwind/source/orders?sort=orderId&pageNum=2", // 
                        "http://localhost/northwind/source/orders?limit=5&sort=orderId&pageNum=2", // 
                        "limit" //
                }, //
                {//
                        "http://localhost/northwind/source/orders?limit=5&pageNum=2", // 
                        "http://localhost/northwind/source/orders?limit=5&sort=orderId&pageNum=2", // 
                        "sort" //
                }//
        };
        for (String[] test : tests) {
            String[] toStrip = new String[test.length - 2];
            System.arraycopy(test, 2, toStrip, 0, toStrip.length);

            String stripped = DbGetAction.stripTerms(test[1], toStrip);
            //System.out.println(stripped);

            if (!test[0].equals(stripped)) {
                System.out.println("RESULTS DON'T MATCH");
                System.out.println("  EXPECTED: " + test[0]);
                System.out.println("  FOUND   : " + stripped);
                fail();
            }
        }
    }

    //   public void testStripTerms2()
    //   {
    //      String[][] tests = {{"http://asdf?offset=5&AAA=BBB&offset = 5&notOffset=123&eq(offset,22)&CCC=DDD&OFFSET=345", "offset", "http://asdf?AAA=BBB&offset = 5&notOffset=123&CCC=DDD"}};
    //      for (String[] test : tests)
    //      {
    //         String stripped = DbGetAction.stripTerms(test[0], test[1]);
    //         System.out.println(stripped);
    //         assertEquals(test[2], stripped);
    //      }
    //   }
}
