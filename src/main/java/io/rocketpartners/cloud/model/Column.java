/*
 * Copyright (c) 2016-2019 Rocket Partners, LLC
 * http://rocketpartners.io
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
package io.rocketpartners.cloud.model;

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

   String            hint     = null;

   boolean           exclude  = false;

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
   public void setPk(Column pk)
   {
      this.pk = pk;
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
   public void setName(String name)
   {
      this.name = name;
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
   public void setType(String type)
   {
      this.type = type;
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
   public void setTable(Table tbl)
   {
      this.table = tbl;
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
   public void setHint(String hint)
   {
      this.hint = hint;
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
   public void setUnique(boolean unique)
   {
      this.unique = unique;
   }

   public boolean isNullable()
   {
      return nullable;
   }

   public void setNullable(boolean nullable)
   {
      this.nullable = nullable;
   }

   public boolean isExclude()
   {
      return exclude;
   }

   public void setExclude(boolean exclude)
   {
      this.exclude = exclude;
   }

}
