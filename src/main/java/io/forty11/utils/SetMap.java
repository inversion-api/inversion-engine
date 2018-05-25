/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
 * http://rocketpartners.io
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.forty11.utils;

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
