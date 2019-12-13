/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Builder<T, P extends Builder>
{
   protected Parser        parser    = null;
   protected P             parent    = null;
   protected List<Builder> builders  = new ArrayList();
   protected List<Term>    terms     = new ArrayList();
   protected T             r         = null;

   /**
    * Term tokens this builder is willing to accept
    */
   protected Set<String>   functions = new HashSet();

   public Builder(P parent)
   {
      withParent(parent);
   }

   /**
    * OVERRIDE ME TO ADD CUSTOM FUNCTIONALITY TO YOUR FLUENT API
    * @param term
    * @return
    */
   protected boolean addTerm(String token, Term term)
   {
      for (Builder builder : builders)
      {
         if (builder.addTerm(token, term))
            return true;
      }

      if (functions.contains(token))
      {
         terms.add(term);
         return true;
      }

      return false;
   }

   protected T r()
   {
      if (r != null)
         return r;
      else
         return (T) this;
   }

   public T withParser(Parser parser)
   {
      this.parser = parser;
      return r();
   }

   public Parser getParser()
   {
      if (parser != null)
         return parser;

      Builder root = getRoot();
      if (root != null)
         return root.getParser();

      return null;
   }

   public Builder getRoot()
   {
      Builder root = this;
      while (root.getParent() != null)
         root = root.getParent();

      return root;
   }

   public P getParent()
   {
      return parent;
   }

   public T withParent(P parent)
   {
      if (this.parent != parent)
      {
         if (this.parent != null)
         {
            this.parent.removeBuilder(this);
         }

         this.parent = parent;

         if (this.parent != null)
         {
            this.parent.withBuilder(this);
         }
      }

      return r();
   }

   public T withBuilder(Builder builder)
   {
      if (!builders.contains(builder))
      {
         builders.add(builder);
         builder.withParent(this);
      }
      return r();
   }

   public T removeBuilder(Builder builder)
   {
      builders.remove(builder);
      return r();
   }

   public T withFunctions(Collection<String> tokens)
   {
      for (String token : tokens)
      {
         this.functions.add(token.trim().toLowerCase());
      }
      return r();
   }

   public T withFunctions(String... tokens)
   {
      for (String token : tokens)
      {
         this.functions.add(token.trim().toLowerCase());
      }

      return r();
   }

   public boolean isFunction(String token)
   {
      token = token.toLowerCase();
      if (functions.contains(token))
         return true;

      for (Builder builder : builders)
      {
         if (builder.isFunction(token))
            return true;
      }
      return false;
   }

   public T clearFunctions()
   {
      this.functions.clear();
      return r();
   }

   public T withTerm(String token, Object... terms)
   {
      withTerm(Term.term(null, token, terms));
      return r();
   }

   /**
    * OVERRIDE ME TO ADD CUSTOM FUNCTIONALITY TO YOUR FLUENT API
    * @param term
    * @return
    */
   public T withTerm(Term term)
   {
      if (terms.contains(term))
         return r();

      if (term.isQuoted())
         return r();

      //      add in support for special cases here like offset limit etc
      //      make parse() call this repeatetly..didn't I write that code already. look for Utils.explode

      String token = term.getToken().toLowerCase();
      if ("eq".equals(token))
      {
         //FIX ME this has to happen in two separate recursions.....otherwise where will grab the eq for limit=5 etc. 

         //this param came in in function=arg format not function(col,arg) format
         Term child = term.getTerm(0);
         if (child != null && !child.isQuoted() && child.isLeaf() && isFunction(child.getToken()))
         {
            String childToken = child.getToken().toLowerCase();

            if (!"eq".equals(childToken))
            {
               term.withToken(childToken);
               term.removeTerm(child);

               if (addTerm(childToken, term))
                  return r();

               //OK, this was not an inverted "eq" term so put things back the way they started
               term.withToken(token);
               term.withTerm(0, child);
            }
         }
      }

      addTerm(token, term);

      return r();
   }

   public List<Term> getTerms()
   {
      return new ArrayList(terms);
   }

   public final T withTerms(Object... rqlTerms)
   {
      for (Object term : rqlTerms)
      {
         if (term instanceof Term)
         {
            withTerm((Term) term);
         }
         else if (term instanceof Collection)
         {
            for (Object t : ((Collection) term))
            {
               withTerms(t);
            }
         }
         else if (term instanceof Map)
         {
            Map<String, Object> map = (Map) term;

            for (String key : map.keySet())
            {
               if (empty(key))
                  continue;

               String value = (String) map.get(key);

               if (empty(value) && key.indexOf("(") > -1)
               {
                  term = key;
               }
               else
               {
                  term = "eq(" + key + "," + value + ")";
               }
               withTerm((String) term);
            }
         }
         else
         {
            withTerm(term.toString());
         }
      }
      return r();
   }

   public final T withTerm(String rql)
   {
      List<Term> terms = parse(rql);
      for (Term term : terms)
         withTerm(term);

      return r();
   }

   protected List<Term> parse(Object... rqlTerms)
   {
      List<Term> terms = new ArrayList();

      for (Object term : rqlTerms)
      {
         if (empty(term))
         {
            continue;
         }
         else if (term instanceof Term)
         {
            terms.add((Term) term);
         }
         else
         {
            String[] parts = term.toString().split("\\&");
            for (int i = 0; i < parts.length; i++)
            {
               if (parts[i] == null || parts[i].length() == 0)
                  continue;

               Term parsed = getParser().parse(parts[i]);
               terms.add(parsed);
            }
         }
      }

      return terms;
   }

   public int findInt(String token, int childToken, int defaultValue)
   {
      Object found = find(token, childToken);
      if (found != null)
         return Integer.parseInt(found.toString());

      return defaultValue;
   }

   public Object find(String token, int childToken)
   {
      Term term = find(token);
      if (term != null)
      {
         term = term.getTerm(childToken);
         if (term != null)
            return term.getToken();
      }

      return null;
   }

   public List<Term> findAll(String token)
   {
      return findAll(token, new ArrayList());
   }

   List<Term> findAll(String token, List<Term> found)
   {
      for (Term term : terms)
      {
         if (term.hasToken(token))
            found.add(term);
      }

      for (Builder builder : builders)
      {
         builder.findAll(token, found);
      }
      return found;
   }

   public Term find(String token)
   {
      for (Term term : terms)
      {
         if (term.hasToken(token))
            return term;
      }

      for (Builder builder : builders)
      {
         Term term = builder.find(token);
         if (term != null)
            return term;
      }
      return null;
   }

   public Term findTerm(String childToken, String... parentFunctions)
   {
      if (childToken == null)
         return null;

      for (Term term : getTerms())
      {
         if (term.hasToken(parentFunctions))
         {
            for (Term child : term.getTerms())
            {
               if (child.hasToken(childToken) && child.isLeaf())
                  return term;
            }
         }
      }

      for (Builder builder : builders)
      {
         Term t = builder.findTerm(childToken, parentFunctions);
         if (t != null)
            return t;
      }

      return null;
   }

   public String toString()
   {
      return toString(terms);
   }

   protected String toString(List<Term> terms)
   {
      StringBuffer buff = new StringBuffer("");

      for (int i = 0; i < terms.size(); i++)
      {
         buff.append(terms.get(i));
         if (i < terms.size() - 1)
            buff.append("&");
      }

      for (Builder builder : builders)
      {
         String rql = builder.toString();
         if (!empty(rql))
         {
            if (buff.length() > 0)
               buff.append("&");
            buff.append(rql);
         }
      }

      return buff.toString();
   }

   protected boolean empty(Object... arr)
   {
      boolean empty = true;
      for (int i = 0; empty && arr != null && i < arr.length; i++)
      {
         Object obj = arr[i];
         if (obj != null && obj.toString().length() > 0)
            empty = false;
      }
      return empty;
   }

}
