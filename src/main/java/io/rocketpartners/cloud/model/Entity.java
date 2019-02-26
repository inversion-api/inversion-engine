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
import java.util.LinkedList;
import java.util.List;

import io.rocketpartners.cloud.rql.Term;
import io.rocketpartners.cloud.utils.Utils;

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
   public Entity withRelationships(List<Relationship> relationships)
   {
      for (Relationship rel : relationships)
         withRelationship(rel);
      return this;
   }

   public Entity withRelationship(Relationship relationship)
   {
      if (relationship != null && !relationships.contains(relationship))
         relationships.add(relationship);
      return this;
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
   public Entity withHint(String hint)
   {
      this.hint = hint;
      return this;
   }

   public Attribute getKey()
   {
      Table table = getTable();
      if (table != null)
      {
         for (Index index : table.getIndexes())
         {
            if (index.isUnique() && index.getColumns().size() == 1)
            {
               Column col = index.getColumns().get(0);
               for (Attribute attr : getAttributes())
               {
                  if (attr.getColumn() == col)
                     return attr;
               }
            }
         }
      }
      return null;
   }

   public boolean hasUniqueKey()
   {
      return table.getPrimaryIndex() != null;
   }

   public String encodeKey(ObjectNode node)
   {
      Index index = table.getPrimaryIndex();
      if (index == null)
         return null;

      StringBuffer key = new StringBuffer("");
      for (Column col : index.getColumns())
      {
         String attr = collection.getAttributeName(col.getName());
         String val = node.getString(attr);
         if (Utils.empty(val))
            throw new ApiException("Unable to encode entity key because column '" + col + "' value is empty or can't be mapped to an attribute");

         val = val.replace("\\", "\\\\").replace("~", "\\~");

         if (key.length() > 0)
            key.append("~");

         key.append(val);
      }

      return key.toString();
   }

   public List<Term> decodeKey(String inKey)
   {
      String entityKey = inKey;
      Index index = table.getPrimaryIndex();
      if (index == null)
         throw new ApiException("Table '" + table + "' does not have a unique index");

      List<String> splits = new ArrayList();
      List<Column> columns = new ArrayList(index.getColumns());

      boolean escaped = false;
      for (int i = 0; i < entityKey.length(); i++)
      {
         char c = entityKey.charAt(i);
         switch (c)
         {
            case '\\':
               escaped = !escaped;
               continue;
            case '~':
               if (!escaped)
               {
                  splits.add(entityKey.substring(0, i));
                  entityKey = entityKey.substring(i, entityKey.length());
                  i = 0;
                  continue;
               }
            default :
               escaped = false;
         }
      }
      if (entityKey.length() > 0)//won't this always happen?
         splits.add(entityKey);

      if (splits.size() == 0 || splits.size() != columns.size())
         throw new ApiException("Supplied entity key '" + entityKey + "' has " + splits.size() + "' parts but the primary index for table '" + table + "' has " + columns.size());

      List<Term> terms = new ArrayList();

      for (int i = 0; i < splits.size(); i++)
      {
         String value = splits.get(i).replace("\\\\", "\\").replace("\\~", "~");
         String colName = columns.get(i).getName();
         String attrName = collection.getAttributeName(colName);

         if (value.length() == 0 || Utils.empty(attrName))
            throw new ApiException("Error mapping entity key '" + entityKey + "'");

         terms.add(Term.term(null, "eq", attrName, value));
      }

      return terms;
   }

}
