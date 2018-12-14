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
package io.rcktapp.api.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.forty11.j.J;
import io.forty11.j.utils.AutoWire;
import io.forty11.j.utils.AutoWire.Includer;
import io.forty11.j.utils.AutoWire.Namer;
import io.rcktapp.api.AclRule;
import io.rcktapp.api.Action;
import io.rcktapp.api.Api;
import io.rcktapp.api.ApiException;
import io.rcktapp.api.Attribute;
import io.rcktapp.api.Collection;
import io.rcktapp.api.Column;
import io.rcktapp.api.Db;
import io.rcktapp.api.Endpoint;
import io.rcktapp.api.Entity;
import io.rcktapp.api.Index;
import io.rcktapp.api.Relationship;
import io.rcktapp.api.SC;
import io.rcktapp.api.Table;

public class Configurator
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

      //      if (service.getrereloadTimeout > 0)
      //      {
      //         Thread t = new Thread(new Runnable()
      //            {
      //               @Override
      //               public void run()
      //               {
      //                  while (true)
      //                  {
      //                     try
      //                     {
      //                        J.sleep(reloadTimeout);
      //                        if (destroyed)
      //                           return;
      //
      //                        Config config = findConfig();
      //                        loadConfig(config, false);
      //                     }
      //                     catch (Throwable t)
      //                     {
      //                        log.warn("Error loading config", t);
      //                     }
      //                  }
      //               }
      //            }, "snooze-config-reloader");
      //
      //         t.setDaemon(true);
      //         t.start();
      //      }
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
            if (J.empty(api.getAccountCode()) || J.empty(api.getApiCode()))
               throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Api '" + api.getName() + "' is missing an 'accountCode' or 'apiCode'.  An Api can not be loaded without ");

            Api existingApi = service.getApi(api.getAccountCode(), api.getApiCode());
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

            if (!J.empty(service.getConfigOut()))
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

               Properties sorted = new Properties() {
                 
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
            Api existingApi = service.getApi(api.getAccountCode(), api.getApiCode());
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
      for (io.rcktapp.api.Collection col : api.getCollections())
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
      List        includes = Arrays.asList(Api.class, Collection.class, Entity.class, Attribute.class, Relationship.class, Db.class, Table.class, Column.class, Index.class,   //
            Boolean.class, Character.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class, Void.class, String.class);

      List<Field> excludes = Arrays.asList(J.getField("handlers", Api.class));

      @Override
      public boolean include(Field field)
      {
         if (excludes.contains(field))
            return false;

         Class c = field.getType();
         if (java.util.Collection.class.isAssignableFrom(c))
         {
            Type t = field.getGenericType();
            if (t instanceof ParameterizedType)
            {
               ParameterizedType pt = (ParameterizedType) t;
               c = (Class) pt.getActualTypeArguments()[0];
            }

            boolean inc = includes.contains(c);
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

               return includes.contains(keyType) && includes.contains(valueType);
            }
            else
            {
               throw new RuntimeException("You need to parameterize this object: " + field);
            }
         }
         else
         {
            boolean inc = includes.contains(c);
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
         if (o instanceof Api || o instanceof Db)
         {
            name = J.getField("name", clazz).get(o);
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
            name = col.getApi().getName() + ".collections." + col.getName();
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

         if (name != null)
            return name.toString();
         return null;
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

      List keys = new ArrayList(config.props.keySet());
      Collections.sort(keys);
      StringBuffer buff = new StringBuffer();
      for (Object key : config.props.keySet())
      {
         buff.append(key).append(config.props.get(key));
      }

      config.hash = J.md5(buff.toString().getBytes());

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
