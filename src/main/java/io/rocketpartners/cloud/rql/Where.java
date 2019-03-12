/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
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
package io.rocketpartners.cloud.rql;

import java.util.List;

import io.rocketpartners.cloud.model.ApiException;
import io.rocketpartners.cloud.model.Index;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.utils.Rows.Row;

public class Where<T extends Where, P extends Query> extends Builder<T, P>
{

   public Where(P query)
   {
      super(query);
      withFunctions("_key", "and", "or", "not", "eq", "ne", "n", "nn", "like", "w", "sw", "ew", "lt", "le", "gt", "ge", "in", "out", "if", "w", "wo", "emp", "nemp");
   }

   protected boolean addTerm(String token, Term term)
   {
      if (term.hasToken("_key"))
      {
         if (term.getParent() != null)
            throw new ApiException(SC.SC_400_BAD_REQUEST, "Function key() can not be nested within other functions.");

         String indexName = term.getToken(0);

         Index index = getParent().table().getIndex(indexName);
         if (index == null)
            throw new ApiException(SC.SC_400_BAD_REQUEST, "You can't use the key() function unless your table has a unique index");

         if (index.getColumns().size() == 1)
         {
            Term t = Term.term(null, "in", index.getColumns().get(0).getName());
            List<Term> children = term.getTerms();
            for (int i = 1; i < children.size(); i++)
            {
               Term child = children.get(i);
               t.withTerm(child);
            }
            if (t.getNumTerms() == 2)
               t.withToken("eq");

            term = t;
         }
         else
         {
            //collection/valCol1~valCol2,valCol1~valCol2,valCol1~valCol2
            //keys(valCol1~valCol2,valCol1~valCol2,valCol1~valCol2)

            //or( and(eq(col1,val),eq(col2,val)), and(eq(col1,val),eq(col2,val)), and(eq(col1val), eq(col2,val)) 
            Term or = Term.term(null, "or");
            List<Term> children = term.getTerms();
            
            for (int i = 1; i < children.size(); i++)
            {
               Term child = children.get(i);
               if (!child.isLeaf())
                  throw new ApiException(SC.SC_400_BAD_REQUEST, "Entity key value is not a leaf node: " + child);

               Row keyParts = getParent().table().decodeKey(index, child.getToken());
               Term and = Term.term(or, "and");
               for (String key : keyParts.keySet())
               {
                  and.withTerm(Term.term(and, "eq", key, keyParts.get(key).toString()));
               }
            }
            if (or.getNumTerms() == 1)
            {
               or = or.getTerm(0);
               or.withParent(null);
            }
            term = or;
         }
      }
      return super.addTerm(token, term);
   }

   public List<Term> filters()
   {
      return getTerms();
   }

   public T key(Object... terms)
   {
      return withTerm("key", terms);
   }

   public T and(Object... terms)
   {
      return withTerm("and", terms);
   }

   public T or(Object... terms)
   {
      return withTerm("or", terms);
   }

   public T not(Object... terms)
   {
      return withTerm("not", terms);
   }

   public T eq(Object... terms)
   {
      return withTerm("eq", terms);
   }

   public T ne(Object... terms)
   {
      return withTerm("ne", terms);
   }

   public T like(Object... terms)
   {
      return withTerm("like", terms);
   }

   public T lt(Object... terms)
   {
      return withTerm("lt", terms);
   }

   public T gt(Object... terms)
   {
      return withTerm("eq", terms);
   }

   public T in(Object... terms)
   {
      return withTerm("in", terms);
   }

   public T out(Object... terms)
   {
      return withTerm("out", terms);
   }

   public T iff(Object... terms)
   {
      return withTerm("if", terms);
   }

   public T w(Object... terms)
   {
      return withTerm("w", terms);
   }

   public T wo(Object... terms)
   {
      return withTerm("wo", terms);
   }

   public T emp(Object... terms)
   {
      return withTerm("emp", terms);
   }

   public T nemp(Object... terms)
   {
      return withTerm("nemp", terms);
   }

}
