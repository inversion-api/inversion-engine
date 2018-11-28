/**
 * 
 */
package io.rcktapp.rql.dynamo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.rcktapp.api.Index;
import io.rcktapp.api.Table;
import io.rcktapp.api.handler.dynamo.DynamoDb;
import io.rcktapp.rql.Order;
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
      String pk = DynamoDb.findPartitionKeyName(table);
      String sk = DynamoDb.findSortKeyName(table);

      DynamoExpression dynamoExpression = new DynamoExpression(table);

      Stmt stmt = buildStmt(new Stmt(this, null, null, null), null, requestParams, null);
      List<Predicate> predicates = stmt.where;
      List<Order> orderList = stmt.order;

      //ElasticRql rql = (ElasticRql) Rql.getRql("elastic");
      //QueryDsl queryDsl = rql.toQueryDsl(requestParams);
      //List<Predicate> predicates = queryDsl.getStmt().where;
      boolean hasPrimaryKey = predicatesContainField(predicates, pk);
      List<String> excludeList = new ArrayList<>();
      if (hasPrimaryKey)
      {
         // sorting only works for querying which means we must have a primary key to sort
         //         if (queryDsl.getOrder() != null && !queryDsl.getOrder().getOrderList().isEmpty())
         //         {
         //            Map<String, String> order = queryDsl.getOrder().getOrderList().get(0);

         if (orderList != null && !orderList.isEmpty())
         {
            Order order = orderList.get(0);
            String sortField = order.col;
            String sortDirection = order.dir;
            Index index = DynamoDb.findIndexByColumnName(table, sortField);
            if (index != null)
            {
               // we must have an index for this field to be able to sort
               dynamoExpression.setSortIndexInformation(sortField, sortDirection, index);
            }
            else if (sk != null && sk.equals(sortField))
            {
               // trying to sort by the table's sort key, no index is needed for this
               dynamoExpression.setSortIndexInformation(sortField, sortDirection, null);
            }
         }

         excludeList.add(pk);
         if (dynamoExpression.getSortField() != null)
         {
            excludeList.add(dynamoExpression.getSortField());
         }
      }

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
