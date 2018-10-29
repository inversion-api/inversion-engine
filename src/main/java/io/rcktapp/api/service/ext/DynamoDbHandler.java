/**
 * 
 */
package io.rcktapp.api.service.ext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.RangeKeyCondition;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
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
 */
public class DynamoDbHandler implements Handler
{

   Logger                 log                       = LoggerFactory.getLogger(DynamoDbHandler.class);

   static ObjectMapper    mapper                    = new ObjectMapper();

   /**
    * a csv string of collection name | dynamo table name of allowed dynamo tables
    * EXAMPLE:
    * promo|promo-dev,token|token-tracker
    */
   String                 tablemap;

   /**
    * A csv of pipe delimited info to configure a conditional write
    * The format is collection name | withConditionExpression | payload fields  (comma seperated)
    * NOTE: to specify multiple payload fields you additional pipes.
    * EXAMPLE:
    * promo|attribute_not_exists(primarykey) OR enddate <= :enddate OR startdate < :startdate|enddate|startdate
    * 
    */
   String                 conditionalWriteConf;
   String                 awsRegion                 = "us-east-1";

   //   Map<String, String>     collectionTableMap;
   //
   //   Map<String, String>     conditionalExpressionMap;
   //   ListMap<String, String> conditionalExpressionFieldsMap;
   Map<String, TableInfo> collectionKeyTableInfoMap = new HashMap<>();

   private AmazonDynamoDB dynamoClient              = null;

