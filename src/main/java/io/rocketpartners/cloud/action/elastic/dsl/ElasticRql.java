package io.rocketpartners.cloud.action.elastic.dsl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.rocketpartners.cloud.action.elastic.rql.Parser;
import io.rocketpartners.cloud.action.elastic.rql.Predicate;
import io.rocketpartners.cloud.action.elastic.rql.Rql;
import io.rocketpartners.cloud.action.elastic.rql.Stmt;

public class ElasticRql extends Rql
{
   static
   {
      Rql.addRql(new ElasticRql());
   }

   public static final int MAX_NORMAL_ELASTIC_QUERY_SIZE = 10000;

   private enum WithType {
      WITH, STARTS_WITH, ENDS_WITH
   };

   private ElasticRql()
   {
      super("elastic");
      setDoQuote(false);
   }

   public QueryDsl toQueryDsl(Map<String, String> params) throws Exception
   {
      QueryDsl query = new QueryDsl();
      List<ElasticQuery> elasticList = new ArrayList<ElasticQuery>();

      // If a 'source' parameter is sent, remove it and add it's value to the DSL.
      // This must be done before the Stmt object is created.
      if (params.containsKey("source"))
      {
         query.addSources(params.remove("source").split(","));
      }

      if (params.containsKey("excludes"))
      {
         query.addExcludes(params.remove("excludes").split(","));
      }

      Stmt stmt = new Stmt(this, null, null, null);

      // pageNum and pagesize can be used to determine if an 'search_after' query should occur 
      String sizeStr = params.get("pagesize"); // if no pagesize is specified, assume max_limit
      int size = stmt.getMaxRows();
      if (sizeStr != null)
         size = toInt("pagesize", sizeStr);
      String pageStr = params.get("pagenum");
      int pageNum = 1;
      if (pageStr != null)
         pageNum = toInt("pageNum", pageStr);

      // A 'start' param indicates an elastic 'search after' query should be used.
      // 'search after' queries should ONLY be used if it is believe the result 
      // will come from a row index > 10k.  
      if (params.containsKey("start"))
      {
         List<String> searchAfterList = Arrays.asList(params.remove("start").split(","));
         if (pageNum * size > MAX_NORMAL_ELASTIC_QUERY_SIZE - 1)
         {
            for (int i = 0; i < searchAfterList.size(); i++)
            {
               if (searchAfterList.get(i).equals("[NULL]")) // [NULL] is used to indicate an actual null value, not a null string.
                  searchAfterList.set(i, null);
            }
            query.setSearchAfter(searchAfterList);
         }

         // use this value if wantedpage was not set; prevents having to lookup the prev value...of course.
         params.remove("prevstart");
      }

      // create the QDSL from the statement
      buildStmt(stmt, null, params, null);
      query.setStmt(stmt);

      // use 'stmt.where' to create the elastic search json. 
      // 'where' contains Predicate objects that should be used
      // to break down the statement
      List<Predicate> predicateList = stmt.where;
      for (int i = 0; i < predicateList.size(); i++)
      {
         Predicate pred = predicateList.get(i);
         // recursion ... be afraid!
         elasticList.add(convertPredicate(pred));
      }

      // Use 'stmt.order' to create the sorting json.
      // A sort order MUST be set no matter the query.  If no sort was requested
      // by the client, default the sort to 'id' ascending.  If a sort order was
      // requested and that order is NOT 'id', add 'id' as the last sorting option.
      // This is necessary due to 'search after' queries that may need to continue
      // within the 'middle' of a value.  EX: A list of 100 rows sorted by 'state',
      // 50 of which have AL as a value.  If the pageSize is 30, 30 results will be
      // returned to the client, when the next page is requested (assuming a 'search
      // after' is required, the remaining 20 results will automatically be skipped
      // because we are starting the search after 'AL'
      Order elasticOrder = null;
      List<io.rocketpartners.cloud.action.elastic.rql.Order> orderList = stmt.order;
      //      if (orderList.size() > 0)
      //      {
      boolean idSortExists = false;
      for (io.rocketpartners.cloud.action.elastic.rql.Order order : orderList)
      {
         if (Parser.dequote(order.col).equalsIgnoreCase("id"))
            idSortExists = true;
      }
      if (!idSortExists)
         orderList.add(new io.rocketpartners.cloud.action.elastic.rql.Order("id", "asc"));
      //      }
      for (int i = 0; i < orderList.size(); i++)
      {
         io.rocketpartners.cloud.action.elastic.rql.Order order = orderList.get(i);
         if (elasticOrder == null)
            elasticOrder = new Order(Parser.dequote(order.col), order.dir);
         else
            elasticOrder.addOrder(Parser.dequote(order.col), order.dir);
      }

      query.setOrder(elasticOrder);

      elasticListToDsl(query, elasticList);

      return query;
   }

