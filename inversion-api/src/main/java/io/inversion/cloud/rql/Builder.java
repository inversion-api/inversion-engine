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
package io.inversion.cloud.rql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.inversion.cloud.utils.Utils;

public class Builder<T, P extends Builder>
{
   protected Parser        parser    = null;
   protected P             parent    = null;
   protected List<Builder> builders  = null;
   protected List<Term>    terms     = new ArrayList();
   protected T             r         = null;

   /**
    * Term tokens this builder is willing to accept
    */
   protected Set<String>   functions = new HashSet();

   public Builder()
   {

   }

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
      for (Builder builder : getBuilders())
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

   public List<Builder> getBuilders()
   {
      if (builders == null)
      {
         builders = new ArrayList();
      }
      return builders;
   }

   public T withBuilder(Builder builder)
   {
      if (!getBuilders().contains(builder))
      {
         getBuilders().add(builder);
         builder.withParent(this);
      }
      return r();
   }

   public T removeBuilder(Builder builder)
   {
      getBuilders().remove(builder);
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
      for (int i = 0; tokens != null && i < tokens.length; i++)
      {
         String token = tokens[i];
         if (!Utils.empty(token))
         {
            this.functions.add(token.trim().toLowerCase());
         }
      }

      return r();
   }

   public boolean isFunction(String token)
   {
      token = token.toLowerCase();
      if (functions.contains(token))
         return true;

      for (Builder builder : getBuilders())
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

      String token = term.getToken().toLowerCase();
      if ("eq".equals(token))
      {
         //-- single arg functions such as limit(x) or page(y) can be written as limit=x and page=y 
         //-- which will parse here as eq(limit,x) or eq(page,y).  If the terms first child is a leaf
         //-- with a token that is a fucntion name of a child builder, then this logic reorders eq 
         //-- functions and attempts to add them before attempting to add the eq function that was
         //-- passed in.

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

               //-- OK, this was not an inverted "eq" term so put things back the way they started
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

      for (Builder builder : getBuilders())
      {
         builder.findAll(token, found);
      }
      return found;
   }

   public Term find(String... tokens)
   {
      for (Term term : terms)
      {
         Term found = find(term, tokens);
         if (found != null)
            return found;
      }

      for (Builder builder : getBuilders())
      {
         Term term = builder.find(tokens);
         if (term != null)
            return term;
      }
      return null;
   }

   Term find(Term term, String... tokens)
   {
      if (term.hasToken(tokens))
         return term;

      for (Term child : term.getTerms())
      {
         if (child.hasToken(tokens))
            return child;
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

      for (Builder builder : getBuilders())
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

      for (Builder builder : getBuilders())
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
