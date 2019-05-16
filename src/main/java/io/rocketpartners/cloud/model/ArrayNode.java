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
package io.rocketpartners.cloud.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public class ArrayNode extends ObjectNode implements Iterable
{
   List objects = new ArrayList();

   public ArrayNode(Object... objects)
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

   public Object get(int index)
   {
      if (index >= objects.size())
         return null;
      return objects.get(index);
   }

   @Override
   public Object get(Object index)
   {
      return get(Integer.parseInt(index.toString().trim()));
   }

   public Object set(int index, Object o)
   {
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

   public void addAll(ArrayNode array)
   {
      objects.addAll(array.asList());
   }

   public void add(Object object)
   {
      objects.add(object);
   }

   public String getString(int index)
   {
      return (String) get(index);
   }

   public ObjectNode getObject(int index)
   {
      return (ObjectNode) get(index);
   }

   public void setObject(int index, Object o)
   {
      objects.set(index, o);
   }

   public ArrayNode getArray(int index)
   {
      return (ArrayNode) get(index);
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
      Collections.sort(objects, new Comparator<ObjectNode>()
         {
            @Override
            public int compare(ObjectNode o1, ObjectNode o2)
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

   @Override
   public Iterator iterator()
   {
      return asList().iterator();
   }

   public Stream stream()
   {
      return asList().stream();
   }

}
