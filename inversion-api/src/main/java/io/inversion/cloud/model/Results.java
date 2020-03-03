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

import io.inversion.cloud.rql.Query;
import io.inversion.cloud.rql.Term;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Results<M extends Map> implements Iterable<M>
{
   protected Query      query      = null;
   protected List       rows       = new ArrayList();
   protected List<Term> next       = new ArrayList();
   protected int        foundRows  = -1;
   protected String     debugQuery = null;
   protected String     testQuery  = null;

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

   public boolean isDryRun()
   {
      return query.isDryRun();
   }

   public String getDebugQuery()
   {
      return debugQuery;
   }

   public Results withDebugQuery(String debugQuery)
   {
      this.debugQuery = debugQuery;
      return this;
   }

   public String getTestQuery()
   {
      return testQuery;
   }

   public Results withTestQuery(String testQuery)
   {
      this.testQuery = testQuery;
      return this;
   }

}
