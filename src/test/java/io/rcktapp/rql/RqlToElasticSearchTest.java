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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.rcktapp.rql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.rcktapp.rql.elasticsearch.BoolQuery;
import io.rcktapp.rql.elasticsearch.NestedQuery;
import io.rcktapp.rql.elasticsearch.QueryDsl;
import io.rcktapp.rql.elasticsearch.Range;
import io.rcktapp.rql.elasticsearch.Term;
import io.rcktapp.rql.elasticsearch.Wildcard;

/**
 * @author kfrankic
 *
 */
public class RqlToElasticSearchTest
{
   private static Logger log = LoggerFactory.getLogger(RqlToElasticSearchTest.class);

   @Test
   public void testMapper() throws Exception
   {
      // This test is an attempt to take class attributes(BCD) and set them all 
      // on another attribute(A), as though those that attribute(A) is an object 
      // made up of those attributes(BCD).  attribute.
      ObjectMapper mapper = new ObjectMapper();
      Range range = new Range("testRange");
      range.setGt(25);
      String json = mapper.writeValueAsString(range);

      assertNotNull("json should not be empty.", json);
      assertEquals("{\"testRange\":{\"gt\":25}}", json);

   }

   @Test
   public void startsWith()
   {
      try
      {
         QueryDsl dsl = new RQL("elastic").toQueryDsl(split("sw(city,Chand)"));

         assertNull(dsl.getBool());
         assertNull(dsl.getNested());
         assertNull(dsl.getNestedPath());
         assertNull(dsl.getRange());
         assertNull(dsl.getTerm());
         assertNotNull(dsl.getWildcard());

         assertEquals("city", dsl.getWildcard().getName());
         assertEquals("Chand*", dsl.getWildcard().getValue());
         assertNull(dsl.getWildcard().getNestedPath());

         ObjectMapper mapper = new ObjectMapper();
         String json = mapper.writeValueAsString(dsl);

         assertNotNull("json should not be empty.", json);
         assertEquals("{\"wildcard\":{\"city\":\"Chand*\"}}", json);

      }
      catch (Exception e)
      {
         log.debug("derp! ", e);
         fail();
      }
   }

   @Test
   public void endsWith()
   {
      try
      {
         QueryDsl dsl = new RQL("elastic").toQueryDsl(split("ew(city,andler)"));

         assertNull(dsl.getBool());
         assertNull(dsl.getNested());
         assertNull(dsl.getNestedPath());
         assertNull(dsl.getRange());
         assertNull(dsl.getTerm());
         assertNotNull(dsl.getWildcard());

         assertEquals("city", dsl.getWildcard().getName());
         assertEquals("*andler", dsl.getWildcard().getValue());
         assertNull(dsl.getWildcard().getNestedPath());

         ObjectMapper mapper = new ObjectMapper();
         String json = mapper.writeValueAsString(dsl);

         assertNotNull("json should not be empty.", json);
         assertEquals("{\"wildcard\":{\"city\":\"*andler\"}}", json);

      }
      catch (Exception e)
      {
         log.debug("derp! ", e);
         fail();
      }
   }

   @Test
   public void contains()
   {
      try
      {
         QueryDsl dsl = new RQL("elastic").toQueryDsl(split("w(city,andl)"));

         assertNull(dsl.getBool());
         assertNull(dsl.getNested());
         assertNull(dsl.getNestedPath());
         assertNull(dsl.getRange());
         assertNull(dsl.getTerm());
         assertNotNull(dsl.getWildcard());

         assertEquals("city", dsl.getWildcard().getName());
         assertEquals("*andl*", dsl.getWildcard().getValue());
         assertNull(dsl.getWildcard().getNestedPath());

         ObjectMapper mapper = new ObjectMapper();
         String json = mapper.writeValueAsString(dsl);

         assertNotNull("json should not be empty.", json);
         assertEquals("{\"wildcard\":{\"city\":\"*andl*\"}}", json);

      }
      catch (Exception e)
      {
         log.debug("derp! ", e);
         fail();
      }
   }

   @Test
   public void empty()
   {
      try
      {
         QueryDsl dsl = new RQL("elastic").toQueryDsl(split("emp(state)"));

         // TODO update this test

         ObjectMapper mapper = new ObjectMapper();
         String json = mapper.writeValueAsString(dsl);

         assertNotNull("json should not be empty.", json);
         assertEquals("{\"bool\":{\"should\":[{\"term\":{\"state\":\"\"}},{\"bool\":{\"must_not\":[{\"exists\":{\"field\":\"state\"}}]}}]}}", json);

      }
      catch (Exception e)
      {
         log.debug("derp! ", e);
         fail();
      }
   }

