/**
 * 
 */
package io.rcktapp.api.service.ext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.KeyAttribute;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.RangeKeyCondition;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.forty11.j.utils.ListMap;
import io.forty11.web.js.JSArray;
import io.forty11.web.js.JSObject;
import io.rcktapp.api.Action;
import io.rcktapp.api.Api;
import io.rcktapp.api.ApiException;
import io.rcktapp.api.Chain;
import io.rcktapp.api.Endpoint;
import io.rcktapp.api.Handler;
import io.rcktapp.api.Request;
import io.rcktapp.api.Response;
import io.rcktapp.api.SC;
import io.rcktapp.api.service.Service;
import io.rcktapp.rql.Parser;
import io.rcktapp.rql.Predicate;
import io.rcktapp.rql.RQL;
import io.rcktapp.rql.elasticsearch.QueryDsl;

/**
 * @author tc-rocket
 *
 * Example Config:
 * 
 * dynamoEp.class=io.rcktapp.api.Endpoint
 * dynamoEp.includePaths=dynamo*
 * dynamoEp.methods=GET,POST
 * dynamoEp.handler=dynamoH
 * dynamoEp.config=tableMap=promo|promo-dev,loyalty-punchcard|loyalty-punchcard-dev&conditionalWriteConf=promo|attribute_not_exists(primarykey) OR enddate <= :enddate|enddate&blueprintRow=loyalty-punchcard|4045551111|aabbccddeeffgg
 * 
 *  - Valid config props
 *      - tableMap :                Maps a collection name to a dynamo table
 *                                  FORMAT: collection name | dynamodb name  (comma seperated)  - EXAMPLE: promo|promo-dev
 *      - conditionalWriteConf :    Allows a conditional write expression to be configured for a dynamo table
 *                                  FORMAT: collection name | withConditionExpression | payload fields  (comma seperated)  - EXAMPLE: promo|attribute_not_exists(primarykey) OR enddate <= :enddate|enddate        
 *      - blueprintRow :            Config which row should be used for building the collection typeMap (otherwise first row of scan will be used) - NOTE: you will probably not need to use this unless new columns are introduced to a table in the future.
 *                                  FORMAT: collection name | primaryKey | sortKey (optional)
 *      - appendTenantIdToPk :      Enables appending the tenant id to the primary key
 *                                  FORMAT: collection name (comma seperated)                            
 */
public class DynamoDbHandler implements Handler
{

   Logger                 log                       = LoggerFactory.getLogger(DynamoDbHandler.class);

   static ObjectMapper    mapper                    = new ObjectMapper();

   String                 awsRegion                 = "us-east-1";
   String                 tenantIdDelimiter         = "::";

   Map<String, TableInfo> collectionKeyTableInfoMap = new HashMap<>();

   private AmazonDynamoDB dynamoClient              = null;

   void initDynamoClient() throws Exception
   {
      if (dynamoClient == null)
         this.dynamoClient = AmazonDynamoDBClientBuilder.standard().withRegion(awsRegion).build();

   }

   @Override
   public void service(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response resp) throws Exception
   {
      initDynamoClient();

      String collectionKey = req.getEntityKey();
      TableInfo tableInfo = findOrCreateTableInfo(collectionKey, endpoint);

      if (tableInfo == null)
      {
         throw new ApiException(SC.SC_400_BAD_REQUEST, "A dynamo table is not configured for this collection key, please edit your query or your config and try again");
      }

      if (tableInfo.typeMap.isEmpty() || req.removeParam("refresh-types") != null)
      {
         // Type map is empty, attempt to reload it (this can happen with a new empty table and a get call is made against it)
         tableInfo.typeMap = buildTableTypeMap(tableInfo);
      }

      if (chain.getRequest().isDebug())
      {
         resp.debug("Dynamo Table: " + tableInfo.table + ", PK: " + tableInfo.primaryKey + ", SK: " + tableInfo.sortKey + ", Type Map: " + tableInfo.typeMap);
      }

      if (req.isGet())
      {
         handleGet(service, api, endpoint, action, chain, req, resp, tableInfo);
      }
      else if (req.isPost())
      {
         handlePost(service, api, endpoint, action, chain, req, resp, tableInfo);
      }
      else if (req.isDelete())
      {
         handleDelete(service, api, endpoint, action, chain, req, resp, tableInfo);
      }
      else
      {
         throw new ApiException(SC.SC_400_BAD_REQUEST, "This handler only supports GET, POST and DELETE requests");
      }
   }

