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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.rocketpartners.cloud.action.security.AclRule;
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
import io.rocketpartners.cloud.utils.Wirer.Includer;
import io.rocketpartners.cloud.utils.Wirer.Namer;

public class Configurator
{
   public static final String ROOT_BEAN_NAME = "inversion";

   Logger                     log            = LoggerFactory.getLogger(Service.class.getName() + ".configuration");

   boolean                    destroyed      = false;

   Service                    service        = null;

   public void destroy()
   {
      destroyed = true;
   }

   public synchronized void loadConfig(Service service)
   {
      if (this.service != null)
         return;

      this.service = service;

      try
      {
         Config config = findConfig();

         if (config.files.size() == 0)
            return;

         //all this does is set inversion.* properties on the service class
         Wirer w = new Wirer();
         w.putBean(ROOT_BEAN_NAME, service);
         w.load(config.props);

         loadConfig(config, true, service.isConfigFast());
      }
      catch (Exception e)
      {
         e.printStackTrace();
         throw new RuntimeException("Unable to load config files: " + e.getMessage(), e);
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
            }, "inversion-config-reloader");

         t.setDaemon(true);
         t.start();
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
               if (bean instanceof Endpoint && prop.equals("path"))
               {
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
      autoWireApi(wire);

      boolean doLoad = false;

      if (!fastLoad)
      {
         for (Api api : wire.getBeans(Api.class))
         {
            if (Utils.empty(api.getApiCode()))
               throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Api '" + api.getApiCode() + "' is missing an 'apiCode'.  An Api cannot be loaded without one.");

            Api existingApi = service.getApi(api.getApiCode());
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
            Properties autoProps = Wirer.encode(new WirerSerializerNamer(), new WirerSerializerIncluder(), wire.getBeans(Api.class).toArray());
            autoProps.putAll(config.props);
            wire.clear();
            wire.load(autoProps);
            autoWireApi(wire);

            if (!Utils.empty(service.getConfigOut()))
            {
               String fileName = "./" + service.getConfigOut().trim();

               File file = new File(fileName);

               log.info("writing merged config file to: '" + file.getCanonicalPath() + "'");

               file.getParentFile().mkdirs();
               BufferedWriter out = new BufferedWriter(new FileWriter(file));

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
            Api existingApi = service.getApi(api.getApiCode());
            if (forceReload || existingApi == null || !existingApi.getHash().equals(config.hash))
            {
               api.withHash(config.hash);
               api.removeExcludes();
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

   void autoWireApi(Wirer wire)
   {
      List<Api> apis = wire.getBeans(Api.class);

      if (apis.size() == 1)
      {
         List found = wire.getBeans(Db.class);

         Api api = apis.get(0);

         if (api.getDbs().size() == 0)
            api.withDbs((Db[]) found.toArray(new Db[found.size()]));

         found = wire.getBeans(Endpoint.class);
         if (api.getEndpoints().size() == 0)
            api.withEndpoints((Endpoint[]) found.toArray(new Endpoint[found.size()]));

         found = wire.getBeans(Action.class);
         if (api.getActions().size() == 0)
            api.withActions((Action[]) found.toArray(new Action[found.size()]));

         //         found = wire.getBeans(AclRule.class);
         //         if (api.getAclRules().size() == 0)
         //            api.withAclRules((AclRule[]) found.toArray(new AclRule[found.size()]));
      }
   }

   static class WirerSerializerIncluder implements Includer
   {
      List<Field> excludes     = new ArrayList();                                                                 //TODO:  why was api.actions excluded?  //List<Field> excludes     =  Arrays.asList(Utils.getField("actions", Api.class));

      List        excludeTypes = new ArrayList(Arrays.asList(Logger.class,                                        //don't care to persist info on loggers
            Action.class, Endpoint.class, Rule.class));                                                           //these are things that must be supplied by manual config so don't write them out.

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
            name = col.getApi().getApiCode() + ".collections." + col.getDb().getName() + "_" + col.getName();
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

      String configPath = service.getConfigPath();

      if (configPath.length() > 0 && !(configPath.endsWith("/") || configPath.endsWith("\\")))
         configPath += "/";

      for (int i = -1; i <= 100; i++)
      {
         String fileName = configPath + "inversion" + (i < 0 ? "" : i) + ".properties";
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
            String fileName = configPath + "inversion" + (i < 0 ? "" : i) + "-" + service.getProfile() + ".properties";
            InputStream is = service.getResource(fileName);
            if (is != null)
            {
               config.files.add(fileName);
               config.props.load(is);
            }
         }
      }

      if (service.getApis().size() == 0 && config.files.isEmpty())
      {
         log.warn("\n\n#########################################################################\n# WARNING!!! No '.properties' files have been loaded.                   #\n# Are you still using snooze.properties? Change to inversion.properties #\n#########################################################################\n");
      }
      //      else
      //      {
      //         for (String fileName : config.files)
      //         {
      //            log.warn("LOADING CONFIG FILE: " + fileName);
      //         }
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
