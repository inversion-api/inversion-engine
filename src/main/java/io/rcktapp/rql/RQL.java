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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.rcktapp.rql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.forty11.j.J;
import io.forty11.sql.Sql;
import io.rcktapp.api.ApiException;
import io.rcktapp.api.SC;
import io.rcktapp.api.Table;
import io.rcktapp.rql.elasticsearch.BoolQuery;
import io.rcktapp.rql.elasticsearch.ElasticQuery;
import io.rcktapp.rql.elasticsearch.ExistsQuery;
import io.rcktapp.rql.elasticsearch.FuzzyQuery;
import io.rcktapp.rql.elasticsearch.NestedQuery;
import io.rcktapp.rql.elasticsearch.QueryDsl;
import io.rcktapp.rql.elasticsearch.Range;
import io.rcktapp.rql.elasticsearch.Term;
import io.rcktapp.rql.elasticsearch.Wildcard;

public class RQL
{

   public static final HashSet         SQL_RESERVED_KEYWORDS         = new HashSet(Arrays.asList(new String[]{"as", "includes", "sort", "order", "offset", "limit", "distinct", "aggregate", "function", "sum", "count", "min", "max"}));

   public static final HashSet<String> URL_RESERVED_PARAMETERS       = new HashSet<String>(                                                                                                                                                    //
                                                                                            Arrays.asList(                                                                                                                                     //
                                                                                                  new String[]{"q", "filter", "expands", "excludes", "format", "replace", "ignores"}                                                           //
                                                                                            ));

   public static final HashSet<String> OPS_RESERVED_KEYWORDS         = new HashSet<String>(Arrays.asList(new String[]{"nemp", "emp", "w", "ew", "sw", "eq", "ne", "lt", "le", "gt", "ge", "in", "out", "if", "or", "and", "miles", "search"}));

   Parser                              parser                        = null;

   public static final int             MAX_NORMAL_ELASTIC_QUERY_SIZE = 10000;

   public RQL(String db)
   {
      parser = new Parser(db);
   }

   public Parser getParser()
   {
      return parser;
   }

   public Stmt toSql(String sqlStr, Table table, Map<String, String> params) throws Exception
   {
      return toSql(sqlStr, table, params, null);
   }

   public Stmt toSql(String sqlStr, Table table, Map<String, String> params, Replacer r) throws Exception
   {
      String ignoresStr = params.get("ignores");

      if (!J.empty(ignoresStr))
      {
         List ignores = J.explode(",", ignoresStr.toLowerCase());
         for (String key : ((Map<String, String>) new HashMap(params)).keySet())
         {
            if (ignores.contains(key.toLowerCase()))
               params.remove(key);
         }
      }

      //-- do a search and replace on ${} vars in the passed in sql
      //-- before anything else is done.  Once replaced, these
      //-- vars are not considred by RQL
      {
         StringBuffer buff = new StringBuffer("");
         Pattern p = Pattern.compile("\\$\\{([^\\}]*)\\}");
         Matcher m = p.matcher(sqlStr);
         Set<String> toRemove = new HashSet();
         while (m.find())
         {
            String key = m.group(1);
            String value = Sql.check(params.get(key));
            if (value == null)
               throw new ApiException(SC.SC_400_BAD_REQUEST, "The value for param \"" + key + "\" can not be empty.");

            toRemove.add(key);
            m.appendReplacement(buff, value);
         }
         m.appendTail(buff);
         sqlStr = buff.toString();

         for (String key : toRemove)
         {
            params.remove(key);
         }
      }
      //--
      //-- end ${} search/replace

      Parts sql = new Parts(sqlStr);
      Stmt stmt = new Stmt(parser, sql, r, table);
      buildStmt(stmt, table, params, r, OPS_RESERVED_KEYWORDS);

      return stmt;
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

      Stmt stmt = new Stmt(parser, null, null, null);

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

         // remove the prevStart param if it exists...it wont be used.
         params.remove("prevstart");
      }

      // TODO
      // If a 'start' param is not supplied, but the pageNum * pageSize > 10k, 
      // we can do a normal search starting at pageNum-1 * pageSize, retrieve 
      // it's final value, then do a 'search after' from there.
      if (query.isSearchAfterNull() && pageNum * size > MAX_NORMAL_ELASTIC_QUERY_SIZE - 1 && pageNum - 1 > 0) {
         // loop from pageNum to wantedNum, each loop 
         // use the query, and only adjust the 'from' for each request.
      }

