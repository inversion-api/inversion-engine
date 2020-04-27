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
package io.inversion.utils;

import java.util.Collection;

import io.inversion.utils.Rows.Row;

import java.util.*;

/**
 * An utility abstraction of a database result set where all child <code>Row</code> objects are themselves maps that share the same case insensitive key set.
 * <p>
 * The idea is to be a little more memory efficient, offer zero base integer index or case insensitive key/column name access, and have key/column order match on all rows.
 * <p>
 * This was initially developed so that a JDBC {@link java.sql.ResultSet} could be loaded into a list of maps without having to replicate the keys for every row.
 * <p>
 * Implementation Notes: 
 * <p>
 * While Row implements Map, it actually uses a List to maintain its values.
 * <p>  
 * A instance of the <code>RowKeys</code>, which maintains a Map from case insensitive string keys their index position in each Row's list, is shared by all Row instances.
 */
public class Rows extends ArrayList<Row>
{
   /**
    * A case insensitive map from column name to column index 
    */
   RowKeys keys    = null;

   /**
    * The row currently being added to {@code #put(String, Object)}
    */
   Row     lastRow = null;

   /**
    * Creates an empty Rows with no keys/columns.
    */
   public Rows()
   {
      this.keys = new RowKeys();
   }

   /**
    * Creates a Rows with a single Row with keys/columns equal to <code>row.getKeySet()</code>
    * @param row
    */
   public Rows(Map row)
   {
      addRow(row);
   }

   /**
    * Creates a Rows with keys/columns equal to <code>keys</code>
    * @param keys
    */
   public Rows(String[] keys)
   {
      this.keys = new RowKeys(Arrays.asList(keys));
   }

   /**
    * Creates a Rows with keys/columns equal to <code>keys</code>
    * @param keys
    */
   public Rows(List<String> keys)
   {
      this.keys = new RowKeys(keys);
   }

   /**
    * @return the ordered key/column names
    */
   public List<String> keyList()
   {
      return new ArrayList(keys.keys);
   }

   /**
    * @return key/column names as a set that preserves iteration order  
    */
   public Set<String> keySet()
   {
      return keys.keySet();
   }

   /**
    * Adds a key/column for each Row at the end of the iteration order.
    * 
    * @param key  the new key to add
    * @return the integer index of key/column which will be <code>keys.size() -1</code> if the key is new or the existing index if a case insensitive match of <code>key</code> already exited
    */
   public int addKey(String key)
   {
      return keys.addKey(key);
   }

   /**
    * Adds a new empty Row to the end of the list.
    * 
    * @return
    */
   public Row addRow()
   {
      lastRow = new Row(keys);
      super.add(lastRow);
      return lastRow;
   }

   /**
    * Adds key/values from <code>map</code> to a new Row.
    * 
    * @param map the key/values to add to the new Row
    * @return the new Row
    * @see {@link #addRow(int, Map)}
    */
   public Row addRow(Map map)
   {
      return addRow(-1, map);
   }

   /**
    * Insert key/values from <code>map</code> as the new <code>index</code>th Row.
    * <p>
    * If RowKeys has not been initialized, it will be initialized with <code>map.keySet()</code>. 
    * <p>
    * If RowKeys has been initialized, only keys/values with a case insensitive matching key in RowKeys will be copied into the new Row.   
    * 
    * @param index the position to insert the new Row or -1 to indicate the 'at the end'
    * @param map the key/values to add to the new Row
    * @return the new Row
    * @see {@link #addRow(int, Object[])
    */
   public Row addRow(int index, Map map)
   {
      if (keys == null || keys.size() == 0)
      {
         keys = new RowKeys(new ArrayList(map.keySet()));
      }

      ArrayList arr = new ArrayList(keys.keys.size());
      for (int i = 0; i < keys.keys.size(); i++)
      {
         Object value = map.get(keys.keys.get(i));
         arr.add(value);
      }

      return addRow(index, arr.toArray());
   }

   /**
    * Adds <code>values</code>as a new Row to the end of the list.
    * 
    * @param values  the values to add to the new Row
    * @return the new Row
    * @see {@link #addRow(int, Object[])}
    */
   public Row addRow(List values)
   {
      return addRow(-1, values.toArray());
   }