   @Test
   public void fuzzySearch()
   {
      try
      {
         QueryDsl dsl = new RQL("elastic").toQueryDsl(split("search(keywords,Tim)"));

         // TODO update this test

         ObjectMapper mapper = new ObjectMapper();
         String json = mapper.writeValueAsString(dsl);

         assertNotNull("json should not be empty.", json);
         assertEquals("{\"fuzzy\":{\"keywords\":\"Tim\"}}", json);

      }
      catch (Exception e)
      {
         log.debug("derp! ", e);
         fail();
      }
   }

   @Test
   public void notEmpty()
   {
      try
      {
         QueryDsl dsl = new RQL("elastic").toQueryDsl(split("nemp(state)"));

         // TODO update this test

         ObjectMapper mapper = new ObjectMapper();
         String json = mapper.writeValueAsString(dsl);

         assertNotNull("json should not be empty.", json);
         assertEquals("{\"bool\":{\"must\":[{\"bool\":{\"must_not\":[{\"term\":{\"state\":\"\"}}]}},{\"bool\":{\"must\":[{\"exists\":{\"field\":\"state\"}}]}}]}}", json);

      }
      catch (Exception e)
      {
         log.debug("derp! ", e);
         fail();
      }
   }

   @Test
   public void simpleSort()
   {
      try
      {
         QueryDsl dsl = new RQL("elastic").toQueryDsl(split("order=test"));

         ObjectMapper mapper = new ObjectMapper();
         String json = mapper.writeValueAsString(dsl.toDslMap());

         assertNotNull("json should not be empty.", json);
         assertTrue(json.contains("\"sort\":[{\"test\":\"ASC\"}]"));

         io.rcktapp.rql.elasticsearch.Order order = dsl.getOrder();
         assertNotNull(order);
         assertEquals(1, order.getOrderList().size());
         Map<String, String> orderMap = order.getOrderList().get(0);
         assertEquals("ASC", orderMap.get("test"));
      }
      catch (Exception e)
      {
         log.debug("derp! ", e);
         fail();
      }
   }

   @Test
   public void multiSort()
   {
      try
      {
         QueryDsl dsl = new RQL("elastic").toQueryDsl(split("sort=test,-test2,+test3"));

         ObjectMapper mapper = new ObjectMapper();
         String json = mapper.writeValueAsString(dsl.toDslMap());

         assertNotNull("json should not be empty.", json);
         assertTrue(json.contains("\"sort\":[{\"test\":\"ASC\"},{\"test2\":\"DESC\"},{\"test3\":\"ASC\"}]"));

         io.rcktapp.rql.elasticsearch.Order order = dsl.getOrder();
         assertNotNull(order);
         assertEquals(3, order.getOrderList().size());
         Map<String, String> orderMap = order.getOrderList().get(0);
         assertEquals("ASC", orderMap.get("test"));
         orderMap = order.getOrderList().get(1);
         assertEquals("DESC", orderMap.get("test2"));
         orderMap = order.getOrderList().get(2);
         assertEquals("ASC", orderMap.get("test3"));
      }
      catch (Exception e)
      {
         log.debug("derp! ", e);
         fail();
      }
   }

   @Test
   public void testGt()
   {
      try
      {
         QueryDsl dsl = new RQL("elastic").toQueryDsl(split("gt(hispanicRank,25)"));
         ObjectMapper mapper = new ObjectMapper();

         String json = mapper.writeValueAsString(dsl);

         assertNotNull("json should not be empty.", json);
         assertEquals("{\"range\":{\"hispanicRank\":{\"gt\":\"25\"}}}", json);

         assertNull(dsl.getBool());
         assertNull(dsl.getWildcard());
         assertNull(dsl.getTerm());
         assertNotNull(dsl.getRange());
         assertEquals("hispanicRank", (dsl.getRange().getName()));
         assertEquals("25", (dsl.getRange().getGt()));
         assertNull((dsl.getRange().getGte()));
         assertNull((dsl.getRange().getLte()));
         assertNull((dsl.getRange().getLt()));
      }
      catch (Exception e)
      {
         log.debug("derp! ", e);
         fail();
      }
   }
   
