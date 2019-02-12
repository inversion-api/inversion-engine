/*
 * Copyright (c) 2016-2019 Rocket Partners, LLC
 * http://rocketpartners.io
 * 
 * Copyright 2008-2016 Wells Burke
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
package io.rocketpartners.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author wells.burke
 */
public class ListMap<K, V>
{
   Map<K, List<V>> map = new HashMap();

   public int size()
   {
      return map.size();
   }

   public void clear()
   {
      map.clear();
   }

   public boolean containsKey(K key)
   {
      return map.containsKey(key);
   }

   public boolean containsKey(K key, V value)
   {
      return get(key).contains(value);
   }

   public void putAll(K key, Collection<V> values)
   {
      List<V> list = map.get(key);
      if (list == null)
      {
         list = new ArrayList();
         map.put(key, list);
      }

      for (V value : values)
      {
         if (!list.contains(value))
            list.add(value);
      }
   }

   public Object put(K key, V value)
   {
      List<V> list = map.get(key);
      if (list == null)
      {
         list = new ArrayList();
         map.put(key, list);
      }

      if (!list.contains(value))
         list.add(value);

      return null;
   }

   public Set<K> keySet()
   {
      return map.keySet();
   }

   public Collection<V> values()
   {
      Collection<V> values = new HashSet();
      for (List list : map.values())
      {
         values.addAll(list);
      }
      return values;
   }

   public List<V> remove(K key)
   {
      List<V> removed = map.remove(key);
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
   public void remove(K key, V value)
   {
      List<V> list = map.get(key);
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
   public List<V> get(K key)
   {
      List<V> list = map.get(key);
      if (list == null)
      {
         return Collections.EMPTY_LIST;
      }
      return list;
   }

}
