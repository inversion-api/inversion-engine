/**
 * 
 */
package io.rcktapp.api.handler.dynamo;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.rcktapp.api.Action;
import io.rcktapp.api.Api;
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
public class DynamoDbPostHandler extends DynamoDbHandler
{
   static ObjectMapper mapper = new ObjectMapper();

   @Override
   public void service(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res) throws Exception
   {
      Collection collection = findCollectionOrThrow404(api, chain, req);
      Table table = collection.getEntity().getTable();
      DynamoDb db = (DynamoDb) table.getDb();
      AmazonDynamoDB dynamoClient = db.getDynamoClient();
      String pk = DynamoDb.findPartitionKeyName(table);
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

      DynamoDB dynamoDB = new DynamoDB(dynamoClient);
      com.amazonaws.services.dynamodbv2.document.Table dynamoTable = dynamoDB.getTable(table.getName());

      // using this instead of the built in req.getJson(), because JSObject converts everything to strings even if they are sent up as a number
      Object payloadObj = jsonStringToObject(req.getBody());

      if (payloadObj instanceof List)
      {
         List l = (List) payloadObj;
         for (Object obj : l)
         {
            putMapToDynamo((Map) obj, dynamoTable, pk, req.getApi().isMultiTenant(), appendTenantIdToPk, tenantIdOrCode);
         }
      }
      else if (payloadObj instanceof Map)
      {
         putMapToDynamo((Map) payloadObj, dynamoTable, pk, req.getApi().isMultiTenant(), appendTenantIdToPk, tenantIdOrCode);
      }

      res.setStatus(SC.SC_200_OK);

   }

   void putMapToDynamo(Map json, com.amazonaws.services.dynamodbv2.document.Table dynamoTable, String pk, boolean isMultiTenant, boolean appendTenantIdToPk, Object tenantIdOrCode)
   {
      try
      {
         // TODO get conditionalExpression config back in somehow
         //String conditionalExpression = tableInfo.conditionalWriteExpression;
         //List<String> conditionalExpressionFields = tableInfo.conditionalWriteExpressionFields;
         String conditionalExpression = null;
         List<String> conditionalExpressionFields = null;

         Map m = new HashMap<>(json);

         if (isMultiTenant && appendTenantIdToPk && tenantIdOrCode != null)
         {
            m.put("tenantid", tenantIdOrCode);
            // add the tenantCode to the primary key
            String pkVal = (String) m.get(pk);
            m.put(pk, addTenantIdToKey(tenantIdOrCode, pkVal));
         }
         Item item = Item.fromMap(m);

         PutItemSpec putItemSpec = new PutItemSpec().withItem(item);

         if (conditionalExpression != null)
         {
            putItemSpec = putItemSpec.withConditionExpression(conditionalExpression);
            if (conditionalExpressionFields != null && !conditionalExpressionFields.isEmpty())
            {
               Map<String, Object> valueMap = new HashMap<>();
               for (String field : conditionalExpressionFields)
               {
                  valueMap.put(":" + field, m.get(field));
               }

               putItemSpec = putItemSpec.withValueMap(valueMap);
            }
         }

         dynamoTable.putItem(putItemSpec);
      }
      catch (ConditionalCheckFailedException ccfe)
      {
         // catch this and do nothing.
         // this just means the that conditional write wasn't satisfied
         // so the record was not written
      }
   }

   static Object jsonStringToObject(String jsonStr) throws JsonParseException, JsonMappingException, IOException
   {
      return mapper.readValue(jsonStr, Object.class);
   }
}
