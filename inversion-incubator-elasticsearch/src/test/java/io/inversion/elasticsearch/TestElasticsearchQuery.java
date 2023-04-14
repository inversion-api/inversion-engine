/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
 * http://rocketpartners.io
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package io.inversion.elasticsearch;

/**
 * @author kfrankic
 */

//FIXME
public class TestElasticsearchQuery {
//    private static Logger log = LoggerFactory.getLogger(TestElasticsearchQuery.class);
//
//    @Test
//    public void gt01() throws Exception {
//        //      {
//        //         "query" : {
//        //           "range" : {
//        //             "testRange" : {
//        //               "from" : "25",
//        //               "to" : null,
//        //               "include_lower" : false,
//        //               "include_upper" : true,
//        //               "boost" : 1.0
//        //             }
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("gt(testRange, 25)");
//
//        JSNode JSNode = query.getJson();
//
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertEquals(2, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        assertNotNull(queryNode.get("range"));
//
//        JSNode rangeNode = queryNode.getNode("range");
//        assertEquals(1, rangeNode.size());
//        assertNotNull(rangeNode.get("testRange"));
//
//        JSNode testRangeNode = rangeNode.getNode("testRange");
//        assertEquals(5, testRangeNode.size());
//        assertEquals("25", testRangeNode.get("from"));
//        assertNull(testRangeNode.get("to"));
//        assertFalse((Boolean) testRangeNode.get("include_lower"));
//        assertTrue((Boolean) testRangeNode.get("include_upper"));
//        assertEquals(new Double(1.0), (Double) testRangeNode.get("boost"));
//
//    }
//
//    //   @Test
//    //   public void gt02() throws Exception
//    //   {
//    //      //      {
//    //      //         "query" : {
//    //      //           "range" : {
//    //      //             "testRange" : {
//    //      //               "from" : "25",
//    //      //               "to" : null,
//    //      //               "include_lower" : false,
//    //      //               "include_upper" : true,
//    //      //               "boost" : 1.0
//    //      //             }
//    //      //           }
//    //      //         },
//    //      //         "sort" : [ {
//    //      //           "id" : {
//    //      //             "order" : "asc"
//    //      //           }
//    //      //         } ]
//    //      //       }
//    //
//    //      ElasticsearchQuery query = new ElasticsearchQuery();
//    //      query.getWhere().gt("testRange", 25);
//    //
//    //      JSNode JSNode = query.toJson();
//    //
//    //      assertNotNull(JSNode, "json should not be empty.");
//    //
//    //      assertEquals(2, JSNode.size());
//    //
//    //      assertNotNull(JSNode.get("sort"));
//    //
//    //      assertNotNull(JSNode.get("query"));
//    //      JSNode queryNode = JSNode.getNode("query");
//    //
//    //      assertNotNull(queryNode.get("range"));
//    //
//    //      JSNode rangeNode = queryNode.getNode("range");
//    //      assertEquals(1, rangeNode.size());
//    //      assertNotNull(rangeNode.get("testRange"));
//    //
//    //      JSNode testRangeNode = rangeNode.getNode("testRange");
//    //      assertEquals(5, testRangeNode.size());
//    //      assertEquals("25", testRangeNode.get("from"));
//    //      assertNull(testRangeNode.get("to"));
//    //      assertFalse((Boolean) testRangeNode.get("include_lower"));
//    //      assertTrue((Boolean) testRangeNode.get("include_upper"));
//    //      assertEquals(new Double(1.0), (Double) testRangeNode.get("boost"));
//    //
//    //   }
//
//    @Test
//    public void lt() throws Exception {
//        //      {
//        //         "query" : {
//        //           "range" : {
//        //             "testRange" : {
//        //               "from" : null,
//        //               "to" : "25",
//        //               "include_lower" : true,
//        //               "include_upper" : false,
//        //               "boost" : 1.0
//        //             }
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("lt(testRange, 25)");
//
//        JSNode JSNode = query.getJson();
//
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertEquals(2, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        assertNotNull(queryNode.get("range"));
//
//        JSNode rangeNode = queryNode.getNode("range");
//        assertEquals(1, rangeNode.size());
//        assertNotNull(rangeNode.get("testRange"));
//
//        JSNode testRangeNode = rangeNode.getNode("testRange");
//        assertEquals(5, testRangeNode.size());
//        assertEquals("25", testRangeNode.get("to"));
//        assertNull(testRangeNode.get("from"));
//        assertTrue((Boolean) testRangeNode.get("include_lower"));
//        assertFalse((Boolean) testRangeNode.get("include_upper"));
//        assertEquals(new Double(1.0), (Double) testRangeNode.get("boost"));
//
//    }
//
//    @Test
//    public void startsWith() {
//        //      {
//        //         "query" : {
//        //           "wildcard" : {
//        //             "city" : {
//        //               "wildcard" : "Chand*",
//        //               "boost" : 1.0
//        //             }
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("sw(city,Chand,Atl)");
//
//        JSNode JSNode = query.getJson();
//
//        assertNotNull(JSNode, "json should not be empty.");
//        assertEquals(2, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        assertNotNull(queryNode.get("wildcard"));
//
//        JSNode wildcardNode = queryNode.getNode("wildcard");
//        assertEquals(1, wildcardNode.size());
//        assertNotNull(wildcardNode.get("city"));
//
//        JSNode cityNode = wildcardNode.getNode("city");
//        assertEquals(2, cityNode.size());
//        assertEquals("Chand*", cityNode.get("wildcard"));
//        assertEquals(new Double(1.0), (Double) cityNode.get("boost"));
//
//    }
//
//    @Test
//    public void with01() {
//        //      {
//        //         "query" : {
//        //           "wildcard" : {
//        //             "city" : {
//        //               "wildcard" : "*andl*",
//        //               "boost" : 1.0
//        //             }
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("w(city,andl)");
//
//        JSNode JSNode = query.getJson();
//
//        assertNotNull(JSNode, "json should not be empty.");
//        assertEquals(2, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        assertNotNull(queryNode.get("wildcard"));
//
//        JSNode wildcardNode = queryNode.getNode("wildcard");
//        assertEquals(1, wildcardNode.size());
//        assertNotNull(wildcardNode.get("city"));
//
//        JSNode cityNode = wildcardNode.getNode("city");
//        assertEquals(2, cityNode.size());
//        assertEquals("*andl*", cityNode.get("wildcard"));
//        assertEquals(new Double(1.0), (Double) cityNode.get("boost"));
//
//    }
//
//    @Test
//    public void with02() {
//        //      {
//        //         "query" : {
//        //           "bool" : {
//        //             "should" : [ {
//        //               "wildcard" : {
//        //                 "name" : {
//        //                   "wildcard" : "*nestl*",
//        //                   "boost" : 1.0
//        //                 }
//        //               }
//        //             }, {
//        //               "wildcard" : {
//        //                 "name" : {
//        //                   "wildcard" : "*f'*",
//        //                   "boost" : 1.0
//        //                 }
//        //               }
//        //             } ],
//        //             "adjust_pure_negative" : true,
//        //             "boost" : 1.0
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("w(name,'nestl','f\\'')");
//
//        JSNode JSNode = query.getJson();
//
//        assertNotNull(JSNode, "json should not be empty.");
//        assertEquals(2, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        assertNotNull(queryNode.get("bool"));
//
//        JSNode boolNode = queryNode.getNode("bool");
//        assertEquals(3, boolNode.size());
//        assertTrue((Boolean) boolNode.get("adjust_pure_negative"));
//        assertEquals(new Double(1.0), (Double) boolNode.get("boost"));
//        assertNotNull(boolNode.get("should"));
//
//        JSList shouldArr = boolNode.getList("should");
//        assertEquals(2, shouldArr.length());
//
//        boolean nestlFound = false;
//        boolean fFound     = false;
//
//        for (JSNode node : shouldArr.asNodeList()) {
//            if (node.hasProperty("wildcard")) {
//                assertEquals(1, node.size());
//                JSNode wildNode = node.getNode("wildcard");
//                JSNode nameNode = wildNode.getNode("name");
//                assertEquals(2, nameNode.size());
//                assertEquals(new Double(1.0), (Double) nameNode.get("boost"));
//                assertTrue(nameNode.hasProperty("wildcard"));
//
//                if (nameNode.getString("wildcard").equals("*nestl*"))
//                    nestlFound = true;
//
//                if (nameNode.getString("wildcard").equals("*f'*"))
//                    fFound = true;
//            }
//        }
//
//        assertTrue(nestlFound);
//        assertTrue(fFound);
//
//    }
//
//    @Test
//    public void without() {
//        //      {
//        //         "query" : {
//        //           "bool" : {
//        //             "must_not" : [ {
//        //               "wildcard" : {
//        //                 "city" : {
//        //                   "wildcard" : "*h*",
//        //                   "boost" : 1.0
//        //                 }
//        //               }
//        //             } ],
//        //             "adjust_pure_negative" : true,
//        //             "boost" : 1.0
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("wo(city,h)");
//
//        JSNode JSNode = query.getJson();
//
//        assertNotNull(JSNode, "json should not be empty.");
//        assertEquals(2, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        assertNotNull(queryNode.get("bool"));
//
//        JSNode boolNode = queryNode.getNode("bool");
//        assertEquals(3, boolNode.size());
//        assertEquals(new Double(1.0), (Double) boolNode.get("boost"));
//        assertTrue((Boolean) boolNode.get("adjust_pure_negative"));
//        assertNotNull(boolNode.get("must_not"));
//
//        JSList mustNotArr = boolNode.getList("must_not");
//        assertEquals(1, mustNotArr.length());
//        JSNode wildcardNode = mustNotArr.getNode(0).getNode("wildcard");
//        assertEquals(1, wildcardNode.size());
//
//        JSNode cityNode = wildcardNode.getNode("city");
//        assertEquals(2, cityNode.size());
//        assertEquals("*h*", cityNode.get("wildcard"));
//        assertEquals(new Double(1.0), (Double) cityNode.get("boost"));
//
//    }
//
//    @Test
//    public void endsWith() {
//        //      {
//        //         "query" : {
//        //           "wildcard" : {
//        //             "city" : {
//        //               "wildcard" : "*andler",
//        //               "boost" : 1.0
//        //             }
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("ew(city,andler)");
//
//        JSNode JSNode = query.getJson();
//
//        assertNotNull(JSNode, "json should not be empty.");
//        assertEquals(2, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        assertNotNull(queryNode.get("wildcard"));
//
//        JSNode wildcardNode = queryNode.getNode("wildcard");
//        assertEquals(1, wildcardNode.size());
//        assertNotNull(wildcardNode.get("city"));
//
//        JSNode cityNode = wildcardNode.getNode("city");
//        assertEquals(2, cityNode.size());
//        assertEquals("*andler", cityNode.get("wildcard"));
//        assertEquals(new Double(1.0), (Double) cityNode.get("boost"));
//
//    }
//
//    @Test
//    public void contains() {
//        //      {
//        //         "query" : {
//        //           "wildcard" : {
//        //             "city" : {
//        //               "wildcard" : "*andl*",
//        //               "boost" : 1.0
//        //             }
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("w(city,andl)");
//
//        JSNode JSNode = query.getJson();
//
//        assertNotNull(JSNode, "json should not be empty.");
//        assertEquals(2, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        assertNotNull(queryNode.get("wildcard"));
//
//        JSNode wildcardNode = queryNode.getNode("wildcard");
//        assertEquals(1, wildcardNode.size());
//        assertNotNull(wildcardNode.get("city"));
//
//        JSNode cityNode = wildcardNode.getNode("city");
//        assertEquals(2, cityNode.size());
//        assertEquals("*andl*", cityNode.get("wildcard"));
//        assertEquals(new Double(1.0), (Double) cityNode.get("boost"));
//
//    }
//
//    @Test
//    public void empty() {
//        //      {
//        //         "query" : {
//        //           "bool" : {
//        //             "should" : [ {
//        //               "term" : {
//        //                 "state" : {
//        //                   "value" : "",
//        //                   "boost" : 1.0
//        //                 }
//        //               }
//        //             }, {
//        //               "bool" : {
//        //                 "must_not" : [ {
//        //                   "exists" : {
//        //                     "field" : "state",
//        //                     "boost" : 1.0
//        //                   }
//        //                 } ],
//        //                 "adjust_pure_negative" : true,
//        //                 "boost" : 1.0
//        //               }
//        //             } ],
//        //             "adjust_pure_negative" : true,
//        //             "boost" : 1.0
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("emp(state)");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//        assertEquals(2, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        assertNotNull(queryNode.get("bool"));
//
//        JSNode boolNode = queryNode.getNode("bool");
//        assertEquals(3, boolNode.size());
//        assertTrue((Boolean) boolNode.get("adjust_pure_negative"));
//        assertEquals(new Double(1.0), (Double) boolNode.get("boost"));
//
//        assertNotNull(boolNode.get("should"));
//        JSList shouldArr = boolNode.getList("should");
//
//        assertEquals(2, shouldArr.length());
//        JSNode termNode      = null;
//        JSNode innerBoolNode = null;
//
//        for (JSNode node : shouldArr.asNodeList()) {
//            if (node.hasProperty("term"))
//                termNode = node.getNode("term");
//            else if (node.hasProperty("bool"))
//                innerBoolNode = node.getNode("bool");
//        }
//
//        assertEquals(1, termNode.size());
//        assertNotNull(termNode.getNode("state"));
//        assertEquals(2, termNode.getNode("state").size());
//        assertEquals("", termNode.getNode("state").getString("value"));
//        assertEquals(new Double(1.0), (Double) termNode.getNode("state").get("boost"));
//
//        assertEquals(3, innerBoolNode.size());
//        assertTrue((Boolean) innerBoolNode.get("adjust_pure_negative"));
//        assertEquals(new Double(1.0), (Double) innerBoolNode.get("boost"));
//
//        JSList mustNotArr = innerBoolNode.getList("must_not");
//        assertEquals(1, mustNotArr.length());
//        JSNode existsNode = mustNotArr.getNode(0).getNode("exists");
//        assertEquals(2, existsNode.size());
//        assertEquals("state", existsNode.getString("field"));
//        assertEquals(new Double(1.0), (Double) existsNode.get("boost"));
//
//    }
//
//    @Test
//    public void fuzzySearch() {
//        //      {
//        //         "query" : {
//        //           "fuzzy" : {
//        //             "keywords" : {
//        //               "value" : "Tim",
//        //               "fuzziness" : "AUTO",
//        //               "prefix_length" : 0,
//        //               "max_expansions" : 50,
//        //               "transpositions" : false,
//        //               "boost" : 1.0
//        //             }
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("search(keywords,Tim)");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        JSNode fuzzyNode = queryNode.getNode("fuzzy");
//        assertEquals(1, fuzzyNode.size());
//
//        JSNode keywordsNode = fuzzyNode.getNode("keywords");
//        assertEquals(6, keywordsNode.size());
//
//        assertEquals("Tim", keywordsNode.getString("value"));
//        assertEquals("AUTO", keywordsNode.getString("fuzziness"));
//        assertEquals(0, keywordsNode.getInt("prefix_length"));
//        assertEquals(50, keywordsNode.getInt("max_expansions"));
//        assertFalse((Boolean) keywordsNode.get("transpositions"));
//        assertEquals(new Double(1.0), (Double) keywordsNode.get("boost"));
//
//    }
//
//    @Test
//    public void notEmpty() {
//        //      {
//        //         "query" : {
//        //           "bool" : {
//        //             "must" : [ {
//        //               "bool" : {
//        //                 "must_not" : [ {
//        //                   "term" : {
//        //                     "state" : {
//        //                       "value" : "",
//        //                       "boost" : 1.0
//        //                     }
//        //                   }
//        //                 } ],
//        //                 "adjust_pure_negative" : true,
//        //                 "boost" : 1.0
//        //               }
//        //             }, {
//        //               "bool" : {
//        //                 "must" : [ {
//        //                   "exists" : {
//        //                     "field" : "state",
//        //                     "boost" : 1.0
//        //                   }
//        //                 } ],
//        //                 "adjust_pure_negative" : true,
//        //                 "boost" : 1.0
//        //               }
//        //             } ],
//        //             "adjust_pure_negative" : true,
//        //             "boost" : 1.0
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("nemp(state)");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//        assertEquals(2, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        assertNotNull(queryNode.get("bool"));
//
//        JSNode boolNode = queryNode.getNode("bool");
//        assertEquals(3, boolNode.size());
//        assertTrue((Boolean) boolNode.get("adjust_pure_negative"));
//        assertEquals(new Double(1.0), (Double) boolNode.get("boost"));
//
//        assertNotNull(boolNode.get("must"));
//        JSList mustArr = boolNode.getList("must");
//
//        assertEquals(2, mustArr.length());
//        JSList mustNotArr   = null;
//        JSList innerMustArr = null;
//
//        for (JSNode node : mustArr.asNodeList()) {
//            JSNode bool = node.getNode("bool");
//            assertEquals(3, bool.size());
//            assertTrue((Boolean) bool.get("adjust_pure_negative"));
//            assertEquals(new Double(1.0), (Double) bool.get("boost"));
//
//            if (bool.hasProperty("must_not"))
//                mustNotArr = bool.getList("must_not");
//            else if (bool.hasProperty("must"))
//                innerMustArr = bool.getList("must");
//        }
//
//        assertEquals(1, mustNotArr.length());
//        JSNode termNode = mustNotArr.getNode(0).getNode("term");
//        assertEquals(1, termNode.size());
//        JSNode stateNode = termNode.getNode("state");
//        assertEquals(2, stateNode.size());
//        assertEquals("", stateNode.getString("value"));
//        assertEquals(new Double(1.0), (Double) stateNode.get("boost"));
//
//        assertEquals(1, innerMustArr.length());
//        JSNode existsNode = innerMustArr.getNode(0).getNode("exists");
//        assertEquals(2, existsNode.size());
//        assertEquals("state", existsNode.getString("field"));
//        assertEquals(new Double(1.0), (Double) existsNode.get("boost"));
//
//    }
//
//    @Test
//    public void notNull() {
//        //      {
//        //         "query" : {
//        //           "exists" : {
//        //             "field" : "state",
//        //             "boost" : 1.0
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("nn(state)");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//        assertEquals(2, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        JSNode existsNode = queryNode.getNode("exists");
//        assertEquals(2, existsNode.size());
//        assertEquals("state", existsNode.getString("field"));
//        assertEquals(new Double(1.0), (Double) existsNode.get("boost"));
//
//    }
//
//    @Test
//    public void isNull() {
//        //      {
//        //         "query" : {
//        //           "bool" : {
//        //             "must_not" : [ {
//        //               "exists" : {
//        //                 "field" : "state",
//        //                 "boost" : 1.0
//        //               }
//        //             } ],
//        //             "adjust_pure_negative" : true,
//        //             "boost" : 1.0
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("n(state)");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//        assertEquals(2, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        assertNotNull(queryNode.get("bool"));
//
//        JSNode boolNode = queryNode.getNode("bool");
//        assertEquals(3, boolNode.size());
//        assertTrue((Boolean) boolNode.get("adjust_pure_negative"));
//        assertEquals(new Double(1.0), (Double) boolNode.get("boost"));
//
//        assertNotNull(boolNode.get("must_not"));
//        JSList mustNotArr = boolNode.getList("must_not");
//
//        assertEquals(1, mustNotArr.length());
//        JSNode existsNode = mustNotArr.getNode(0).getNode("exists");
//        assertEquals(2, existsNode.size());
//        assertEquals("state", existsNode.getString("field"));
//        assertEquals(new Double(1.0), (Double) existsNode.get("boost"));
//
//    }
//
//    @Test
//    public void pageSize() {
//        //      {
//        //         "size" : 1000,
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("pageSize=1000");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertEquals(2, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertEquals(1000, JSNode.getInt("size"));
//
//    }
//
//    @Test
//    public void simplePaging() {
//        //      {
//        //         "from" : 2,
//        //         "size" : 100,
//        //         "query" : {
//        //           "range" : {
//        //             "testRange" : {
//        //               "from" : "25",
//        //               "to" : null,
//        //               "include_lower" : false,
//        //               "include_upper" : true,
//        //               "boost" : 1.0
//        //             }
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("gt(testRange, 25)&page=2");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertEquals(4, JSNode.size());
//        assertEquals(100, JSNode.getInt("size"));
//        assertEquals(2, JSNode.getInt("from"));
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        assertNotNull(queryNode.get("range"));
//
//        JSNode rangeNode = queryNode.getNode("range");
//        assertEquals(1, rangeNode.size());
//        assertNotNull(rangeNode.get("testRange"));
//
//        JSNode testRangeNode = rangeNode.getNode("testRange");
//        assertEquals(5, testRangeNode.size());
//        assertNull(testRangeNode.get("to"));
//        assertEquals("25", testRangeNode.get("from"));
//        assertFalse((Boolean) testRangeNode.get("include_lower"));
//        assertTrue((Boolean) testRangeNode.get("include_upper"));
//        assertEquals(new Double(1.0), (Double) testRangeNode.get("boost"));
//
//    }
//
////   @Test
////   public void complexPaging()
////   {
////
////      ElasticsearchQuery query = new ElasticsearchQuery();
////      query.withTerm("gt(testRange, 25)&pagesize=100&start=2000");
////
////      JSNode JSNode = query.toJson();
////      assertNotNull(JSNode, "json should not be empty.");
////
//    //      assertNotNull(JSNode.get("sort"));
////
//    //      assertNotNull(JSNode.get("query"));
////      JSNode queryNode = JSNode.getNode("query");
////
////      fail("doom doom doooooom.");
////
////   }
//
//    @Test
//    public void simpleSortAsc1() {
//        //      {
//        //         "sort" : [ {
//        //           "test" : {
//        //             "order" : "asc"
//        //           }
//        //         }, {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("order=test");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertEquals(1, JSNode.size());
//        assertNotNull(JSNode.get("sort"));
//
//        JSList sortArr = JSNode.getList("sort");
//        assertEquals(2, sortArr.size());
//
//        boolean sort1Found = false;
//        boolean sort2Found = false;
//
//        for (JSNode node : sortArr.asNodeList()) {
//            assertEquals(1, node.size());
//
//            if (node.hasProperty("test")) {
//                assertEquals(1, node.getNode("test").size());
//                assertEquals("asc", node.getNode("test").getString("order"));
//                sort1Found = true;
//            } else if (node.hasProperty("id")) {
//                assertEquals(1, node.getNode("id").size());
//                assertEquals("asc", node.getNode("id").getString("order"));
//                sort2Found = true;
//            }
//        }
//
//        assertTrue(sort1Found);
//        assertTrue(sort2Found);
//
//    }
//
//    @Test
//    public void simpleSortAsc2() {
//        //      {
//        //         "sort" : [ {
//        //           "test" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("order=+test");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertEquals(1, JSNode.size());
//        assertNotNull(JSNode.get("sort"));
//
//        JSList sortArr = JSNode.getList("sort");
//        assertEquals(2, sortArr.size());
//
//        boolean sort1Found = false;
//        boolean sort2Found = false;
//
//        for (JSNode node : sortArr.asNodeList()) {
//            assertEquals(1, node.size());
//
//            if (node.hasProperty("test")) {
//                assertEquals(1, node.getNode("test").size());
//                assertEquals("asc", node.getNode("test").getString("order"));
//                sort1Found = true;
//            } else if (node.hasProperty("id")) {
//                assertEquals(1, node.getNode("id").size());
//                assertEquals("asc", node.getNode("id").getString("order"));
//                sort2Found = true;
//            }
//        }
//
//        assertTrue(sort1Found);
//        assertTrue(sort2Found);
//
//    }
//
//    @Test
//    public void simpleSortDesc() {
//        //      {
//        //         "sort" : [ {
//        //           "test" : {
//        //             "order" : "desc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("order=-test");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertEquals(1, JSNode.size());
//        assertNotNull(JSNode.get("sort"));
//
//        JSList sortArr = JSNode.getList("sort");
//        assertEquals(2, sortArr.size());
//
//        boolean sort1Found = false;
//        boolean sort2Found = false;
//
//        for (JSNode node : sortArr.asNodeList()) {
//            assertEquals(1, node.size());
//
//            if (node.hasProperty("test")) {
//                assertEquals(1, node.getNode("test").size());
//                assertEquals("desc", node.getNode("test").getString("order"));
//                sort1Found = true;
//            } else if (node.hasProperty("id")) {
//                assertEquals(1, node.getNode("id").size());
//                assertEquals("asc", node.getNode("id").getString("order"));
//                sort2Found = true;
//            }
//        }
//
//        assertTrue(sort1Found);
//        assertTrue(sort2Found);
//
//    }
//
//    @Test
//    public void multiSort() {
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("sort=test,-test2,+test3");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertEquals(1, JSNode.size());
//        assertNotNull(JSNode.get("sort"));
//
//        JSList sortArr = JSNode.getList("sort");
//        assertEquals(4, sortArr.size());
//
//        boolean testFound  = false;
//        boolean test2Found = false;
//        boolean test3Found = false;
//        boolean test4Found = false;
//
//        for (JSNode node : sortArr.asNodeList()) {
//            assertEquals(1, node.size());
//
//            if (node.hasProperty("test")) {
//                testFound = true;
//                assertEquals(1, node.getNode("test").size());
//                assertEquals("asc", node.getNode("test").getString("order"));
//            } else if (node.hasProperty("test2")) {
//                test2Found = true;
//                assertEquals(1, node.getNode("test2").size());
//                assertEquals("desc", node.getNode("test2").getString("order"));
//            } else if (node.hasProperty("test3")) {
//                test3Found = true;
//                assertEquals(1, node.getNode("test3").size());
//                assertEquals("asc", node.getNode("test3").getString("order"));
//            } else if (node.hasProperty("id")) {
//                assertEquals(1, node.getNode("id").size());
//                assertEquals("asc", node.getNode("id").getString("order"));
//                test4Found = true;
//            }
//
//        }
//
//        assertTrue(testFound);
//        assertTrue(test2Found);
//        assertTrue(test3Found);
//        assertTrue(test4Found);
//
//    }
//
//    @Test
//    public void simpleSortAndSize() {
//        //      {
//        //         "size" : 1000,
//        //         "sort" : [ {
//        //           "test" : {
//        //             "order" : "asc"
//        //           }
//        //         }, {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("order=test&pageSize=1000");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertEquals(2, JSNode.size());
//        assertEquals(1000, JSNode.getInt("size"));
//        assertNotNull(JSNode.get("sort"));
//
//        JSList sortArr = JSNode.getList("sort");
//        assertEquals(2, sortArr.size());
//
//        boolean sort1Found = false;
//        boolean sort2Found = false;
//
//        for (JSNode node : sortArr.asNodeList()) {
//            assertEquals(1, node.size());
//
//            if (node.hasProperty("test")) {
//                assertEquals(1, node.getNode("test").size());
//                assertEquals("asc", node.getNode("test").getString("order"));
//                sort1Found = true;
//            } else if (node.hasProperty("id")) {
//                assertEquals(1, node.getNode("id").size());
//                assertEquals("asc", node.getNode("id").getString("order"));
//                sort2Found = true;
//            }
//        }
//
//        assertTrue(sort1Found);
//        assertTrue(sort2Found);
//
//    }
//
//    @Test
//    public void simpleSource() {
//        //      {
//        //         "query" : {
//        //           "range" : {
//        //             "priority" : {
//        //               "from" : "25",
//        //               "to" : null,
//        //               "include_lower" : false,
//        //               "include_upper" : true,
//        //               "boost" : 1.0
//        //             }
//        //           }
//        //         },
//        //         "_source" : {
//        //           "includes" : [ "priority" ],
//        //           "excludes" : [ ]
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("gt(priority,25)&source=priority");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertEquals(3, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//        assertEquals(1, queryNode.size());
//        JSNode rangeNode = queryNode.getNode("range");
//        assertEquals(1, rangeNode.size());
//        JSNode priorityNode = rangeNode.getNode("priority");
//        assertEquals(5, priorityNode.size());
//        assertEquals("25", priorityNode.get("from"));
//        assertNull(priorityNode.get("to"));
//        assertFalse((Boolean) priorityNode.get("include_lower"));
//        assertTrue((Boolean) priorityNode.get("include_upper"));
//        assertEquals(new Double(1.0), (Double) priorityNode.get("boost"));
//
//        assertNotNull(JSNode.get("_source"));
//        JSNode sourceNode = JSNode.getNode("_source");
//        assertEquals(2, sourceNode.size());
//        JSList includes = sourceNode.getList("includes");
//        assertEquals(1, includes.size());
//        assertEquals("priority", includes.getString(0));
//
//        JSList excludes = sourceNode.getList("excludes");
//        assertEquals(0, excludes.size());
//
//    }
//
//    @Test
//    public void simpleSourceIncludes() {
//        //      {
//        //         "query" : {
//        //           "range" : {
//        //             "priority" : {
//        //               "from" : "25",
//        //               "to" : null,
//        //               "include_lower" : false,
//        //               "include_upper" : true,
//        //               "boost" : 1.0
//        //             }
//        //           }
//        //         },
//        //         "_source" : {
//        //           "includes" : [ "priority", "test2", "banana" ],
//        //           "excludes" : [ ]
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("gt(priority,25)&includes=priority,test2,banana");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertEquals(3, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//        assertEquals(1, queryNode.size());
//        JSNode rangeNode = queryNode.getNode("range");
//        assertEquals(1, rangeNode.size());
//        JSNode priorityNode = rangeNode.getNode("priority");
//        assertEquals(5, priorityNode.size());
//        assertEquals("25", priorityNode.get("from"));
//        assertNull(priorityNode.get("to"));
//        assertFalse((Boolean) priorityNode.get("include_lower"));
//        assertTrue((Boolean) priorityNode.get("include_upper"));
//        assertEquals(new Double(1.0), (Double) priorityNode.get("boost"));
//
//        assertNotNull(JSNode.get("_source"));
//        JSNode sourceNode = JSNode.getNode("_source");
//        assertEquals(2, sourceNode.size());
//        JSList includes = sourceNode.getList("includes");
//        assertEquals(3, includes.size());
//        assertTrue(includes.contains("priority"));
//        assertTrue(includes.contains("test2"));
//        assertTrue(includes.contains("banana"));
//
//        JSList excludes = sourceNode.getList("excludes");
//        assertEquals(0, excludes.size());
//
//    }
//
//    @Test
//    public void simpleSourceExcludes() {
//        //      {
//        //         "query" : {
//        //           "range" : {
//        //             "priority" : {
//        //               "from" : "25",
//        //               "to" : null,
//        //               "include_lower" : false,
//        //               "include_upper" : true,
//        //               "boost" : 1.0
//        //             }
//        //           }
//        //         },
//        //         "_source" : {
//        //           "includes" : [ ],
//        //           "excludes" : [ "priority" ]
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("gt(priority,25)&excludes=priority");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertEquals(3, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//        assertEquals(1, queryNode.size());
//        JSNode rangeNode = queryNode.getNode("range");
//        assertEquals(1, rangeNode.size());
//        JSNode priorityNode = rangeNode.getNode("priority");
//        assertEquals(5, priorityNode.size());
//        assertEquals("25", priorityNode.get("from"));
//        assertNull(priorityNode.get("to"));
//        assertFalse((Boolean) priorityNode.get("include_lower"));
//        assertTrue((Boolean) priorityNode.get("include_upper"));
//        assertEquals(new Double(1.0), (Double) priorityNode.get("boost"));
//
//        assertNotNull(JSNode.get("_source"));
//        JSNode sourceNode = JSNode.getNode("_source");
//        assertEquals(2, sourceNode.size());
//        JSList excludes = sourceNode.getList("excludes");
//        assertEquals(1, excludes.size());
//        assertEquals("priority", excludes.getString(0));
//
//        JSList includes = sourceNode.getList("includes");
//        assertEquals(0, includes.size());
//
//    }
//
//    @Test
//    public void testGt() {
//        //      {
//        //         "query" : {
//        //           "range" : {
//        //             "hispanicRank" : {
//        //               "from" : "25",
//        //               "to" : null,
//        //               "include_lower" : false,
//        //               "include_upper" : true,
//        //               "boost" : 1.0
//        //             }
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("gt(hispanicRank,25)");
//
//        JSNode JSNode = query.getJson();
//
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertEquals(2, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        assertNotNull(queryNode.get("range"));
//
//        JSNode rangeNode = queryNode.getNode("range");
//        assertEquals(1, rangeNode.size());
//        assertNotNull(rangeNode.get("hispanicRank"));
//
//        JSNode testRangeNode = rangeNode.getNode("hispanicRank");
//        assertEquals(5, testRangeNode.size());
//        assertEquals("25", testRangeNode.get("from"));
//        assertNull(testRangeNode.get("to"));
//        assertFalse((Boolean) testRangeNode.get("include_lower"));
//        assertTrue((Boolean) testRangeNode.get("include_upper"));
//        assertEquals(new Double(1.0), (Double) testRangeNode.get("boost"));
//
//    }
//
//    @Test
//    public void testGte() {
//        //      {
//        //         "query" : {
//        //           "range" : {
//        //             "hispanicRank" : {
//        //               "from" : "25",
//        //               "to" : null,
//        //               "include_lower" : true,
//        //               "include_upper" : true,
//        //               "boost" : 1.0
//        //             }
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("ge(hispanicRank,25)");
//
//        JSNode JSNode = query.getJson();
//
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertNotNull(JSNode.get("sort"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        assertEquals(1, queryNode.size());
//        assertNotNull(queryNode.get("range"));
//
//        JSNode rangeNode = queryNode.getNode("range");
//        assertEquals(1, rangeNode.size());
//        assertNotNull(rangeNode.get("hispanicRank"));
//
//        JSNode testRangeNode = rangeNode.getNode("hispanicRank");
//        assertEquals(5, testRangeNode.size());
//        assertEquals("25", testRangeNode.get("from"));
//        assertNull(testRangeNode.get("to"));
//        assertTrue((Boolean) testRangeNode.get("include_lower"));
//        assertTrue((Boolean) testRangeNode.get("include_upper"));
//        assertEquals(new Double(1.0), (Double) testRangeNode.get("boost"));
//
//    }
//
//    @Test
//    public void testNestedLt() {
//        //      {
//        //         "query" : {
//        //           "nested" : {
//        //             "query" : {
//        //               "range" : {
//        //                 "players.registerNum" : {
//        //                   "from" : null,
//        //                   "to" : "3",
//        //                   "include_lower" : true,
//        //                   "include_upper" : false,
//        //                   "boost" : 1.0
//        //                 }
//        //               }
//        //             },
//        //             "path" : "players",
//        //             "ignore_unmapped" : false,
//        //             "score_mode" : "avg",
//        //             "boost" : 1.0
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("lt(players.registerNum,3)");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertEquals(2, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//        assertNotNull(queryNode.get("nested"));
//
//        JSNode nestedNode = queryNode.getNode("nested");
//        assertEquals(5, nestedNode.size());
//        assertEquals("players", nestedNode.get("path"));
//        assertFalse((Boolean) nestedNode.get("ignore_unmapped"));
//        assertEquals("avg", nestedNode.get("score_mode"));
//        assertEquals(new Double(1.0), (Double) nestedNode.get("boost"));
//
//        JSNode innerQueryNode = nestedNode.getNode("query");
//        assertEquals(1, innerQueryNode.size());
//
//        JSNode rangeNode = innerQueryNode.getNode("range");
//        assertEquals(1, rangeNode.size());
//
//        JSNode registerNode = rangeNode.getNode("players.registerNum");
//        assertEquals(5, registerNode.size());
//        assertEquals("3", registerNode.get("to"));
//        assertNull(registerNode.get("from"));
//        assertTrue((Boolean) registerNode.get("include_lower"));
//        assertFalse((Boolean) registerNode.get("include_upper"));
//        assertEquals(new Double(1.0), (Double) registerNode.get("boost"));
//
//    }
//
//    @Test
//    public void testEq1() {
//        //      {
//        //         "term" : {
//        //           "city" : {
//        //             "value" : "CHANDLER",
//        //             "boost" : 1.0
//        //           }
//        //         }
//        //       }
//
//        //      {
//        //         "query" : {
//        //           "term" : {
//        //             "city" : {
//        //               "value" : "CHANDLER",
//        //               "boost" : 1.0
//        //             }
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("eq(city,CHANDLER)");
//
//        JSNode JSNode = query.getJson();
//
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertEquals(2, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        JSNode termNode = queryNode.getNode("term");
//        assertEquals(1, termNode.size());
//        JSNode cityNode = termNode.getNode("city");
//        assertEquals(2, cityNode.size());
//        assertEquals("CHANDLER", cityNode.getString("value"));
//        assertEquals(new Double(1.0), (Double) cityNode.get("boost"));
//
//    }
//
//    @Test
//    public void testEq2() {
//        //      {
//        //         "query" : {
//        //           "term" : {
//        //             "uninstalled" : {
//        //               "value" : "true",
//        //               "boost" : 1.0
//        //             }
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("eq(uninstalled,true)");
//
//        JSNode JSNode = query.getJson();
//
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertNotNull(JSNode.get("sort"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        JSNode termNode = queryNode.getNode("term");
//        assertEquals(1, termNode.size());
//        JSNode cityNode = termNode.getNode("uninstalled");
//        assertEquals(2, cityNode.size());
//        assertEquals("true", cityNode.getString("value"));
//        assertEquals(new Double(1.0), (Double) cityNode.get("boost"));
//
//    }
//
//    @Test
//    public void testNotEq() {
//        //      {
//        //         "query" : {
//        //           "bool" : {
//        //             "must_not" : [ {
//        //               "term" : {
//        //                 "hispanicRank" : {
//        //                   "value" : "25",
//        //                   "boost" : 1.0
//        //                 }
//        //               }
//        //             } ],
//        //             "adjust_pure_negative" : true,
//        //             "boost" : 1.0
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("ne(hispanicRank,25)");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//        assertEquals(2, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        assertNotNull(queryNode.get("bool"));
//
//        JSNode boolNode = queryNode.getNode("bool");
//        assertEquals(3, boolNode.size());
//        assertTrue((Boolean) boolNode.get("adjust_pure_negative"));
//        assertEquals(new Double(1.0), (Double) boolNode.get("boost"));
//
//        assertNotNull(boolNode.get("must_not"));
//        JSList mustNotArr = boolNode.getList("must_not");
//
//        assertEquals(1, mustNotArr.length());
//        JSNode termNode = mustNotArr.getNode(0).getNode("term");
//        assertEquals(1, termNode.size());
//        assertEquals(2, termNode.getNode("hispanicRank").size());
//        assertEquals("25", termNode.getNode("hispanicRank").getString("value"));
//        assertEquals(new Double(1.0), (Double) termNode.getNode("hispanicRank").get("boost"));
//
//    }
//
//    @Test
//    public void testCompoundNotEq() {
//        //      {
//        //         "query" : {
//        //           "bool" : {
//        //             "filter" : [ {
//        //               "bool" : {
//        //                 "must_not" : [ {
//        //                   "term" : {
//        //                     "hispanicRank" : {
//        //                       "value" : "95",
//        //                       "boost" : 1.0
//        //                     }
//        //                   }
//        //                 } ],
//        //                 "adjust_pure_negative" : true,
//        //                 "boost" : 1.0
//        //               }
//        //             }, {
//        //               "range" : {
//        //                 "hispanicRank" : {
//        //                   "from" : "93",
//        //                   "to" : null,
//        //                   "include_lower" : false,
//        //                   "include_upper" : true,
//        //                   "boost" : 1.0
//        //                 }
//        //               }
//        //             } ],
//        //             "adjust_pure_negative" : true,
//        //             "boost" : 1.0
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("and(ne(hispanicRank,95),gt(hispanicRank,93))");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertEquals(2, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        assertNotNull(queryNode.get("bool"));
//
//        JSNode bool1Node = queryNode.getNode("bool");
//        assertEquals(3, bool1Node.size());
//        assertTrue((Boolean) bool1Node.get("adjust_pure_negative"));
//        assertEquals(new Double(1.0), (Double) bool1Node.get("boost"));
//
//        assertNotNull(bool1Node.get("filter"));
//        JSList filterArr = bool1Node.getList("filter");
//
//        assertEquals(2, filterArr.length());
//
//        boolean rangeFound = false;
//        boolean boolFound  = false;
//
//        for (JSNode node : filterArr.asNodeList()) {
//            if (node.hasProperty("range")) {
//                assertEquals(1, node.size());
//                JSNode rangeNode = node.getNode("range");
//                assertEquals(1, rangeNode.size());
//                assertEquals(5, rangeNode.getNode("hispanicRank").size());
//                assertEquals("93", rangeNode.getNode("hispanicRank").getString("from"));
//                assertNull(rangeNode.getNode("hispanicRank").get("to"));
//                assertFalse((Boolean) rangeNode.getNode("hispanicRank").get("include_lower"));
//                assertTrue((Boolean) rangeNode.getNode("hispanicRank").get("include_upper"));
//                assertEquals(new Double(1.0), (Double) rangeNode.getNode("hispanicRank").get("boost"));
//                rangeFound = true;
//            } else if (node.hasProperty("bool")) {
//                assertEquals(1, node.size());
//                JSNode bool2Node = node.getNode("bool");
//                assertEquals(3, bool2Node.size());
//                assertTrue((Boolean) bool2Node.get("adjust_pure_negative"));
//                assertEquals(new Double(1.0), (Double) bool2Node.get("boost"));
//
//                JSList mustNotArr = bool2Node.getList("must_not");
//
//                assertEquals(1, mustNotArr.length());
//                JSNode termNode = mustNotArr.getNode(0).getNode("term");
//                assertEquals(1, termNode.size());
//                assertEquals(2, termNode.getNode("hispanicRank").size());
//                assertEquals("95", termNode.getNode("hispanicRank").getString("value"));
//                assertEquals(new Double(1.0), (Double) termNode.getNode("hispanicRank").get("boost"));
//                boolFound = true;
//            }
//
//        }
//
//        assertTrue(rangeFound);
//        assertTrue(boolFound);
//
//    }
//
//    @Test
//    public void simpleAnd() {
//        //      {
//        //         "query" : {
//        //           "bool" : {
//        //             "filter" : [ {
//        //               "bool" : {
//        //                 "must_not" : [ {
//        //                   "term" : {
//        //                     "hispanicRank" : {
//        //                       "value" : "95",
//        //                       "boost" : 1.0
//        //                     }
//        //                   }
//        //                 } ],
//        //                 "adjust_pure_negative" : true,
//        //                 "boost" : 1.0
//        //               }
//        //             }, {
//        //               "range" : {
//        //                 "hispanicRank" : {
//        //                   "from" : "93",
//        //                   "to" : null,
//        //                   "include_lower" : false,
//        //                   "include_upper" : true,
//        //                   "boost" : 1.0
//        //                 }
//        //               }
//        //             } ],
//        //             "adjust_pure_negative" : true,
//        //             "boost" : 1.0
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("ne(hispanicRank,95)&gt(hispanicRank,93)");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertEquals(2, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//
//        JSNode queryNode = JSNode.getNode("query");
//        assertEquals(1, queryNode.size());
//
//        assertNotNull(queryNode.get("bool"));
//
//        JSNode bool1Node = queryNode.getNode("bool");
//        assertEquals(3, bool1Node.size());
//        assertTrue((Boolean) bool1Node.get("adjust_pure_negative"));
//        assertEquals(new Double(1.0), (Double) bool1Node.get("boost"));
//
//        assertNotNull(bool1Node.get("filter"));
//        JSList filterArr = bool1Node.getList("filter");
//
//        assertEquals(2, filterArr.length());
//
//        boolean rangeFound = false;
//        boolean boolFound  = false;
//
//        for (JSNode node : filterArr.asNodeList()) {
//            if (node.hasProperty("range")) {
//                assertEquals(1, node.size());
//                JSNode rangeNode = node.getNode("range");
//                assertEquals(1, rangeNode.size());
//                assertEquals(5, rangeNode.getNode("hispanicRank").size());
//                assertEquals("93", rangeNode.getNode("hispanicRank").getString("from"));
//                assertNull(rangeNode.getNode("hispanicRank").get("to"));
//                assertFalse((Boolean) rangeNode.getNode("hispanicRank").get("include_lower"));
//                assertTrue((Boolean) rangeNode.getNode("hispanicRank").get("include_upper"));
//                assertEquals(new Double(1.0), (Double) rangeNode.getNode("hispanicRank").get("boost"));
//                rangeFound = true;
//            } else if (node.hasProperty("bool")) {
//                assertEquals(1, node.size());
//                JSNode bool2Node = node.getNode("bool");
//                assertEquals(3, bool2Node.size());
//                assertTrue((Boolean) bool2Node.get("adjust_pure_negative"));
//                assertEquals(new Double(1.0), (Double) bool2Node.get("boost"));
//
//                JSList mustNotArr = bool2Node.getList("must_not");
//
//                assertEquals(1, mustNotArr.length());
//                JSNode termNode = mustNotArr.getNode(0).getNode("term");
//                assertEquals(1, termNode.size());
//                assertEquals(2, termNode.getNode("hispanicRank").size());
//                assertEquals("95", termNode.getNode("hispanicRank").getString("value"));
//                assertEquals(new Double(1.0), (Double) termNode.getNode("hispanicRank").get("boost"));
//                boolFound = true;
//            }
//
//        }
//
//        assertTrue(rangeFound);
//        assertTrue(boolFound);
//
//    }
//
//    @Test
//    public void orFunction() {
//        //      {
//        //         "query" : {
//        //           "bool" : {
//        //             "should" : [ {
//        //               "term" : {
//        //                 "name" : {
//        //                   "value" : "fwqa",
//        //                   "boost" : 1.0
//        //                 }
//        //               }
//        //             }, {
//        //               "term" : {
//        //                 "name" : {
//        //                   "value" : "cheetos",
//        //                   "boost" : 1.0
//        //                 }
//        //               }
//        //             } ],
//        //             "adjust_pure_negative" : true,
//        //             "boost" : 1.0
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("or(eq(name,fwqa),eq(name,cheetos))");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        JSNode boolNode = queryNode.getNode("bool");
//        assertEquals(3, boolNode.size());
//        assertTrue((Boolean) boolNode.get("adjust_pure_negative"));
//        assertEquals(new Double(1.0), (Double) boolNode.get("boost"));
//        assertNotNull(boolNode.get("should"));
//
//        JSList shouldArr = boolNode.getList("should");
//        assertEquals(2, shouldArr.length());
//
//        boolean fwqaFound    = false;
//        boolean cheetosFound = false;
//
//        for (JSNode node : shouldArr.asNodeList()) {
//            if (node.hasProperty("term")) {
//                assertEquals(1, node.size());
//                JSNode termNode = node.getNode("term");
//                JSNode nameNode = termNode.getNode("name");
//                assertEquals(2, nameNode.size());
//                assertEquals(new Double(1.0), (Double) nameNode.get("boost"));
//                assertTrue(nameNode.hasProperty("value"));
//
//                if (nameNode.getString("value").equals("fwqa"))
//                    fwqaFound = true;
//
//                if (nameNode.getString("value").equals("cheetos"))
//                    cheetosFound = true;
//            }
//        }
//
//        assertTrue(fwqaFound);
//        assertTrue(cheetosFound);
//
//    }
//
//    @Test
//    public void inFunction() {
//        //      {
//        //         "query" : {
//        //           "terms" : {
//        //             "city" : [ "Chicago", "Tempe", "Chandler" ],
//        //             "boost" : 1.0
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("in(city,Chicago,Tempe,Chandler)");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        JSNode termsNode = queryNode.getNode("terms");
//        assertEquals(2, termsNode.size());
//        assertEquals(new Double(1.0), (Double) termsNode.get("boost"));
//
//        JSList cityArr = termsNode.getList("city");
//        assertEquals(3, cityArr.length());
//
//        boolean chicagoFound  = false;
//        boolean tempeFound    = false;
//        boolean chandlerFound = false;
//
//        for (Object city : cityArr.asList()) {
//            if (city.equals("Chicago"))
//                chicagoFound = true;
//            else if (city.equals("Tempe"))
//                tempeFound = true;
//            else if (city.equals("Chandler"))
//                chandlerFound = true;
//        }
//
//        assertTrue(chicagoFound);
//        assertTrue(tempeFound);
//        assertTrue(chandlerFound);
//
//    }
//
//    @Test
//    public void outFunction() {
//        //      {
//        //         "query" : {
//        //           "bool" : {
//        //             "must_not" : [ {
//        //               "terms" : {
//        //                 "city" : [ "Chicago", "Tempe", "Chandler" ],
//        //                 "boost" : 1.0
//        //               }
//        //             } ],
//        //             "adjust_pure_negative" : true,
//        //             "boost" : 1.0
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("out(city,Chicago,Tempe,Chandler)");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertEquals(2, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        assertNotNull(queryNode.get("bool"));
//
//        JSNode boolNode = queryNode.getNode("bool");
//        assertEquals(3, boolNode.size());
//        assertEquals(new Double(1.0), (Double) boolNode.get("boost"));
//        assertTrue((Boolean) boolNode.get("adjust_pure_negative"));
//        assertNotNull(boolNode.get("must_not"));
//
//        JSList mustNotArr = boolNode.getList("must_not");
//        assertEquals(1, mustNotArr.length());
//        JSNode termsNode = mustNotArr.getNode(0).getNode("terms");
//        assertEquals(2, termsNode.size());
//        assertEquals(new Double(1.0), (Double) termsNode.get("boost"));
//
//        JSList cityArr = termsNode.getList("city");
//        assertEquals(3, cityArr.length());
//
//        boolean chicagoFound  = false;
//        boolean tempeFound    = false;
//        boolean chandlerFound = false;
//
//        for (Object city : cityArr.asList()) {
//            if (city.equals("Chicago"))
//                chicagoFound = true;
//            else if (city.equals("Tempe"))
//                tempeFound = true;
//            else if (city.equals("Chandler"))
//                chandlerFound = true;
//        }
//
//        assertTrue(chicagoFound);
//        assertTrue(tempeFound);
//        assertTrue(chandlerFound);
//
//    }
//
//    @Test
//    public void complexAnd() {
//        //      {
//        //         "query" : {
//        //           "bool" : {
//        //             "filter" : [ {
//        //               "wildcard" : {
//        //                 "locationCode" : {
//        //                   "wildcard" : "270*",
//        //                   "boost" : 1.0
//        //                 }
//        //               }
//        //             }, {
//        //               "term" : {
//        //                 "city" : {
//        //                   "value" : "Chandler",
//        //                   "boost" : 1.0
//        //                 }
//        //               }
//        //             }, {
//        //               "wildcard" : {
//        //                 "address1" : {
//        //                   "wildcard" : "*McQueen*",
//        //                   "boost" : 1.0
//        //                 }
//        //               }
//        //             } ],
//        //             "adjust_pure_negative" : true,
//        //             "boost" : 1.0
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("eq(locationCode,270*)&eq(city,Chandler)&eq(address1,*McQueen*)");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertEquals(2, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        assertNotNull(queryNode.get("bool"));
//
//        JSNode boolNode = queryNode.getNode("bool");
//        assertEquals(3, boolNode.size());
//        assertTrue((Boolean) boolNode.get("adjust_pure_negative"));
//        assertEquals(new Double(1.0), (Double) boolNode.get("boost"));
//
//        assertNotNull(boolNode.get("filter"));
//        JSList filterArr = boolNode.getList("filter");
//
//        assertEquals(3, filterArr.length());
//
//        boolean locationNodeFound = false;
//        boolean termNodeFound     = false;
//        boolean addressNodeFound  = false;
//
//        for (JSNode node : filterArr.asNodeList()) {
//            if (node.hasProperty("wildcard")) {
//                assertEquals(1, node.size());
//                JSNode wildcardNode = node.getNode("wildcard");
//
//                assertEquals(1, wildcardNode.size());
//
//                if (wildcardNode.get("locationCode") != null) {
//                    JSNode locationNode = wildcardNode.getNode("locationCode");
//                    assertEquals(2, locationNode.size());
//                    assertEquals("270*", locationNode.get("wildcard"));
//                    assertEquals(new Double(1.0), (Double) locationNode.get("boost"));
//                    locationNodeFound = true;
//                } else if (wildcardNode.get("address1") != null) {
//                    JSNode addressNode = wildcardNode.getNode("address1");
//                    assertEquals(2, addressNode.size());
//                    assertEquals("*McQueen*", addressNode.get("wildcard"));
//                    assertEquals(new Double(1.0), (Double) addressNode.get("boost"));
//                    addressNodeFound = true;
//                }
//            } else if (node.hasProperty("term")) {
//                assertEquals(1, node.size());
//                JSNode termNode = node.getNode("term");
//
//                assertEquals(1, termNode.size());
//                JSNode cityNode = termNode.getNode("city");
//                assertEquals(2, cityNode.size());
//                assertEquals(new Double(1.0), (Double) cityNode.get("boost"));
//                assertEquals("Chandler", cityNode.get("value"));
//                termNodeFound = true;
//            }
//        }
//
//        assertTrue(locationNodeFound);
//        assertTrue(termNodeFound);
//        assertTrue(addressNodeFound);
//
//    }
//
//    @Test
//    public void complexSearch1() {
//        //      {
//        //         "query" : {
//        //           "bool" : {
//        //             "filter" : [ {
//        //               "fuzzy" : {
//        //                 "keywords" : {
//        //                   "value" : "test",
//        //                   "fuzziness" : "AUTO",
//        //                   "prefix_length" : 0,
//        //                   "max_expansions" : 50,
//        //                   "transpositions" : false,
//        //                   "boost" : 1.0
//        //                 }
//        //               }
//        //             }, {
//        //               "fuzzy" : {
//        //                 "keywords" : {
//        //                   "value" : "matt",
//        //                   "fuzziness" : "AUTO",
//        //                   "prefix_length" : 0,
//        //                   "max_expansions" : 50,
//        //                   "transpositions" : false,
//        //                   "boost" : 1.0
//        //                 }
//        //               }
//        //             } ],
//        //             "adjust_pure_negative" : true,
//        //             "boost" : 1.0
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("and(search(keywords,test),search(keywords,matt))");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertEquals(2, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        assertNotNull(queryNode.get("bool"));
//
//        JSNode boolNode = queryNode.getNode("bool");
//        assertEquals(3, boolNode.size());
//        assertTrue((Boolean) boolNode.get("adjust_pure_negative"));
//        assertEquals(new Double(1.0), (Double) boolNode.get("boost"));
//
//        assertNotNull(boolNode.get("filter"));
//        JSList filterArr = boolNode.getList("filter");
//
//        assertEquals(2, filterArr.length());
//
//        boolean testFound = false;
//        boolean mattFound = false;
//
//        for (JSNode node : filterArr.asNodeList()) {
//            if (node.hasProperty("fuzzy")) {
//                assertEquals(1, node.size());
//                JSNode fuzzyNode = node.getNode("fuzzy");
//                assertEquals(1, fuzzyNode.size());
//
//                JSNode keywordsNode = fuzzyNode.getNode("keywords");
//                assertEquals(6, keywordsNode.size());
//
//                assertEquals("AUTO", keywordsNode.getString("fuzziness"));
//                assertEquals(0, keywordsNode.getInt("prefix_length"));
//                assertEquals(50, keywordsNode.getInt("max_expansions"));
//                assertFalse((Boolean) keywordsNode.get("transpositions"));
//                assertEquals(new Double(1.0), (Double) keywordsNode.get("boost"));
//
//                if (keywordsNode.getString("value").equals("test"))
//                    testFound = true;
//
//                if (keywordsNode.getString("value").equals("matt"))
//                    mattFound = true;
//            }
//        }
//
//        assertTrue(testFound);
//        assertTrue(mattFound);
//
//    }
//
//    @Test
//    public void complexSearch2() {
//        //      {
//        //         "query" : {
//        //           "bool" : {
//        //             "filter" : [ {
//        //               "fuzzy" : {
//        //                 "keywords" : {
//        //                   "value" : "test",
//        //                   "fuzziness" : "AUTO",
//        //                   "prefix_length" : 0,
//        //                   "max_expansions" : 50,
//        //                   "transpositions" : false,
//        //                   "boost" : 1.0
//        //                 }
//        //               }
//        //             }, {
//        //               "fuzzy" : {
//        //                 "keywords" : {
//        //                   "value" : "matt",
//        //                   "fuzziness" : "AUTO",
//        //                   "prefix_length" : 0,
//        //                   "max_expansions" : 50,
//        //                   "transpositions" : false,
//        //                   "boost" : 1.0
//        //                 }
//        //               }
//        //             } ],
//        //             "adjust_pure_negative" : true,
//        //             "boost" : 1.0
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("search(keywords,test)&search(keywords,matt)");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertEquals(2, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        assertNotNull(queryNode.get("bool"));
//
//        JSNode boolNode = queryNode.getNode("bool");
//        assertEquals(3, boolNode.size());
//        assertTrue((Boolean) boolNode.get("adjust_pure_negative"));
//        assertEquals(new Double(1.0), (Double) boolNode.get("boost"));
//
//        assertNotNull(boolNode.get("filter"));
//        JSList filterArr = boolNode.getList("filter");
//
//        assertEquals(2, filterArr.length());
//
//        boolean testFound = false;
//        boolean mattFound = false;
//
//        for (JSNode node : filterArr.asNodeList()) {
//            if (node.hasProperty("fuzzy")) {
//                assertEquals(1, node.size());
//                JSNode fuzzyNode = node.getNode("fuzzy");
//                assertEquals(1, fuzzyNode.size());
//
//                JSNode keywordsNode = fuzzyNode.getNode("keywords");
//                assertEquals(6, keywordsNode.size());
//
//                assertEquals("AUTO", keywordsNode.getString("fuzziness"));
//                assertEquals(0, keywordsNode.getInt("prefix_length"));
//                assertEquals(50, keywordsNode.getInt("max_expansions"));
//                assertFalse((Boolean) keywordsNode.get("transpositions"));
//                assertEquals(new Double(1.0), (Double) keywordsNode.get("boost"));
//
//                if (keywordsNode.getString("value").equals("test"))
//                    testFound = true;
//
//                if (keywordsNode.getString("value").equals("matt"))
//                    mattFound = true;
//            }
//        }
//
//        assertTrue(testFound);
//        assertTrue(mattFound);
//
//    }
//
//    @Test
//    public void complexOr() {
//        //      {
//        //         "query" : {
//        //           "bool" : {
//        //             "should" : [ {
//        //               "term" : {
//        //                 "id" : {
//        //                   "value" : "3",
//        //                   "boost" : 1.0
//        //                 }
//        //               }
//        //             }, {
//        //               "wildcard" : {
//        //                 "name" : {
//        //                   "wildcard" : "*POST*",
//        //                   "boost" : 1.0
//        //                 }
//        //               }
//        //             } ],
//        //             "adjust_pure_negative" : true,
//        //             "boost" : 1.0
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("or(eq(id,3),eq(name,*POST*))");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertEquals(2, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        assertNotNull(queryNode.get("bool"));
//
//        JSNode boolNode = queryNode.getNode("bool");
//        assertEquals(3, boolNode.size());
//        assertTrue((Boolean) boolNode.get("adjust_pure_negative"));
//        assertEquals(new Double(1.0), (Double) boolNode.get("boost"));
//        assertNotNull(boolNode.get("should"));
//
//        JSList shouldArr = boolNode.getList("should");
//        assertEquals(2, shouldArr.length());
//
//        boolean termNodeFound     = false;
//        boolean wildcardNodeFound = false;
//
//        for (JSNode node : shouldArr.asNodeList()) {
//            if (node.hasProperty("term")) {
//                assertEquals(1, node.size());
//                JSNode termNode = node.getNode("term");
//                assertEquals(1, termNode.size());
//                JSNode idNode = termNode.getNode("id");
//                assertEquals(2, idNode.size());
//                assertEquals(new Double(1.0), (Double) idNode.get("boost"));
//                assertEquals("3", idNode.get("value"));
//
//                termNodeFound = true;
//            } else if (node.hasProperty("wildcard")) {
//                assertEquals(1, node.size());
//                JSNode wildNode = node.getNode("wildcard");
//                JSNode nameNode = wildNode.getNode("name");
//                assertEquals(2, nameNode.size());
//                assertEquals(new Double(1.0), (Double) nameNode.get("boost"));
//                assertEquals("*POST*", nameNode.get("wildcard"));
//
//                wildcardNodeFound = true;
//            }
//        }
//
//        assertTrue(termNodeFound);
//        assertTrue(wildcardNodeFound);
//
//    }
//
//    @Test
//    public void testWildcard() {
//        //      {
//        //         "query" : {
//        //           "wildcard" : {
//        //             "address1" : {
//        //               "wildcard" : "*GILBERT*",
//        //               "boost" : 1.0
//        //             }
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("eq(address1,*GILBERT*)");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//        assertEquals(2, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        assertNotNull(queryNode.get("wildcard"));
//
//        JSNode wildcardNode = queryNode.getNode("wildcard");
//        assertEquals(1, wildcardNode.size());
//        assertNotNull(wildcardNode.get("address1"));
//
//        JSNode addressNode = wildcardNode.getNode("address1");
//        assertEquals(2, addressNode.size());
//        assertEquals("*GILBERT*", addressNode.get("wildcard"));
//        assertEquals(new Double(1.0), (Double) addressNode.get("boost"));
//
//    }
//
//    @Test
//    public void testAndTerms() {
//        //      {
//        //         "query" : {
//        //           "bool" : {
//        //             "filter" : [ {
//        //               "term" : {
//        //                 "locationCode" : {
//        //                   "value" : "9187",
//        //                   "boost" : 1.0
//        //                 }
//        //               }
//        //             }, {
//        //               "term" : {
//        //                 "city" : {
//        //                   "value" : "CHANDLER",
//        //                   "boost" : 1.0
//        //                 }
//        //               }
//        //             } ],
//        //             "adjust_pure_negative" : true,
//        //             "boost" : 1.0
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("and(eq(locationCode,9187),eq(city,CHANDLER))");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertEquals(2, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        assertNotNull(queryNode.get("bool"));
//
//        JSNode boolNode = queryNode.getNode("bool");
//        assertEquals(3, boolNode.size());
//        assertTrue((Boolean) boolNode.get("adjust_pure_negative"));
//        assertEquals(new Double(1.0), (Double) boolNode.get("boost"));
//
//        assertNotNull(boolNode.get("filter"));
//        JSList filterArr = boolNode.getList("filter");
//
//        assertEquals(2, filterArr.length());
//
//        boolean locationNodeFound = false;
//        boolean cityNodeFound     = false;
//
//        for (JSNode node : filterArr.asNodeList()) {
//            if (node.hasProperty("term")) {
//                assertEquals(1, node.size());
//                JSNode termNode = node.getNode("term");
//
//                if (termNode.hasProperty("locationCode")) {
//                    JSNode locationNode = termNode.getNode("locationCode");
//                    assertEquals(2, locationNode.size());
//                    assertEquals(new Double(1.0), (Double) locationNode.get("boost"));
//                    assertEquals("9187", locationNode.get("value"));
//                    locationNodeFound = true;
//                } else if (termNode.hasProperty("city")) {
//                    JSNode cityNode = termNode.getNode("city");
//                    assertEquals(2, cityNode.size());
//                    assertEquals(new Double(1.0), (Double) cityNode.get("boost"));
//                    assertEquals("CHANDLER", cityNode.get("value"));
//                    cityNodeFound = true;
//                }
//            }
//        }
//
//        assertTrue(locationNodeFound);
//        assertTrue(cityNodeFound);
//
//    }
//
//    @Test
//    public void testAndWildcard() {
//        //      {
//        //         "query" : {
//        //           "bool" : {
//        //             "filter" : [ {
//        //               "wildcard" : {
//        //                 "address1" : {
//        //                   "wildcard" : "*GILBERT*",
//        //                   "boost" : 1.0
//        //                 }
//        //               }
//        //             }, {
//        //               "term" : {
//        //                 "city" : {
//        //                   "value" : "CHANDLER",
//        //                   "boost" : 1.0
//        //                 }
//        //               }
//        //             } ],
//        //             "adjust_pure_negative" : true,
//        //             "boost" : 1.0
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("and(eq(address1,*GILBERT*),eq(city,CHANDLER))");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertEquals(2, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        assertNotNull(queryNode.get("bool"));
//
//        JSNode boolNode = queryNode.getNode("bool");
//        assertEquals(3, boolNode.size());
//        assertTrue((Boolean) boolNode.get("adjust_pure_negative"));
//        assertEquals(new Double(1.0), (Double) boolNode.get("boost"));
//
//        assertNotNull(boolNode.get("filter"));
//        JSList filterArr = boolNode.getList("filter");
//
//        assertEquals(2, filterArr.length());
//
//        boolean wildcardNodeFound = false;
//        boolean termNodeFound     = false;
//
//        for (JSNode node : filterArr.asNodeList()) {
//            if (node.hasProperty("wildcard")) {
//                assertEquals(1, node.size());
//                JSNode wildcardNode = node.getNode("wildcard");
//
//                assertEquals(1, wildcardNode.size());
//                assertNotNull(wildcardNode.get("address1"));
//
//                JSNode addressNode = wildcardNode.getNode("address1");
//                assertEquals(2, addressNode.size());
//                assertEquals("*GILBERT*", addressNode.get("wildcard"));
//                assertEquals(new Double(1.0), (Double) addressNode.get("boost"));
//                wildcardNodeFound = true;
//            } else if (node.hasProperty("term")) {
//                assertEquals(1, node.size());
//                JSNode termNode = node.getNode("term");
//
//                assertEquals(1, termNode.size());
//                JSNode cityNode = termNode.getNode("city");
//                assertEquals(2, cityNode.size());
//                assertEquals(new Double(1.0), (Double) cityNode.get("boost"));
//                assertEquals("CHANDLER", cityNode.get("value"));
//                termNodeFound = true;
//            }
//        }
//
//        assertTrue(wildcardNodeFound);
//        assertTrue(termNodeFound);
//
//    }
//
//    @Test
//    public void smallCompoundQuery() {
//        //      {
//        //         "query" : {
//        //           "bool" : {
//        //             "filter" : [ {
//        //               "range" : {
//        //                 "hispanicRank" : {
//        //                   "from" : "25",
//        //                   "to" : null,
//        //                   "include_lower" : false,
//        //                   "include_upper" : true,
//        //                   "boost" : 1.0
//        //                 }
//        //               }
//        //             }, {
//        //               "range" : {
//        //                 "hispanicRank" : {
//        //                   "from" : null,
//        //                   "to" : "40",
//        //                   "include_lower" : true,
//        //                   "include_upper" : true,
//        //                   "boost" : 1.0
//        //                 }
//        //               }
//        //             } ],
//        //             "adjust_pure_negative" : true,
//        //             "boost" : 1.0
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("and(gt(hispanicRank,25),le(hispanicRank,40))");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertEquals(2, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//
//        JSNode queryNode = JSNode.getNode("query");
//
//        assertNotNull(queryNode.get("bool"));
//
//        JSNode boolNode = queryNode.getNode("bool");
//        assertEquals(3, boolNode.size());
//        assertTrue((Boolean) boolNode.get("adjust_pure_negative"));
//        assertEquals(new Double(1.0), (Double) boolNode.get("boost"));
//
//        assertNotNull(boolNode.get("filter"));
//        JSList filterArr = boolNode.getList("filter");
//
//        assertEquals(2, filterArr.length());
//
//        boolean fromFound = false;
//        boolean toFound   = false;
//
//        for (JSNode node : filterArr.asNodeList()) {
//            if (node.hasProperty("range")) {
//                assertEquals(1, node.size());
//                JSNode rangeNode = node.getNode("range");
//
//                assertEquals(1, rangeNode.size());
//                JSNode hispanicNode = rangeNode.getNode("hispanicRank");
//                assertEquals(5, hispanicNode.size());
//
//                if (hispanicNode.getString("from") != null) {
//                    assertEquals("25", hispanicNode.getString("from"));
//                    assertNull(hispanicNode.get("to"));
//                    assertFalse((Boolean) hispanicNode.get("include_lower"));
//                    assertTrue((Boolean) hispanicNode.get("include_upper"));
//                    assertEquals(new Double(1.0), (Double) hispanicNode.get("boost"));
//                    fromFound = true;
//                } else if (hispanicNode.getString("to") != null) {
//                    assertEquals("40", hispanicNode.getString("to"));
//                    assertNull(hispanicNode.get("from"));
//                    assertTrue((Boolean) hispanicNode.get("include_lower"));
//                    assertTrue((Boolean) hispanicNode.get("include_upper"));
//                    assertEquals(new Double(1.0), (Double) hispanicNode.get("boost"));
//                    toFound = true;
//                }
//            }
//        }
//
//        assertTrue(fromFound);
//        assertTrue(toFound);
//
//    }
//
//    @Test
//    public void largeCompoundQuery() {
//        //      {
//        //         "query" : {
//        //           "bool" : {
//        //             "filter" : [ {
//        //               "bool" : {
//        //                 "filter" : [ {
//        //                   "wildcard" : {
//        //                     "locationCode" : {
//        //                       "wildcard" : "270*",
//        //                       "boost" : 1.0
//        //                     }
//        //                   }
//        //                 }, {
//        //                   "term" : {
//        //                     "city" : {
//        //                       "value" : "Chandler",
//        //                       "boost" : 1.0
//        //                     }
//        //                   }
//        //                 } ],
//        //                 "adjust_pure_negative" : true,
//        //                 "boost" : 1.0
//        //               }
//        //             }, {
//        //               "wildcard" : {
//        //                 "address1" : {
//        //                   "wildcard" : "*McQueen*",
//        //                   "boost" : 1.0
//        //                 }
//        //               }
//        //             } ],
//        //             "adjust_pure_negative" : true,
//        //             "boost" : 1.0
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("and(and(eq(locationCode,270*),eq(city,Chandler)),and(eq(address1,*McQueen*)))");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertEquals(2, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        assertNotNull(queryNode.get("bool"));
//
//        JSNode boolNode = queryNode.getNode("bool");
//        assertEquals(3, boolNode.size());
//        assertTrue((Boolean) boolNode.get("adjust_pure_negative"));
//        assertEquals(new Double(1.0), (Double) boolNode.get("boost"));
//
//        assertNotNull(boolNode.get("filter"));
//        JSList filterArr = boolNode.getList("filter");
//
//        assertEquals(2, filterArr.length());
//
//        boolean wildcardFound      = false;
//        boolean innerWildcardFound = false;
//        boolean termFound          = false;
//
//        for (JSNode node : filterArr.asNodeList()) {
//            if (node.hasProperty("wildcard")) {
//                assertEquals(1, node.size());
//                JSNode wildcardNode = node.getNode("wildcard");
//                assertEquals(1, wildcardNode.size());
//                assertEquals(2, wildcardNode.getNode("address1").size());
//                assertEquals("*McQueen*", wildcardNode.getNode("address1").getString("wildcard"));
//                assertEquals(new Double(1.0), (Double) wildcardNode.getNode("address1").get("boost"));
//                wildcardFound = true;
//            } else if (node.hasProperty("bool")) {
//                JSNode innerBoolNode = node.getNode("bool");
//                assertEquals(3, innerBoolNode.size());
//                assertTrue((Boolean) innerBoolNode.get("adjust_pure_negative"));
//                assertEquals(new Double(1.0), (Double) innerBoolNode.get("boost"));
//
//                assertNotNull(innerBoolNode.get("filter"));
//                JSList innerFilterArr = innerBoolNode.getList("filter");
//
//                assertEquals(2, innerFilterArr.length());
//
//                for (JSNode innerNode : innerFilterArr.asNodeList()) {
//
//                    if (innerNode.hasProperty("term")) {
//                        assertEquals(1, innerNode.size());
//                        JSNode termNode = innerNode.getNode("term");
//                        assertEquals(1, termNode.size());
//                        assertEquals(2, termNode.getNode("city").size());
//                        assertEquals("Chandler", termNode.getNode("city").getString("value"));
//                        assertEquals(new Double(1.0), (Double) termNode.getNode("city").get("boost"));
//                        termFound = true;
//                    } else if (innerNode.hasProperty("wildcard")) {
//                        JSNode innerWildcardNode = innerNode.getNode("wildcard");
//                        assertEquals(1, innerWildcardNode.size());
//                        assertEquals(2, innerWildcardNode.getNode("locationCode").size());
//                        assertEquals("270*", innerWildcardNode.getNode("locationCode").getString("wildcard"));
//                        assertEquals(new Double(1.0), (Double) innerWildcardNode.getNode("locationCode").get("boost"));
//                        innerWildcardFound = true;
//                    }
//                }
//            }
//
//        }
//
//        assertTrue(wildcardFound);
//        assertTrue(innerWildcardFound);
//        assertTrue(termFound);
//
//    }
//
//    @Test
//    public void simpleNestedQuery() {
//        //      {
//        //         "query" : {
//        //           "nested" : {
//        //             "query" : {
//        //               "range" : {
//        //                 "players.registerNum" : {
//        //                   "from" : "5",
//        //                   "to" : null,
//        //                   "include_lower" : false,
//        //                   "include_upper" : true,
//        //                   "boost" : 1.0
//        //                 }
//        //               }
//        //             },
//        //             "path" : "players",
//        //             "ignore_unmapped" : false,
//        //             "score_mode" : "avg",
//        //             "boost" : 1.0
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("gt(players.registerNum,5)");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertEquals(2, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        assertNotNull(queryNode.get("nested"));
//
//        JSNode nestedNode = queryNode.getNode("nested");
//        assertEquals(5, nestedNode.size());
//        assertEquals("players", nestedNode.get("path"));
//        assertFalse((Boolean) nestedNode.get("ignore_unmapped"));
//        assertEquals("avg", nestedNode.get("score_mode"));
//        assertEquals(new Double(1.0), (Double) nestedNode.get("boost"));
//
//        JSNode innerQueryNode = nestedNode.getNode("query");
//        assertEquals(1, innerQueryNode.size());
//
//        JSNode rangeNode = innerQueryNode.getNode("range");
//        assertEquals(1, rangeNode.size());
//
//        JSNode registerNode = rangeNode.getNode("players.registerNum");
//        assertEquals(5, registerNode.size());
//        assertEquals("5", registerNode.get("from"));
//        assertNull(registerNode.get("to"));
//        assertFalse((Boolean) registerNode.get("include_lower"));
//        assertTrue((Boolean) registerNode.get("include_upper"));
//        assertEquals(new Double(1.0), (Double) registerNode.get("boost"));
//
//    }
//
//    @Test
//    public void complexNestedQuery1() {
//        //      {
//        //         "query" : {
//        //           "nested" : {
//        //             "query" : {
//        //               "bool" : {
//        //                 "filter" : [ {
//        //                   "term" : {
//        //                     "keywords.name" : {
//        //                       "value" : "color",
//        //                       "boost" : 1.0
//        //                     }
//        //                   }
//        //                 }, {
//        //                   "term" : {
//        //                     "keywords.value" : {
//        //                       "value" : "33",
//        //                       "boost" : 1.0
//        //                     }
//        //                   }
//        //                 } ],
//        //                 "adjust_pure_negative" : true,
//        //                 "boost" : 1.0
//        //               }
//        //             },
//        //             "path" : "keywords",
//        //             "ignore_unmapped" : false,
//        //             "score_mode" : "avg",
//        //             "boost" : 1.0
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("and(eq(keywords.name,color),eq(keywords.value,33))");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertEquals(2, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        assertNotNull(queryNode.get("nested"));
//
//        JSNode nestedNode = queryNode.getNode("nested");
//        assertEquals(5, nestedNode.size());
//        assertEquals("keywords", nestedNode.get("path"));
//        assertFalse((Boolean) nestedNode.get("ignore_unmapped"));
//        assertEquals("avg", nestedNode.get("score_mode"));
//        assertEquals(new Double(1.0), (Double) nestedNode.get("boost"));
//
//        JSNode innerQueryNode = nestedNode.getNode("query");
//        assertEquals(1, innerQueryNode.size());
//
//        boolean nestedNameFound  = false;
//        boolean nestedValueFound = false;
//
//        if (innerQueryNode.hasProperty("bool")) {
//            assertEquals(1, innerQueryNode.size());
//
//            JSNode boolNode = innerQueryNode.getNode("bool");
//            assertEquals(3, boolNode.size());
//            assertTrue((Boolean) boolNode.get("adjust_pure_negative"));
//            assertEquals(new Double(1.0), (Double) boolNode.get("boost"));
//
//            assertNotNull(boolNode.get("filter"));
//            JSList filterArr = boolNode.getList("filter");
//
//            assertEquals(2, filterArr.length());
//
//            for (JSNode innerNode : filterArr.asNodeList()) {
//
//                if (innerNode.hasProperty("term")) {
//                    assertEquals(1, innerNode.size());
//                    JSNode termNode = innerNode.getNode("term");
//                    assertEquals(1, termNode.size());
//
//                    if (termNode.hasProperty("keywords.name")) {
//                        assertEquals(2, termNode.getNode("keywords.name").size());
//                        assertEquals("color", termNode.getNode("keywords.name").getString("value"));
//                        assertEquals(new Double(1.0), (Double) termNode.getNode("keywords.name").get("boost"));
//                        nestedNameFound = true;
//
//                    } else if (termNode.hasProperty("keywords.value")) {
//                        assertEquals(2, termNode.getNode("keywords.value").size());
//                        assertEquals("33", termNode.getNode("keywords.value").getString("value"));
//                        assertEquals(new Double(1.0), (Double) termNode.getNode("keywords.value").get("boost"));
//                        nestedValueFound = true;
//                    }
//                }
//            }
//        }
//        assertTrue(nestedNameFound);
//        assertTrue(nestedValueFound);
//
//    }
//
//    @Test
//    public void complexNestedQuery2() {
//        //      {
//        //         "query" : {
//        //           "nested" : {
//        //             "query" : {
//        //               "bool" : {
//        //                 "filter" : [ {
//        //                   "term" : {
//        //                     "keywords.name" : {
//        //                       "value" : "age",
//        //                       "boost" : 1.0
//        //                     }
//        //                   }
//        //                 }, {
//        //                   "range" : {
//        //                     "keywords.value" : {
//        //                       "from" : "30",
//        //                       "to" : null,
//        //                       "include_lower" : false,
//        //                       "include_upper" : true,
//        //                       "boost" : 1.0
//        //                     }
//        //                   }
//        //                 } ],
//        //                 "adjust_pure_negative" : true,
//        //                 "boost" : 1.0
//        //               }
//        //             },
//        //             "path" : "keywords",
//        //             "ignore_unmapped" : false,
//        //             "score_mode" : "avg",
//        //             "boost" : 1.0
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("and(eq(keywords.name,age),gt(keywords.value,30))");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertEquals(2, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        assertNotNull(queryNode.get("nested"));
//
//        JSNode nestedNode = queryNode.getNode("nested");
//        assertEquals(5, nestedNode.size());
//        assertEquals("keywords", nestedNode.get("path"));
//        assertFalse((Boolean) nestedNode.get("ignore_unmapped"));
//        assertEquals("avg", nestedNode.get("score_mode"));
//        assertEquals(new Double(1.0), (Double) nestedNode.get("boost"));
//
//        JSNode innerQueryNode = nestedNode.getNode("query");
//        assertEquals(1, innerQueryNode.size());
//
//        boolean termFound  = false;
//        boolean rangeFound = false;
//
//        if (innerQueryNode.hasProperty("bool")) {
//            assertEquals(1, innerQueryNode.size());
//
//            JSNode boolNode = innerQueryNode.getNode("bool");
//            assertEquals(3, boolNode.size());
//            assertTrue((Boolean) boolNode.get("adjust_pure_negative"));
//            assertEquals(new Double(1.0), (Double) boolNode.get("boost"));
//
//            assertNotNull(boolNode.get("filter"));
//            JSList filterArr = boolNode.getList("filter");
//
//            assertEquals(2, filterArr.length());
//
//            for (JSNode innerNode : filterArr.asNodeList()) {
//
//                if (innerNode.hasProperty("term")) {
//                    assertEquals(1, innerNode.size());
//                    JSNode termNode = innerNode.getNode("term");
//                    assertEquals(1, termNode.size());
//                    assertEquals(2, termNode.getNode("keywords.name").size());
//                    assertEquals("age", termNode.getNode("keywords.name").getString("value"));
//                    assertEquals(new Double(1.0), (Double) termNode.getNode("keywords.name").get("boost"));
//                    termFound = true;
//                } else if (innerNode.hasProperty("range")) {
//                    JSNode rangeNode = innerNode.getNode("range");
//                    assertEquals(1, rangeNode.size());
//
//                    JSNode registerNode = rangeNode.getNode("keywords.value");
//                    assertEquals(5, registerNode.size());
//                    assertEquals("30", registerNode.get("from"));
//                    assertNull(registerNode.get("to"));
//                    assertFalse((Boolean) registerNode.get("include_lower"));
//                    assertTrue((Boolean) registerNode.get("include_upper"));
//                    assertEquals(new Double(1.0), (Double) registerNode.get("boost"));
//                    rangeFound = true;
//                }
//            }
//        }
//        assertTrue(termFound);
//        assertTrue(rangeFound);
//
//    }
//
//    @Test
//    public void complexNestedQuery3() {
//        //      {
//        //         "query" : {
//        //           "nested" : {
//        //             "query" : {
//        //               "bool" : {
//        //                 "filter" : [ {
//        //                   "term" : {
//        //                     "keywords.name" : {
//        //                       "value" : "items.name",
//        //                       "boost" : 1.0
//        //                     }
//        //                   }
//        //                 }, {
//        //                   "wildcard" : {
//        //                     "keywords.value" : {
//        //                       "wildcard" : "*Powerade*",
//        //                       "boost" : 1.0
//        //                     }
//        //                   }
//        //                 } ],
//        //                 "adjust_pure_negative" : true,
//        //                 "boost" : 1.0
//        //               }
//        //             },
//        //             "path" : "keywords",
//        //             "ignore_unmapped" : false,
//        //             "score_mode" : "avg",
//        //             "boost" : 1.0
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("and(eq(keywords.name,items.name),w(keywords.value,Powerade))");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertEquals(2, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        assertNotNull(queryNode.get("nested"));
//
//        JSNode nestedNode = queryNode.getNode("nested");
//        assertEquals(5, nestedNode.size());
//        assertEquals("keywords", nestedNode.get("path"));
//        assertFalse((Boolean) nestedNode.get("ignore_unmapped"));
//        assertEquals("avg", nestedNode.get("score_mode"));
//        assertEquals(new Double(1.0), (Double) nestedNode.get("boost"));
//
//        JSNode innerQueryNode = nestedNode.getNode("query");
//        assertEquals(1, innerQueryNode.size());
//
//        boolean termFound          = false;
//        boolean innerWildcardFound = false;
//
//        if (innerQueryNode.hasProperty("bool")) {
//            assertEquals(1, innerQueryNode.size());
//
//            JSNode boolNode = innerQueryNode.getNode("bool");
//            assertEquals(3, boolNode.size());
//            assertTrue((Boolean) boolNode.get("adjust_pure_negative"));
//            assertEquals(new Double(1.0), (Double) boolNode.get("boost"));
//
//            assertNotNull(boolNode.get("filter"));
//            JSList filterArr = boolNode.getList("filter");
//
//            assertEquals(2, filterArr.length());
//
//            for (JSNode innerNode : filterArr.asNodeList()) {
//
//                if (innerNode.hasProperty("term")) {
//                    assertEquals(1, innerNode.size());
//                    JSNode termNode = innerNode.getNode("term");
//                    assertEquals(1, termNode.size());
//                    assertEquals(2, termNode.getNode("keywords.name").size());
//                    assertEquals("items.name", termNode.getNode("keywords.name").getString("value"));
//                    assertEquals(new Double(1.0), (Double) termNode.getNode("keywords.name").get("boost"));
//                    termFound = true;
//                } else if (innerNode.hasProperty("wildcard")) {
//                    JSNode innerWildcardNode = innerNode.getNode("wildcard");
//                    assertEquals(1, innerWildcardNode.size());
//                    assertEquals(2, innerWildcardNode.getNode("keywords.value").size());
//                    assertEquals("*Powerade*", innerWildcardNode.getNode("keywords.value").getString("wildcard"));
//                    assertEquals(new Double(1.0), (Double) innerWildcardNode.getNode("keywords.value").get("boost"));
//                    innerWildcardFound = true;
//                }
//            }
//        }
//        assertTrue(termFound);
//        assertTrue(innerWildcardFound);
//
//    }
//
//    @Test
//    public void complexNestedQuery4() {
//        //      {
//        //         "query" : {
//        //           "nested" : {
//        //             "query" : {
//        //               "bool" : {
//        //                 "filter" : [ {
//        //                   "wildcard" : {
//        //                     "keywords.value" : {
//        //                       "wildcard" : "*Powerade*",
//        //                       "boost" : 1.0
//        //                     }
//        //                   }
//        //                 }, {
//        //                   "term" : {
//        //                     "keywords.name" : {
//        //                       "value" : "items.name",
//        //                       "boost" : 1.0
//        //                     }
//        //                   }
//        //                 } ],
//        //                 "adjust_pure_negative" : true,
//        //                 "boost" : 1.0
//        //               }
//        //             },
//        //             "path" : "keywords",
//        //             "ignore_unmapped" : false,
//        //             "score_mode" : "avg",
//        //             "boost" : 1.0
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("and(w(keywords.value,Powerade),eq(keywords.name,items.name))");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertEquals(2, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        assertNotNull(queryNode.get("nested"));
//
//        JSNode nestedNode = queryNode.getNode("nested");
//        assertEquals(5, nestedNode.size());
//        assertEquals("keywords", nestedNode.get("path"));
//        assertFalse((Boolean) nestedNode.get("ignore_unmapped"));
//        assertEquals("avg", nestedNode.get("score_mode"));
//        assertEquals(new Double(1.0), (Double) nestedNode.get("boost"));
//
//        JSNode innerQueryNode = nestedNode.getNode("query");
//        assertEquals(1, innerQueryNode.size());
//
//        boolean termFound          = false;
//        boolean innerWildcardFound = false;
//
//        if (innerQueryNode.hasProperty("bool")) {
//            assertEquals(1, innerQueryNode.size());
//
//            JSNode boolNode = innerQueryNode.getNode("bool");
//            assertEquals(3, boolNode.size());
//            assertTrue((Boolean) boolNode.get("adjust_pure_negative"));
//            assertEquals(new Double(1.0), (Double) boolNode.get("boost"));
//
//            assertNotNull(boolNode.get("filter"));
//            JSList filterArr = boolNode.getList("filter");
//
//            assertEquals(2, filterArr.length());
//
//            for (JSNode innerNode : filterArr.asNodeList()) {
//
//                if (innerNode.hasProperty("term")) {
//                    assertEquals(1, innerNode.size());
//                    JSNode termNode = innerNode.getNode("term");
//                    assertEquals(1, termNode.size());
//                    assertEquals(2, termNode.getNode("keywords.name").size());
//                    assertEquals("items.name", termNode.getNode("keywords.name").getString("value"));
//                    assertEquals(new Double(1.0), (Double) termNode.getNode("keywords.name").get("boost"));
//                    termFound = true;
//                } else if (innerNode.hasProperty("wildcard")) {
//                    JSNode innerWildcardNode = innerNode.getNode("wildcard");
//                    assertEquals(1, innerWildcardNode.size());
//                    assertEquals(2, innerWildcardNode.getNode("keywords.value").size());
//                    assertEquals("*Powerade*", innerWildcardNode.getNode("keywords.value").getString("wildcard"));
//                    assertEquals(new Double(1.0), (Double) innerWildcardNode.getNode("keywords.value").get("boost"));
//                    innerWildcardFound = true;
//                }
//            }
//        }
//        assertTrue(termFound);
//        assertTrue(innerWildcardFound);
//
//    }
//
//    @Test
//    public void compoundNestedQuery1() {
//        //      {
//        //         "query" : {
//        //           "bool" : {
//        //             "filter" : [ {
//        //               "term" : {
//        //                 "city" : {
//        //                   "value" : "Chandler",
//        //                   "boost" : 1.0
//        //                 }
//        //               }
//        //             }, {
//        //               "nested" : {
//        //                 "query" : {
//        //                   "range" : {
//        //                     "players.registerNum" : {
//        //                       "from" : "5",
//        //                       "to" : null,
//        //                       "include_lower" : false,
//        //                       "include_upper" : true,
//        //                       "boost" : 1.0
//        //                     }
//        //                   }
//        //                 },
//        //                 "path" : "players",
//        //                 "ignore_unmapped" : false,
//        //                 "score_mode" : "avg",
//        //                 "boost" : 1.0
//        //               }
//        //             } ],
//        //             "adjust_pure_negative" : true,
//        //             "boost" : 1.0
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("and(gt(players.registerNum,5),eq(city,Chandler))");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertEquals(2, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        assertNotNull(queryNode.get("bool"));
//
//        JSNode boolNode = queryNode.getNode("bool");
//        assertEquals(3, boolNode.size());
//        assertTrue((Boolean) boolNode.get("adjust_pure_negative"));
//        assertEquals(new Double(1.0), (Double) boolNode.get("boost"));
//
//        assertNotNull(boolNode.get("filter"));
//        JSList filterArr = boolNode.getList("filter");
//
//        assertEquals(2, filterArr.length());
//
//        boolean termFound   = false;
//        boolean nestedFound = false;
//
//        for (JSNode node : filterArr.asNodeList()) {
//            if (node.hasProperty("term")) {
//                assertEquals(1, node.size());
//                JSNode termNode = node.getNode("term");
//                assertEquals(1, termNode.size());
//                assertEquals(2, termNode.getNode("city").size());
//                assertEquals("Chandler", termNode.getNode("city").getString("value"));
//                assertEquals(new Double(1.0), (Double) termNode.getNode("city").get("boost"));
//                termFound = true;
//            } else if (node.hasProperty("nested")) {
//                assertEquals(1, node.size());
//                JSNode nestedNode = node.getNode("nested");
//                assertEquals(5, nestedNode.size());
//                assertEquals("players", nestedNode.get("path"));
//                assertFalse((Boolean) nestedNode.get("ignore_unmapped"));
//                assertEquals("avg", nestedNode.get("score_mode"));
//                assertEquals(new Double(1.0), (Double) nestedNode.get("boost"));
//
//                JSNode innerQueryNode = nestedNode.getNode("query");
//                assertEquals(1, innerQueryNode.size());
//
//                JSNode rangeNode = innerQueryNode.getNode("range");
//                assertEquals(1, rangeNode.size());
//
//                JSNode registerNode = rangeNode.getNode("players.registerNum");
//                assertEquals(5, registerNode.size());
//                assertEquals("5", registerNode.get("from"));
//                assertNull(registerNode.get("to"));
//                assertFalse((Boolean) registerNode.get("include_lower"));
//                assertTrue((Boolean) registerNode.get("include_upper"));
//                assertEquals(new Double(1.0), (Double) registerNode.get("boost"));
//                nestedFound = true;
//            }
//        }
//
//        assertTrue(termFound);
//        assertTrue(nestedFound);
//
//    }
//
//    @Test
//    public void compoundNestedQuery2() {
//        //      {
//        //         "query" : {
//        //           "bool" : {
//        //             "filter" : [ {
//        //               "bool" : {
//        //                 "filter" : [ {
//        //                   "term" : {
//        //                     "city" : {
//        //                       "value" : "PHOENIX",
//        //                       "boost" : 1.0
//        //                     }
//        //                   }
//        //                 }, {
//        //                   "nested" : {
//        //                     "query" : {
//        //                       "term" : {
//        //                         "players.deleted" : {
//        //                           "value" : "true",
//        //                           "boost" : 1.0
//        //                         }
//        //                       }
//        //                     },
//        //                     "path" : "players",
//        //                     "ignore_unmapped" : false,
//        //                     "score_mode" : "avg",
//        //                     "boost" : 1.0
//        //                   }
//        //                 } ],
//        //                 "adjust_pure_negative" : true,
//        //                 "boost" : 1.0
//        //               }
//        //             }, {
//        //               "wildcard" : {
//        //                 "address1" : {
//        //                   "wildcard" : "*VALLEY*",
//        //                   "boost" : 1.0
//        //                 }
//        //               }
//        //             } ],
//        //             "adjust_pure_negative" : true,
//        //             "boost" : 1.0
//        //           }
//        //         },
//        //         "sort" : [ {
//        //           "id" : {
//        //             "order" : "asc"
//        //           }
//        //         } ]
//        //       }
//
//        ElasticsearchQuery query = new ElasticsearchQuery();
//        query.withTerm("and(and(eq(players.deleted,true),eq(city,PHOENIX)),and(eq(address1,*VALLEY*)))");
//
//        JSNode JSNode = query.getJson();
//        assertNotNull(JSNode, "json should not be empty.");
//
//        assertEquals(2, JSNode.size());
//
//        assertNotNull(JSNode.get("sort"));
//
//        assertNotNull(JSNode.get("query"));
//        JSNode queryNode = JSNode.getNode("query");
//
//        assertNotNull(queryNode.get("bool"));
//
//        JSNode boolNode = queryNode.getNode("bool");
//        assertEquals(3, boolNode.size());
//        assertTrue((Boolean) boolNode.get("adjust_pure_negative"));
//        assertEquals(new Double(1.0), (Double) boolNode.get("boost"));
//
//        assertNotNull(boolNode.get("filter"));
//        JSList filterArr = boolNode.getList("filter");
//
//        assertEquals(2, filterArr.length());
//
//        boolean wildcardFound = false;
//        boolean nestedFound   = false;
//        boolean termFound     = false;
//
//        for (JSNode node : filterArr.asNodeList()) {
//            if (node.hasProperty("wildcard")) {
//                assertEquals(1, node.size());
//                JSNode wildcardNode = node.getNode("wildcard");
//                assertEquals(1, wildcardNode.size());
//                assertEquals(2, wildcardNode.getNode("address1").size());
//                assertEquals("*VALLEY*", wildcardNode.getNode("address1").getString("wildcard"));
//                assertEquals(new Double(1.0), (Double) wildcardNode.getNode("address1").get("boost"));
//                wildcardFound = true;
//            } else if (node.hasProperty("bool")) {
//                assertEquals(1, node.size());
//
//                JSNode innerBoolNode = node.getNode("bool");
//                assertEquals(3, innerBoolNode.size());
//                assertTrue((Boolean) innerBoolNode.get("adjust_pure_negative"));
//                assertEquals(new Double(1.0), (Double) innerBoolNode.get("boost"));
//
//                assertNotNull(innerBoolNode.get("filter"));
//                JSList innerFilterArr = innerBoolNode.getList("filter");
//
//                assertEquals(2, innerFilterArr.length());
//
//                for (JSNode innerNode : innerFilterArr.asNodeList()) {
//
//                    if (innerNode.hasProperty("term")) {
//                        assertEquals(1, innerNode.size());
//                        JSNode termNode = innerNode.getNode("term");
//                        assertEquals(1, termNode.size());
//                        assertEquals(2, termNode.getNode("city").size());
//                        assertEquals("PHOENIX", termNode.getNode("city").getString("value"));
//                        assertEquals(new Double(1.0), (Double) termNode.getNode("city").get("boost"));
//                        termFound = true;
//                    } else if (innerNode.hasProperty("nested")) {
//                        JSNode nestedNode = innerNode.getNode("nested");
//                        assertEquals(5, nestedNode.size());
//                        assertEquals("players", nestedNode.get("path"));
//                        assertFalse((Boolean) nestedNode.get("ignore_unmapped"));
//                        assertEquals("avg", nestedNode.get("score_mode"));
//                        assertEquals(new Double(1.0), (Double) nestedNode.get("boost"));
//
//                        JSNode innerQueryNode = nestedNode.getNode("query");
//                        assertEquals(1, innerQueryNode.size());
//
//                        JSNode nestedTermNode = innerQueryNode.getNode("term");
//                        assertEquals(1, nestedTermNode.size());
//
//                        JSNode deletedNode = nestedTermNode.getNode("players.deleted");
//                        assertEquals(2, deletedNode.size());
//                        assertEquals("true", deletedNode.get("value"));
//                        assertEquals(new Double(1.0), (Double) deletedNode.get("boost"));
//                        nestedFound = true;
//                    }
//                }
//            }
//        }
//
//        assertTrue(wildcardFound);
//        assertTrue(termFound);
//        assertTrue(nestedFound);
//
//    }

}
