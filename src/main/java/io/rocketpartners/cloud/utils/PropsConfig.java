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
package io.rocketpartners.cloud.utils;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.rocketpartners.cloud.model.AclRule;
import io.rocketpartners.cloud.model.Action;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.ApiException;
import io.rocketpartners.cloud.model.Attribute;
import io.rocketpartners.cloud.model.Collection;
import io.rocketpartners.cloud.model.Column;
import io.rocketpartners.cloud.model.Db;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.model.Entity;
import io.rocketpartners.cloud.model.Index;
import io.rocketpartners.cloud.model.Relationship;
import io.rocketpartners.cloud.model.Rule;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.model.Table;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.PropsConfig.AutoWire.Ignore;
import io.rocketpartners.cloud.utils.PropsConfig.AutoWire.Includer;
import io.rocketpartners.cloud.utils.PropsConfig.AutoWire.Namer;

public class PropsConfig
{
   Logger  log       = LoggerFactory.getLogger(Service.class.getName() + ".configuration");

   boolean destroyed = false;

   Service service   = null;

   public void destroy()
   {
      destroyed = true;
   }

   public synchronized void loadConfg(Service service)
   {
      if (this.service != null)
         return;

      this.service = service;

      try
      {
         Config config = findConfig();

         if (config.files.size() == 0)
            return;

         AutoWire w = new AutoWire();
         w.putBean("snooze", service);
         w.load(config.props);

         loadConfig(config, true, service.isConfigFast());
      }
      catch (Exception e)
      {
         e.printStackTrace();
         throw new RuntimeException("Unable to load snooze configs: " + e.getMessage(), e);
      }

      if (service.getConfigTimeout() > 0 && !service.isConfigFast())
      {
         Thread t = new Thread(new Runnable()
            {
               @Override
               public void run()
               {
                  while (true)
                  {
                     try
                     {
                        Utils.sleep(service.getConfigTimeout());
                        if (destroyed)
                           return;

                        Config config = findConfig();
                        loadConfig(config, false, false);
                     }
                     catch (Throwable t)
                     {
                        log.warn("Error loading config", t);
                     }
                  }
               }
            }, "snooze-config-reloader");

         t.setDaemon(true);
         t.start();
      }
   }

   void loadConfig(Config config, boolean forceReload, boolean fastLoad) throws Exception
   {
      AutoWire wire = new AutoWire()
         {
            @Override
            public void onLoad(String name, Object module, Map<String, Object> props) throws Exception
            {
               Field field = getField("name", module.getClass());
               if (field != null && field.get(module) == null)
                  field.set(module, name);
            }
         };
      wire.load(config.props);
      autoWireApi(wire);

      boolean doLoad = false;

      if (!fastLoad)
      {
         for (Api api : wire.getBeans(Api.class))
         {
            if (Utils.empty(api.getApiCode()))
               throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Api '" + api.getName() + "' is missing an 'apiCode'.  An Api can not be loaded without one.");

            Api existingApi = service.getApi(api.getApiCode());
            if (forceReload || existingApi == null || !existingApi.getHash().equals(config.hash))
            {
               doLoad = true;

               for (Db db : ((Api) api).getDbs())
               {
                  db.bootstrapApi();
               }
            }
         }

         if (doLoad)
         {
            Properties autoProps = AutoWire.encode(new ApiNamer(), new ApiIncluder(), wire.getBeans(Api.class).toArray());
            autoProps.putAll(config.props);
            wire.clear();
            wire.load(autoProps);
            autoWireApi(wire);

            if (!Utils.empty(service.getConfigOut()))
            {
               //               autoProps = new Properties(autoProps)
               //                  {
               //                     public Enumeration keys()
               //                     {
               //                        Vector keys = new Vector(super.keySet());
               //                        Collections.sort(keys);
               //                        return keys.elements();
               //                     }
               //                  };

               String fileName = "./" + service.getConfigOut().trim();

               File file = new File(fileName);

               log.info("writing merged config file to: '" + file.getCanonicalPath() + "'");

               file.getParentFile().mkdirs();
               BufferedWriter out = new BufferedWriter(new FileWriter(file));

               Properties sorted = new Properties()
                  {

                     public Enumeration keys()
                     {
                        Vector v = new Vector(AutoWire.sort(keySet()));
                        return v.elements();
                     }
                  };

               sorted.putAll(autoProps);
               autoProps = sorted;

               autoProps.store(out, "");

               //               for (String key : AutoWire.sort(autoProps.keySet()))
               //               {
               //                  String value = autoProps.getProperty(key);
               //                  if (shouldMask(key))
               //                     value = "###############";
               //               }
               out.flush();
               out.close();

               List<String> keys = AutoWire.sort(autoProps.keySet());//new ArrayList(autoProps.keySet());
               Collections.sort(keys);
               log.info("-- merged user supplied configuration -------------------------");
               for (String key : keys)
               {
                  String value = autoProps.getProperty(key);

                  if (shouldMask(key))
                     value = "###############";

                  log.info(" > " + key + "=" + value);
               }
               log.info("-- end merged user supplied configuration ---------------------");

            }
         }
      }

      if (doLoad || fastLoad)
      {

         for (Api api : wire.getBeans(Api.class))
         {
            Api existingApi = service.getApi(api.getApiCode());
            if (forceReload || existingApi == null || !existingApi.getHash().equals(config.hash))
            {
               api.setHash(config.hash);

               removeExcludes(api);
               service.addApi(api);
            }
         }

         // if (log.isInfoEnabled() && service.isConfigDebug())
         {
            List<String> keys = new ArrayList(config.props.keySet());
            Collections.sort(keys);
            log.info("-- merged user supplied configuration -------------------------");
            for (String key : keys)
            {
               String value = config.props.getProperty(key);

               if (shouldMask(key))
                  value = "###############";

               log.info(" > " + key + "=" + value);
            }
            for (String file : config.files)
            {
               log.info("# config file: " + file);
            }
            log.info("-- end merged user supplied configuration ---------------------");
         }
      }
   }

