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
package io.inversion.cloud.rql;

import java.util.ArrayList;
import java.util.List;

import io.inversion.cloud.model.Index;

public class Select<T extends Select, P extends Query> extends Builder<T, P>
{
   public Select(P query)
   {
      super(query);
      withFunctions("as", "includes", "excludes", "distinct", "count", "sum", "min", "max", "if", "aggregate", "function", "countascol", "rowcount");
   }

   public boolean isDistinct()
   {
      Term distinct = find("distinct");
      return distinct != null;

   }

   protected boolean addTerm(String token, Term term)
   {

      if (term.hasToken("function", "aggregate"))
      {
         String func = term.getTerm(0).getToken();
         String col = term.getTerm(1).getToken();

         getParent().withTerm("group", col);

         Term t = Term.term(null, func, col);

         if (term.size() > 2)
         {
            Term as = Term.term(null, "as", t, term.getTerm(2).getToken());
            getParent().withTerm(as);
         }
         else
         {
            getParent().withTerm(t);
         }
         return true;
      }

      if (term.hasToken("countascol"))
      {
         //this is done as a transformation here instead of in SqlRql.print because it requires the addition of a select and where clause
         String col = term.getToken(0);
         List<Term> terms = term.getTerms();

         for (int i = 1; i < terms.size(); i++)
         {
            getParent().withTerm("as(sum(if(eq(" + col + ", " + terms.get(i) + "), 1, 0))," + terms.get(i) + ")");
         }

         String str = "in(" + col;
         for (int i = 1; i < terms.size(); i++)
         {
            str += "," + terms.get(i).token;
         }
         str += ")";
         getParent().withTerm(str);

         return true;

      }

      if (functions.contains(token.toLowerCase()) && !term.hasToken("as", "includes", "excludes", "distinct"))
      {
         String asName = "$$$ANON";
         if (term.size() > 1 && term.hasToken("count", "sum", "min", "max"))
         {
            Term asT = term.getTerm(1);
            term.removeTerm(asT);
            asName = asT.getToken();
         }

         Term as = Term.term(null, "as", term, asName);
         withTerm(as);
         return true;
      }
      else
      {
         return super.addTerm(token, term);
      }
   }

   public List<String> getColumnNames()
   {
      List<String> columns = new ArrayList();

      for (Term include : findAll("includes"))
      {
         for (Term child : include.getTerms())
         {
            columns.add(child.getToken());
         }
      }
      return columns;//getColumnNames();
   }

   public List<Term> columns()
   {
      List<Term> columns = new ArrayList();

      for (Term include : findAll("includes"))
      {
         columns.addAll(include.getTerms());
      }
      
      boolean hasIncludes = columns.size() > 0;

      for (Term as : findAll("as"))
      {
         if (!hasIncludes)
         {
            columns.add(as);
         }
         else
         {
            String name = as.getToken(1);

            boolean replaced = false;
            for (int i = 0; i < columns.size(); i++)
            {
               Term column = columns.get(i);
               if (column.isLeaf() && column.hasToken(name))
               {
                  columns.set(i, as);
                  replaced = true;
                  break;
               }
            }
            if (!replaced)
            {
               columns.add(as);
            }
         }
      }
      return columns;
   }
}
