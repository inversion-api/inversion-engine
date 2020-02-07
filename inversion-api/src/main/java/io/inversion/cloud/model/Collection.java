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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringEscapeUtils;

import io.inversion.cloud.utils.Rows;
import io.inversion.cloud.utils.Rows.Row;
import io.inversion.cloud.utils.Utils;

public class Collection extends Rule<Collection>
{
   protected Db                      db                = null;

   protected String                  collectionName    = null;
   protected List<String>            collectionAliases = new ArrayList();
   protected String                  tableName         = null;

   protected ArrayList<Property>     properties        = new ArrayList();
   protected ArrayList<Index>        indexes           = new ArrayList();
   protected ArrayList<Relationship> relationships     = new ArrayList();

   protected boolean                 exclude           = false;

   public Collection()
   {
      super();
   }

   public Collection(String defaultName)
   {
      withCollectionName(defaultName);
      withTableName(defaultName);
   }

   /**
    * @return the linkTbl
    */
   public boolean isLinkTbl()
   {
      if (properties.size() == 0)
         return false;

      boolean isLinkTbl = true;
      for (Property c : properties)
      {
         if (!c.isFk())
         {
            isLinkTbl = false;
            break;
         }
      }
      //System.out.println("IS LINK TABLE: " + name + " -> " + isLinkTbl);

      return isLinkTbl;
   }

   public Property getProperty(String name)
   {
      for (Property prop : properties)
      {
         if (name.equalsIgnoreCase(prop.getColumnName()))
            return prop;
      }
      return null;
   }

   public Property findProperty(String jsonOrColumnName)
   {
      Property prop = getPropertyByColumnName(jsonOrColumnName);
      if (prop == null)
         prop = getPropertyByJsonName(jsonOrColumnName);

      return prop;
   }

   public Property getPropertyByJsonName(String name)
   {
      for (Property prop : properties)
      {
         if (name.equalsIgnoreCase(prop.getJsonName()))
            return prop;
      }
      return null;
   }

   public Property getPropertyByColumnName(String name)
   {
      for (Property col : properties)
      {
         if (name.equalsIgnoreCase(col.getColumnName()))
            return col;
      }
      return null;
   }

   public boolean equals(Object object)
   {
      if (object == this)
         return true;

      if (object instanceof Collection)
      {
         Collection coll = (Collection) object;
         return (db == null || db == coll.db) && Utils.equal(getTableName(), coll.getTableName());
      }
      return false;
   }

   public String toString()
   {
      return getCollectionName() != null ? getCollectionName() : super.toString();
   }

   /**
    * @return the db
    */
   public Db getDb()
   {
      return db;
   }

   /**
    * @param db the db to set
    */
   public Collection withDb(Db db)
   {
      this.db = db;
      return this;
   }

   /**
    * @return the name
    */
   public String getTableName()
   {
      return tableName != null ? tableName : collectionName;
   }

   /**
    * @param name the name to set
    */
   public Collection withTableName(String name)
   {
      this.tableName = name;
      return this;
   }

   /**
    * @return the name
    */
   public String getCollectionName()
   {
      return collectionName != null ? collectionName : tableName;
   }

   /**
    * @param name the name to set
    */
   public Collection withCollectionName(String name)
   {
      this.collectionName = name;
      return this;
   }

   /**
    * @return the properties
    */
   public List<Property> getProperties()
   {
      ArrayList props = new ArrayList(properties);
      Collections.sort(props);
      return props;
   }

   public int indexOf(Property property)
   {
      return properties.indexOf(property);
   }

   /**
    * @param columnNames the columns to set
    */
   public Collection withProperties(Property... props)
   {
      for (Property prop : props)
      {
         Property existing = prop.getColumnName() == null ? null : getProperty(prop.getColumnName());

         if (existing == null)
         {
            properties.add(prop);
            if (prop.getTable() != this)
               prop.withTable(this);
         }
         else
         {
            //TODO: should the new props be copied over?
         }
      }
      return this;
   }

   public Collection withProperties(String... nameTypePairs)
   {
      for (int i = 0; nameTypePairs != null && i < nameTypePairs.length; i++)
      {
         if (nameTypePairs[i] != null)
         {
            for (String pair : Utils.explode(",", nameTypePairs))
            {
               pair = pair.replace("||", "|null|");

               List<String> parts = Utils.explode("\\|", pair);
               String name = parts.get(0);
               String type = parts.size() > 1 ? parts.get(1) : "string";
               boolean nullable = parts.size() < 3 || !"false".equals(parts.get(2));

               withProperty(name, type, nullable);
            }
         }
      }
      return this;
   }

   public Collection withProperty(String name, String type)
   {
      return withProperty(name, type, true);
   }