   void autoWireApi(AutoWire wire)
   {
      List<Api> apis = wire.getBeans(Api.class);
      if (apis.size() == 1)
      {
         Api api = apis.get(0);
         if (api.getDbs().size() == 0)
            api.setDbs(wire.getBeans(Db.class));

         if (api.getEndpoints().size() == 0)
            api.setEndpoints(wire.getBeans(Endpoint.class));

         if (api.getActions().size() == 0)
            api.setActions(wire.getBeans(Action.class));

         if (api.getAclRules().size() == 0)
            api.setAclRules(wire.getBeans(AclRule.class));
      }
   }

   void removeExcludes(Api api)
   {
      for (io.rocketpartners.cloud.model.Collection col : api.getCollections())
      {
         if (col.isExclude() || col.getEntity().isExclude())
         {
            api.removeCollection(col);
         }
         else
         {
            for (Attribute attr : col.getEntity().getAttributes())
            {
               if (attr.isExclude())
               {
                  col.getEntity().removeAttribute(attr);
               }
            }

            for (Relationship rel : col.getEntity().getRelationships())
            {
               if (rel.isExclude())
               {
                  col.getEntity().removeRelationship(rel);
               }
            }
         }
      }

      for (Db db : api.getDbs())
      {
         for (Table table : db.getTables())
         {
            if (table.isExclude())
            {
               db.removeTable(table);
            }
            else
            {
               for (Column col : table.getColumns())
               {
                  if (col.isExclude())
                     table.removeColumn(col);
               }
            }
         }
      }
   }

   static class ApiIncluder implements Includer
   {
      //      List        includes     = Arrays.asList(Api.class, Collection.class, Entity.class, Attribute.class, Relationship.class, Db.class, Table.class, Column.class, Index.class,   //
      //            Boolean.class, Character.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class, Void.class, String.class,                               //
      //            boolean.class, char.class, byte.class, short.class, int.class, long.class, float.class, double.class, void.class);

      List<Field> excludes     = Arrays.asList(Utils.getField("handlers", Api.class));

      List        excludeTypes = new ArrayList(Arrays.asList(Logger.class, Action.class, Endpoint.class, Rule.class));

      @Override
      public boolean include(Field field)
      {
         if (field.getAnnotation(Ignore.class) != null || Modifier.isTransient(field.getModifiers()))
            return false;

         if (excludes.contains(field) || excludeTypes.contains(field.getType()))
            return false;

         Class c = field.getType();
         if (java.util.Collection.class.isAssignableFrom(c))
         {
            Type t = field.getGenericType();
            if (t instanceof ParameterizedType)
            {
               ParameterizedType pt = (ParameterizedType) t;
               if (pt.getActualTypeArguments()[0] instanceof TypeVariable)
               {
                  //can't figure out the type so consider it important
                  return true;
               }

               c = (Class) pt.getActualTypeArguments()[0];
            }

            boolean inc = !excludeTypes.contains(c);
            return inc;
         }
         else if (Map.class.isAssignableFrom(c))
         {
            Type t = field.getGenericType();
            if (t instanceof ParameterizedType)
            {
               ParameterizedType pt = (ParameterizedType) t;
               Class keyType = (Class) pt.getActualTypeArguments()[0];
               Class valueType = (Class) pt.getActualTypeArguments()[1];

               return !excludeTypes.contains(keyType) && !excludeTypes.contains(valueType);
            }
            else
            {
               throw new RuntimeException("You need to parameterize this object: " + field);
            }
         }
         else
         {
            boolean inc = !excludeTypes.contains(c);

            if (!inc)
               System.out.println("skipping field: " + field);
            return inc;
         }
      }

   }