   void initDynamoClient() throws Exception
   {
      if (dynamoClient == null)
         this.dynamoClient = AmazonDynamoDBClientBuilder.standard().withRegion(awsRegion).build();

      //      if (collectionTableMap == null)
      //      {
      //         collectionTableMap = new HashMap<>();
      //         if (tablemap != null)
      //         {
      //            String[] parts = tablemap.split(",");
      //            for (String part : parts)
      //            {
      //               String[] arr = part.split("\\|");
      //               collectionTableMap.put(arr[0], arr[1]);
      //            }
      //         }
      //      }
      //
      //      if (conditionalExpressionMap == null)
      //      {
      //         conditionalExpressionMap = new HashMap<>();
      //         conditionalExpressionFieldsMap = new ListMap<>();
      //
      //         if (conditionalWriteConf != null)
      //         {
      //            String[] parts = conditionalWriteConf.split(",");
      //            for (String part : parts)
      //            {
      //               String[] arr = part.split("\\|");
      //               String collection = arr[0];
      //               String condition = arr[1];
      //               conditionalExpressionMap.put(collection, condition);
      //
      //               if (arr.length > 2)
      //               {
      //                  for (int i = 2; i < arr.length; i++)
      //                  {
      //                     conditionalExpressionFieldsMap.put(collection, arr[i]);
      //                  }
      //               }
      //            }
      //         }
      //
      //      }

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

               collectionKeyTableInfoMap.put(ti.collectionKey, ti);
            }
         }

         tableInfo = collectionKeyTableInfoMap.get(collectionKey);
      }

      return tableInfo;
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

      if (chain.getRequest().isDebug())
      {
         resp.debug("Dynamo Table: " + tableInfo.table + ", PK: " + tableInfo.primaryKey + ", SK: " + tableInfo.sortKey);
      }

      if (req.isPost())
      {
         handlePost(service, api, endpoint, action, chain, req, resp, tableInfo);
      }
      else if (req.isGet())
      {
         handleGet(service, api, endpoint, action, chain, req, resp, tableInfo);
      }
      else
      {
         throw new ApiException(SC.SC_400_BAD_REQUEST, "This handler only supports GET and POST requests");
      }
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

   void handleGet(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response resp, TableInfo tableInfo) throws Exception
   {
      String nextKeyDelimeter = "~";
      String tableName = tableInfo.table;
      String primaryKey = tableInfo.primaryKey;
      String sortKey = tableInfo.sortKey;

      DynamoDB dynamoDB = new DynamoDB(dynamoClient);
      Table table = dynamoDB.getTable(tableName);

      String pkValFromUrl = req.getSubCollectionKey();
      if (pkValFromUrl != null)
      {
         req.putParam(primaryKey, pkValFromUrl);
      }

      int pageSize = req.getParam("pagesize") != null ? Integer.parseInt(req.removeParam("pagesize")) : 100;
      String next = req.removeParam("next");
      String nextPkName = null;
      Object nextPkVal = null;

      if (next != null)
      {
         String[] sArr = next.split(nextKeyDelimeter);
         nextPkName = sArr[0];
         nextPkVal = sArr[1];

         if (sArr[1].startsWith("n:"))
         {
            nextPkVal = Integer.parseInt(sArr[1].substring(2));
         }
      }

      //////
      ScanSpec scanSpec2 = new ScanSpec()//
                                         .withMaxPageSize(1)//
                                         .withMaxResultSize(1);

      ItemCollection<ScanOutcome> scanResults2 = table.scan(scanSpec2);
      Map<String, Class> typeMap = new HashMap<>();
      for (Item item : scanResults2)
      {
         Map<String, Object> m = item.asMap();
         for (String k : m.keySet())
         {
            Object obj = m.get(k);
            if (obj instanceof Number)
            {
               typeMap.put(k, Number.class);
            }
            else if (obj instanceof Boolean)
            {
               typeMap.put(k, Boolean.class);
            }
            else
            {
               typeMap.put(k, String.class);
            }
         }
      }
      ///////

     // req.removeParam("tenantId");

      FilterExpressionAndArgs filterExpress = buildFilterExpressionFromRequestParams(req.getParams(), primaryKey, sortKey);
      String filterExpression = filterExpress.buildExpression();

      String primaryKeyValue = null;
      Predicate pkPred = filterExpress.getExcludedPredicate(primaryKey);
      if (pkPred != null)
      {
         primaryKeyValue = pkPred.getTerms().get(1).getToken();
      }

      JSArray returnData = new JSArray();
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
            RangeKeyCondition rkc = predicateToRangeKeyCondition(skPred);
            querySpec = querySpec.withRangeKeyCondition(rkc);

            if (chain.getRequest().isDebug())
            {
               resp.debug("Sort Key:     " + rkc.getAttrName() + " " + rkc.getKeyCondition() + " " + rkc.getValues()[0]);
            }
         }

         if (nextPkName != null && nextPkVal != null)
         {
            querySpec = querySpec.withExclusiveStartKey(nextPkName, nextPkVal);
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
               resp.debug(filterExpress.getArgs());
            }
         }

         ItemCollection<QueryOutcome> queryResults = table.query(querySpec);

         for (Item item : queryResults)
         {
            returnData.add(new JSObject(item.asMap()));
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
               resp.debug(filterExpress.getArgs());
            }
         }

         if (nextPkName != null && nextPkVal != null)
         {
            scanSpec = scanSpec.withExclusiveStartKey(nextPkName, nextPkVal);
         }

         ItemCollection<ScanOutcome> scanResults = table.scan(scanSpec);

         for (Item item : scanResults)
         {
            returnData.add(new JSObject(item.asMap()));
         }

         if (scanResults.getLastLowLevelResult() != null)
         {
            lastKey = scanResults.getLastLowLevelResult().getScanResult().getLastEvaluatedKey();
         }

      }

      String returnNext = null;
      if (lastKey != null && !lastKey.isEmpty())
      {
         for (String k : lastKey.keySet())
         {
            if (lastKey.get(k).getS() != null)
            {
               returnNext = k + nextKeyDelimeter + lastKey.get(k).getS();
            }
            else if (lastKey.get(k).getN() != null)
            {
               returnNext = k + nextKeyDelimeter + lastKey.get(k).getN();
            }
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

   FilterExpressionAndArgs buildFilterExpressionFromRequestParams(Map<String, String> requestParams, String primaryKeyField, String sortKeyField) throws Exception
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

      return buildFilterExpressionFromPredicates(predicates, new FilterExpressionAndArgs(), "and", excludeList, 0);
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
               if (cnt - excludeCnt > 0 && cnt < predicates.size() - excludeCnt)
               {
                  express.append("\n");
                  express.appendSpaces(depth);
                  express.append(andOr);
               }

               if (FilterExpressionAndArgs.isKnownOperator(pred.getToken()))
               {
                  Object val = convertValueTypeIfNeeded(pred.getTerms().get(1).getToken(), name, pred.getToken());

                  express.append("\n");
                  express.appendSpaces(depth);
                  express.appendOperatorExpression(pred.getToken(), name, val);
               }
               else if (FilterExpressionAndArgs.isKnownFunction(pred.getToken()))
               {
                  Object val = convertValueTypeIfNeeded(pred.getTerms().get(1).getToken(), name, pred.getToken());

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

   Object convertValueTypeIfNeeded(String value, String name, String operator)
   {
      List<String> numberOperators = new ArrayList<>(Arrays.asList("gt", "ge", "lt", "le"));

      if (name.equalsIgnoreCase("tenantid"))
      {
         return Integer.parseInt(value);
      }
      else if (numberOperators.contains(operator))
      {
         try
         {
            return Long.parseLong(value);
         }
         catch (NumberFormatException nfe)
         {
         }
      }
      else if (value.startsWith("n:"))
      {
         try
         {
            return Long.parseLong(value.substring(2));
         }
         catch (NumberFormatException nfe)
         {
         }
      }

      return Parser.dequote(value);
   }

   RangeKeyCondition predicateToRangeKeyCondition(Predicate pred)
   {
      String name = pred.getTerms().get(0).getToken();
      Object val = pred.getTerms().get(1).getToken();

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

   static class TableInfo
   {
      String       collectionKey;
      String       table;
      String       primaryKey;
      String       sortKey;
      String       conditionalWriteExpression;
      List<String> conditionalWriteExpressionFields;
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

      int                    fieldCnt           = 0;
      int                    argCnt             = 0;
      StringBuilder          buffer             = new StringBuilder();
      Map<String, String>    fields             = new HashMap<>();
      Map<String, Object>    args               = new HashMap<>();

      Map<String, Predicate> excludedPredicates = new HashMap<>();

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

      public void appendOperatorExpression(String token, String field, Object val)
      {
         String fieldName = nextFieldName();
         String argName = nextArgName();
         buffer.append(fieldName + " " + OPERATOR_MAP.get(token) + " " + argName);
         fields.put(fieldName, field);
         args.put(argName, val);
      }

      public void appendFunctionExpression(String token, String field, Object val)
      {
         String fieldName = nextFieldName();
         String argName = nextArgName();
         buffer.append(FUNCTION_MAP.get(token) + "(" + fieldName + ", " + argName + ")");
         fields.put(fieldName, field);
         args.put(argName, val);
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
