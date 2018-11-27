/**
 * 
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
   Table        table   = null;
   String       name    = null;
   String       type    = null;             // primary, partition, sort, localsecondary, etc
   List<Column> columns = new ArrayList<>();

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
      return columns;
   }

   public void setColumns(List<Column> columns)
   {
      this.columns = columns;
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
