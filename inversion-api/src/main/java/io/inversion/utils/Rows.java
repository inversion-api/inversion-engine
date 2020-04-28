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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.inversion.utils.Rows.Row;

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

   /**
    * Represents a single row in a database result set where values can be accessed by a zero based integer index or by a case insensitive key/column name.
    * <p>
    * It is not possible to implement both List and Map but that is the practical purpose of this class.
    */
   public static class Row implements Map<String, Object>
   {
      /**
       * The shared keys/column names
       * <p>
       * RowKeys maps the case insensitive column name to an index for the <code>values</code> List. 
       */
      RowKeys      keys   = null;

      /**
       * Vales in the row
       */
      List<Object> values = null;

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

      /**
       * @param index
       * @return the key/column name for the given index
       */
      public String getKey(int index)
      {
         return keys.getKey(index);
      }

      /**
       * @param index
       * @return the value at <code>index</code> stringified if it exists
       */
      public String getString(int index)
      {
         Object value = get(index);
         if (value != null)
            return value.toString();
         return null;
      }

      /**
       * @param key
       * @return the value for <code>key</code> stringified if it exists
       */
      public String getString(String key)
      {
         Object value = get(key);
         if (value != null)
            return value.toString();
         return null;
      }

      /**
       * @param index
       * @return the value at <code>index</code> stringified and parsed as an int if it exists
       */
      public int getInt(int index)
      {
         Object value = get(index);
         if (value != null)
            return Integer.parseInt(value.toString());
         return -1;
      }

      /**
       * @param key
       * @return the value for <code>key</code> stringified and parsed as an int if it exists
       */
      public int getInt(String key)
      {
         Object value = get(key);
         if (value != null)
            return Integer.parseInt(value.toString());
         return -1;
      }

      /**
       * @param index
       * @return the value at <code>index</code> stringified and parsed as a long if it exists
       */
      public long getLong(int index)
      {
         Object value = get(index);
         if (value != null)
            return Long.parseLong(value.toString());
         return -1;
      }

      /**
       * @param key
       * @return the value for <code>key</code> stringified and parsed as a long if it exists
       */
      public long getLong(String key)
      {
         Object value = get(key);
         if (value != null)
            return Long.parseLong(value.toString());
         return -1;
      }

      /**
       * @param index
       * @return the value at <code>index</code> stringified and parsed as a float if it exists
       */
      public float getFloat(int index)
      {
         Object value = get(index);
         if (value != null)
            return Float.parseFloat(value.toString());
         return -1;
      }

      /**
       * @param key
       * @return the value for <code>key</code> stringified and parsed as a float if it exists
       */
      public float getFloat(String key)
      {
         Object value = get(key);
         if (value != null)
            return Float.parseFloat(value.toString());
         return -1;
      }

      /**
       * @param index
       * @return the value at <code>index</code> stringified and parsed as a boolean if it exists
       */
      public boolean getBoolean(int index)
      {
         Object value = get(index);
         if (value != null)
            return value.toString().toLowerCase().startsWith("t") || value.toString().equals("1");
         return false;
      }

      /**
       * @param key
       * @return the value for <code>key</code> stringified and parsed as a boolean if it exists
       */
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

      /**
       * Gets the value for the key/column.
       * 
       * @param key
       * @return the value at the index associate with <code>key</code> if it exists, otherwise null.       
       */
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
         }
         return null;
      }

      /**
       * @param index
       * @return the value at <code>index</code>
       * @throws ArrayIndexOutOfBoundsException
       */
      public Object get(int index) throws ArrayIndexOutOfBoundsException
      {
         return values.get(index);
      }

      /**
       * @param keyOrIndex  the String key or an Integer index to retrieve
       * @return the value at keyOrIndex as an Integer index or the value for keyOrIndex as a String key 
       */
      @Override
      public Object get(Object keyOrIndex)
      {
         if (keyOrIndex == null)
            return null;

         int idx = -1;
         if (keyOrIndex instanceof String)
            idx = keys.indexOf((String) keyOrIndex);
         else
            idx = ((Integer) keyOrIndex).intValue();

         if (idx < 0 || idx >= size())
            return null;

         return values.get(idx);
      }

      /**
       * Sets the <code>index</code>th column to <code>value</code>
       * @param index
       * @param value
       */
      public void set(int index, Object value)
      {
         values.set(index, value);
      }

      /**
       * Adds <code>value</code> as the last column
       * @param value
       */
      public void add(Object value)
      {
         values.add(value);
      }

      /**
       * Translates <code>key</code> into a column index and inserts <code>value</code> at that index.
       * <p>
       * If <code>key</code> does not exists, it is add to the shared RowKeys as the last column.
       * 
       * @param key
       * @param value
       */
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

      /**
       * Sets the value for <code>keyOrIndex</code> to null.
       * <p>
       * It does not actually remove the key from RowKeys or remove
       * the Row list element because RowKeys is shared across all Row
       * instances and the iteration order and number of keys/columns
       * needs to be the same for all of them.
       * 
       * @param keyOrIndex  the String key or an Integer index to remove.
       */
      public Object remove(Object keyOrIndex)
      {
         int idx = -1;
         if (keyOrIndex instanceof String)
            idx = keys.indexOf((String) keyOrIndex);
         else
            idx = ((Integer) keyOrIndex).intValue();

         if (idx >= 0)
         {
            if (idx > 0 && idx < size())
               values.set(idx, null);
         }
         return null;
      }

      @Override
      public void putAll(Map<? extends String, ? extends Object> m)
      {
         for (String key : m.keySet())
            put(key, m.get(key));

      }

      /**
       * Sets all values to null, but does not modify RowKeys or change the length of <code>values</code>.
       */
      @Override
      public void clear()
      {
         for (int i = 0; i < values.size(); i++)
         {
            values.set(i, null);
         }
      }

      /**
       * @return the RowKeys keySet which is common to all Row instances in this Rows.
       * @see io.inversion.utils.Rows.RowKeys.keySet()
       */
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

      /**
       * Constructs a new LinkedHashSet of key/value pairs preserving column iteration order.
       */
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
    * An ordered list of case insensitive key/column names shared by all Row instances in a Rows.
    * <p>
    * This allows you to map keys/column names to List indexes on each Row.
    */
   protected static class RowKeys
   {
      /** 
       * The ordered list of keys aka "column names" for this Rows object.
       * <p>
       * The values in this list preserver the case of the first entered occurrence of each case insensitive string.
       */
      List<String>         keys         = new ArrayList();

      /**
       * A map of lowercase key strings to their position in <code>keys</code> 
       */
      Map<String, Integer> lc           = new HashMap();

      /**
       * A reusable return value for {@code Rows#keySet()}
       */
      Set<String>          cachedKeySet = null;

      RowKeys()
      {
      }

      RowKeys(List<String> keys)
      {
         setKeys(keys);
      }

      int addKey(String key)
      {
         cachedKeySet = null;

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

      void setKeys(List<String> keys)
      {
         cachedKeySet = null;

         this.keys.clear();
         this.lc.clear();

         for (String key : keys)
         {
            addKey(key);
         }
      }

      /**
       * @param key
       * @return the case insensitive index of <code>key</code> in <code>keys</code> if it exists or -1
       */
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

      /**
       * @param index
       * @return the origional case string key for column <code>index</code>
       */
      String getKey(int index)
      {
         return keys.get(index);
      }

      /**
       * @return a cached/reusable iteration order preserving set of original case key/column names
       */
      Set<String> keySet()
      {
         if (cachedKeySet == null)
            cachedKeySet = new LinkedHashSet(this.keys);

         return cachedKeySet;
      }
   }

}
