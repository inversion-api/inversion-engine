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

import io.inversion.cloud.utils.Rows;
import io.inversion.cloud.utils.Rows.Row;
import io.inversion.cloud.utils.Utils;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.text.StringEscapeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Table
{
   protected Db                db         = null;
   protected String            name       = null;
   protected String            actualName = null;

   protected ArrayList<Column> columns    = new ArrayList();
   protected ArrayList<Index>  indexes    = new ArrayList();

   protected boolean           exclude    = false;

   public Table()
   {
      super();
   }

   public Table(String name)
   {
      withName(name);
   }

   //   public Table(Db db, String name)
   //   {
   //      super();
   //      this.db = db;
   //      this.name = name;
   //   }

   /**
    * @return the linkTbl
    */
   public boolean isLinkTbl()
   {
      if (columns.size() == 0)
         return false;

      boolean isLinkTbl = true;
      for (Column c : columns)
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

   public Column getColumn(String name)
   {
      for (Column col : columns)
      {
         if (name.equalsIgnoreCase(col.getName()))
            return col;
      }
      return null;
   }

   public boolean equals(Object object)
   {
      if (object == this)
         return true;

      if (object instanceof Table)
      {
         Table table = (Table) object;
         return (db == null || db == table.db) && Utils.equal(name, table.name);
      }
      return false;
   }

   public String toString()
   {
      return name != null ? name : super.toString();
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
   public Table withDb(Db db)
   {
      this.db = db;
      return this;
   }

   /**
    * @return the name
    */
   public String getName()
   {
      return name != null ? name : actualName;
   }

   /**
    * @param name the name to set
    */
   public Table withName(String name)
   {
      this.name = name;
      return this;
   }

   /**
    * @return the name
    */
   public String getActualName()
   {
      return actualName != null ? actualName : name;
   }

   /**
    * @param name the name to set
    */
   public Table withActualName(String name)
   {
      this.actualName = name;
      return this;
   }

   /**
    * @return the columns
    */
   public List<Column> getColumns()
   {
      ArrayList cols = new ArrayList(columns);
      Collections.sort(cols);
      return cols;
   }

   public int indexOf(Column column)
   {
      return columns.indexOf(column);
   }

   /**
    * @param columnNames the columns to set
    */
   public Table withColumns(Column... cols)
   {
      for (Column column : cols)
      {
         Column existing = column.getName() == null ? null : getColumn(column.getName());

         if (existing == null)
         {
            columns.add(column);
            if (column.getTable() != this)
               column.withTable(this);
         }
         else
         {
            //TODO: should the new props be copied over?
         }
      }
      return this;
   }

   public Table withColumns(String... nameTypePairs)
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

               withColumn(name, type, nullable);
            }
         }
      }
      return this;
   }

   public Table withColumn(String name, String type)
   {
      return withColumn(name, type, true);
   }

   public Table withColumn(String name, String type, boolean nullable)
   {
      return withColumns(new Column(name, type, nullable));
   }

   public void removeColumn(Column column)
   {
      columns.remove(column);
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
         throw new ApiException("Table '" + this.getName() + "' does not have a unique index");

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
            throw new ApiException(SC.SC_400_BAD_REQUEST, "Supplied entity key '" + inKeys + "' has " + row.size() + "' parts but the primary index for table '" + this.getName() + "' has " + index.size());

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

   public Table withIndexes(Index... indexes)
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

   public Table withIndex(String name, String type, boolean unique, String column1Name, String... columnsN)
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

   public Table withExclude(boolean exclude)
   {
      this.exclude = exclude;
      return this;
   }

}