   @Test
   public void testGte()
   {
      try
      {
         QueryDsl dsl = new RQL("elastic").toQueryDsl(split("ge(hispanicRank,25)"));
         ObjectMapper mapper = new ObjectMapper();

         String json = mapper.writeValueAsString(dsl);

         assertNotNull("json should not be empty.", json);
         assertEquals("{\"range\":{\"hispanicRank\":{\"gte\":\"25\"}}}", json);

         assertNull(dsl.getBool());
         assertNull(dsl.getWildcard());
         assertNull(dsl.getTerm());
         assertNotNull(dsl.getRange());
         assertEquals("hispanicRank", (dsl.getRange().getName()));
         assertEquals("25", (dsl.getRange().getGte()));
         assertNull((dsl.getRange().getGt()));
         assertNull((dsl.getRange().getLte()));
         assertNull((dsl.getRange().getLt()));
      }
      catch (Exception e)
      {
         log.debug("derp! ", e);
         fail();
      }
   }

   @Test
   public void testNestedGt()
   {
      try
      {
         QueryDsl dsl = new RQL("elastic").toQueryDsl(split("gt(players.registerNum,3)"));
         ObjectMapper mapper = new ObjectMapper();

         String json = mapper.writeValueAsString(dsl);

         assertNotNull("json should not be empty.", json);
         assertEquals("{\"nested\":{\"path\":\"players\",\"query\":{\"range\":{\"players.registerNum\":{\"gt\":\"3\"}}}}}", json);

         assertNull(dsl.getBool());
         assertNull(dsl.getWildcard());
         assertNull(dsl.getTerm());
         assertNull(dsl.getRange());
         assertNotNull(dsl.getNested());
      }
      catch (Exception e)
      {
         log.debug("derp! ", e);
         fail();
      }
   }

   @Test
   public void testEq1()
   {
      try
      {
         QueryDsl dsl = new RQL("elastic").toQueryDsl(split("eq(city,CHANDLER)"));
         ObjectMapper mapper = new ObjectMapper();

         String json = mapper.writeValueAsString(dsl);

         assertNotNull("json should not be empty.", json);
         assertEquals("{\"term\":{\"city\":\"CHANDLER\"}}", json);

         assertNull(dsl.getRange());
         assertNull(dsl.getBool());
         assertNull(dsl.getWildcard());
         assertNotNull(dsl.getTerm());
         assertEquals("city", (dsl.getTerm().getName()));
         assertEquals("CHANDLER", (dsl.getTerm().getValueList().get(0)));
      }
      catch (Exception e)
      {
         log.debug("derp! ", e);
         fail();
      }
   }

   @Test
   public void testEq2()
   {
      try
      {
         QueryDsl dsl = new RQL("elastic").toQueryDsl(split("eq(uninstalled,true)"));
         ObjectMapper mapper = new ObjectMapper();

         assertNull(dsl.getRange());
         assertNull(dsl.getBool());
         assertNull(dsl.getWildcard());
         assertNotNull(dsl.getTerm());
         assertEquals("uninstalled", (dsl.getTerm().getName()));
         assertEquals("true", (dsl.getTerm().getValueList().get(0)));

         String json = mapper.writeValueAsString(dsl);

         assertNotNull("json should not be empty.", json);
         assertEquals("{\"term\":{\"uninstalled\":\"true\"}}", json);

      }
      catch (Exception e)
      {
         log.debug("derp! ", e);
         fail();
      }
   }

   @Test
   public void testNotEq()
   {
      try
      {
         QueryDsl dsl = new RQL("elastic").toQueryDsl(split("ne(hispanicRank,25)"));
         ObjectMapper mapper = new ObjectMapper();

         String json = mapper.writeValueAsString(dsl);

         assertNotNull("json should not be empty.", json);
         assertEquals("{\"bool\":{\"must_not\":[{\"term\":{\"hispanicRank\":\"25\"}}]}}", json);

         assertNull(dsl.getRange());
         assertNull(dsl.getWildcard());
         assertNull(dsl.getTerm());

         assertNotNull(dsl.getBool());
         assertNull(dsl.getBool().getFilter());
         assertNull(dsl.getBool().getMust());
         assertNull(dsl.getBool().getShould());
         assertNotNull(dsl.getBool().getMustNot());

         assertEquals(1, (dsl.getBool().getMustNot().size()));
         assertEquals(Term.class, (dsl.getBool().getMustNot().get(0).getClass()));
         assertEquals("hispanicRank", (((Term) dsl.getBool().getMustNot().get(0))).getName());
         assertEquals("25", (((Term) dsl.getBool().getMustNot().get(0))).getValueList().get(0));

      }
      catch (Exception e)
      {
         log.debug("derp! ", e);
         fail();
      }
   }

