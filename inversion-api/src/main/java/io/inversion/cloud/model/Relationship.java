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

import java.io.Serializable;

public class Relationship implements Serializable
{
   public static final String REL_MANY_TO_ONE  = "MANY_TO_ONE";
   public static final String REL_ONE_TO_MANY  = "ONE_TO_MANY";
   public static final String REL_MANY_TO_MANY = "MANY_TO_MANY";

   protected String           name             = null;
   protected String           type             = null;
   protected Index            fkIndex1         = null;
   protected Index            fkIndex2         = null;

   protected Collection       collection       = null;
   protected Collection       related          = null;

   protected boolean          exclude          = false;

   public Relationship()
   {

   }

   public Relationship(String name, String type, Collection collection, Collection related, Index fkIndex1, Index fkIndex2)
   {
      withName(name);
      withType(type);
      withCollection(collection);
      withRelated(related);
      withFkIndex1(fkIndex1);
      withFkIndex2(fkIndex2);
   }

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
    * @return the collection
    */
   public Collection getCollection()
   {
      return collection;
   }

   public Relationship withCollection(Collection collection)
   {
      if (this.collection != collection)
      {
         this.collection = collection;
         if (collection != null)
            collection.withRelationship(this);
      }
      return this;
   }

   /**
    * @return the related
    */
   public Collection getRelated()
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
            if (isManyToOne() && !other.isOneToMany())
               continue;

            if (isManyToMany() && !other.isManyToOne())
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
   public Relationship withRelated(Collection related)
   {
      this.related = related;
      return this;
   }

   public boolean isManyToMany()
   {
      return REL_MANY_TO_MANY.equalsIgnoreCase(type);
   }

   public boolean isOneToMany()
   {
      return REL_ONE_TO_MANY.equalsIgnoreCase(type);
   }

   public boolean isManyToOne()
   {
      return REL_MANY_TO_ONE.equalsIgnoreCase(type);
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
      if(name.equals("value1"))
         System.out.println("asdfas");
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
         String str = "Relationship: " + collection + "." + getName() + ":" + getType() + " ";
         if (isManyToOne())
         {
            str += collection.getPrimaryIndex() + " -> " + getFkIndex1();
         }
         if (isOneToMany())
         {
            str += collection.getPrimaryIndex() + " <- " + getFkIndex1();
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

   public Collection getPrimaryKeyTable1()
   {
      return fkIndex1.getProperty(0).getCollection();
   }

   public Collection getPrimaryKeyTable2()
   {
      return fkIndex2.getProperty(0).getCollection();
   }

   /**
    * @return the fkCol1
    */
   public Property getFk1Col1()
   {
      return fkIndex1.getProperty(0);
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
   public Property getFk2Col1()
   {
      return fkIndex2.getProperty(0);
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
