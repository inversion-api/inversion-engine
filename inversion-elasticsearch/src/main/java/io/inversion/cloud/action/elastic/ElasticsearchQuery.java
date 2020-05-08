/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.cloud.action.elastic;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;

import io.inversion.cloud.model.Collection;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Results;
import io.inversion.cloud.rql.Group;
import io.inversion.cloud.rql.Order;
import io.inversion.cloud.rql.Order.Sort;
import io.inversion.cloud.rql.Page;
import io.inversion.cloud.rql.Query;
import io.inversion.cloud.rql.Select;
import io.inversion.cloud.rql.Term;
import io.inversion.cloud.rql.Where;
import io.inversion.cloud.service.Chain;

/**
 * @author kfrankic
 * @see https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html
 */
// 
public class ElasticsearchQuery extends Query<ElasticsearchQuery, ElasticsearchDb, Select<Select<Select, ElasticsearchQuery>, ElasticsearchQuery>, Where<Where<Where, ElasticsearchQuery>, ElasticsearchQuery>, Group<Group<Group, ElasticsearchQuery>, ElasticsearchQuery>, Order<Order<Order, ElasticsearchQuery>, ElasticsearchQuery>, Page<Page<Page, ElasticsearchQuery>, ElasticsearchQuery>>
{

   public ElasticsearchQuery()
   {

   }

   public ElasticsearchQuery(Collection index, List<Term> terms)
   {
      super(index, terms);
   }

   @Override
   protected Select createSelect()
   {
      return new ElasticsearchSelect(this);
   }

   @Override
   protected ElasticsearchWhere createWhere()
   {
      return new ElasticsearchWhere(this);
   }

   protected ElasticsearchPage createPage()
   {
      return new ElasticsearchPage(this);
   }

   public Results doSelect() throws Exception
   {
      Results results = new Results(this);
      ElasticsearchDb db = getDb();

      Chain.debug(collection);

      //-- for test cases and query explain
      String debug = "ElasticsearchDb: index: " + collection.getName() + ", QueryBuilder=" + this.getJson();
      debug = debug.replaceAll("\r", "");
      debug = debug.replaceAll("\n", " ");
      debug = debug.replaceAll(" +", " ");
      Chain.debug(debug);
      results.withTestQuery(debug);
      //-- end test case debug stuff

      System.out.println();

      return results;
   }

   protected void push(List<JSNode> stack, JSNode child)
   {

   }

