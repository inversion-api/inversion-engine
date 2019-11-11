/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
 * https://github.com/inversion-api
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
