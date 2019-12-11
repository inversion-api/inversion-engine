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

public class Relationship
{
   public static final String REL_MANY_TO_MANY = "MANY_TO_MANY";
   public static final String REL_ONE_TO_MANY  = "ONE_TO_MANY";
   public static final String REL_MANY_TO_ONE  = "MANY_TO_ONE";

   protected String           name             = null;
   protected String           type             = null;
   protected Index            fkIndex1         = null;
   protected Index            fkIndex2         = null;

   protected Entity           entity           = null;
   protected Entity           related          = null;

   protected boolean          exclude          = false;

   public boolean isExclude()
   {
      return exclude || (fkIndex1 != null && fkIndex1.isExclude()) || (fkIndex2 != null && fkIndex2.isExclude());
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
      if (this.entity != entity)
      {
         this.entity = entity;
         if (entity != null)
            entity.withRelationship(this);
      }
      return this;
   }

   /**
    * @return the related
    */
   public Entity getRelated()
   {
      return related;
   }

   public Relationship getInverse()
   {
      if (isManyToMany())
      {
         for (Relationship other : related.getRelationships())
         {
            if (!other.isManyToMany())
               continue;

            if (getFkIndex1().equals(other.getFkIndex2()))
            {
               return other;
            }
         }
      }
      else
      {
         for (Relationship other : related.getRelationships())
         {
            if (isOneToMany() && !other.isManyToOne())
               continue;

            if (isManyToMany() && !other.isOneToMany())
               continue;

            if (getFkIndex1().equals(other.getFkIndex1()) //
                  && getPrimaryKeyTable1().getPrimaryIndex().equals(other.getPrimaryKeyTable1().getPrimaryIndex()))
            {
               return other;
            }
         }
      }

      return null;
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
      this.name = name;
      return this;
   }

   public boolean equals(Object obj)
   {
      if (obj == null)
         return false;

      if (obj == this)
         return true;

      return toString().equals(obj.toString());
   }

   public int hashCode()
   {
      //return toString().hashCode();
      return super.hashCode();
   }

   public String toString()
   {
      try
      {
         String str = "Relationship: " + getEntity().getCollection() + "." + getName() + ":" + getType() + " ";
         if (isOneToMany())
         {
            str += getEntity().getTable().getPrimaryIndex() + " -> " + getFkIndex1();
         }
         if (isManyToOne())
         {
            str += getEntity().getTable().getPrimaryIndex() + " <- " + getFkIndex1();
         }
         else
         {
            str += getFkIndex1() + " <--> " + getFkIndex2();
         }

         return str;
      }
      catch (NullPointerException ex)
      {
         return "Relationship: " + name + "-" + type + "-" + fkIndex1 + "-" + fkIndex2;
      }
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

   public Index getFkIndex1()
   {
      return fkIndex1;
   }

   public Relationship withFkIndex1(Index fkIndex1)
   {
      this.fkIndex1 = fkIndex1;
      return this;
   }

   public Index getFkIndex2()
   {
      return fkIndex2;
   }

   public Relationship withFkIndex2(Index fkIndex2)
   {
      this.fkIndex2 = fkIndex2;
      return this;
   }

   public Table getPrimaryKeyTable1()
   {
      return fkIndex1.getColumns().get(0).getTable();
   }

   public Table getPrimaryKeyTable2()
   {
      return fkIndex2.getColumns().get(0).getTable();
   }

   /**
    * @return the fkCol1
    */
   public Column getFk1Col1()
   {
      return fkIndex1.getColumns().get(0);
   }

   //
   //   /**
   //    * @param fkCol1 the fkCol1 to set
   //    */
   //   public Relationship withFkCol1(Column fkCol1)
   //   {
   //      this.fkCol1 = fkCol1;
   //      return this;
   //   }
   //
   /**
    * @return the fkCol2
    */
   public Column getFk2Col1()
   {
      return fkIndex2.getColumns().get(0);
   }
   //
   //   /**
   //    * @param fkCol2 the fkCol2 to set
   //    */
   //   public Relationship withFkCol2(Column fkCol2)
   //   {
   //      this.fkCol2 = fkCol2;
   //      return this;
   //   }

}
