/**
 * 
 */
package io.rocketpartners.cloud.action.dynamo;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.document.Index;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import io.rocketpartners.cloud.action.dynamo.DynamoDb.DynamoDbIndex;
import io.rocketpartners.cloud.model.ApiException;
import io.rocketpartners.cloud.model.Column;
import io.rocketpartners.cloud.model.Results;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.model.Table;
import io.rocketpartners.cloud.rql.Group;
import io.rocketpartners.cloud.rql.Order;
import io.rocketpartners.cloud.rql.Page;
import io.rocketpartners.cloud.rql.Query;
import io.rocketpartners.cloud.rql.Select;
import io.rocketpartners.cloud.rql.Term;
import io.rocketpartners.cloud.rql.Where;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.utils.Utils;

/**
 * @author tc-rocket, wells
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
 * 
 */
public class DynamoDbQuery extends Query<DynamoDbQuery, DynamoDb, Table, Select<Select<Select, DynamoDbQuery>, DynamoDbQuery>, Where<Where<Where, DynamoDbQuery>, DynamoDbQuery>, Group<Group<Group, DynamoDbQuery>, DynamoDbQuery>, Order<Order<Order, DynamoDbQuery>, DynamoDbQuery>, Page<Page<Page, DynamoDbQuery>, DynamoDbQuery>>
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
      FUNCTION_MAP.put("nn", "attribute_exists");
      FUNCTION_MAP.put("n", "attribute_not_exists");
   }

   com.amazonaws.services.dynamodbv2.document.Table dynamoTable = null;
   DynamoDbIndex                                    index;

   Term                                             partKey     = null;
   Term                                             sortKey     = null;

   public DynamoDbQuery(Table table, List<Term> terms)
   {
      super(table, terms);
      where().clearFunctions();
      where().withFunctions("eq", "ne", "gt", "ge", "lt", "le", "w", "sw", "nn", "n", "and", "or");
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

   protected Results<Map<String, Object>> doSelect() throws Exception
   {
      Index dynamoIndex = null;
      Results result = new Results(this);

      DynamoDbIndex index = getIndex();
      if (index != null && !index.isPrimaryIndex())
      {
         dynamoIndex = dynamoTable.getIndex(index.getName());
      }

      Object spec = getSelectSpec();
      if (spec instanceof GetItemSpec)
      {
         GetItemSpec gis = (GetItemSpec) spec;
         Item item = dynamoTable.getItem(gis);
         if (item != null)
         {
            result.withRow(item.asMap());
         }
      }
      else if (spec instanceof QuerySpec)
      {
         QuerySpec qs = ((QuerySpec) spec);
         ItemCollection<QueryOutcome> queryResult = dynamoIndex != null ? dynamoIndex.query(qs) : dynamoTable.query(qs);
         for (Item item : queryResult)
         {
            result.withRow(item.asMap());
         }

         result.withNext(after(queryResult.getLastLowLevelResult().getQueryResult().getLastEvaluatedKey()));
      }
      else if (spec instanceof ScanSpec)
      {
         ScanSpec ss = ((ScanSpec) spec);
         ItemCollection<ScanOutcome> scanResult = dynamoIndex != null ? dynamoIndex.scan(ss) : dynamoTable.scan(ss);
         for (Item item : scanResult)
         {
            result.withRow(item.asMap());
         }

         result.withNext(after(scanResult.getLastLowLevelResult().getScanResult().getLastEvaluatedKey()));
      }

      return result;
   }

   protected List<Term> after(java.util.Map<String, AttributeValue> attrs)
   {
      if (attrs == null)
         return Collections.EMPTY_LIST;

      Term after = Term.term(null, "after");
      for (String key : attrs.keySet())
      {
         after.withTerm(Term.term(after, key));
         after.withTerm(Term.term(after, getValue(attrs.get(key)).toString()));
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

      throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Unable to get value from AttributeValue: " + v);
   }

   /**
    * @return the best fit index to use for the query based on the params supplied
    */
   public DynamoDbIndex getIndex()
   {
      //TODO consider sort
      //      String sortBy = order.getProperty(0);
      //      if(sortBy != null)
      //      {
      //         sortBy = getColumnName(sortBy);
      //      }

      if (index == null)
      {
         DynamoDbIndex foundIndex = null;
         Term foundPartKey = null;
         Term foundSortKey = null;

         for (io.rocketpartners.cloud.model.Index idx : table().getIndexes())
         {
            DynamoDbIndex index = (DynamoDbIndex) idx;

            String partCol = index.getHashKey().getName();
            String sortCol = index.getSortKey() != null ? index.getSortKey().getName() : null;

            Term partKey = findTerm(partCol, "eq");

            if (partKey == null)
               continue;

            Term sortKey = findTerm(sortCol, "eq");
            if (sortKey == null)
               sortKey = findTerm(sortCol, "gt", "ne", "gt", "ge", "lt", "le", "w", "sw", "nn", "n");

            if (sortKey == null && foundSortKey != null)
               continue;

            if (foundIndex == null //
                  || (sortKey != null && foundSortKey == null) //
                  || (sortKey != null && sortKey.hasToken("eq") && !foundSortKey.hasToken("eq")))
            {
               foundIndex = index;
               foundPartKey = partKey;
               foundSortKey = sortKey;
            }
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
         getIndex();
      }
      return partKey;
   }

   public Term getSortKey()
   {
      if (index == null)
      {
         getIndex();
      }
      return sortKey;
   }

   protected boolean addTerm(String token, Term term)
   {
      index = null;
      return super.addTerm(token, term);
   }

   public Object getSelectSpec()
   {
      Term partKey = getPartKey();
      Term sortKey = getSortKey();

      Map nameMap = new HashMap();
      Map valueMap = new HashMap();

      StringBuffer keyExpr = new StringBuffer("");
      StringBuffer filterExpr = new StringBuffer("");

      if (index != null && index.isPrimaryIndex() && partKey != null && sortKey != null && sortKey.hasToken("eq") && sortKey.getTerm(1).isLeaf())//sortKey is a single eq expression not a logic expr
      {
         String partKeyCol = partKey.getToken(0);
         String type = table.getColumn(partKeyCol).getType();
         Object partKeyVal = db().cast(type, partKey.getToken(1));

         String sortKeyCol = sortKey.getToken(0);
         Object sortKeyVal = db.cast(table.getColumn(sortKeyCol).getType(), sortKey.getToken(1));

         Chain.debug("DynamoDbQuery: GetItemSpec partKeyCol=" + partKeyCol + " partKeyVal=" + partKeyVal + " sortKeyCol=" + sortKeyCol + " sortKeyVal=" + sortKeyVal);

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

      for (Term term : where().getTerms())
      {
         if (term == partKey || term == sortKey)
            continue;
         toString(filterExpr, term, nameMap, valueMap);
      }

      boolean doQuery = partKey != null && partKey.getTerm(1).isLeaf();

      StringBuffer debug = new StringBuffer("DynamoDbQuery: ").append(doQuery ? "QuerySpec" : "ScanSpec").append(index != null ? ":'" + index.getName() + "'" : "");

      int pageSize = page().getPageSize();
      debug.append(" maxPageSize=" + pageSize);

      boolean scanIndexForward = order().isAsc(0);
      debug.append(" scanIndexForward=" + scanIndexForward);

      debug.append(" nameMap=" + nameMap + " valueMap=" + valueMap);
      debug.append(" keyConditionExpression='" + keyExpr + "'");
      debug.append(" filterExpression='" + filterExpr + "'");

      String projectionExpression = null;
      List columns = select().getColumnNames();
      if (columns.size() > 0)
         projectionExpression = Utils.implode(",", columns);

      debug.append(" projectionExpression='" + (projectionExpression != null ? projectionExpression : "") + "'");

      Chain.debug(debug);

      if (doQuery)
      {
         QuerySpec querySpec = new QuerySpec();//
         querySpec.withMaxPageSize(pageSize);
         querySpec.withMaxResultSize(pageSize);
         querySpec.withScanIndexForward(order().isAsc(0));

         Term after = page().getAfter();
         if (after != null)
         {
            DynamoDbIndex index = (DynamoDbIndex) table().getPrimaryIndex();
            Column afterHashKeyCol = table().getColumn(after.getToken(0));
            Column afterSortKeyCol = after.size() > 2 ? table().getColumn(after.getToken(2)) : null;

            if (afterHashKeyCol == null || (after.size() > 2 && afterSortKeyCol == null))
               throw new ApiException(SC.SC_400_BAD_REQUEST, "Invalid column in 'after' key: " + after);

            Object hashValue = db.cast(afterHashKeyCol, after.getToken(0));
            Object sortValue = afterSortKeyCol != null ? db.cast(afterSortKeyCol, after.getToken(1)) : null;

            if (afterSortKeyCol != null)
            {
               querySpec.withExclusiveStartKey(afterHashKeyCol.getName(), hashValue, afterSortKeyCol.getName(), sortValue);
            }
            else
            {
               querySpec.withExclusiveStartKey(afterHashKeyCol.getName(), hashValue);
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
         scanSpec.withMaxPageSize(pageSize);
         scanSpec.withMaxResultSize(pageSize);

         Term after = page().getAfter();
         if (after != null)
         {
            DynamoDbIndex index = (DynamoDbIndex) table().getPrimaryIndex();
            Column afterHashKeyCol = table().getColumn(after.getToken(0));
            Column afterSortKeyCol = after.size() > 2 ? table().getColumn(after.getToken(2)) : null;

            if (afterHashKeyCol == null || (after.size() > 2 && afterSortKeyCol == null))
               throw new ApiException(SC.SC_400_BAD_REQUEST, "Invalid column in 'after' key: " + after);

            Object hashValue = db.cast(afterHashKeyCol, after.getToken(1));
            Object sortValue = afterSortKeyCol != null ? db.cast(afterSortKeyCol, after.getToken(3)) : null;

            if (afterSortKeyCol != null)
            {
               scanSpec.withExclusiveStartKey(afterHashKeyCol.getName(), hashValue, afterSortKeyCol.getName(), sortValue);
            }
            else
            {
               scanSpec.withExclusiveStartKey(afterHashKeyCol.getName(), hashValue);
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

      if (term.hasToken("and", "or"))
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
         String col = term.getToken(0);
         
         String nameKey = "#var" + (nameMap.size() + 1);
         nameMap.put(nameKey, col);
         
         String expr = toString(new StringBuffer(""), term.getTerm(1), nameMap, valueMap);

         if (buff.length() > 0)
            space(buff).append("and ");

         space(buff).append(func).append("(").append(nameKey).append(",").append(expr).append(")");
      }
      else if (term.isLeaf())
      {
         String colName = term.getParent().getToken(0);

         Object value = term.getToken();
         Column col = table.getColumn(colName);
         if(col != null)
            value = db.cast(col,  term.getToken());

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
