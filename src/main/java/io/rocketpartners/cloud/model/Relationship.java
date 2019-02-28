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

public class Relationship
{
   public static final String REL_MANY_TO_MANY = "MANY_TO_MANY";
   public static final String REL_ONE_TO_MANY  = "ONE_TO_MANY";
   public static final String REL_MANY_TO_ONE  = "MANY_TO_ONE";

   protected String           name             = null;
   protected String           hint             = null;
   protected String           type             = null;

   protected Column           fkCol1           = null;
   protected Column           fkCol2           = null;

   protected Entity           entity           = null;
   protected Entity           related          = null;

   protected boolean          exclude          = false;

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

   public Relationship withExclude(boolean exclude)
   {
      this.exclude = exclude;
      return this;
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
   public Relationship withEntity(Entity entity)
   {
      this.entity = entity;
      return this;
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
   public Relationship withRelated(Entity related)
   {
      this.related = related;
      return this;
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
   public Relationship withName(String name)
   {
      if("cUSTOMERDEMOGRAPHICses".equals(name))
         System.out.println(name);
      this.name = name;
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
   public Relationship withHint(String hint)
   {
      this.hint = hint;
      return this;
   }

   public String toString()
   {
      return getName() + " : " + getHint();
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
   public Relationship withType(String type)
   {
      this.type = type;
      return this;
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
   public Relationship withFkCol1(Column fkCol1)
   {
      this.fkCol1 = fkCol1;
      return this;
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
   public Relationship withFkCol2(Column fkCol2)
   {
      this.fkCol2 = fkCol2;
      return this;
   }

}