   void handleGet(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response resp, TableInfo tableInfo) throws Exception
   {
      String nextKeyDelimeter = "~";
      String tableName = tableInfo.table;
      String primaryKey = tableInfo.primaryKey;
      String sortKey = tableInfo.sortKey;

      DynamoDB dynamoDB = new DynamoDB(dynamoClient);
      Table table = dynamoDB.getTable(tableName);

      int tenantId = 0;
      if (req.getApi().isMultiTenant())
      {
         tenantId = Integer.parseInt(req.removeParam("tenantId"));
      }

      String pkValFromUrl = req.getSubCollectionKey();
      if (pkValFromUrl != null)
      {
         req.putParam(primaryKey, pkValFromUrl);
      }

      int pageSize = req.getParam("pagesize") != null ? Integer.parseInt(req.removeParam("pagesize")) : 100;
      String next = req.removeParam("next");
      KeyAttribute[] nextKeys = null;

      if (next != null)
      {
         String[] sArr = next.split(nextKeyDelimeter);
         String pk = sArr[0];
         if (api.isMultiTenant() && tableInfo.appendTenantIdToPk)
         {
            pk = addTenantIdToKey(tenantId, pk);
         }
         KeyAttribute pkAttr = new KeyAttribute(tableInfo.primaryKey, pk);
         nextKeys = new KeyAttribute[]{pkAttr};
         if (sArr.length > 1)
         {
            KeyAttribute skAttr = new KeyAttribute(tableInfo.sortKey, tableInfo.cast(sArr[1], tableInfo.sortKey));
            nextKeys = new KeyAttribute[]{pkAttr, skAttr};
         }
      }

      FilterExpressionAndArgs filterExpress = buildFilterExpressionFromRequestParams(req.getParams(), primaryKey, sortKey, tableInfo);
      String filterExpression = filterExpress.buildExpression();

      String primaryKeyValue = null;
      Predicate pkPred = filterExpress.getExcludedPredicate(primaryKey);
      if (pkPred != null)
      {
         primaryKeyValue = pkPred.getTerms().get(1).getToken();
         if (api.isMultiTenant() && tableInfo.appendTenantIdToPk)
         {
            primaryKeyValue = addTenantIdToKey(tenantId, primaryKeyValue);
         }
      }

      List<Map> itemList = new ArrayList<>();

      Map<String, AttributeValue> lastKey = null;

      if (primaryKeyValue != null)
      {
         if (chain.getRequest().isDebug())
         {
            resp.debug("Query Type:   Query");
            resp.debug("Primary Key:  " + primaryKey + " = " + primaryKeyValue);
         }

         QuerySpec querySpec = new QuerySpec()//
                                              .withHashKey(primaryKey, primaryKeyValue)//
                                              .withMaxPageSize(pageSize)//
                                              .withMaxResultSize(pageSize);

         Predicate skPred = filterExpress.getExcludedPredicate(sortKey);
         if (skPred != null)
         {
            RangeKeyCondition rkc = predicateToRangeKeyCondition(skPred, tableInfo);
            querySpec = querySpec.withRangeKeyCondition(rkc);

            if (chain.getRequest().isDebug())
            {
               resp.debug("Sort Key:     " + rkc.getAttrName() + " " + rkc.getKeyCondition() + " " + rkc.getValues()[0]);
            }
         }

         if (nextKeys != null)
         {
            querySpec = querySpec.withExclusiveStartKey(nextKeys);
         }

         if (!filterExpress.getFields().isEmpty())
         {
            querySpec = querySpec.withFilterExpression(filterExpression)//
                                 .withNameMap(filterExpress.getFields())//
                                 .withValueMap(filterExpress.getArgs());

            if (chain.getRequest().isDebug())
            {
               resp.debug("Filter:");
               resp.debug(filterExpression);
               resp.debug(filterExpress.getFields());
               resp.debug(filterArgsToString(filterExpress.getArgs()));
            }
         }

         ItemCollection<QueryOutcome> queryResults = table.query(querySpec);

         for (Item item : queryResults)
         {
            itemList.add(item.asMap());
         }

         if (queryResults.getLastLowLevelResult() != null)
         {
            lastKey = queryResults.getLastLowLevelResult().getQueryResult().getLastEvaluatedKey();
         }

      }
      else
      {
         if (chain.getRequest().isDebug())
         {
            resp.debug("Query Type:   Scan");
         }

         ScanSpec scanSpec = new ScanSpec()//
                                           .withMaxPageSize(pageSize)//
                                           .withMaxResultSize(pageSize);

         if (!filterExpress.getFields().isEmpty())
         {
            scanSpec = scanSpec.withFilterExpression(filterExpression)//
                               .withNameMap(filterExpress.getFields())//
                               .withValueMap(filterExpress.getArgs());

            if (chain.getRequest().isDebug())
            {
               resp.debug("Filter:");
               resp.debug(filterExpression);
               resp.debug(filterExpress.getFields());
               resp.debug(filterArgsToString(filterExpress.getArgs()));
            }
         }

         if (nextKeys != null)
         {
            scanSpec = scanSpec.withExclusiveStartKey(nextKeys);
         }

         ItemCollection<ScanOutcome> scanResults = table.scan(scanSpec);

         for (Item item : scanResults)
         {
            itemList.add(item.asMap());
         }

         if (scanResults.getLastLowLevelResult() != null)
         {
            lastKey = scanResults.getLastLowLevelResult().getScanResult().getLastEvaluatedKey();
         }

      }

      String returnNext = null;
      if (lastKey != null && !lastKey.isEmpty())
      {
         returnNext = lastKey.get(tableInfo.primaryKey).getS();
         if (api.isMultiTenant() && tableInfo.appendTenantIdToPk)
         {
            returnNext = removeTenantIdFromKey(tenantId, returnNext);
         }

         if (lastKey.get(tableInfo.sortKey) != null)
         {
            String sortKeyVal = attributeValueAsString(lastKey.get(tableInfo.sortKey), tableInfo.sortKey, tableInfo.typeMap);
            returnNext = returnNext + nextKeyDelimeter + sortKeyVal;
         }
      }

      JSArray returnData = new JSArray();
      if (!itemList.isEmpty())
      {
         for (Map map : itemList)
         {
            if (api.isMultiTenant() && tableInfo.appendTenantIdToPk)
            {
               String pkValue = (String) map.get(primaryKey);
               map.put(primaryKey, removeTenantIdFromKey(tenantId, pkValue));
            }

            returnData.add(new JSObject(map));
         }
      }

      JSObject meta = new JSObject("pageSize", pageSize, "results", returnData.asList().size());
      if (returnNext != null)
      {
         meta.put("next", returnNext);
      }
      JSObject wrapper = new JSObject("meta", meta, "data", returnData);
      resp.setJson(wrapper);

   }

