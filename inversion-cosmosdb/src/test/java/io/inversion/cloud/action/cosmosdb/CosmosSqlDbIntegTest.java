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

import io.inversion.cloud.model.Response;
import io.inversion.cloud.service.Engine;
import junit.framework.TestCase;

public class CosmosSqlDbIntegTest extends TestCase
{
   @Test
   public void testBasicQuery() throws Exception
   {
      Engine e = CosmosEngineFactory.engine();

      Response res = null;

      res = e.get("/northwind/cosmosdb/orders?shipCountry=France");
      res.dump();

      res = e.get("http://localhost/northwind/cosmosdb/orders/order*002d10265~10265");
      res.dump();
      
      assertTrue(res.data().size() == 1);
   }

   public void test_multipleCollectionsBackedBySingleTable()
   {

   }

}
