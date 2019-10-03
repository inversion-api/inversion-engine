/*
 * Copyright (c) 2015-2018 Inversion.org, LLC
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
package io.inversion.cloud.rql;

import java.util.ArrayList;
import java.util.List;

public class Parser
{
   public Term parse(String clause)
   {
      TermBuilder tb = new TermBuilder();
      Tokenizer t = new Tokenizer(clause);

      String token = null;
      while ((token = t.next()) != null)
      {
         String lc = token.toLowerCase();
         String func = lc.endsWith("(") ? lc.substring(0, lc.length() - 1) : null;

         if (func != null)
         {
            tb.push(Term.term(null, func));
         }
         else if (")".equals(lc))
         {
            tb.pop();
         }
         else if ("=".equals(lc))
         {
            Term top = tb.top();
            List<Term> children = top.getTerms();

            if ("eq".equalsIgnoreCase(top.getToken()) && children.size() == 2)
            {
               top.withToken(children.get(1).getToken());
               top.removeTerm(children.get(1));
            }
            else
            {
               tb.top().withToken("eq");
            }
         }
         else
         {
            tb.top().withTerm(Term.term(null, token));
         }
      }

      Term root = tb.root();

      if ("NULL".equals(root.getToken()))
      {
         Term child = root.getTerm(0);
         child.withParent(null);
         root = child;
      }

      return root;
   }

   class TermBuilder
   {
      Term       root  = null;
      List<Term> terms = new ArrayList();

      public Term top()
      {
         if (terms.size() == 0)
         {
            if (root == null)
               root = Term.term(null, null);

            terms.add(root);
         }

         return terms.get(terms.size() - 1);
      }

      public void push(Term term)
      {
         if (root == null)
         {
            root = term;
            terms.add(term);
         }
         else
         {
            top().withTerm(term);
            terms.add(term);
         }
      }

      public void push(String token)
      {
         top().withToken(token);
      }

      public void pop()
      {
         if (terms.size() > 0)
            terms.remove(terms.size() - 1);
      }

      public Term root()
      {
         return root;
      }
   }

}