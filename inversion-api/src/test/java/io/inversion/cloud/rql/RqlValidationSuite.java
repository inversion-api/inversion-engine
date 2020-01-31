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

import java.util.HashMap;
import java.util.LinkedHashMap;
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
                                    .withColumn("OrderID", "VARCHAR")//
                                    .withColumn("CustomerID", "INTEGER")//
                                    .withColumn("EmployeeID", "DATETIME")//
                                    .withColumn("OrderDate", "DATETIME")//
                                    .withColumn("RequiredDate", "DATETIME")//
                                    .withColumn("ShippedDate", "DATETIME")//
                                    .withColumn("ShipVia", "INTEGER")//
                                    .withColumn("Freight", "DECIMAL")//
                                    .withColumn("ShipName", "VARCHAR")//
                                    .withColumn("ShipAddress", "VARCHAR")//
                                    .withColumn("ShipCity", "VARCHAR")//
                                    .withColumn("ShipRegion", "VARCHAR")//
                                    .withColumn("ShipPostalCode", "VARCHAR")//
                                    .withColumn("ShipCountry", "VARCHAR")//
                                    .withIndex("PK_Orders", "primary", true, "OrderID"));

      withTest("eq", "orders?eq(OrderID, 1234)&eq(ShipCountry,France)");
      withTest("ne", "orders?ne(ShipCountry,France)");

      
      
      withTest("n", "orders?n(ShipCountry)");
      withTest("nn", "orders?nn(ShipCountry)");
      withTest("emp", "");
      withTest("nemp", "");
      
      
      withTest("like", "order?like(ShipCountry,anc)");
      withTest("sw", "order?sw(ShipCountry,Franc)");
      withTest("ew", "order?ew(ShipCountry,ance)");
      
      withTest("lt", "");
      withTest("le", "");
      withTest("gt", "");
      withTest("ge", "");
      withTest("in", "");
      withTest("out", "");
      withTest("w", "");
      withTest("wo", "");
      
      withTest("if", "");
      withTest("and", "");
      withTest("or", "");
      withTest("not", "");
      withTest("_key", "");
      withTest("_index", "");
      
      
      withTest("offset", "");
      withTest("limit", "");
      withTest("page", "");
      withTest("pageNum", "");
      withTest("pageSize", "");
      withTest("after", "");
      
      withTest("as", "");
      withTest("includes", "");
      withTest("excludes", "");
      withTest("distinct", "");
      withTest("count", "");
      withTest("sum", "");
      withTest("min", "");
      withTest("max", "");
      withTest("if", "");
      withTest("aggregate", "");
      withTest("function", "");
      withTest("countascol", "");
      withTest("rowcount", "");
      
      
      
      //("as", "includes", "excludes", "distinct", "count", "sum", "min", "max", "if", "aggregate", "function", "countascol", "rowcount");
      

      withQueryClass(queryClass);
      withDb(db);
      withTables(tables);
   }

   public void run() throws Exception
   {
      for (String testKey : tests.keySet())
      {
         String queryString = tests.get(testKey);

         String tableName = queryString.substring(0, queryString.indexOf("?"));
         if (tableName.indexOf("/") > 0)
            tableName = tableName.substring(tableName.lastIndexOf("/") + 1);

         Query query = (Query) Class.forName(queryClass).newInstance();

         String rql = queryString.substring(queryString.indexOf("?") + 1);
         query.withTerms(rql);

         Table table = tables.get(tableName);
         table.withDb(db);
         query.withTable(table);
         query.withDryRun(true);

         String expected = results.get(testKey);

         if ("UNSUPPORTED".equalsIgnoreCase(expected))
         {
            System.err.println("SKIPPING UNSUPPORTED TEST CASE: " + testKey + " - " + queryString);
            continue;
         }

         if (Utils.empty(expected))
            expected = "You are missing a validation for test '" + testKey + "' with RQL '" + queryString + "'.  If this case is unsupported please call 'withResult(\"" + testKey + "\", \"UNSUPPORTED\") in your setup to declare that this validation should be skipped.";

         Results results = query.doSelect();
         String actual = results.getTestQuery();

         if (!TestSqlQuery.compare(expected, actual))
         {
            System.err.println("VALIDATION FAILED: " + testKey + " " + queryString);
            System.err.println(" - " + results.getDebugQuery());

            throw new RuntimeException("Failed validation case '" + testKey + "'");
         }
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
