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
   //   withFunctions("_key", "and", "or", "not", "eq", "ne", "n", "nn", "like", "sw", "ew", "lt", "le", "gt", "ge", "in", "out", "if", "w", "wo", "emp", "nemp");
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
      withTables(new Table("orders")//
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

      withTest("eq_queryOnEquality", "orders?eq(OrderID, 1234)&ShipCountry=France");

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

         if (Utils.empty(""))
            expected = "You are missing a validation for test '" + testKey + "' with RQL '" + expected + "'.  If this case is unsupported please call 'withResult(\"" + testKey + "\", \"UNSUPPORTED\") in your setup to declare that this validation should be skipped.";

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
