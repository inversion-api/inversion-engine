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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class UtilsTest {

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
}
