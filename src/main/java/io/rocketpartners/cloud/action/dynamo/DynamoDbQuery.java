/**
 * 
 */
package io.rocketpartners.cloud.action.dynamo;

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

import io.rocketpartners.cloud.action.dynamo.DynamoDb.DynamoDbIndex;
import io.rocketpartners.cloud.action.sql.SqlDb;
import io.rocketpartners.cloud.model.Collection;
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
 * @author tc-rocket
 * @see https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/index.html
 * @see https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Introduction.html
 * https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/QueryingJavaDocumentAPI.html
 * https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Query.html#FilteringResults
 * https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.ExpressionAttributeNames.html
 * 
 */
public class DynamoDbQuery extends Query<DynamoDbQuery, SqlDb, Table, Select<Select<Select, DynamoDbQuery>, DynamoDbQuery>, Where<Where<Where, DynamoDbQuery>, DynamoDbQuery>, Group<Group<Group, DynamoDbQuery>, DynamoDbQuery>, Order<Order<Order, DynamoDbQuery>, DynamoDbQuery>, Page<Page<Page, DynamoDbQuery>, DynamoDbQuery>>
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

   public DynamoDbQuery(Collection collection, Object terms)
   {
      super(collection, terms);
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

   protected TableResults doSelect() throws Exception
   {
      Index dynamoIndex = null;
      TableResults result = new TableResults();

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

         //result.setLastKey(queryResult.getLastLowLevelResult().getQueryResult().getLastEvaluatedKey());
      }
      else if (spec instanceof ScanSpec)
      {
         ScanSpec ss = ((ScanSpec) spec);
         ItemCollection<ScanOutcome> scanResult = dynamoIndex != null ? dynamoIndex.scan(ss) : dynamoTable.scan(ss);
         for (Item item : scanResult)
         {
            result.withRow(item.asMap());
         }

         //result.setLastKey(scanResult.getLastLowLevelResult().getScanResult().getLastEvaluatedKey());
      }

      return result;
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

            String partAttr = collection.getAttributeName(index.getHashKey().getName());
            String sortAttr = index.getSortKey() != null ? collection.getAttributeName(index.getSortKey().getName()) : null;

            Term partKey = findTerm(partAttr, "eq");

            if (partKey == null)
               continue;

            Term sortKey = findTerm(sortAttr, "eq");
            if (sortKey == null)
               sortKey = findTerm(sortAttr, "gt", "ne", "gt", "ge", "lt", "le", "w", "sw", "nn", "n");

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
         String partKeyCol = getColumnName(partKey.getToken(0));
         Object partKeyVal = cast(partKeyCol, partKey.getToken(1));

         String sortKeyCol = getColumnName(sortKey.getToken(0));
         Object sortKeyVal = cast(sortKeyCol, sortKey.getToken(1));

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
            toString(buff, term.getTerm(i), nameMap, valueMap);
            if (i < term.getNumTerms() - 1)
            {
               space(buff).append(toString(buff, term.getTerm(i), nameMap, valueMap)).append(" ");
            }
         }
         buff.append(")");
      }
      else if (op != null)
      {
         String col = getColumnName(term.getToken(0));

         String nameKey = "#var" + (nameMap.size() + 1);
         nameMap.put(nameKey, col);

         String expr = toString(new StringBuffer(""), term.getTerm(1), nameMap, valueMap);

         if (buff.length() > 0)
            space(buff).append("and ");

         buff.append(nameKey).append(" ").append(op).append(" ").append(expr);
      }
      else if (func != null)
      {
         String col = getColumnName(term.getToken(0));
         String expr = toString(new StringBuffer(""), term.getTerm(1), nameMap, valueMap);

         if (buff.length() > 0)
            space(buff).append("and ");

         space(buff).append(func).append("(").append(col).append(",").append(expr).append(")");
      }
      else if (term.isLeaf())
      {
         String attr = term.getParent().getToken(0);
         String col = getColumnName(attr);
         Object value = cast(col, term.getToken());

         String key = ":val" + (valueMap.size() + 1);
         valueMap.put(key, value);

         space(buff).append(key);
      }

      return buff.toString();
   }

   StringBuffer space(StringBuffer buff)
   {
      if (buff.length() > 0 && buff.charAt(buff.length() - 1) != ' ')
         buff.append(' ');

      return buff;
   }

}
