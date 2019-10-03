/*
 * Copyright (c) 2015-2019 Inversion.org, LLC
 * https://github.com/inversion-api
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
package io.inversion.cloud.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.inversion.cloud.rql.Query;
import io.inversion.cloud.rql.Term;

public class Results<M extends Map> implements Iterable<M>
{
   protected Query      query     = null;
   protected List       rows      = new ArrayList();
   protected List<Term> next      = new ArrayList();
   protected int        foundRows = -1;

   public Results(Query query)
   {
      this.query = query;
   }

   public Results(Query query, int foundRows, List rows)
   {
      this.query = query;
      this.foundRows = foundRows;
      this.rows = rows;
   }

   public Query getQuery()
   {
      return query;
   }

   public Results withQuery(Query query)
   {
      this.query = query;
      return this;
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

   public M getRow(int index)
   {
      return (M) rows.get(index);
   }

   public Results setRow(int index, Map row)
   {
      rows.set(index, row);
      return this;
   }

   public List<M> getRows()
   {
      return rows;
   }

   public Results withRows(List rows)
   {
      this.rows = rows;
      return this;
   }

   public Results withRow(Map row)
   {
      rows.add(row);
      return this;
   }

   public List<Term> getNext()
   {
      return new ArrayList(next);
   }

   public Results withNext(Term next)
   {
      this.next.add(next);
      return this;
   }

   public Results withNext(List<Term> next)
   {
      if (next != null)
         this.next.addAll(next);
      return this;
   }

   public int getFoundRows()
   {
      return foundRows;
   }

   public Results withFoundRows(int foundRows)
   {
      this.foundRows = foundRows;
      return this;
   }

}
