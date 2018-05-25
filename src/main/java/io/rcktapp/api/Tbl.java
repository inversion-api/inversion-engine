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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.rcktapp.api;

import java.util.ArrayList;
import java.util.List;

public class Tbl extends Dto
{
   Db             db          = null;
   String         name        = null;
   ArrayList<Col> cols        = new ArrayList();

   /**
    * Set to true if this is a two column
    * table where both columns are foreign
    * keys.  Means this is the link table
    * in a MANY_TO_MANY relationship
    */
   boolean        linkTbl     = false;

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
   Col            deletedFlag = null;

   public Tbl()
   {
      super();
   }

   public Tbl(Db db, String name)
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

   public Col getCol(String name)
   {
      for (Col col : cols)
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
   public List<Col> getCols()
   {
      return cols;
   }

   /**
    * @param columns the columns to set
    */
   public void setCols(List<Col> cols)
   {
      this.cols = new ArrayList(cols);
   }

   public void addCol(Col column)
   {
      if (column != null && !cols.contains(column))
         cols.add(column);

   }

   /**
    * @return the deletedFlag
    */
   public Col getDeletedFlag()
   {
      return deletedFlag;
   }

   /**
    * @param deletedFlag the deletedFlag to set
    */
   public void setDeletedFlag(Col deletedFlag)
   {
      this.deletedFlag = deletedFlag;
   }

}
