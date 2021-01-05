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

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class PathTest {



    @Test
    public void extract_stopsOnWildcard() {
        Map params = new HashMap<>();

        Path rule = new Path("part1/part2/*");
        Path path = new Path("/part1/part2/part3/part4");

        Path matched = rule.extract(params, path);

        assertEquals(0, params.size());
        assertEquals("part1/part2", matched.toString());
        assertEquals("part3/part4", path.toString());
    }

    @Test
    public void extract_stopsOnOptional() {
        Map params = new HashMap<>();

        Path rule = new Path("part1/[part2]/part3/*");
        Path path = new Path("/part1/part2/part3/part4");

        Path matched = rule.extract(params, path);

        assertEquals(0, params.size());
        assertEquals("part1", matched.toString());
        assertEquals("part2/part3/part4", path.toString());
    }

    @Test
    public void extract_stopsOnOptionalWithColonVarParsing() {
        Map<String, String> params = new HashMap<>();

        Path rule = new Path("part1/:part2/part3/[{varName1:part4}]/[:varName2]/*");
        Path path = new Path("part1/val2/part3/part4/part5/part6/part7/part8/part9");

        Path matched = rule.extract(params, path);

        assertEquals(3, params.size());
        assertEquals("val2", params.get("part2"));
        assertEquals("part4", params.get("varName1"));
        assertEquals("part5", params.get("varName2"));

        assertEquals("part1/val2/part3", matched.toString());
        assertEquals("part4/part5/part6/part7/part8/part9", path.toString());
    }

    @Test
    public void extract_stopsOnOptionalWithBracketVarParsing() {
        Map<String, String> params = new HashMap<>();

        Path rule = new Path("part1/{part2}/part3/*");
        Path path = new Path("part1/val2/part3/part4");

        Path matched = rule.extract(params, path);

        assertEquals(1, params.size());
        assertEquals("val2", params.get("part2"));
        assertEquals("part1/val2/part3", matched.toString());
        assertEquals("part4", path.toString());
    }

    @Test
    public void extract_stopsOnOptionalWithRegexVarParsing() {
        Map<String, String> params = new HashMap<>();

        Path rule = new Path("part1/{part2:[0-9a-zA-Z]{1,8}}/part3/*");
        Path path = new Path("part1/val2/part3/part4");

        Path matched = rule.extract(params, path);

        assertEquals(1, params.size());
        assertEquals("val2", params.get("part2"));
        assertEquals("part1/val2/part3", matched.toString());
        assertEquals("part4", path.toString());
    }

    @Test
    public void extract_regexDoesNotMatch_error() {
        boolean error = false;
        try {
            Map<String, String> params = new HashMap<>();
            Path                rule   = new Path("part1/{part2:[0-9a-zA-Z]{1,8}}/part3/*");
            Path                path   = new Path("part1/23452345234523452345/part3/part4");

            Path matched = rule.extract(params, path);
        } catch (Exception e) {
            error = true;
        }
        assertTrue(error, "The test should have errored because 'the regex does not match'");
    }

    @Test
    public void extract_pathDoesNotMatch_error() {
        boolean error = false;
        try {
            Map<String, String> params  = new HashMap<>();
            Path                rule    = new Path("part1/part2/part3/*");
            Path                path    = new Path("part1/part5/part3/part4");
            Path                matched = rule.extract(params, path);
        } catch (Exception e) {
            error = true;
        }
        assertTrue(error, "The test should have errored because 'the paths do not match'");
    }

    @Test
    public void extract_stopsOnOptional2() {
        Map<String, String> params = new HashMap<>();

        Path rule = new Path("part1/{part2}/part3/*");
        Path path = new Path("part1/val2/part3/part4");

        Path matched = rule.extract(params, path);

        assertEquals(1, params.size());
        assertEquals("val2", params.get("part2"));
        assertEquals("part1/val2/part3", matched.toString());
        assertEquals("part4", path.toString());
    }

    @Test
    public void match() {
        assertTrue(new Path("*").matches("/something/asdfas/"));
        assertTrue(new Path("*").matches("something/asdfas/"));
        assertTrue(new Path("something/{collection:books|customers}").matches("something/books"));
        assertTrue(new Path("something/{collection:books|customers}").matches("something/Books"));
        assertTrue(new Path("something/{collection:books|customers}").matches("something/customers"));
        assertFalse(new Path("somsething/{collection:books|customers}").matches("something/blah"));
        assertTrue(new Path("something/{collection:books|customers}/*").matches("something/customers/1234"));
        assertTrue(new Path("something/{collection:books|customers}/{resource:[0-9a-fA-F]{1,8}}").matches("something/customers/11111111"));
        assertTrue(new Path("something/{collection:books|customers}/{resource:[0-9a-fA-F]{1,8}}").matches("something/customers/aaaaaaaa"));
        assertFalse(new Path("something/{collection:books|customers}/{resource:[0-9a-fA-F]{1,8}}").matches("something/customers/aaaaaaaaaa"));
        assertFalse(new Path("something/{collection:books|customers}/{resource:[0-9a-fA-F]{1,8}}").matches("something/customers/1111111111"));
        assertFalse(new Path("something/{collection:books|customers}/{resource:[0-9a-fA-F]{1,8}}").matches("something/customers/zzzzzzzz"));
        assertTrue(new Path("something/{collection:books|customers}/{resource:[0-9]{1,8}}/{relationship:[a-zA-Z]*}").matches("something/customers/1234/orders"));
        assertTrue(new Path("something/{collection:books|customers}/{resource:[0-9]{1,8}}/{relationship:[a-zA-Z]*}").matches("something/customers/1234/orders/"));
        assertTrue(new Path("something/{collection:books|customers}/[{resource:[0-9]{1,8}}]/[{relationship:[a-zA-Z]*}]").matches("something/customers/1234/"));
        assertFalse(new Path("something/{collection:books|customers}/{resource:[0-9]{1,8}}/{relationship:[a-zA-Z]*}").matches("something/customers/1234/"));
        assertTrue(new Path("{collection:players|locations|ads}/[{resource:[0-9]{1,12}}]/{relationship:[a-z]*}").matches("Locations/698/players"));
    }
}