   private void elasticToDsl(QueryDsl dsl, ElasticQuery elastic)
   {
      if (elastic != null)
      {
         if (elastic instanceof Range)
         {
            dsl.setRange((Range) elastic);
         }
         else if (elastic instanceof Wildcard)
         {
            dsl.setWildcard((Wildcard) elastic);
         }
         else if (elastic instanceof Term)
         {
            if (((Term) elastic).getToken().equals("ne"))
            {
               BoolQuery bool = new BoolQuery();
               bool.addMustNot(elastic);
               dsl.setBool(bool);
            }
            else
               dsl.setTerm((Term) elastic);
         }
         else if (elastic instanceof BoolQuery)
         {
            dsl.setBool((BoolQuery) elastic);
         }
         else if (elastic instanceof NestedQuery)
            dsl.setNested((NestedQuery) elastic);
         else if (elastic instanceof FuzzyQuery)
            dsl.setFuzzy((FuzzyQuery) elastic);
      }
   }

   private void elasticListToDsl(QueryDsl dsl, List<ElasticQuery> elasticList)
   {
      if (elasticList.size() == 1)
      {
         elasticToDsl(dsl, elasticList.get(0));
      }
      else
      {
         BoolQuery bool = new BoolQuery();
         bool.divvyElasticList(elasticList);
         dsl.setBool(bool);
      }
   }

