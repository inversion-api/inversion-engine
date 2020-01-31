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
//      RqlValidationSuite suite = new RqlValidationSuite(CosmosSqlQuery.class.getName(), new CosmosDocumentDb());
//
//      suite.withResult("eq_queryForEquals", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE orders[\\\"OrderID\\\"] = @OrderID1 AND orders[\\\"ShipCountry\\\"] = @ShipCountry2 ORDER BY orders[\\\"id\\\"] ASC\",\"parameters\":[{\"name\":\"@OrderID1\",\"value\":\"1234\"},{\"name\":\"@ShipCountry2\",\"value\":\"France\"}]} FeedOptions={enableCrossPartitionQuery=true}")//
//           .withResult("ne_queryForNotEquals", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE (NOT (orders[\\\"ShipCountry\\\"] = @ShipCountry1)) ORDER BY orders[\\\"id\\\"] ASC\",\"parameters\":[{\"name\":\"@ShipCountry1\",\"value\":\"France\"}]} FeedOptions={enableCrossPartitionQuery=true}").withResult("", "")//
//           .withResult("", "")//
//           .withResult("", "")//
//           .withResult("", "")//
//           .withResult("", "")//
//           .withResult("", "")//
//           .withResult("", "")//
//           .withResult("", "")//
//           .withResult("", "")//
//           .withResult("", "")//
//           .withResult("", "")//
//           .withResult("", "")//
//           .withResult("", "")//
//           .withResult("", "")//
//      ;
//      suite.run();
   }
}
