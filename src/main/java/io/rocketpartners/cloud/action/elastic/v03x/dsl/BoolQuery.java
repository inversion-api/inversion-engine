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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package io.rocketpartners.cloud.action.elastic.v03x.dsl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @author kfrankic
 *
 */
//only include non-null values.
@JsonInclude(Include.NON_NULL)
public class BoolQuery extends ElasticQuery
{
   // The clause (query) must appear in matching documents. However 
   // unlike must the score of the query will be ignored. Filter 
   // clauses are executed in filter context, meaning that scoring 
   // is ignored and clauses are considered for caching.
   // Could be of any type: Range, Term, etc
   private List<ElasticQuery> filter;

   // The clause (query) must appear in matching documents and will contribute to the score
   private List<ElasticQuery> must;

   private List<ElasticQuery> should;

   // The clause (query) must not appear in the matching documents. 
   // Clauses are executed in filter context meaning that scoring is 
   // ignored and clauses are considered for caching. Because scoring 
   // is ignored, a score of 0 for all documents is returned.
   private List<ElasticQuery> must_not;

   /**
    * This method is needed for Jackson to properly format the 'filter' value
    * @return
    */
   @JsonGetter("filter")
   public List<Map<String, ElasticQuery>> getFilterForElasticJson()
   {
      return getListForElasticJson(filter);
   }

   /**
    * This method is needed for Jackson to properly format the 'must' value.
    * @return
    */
   @JsonGetter("must")
   public List<Map<String, ElasticQuery>> getMustForElasticJson()
   {
      return getListForElasticJson(must);
   }

   /**
    * This method is needed for Jackson to properly format the 'must_not' value.
    * Used specifically with terms
    * @return
    */
   @JsonGetter("must_not")
   public List<Map<String, ElasticQuery>> getMustNotForElasticJson()
   {
      return getListForElasticJson(must_not);
   }

   /**
    * This method is needed for Jackson to properly format the 'should' value.
    * @return
    */
   @JsonGetter("should")
   public List<Map<String, ElasticQuery>> getShouldForElasticJson()
   {
      return getListForElasticJson(should);
   }

   public void divvyElasticList(List<ElasticQuery> elasticList)
   {
      // split the list up based on types
      for (int i = 0; i < elasticList.size(); i++)
      {
         ElasticQuery elastic = elasticList.get(i);
         if (elastic.nestedPath != null)
            this.nestedPath = elastic.getNestedPath();

         if (elastic instanceof Range || elastic instanceof Wildcard)
            addFilter(elastic);
         else if (elastic instanceof Term)
         {
            if (((Term) elastic).getToken().equals("ne"))
               addMustNot(elastic);
            else
               addFilter(elastic);
         }
         else if (elastic instanceof NestedQuery)
         {
            // combine any nested queries for the list that share the same path as this nested query.

            List<ElasticQuery> nestedList = new ArrayList<ElasticQuery>();
            nestedList.add(elastic);
            for (int n = i + 1; n < elasticList.size(); n++)
            {
               if (elasticList.get(n) instanceof NestedQuery && elasticList.get(n).getNestedPath() == elastic.getNestedPath())
               {
                  nestedList.add(elasticList.remove(n));
                  n--;
               }
            }

            if (nestedList.size() > 1)
            {
               BoolQuery bool = new BoolQuery();

               // move the filters from each nest to this nested query
               for (ElasticQuery nest : nestedList)
               {
                  if (((NestedQuery) nest).getQuery() != null)
                  {
                     QueryDsl query = ((NestedQuery) nest).getQuery();
                     if (query.getTerm() != null)
                        bool.addFilter(query.getTerm());
                     else if (query.getTerms() != null)
                        bool.addFilter(query.getTerms());
                     else if (query.getRange() != null)
                        bool.addFilter(query.getRange());
                     else if (query.getNested() != null)
                        bool.addFilter(query.getNested());
                     else if (query.getWildcard() != null)
                        bool.addFilter(query.getWildcard());
                     else if (query.getBool() != null)
                        bool.addFilter(query.getBool());
                  }
               }
               QueryDsl dsl = new QueryDsl();
               dsl.setBool(bool);

               ((NestedQuery) elastic).setQuery(dsl);
            }

            // add this new object to a filter.
            addFilter(elastic);

         }
         else
            addFilter(elastic);
      }

   }

   private List<Map<String, ElasticQuery>> getListForElasticJson(List<ElasticQuery> queryList)
   {
      if (queryList == null)
         return null;

      List<Map<String, ElasticQuery>> jsonList = new ArrayList<Map<String, ElasticQuery>>();

      for (int i = 0; i < queryList.size(); i++)
      {
         ElasticQuery elastic = queryList.get(i);
         if (elastic instanceof Range)
            jsonList.add(Collections.singletonMap("range", elastic));
         if (elastic instanceof Term)
         {
            if (((Term) elastic).isTerm())
               jsonList.add(Collections.singletonMap("term", elastic));
            else
               jsonList.add(Collections.singletonMap("terms", elastic));
         }
         else if (elastic instanceof Wildcard)
            jsonList.add(Collections.singletonMap("wildcard", elastic));
         else if (elastic instanceof BoolQuery)
            jsonList.add(Collections.singletonMap("bool", elastic));
         else if (elastic instanceof NestedQuery)
            jsonList.add(Collections.singletonMap("nested", elastic));
         else if (elastic instanceof ExistsQuery)
            jsonList.add(Collections.singletonMap("exists", elastic));
         else if (elastic instanceof FuzzyQuery)
            jsonList.add(Collections.singletonMap("fuzzy", elastic));
      }

      return jsonList;
   }

   public void addFilter(ElasticQuery elastic)
   {
      if (filter == null)
         filter = new ArrayList<ElasticQuery>();

      filter.add(elastic);

      if (elastic.getNestedPath() != null)
      {
         this.nestedPath = elastic.getNestedPath();
      }
   }

   public void addMust(ElasticQuery elastic)
   {
      if (must == null)
         must = new ArrayList<ElasticQuery>();

      must.add(elastic);

      if (elastic.getNestedPath() != null)
      {
         this.nestedPath = elastic.getNestedPath();
      }
   }

   public void addMustNot(ElasticQuery elastic)
   {
      if (must_not == null)
         must_not = new ArrayList<ElasticQuery>();

      must_not.add(elastic);

      if (elastic.getNestedPath() != null)
      {
         this.nestedPath = elastic.getNestedPath();
      }
   }

   public void addShould(ElasticQuery elastic)
   {
      if (should == null)
         should = new ArrayList<ElasticQuery>();

      should.add(elastic);

      if (elastic.getNestedPath() != null)
      {
         this.nestedPath = elastic.getNestedPath();
      }
   }

   /**
    * @return the must
    */
   public List<ElasticQuery> getMust()
   {
      return must;
   }

   /**
    * @return the filter
    */
   public List<ElasticQuery> getFilter()
   {
      return filter;
   }

   /**
    * @return the should
    */
   public List<ElasticQuery> getShould()
   {
      return should;
   }

   /**
    * @return the must_not
    */
   @JsonIgnore
   public List<ElasticQuery> getMustNot()
   {
      return must_not;
   }

}
