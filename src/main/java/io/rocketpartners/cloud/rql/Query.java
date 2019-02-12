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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.rocketpartners.cloud.model.Table;
import io.rocketpartners.utils.KVPair;

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
public class Query<E extends Table, T extends Query, P extends Builder, S extends Select, W extends Where, R extends Group, O extends Order, G extends Page> extends Builder<T, P>
{
   protected E                   table    = null;

   protected S                   select   = null;
   protected W                   where    = null;
   protected R                   group    = null;
   protected O                   order    = null;
   protected G                   page     = null;

   //hold ordered list of columnName=literalValue pairs
   protected List<KVPair>        values   = new ArrayList();

   //a map of rql attribute names to underlying db column names
   protected Map<String, String> colNames = new HashMap();

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

      this.table = table;

      //order matters when multiple clauses can accept the same term 
      where();
      page();
      order();
      group();
      select();
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

   public T withColNames(Map<String, String> attrToColNames)
   {
      for (String attrName : attrToColNames.keySet())
      {
         colNames.put(attrName.toLowerCase(), attrToColNames.get(attrName));
      }
      return r();
   }

   public String getColumnName(String attribute)
   {
      return colNames.get(attribute.toLowerCase());
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

   protected T withColValue(String key, String value)
   {
      String column = key != null ? colNames.get(key.toLowerCase()) : null;
      if (column == null)
         column = key;

      values.add(new KVPair(column, value));
      return r();
   }

   public List<String> getColValueKeys()
   {
      List keys = new ArrayList();
      for (KVPair kv : values)
         keys.add(kv.getKey());
      return keys;
   }

   public KVPair<String, String> getColValue(int index)
   {
      return values.get(index);
   }
}
