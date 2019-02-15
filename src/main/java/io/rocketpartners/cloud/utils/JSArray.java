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
package io.rocketpartners.cloud.utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonGenerator;

public class JSArray extends JSObject
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

   public Object get(int index)
   {
      return objects.get(index);
   }

   public Object get(String index)
   {
      return get(Integer.parseInt(index.trim()));
   }

   public Object set(int index, Object o)
   {
      return objects.set(index, o);
   }

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

   public Object remove(String index)
   {
      return remove(Integer.parseInt(index.trim()));
   }

   public void add(Object object)
   {
      objects.add(object);
   }

   public String getString(int index)
   {
      return (String) get(index);
   }

   public JSObject getObject(int index)
   {
      return (JSObject) get(index);
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
   void write(JsonGenerator json, HashSet visited, boolean lowercaseNames) throws Exception
   {
      json.writeStartArray();
      for (Object obj : objects)
      {
         if (obj == null)
         {
            json.writeNull();
         }
         else if (obj instanceof JSObject)
         {
            ((JSObject) obj).write(json, visited, lowercaseNames);
         }
         else if (obj instanceof BigDecimal)
         {
            json.writeNumber((BigDecimal) obj);
         }
         else if (obj instanceof Double)
         {
            json.writeNumber((Double) obj);
         }
         else if (obj instanceof Float)
         {
            json.writeNumber((Float) obj);
         }
         else if (obj instanceof Integer)
         {
            json.writeNumber((Integer) obj);
         }
         else if (obj instanceof Long)
         {
            json.writeNumber((Long) obj);
         }
         else if (obj instanceof BigDecimal)
         {
            json.writeNumber((BigDecimal) obj);
         }
         else if (obj instanceof BigDecimal)
         {
            json.writeNumber((BigDecimal) obj);
         }
         else if (obj instanceof Boolean)
         {
            json.writeBoolean((Boolean) obj);
         }
         else
         {
            json.writeString(JS.encodeString(obj + ""));
         }
      }
      json.writeEndArray();
   }

   public void sort(final String key)
   {
      Collections.sort(objects, new Comparator<JSObject>()
         {
            @Override
            public int compare(JSObject o1, JSObject o2)
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

   /**
    * @return the objects
    */
   public List getObjects()
   {
      return objects;
   }

   /**
    * @param objects the objects to set
    */
   public void setObjects(List objects)
   {
      this.objects = objects;
   }

   public List asList()
   {
      return new ArrayList(objects);
   }
   
   public Stream stream()
   {
      return asList().stream();
   }

   public int length()
   {
      return objects.size();
   }

}