   private ElasticQuery convertPredicate(Predicate pred) throws Exception
   {

      List<ElasticQuery> elasticList = new ArrayList<ElasticQuery>();
      ElasticQuery elastic = null;

      if (pred.terms.size() > 0)
      {
         for (int i = 0; i < pred.terms.size(); i++)
         {
            elastic = convertPredicate(pred.term(i));
            if (elastic != null)
               elasticList.add(elastic);
         }
      }
      else // nothing to do atm
         return null;

      BoolQuery mustNotBool = null;
      List<Predicate> termsList = null;
      String termToken = null;

      switch (pred.token)
      {

         case "gt": // greater than
            elastic = new Range(pred.terms.get(0).token);
            ((Range) elastic).setGt(pred.terms.get(1).token);
            break;
         case "ge": // greater than or equal
            elastic = new Range(pred.terms.get(0).token);
            ((Range) elastic).setGte(pred.terms.get(1).token);
            break;
         case "lt": // less than
            elastic = new Range(pred.terms.get(0).token);
            ((Range) elastic).setLt(pred.terms.get(1).token);
            break;
         case "le": // less than or equal
            elastic = new Range(pred.terms.get(0).token);
            ((Range) elastic).setLte(pred.terms.get(1).token);
            break;
         case "eq": // equal
            if (pred.terms.get(1).token.contains("*"))
            {
               elastic = new Wildcard(pred.terms.get(0).token, Parser.dequote(pred.terms.get(1).token));
            }
            else
            {
               elastic = new Term(pred.terms.get(0).token, Parser.dequote(pred.terms.get(1).token), pred.token);
            }
            break;
         case "ne": // not equal
            elastic = new Term(pred.terms.get(0).token, Parser.dequote(pred.terms.get(1).token), pred.token);
            break;
         case "and":
            elastic = new BoolQuery();
            ((BoolQuery) elastic).divvyElasticList(elasticList);
            break;
         case "or":
            elastic = new BoolQuery();
            // add everything in the list to the 'should' to 'or' the list together.
            for (ElasticQuery eq : elasticList)
               ((BoolQuery) elastic).addShould(eq);
            break;
         case "sw":
            elastic = withWildCardPopulater(pred, WithType.STARTS_WITH);
            break;
         case "ew":
            elastic = withWildCardPopulater(pred, WithType.ENDS_WITH);
            break;
         case "w":
            elastic = withWildCardPopulater(pred, WithType.WITH);
            break;
         case "wo":
            elastic = new BoolQuery();
            ((BoolQuery) elastic).addMustNot(new Wildcard(pred.terms.get(0).token, "*" + Parser.dequote(pred.terms.get(1).token) + "*"));
            break;
         case "emp": // checks for empty strings AND null values
            elastic = new BoolQuery();
            ((BoolQuery) elastic).addShould(new Term(pred.terms.get(0).token, "", "emp"));
            mustNotBool = new BoolQuery();
            mustNotBool.addMustNot(new ExistsQuery(pred.terms.get(0).token));
            ((BoolQuery) elastic).addShould(mustNotBool);
            break;
         case "nemp": // checks for empty strings AND null values
            elastic = new BoolQuery();
            mustNotBool = new BoolQuery(); // 'mustNotBool' is used even-though it should be named 'mustBool'
            mustNotBool.addMustNot(new Term(pred.terms.get(0).token, "", "nemp"));
            ((BoolQuery) elastic).addMust(mustNotBool);
            BoolQuery mustBool = new BoolQuery();
            mustBool.addMust(new ExistsQuery(pred.terms.get(0).token));
            ((BoolQuery) elastic).addMust(mustBool);
            break;
         case "nn": // NOT NULL
            elastic = new BoolQuery();
            ((BoolQuery) elastic).addMust(new ExistsQuery(pred.terms.get(0).token));
            break;
         case "n": // NULL
            elastic = new BoolQuery();
            ((BoolQuery) elastic).addMustNot(new ExistsQuery(pred.terms.get(0).token));
            break;
         case "in":
            termsList = new ArrayList<Predicate>(pred.terms);
            elastic = new Term(termsList.remove(0).token, pred.token);
            for (Predicate pTerm : termsList)
               ((Term) elastic).addValue(Parser.dequote(pTerm.token));
            break;
         case "out":
            elastic = new BoolQuery();
            termsList = new ArrayList<Predicate>(pred.terms);
            Term term = new Term(termsList.remove(0).token, pred.token);
            for (Predicate pTerm : termsList)
               term.addValue(Parser.dequote(pTerm.token));
            ((BoolQuery) elastic).addMustNot(term);
            break;
         case "search":
            elastic = new FuzzyQuery(pred.terms.get(0).token, Parser.dequote(pred.terms.get(1).token));
            break;
         default :
            throw new Exception("unexpected rql token: " + pred.token);
      }

      // check for nested values
      if (elastic.getNestedPath() != null)
      {
         NestedQuery nested = new NestedQuery();
         QueryDsl query = new QueryDsl();
         elasticListToDsl(query, Collections.singletonList(elastic));
         nested.setQuery(query);
         nested.setPath(elastic.getNestedPath());
         return nested;
      }
      else
         return elastic;

   }

   /**
    * 
    * @param pred
    * @param withType WITH, STARTS_WITH, ENDS_WITH
    */
   private BoolQuery withWildCardPopulater(Predicate pred, WithType withType)
   {
      BoolQuery bq = new BoolQuery();
      String termToken = pred.terms.get(0).token;
      for (int i = 1; i < pred.terms.size(); i++)
      {
         switch (withType)
         {
            case WITH:
               bq.addShould(new Wildcard(termToken, "*" + Parser.dequote(pred.terms.get(i).token) + "*"));
               break;
            case STARTS_WITH:
               bq.addShould(new Wildcard(termToken, Parser.dequote(pred.terms.get(i).token) + "*"));
               break;
            case ENDS_WITH:
               bq.addShould(new Wildcard(termToken, "*" + Parser.dequote(pred.terms.get(i).token)));
               break;
         }
      }
      return bq;
   }
}