   @Test
   public void testCompoundNotEq()
   {
      try
      {
         QueryDsl dsl = new RQL("elastic").toQueryDsl(split("and(ne(hispanicRank,95),gt(hispanicRank,93))"));
         ObjectMapper mapper = new ObjectMapper();

         String json = mapper.writeValueAsString(dsl);

         assertNotNull("json should not be empty.", json);
         assertEquals("{\"bool\":{\"filter\":[{\"range\":{\"hispanicRank\":{\"gt\":\"93\"}}}],\"must_not\":[{\"term\":{\"hispanicRank\":\"95\"}}]}}", json);

         assertNull(dsl.getRange());
         assertNull(dsl.getWildcard());
         assertNull(dsl.getTerm());

         assertNotNull(dsl.getBool());
         assertNull(dsl.getBool().getMust());
         assertNull(dsl.getBool().getShould());
         assertNotNull(dsl.getBool().getMustNot());
         assertNotNull(dsl.getBool().getFilter());

         assertEquals(1, (dsl.getBool().getMustNot().size()));

         assertEquals(Term.class, (dsl.getBool().getMustNot().get(0).getClass()));
         assertEquals("hispanicRank", ((Term) dsl.getBool().getMustNot().get(0)).getName());
         assertEquals("95", ((Term) dsl.getBool().getMustNot().get(0)).getValueList().get(0));
         assertEquals(1, (dsl.getBool().getFilter().size()));
         assertEquals(Range.class, (dsl.getBool().getFilter().get(0).getClass()));
         assertEquals("hispanicRank", ((Range) dsl.getBool().getFilter().get(0)).getName());
         assertEquals("93", ((Range) dsl.getBool().getFilter().get(0)).getGt());

      }
      catch (Exception e)
      {
         log.debug("derp! ", e);
         fail();
      }
   }

   @Test
   public void simpleAnd()
   {
      try
      {
         QueryDsl dsl = new RQL("elastic").toQueryDsl(split("ne(hispanicRank,95)&gt(hispanicRank,93)"));
         ObjectMapper mapper = new ObjectMapper();

         String json = mapper.writeValueAsString(dsl);

         assertNotNull("json should not be empty.", json);
         assertEquals("{\"bool\":{\"filter\":[{\"range\":{\"hispanicRank\":{\"gt\":\"93\"}}}],\"must_not\":[{\"term\":{\"hispanicRank\":\"95\"}}]}}", json);

         assertNull(dsl.getRange());
         assertNull(dsl.getWildcard());
         assertNull(dsl.getTerm());

         assertNotNull(dsl.getBool());
         assertNull(dsl.getBool().getMust());
         assertNull(dsl.getBool().getShould());
         assertNotNull(dsl.getBool().getMustNot());
         assertNotNull(dsl.getBool().getFilter());

         assertEquals(1, (dsl.getBool().getMustNot().size()));

         assertEquals(Term.class, (dsl.getBool().getMustNot().get(0).getClass()));
         assertEquals("hispanicRank", ((Term) dsl.getBool().getMustNot().get(0)).getName());
         assertEquals("95", ((Term) dsl.getBool().getMustNot().get(0)).getValueList().get(0));

         assertEquals(1, (dsl.getBool().getFilter().size()));
         assertEquals(Range.class, (dsl.getBool().getFilter().get(0).getClass()));
         assertEquals("hispanicRank", ((Range) dsl.getBool().getFilter().get(0)).getName());
         assertEquals("93", ((Range) dsl.getBool().getFilter().get(0)).getGt());

      }
      catch (Exception e)
      {
         log.debug("derp! ", e);
         fail();
      }
   }

   @Test
   public void orFunction()
   {
      try
      {
         QueryDsl dsl = new RQL("elastic").toQueryDsl(split("or(eq(name,fwqa),eq(name,cheetos)"));
         ObjectMapper mapper = new ObjectMapper();

         String json = mapper.writeValueAsString(dsl);

         assertNotNull("json should not be empty.", json);
         assertEquals("{\"bool\":{\"should\":[{\"term\":{\"name\":\"fwqa\"}},{\"term\":{\"name\":\"cheetos\"}}]}}", json);

         assertNull(dsl.getRange());
         assertNull(dsl.getWildcard());
         assertNull(dsl.getTerm());
         assertNotNull(dsl.getBool());

         assertNull(dsl.getBool().getMust());
         assertNull(dsl.getBool().getMustNot());
         assertNull(dsl.getBool().getFilter());
         assertNotNull(dsl.getBool().getShould());

         assertEquals(2, (dsl.getBool().getShould().size()));

         assertEquals(Term.class, (dsl.getBool().getShould().get(0).getClass()));
         assertEquals("name", ((Term) dsl.getBool().getShould().get(0)).getName());
         assertEquals("fwqa", ((Term) dsl.getBool().getShould().get(0)).getValueList().get(0));

         assertEquals(Term.class, (dsl.getBool().getShould().get(1).getClass()));
         assertEquals("name", ((Term) dsl.getBool().getShould().get(1)).getName());
         assertEquals("cheetos", ((Term) dsl.getBool().getShould().get(1)).getValueList().get(0));

      }
      catch (Exception e)
      {
         log.debug("derp! ", e);
         fail();
      }
   }

