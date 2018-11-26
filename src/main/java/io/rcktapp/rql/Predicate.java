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
package io.rcktapp.rql;

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