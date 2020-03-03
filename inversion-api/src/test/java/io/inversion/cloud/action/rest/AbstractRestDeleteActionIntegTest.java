/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
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
package io.inversion.cloud.action.rest;

import io.inversion.cloud.model.JSArray;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.service.Engine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class AbstractRestDeleteActionIntegTest extends AbstractRestActionIntegTest
{

   public AbstractRestDeleteActionIntegTest(String dbType)
   {
      super(dbType);
   }

   @Test
   public void testSingleDelete() throws Exception
   {
      Response res = null;
      Engine engine = engine();

      res = engine.get(url("orderdetails?limit=1&sort=orderid"));
      res.dump();
      res.assertOk();
      String href = res.findString("data.0.href");

      res = engine.delete(href);
      res.dump();
      res = engine.get(href);
      res.dump();
      res.assertStatus(404);
   }

   @Test
   public void testBatchHrefDelete() throws Exception
   {
      Response res = null;
      Engine engine = engine();

      res = engine.get(url("orderdetails?limit=10&sort=orderid")).dump().assertOk();

      JSArray hrefs = new JSArray();

      res.data().forEach(o -> hrefs.add(((JSNode) o).getString("href")));

      assertEquals(10, hrefs.size());

      res = engine.delete(url("orderdetails"), hrefs);

      for (int i = 0; i < hrefs.size(); i++)
         engine.get(hrefs.getString(i)).hasStatus(404);
   }

   @Test
   public void testBatchQueryDelete() throws Exception
   {
      Response res = null;
      Engine engine = engine();

      JSArray hrefs = new JSArray(url("orderdetails/10248~11"), url("orderdetails?orderid=10249"), url("orderdetails?orderid=10250"));

      for (int i = 0; i < hrefs.size(); i++)
         assertTrue(engine.get(hrefs.getString(i)).assertOk().getFoundRows() > 0);

      engine.delete(url("orderdetails"), hrefs).isSuccess();

      for (int i = 0; i < hrefs.size(); i++)
      {
         res = engine.get(hrefs.getString(i));
         assertTrue(res.hasStatus(404) || (res.hasStatus(200) && res.getFoundRows() == 0));
      }
   }

   @Test
   public void testBatchQueryDeleteWithMultipleConditionsOnMultiPKTable() throws Exception
   {
      Engine engine = engine();

      int allRecordsSize = engine.get(url("orderdetails")).getJson().findArray("data").size();

      // The Order Detail table has a two column PK
      // select * from `Order Details` where Quantity = 60 and UnitPrice > 10;
      String url = url("orderdetails?Quantity=60&gt(UnitPrice,10)");

      JSArray data = engine.get(url).getJson().findArray("data");
      assertTrue(data.size() == 2, "data should contain two records");

      Response res = engine.delete(url("orderdetails"), new JSArray(url));
      assertTrue(res.isSuccess(), "bulk delete should succeed");

      data = engine.get(url).getJson().findArray("data");
      assertTrue(data.size() == 0, "data should contain zero records after delete");

      int allRecordsSizeAfterDelete = engine.get(url("orderdetails")).getJson().findArray("data").size();
      assertEquals(2, (allRecordsSize - allRecordsSizeAfterDelete), "Wrong number of records were deleted");

   }

   @Test
   public void testBatchQueryDeleteWithMultipleConditionsOnSinglePKtable() throws Exception
   {
      Engine engine = engine();

      Response res = engine.get(url("indexlogs"));
      int allRecordsSize = res.data().size();

      //res = engine.get(url("indexlogs/1,4,16,18"));
      //res.dump();

      // The IndexLog table has a single column PK
      // select * from IndexLog where tenantCode = 'us' and error is null and modifiedAt < '2019-04-01 00:00:00';
      String url = url("indexlogs?tenantCode=us&n(error)&lt(modifiedAt,2019-04-01 00:00:00)");
      res = engine.get(url);

      JSArray data = res.getJson().findArray("data");
      assertTrue(data.size() == 3, "data should contain three records");

      res = engine.delete(url("indexlogs"), new JSArray(url));
      res.dump();
      assertTrue(res.isSuccess(), "bulk delete should succeed");

      res = engine.get(url);
      data = res.getJson().findArray("data");
      assertTrue(data.size() == 0, "data should contain zero records after delete");

      res = engine.get(url("indexlogs"));
      int allRecordsSizeAfterDelete = res.getJson().findArray("data").size();
      assertEquals(3, (allRecordsSize - allRecordsSizeAfterDelete), "Wrong number of records were deleted");
   }

   //2019-05-16 this is currently failing because of the OrderDetails child records...not sure what to do with this test
   //   @Test
   //   public void testBatchQueryDeleteWithForeignKeyConstraint() throws Exception
   //   {
   //      Engine engine = service();
   //
   //      // select * from Orders where ShipVia = '2' and ShipRegion is null and OrderDate < '2014-09-01 00:00:00';
   //      String url = url("orders?shipvia=2&n(shipregion)&lt(orderdate,2014-09-01 00:00:00)");
   //
   //      ArrayNode data = engine.get(url).getJson().findArray("data");
   //      assertTrue("data should contain two records", data.size() == 2);
   //
   //      Response res = engine.delete(url("orders"), new ArrayNode(url));
   //      res.dump();
   //      assertTrue("bulk delete should succeed", res.isSuccess());
   //
   //      data = engine.get(url).getJson().findArray("data");
   //      assertTrue("data should contain zero records after delete", data.size() == 0);
   //
   //   }

}
