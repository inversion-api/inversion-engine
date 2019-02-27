package io.rocketpartners.cloud.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.rocketpartners.cloud.rql.Query;
import io.rocketpartners.cloud.rql.Term;

public class Results<M extends Map> implements Iterable<M>
{
   protected Query      query    = null;
   protected List       rows     = new ArrayList();
   protected List<Term> next     = new ArrayList();
   protected int        rowCount = -1;

   public Results(Query query)
   {
      this.query = query;
   }

   public Results(Query query, int rowCount, List rows)
   {
      this.query = query;
      this.rowCount = rowCount;
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

   public int getRowCount()
   {
      return rowCount;
   }

   public Results withRowCount(int rowCount)
   {
      this.rowCount = rowCount;
      return this;
   }

}
