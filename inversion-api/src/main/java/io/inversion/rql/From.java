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
package io.inversion.rql;

public class From<T extends From, P extends Query> extends Builder<T, P>
{
   public From(P query)
   {
      super(query);
      withFunctions("_table", "_subquery", "_alias");
   }

   public String getSubquery()
   {
      return (String) find("_subquery", 0);
   }

   public String getTable()
   {
      String tableName = (String) find("_table", 0);

      if (tableName == null && getParent().getCollection() != null)
      {
         tableName = getParent().getCollection().getTableName();
      }

      if (tableName == null)
         tableName = "";

      return tableName;
   }

   public String getAlias()
   {
      String alias = (String) find("_alias", 0);

      if (alias == null)
      {
         alias = (String) find("_table", 1);
      }

      if (alias == null)
      {
         alias = (String) find("_subquery", 1);
      }

      if (alias == null)
         alias = getTable();

      return alias;
   }

}
