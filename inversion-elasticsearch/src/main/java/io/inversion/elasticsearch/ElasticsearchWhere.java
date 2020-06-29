/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.elasticsearch;

import java.util.List;

import io.inversion.ApiException;
import io.inversion.Index;
import io.inversion.rql.Term;
import io.inversion.rql.Where;
import io.inversion.utils.Rows.Row;

public class ElasticsearchWhere<T extends ElasticsearchWhere, P extends ElasticsearchQuery> extends Where<T, P> {

   public ElasticsearchWhere(P query) {
      super(query);
      clearFunctions();
      withFunctions("eq", "ne", "gt", "ge", "lt", "le", "ew", "sw", "w", "wo", "nn", "n", "and", "or", "emp", "nemp", "search", "in", "out");
   }

   @Override
   protected Term transform(Term parent) {
      Term transformed = parent;

      for (Term child : parent.getTerms()) {
         if (!child.isLeaf()) {
            if (!functions.contains(child.getToken()))
               ApiException.throw400BadRequest("Invalid where function token '%s' : %s", child.getToken(), parent);
            transform(child);
         }
      }

      //      if (!parent.isLeaf())
      //      {
      //         //check the first child expecting that to be the column name
      //         //if it is in the form "relationship.column then wrap this 
      //         //in an "exists" or "notExists" function
      //
      //         if (parent.getTerm(0).isLeaf() && parent.getToken(0).indexOf(".") > 0)
      //         {
      //            Term relCol = parent.getTerm(0);
      //            relCol.withToken("~~relTbl_" + relCol.getToken());
      //
      //            String token = parent.getToken().toLowerCase();
      //            if (existsFunctions.contains(token))
      //            {
      //               transformed = Term.term(parent.getParent(), "_exists", parent);
      //            }
      //            else if (notExistsFunctions.contains(token))
      //            {
      //               parent.withToken(notExistsMap.get(token));
      //               transformed = Term.term(parent.getParent(), "_notexists", parent);
      //            }
      //
      //            return transformed;
      //         }
      //      }

      if (parent.hasToken("_key")) {
         String indexName = parent.getToken(0);

         Index index = getParent().getCollection().getIndex(indexName);
         if (index == null)
            ApiException.throw400BadRequest("You can't use the _key() function unless your table has a unique index");

         if (index.size() == 1) {
            Term t = Term.term(null, "in", index.getColumnName(0));
            List<Term> children = parent.getTerms();
            for (int i = 1; i < children.size(); i++) {
               Term child = children.get(i);
               t.withTerm(child);
            }
            if (t.getNumTerms() == 2)
               t.withToken("eq");

            transformed = t;
         } else {
            //collection/valCol1~valCol2,valCol1~valCol2,valCol1~valCol2
            //keys(valCol1~valCol2,valCol1~valCol2,valCol1~valCol2)

            //or( and(eq(col1,val),eq(col2,val)), and(eq(col1,val),eq(col2,val)), and(eq(col1val), eq(col2,val)) 
            Term or = Term.term(null, "or");
            List<Term> children = parent.getTerms();
            transformed = or;

            for (int i = 1; i < children.size(); i++) {
               Term child = children.get(i);
               if (!child.isLeaf())
                  ApiException.throw400BadRequest("Entity key value is not a leaf node: %s", child);

               Row keyParts = getParent().getCollection().decodeResourceKey(index, child.getToken());
               Term and = Term.term(or, "and");
               for (String key : keyParts.keySet()) {
                  and.withTerm(Term.term(and, "eq", key, keyParts.get(key).toString()));
               }
            }
            if (or.getNumTerms() == 1) {
               transformed = or.getTerm(0);
               transformed.withParent(null);
            }
         }
      }

      if (parent.getParent() != null && transformed != parent)
         parent.getParent().replaceTerm(parent, transformed);

      return transformed;
   }

}