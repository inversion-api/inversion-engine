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

import io.inversion.cloud.utils.Utils;

public class Property implements Comparable<Property>
{
   protected String     jsonName   = null;
   protected String     columnName = null;
   protected String     type       = "string";
   protected boolean    nullable   = false;

   protected String     hint       = null;

   protected boolean    exclude    = false;

   /**
    *  If this Property is a foreign key, this will be populated
    *  with the referenced primary key from the referred Collection
    */
   protected Property   pk         = null;

   protected Collection collection      = null;

   public Property()
   {

   }

   public Property(String name)
   {
      this(name, "string", true);
   }

   public Property(String name, String type)
   {
      this(name, type, true);
   }

   public Property(String name, String type, boolean nullable)
   {
      withColumnName(name);
      withJsonName(name);
      withType(type);
      withNullable(nullable);
   }

   @Override
   public int compareTo(Property o)
   {
      if (o == null)
         return 1;

      if (o.collection == collection)
      {
         return collection.indexOf(this) > collection.indexOf(o) ? 1 : -1;
      }

      return 0;
   }

   public boolean equals(Object object)
   {
      if (object == this)
         return true;

      if (object instanceof Property)
      {
         Property column = (Property) object;
         return ((collection == null || collection == column.collection) && Utils.equal(columnName, column.columnName));
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
   public Property getPk()
   {
      return pk;
   }

   /**
    * @param primaryKey the primaryKey to set
    */
   public Property withPk(Property pk)
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
   public String getColumnName()
   {
      return columnName;
   }

   /**
    * @param name the name to set
    */
   public Property withColumnName(String name)
   {
      this.columnName = name;
      return this;
   }

   /**
    * @return the name
    */
   public String getJsonName()
   {
      return jsonName;
   }

   /**
    * @param name the name to set
    */
   public Property withJsonName(String name)
   {
      this.jsonName = name;
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
   public Property withType(String type)
   {
      if (!Utils.empty(type) && !"null".equalsIgnoreCase(type))
         this.type = type;
      return this;
   }

   /**
    * @return the tbl
    */
   public Collection getCollection()
   {
      return collection;
   }

   /**
    * @param tbl the tbl to set
    */
   public Property withCollection(Collection collection)
   {
      if (this.collection != collection)
      {
         this.collection = collection;
         collection.withProperties(this);
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
   public Property withHint(String hint)
   {
      this.hint = hint;
      return this;
   }

   public boolean isNullable()
   {
      return nullable;
   }

   public Property withNullable(boolean nullable)
   {
      this.nullable = nullable;
      return this;
   }

   public boolean isExclude()
   {
      return exclude;
   }

   public Property withExclude(boolean exclude)
   {
      this.exclude = exclude;
      return this;
   }

}
