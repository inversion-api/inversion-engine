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
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.rocketpartners.cloud.rql.Term;
import io.rocketpartners.cloud.utils.English;
import io.rocketpartners.cloud.utils.Rows.Row;
import io.rocketpartners.cloud.utils.SqlUtils;

public abstract class Db<T extends Db>
{
   transient volatile boolean started        = false;
   transient volatile boolean starting       = false;
   transient volatile boolean shutdown       = false;

   protected Api              api            = null;

   protected Logger           log            = LoggerFactory.getLogger(getClass());
   protected boolean          bootstrap      = true;

   protected String           name           = null;
   protected String           type           = null;

   protected String           collectionPath = null;

   protected ArrayList<Table> tables         = new ArrayList();

   public Db()
   {
   }

   public Db(String name)
   {
      this.name = name;
   }

   /**
    * Made to be overridden by subclasses 
    * or anonymous inner classes to do specific init
    */
   protected void startup0()
   {

   }

   /**
    * Made to be overridden by subclasses 
    * or anonymous inner classes to do specific init
    */
   protected void shutdown0()
   {

   }

   /**
    * Finds the entity keys on the other side of the relationship
    * @param relationship
    * @param sourceEntityKeys
    * @return Map<sourceEntityKey, relatedEntityKey>
    * @throws Exception
    */
   //   public Map<String, String> select(Index toMatch, Index toRetrieve, List<String> matchKeys) throws Exception
   //   {
   //      throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Unsupported Operation.  Implement " + getClass().getName() + ".select() to implement");
   //   }
   //
   //   public Rows select(Table table, List<Column> toMatch, List<Column> toRetrieve, Rows matchValues) throws Exception
   //   {
   //      throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Unsupported Operation.  Implement " + getClass().getName() + ".select() to implement");
   //   }

   public abstract Results<Row> select(Table table, List<Term> columnMappedTerms) throws Exception;
   //   {
   //      throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Unsupported Operation.  Implement " + getClass().getName() + ".select() to implement");
   //   }

   public void delete(Table table, List<String> entityKeys) throws Exception
   {
      for (String entityKey : entityKeys)
      {
         delete(table, entityKey);
      }
   }

   public abstract void delete(Table table, String entityKey) throws Exception;
   //   {
   //      throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Unsupported Operation.  Implement " + getClass().getName() + ".delete() to implement");
   //   }

   public abstract String upsert(Table table, Map<String, Object> row) throws Exception;

   public List<String> upsert(Table table, List<Map<String, Object>> rows) throws Exception
   {
      List keys = new ArrayList();
      for (Map<String, Object> row : rows)
      {
         keys.add(upsert(table, row));
      }
      return keys;
   }

   public Object cast(String type, Object value)
   {
      try
      {
         if (value == null)
            return null;

         if (type == null)
            return value.toString();

         switch (type)
         {
            case "S":
               return value.toString();

            case "N":
               return Long.parseLong(value.toString());

            case "BOOL":
               return Boolean.parseBoolean(value.toString());

            default :
               return SqlUtils.cast(value, type);
         }
      }
      catch (Exception ex)
      {
         throw new RuntimeException("Error casting '" + value + "' as type '" + type + "'", ex);
      }
   }

   public synchronized Db startup()
   {
      if (started || starting) //starting is an accidental recursion guard
         return this;

      starting = true;
      try
      {
         startup0();

         started = true;
         return this;
      }
      finally
      {
         starting = false;
      }
   }

   public synchronized void shutdown()
   {
      if ((started || starting) && !shutdown)
      {
         shutdown = true;
         shutdown0();
      }
   }

   public boolean isStarted()
   {
      return started;
   }

