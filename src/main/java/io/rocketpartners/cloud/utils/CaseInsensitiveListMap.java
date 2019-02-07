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
package io.rocketpartners.cloud.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.map.CaseInsensitiveMap;

/**
 * @author wells.burke
 */
public class CaseInsensitiveListMap<K, V>
{
   Map<K, List<V>> map = new CaseInsensitiveMap();

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
