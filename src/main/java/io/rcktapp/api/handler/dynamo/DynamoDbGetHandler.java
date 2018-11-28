/**
 * 
 */
package io.rcktapp.api.handler.dynamo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.KeyAttribute;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import io.forty11.web.js.JSArray;
import io.forty11.web.js.JSObject;
import io.rcktapp.api.Action;
import io.rcktapp.api.Api;
import io.rcktapp.api.Chain;
import io.rcktapp.api.Collection;
import io.rcktapp.api.Endpoint;
import io.rcktapp.api.Request;
import io.rcktapp.api.Response;
import io.rcktapp.api.Table;
import io.rcktapp.api.service.Service;
import io.rcktapp.rql.Predicate;
import io.rcktapp.rql.Rql;
import io.rcktapp.rql.dynamo.DynamoExpression;
import io.rcktapp.rql.dynamo.DynamoRql;

/**
 * @author tc-rocket
 *
 */
public class DynamoDbGetHandler extends DynamoDbHandler
{

   @Override
   public void service(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res) throws Exception
   {
      Collection collection = findCollectionOrThrow404(api, chain, req);
      Table table = collection.getEntity().getTable();
      DynamoDb db = (DynamoDb) table.getDb();
      AmazonDynamoDB dynamoClient = db.getDynamoClient();
      String pk = DynamoDb.findPartitionKeyName(table);
      String sk = DynamoDb.findSortKeyName(table);

      if (chain.getRequest().isDebug())
      {
         res.debug("Dynamo Table: " + table.getName() + ", PK: " + pk + ", SK: " + sk);
      }

      int tenantId = 0;
      if (req.getApi().isMultiTenant())
      {
         tenantId = Integer.parseInt(req.removeParam("tenantId"));
      }

      int pageSize = req.getParam("pagesize") != null ? Integer.parseInt(req.removeParam("pagesize")) : 100;
      Set<String> includes = new HashSet<>(splitToList(req.removeParam("includes")));
      Set<String> excludes = new HashSet<>(splitToList(req.removeParam("excludes")));

      DynamoRql rql = (DynamoRql) Rql.getRql(db.getType());
      DynamoExpression dynamoExpression = rql.buildDynamoExpression(req.getParams(), table);

      // TODO handle "next"
      KeyAttribute[] nextKeys = null;

      String primaryKeyValue = null;
      Predicate pkPred = dynamoExpression.getExcludedPredicate(pk);
      if (pkPred != null)
      {
         primaryKeyValue = pkPred.getTerms().get(1).getToken();
         // TODO add/remove tenantid to key - where/how is this configured
         //         if (api.isMultiTenant() && tableInfo.appendTenantIdToPk)
         //         {
         //            primaryKeyValue = addTenantIdToKey(tenantId, primaryKeyValue);
         //         }
      }

      DynamoResult dynamoResult = null;
      if (primaryKeyValue != null)
      {
         // Query
      }
      else
      {
         // Scan
         dynamoResult = doScan(dynamoExpression, dynamoClient, table, chain, res, pageSize, nextKeys);
      }

      String returnNext = null;
      /*
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
      
         String sortField = filterExpress.getSortField();
         if (!sortField.equals(tableInfo.sortKey) && lastKey.get(sortField) != null)
         {
            String sortKeyVal = attributeValueAsString(lastKey.get(sortField), sortField, tableInfo.typeMap);
            returnNext = returnNext + nextKeyDelimeter + sortKeyVal;
         }
      }
      */

      JSArray returnData = new JSArray();
      if (dynamoResult != null && !dynamoResult.items.isEmpty())
      {
         for (Map map : dynamoResult.items)
         {
            // TODO
            //            if (api.isMultiTenant() && tableInfo.appendTenantIdToPk)
            //            {
            //               String pkValue = (String) map.get(primaryKey);
            //               map.put(primaryKey, removeTenantIdFromKey(tenantId, pkValue));
            //            }

            returnData.add(new JSObject(includeExclude(map, includes, excludes)));
         }
      }

      JSObject meta = new JSObject("pageSize", pageSize, "results", returnData.asList().size());
      if (returnNext != null)
      {
         meta.put("next", returnNext);
      }
      JSObject wrapper = new JSObject("meta", meta, "data", returnData);
      res.setJson(wrapper);

   }

   DynamoResult doQuery()
   {
      return null;
   }

   DynamoResult doScan(DynamoExpression dynamoExpression, AmazonDynamoDB dynamoClient, Table table, Chain chain, Response res, int pageSize, KeyAttribute[] nextKeys)
   {
      DynamoDB dynamoDB = new DynamoDB(dynamoClient);
      com.amazonaws.services.dynamodbv2.document.Table dynamoTable = dynamoDB.getTable(table.getName());
      String expressionStr = dynamoExpression.buildExpression();

      if (chain.getRequest().isDebug())
      {
         res.debug("Query Type:   Scan");
      }

      ScanSpec scanSpec = new ScanSpec()//
                                        .withMaxPageSize(pageSize)//
                                        .withMaxResultSize(pageSize);

      if (!dynamoExpression.getFields().isEmpty())
      {
         scanSpec = scanSpec.withFilterExpression(expressionStr)//
                            .withNameMap(dynamoExpression.getFields());

         if (!dynamoExpression.getArgs().isEmpty())
         {
            scanSpec = scanSpec.withValueMap(dynamoExpression.getArgs());
         }

         if (chain.getRequest().isDebug())
         {
            res.debug("Filter:");
            res.debug(expressionStr);
            res.debug(dynamoExpression.getFields());
            res.debug(filterArgsToString(dynamoExpression.getArgs()));
         }
      }

      if (nextKeys != null)
      {
         scanSpec = scanSpec.withExclusiveStartKey(nextKeys);
      }

      ItemCollection<ScanOutcome> scanResults = dynamoTable.scan(scanSpec);

      List<Map> items = new ArrayList<>();
      Map<String, AttributeValue> lastKey = null;

      for (Item item : scanResults)
      {
         items.add(item.asMap());
      }

      if (scanResults.getLastLowLevelResult() != null)
      {
         lastKey = scanResults.getLastLowLevelResult().getScanResult().getLastEvaluatedKey();
      }

      return new DynamoResult(items, lastKey);

   }

   String filterArgsToString(Map<String, Object> args)
   {
      if (args != null)
      {
         String s = "{";
         int cnt = 0;
         for (String k : args.keySet())
         {
            s = s + k + "=" + args.get(k) + " (" + DynamoDb.getTypeStringFromObject(args.get(k)) + ")";
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

   Map includeExclude(Map m, Set<String> includes, Set<String> excludes)
   {
      if (m != null)
      {
         if (!includes.isEmpty())
         {
            Map newMap = new HashMap<>();
            for (String include : includes)
            {
               if (m.containsKey(include))
               {
                  newMap.put(include, m.get(include));
               }
            }
            m = newMap;
         }
         else if (!excludes.isEmpty())
         {
            for (String exclude : excludes)
            {
               m.remove(exclude);
            }
         }
      }
      return m;
   }

   static class DynamoResult
   {
      List<Map>                   items;
      Map<String, AttributeValue> lastKey;

      public DynamoResult(List<Map> items, Map<String, AttributeValue> lastKey)
      {
         super();
         this.items = items;
         this.lastKey = lastKey;
      }

      public List<Map> getItems()
      {
         return items;
      }

      public Map<String, AttributeValue> getLastKey()
      {
         return lastKey;
      }

   }

}
