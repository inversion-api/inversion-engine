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

package io.inversion.rql;

import io.inversion.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class Rql {

    public static Term parse(String paramName, String paramValue) {
        String termStr;
        if (Utils.empty(paramValue) && paramName.contains("(")) {
            termStr = paramName;
        } else {
            if (Utils.empty(paramValue))
                paramValue = "true";

            termStr = "eq(" + paramName + "," + paramValue + ")";
        }
        Term term = parse(termStr);
        return term;
    }

    public static Term parse(String clause) {
        TermStack    tb = new TermStack();
        RqlTokenizer t  = new RqlTokenizer(clause);

        String token;
        while ((token = t.next()) != null) {
            String lc   = token.toLowerCase();
            String func = lc.endsWith("(") ? lc.substring(0, lc.length() - 1) : null;

            if (func != null) {
                tb.push(Term.term(null, func));
            } else if (")".equals(lc)) {
                tb.pop();
            } else if ("=".equals(lc)) {
                Term       top      = tb.top();
                List<Term> children = top.getTerms();

                if ("eq".equalsIgnoreCase(top.getToken()) && children.size() == 2) {
                    top.withToken(children.get(1).getToken());
                    top.removeTerm(children.get(1));
                } else {
                    tb.top().withToken("eq");
                }
            } else {
                tb.top().withTerm(Term.term(null, token));
            }
        }

        Term root = tb.root();

        if ("NULL".equals(root.getToken())) {
            Term child = root.getTerm(0);
            child.withParent(null);
            root = child;
        }

        return root;
    }

    static class TermStack {
        final List<Term> terms = new ArrayList<>();
        Term root = null;

        public Term top() {
            if (terms.size() == 0) {
                if (root == null)
                    root = Term.term(null, null);

                terms.add(root);
            }

            return terms.get(terms.size() - 1);
        }

        public void push(Term term) {
            if (root == null)
                root = term;
            else
                top().withTerm(term);

            terms.add(term);
        }

        public void pop() {
            if (terms.size() > 0)
                terms.remove(terms.size() - 1);
        }

        public Term root() {
            return root;
        }
    }

    static Term func(String function, Object... terms) {
        Term or = new Term(function);
        for (Object term : terms) {
            if (!(term instanceof Term)) {
                String token = term == null ? "null" : term.toString();
                term = new Term(token);
            }
            or.withTerm((Term) term);
        }
        return or;
    }



    /**
     * "Equals" RQL function.
     * Examples:
     * eq(property, value)
     * <br />eq(property, value with spaces)
     * <br />eq(property, 'single, t"ick\'s')
     * <br />eq(property, "dou,bl'e qu\"otes")
     * <br />eq(property, v*lue)
     * <br />eq(property, \*alue)
     * <br />eq(property, valu\*)
     * <br />eq(property, "va, '\"l\*e"
     */
    public static Term eq(Object term1, Object term2) {
        return func("eq", term1, term2);
    }

    /**
     * "Not Equals" RQL function.
     */
    public static Term ne(Object term1, Object term2) {
        return func("ne", term1, term2);
    }

    /**
     * "Greater Than" RQL function.
     * Examples:
     * <li>gt(price, 200)
     */
    public static Term gt(Object term1, Object term2) {
        return func("gt", term1, term2);
    }

    /**
     * "Greater Than or Equal To" RQL function.
     */
    public static Term ge(Object term1, Object term2) {
        return func("ge", term1, term2);
    }


    /**
     * "Less Than" RQL function.
     * Examples:
     * <li>lt(price, 2.99)
     */
    public static Term lt(Object term1, Object term2) {
        return func("lt", term1, term2);
    }



    /**
     * "Less Than or Equal To" RQL function.
     */
    public static Term le(Object term1, Object term2) {
        return func("le", term1, term2);
    }

    /**
     * "With" RQL function.
     */
    public static Term w(Object term1, Object term2) {
        return func("w", term1, term2);
    }

    /**
     * "Starts With" RQL function.
     */
    public static Term sw(Object term1, Object term2) {
        return func("sw", term1, term2);
    }

    /**
     * "Ends With" RQL function.
     */
    public static Term ew(Object term1, Object term2) {
        return func("ew", term1, term2);
    }

    /**
     * "In" RQL function.
     */
    public static Term in(Object... terms) {
        return func("in", terms);
    }

    /**
     * "Out" RQL function.
     */
    public static Term out(Object... terms) {
        return func("out", terms);
    }


    /**
     * "Empty" RQL function.
     */
    public static Term emp(Object term1, Object term2) {
        return func("emp", term1, term2);
    }

    /**
     * "Not Empty" RQL function.
     */
    public static Term nemp(Object term1, Object term2) {
        return func("nemp", term1, term2);
    }


    /**
     * "Null" RQL function.
     */
    public static Term n(Object term) {
        return func("n", term);
    }


    /**
     * "Not Null" RQL function.
     */
    public static Term nn(Object term) {
        return func("nn", term);
    }

    /**
     * "And" RQL function.
     */
    public static Term and(Object... terms) {
        return func("and", terms);
    }

    /**
     * "Or" RQL function.
     */
    public static Term or(Object... terms) {
        return func("or", terms);
    }

    /**
     * "Not" RQL function.
     */
    public static Term not(Object term) {
        return func("not", term);
    }

}
