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

public class Table extends Dto
{
   Db                db          = null;
   String            name        = null;
   ArrayList<Column> columns     = new ArrayList();

   /**
    * Set to true if this is a two column
    * table where both columns are foreign
    * keys.  Means this is the link table
    * in a MANY_TO_MANY relationship
    */
   boolean           linkTbl     = false;

   /**
    * Set to a colunn matching a name
    * in ApiService.DELETED_FLAGS. If
    * exists, rows won't be deleted
    * only marked as deletedFlag = true.
    * Additionally deletedFlag = true
    * columns won't be returned
    * in gets unless the matching
    * attribute is specifically set to
    * true in as a query parameter
    */
   Column            deletedFlag = null;

   /**
    * Set to true to completely exclude from API.
    */
   boolean           exclude     = false;

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
      return columns;
   }

   /**
    * @param columns the columns to set
    */
   public void setColumns(List<Column> cols)
   {
      this.columns = new ArrayList(cols);
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

   /**
    * @return the deletedFlag
    */
   public Column getDeletedFlag()
   {
      return deletedFlag;
   }

   /**
    * @param deletedFlag the deletedFlag to set
    */
   public void setDeletedFlag(Column deletedFlag)
   {
      this.deletedFlag = deletedFlag;
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
