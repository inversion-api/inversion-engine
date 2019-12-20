/*
 * Copyright 2008-2017 Wells Burke
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
package io.inversion.cloud.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class DoubleKeyListMap<K1, K2, V>
{
   Map<Object, ListMap> root = new Hashtable();

   public synchronized Object put(K1 key1, K2 key2, V value)
   {
      ListMap key2Map = root.get(key1);
      if (key2Map == null)
      {
         key2Map = new ListMap();
         root.put(key1, key2Map);
      }

      return key2Map.put(key2, value);
   }

   public boolean contains(K1 key1, K2 key2, V value)
   {
      ListMap lm = root.get(key1);
      return lm != null && lm.containsKey(key2, value);
   }

   public boolean contains(K1 key1, K2 key2)
   {
      ListMap lm = root.get(key1);
      return lm != null && lm.size() > 0;
   }

   public List<V> get(K1 key1, K2 key2)
   {
      ListMap key2Map = root.get(key1);
      if (key2Map != null)
      {
         return key2Map.get(key2);
      }

      return Collections.EMPTY_LIST;
   }

   public ListMap<K2, V> get(K1 key)
   {
      return root.get(key);
   }

   public Set<K1> keySet()
   {
      return (Set<K1>) root.keySet();
   }

   public static class ListMap<K3, V2>
   {
      Map<K3, List<V2>> map = new Hashtable();

      public int size()
      {
         return map.size();
      }

      public void clear()
      {
         map.clear();
      }

      public boolean containsKey(K3 key)
      {
         return map.containsKey(key);
      }

      public boolean containsKey(K3 key, V2 value)
      {
         return get(key).contains(value);
      }

      public synchronized void putAll(K3 key, Collection<V2> values)
      {
         List<V2> list = map.get(key);
         if (list == null)
         {
            list = new Vector();
            map.put(key, list);
         }

         for (V2 value : values)
         {
            if (!list.contains(value))
               list.add(value);
         }
      }

      public synchronized Object put(K3 key, V2 value)
      {
         List<V2> list = map.get(key);
         if (list == null)
         {
            list = new Vector();
            map.put(key, list);
         }

         if (!list.contains(value))
            list.add(value);

         return null;
      }

      public synchronized Set<K3> keySet()
      {
         return map.keySet();
      }

      public Collection<V2> values()
      {
         Collection<V2> values = new HashSet();
         for (List list : map.values())
         {
            values.addAll(list);
         }
         return values;
      }

      public synchronized List<V2> remove(K3 key)
      {
         List<V2> removed = map.remove(key);
         if (removed == null)
            removed = Collections.EMPTY_LIST;

         return removed;
      }

      /**
       * Removes the value from the list if thhe
       * list exists
       * 
       * @param key
       * @param value
       */
      public void remove(K3 key, V2 value)
      {
         List<V2> list = map.get(key);
         if (list != null)
         {
            while (list.contains(value))
            {
               list.remove(value);
            }

            if (list.size() == 0)
            {
               remove(key);
            }
         }
      }

      /**
       * Overridden to return empty list instead of null
       */
      public List<V2> get(K3 key)
      {
         List<V2> list = map.get(key);
         if (list == null)
         {
            return Collections.EMPTY_LIST;
         }
         return list;
      }
   }
}