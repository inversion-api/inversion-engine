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
package io.inversion.cloud.utils;

import io.inversion.cloud.model.Path;
import org.apache.commons.collections4.map.MultiKeyMap;

import java.io.ByteArrayInputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.*;

public class Wirer
{
   @Retention(RetentionPolicy.RUNTIME)
   @Target(ElementType.FIELD)
   public @interface Ignore {
      //public String value() default "";
   }

   Properties          props = new Properties();
   TreeSet<String>          propKeys = new TreeSet<String>();

   Map<String, Object> beans = new HashMap();

   //designed to be overridden
   public void onLoad(String name, Object bean, Map<String, Object> properties) throws Exception
   {

   }

   //designed to be overridden
   public boolean handleProp(Object bean, String prop, String value) throws Exception
   {
      return false;
   }

   public void clear()
   {
      props.clear();
      beans.clear();
      propKeys.clear();
   }

   public void add(Properties props)
   {
      this.props.putAll(props);
      this.propKeys.addAll((Set)props.keySet());
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
         Utils.rethrow(ex);
      }
      add(props);
   }

   public void add(String key, String value)
   {
      props.put(key, value);
      propKeys.add(key);
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
         //System.out.p("using syso prop for key: " + name);
      }
      else
      {
         value = System.getenv(name);
         if (value != null)
         {
            //log.info("using env var for key: " + name);
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
      Set<String> keys = new HashSet<String>();
      String beanPrefix = beanName + ".";
      SortedSet<String> keySet = propKeys.tailSet(beanPrefix);
      for (String key : keySet)
      {
         if (!key.startsWith(beanPrefix))
         {
            break;
         }
         if (!(key.endsWith(".class") || key.endsWith(".className")))
         {
            if (!keys.contains(beanName))
               keys.add(key);
         }
      }

      for (Object p : System.getProperties().keySet())
      {
         String key = (String) p;
         if (key.startsWith(beanPrefix) && !(key.endsWith(".class") || key.endsWith(".className")))
         {
            if (!keys.contains(beanName))
               keys.add(key);
         }
      }

      for (Object p : System.getenv().keySet())
      {
         String key = (String) p;
         if (key.startsWith(beanPrefix) && !(key.endsWith(".class") || key.endsWith(".className")))
         {
            if (!keys.contains(beanName))
               keys.add(key);
         }
      }

      return new ArrayList(keys);
   }

   /**
    * Sorts based on the number of "." characters first and then
    * based on the string value.
    * 
    * @param keys
    * @return
    */
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

   /**
    * Four step process
    * 1. Instantiate all beans
    * 2. Set primitiave types on all beans
    * 3. Set object types on all beans
    * 4. Path compression 
    * 
    * @throws Exception
    */
   public void load() throws Exception
   {
      HashMap<String, Map> loaded = new LinkedHashMap();

      //FIRST STEP
      // - instantiate all beans

      for (Object p : props.keySet())
      {
         String key = (String) p;
         if (key.endsWith(".class") || key.endsWith(".className"))
         {
            String name = key.substring(0, key.lastIndexOf("."));
            String cn = (String) props.get(key);
            Object obj = null;
            try
            {
               obj = Class.forName(cn).newInstance();
            }
            catch (Exception ex)
            {
               System.err.println("Error instantiating class: '" + cn + "'");
               throw ex;
            }

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

      //LOOP THROUGH TWICE.  
      // - First loop, set atomic props
      // - Second loop, set bean props

      for (int i = 0; i <= 1; i++)
      {
         boolean isFirstPassSoLoadOnlyPrimitives = i == 0;

         for (String beanName : keys)
         {
            Object obj = beans.get(beanName);
            List beanKeys = getKeys(beanName);
            for (Object p : beanKeys)
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

                  //if (value != null && (value.length() == 0 || "null".equals(value)))
                  if ("null".equals(value))
                  {
                     value = null;
                  }

                  boolean valueIsBean = (!(value.equals("") || value.equals("null")) && (beans.containsKey(value) || beans.containsKey(Utils.explode(",", value).get(0))));

                  if (isFirstPassSoLoadOnlyPrimitives && valueIsBean)
                  {
                     continue;
                  }
                  else if (!isFirstPassSoLoadOnlyPrimitives && !valueIsBean)
                  {
                     continue;
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

                        if (beans.containsKey(value) && type.isAssignableFrom(beans.get(value).getClass()))
                        {
                           field.set(obj, beans.get(value));
                        }
                        else if (java.util.Collection.class.isAssignableFrom(type))
                        {
                           java.util.Collection list = (java.util.Collection) cast(key, value, type, field);
                           ((java.util.Collection) field.get(obj)).addAll(list);
                        }
                        else if (Map.class.isAssignableFrom(type))
                        {
                           Map map = (Map) cast(key, value, type, null);
                           ((Map) field.get(obj)).putAll(map);
                        }
                        else
                        {
                           field.set(obj, cast(key, value, type, null));
                        }
                     }
                     else
                     {
                        System.out.println("Can't map: " + key + " = " + value);
                     }

                  }
               }
            }
         }
      }

      //THIRD STEP
      // - Perform implicit setters based on nested paths of keys

      for (String beanName : keys)
      {
         Object obj = beans.get(beanName);
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
                     else if (java.util.Collection.class.isAssignableFrom(field.getType()))
                     {
                        //System.err.println("You should consider adding " + parent.getClass().getName() + ".with" + propKey + "in camel case singular or plural form");//a settinger to accomodate property: " + beanName);
                        java.util.Collection list = (java.util.Collection) field.get(parent);
                        if (!list.contains(obj))
                        {
                           list.add(obj);
                        }
                     }
                     else
                     {
                        System.err.println("Unable to set nested value: '" + beanName + "'");
                     }
                  }
                  else
                  {
                     System.err.println("Field is not a mapped: " + beanName + " - " + field);
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

   protected <T> T cast(String key, String stringVal, Class<T> type, Field field) throws Exception
   {
      if (String.class.isAssignableFrom(type))
      {
         return (T) stringVal;
      }
      else if (Path.class.isAssignableFrom(type))
      {
         return (T) new Path(stringVal);
      }
      else if (boolean.class.isAssignableFrom(type))
      {
         stringVal = stringVal.toLowerCase();
         return (T) (Boolean) (stringVal.equals("true") || stringVal.equals("t") || stringVal.equals("1"));
      }
      else if (byte.class.isAssignableFrom(type))
      {
         return (T) (Byte) Byte.parseByte(stringVal);
      }
      else if (char.class.isAssignableFrom(type))
      {
         return (T) (Character) stringVal.charAt(0);
      }
      else if (int.class.isAssignableFrom(type))
      {
         return (T) (Integer) Integer.parseInt(stringVal);
      }
      else if (long.class.isAssignableFrom(type))
      {
         return (T) (Long) Long.parseLong(stringVal);
      }
      else if (float.class.isAssignableFrom(type))
      {
         return (T) (Float) Float.parseFloat(stringVal);
      }
      else if (double.class.isAssignableFrom(type))
      {
         return (T) (Double) Double.parseDouble(stringVal);
      }
      else if (type.isArray() || java.util.Collection.class.isAssignableFrom(type))
      {
         Class subtype = null;
         if (type.isArray())
         {
            subtype = getArrayElementClass(type);
         }

         if (subtype == null && field != null)
            subtype = (Class) ((((ParameterizedType) field.getGenericType()).getActualTypeArguments())[0]);

         //if (subtype == null)
         //   subtype = String.class;

         java.util.Collection list = new ArrayList();
         String[] parts = stringVal.split(",");
         for (String part : parts)
         {
            part = part.trim();

            Object val = beans.containsKey(part) ? beans.get(part) : part;

            if (val != null && subtype != null && !subtype.isAssignableFrom(val.getClass()))
               val = cast(key, val + "", subtype, null);

            list.add(val);
         }

         if (type.isArray())
            return (T) list.toArray((Object[]) Array.newInstance(subtype, list.size()));

         return (T) list;
      }
      else if (Map.class.isAssignableFrom(type))
      {
         Map map = new HashMap();
         String[] parts = stringVal.split(",");
         for (String part : parts)
         {
            Object val = beans.get(part);
            map.put(part, val);
         }
         return (T) map;
      }
      else
      {
         Object o = getBean(stringVal);
         if (o != null && type.isAssignableFrom(o.getClass()))
            return (T) o;
      }

      if (stringVal != null)
         throw new RuntimeException("Error setting '" + key + "=" + stringVal + "'.  You must add support for type " + type + " into the Configurator");

      return null;
   }

   public static Class getArrayElementClass(Class arrayClass) throws ClassNotFoundException
   {
      Class subtype = null;
      String typeStr = arrayClass.toString();

      if (typeStr.startsWith("class [Z"))
      {
         subtype = boolean.class;
      }
      else if (typeStr.startsWith("class [B"))
      {
         subtype = byte.class;
      }
      else if (typeStr.startsWith("class [C"))
      {
         subtype = char.class;
      }
      else if (typeStr.startsWith("class [I"))
      {
         subtype = int.class;
      }
      else if (typeStr.startsWith("class [J"))
      {
         subtype = long.class;
      }
      else if (typeStr.startsWith("class [F"))
      {
         subtype = float.class;
      }
      else if (typeStr.startsWith("class [D"))
      {
         subtype = double.class;
      }
      else //if (typeStr.startsWith("class ["))
      {
         subtype = Class.forName(typeStr.substring(typeStr.indexOf("[") + 2, typeStr.indexOf(";")));
      }
      return subtype;
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
      MultiKeyMap<String, String> defaults = new MultiKeyMap();

      for (Object object : objects)
      {
         encode(object, props, namer, includer, names, defaults);
      }
      return props;
   }

   static String encode(Object object, Properties props, Namer namer, Includer includer, Map<Object, String> names, MultiKeyMap defaults) throws Exception
   {
      try
      {
         if (WRAPPER_TYPES.contains(object.getClass()))
            return object + "";

         String name = getName(object, namer, names);

         if (props.containsKey(name + ".class"))
            return name;

         props.put(name + ".class", object.getClass().getName());

         List<Field> fields = Utils.getFields(object.getClass());

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

               if (value instanceof java.util.Collection)
               {
                  if (((java.util.Collection) value).size() == 0)
                     continue;

                  List values = new ArrayList();
                  for (Object child : ((java.util.Collection) value))
                  {
                     String childKey = encode(child, props, namer, includer, names, defaults);
                     values.add(childKey);
                  }
                  props.put(fieldKey, Utils.implode(",", values));
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
         System.err.println("Error encoding " + object.getClass().getName() + " - " + ex.getMessage());
         ex.printStackTrace();
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

      Field nameField = Utils.getField("name", object.getClass());
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
