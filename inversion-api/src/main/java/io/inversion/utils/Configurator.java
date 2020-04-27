/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
 * https://github.com/inversion-api
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
package io.inversion.utils;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
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
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inversion.Action;
import io.inversion.Api;
import io.inversion.Collection;
import io.inversion.Db;
import io.inversion.Endpoint;
import io.inversion.Engine;
import io.inversion.Index;
import io.inversion.Property;
import io.inversion.Relationship;
import io.inversion.Rule;
import io.inversion.utils.Configurator.Wirer.Includer;
import io.inversion.utils.Configurator.Wirer.Namer;

public class Configurator
{
   public static final String ROOT_BEAN_NAME = "inversion";

   protected final Logger     log            = LoggerFactory.getLogger(Engine.class.getName() + ".configuration");

   protected Engine           engine         = null;

   /**
    * An identifier, such as 'dev', 'stage', 'prod' used to locate and load additional "inversion[1-99][-${profile}].properties" config files.
    * <p>
    * A profile allows you to build a single jar/war that can be deployed in multiple environments. 
    * Configuration common to all deployments can be added to "inversion[1-99].properties" files and then
    * augmented with the differences between deployments isolated to the profile specific files.
    * <p>
    * To set the <code>profile</code>, you can directly wire <code>engine.withProfile</code> or set "inversion.profile"
    * as an environment variable or Java system property. 
    * 
    * @see io.inversion.utils.Configurator
    */
   protected String           profile        = null;

   /**
    * The path to inversion*.properties files
    */
   protected String           configPath     = "";

   /**
    * The number of milliseconds between background reloads of the Api config
    */
   protected int              configTimeout  = 10000;

   /**
    * Indicates that the supplied config files contain all the setup info and the Api
    * will not be reflectively configured as it otherwise would.
    */
   protected boolean          configFast     = false;
   protected boolean          configDebug    = false;
   protected String           configOut      = null;

   public synchronized void loadConfig(Engine engine)
   {
      if (this.engine != null)
         return;

      this.engine = engine;

      try
      {
         Config config = findConfig();

         if (config.files.size() == 0)
            return;

         if (engine.getApis().size() > 0)
         {
            System.out.println("NOTICE: Api have been wired up already in code.  Settings from your inversion*.properties files will be loaded into System.properties instead of being used to reflectively wire the Api model");
            //-- if there are already APIs loaded, we will not try to rebootstrap the core model from
            //-- the properties files.  Instead, we will just put all properties in the env so that 
            //-- lazy loading methods from individual classes can pull them.
            config.props.entrySet().forEach(entry -> {
               System.setProperty(entry.getKey() + "", entry.getValue() + "");
            });
         }
         else
         {
            //all this does is set inversion.* properties on the engine class
            Wirer w = new Wirer();
            w.putBean(ROOT_BEAN_NAME, engine);
            w.load(config.props);

            loadConfig(config, true, isConfigFast());
         }
      }
      catch (Exception e)
      {
         e.printStackTrace();
         throw new RuntimeException("Unable to load config files: " + e.getMessage(), e);
      }
   }

   public static Properties encode(Object... beans) throws Exception
   {
      Properties autoProps = Wirer.encode(new WirerSerializerNamer(), new WirerSerializerIncluder(), beans);
      return autoProps;
   }

