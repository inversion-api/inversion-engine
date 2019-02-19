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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Table
{
   protected Db                db      = null;
   protected String            name    = null;
   protected ArrayList<Column> columns = new ArrayList();
   protected ArrayList<Index>  indexes = new ArrayList();

   protected boolean           exclude = false;

   /**
    * Set to true if this is a two column
    * table where both columns are foreign
    * keys.  Means this is the link table
    * in a MANY_TO_MANY relationship
    */
   protected boolean           linkTbl = false;

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
      return linkTbl;
   }

   /**
    * @param linkTbl the linkTbl to set
    */
   public Table withLinkTbl(boolean linkTbl)
   {
      this.linkTbl = linkTbl;
      return this;
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
   public Table withColumns(List<Column> cols)
   {
      for (Column col : cols)
         withColumn(col);
      return this;
   }

   public Table withColumn(Column column)
   {
      if (column != null && !columns.contains(column))
      {
         columns.add(column);
         column.withTable(this);
      }
      return this;
   }

   public Table withColumn(String name, String type)
   {
      Column column = new Column(this, getColumns().size() + 1, name, type, true);
      withColumn(column);
      return this;
   }

   public void removeColumn(Column column)
   {
      columns.remove(column);
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

   public Table withIndexes(ArrayList<Index> indexes)
   {
      for (Index index : indexes)
         withIndex(index);

      return this;
   }

   public Table withIndex(Index index)
   {
      if (index != null && !indexes.contains(index))
         indexes.add(index);

      return this;
   }

   public Index withIndex(Column column, String name, String type, boolean unique)
   {
      Index index = new Index(this, column, name, type, unique);
      withIndex(index);
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
