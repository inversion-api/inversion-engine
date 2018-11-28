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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import io.forty11.j.J;
import io.forty11.j.utils.AutoWire;
import io.forty11.j.utils.AutoWire.Includer;
import io.forty11.j.utils.AutoWire.Namer;
import io.forty11.sql.Rows.Row;
import io.forty11.sql.Sql;
import io.forty11.web.Url;
import io.rcktapp.api.AclRule;
import io.rcktapp.api.Action;
import io.rcktapp.api.Api;
import io.rcktapp.api.ApiException;
import io.rcktapp.api.Attribute;
import io.rcktapp.api.Chain;
import io.rcktapp.api.Collection;
import io.rcktapp.api.Column;
import io.rcktapp.api.Db;
import io.rcktapp.api.Endpoint;
import io.rcktapp.api.Entity;
import io.rcktapp.api.Handler;
import io.rcktapp.api.Index;
import io.rcktapp.api.Permission;
import io.rcktapp.api.Relationship;
import io.rcktapp.api.Request;
import io.rcktapp.api.Response;
import io.rcktapp.api.Role;
import io.rcktapp.api.SC;
import io.rcktapp.api.Table;
import io.rcktapp.api.handler.sql.SqlDb;

public class Snooze extends Service
{
   boolean          inited     = false;

   protected String profile    = null;

   protected String configPath = "/WEB-INF/";

   public void init() throws ServletException
   {
      try
      {
         if (inited)
            return;

         inited = true;

         Properties props = findProps();
         
         if (log.isInfoEnabled())
         {
            List<String> keys = new ArrayList(props.keySet());
            Collections.sort(keys);
            log.info("-- merged user supplied configuration -------------------------");
            for (String key : keys)
            {
               String value = props.getProperty(key);

               if (shouldMask(key))
                  value = "###############";

               log.info(" > " + key + "=" + value);
            }
            log.info("-- end merged user supplied configuration ---------------------");
         }

         AutoWire w = new AutoWire();
         w.putBean("snooze", this);
         w.load(props);

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
         wire.load(props);

         for (Api api : wire.getBeans(Api.class))
         {
            for (Db db : ((Api) api).getDbs())
            {
               db.bootstrapApi();
            }
         }

         Properties autoProps = AutoWire.encode(new ApiNamer(), new ApiIncluder(), wire.getBeans(Api.class).toArray());
         autoProps.putAll(props);

         log.info("-- loading final configuration -------------------------");
         for (String key : AutoWire.sort(autoProps.keySet()))
         {
            String value = autoProps.getProperty(key);
            if (shouldMask(key))
               value = "###############";

            log.info(" > " + key + "=" + value);
         }
         log.info("-- end final config -------");

         wire.clear();
         wire.load(autoProps);

         for (Api api : wire.getBeans(Api.class))
         {
            api.updateDbMap();

            //process excluded
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

            addApi(api);
         }

      }
      catch (Exception e)
      {
         throw new ServletException("Error loading configuration", e);
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

   Properties findProps() throws IOException
   {
      Properties props = new Properties();

      for (int i = -1; i <= 100; i++)
      {
         String fileName = configPath + "snooze" + (i < 0 ? "" : i) + ".properties";
         InputStream is = getServletContext().getResourceAsStream(fileName);
         if (is != null)
         {
            log.info("Loading properties file: " + fileName);
            props.load(is);
         }
      }

      if (profile != null)
      {
         for (int i = -1; i <= 100; i++)
         {
            String fileName = configPath + "snooze" + (i < 0 ? "" : i) + "-" + profile + ".properties";
            InputStream is = getServletContext().getResourceAsStream(fileName);
            if (is != null)
            {
               log.info("Loading properties file: " + fileName);
               props.load(is);
            }
         }
      }

      return props;
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
      return profile;
   }

   public void setProfile(String profile)
   {
      this.profile = profile;
   }

   public String getConfigPath()
   {
      return configPath;
   }

   public void setConfigPath(String configPath)
   {
      this.configPath = configPath;
   }

}
