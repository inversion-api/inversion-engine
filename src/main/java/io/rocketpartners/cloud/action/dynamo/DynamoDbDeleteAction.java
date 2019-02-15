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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.document.KeyAttribute;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;

import io.rocketpartners.cloud.action.dynamo.DynamoDb.DynamoDbIndex;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.ApiException;
import io.rocketpartners.cloud.model.Collection;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.model.Table;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.service.Request;
import io.rocketpartners.cloud.service.Response;
import io.rocketpartners.cloud.service.Service;

/**
 * @author tc-rocket
 *
 */
public class DynamoDbDeleteAction extends DynamoDbAction
{

   @Override
   public void run(Service service, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
   {
      Collection collection = api.getCollection(req.getCollectionKey(), DynamoDb.class);
      Table table = collection.getEntity().getTable();
      DynamoDb db = (DynamoDb) table.getDb();
      com.amazonaws.services.dynamodbv2.document.Table dynamoTable = db.getDynamoTable(table.getName());
      DynamoDbIndex dynamoIdx = DynamoDb.findIndexByName(table, DynamoDbIndex.PRIMARY_INDEX);
      String pk = dynamoIdx.getPartitionKey().getName();
      String sk = dynamoIdx.getSortKey() != null ? dynamoIdx.getSortKey().getName() : null;
      boolean appendTenantIdToPk = isAppendTenantIdToPk(chain, collection.getName());

      Object tenantIdOrCode = null;
      if (req.getApi().isMultiTenant())
      {
         tenantIdOrCode = req.removeParam("tenantId");
         if (tenantIdOrCode != null)
         {
            tenantIdOrCode = Integer.parseInt((String) tenantIdOrCode);
         }
         else
         {
            tenantIdOrCode = req.getTenantCode();
         }
      }

      // using this instead of the built in req.getJson(), because JSObject converts everything to strings even if they are sent up as a number
      Object payloadObj = jsonStringToObject(req.getBody());

      if (payloadObj instanceof List)
      {
         List l = (List) payloadObj;
         for (Object obj : l)
         {
            deleteMapFromDynamo((Map) obj, dynamoTable, pk, sk, tenantIdOrCode, api.isMultiTenant(), appendTenantIdToPk);
         }
      }
      else if (payloadObj instanceof Map)
      {
         deleteMapFromDynamo((Map) payloadObj, dynamoTable, pk, sk, tenantIdOrCode, api.isMultiTenant(), appendTenantIdToPk);
      }

      res.withStatus(SC.SC_200_OK);

   }

   void deleteMapFromDynamo(Map json, com.amazonaws.services.dynamodbv2.document.Table dynamoTable, String pk, String sk, Object tenantIdOrCode, boolean isMultiTenant, boolean appendTenantIdToPk)
   {
      try
      {
         Map m = new HashMap<>(json);

         KeyAttribute[] keys = null;

         if (!m.containsKey(pk) || (sk != null && !m.containsKey(sk)))
         {
            String msg = "The JSON body must contain a '" + pk + "' field";
            if (sk != null)
            {
               msg = msg + " and a '" + sk + "' field.";
            }

            throw new ApiException(SC.SC_400_BAD_REQUEST, msg);
         }

         String pkValue = (String) m.get(pk);
         if (isMultiTenant && appendTenantIdToPk)
         {
            pkValue = addTenantIdToKey(tenantIdOrCode, pkValue);
         }
         KeyAttribute pkAttr = new KeyAttribute(pk, pkValue);
         keys = new KeyAttribute[]{pkAttr};
         if (sk != null)
         {
            KeyAttribute skAttr = new KeyAttribute(sk, m.get(sk));
            keys = new KeyAttribute[]{pkAttr, skAttr};
         }

         DeleteItemSpec spec = new DeleteItemSpec()//
                                                   .withPrimaryKey(keys);

         if (isMultiTenant)
         {
            spec = spec.withConditionExpression("tenantid = :val")//
                       .withValueMap(new ValueMap().with(":val", tenantIdOrCode));
         }

         dynamoTable.deleteItem(spec);
      }
      catch (ConditionalCheckFailedException ccfe)
      {
         // catch this and do nothing.
         // this just means the that conditional check wasn't satisfied
         // so the record was not deleted
         // This is probably because the the record doesn't exist
      }

   }

}
