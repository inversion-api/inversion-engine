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

import org.junit.Test;

import io.inversion.cloud.rql.RqlValidationSuite;
import junit.framework.TestCase;

public class CosmosSqlQueryRqlValidationTests extends TestCase
{
   @Test
   public void test_doSelect_validateRql() throws Exception
   {
      RqlValidationSuite suite = new RqlValidationSuite(CosmosSqlQuery.class.getName(), new CosmosDocumentDb());

      suite.withResult("eq", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE orders[\\\"orderID\\\"] = @orderID1 AND orders[\\\"shipCountry\\\"] = @shipCountry2 ORDER BY orders[\\\"id\\\"] ASC\",\"parameters\":[{\"name\":\"@orderID1\",\"value\":\"10248\"},{\"name\":\"@shipCountry2\",\"value\":\"France\"}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("ne", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE (NOT (orders[\\\"shipCountry\\\"] = @shipCountry1)) ORDER BY orders[\\\"id\\\"] ASC\",\"parameters\":[{\"name\":\"@shipCountry1\",\"value\":\"France\"}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("n", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE IS_NULL (orders[\\\"shipRegion\\\"]) ORDER BY orders[\\\"id\\\"] ASC\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("nn", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE orders[\\\"shipRegion\\\"] <> null ORDER BY orders[\\\"id\\\"] ASC\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("sw", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE STARTSWITH (orders[\\\"shipCountry\\\"], @shipCountry1) ORDER BY orders[\\\"id\\\"] ASC\",\"parameters\":[{\"name\":\"@shipCountry1\",\"value\":\"Franc\"}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("ew", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE ENDSWITH (orders[\\\"shipCountry\\\"], @shipCountry1) ORDER BY orders[\\\"id\\\"] ASC\",\"parameters\":[{\"name\":\"@shipCountry1\",\"value\":\"nce\"}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("likeMiddle", "The 'like' RQL operator for CosmosDb expects a single wildcard at the beginning OR the end of a value.  CosmosDb does not really support 'like' but compatible 'like' statements are turned into 'sw' or 'ew' statments that are supported.")//
           .withResult("likeStartsWith", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE STARTSWITH (orders[\\\"shipCountry\\\"], @shipCountry1) ORDER BY orders[\\\"id\\\"] ASC\",\"parameters\":[{\"name\":\"@shipCountry1\",\"value\":\"Franc\"}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("likeEndsWith", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE ENDSWITH (orders[\\\"shipCountry\\\"], @shipCountry1) ORDER BY orders[\\\"id\\\"] ASC\",\"parameters\":[{\"name\":\"@shipCountry1\",\"value\":\"ance\"}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("", "")//
           .withResult("", "")//
           .withResult("", "")//
           .withResult("", "")//
           .withResult("", "")//
           .withResult("", "")//
      ;
      suite.run();
   }
}
