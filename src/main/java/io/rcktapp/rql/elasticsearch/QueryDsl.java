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
package io.rcktapp.rql.elasticsearch;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.rcktapp.rql.Stmt;

/**
 * @author kfrankic
 *
 */
// only include non-null values.
@JsonInclude(Include.NON_NULL)
public class QueryDsl extends ElasticQuery
{
   private Range        range;

   // Either 'term' or 'terms' should be null.  Both should not have 
   // values at the same time.  This is because a query does not 
   // allow both (side-by-side).  Unfortunately, I could not think
   // of a clever way to only use one field to represent both and
   // still get the proper field name of 'term' or 'terms'.
   private Term         term;
   private Term         terms;

   private FuzzyQuery   fuzzy;

   private Wildcard     wildcard;
   private BoolQuery    bool;
   private NestedQuery  nested;

   @JsonIgnore
   private List<String> searchAfter;

   @JsonIgnore
   private String       prevStart;

   @JsonIgnore
   private List<String> source;

   @JsonIgnore
   private List<String> excludes;

   // the statement used to create this query
   @JsonIgnore
   private Stmt         stmt;

   @JsonIgnore
   private Order        order;

   public Map<String, Object> toDslMap()
   {
      Map<String, Object> dslMap = new HashMap<String, Object>();

      Object _source = buildSource();
      if (_source != null)
      {
         dslMap.put("_source", _source);
      }

      if (range != null || term != null || terms != null || bool != null || wildcard != null || nested != null || fuzzy != null)
      {
         // @JsonIgnore properties will be ... ignored
         dslMap.put("query", this);
      }

      // Pagination
      int pagesize = stmt.pagesize == -1 ? stmt.maxRows : stmt.pagesize;
      int from = stmt.pagenum == -1 ? 0 : (stmt.pagenum * pagesize) - pagesize;
      dslMap.put("from", from);
      dslMap.put("size", pagesize);

      if (searchAfter != null)
      {
         dslMap.put("search_after", searchAfter);
         // search_after does not need 'from' to be set.
         dslMap.remove("from");
      }

      // Sorting - very basic 
      if (order != null)
         dslMap.put("sort", order.getOrderList());

      return dslMap;
   }

   Object buildSource()
   {
      if (excludes != null)
      {
         Map<String, Object> m = new HashMap<String, Object>();
         m.put("excludes", excludes);
         if (source != null)
         {
            m.put("includes", source);
         }

         return m;
      }
      else if (source != null)
      {
         return source;
      }

      return null;
   }

   /**
    * @return the stmt
    */
   public Stmt getStmt()
   {
      return stmt;
   }

   /**
    * @param stmt the stmt to set
    */
   public void setStmt(Stmt stmt)
   {
      this.stmt = stmt;
   }

   /**
    * @return the nested
    */
   public NestedQuery getNested()
   {
      return nested;
   }

   /**
    * @param nested the nested to set
    */
   public void setNested(NestedQuery nested)
   {
      this.nested = nested;
   }

   public void addSources(String[] sources)
   {
      this.source = Arrays.asList(sources);
   }

   /**
    * @return the sources
    */
   @JsonIgnore
   public List<String> getSources()
   {
      return source;
   }

   public void addExcludes(String[] excludes)
   {
      this.excludes = Arrays.asList(excludes);
   }

   /**
    * @return the sources
    */
   @JsonIgnore
   public List<String> getExcludes()
   {
      return excludes;
   }

   /**
    * @param searchAfter the searchAfter to set
    */
   public void setPreviousStart(String prev)
   {
      this.prevStart = prev;
   }

   @JsonIgnore
   public String getPreviousStart()
   {
      return this.prevStart;
   }

   @JsonIgnore
   public boolean isSearchAfterNull()
   {
      return searchAfter == null;
   }
   
   public void setSearchAfter(List<String> searchAfter)
   {
      this.searchAfter = searchAfter;
   }

   @JsonIgnore
   public String getSearchAfterAsString()
   {
      return this.searchAfter.stream().map(String::valueOf).collect(Collectors.joining(","));
   }

   /**
    * @return the range
    */
   public Range getRange()
   {
      return range;
   }

   /**
    * @param range the range to set
    */
   public void setRange(Range range)
   {
      this.range = range;
   }

   /**
    * @return the term
    */
   public Term getTerm()
   {
      return term;
   }

   /**
    * @param term the term to set
    */
   public void setTerm(Term term)
   {
      // while this should be unnecessary, I'm setting the 
      // 'opposite' value to null to ensure that only one of 
      // 'term'/'terms' has a value.
      if (term.isTerm())
      {
         this.term = term;
         this.terms = null;
      }
      else
      {
         this.terms = term;
         this.term = null;
      }
   }

   /**
    * @return the terms
    */
   public Term getTerms()
   {
      return terms;
   }

   /**
    * @return the wildcard
    */
   public Wildcard getWildcard()
   {
      return wildcard;
   }

   /**
    * @param wildcard the wildcard to set
    */
   public void setWildcard(Wildcard wildcard)
   {
      this.wildcard = wildcard;
   }

   /**
    * @return the bool
    */
   public BoolQuery getBool()
   {
      return bool;
   }

   /**
    * @param bool the bool to set
    */
   public void setBool(BoolQuery bool)
   {
      this.bool = bool;
   }

   /**
    * @return the fuzzy
    */
   public FuzzyQuery getFuzzy()
   {
      return fuzzy;
   }

   /**
    * @param fuzzy the fuzzy to set
    */
   public void setFuzzy(FuzzyQuery fuzzy)
   {
      this.fuzzy = fuzzy;
   }

   public void setOrder(Order order)
   {
      this.order = order;
   }

   public Order getOrder()
   {
      return this.order;
   }

}
