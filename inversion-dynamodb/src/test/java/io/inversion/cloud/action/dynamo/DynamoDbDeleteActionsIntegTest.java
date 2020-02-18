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
package io.inversion.cloud.action.dynamo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Test;

import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.service.Engine;
import io.inversion.cloud.utils.Utils;

public class DynamoDbDeleteActionsIntegTest
{
   @Test
   public void test1() throws Exception
   {
      Engine engine = DynamoDbEngineFactory.service();
      Response res = null;

      //get a random record, 
      res = engine.get("northwind/dynamodb/orders?limit=1");

      String href = res.findString("data.0.href");
      assertNotNull(href);

      //delete that random record
      JSNode record = res.findNode("data.0");
      engine.delete(href).hasStatus(204);

      //make sure it was deleted
      engine.get(href).assertStatus(404);

      //now put the recored back and lets make sure
      //it looks the same as it did when we pulled it the first time
      record.remove("href");
      res = engine.post("northwind/dynamodb/orders", record);
      res.assertStatus(201);
      res.dump();

      //set this back for the comaprison below
      record.put("href", href);

      //makes sure the write back worked
      res = engine.get(href);
      assertEquals(1, res.data().length());

      //makes sure the orig and the written back record match
      JSNode updatedRecord = res.findNode("data.0");
      for (String key : record.keySet())
      {
         assertTrue(Utils.equal(record.get(key), updatedRecord.get(key)));
      }

      java.util.Collection disjunction = CollectionUtils.disjunction(record.keySet(), updatedRecord.keySet());
      if (disjunction.size() > 0)
      {
         //there is an extra or missing column somewhere
         System.err.println("DISJUNCTION: " + disjunction);
         fail();
      }
   }
}
