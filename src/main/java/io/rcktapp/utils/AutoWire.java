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
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.forty11.j.J;
import io.forty11.utils.DoubleKeyMap;

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

   public void clear()
   {
      props.clear();
      beans.clear();
   }

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

   public static List<String> sort(java.util.Collection keys)
   {
      List<String> sorted = new ArrayList(keys);
      Collections.sort(sorted, new Comparator<String>()
         {
            @Override
            public int compare(String o1, String o2)
            {
               int count1 = o1.length() - o1.replace(".", "").length();
               int count2 = o2.length() - o2.replace(".", "").length();
               if (count1 != count2)
                  return count1 > count2 ? 1 : -1;

               return o1.compareTo(o2);
            }
         });

      return sorted;
   }

   public void load() throws Exception
   {
      HashMap<String, Map> loaded = new LinkedHashMap();

      for (Object p : props.keySet())
      {
         String key = (String) p;
         if (key.endsWith(".class") || key.endsWith(".className"))
         {
            String name = key.substring(0, key.lastIndexOf("."));
            String cn = (String) props.get(key);
            Object obj = Class.forName(cn).newInstance();

            loaded.put(name, new HashMap());
            beans.put(name, obj);
            //System.out.println(name + "->" + cn);
         }
         if (key.lastIndexOf(".") < 0)
         {
            beans.put(key, cast(props.getProperty(key)));
         }
      }

      List<String> keys = new ArrayList(beans.keySet());
      keys = sort(keys);

      //      for (String key : keys)
      //         System.out.println(key);

      for (String beanName : keys)
      {
         Object obj = beans.get(beanName);
         for (Object p : getKeys(beanName))
         {
            String key = (String) p;

            if (key.endsWith(".class") || key.endsWith(".className"))
               continue;

            if ((key.startsWith(beanName + ".") && key.lastIndexOf(".") == beanName.length()))
            {
               String prop = key.substring(key.lastIndexOf(".") + 1, key.length());
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
                  set(key, obj, prop, value);
               }
            }
         }

         int count = beanName.length() - beanName.replace(".", "").length();
         if (count > 0)
         {
            String parentKey = beanName.substring(0, beanName.lastIndexOf("."));
            String propKey = beanName.substring(beanName.lastIndexOf(".") + 1);
            if (beans.containsKey(parentKey))
            {
               //               Object parent = beans.get(parentKey);
               //               System.out.println(parent);
            }
            else if (count > 1)
            {
               String mapKey = propKey;
               propKey = parentKey.substring(parentKey.lastIndexOf(".") + 1);
               parentKey = parentKey.substring(0, parentKey.lastIndexOf("."));

               Object parent = beans.get(parentKey);
               if (parent != null)
               {
                  Field field = getField(propKey, parent.getClass());
                  if (field != null)
                  {
                     if (Map.class.isAssignableFrom(field.getType()))
                     {
                        Map map = (Map) field.get(parent);
                        if (!map.containsKey(mapKey))
                           map.put(mapKey, obj);
                     }
                     //                     else if (java.util.Collection.class.isAssignableFrom(field.getType()))
                     //                     {
                     //                        java.util.Collection list = (java.util.Collection) field.get(parent);
                     //                        if (!list.contains(obj))
                     //                        {
                     //                           System.out.println("need to add it?");
                     //                        }
                     //                     }
                     //                     else
                     //                     {
                     //                        System.out.println("asdf?");
                     //                     }
                  }
                  else
                  {
                     //System.err.println("Field is not a map: " + beanName + " - " + field);
                  }
               }
               else
               {
                  System.err.println("Missing parent for map compression: " + beanName);
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

   public void set(String key, Object obj, String prop, String value) throws Exception
   {
      Method setter = getMethod(obj.getClass(), "set" + Character.toUpperCase(prop.charAt(0)) + prop.substring(1, prop.length()));
      if (setter == null)
      {
         setter = getMethod(obj.getClass(), "add" + Character.toUpperCase(prop.charAt(0)) + prop.substring(1, prop.length()));
         if (setter == null && prop.endsWith("s"))
            setter = getMethod(obj.getClass(), "add" + Character.toUpperCase(prop.charAt(0)) + prop.substring(1, prop.length() - 1));
      }

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
         else
         {
            System.out.println("Can't map: " + key + " = " + value);
         }

      }
   }

   //   public void set(String key, Object obj, String prop, String mapKey, Object value) throws Exception
   //   {
   //
   //      Field field = getField(prop, obj.getClass());
   //      if (field != null)
   //      {
   //         Class type = field.getType();
   //
   //         if (type.isAssignableFrom(value.getClass()))
   //         {
   //            field.set(obj, value);
   //         }
   //         else if (Collection.class.isAssignableFrom(type))
   //         {
   //            Method adder = getMethod(obj.getClass(), "add" + Character.toUpperCase(prop.charAt(0)) + prop.substring(1, (prop.endsWith("s") ? prop.length() - 1 : prop.length())));
   //            if (adder != null)
   //            {
   //               adder.invoke(obj, value);
   //            }
   //            else
   //            {
   //               ((Collection) field.get(obj)).add(value);
   //            }
   //         }
   //         else if (Map.class.isAssignableFrom(type))
   //         {
   //            System.out.println("asdasdf");
   //            //               Map map = (Map) cast(value, type);
   //            //               ((Map) field.get(obj)).putAll(map);
   //         }
   //      }
   //      else
   //      {
   //         System.out.println("Can't map: " + key + " = " + value);
   //      }
   //
   //   }

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

   public <T> List<T> getBeans(Class<T> type)
   {
      List found = new ArrayList();
      for (Object bean : beans.values())
      {
         if (type.isAssignableFrom(bean.getClass()))
            found.add(bean);
      }
      return found;
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
         Object o = getBean(str);
         if (o != null && type.isAssignableFrom(o.getClass()))
            return (T) o;

         log.info("NO Match " + str + " - class " + type.getName());
      }

      return null;
   }

   public static interface Namer
   {
      public String getName(Object o) throws Exception;
   }

   public static interface Includer
   {
      public boolean include(Field field);
   }

   public static Properties encode(Namer namer, Includer includer, Object... objects) throws Exception
   {
      Properties props = new Properties();
      Map names = new HashMap();
      DoubleKeyMap defaults = new DoubleKeyMap();

      for (Object object : objects)
      {
         encode(object, props, namer, includer, names, defaults);
      }
      return props;
   }

   static String encode(Object object, Properties props, Namer namer, Includer includer, Map<Object, String> names, DoubleKeyMap defaults) throws Exception
   {
      try
      {
         if (WRAPPER_TYPES.contains(object.getClass()))
            return object + "";

         String name = getName(object, namer, names);

         if (props.containsKey(name + ".class"))
            return name;

         props.put(name + ".class", object.getClass().getName());

         List<Field> fields = J.getFields(object.getClass());

         if (!defaults.containsKey(object.getClass()))
         {
            for (Field field : fields)
            {
               if (Modifier.isTransient(field.getModifiers()))
                  continue;

               if (Modifier.isStatic(field.getModifiers()))
                  continue;

               Object clean = object.getClass().newInstance();

               Object defaultValue = field.get(clean);
               if (defaultValue != null && WRAPPER_TYPES.contains(defaultValue.getClass()))
                  defaults.put(object.getClass(), field.getName(), defaultValue);
            }
         }

         for (Field field : fields)
         {
            if (Modifier.isTransient(field.getModifiers()))
               continue;

            if (Modifier.isStatic(field.getModifiers()))
               continue;

            if (!includer.include(field))
               continue;

            Object value = field.get(object);

            String fieldKey = name + "." + field.getName();
            if (value != null)
            {
               if (value.getClass().isArray())
                  value = Arrays.asList(value);

               if (value instanceof Collection)
               {
                  if (((Collection) value).size() == 0)
                     continue;

                  List values = new ArrayList();
                  for (Object child : ((Collection) value))
                  {
                     String childKey = encode(child, props, namer, includer, names, defaults);
                     values.add(childKey);
                  }
                  props.put(fieldKey, J.implode(",", values));
               }
               else if (value instanceof Map)
               {
                  Map map = (Map) value;
                  if (map.size() == 0)
                     continue;

                  for (Object mapKey : map.keySet())
                  {
                     String encodedKey = encode(mapKey, props, namer, includer, names, defaults);
                     String encodedValue = encode(map.get(mapKey), props, namer, includer, names, defaults);
                     props.put(fieldKey + "." + encodedKey, encodedValue);
                  }
               }
               else
               {
                  if (WRAPPER_TYPES.contains(value.getClass()))
                  {
                     Object defaultVal = defaults.get(object.getClass(), field.getName());
                     if (defaultVal != null && defaultVal.equals(value))
                        continue;
                  }
                  else if (!includer.include(field))
                     continue;

                  props.put(fieldKey, encode(value, props, namer, includer, names, defaults));
               }
            }

         }
         return name;
      }
      catch (Exception ex)
      {
         System.err.println("Error encoding " + object.getClass().getName());
         throw ex;
      }
   }

   public static String getName(Object object, Namer namer, Map<Object, String> names) throws Exception
   {
      if (names.containsKey(object))
         return names.get(object);

      if (namer != null)
      {
         String name = namer.getName(object);
         if (name != null)
         {
            names.put(object, name);
            return name;
         }
      }

      String name = "";

      Field nameField = J.getField("name", object.getClass());
      if (nameField != null)
      {
         name = nameField.get(object) + "";
      }

      name = "_" + object.getClass().getSimpleName() + "_" + name + "_" + names.size();
      names.put(object, name);
      return name;
   }

   private static final Set<Class<?>> WRAPPER_TYPES = getWrapperTypes();

   private static Set<Class<?>> getWrapperTypes()
   {
      Set<Class<?>> ret = new HashSet<Class<?>>();
      ret.add(Boolean.class);
      ret.add(Character.class);
      ret.add(Byte.class);
      ret.add(Short.class);
      ret.add(Integer.class);
      ret.add(Long.class);
      ret.add(Float.class);
      ret.add(Double.class);
      ret.add(Void.class);
      ret.add(String.class);
      return ret;
   }
}
