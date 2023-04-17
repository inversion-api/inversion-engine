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
package io.inversion.query;

import io.inversion.rql.RqlTokenizer;
import io.inversion.utils.Utils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RqlTokenizerTest {
    public static void add(List tests, Object... vals) {
        tests.add(Arrays.asList(vals));
    }

    @Test
    public void test1() throws Throwable {
        List<List> tests = new ArrayList<>();

//        add(tests, "f(a, b),f(b,c)", "f(", "a", "b", ")", "f(", "b", "c", ")");
//
//        add(tests, "this is a test", "this is a test");
//        add(tests, "  this is a test    ", "this is a test");
//        add(tests, " ' this is a test    '", "' this is a test    '");
//        add(tests, " ' this \"is a test    '", "' this \"is a test    '");
//        add(tests, " \" this 'is a test    \"", "\" this 'is a test    \"");
//        add(tests, "func(arg, arg, arg with spaces)", "func(", "arg", "arg", "arg with spaces", ")");
//        add(tests, "f(a, b)", "f(", "a", "b", ")");
//        add(tests, "f(,,,,a, , , , b)", "f(", "a", "b", ")");
//        add(tests, "f(a, '   ', b)", "f(", "a", "'   '", "b", ")");
//        add(tests, "f(a, \'   \', b)", "f(", "a", "\'   \'", "b", ")");
//        add(tests, "f(a, \'   \', b)", "f(", "a", "\'   \'", "b", ")");
//        add(tests, "a=b", "a", "=", "b");
//        add(tests, "'a=b'", "'a=b'");
//        add(tests, "\"a=b\"", "\"a=b\"");
//        add(tests, "fun\\(ction(a,b,c)", "fun(ction(", "a", "b", "c", ")");
//        add(tests, "fun\\\"ction(a,b,c)", "fun\"ction(", "a", "b", "c", ")");
//        add(tests, "fun\\\"ction(a,b,c)", "fun\"ction(", "a", "b", "c", ")");
//        add(tests, "\"fun\\\\\"ction(a,b,c)", "\"fun\\\"", "ction(", "a", "b", "c", ")");
//        add(tests, "asdf\\asdf", "asdfasdf");
//        add(tests, "as''asdf", "as''asdf");
//        add(tests, "as''a,sdf", "as''a", "sdf");
//        add(tests, "'ab\\'cde'", "'ab'cde'");
//        add(tests, "\"ab\\'cde\"", "\"ab'cde\"");
//        add(tests, "'ab'cde", "'ab'", "cde");
//        add(tests, "a'b", "a'b");
//        add(tests, "a'\"\"\"''b", "a'\"\"\"''b");
//
//        add(tests, "'a,b,c'", "'a,b,c'");
//        add(tests, "a,b,c", "a", "b", "c");
//        add(tests, "a,b\\,c", "a", "b,c");
//        add(tests, "(abc)", "(", "abc", ")");
//        add(tests, "(abc())", "(", "abc(", ")", ")");
//
//        add(tests, "eq(brandfamily,'LAYS')", "eq(", "brandfamily", "'LAYS'", ")");
//        add(tests, "eq(brandfamily,'LAY\\'S')", "eq(", "brandfamily", "'LAY'S'", ")");
//        add(tests, "eq(brandfamily,'\"LAY\\'S\"')", "eq(", "brandfamily", "'\"LAY'S\"'", ")");


        add(tests, "eq(_query, \"SELECT * FROM A WHERE CONCAT('a', 'b') == 'ab'\")", "eq(", "_query", "\"SELECT * FROM A WHERE CONCAT('a', 'b') == 'ab'\"", ")");

        for (int i = tests.size() - 1; i >= 0; i--) {
            List<String> test    = tests.get(i);
            String       toParse = test.get(0);
            List<String> tokens  = null;

            try {
                tokens = new RqlTokenizer(toParse).asList();

                assertEquals(test.size() - 1, tokens.size(), "Token count wrong: " + (test.size() - 1) + " != " + tokens.size() + "'" + toParse + "' != " + tokens);

                for (int j = 0; j < tokens.size(); j++) {
                    String token = tokens.get(j);
                    String check = test.get(j + 1);
                    assertTrue(Utils.testCompare(check, token));
                }
            } catch (Throwable ex) {
                System.err.println("Error in test " + i + ": '" + toParse + "' != " + tokens);
                throw ex;
            }
        }

        List<String> fails = new ArrayList<>();
        fails.add("'hanging quote");
        fails.add("\"hanging quote");
        fails.add("hanging escape\\");

        fails.add("func(");
        fails.add("func(abc");

        fails.add(")");
        fails.add("asdfasdf)");

        for (int i = fails.size() - 1; i >= 0; i--) {
            boolean failed = false;
            String  test   = fails.get(i);

            try {
                new RqlTokenizer(test).asList();
            } catch (Exception ex) {
                failed = true;
            }
            assertTrue(failed, "String should have thrown a parsing error: " + (i + 1) + " '" + test + "'");
        }

    }

}
