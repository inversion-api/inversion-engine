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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import io.inversion.cloud.model.Table;

public class Stmt
{

   Rql                               rql          = null;

   public Parts                      parts        = null;
   public Replacer                   replacer     = null;
   public Table                      table        = null;

   public boolean                    restrictCols = false;

   //url arg order is undependable so order does not matter here either
   public HashMap<String, Predicate> cols         = new LinkedHashMap();

   public boolean                    distinct     = false;

   public List<Predicate>            where        = new ArrayList();
   public List<String>               groups       = new ArrayList();
   public List<Order>                order        = new ArrayList();

   public int                        pagenum      = -1;
   public int                        pagesize     = -1;

   public int                        limit        = -1;
   public int                        offset       = -1;

   public int                        maxRows      = 100;

   public String                     rowcount     = null;

   public Stmt(Rql rql, Parts parts, Replacer replacer, Table table)
   {
      this.rql = rql;

      this.parts = parts;
      this.replacer = replacer;
      this.table = table;
   }

   void addGroupBy(String col)
   {
      if (!groups.contains(col))
         groups.add(col);
   }

   void addCol(String name, Predicate predicate)
   {
      if (name == null)
         name = "$$$ANON" + UUID.randomUUID().toString();

      name = Parser.dequote(name);

      cols.put(name, predicate);
   }

   /**
    * The select clause will include at least the columns supplied 
    * in the order in which they are supplied here.  Functions such
    * as 'as()' can add addition columns to this list.  Such a column
    * if not included in the arg here would come after all of the 
    * cols supplied here in the select statement.
    * 
    * Also causes a SELECT * to be replace with the specific column
    * names, removeing the *.  Simply adding columns via "as(" would
    * add to the * not replace it.
    * 
    * @param ordered
    */
   void setCols(List<String> ordered)
   {
      restrictCols = true;

      LinkedHashMap<String, Predicate> newCols = new LinkedHashMap();
      for (String c : ordered)
      {
         c = Parser.dequote(c);
         newCols.put(c, null);
      }

      for (String c : cols.keySet())
      {
         c = Parser.dequote(c);
         newCols.put(c, cols.get(c));
      }

      cols = newCols;
   }

   public int getMaxRows()
   {
      return maxRows;
   }

   public void setMaxRows(int maxRows)
   {
      this.maxRows = maxRows;
   }

}