   public Collection withProperty(String name, String type, boolean nullable)
   {
      return withProperties(new Property(name, type, nullable));
   }

   public void removeProperty(Property prop)
   {
      properties.remove(prop);
   }

   public Index getPrimaryIndex()
   {
      Index found = null;
      for (Index index : indexes)
      {
         if (!index.isUnique())
            continue;

         if (index.size() == 0)
            return index;

         if (found == null)
         {
            found = index;
         }
         else if (index.size() < found.size())
         {
            found = index;
         }
      }
      return found;
   }

   public Index getIndex(String indexName)
   {
      for (Index index : indexes)
      {
         if (indexName.equalsIgnoreCase(index.getName()))
            return index;
      }
      return null;
   }

   public ArrayList<Index> getIndexes()
   {
      return new ArrayList(indexes);
   }

   public List<Index> getIndexes(String column)
   {
      List<Index> found = new ArrayList();
      for (Index index : indexes)
      {
         if (index.hasColumn(column))
            found.add(index);
      }
      return found;
   }

   public Collection withIndexes(Index... indexes)
   {
      for (int i = 0; indexes != null && i < indexes.length; i++)
      {
         Index index = indexes[i];
         if (index != null)
         {
            if (index.getTable() != this)
               index.withTable(this);

            if (!this.indexes.contains(index))
               this.indexes.add(index);
         }
      }

      return this;
   }

   public Collection withIndex(String name, String type, boolean unique, String column1Name, String... columnsN)
   {
      Index index = getIndex(name);
      if (index == null)
      {
         index = new Index(name, type, unique, column1Name, columnsN);
         withIndexes(index);
      }
      else
      {
         index.withType(type);
         index.withUnique(unique);
         index.withColumnNames(column1Name);
         index.withColumnNames(columnsN);
      }

      return this;
   }

   //   public Index makeIndex(Column column, String name, String type, boolean unique)
   //   {
   //      //System.out.println("WITH INDEX: " + name + " - " + column);
   //      Index index = getIndex(name);
   //      if (index != null)
   //      {
   //         index.withColumn(column);
   //      }
   //      else
   //      {
   //         index = new Index(this, column, name, type, unique);
   //         withIndex(index);
   //      }
   //      return index;
   //   }

   public void removeIndex(Index index)
   {
      indexes.remove(index);
   }

   public boolean isExclude()
   {
      return exclude;
   }

   public Collection withExclude(boolean exclude)
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
    * @return the relationships
    */
   public List<Relationship> getRelationships()
   {
      return new ArrayList(relationships);
   }

   /**
    * @param relationships the relationships to set
    */
   public Collection withRelationships(Relationship... relationships)
   {
      for (Relationship rel : relationships)
         withRelationship(rel);
      return this;
   }

   public Collection withRelationship(Relationship relationship)
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

   /**
    * @param tableName
    * @return true if the name or aliases patch
    */
   public boolean hasName(String nameOrAlias)
   {
      if (nameOrAlias == null)
         return false;

      if (nameOrAlias.equalsIgnoreCase(this.collectionName) //
            || this.collectionAliases.stream().anyMatch(nameOrAlias::equalsIgnoreCase))
         return true;

      return false;
   }

   public List<String> getAliases()
   {
      return new ArrayList(collectionAliases);
   }

   public Collection withAliases(List<String> aliases)
   {
      this.collectionAliases.clear();
      for (String alias : aliases)
         withAlias(alias);
      return this;
   }

   public Collection withAlias(String alias)
   {
      if (!collectionAliases.contains(alias))
         collectionAliases.add(alias);
      return this;
   }

   @Override
   public Collection withApi(Api api)
   {
      //this.api = api;
      return this;
   }

   public String encodeKey(Map<String, Object> values)
   {
      Index index = getPrimaryIndex();
      if (index == null)
         return null;

      return encodeKey(values, index);
   }

   public static String encodeKey(Map values, Index index)
   {
      StringBuffer key = new StringBuffer("");
      for (String colName : index.getColumnNames())
      {
         Object val = values.get(colName);
         if (Utils.empty(val))
            return null;

         val = encodeStr(val.toString());

         if (key.length() > 0)
            key.append("~");

         key.append(val);
      }

      return key.toString();
   }

   public static String encodeKey(List pieces)
   {
      StringBuffer entityKey = new StringBuffer("");
      for (int i = 0; i < pieces.size(); i++)
      {
         Object piece = pieces.get(i);
         if (piece == null)
            throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Trying to encode an entity key with a null component: '" + pieces + "'");

         entityKey.append(decodeStr(piece.toString()));//piece.toString().replace("\\", "\\\\").replace("~", "\\~").replaceAll(",", "\\,"));
         if (i < pieces.size() - 1)
            entityKey.append("~");
      }
      return entityKey.toString();
   }

