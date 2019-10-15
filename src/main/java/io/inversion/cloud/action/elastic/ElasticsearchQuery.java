/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
 * https://github.com/inversion-api
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
package io.inversion.cloud.action.elastic;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.inversion.cloud.action.sql.SqlDb;
import io.inversion.cloud.model.Collection;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Table;
import io.inversion.cloud.rql.Group;
import io.inversion.cloud.rql.Order;
import io.inversion.cloud.rql.Page;
import io.inversion.cloud.rql.Query;
import io.inversion.cloud.rql.Select;
import io.inversion.cloud.rql.Term;
import io.inversion.cloud.rql.Where;

/**
 * @author kfrankic
 * @see https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html
 */
// 
public class ElasticsearchQuery extends Query<ElasticsearchQuery, SqlDb, Table, Select<Select<Select, ElasticsearchQuery>, ElasticsearchQuery>, Where<Where<Where, ElasticsearchQuery>, ElasticsearchQuery>, Group<Group<Group, ElasticsearchQuery>, ElasticsearchQuery>, Order<Order<Order, ElasticsearchQuery>, ElasticsearchQuery>, Page<Page<Page, ElasticsearchQuery>, ElasticsearchQuery>>
{
   // identifies a nested path
   @JsonIgnore
   protected String nestedPath;

   public String getNestedPath()
   {
      return nestedPath;
   }

   //   private List<String> searchAfter;
   //
   //   private String       prevStart;
   //
   //   private List<String> source;
   //
   //   private List<String> excludes;

   public ElasticsearchQuery(Collection collection, Object terms)
   {
      super(null, terms);
      withTerms(terms);
   }

   protected ElasticsearchPage createPage()
   {
      return new ElasticsearchPage(this);
   }

   protected void push(List<JSNode> stack, JSNode child)
   {

   }

   public JSNode getJson()
   {
      JSNode root = new JSNode();
      for (Term term : getTerms())
      {
         JSNode child = toJson(null, term);
      }

      return root;
   }

   public JSNode toJson(Term parent, Term child)
   {
      JSNode query = null;

      String token = child.getToken().toLowerCase();
      String field = child.getToken(0);

      Object value = child.getTerm(1).isLeaf() ? child.getToken(1) : toJson(child, child.getTerm(1));

      switch (token)
      {
         case "gt":
         case "ge":
         case "lt":
         case "le":
            //       "range" : {
            //            "age" : {
            //                "gte" : 10,
            //                "lte" : 20,
            //            }
            //        }
            query = new JSNode("range", new JSNode(field, new JSNode(token, value)));
            break;
         case "eq": // equal
         case "ne": // not equal
            if (value instanceof String && ((String) value).contains("*"))
            {
               //               {
               //                  "query": {
               //                      "wildcard" : { "user" : "ki*y" }
               //                  }
               //              }               
               query = new JSNode("wildcard", new JSNode(field, value));
            }
            else
            {
               //               {
               //                  "query": {
               //                    "term" : { "user" : "Kimchy" } 
               //                  }
               //                }
               query = new JSNode("term", new JSNode(field, value));
            }

            if ("ne".equals(token))
            {
               //               "bool" : {
               //                  "must_not" : {
               //                    "range" : {
               //                      "age" : { "gte" : 10, "lte" : 20 }
               //                    }
               //                  }
               //                }
               query = new JSNode("bool", new JSNode("must_not", query));
            }
            break;
         //         case "and":
         //            elastic = new BoolQuery();
         //            ((BoolQuery) elastic).divvyElasticList(elasticList);
         //            break;
         //         case "or":
         //            elastic = new BoolQuery();
         //            // add everything in the list to the 'should' to 'or' the list together.
         //            for (ElasticQuery eq : elasticList)
         //               ((BoolQuery) elastic).addShould(eq);
         //            break;
         case "sw":
            query = new JSNode("wildcard", new JSNode(field, value + "*"));
            break;
         case "ew":
            query = new JSNode("wildcard", new JSNode(field, "*" + value));
            break;
         case "w":
            query = new JSNode("wildcard", new JSNode(field, "*" + value + "*"));
            break;
         case "wo":

            //        "bool" : {
            //            "must_not" : {
            //              "range" : {
            //                "age" : { "gte" : 10, "lte" : 20 }
            //              }
            //            }
            //         }
            break;
         //         case "emp": // checks for empty strings AND null values
         //            elastic = new BoolQuery();
         //            ((BoolQuery) elastic).addShould(new Term(pred.getTerms().get(0).token, "", "emp"));
         //            mustNotBool = new BoolQuery();
         //            mustNotBool.addMustNot(new ExistsQuery(pred.getTerms().get(0).token));
         //            ((BoolQuery) elastic).addShould(mustNotBool);
         //            break;
         //         case "nemp": // checks for empty strings AND null values
         //            elastic = new BoolQuery();
         //            mustNotBool = new BoolQuery(); // 'mustNotBool' is used even-though it should be named 'mustBool'
         //            mustNotBool.addMustNot(new Term(pred.getTerms().get(0).token, "", "nemp"));
         //            ((BoolQuery) elastic).addMust(mustNotBool);
         //            BoolQuery mustBool = new BoolQuery();
         //            mustBool.addMust(new ExistsQuery(pred.getTerms().get(0).token));
         //            ((BoolQuery) elastic).addMust(mustBool);
         //            break;
         //         case "nn": // NOT NULL
         //            query = new ObjectNode("exists", new ObjectNode("field", field));
         //            break;
         //         case "n": // NULL
         //            elastic = new BoolQuery();
         //            ((BoolQuery) elastic).addMustNot(new ExistsQuery(pred.getTerms().get(0).token));
         //            break;
         //         case "in":
         //            termsList = new ArrayList<Term>(pred.terms);
         //            elastic = new Term(termsList.remove(0).token, pred.token);
         //            for (Term pTerm : termsList)
         //               ((Term) elastic).addValue(Parser.dequote(pTerm.token));
         //            break;
         //         case "out":
         //            elastic = new BoolQuery();
         //            termsList = new ArrayList<Term>(pred.terms);
         //            Term term = new Term(termsList.remove(0).token, pred.token);
         //            for (Term pTerm : termsList)
         //               term.addValue(Parser.dequote(pTerm.token));
         //            ((BoolQuery) elastic).addMustNot(term);
         //            break;
         case "search":
            //            "query": {
            //               "fuzzy" : { "user" : "ki" }
            //            }
            query = new JSNode("fuzzy", new JSNode(field, value));
            break;
         default :
            throw new RuntimeException("unexpected rql token: " + token);

      }
      return query;

   }

}