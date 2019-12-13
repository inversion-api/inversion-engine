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