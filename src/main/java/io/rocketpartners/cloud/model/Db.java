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

import com.mysql.jdbc.Buffer;

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
   public Results<Row> select(Table table, List<Term> columnMappedTerms) throws Exception
   {
      throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Unsupported Operation.  Implement " + getClass().getName() + ".select()");
   }

   public void delete(Table table, List<String> entityKeys) throws Exception
   {
      for (String entityKey : entityKeys)
      {
         delete(table, entityKey);
      }
   }

   public void delete(Table table, String entityKey) throws Exception
   {
      throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Unsupported Operation.  Implement " + getClass().getName() + ".delete()");
   }

   public String upsert(Table table, Map<String, Object> row) throws Exception
   {
      throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Unsupported Operation.  Implement " + getClass().getName() + ".upsert()");
   }

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

      tableName = tableName.replaceAll("\\s+", "");
      for (Table t : tables)
      {
         String name = t.getName();

         if (name.indexOf(" ") > -1)
         {
            name = name.replaceAll("\\s+", "");
            if (name.equalsIgnoreCase(tableName))
               return t;
         }
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

   public Table makeTable(String name)
   {
      Table table = new Table(this, name);
      withTable(table);
      return table;
   }

   /**
    * @param tables the tables to set
    */
   public T withTables(Table... tbls)
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
         api.withDb(this);
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

   protected String beautifyCollectionName(String name)
   {
      name = beautifyAttributeName(name);

      if (!(name.endsWith("s") || name.endsWith("S")))
         name = English.plural(name);

      return name;
   }

   /**
    * Try to make a camel case valid java script variable name.
    * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Grammar_and_types#Variables
    * 
    * @param name
    * @return
    */
   protected String beautifyAttributeName(String name)
   {
      //all upper case...U.G.L.Y you ain't got on alibi you UGLY, hay hay you UGLY
      if (name.toUpperCase().equals(name))
      {
         name = name.toLowerCase();
      }

      StringBuffer buff = new StringBuffer("");

      boolean nextUpper = false;
      for (int i = 0; i < name.length(); i++)
      {
         char next = name.charAt(i);
         if (next == ' ' || next == '_')
         {
            nextUpper = true;
            continue;
         }

         if (buff.length() == 0 && // 
               !(Character.isAlphabetic(next)// 
                     || next == '$'))//OK $ is a valid initial character in a JS identifier but seriously why dude, just why? 
         {
            next = 'x';
         }

         if (nextUpper)
         {
            next = Character.toUpperCase(next);
            nextUpper = false;
         }

         if (buff.length() == 0)
            next = Character.toLowerCase(next);

         buff.append(next);
      }
      return buff.toString();

      //      name = name.trim().replaceAll(" +", " ");
      //
      //      while (name.startsWith("_") && name.length() > 1)
      //         name = name.substring(1, name.length());
      //
      //      while (name.endsWith("_") && name.length() > 1)
      //         name = name.substring(0, name.length() - 1);
      //
      //      name = name.trim().replaceAll("_+", " ");
      //
      //      //convert "_" case aka "Snake Case" to camel case ex: something_like_this to somethingLikeThis
      //      int idx = name.indexOf("_");
      //      while (idx > -1 && name.length() > 1)
      //      {
      //         name = name.substring(0, idx) + name.substring(idx + 1, name.length());
      //         if (idx < name.length())
      //            name = name.substring(0, idx) + Character.toUpperCase(name.charAt(idx)) + name.substring(idx + 1, name.length());
      //
      //         idx = name.indexOf("_");
      //      }
      //
      //      //convert "space case" to camel case ex: "something like this" to somethingLikeThis
      //      idx = name.indexOf(" ");
      //      while (idx > -1 && name.length() > 1)
      //      {
      //         name = name.substring(0, idx) + name.substring(idx + 1, name.length());
      //         if (idx < name.length())
      //            name = name.substring(0, idx) + Character.toUpperCase(name.charAt(idx)) + name.substring(idx + 1, name.length());
      //
      //         idx = name.indexOf(" ");
      //      }

      //probably camel case with leading cap, lower first char 
      //      if (Character.isUpperCase(name.charAt(0)))
      //         name = Character.toLowerCase(name.charAt(0)) + name.substring(1, name.length());
      //
      //      return name;
   }

   protected String makeRelationshipName(Entity entity, Relationship rel)
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
         //Example
         //
         //if the Alarm table has a FK to the Category table
         //this would be called to add a relationship to the Category
         //collection called "alarms"....this is the default case
         //assuming the Alarm fk column is semantically related to 
         //the Category table with a name such as:
         //category, categories, categoryId or categoriesId 
         //
         //say for example that the Alarm table had two foreign
         //keys to the Category table.  One called "categoryId"
         //and the other called "subcategoryId".  In this case
         //the "categoryId" column is semantically related and would
         //result in the collection property "alarms" being added
         //to the Category collection.  The "subcategoyId" column
         //name is not one of the semantically related names 
         //so it results in a property called "subcategoryAlarms"
         //being added to the Category collection.
         
         
         String idxColName = rel.getFk1Col1().getName();
         if (idxColName.toLowerCase().endsWith("id") && idxColName.length() > 2)
         {
            idxColName = idxColName.substring(0, idxColName.length() - 2);
         }

         String collectionName = entity.getCollection().getName();
         String relatedCollectionName = rel.getRelated().getCollection().getName();
         //String tableName = entity.getTable().getName();
         if (!collectionName.equalsIgnoreCase(idxColName) //
               && !English.plural(idxColName).equalsIgnoreCase(collectionName))
         {
            name = idxColName + Character.toUpperCase(relatedCollectionName.charAt(0)) + relatedCollectionName.substring(1, relatedCollectionName.length());
            //System.out.println("RELATIONSHIP: " + name + " " +  rel);
         }
         else
         {
            name = relatedCollectionName;
         }

         pluralize = true;
      }
      else if (type.equals(Relationship.REL_MANY_TO_MANY))
      {
         name = rel.getFk2Col1().getPk().getTable().getName();
         pluralize = true;
      }

      name = beautifyAttributeName(name);

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
