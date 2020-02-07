/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * https://github.com/inversion-api
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
package io.inversion.cloud.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inversion.cloud.rql.Term;
import io.inversion.cloud.utils.English;
import io.inversion.cloud.utils.Rows.Row;
import io.inversion.cloud.utils.Utils;

public abstract class Db<T extends Db>
{
   transient protected Logger      log            = LoggerFactory.getLogger(getClass());

   transient volatile boolean      started        = false;
   transient volatile boolean      starting       = false;
   transient volatile boolean      shutdown       = false;

   protected Api                   api            = null;

   /**
    * A CSV of pipe delimited table name to collection pairs
    * 
    * Example: db.tables=promo-dev|promo,loyalty-punchcard-dev|punchcard
    * 
    * Or if the collection name is the name as the table name you can just send a the name
    * 
    * Example: db.includeTables=orders,users,events
    */
   protected Map<String, String>   includeTables  = new HashMap();

   protected boolean               bootstrap      = true;

   protected String                name           = null;
   protected String                type           = null;

   protected String                collectionPath = null;

   protected ArrayList<Collection> tables         = new ArrayList();

   public Db()
   {
   }

   public Db(String name)
   {
      this.name = name;
   }

   public synchronized Db startup()
   {
      if (started || starting) //starting is an accidental recursion guard
         return this;

      starting = true;
      try
      {
         doStartup();

         started = true;
         return this;
      }
      finally
      {
         starting = false;
      }
   }

   /**
    * Made to be overridden by subclasses 
    * or anonymous inner classes to do specific init
    */
   protected void doStartup()
   {
      try
      {
         if (isBootstrap())// && getTables().size() == 0)
         {
            configDb();
            configApi();
         }
      }
      catch (Exception ex)
      {
         ex.printStackTrace();
         Utils.rethrow(ex);
      }
   }

   public synchronized void shutdown()
   {
      if ((started || starting) && !shutdown)
      {
         shutdown = true;
         doShutdown();
      }
   }

   /**
    * Made to be overridden by subclasses 
    * or anonymous inner classes to do specific init
    */
   protected void doShutdown()
   {

   }

   /**
    * Finds all rows that match the supplied query terms.  
    * 
    * IMPORTANT The Result object contains a list of Row objects. Unlike the Rows class
    * all Row objects in the Result do not have to share the same keys. 
    * 
    * @param table
    * @param queryTerms
    * @return
    * @throws Exception
    */
   public abstract Results<Row> select(Collection table, List<Term> queryTerms) throws Exception;

   /**
    * Upserts the key/values pairs for each row into the underlying data source as a PATCH,
    * not as a full replacement.  Keys that are not supplied in the call but that exist in the row in 
    * the target DB should not be modified.
    * 
    * Each row should minimally contain key value pairs that satisfy one of the 
    * tables unique index constraints allowing an update to take place instead  
    * of an insert if the row already exists in the underlying data source.
    *
    * IMPORTANT #1 - implementors should note that the keys on each row may be different.
    * 
    * IMPORTANT #2 - strict POST/PUT vs POST/PATCH semantics are implementation specific.
    * For example, a RDBMS backed implementation may choose to upsert only the supplied
    * client supplied keys effectively making this a POST/PATCH operation.  A 
    * document store that is simply storing the supplied JSON may not be able to do
    * partial updates elegantly and replace existing documents entirely rendering
    * this a POST/PUT.    
    * 
    * @param table
    * @param rows
    * @return
    * @throws Exception
    */
   public abstract List<String> upsert(Collection table, List<Map<String, Object>> rows) throws Exception;

   /**
    * Deletes rows identified by the unique index values from the underlying data source.
    * 
    * IMPORTANT implementors should note that the keys on each row may be different.
    * The keys should have come from a unique index, meaning that the key/value pairs
    * for each row should uniquely identify the row...however there is no guarantee 
    * that each row will reference the same index.
    * 
    * @param table
    * @param indexValues
    * @throws Exception
    */
   public abstract void delete(Collection table, List<Map<String, Object>> indexValues) throws Exception;

   public void configDb() throws Exception
   {
      for (String key : includeTables.keySet())
      {
         withCollection(new Collection(key));
      }
   }

