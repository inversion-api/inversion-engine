/**
 * 
 */
package io.rocketpartners.cloud.api.handler.dynamo;

import java.util.Map;

import io.rocketpartners.cloud.api.db.Table;
import io.rocketpartners.rql.Rql;

/**
 * @author tc-rocket
 *
 */
public class DynamoDbRql extends Rql<Table, DynamoDbQuery>
{

   static
   {
      Rql.addRql(new DynamoDbRql());
   }

   private DynamoDbRql()
   {
      super("dynamo");
      //setDoQuote(false);
   }

   public DynamoDbQuery buildQuery(Table table, Map<String, String> requestParams)
   {
      DynamoDbQuery dynamoQuery = new DynamoDbQuery(table);
      dynamoQuery.withTerms(requestParams);
      return dynamoQuery;
   }
   //      Stmt stmt = buildStmt(new Stmt(this, null, null, table), null, requestParams, null);
   //
   //      DynamoDbIndex dynamoIdx = null;
   //
   //      List<Predicate> predicates = stmt.where;
   //      List<Order> orderList = stmt.order;
   //
   //      boolean hasPartitionKey = false;
   //
   //      DynamoDbIndex primaryIdx = null;
   //
   //      // find the best index to use based on the given request params
   //      List<DynamoDbIndex> potentialIndexList = new ArrayList<DynamoDbIndex>();
   //      for (DynamoDbIndex idx : (List<DynamoDbIndex>) (List<?>) table.getIndexes())
   //      {
   //         if (predicatesContainField(predicates, idx.getPartitionKey()))
   //         {
   //            potentialIndexList.add(idx);
   //            dynamoIdx = idx;
   //            hasPartitionKey = true;
   //         }
   //
   //         // the primary index is the fallback 'best' index when no other appropriate indexes are found
   //         if (idx.getType().equals(DynamoDb.PRIMARY_TYPE))
   //            primaryIdx = idx;
   //      }
   //
   //      boolean hasSortKey = false;
   //
   //      // does an index with a sort key exist?
   //      for (DynamoDbIndex idx : potentialIndexList)
   //      {
   //         if (predicatesContainField(predicates, idx.getSortKey()))
   //         {
   //            dynamoIdx = idx;
   //            hasSortKey = true;
   //         }
   //      }
   //
   //      // This isn't necessary but makes debugging easier to understand when the primary index is expected though
   //      // a secondary index could have been used.
   //      if ((hasPartitionKey && !hasSortKey && dynamoIdx.getPartitionKey().equals(primaryIdx.getPartitionKey())) || !hasPartitionKey)
   //         dynamoIdx = primaryIdx;
   //
   //      dynamoExpression.setIndex(dynamoIdx);
   //
   //      String pk = dynamoIdx.getPartitionKey();
   //      String sk = dynamoIdx.getSortKey();
   //
   //      List<String> excludeList = new ArrayList<>();
   //      if (hasPartitionKey)
   //      {
   //         // sorting only works for querying which means we must have a partition key to sort
   //         if (orderList != null && !orderList.isEmpty())
   //         {
   //            Order order = orderList.get(0); // an index can only have one sort key
   //            order.col = Parser.dequote(order.col);
   //            if (sk != null)
   //            {
   //               if (sk.equals(order.col))
   //                  dynamoExpression.setOrder(order);
   //               else
   //                  throw new Exception("Cannot sort on field: '" + order.col + "' that does not match the sort key: '" + sk + "'");
   //            }
   //         }
   //         else if (hasSortKey) // default to the index's sort key if it exists but is not being specified.
   //            dynamoExpression.setOrder(new Order(sk, "ASC"));
   //
   //         excludeList.add(pk);
   //         if (dynamoExpression.getOrder() != null)
   //         {
   //            excludeList.add(dynamoExpression.getOrder().col);
   //         }
   //      }
   //      else if (orderList != null && !orderList.isEmpty())
   //      {
   //         throw new Exception("Cannot sort when executing a SCAN.");
   //      }
   //
   //      String andOr = "and";
   //      if (predicates.size() == 1)
   //      {
   //         if (predicates.get(0).getToken().equalsIgnoreCase("and") || predicates.get(0).getToken().equalsIgnoreCase("or"))
   //         {
   //            andOr = predicates.get(0).getToken();
   //            Predicate pred = predicates.get(0);
   //            predicates = pred.getTerms();
   //         }
   //      }
   //
   //      return recursePredicates(predicates, dynamoExpression, andOr, excludeList, 0);
   //   }
   //
   //   DynamoDbQuery recursePredicates(List<Predicate> predicates, DynamoDbQuery express, String andOr, List<String> excludes, int depth) throws Exception
   //   {
   //      if (predicates != null)
   //      {
   //         if (depth > 0)
   //         {
   //            express.append("\n");
   //         }
   //         express.appendSpaces(depth);
   //         express.append("(");
   //
   //         int cnt = 0;
   //         int excludeCnt = 0;
   //         depth++;
   //
   //         for (Predicate pred : predicates)
   //         {
   //            String name = pred.getTerms().get(0).getToken();
   //            if (excludes.contains(name.toLowerCase()))
   //            {
   //               express.addExcludedPredicate(name, pred);
   //               excludeCnt++;
   //            }
   //            else
   //            {
   //               if (cnt - excludeCnt > 0 && cnt < predicates.size())
   //               {
   //                  express.append("\n");
   //                  express.appendSpaces(depth);
   //                  express.append(andOr);
   //               }
   //
   //               if (DynamoDbQuery.isKnownOperator(pred.getToken()))
   //               {
   //                  String val = pred.getTerms().get(1).getToken();
   //
   //                  express.append("\n");
   //                  express.appendSpaces(depth);
   //                  express.appendOperatorExpression(pred.getToken(), name, val);
   //               }
   //               else if (DynamoDbQuery.isKnownFunction(pred.getToken()))
   //               {
   //                  String val = null;
   //                  if (pred.getTerms().size() >= 2)
   //                  {
   //                     val = pred.getTerms().get(1).getToken();
   //                  }
   //
   //                  express.append("\n");
   //                  express.appendSpaces(depth);
   //                  express.appendFunctionExpression(pred.getToken(), name, val);
   //               }
   //               else if ("and".equalsIgnoreCase(pred.getToken()) || "or".equalsIgnoreCase(pred.getToken()))
   //               {
   //                  recursePredicates(pred.getTerms(), express, pred.getToken(), excludes, depth);
   //               }
   //               else
   //               {
   //                  throw new Exception("unexpected rql token: " + pred.getToken());
   //               }
   //            }
   //            cnt++;
   //
   //         }
   //
   //         depth--;
   //         express.append("\n");
   //         express.appendSpaces(depth);
   //         express.append(")");
   //
   //      }
   //      return express;
   //   }
   //
   //   boolean predicatesContainField(List<Predicate> predicates, String field)
   //   {
   //      if (predicates != null)
   //      {
   //         for (Predicate pred : predicates)
   //         {
   //            if (DynamoDbQuery.isKnownOperator(pred.getToken()) || DynamoDbQuery.isKnownFunction(pred.getToken()))
   //            {
   //               String name = pred.getTerms().get(0).getToken();
   //               if (name.equalsIgnoreCase(field))
   //               {
   //                  return true;
   //               }
   //            }
   //            else if ("and".equalsIgnoreCase(pred.getToken()) || "or".equalsIgnoreCase(pred.getToken()))
   //            {
   //               return predicatesContainField(pred.getTerms(), field);
   //            }
   //         }
   //      }
   //      return false;
   //   }

}