   @Test
   public void inFunction()
   {
      try
      {
         QueryDsl dsl = new RQL("elastic").toQueryDsl(split("in(city,Chicago,Tempe,Chandler)"));
         ObjectMapper mapper = new ObjectMapper();

         String json = mapper.writeValueAsString(dsl);

         assertNotNull("json should not be empty.", json);
         assertEquals("{\"terms\":{\"city\":[\"Chicago\",\"Tempe\",\"Chandler\"]}}", json);

         assertNull(dsl.getRange());
         assertNull(dsl.getWildcard());
         assertNull(dsl.getTerm());
         assertNull(dsl.getBool());
         assertNotNull(dsl.getTerms());

         assertEquals("city", dsl.getTerms().getName());
         assertEquals(3, dsl.getTerms().getValueList().size());

         assertEquals("Chicago", dsl.getTerms().getValueList().get(0));
         assertEquals("Tempe", dsl.getTerms().getValueList().get(1));
         assertEquals("Chandler", dsl.getTerms().getValueList().get(2));

      }
      catch (Exception e)
      {
         log.debug("derp! ", e);
         fail();
      }
   }

   @Test
   public void outFunction()
   {
      try
      {
         QueryDsl dsl = new RQL("elastic").toQueryDsl(split("out(city,Chicago,Tempe,Chandler)"));
         ObjectMapper mapper = new ObjectMapper();

         String json = mapper.writeValueAsString(dsl);

         assertNotNull("json should not be empty.", json);
         assertEquals("{\"bool\":{\"must_not\":[{\"terms\":{\"city\":[\"Chicago\",\"Tempe\",\"Chandler\"]}}]}}", json);

         assertNull(dsl.getRange());
         assertNull(dsl.getWildcard());
         assertNull(dsl.getTerm());
         assertNull(dsl.getTerms());
         assertNotNull(dsl.getBool());

         assertNull(dsl.getBool().getFilter());
         assertNull(dsl.getBool().getMust());
         assertNull(dsl.getBool().getShould());
         assertNotNull(dsl.getBool().getMustNot());

         assertEquals(1, dsl.getBool().getMustNot().size());
         assertEquals(Term.class, dsl.getBool().getMustNot().get(0).getClass());
         assertEquals("city", ((Term) dsl.getBool().getMustNot().get(0)).getName());
         assertEquals(3, ((Term) dsl.getBool().getMustNot().get(0)).getValueList().size());

         assertEquals("Chicago", ((Term) dsl.getBool().getMustNot().get(0)).getValueList().get(0));
         assertEquals("Tempe", ((Term) dsl.getBool().getMustNot().get(0)).getValueList().get(1));
         assertEquals("Chandler", ((Term) dsl.getBool().getMustNot().get(0)).getValueList().get(2));

      }
      catch (Exception e)
      {
         log.debug("derp! ", e);
         fail();
      }
   }

   @Test
   public void complexAnd()
   {
      try
      {
         QueryDsl dsl = new RQL("elastic").toQueryDsl(split("eq(locationCode,270*)&eq(city,Chandler)&eq(address1,*McQueen*)"));

         assertNull(dsl.getWildcard());
         assertNull(dsl.getTerm());
         assertNull(dsl.getRange());
         assertNull(dsl.getNested());
         assertNotNull(dsl.getBool());

         assertEquals(3, dsl.getBool().getFilter().size());

         ObjectMapper mapper = new ObjectMapper();

         String json = mapper.writeValueAsString(dsl);

         assertNotNull("json should not be empty.", json);
         assertEquals("{\"bool\":{\"filter\":[{\"wildcard\":{\"locationCode\":\"270*\"}},{\"term\":{\"city\":\"Chandler\"}},{\"wildcard\":{\"address1\":\"*McQueen*\"}}]}}", json);
      }
      catch (Exception e)
      {
         log.debug("derp! ", e);
         fail();
      }
   }
   
