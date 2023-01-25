/*
 * Copyright (c) 2015-2022 Rocket Partners, LLC
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

package io.inversion.context;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class EscaperTest {


    @Test
    public void escapeUnescape(){
        escapeUnescape("asdf=asdf");
        escapeUnescape("asdf=\\asdf");
        escapeUnescape("asdf,asdf\\");
        escapeUnescape("asdf, asdf");
        escapeUnescape("asdf\r\n, asdf");
    }

    void escapeUnescape(String input){
        String escaped = Escaper.escape(input);
        List<String> list = Escaper.unescape(escaped);
        String unescaped = "";
        for(String str : list)
            unescaped += str;

        System.out.println("------------------------------");
        System.out.println("INPUT  : " + input);
        System.out.println("ESCAPED: " + escaped);
        System.out.println("LIST   : " + list);
        System.out.println("OUTPUT : " + unescaped);
        assertEquals(input, unescaped);
    }


    @Test
    public void escapeTest(){
        String input = "asdf,sdf=sdf\\sfd\r\n";
        String expected = "asdf\\,sdf\\=sdf\\\\sfd\r\n";
        String actual = Escaper.escape(input);
        assertEquals(expected, actual);
    }


    @Test
    public void testUnescape(){
        unescape("asdf", "[asdf]");
        unescape("asdf,asdf", "[asdf, asdf]");
        unescape("asdf, asdf", "[asdf,  asdf]");
        unescape("asdf=asdf", "[asdf, asdf]");
        unescape("asdf = asdf", "[asdf ,  asdf]");
        unescape("asdf\\=asdf", "[asdf=asdf]");
        unescape("asdf\\,asdf", "[asdf,asdf]");
        unescape("asdf\\\\asdf", "[asdf\\asdf]");
        unescape("asdf\\asdf", null);
        unescape("asdf\\,sdf\\=sdf\\\\sfd\\\\r\\\\n", "[asdf,sdf=sdf\\sfd\\r\\n]");
    }

    void unescape(String input, String expected){
        boolean thrown = false;
        String actual = null;
        try {
            actual = Escaper.unescape(input).toString();
            if (!actual.equals(expected)) {
                System.out.println("INPUT   : " + input);
                System.out.println("EXPECTED: " + expected);
                System.out.println("ACTUAL  : " + actual);
            }
        }
        catch(Exception ex){
            System.out.println("ERROR: " + ex.getMessage());
            thrown = true;
        }
        if (expected == null && !thrown){
            fail("Your input '" + input + "' should have thrown a parsing error but did not.");
        }
        else{
            assertEquals(expected, actual);
        }
    }

}
