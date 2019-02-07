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

import java.util.ArrayList;
import java.util.List;

public class Entity extends Dto
{
   Table                   table         = null;
   Collection              collection    = null;
   ArrayList<Attribute>    keys          = new ArrayList();
   ArrayList<Attribute>    attributes    = new ArrayList();
   ArrayList<Relationship> relationships = new ArrayList();
   String                  hint          = null;

   boolean                 exclude       = false;

   public Entity()
   {

   }

   public Entity(Table table)
   {
      setTbl(table);
   }

   public boolean isExclude()
   {
      return exclude || table.isExclude();
   }

   public void setExclude(boolean exclude)
   {
      this.exclude = exclude;
   }

   public Attribute getAttribute(String name)
   {
      for (Attribute a : attributes)
      {
         if (a.getName().equalsIgnoreCase(name))
            return a;
      }

      return null;
   }

   public Relationship getRelationship(String name)
   {
      for (Relationship r : relationships)
      {
         if (r.getName().equalsIgnoreCase(name))
            return r;
      }
      return null;
   }

   /**
    * @return the tbl
    */
   public Table getTable()
   {
      return table;
   }

   /**
    * @return the collection
    */
   public Collection getCollection()
   {
      return collection;
   }

   /**
    * @param collection the collection to set
    */
   public void setCollection(Collection collection)
   {
      this.collection = collection;
   }

   /**
    * @param tbl the tbl to set
    */
   public void setTbl(Table tbl)
   {
      this.table = tbl;
   }

   /**
    * @return the attributes
    */
   public List<Attribute> getAttributes()
   {
      return new ArrayList(attributes);
   }

   /**
    * @param attributes the attributes to set
    */
   public void setAttributes(List<Attribute> attributes)
   {
      this.attributes.clear();
      for (Attribute attr : attributes)
         addAttribute(attr);
   }

   /**
    * @return the relationships
    */
   public List<Relationship> getRelationships()
   {
      return new ArrayList(relationships);
   }

   /**
    * @param relationships the relationships to set
    */
   public void setRelationships(List<Relationship> relationships)
   {
      this.relationships.clear();
      for (Relationship rel : relationships)
         addRelationship(rel);
   }

   public void addRelationship(Relationship relationship)
   {
      if (relationship != null && !relationships.contains(relationship))
         relationships.add(relationship);
   }

   public void removeRelationship(Relationship relationship)
   {
      relationships.remove(relationship);
   }

   public void addAttribute(Attribute attribute)
   {
      if (attribute != null && !attributes.contains(attribute))
         attributes.add(attribute);
   }

   public void removeAttribute(Attribute attribute)
   {
      attributes.remove(attribute);
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

   public ArrayList<Attribute> getKeys()
   {
      return keys;
   }

   public void setKeys(ArrayList<Attribute> keys)
   {
      this.keys = keys;
   }

   public void addKey(Attribute attribute)
   {
      if (attribute != null && !keys.contains(attribute))
         keys.add(attribute);
   }

   public void removeKey(Attribute attribute)
   {
      keys.remove(attribute);
   }

   /**
    * @return the key
    */
   public Attribute getKey()
   {
      if (!this.keys.isEmpty())
      {
         return this.keys.get(0);
      }
      return null;
   }

   /**
    * @param key the key to set
    */
   public void setKey(Attribute key)
   {
      this.keys.clear();
      this.keys.add(key);
   }

}