   @Test
   public void complexSearch1()
   {
      try
      {
         QueryDsl dsl = new RQL("elastic").toQueryDsl(split("and(search(keywords,test),search(keywords,matt))"));

         assertNull(dsl.getWildcard());
         assertNull(dsl.getTerm());
         assertNull(dsl.getRange());
         assertNull(dsl.getNested());
         assertNotNull(dsl.getBool());

         ObjectMapper mapper = new ObjectMapper();

         String json = mapper.writeValueAsString(dsl);

         assertNotNull("json should not be empty.", json);
         assertEquals("{\"bool\":{\"filter\":[{\"fuzzy\":{\"keywords\":\"test\"}},{\"fuzzy\":{\"keywords\":\"matt\"}}]}}", json);
      }
      catch (Exception e)
      {
         log.debug("derp! ", e);
         fail();
      }
   }
   
   @Test
   public void complexSearch2()
   {
      try
      {
         QueryDsl dsl = new RQL("elastic").toQueryDsl(split("search(keywords,test)&search(keywords,matt)"));

         assertNull(dsl.getWildcard());
         assertNull(dsl.getTerm());
         assertNull(dsl.getRange());
         assertNull(dsl.getNested());
         assertNotNull(dsl.getBool());

         ObjectMapper mapper = new ObjectMapper();

         String json = mapper.writeValueAsString(dsl);

         assertNotNull("json should not be empty.", json);
         assertEquals("{\"bool\":{\"filter\":[{\"fuzzy\":{\"keywords\":\"test\"}},{\"fuzzy\":{\"keywords\":\"matt\"}}]}}", json);
      }
      catch (Exception e)
      {
         log.debug("derp! ", e);
         fail();
      }
   }
   
   @Test
   public void complexOr()
   {
      try
      {
         QueryDsl dsl = new RQL("elastic").toQueryDsl(split("or(eq(id,3),eq(name,*POST*))"));

         assertNull(dsl.getWildcard());
         assertNull(dsl.getTerm());
         assertNull(dsl.getRange());
         assertNull(dsl.getNested());
         assertNotNull(dsl.getBool());

         ObjectMapper mapper = new ObjectMapper();

         String json = mapper.writeValueAsString(dsl);

         assertNotNull("json should not be empty.", json);
         assertEquals("{\"bool\":{\"should\":[{\"term\":{\"id\":\"3\"}},{\"wildcard\":{\"name\":\"*POST*\"}}]}}", json);
      }
      catch (Exception e)
      {
         log.debug("derp! ", e);
         fail();
      }
   }
   
   

   @Test
   public void testWildcard()
   {
      try
      {
         QueryDsl dsl = new RQL("elastic").toQueryDsl(split("eq(address1,*GILBERT*)"));
         ObjectMapper mapper = new ObjectMapper();

         String json = mapper.writeValueAsString(dsl);

         assertNotNull("json should not be empty.", json);
         assertEquals("{\"wildcard\":{\"address1\":\"*GILBERT*\"}}", json);

         assertNull(dsl.getRange());
         assertNull(dsl.getTerm());
         assertNull(dsl.getBool());
         assertNotNull(dsl.getWildcard());
         assertEquals("address1", (dsl.getWildcard().getName()));
         assertEquals("*GILBERT*", (dsl.getWildcard().getValue()));

      }
      catch (Exception e)
      {
         log.debug("derp! ", e);
         fail();
      }
   }

   @Test
   public void testAndTerms()
   {
      try
      {
         QueryDsl dsl = new RQL("elastic").toQueryDsl(split("and(eq(locationCode,9187),eq(city,CHANDLER))"));
         ObjectMapper mapper = new ObjectMapper();

         String json = mapper.writeValueAsString(dsl);

         assertNotNull("json should not be empty.", json);
         assertEquals("{\"bool\":{\"filter\":[{\"term\":{\"locationCode\":\"9187\"}},{\"term\":{\"city\":\"CHANDLER\"}}]}}", json);

         assertNull(dsl.getRange());
         assertNull(dsl.getTerm());
         assertNull(dsl.getWildcard());
         assertNotNull(dsl.getBool());
         assertTrue(dsl.getBool().getFilter().size() == 2);
         assertTrue(dsl.getBool().getFilter().get(0) instanceof Term);
         assertEquals("locationCode", ((Term) dsl.getBool().getFilter().get(0)).getName());
         assertEquals("9187", ((Term) dsl.getBool().getFilter().get(0)).getValueList().get(0));
         assertTrue(dsl.getBool().getFilter().get(1) instanceof Term);
         assertEquals("city", ((Term) dsl.getBool().getFilter().get(1)).getName());
         assertEquals("CHANDLER", ((Term) dsl.getBool().getFilter().get(1)).getValueList().get(0));
      }
      catch (Exception e)
      {
         log.debug("derp! ", e);
         fail();
      }
   }