   static class ApiNamer implements Namer
   {
      @Override
      public String getName(Object o) throws Exception
      {
         Object name = null;
         Class clazz = o.getClass();
         if (o instanceof Api || o instanceof Db)// || o instanceof Action || o instanceof Endpoint)
         {
            name = Utils.getField("name", clazz).get(o);
         }
         else if (o instanceof Table)
         {
            Table t = (Table) o;
            name = t.getDb().getName() + ".tables." + t.getName();
         }
         else if (o instanceof Column)
         {
            Column col = (Column) o;
            name = col.getTable().getDb().getName() + ".tables." + col.getTable().getName() + ".columns." + col.getName();
         }
         else if (o instanceof Index)
         {
            Index index = (Index) o;
            name = index.getTable().getDb().getName() + ".tables." + index.getTable().getName() + ".indexes." + index.getName();
         }
         else if (o instanceof Collection)
         {
            Collection col = (Collection) o;
            name = col.getApi().getName() + ".collections." + col.getDb().getName() + "_" + col.getName();
         }
         else if (o instanceof Entity)
         {
            Entity e = (Entity) o;
            name = getName(e.getCollection()) + ".entity";
         }
         else if (o instanceof Attribute)
         {
            Attribute a = (Attribute) o;
            name = getName(a.getEntity()) + ".attributes." + a.getName();
         }
         else if (o instanceof Relationship)
         {
            Relationship a = (Relationship) o;
            name = getName(a.getEntity()) + ".relationships." + a.getName();
         }

         if (name == null)
            name = Utils.getProperty("name", o);

         if (name != null)
            return name.toString();

         throw new RuntimeException("Unable to name: " + o);
      }
   }

   class Config
   {
      String       hash  = null;
      List<String> files = new ArrayList();
      Properties   props = new Properties();
   }

   Config findConfig() throws IOException
   {
      Config config = new Config();

      for (int i = -1; i <= 100; i++)
      {
         String fileName = service.getConfigPath() + "snooze" + (i < 0 ? "" : i) + ".properties";
         InputStream is = service.getResource(fileName);
         if (is != null)
         {
            config.files.add(fileName);
            config.props.load(is);
         }
      }

      if (service.getProfile() != null)
      {
         for (int i = -1; i <= 100; i++)
         {
            String fileName = service.getConfigPath() + "snooze" + (i < 0 ? "" : i) + "-" + service.getProfile() + ".properties";
            InputStream is = service.getResource(fileName);
            if (is != null)
            {
               config.files.add(fileName);
               config.props.load(is);
            }
         }
      }

      if (config.files.isEmpty())
         log.warn("\n\n######################################################\n# WARNING!!! No '.properties' files have been loaded.#\n######################################################\n");

      List keys = new ArrayList(config.props.keySet());
      Collections.sort(keys);
      StringBuffer buff = new StringBuffer();
      for (Object key : config.props.keySet())
      {
         buff.append(key).append(config.props.get(key));
      }

      config.hash = Utils.md5(buff.toString().getBytes());

      return config;
   }

   boolean shouldMask(String str)
   {
      if (str.indexOf(".") > 0)
         str = str.substring(str.lastIndexOf(".") + 1, str.length());

      if (str.indexOf("pass") > -1 || str.indexOf("secret") > -1)
         return true;

      return false;
   }

   public static class AutoWire
   {
      @Retention(RetentionPolicy.RUNTIME)
      @Target(ElementType.FIELD)
      public @interface Ignore {
         //public String value() default "";
      }

      Properties          props = new Properties();

      Map<String, Object> beans = new HashMap();

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
            Utils.rethrow(ex);
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
         }

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
                     String methodName = "add" + Character.toUpperCase(propKey.charAt(0)) + propKey.substring(1, propKey.length());
                     Method adder = getMethod(parent.getClass(), methodName);
                     if (adder == null && methodName.endsWith("s"))
                        adder = getMethod(parent.getClass(), methodName.substring(0, methodName.length() - 1));

                     if (adder == null && methodName.endsWith("es"))
                        adder = getMethod(parent.getClass(), methodName.substring(0, methodName.length() - 2));

                     if (adder != null)
                     {
                        adder.invoke(parent, obj);
                     }
                     else
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
         try
         {
            Method setter = getMethod(obj.getClass(), "set" + Character.toUpperCase(prop.charAt(0)) + prop.substring(1, prop.length()));
            if (setter == null)
            {
               setter = getMethod(obj.getClass(), "add" + Character.toUpperCase(prop.charAt(0)) + prop.substring(1, prop.length()));
               if (setter == null && prop.endsWith("s"))
                  setter = getMethod(obj.getClass(), "add" + Character.toUpperCase(prop.charAt(0)) + prop.substring(1, prop.length() - 1));

               if (setter == null && prop.endsWith("es"))
                  setter = getMethod(obj.getClass(), "add" + Character.toUpperCase(prop.charAt(0)) + prop.substring(1, prop.length() - 2));
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
                     java.util.Collection list = (java.util.Collection) cast(value, type);
                     ((java.util.Collection) field.get(obj)).addAll(list);
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
         catch (Exception ex)
         {
            Utils.rethrow("Error setting " + key + " = " + prop, ex);
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
            java.util.Collection list = new ArrayList();
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

                  if (value instanceof Collection)
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

}
