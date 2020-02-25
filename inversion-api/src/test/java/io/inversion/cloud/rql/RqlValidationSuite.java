/*
 * Copyright (c) 2015-2020 Rocket Partners, LLC
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
package io.inversion.cloud.rql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;

import io.inversion.cloud.model.Collection;
import io.inversion.cloud.model.Db;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.model.Results;
import io.inversion.cloud.service.Engine;
import io.inversion.cloud.utils.Utils;

public class RqlValidationSuite
{
   //   //select
   //   withFunctions("as", "includes", "excludes", "distinct", "count", "sum", "min", "max", "if", "aggregate", "function", "countascol", "rowcount");
   //
   //
   //   //join
   //   
   //   //where
   //   withFunctions("_key", "if", "and", "or", "not", "eq", "ne", "n", "nn", "lt", "le", "gt", "ge", "like", "sw", "ew",  "in", "out", , "w", "wo", "emp", "nemp");
   //   
   //   //group
   //  
   //   withFunctions("offset", "limit", "page", "pageNum", "pageSize", "after");
   //   
   //   withFunctions("order", "sort");

   Map<String, String>     tests      = new LinkedHashMap();
   Map<String, String>     results    = new HashMap();

   Map<String, Collection> tables     = new HashMap();
   Db                      db         = null;
   String                  queryClass = null;

   public RqlValidationSuite(String queryClass, Db db, Collection... tables)
   {
      withQueryClass(queryClass);
      withDb(db);
      withTables(tables);

      withTables(new Collection("orders")//s
                                         .withProperty("orderId", "VARCHAR")//
                                         .withProperty("customerId", "INTEGER")//
                                         .withProperty("employeeId", "DATETIME")//
                                         .withProperty("orderDate", "DATETIME")//
                                         .withProperty("requiredDate", "DATETIME")//
                                         .withProperty("shippedDate", "DATETIME")//
                                         .withProperty("shipVia", "INTEGER")//
                                         .withProperty("freight", "DECIMAL")//
                                         .withProperty("shipName", "VARCHAR")//
                                         .withProperty("shipAddress", "VARCHAR")//
                                         .withProperty("shipCity", "VARCHAR")//
                                         .withProperty("shipRegion", "VARCHAR")//
                                         .withProperty("shipPostalCode", "VARCHAR")//
                                         .withProperty("shipCountry", "VARCHAR")//
                                         .withIndex("PK_Orders", "primary", true, "orderId"));

      withTest("eq", "orders?eq(orderID, 10248)&eq(shipCountry,France)");
      withTest("ne", "orders?ne(shipCountry,France)");

      withTest("n", "orders?n(shipRegion)");
      withTest("nn", "orders?nn(shipRegion)");
      withTest("emp", "orders?emp(shipRegion)");
      withTest("nemp", "orders?nemp(shipRegion)");

      withTest("likeMiddle", "orders?like(shipCountry,F*ance)");
      withTest("likeStartsWith", "orders?like(shipCountry,Franc*)");
      withTest("likeEndsWith", "orders?like(shipCountry,*ance)");
      withTest("sw", "orders?sw(shipCountry,Franc)");
      withTest("ew", "orders?ew(shipCountry,nce)");
      withTest("w", "orders?w(shipCountry,ance)");
      withTest("wo", "orders?wo(shipCountry,ance)");

      withTest("lt", "orders?lt(freight,10)");
      withTest("le", "orders?le(freight,10)");
      withTest("gt", "orders?gt(freight,3.67)");
      withTest("ge", "orders?ge(freight,3.67)");
      withTest("in", "orders?in(shipCity,Reims,Charleroi)");
      withTest("out", "orders?out(shipCity,Reims,Charleroi)");

      withTest("and", "orders?and(eq(orderID, 10248),eq(shipCountry,France))");
      withTest("or", "orders?or(eq(shipCity, Reims),eq(shipCity,Charleroi))");
      withTest("not", "orders?not(or(eq(shipCity, Reims),eq(shipCity,Charleroi)))");

      withTest("as", "orders?as(orderid,order_identifier)");
      withTest("includes", "orders?includes(shipCountry,shipCity)");
      withTest("distinct", "orders?distinct&includes=shipCountry");

      withTest("count1", "orders?count(*)");
      withTest("count2", "orders?count(1)");
      withTest("count3", "orders?count(shipRegion)");//in the data some shipRegions are null so this would be fewer than count(*) or count(1)
      withTest("countAs", "orders?as(count(*),countOrders)");
      //-- this is commented out because Select.java translates this to as(count(shipCountry), numRows) which is tested above
      //-- withTest("countAs2", "orders?count(shipCountry,numRows)"); //

      withTest("sum", "orders?sum(freight)");
      withTest("sumAs", "orders?as(sum(freight),'Sum Freight')");

      //-- this is commented out because Select.java translates this to as(sum(freight), sumFreight) which is tested above
      //-- withTest("sumAs", "orders?sum(freight,sumFreight)");

      withTest("sumIf", "orders?as(sum(if(eq(shipCountry,France),1,0)), 'French Orders')");
      withTest("min", "orders?min(freight)");
      withTest("max", "orders?max(freight)");
      //withTest("if", "orders?");
      withTest("groupCount", "orders?group(shipCountry)&as(count(*),countryCount)&includes=shipCountry,countryCount");

      //      withTest("aggregate", "orders?");
      //      withTest("function", "orders?");
      //      withTest("countascol", "orders?");
      //      withTest("rowcount", "orders?");

      withTest("offset", "orders?offset=3");
      withTest("limit", "orders?limit=7");
      withTest("page", "orders?pageSize=7&page=3");
      withTest("pageNum", "orders?pageSize=7&pageNum=3");
      withTest("after", "orders?after=10248");

      withTest("sort", "orders?sort(-shipCountry,shipCity)");
      withTest("order", "orders?order(shipCountry,-shipCity)");

      //withTest("_key", "orders?");
      //withTest("join", "orders?");

   }

   public void runIntegTests(Engine engine, String urlPrefix) throws Exception
   {
      LinkedHashMap<String, String> failures = new LinkedHashMap();

      for (String testKey : tests.keySet())
      {
         String queryString = tests.get(testKey);

         if (Utils.empty(testKey) || Utils.empty(queryString))
            continue;

         System.out.println("\r\nTESTING: " + testKey + " - " + queryString);

         Response res = engine.get(urlPrefix + queryString);
         if (!verifyIntegTest(testKey, queryString, res))
         {
            failures.put(testKey, res.getDebug());
         }
      }

      if (failures.size() > 0)
      {
         System.out.println("Failed cases...");
         for (String key : failures.keySet())
         {
            String failure = null;
            failure = failures.get(key);

            int idx = failure.indexOf("\"message\"");
            if (idx > -1)
            {
               int idx2 = failure.indexOf("\n", idx);
               failure = failure.substring(idx + 12, idx2);
            }

            System.out.println("  - " + key + " - " + failure);
         }

         throw new RuntimeException("Failed...");
      }
   }

   /**
    * Override me to add additional special case validations per test.
    * By default, all integ test pass if the return code is 200 or 404
    * 
    * @param testKey
    * @param queryString
    * @param res
    * @return
    */
   protected boolean verifyIntegTest(String testKey, String queryString, Response res)
   {
      boolean success = res.hasStatus(200, 404);
      return success;
   }

   public void runUnitTests() throws Exception
   {
      LinkedHashMap<String, String> failures = new LinkedHashMap();

      for (String testKey : tests.keySet())
      {
         String queryString = tests.get(testKey);

         if (Utils.empty(testKey) || Utils.empty(queryString))
            continue;

         System.out.println("\r\nTESTING: " + testKey + " - " + queryString);

         String tableName = queryString.substring(0, queryString.indexOf("?"));
         if (tableName.indexOf("/") > 0)
            tableName = tableName.substring(tableName.lastIndexOf("/") + 1);

         Query query = (Query) Class.forName(queryClass).newInstance();
         query.withDb(db);

         String rql = queryString.substring(queryString.indexOf("?") + 1);

         //-- RestGetAction sorts the terms so we do it here
         //-- to try and mimic the order of terms in the sql
         //-- when a live call is made
         List<Term> terms = new ArrayList();
         String[] parts = rql.split("\\&");
         RqlParser parser = new RqlParser();
         for (int i = 0; i < parts.length; i++)
         {
            if (parts[i] == null || parts[i].length() == 0)
               continue;

            Term parsed = parser.parse(parts[i]);
            terms.add(parsed);
         }
         Collections.sort(terms);
         //-- end sorting

         String actual = null;
         String expected = results.get(testKey);;
         try
         {

            query.withTerms(terms);

            Collection coll = tables.get(tableName);
            if (coll == null)
               throw new Exception("Unable to find table for query: " + testKey + " - " + queryString);
            coll.withDb(db);
            query.withCollection(coll);
            query.withDryRun(true);

            if ("UNSUPPORTED".equalsIgnoreCase(expected))
            {
               System.out.println("SKIPPING UNSUPPORTED TEST CASE: " + testKey + " - " + queryString);
               continue;
            }

            if (Utils.empty(expected))
               expected = "You are missing a validation for test '" + testKey + "' with RQL '" + queryString + "'.  If this case is unsupported please call 'withResult(\"" + testKey + "\", \"UNSUPPORTED\") in your setup to declare that this validation should be skipped.";

            Results results = query.doSelect();
            actual = results.getTestQuery();
            actual = actual.substring(actual.indexOf(":") + 1).trim();
         }
         catch (Exception ex)
         {
            actual = ex.getMessage() + "";
         }

         if (!verifyUnitTest(testKey, queryString, expected, actual, query))
         {
            failures.put(testKey, actual);
         }
         else
         {
            System.out.println("PASSED");
         }
      }

      if (failures.size() > 0)
      {
         System.out.println("Failed cases...");
         for (String key : failures.keySet())
         {
            System.out.println("  - " + key + " - " + failures.get(key));
         }

         throw new RuntimeException("Failed...");
      }

      java.util.Collection<String> unknownTests = CollectionUtils.disjunction(tests.keySet(), results.keySet());
      for (String testKey : unknownTests)
      {
         System.out.println("It looks like you tried to run a test for '" + testKey + "' but that is an unknown test.");
      }
      if (unknownTests.size() > 0)
         throw new Exception("Unknown tests: " + unknownTests);

   }

   /**
    * Override me to add additional special case validations.
    * 
    * @param testKey
    * @param queryString
    * @param expected
    * @param actual
    * @param query
    * @return
    */
   protected boolean verifyUnitTest(String testKey, String queryString, String expected, String actual, Query query)
   {
      return Utils.testCompare(expected, actual);
   }

   public RqlValidationSuite withTables(Collection... tables)
   {
      for (int i = 0; tables != null && i < tables.length; i++)
      {
         Collection t = tables[i];
         if (t != null)
            this.tables.put(t.getTableName(), t);
      }

      return this;
   }

   public RqlValidationSuite withTest(String testKey, String testRql)
   {
      tests.put(testKey, testRql);
      return this;
   }

   public RqlValidationSuite withResult(String testKey, String queryOutput)
   {
      results.put(testKey, queryOutput);
      return this;
   }

   public RqlValidationSuite withDb(Db db)
   {
      this.db = db;
      return this;
   }

   public RqlValidationSuite withQueryClass(String queryClass)
   {
      this.queryClass = queryClass;
      return this;
   }

   public Map<String, String> getTests()
   {
      return tests;
   }

   public Map<String, String> getResults()
   {
      return results;
   }

}