   @Test
   public void testAndWildcard()
   {
      try
      {
         QueryDsl dsl = new RQL("elastic").toQueryDsl(split("and(eq(address1,*GILBERT*),eq(city,CHANDLER))"));
         ObjectMapper mapper = new ObjectMapper();

         String json = mapper.writeValueAsString(dsl);

         assertNotNull("json should not be empty.", json);
         assertEquals("{\"bool\":{\"filter\":[{\"wildcard\":{\"address1\":\"*GILBERT*\"}},{\"term\":{\"city\":\"CHANDLER\"}}]}}", json);
         assertNull(dsl.getRange());
         assertNull(dsl.getTerm());
         assertNull(dsl.getWildcard());
         assertNotNull(dsl.getBool());
         assertTrue(dsl.getBool().getFilter().size() == 2);
         assertTrue(dsl.getBool().getFilter().get(0) instanceof Wildcard);
         assertEquals("address1", ((Wildcard) dsl.getBool().getFilter().get(0)).getName());
         assertEquals("*GILBERT*", ((Wildcard) dsl.getBool().getFilter().get(0)).getValue());
         assertTrue(dsl.getBool().getFilter().get(1) instanceof Term);
         assertEquals("city", ((Term) dsl.getBool().getFilter().get(1)).getName());
         assertEquals("CHANDLER", ((Term) dsl.getBool().getFilter().get(1)).getValueList().get(0));
      }
      catch (Exception e)
      {
         log.debug("derp! ", e);
         fail();
      }
   }

   @Test
   public void smallCompoundQuery()
   {
      try
      {
         QueryDsl dsl = new RQL("elastic").toQueryDsl(split("and(gt(hispanicRank,25),le(hispanicRank,40))"));
         ObjectMapper mapper = new ObjectMapper();

         String json = mapper.writeValueAsString(dsl);

         assertNotNull("json should not be empty.", json);
         assertEquals("{\"bool\":{\"filter\":[{\"range\":{\"hispanicRank\":{\"gt\":\"25\"}}},{\"range\":{\"hispanicRank\":{\"le\":\"40\"}}}]}}", json);
         assertNull(dsl.getRange());
         assertNull(dsl.getTerm());
         assertNull(dsl.getWildcard());
         assertNotNull(dsl.getBool());
         assertTrue(dsl.getBool().getFilter().size() == 2);
         assertTrue(dsl.getBool().getFilter().get(0) instanceof Range);
         assertEquals("hispanicRank", ((Range) dsl.getBool().getFilter().get(0)).getName());
         assertNull(((Range) dsl.getBool().getFilter().get(0)).getGte());
         assertNull(((Range) dsl.getBool().getFilter().get(0)).getLte());
         assertNull(((Range) dsl.getBool().getFilter().get(0)).getLt());
         assertEquals("25", ((Range) dsl.getBool().getFilter().get(0)).getGt());
      }
      catch (Exception e)
      {
         log.debug("derp! ", e);
         fail();
      }
   }

   @Test
   public void largeCompoundQuery()
   {
      try
      {
         QueryDsl dsl = new RQL("elastic").toQueryDsl(split("and(and(eq(locationCode,270*),eq(city,Chandler)),and(eq(address1,*McQueen*)))"));
         ObjectMapper mapper = new ObjectMapper();

         String json = mapper.writeValueAsString(dsl);

         assertNotNull("json should not be empty.", json);
         assertEquals("{\"bool\":{\"filter\":[{\"bool\":{\"filter\":[{\"wildcard\":{\"locationCode\":\"270*\"}},{\"term\":{\"city\":\"Chandler\"}}]}},{\"bool\":{\"filter\":[{\"wildcard\":{\"address1\":\"*McQueen*\"}}]}}]}}", json);
      }
      catch (Exception e)
      {
         log.debug("derp! ", e);
         fail();
      }
   }