   /**
    * Adds <code>values</code> as the new <code>index</code>th Row.
    *
    * @param index the position to insert the new Row or -1 to indicate the 'at the end'
    * @param values  the values to add to the new Row
    * @return the new Row
    * @see {@link #addRow(int, Object[])}
    */
   public Row addRow(int index, List values)
   {
      return addRow(index, values.toArray());
   }

   /**
    * Adds <code>values</code>as a new Row to the end of the list.
    * 
    * @param values  the values to add to the new Row
    * @return the new Row
    * @see {@link #addRow(int, Object[])}
    */
   public Row addRow(Object[] values)
   {
      return addRow(-1, values);
   }

   /**
    * Adds <code>values</code> as the new <code>index</code>th Row.
    * <p>
    * The returned Row becomes <code>lastRow</code>
    * 
    * @param index  the position to insert the new Row or -1 to indicate the 'at the end'
    * @param values  the values to add to the new Row
    * @return the new Row
    * @see {@link #addRow(int, Object[])}
    */
   public Row addRow(int index, Object[] values)
   {
      lastRow = new Row(keys, values);
      if (index > -1)
         super.add(index, lastRow);
      else
         super.add(lastRow);

      return lastRow;
   }

   /**
    * Sets key/value on <code>lastRow</code>.
    * <p>
    * If RowKeys does not have a case insensitive match for <code>key</code> then <code>key</code>
    * automatically becomes the new last column for all rows.
    *  
    * @param key
    * @param value
    */
   public void put(String key, Object value)
   {
      lastRow.put(key, value);
   }

   /**
    * Adds <code>value</code> to the end of <code>lastRow</code>. 
    * 
    * @param value
    */
   public void put(Object value)
   {
      lastRow.add(value);
   }

   /**
    * Adds the key/values from <code>row</code> as a new Row.
    * <p>
    * The actual Row object is not added to the Rows list because its RowKeys object
    * will not be the same.  Instead all key/values are copied into a new Row.
    * 
    * @param row a map containing the key/values to add
    * @see {@link #addRow(Map)}
    */
   @Override
   public boolean add(Row row)
   {
      addRow(row);
      return true;
   }

   //   /**
   //    * Inserts the key/values from <code>row</code> as the new <code>index</code>th Row.
   //    * <p>
   //    * The actual Row object is not added to the Rows list because its RowKeys object
   //    * will not be the same.  Instead all key/values are copied into a new Row.
   //    * 
   //    * @param 
   //    * @param row a map containing the key/values to add
   //    * @see {@link #addRow(Map)}
   //    */
   //   public void add(int index, Row element)
   //   {
   //      // TODO Auto-generated method stub
   //      super.add(index, element);
   //   }

   /**
    * Calls {@code #addRow(Map)} for each Row in <code>rows</code>
    */
   @Override
   public boolean addAll(Collection<? extends Row> rows)
   {
      for (Row row : rows)
      {
         addRow(row);
      }
      return true;
   }

   /**
    * Calls {@code #addRow(int, Map)} for each Row in <code>rows</code>
    */
   @Override
   public boolean addAll(int index, Collection<? extends Row> rows)
   {
      for (Row row : rows)
      {
         addRow(index, row);
      }
      return true;
   }

   //   public void sortBy(final String... keys)
   //   {
   //      Collections.sort(this, new Comparator<Rows.Row>()
   //         {
   //            @Override
   //            public int compare(Row o1, Row o2)
   //            {
   //               for (String key : keys)
   //               {
   //                  Object obj1 = o1.get(key);
   //                  Object obj2 = o2.get(key);
   //
   //                  if (obj1 == null && obj2 == null)
   //                     return 0;
   //
   //                  if (obj1 == null && obj2 != null)
   //                     return -1;
   //
   //                  if (obj1 != null && obj2 == null)
   //                     return 1;
   //
   //                  int strcmp = obj1.toString().compareTo(obj2.toString());
   //                  if (strcmp != 0)
   //                     return strcmp;
   //               }
   //               return 0;
   //            }
   //         });
   //   }

