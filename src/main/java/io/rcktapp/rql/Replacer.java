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

public class Replacer
{
   public Rql          rql  = null;
   public List<String> cols = new ArrayList();
   public List<String> vals = new ArrayList();

   public Replacer(Rql rql)
   {
      this.rql = rql;
   }

   public String replace(Predicate p, String col, String val) throws Exception
   {
      if (val == null || val.trim().equalsIgnoreCase("null"))
         return "NULL";

      if (rql.isReserved(p.token.toLowerCase()))
         return val;

      if (val.indexOf('*') > 0)
         val = val.replace('*', '%');

      if (col.startsWith("\"") || col.startsWith("`"))
      {
         cols.add(Parser.dequote(col, '\"', '`'));
         vals.add(val);
         return "?";
      }

      return val;
   }

}