/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.rql;

import io.inversion.utils.Utils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RqlParserTest {
    void add(List tests, Object... vals) {
        tests.add(Arrays.asList(vals));
    }

    @Test
    public void test1() throws Exception {

        List<List> tests = new ArrayList<>();

        add(tests, "eq(column,String)", "column=String");
        add(tests, "eq(column,string)", "column=    string");
        add(tests, "eq(column,string)", "column=    string   ");
        add(tests, "eq(column,'string')", "column='string'");
        add(tests, "eq(\"column\",string)", "\"column\"=string");
        add(tests, "eq('column','str ing')", "'column'='str ing'");

        add(tests, "eq(column,'str\"ing')", "column='str\"ing'");
        add(tests, "eq(column,\"column\")", "column=\"column\"");

        //      add(tests, "eq('true', 'string')", "eq(true,string)");
        //      add(tests, "eq('123', 'string')", "eq(123,string)");
        //      add(tests, "eq('NULL', 'string')", "eq(null,string)");
        //
        add(tests, "eq(column with spaces,string with spaces)", "eq(column with spaces, string with spaces)");
        add(tests, "eq(a'bc)", "eq(a'bc)");
        add(tests, "func(a,b)", "a=func=b");
        add(tests, "func(a,b)", "func(a  , b   )");
        add(tests, "eq(column,test')", "column=test\\\'");
        add(tests, "eq(column,test\")", "column=test\\\"");
        add(tests, "eq(column,Barb 1\\2 Pnt W 6 Tort)", "eq(column,Barb 1\\\\2 Pnt W 6 Tort)");

        //add(test, "includes=a,b,c", "includes(a,b,c)")

        //      //
        //      add(tests, "eq(\"col'umn\", 'str\"ing')", "eq(\"col'umn\",'str\"ing')");
        //      //
        //      add(tests, "eq(\"column\", 'string')", "eq(\"column\",'string')");
        //      add(tests, "eq('string','string')", "eq('string','string')");
        //      add(tests, "eq(\"column\", \"column\")", "eq(\"column\",\"column\")");
        //      add(tests, "eq('string', \"column\")", "eq('string',\"column\")");
        //      //
        //add(tests, "eq(col,val)", "col=val", new Query().where("col=val"), new Query().where().eq("col", "val").getQuery());
        //      add(tests, "in(\"firstname\",'fred','george','john')", "in(firstname,fred,george,john)");
        //      add(tests, "in(\"firstname\",'fred','george','john')", "in(firstname,fred,george,john)");

        for (int i = tests.size() - 1; i >= 0; i--) {
            List test = tests.get(i);

            String reference = test.get(0).toString();
            for (int j = 1; j < test.size(); j++) {
                Object query = test.get(j).toString();
                if (query instanceof String)
                    query = RqlParser.parse(query.toString()).toString();
                else
                    query = query.toString();

                if (!Utils.testCompare(reference, (String) query))
                    throw new Exception("Test case: " + (i + 1) + "[" + j + "] failed. src = " + reference + " - term = " + query);
            }
        }

        System.out.println("PASSED");
    }

    @Test
    public void test2() throws Exception {
        Term t = RqlParser.parse("w(name,'BANANA KG (RESEAU')");

        assertEquals("BANANA KG (RESEAU", t.getTerm(1).getToken());

        t = RqlParser.parse("w(name,'BANANA KG (RESEAU)')");
        assertEquals("BANANA KG (RESEAU)", t.getTerm(1).getToken());
    }
}
