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
package io.forty11.js;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;

import io.rcktapp.api.Collection;

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

   public void add(Object object)
   {
      objects.add(object);
   }

   public String getString(int i)
   {
      return (String) get(i);
   }

   public JSObject getObject(int i)
   {
      return (JSObject) get(i);
   }

   public JSArray getArray(int i)
   {
      return (JSArray) get(i);
   }

   public Object get(int i)
   {
      return objects.get(i);
   }

   public boolean contains(Object object)
   {
      return objects.contains(object);
   }

   void write(JsonGenerator json, HashSet visited) throws Exception
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
            ((JSObject) obj).write(json, visited);
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
            json.writeString(obj + "");
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

   public int length()
   {
      return objects.size();
   }

}