   void handlePost(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response resp, TableInfo tableInfo) throws Exception
   {
      String collectionKey = req.getEntityKey();
      String tableName = tableInfo.table;

      int tenantId = 0;
      if (req.getApi().isMultiTenant())
      {
         tenantId = Integer.parseInt(req.removeParam("tenantId"));
      }

      DynamoDB dynamoDB = new DynamoDB(dynamoClient);
      Table table = dynamoDB.getTable(tableName);

      // using this instead of the built in req.getJson(), because JSObject converts everything to strings even if they are sent up as a number
      Object payloadObj = jsonStringToObject(req.getBody());

      if (payloadObj instanceof List)
      {
         List l = (List) payloadObj;
         for (Object obj : l)
         {
            putMapToDynamo((Map) obj, table, collectionKey, req.getApi().isMultiTenant(), tenantId, tableInfo);
         }
      }
      else if (payloadObj instanceof Map)
      {
         putMapToDynamo((Map) payloadObj, table, collectionKey, req.getApi().isMultiTenant(), tenantId, tableInfo);
      }

      resp.setStatus(SC.SC_200_OK);

   }

   void handleDelete(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response resp, TableInfo tableInfo) throws Exception
   {
      int tenantId = 0;
      if (req.getApi().isMultiTenant())
      {
         tenantId = Integer.parseInt(req.removeParam("tenantId"));
      }

      // using this instead of the built in req.getJson(), because JSObject converts everything to strings even if they are sent up as a number
      Object payloadObj = jsonStringToObject(req.getBody());

      DynamoDB dynamoDB = new DynamoDB(dynamoClient);
      Table table = dynamoDB.getTable(tableInfo.table);

      if (payloadObj instanceof List)
      {
         List l = (List) payloadObj;
         for (Object obj : l)
         {
            deleteMapFromDynamo((Map) obj, table, api.isMultiTenant(), tenantId, tableInfo);
         }
      }
      else if (payloadObj instanceof Map)
      {
         deleteMapFromDynamo((Map) payloadObj, table, api.isMultiTenant(), tenantId, tableInfo);
      }

      resp.setStatus(SC.SC_200_OK);

   }

