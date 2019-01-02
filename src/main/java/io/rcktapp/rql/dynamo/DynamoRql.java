/**
 * 
 */
package io.rcktapp.rql.dynamo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.rcktapp.api.Table;
import io.rcktapp.api.handler.dynamo.DynamoDb;
import io.rcktapp.api.handler.dynamo.DynamoIndex;
import io.rcktapp.rql.Order;
import io.rcktapp.rql.Parser;
import io.rcktapp.rql.Predicate;
import io.rcktapp.rql.Rql;
import io.rcktapp.rql.Stmt;

/**
 * @author tc-rocket
 *
 */
public class DynamoRql extends Rql
{

   static
   {
      Rql.addRql(new DynamoRql());
   }

   private DynamoRql()
   {
      super("dynamo");
      setDoQuote(false);
   }

   public DynamoExpression buildDynamoExpression(Map<String, String> requestParams, Table table) throws Exception
   {

      DynamoExpression dynamoExpression = new DynamoExpression(table);

      Stmt stmt = buildStmt(new Stmt(this, null, null, table), null, requestParams, null);

      DynamoIndex dynamoIdx = null;

      List<Predicate> predicates = stmt.where;
      List<Order> orderList = stmt.order;

      boolean hasPartitionKey = false;

      // find the best index to use based on the given request params
      List<DynamoIndex> potentialIndexList = new ArrayList<DynamoIndex>();
      for (DynamoIndex idx : (List<DynamoIndex>) (List<?>) table.getIndexes())
      {
         if (predicatesContainField(predicates, idx.getPartitionKey()))
         {
            potentialIndexList.add(idx);
            dynamoIdx = idx;
            hasPartitionKey = true;
         }
         // always fallback to the index to the primary index
         if (dynamoIdx == null && idx.getType().equals(DynamoDb.PRIMARY_TYPE))
            dynamoIdx = idx;
      }

      boolean hasSortKey = false;

      // does an index with a sort key exist?
      for (DynamoIndex idx : potentialIndexList)
      {
         if (predicatesContainField(predicates, idx.getSortKey()))
         {
            dynamoIdx = idx;
            hasSortKey = true;
         }
      }

      String pk = dynamoIdx.getPartitionKey();
      String sk = dynamoIdx.getSortKey();

      List<String> excludeList = new ArrayList<>();
      if (hasPartitionKey)
      {
         // sorting only works for querying which means we must have a primary key to sort
         if (orderList != null && !orderList.isEmpty())
         {
            Order order = orderList.get(0);
            order.col = Parser.dequote(order.col);
            DynamoIndex index = DynamoDb.findIndexByTypeAndColumnName(table, DynamoDb.LOCAL_SECONDARY_TYPE, order.col);
            if (index != null)
            {
               // we must have an index for this field to be able to sort
               dynamoExpression.setOrderIndexInformation(order, index);
            }
            else if (sk != null && sk.equals(order.col))
            {
               // trying to sort by the table's sort key, no index is needed for this
               dynamoExpression.setOrderIndexInformation(order, null);
            }
         }
         else if (hasSortKey) // necessary for Global Secondary Indexes
            dynamoExpression.setOrderIndexInformation(new Order(sk, null), dynamoIdx);
         else // no sort key was found
            dynamoExpression.setOrderIndexInformation(null, dynamoIdx);

         excludeList.add(pk);
         if (dynamoExpression.getOrder() != null)
         {
            excludeList.add(dynamoExpression.getOrder().col);
         }
      }
      else
         dynamoExpression.setOrderIndexInformation(null, dynamoIdx);

      String andOr = "and";
      if (predicates.size() == 1)
      {
         if (predicates.get(0).getToken().equalsIgnoreCase("and") || predicates.get(0).getToken().equalsIgnoreCase("or"))
         {
            andOr = predicates.get(0).getToken();
            Predicate pred = predicates.get(0);
            predicates = pred.getTerms();
         }
      }

      return recursePredicates(predicates, dynamoExpression, andOr, excludeList, 0);
   }

   DynamoExpression recursePredicates(List<Predicate> predicates, DynamoExpression express, String andOr, List<String> excludes, int depth) throws Exception
   {
      if (predicates != null)
      {
         if (depth > 0)
         {
            express.append("\n");
         }
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

               if (DynamoExpression.isKnownOperator(pred.getToken()))
               {
                  String val = pred.getTerms().get(1).getToken();

                  express.append("\n");
                  express.appendSpaces(depth);
                  express.appendOperatorExpression(pred.getToken(), name, val);
               }
               else if (DynamoExpression.isKnownFunction(pred.getToken()))
               {
                  String val = null;
                  if (pred.getTerms().size() >= 2)
                  {
                     val = pred.getTerms().get(1).getToken();
                  }

                  express.append("\n");
                  express.appendSpaces(depth);
                  express.appendFunctionExpression(pred.getToken(), name, val);
               }
               else if ("and".equalsIgnoreCase(pred.getToken()) || "or".equalsIgnoreCase(pred.getToken()))
               {
                  recursePredicates(pred.getTerms(), express, pred.getToken(), excludes, depth);
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

   boolean predicatesContainField(List<Predicate> predicates, String field)
   {
      if (predicates != null)
      {
         for (Predicate pred : predicates)
         {
            if (DynamoExpression.isKnownOperator(pred.getToken()) || DynamoExpression.isKnownFunction(pred.getToken()))
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

}
