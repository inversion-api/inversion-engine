/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
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
package io.rocketpartners.cloud.rql;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.KeyValue;
import org.apache.commons.collections4.keyvalue.DefaultKeyValue;

import io.rocketpartners.cloud.model.ApiException;
import io.rocketpartners.cloud.model.Column;
import io.rocketpartners.cloud.model.Db;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.model.Table;

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

   //------------------------------------------------------------------
   //------------------------------------------------------------------

   public Query(E table)
   {
      super(null);
      withTable(table);
   }

   public Query(E table, Object terms)
   {
      super(null);
      withTable(table);

      //order matters when multiple clauses can accept the same term 
      where();
      page();
      order();
      group();
      select();

      if (terms != null)
         withTerms(terms);
   }

   //   public QueryResults runQuery() throws Exception
   //   {
   //      TableResults tableResults = doSelect();
   //      QueryResults queryResutls = transformResults(tableResults);
   //      return queryResutls;
   //   }
   //
   //   protected TableResults doSelect() throws Exception
   //   {
   //      throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "You must implement Query.doSelect()");
   //   }
   //
   //   protected QueryResults transformResults(TableResults tableResults) throws Exception
   //   {
   //      QueryResults queryResults = new QueryResults(this, tableResults.getRowCount());
   //
   //      for (Map row : tableResults)
   //      {
   //         queryResults.withRow(transformRow(row));
   //      }
   //
   //      return queryResults;
   //   }
   //
   //   protected ObjectNode transformRow(Map<String, Object> row)
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
   //      return node;
   //   }

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
         withDb((D) table.getDb());
      }

      return r();
   }

   //   public Collection collection()
   //   {
   //      return collection;
   //   }
   //
   //   public T withCollection(Collection collection)
   //   {
   //      this.collection = collection;
   //
   //      if (collection != null)
   //      {
   //         Entity entity = collection.getEntity();
   //         if (entity != null)
   //         {
   //            Table table = entity.getTable();
   //            withTable((E) table);
   //         }
   //      }
   //
   //      return r();
   //   }
   //
   //   public String getColumnName(String attributeName)
   //   {
   //      String name = attributeName;
   //      if (collection != null)
   //      {
   //         Attribute attr = collection.getAttribute(attributeName);
   //         if (attr != null)
   //            name = attr.getColumn().getName();
   //      }
   //
   //      if (name == null)
   //         name = attributeName;
   //
   //      return name;
   //   }
   //
   //   public String getAttributeName(String columnName)
   //   {
   //      String name = columnName;
   //      if (collection != null)
   //         name = collection.getAttributeName(columnName);
   //
   //      if (name == null)
   //         name = columnName;
   //
   //      return name;
   //   }

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

}