   private TableInfo findOrCreateTableInfo(String collectionKey, Endpoint endpoint)
   {
      TableInfo tableInfo = collectionKeyTableInfoMap.get(collectionKey);

      if (tableInfo == null)
      {
         String conditionalWriteConf = endpoint.getConfig("conditionalWriteConf");
         Map<String, String> conditionalExpressionMap = new HashMap<>();
         ListMap<String, String> conditionalExpressionFieldsMap = new ListMap<>();
         if (conditionalWriteConf != null)
         {
            String[] parts = conditionalWriteConf.split(",");
            for (String part : parts)
            {
               String[] arr = part.split("\\|");
               String collection = arr[0];
               String condition = arr[1];
               conditionalExpressionMap.put(collection, condition);

               if (arr.length > 2)
               {
                  for (int i = 2; i < arr.length; i++)
                  {
                     conditionalExpressionFieldsMap.put(collection, arr[i]);
                  }
               }
            }
         }

         String blueprintRow = endpoint.getConfig("blueprintRow");
         Map<String, String[]> blueprintRowMap = new HashMap<>();
         if (blueprintRow != null)
         {
            String[] parts = blueprintRow.split(",");
            for (String part : parts)
            {
               String[] arr = part.split("\\|");
               String collection = arr[0];
               blueprintRowMap.put(collection, arr);
            }
         }

         String appendTenantIdToPk = endpoint.getConfig("appendTenantIdToPk");
         List<String> appendTenantIdToPkList = new ArrayList<>();
         if (appendTenantIdToPk != null)
         {
            String[] parts = appendTenantIdToPk.split(",");
            appendTenantIdToPkList.addAll(Arrays.asList(parts));
         }

         String tableMap = endpoint.getConfig("tableMap");

         if (tableMap != null)
         {
            String[] parts = tableMap.split(",");
            for (String part : parts)
            {
               String[] arr = part.split("\\|");

               TableInfo ti = new TableInfo();
               ti.collectionKey = arr[0];
               ti.table = arr[1];
               ti.conditionalWriteExpression = conditionalExpressionMap.get(ti.collectionKey);
               ti.conditionalWriteExpressionFields = conditionalExpressionFieldsMap.get(ti.collectionKey);
               ti.appendTenantIdToPk = appendTenantIdToPkList.contains(ti.collectionKey);

               String[] bpArr = blueprintRowMap.get(ti.collectionKey);
               if (bpArr != null && bpArr.length >= 2)
               {
                  ti.bluePrintPK = bpArr[1];
                  if (bpArr.length == 3)
                     ti.bluePrintSK = bpArr[2];
               }

               // lookup key info
               DynamoDB dynamoDB = new DynamoDB(dynamoClient);
               TableDescription tableDescription = dynamoDB.getTable(ti.table).describe();
               List<KeySchemaElement> keySchema = tableDescription.getKeySchema();
               for (KeySchemaElement keyInfo : keySchema)
               {
                  if (keyInfo.getKeyType().equalsIgnoreCase("HASH"))
                  {
                     ti.primaryKey = keyInfo.getAttributeName();
                  }
                  else if (keyInfo.getKeyType().equalsIgnoreCase("RANGE"))
                  {
                     ti.sortKey = keyInfo.getAttributeName();
                  }
               }

               ti.typeMap = buildTableTypeMap(ti);

               collectionKeyTableInfoMap.put(ti.collectionKey, ti);
            }
         }

         tableInfo = collectionKeyTableInfoMap.get(collectionKey);
      }

      return tableInfo;
   }

