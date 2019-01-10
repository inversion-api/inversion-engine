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
package io.rcktapp.api.handler.dynamo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.KeyAttribute;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.RangeKeyCondition;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.api.QueryApi;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
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
import io.rcktapp.rql.Order;
import io.rcktapp.rql.Predicate;
import io.rcktapp.rql.Rql;
import io.rcktapp.rql.dynamo.DynamoExpression;
import io.rcktapp.rql.dynamo.DynamoRql;

/**
 * @author tc-rocket
 *
 * Endpoint/Action Config
 *  - appendTenantIdToPk :        Enables appending the tenant id to the primary key
 *                                FORMAT: collection name (comma separated)
 *
 */
public class DynamoDbGetHandler extends DynamoDbHandler
{

   protected String nextKeyDelimeter = "~";

   @Override
   public void service(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res) throws Exception
   {
      Collection collection = api.getCollection(req.getCollectionKey(), DynamoDb.class);
      Table table = collection.getEntity().getTable();
      DynamoDb db = (DynamoDb) table.getDb();
      com.amazonaws.services.dynamodbv2.document.Table dynamoTable = db.getDynamoTable(table.getName());

      String tenantIdOrCode = null;
      if (req.getApi().isMultiTenant())
      {
         tenantIdOrCode = req.removeParam("tenantId");
         if (tenantIdOrCode == null)
         {
            tenantIdOrCode = req.getTenantCode();
         }
      }

      int pageSize = req.getParam("pagesize") != null ? Integer.parseInt(req.removeParam("pagesize")) : 100;
      String next = req.removeParam("next");

      Set<String> includes = new HashSet<>(splitToList(req.removeParam("includes")));
      Set<String> excludes = new HashSet<>(splitToList(req.removeParam("excludes")));

      DynamoRql rql = (DynamoRql) Rql.getRql(db.getType());
      DynamoExpression dynamoExpression = rql.buildDynamoExpression(req.getParams(), table);
      Order order = dynamoExpression.getOrder();

      DynamoIndex dynamoIdx = dynamoExpression.getIndex();
      String pk = dynamoIdx.getPartitionKey();
      String sk = dynamoIdx.getSortKey();
      boolean appendTenantIdToPk = !dynamoIdx.getType().equals(DynamoDb.GLOBAL_SECONDARY_TYPE) ? isAppendTenantIdToPk(chain, collection.getName()) : false;

      if (chain.getRequest().isDebug())
      {
         res.debug("Dynamo Table:       " + table.getName() + ", PK: " + pk + ", SK: " + sk);

         List<DynamoIndex> lsIndexes = DynamoDb.findIndexesByType(table, DynamoDb.GLOBAL_SECONDARY_TYPE);

         if (!lsIndexes.isEmpty())
         {
            res.debug("Global Sec Indexes:  " + lsIndexes.stream().map(i -> i.getName()).collect(Collectors.joining(",")));
         }

         lsIndexes = DynamoDb.findIndexesByType(table, DynamoDb.LOCAL_SECONDARY_TYPE);

         if (!lsIndexes.isEmpty())
         {
            res.debug("Local Sec Indexes:  " + lsIndexes.stream().map(i -> i.getName()).collect(Collectors.joining(",")));
         }

      }

      KeyAttribute[] nextKeys = null;

      if (next != null)
      {
         List<KeyAttribute> keyAttrList = new ArrayList<>();
         String[] sArr = next.split(nextKeyDelimeter);
         String nextPkVal = sArr[0];
         if (api.isMultiTenant() && appendTenantIdToPk)
         {
            nextPkVal = addTenantIdToKey(tenantIdOrCode, nextPkVal);
         }
         keyAttrList.add(new KeyAttribute(pk, nextPkVal));

         if (sArr.length > 1)
         {
            keyAttrList.add(new KeyAttribute(sk, DynamoDb.cast(sArr[1], sk, table)));
         }

         if (sArr.length > 2 && order != null && !sk.equals(order.col))
         {
            keyAttrList.add(new KeyAttribute(order.col, DynamoDb.cast(sArr[2], order.col, table)));
         }

         nextKeys = keyAttrList.toArray(new KeyAttribute[keyAttrList.size()]);
      }

      Object partitionKeyValue = null;
      Predicate pkPred = dynamoExpression.getExcludedPredicate(pk);
      if (pkPred != null)
      {
         partitionKeyValue = pkPred.getTerms().get(1).getToken();
         if (api.isMultiTenant() && appendTenantIdToPk)
         {
            partitionKeyValue = addTenantIdToKey(tenantIdOrCode, partitionKeyValue.toString());
         }
         else
            partitionKeyValue = DynamoDb.cast(pkPred.getTerms().get(1).getToken(), pk, table);
      }

      DynamoResult dynamoResult = null;
      if (partitionKeyValue != null)
      {
         // Query
         dynamoResult = doQuery(dynamoExpression, dynamoTable, chain, res, pageSize, nextKeys, pk, partitionKeyValue);
      }
      else
      {
         // Scan
         dynamoResult = doScan(dynamoExpression, dynamoTable, chain, res, pageSize, nextKeys);
      }

      String returnNext = null;
      if (dynamoResult != null && dynamoResult.lastKey != null && !dynamoResult.lastKey.isEmpty())
      {
         returnNext = dynamoResult.lastKey.get(pk).getS();
         if (api.isMultiTenant() && appendTenantIdToPk)
         {
            returnNext = removeTenantIdFromKey(tenantIdOrCode, returnNext);
         }

         if (dynamoResult.lastKey.get(sk) != null)
         {
            String sortKeyVal = DynamoDb.attributeValueAsString(dynamoResult.lastKey.get(sk), sk, table);
            returnNext = returnNext + nextKeyDelimeter + sortKeyVal;
         }

         if (order != null && !order.col.equals(sk) && dynamoResult.lastKey.get(order.col) != null)
         {
            String sortKeyVal = DynamoDb.attributeValueAsString(dynamoResult.lastKey.get(order.col), order.col, table);
            returnNext = returnNext + nextKeyDelimeter + sortKeyVal;
         }

      }

      JSArray returnData = new JSArray();
      if (dynamoResult != null && !dynamoResult.items.isEmpty())
      {
         for (Map map : dynamoResult.items)
         {
            if (api.isMultiTenant() && appendTenantIdToPk)
            {
               String pkValue = (String) map.get(pk);
               map.put(pk, removeTenantIdFromKey(tenantIdOrCode, pkValue));
            }

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

   DynamoResult doQuery(DynamoExpression dynamoExpression, com.amazonaws.services.dynamodbv2.document.Table dynamoTable, Chain chain, Response res, int pageSize, KeyAttribute[] nextKeys, String pk, Object primaryKeyValue)
   {
      String expressionStr = dynamoExpression.buildExpression();
      Order order = dynamoExpression.getOrder();
      String orderCol = order != null ? order.col : "";
      String orderDir = order != null ? order.dir : null;

      if (chain.getRequest().isDebug())
      {
         res.debug("Query Type:         Query");
         res.debug("Partition Key:      " + pk + " = " + primaryKeyValue);
      }

      QueryApi queryApi = dynamoTable;
      if (dynamoExpression.getIndex() != null && !dynamoExpression.getIndex().getType().equals(DynamoDb.PRIMARY_TYPE))
      {
         queryApi = dynamoTable.getIndex(dynamoExpression.getIndex().getName());
         if (chain.getRequest().isDebug())
         {
            res.debug("Index:              " + dynamoExpression.getIndex().getName());
         }
      }

      QuerySpec querySpec = new QuerySpec()//
                                           .withHashKey(pk, primaryKeyValue)//
                                           .withMaxPageSize(pageSize)//
                                           .withMaxResultSize(pageSize);

      Predicate skPred = dynamoExpression.getExcludedPredicate(orderCol);
      if (skPred != null)
      {
         RangeKeyCondition rkc = DynamoDb.predicateToRangeKeyCondition(skPred, dynamoExpression.getTable());
         querySpec = querySpec.withRangeKeyCondition(rkc);

         if (chain.getRequest().isDebug())
         {
            res.debug("Sort Key:           " + rkc.getAttrName() + " " + rkc.getKeyCondition() + " " + rkc.getValues()[0]);
         }
      }

      if (orderDir != null)
      {
         boolean scanForward = !orderDir.equalsIgnoreCase("DESC");
         querySpec.withScanIndexForward(scanForward);
         if (chain.getRequest().isDebug())
         {
            res.debug("Sorting By:         " + orderCol + " " + orderDir);
         }
      }

      if (nextKeys != null)
      {
         querySpec = querySpec.withExclusiveStartKey(nextKeys);
      }

      if (!dynamoExpression.getFields().isEmpty())
      {
         querySpec = querySpec.withFilterExpression(expressionStr)//
                              .withNameMap(dynamoExpression.getFields());

         if (!dynamoExpression.getArgs().isEmpty())
         {
            querySpec = querySpec.withValueMap(dynamoExpression.getArgs());
         }

         if (chain.getRequest().isDebug())
         {
            res.debug("Filter:");
            res.debug(expressionStr);
            res.debug(dynamoExpression.getFields());
            res.debug(filterArgsToString(dynamoExpression.getArgs()));
         }
      }

      ItemCollection<QueryOutcome> queryResults = queryApi.query(querySpec);

      List<Map> items = new ArrayList<>();
      Map<String, AttributeValue> lastKey = null;

      if (queryResults != null)
      {
         for (Item item : queryResults)
         {
            items.add(item.asMap());
         }
      }

      if (queryResults.getLastLowLevelResult() != null)
      {
         lastKey = queryResults.getLastLowLevelResult().getQueryResult().getLastEvaluatedKey();
      }

      return new DynamoResult(items, lastKey);

   }

   DynamoResult doScan(DynamoExpression dynamoExpression, com.amazonaws.services.dynamodbv2.document.Table dynamoTable, Chain chain, Response res, int pageSize, KeyAttribute[] nextKeys)
   {
      String expressionStr = dynamoExpression.buildExpression();

      if (chain.getRequest().isDebug())
      {
         res.debug("Query Type:         Scan");
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

   public void setNextKeyDelimeter(String nextKeyDelimeter)
   {
      this.nextKeyDelimeter = nextKeyDelimeter;
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
