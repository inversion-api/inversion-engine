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

public class Order<T extends Order, P extends Query> extends Builder<T, P>
{

   public Order(P query)
   {
      super(query);
      withTokens("order", "sort");
   }

   /**
    * Returns true if the first sort is ascending or if there are no sorts.
    * @return
    */
   public boolean isAsc(int index)
   {
      List<Sort> sorts = getSorts();
      return sorts.size() <= index ? true : sorts.get(index).isAsc();
   }

   public String getProperty(int index)
   {
      List<Sort> sorts = getSorts();
      return sorts.size() <= index ? null : sorts.get(index).getProperty();
   }

   public List<Sort> getSorts()
   {
      List<Sort> sorts = new ArrayList();
      for (Term term : getTerms())
      {
         if (term.hasToken("sort", "order"))
         {
            for (Term child : term.getTerms())
            {
               String property = child.token;
               boolean asc = true;
               if (property.startsWith("-"))
               {
                  asc = false;
                  property = property.substring(1, property.length());
               }
               else if (property.startsWith("+"))
               {
                  property = property.substring(1, property.length());
               }
               sorts.add(new Sort(property, asc));
            }
         }
      }
      return sorts;
   }

   public T order(String... properties)
   {
      withTerm("order", (Object[]) properties);
      return r();
   }

   public T order(String property, boolean asc)
   {
      if (!asc && !property.startsWith("-"))
      {
         property = "-" + property;
      }
      return order(property);
   }

   public static class Sort
   {
      String  property = null;
      boolean asc      = true;

      public Sort(String property, boolean asc)
      {
         super();
         this.property = property;
         this.asc = asc;
      }

      public String getProperty()
      {
         return property;
      }

      public void setProperty(String property)
      {
         this.property = property;
      }

      public boolean isAsc()
      {
         return asc;
      }

      public void setAsc(boolean asc)
      {
         this.asc = asc;
      }
   }
}
