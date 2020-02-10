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
package io.inversion.cloud.rql;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.KeyValue;
import org.apache.commons.collections4.keyvalue.DefaultKeyValue;

import io.inversion.cloud.model.ApiException;
import io.inversion.cloud.model.Column;
import io.inversion.cloud.model.Db;
import io.inversion.cloud.model.Results;
import io.inversion.cloud.model.SC;
import io.inversion.cloud.model.Table;

/**
 * 
   //      query q = new Query().where('field').gt(100)
   //                           .where('ield2').lt(500)
   //                           .where("field>200")
   //                           .where("and(gt(field, 100)&lt(field2, 0))")
   //                           .where(or(gt("field", 5), lt("field", 2)))
   //                           .where().gt("field", 5).lt("field", 2)
 * @author wells
 *
 */
public class Query<T extends Query, D extends Db, E extends Table, S extends Select, W extends Where, R extends Group, O extends Order, G extends Page> extends Builder<T, T>
{
   protected D              db     = null;
   protected E              table  = null;

   protected S              select = null;
   protected W              where  = null;
   protected R              group  = null;
   protected O              order  = null;
   protected G              page   = null;

   protected boolean        dryRun = false;

   //hold ordered list of columnName=literalValue pairs
   protected List<KeyValue> values = new ArrayList();

   //-- OVERRIDE ME TO ADD NEW FUNCTIONALITY --------------------------
   //------------------------------------------------------------------
   //------------------------------------------------------------------
   protected Parser createParser()
   {
      return new Parser();
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

   public Results doSelect() throws Exception
   {
      return null;
   }

   //------------------------------------------------------------------
   //------------------------------------------------------------------

   public Query()
   {
      this(null);
   }

   public Query(E table)
   {
      this(table, null);
   }

   public Query(E table, Object terms)
   {
      super(null);
      withTable(table);

      if (terms != null)
         withTerms(terms);
   }

   public List<Builder> getBuilders()
   {
      if (builders == null)
      {
         builders = new ArrayList();

         //order matters when multiple clauses can accept the same term 
         where();
         page();
         order();
         group();
         select();
      }
      return builders;
   }

   @Override
   public Parser getParser()
   {
      if (parser == null)
         parser = createParser();

      return parser;
   }

   public S select()
   {
      if (select == null)
      {
         select = createSelect();
         withBuilder(select);
      }
      return select;
   }

   public W where()
   {
      if (where == null)
      {
         where = createWhere();
         withBuilder(where);
      }
      return where;
   }

   public R group()
   {
      if (group == null)
      {
         group = createGroup();
         withBuilder(group);
      }
      return group;
   }

   public O order()
   {
      if (order == null)
      {
         order = createOrder();
         withBuilder(order);
      }
      return order;
   }

   public G page()
   {
      if (page == null)
      {
         page = createPage();
         withBuilder(page);
      }
      return page;
   }

   public E table()
   {
      return table;
   }

   public D db()
   {
      return db;
   }

   public T withDb(D db)
   {
      this.db = db;
      return r();
   }

   public D getDb()
   {
      return db;
   }

   public T withTable(E table)
   {
      this.table = table;
      if (table != null)
      {
         if (table.getDb() != null)
            withDb((D) table.getDb());
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
      Table table = this.table;
      String shortName = columnName;

      if (columnName != null)
      {
         if (columnName.indexOf(".") > -1)
         {
            table = table.getDb().getTable(columnName.substring(0, columnName.indexOf(".")));
            shortName = columnName.substring(columnName.indexOf(".") + 1, columnName.length());
         }

         if (table != null)
         {
            Column col = table.getColumn(shortName);
            if (col == null)
               throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, " unable to find column '" + columnName + "' on table '" + table.getName() + "'");

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
