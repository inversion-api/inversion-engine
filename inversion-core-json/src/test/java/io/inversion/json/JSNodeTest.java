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
package io.inversion.json;

import io.inversion.utils.Utils;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class JSNodeTest {

//    @Test
//    public void testStreamingParser(){
//        JSNode objectParsed = JSNode.parseJsonNode(Utils.read(getClass().getResourceAsStream("orders.json")));
//        JSNode streamParsed = JSNode.parseJsonStreaming(Utils.read(getClass().getResourceAsStream("orders.json")));
//
//        String str1 = objectParsed.toString();
//        String str2 = streamParsed.toString();
//        assertEquals(str1, str2);
//    }
//

//    @Test
//    public void test_concurrent_modification_exception(){
//        Map<String, String> map = new HashMap();
//        Utils.addToMap(map, "a", "1", "b", "2", "c", "3");
//
//        boolean thrown = false;
//        try {
//            Set<String> keys = map.keySet();
//            for (String key : keys) {
//                System.out.println(key);
//                map.put("x", "x");
//            }
//        }
//        catch(ConcurrentModificationException cme){
//            //cme.printStackTrace();
//            thrown = true;
//        }
//        if(!thrown)
//            fail();
//    }




    public static void main(String[] ars)throws Exception{
        long start1 = Utils.time();
        List parsed = new ArrayList();
        for(int i=0;i<1000; i++){
            parsed.add(JSParser.parseJson(new BufferedInputStream(JSNodeTest.class.getResourceAsStream("orders.json"))));
        }
        long end1 = Utils.time();
//        long start2 = Utils.time();
//
//        for(int i=0;i<1000; i++){
//            JSNode objectParsed = JSNode.parseJsonNode(json);
//
//            System.out.println(i);
//        }
//        long end2 = Utils.time();
//
//        System.out.println(end1 - start1);
//        System.out.println(end2 - start2);
//        System.out.println((end2-start2) / (end1 - start1));
    }


    @Test
    public void testIdentityRef(){

        JSNode abc1 = new JSMap("abc", "123");
        JSNode abc2 = new JSMap("abc", "123");

        JSNode parent = new JSMap("1", abc1, "2", abc1);
        String str = parent.toString();
        System.out.println(str);

        assertTrue(str.indexOf("$ref")> 0);

        parent = new JSMap("1", abc1, "2", abc2);
        str = parent.toString();
        System.out.println(str);

        assertTrue(str.indexOf("$ref")< 0);
    }


    @Test
    public void test_ref_pointer_nested_arrays(){
        JSNode doc = JSParser.asJSNode(Utils.read(getClass().getResourceAsStream("testJsonPointer1.json")));
        System.out.println(doc);


//        JSList arr1 = new JSList("a0", "a1", "a2");
//        JSList arr1 = new JSList("b0", "b1", "b2");
//        JSNode root = new JSMap("arr1", arr1, "arr2", arr2);
//
//        JSNode abc1 = new JSMap("abc", "123");
//        JSNode abc2 = new JSMap("abc", "123");
//
//        JSNode parent = new JSMap("1", abc1, "2", abc1);
//        String str = parent.toString();
//        System.out.println(str);
//
//        assertTrue(str.indexOf("$ref")> 0);
//
//        parent = new JSMap("1", abc1, "2", abc2);
//        str = parent.toString();
//        System.out.println(str);
//
//        assertTrue(str.indexOf("$ref")< 0);
    }



    @Test
    void fromJsonPath() {
        assertEquals("**.book.[(@_length-1)]", JSFind.fromJsonPath("$..book[(@.length-1)]"));
        assertEquals("**.book.[0,1]", JSFind.fromJsonPath("$..book[0,1]"));
        assertEquals("book.0.asdf", JSFind.fromJsonPath("book[0].asdf"));
        assertEquals(".store.**.price", JSFind.fromJsonPath(".store..price"));
    }

    @Test
    public void testJsonPath1() {
        final JSNode doc = JSParser.asJSNode(Utils.read(getClass().getResourceAsStream("testJsonPath1.json")));
        JSList      found1;
        JSList      found2;

        found1 = doc.findAll("$..book[?(@.author = 'Herman Melville')]");

        assertEquals(1, found1.size());
        assertEquals("Herman Melville", found1.find("0.author"));

        found1 = doc.findAll("$.store..price");
        assertEquals(5, found1.size());

        found2 = doc.findAll("**.store.**.price");
        assertEquals(0, CollectionUtils.disjunction(found1, found2).size());

        found1 = doc.findAll("$..book[2]");
        found2 = doc.findAll("**.book.2");

        assertEquals("Herman Melville", found1.find("0.author"));
        assertEquals(0, CollectionUtils.disjunction(found1, found2).size());

        found1 = doc.findAll("$..*");
        found2 = doc.findAll("**.*");
        assertEquals(0, CollectionUtils.disjunction(found1, found2).size());

        found1 = doc.findAll("$.store.book[*].author");
        found2 = doc.findAll("store.book.*.author");
        assertEquals(0, CollectionUtils.disjunction(found1, found2).size());

        assertEquals(0, CollectionUtils.disjunction(found1, doc.findAll("**.store.book.*.author")).size());

        assertEquals(0, doc.findAll("*.store.book.*.author").size());

        found1 = doc.findAll("$..book[?(@.price = 12.99)]");
        assertEquals(1, found1.size());

        found1 = doc.findAll("$..book[?(@.price >= 12.99)]");
        System.out.println(found1);
        assertEquals(2, found1.size());

        found1 = doc.findAll("$..book[?(@.price!=8.99)]");
        System.out.println(found1);
        assertEquals(3, found1.size());

        found1 = doc.findAll("$..book[?(@.price != 100)]");
        System.out.println(found1);
        assertEquals(4, found1.size());

        found1 = doc.findAll("**.bicycle");
        System.out.println(found1);
        assertEquals(1, found1.size());

        found1 = doc.findAll("**.*.[?(@.price)]");
        System.out.println(found1);
        assertEquals(5, found1.size());

        found1 = doc.findAll("**.*.[?(@.price != 100)]");
        System.out.println(found1);
        assertEquals(5, found1.size());

        found1 = doc.findAll("$..[?(@.price != 100)]");
        System.out.println(found1);
        assertEquals(5, found1.size());

        found1 = doc.findAll(1, "$..[?(@.price != 100)]");
        System.out.println(found1);
        assertEquals(1, found1.size());

        found1 = doc.findAll(3, "$..[?(@.price != 100)]");
        System.out.println(found1);
        assertEquals(3, found1.size());

        found1 = doc.findAll(3, "$..[?(@.price!=100)]");
        System.out.println(found1);
        assertEquals(3, found1.size());

        found1 = doc.findAll(2, "$..book[?(@.isbn)]");
        System.out.println(found1);
        assertEquals(2, found1.size());

        found1 = doc.findAll(-1, "$..[?(@.*.*.isbn)]");
        assertEquals(1, found1.size());

        found1 = doc.findAll(-1, "$..[?(@.price<10)]");
        assertEquals(2, found1.size());

        found1 = doc.findAll(-1, "$..[?(@.*.price<30)]");
        assertEquals(1, found1.size());

        found1 = doc.findAll(-1, "$..[?(@.bicycle.price)]");
        assertEquals(1, found1.size());

        assertEquals("19.95", doc.findString("store.bicycle.price"));
        assertEquals("19.95", doc.findString("*.bicycle.price"));
        assertEquals("red", doc.findString("store.bicycle.color"));
        assertEquals("red", doc.findString("*.bicycle.color"));
        assertEquals("red", doc.findString("*.*.color"));
        assertEquals("red", doc.findString("**.color"));
        assertNull(doc.findString("*.*.*.color"));

        found1 = doc.findAll(-1, "$..[?(@.store.bicycle.price)]");
        assertEquals(1, found1.size());

        found1 = doc.findAll(-1, "$..[?(@.*.*.color)]");
        assertEquals(1, found1.size());
        assertNotNull(found1.getMap(0).get("store"));

        found1 = doc.findAll(-1, "$..[?(@.*.bicycle.price)]");
        assertEquals(1, found1.size());

        found1 = doc.findAll(-1, "$..[?(@.bicycle.price>10)]");
        assertEquals(1, found1.size());

        found1 = doc.findAll(-1, "$..[?(@.store.bicycle.price>10)]");
        assertEquals(1, found1.size());

        found1 = doc.findAll("$.store.book[(@.length-1)]");
        assertEquals("J. R. R. Tolkien", found1.getMap(0).get("author"));

        found1 = doc.findAll("$.store.book[-1:]");
        assertEquals("J. R. R. Tolkien", found1.getMap(0).get("author"));

        found1 = doc.findAll("$.store.book[-2:]");
        assertEquals("Herman Melville", found1.getMap(0).get("author"));

        found1 = doc.findAll("$.store.book[(@.length-2)]");
        assertEquals("Herman Melville", found1.getMap(0).get("author"));

        found1 = doc.findAll("$.store.book[:3]");
        assertEquals(3, found1.size());

        found1 = doc.findAll("$.store.book[1:3]");
        assertEquals(3, found1.size());
        assertEquals("Evelyn Waugh", found1.getMap(0).get("author"));
        assertEquals("Herman Melville", found1.getMap(1).get("author"));
        assertEquals("J. R. R. Tolkien", found1.getMap(2).get("author"));


        //JSList categories = doc.findAll("**.category");
        found1 = doc.findAll("**.book[?(@.category)]");
        //found1 = doc.findAll("$..[?(@.isbn)]");

        System.out.println(found1);

    }



    @Test
    public void testWith() {
        JSList arr  = new JSList(1, 2, 3, 4);
        JSNode  node = new JSMap("name", "value", "name2", "value2", "arr", arr);
        assertEquals("value", node.find("name"));
        assertEquals("value2", node.find("name2"));
        assertEquals(arr, node.find("arr"));
    }

    @Test
    public void find_wildcards() {
        JSList found;
        JSNode  doc = JSParser.asJSNode(Utils.read(getClass().getResourceAsStream("testCollectNodes1.json")));

//        found = doc.findAll("data.*.basket.lineItems.code");
//        assertEquals(0, found.size());

        found = doc.findAll("data.*.basket.lineItems.*.code");
        assertEquals(2, found.size());

        found = doc.findAll("lineItems.*.code");
        assertEquals(0, found.size());

        found = doc.findAll("lineItems.code");
        assertEquals(1, found.size());

        found = doc.findAll("data.*.basket.*");
        assertEquals(3, found.size());

        found = doc.findAll("**.lineItems.*.code");
        assertEquals(3, found.size());

        System.out.println(found);
    }

//    @Test
//    public void diff_1() {
//        JSNode  doc1    = JSParser.asJSNode(Utils.read(getClass().getResourceAsStream("testDiff1.1.json")));
//        JSNode  doc2    = JSParser.asJSNode(Utils.read(getClass().getResourceAsStream("testDiff1.2.json")));
//        JSList patches = doc2.diff(doc1);
//        doc1.patch(patches);
//
//        String str1 = doc1.toString(false);
//        String str2 = doc2.toString(false);
//        Utils.checkSame(str1, str2);
//        assertEquals(str1, str2);
//    }
//
//    /**
//     * This test was developed for an error in diff/patch that could result in the same JSNode
//     * appearing multiple times in the object graph and causing serialization problems.
//     * <p>
//     * The fix was to copy the patches before applying or after computing inside to the JSNode methods
//     */
//    @Test
//    public void diff_3() {
//        JSNode stateDoc = JSParser.asJSNode(Utils.read(getClass().getResourceAsStream("testDiff3.1.json")));
//        JSNode apiEvent = JSParser.asJSNode(Utils.read(getClass().getResourceAsStream("testDiff3.2.json")));
//
//        JSMap meta = stateDoc.getMap("meta");
//        meta.put("events", new JSList(apiEvent));
//
//        JSNode body = apiEvent.getMap("body");
//
//        if (!body.isList()) {
//            body = new JSList(body);
//        }
//
//        JSList patches = body.diff(stateDoc.getNode("data"));
//        if (patches.size() > 0) {
//            apiEvent.put("patches", patches);
//            stateDoc.getNode("data").patch(patches);
//        }
//
//        stateDoc = JSParser.asJSNode(stateDoc.toString());
//
//        assertEquals("028000003647", stateDoc.findString("data.0.basket.lineItems.1.code"));
//        assertEquals("028000003647", stateDoc.findString("meta.events.0.body.basket.lineItems.1.code"));
//
//    }
//
//    @Test
//    public void diff_4() {
//        JSNode array1 = JSParser.asJSNode(Utils.read(getClass().getResourceAsStream("testDiff4.1.json")));
//        JSNode array2 = JSParser.asJSNode(Utils.read(getClass().getResourceAsStream("testDiff4.2.json")));
//
//        JSList patches = array1.diff(array2);
//
//        array2.patch(patches);
//
//    }
//
//    /**
//     * This test was developed to test the patch replace behavior, which caused a replacement of
//     * a value in a json package to be set to null instead of the new value
//     */
//    @Test
//    public void diff_5() {
//        JSNode doc1 = JSParser.asJSNode(Utils.read(getClass().getResourceAsStream("testDiff5.1.json")));
//        JSNode doc2 = JSParser.asJSNode(Utils.read(getClass().getResourceAsStream("testDiff5.2.json")));
//
//        JSList patches = doc2.diff(doc1);
//
//        doc1.patch(patches);
//
//        assertEquals(doc2.toString(), doc1.toString());
//    }
//
//    @Test
//    public void diff_remove_element_from_array() {
//        JSList arr1 = new JSList("one", "two", "three");
//        JSList arr2 = new JSList("one", "two", "three", "four");
//
//        JSList patches = arr1.diff(arr2);
//    }
//
//    @Test
//    public void diff_remove_element_from_end_of_array() {
//        JSNode jsonShort = new JSMap("array", new JSList("a", "b", "c", "d", "e"));
//        JSNode jsonLong  = new JSMap("array", new JSList("a", "b", "c", "d", "e", "f", "g", "h"));
//
//        JSList patches = jsonShort.diff(jsonLong);
//        assertEquals(3, patches.size());
//
//        jsonLong.patch(patches);
//        assertEquals(jsonShort.toString(), jsonLong.toString());
//    }
//
//    @Test
//    public void diff_remove_element_from_start_of_array() {
//        JSNode jsonShort = new JSMap("array", new JSList("e", "f", "g", "h"));
//        JSNode jsonLong  = new JSMap("array", new JSList("a", "b", "c", "d", "e", "f", "g", "h"));
//
//        JSList patches = jsonShort.diff(jsonLong);
//        System.out.println(patches);
//
//        assertEquals(4, patches.size());
//    }
//
//    @Test
//    public void diff_remove_elements_from_middle_of_array() {
//        JSNode jsonShort = new JSMap("array", new JSList("a", "b", "e", "f", "g", "h"));
//        JSNode jsonLong  = new JSMap("array", new JSList("a", "b", "c", "d", "e", "f", "g", "h"));
//
//        JSList patches = jsonShort.diff(jsonLong);
//        System.out.println(patches);
//        assertEquals(2, patches.size());
//
//        jsonLong.patch(patches);
//        assertEquals(jsonShort.toString(), jsonLong.toString());
//    }
//
//    @Test
//    public void diff_remove_elements_from_middle_of_array2() {
//        JSNode jsonShort = new JSMap("array", new JSList("a", "b", "e", "f", "g", "h"));
//        JSNode jsonLong  = new JSMap("array", new JSList("a", "b", "c", "d", "e", "f", "g", "h"));
//
//        JSList patches = jsonShort.diff(jsonLong);
//        System.out.println(patches);
//        assertEquals(2, patches.size());
//
//        jsonLong.patch(patches);
//        assertEquals(jsonShort.toString(), jsonLong.toString());
//    }
//
//    @Test
//    public void diff_remove_elements_from_start_middle_end_of_array() {
//        JSNode jsonShort = new JSMap("array", new JSList("b", "e", "f", "g"));
//        JSNode jsonLong  = new JSMap("array", new JSList("a", "b", "c", "d", "e", "f", "g", "h"));
//
//        JSList patches = jsonShort.diff(jsonLong);
//        System.out.println(patches);
//        assertEquals(4, patches.size());
//
//        jsonLong.patch(patches);
//        assertEquals(jsonShort.toString(), jsonLong.toString());
//    }
//
//    @Test
//    public void diff_add_elements_at_start_of_array() {
//        JSNode jsonShort = new JSMap("array", new JSList("c", "d"));
//        JSNode jsonLong  = new JSMap("array", new JSList("a", "b", "c", "d"));
//
//        JSList patches = jsonLong.diff(jsonShort);
//        System.out.println(patches);
//        assertEquals(2, patches.size());
//
//        jsonShort.patch(patches);
//        assertEquals(jsonShort.toString(), jsonLong.toString());
//    }
//
//    @Test
//    public void diff_array_swap() {
//        JSNode jsonShort = new JSMap("array", new JSList("a"));
//        JSNode jsonLong  = new JSMap("array", new JSList("b"));
//
//        JSList patches = jsonLong.diff(jsonShort);
//        System.out.println(patches);
//        assertEquals(1, patches.size());
//
//        jsonShort.patch(patches);
//        assertEquals(jsonShort.toString(), jsonLong.toString());
//    }

    //   @Test
    //   public void diff_add_element_to_start_of_array2()
    //   {
    //      JSNode jsonShort = new JSMap("array", new JSList("a", new JSMap("B", "b"), "d", "e", "i"));
    //      JSNode jsonShort = new JSMap("array", new JSList("x", "a", new JSMap("B", "b"), "d", "e", "i"));
    //      //      JSNode jsonLong = new JSMap("array", new JSList("x""a", new JSMap("B", "b"), "c", "d", "e", "f", "g", "h", "i", "j", "k"));
    //
    //      JSList patches = jsonShort.diff(jsonLong);
    //      System.out.println(patches);
    //
    //      jsonLong.patch(patches);
    //      assertEquals(jsonShort.toString(), jsonLong.toString());
    //
    //   }

    @Test
    public void find_handles_infinate_loops(){
        JSMap test = new JSMap();
        test.put("test", test);
        JSList found = test.findAll("**.*");
    }

    @Test
    public void testVisit(){
        String expected = "[, /store, /store/book, /store/book/0, /store/book/1, /store/book/2, /store/book/3, /store/bicycle]";
        List found = new ArrayList();
        JSNode parent = JSParser.asJSNode(Utils.read(getClass().getResourceAsStream("testJsonPath1.json")));
        parent.visit(new JSVisitor() {
            public boolean visit(JSPointer path) {
                found.add(path.toString());
                return true;
            }
        });
        assertEquals(expected, found.toString());
    }

    @Test
    public void testVisitWithCircularReference(){
        String expected = "[, /store, /store/book, /store/book/0, /store/book/1, /store/book/2, /store/book/3, /store/bicycle, /store/root]";
        List found = new ArrayList();
        JSNode parent = JSParser.asJSNode(Utils.read(getClass().getResourceAsStream("testJsonPath1.json")));

        parent.getNode("store").put("root", parent);

        parent.visit(new JSVisitor() {
            public boolean visit(JSPointer path) {
                if(found.size() > 1000)
                    fail("a circular reference in the document was not resolved");
                found.add(path.toString());
                return true;
            }
        });

        assertEquals(expected, found.toString());
    }

    @Test
    public void testVisitIterator(){
        String expected = "[, /store, /store/book, /store/book/0, /store/book/1, /store/book/2, /store/book/3, /store/bicycle]";
        List found = new ArrayList();
        JSNode parent = JSParser.asJSNode(Utils.read(getClass().getResourceAsStream("testJsonPath1.json")));

        for(JSPointer pointer : parent.visit())
            found.add(pointer);

        assertEquals(expected, found.toString());
    }

    @Test
    public void testVisitIteratorWithCircularReference(){
        String expected = "[, /store, /store/book, /store/book/0, /store/book/1, /store/book/2, /store/book/3, /store/bicycle, /store/root]";
        JSNode parent = JSParser.asJSNode(Utils.read(getClass().getResourceAsStream("testJsonPath1.json")));

        //--creates a loop
        parent.getNode("store").put("root", parent);

        List found = new ArrayList();
        for(JSPointer pointer : parent.visit()){
            found.add(pointer);
            if(found.size() > 1000)
                fail("a circular reference in the document was not resolved");
        }
        assertEquals(expected, found.toString());
    }



    @Test
    public void test_find_withJsonPointerAndNodeBody() {
        JSNode node = JSParser.asJSNode(Utils.read(getClass().getResourceAsStream("testVisit.json")));
        Object found = node.find("#/data[0]/territories[0]/region");
        assertNotNull(found);
    }
    @Test
    public void test_find_withJsonPointerAndArrayBody() {
        JSNode node = JSParser.asJSNode(Utils.read(getClass().getResourceAsStream("test_find_withJsonPointerAndArrayBody.json")));
        Object found = node.find("#/[0]/territories[0]/region");
        assertNotNull(found);
    }

}
