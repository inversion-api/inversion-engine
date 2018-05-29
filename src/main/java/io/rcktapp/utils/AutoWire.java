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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package io.rcktapp.utils;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.forty11.j.J;

public class AutoWire
{
   static Logger       log   = LoggerFactory.getLogger(AutoWire.class);
   Properties          props = new Properties();

   Map<String, Object> beans = new HashMap();

   //   public static void wire(Object obj, String props)
   //   {
   //      
   //   }
   //
   //   public static void wire(Object obj, Properties properties)
   //   {
   //      AutoWire w = new AutoWire();
   //      w.add
   //   }

   public void add(Properties props)
   {
      this.props.putAll(props);
   }

   public void add(String propsStr)
   {
      Properties props = new Properties();
      try
      {
         props.load(new ByteArrayInputStream(propsStr.getBytes()));
      }
      catch (Exception ex)
      {
         J.rethrow(ex);
      }
      add(props);
   }

   public void add(String key, String value)
   {
      props.put(key, value);
   }

   public void load(Properties props) throws Exception
   {
      add(props);
      load();
   }

   String getProperty(String name)
   {
      String value = System.getProperty(name);
      if (value != null)
      {
         log.info("using syso prop for key: " + name);
      }
      else
      {
         value = System.getenv(name);
         if (value != null)
         {
            log.info("using env var for key: " + name);
         }
         else
         {
            value = props.getProperty(name);
         }
      }
      return value;
   }

   List<String> getKeys(String beanName)
   {
      List keys = new ArrayList();
      for (Object p : props.keySet())
      {
         String key = (String) p;

         if (key.startsWith(beanName + ".") && !(key.endsWith(".class") || key.endsWith(".className")))
         {
            if (!keys.contains(beanName))
               keys.add(key);
         }
      }

      for (Object p : System.getProperties().keySet())
      {
         String key = (String) p;
         if (key.startsWith(beanName + ".") && !(key.endsWith(".class") || key.endsWith(".className")))
         {
            if (!keys.contains(beanName))
               keys.add(key);
         }
      }

      for (Object p : System.getenv().keySet())
      {
         String key = (String) p;
         if (key.startsWith(beanName + ".") && !(key.endsWith(".class") || key.endsWith(".className")))
         {
            if (!keys.contains(beanName))
               keys.add(key);
         }
      }

      return keys;
   }

   public void load() throws Exception
   {
      HashMap<String, Map> loaded = new LinkedHashMap();

      for (Object p : props.keySet())
      {
         String key = (String) p;
         if (key.endsWith(".class") || key.endsWith(".className"))
         {
            String name = key.substring(0, key.indexOf("."));
            String cn = (String) props.get(key);
            Object obj = Class.forName(cn).newInstance();

            loaded.put(name, new HashMap());
            beans.put(name, obj);
         }
         if (key.indexOf(".") < 0)
         {
            beans.put(key, cast(props.getProperty(key)));
         }
      }

      for (String beanName : beans.keySet())
      {
         Object obj = beans.get(beanName);
         for (Object p : getKeys(beanName))
         {
            String key = (String) p;

            if (key.startsWith(beanName + ".") && !(key.endsWith(".class") || key.endsWith(".className")))
            {
               String prop = key.substring(key.indexOf(".") + 1, key.length());
               String value = getProperty(key);

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
                  Method setter = getMethod(obj.getClass(), "set" + Character.toUpperCase(prop.charAt(0)) + prop.substring(1, prop.length()));
                  if (setter == null)
                     setter = getMethod(obj.getClass(), "add" + Character.toUpperCase(prop.charAt(0)) + prop.substring(1, prop.length()));

                  if (setter != null && setter.getParameterTypes().length == 1)
                  {
                     setter.invoke(obj, cast(value, setter.getParameterTypes()[0]));
                  }
                  else
                  {
                     Field field = getField(prop, obj.getClass());
                     if (field != null)
                     {
                        Class type = field.getType();

                        if (beans.containsKey(value) && type.isAssignableFrom(beans.get(value).getClass()))
                        {
                           field.set(obj, beans.get(value));
                        }
                        else if (Collection.class.isAssignableFrom(type))
                        {
                           Collection list = (Collection) cast(value, type);
                           ((Collection) field.get(obj)).addAll(list);
                        }
                        else if (Map.class.isAssignableFrom(type))
                        {
                           Map map = (Map) cast(value, type);
                           ((Map) field.get(obj)).putAll(map);
                        }
                        else
                        {
                           field.set(obj, cast(value, type));
                        }
                     }

                  }
               }
            }
         }
      }

      for (String name : loaded.keySet())
      {
         Object bean = beans.get(name);
         Map loadedPros = loaded.get(name);
         onLoad(name, bean, loadedPros);
      }

   }

   public void onLoad(String name, Object bean, Map<String, Object> properties) throws Exception
   {

   }

   public boolean handleProp(Object bean, String prop, String value) throws Exception
   {
      return false;
   }

   public void putBean(String key, Object bean)
   {
      beans.put(key, bean);
   }

   public Object getBean(String key)
   {
      return beans.get(key);
   }

   public <T> T getBean(Class<T> type)
   {
      for (Object bean : beans.values())
      {
         if (type.isAssignableFrom(bean.getClass()))
            return (T) bean;
      }
      return null;
   }

   public List findBeans(Class type)
   {
      List matches = new ArrayList();
      for (Object bean : beans.values())
      {
         if (type.isAssignableFrom(bean.getClass()))
            matches.add(bean);
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

   protected Object cast(String str)
   {
      if ("true".equalsIgnoreCase(str))
         return true;

      if ("false".equalsIgnoreCase(str))
         return true;

      if (str.matches("\\d+"))
         return Integer.parseInt(str);

      return str;
   }

   protected <T> T cast(String str, Class<T> type)
   {
      if (String.class.isAssignableFrom(type))
      {
         return (T) str;
      }
      else if (boolean.class.isAssignableFrom(type))
      {
         str = str.toLowerCase();
         return (T) (Boolean) (str.equals("true") || str.equals("t") || str.equals("1"));
      }
      else if (int.class.isAssignableFrom(type))
      {
         return (T) (Integer) Integer.parseInt(str);
      }
      else if (long.class.isAssignableFrom(type))
      {
         return (T) (Long) Long.parseLong(str);
      }
      else if (float.class.isAssignableFrom(type))
      {
         return (T) (Float) Float.parseFloat(str);
      }
      else if (Collection.class.isAssignableFrom(type))
      {
         Collection list = new ArrayList();
         String[] parts = str.split(",");
         for (String part : parts)
         {
            part = part.trim();
            if (beans.containsKey(part))
            {
               list.add(beans.get(part));
            }
            else
            {
               list.add(part);
            }
         }
         return (T) list;
      }
      else if (Map.class.isAssignableFrom(type))
      {
         Map map = new HashMap();
         String[] parts = str.split(",");
         for (String part : parts)
         {
            Object val = beans.get(part);
            map.put(part, val);
         }
         return (T) map;
      }
      else
      {
         log.info("NO Match " + str + " - class " + type.getName());

         Object o = getBean(str);
         if (o != null && type.isAssignableFrom(o.getClass()))
            return (T) o;
      }

      return null;
   }

}
