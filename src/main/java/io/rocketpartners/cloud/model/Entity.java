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
package io.rocketpartners.cloud.model;

import java.util.ArrayList;
import java.util.List;

public class Entity
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
      withTable(table);
   }

   public Entity(Collection collection, Table table)
   {
      withCollection(collection);
      withTable(table);
   }

   public boolean isExclude()
   {
      return exclude || table.isExclude();
   }

   public Entity withExclude(boolean exclude)
   {
      this.exclude = exclude;
      return this;
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
   public Entity withCollection(Collection collection)
   {
      this.collection = collection;
      return this;
   }

   /**
    * @param tbl the tbl to set
    */
   public Entity withTable(Table tbl)
   {
      attributes.clear();
      this.table = tbl;

      for (Column column : tbl.getColumns())
      {
         Attribute attr = new Attribute(this, column);
         withAttribute(attr);
      }
      return this;
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
   public Entity withAttributes(List<Attribute> attributes)
   {
      this.attributes.clear();
      for (Attribute attr : attributes)
         withAttribute(attr);

      return this;
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

   public Attribute withAttribute(Column column)
   {
      return withAttribute(column, null);
   }

   public Attribute withAttribute(Column column, String name)
   {
      Attribute attr = new Attribute(this, column);
      if (name != null)
         attr.withName(name);

      withAttribute(attr);

      return attr;
   }

   public Entity withAttribute(Attribute attribute)
   {
      if (attribute != null && !attributes.contains(attribute))
         attributes.add(attribute);

      return this;
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

   public Attribute getKey()
   {
//      Table table = getTable();
//      if (table != null)
//      {
//         for (Index index : table.getIndexes())
//         {
//            if (index.isUnique() && index.getColumns().size() == 1)
//            {
//               Column col = index.getColumns().get(0);
//               for (Attribute attr : getAttributes())
//               {
//                  if (attr.getColumn() == col)
//                     return attr;
//               }
//            }
//         }
//      }
      return null;
   }

}
