/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.cloud.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class JSArray extends JSNode implements Iterable
{
   List objects = new ArrayList();

   public JSArray(Object... objects)
   {
      if (objects != null && objects.length == 1 && objects[0].getClass().isArray())
      {
         objects = (Object[]) objects[0];
      }
      else if (objects != null && objects.length == 1 && java.util.Collection.class.isAssignableFrom(objects[0].getClass()))
      {
         objects = ((java.util.Collection) objects[0]).toArray();
      }

      for (int i = 0; objects != null && i < objects.length; i++)
         add(objects[i]);
   }

   public boolean isArray()
   {
      return true;
   }

   public Object get(int index)
   {
      if (index >= objects.size())
         return null;
      return objects.get(index);
   }

   public Set<String> keySet()
   {
      //TODO make more efficient!!!!
      LinkedHashSet set = new LinkedHashSet();
      for (int i = 0; i < objects.size(); i++)
      {
         set.add(i + "");
      }
      return set;
   }

   public JSNode getNode(int index)
   {
      return (JSNode) get(index + "");
   }

   @Override
   public Object get(Object index)
   {
      return get(Integer.parseInt(index.toString().trim()));
   }

   public Object set(int index, Object o)
   {
      while (objects.size() < index + 1)
         objects.add(null);

      return objects.set(index, o);
   }

   @Override
   public Object put(String index, Object value)
   {
      return set(Integer.parseInt(index.trim()), value);
   }

   public Object put(int index, Object value)
   {
      return set(index, value);
   }

   public Object remove(int index)
   {
      return objects.remove(index);
   }

   @Override
   public Object remove(Object index)
   {
      return remove(Integer.parseInt(index.toString().trim()));
   }

   public void addAll(JSArray array)
   {
      objects.addAll(array.asList());
   }

   public void add(int index, Object object)
   {
      objects.add(index, object);
   }

   public void add(Object object)
   {
      objects.add(object);
   }

   public String getString(int index)
   {
      return (String) get(index);
   }

   public JSNode getObject(int index)
   {
      return (JSNode) get(index);
   }

   public void setObject(int index, Object o)
   {
      objects.set(index, o);
   }

   public JSArray getArray(int index)
   {
      return (JSArray) get(index);
   }

   public boolean contains(Object object)
   {
      return objects.contains(object);
   }

   @Override
   public boolean isEmpty()
   {
      return objects.isEmpty();
   }

   @Override
   public void clear()
   {
      objects.clear();
   }

   @Override
   public int size()
   {
      return objects.size();
   }

   public int length()
   {
      return objects.size();
   }

   public void sort(final String key)
   {
      Collections.sort(objects, new Comparator<JSNode>()
         {
            @Override
            public int compare(JSNode o1, JSNode o2)
            {
               Object val1 = o1.get(key);
               Object val2 = o2.get(key);
               if (val1 == null)
                  return -1;
               if (val2 == null)
                  return 1;

               return val1.toString().compareTo(val2.toString());
            }
         });
   }

   public List asList()
   {
      return new ArrayList(objects);
   }

   public List<JSNode> asNodeList()
   {
      return new ArrayList(objects);
   }

   public List<JSArray> asArrayList()
   {
      return new ArrayList(objects);
   }

   @Override
   public Iterator iterator()
   {
      return asList().iterator();
   }

   public Stream stream()
   {
      return asList().stream();
   }

   @Override
   public Collection values()
   {
      return asList();
   }

}
