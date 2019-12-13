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
package io.inversion.cloud.action.elastic.v03x.rql;

import java.util.ArrayList;
import java.util.List;

public class Predicate
{
   public Predicate       parent = null;
   public String          src    = null;

   public String          token  = null;
   public List<Predicate> terms  = new ArrayList();

   Predicate()
   {

   }

   Predicate(String token)
   {
      this.token = token;
   }

   Predicate(String token, Predicate... terms)
   {
      this.token = token;
      for (Predicate term : terms)
      {
         addTerm(term);
      }
   }

   public String getSrc()
   {
      return src;
   }

   public void setSrc(String src)
   {
      this.src = src;
   }

   public List<Predicate> getTerms()
   {
      return terms;
   }

   public void setTerms(List<Predicate> terms)
   {
      this.terms = terms;
   }

   void addTerm(Predicate p)
   {
      terms.add(p);
      p.setParent(this);
   }

   public Predicate term(int index)
   {
      return terms.get(index);
   }

   public Predicate getParent()
   {
      return parent;
   }

   public void setParent(Predicate parent)
   {
      this.parent = parent;
   }

   public String getToken()
   {
      return token;
   }

   public void setToken(String token)
   {
      this.token = token;
   }

   public String toString()
   {
      String token = this.token + "";
      StringBuffer buff = new StringBuffer(token);
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
}