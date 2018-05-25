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
package io.rcktapp.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class AutoWire
{
   Map<String, Object> modules = new HashMap();

   public void load(Properties props) throws Exception
   {
      HashMap<String, Map> loaded = new LinkedHashMap();

      for (Object p : props.keySet())
      {
         String key = (String) p;
         if (key.endsWith(".class"))
         {
            String name = key.substring(0, key.indexOf("."));
            String cn = (String) props.get(key);
            Object obj = Class.forName(cn).newInstance();

            loaded.put(name, new HashMap());
            modules.put(name, obj);
         }
         if (key.indexOf(".") < 0)
         {
            modules.put(key, convert(props.getProperty(key)));
         }
      }

      for (String moduleName : loaded.keySet())
      {
         Object obj = modules.get(moduleName);
         for (Object p : props.keySet())
         {
            String key = (String) p;

            if (key.startsWith(moduleName + ".") && !key.endsWith(".class"))
            {
               String prop = key.substring(key.indexOf(".") + 1, key.length());
               String value = props.getProperty(key);

               if (value != null)
                  value = value.trim();

               if (value != null && (value.length() == 0 || "null".equals(value)))
               {
                  value = null;
               }

               if (handleProp(obj, prop, value))
               {
                  //do nothing, already handled
               }
               else
               {
                  Field field = getField(prop, obj.getClass());
                  if (field != null)
                  {
                     Class type = field.getType();

                     if (modules.containsKey(value) && type.isAssignableFrom(modules.get(value).getClass()))
                     {
                        field.set(obj, modules.get(value));
                     }
                     else if (Collection.class.isAssignableFrom(type))
                     {
                        Collection list = (Collection) field.get(obj);

                        String[] parts = value.split(",");
                        for (String part : parts)
                        {
                           if (modules.containsKey(part))
                           {
                              list.add(modules.get(part));
                           }
                           else
                           {
                              list.add(part);
                           }
                        }
                     }
                     else if (Map.class.isAssignableFrom(type))
                     {
                        Map map = (Map) field.get(obj);
                        String[] parts = value.split(",");
                        for (String part : parts)
                        {
                           Object val = modules.get(part);
                           map.put(part, val);
                        }
                     }
                     else
                     {
                        field.set(obj, convert(value, type));
                     }
                  }
                  else
                  {
                     throw new RuntimeException("Unmappable property: " + key);
                  }
               }
            }
         }
      }

      for (String name : loaded.keySet())
      {
         Object module = modules.get(name);
         Map loadedPros = loaded.get(name);
         onLoad(name, module, loadedPros);
      }

   }

   public void onLoad(String name, Object object, Map<String, Object> properties) throws Exception
   {

   }

   public boolean handleProp(Object module, String prop, String value) throws Exception
   {
      return false;
   }

   public Object getModule(String key)
   {
      return modules.get(key);
   }

   public List getModules(Class type)
   {
      List matches = new ArrayList();
      for (Object module : modules.values())
      {
         if (type.isAssignableFrom(module.getClass()))
            matches.add(module);
      }
      return matches;
   }

   public static Method getMethod(Class clazz, String name)
   {
      for (Method m : clazz.getMethods())
      {
         if (m.getName().equalsIgnoreCase(name))
            return m;
      }
      return null;
   }

   protected Field getField(String fieldName, Class clazz)
   {
      if (fieldName == null || clazz == null)
      {
         return null;
      }

      Field[] fields = clazz.getDeclaredFields();
      for (int i = 0; i < fields.length; i++)
      {
         if (fields[i].getName().equals(fieldName))
         {
            Field field = fields[i];
            field.setAccessible(true);
            return field;
         }
      }

      if (clazz.getSuperclass() != null && !clazz.equals(clazz.getSuperclass()))
      {
         return getField(fieldName, clazz.getSuperclass());
      }

      return null;
   }

   protected Object convert(String str)
   {
      if ("true".equalsIgnoreCase(str))
         return true;

      if ("false".equalsIgnoreCase(str))
         return true;

      if (str.matches("\\d+"))
         return Integer.parseInt(str);

      return str;
   }

   protected Object convert(String str, Class type)
   {
      if (String.class.isAssignableFrom(type))
      {
         return str;
      }
      else if (boolean.class.isAssignableFrom(type))
      {
         str = str.toLowerCase();
         return str.equals("true") || str.equals("t") || str.equals("1");
      }
      else if (int.class.isAssignableFrom(type))
      {
         return Integer.parseInt(str);
      }
      else if (long.class.isAssignableFrom(type))
      {
         return Long.parseLong(str);
      }
      else if (float.class.isAssignableFrom(type))
      {
         return Float.parseFloat(str);
      }
      else
      {
         System.out.println("NO Match " + str + " - class " + type.getName());

         Object o = getModule(str);
         if (o != null && type.isAssignableFrom(o.getClass()))
            return o;
      }

      return null;
   }

}
