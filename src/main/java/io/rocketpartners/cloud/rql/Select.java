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

import java.util.ArrayList;
import java.util.List;

import io.rocketpartners.cloud.model.Index;

public class Select<T extends Select, P extends Query> extends Builder<T, P>
{
   public Select(P query)
   {
      super(query);
      withFunctions("as", "includes", "distinct", "count", "sum", "min", "max", "if", "aggregate", "function", "countascol", "rowcount");
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
