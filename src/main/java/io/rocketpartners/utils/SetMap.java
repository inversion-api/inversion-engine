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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author wells.burke
 */
public class SetMap<K, V>
{
   Map<K, Set<V>> map = new HashMap();

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
      Set<V> set = map.get(key);
      if (set == null)
      {
         set = new LinkedHashSet();
         map.put(key, set);
      }

      for (V value : values)
      {
         if (!set.contains(value))
            set.add(value);
      }
   }

   public Object put(K key, V value)
   {
      Set<V> set = map.get(key);
      if (set == null)
      {
         set = new LinkedHashSet();
         map.put(key, set);
      }

      if (!set.contains(value))
         set.add(value);

      return null;
   }

   public Set<K> keySet()
   {
      return map.keySet();
   }

   public Collection<V> values()
   {
      Collection<V> values = new LinkedHashSet();
      for (Set set : map.values())
      {
         values.addAll(set);
      }
      return values;
   }

   public Set<V> remove(K key)
   {
      Set<V> removed = map.remove(key);
      if (removed == null)
         removed = Collections.EMPTY_SET;

      return removed;
   }

   /**
    * Removes the value from the set if the
    * set exists
    * 
    * @param key
    * @param value
    */
   public void remove(K key, V value)
   {
      Set<V> set = map.get(key);
      if (set != null)
      {
         while (set.contains(value))
         {
            set.remove(value);
         }

         if (set.size() == 0)
         {
            remove(key);
         }
      }
   }

   /**
    * Overridden to return empty set instead of null
    */
   public Set<V> get(K key)
   {
      Set<V> set = map.get(key);
      if (set == null)
      {
         return Collections.EMPTY_SET;
      }
      return set;
   }

}
