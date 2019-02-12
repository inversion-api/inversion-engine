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
package io.rocketpartners.utils;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class CaseInsensitiveSet<K> implements Set<K>
{
   Set set = new LinkedHashSet();

   public CaseInsensitiveSet()
   {

   }

   public CaseInsensitiveSet(Collection c)
   {
      for (Object o : c)
      {
         add(o);
      }
   }

   @Override
   public int size()
   {
      return set.size();
   }

   @Override
   public boolean isEmpty()
   {
      return set.isEmpty();
   }

   @Override
   public boolean contains(Object o)
   {
      o = o instanceof String ? ((String) o).toLowerCase() : o;
      return set.contains(o);
   }

   @Override
   public Iterator<K> iterator()
   {
      return set.iterator();
   }

   @Override
   public Object[] toArray()
   {
      return set.toArray();
   }

   @Override
   public Object[] toArray(Object[] a)
   {
      return set.toArray(a);
   }

   @Override
   public boolean add(Object e)
   {
      set.add(e instanceof String ? ((String) e).toLowerCase() : e);
      return true;
   }

   @Override
   public boolean remove(Object o)
   {
      o = o instanceof String ? ((String) o).toLowerCase() : o;
      if (set.contains(o))
      {
         set.remove(o);
         return true;
      }
      return false;
   }

   @Override
   public boolean containsAll(Collection c)
   {
      for (Object o : c)
      {
         if (!contains(o))
            return false;
      }
      return true;
   }

   @Override
   public boolean addAll(Collection c)
   {
      boolean modified = false;
      for (Object o : c)
      {
         modified |= add(o);
      }
      return modified;
   }

   @Override
   public boolean retainAll(Collection c)
   {
      throw new RuntimeException("retainAll(Collection c) is not implemented");
   }

   @Override
   public boolean removeAll(Collection c)
   {
      boolean modified = false;
      for (Object o : c)
      {
         modified |= remove(o);
      }
      return modified;
   }

   @Override
   public void clear()
   {
      set.clear();
   }

}
