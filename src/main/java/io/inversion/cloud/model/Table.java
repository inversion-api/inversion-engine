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

public class Table
{
   protected Db                db      = null;
   protected String            name    = null;
   protected ArrayList<Column> columns = new ArrayList();
   protected ArrayList<Index>  indexes = new ArrayList();

   protected boolean           exclude = false;

   public Table()
   {
      super();
   }

   public Table(Db db, String name)
   {
      super();
      this.db = db;
      this.name = name;
   }

   /**
    * @return the linkTbl
    */
   public boolean isLinkTbl()
   {
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
      if (object instanceof Table)
      {
         Table table = (Table) object;
         return table.getDb().equals(db) && name.equals(table.getName());
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
      return name;
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
    * @return the columns
    */
   public List<Column> getColumns()
   {
      ArrayList cols = new ArrayList(columns);
      Collections.sort(cols);
      return cols;
   }

   /**
    * @param columns the columns to set
    */
   public Table withColumns(Column... cols)
   {
      for (Column col : cols)
         withColumn(col);
      return this;
   }

   public Table withColumn(Column column)
   {
      Column existing = column.getName() == null ? null : getColumn(column.getName());

      if (existing != null && existing == column)
         return this;

      if (existing != null)
         throw new ApiException("you are trying to add a column name that already exists: " + column.getName());

      if (column != null && !columns.contains(column))
      {
         columns.add(column);

         if (column.getTable() != this)
            column.withTable(this);
      }
      return this;
   }

   public Column makeColumn(String name, String type)
   {
      Column column = getColumn(name);

      if (column == null)
      {
         column = new Column();
         column.withName(name);
         column.withType(type);

         withColumn(column);
      }

      return column;
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

      StringBuffer key = new StringBuffer("");
      for (Column col : index.getColumns())
      {
         Object val = values.get(col.getName());
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

      List<Column> columns = index.getColumns();

      List colNames = new ArrayList();
      columns.forEach(c -> colNames.add(c.getName()));
      Rows rows = new Rows(colNames);

      for (List row : parseKeys(inKeys))
      {
         if (row.size() != columns.size())
            throw new ApiException(SC.SC_400_BAD_REQUEST, "Supplied entity key '" + inKeys + "' has " + row.size() + "' parts but the primary index for table '" + this.getName() + "' has " + columns.size());

         for (int i = 0; i < columns.size(); i++)
         {
            Object value = decodeStr(row.get(i).toString());//.replace("\\\\", "\\").replace("\\~", "~").replace("\\,", ",");

            if (((String) value).length() == 0)
               throw new ApiException(SC.SC_400_BAD_REQUEST, "A key component can not be empty '" + inKeys + "'");

            value = getDb().cast(columns.get(i), value);
            row.set(i, value);

            rows.addRow(row);
         }
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

         if (index.getColumns().size() == 0)
            return index;

         if (found == null)
         {
            found = index;
         }
         else if (index.getColumns().size() < found.getColumns().size())
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
      for (Index index : indexes)
         withIndex(index);

      return this;
   }

   public Table withIndex(Index index)
   {
      if (index != null && !indexes.contains(index))
      {
         indexes.add(index);
         if (index.getTable() != this)
            index.withTable(this);
      }

      return this;
   }

   public Index makeIndex(Column column, String name, String type, boolean unique)
   {
      //System.out.println("WITH INDEX: " + name + " - " + column);
      Index index = getIndex(name);
      if (index != null)
      {
         index.withColumn(column);
      }
      else
      {
         index = new Index(this, column, name, type, unique);
         withIndex(index);
      }
      return index;
   }

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
