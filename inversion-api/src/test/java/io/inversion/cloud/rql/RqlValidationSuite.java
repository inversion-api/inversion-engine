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

import io.inversion.cloud.action.sql.TestSqlQuery;
import io.inversion.cloud.model.Db;
import io.inversion.cloud.model.Results;
import io.inversion.cloud.model.Table;
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

   Map<String, String> tests      = new LinkedHashMap();
   Map<String, String> results    = new HashMap();

   Map<String, Table>  tables     = new HashMap();
   Db                  db         = null;
   String              queryClass = null;

   public RqlValidationSuite(String queryClass, Db db, Table... tables)
   {
      withTables(new Table("orders")//s
                                    .withColumn("orderId", "VARCHAR")//
                                    .withColumn("customerId", "INTEGER")//
                                    .withColumn("employeeId", "DATETIME")//
                                    .withColumn("orderDate", "DATETIME")//
                                    .withColumn("requiredDate", "DATETIME")//
                                    .withColumn("shippedDate", "DATETIME")//
                                    .withColumn("shipVia", "INTEGER")//
                                    .withColumn("freight", "DECIMAL")//
                                    .withColumn("shipName", "VARCHAR")//
                                    .withColumn("shipAddress", "VARCHAR")//
                                    .withColumn("shipCity", "VARCHAR")//
                                    .withColumn("shipRegion", "VARCHAR")//
                                    .withColumn("shipPostalCode", "VARCHAR")//
                                    .withColumn("shipCountry", "VARCHAR")//
                                    .withIndex("PK_Orders", "primary", true, "orderId"));

      withTest("eq", "orders?eq(orderID, 10248)&eq(shipCountry,France)");
      withTest("ne", "orders?ne(shipCountry,France)");

      withTest("n", "orders?n(shipRegion)");
      withTest("nn", "orders?nn(shipRegion)");
      //      withTest("emp", "");
      //      withTest("nemp", "");

      withTest("likeMiddle", "orders?like(shipCountry,F*ance)");
      withTest("likeStartsWith", "orders?like(shipCountry,Franc*)");
      withTest("likeEndsWith", "orders?like(shipCountry,*ance)");
      withTest("sw", "orders?sw(shipCountry,Franc)");
      withTest("ew", "orders?ew(shipCountry,nce)");

      withTest("lt", "orders?lt(freight,10)");
      withTest("le", "orders?le(freight,10)");
      withTest("gt", "orders?gt(freight,3.67)");
      withTest("ge", "orders?ge(freight,3.67)");
      withTest("in", "in(shipCity,Reims,Charleroi)");
      withTest("out", "out(shipCity,Reims,Charleroi)");
      withTest("w", "");
      withTest("wo", "");
      //
      //      withTest("if", "");
      //      withTest("and", "");
      //      withTest("or", "");
      //      withTest("not", "");
      //      withTest("_key", "");
      //      withTest("_index", "");
      //
      //      withTest("offset", "");
      //      withTest("limit", "");
      //      withTest("page", "");
      //      withTest("pageNum", "");
      //      withTest("pageSize", "");
      //      withTest("after", "");
      //
      //      withTest("as", "");
      //      withTest("includes", "");
      //      withTest("excludes", "");
      //      withTest("distinct", "");
      //      withTest("count", "");
      //      withTest("sum", "");
      //      withTest("min", "");
      //      withTest("max", "");
      //      withTest("if", "");
      //      withTest("aggregate", "");
      //      withTest("function", "");
      //      withTest("countascol", "");
      //      withTest("rowcount", "");

      //("as", "includes", "excludes", "distinct", "count", "sum", "min", "max", "if", "aggregate", "function", "countascol", "rowcount");

      withQueryClass(queryClass);
      withDb(db);
      withTables(tables);
   }

   public void run() throws Exception
   {
      LinkedHashMap<String, String> failures = new LinkedHashMap();

      for (String testKey : tests.keySet())
      {

         String queryString = tests.get(testKey);

         System.out.println("\r\nTESTING: " + testKey + " - " + queryString);

         String tableName = queryString.substring(0, queryString.indexOf("?"));
         if (tableName.indexOf("/") > 0)
            tableName = tableName.substring(tableName.lastIndexOf("/") + 1);

         Query query = (Query) Class.forName(queryClass).newInstance();

         String rql = queryString.substring(queryString.indexOf("?") + 1);

         //-- RestGetAction sorts the terms so we do it here
         //-- to try and mimic the order of terms in the sql
         //-- when a live call is made
         List<Term> terms = new ArrayList();
         String[] parts = rql.split("\\&");
         Parser parser = new Parser();
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

            Table table = tables.get(tableName);
            if (table == null)
               throw new Exception("Unable to find table for query: " + testKey + " - " + queryString);
            table.withDb(db);
            query.withTable(table);
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
         }
         catch (Exception ex)
         {
            actual = ex.getMessage();
         }

         if (!TestSqlQuery.compare(expected, actual))
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
   }

   public RqlValidationSuite withTables(Table... tables)
   {
      for (int i = 0; tables != null && i < tables.length; i++)
      {
         Table t = tables[i];
         if (t != null)
            this.tables.put(t.getName(), t);
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

}
