/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
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
package io.inversion.rql;

import java.util.ArrayList;
import java.util.List;

public class RqlParser
{
   public Term parse(String clause)
   {
      TermBuilder tb = new TermBuilder();
      RqlTokenizer t = new RqlTokenizer(clause);

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