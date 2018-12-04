/**
 * 
 */
package io.rcktapp.api.handler.dynamo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.document.KeyAttribute;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;

import io.rcktapp.api.Action;
import io.rcktapp.api.Api;
import io.rcktapp.api.ApiException;
import io.rcktapp.api.Chain;
import io.rcktapp.api.Collection;
import io.rcktapp.api.Endpoint;
import io.rcktapp.api.Request;
import io.rcktapp.api.Response;
import io.rcktapp.api.SC;
import io.rcktapp.api.Table;
import io.rcktapp.api.service.Service;

/**
 * @author tc-rocket
 *
 */
public class DynamoDbDeleteHandler extends DynamoDbHandler
{

   @Override
   public void service(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res) throws Exception
   {
      Collection collection = findCollectionOrThrow404(api, chain, req);
      Table table = collection.getEntity().getTable();
      DynamoDb db = (DynamoDb) table.getDb();
      com.amazonaws.services.dynamodbv2.document.Table dynamoTable = db.getDynamoTable(table.getName());
      String pk = DynamoDb.findPartitionKeyName(table);
      String sk = DynamoDb.findSortKeyName(table);
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

      res.setStatus(SC.SC_200_OK);

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