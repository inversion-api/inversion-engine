/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
 * http://rocketpartners.io
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
package io.rcktapp.api;

public class Relationship extends Dto
{
   public static final String REL_MANY_TO_MANY = "MANY_TO_MANY";
   public static final String REL_ONE_TO_MANY  = "ONE_TO_MANY";
   public static final String REL_MANY_TO_ONE  = "MANY_TO_ONE";

   Entity                     entity           = null;
   Entity                     related          = null;
   String                     name             = null;
   String                     hint             = null;
   String                     type             = null;

   Column                     fkCol1           = null;
   Column                     fkCol2           = null;

   boolean                    exclude          = false;

   public boolean isExcluded()
   {
      if (exclude)
         return true;

      if (entity != null && entity.isExclude())
         return true;

      if (related != null && related.isExclude())
         return true;

      if (fkCol1 != null && fkCol1.isExclude())
         return true;

      if (fkCol2 != null && fkCol2.isExclude())
         return true;

      return exclude;
   }

   public boolean isExclude()
   {
      return exclude;
   }

   public void setExclude(boolean exclude)
   {
      this.exclude = exclude;
   }

   public boolean isManyToMany()
   {
      return REL_MANY_TO_MANY.equalsIgnoreCase(type);
   }

   public boolean isManyToOne()
   {
      return REL_MANY_TO_ONE.equalsIgnoreCase(type);
   }

   public boolean isOneToMany()
   {
      return REL_ONE_TO_MANY.equalsIgnoreCase(type);
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
    * @return the hint
    */
   public String getHint()
   {
      return hint;
   }

   public String toString()
   {
      return getName() + " : " + getHint();
   }

   /**
    * @param hint the hint to set
    */
   public void setHint(String hint)
   {
      this.hint = hint;
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
    * @return the fkCol1
    */
   public Column getFkCol1()
   {
      return fkCol1;
   }

   /**
    * @param fkCol1 the fkCol1 to set
    */
   public void setFkCol1(Column fkCol1)
   {
      this.fkCol1 = fkCol1;
   }

   /**
    * @return the fkCol2
    */
   public Column getFkCol2()
   {
      return fkCol2;
   }

   /**
    * @param fkCol2 the fkCol2 to set
    */
   public void setFkCol2(Column fkCol2)
   {
      this.fkCol2 = fkCol2;
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
   public void setEntity(Entity entity)
   {
      this.entity = entity;
   }

   /**
    * @return the related
    */
   public Entity getRelated()
   {
      return related;
   }

   /**
    * @param related the related to set
    */
   public void setRelated(Entity related)
   {
      this.related = related;
   }

}
