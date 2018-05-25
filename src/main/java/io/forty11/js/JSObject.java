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

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import io.forty11.j.J;

public class JSObject
{
   LinkedHashMap<String, Property> properties = new LinkedHashMap();

   public JSObject()
   {

   }

   public JSObject(Object... nvPairs)
   {
      for (int i = 0; i < nvPairs.length - 1; i += 2)
      {
         if (i == 0 && nvPairs[i] instanceof Map)
            throw new RuntimeException("Incorrect constructor called.  Should have called JSObject(Map)");

         put(nvPairs[i] + "", nvPairs[i + 1]);
      }
   }

   public JSObject(Map map)
   {
      for (Object key : map.keySet())
      {
         put(key + "", map.get(key));
      }
   }

   public JSObject getObject(String name)
   {
      return (JSObject) get(name);
   }

   public JSArray getArray(String name)
   {
      return (JSArray) get(name);
   }

   public String getString(String name)
   {
      Object value = get(name);
      if (value != null)
         return value.toString();
      return null;
   }

   public Object get(String name)
   {
      Property p = getProperty(name);
      if (p != null)
         return p.getValue();

      return null;
   }

   public Object put(String name, Object value)
   {
      Property prop = putProperty(new Property(name, value));
      return prop;
   }

   public boolean hasProperty(String name)
   {
      return properties.containsKey(name);
   }

   public boolean containsKey(String name)
   {
      return properties.containsKey(name);
   }

   Property putProperty(Property prop)
   {
      String name = prop.getName();
      Object value = prop.getValue();

      //hack to support case insensitivity on property lookup
      for (String key : (List<String>) new ArrayList(properties.keySet()))
      {
         if (key.equalsIgnoreCase(name))
            properties.remove(key);
      }

      properties.put(name, prop);
      return prop;
   }

   public Object remove(String name)
   {
      Property old = removeProperty(name);
      return old != null ? old.getValue() : old;
   }

   public Set<String> keys()
   {
      return properties.keySet();
   }

   public Set<String> keySet()
   {
      return properties.keySet();
   }

   public Property getProperty(String name)
   {
      Property p = properties.get(name);
      if (p == null && properties.size() > 0)
      {
         //hack to support case insensitivity on property lookup
         for (String key : properties.keySet())
         {
            if (key.equalsIgnoreCase(name))
            {
               p = properties.get(key);
               break;
            }
         }
      }
      return p;
   }

   public List<Property> getProperties()
   {
      return new ArrayList(properties.values());
   }

   public Property removeProperty(String name)
   {
      Property p = properties.get(name);
      if (p == null && properties.size() > 0)
      {
         //hack to support case insensitivity on property lookup
         for (String key : properties.keySet())
         {
            if (key.equalsIgnoreCase(name))
            {
               p = properties.get(key);
               break;
            }
         }
      }
      if (p != null)
         properties.remove(p.getName());
      return p;
   }

   public static class Property
   {
      String name  = null;
      Object value = null;

      public Property(String name, Object value)
      {
         super();
         this.name = name;
         this.value = value;
      }

      public String toString()
      {
         return name + " = " + value;
      }

      /**
       * @return the name
       */
      public String getName()
      {
         return name;
      }

      /**
       * @param name the name to set
       */
      public void setName(String name)
      {
         this.name = name;
      }

      /**
       * @return the value
       */
      public Object getValue()
      {
         return value;
      }

      /**
       * @param value the value to set
       */
      public void setValue(Object value)
      {
         this.value = value;
      }
   }

   public Map asMap()
   {
      Map map = new HashMap();
      for (Property p : properties.values())
      {
         String name = p.name;
         Object value = p.value;

         if (value instanceof JSArray)
         {
            map.put(name, ((JSArray) p.getValue()).asList());
         }
         else
         {

            map.put(name, value);
         }
      }
      return map;
   }

   @Override
   public String toString()
   {
      return toString(true);
   }

   public String toString(boolean pretty)
   {
      try
      {
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         JsonGenerator json = new JsonFactory().createGenerator(baos);
         if (pretty)
            json.useDefaultPrettyPrinter();
         write(json, new HashSet());
         json.flush();
         baos.flush();

         return new String(baos.toByteArray());
      }
      catch (Exception ex)
      {
         throw new RuntimeException(ex);
      }
   }

   void write(JsonGenerator json, HashSet visited) throws Exception
   {
      Property href = getProperty("href");

      if (visited.contains(this))
      {
         json.writeStartObject();
         if (href != null)
         {
            json.writeStringField("@link", href.getValue() + "");
         }

         json.writeEndObject();
         return;
      }
      visited.add(this);

      json.writeStartObject();

      if (href != null)
         json.writeStringField("href", href.getValue() + "");

      //for (String key : Collections.sort(new ArrayList(properties.keySet())))
      for (String key : properties.keySet())
      {
         Property p = properties.get(key);
         if (p == href)
            continue;

         if (p.value == null)
         {
            json.writeNullField(p.name);
         }
         else if (p.value instanceof JSObject)
         {
            json.writeFieldName(p.name);
            ((JSObject) p.value).write(json, visited);
         }
         else if (p.value instanceof Date)
         {
            json.writeStringField(p.name, J.formatDate((Date) p.value));
         }
         else if (p.value instanceof BigDecimal)
         {
            json.writeNumberField(p.name, (BigDecimal) p.value);
         }
         else if (p.value instanceof Double)
         {
            json.writeNumberField(p.name, (Double) p.value);
         }
         else if (p.value instanceof Float)
         {
            json.writeNumberField(p.name, (Float) p.value);
         }
         else if (p.value instanceof Integer)
         {
            json.writeNumberField(p.name, (Integer) p.value);
         }
         else if (p.value instanceof Long)
         {
            json.writeNumberField(p.name, (Long) p.value);
         }
         else if (p.value instanceof BigDecimal)
         {
            json.writeNumberField(p.name, (BigDecimal) p.value);
         }
         else if (p.value instanceof BigDecimal)
         {
            json.writeNumberField(p.name, (BigDecimal) p.value);
         }
         else if (p.value instanceof Boolean)
         {
            json.writeBooleanField(p.name, (Boolean) p.value);
         }
         else
         {
            String strVal = p.value + "";
            if ("null".equals(strVal))
            {
               json.writeNullField(p.name);
            }
            else
            {
               json.writeStringField(p.name, strVal);
            }
         }
      }
      json.writeEndObject();
   }

}
