/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
 * http://rocketpartners.io
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package io.rocketpartners.cloud.action.dynamo;

import io.rocketpartners.cloud.action.dynamo.DynamoDbQuery.DynamoResult;
import io.rocketpartners.cloud.model.Action;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.Collection;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.model.Table;
import io.rocketpartners.cloud.rql.Rql;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.service.Request;
import io.rocketpartners.cloud.service.Response;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.JSArray;
import io.rocketpartners.cloud.utils.JSObject;
import io.rocketpartners.cloud.utils.Rows.Row;

/**
 * @author tc-rocket
 *
 * Endpoint/Action Config
 *  - appendTenantIdToPk :        Enables appending the tenant id to the primary key
 *                                FORMAT: collection name (comma separated)
 *
 */
public class DynamoDbGetAction extends DynamoDbAction
{

   protected String nextKeyDelimeter = "~";

   @Override
   public void service(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res) throws Exception
   {
      Collection collection = api.getCollection(req.getCollectionKey(), DynamoDb.class);
      Table table = collection.getEntity().getTable();
      DynamoDb db = (DynamoDb) table.getDb();
      com.amazonaws.services.dynamodbv2.document.Table dynamoTable = db.getDynamoTable(table.getName());

      //      String tenantIdOrCode = null;
      //      if (req.getApi().isMultiTenant())
      //      {
      //         tenantIdOrCode = req.removeParam("tenantId");
      //         if (tenantIdOrCode == null)
      //         {
      //            tenantIdOrCode = req.getTenantCode();
      //         }
      //      }

      DynamoDbRql rql = (DynamoDbRql) Rql.getRql(db.getType());
      DynamoDbQuery query = rql.buildQuery(table, req.getParams());
      DynamoResult dynamoResult = query.doSelect(dynamoTable);

      JSArray returnData = new JSArray();

      for (Row row : dynamoResult)
      {
         returnData.add(new JSObject(row));
      }

      JSObject meta = new JSObject("pageSize", query.page().getPageSize(), "results", returnData.asList().size());
      //      if (returnNext != null)
      //      {
      //         meta.put("next", returnNext);
      //      }
      JSObject wrapper = new JSObject("meta", meta, "data", returnData);
      res.setJson(wrapper);

   }
}
