/**
 * 
 */
package io.rocketpartners.cloud.api.handler.dynamo;

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

import io.rocketpartners.J;
import io.rocketpartners.cloud.api.Table;
import io.rocketpartners.cloud.api.handler.dynamo.DynamoDb.DynamoDbColumn;
import io.rocketpartners.cloud.api.handler.dynamo.DynamoDb.DynamoDbIndex;
import io.rocketpartners.cloud.api.handler.dynamo.DynamoDb.DynamoDbTable;
import io.rocketpartners.db.Rows;
import io.rocketpartners.fluent.Term;
import io.rocketpartners.rql.Group;
import io.rocketpartners.rql.Order;
import io.rocketpartners.rql.Page;
import io.rocketpartners.rql.Query;
import io.rocketpartners.rql.Select;
import io.rocketpartners.rql.Where;
import io.rocketpartners.rql.sql.SqlQuery;

/**
 * @author tc-rocket
 * @see https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/index.html
 * @see https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Introduction.html
 * https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/QueryingJavaDocumentAPI.html
 * https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Query.html#FilteringResults
 * https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.ExpressionAttributeNames.html
 * 
 */
public class DynamoDbQuery extends Query<Table<DynamoDb, DynamoDbColumn, DynamoDbIndex>, DynamoDbQuery, SqlQuery, Select<Select<Select, DynamoDbQuery>, SqlQuery>, Where<Where<Where, DynamoDbQuery>, DynamoDbQuery>, Group<Group<Group, DynamoDbQuery>, DynamoDbQuery>, Order<Order<Order, DynamoDbQuery>, DynamoDbQuery>, Page<Page<Page, DynamoDbQuery>, DynamoDbQuery>>
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

   DynamoDbIndex index;

   Term          partKey = null;
   Term          sortKey = null;

   public DynamoDbQuery(DynamoDbTable table)
   {
      super(table);
      where().clearTokens();
      where().withTokens("eq", "ne", "gt", "ge", "lt", "le", "w", "sw", "nn", "n", "and", "or");
   }

   DynamoResult doSelect(com.amazonaws.services.dynamodbv2.document.Table dynamoTable)
   {
      Index dynamoIndex = null;
      DynamoResult rows = new DynamoResult();

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
            rows.addRow(item.asMap());
         }
      }
      else if (spec instanceof QuerySpec)
      {
         QuerySpec qs = ((QuerySpec) spec);
         ItemCollection<QueryOutcome> queryResult = dynamoIndex != null ? dynamoIndex.query(qs) : dynamoTable.query(qs);
         for (Item item : queryResult)
         {
            rows.addRow(item.asMap());
         }

         rows.setLastKey(queryResult.getLastLowLevelResult().getQueryResult().getLastEvaluatedKey());
      }
      else if (spec instanceof ScanSpec)
      {
         ScanSpec ss = ((ScanSpec) spec);
         ItemCollection<ScanOutcome> scanResult = dynamoIndex != null ? dynamoIndex.scan(ss) : dynamoTable.scan(ss);
         for (Item item : scanResult)
         {
            rows.addRow(item.asMap());
         }

         rows.setLastKey(scanResult.getLastLowLevelResult().getScanResult().getLastEvaluatedKey());
      }

      return rows;
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
         for (DynamoDbIndex index : (List<DynamoDbIndex>) table().getIndexes())
         {
            if (index.isLocalIndex())
               continue;

            Term partKey = findTerm(index.getPartitionKey(), "eq");
            if (partKey != null)
            {
               this.index = index;
               this.partKey = partKey;

               Term sortKey = findTerm(index.getSortKey(), "eq");
               if (sortKey != null)
               {
                  //this index has both values passed in with 'eq' so we are done
                  this.sortKey = sortKey;
                  return index;
               }
               else
               {
                  sortKey = findTerm(index.getSortKey(), "gt", "ne", "gt", "ge", "lt", "le", "w", "sw", "nn", "n");
                  if (sortKey != null)
                  {
                     this.sortKey = sortKey;
                     //there could still be a double equality match so keep looking
                  }
               }

            }
         }
      }
      return index;
   }

   Term findTerm(String column, String... tokens)
   {
      for (Term term : getTerms())
      {
         if ((tokens == null || term.hasToken(tokens)) && term.isLeaf(0))
         {
            String attr = term.getToken(0);

            String colName = getColumnName(attr);
            if (column.equalsIgnoreCase(colName))
               return term;
         }
      }
      return null;
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
      Term sortKey = getPartKey();

      Map valueMap = new HashMap();

      StringBuffer keyExpr = new StringBuffer("");
      StringBuffer filterExpr = new StringBuffer("");

      if (partKey != null && sortKey != null && sortKey.getTerm(1).isLeaf())//sortKey is a single eq expression not a logic expr
      {
         String partKeyCol = getColumnName(partKey.getToken(0));
         Object partKeyVal = cast(partKeyCol, partKey.getToken(1));

         String sortKeyCol = getColumnName(sortKey.getToken(0));
         Object sortKeyVal = cast(sortKeyCol, sortKey.getToken(1));

         return new GetItemSpec().withPrimaryKey(partKeyCol, partKeyVal, sortKeyCol, sortKeyVal);
      }

      if (sortKey != null)
      {
         toString(keyExpr, sortKey, valueMap);
      }

      for (Term term : where().getTerms())
      {
         if (term == partKey || term == sortKey)
            continue;

         if (filterExpr.length() > 0)
            filterExpr.append(" and ");

         toString(filterExpr, term, valueMap);
      }

      int pageSize = page().getPageSize();

      if (partKey != null && partKey.getTerm(1).isLeaf())
      {
         QuerySpec querySpec = new QuerySpec();//
         querySpec.withMaxPageSize(pageSize);
         querySpec.withMaxResultSize(pageSize); //todo are these aliases or actually have different function?

         querySpec.withScanIndexForward(order().isAsc(0));

         List columns = select().getColumnNames();
         if (columns.size() > 0)
            querySpec.withProjectionExpression(J.implode(",", columns));

         if (filterExpr.length() > 0)
         {
            querySpec.withFilterExpression(filterExpr.toString());
         }

         if (valueMap.size() > 0)
            querySpec.withValueMap(valueMap);

         if (keyExpr.length() > 0)
         {
            querySpec.withKeyConditionExpression(keyExpr.toString());
         }

         return querySpec;
      }
      else
      {
         ScanSpec scanSpec = new ScanSpec();
         scanSpec.withMaxPageSize(pageSize);
         scanSpec.withMaxResultSize(pageSize);

         List columns = select().getColumnNames();
         if (columns.size() > 0)
            scanSpec.withProjectionExpression(J.implode(",", columns));

         if (filterExpr.length() > 0)
         {
            scanSpec.withFilterExpression(filterExpr.toString());
         }
         if (valueMap.size() > 0)
            scanSpec.withValueMap(valueMap);

         return scanSpec;
      }
   }

   String toString(StringBuffer buff, Term term, Map valueMap)
   {
      String op = OPERATOR_MAP.get(term.getToken().toLowerCase());
      if (term.hasToken("and", "or"))
      {
         buff.append("(");
         for (int i = 0; i < term.getNumTerms(); i++)
         {
            toString(buff, term.getTerm(i), valueMap);
            if (i < term.getNumTerms() - 1)
               buff.append(" ").append(toString(buff, term.getTerm(i), valueMap)).append(" ");
         }
         buff.append(")");
      }
      else if (op != null)
      {
         String col = getColumnName(term.getToken(0));
         String expr = toString(buff, term.getTerm(1), valueMap);

         if (buff.length() > 0)
            buff.append(" and");

         buff.append(col).append(" ").append(" ").append(expr);
      }
      else if (term.isLeaf())
      {
         String attr = term.getParent().getToken(0);
         String col = getColumnName(attr);

         Object value = cast(col, term.getToken());

         String key = ":" + attr;
         valueMap.put(key, value);

         buff.append(" ").append(key);
      }

      return buff.toString();
   }

   public Object cast(String colName, String value)
   {
      io.rocketpartners.db.Column col = table().getColumn(colName);

      if (col != null)
      {
         switch (col.getType())
         {
            case "N":
               return Long.parseLong(value);

            case "BOOL":
               return Boolean.parseBoolean(value);

         }
      }

      return value;
   }

   static class DynamoResult extends Rows
   {
      Map<String, AttributeValue> lastKey;

      public Map<String, AttributeValue> getLastKey()
      {
         return lastKey;
      }

      public void setLastKey(Map<String, AttributeValue> lastKey)
      {
         this.lastKey = lastKey;
      }

   }

}
