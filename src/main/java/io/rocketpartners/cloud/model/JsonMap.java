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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.rocketpartners.cloud.utils.Utils;

public class JsonMap implements Map<String, Object>
{
   LinkedHashMap<String, Property> properties = new LinkedHashMap();

   public JsonMap()
   {

   }

   public JsonMap(Object... nvPairs)
   {
      for (int i = 0; i < nvPairs.length - 1; i += 2)
      {
         if (i == 0 && (nvPairs[i] instanceof Map && !(nvPairs[i] instanceof JsonMap)))
            throw new RuntimeException("Incorrect constructor called.  Should have called JSMap(Map)");

         put(nvPairs[i] + "", nvPairs[i + 1]);
      }
   }

   public JsonMap(Map map)
   {
      for (Object key : map.keySet())
      {
         put(key + "", map.get(key));
      }
   }

   public boolean isArray()
   {
      return false;
   }

   public JsonMap getMap(String name)
   {
      return (JsonMap) get(name);
   }

   public JsonArray getArray(String name)
   {
      return (JsonArray) get(name);
   }

   public String getString(String name)
   {
      Object value = get(name);
      if (value != null)
         return value.toString();
      return null;
   }

   public int getInt(String name)
   {
      return findInt(name);
   }

   public boolean getBoolean(String name)
   {
      return findBoolean(name);
   }

   public String findString(String path)
   {
      Object found = find(path);
      if (found != null)
         return found.toString();

      return null;
   }

   public int findInt(String path)
   {
      Object found = find(path);
      if (found != null)
         return Utils.atoi(found);

      return -1;
   }

   public boolean findBoolean(String path)
   {
      Object found = find(path);
      if (found != null)
         return Utils.atob(found);

      return false;
   }

   public JsonMap findMap(String path)
   {
      return (JsonMap) find(path);
   }

   public JsonArray findArray(String path)
   {
      return (JsonArray) find(path);
   }

   public Object find(String path)
   {
      List<String> props = Utils.explode("\\.", path);

      Object obj = this;
      for (String prop : props)
      {
         if (obj == null)
            break;
         obj = ((JsonMap) obj).get(prop);
      }
      return obj;
   }

   public List collect(String pathStr)
   {
      List<String> path = Utils.explode("\\.", pathStr);
      return collect(path, new ArrayList());
   }

   protected List collect(List<String> path, List collected)
   {
      String nextSegment = path.get(0);

      if ("*".equals(nextSegment))
      {
         if (path.size() == 1)
         {
            collected.addAll(values());
         }
         else
         {
            List<String> nextPath = path.subList(1, path.size());
            for (Object value : values())
            {
               if (value instanceof JsonMap)
               {
                  ((JsonMap) value).collect(nextPath, collected);
               }
            }
         }
      }
      else
      {
         Object found = get(nextSegment);
         if (found != null)
         {
            if (path.size() == 1)
            {
               collected.add(found);
            }
            else if (found instanceof JsonMap)
            {
               ((JsonMap) found).collect(path.subList(1, path.size()), collected);
            }
         }
      }

      return collected;
   }

   @Override
   public Object get(Object name)
   {
      if (name == null)
         return null;

      Property p = getProperty(name.toString());
      if (p != null)
         return p.getValue();

      return null;
   }

   @Override
   public Object put(String name, Object value)
   {
      Property prop = properties.put(name.toLowerCase(), new Property(name, value));
      return prop;
   }

   public Property getProperty(String name)
   {
      return properties.get(name.toLowerCase());
   }

   @Override
   public boolean containsKey(Object name)
   {
      if (name == null)
         return false;

      return properties.containsKey(name.toString().toLowerCase());
   }

   @Override
   public Object remove(Object name)
   {
      if (name == null)
         return null;

      Property old = removeProperty(name.toString());
      return old != null ? old.getValue() : old;
   }

   @Override
   public Set<String> keySet()
   {
      //properties.getKeySet contains the lower case versions.
      LinkedHashSet keys = new LinkedHashSet();
      for (String key : properties.keySet())
      {
         Property p = properties.get(key);
         keys.add(p.getName());
      }
      return keys;
   }

   public boolean hasProperty(String name)
   {
      Property property = properties.get(name.toLowerCase());
      return property != null;
   }

   public List<Property> getProperties()
   {
      return new ArrayList(properties.values());
   }

   public Property removeProperty(String name)
   {
      Property property = properties.get(name.toLowerCase());
      if (property != null)
         properties.remove(name.toLowerCase());

      return property;
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

         if (value instanceof JsonArray)
         {
            map.put(name, ((JsonArray) p.getValue()).asList());
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
      return Utils.toJson((JsonMap) this);
   }

   public String toString(boolean pretty)
   {
      return Utils.toJson((JsonMap) this, pretty, false);
   }

   public String toString(boolean pretty, boolean tolowercase)
   {
      return Utils.toJson((JsonMap) this, pretty, tolowercase);
   }

   @Override
   public int size()
   {
      return properties.size();
   }

   @Override
   public boolean isEmpty()
   {
      return properties.isEmpty();
   }

   @Override
   public boolean containsValue(Object value)
   {
      if (value == null)
         return false;

      for (Property prop : properties.values())
         if (value.equals(prop.getValue()))
            return true;

      return false;
   }

   @Override
   public void clear()
   {
      properties.clear();
   }

   @Override
   public Collection values()
   {
      return asMap().values();
   }

   @Override
   public Set entrySet()
   {
      return asMap().entrySet();
   }

   @Override
   public void putAll(Map map)
   {
      for (Object key : map.keySet())
      {
         put(key.toString(), map.get(key.toString()));
      }
   }

}