   /**
    * Represents a single row in a database result set where values can be accessed by a zero based integer index or by a case insensitive key/column name.
    * <p>
    * It is not possible to implement both List and Map but that is the practical purpose of this class.
    */
   public static class Row implements Map<String, Object>
   {
      /**
       * The shared key/column 
       */
      RowKeys      keys   = null;
      List<Object> values = null;
      boolean      cloned = false;

      public Row()
      {
         this.keys = new RowKeys();
         this.values = new ArrayList();
      }

      Row(RowKeys keys)
      {
         this.keys = keys;
         this.values = new ArrayList(keys.size());
      }

      Row(RowKeys keys, Object[] values)
      {
         this.keys = keys;
         this.values = new ArrayList(Arrays.asList(values));
      }

      public String getKey(int index)
      {
         return keys.getKey(index);
      }

      public String getString(int index)
      {
         Object value = get(index);
         if (value != null)
            return value.toString();
         return null;
      }

      public String getString(String key)
      {
         Object value = get(key);
         if (value != null)
            return value.toString();
         return null;
      }

      public int getInt(int index)
      {
         Object value = get(index);
         if (value != null)
            return Integer.parseInt(value.toString());
         return -1;
      }

      public int getInt(String key)
      {
         Object value = get(key);
         if (value != null)
            return Integer.parseInt(value.toString());
         return -1;
      }

      public long getLong(int index)
      {
         Object value = get(index);
         if (value != null)
            return Long.parseLong(value.toString());
         return -1;
      }

      public long getLong(String key)
      {
         Object value = get(key);
         if (value != null)
            return Long.parseLong(value.toString());
         return -1;
      }

      public float getFloat(int index)
      {
         Object value = get(index);
         if (value != null)
            return Float.parseFloat(value.toString());
         return -1;
      }

      public float getFloat(String key)
      {
         Object value = get(key);
         if (value != null)
            return Float.parseFloat(value.toString());
         return -1;
      }

      public boolean getBoolean(int index)
      {
         Object value = get(index);
         if (value != null)
            return value.toString().toLowerCase().startsWith("t") || value.toString().equals("1");
         return false;
      }

      public boolean getBoolean(String key)
      {
         Object value = get(key);
         if (value != null)
            return value.toString().toLowerCase().startsWith("t") || value.toString().equals("1");
         return false;
      }

      public String toString()
      {
         StringBuffer buff = new StringBuffer("{");
         for (int i = 0; i < keys.size(); i++)
         {
            buff.append(keys.getKey(i)).append("=").append(values.get(i));
            if (i < keys.size() - 1)
               buff.append(", ");
         }
         buff.append("}");
         return buff.toString();
      }

      @Override
      public int size()
      {
         //return keys.size();
         return values.size();
      }

      @Override
      public boolean isEmpty()
      {
         return size() == 0;
      }

      @Override
      public boolean containsKey(Object key)
      {
         return indexOf((String) key) >= 0;
      }

      public int indexOf(String key)
      {
         return keys.indexOf(key);
      }

      @Override
      public boolean containsValue(Object value)
      {
         for (Object v : values)
         {
            if (v == null && value == null)
               return true;

            if (v != null && v.equals(value))
               return true;
         }
         return false;
      }

      public Object get(String key)
      {
         try
         {
            int idx = indexOf(key);
            if (idx >= 0)
               return values.get(idx);
         }
         catch (Exception ex)
         {
            //System.err.println("Trying to get invalid key '" + key + "' from row.  Valide keys are " + keys.keys);
            // ex.printStackTrace();
            //
            //            int idx = indexOf(key);
            //            System.out.println(idx);
            //            System.out.println(values.size());
            //
            //            System.out.println(keys.keys.size());
            //            System.out.println(keys.lc.size());
         }
         return null;
      }

      public Object get(int index)
      {
         return values.get(index);
      }

      @Override
      public Object get(Object key)
      {
         if (key == null)
            return null;

         if (key instanceof Integer)
            return values.get(((Integer) key).intValue());

         int idx = keys.indexOf((String) key);
         if (idx < 0)
            return null;

         return values.get(idx);
      }

