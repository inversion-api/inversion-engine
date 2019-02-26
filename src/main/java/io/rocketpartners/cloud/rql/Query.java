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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.KeyValue;
import org.apache.commons.collections4.keyvalue.DefaultKeyValue;

import io.rocketpartners.cloud.model.ApiException;
import io.rocketpartners.cloud.model.Attribute;
import io.rocketpartners.cloud.model.Collection;
import io.rocketpartners.cloud.model.Db;
import io.rocketpartners.cloud.model.Entity;
import io.rocketpartners.cloud.model.ObjectNode;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.model.Table;
import io.rocketpartners.cloud.utils.SqlUtils;

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
   protected D              db         = null;
   protected Collection     collection = null;
   protected E              table      = null;

   protected S              select     = null;
   protected W              where      = null;
   protected R              group      = null;
   protected O              order      = null;
   protected G              page       = null;

   //hold ordered list of columnName=literalValue pairs
   protected List<KeyValue> values     = new ArrayList();

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

   public Query(Collection collection)
   {
      this(collection, null);
   }

   public Query(Collection collection, Object terms)
   {
      super(null);

      if (collection != null)
      {
         withCollection(collection);
      }

      //      this.table = table;

      //order matters when multiple clauses can accept the same term 
      where();
      page();
      order();
      group();
      select();

      if (terms != null)
         withTerms(terms);
   }

   public QueryResults runQuery() throws Exception
   {
      TableResults tableResults = doSelect();
      QueryResults queryResutls = transformResults(tableResults);
      return queryResutls;
   }

   protected TableResults doSelect() throws Exception
   {
      throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "You must implement Query.doSelect()");
   }

   protected QueryResults transformResults(TableResults tableResults) throws Exception
   {
      QueryResults queryResults = new QueryResults(this, tableResults.getNext(), tableResults.getRowCount());

      for (Map row : tableResults)
      {
         queryResults.withRow(transformRow(row));
      }

      return queryResults;
   }

   protected ObjectNode transformRow(Map<String, Object> row)
   {
      ObjectNode node = new ObjectNode();
      if (collection == null)
         return new ObjectNode(row);

      for (Attribute attr : collection.getEntity().getAttributes())
      {
         String attrName = attr.getName();
         String colName = attr.getColumn().getName();
         Object val = row.get(colName);
         node.put(attrName, val);
      }

      //      
      //      DynamoDbIndex primaryIndex = (DynamoDbIndex) table.getIndex(DynamoDbIndex.PRIMARY_INDEX);
      //      String hashKeyName = primaryIndex.getHashKeyName();
      //      String sortKeyName = primaryIndex.getSortKeyName();
      //
      //      for (int i = 0; i < result.rows.size(); i++)
      //      {
      //         ObjectNode row = (ObjectNode) result.rows.get(i);
      //         ObjectNode json = new ObjectNode();
      //
      //         result.rows.set(i, json);
      //         String hashKey = row.getString(hashKeyName);
      //         if (hashKey != null)
      //         {
      //            String sortKey = sortKeyName != null ? row.getString(sortKeyName) : null;
      //            String entityKey = DynamoDb.toEntityKey(hashKey, sortKey);
      //            String href = Chain.buildLink(collection, entityKey, null);
      //            json.put("href", href);
      //         }
      //
      //         //this preservers attribute order
      //         for (Attribute attr : collection.getEntity().getAttributes())
      //         {
      //            String colName = attr.getColumn().getName();
      //            Object value = row.remove(colName);
      //            json.put(attr.getName(), value);
      //         }
      //
      //         //copy remaining columns that were in result set but not defined in the entity
      //         //TODO...do we really want to do this?
      //         for (Object key : row.keySet())
      //         {
      //            json.put(key.toString(), row.get(key));
      //         }
      //      }

      return node;
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
      withDb((D) table.getDb());
      this.table = table;
      return r();
   }

   public Collection collection()
   {
      return collection;
   }

   public T withCollection(Collection collection)
   {
      this.collection = collection;

      if (collection != null)
      {
         Entity entity = collection.getEntity();
         if (entity != null)
         {
            Table table = entity.getTable();
            withTable((E) table);
         }
      }

      return r();
   }

   public String getColumnName(String attributeName)
   {
      String name = attributeName;
      if (collection != null)
      {
         Attribute attr = collection.getAttribute(attributeName);
         if (attr != null)
            name = attr.getColumn().getName();
      }

      if (name == null)
         name = attributeName;

      return name;
   }

   public String getAttributeName(String columnName)
   {
      String name = columnName;
      if (collection != null)
         name = collection.getAttributeName(columnName);

      if (name == null)
         name = columnName;

      return name;
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

   public Object cast(String column, Object value)
   {
      return cast0(collection.getTable().getColumn(column).getType(), value);
   }

   public Object cast(Attribute attr, Object value)
   {
      return cast0(attr.getType(), value);
   }

   protected Object cast0(String type, Object value)
   {
      try
      {
         if (value == null)
            return null;

         if (type == null)
            return value.toString();

         switch (type)
         {
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

   protected T withColValue(String attributeName, Object value)
   {
      String columnName = getColumnName(attributeName);

      Attribute attr = collection.getAttribute(attributeName);
      if (attr != null)
      {
         value = cast(attr, value);
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

   public static class QueryResults extends Results<ObjectNode, QueryResults>
   {
      Query query = null;

      public QueryResults(Query query, String next, int rowCount)
      {
         super(null, next, rowCount);
         this.query = query;
      }

      public Query getQuery()
      {
         return query;
      }

   }

   public static class TableResults extends Results<Map<String, Object>, TableResults>
   {
      public TableResults()
      {

      }

      public TableResults(List rows, String next, int rowCount)
      {
         super();
         this.rows = (List<Map<String, Object>>) rows;
         this.next = next;
         this.rowCount = rowCount;
      }
   }

   private static class Results<M extends Map, T extends Results> implements Iterable<M>
   {
      List<M> rows     = new ArrayList();
      String  next     = null;
      int     rowCount = -1;

      public Results()
      {

      }

      public Results(List<M> rows, String next, int rowCount)
      {
         super();
         this.rows = rows != null ? rows : this.rows;
         this.next = next;
         this.rowCount = rowCount;
      }

      @Override
      public Iterator<M> iterator()
      {
         return rows.iterator();
      }

      public int size()
      {
         return rows.size();
      }

      public List<M> getRows()
      {
         return rows;
      }

      public T withRows(List<M> rows)
      {
         this.rows = rows;
         return (T) this;
      }

      public T withRow(M row)
      {
         rows.add(row);
         return (T) this;
      }

      public String getNext()
      {
         return next;
      }

      public T withNext(String next)
      {
         this.next = next;
         return (T) this;
      }

      public int getRowCount()
      {
         return rowCount;
      }

      public T withRowCount(int rowCount)
      {
         this.rowCount = rowCount;
         return (T) this;
      }

   }

}
