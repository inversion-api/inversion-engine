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

import java.util.ArrayList;
import java.util.List;

public class Entity
{
   protected Table                   table         = null;
   protected Collection              collection    = null;
   protected ArrayList<Attribute>    keys          = new ArrayList();
   protected ArrayList<Attribute>    attributes    = new ArrayList();
   protected ArrayList<Relationship> relationships = new ArrayList();
   protected String                  hint          = null;

   protected boolean                 exclude       = false;

   public Entity()
   {
   }

   protected Entity(Collection collection, Table table)
   {
      withCollection(collection);
      withTable(table);

      for (Column column : table.getColumns())
      {
         Attribute attr = new Attribute(this, column);
         withAttribute(attr);
      }
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
      this.table = tbl;
      return this;
   }

   /**
    * @return the attributes
    */
   public List<Attribute> getAttributes()
   {
      return new ArrayList(attributes);
   }

   public Entity withAttributes(Attribute... attributes)
   {
      for (Attribute attribute : attributes)
         withAttribute(attribute);

      return this;
   }

   public Attribute getAttribute(String name)
   {
      for (Attribute attr : attributes)
      {
         if (name.equalsIgnoreCase(attr.getName()))
            return attr;
      }
      return null;
   }

   public Entity withAttribute(Attribute attribute)
   {
      Attribute a = getAttribute(attribute.getName());
      if (a != null && a != attribute)
         System.out.println("???");

      if (attribute != null && !attributes.contains(attribute))
      {
         attributes.add(attribute);

         if (attribute.getEntity() != this)
            attribute.withEntity(this);
      }

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
   public Entity withRelationships(Relationship... relationships)
   {
      for (Relationship rel : relationships)
         withRelationship(rel);
      return this;
   }

   public Entity withRelationship(Relationship relationship)
   {
      if (!relationships.contains(relationship))
         relationships.add(relationship);

      if (relationship.getEntity() != this)
         relationship.withEntity(this);

      return this;
   }

   public void removeRelationship(Relationship relationship)
   {
      relationships.remove(relationship);
   }

   //   public Attribute withAttribute(Column column)
   //   {
   //      return withAttribute(column, null);
   //   }
   //
   //   public Attribute withAttribute(Column column, String name)
   //   {
   //      Attribute attr = new Attribute(this, column);
   //      if (name != null)
   //         attr.withName(name);
   //
   //      withAttribute(attr);
   //
   //      return attr;
   //   }

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
   public Entity withHint(String hint)
   {
      this.hint = hint;
      return this;
   }

   //   public Attribute getKey()
   //   {
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
   //      return null;
   //   }

   public boolean hasUniqueKey()
   {
      return table.getPrimaryIndex() != null;
   }

}
