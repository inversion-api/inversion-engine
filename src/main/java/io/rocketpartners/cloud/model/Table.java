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

   boolean                     exclude = false;

   /**
    * Set to true if this is a two column
    * table where both columns are foreign
    * keys.  Means this is the link table
    * in a MANY_TO_MANY relationship
    */
   boolean                     linkTbl = false;

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
   public void setLinkTbl(boolean linkTbl)
   {
      this.linkTbl = linkTbl;
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
   public void setDb(Db db)
   {
      this.db = db;
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
   public void setName(String name)
   {
      this.name = name;
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
   public void setColumns(List<Column> cols)
   {
      this.columns.clear();
      for (Column col : cols)
         addColumn(col);
   }

   public void addColumn(Column column)
   {
      if (column != null && !columns.contains(column))
         columns.add(column);

   }

   public void removeColumn(Column column)
   {
      columns.remove(column);
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

   public void setIndexes(ArrayList<Index> indexes)
   {
      this.indexes.clear();
      for (Index index : indexes)
         addIndex(index);
   }

   public void addIndex(Index index)
   {
      if (index != null && !indexes.contains(index))
         indexes.add(index);
   }

   public void removeIndex(Index index)
   {
      indexes.remove(index);
   }

   public boolean isExclude()
   {
      return exclude;
   }

   public void setExclude(boolean exclude)
   {
      this.exclude = exclude;
   }

}
