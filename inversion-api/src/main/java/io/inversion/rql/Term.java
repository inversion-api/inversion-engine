/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
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
package io.inversion.rql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class Term implements Comparable<Term>
{
   public Term       parent = null;
   public char       quote  = 0;
   public String     token  = null;
   public List<Term> terms  = new ArrayList();

   protected Term()
   {

   }

   protected Term(Term parent, String token)
   {
      withParent(parent);
      withToken(token);
   }

   public Term copy()
   {
      Term copy = new Term();

      copy.quote = quote;
      copy.token = token;

      terms.forEach(child -> copy.withTerm(child.copy()));

      return copy;
   }

   @Override
   public int compareTo(Term o)
   {
      int val = token.compareTo(o.token);
      if (val == 0)
      {
         for (int i = 0; i < terms.size(); i++)
         {
            if (o.terms.size() <= i)
               return 1;

            val = terms.get(i).compareTo(o.terms.get(i));

            if (val != 0)
               break;
         }
      }
      return val;
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

   public Term replaceTerm(Term oldTerm, Term newTerm)
   {
      terms.remove(newTerm);//make sure not in there twice

      int idx = terms.indexOf(oldTerm);
      if (idx < 0)
         terms.add(newTerm);
      else
         terms.set(idx, newTerm);
      return this;
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

   public void clear()
   {
      terms.clear();
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
      Term newTerm = new Term(parent, token);
      List deconstructed = deconstructed(new ArrayList(), terms);
      for (Object aTerm : deconstructed)
      {
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

   static List deconstructed(List found, Object... terms)
   {
      if (terms.length == 1 && terms[0].getClass().isArray())
         terms = (Object[]) terms[0];

      for (Object o : terms)
      {
         if (o instanceof Collection)
         {
            ((Collection) o).forEach(o2 -> deconstructed(found, o2));
         }
         else if (o.getClass().isArray())
         {
            Object[] arr = (Object[]) o;
            for (Object o2 : arr)
            {
               deconstructed(found, o2);
            }
         }
         else
         {
            found.add(o);
         }
      }
      return found;
   }

   /**
    * Returns true if <code>toFind</code> is in <code>values</code> 
    * ignoring case.
    * 
    * @param toFind
    * @param values
    * @return
    */
   public static boolean in(String toFind, String... values)
   {
      toFind = toFind.toLowerCase();
      for (String val : values)
      {
         if (toFind.equals(val.toLowerCase()))
            return true;
      }
      return false;
   }

   public void stream(Consumer action)
   {
      action.accept(token);
      for (Term child : terms)
         child.stream(action);
   }
}