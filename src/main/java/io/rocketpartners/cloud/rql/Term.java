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
package io.rocketpartners.cloud.rql;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.util.Arrays;

public class Term
{
   public Term       parent = null;

   public String     token  = null;
   public List<Term> terms  = new ArrayList();

   public char       quote  = 0;

   protected Term(Term parent, String token)
   {
      withParent(parent);
      withToken(token);
   }

   public String getToken(int childIndex)
   {
      if (terms.size() > childIndex)
         return terms.get(childIndex).getToken();
      return null;
   }

   public String getToken()
   {
      if (token == null)
         return "NULL";

      return token;
   }

   public boolean hasToken(String... tokens)
   {
      if (token == null)
         return false;

      for (int i = 0; tokens != null && i < tokens.length; i++)
      {
         if (token.equalsIgnoreCase(tokens[i]))
            return true;
      }
      return false;
   }

   public Term withToken(String token)
   {
      quote = 0;
      if (token != null)
      {
         token = token.trim();

         if (token.length() > 1)
         {
            char start = token.charAt(0);
            if (token.charAt(token.length() - 1) == start && (start == '\'' || start == '"' || start == '`'))
            {
               quote = start;
               token = token.substring(1, token.length() - 1);
            }
         }
      }

      if (quote == '`') //replace mysql style with ansi sql style
         quote = '"';

      this.token = token;
      return this;
   }

   public Term getParent()
   {
      return parent;
   }

   public Term withParent(Term parent)
   {
      if (this.parent != parent)
      {
         this.parent = parent;
         if (parent != null)
         {
            parent.withTerm(this);
         }
      }
      this.parent = parent;

      return this;
   }

   public boolean isLeaf()
   {
      return terms.size() == 0;
   }

   public boolean isLeaf(int childIndex)
   {
      if (childIndex >= terms.size())
         return false;

      return getTerm(childIndex).isLeaf();
   }

   public int size()
   {
      return terms.size();
   }

   public int indexOf(Term child)
   {
      return terms.indexOf(child);
   }

   public int getNumTerms()
   {
      return terms.size();
   }

   public List<Term> getTerms()
   {
      return terms;
   }

   public Term getTerm(int index)
   {
      if (terms.size() > index)
         return terms.get(index);

      return null;
   }

   public Term withTerms(Term... terms)
   {
      for (Term term : terms)
      {
         withTerm(term);
      }
      return this;
   }

   public Term withTerm(Term term)
   {
      if (term == this)
         throw new RuntimeException("A term can not be a child of itself");

      if (!terms.contains(term))
      {
         terms.add(term);
         if (term.getParent() != this)
            term.withParent(this);
      }
      return this;
   }

   public Term withTerm(String token, Object... terms)
   {
      withTerm(term(this, token, terms));
      return this;
   }

   public Term withTerm(int index, Term term)
   {
      if (term == this)
         throw new RuntimeException("A term can not be a child of itself");

      if (terms.contains(term))
         terms.remove(term);

      terms.add(index, term);

      if (term.getParent() != this)
         term.withParent(this);

      return this;
   }

   public void removeTerm(Term term)
   {
      terms.remove(term);
   }

   public boolean isQuoted()
   {
      return quote != 0;
   }

   public char getQuote()
   {
      return quote;
   }

   public String toString()
   {
      StringBuffer buff = null;
      if (quote > 0)
      {
         buff = new StringBuffer("").append(quote).append(getToken()).append(quote);
      }
      else
      {
         buff = new StringBuffer(getToken());
      }
      if (terms.size() > 0)
      {
         buff.append("(");

         for (int i = 0; i < terms.size(); i++)
         {
            buff.append(terms.get(i).toString());
            if (i < terms.size() - 1)
               buff.append(",");
         }

         buff.append(")");
      }

      return buff.toString();
   }

   public static Term term(Term parent, String token, Object... terms)
   {
      if (terms != null && terms.length == 1 && Arrays.isArray(terms[0]))
         terms = (Object[]) terms[0];

      Term newTerm = new Term(parent, token);
      for (int i = 0; terms != null && i < terms.length; i++)
      {
         Object aTerm = terms[i];
         if (aTerm instanceof Term)
         {
            newTerm.withTerm((Term) aTerm);
         }
         else
         {
            newTerm.withTerm(new Term(newTerm, aTerm.toString()));
         }
      }

      return newTerm;
   }

   public Term val(String value)
   {
      withTerm(term(this, value));
      return this;
   }

   public Term pop()
   {
      return getParent();
   }

   public Term and()
   {
      Term or = term(this, "and");
      return or;
   }

   public Term or()
   {
      Term or = term(this, "or");
      return or;
   }

   public Term ge(String field, String value)
   {
      return withTerm("ge", field, value);
   }

   public Term lt(String field, String value)
   {
      return withTerm("lt", field, value);
   }

   public Term pageSize(int pageSize)
   {
      return withTerm("pageSize", pageSize);
   }

   public Term order(String... order)
   {
      return withTerm("order", (Object[]) order);
   }

}