   public boolean isShutdown()
   {
      return shutdown;
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

   /**
    * @return the tables
    */
   public List<Table> getTables()
   {
      return tables;
   }

   public Table withTable(String name)
   {
      Table table = new Table(this, name);
      withTable(table);
      return table;
   }

   /**
    * @param tables the tables to set
    */
   public T withTables(List<Table> tbls)
   {
      for (Table table : tbls)
         withTable(table);

      return (T) this;
   }

   public T withTable(Table tbl)
   {
      if (tbl != null && !tables.contains(tbl))
      {
         tables.add(tbl);
         tbl.withDb(this);
      }
      return (T) this;
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
   public T withName(String name)
   {
      this.name = name;
      return (T) this;
   }

   public boolean isType(String... types)
   {
      String type = getType();

      for (String t : types)
      {
         if (type.equalsIgnoreCase(t))
            return true;
      }
      return false;
   }

   public String getType()
   {
      return type;
   }

   public T withType(String type)
   {
      this.type = type;
      return (T) this;
   }

   public T withApi(Api api)
   {
      if (this.api != api)
      {
         this.api = api;
         api.addDb(this);
      }
      return (T) this;
   }

   public Api getApi()
   {
      return api;
   }

   public boolean isBootstrap()
   {
      return bootstrap;
   }

   public T withBootstrap(boolean bootstrap)
   {
      this.bootstrap = bootstrap;
      return (T) this;
   }

   public String getCollectionPath()
   {
      return collectionPath;
   }

   public T withCollectionPath(String collectionPath)
   {
      if (collectionPath != null && !collectionPath.endsWith("/"))
         collectionPath += "/";

      this.collectionPath = collectionPath;
      return (T) this;
   }

   protected String beautifyCollectionName(String inName)
   {
      String collectionName = inName.replaceAll("\\s+","");

      if (collectionName.toUpperCase().equals(collectionName))//crappy oracle style all uppercase name
         collectionName = collectionName.toLowerCase();

      collectionName = Character.toLowerCase(collectionName.charAt(0)) + collectionName.substring(1, collectionName.length());

      if (!(collectionName.endsWith("s") || collectionName.endsWith("S")))
         collectionName = English.plural(collectionName);

      return collectionName;
   }

   protected String beautifyAttributeName(String inName)
   {
      if (inName.toUpperCase().equals(inName))
         inName = inName.toLowerCase();

      return inName;
   }

   protected String makeRelationshipName(Relationship rel)
   {
      String name = null;
      String type = rel.getType();
      boolean pluralize = false;
      if (type.equals(Relationship.REL_ONE_TO_MANY))
      {
         name = rel.getFk1Col1().getName();
         if (name.toLowerCase().endsWith("id") && name.length() > 2)
         {
            name = name.substring(0, name.length() - 2);
         }
      }
      else if (type.equals(Relationship.REL_MANY_TO_ONE))
      {
         name = rel.getRelated().getCollection().getName();//.getTbl().getName();
         pluralize = true;
      }
      else if (type.equals(Relationship.REL_MANY_TO_MANY))
      {
         name = rel.getFk2Col1().getPk().getTable().getName();
         pluralize = true;
      }

      if (name.toUpperCase().equals(name))
      {
         name = name.toLowerCase();
      }
      else
      {
         name = Character.toLowerCase(name.charAt(0)) + name.substring(1, name.length());
      }

      if (pluralize)
      {
         name = English.plural(name);
      }

      return name;
   }

   public Object cast(Column column, Object value)
   {
      return cast(column.getType(), value);
   }

   public Object cast(Attribute attr, Object value)
   {
      return cast(attr.getType(), value);
   }
   //
   //   public Object cast(String type, Object value)
   //   {
   //      return cast0(type, value);
   //   }
   //
   //   protected Object cast0(String type, Object value)
   //   {
   //      try
   //      {
   //         return SqlUtils.cast(value, type);
   //      }
   //      catch (Exception ex)
   //      {
   //         throw new RuntimeException("Error casting '" + value + "' as type '" + type + "'", ex);
   //      }
   //   }

   //   protected Map transformIn(ObjectNode node)
   //   {
   //      return null;
   //   }
   //
   //   protected ObjectNode transformOut(Map<String, Object> row)
   //   {
   //      ObjectNode node = new ObjectNode();
   //      if (collection == null)
   //         return new ObjectNode(row);
   //
   //      for (Attribute attr : collection.getEntity().getAttributes())
   //      {
   //         String attrName = attr.getName();
   //         String colName = attr.getColumn().getName();
   //         Object val = row.get(colName);
   //         node.put(attrName, val);
   //      }
   //
   //      m return node;
   //   }

}
