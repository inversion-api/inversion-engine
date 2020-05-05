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
package io.inversion.dynamodb;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import io.inversion.ApiException;
import io.inversion.Chain;
import io.inversion.Collection;
import io.inversion.Index;
import io.inversion.Property;
import io.inversion.Results;
import io.inversion.rql.Group;
import io.inversion.rql.Order;
import io.inversion.rql.Page;
import io.inversion.rql.Query;
import io.inversion.rql.Select;
import io.inversion.rql.Term;
import io.inversion.rql.Where;
import io.inversion.utils.Utils;
import io.inversion.utils.Rows.Row;

/**
 * 
 * 
 * @see https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.OperatorsAndFunctions.html
 * 
 * @see https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_Query.html
 * 
 * @see https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/index.html
 * @see https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Introduction.html
 * https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/QueryingJavaDocumentAPI.html
 * https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Query.html#FilteringResults
 * https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.ExpressionAttributeNames.html
 * 
 * https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/LegacyConditionalParameters.KeyConditions.html
 * 
 * https://stackoverflow.com/questions/34349135/how-do-you-query-for-a-non-existent-null-attribute-in-dynamodb
 *  https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/LegacyConditionalParameters.QueryFilter.html
 *  TODO: are all of these covered? EQ | NE | LE | LT | GE | GT | NOT_NULL | NULL | CONTAINS | NOT_CONTAINS | BEGINS_WITH | IN | BETWEEN
 * 
 */