         // create the QDSL from the statement
         buildStmt(stmt, null, params, null, OPS_RESERVED_KEYWORDS);
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
      io.rcktapp.rql.elasticsearch.Order elasticOrder = null;
      List<Order> orderList = stmt.order;
      if (orderList.size() > 0)
      {
         boolean idSortExists = false;
         for (Order order : orderList)
         {
            if (Parser.dequote(order.col).equalsIgnoreCase("id"))
               idSortExists = true;
         }
         if (!idSortExists)
            orderList.add(new Order("id", "asc"));
      }
      for (int i = 0; i < orderList.size(); i++)
      {
         Order order = orderList.get(i);
         if (elasticOrder == null)
            elasticOrder = new io.rcktapp.rql.elasticsearch.Order(Parser.dequote(order.col), order.dir);
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
               elastic = new Wildcard(pred.terms.get(0).token, pred.terms.get(1).token);
            }
            else
            {
               elastic = new Term(pred.terms.get(0).token, pred.terms.get(1).token, pred.token);
            }
            break;
         case "ne": // not equal
            elastic = new Term(pred.terms.get(0).token, pred.terms.get(1).token, pred.token);
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
            elastic = new Wildcard(pred.terms.get(0).token, pred.terms.get(1).token + "*");
            break;
         case "ew":
            elastic = new Wildcard(pred.terms.get(0).token, "*" + pred.terms.get(1).token);
            break;
         case "w":
            elastic = new Wildcard(pred.terms.get(0).token, "*" + pred.terms.get(1).token + "*");
            break;
         case "emp":
            elastic = new BoolQuery();
            ((BoolQuery) elastic).addShould(new Term(pred.terms.get(0).token, "", "emp"));
            mustNotBool = new BoolQuery();
            mustNotBool.addMustNot(new ExistsQuery(pred.terms.get(0).token));
            ((BoolQuery) elastic).addShould(mustNotBool);
            break;
         case "nemp":
            elastic = new BoolQuery();
            mustNotBool = new BoolQuery();
            mustNotBool.addMustNot(new Term(pred.terms.get(0).token, "", "nemp"));
            ((BoolQuery) elastic).addMust(mustNotBool);
            BoolQuery mustBool = new BoolQuery();
            mustBool.addMust(new ExistsQuery(pred.terms.get(0).token));
            ((BoolQuery) elastic).addMust(mustBool);
            break;
         case "in":
            termsList = new ArrayList<Predicate>(pred.terms);
            elastic = new Term(termsList.remove(0).token, pred.token);
            for (Predicate pTerm : termsList)
               ((Term) elastic).addValue(pTerm.token);
            break;
         case "out":
            elastic = new BoolQuery();
            termsList = new ArrayList<Predicate>(pred.terms);
            Term term = new Term(termsList.remove(0).token, pred.token);
            for (Predicate pTerm : termsList)
               term.addValue(pTerm.token);
            ((BoolQuery) elastic).addMustNot(term);
            break;
         case "search":
            elastic = new FuzzyQuery(pred.terms.get(0).token, pred.terms.get(1).token);
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

   Stmt buildStmt(Stmt stmt, Table table, Map<String, String> params, Replacer r, HashSet<String> ops) throws Exception
   {
      List<Predicate> predicates = new ArrayList<Predicate>();

      for (String key : (Set<String>) params.keySet())
      {
         if (J.empty(key))
            continue;

         String value = (String) params.get(key);

         //skip if this is a reserved keyword
         if (!key.equalsIgnoreCase("q") && //
               !key.equalsIgnoreCase("filter") && //
               URL_RESERVED_PARAMETERS.contains(key.toLowerCase()))
            continue;

         String clauseStr = null;

         if (key.equalsIgnoreCase("q") || key.equalsIgnoreCase("filter"))
         {
            clauseStr = value;
         }
         else if (J.empty(value) && key.indexOf("(") > -1)
         {
            clauseStr = key;
         }
         else
         {
            clauseStr = key + "=" + value;
         }

         Predicate pred = parser.parse(clauseStr);
         predicates.add(pred);
      }

      for (Predicate p : predicates)
      {
         buildStmt(table, stmt, p, ops);
      }

      return stmt;
   }

