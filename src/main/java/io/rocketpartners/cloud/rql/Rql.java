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

import java.util.HashMap;

import io.rocketpartners.cloud.model.Table;

public abstract class Rql<T extends Table, Q extends Query>
{
   static HashMap<String, Rql> RQLS = new HashMap();

   public static void addRql(Rql rql)
   {
      RQLS.put(rql.getType().toLowerCase(), rql);
   }

   public static Rql getRql(String type)
   {
      return RQLS.get(type.toLowerCase());
   }

   protected String type = null;

   protected Rql(String type)
   {
      this.type = type;
   }

   abstract protected Q buildQuery(T table, Object queryParams);

   public String getType()
   {
      return type;
   }

   //   public B build(String rql) throws Exception
   //   {
   //      B builder = build();
   //
   //      String[] terms = rql.split("\\&");
   //      for (int i = 0; i < terms.length; i++)
   //      {
   //         builder.withTerm(terms[i]);
   //      }
   //
   //      return builder;
   //   }
   //
   //   public B build(Map<String, String> queryParams) throws Exception
   //   {
   //      B builder = build();
   //      for (String term : queryParams.keySet())
   //      {
   //         String value = queryParams.get(term);
   //         if (!J.empty(term))
   //         {
   //            term = term + "=" + value;
   //         }
   //         builder.withTerm(term);
   //      }
   //      return builder;
   //
   //   }

}
