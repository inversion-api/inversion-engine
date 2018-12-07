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

/**
 * @author tc-rocket
 *
 */
public class Index extends Dto
{
   protected Table        table   = null;
   protected String       name    = null;
   protected String       type    = null;             // primary, partition, sort, localsecondary, etc
   protected List<Column> columns = new ArrayList<>();

   public Index()
   {
      super();
   }

   public Index(Table table, String name, String type)
   {
      super();
      this.table = table;
      this.name = name;
      this.type = type;
   }

   public Table getTable()
   {
      return table;
   }

   public void setTable(Table table)
   {
      this.table = table;
   }

   public String getName()
   {
      return name;
   }

   public void setName(String name)
   {
      this.name = name;
   }

   public String getType()
   {
      return type;
   }

   public void setType(String type)
   {
      this.type = type;
   }

   public List<Column> getColumns()
   {
      return new ArrayList<>(columns);
   }

   public void setColumns(List<Column> columns)
   {
      this.columns.clear();
      for (Column column : columns)
      {
         addColumn(column);
      }
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

   public String toString()
   {
      return name == null ? super.toString() : name + " " + columns;
   }

}