   void buildStmt(Table table, Stmt stmt, Predicate p, HashSet<String> ops) throws Exception
   {
      String token = p.token;

      if (token.endsWith("("))
         token = token.substring(0, token.length() - 1);

      if (ops.contains(token.toLowerCase()))
      {
         stmt.where.add(p);
      }
      else if ("group".equalsIgnoreCase(token))
      {
         for (int i = 0; i < p.terms.size(); i++)
         {
            stmt.addGroupBy(p.term(i).token);
         }
      }
      else if ("includes".equalsIgnoreCase(token))
      {
         check(p.getParent() == null, "Token 'includes' is not valid as a nested function.");

         List<String> cols = new ArrayList<String>();

         for (int i = 0; i < p.terms.size(); i++)
         {
            cols.add(p.term(i).token);
         }
         stmt.setCols(cols);
      }
      else if ("distinct".equalsIgnoreCase(token))
      {
         check(p.getParent() == null, "Token 'distinct' is not valid as a nested function");

         stmt.distinct = true;
      }
      else if ("function".equalsIgnoreCase(token) || "aggregate".equalsIgnoreCase(token))
      {
         String func = Parser.dequote(p.term(0).token);
         String col = parser.asCol(p.term(1).token);

         Predicate newP = new Predicate(func);
         for (int i = 1; i < p.terms.size(); i++)
         {
            newP.addTerm(p.term(i));
         }

         String as = p.terms.size() > 2 ? parser.asCol(p.term(2).token) : null;

         stmt.addCol(as, newP);
         stmt.addGroupBy(col);
      }
      else if ("sum".equalsIgnoreCase(token) || "count".equalsIgnoreCase(token) || "min".equalsIgnoreCase(token) || "max".equalsIgnoreCase(token))
      {
         check((p.getParent() == null && p.terms.size() <= 2) || p.terms.size() == 1, "Function " + token + " may only have a second 'as' paramter if it is the root function");

         String as = p.terms.size() == 2 ? p.term(1).token : null;

         stmt.addCol(as, p);
      }
      else if ("rowcount".equalsIgnoreCase(token))
      {
         stmt.rowcount = "rowCount";
         if (p.terms.size() > 0)
            stmt.rowcount = p.term(0).token;
      }
      else if ("countascol".equalsIgnoreCase(token))
      {
         String col = p.term(0).token;

         for (int i = 1; i < p.terms.size(); i++)
         {
            Predicate countif = parser.parse("sum(if(eq(" + col + ", " + p.term(i).token + "), 1, 0))");
            stmt.addCol(Sql.check(p.term(i).token), countif);
         }

         String str = "in(" + col;
         for (int i = 1; i < p.terms.size(); i++)
         {
            str += "," + p.term(i).token;
         }
         Predicate in = parser.parse(str);
         stmt.where.add(in);
      }
      else if ("as".equalsIgnoreCase(token))
      {
         stmt.addCol(p.term(1).token, p.term(0));
      }
      else if ("offset".equalsIgnoreCase(token))
      {
         stmt.offset = toInt("offest", p.term(0).token);
         if (p.terms.size() > 1)
         {
            stmt.limit = toInt("limit", p.term(1).token);
         }
      }
      else if ("limit".equalsIgnoreCase(token))
      {
         stmt.limit = toInt("limit", p.term(0).token);
         if (p.terms.size() > 1)
         {
            stmt.offset = toInt("offset", p.term(1).token);
         }
      }
      else if ("page".equalsIgnoreCase(token) || "pagenum".equalsIgnoreCase(token))
      {
         stmt.pagenum = toInt(token, p.term(0).token);
         if (p.terms.size() > 1)
         {
            stmt.pagesize = toInt("pagesize", p.term(1).token);
         }
      }
      else if ("pagesize".equalsIgnoreCase(token))
      {
         stmt.pagesize = toInt(token, p.term(0).token);
      }
      else if ("order".equalsIgnoreCase(token) || "sort".equalsIgnoreCase(token))
      {
         for (int i = 0; i < p.terms.size(); i++)
         {
            String sort = parser.asCol(p.term(i).token);
            String desc = sort.indexOf('-') > -1 ? "DESC" : "ASC";
            sort = sort.replace("-", "");
            sort = sort.replace("+", "");
            stmt.order.add(new Order(sort, desc));
         }
      }
      else if (ops.contains(token.toLowerCase()))
      {
         stmt.where.add(p);
      }
      else
      {
         throw new RuntimeException("Unable to parse: " + p);
      }
   }

   void check(boolean condition, Object error)
   {
      if (!condition)
         throw new ApiException(SC.SC_400_BAD_REQUEST, "Unable to parse q/filter terms. Reason: " + error);
   }

   public int toInt(String field, String inStr)
   {
      String str = inStr;
      try
      {
         while (str.startsWith("`"))
            str = str.substring(1, str.length());

         while (str.endsWith("`"))
            str = str.substring(0, str.length() - 1);

         while (str.startsWith("'"))
            str = str.substring(1, str.length());

         while (str.endsWith("'"))
            str = str.substring(0, str.length() - 1);

         while (str.startsWith("\""))
            str = str.substring(1, str.length());

         while (str.endsWith("\""))
            str = str.substring(0, str.length() - 1);

         String limit = str.trim();
         return Integer.parseInt(limit);
      }
      catch (Exception ex)
      {
         throw new ApiException(SC.SC_400_BAD_REQUEST, "Expected an integer for field \"" + field + "\" but found value \"" + inStr + "\"");
      }
   }

   public String asCol(String string)
   {
      return getParser().asCol(Sql.check(string));
   }

   public String asStr(String string)
   {
      return getParser().asStr(Sql.check(string));
   }
}