   /**
    * This is needed since DynamoDB is schema-less and has no meta data methods that will
    * return all the known columns and data types for the columns.
    * 
    * As a work-around, this method will either grab a row in the table and use the fields and types
    * from that record to produce a typeMap for the TableInfo object.
    * 
    * The default behavior is to just grab the first row in the table to use as the "blue print" row.
    * 
    * If new columns are added to the table in the future and you need to use a different row for the "blue print"
    * you can configure a primary key and sort key (if needed) to specify what row should be used.
    * 
    * @param tableInfo
    * @param bluePrintConfig
    * @return Map<String, String>
    */
   Map<String, String> buildTableTypeMap(TableInfo tableInfo)
   {
      Map<String, String> typeMap = new HashMap<>();
      DynamoDB dynamoDB = new DynamoDB(dynamoClient);
      Table table = dynamoDB.getTable(tableInfo.table);

      Map<String, Object> bluePrintMap = null;

      if (tableInfo.bluePrintPK != null)
      {
         QuerySpec querySpec = new QuerySpec()//
                                              .withHashKey(tableInfo.primaryKey, tableInfo.bluePrintPK)//
                                              .withMaxPageSize(1)//
                                              .withMaxResultSize(1);
         if (tableInfo.bluePrintSK != null)
         {
            querySpec = querySpec.withRangeKeyCondition(new RangeKeyCondition(tableInfo.sortKey).eq(tableInfo.bluePrintSK));
         }

         ItemCollection<QueryOutcome> queryResults = table.query(querySpec);

         for (Item item : queryResults)
         {
            bluePrintMap = item.asMap();
         }

      }
      else
      {
         ScanSpec scanSpec = new ScanSpec()//
                                           .withMaxPageSize(1)//
                                           .withMaxResultSize(1);

         ItemCollection<ScanOutcome> scanResults = table.scan(scanSpec);
         for (Item item : scanResults)
         {
            bluePrintMap = item.asMap();
         }
      }

      if (bluePrintMap != null)
      {
         for (String k : bluePrintMap.keySet())
         {
            Object obj = bluePrintMap.get(k);
            typeMap.put(k, getTypeStringFromObject(obj));
         }
      }

      return typeMap;

   }

   /*
    * These match the string that dynamo uses for these types.
    * https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBMapper.DataTypes.html
    */
   static String getTypeStringFromObject(Object obj)
   {
      if (obj instanceof Number)
      {
         return "N";
      }
      else if (obj instanceof Boolean)
      {
         return "BOOL";
      }
      else
      {
         return "S";
      }
   }

   static String attributeValueAsString(AttributeValue attr, String name, Map<String, String> typeMap)
   {
      String type = typeMap.get(name);

      if (type != null)
      {
         switch (type)
         {
            case "N":
               return attr.getN();

            case "BOOL":
               return attr.getBOOL().toString();

         }
      }

      return attr.getS();
   }