   /**
    * Encodes all non alpha numeric characters in a URL friendly four digit
    * hex code equivalent preceded by a "*".  Similar to Java's unicode
    * escape sequences but designed for URLs.
    * 
    * 
    * @param string
    * @return
    */
   public static String encodeStr(String string)
   {
      Pattern p = Pattern.compile("[^A-Za-z0-9]");
      Matcher m = p.matcher(string);
      StringBuffer sb = new StringBuffer();
      while (m.find())
      {
         String chars = m.group();
         String hex = new String(Hex.encodeHex(chars.getBytes()));
         while (hex.length() < 4)
            hex = "0" + hex;

         //System.out.println(chars + " -> " + hex);
         m.appendReplacement(sb, "*" + hex);
      }
      m.appendTail(sb);
      return sb.toString();
   }

   /**
    * Replaces *[0-9a-f]{4} hex sequences with the unescaped 
    * character...this is the reciprocal to encodeStr()
    * @param string
    * @return
    */
   public static String decodeStr(String string)
   {
      try
      {
         Pattern p = Pattern.compile("\\*[0-9a-f]{4}");
         Matcher m = p.matcher(string);
         StringBuffer sb = new StringBuffer();
         while (m.find())
         {
            String group = m.group();
            String hex = group.substring(1);
            String chars = StringEscapeUtils.unescapeJava("\\u" + hex);
            m.appendReplacement(sb, chars);
         }
         m.appendTail(sb);
         return sb.toString();
      }
      catch (Exception ex)
      {
         throw new RuntimeException(ex);
      }
   }

   public Row decodeKey(String inKey)
   {
      return decodeKeys(inKey).iterator().next();
   }

   public Row decodeKey(Index index, String inKey)
   {
      return decodeKeys(index, inKey).iterator().next();
   }

   //parses val1~val2,val3~val4,val5~valc6
   public Rows decodeKeys(String inKeys)
   {
      Index index = getPrimaryIndex();
      if (index == null)
         throw new ApiException("Table '" + this.getTableName() + "' does not have a unique index");

      return decodeKeys(index, inKeys);
   }

   //parses val1~val2,val3~val4,val5~valc6
   public Rows decodeKeys(Index index, String inKeys)
   {
      //someone passed in the whole href...no problem, just strip it out.
      if (inKeys.startsWith("http") && inKeys.indexOf("/") > 0)
         inKeys = inKeys.substring(inKeys.lastIndexOf("/") + 1, inKeys.length());

      List colNames = index.getColumnNames();

      Rows rows = new Rows(colNames);
      for (List row : parseKeys(inKeys))
      {
         if (row.size() != colNames.size())
            throw new ApiException(SC.SC_400_BAD_REQUEST, "Supplied entity key '" + inKeys + "' has " + row.size() + "' parts but the primary index for table '" + this.getTableName() + "' has " + index.size());

         for (int i = 0; i < colNames.size(); i++)
         {
            Object value = decodeStr(row.get(i).toString());//.replace("\\\\", "\\").replace("\\~", "~").replace("\\,", ",");

            if (((String) value).length() == 0)
               throw new ApiException(SC.SC_400_BAD_REQUEST, "A key component can not be empty '" + inKeys + "'");

            value = getDb().cast(index.getColumn(i), value);
            row.set(i, value);
         }
         rows.addRow(row);
      }

      return rows;
   }

   //parses val1~val2,val3~val4,val5~valc6
   public static List<List<String>> parseKeys(String inKeys)
   {
      String entityKeys = inKeys;
      List<String> splits = new ArrayList();

      List<List<String>> rows = new ArrayList();

      boolean escaped = false;
      for (int i = 0; i < entityKeys.length(); i++)
      {
         char c = entityKeys.charAt(i);
         switch (c)
         {
            case '\\':
               escaped = !escaped;
               continue;
            case ',':
               if (!escaped)
               {
                  rows.add(splits);
                  splits = new ArrayList();
                  entityKeys = entityKeys.substring(i + 1, entityKeys.length());
                  i = 0;
                  continue;
               }
            case '~':
               if (!escaped)
               {
                  splits.add(entityKeys.substring(0, i));
                  entityKeys = entityKeys.substring(i + 1, entityKeys.length());
                  i = 0;
                  continue;
               }
            default :
               escaped = false;
         }
      }
      if (entityKeys.length() > 0)
      {
         splits.add(entityKeys);
      }

      if (splits.size() > 0)
      {
         rows.add(splits);
      }

      for (List<String> row : rows)
      {
         for (int i = 0; i < row.size(); i++)
         {
            String value = row.get(i).replace("\\\\", "\\").replace("\\~", "~").replace("\\,", ",");
            row.set(i, value);
         }
      }

      return rows;
   }
}