      public void set(int index, Object value)
      {
         values.set(index, value);
      }

      public void add(Object value)
      {
         values.add(value);
      }

      @Override
      public Object put(String key, Object value)
      {
         int idx = keys.indexOf(key);
         if (idx >= 0)
         {
            while (idx > values.size() - 1)
               values.add(null);

            return values.set(idx, value);
         }
         else
         {
            keys.addKey(key);
            values.add(value);
            return value;
         }
      }

      @Override
      public Object remove(Object key)
      {
         int idx = keys.indexOf((String) key);
         if (idx >= 0)
         {
            if (!cloned)
            {
               //copy on write
               cloned = true;
               keys = keys.clone();
            }
            Object value = values.get(idx);
            keys.removeKey((String) key);
            values.remove(idx);
            return value;
         }
         return null;
      }

      @Override
      public void putAll(Map<? extends String, ? extends Object> m)
      {
         for (String key : m.keySet())
            put(key, m.get(key));

      }

      @Override
      public void clear()
      {
         keys = new RowKeys();
         values.clear();
      }

      @Override
      public Set<String> keySet()
      {
         return keys.keySet();
      }

      @Override
      public Collection<Object> values()
      {
         return Collections.unmodifiableList(values);
      }

      public List<Object> asList()
      {
         return Collections.unmodifiableList(values);
      }

      @Override
      public Set<Entry<String, Object>> entrySet()
      {
         LinkedHashSet<Entry<String, Object>> entries = new LinkedHashSet();
         for (int i = 0; i < keys.size(); i++)
            entries.add(new E(keys.getKey(i), values.get(i)));

         return entries;
      }

      class E implements Entry<String, Object>
      {
         String key   = null;
         Object value = null;

         public E(String key, Object value)
         {
            super();
            this.key = key;
            this.value = value;
         }

         public String getKey()
         {
            return key;
         }

         public void setKey(String key)
         {
            this.key = key;
         }

         public Object getValue()
         {
            return value;
         }

         public Object setValue(Object value)
         {
            Object v = this.value;
            this.value = value;
            return v;
         }
      }

   }

   /**
    * An ordered list of case insensitive key/column names.
    */
   protected static class RowKeys
   {
      List<String>         keys   = new ArrayList();
      Map<String, Integer> lc     = new HashMap();
      Set<String>          keySet = null;

      RowKeys()
      {
      }

      RowKeys(List<String> keys)
      {
         setKeys(keys);
      }

      public RowKeys clone()
      {
         RowKeys clone = new RowKeys();
         clone.keys = new LinkedList(keys);
         clone.lc = new HashMap(lc);
         return clone;
      }

      int addKey(String key)
      {
         keySet = null;

         if (key == null)
            return -1;

         String lc = key.toLowerCase();

         int existing = this.lc.get(lc);
         if (existing > -1)
            return existing;

         this.keys.add(key);
         this.lc.put(lc, this.keys.size() - 1);

         return this.keys.size() - 1;
      }

      int removeKey(String key)
      {
         keySet = null;

         key = key.toLowerCase();
         Integer idx = lc.get(key);
         if (idx != null)
         {
            for (int i = idx; i < keys.size(); i++)
               lc.remove(keys.get(i).toLowerCase());

            this.keys.remove(idx.intValue());

            for (int i = idx; i < keys.size(); i++)
               lc.put(keys.get(i).toLowerCase(), i);

            return idx.intValue();
         }
         return -1;
      }

      void setKeys(List<String> keys)
      {
         keySet = null;

         this.keys.clear();
         this.lc.clear();

         for (String key : keys)
         {
            addKey(key);
         }
      }

      int indexOf(String key)
      {
         if (key == null)
            return -1;

         Integer idx = lc.get(key.toLowerCase());
         if (idx != null)
            return idx;

         return -1;
      }

      int size()
      {
         return keys.size();
      }

      String getKey(int index)
      {
         return keys.get(index);
      }

      Set<String> keySet()
      {
         if (keySet == null)
            keySet = new LinkedHashSet(this.keys);

         return keySet;
      }
   }

}
