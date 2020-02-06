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
package io.inversion.cloud.model;

import java.util.ArrayList;
import java.util.List;

import io.inversion.cloud.utils.Utils;

/**
 * @author tc-rocket
 *
 */
public class Index
{
   protected Table        table       = null;
   protected String       name        = null;
   protected String       type        = null;           // primary, partition, sort, localsecondary, etc
   protected boolean      unique      = true;
   protected List<String> columnNames = new ArrayList();

   public Index()
   {
      super();
   }

   public Index(String name, String type, boolean unique, String columnName1, String... columnNameN)
   {
      withName(name);
      withType(type);
      withUnique(unique);
      withColumnNames(columnName1);
      withColumnNames(columnNameN);
   }

   public boolean equals(Object object)
   {
      if (object == this)
         return true;

      if (object instanceof Index)
      {
         Index index = ((Index) object);
         return ((table == null || table == index.table) && Utils.equal(name, index.name));
      }
      return false;
   }

   public String toString()
   {
      //StringBuffer buff = new StringBuffer("Index: ").append(table.getName()).append(".").append(name).append(" ").append(type).append(" ").append(unique).append("(");
      StringBuffer buff = new StringBuffer(getTable().getName()).append(".").append(name).append("(");
      for (int i = 0; i < columnNames.size(); i++)
      {
         buff.append(columnNames.get(i));
         if (i < columnNames.size() - 1)
            buff.append(", ");
      }
      buff.append(")");
      return buff.toString();
   }

   public boolean isExclude()
   {
      if (table.isExclude())
         return true;

      for (String c : columnNames)
         if (table.getColumn(c).isExclude())
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
         table.withIndexes(this);
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
      for (String col : columnNames)
      {
         if (col.equalsIgnoreCase(name))
            return true;
      }
      return false;
   }

   public Column getColumn(int idx)
   {
      return table.getColumn(columnNames.get(idx));
   }

   //   public List<Column> getColumns()
   //   {
   //      return new ArrayList(columns);
   //   }

   public int size()
   {
      return columnNames.size();
   }

   public Index withColumnNames(String... columnNames)
   {
      for (int i = 0; columnNames != null && i < columnNames.length; i++)
      {
         String columnName = columnNames[i];
         if (!Utils.empty(columnName) && !this.columnNames.contains(columnName))
         {
            this.columnNames.add(columnName);
         }
      }
      
      return this;
   }

   public List<String> getColumnNames()
   {
      return new ArrayList(columnNames);
   }

   public void removeColumn(Column column)
   {
      columnNames.remove(column);
   }
}