   protected void configApi() throws Exception
   {
      List<String> relationshipStrs = new ArrayList();

      for (Collection coll : getCollections())
      {
         if (!coll.isLinkTbl() && !coll.isExclude())
         {
            api.withCollection(coll);
            
            if (getCollectionPath() != null)
               coll.withIncludePaths(getCollectionPath());
         }
      }

      for (Collection coll : getCollections())
      {
         if (coll.isLinkTbl())
            continue;

         if (coll.getCollectionName().equals(coll.getTableName()))
         {
            //collection has not already been specifically customized
            String prettyName = beautifyCollectionName(coll.getTableName());
            coll.withCollectionName(prettyName);
         }

         for (Property prop : coll.getProperties())
         {
            if (prop.getColumnName().equals(prop.getJsonName()))
            {
               //json name has not already been specifically customized
               String prettyName = beautifyAttributeName(prop.getColumnName());
               prop.withJsonName(prettyName);
            }

         }
      }

      //-- Now go back through and create relationships for all foreign keys
      //-- two relationships objects are created for every relationship type
      //-- representing both sides of the relationship...ONE_TO_MANY also
      //-- creates a MANY_TO_ONE and there are always two for a MANY_TO_MANY.
      //-- API designers may want to represent one or both directions of the
      //-- relationship in their API and/or the names of the JSON properties
      //-- for the relationships will probably be different
      for (Collection coll : getCollections())
      {
         if (coll.isLinkTbl())
         {
            //create reciprocal pairs for of MANY_TO_MANY relationships
            //for each pair combination in the link table.
            List<Index> indexes = coll.getIndexes();
            for (int i = 0; i < indexes.size(); i++)
            {
               for (int j = 0; j < indexes.size(); j++)
               {
                  Index idx1 = indexes.get(i);
                  Index idx2 = indexes.get(j);

                  if (i == j || !idx1.getType().equals("FOREIGN_KEY") || !idx2.getType().equals("FOREIGN_KEY"))
                     continue;

                  Collection entity1 = idx1.getColumn(0).getPk().getTable();
                  Collection entity2 = idx2.getColumn(0).getPk().getTable();

                  Relationship r = new Relationship();
                  r.withType(Relationship.REL_MANY_TO_MANY);

                  r.withRelated(entity2);
                  r.withFkIndex1(idx1);
                  r.withFkIndex2(idx2);
                  r.withName(makeRelationshipName(entity1, r));
                  r.withEntity(entity1);
                  relationshipStrs.add(r.toString());
               }
            }
         }
         else
         {
            for (Index fkIdx : coll.getIndexes())
            {
               try
               {
                  if (!fkIdx.getType().equals("FOREIGN_KEY"))
                     continue;

                  Collection pkEntity = fkIdx.getColumn(0).getPk().getTable();
                  Collection fkEntity = fkIdx.getColumn(0).getTable();

                  //ONE_TO_MANY
                  {
                     Relationship r = new Relationship();
                     //TODO:this name may not be specific enough or certain types
                     //of relationships. For example where an entity is related
                     //to another entity twice
                     r.withType(Relationship.REL_MANY_TO_ONE);
                     r.withFkIndex1(fkIdx);
                     r.withRelated(fkEntity);
                     r.withName(makeRelationshipName(pkEntity, r));
                     r.withEntity(pkEntity);
                     relationshipStrs.add(r.toString());
                  }

                  //MANY_TO_ONE
                  {
                     Relationship r = new Relationship();
                     r.withType(Relationship.REL_ONE_TO_MANY);
                     r.withFkIndex1(fkIdx);
                     r.withRelated(pkEntity);
                     r.withName(makeRelationshipName(fkEntity, r));
                     r.withEntity(fkEntity);
                     relationshipStrs.add(r.toString());
                  }
               }
               catch (Exception ex)
               {
                  throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Error creating relationship for index: " + fkIdx, ex);
               }
            }
         }
      }

      //TODO...should this operate on all tables or just this DBs tables...?
      //now we need to see if any relationship names conflict and need to be made unique
      for (Collection coll : api.getCollections())
      {
         List<Relationship> relationships = coll.getRelationships();

         for (int i = 0; i < relationships.size(); i++)
         {
            String nameA = relationships.get(i).getName();

            for (int j = i + 1; j < relationships.size(); j++)
            {
               String nameB = relationships.get(j).getName();

               if (nameA.equalsIgnoreCase(nameB))
               {
                  String uniqueName = makeRelationshipUniqueName(coll, relationships.get(j));
                  relationships.get(j).withName(uniqueName);
               }
            }
         }
      }
   }

   protected String beautifyCollectionName(String name)
   {
      if (includeTables.containsKey(name))
         return includeTables.get(name);

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
   }