   public WrappedQueryBuilder buildQuery(Term parent, Term child)
   {
      QueryBuilder qb = null;

      String token = child.getToken().toLowerCase();
      String field = child.getToken(0);

      Object value = null;

      List<WrappedQueryBuilder> childBuilderList = new ArrayList<WrappedQueryBuilder>();

      // check the child terms' for the same nested base field value. 
      // ex: 'test.yellow' & 'test.blue' would have the same base of 'test' 
      String nestedPath = getNestedBaseIfExists(field);

      for (Term term : child.getTerms())
      {
         if (!term.isLeaf())
         {
            WrappedQueryBuilder childBuilder = buildQuery(child, term);
            childBuilderList.add(childBuilder);
         }
      }

      // pre-setting 'value' so that isn't not needed to be set several times below.
      if (child.getNumTerms() > 1)
         value = child.getTerm(1).getToken();

      BoolQueryBuilder boolBuilder = null;
      List<Object> valueList = null;
      switch (token)
      {
         case "gt":
            qb = QueryBuilders.rangeQuery(field).gt(value);
            break;
         case "ge":
            qb = QueryBuilders.rangeQuery(field).gte(value);
            break;
         case "lt":
            qb = QueryBuilders.rangeQuery(field).lt(value);
            break;
         case "le":
            qb = QueryBuilders.rangeQuery(field).lte(value);
            break;
         case "eq": // equal
            if (value instanceof String && ((String) value).contains("*"))
            {
               //               {
               //                  "query": {
               //                      "wildcard" : { "user" : "ki*y" }
               //                  }
               //              }               
               qb = QueryBuilders.wildcardQuery(field, value.toString());
            }
            else
            {
               //               {
               //                  "query": {
               //                    "term" : { "user" : "Kimchy" } 
               //                  }
               //                }
               qb = QueryBuilders.termQuery(field, value);
            }
            break;
         case "ne": // not equal

            //               "bool" : {
            //                  "must_not" : {
            //                    "term" : { "user" : "Kimchy" }
            //                  }
            //                }
            // * builders other than 'termQuery' could be added to the 'mustNot'
            qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.termQuery(field, value));
            break;
         case "and":
            WrappedQueryBuilder wqb = mergeChildBuilders(childBuilderList, child);

            if (wqb.hasNestedPath())
               nestedPath = wqb.getNestedPath();

            qb = wqb.getBuilder();
            break;
         case "or":
            // TODO break out into a method similar to mergeChildBuilders()
            boolBuilder = QueryBuilders.boolQuery();
            for (WrappedQueryBuilder childBuilder : childBuilderList)
            {
               if (childBuilder.hasNestedPath())
               {
                  nestedPath = childBuilder.getNestedPath();
               }

               boolBuilder.should(childBuilder.getBuilder());
            }
            if (nestedPath != null)
            {
               qb = QueryBuilders.nestedQuery(nestedPath, boolBuilder, ScoreMode.Avg);
               nestedPath = getNestedBaseIfExists(nestedPath);
            }
            else
               qb = boolBuilder;
            break;
         case "sw":
            qb = QueryBuilders.wildcardQuery(field, value + "*");
            break;
         case "ew":
            qb = QueryBuilders.wildcardQuery(field, "*" + value);
            break;
         case "w":
            // TODO break out into a method similar to mergeChildBuilders()
            List<QueryBuilder> withList = new ArrayList<QueryBuilder>();
            for (int i = 1; i < child.getNumTerms(); i++)
            {
               if (child.getTerm(i).isLeaf())
               {
                  value = child.getTerm(i).getToken();

                  withList.add(QueryBuilders.wildcardQuery(field, "*" + value + "*"));
               }
            }

            if (withList.size() > 1)
            {
               BoolQueryBuilder bqb = QueryBuilders.boolQuery();
               for (QueryBuilder withBuilder : withList)
               {
                  bqb.should(withBuilder);
               }
               qb = bqb;
            }
            else
            {
               qb = withList.get(0);
            }
            break;
         case "wo":
            qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.wildcardQuery(field, "*" + value + "*"));
            break;
         case "emp": // checks for empty strings AND null values
            qb = QueryBuilders.boolQuery()//
                              .should(QueryBuilders.termQuery(field, ""))//
                              .should(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(field)));
            break;
         case "nemp": // checks for empty strings AND null values
            qb = QueryBuilders.boolQuery()//
                              .must(QueryBuilders.boolQuery().mustNot(QueryBuilders.termQuery(field, "")))//
                              .must(QueryBuilders.boolQuery().must(QueryBuilders.existsQuery(field)));
            break;
         case "nn": // NOT NULL
            qb = QueryBuilders.existsQuery(field);
            break;
         case "n": // NULL
            qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(field));
            break;
         case "in":
            valueList = new ArrayList<Object>();
            for (int i = 1; i < child.getNumTerms(); i++)
            {
               if (child.getTerm(i).isLeaf())
               {
                  valueList.add(child.getTerm(i).getToken());
               }
            }
            qb = QueryBuilders.termsQuery(field, valueList);
            break;
         case "out":
            valueList = new ArrayList<Object>();
            for (int i = 1; i < child.getNumTerms(); i++)
            {
               if (child.getTerm(i).isLeaf())
               {
                  valueList.add(child.getTerm(i).getToken());
               }
            }
            qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.termsQuery(field, valueList));
            break;
         case "search":
            //            "query": {
            //               "fuzzy" : { "user" : "ki" }
            //            }
            qb = QueryBuilders.fuzzyQuery(field, value);
            break;
         default :
            throw new RuntimeException("unexpected rql token: " + token);

      }

      return new WrappedQueryBuilder(qb, child, nestedPath);

   }

   public SearchSourceBuilder getSearchBuilder()
   {
      QueryBuilder root = null;

      List<WrappedQueryBuilder> childList = new ArrayList<WrappedQueryBuilder>();

      for (Term term : where.getTerms())
      {
         WrappedQueryBuilder wrappedChild = buildQuery(null, term);
         childList.add(wrappedChild);
      }

      WrappedQueryBuilder wrappedBuilder = mergeChildBuilders(childList, null);
      root = wrappedBuilder.getBuilder();

      SearchSourceBuilder searchBuilder = null;

      if (select.getTerms().size() > 0)
      {
         if (searchBuilder == null)
            searchBuilder = new SearchSourceBuilder();

         List<String> includesList = null;
         List<String> excludesList = null;

         for (Term term : select.getTerms())
         {
            String token = term.getToken();
            if (token.equalsIgnoreCase("source") || token.equalsIgnoreCase("includes"))
            {
               if (includesList == null)
                  includesList = new ArrayList<>();

               for (Term selectTerm : term.getTerms())
               {
                  includesList.add(selectTerm.getToken());
               }
            }
            else if (token.equalsIgnoreCase("excludes"))
            {
               if (excludesList == null)
                  excludesList = new ArrayList<>();

               for (Term selectTerm : term.getTerms())
               {
                  excludesList.add(selectTerm.getToken());
               }
            }
         }

         String[] includesArray = null;
         String[] excludesArray = null;

         if (includesList != null && includesList.size() > 0)
         {
            includesArray = new String[includesList.size()];
            includesArray = includesList.toArray(includesArray);
         }
         if (excludesList != null && excludesList.size() > 0)
         {
            excludesArray = new String[excludesList.size()];
            excludesArray = excludesList.toArray(excludesArray);
         }

         searchBuilder.fetchSource(includesArray, excludesArray);
      }

      // A sort order MUST be set no matter the query.  If no sort was requested
      // by the client, default the sort to 'id' ascending.  If a sort order was
      // requested and that order is NOT 'id', add 'id' as the last sorting option.
      // This is necessary due to 'search after' queries that may need to continue
      // within the 'middle' of a value.  EX: A list of 100 rows sorted by 'state',
      // 50 of which have AL as a value.  If the pageSize is 30, 30 results will be
      // returned to the client, when the next page is requested (assuming a 'search
      // after' is required, the remaining 20 results will automatically be skipped
      // because we are starting the search after 'AL'
      if (searchBuilder == null)
         searchBuilder = new SearchSourceBuilder();

      boolean isSortingById = false;
      for (Sort sort : order.getSorts())
      {
         SortOrder so = sort.isAsc() ? SortOrder.ASC : SortOrder.DESC;
         String sortProp = sort.getProperty();
         searchBuilder.sort(sortProp, so);

         if (sortProp.equals("id"))
            isSortingById = true;
      }

      if (!isSortingById)
      {
         searchBuilder.sort("id", SortOrder.ASC);
      }

      // TODO .searchAfter() - use when searching/paging beyond the elastic search limit, 
      // which by default is 10,000, this will need to occur in the GetAction as several
      // .searchAfter()'s may be necessary to obtain the desired 'page'

      if (page.getTerms().size() > 0)
      {
         searchBuilder.size(page.getPageSize());

         // Dont set a 'from' value if it is not necessary.
         // Inversion defaults the first page to a value of 1.
         // Elastic defaults the first page to a value of 0.
         if (page.getPageNum() - 1 > 0)
         {
            // TODO remove 'from' from the query if this is a 'search after' query
            searchBuilder.from(page.getPageNum());
         }
      }

      //      if (searchBuilder != null)
      //      {
      return searchBuilder.query(root);

      //      }
      //      else if (root != null)
      //      {
      //         return root.toString();
      //      }

      //      return null;

   }

   public JSNode getJson()
   {
      return JSNode.parseJsonNode(getSearchBuilder().toString());
   }

   /**
    * specifically needed for handling nested children
    * @param childBuilderList
    * @return
    */
   private WrappedQueryBuilder mergeChildBuilders(List<WrappedQueryBuilder> childBuilderList, Term term)
   {

      QueryBuilder qb = null;
      BoolQueryBuilder boolBuilder = null;
      List<WrappedQueryBuilder> nestedList = new ArrayList<>();

      boolean nestedPathsMatch = true;
      String nestedPath = null;

      for (WrappedQueryBuilder childBuilder : childBuilderList)
      {
         if (childBuilder.hasNestedPath())
         {
            nestedList.add(childBuilder);
            if (nestedPath != null && !nestedPath.equalsIgnoreCase(childBuilder.getNestedPath()))
            {
               nestedPathsMatch = false;
            }
            else
            {
               nestedPath = childBuilder.getNestedPath();
            }
         }
         else if (childBuilderList.size() == 1)
         {
            // dont automatically add to the bool builder as it may be an only child
            qb = childBuilder.getBuilder();
         }
         else
         {
            if (boolBuilder == null)
               boolBuilder = QueryBuilders.boolQuery();
            boolBuilder.filter(childBuilder.getBuilder());
         }
      }

      if (nestedList.size() > 0)
      {
         BoolQueryBuilder nestedBoolBuilder = QueryBuilders.boolQuery();

         if (nestedList.size() > 1)
         {
            for (WrappedQueryBuilder nestedBuilder : nestedList)
            {
               if (nestedPathsMatch)
               {
                  // if the paths are the same, wrap the queries in a bool filter and then nest
                  nestedBoolBuilder.filter(nestedBuilder.getBuilder());
               }
               else
               {
                  // otherwise, create a nest for each builder, then add to a bool filter
                  qb = QueryBuilders.nestedQuery(nestedBuilder.getNestedPath(), nestedBuilder.getBuilder(), ScoreMode.Avg);
                  nestedBoolBuilder.filter(qb);
               }
            }

            if (nestedPathsMatch)
            {
               qb = QueryBuilders.nestedQuery(nestedPath, nestedBoolBuilder, ScoreMode.Avg);
               nestedPath = getNestedBaseIfExists(nestedPath);
            }
            else
            {
               qb = nestedBoolBuilder;
               nestedPath = null;
            }
         }
         else
         {
            // found an only child
            WrappedQueryBuilder wrappedBuilder = nestedList.get(0);
            qb = QueryBuilders.nestedQuery(nestedPath, wrappedBuilder.getBuilder(), ScoreMode.Avg);
            nestedPath = getNestedBaseIfExists(nestedPath);
         }

      }

      if (boolBuilder != null)
      {
         if (qb == null)
            qb = boolBuilder;
         else
            qb = boolBuilder.filter(qb);
      }

      return new WrappedQueryBuilder(qb, term, nestedPath);
   }

   private String getNestedBaseIfExists(String possibleNestedField)
   {
      String base = null;
      int lastPeriodIndex = possibleNestedField.lastIndexOf(".");
      if (lastPeriodIndex > 0 && lastPeriodIndex < possibleNestedField.length())
      {
         base = possibleNestedField.substring(0, lastPeriodIndex);
      }
      return base;
   }

}