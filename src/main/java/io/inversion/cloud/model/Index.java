/*
 * Copyright (c) 2016-2019 Inversion.org, LLC
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
package io.inversion.cloud.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author tc-rocket
 *
 */
public class Index
{
   protected Table        table   = null;
   protected String       name    = null;
   protected String       type    = null;           // primary, partition, sort, localsecondary, etc
   protected boolean      unique  = true;
   protected List<Column> columns = new ArrayList();

   public Index()
   {
      super();
   }

   public Index(Table table, String name, String type)
   {
      this(table, null, name, type, true);
   }

   public Index(Table table, Column column, String name, String type, boolean unique)
   {
      super();
      withTable(table);
      withColumn(column);
      withName(name);
      withType(type);
      withUnique(unique);
   }

   public String toString()
   {
      //StringBuffer buff = new StringBuffer("Index: ").append(table.getName()).append(".").append(name).append(" ").append(type).append(" ").append(unique).append("(");
      StringBuffer buff = new StringBuffer(getTable().getName()).append(".").append(name).append("(");
      for (int i = 0; i < columns.size(); i++)
      {
         buff.append(columns.get(i).getName());
         if (i < columns.size() - 1)
            buff.append(", ");
      }
      buff.append(")");
      return buff.toString();
   }

   public boolean isExclude()
   {
      if (table.isExclude())
         return true;

      for (Column c : columns)
         if (c.isExclude())
            return true;

      return false;
   }

   public Table getTable()
   {
      return table;
   }

   public Index withTable(Table table)
   {
      if (this.table != table)
      {
         this.table = table;
         table.withIndex(this);
      }
      return this;
   }

   public String getName()
   {
      return name;
   }

   public Index withName(String name)
   {
      this.name = name;
      return this;
   }

   public String getType()
   {
      return type;
   }

   public Index withType(String type)
   {
      this.type = type;
      return this;
   }

   public boolean isType(String... types)
   {
      for (String type : types)
      {
         if (type.equalsIgnoreCase(this.type))
            return true;
      }
      return false;
   }

   public boolean isUnique()
   {
      return unique;
   }

   public Index withUnique(boolean unique)
   {
      this.unique = unique;
      return this;
   }

   public boolean hasColumn(String name)
   {
      for (Column col : columns)
      {
         if (col.getName().equalsIgnoreCase(name))
            return true;
      }
      return false;
   }

   public Column getColumn(int idx)
   {
      return columns.get(idx);
   }

   public List<Column> getColumns()
   {
      return new ArrayList(columns);
   }

   public Index withColumns(List<Column> columns)
   {
      for (Column column : columns)
      {
         withColumn(column);
      }
      return this;
   }

   public Index withColumn(Column column)
   {
      if (column != null && !columns.contains(column))
      {
         columns.add(column);
      }

      return this;
   }

   public void removeColumn(Column column)
   {
      columns.remove(column);
   }
}
