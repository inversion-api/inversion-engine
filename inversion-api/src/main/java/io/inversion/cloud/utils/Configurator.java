/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inversion.cloud.model.Action;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.ApiException;
import io.inversion.cloud.model.Property;
import io.inversion.cloud.model.Db;
import io.inversion.cloud.model.Endpoint;
import io.inversion.cloud.model.Index;
import io.inversion.cloud.model.Path;
import io.inversion.cloud.model.Relationship;
import io.inversion.cloud.model.Rule;
import io.inversion.cloud.model.Status;
import io.inversion.cloud.model.Collection;
import io.inversion.cloud.service.Engine;
import io.inversion.cloud.utils.Wirer.Ignore;
import io.inversion.cloud.utils.Wirer.Includer;
import io.inversion.cloud.utils.Wirer.Namer;

public class Configurator
{
   public static final String ROOT_BEAN_NAME = "inversion";

   Logger                     log            = LoggerFactory.getLogger(Engine.class.getName() + ".configuration");

   Engine                     engine         = null;

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

            loadConfig(config, true, engine.isConfigFast());
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
               if (bean instanceof Endpoint && "path".equalsIgnoreCase(value))
               {
                  //this is done as a special case because of the 
                  //special business logic in the setter
                  ((Endpoint) bean).withPath(value);

                  return true;
               }

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
            if (Utils.empty(api.getApiCode()))
               throw new ApiException(Status.SC_500_INTERNAL_SERVER_ERROR, "Api '" + api.getApiCode() + "' is missing an 'apiCode'.  An Api cannot be loaded without one.");

            Api existingApi = engine.getApi(api.getApiCode());
            if (forceReload || existingApi == null || !existingApi.getHash().equals(config.hash))
            {
               doLoad = true;

               for (Db db : ((Api) api).getDbs())
               {
                  db.startup();
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

            if (!Utils.empty(engine.getConfigOut()))
            {
               String fileName = "./" + engine.getConfigOut().trim();

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
            Api existingApi = engine.getApi(api.getApiCode());
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

         //         found = wire.getBeans(AclRule.class);
         //         if (api.getAclRules().size() == 0)
         //            api.withAclRules((AclRule[]) found.toArray(new AclRule[found.size()]));
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
            name = t.getDb().getName() + ".tables." + t.getTableName();
         }
         else if (o instanceof Property)
         {
            Property col = (Property) o;
            name = col.getCollection().getDb().getName() + ".tables." + col.getCollection().getTableName() + ".columns." + col.getColumnName();
         }
         else if (o instanceof Index)
         {
            Index index = (Index) o;
            name = index.getCollection().getDb().getName() + ".tables." + index.getCollection().getTableName() + ".indexes." + index.getName();
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

      String configPath = engine.getConfigPath();

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

      if (engine.getProfile() != null)
      {
         for (int i = -1; i <= 100; i++)
         {
            InputStream is = null;
            String fileName = null;

            fileName = configPath + "inversion" + (i < 0 ? "" : i) + "-" + engine.getProfile() + ".properties";
            is = engine.getResource(fileName);

            if (is == null)
            {
               fileName = configPath + "inversion" + "-" + (i < 0 ? "" : i) + "-" + engine.getProfile() + ".properties";
               is = engine.getResource(fileName);
            }
            if (is == null)
            {
               fileName = configPath + "inversion" + "-" + engine.getProfile() + (i < 0 ? "" : i) + ".properties";
               is = engine.getResource(fileName);
            }
            if (is == null)
            {
               fileName = configPath + "inversion" + "-" + engine.getProfile() + "-" + (i < 0 ? "" : i) + ".properties";
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

}
