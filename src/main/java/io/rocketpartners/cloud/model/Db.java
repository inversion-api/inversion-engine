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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.rocketpartners.utils.English;

public class Db
{
   protected Api              api       = null;

   protected Logger           log       = LoggerFactory.getLogger(getClass());
   boolean                    bootstrap = true;

   protected String           name      = null;
   protected String           type      = null;

   protected ArrayList<Table> tables    = new ArrayList();

   public Db()
   {
   }

   public Db(String name)
   {
      this.name = name;
   }

   public void bootstrapApi() throws Exception
   {

   }

   public void startup()
   {
   }

   public void shutdown()
   {
   }

   public boolean isType(String... types)
   {
      if (this.type == null)
         return false;

      for (String type : types)
      {
         if (this.type.equalsIgnoreCase(type))
            return true;
      }
      return false;
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
      this.tables.clear();
      for (Table table : tbls)
         addTable(table);
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

   public String getType()
   {
      return type;
   }

   public void setType(String type)
   {
      this.type = type;
   }

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

   protected String lowercaseAndPluralizeString(String collectionName)
   {
      collectionName = Character.toLowerCase(collectionName.charAt(0)) + collectionName.substring(1, collectionName.length());

      if (!collectionName.endsWith("s"))
         collectionName = English.plural(collectionName);

      return collectionName;
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

   public boolean isBootstrap()
   {
      return bootstrap;
   }

   public void setBootstrap(boolean bootstrap)
   {
      this.bootstrap = bootstrap;
   }
}