public class DynamoDbQuery extends Query<DynamoDbQuery, DynamoDb, Select<Select<Select, DynamoDbQuery>, DynamoDbQuery>, Where<Where<Where, DynamoDbQuery>, DynamoDbQuery>, Group<Group<Group, DynamoDbQuery>, DynamoDbQuery>, Order<Order<Order, DynamoDbQuery>, DynamoDbQuery>, Page<Page<Page, DynamoDbQuery>, DynamoDbQuery>>
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
      FUNCTION_MAP.put("attribute_not_exists", "attribute_not_exists");
      FUNCTION_MAP.put("attribute_exists", "attribute_exists");

      //attribute_not_exists
      //attribute_exists

      //https://stackoverflow.com/questions/34349135/how-do-you-query-for-a-non-existent-null-attribute-in-dynamodb
      //FUNCTION_MAP.put("nn", "attribute_exists");//needs to be
      //FUNCTION_MAP.put("n", "attribute_not_exists");
   }

   com.amazonaws.services.dynamodbv2.document.Table dynamoTable = null;
   Index                                            index;

   Term                                             partKey     = null;
   Term                                             sortKey     = null;

   public DynamoDbQuery()
   {
   }

   public DynamoDbQuery(Collection table, List<Term> terms)
   {
      super(table, terms);
   }

   @Override
   protected Where createWhere()
   {
      Where where = new Where(this);
      where.withFunctions("_key", "eq", "ne", "gt", "ge", "lt", "le", "w", "wo", "sw", /* "ew" is not supported */ "nn", "n", "emp", "nemp", "in", "out", "and", "or", "not", "attribute_not_exists", "attribute_exists");
      return where;
   }

   protected boolean addTerm(String token, Term term)
   {
      index = null;

      if (term.hasToken("n", "nn", "emp", "nemp"))
      {
         if (term.size() > 1)
            ApiException.throw400BadRequest("The n() and nn() functions only take one column name arg.");

         if (term.hasToken("n", "emp"))
         {
            Term eqNull = Term.term(term.getParent(), "eq", term.getTerm(0), "null");
            Term attrNotExists = Term.term(null, "attribute_not_exists", term.getTerm(0));

            term = Term.term(term.getParent(), "or", attrNotExists, eqNull);
         }
         else if (term.hasToken("nn", "nemp"))
         {
            Term neNull = Term.term(term.getParent(), "ne", term.getTerm(0), "null");
            Term attrExists = Term.term(null, "attribute_exists", term.getTerm(0));
            term = Term.term(term.getParent(), "and", attrExists, neNull);
         }
      }

      if (term.hasToken("like"))
      {
         String val = term.getToken(1);
         int firstWc = val.indexOf("*");
         if (firstWc > -1)
         {
            int wcCount = val.length() - val.replace("*", "").length();
            int lastWc = val.lastIndexOf("*");
            if (wcCount > 2// 
                  || (wcCount == 1 && firstWc != val.length() - 1)//
                  || (wcCount == 2 && !(firstWc == 0 && lastWc == val.length() - 1)))
               ApiException.throw400BadRequest("DynamoDb only supports a 'value*' or '*value*' wildcard formats which are equivalant to the 'sw' and 'w' operators.");

            boolean sw = firstWc == val.length() - 1;

            val = val.replace("*", "");
            term.getTerm(1).withToken(val);

            if (sw)
               term.withToken("sw");
            else
               term.withToken("w");
         }
      }

      if (term.hasToken("sw"))//sw (startswith) includes a implicit trailing wild card
      {
         String val = term.getTerm(1).getToken();
         while (val != null && val.endsWith("*"))
         {
            val = val.substring(0, val.length() - 1);
         }
         term.getTerm(1).withToken(val);
      }

      if(term.hasToken("wo"))
      {
         term.withToken("w");
         term = Term.term(null,  "not", term);//getParent().replaceTerm(, newTerm)
         
         
      }
      
      return super.addTerm(token, term);
   }

   public com.amazonaws.services.dynamodbv2.document.Table getDynamoTable()
   {
      return dynamoTable;
   }

   public DynamoDbQuery withDynamoTable(com.amazonaws.services.dynamodbv2.document.Table dynamoTable)
   {
      this.dynamoTable = dynamoTable;
      return this;
   }

   public Results doSelect() throws ApiException
   {
      Results.LAST_QUERY = null;
      try
      {
         return doSelect0();
      }
      catch (Exception ex)
      {
         if (Results.LAST_QUERY != null)
         {
            System.out.println("Error after query: " + Results.LAST_QUERY);
            System.out.println(ex.getMessage());
         }

         Utils.rethrow(ex);
      }
      return null;
   }

   protected Results doSelect0() throws Exception
   {
      com.amazonaws.services.dynamodbv2.document.Index dynamoIndex = null;
      Results result = new Results(this);

      Object spec = getSelectSpec();

      Index index = calcIndex();
      if (!isDryRun() //
            && index != null //
            && index != collection.getIndex(DynamoDb.PRIMARY_INDEX_NAME))
      {
         dynamoIndex = dynamoTable.getIndex(index.getName());
      }

      if (spec instanceof GetItemSpec)
      {
         GetItemSpec gis = (GetItemSpec) spec;

         StringBuffer debug = new StringBuffer("DynamoDb: ").append("GetItemSpec").append(index != null ? ":'" + index.getName() + "'" : "");
         debug.append(" key: ").append(gis.getKeyComponents());

         result.withTestQuery(debug.toString());
         Chain.debug(debug);

         if (!isDryRun())
         {
            Item item = dynamoTable.getItem(gis);
            if (item != null)
            {
               result.withRow(item.asMap());
            }
         }
      }
      else if (spec instanceof QuerySpec)
      {
         QuerySpec qs = ((QuerySpec) spec);

         StringBuffer debug = new StringBuffer("DynamoDb: ").append("QuerySpec").append(index != null ? ":'" + index.getName() + "'" : "");

         if (qs.getMaxResultSize() != 100)
            debug.append(" maxResultSize=").append(qs.getMaxResultSize());

         if (qs.getNameMap() != null)
            debug.append(" nameMap=").append(qs.getNameMap());

         if (qs.getValueMap() != null)
            debug.append(" valueMap=").append(qs.getValueMap());

         if (qs.getFilterExpression() != null)
            debug.append(" filterExpression='").append(qs.getFilterExpression()).append("'");

         if (qs.getProjectionExpression() != null)
            debug.append(" projectionExpression='").append(qs.getProjectionExpression()).append("'");

         if (qs.getExclusiveStartKey() != null)
            debug.append(" exclusiveStartKey='" + qs.getExclusiveStartKey());

         if (qs.getKeyConditionExpression() != null)
            debug.append(" keyConditionExpression='").append(qs.getKeyConditionExpression()).append("'");

         if (!getOrder().isAsc(0))
            debug.append(" scanIndexForward=false");

         result.withTestQuery(debug.toString());
         Chain.debug(debug);

         if (!isDryRun())
         {
            ItemCollection<QueryOutcome> queryResult = dynamoIndex != null ? dynamoIndex.query(qs) : dynamoTable.query(qs);
            for (Item item : queryResult)
            {
               result.withRow(item.asMap());
            }

            result.withNext(after(index, queryResult.getLastLowLevelResult().getQueryResult().getLastEvaluatedKey()));
         }
      }
      else if (spec instanceof ScanSpec)
      {
         ScanSpec ss = ((ScanSpec) spec);

         StringBuffer debug = new StringBuffer("DynamoDb: ").append("ScanSpec").append(index != null ? ":'" + index.getName() + "'" : "");

         if (ss.getMaxResultSize() != 100)
            debug.append(" maxResultSize=").append(ss.getMaxResultSize());

         if (ss.getNameMap() != null)
            debug.append(" nameMap=").append(ss.getNameMap());

         if (ss.getValueMap() != null)
            debug.append(" valueMap=").append(ss.getValueMap());

         if (ss.getFilterExpression() != null)
            debug.append(" filterExpression='").append(ss.getFilterExpression()).append("'");

         if (ss.getProjectionExpression() != null)
            debug.append(" projectionExpression='").append(ss.getProjectionExpression()).append("'");

         if (ss.getExclusiveStartKey() != null)
            debug.append(" exclusiveStartKey='" + ss.getExclusiveStartKey());

         result.withTestQuery(debug.toString());
         Chain.debug(debug);

         if (!isDryRun())
         {
            ItemCollection<ScanOutcome> scanResult = dynamoIndex != null ? dynamoIndex.scan(ss) : dynamoTable.scan(ss);
            for (Item item : scanResult)
            {
               result.withRow(item.asMap());
            }
            result.withNext(after(index, scanResult.getLastLowLevelResult().getScanResult().getLastEvaluatedKey()));
         }
      }

      return result;
   }

   protected List<Term> after(Index index, java.util.Map<String, AttributeValue> attrs)
   {
      if (attrs == null)
         return Collections.EMPTY_LIST;

      Term after = Term.term(null, "after");

      if (index != null)
      {
         after.withTerm(Term.term(after, index.getColumnName(0)));
         after.withTerm(Term.term(after, getValue(attrs.get(index.getColumnName(0))).toString()));

         if (index.getColumnName(1) != null)
         {
            after.withTerm(Term.term(after, index.getColumnName(1)));
            after.withTerm(Term.term(after, getValue(attrs.get(index.getColumnName(1))).toString()));
         }
      }
      else
      {
         for (String key : attrs.keySet())
         {
            after.withTerm(Term.term(after, key));
            after.withTerm(Term.term(after, getValue(attrs.get(key)).toString()));
         }
      }
      return Arrays.asList(after);
   }

   protected Object getValue(AttributeValue v)
   {
      if (v.getS() != null)
         return v.getS();
      if (v.getN() != null)
         return v.getN();
      if (v.getB() != null)
         return v.getB();
      if (v.getSS() != null)
         return v.getSS();
      if (v.getNS() != null)
         return v.getNS();
      if (v.getBS() != null)
         return v.getBS();
      if (v.getM() != null)
         return v.getM();
      if (v.getL() != null)
         return v.getL();
      if (v.getNULL() != null)
         return v.getNULL();
      if (v.getBOOL() != null)
         return v.getBOOL();

      ApiException.throw500InternalServerError("Unable to get value from AttributeValue: {}", v);
      return null;
   }

   /**
    * @return the best fit index to use for the query based on the params supplied
    */
   protected Index calcIndex()
   {
      //if the users requested a sort, you need to find an index with that sort: String sortBy = order.getProperty(0);
      String sortBy = order.getProperty(0);

      Term after = getPage().getAfter();
      if (after != null)
      {
         String afterHashKeyCol = getCollection().getProperty(after.getToken(0)).getColumnName();
         String afterSortKeyCol = after.size() > 2 ? getCollection().getProperty(after.getToken(2)).getColumnName() : null;

         for (Index idx : collection.getIndexes())
         {
            if (Utils.equal(idx.getColumnName(0), afterHashKeyCol)//
                  && Utils.equal(idx.getColumnName(1), afterSortKeyCol))
            {
               index = idx;

               partKey = findTerm(afterHashKeyCol, "eq");

               if (partKey == null)
                  continue;

               if (afterSortKeyCol != null)
               {
                  sortKey = findTerm(afterSortKeyCol, "eq");
                  if (sortKey == null)
                     sortKey = findTerm(afterSortKeyCol, "gt", "ne", "gt", "ge", "lt", "le", "w", "sw", "nn", "n", "emp", "nemp", "in", "out");
               }

               break;
            }
         }

         if (sortBy != null && (afterSortKeyCol == null || !sortBy.equalsIgnoreCase(afterSortKeyCol)))
         {
            //TODO make test
            ApiException.throw400BadRequest("The requested sort key does not match the supplied 'after' continuation token.");
         }
      }

      if (index == null)
      {
         Index foundIndex = null;
         Term foundPartKey = null;
         Term foundSortKey = null;

         for (io.inversion.Index idx : getCollection().getIndexes())
         {
            Index index = (Index) idx;

            String partCol = index.getColumnName(0);//getHashKey().getColumnName();
            String sortCol = index.getColumnName(1);//index.getSortKey() != null ? index.getSortKey().getColumnName() : null;

            if (sortBy != null && !sortBy.equalsIgnoreCase(sortCol))
               continue; //incompatible index. if a sort was requested, can't choose an index that has a different sort

            Term partKey = findTerm(partCol, "eq");

            if (partKey == null && sortBy == null)
               continue;

            Term sortKey = findTerm(sortCol, "eq");

            if (sortKey == null)
               sortKey = findTerm(sortCol, "gt", "ne", "gt", "ge", "lt", "le", "w", "sw", "nn", "n", "emp", "nemp", "in", "out");

            boolean use = false;
            if (foundPartKey == null && partKey != null)
               use = true;

            else if (sortKey == null && foundSortKey != null)
               use = false; //if you already have an index with a sort key match, don't replace it

            else if (foundIndex == null //
                  || (sortKey != null && foundSortKey == null) //
                  || (sortKey != null && sortKey.hasToken("eq") && !foundSortKey.hasToken("eq"))) //the new sort key has an equality condition
               use = true;

            if (use)
            {
               foundIndex = index;
               foundPartKey = partKey;
               foundSortKey = sortKey;
            }
         }

         if (sortBy != null && foundIndex == null)
         {
            //TODO: create test case to trigger this exception
            ApiException.throw400BadRequest("Unable to find valid index to query.  The requested sort field '{}' must be the sort key of the primary index, the sort key of a global secondary index, or a local secondary secondary index.", sortBy);
         }

         if (foundPartKey == null && sortBy != null && !order.isAsc(0))
         {
            //an inverse/descending sort can only be run on a QuerySpec which requires a partition key.
            ApiException.throw400BadRequest("Unable to find valid index to query.  A descending sort on '{}' is only possible when a partition key value is supplied.", sortBy);
         }

         this.index = foundIndex;
         this.partKey = foundPartKey;
         this.sortKey = foundSortKey;
      }
      return index;
   }

   /**
    * Finds the primary or a secondary index to use based on 
    * what parameters were passed in. 
    */
   public Term getPartKey()
   {
      if (index == null)
      {
         calcIndex();
      }
      return partKey;
   }

   public Term getSortKey()
   {
      if (index == null)
      {
         calcIndex();
      }
      return sortKey;
   }

   public Object getSelectSpec()
   {
      Map nameMap = new HashMap();
      Map valueMap = new HashMap();

      StringBuffer keyExpr = new StringBuffer("");
      StringBuffer filterExpr = new StringBuffer("");

      Index index = calcIndex();

      if (index != null //
            && Utils.equal(DynamoDb.PRIMARY_INDEX_NAME, index.getName()) //
            && partKey != null //
            && sortKey != null //
            && sortKey.hasToken("eq") //
            && sortKey.getTerm(1).isLeaf())//sortKey is a single eq expression not a logic expr
      {
         String partKeyCol = partKey.getToken(0);
         String type = collection.getProperty(partKeyCol).getType();
         Object partKeyVal = getDb().cast(type, partKey.getToken(1));

         String sortKeyCol = sortKey.getToken(0);
         Object sortKeyVal = getDb().cast(collection.getProperty(sortKeyCol).getType(), sortKey.getToken(1));

         return new GetItemSpec().withPrimaryKey(partKeyCol, partKeyVal, sortKeyCol, sortKeyVal);
      }

      if (partKey != null)
      {
         toString(keyExpr, partKey, nameMap, valueMap);
      }

      if (sortKey != null)
      {
         toString(keyExpr, sortKey, nameMap, valueMap);
      }

      for (Term term : getWhere().getTerms())
      {
         if (term == partKey || term == sortKey)
            continue;
         toString(filterExpr, term, nameMap, valueMap);
      }

      boolean doQuery = partKey != null && partKey.getTerm(1).isLeaf();

      int pageSize = getPage().getPageSize();

      String projectionExpression = null;
      List columns = getSelect().getColumnNames();
      if (columns.size() > 0)
         projectionExpression = Utils.implode(",", columns);

      if (doQuery)
      {
         QuerySpec querySpec = new QuerySpec();//
         //querySpec.withMaxPageSize(pageSize);
         querySpec.withMaxResultSize(pageSize);
         querySpec.withScanIndexForward(getOrder().isAsc(0));

         //-- https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/SQLtoNoSQL.ReadData.Query.html
         //-- You can use Query with any table that has a composite primary key (partition key and sort key). 
         //-- You must specify an equality condition for the partition key, and you can optionally provide another condition for the sort key.

         Term after = getPage().getAfter();
         if (after != null)
         {
            Property afterHashKeyCol = getCollection().getProperty(after.getToken(0));
            Property afterSortKeyCol = after.size() > 2 ? getCollection().getProperty(after.getToken(2)) : null;

            if (afterHashKeyCol == null || (after.size() > 2 && afterSortKeyCol == null))
               ApiException.throw400BadRequest("Invalid column in 'after' key: {}", after);

            Object hashValue = db.cast(afterHashKeyCol, after.getToken(1));
            Object sortValue = afterSortKeyCol != null ? db.cast(afterSortKeyCol, after.getToken(3)) : null;

            if (afterSortKeyCol != null)
            {
               querySpec.withExclusiveStartKey(afterHashKeyCol.getColumnName(), hashValue, afterSortKeyCol.getColumnName(), sortValue);
            }
            else
            {
               querySpec.withExclusiveStartKey(afterHashKeyCol.getColumnName(), hashValue);
            }
         }

         if (!Utils.empty(projectionExpression))
         {
            querySpec.withProjectionExpression(projectionExpression);
         }

         if (keyExpr.length() > 0)
         {
            querySpec.withKeyConditionExpression(keyExpr.toString());
         }

         if (filterExpr.length() > 0)
         {
            querySpec.withFilterExpression(filterExpr.toString());
         }

         if (nameMap.size() > 0)
         {
            querySpec.withNameMap(nameMap);
         }

         if (valueMap.size() > 0)
         {
            querySpec.withValueMap(valueMap);
         }

         return querySpec;
      }
      else
      {
         ScanSpec scanSpec = new ScanSpec();
         //scanSpec.withMaxPageSize(pageSize);
         scanSpec.withMaxResultSize(pageSize);

         Term after = getPage().getAfter();
         if (after != null)
         {
            Property afterHashKeyCol = getCollection().getProperty(after.getToken(0));
            Property afterSortKeyCol = after.size() > 2 ? getCollection().getProperty(after.getToken(2)) : null;

            if (afterHashKeyCol == null || (after.size() > 2 && afterSortKeyCol == null))
               ApiException.throw400BadRequest("Invalid column in 'after' key: {}");

            Object hashValue = db.cast(afterHashKeyCol, after.getToken(1));
            Object sortValue = afterSortKeyCol != null ? db.cast(afterSortKeyCol, after.getToken(3)) : null;

            if (afterSortKeyCol != null)
            {
               scanSpec.withExclusiveStartKey(afterHashKeyCol.getColumnName(), hashValue, afterSortKeyCol.getColumnName(), sortValue);
            }
            else
            {
               scanSpec.withExclusiveStartKey(afterHashKeyCol.getColumnName(), hashValue);
            }
         }

         if (!Utils.empty(projectionExpression))
         {
            scanSpec.withProjectionExpression(projectionExpression);
         }

         if (filterExpr.length() > 0)
         {
            scanSpec.withFilterExpression(filterExpr.toString());
         }
         if (nameMap.size() > 0)
         {
            scanSpec.withNameMap(nameMap);
         }

         if (valueMap.size() > 0)
         {
            scanSpec.withValueMap(valueMap);
         }

         return scanSpec;
      }
   }

   String toString(StringBuffer buff, Term term, Map nameMap, Map valueMap)
   {
      space(buff);

      String lc = term.getToken().toLowerCase();
      String op = OPERATOR_MAP.get(lc);
      String func = FUNCTION_MAP.get(lc);

      if (term.hasToken("not"))
      {
         if (buff.length() > 0)
            space(buff).append("and ");

         buff.append("(NOT ").append(toString(new StringBuffer(""), term.getTerm(0), nameMap, valueMap)).append(")");
      }
      else if (term.hasToken("and", "or"))
      {
         buff.append("(");
         for (int i = 0; i < term.getNumTerms(); i++)
         {
            buff.append(toString(new StringBuffer(""), term.getTerm(i), nameMap, valueMap));
            if (i < term.getNumTerms() - 1)
               space(buff).append(term.getToken()).append(" ");
         }
         buff.append(")");
      }
      else if (term.hasToken("in", "out"))
      {
         String col = term.getToken(0);
         String nameKey = "#var" + (nameMap.size() + 1);
         nameMap.put(nameKey, col);

         if (buff.length() > 0)
            space(buff).append("and ");

         buff.append("(");
         buff.append(term.hasToken("out") ? "NOT " : "");
         buff.append(nameKey).append(" IN (");
         for (int i = 1; i < term.size(); i++)
         {
            if (i > 1)
               buff.append(", ");

            buff.append(toString(new StringBuffer(""), term.getTerm(i), nameMap, valueMap));

         }
         buff.append("))");
      }
      else if (op != null)
      {
         String col = term.getToken(0);

         String nameKey = "#var" + (nameMap.size() + 1);
         nameMap.put(nameKey, col);

         String expr = toString(new StringBuffer(""), term.getTerm(1), nameMap, valueMap);

         if (buff.length() > 0)
            space(buff).append("and ");

         buff.append("(").append(nameKey).append(" ").append(op).append(" ").append(expr).append(")");
      }
      else if (func != null)
      {
         if (buff.length() > 0)
            space(buff).append("and ");

         String col = term.getToken(0);

         String nameKey = "#var" + (nameMap.size() + 1);
         nameMap.put(nameKey, col);

         if (term.size() > 1)
         {
            String expr = toString(new StringBuffer(""), term.getTerm(1), nameMap, valueMap);
            space(buff).append(func).append("(").append(nameKey).append(",").append(expr).append(")");
         }
         else
         {
            space(buff).append(func).append("(").append(nameKey).append(")");
         }
      }
      else if (term.isLeaf())
      {
         String colName = term.getParent().getToken(0);

         Object value = term.getToken();
         Property col = collection.getProperty(colName);
         value = db.cast(col, term.getToken());

         if ("null".equalsIgnoreCase(value + ""))
            value = null;

         String key = ":val" + (valueMap.size() + 1);
         valueMap.put(key, value);

         space(buff).append(key);
      }

      //System.out.println("TOSTRING: " + term + " -> '" + buff + "'" + " - " + nameMap + " - " + valueMap);
      return buff.toString();
   }

   StringBuffer space(StringBuffer buff)
   {
      if (buff.length() > 0 && buff.charAt(buff.length() - 1) != ' ')
         buff.append(' ');

      return buff;
   }

}
