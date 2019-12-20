/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
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
package io.inversion.cloud.model;

public class Attribute
{
   protected Entity  entity  = null;
   protected Column  column  = null;

   protected String  name    = null;
   protected String  type    = null;
   protected String  hint    = null;

   protected boolean exclude = false;

   public Attribute()
   {

   }

   public Attribute(Entity entity, Column column)
   {
      withEntity(entity);
      withColumn(column);
   }

   public boolean isExclude()
   {
      return exclude || column.isExclude();
   }

   public Attribute withExclude(boolean exclude)
   {
      this.exclude = exclude;
      return this;
   }

   public String toString()
   {
      return getName();
   }

   /**
    * @return the entity
    */
   public Entity getEntity()
   {
      return entity;
   }

   /**
    * @param entity the entity to set
    */
   public Attribute withEntity(Entity entity)
   {
      this.entity = entity;
      return this;
   }

   /**
    * @return the col
    */
   public Column getColumn()
   {
      return column;
   }

   /**
    * @param col the col to set
    */
   public Attribute withColumn(Column col)
   {
      this.column = col;
      if (name == null)
      {
         withName(col.getName());
      }
      withType(col.getType());

      return this;
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
   public Attribute withName(String name)
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
   public Attribute withType(String type)
   {
      this.type = type;
      return this;
   }

   /**
    * @return the hint
    */
   public String getHint()
   {
      return hint != null ? hint : (column != null ? column.getTable().getName() + "." + column.getName() : name);
   }

   /**
    * @param hint the hint to set
    */
   public Attribute withHint(String hint)
   {
      this.hint = hint;
      return this;
   }

}