   void putMapToDynamo(Map json, Table table, String collectionKey, boolean isMultiTenant, int tenantId, TableInfo tableInfo)
   {
      try
      {
         String conditionalExpression = tableInfo.conditionalWriteExpression;
         List<String> conditionalExpressionFields = tableInfo.conditionalWriteExpressionFields;

         Map m = new HashMap<>(json);

         if (isMultiTenant)
         {
            m.put("tenantid", tenantId);

            if (tableInfo.appendTenantIdToPk)
            {
               // add the tenantCode to the primary key
               String pk = (String) m.get(tableInfo.primaryKey);
               m.put(tableInfo.primaryKey, addTenantIdToKey(tenantId, pk));
            }
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

         table.putItem(putItemSpec);
      }
      catch (ConditionalCheckFailedException ccfe)
      {
         // catch this and do nothing.
         // this just means the that conditional write wasn't satisfied
         // so the record was not written
      }
   }

   void deleteMapFromDynamo(Map json, Table table, boolean isMultiTenant, int tenantId, TableInfo tableInfo)
   {
      try
      {
         Map m = new HashMap<>(json);

         KeyAttribute[] keys = null;

         if (!m.containsKey(tableInfo.primaryKey) || (tableInfo.sortKey != null && !m.containsKey(tableInfo.sortKey)))
         {
            String msg = "The JSON body must contain a '" + tableInfo.primaryKey + "' field";
            if (tableInfo.sortKey != null)
            {
               msg = msg + " and a '" + tableInfo.sortKey + "' field.";
            }

            throw new ApiException(SC.SC_400_BAD_REQUEST, msg);
         }

         String pk = (String) m.get(tableInfo.primaryKey);
         if (isMultiTenant && tableInfo.appendTenantIdToPk)
         {
            pk = addTenantIdToKey(tenantId, pk);
         }
         KeyAttribute pkAttr = new KeyAttribute(tableInfo.primaryKey, pk);
         keys = new KeyAttribute[]{pkAttr};
         if (tableInfo.sortKey != null)
         {
            KeyAttribute skAttr = new KeyAttribute(tableInfo.sortKey, m.get(tableInfo.sortKey));
            keys = new KeyAttribute[]{pkAttr, skAttr};
         }

         DeleteItemSpec spec = new DeleteItemSpec()//
                                                   .withPrimaryKey(keys);

         if (isMultiTenant)
         {
            spec = spec.withConditionExpression("tenantid = :val")//
                       .withValueMap(new ValueMap().withNumber(":val", tenantId));
         }

         table.deleteItem(spec);
      }
      catch (ConditionalCheckFailedException ccfe)
      {
         // catch this and do nothing.
         // this just means the that conditional check wasn't satisfied
         // so the record was not deleted
         // This is probably because the the record doesn't exist
      }

   }

   String addTenantIdToKey(int tenantId, String key)
   {
      return tenantId + tenantIdDelimiter + key;
   }

   String removeTenantIdFromKey(int tenantId, String key)
   {
      if (key != null)
      {
         int preLength = (tenantId + tenantIdDelimiter).length();
         return key.substring(preLength);
      }
      return key;
   }

   boolean predicatesContainField(List<Predicate> predicates, String field)
   {
      if (predicates != null)
      {
         for (Predicate pred : predicates)
         {
            if (FilterExpressionAndArgs.isKnownOperator(pred.getToken()) || FilterExpressionAndArgs.isKnownFunction(pred.getToken()))
            {
               String name = pred.getTerms().get(0).getToken();
               if (name.equalsIgnoreCase(field))
               {
                  return true;
               }
            }
            else if ("and".equalsIgnoreCase(pred.getToken()) || "or".equalsIgnoreCase(pred.getToken()))
            {
               return predicatesContainField(pred.getTerms(), field);
            }
         }
      }
      return false;
   }

   FilterExpressionAndArgs buildFilterExpressionFromRequestParams(Map<String, String> requestParams, String primaryKeyField, String sortKeyField, TableInfo tableInfo) throws Exception
   {
      RQL rql = new RQL("elastic");
      QueryDsl queryDsl = rql.toQueryDsl(requestParams);
      List<Predicate> predicates = queryDsl.getStmt().where;
      boolean hasPrimaryKey = predicatesContainField(predicates, primaryKeyField);
      List<String> excludeList = new ArrayList<>();
      if (hasPrimaryKey)
      {
         excludeList.add(primaryKeyField);
         excludeList.add(sortKeyField);
      }

      return buildFilterExpressionFromPredicates(predicates, new FilterExpressionAndArgs(tableInfo), "and", excludeList, 0);
   }

   FilterExpressionAndArgs buildFilterExpressionFromPredicates(List<Predicate> predicates, FilterExpressionAndArgs express, String andOr, List<String> excludes, int depth) throws Exception
   {
      if (predicates != null)
      {
         express.appendSpaces(depth);
         express.append("(");

         int cnt = 0;
         int excludeCnt = 0;
         depth++;

         for (Predicate pred : predicates)
         {
            String name = pred.getTerms().get(0).getToken();
            if (excludes.contains(name.toLowerCase()))
            {
               express.addExcludedPredicate(name, pred);
               excludeCnt++;
            }
            else
            {
               if (cnt - excludeCnt > 0 && cnt < predicates.size())
               {
                  express.append("\n");
                  express.appendSpaces(depth);
                  express.append(andOr);
               }

               if (FilterExpressionAndArgs.isKnownOperator(pred.getToken()))
               {
                  String val = pred.getTerms().get(1).getToken();

                  express.append("\n");
                  express.appendSpaces(depth);
                  express.appendOperatorExpression(pred.getToken(), name, val);
               }
               else if (FilterExpressionAndArgs.isKnownFunction(pred.getToken()))
               {
                  String val = pred.getTerms().get(1).getToken();

                  express.append("\n");
                  express.appendSpaces(depth);
                  express.appendFunctionExpression(pred.getToken(), name, val);
               }
               else if ("and".equalsIgnoreCase(pred.getToken()) || "or".equalsIgnoreCase(pred.getToken()))
               {
                  buildFilterExpressionFromPredicates(pred.getTerms(), express, pred.getToken(), excludes, depth);
               }
               else
               {
                  throw new Exception("unexpected rql token: " + pred.getToken());
               }
            }
            cnt++;

         }

         depth--;
         express.append("\n");
         express.appendSpaces(depth);
         express.append(")");

      }
      return express;
   }

   String filterArgsToString(Map<String, Object> args)
   {
      if (args != null)
      {
         String s = "{";
         int cnt = 0;
         for (String k : args.keySet())
         {
            s = s + k + "=" + args.get(k) + " (" + getTypeStringFromObject(args.get(k)) + ")";
            if (cnt < args.keySet().size() - 1)
            {
               s = s + ", ";
            }

            cnt++;
         }
         s = s + "}";
         return s;
      }
      return "null";
   }

   RangeKeyCondition predicateToRangeKeyCondition(Predicate pred, TableInfo tableInfo)
   {
      String name = pred.getTerms().get(0).getToken();
      Object val = tableInfo.cast((String) pred.getTerms().get(1).getToken(), name);

      RangeKeyCondition rkc = new RangeKeyCondition(name);
      switch (pred.getToken())
      {
         case "eq":
            rkc.eq(val);
            break;

         case "gt":
            rkc.gt(val);
            break;

         case "ge":
            rkc.ge(val);
            break;

         case "lt":
            rkc.lt(val);
            break;

         case "le":
            rkc.le(val);
            break;

         case "sw":
            rkc.beginsWith((String) val);
            break;

         default :
            throw new ApiException(SC.SC_400_BAD_REQUEST, "Operator '" + pred.getToken() + "' is not supported for a dynamo range key condition");
      }

      return rkc;
   }

   static Object jsonStringToObject(String jsonStr) throws JsonParseException, JsonMappingException, IOException
   {
      return mapper.readValue(jsonStr, Object.class);
   }

   static Map map(Object... objects)
   {
      Map m = new HashMap<>();
      for (int i = 0; i < objects.length; i = i + 2)
      {
         m.put(objects[i], objects[i + 1]);
      }
      return m;
   }

   public void setAwsRegion(String awsRegion)
   {
      this.awsRegion = awsRegion;
   }

   public void setTenantIdDelimiter(String tenantIdDelimiter)
   {
      this.tenantIdDelimiter = tenantIdDelimiter;
   }

   static class TableInfo
   {
      String              collectionKey;
      String              table;
      String              primaryKey;
      String              sortKey;
      String              conditionalWriteExpression;
      List<String>        conditionalWriteExpressionFields;
      Map<String, String> typeMap;
      String              bluePrintPK;
      String              bluePrintSK;
      boolean             appendTenantIdToPk = false;

      Object cast(String value, String name)
      {
         String type = typeMap.get(name);

         if (type != null)
         {
            switch (type)
            {
               case "N":
                  return Long.parseLong(value);

               case "BOOL":
                  return Boolean.parseBoolean(value);

            }
         }

         return Parser.dequote(value);
      }

   }

   static class FilterExpressionAndArgs
   {
      public static Map<String, String> OPERATOR_MAP = new HashMap<>();
      public static Map<String, String> FUNCTION_MAP = new HashMap<>();
      static
      {
         OPERATOR_MAP.put("eq", "=");
         OPERATOR_MAP.put("ne", "<>");
         OPERATOR_MAP.put("gt", ">");
         OPERATOR_MAP.put("ge", ">=");
         OPERATOR_MAP.put("lt", "<");
         OPERATOR_MAP.put("le", "<=");

         FUNCTION_MAP.put("w", "contains");
         FUNCTION_MAP.put("sw", "begins_with");
      }

      TableInfo              tableInfo;

      int                    fieldCnt           = 0;
      int                    argCnt             = 0;
      StringBuilder          buffer             = new StringBuilder();
      Map<String, String>    fields             = new LinkedHashMap<>();
      Map<String, Object>    args               = new LinkedHashMap<>();

      Map<String, Predicate> excludedPredicates = new HashMap<>();

      public FilterExpressionAndArgs(TableInfo tableInfo)
      {
         super();
         this.tableInfo = tableInfo;
      }

      String nextFieldName()
      {
         fieldCnt++;
         return "#name" + fieldCnt;
      }

      String nextArgName()
      {
         argCnt++;
         return ":val" + argCnt;
      }

      public static boolean isKnownOperator(String operator)
      {
         return OPERATOR_MAP.containsKey(operator);
      }

      public static boolean isKnownFunction(String func)
      {
         return FUNCTION_MAP.containsKey(func);
      }

      public void appendOperatorExpression(String token, String field, String val)
      {
         String fieldName = nextFieldName();
         String argName = nextArgName();
         buffer.append(fieldName + " " + OPERATOR_MAP.get(token) + " " + argName);
         fields.put(fieldName, field);
         args.put(argName, tableInfo.cast(val, field));
      }

      public void appendFunctionExpression(String token, String field, String val)
      {
         String fieldName = nextFieldName();
         String argName = nextArgName();
         buffer.append(FUNCTION_MAP.get(token) + "(" + fieldName + ", " + argName + ")");
         fields.put(fieldName, field);
         args.put(argName, tableInfo.cast(val, field));
      }

      public void appendSpaces(int depth)
      {
         buffer.append(spaces(depth));
      }

      public void append(String s)
      {
         buffer.append(s);
      }

      public String buildExpression()
      {
         return buffer.toString();
      }

      public String spaces(int depth)
      {
         String s = "";
         for (int i = 0; i < depth; i++)
         {
            s = s + "  ";
         }
         return s;
      }

      public Map<String, Object> getArgs()
      {
         return args;
      }

      public Map<String, String> getFields()
      {
         return fields;
      }

      public void addExcludedPredicate(String name, Predicate pred)
      {
         this.excludedPredicates.put(name, pred);
      }

      public Predicate getExcludedPredicate(String name)
      {
         return this.excludedPredicates.get(name);
      }

   }

}