   void loadConfig(Config config, boolean forceReload, boolean fastLoad) throws Exception
   {
      Wirer wire = new Wirer()
         {
            //IMPORTANT IMPORTANT IMPORTANT
            // add special case exceptions here for cases where users may add unclean data
            // that should not be set directly on bean fields but should be passed the approperiate setter
            public boolean handleProp(Object bean, String prop, String value) throws Exception
            {
               //               if (bean instanceof Endpoint && "path".equalsIgnoreCase(value))
               //               {
               //                  //this is done as a special case because of the 
               //                  //special business logic in the setter
               //                  ((Endpoint) bean).withPath(value);
               //
               //                  return true;
               //               }

               return false;
            }

            @Override
            public void onLoad(String name, Object module, Map<String, Object> props) throws Exception
            {
               Field field = getField("name", module.getClass());
               if (field != null && field.get(module) == null)
                  field.set(module, name);
            }
         };
      wire.load(config.props);

      //-- this just loads the bare bones config supplied  
      //-- in inversion*.properties files by the users.  
      autoWireApi(wire);

      boolean doLoad = false;

      if (!fastLoad)
      {
         for (Api api : wire.getBeans(Api.class))
         {
            Api existingApi = engine.getApi(api.getName());

            if (forceReload || existingApi == null || !existingApi.getHash().equals(config.hash))
            {
               doLoad = true;

               for (Db db : ((Api) api).getDbs())
               {
                  db.startup(api);
               }
            }
         }

         if (doLoad)
         {
            //-- this serializes out the object model that was bootsrapped off of the
            //-- configuration files.  At this point the db.startup() has been called
            //-- on all of the DBs and they configured collections on the Api.  
            Properties autoProps = Wirer.encode(new WirerSerializerNamer(), new WirerSerializerIncluder(), wire.getBeans(Api.class).toArray());
            autoProps.putAll(config.props);
            wire.clear();
            wire.load(autoProps);

            autoWireApi(wire);

            String configOut = getConfigOut();
            if (!Utils.empty(configOut))
            {
               String fileName = "./" + configOut.trim();

               File file = new File(fileName);

               log.info("writing merged config file to: '" + file.getCanonicalPath() + "'");

               file.getParentFile().mkdirs();
               BufferedWriter out = new BufferedWriter(new FileWriter(file));

               //properties are sorted based on the number of "." segments they contain so that "shallow"
               //depth properties can be set before deeper depth properties.
               Properties sorted = new Properties()
                  {
                     public Enumeration keys()
                     {
                        Vector v = new Vector(Wirer.sort(keySet()));
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

               List<String> keys = Wirer.sort(autoProps.keySet());//new ArrayList(autoProps.keySet());
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
            Api existingApi = engine.getApi(api.getName());
            if (forceReload || existingApi == null || !existingApi.getHash().equals(config.hash))
            {
               api.withHash(config.hash);
               api.removeExcludes();
               engine.withApi(api);
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

   void autoWireApi(Wirer wire)
   {
      List<Api> apis = wire.getBeans(Api.class);

      if (apis.size() == 1)
      {
         List found = wire.getBeans(Db.class);

         Api api = apis.get(0);

         if (api.getDbs().size() == 0)
            api.withDbs((Db[]) found.toArray(new Db[found.size()]));

         Set<Action> privateActions = new HashSet();
         found = wire.getBeans(Endpoint.class);
         if (api.getEndpoints().size() == 0)
         {
            for (Endpoint ep : (List<Endpoint>) found)
            {
               api.withEndpoint(ep);
               privateActions.addAll(ep.getActions());
            }
         }

         found = wire.getBeans(Action.class);
         if (api.getActions().size() == 0)
         {
            for (Action action : (List<Action>) found)
            {
               if (!privateActions.contains(action))
                  api.withAction(action);
            }
         }
      }
   }

   static class WirerSerializerIncluder implements Includer
   {
      List<Field> excludes     = new ArrayList();                                                                 //TODO:  why was api.actions excluded?  //List<Field> excludes     =  Arrays.asList(Utils.getField("actions", Api.class));

      List        excludeTypes = new ArrayList(Arrays.asList(Logger.class,                                        //don't care to persist info on loggers
            Action.class, Endpoint.class, Rule.class, Path.class));                                               //these are things that must be supplied by manual config so don't write them out.

      @Override
      public boolean include(Field field)
      {
         ///if (field.getAnnotation(Ignore.class) != null || Modifier.isTransient(field.getModifiers()))
         if (Modifier.isTransient(field.getModifiers()))
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
         else if (Properties.class.isAssignableFrom(c))
         {
            return true;
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

   static class WirerSerializerNamer implements Namer
   {
      @Override
      public String getName(Object o) throws Exception
      {
         Object name = null;
         Class clazz = o.getClass();
         if (o instanceof Api)
         {
            name = ((Api) o).getName();
         }
         else if (o instanceof Db)// || o instanceof Action || o instanceof Endpoint)
         {
            name = Utils.getField("name", clazz).get(o);
         }
         else if (o instanceof Collection)
         {
            Collection t = (Collection) o;
            name = t.getDb().getName() + ".collections." + t.getTableName();
         }
         else if (o instanceof Property)
         {
            Property col = (Property) o;
            name = col.getCollection().getDb().getName() + ".collections." + col.getCollection().getTableName() + ".properties." + col.getColumnName();
         }
         else if (o instanceof Index)
         {
            Index index = (Index) o;
            name = index.getCollection().getDb().getName() + ".collections." + index.getCollection().getTableName() + ".indexes." + index.getName();
         }
         else if (o instanceof Relationship)
         {
            Relationship a = (Relationship) o;
            name = getName(a.getCollection()) + ".relationships." + a.getName();
         }

         if (name == null)
            name = Utils.getProperty("name", o);

         if (name != null)
            return name.toString();

         throw new RuntimeException("Unable to name: " + o + " " + o.getClass());
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

      String configPath = getConfigPath();

      if (configPath.length() > 0 && !(configPath.endsWith("/") || configPath.endsWith("\\")))
         configPath += "/";

      for (int i = -1; i <= 100; i++)
      {
         String fileName = configPath + "inversion" + (i < 0 ? "" : i) + ".properties";
         InputStream is = engine.getResource(fileName);

         if (is == null)
         {
            fileName = configPath + "inversion" + "-" + (i < 0 ? "" : i) + ".properties";
            is = engine.getResource(fileName);
         }

         if (is != null)
         {
            config.files.add(fileName);
            config.props.load(is);
         }
      }

      if (getProfile() != null)
      {
         for (int i = -1; i <= 100; i++)
         {
            InputStream is = null;
            String fileName = null;

            fileName = configPath + "inversion" + (i < 0 ? "" : i) + "-" + getProfile() + ".properties";
            is = engine.getResource(fileName);

            if (is == null)
            {
               fileName = configPath + "inversion" + "-" + (i < 0 ? "" : i) + "-" + getProfile() + ".properties";
               is = engine.getResource(fileName);
            }
            if (is == null)
            {
               fileName = configPath + "inversion" + "-" + getProfile() + (i < 0 ? "" : i) + ".properties";
               is = engine.getResource(fileName);
            }
            if (is == null)
            {
               fileName = configPath + "inversion" + "-" + getProfile() + "-" + (i < 0 ? "" : i) + ".properties";
               is = engine.getResource(fileName);
            }

            if (is != null)
            {
               config.files.add(fileName);
               config.props.load(is);
            }
         }
      }

      if (engine.getApis().size() == 0 && config.files.isEmpty())
      {
         log.warn("\n\n#########################################################################\n# WARNING!!! No '.properties' files have been loaded.                   #\n# Are you still using snooze.properties? Change to inversion.properties #\n#########################################################################\n");
      }
      //      else
      //      {
      for (String fileName : config.files)
      {
         log.warn("LOADING CONFIG FILE: " + fileName);
      }
      //      }

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

   public String getProfile()
   {
      return Utils.getSysEnvPropStr("inversion.profile", profile);
   }

   public Configurator withProfile(String profile)
   {
      this.profile = profile;
      return this;
   }

   public String getConfigPath()
   {
      return configPath;
   }

   public Configurator withConfigPath(String configPath)
   {
      this.configPath = configPath;
      return this;
   }

   public int getConfigTimeout()
   {
      return configTimeout;
   }

   public Configurator withConfigTimeout(int configTimeout)
   {
      this.configTimeout = configTimeout;
      return this;
   }

   public boolean isConfigFast()
   {
      return configFast;
   }

   public Configurator withConfigFast(boolean configFast)
   {
      this.configFast = configFast;
      return this;
   }

   public boolean isConfigDebug()
   {
      return configDebug;
   }

   public Configurator withConfigDebug(boolean configDebug)
   {
      this.configDebug = configDebug;
      return this;
   }

   public String getConfigOut()
   {
      return configOut;
   }

   public Configurator withConfigOut(String configOut)
   {
      this.configOut = configOut;
      return this;
   }

   static class Wirer
   {
      Properties          props    = new Properties();
      TreeSet<String>     propKeys = new TreeSet<String>();

      Map<String, Object> beans    = new HashMap();

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
         this.propKeys.addAll((Set) props.keySet());
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

      static interface Namer
      {
         public String getName(Object o) throws Exception;
      }

      static interface Includer
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

                  if (Modifier.isFinal(field.getModifiers()))
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

}
