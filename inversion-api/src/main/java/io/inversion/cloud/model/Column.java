/*
 * Copyright (c) 2016-2019 Rocket Partners, LLC
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

import io.inversion.cloud.utils.Utils;

public class Column implements Comparable<Column>
{
   protected Table   table    = null;

   /**
    *  If this Col is a foreign key, this will be populated
    *  with the refrenced primary key from the referred table
    */
   protected Column  pk       = null;

   protected int     number   = 0;
   protected String  name     = null;
   protected String  type     = null;
   protected boolean nullable = false;
   protected boolean unique   = false;

   protected String  hint     = null;

   protected boolean exclude  = false;

   public Column()
   {

   }

   public Column(Table table, int number, String name, String type, boolean nullable)
   {
      super();
      this.table = table;
      this.number = number;
      this.name = name;
      this.type = type;
      this.nullable = nullable;
      this.hint = table.getName() + "." + name;
   }

   @Override
   public int compareTo(Column o)
   {
      if (o == null)
         return 1;

      if (o.table == table)
         return this.number > o.number ? 1 : -1;

      return 0;
   }

   public boolean equals(Object object)
   {
      if (object instanceof Column)
      {
         Column column = (Column) object;
         if ((table != null && table.equals(column.table)) && Utils.equal(name, column.name))
            return true;
      }
      return false;
   }

   public String toString()
   {
      return hint == null ? super.toString() : hint;
   }

   /**
    * @return the primaryKey
    */
   public Column getPk()
   {
      return pk;
   }

   /**
    * @param primaryKey the primaryKey to set
    */
   public Column withPk(Column pk)
   {
      this.pk = pk;
      return this;
   }

   public boolean isFk()
   {
      return pk != null;
   }

   /**
    * @return the name
    */
   public String getName()
   {
      return name;
   }

   /**
    * @param name the name to set
    */
   public Column withName(String name)
   {
      this.name = name;
      return this;
   }

   /**
    * @return the type
    */
   public String getType()
   {
      return type;
   }

   /**
    * @param type the type to set
    */
   public Column withType(String type)
   {
      this.type = type;
      return this;
   }

   /**
    * @return the tbl
    */
   public Table getTable()
   {
      return table;
   }

   /**
    * @param tbl the tbl to set
    */
   public Column withTable(Table table)
   {
      if (this.table != table)
      {
         this.table = table;
         table.withColumn(this);
      }
      return this;
   }

   /**
    * @return the hint
    */
   public String getHint()
   {
      return hint;
   }

   /**
    * @param hint the hint to set
    */
   public Column withHint(String hint)
   {
      this.hint = hint;
      return this;
   }

   /**
    * @return the unique
    */
   public boolean isUnique()
   {
      return unique;
   }

   /**
    * @param unique the unique to set
    */
   public Column withUnique(boolean unique)
   {
      this.unique = unique;
      return this;
   }

   public boolean isNullable()
   {
      return nullable;
   }

   public Column withNullable(boolean nullable)
   {
      this.nullable = nullable;
      return this;
   }

   public boolean isExclude()
   {
      return exclude;
   }

   public Column withExclude(boolean exclude)
   {
      this.exclude = exclude;
      return this;
   }

}