   @Test
   public void simpleNestedQuery()
   {
      try
      {
         QueryDsl dsl = new RQL("elastic").toQueryDsl(split("gt(players.registerNum,5)"));
         ObjectMapper mapper = new ObjectMapper();

         assertNull(dsl.getRange());
         assertNull(dsl.getTerm());
         assertNull(dsl.getWildcard());
         assertNull(dsl.getBool());
         assertNull(dsl.getNestedPath());
         assertNotNull(dsl.getNested());
         NestedQuery nested = (NestedQuery) dsl.getNested();
         assertEquals("players", nested.getPath());
         assertNotNull(nested.getQuery());

         String json = mapper.writeValueAsString(dsl);

         assertNotNull("json should not be empty.", json);
         assertEquals("{\"nested\":{\"path\":\"players\",\"query\":{\"range\":{\"players.registerNum\":{\"gt\":\"5\"}}}}}", json);

      }
      catch (Exception e)
      {
         log.debug("derp! ", e);
         fail();
      }
   }

   @Test
   public void compoundNestedQuery1()
   {
      try
      {
         QueryDsl dsl = new RQL("elastic").toQueryDsl(split("and(gt(players.registerNum,5),eq(city,Chandler))"));
         ObjectMapper mapper = new ObjectMapper();

         assertNull(dsl.getRange());
         assertNull(dsl.getTerm());
         assertNull(dsl.getWildcard());
         assertNull(dsl.getNested());
         assertNull(dsl.getNestedPath());
         assertNotNull(dsl.getBool());

         BoolQuery bool = dsl.getBool();
         assertNull(bool.getNestedPath());
         assertNull(bool.getMust());
         assertNull(bool.getMustNot());
         assertNull(bool.getShould());
         assertNotNull(bool.getFilter());
         assertEquals(2, bool.getFilter().size());
         assertEquals(NestedQuery.class, bool.getFilter().get(0).getClass());
         assertEquals(Term.class, bool.getFilter().get(1).getClass());

         String json = mapper.writeValueAsString(dsl);

         assertNotNull("json should not be empty.", json);
         assertEquals("{\"bool\":{\"filter\":[{\"nested\":{\"path\":\"players\",\"query\":{\"range\":{\"players.registerNum\":{\"gt\":\"5\"}}}}},{\"term\":{\"city\":\"Chandler\"}}]}}", json);

      }
      catch (Exception e)
      {
         log.debug("derp! ", e);
         fail();
      }
   }

   @Test
   public void compoundNestedQuery2()
   {
      try
      {
         QueryDsl dsl = new RQL("elastic").toQueryDsl(split("and(and(eq(players.deleted,true),eq(city,PHOENIX)),and(eq(address1,*VALLEY*)))"));
         ObjectMapper mapper = new ObjectMapper();

         assertNull(dsl.getRange());
         assertNull(dsl.getTerm());
         assertNull(dsl.getWildcard());
         assertNull(dsl.getNested());
         assertNull(dsl.getNestedPath());
         assertNotNull(dsl.getBool());

         BoolQuery bool = dsl.getBool();
         assertNull(bool.getNestedPath());
         assertNull(bool.getMust());
         assertNull(bool.getMustNot());
         assertNull(bool.getShould());
         assertNotNull(bool.getFilter());
         assertEquals(2, bool.getFilter().size());
         assertEquals(BoolQuery.class, bool.getFilter().get(0).getClass());
         assertEquals(BoolQuery.class, bool.getFilter().get(1).getClass());

         String json = mapper.writeValueAsString(dsl);

         assertNotNull("json should not be empty.", json);
         assertEquals("{\"bool\":{\"filter\":[{\"bool\":{\"filter\":[{\"nested\":{\"path\":\"players\",\"query\":{\"term\":{\"players.deleted\":\"true\"}}}},{\"term\":{\"city\":\"PHOENIX\"}}]}},{\"bool\":{\"filter\":[{\"wildcard\":{\"address1\":\"*VALLEY*\"}}]}}]}}", json);

      }
      catch (Exception e)
      {
         log.debug("derp! ", e);
         fail();
      }
   }

   static LinkedHashMap split(String queryString)
   {
      LinkedHashMap map = new LinkedHashMap();

      String[] terms = queryString.split("&");
      for (String term : terms)
      {
         int eqIdx = term.indexOf('=');
         if (eqIdx < 0)
         {
            map.put(term, null);
         }
         else
         {
            String value = term.substring(eqIdx + 1, term.length());
            term = term.substring(0, eqIdx);
            map.put(term, value);
         }
      }
      return map;
   }

}