   protected String makeRelationshipUniqueName(Collection entity, Relationship rel)
   {
      String name = null;
      String type = rel.getType();
      boolean pluralize = false;
      if (type.equals(Relationship.REL_ONE_TO_MANY))
      {
         name = rel.getFk1Col1().getColumnName();
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

         String idxColName = rel.getFk1Col1().getColumnName();
         if (idxColName.toLowerCase().endsWith("id") && idxColName.length() > 2)
         {
            idxColName = idxColName.substring(0, idxColName.length() - 2);
         }

         idxColName = idxColName.replace("_", "");//allow fk cols like "person_id"
         if (idxColName.toUpperCase().equals(idxColName))
            idxColName = idxColName.toLowerCase();

         String collectionName = entity.getCollectionName();
         String relatedCollectionName = rel.getRelated().getCollectionName();
         //String tableName = entity.getTable().getName();
         if (!collectionName.equalsIgnoreCase(idxColName) //
               && !English.plural(idxColName).equalsIgnoreCase(collectionName))
         {
            name = idxColName + Character.toUpperCase(relatedCollectionName.charAt(0)) + relatedCollectionName.substring(1, relatedCollectionName.length());
            System.out.println("RELATIONSHIP: " + name + " " + rel);
         }
         else
         {
            name = relatedCollectionName;
         }

         pluralize = true;
      }
      else if (type.equals(Relationship.REL_MANY_TO_MANY))
      {
         name = rel.getFk2Col1().getPk().getTable().getTableName();
         pluralize = true;
      }

      name = beautifyAttributeName(name);

      if (pluralize)
      {
         name = English.plural(name);
      }

      return name;
   }

   protected String makeRelationshipName(Collection entity, Relationship rel)
   {
      String name = null;
      String type = rel.getType();
      boolean pluralize = false;
      if (type.equals(Relationship.REL_ONE_TO_MANY))
      {
         name = rel.getFk1Col1().getColumnName();
         if (name.toLowerCase().endsWith("id") && name.length() > 2)
         {
            name = name.substring(0, name.length() - 2);
         }
      }
      else if (type.equals(Relationship.REL_MANY_TO_ONE))
      {
         name = rel.getRelated().getCollectionName();
         pluralize = true;
      }
      else if (type.equals(Relationship.REL_MANY_TO_MANY))
      {
         name = rel.getFk2Col1().getPk().getTable().getCollectionName();
         pluralize = true;
      }

      name = beautifyAttributeName(name);

      if (pluralize)
      {
         name = English.plural(name);
      }

      return name;
   }

   public Object cast(Property column, Object value)
   {
      return Utils.cast(column != null ? column.getType() : null, value);
   }

   public Object cast(String type, Object value)
   {
      return Utils.cast(type, value);
   }

   public Set<Term> mapToColumns(Collection collection, Term term)
   {
      Set<Term> terms = new HashSet();

      if (term.getParent() == null)
         terms.add(term);

      if (collection == null)
         return terms;

      if (term.isLeaf() && !term.isQuoted())
      {
         String token = term.getToken();

         while (token.startsWith("-") || token.startsWith("+"))
            token = token.substring(1, token.length());

         Property attr = collection.findProperty(token);
         if (attr != null)
         {
            String columnName = attr.getColumnName();

            if (term.getToken().startsWith("-"))
               columnName = "-" + columnName;

            term.withToken(columnName);
         }
      }
      else
      {
         for (Term child : term.getTerms())
         {
            terms.addAll(mapToColumns(collection, child));
         }
      }

      return terms;
   }

   public boolean isStarted()
   {
      return started;
   }

   public boolean isShutdown()
   {
      return shutdown;
   }

   public Property getProperty(String table, String col)
   {
      for (Collection t : tables)
      {
         if (t.getTableName().equalsIgnoreCase(table))
         {
            for (Property c : t.getProperties())
            {
               if (c.getColumnName().equalsIgnoreCase(col))
               {
                  return c;
               }
            }

            return null;
         }
      }
      return null;
   }

   public Collection getCollection(String collectionOrTableName)
   {
      for (Collection t : tables)
      {
         if (collectionOrTableName.equalsIgnoreCase(t.getTableName()) //
               || collectionOrTableName.equalsIgnoreCase(t.getCollectionName()))
            return t;
      }

      return null;
   }

   public void removeCollection(Collection table)
   {
      tables.remove(table);
   }

   /**
    * @return the collections
    */
   public List<Collection> getCollections()
   {
      return tables;
   }

   public T withIncludeTables(String includeTables)
   {
      for (String pair : Utils.explode(",", includeTables))
      {
         String tableName = pair.indexOf('|') < 0 ? pair : pair.substring(0, pair.indexOf("|"));
         String collectionName = pair.indexOf('|') < 0 ? pair : pair.substring(pair.indexOf("|") + 1);
         this.includeTables.put(tableName, collectionName);
      }
      return (T) this;
   }

   /**
    * @param collections to include (add not replace)
    */
   public T withCollections(Collection... tbls)
   {
      for (Collection table : tbls)
         withCollection(table);

      return (T) this;
   }

   public T withCollection(Collection tbl)
   {
      if (tbl != null)
      {
         if (tbl.getDb() != this)
            tbl.withDb(this);

         if (!tables.contains(tbl))
            tables.add(tbl);
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
      if (type == null)
         return false;

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

}
