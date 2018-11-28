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

import org.atteo.evo.inflector.English;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Db extends Dto
{
   protected Api    api       = null;

   protected Logger log       = LoggerFactory.getLogger(getClass());

   protected String name      = null;
   protected String type      = null;

   ArrayList<Table> tables    = new ArrayList();

   // set this to false, if you don't want to Snooze.bootstrapDb to do anything
   boolean          bootstrap = true;

   public Db()
   {
   }

   public Db(String name)
   {
      this.name = name;
   }

   public abstract void bootstrapApi() throws Exception;

   protected String makeRelationshipName(Relationship rel)
   {
      String name = null;
      String type = rel.getType();
      if (type.equals(Relationship.REL_ONE_TO_MANY))
      {
         name = rel.getFkCol1().getName();
         if (name.toLowerCase().endsWith("id") && name.length() > 2)
         {
            name = name.substring(0, name.length() - 2);
         }
      }
      else if (type.equals(Relationship.REL_MANY_TO_ONE))
      {
         name = rel.getRelated().getCollection().getName();//.getTbl().getName();
         if (!name.endsWith("s"))
            name = English.plural(name);
      }
      else if (type.equals(Relationship.REL_MANY_TO_MANY))
      {
         name = rel.getFkCol2().getPk().getTable().getName();
         if (!name.endsWith("s"))
            name = English.plural(name);
      }

      name = Character.toLowerCase(name.charAt(0)) + name.substring(1, name.length());

      return name;
   }

   public void setApi(Api api)
   {
      this.api = api;
      api.addDb(this);
   }

   public Api getApi()
   {
      return api;
   }

   public Table getTable(String tableName)
   {
      for (Table t : tables)
      {
         if (t.getName().equalsIgnoreCase(tableName))
            return t;
      }
      return null;
   }

   public void removeTable(Table table)
   {
      tables.remove(table);
   }

   public Column getColumn(String table, String col)
   {
      for (Table t : tables)
      {
         if (t.getName().equalsIgnoreCase(table))
         {
            for (Column c : t.getColumns())
            {
               if (c.getName().equalsIgnoreCase(col))
               {
                  return c;
               }
            }

            return null;
         }
      }
      return null;
   }

   public String getQuote()
   {
      return "'";
   }

   /**
    * @return the tables
    */
   public List<Table> getTables()
   {
      return tables;
   }

   /**
    * @param tables the tables to set
    */
   public void setTables(List<Table> tbls)
   {
      this.tables = new ArrayList(tbls);
   }

   public void addTable(Table tbl)
   {
      if (tbl != null && !tables.contains(tbl))
      {
         tables.add(tbl);
      }
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

   public boolean isBootstrap()
   {
      return bootstrap;
   }

   public void setBootstrap(boolean bootstrap)
   {
      this.bootstrap = bootstrap;
   }

   public String getType()
   {
      return type;
   }

   public void setType(String type)
   {
      this.type = type;
   }

}
