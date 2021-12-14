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

import ioi.inversion.utils.Utils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UtilsTest {

    @Test
    public void testFormat()
    {
        String str = null;
        str = Utils.format("format {}, {}", 1, 2, 3, 4);
        System.out.println(str);
        assertEquals("format 1, 2, 3, 4", str);

        str = Utils.format("format {}, {}, {}", 1, "abc");
        System.out.println(str);
        assertEquals("format %s, %s, %s, {1}, {abc}", str);

        str = Utils.format(null, "asdf", 1, "z");
        System.out.println(str);
        assertEquals("asdf, 1, z", str);


        str = Utils.format("'{}'", "asdf", 1, null, "hello".getBytes(), new boolean[]{true, false, true});
        System.out.println(str);
        assertEquals("'asdf', 1, null, hello, [true, false, true]", str);


        str = Utils.format(null, "asdf");
        System.out.println(str);
        assertEquals("asdf", str);

        str = Utils.format(null, 12345);
        System.out.println(str);
        assertEquals("12345", str);
    }


    @Test
    public void testToDollarAmount1() {
        // test round down
        printAssertEquals("45.00", 45.000001);

        // test round up
        printAssertEquals("76.67", 76.6666666);

        // test a slightly different round down
        printAssertEquals("489.32", 489.321);

        // test a slight different round up
        printAssertEquals("909.49", 909.485000000001);

        // test an even dollarization
        printAssertEquals("10949395.00", 10949395);

        // a zero test
        printAssertEquals("0.00", 0.00001);
    }

    void printAssertEquals(String expected, double orig) {
        System.out.println("Expected : " + expected + ", orig : " + orig + ", result : " + Utils.toDollarAmount(orig));
        assertEquals(expected, Utils.toDollarAmount(orig).toString());
    }

    @Test
    public void containsToken() {
        assertFalse(Utils.containsToken("token", "asdfasdf"));
        assertTrue(Utils.containsToken("token", "token"));
        assertTrue(Utils.containsToken("token", "toKEN"));
        assertTrue(Utils.containsToken("token", "(toKEN)"));
        assertFalse(Utils.containsToken("token", "eq(tokenasdasdf)"));
        assertTrue(Utils.containsToken("token", "eq(token)"));
        assertTrue(Utils.containsToken("INCLUDES", "equals(includes,field1,field2)"));
        assertTrue(Utils.containsToken("includes", "equals(INcluDes,field1,field2)"));
        assertTrue(Utils.containsToken("equals(includes,field1,field2)", "equals(includes,field1,field2)"));
        assertTrue(Utils.containsToken("equals(includes,field1,field2)", "equals(includes,field1,field2), asdasdf"));
    }

    @Test
    public void endsWith() {
        assertTrue(Utils.endsWith("", ""));
        assertTrue(Utils.endsWith("asdf123", "123"));
        assertFalse(Utils.endsWith("asdf123", "456"));
        assertFalse(Utils.endsWith("9294", "92949294"));
        assertTrue(Utils.endsWith("9294", ""));
        assertFalse(Utils.endsWith("http://localhost/northwind", "/"));
        assertTrue(Utils.endsWith("http://localhost/northwind/", "/"));

    }
}
