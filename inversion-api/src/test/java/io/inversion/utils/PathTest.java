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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class PathTest {


    void join(String expected, String leftPath, String rightPath, boolean relative){
        Path path =  Path.joinPaths(new Path(leftPath), new Path(rightPath), relative);
        assertEquals(expected, path != null ? path.toString() : null);
    }

    @Test
    public void test_materializeTrivialRegexes() {
        List<Path> paths = Utils.asList(new Path("{var1:car|bus|plane}"), new Path("{var1:books}/{var2:[a-z]*}/{var3:author|category|price}"));
        paths = Path.materializeTrivialRegexes(paths);
        assertEquals("[car, bus, plane, books/var2/author, books/var2/category, books/var2/price]", paths.toString());
    }

    @Test
    public void test_joinPaths(){
        join("servlet/path/api/v1/*", "/servlet/path/*", "api/v1/*", true);
        join("servlet/path/api/v1/[books]/*", "/servlet/path/*", "api/v1/[books]/*", true);
        join("api/v1/[{collection}]/[{resource}]/[{relationship}]", "/api/v1/[{collection}]/[{resource}]/[{relationship}]", "/*", true);
        join("api/v1/*", "/*", "api/v1/*", true);
        join(null, "servlet/[api1]/*", "api2/*", true);

        join("catalog/books/*", "catalog/*", "catalog/books/*", false);

        join("catalog/books/abook", "catalog/[books]/*", "catalog/books/abook", false);
        join(null, "catalog/[cars]/*", "catalog/books/abook", false);

    }


    @Test
    public void getSubPaths(){

        Path path = null;
        List<Path> paths = null;

//        path = new Path("part1/{var1}/part2/[optional1]/{something}/[optional2]/{var2}/*");
//        paths = path.getSubPaths();
//        System.out.println(paths);
//        assertTrue(paths.size() == 3);
//        assertEquals("part1/{var1}/part2", paths.get(0).toString());
//        assertEquals("part1/{var1}/part2/optional1/{something}", paths.get(1).toString());
//        assertEquals("part1/{var1}/part2/optional1/{something}/optional2/{var2}/*", paths.get(2).toString());
//
//
//        paths = new Path("/*").getSubPaths();
//        System.out.println(paths);
//        assertTrue(paths.size() == 1);
//        assertEquals("*", paths.get(0).toString());

        path = new Path("api/endpoint/[collection]/[resource]/[relationship]/*");
        paths = path.getSubPaths();
        System.out.println(paths);
        assertTrue(paths.size() == 4);
        assertEquals("api/endpoint", paths.get(0).toString());
        assertEquals("api/endpoint/collection", paths.get(1).toString());
        assertEquals("api/endpoint/collection/resource", paths.get(2).toString());
        assertEquals("api/endpoint/collection/resource/relationship/*", paths.get(3).toString());




    }

    @Test
    public void add_ignores_duplicate_trailing_wildcard(){
        Path p = new Path("asd/*");
        p.add("*");
        assertEquals("asd/*", p.toString());
    }


    @Test
    public void add_errors_when_wildcard_is_not_the_last_segment(){
        try{
            new Path("asd/*/sdf");
            fail();
        }catch(Exception ex){
            //System.out.println(ex.getMessage());
        }
    }

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

        Path rule = new Path("part1/{part2}/part3/[{varName1:part4}]/[{varName2}]/*");
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

    @Test
    public void mergePaths() {

        merge("[a/*]", "a/*", "a");
        merge("[a/*]", "a", "a/*");

        merge("[a/b]", "a/*", "a/b");
        merge("[a/b]", "a/b", "a/*");
        merge("[a, a/b]", "a", "a/b");

        merge("[a, a/b/c]", "a", "a/b/c");

        merge("[a/b/c]", "a/*", "a/b/c");
        merge("[a/b/c]", "a/b/c", "a/*");

        merge("[a/b/c/*]", "a/*", "a/b/c/*");
        merge("[a/b/c/*]", "a/b/c/*", "a/*");

        merge("[a/b, a/b/c]", "a/b", "a/b/c");
        merge("[a/b]", "a/{asdf}", "a/b");
        merge("[a/b/c]", "a/{asdf}/c", "a/b/c");


        merge("[a/b/c]", "a/b/c", "a/{asdf}/c");

        merge("[a/b/c]", "a/b/c", "a/{asdf}/*");
        merge("[a/b/c/*]", "a/b/c/*", "a/{asdf}/c");


        merge("[1/2/3/4]", "{v1}/{v1}/{v1}/{v1}", "1/{a}/{b}/{c}/", "{a}/2/{b}/{c}/", "{a}/{b}/3/{c}/", "{a}/{b}/{c}/4");
        merge("[1/2/3/4]", "{v1}/{v1}/{v1}/{v1}", "1/*", "{a}/2/*", "{a}/{b}/3/*", "{a}/{b}/{c}/4");


        merge("[1/2/3/4]","{v1}/{v1}/{v1}/{v1}", "1/*", "{a}/2/3/4");

    }

    static void merge(String expected, String... paths) {
        List<Path> list = new ArrayList<>();
        for (String p : paths) {
            list.add(new Path(p));
        }
        List<Path> merged = new ArrayList();
        Path.mergePaths(merged, list);
        System.out.println("MERGING: " + expected + " vs " + merged + " - " + list);
        assertEquals(expected, merged.toString());
    }
}
