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
package io.inversion.rql;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.KeyValue;
import org.apache.commons.collections4.keyvalue.DefaultKeyValue;

import io.inversion.ApiException;
import io.inversion.Collection;
import io.inversion.Db;
import io.inversion.Property;
import io.inversion.Relationship;
import io.inversion.Results;

/**
 * Represents a full RQL query with a SELECT,WHERE,GROUP,ORDER, and PAGE clause.
 *
 */
public class Query<T extends Query, D extends Db, S extends Select, W extends Where, R extends Group, O extends Order, G extends Page> extends Builder<T, T>
{
   protected D              db         = null;
   protected Collection     collection = null;

   protected S              select     = null;
   protected W              where      = null;
   protected R              group      = null;
   protected O              order      = null;
   protected G              page       = null;

   protected boolean        dryRun     = false;

   //hold ordered list of columnName=literalValue pairs
   protected List<KeyValue> values     = new ArrayList();

   //-- OVERRIDE ME TO ADD NEW FUNCTIONALITY --------------------------
   //------------------------------------------------------------------
   //------------------------------------------------------------------
   protected RqlParser createParser()
   {
      return new RqlParser();
   }

   protected S createSelect()
   {
      return (S) new Select(this);
   }

   protected W createWhere()
   {
      return (W) new Where(this);
   }

   protected R createGroup()
   {
      return (R) new Group(this);
   }

   protected O createOrder()
   {
      return (O) new Order(this);
   }

   protected G createPage()
   {
      return (G) new Page(this);
   }

   public T withTerm(Term term)
   {
      return super.withTerm(term);
   }

   public Results doSelect() throws ApiException
   {
      return null;
   }

   //------------------------------------------------------------------
   //------------------------------------------------------------------

   public Query()
   {
      this(null, null);
   }

   public Query(D db, Collection coll)
   {
      this(db, coll, null);
   }

   public Query(D db, Collection coll, Object terms)
   {
      super(null);
      withDb(db);
      withCollection(coll);

      if (terms != null)
         withTerms(terms);
   }

   public List<Builder> getBuilders()
   {
      if (builders == null)
      {
         builders = new ArrayList();

         //order matters when multiple clauses can accept the same term 
         getWhere();
         getPage();
         getOrder();
         getGroup();
         getSelect();
      }
      return builders;
   }

   @Override
   public RqlParser getParser()
   {
      if (parser == null)
         parser = createParser();

      return parser;
   }

   public S getSelect()
   {
      if (select == null)
      {
         select = createSelect();
         withBuilder(select);
      }
      return select;
   }

   public W getWhere()
   {
      if (where == null)
      {
         where = createWhere();
         withBuilder(where);
      }
      return where;
   }

   public R getGroup()
   {
      if (group == null)
      {
         group = createGroup();
         withBuilder(group);
      }
      return group;
   }

   public O getOrder()
   {
      if (order == null)
      {
         order = createOrder();
         withBuilder(order);
      }
      return order;
   }

   public G getPage()
   {
      if (page == null)
      {
         page = createPage();
         withBuilder(page);
      }
      return page;
   }

   public Collection getCollection()
   {
      return collection;
   }

   public D getDb()
   {
      return db;
   }

   public T withDb(D db)
   {
      this.db = db;
      return r();
   }

   public T withCollection(Collection coll)
   {
      this.collection = coll;
      if (coll != null)
      {
         if (coll.getDb() != null)
            withDb((D) coll.getDb());
      }

      return r();
   }

   public int getNumValues()
   {
      return values.size();
   }

   protected T clearValues()
   {
      values.clear();
      return r();
   }

   protected T withColValue(String columnName, Object value)
   {
      Collection coll = this.collection;
      String shortName = columnName;

      if (columnName != null)
      {
         if (columnName.indexOf(".") > -1)
         {
            String collectionName = columnName.substring(0, columnName.indexOf("."));
            if(columnName.startsWith("~~relTbl_"))
            {
               columnName = columnName.substring(columnName.indexOf("_") + 1);
               collectionName = collectionName.substring(collectionName.indexOf("_") + 1);
               
               Relationship rel =  getCollection().getRelationship(collectionName);
               if(rel != null)
               {
                  collectionName = rel.getRelated().getName();
               }
            }
            coll = coll.getDb().getCollectionByTableName(collectionName);
         }

         if (coll != null)
         {
            shortName = columnName.substring(columnName.indexOf(".") + 1, columnName.length());
            
            Property col = coll.getProperty(shortName);
            if (col == null)
               ApiException.throw500InternalServerError("Unable to find column '{}' on table '{}'", columnName, coll.getTableName());

            value = db.cast(col, value);

         }
      }

      values.add(new DefaultKeyValue(columnName, value));
      return r();
   }

   public List<String> getColValueKeys()
   {
      List keys = new ArrayList();
      for (KeyValue kv : values)
         keys.add(kv.getKey());
      return keys;
   }

   public List<Object> getColValues()
   {
      List keys = new ArrayList();
      for (KeyValue kv : values)
         keys.add(kv.getValue());
      return keys;
   }

   public KeyValue<String, String> getColValue(int index)
   {
      return values.get(index);
   }

   public List<KeyValue> getValues()
   {
      return values;
   }

   public boolean isDryRun()
   {
      return dryRun;
   }

   public T withDryRun(boolean dryRun)
   {
      this.dryRun = dryRun;
      return r();
   }

}
