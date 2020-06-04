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
package io.inversion.cloud.action.cosmosdb;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import io.inversion.cloud.model.Db;
import io.inversion.cloud.rql.AbstractRqlTest;
import io.inversion.cloud.rql.RqlValidationSuite;

@TestInstance(Lifecycle.PER_CLASS)
public class CosmosRqlUnitTest extends AbstractRqlTest
{

   public CosmosRqlUnitTest()
   {
      super(CosmosSqlQuery.class.getName(), "cosmos");
   }

   @Override
   public void initializeDb()
   {
      Db db = getDb();
      if (db == null)
      {
         db = CosmosDbFactory.buildDb();
         setDb(db);
      }
   }

   @Override
   protected void customizeUnitTestSuite(RqlValidationSuite suite)
   {

      suite//
           .withResult("eq", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE orders[\\\"orderID\\\"] = @orderID1 AND orders[\\\"shipCountry\\\"] = @shipCountry2 ORDER BY orders[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@orderID1\",\"value\":\"10248\"},{\"name\":\"@shipCountry2\",\"value\":\"France\"}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("ne", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE (NOT (orders[\\\"shipCountry\\\"] = @shipCountry1)) ORDER BY orders[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@shipCountry1\",\"value\":\"France\"}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("n", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE IS_NULL (orders[\\\"shipRegion\\\"]) ORDER BY orders[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("nn", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE orders[\\\"shipRegion\\\"] <> null ORDER BY orders[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("emp", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE (orders[\\\"shipRegion\\\"] IS NULL OR orders[\\\"shipRegion\\\"] = '') ORDER BY orders[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("nemp", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE (orders[\\\"shipRegion\\\"] IS NOT NULL AND orders[\\\"shipRegion\\\"] != '') ORDER BY orders[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("likeMiddle", "400 Bad Request The 'like' RQL operator for CosmosDb expects a single wildcard at the beginning OR the end of a value.  CosmosDb does not really support 'like' but compatible 'like' statements are turned into 'sw' or 'ew' statments that are supported.")//
           .withResult("likeStartsWith", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE STARTSWITH (orders[\\\"shipCountry\\\"], @shipCountry1) ORDER BY orders[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@shipCountry1\",\"value\":\"Franc\"}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("likeEndsWith", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE ENDSWITH (orders[\\\"shipCountry\\\"], @shipCountry1) ORDER BY orders[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@shipCountry1\",\"value\":\"ance\"}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("sw", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE STARTSWITH (orders[\\\"shipCountry\\\"], @shipCountry1) ORDER BY orders[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@shipCountry1\",\"value\":\"Franc\"}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("ew", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE ENDSWITH (orders[\\\"shipCountry\\\"], @shipCountry1) ORDER BY orders[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@shipCountry1\",\"value\":\"nce\"}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("w", "400 Bad Request CosmosDb supports 'sw' and 'ew' but not 'w' or 'wo' functions.")//
           .withResult("wo", "400 Bad Request CosmosDb supports 'sw' and 'ew' but not 'w' or 'wo' functions.")//
           .withResult("lt", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE orders[\\\"freight\\\"] < @freight1 ORDER BY orders[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@freight1\",\"value\":10}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("le", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE orders[\\\"freight\\\"] <= @freight1 ORDER BY orders[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@freight1\",\"value\":10}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("gt", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE orders[\\\"freight\\\"] > @freight1 ORDER BY orders[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@freight1\",\"value\":3.67}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("ge", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE orders[\\\"freight\\\"] >= @freight1 ORDER BY orders[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@freight1\",\"value\":3.67}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("in", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE orders[\\\"shipCity\\\"] IN(@shipCity1, @shipCity2) ORDER BY orders[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@shipCity1\",\"value\":\"Reims\"},{\"name\":\"@shipCity2\",\"value\":\"Charleroi\"}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("out", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE orders[\\\"shipCity\\\"] NOT IN(@shipCity1, @shipCity2) ORDER BY orders[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@shipCity1\",\"value\":\"Reims\"},{\"name\":\"@shipCity2\",\"value\":\"Charleroi\"}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("and", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE orders[\\\"shipCity\\\"] = @shipCity1 AND orders[\\\"shipCountry\\\"] = @shipCountry2 ORDER BY orders[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@shipCity1\",\"value\":\"Lyon\"},{\"name\":\"@shipCountry2\",\"value\":\"France\"}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("or", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE (orders[\\\"shipCity\\\"] = @shipCity1 OR orders[\\\"shipCity\\\"] = @shipCity2) ORDER BY orders[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@shipCity1\",\"value\":\"Reims\"},{\"name\":\"@shipCity2\",\"value\":\"Charleroi\"}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("not", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE NOT ((orders[\\\"shipCity\\\"] = @shipCity1 OR orders[\\\"shipCity\\\"] = @shipCity2)) ORDER BY orders[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@shipCity1\",\"value\":\"Reims\"},{\"name\":\"@shipCity2\",\"value\":\"Charleroi\"}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("as", "SqlQuerySpec={\"query\":\"SELECT *, orders[\\\"orderid\\\"] AS \\\"order_identifier\\\" FROM orders ORDER BY orders[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("includes", "SqlQuerySpec={\"query\":\"SELECT orders[\\\"shipCountry\\\"], orders[\\\"shipCity\\\"], orders[\\\"orderId\\\"] FROM orders ORDER BY orders[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("distinct", "SqlQuerySpec={\"query\":\"SELECT DISTINCT orders[\\\"shipCountry\\\"] FROM orders ORDER BY orders[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("count1", "SqlQuerySpec={\"query\":\"SELECT COUNT(*) FROM orders ORDER BY orders[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("count2", "SqlQuerySpec={\"query\":\"SELECT COUNT(@null1) FROM orders ORDER BY orders[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@null1\",\"value\":\"1\"}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("count3", "SqlQuerySpec={\"query\":\"SELECT COUNT(orders[\\\"shipRegion\\\"]) FROM orders ORDER BY orders[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("countAs", "SqlQuerySpec={\"query\":\"SELECT COUNT(*) AS \\\"countOrders\\\" FROM orders ORDER BY orders[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("sum", "SqlQuerySpec={\"query\":\"SELECT SUM(orders[\\\"freight\\\"]) FROM orders ORDER BY orders[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("sumAs", "SqlQuerySpec={\"query\":\"SELECT SUM(orders[\\\"freight\\\"]) AS \\\"Sum Freight\\\" FROM orders ORDER BY orders[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("sumIf", "SqlQuerySpec={\"query\":\"SELECT SUM(CASE WHEN orders[\\\"shipCountry\\\"] = @shipCountry1 THEN 1 ELSE 0 END) AS \\\"French Orders\\\" FROM orders ORDER BY orders[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@shipCountry1\",\"value\":\"France\"}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("min", "SqlQuerySpec={\"query\":\"SELECT MIN(orders[\\\"freight\\\"]) FROM orders ORDER BY orders[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("max", "SqlQuerySpec={\"query\":\"SELECT MAX(orders[\\\"freight\\\"]) FROM orders ORDER BY orders[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("groupCount", "SqlQuerySpec={\"query\":\"SELECT orders[\\\"shipCountry\\\"], COUNT(*) AS \\\"countryCount\\\" FROM orders GROUP BY orders[\\\"shipCountry\\\"] ORDER BY orders[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("offset", "SqlQuerySpec={\"query\":\"SELECT * FROM orders ORDER BY orders[\\\"id\\\"] ASC OFFSET 3 LIMIT 100\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("limit", "SqlQuerySpec={\"query\":\"SELECT * FROM orders ORDER BY orders[\\\"id\\\"] ASC OFFSET 0 LIMIT 7\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("page", "SqlQuerySpec={\"query\":\"SELECT * FROM orders ORDER BY orders[\\\"id\\\"] ASC OFFSET 14 LIMIT 7\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("pageNum", "SqlQuerySpec={\"query\":\"SELECT * FROM orders ORDER BY orders[\\\"id\\\"] ASC OFFSET 14 LIMIT 7\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("after", "SqlQuerySpec={\"query\":\"SELECT * FROM orders ORDER BY orders[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("sort", "SqlQuerySpec={\"query\":\"SELECT * FROM orders ORDER BY orders[\\\"shipCountry\\\"] DESC, orders[\\\"shipCity\\\"] ASC OFFSET 0 LIMIT 100\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("order", "SqlQuerySpec={\"query\":\"SELECT * FROM orders ORDER BY orders[\\\"shipCountry\\\"] ASC, orders[\\\"shipCity\\\"] DESC OFFSET 0 LIMIT 100\"} FeedOptions={enableCrossPartitionQuery=true}")//

           .withResult("onToManyExistsEq", "UNSUPPORTED")//
           .withResult("onToManyNotExistsNe", "UNSUPPORTED")//
           .withResult("manyToOneExistsEq", "UNSUPPORTED")//
           .withResult("manyToOneNotExistsNe", "UNSUPPORTED")//
           .withResult("manyTManyNotExistsNe", "UNSUPPORTED")//

           .withResult("eqNonexistantColumn", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE orders[\\\"orderId\\\"] >= @orderId1 ORDER BY orders[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@orderId1\",\"value\":\"1000\"}]} FeedOptions={enableCrossPartitionQuery=true}")//

      ;
   }

}
