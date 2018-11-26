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
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.atteo.evo.inflector.English;
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
import io.rcktapp.api.Permission;
import io.rcktapp.api.Relationship;
import io.rcktapp.api.Request;
import io.rcktapp.api.Response;
import io.rcktapp.api.Role;
import io.rcktapp.api.SC;
import io.rcktapp.api.Table;

public class Snooze extends Service
{
   static Logger  log           = LoggerFactory.getLogger(Snooze.class);

   Set<Api>       reloads       = new HashSet();
   Thread         reloadTimer   = null;
   long           reloadTimeout = 60 * 1000;

   //in progress loads used to prevent the same api
   //from loading multiple times in concurrent threads
   Vector         loadingApis   = new Vector();

   boolean        inited        = false;

   String         profile       = null;
   //saved here so imperfect embeddings can pass in init(ServletContext)
   //instead of having to have
   ServletContext context       = null;

   String         configPath    = "/WEB-INF/";

   public Snooze() throws Exception
   {

   }

   public void init() throws ServletException
   {
      try
      {
         if (inited)
            return;

         inited = true;

         reloadTimer = new Thread(new Runnable()
            {
               @Override
               public void run()
               {
                  while (true)
                  {
                     try
                     {
                        Set<Api> toReload = reloads;
                        reloads = new HashSet();

                        for (Api oldCopy : toReload)
                        {
                           try
                           {
                              Api newCopy = loadApi(oldCopy.getId());
                              addApi(newCopy);
                           }
                           catch (Exception ex)
                           {
                              log.error("Error reloading apis", ex);

                              //unpublishing the api here is a "fail fast" 
                              //technique to prevent it from continually 
                              //failing and crapping out the repubish queue
                              //for all clients
                              removeApi(oldCopy);

                           }
                        }

                        J.sleep(reloadTimeout);
                     }
                     catch (Throwable t)
                     {
                        log.error("Error reloading apis", t);
                     }
                  }
               }
            }, "api-reloader");
         reloadTimer.setDaemon(true);
         reloadTimer.start();

         Properties props = findProps();

         AutoWire w = new AutoWire();
         w.putBean("snooze", this);
         w.load(props);

         if (driver != null)
         {
            ComboPooledDataSource cpds = new ComboPooledDataSource();
            cpds.setDriverClass(driver);
            cpds.setJdbcUrl(url);
            cpds.setUser(user);
            cpds.setPassword(pass);
            cpds.setMinPoolSize(poolMin);
            cpds.setMaxPoolSize(poolMax);

            ds = cpds;
         }

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
               db.bootstrapApi((Api) api);
            }
         }

         Properties autoProps = AutoWire.encode(new ApiNamer(), new ApiIncluder(), wire.getBeans(Api.class).toArray());
         autoProps.putAll(props);

         log.debug("loading configuration -------------------------");
         for (String key : AutoWire.sort(autoProps.keySet()))
         {
            log.debug(key + "=" + autoProps.getProperty(key));
         }
         log.debug("- end -------");

         wire.clear();
         wire.load(autoProps);

         for (Api api : wire.getBeans(Api.class))
         {
            api.updateDbMap();
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
      List        includes = Arrays.asList(Api.class, Collection.class, Entity.class, Attribute.class, Relationship.class, Db.class, Table.class, Column.class,   //
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

      if (log.isInfoEnabled())
      {
         List<String> keys = new ArrayList(props.keySet());
         Collections.sort(keys);
         log.info("Merged properties");
         for (String key : keys)
         {
            log.info(" > " + key + " : " + props.getProperty(key));
         }
      }

      return props;
   }

   @Override
   public void doService(Service service, Chain parent, ApiMatch match, Endpoint endpoint, Request req, Response res) throws Exception
   {
      try
      {
         super.doService(service, parent, match, endpoint, req, res);
         Api api = match.api;
         if (api != null && api.isReloadable() && api.isDebug())
            reloads.add(api);
      }
      catch (Exception ex)
      {
         throw ex;
      }
   }

   /**
    * Overridden to lazy load APIs from the DB
    */
   public ApiMatch findApi(Url url) throws Exception
   {
      ApiMatch match = super.findApi(url);
      if (match == null)
      {
         Api api = loadApi(url);
         if (api != null)
         {
            addApi(api);
            match = super.findApi(url);
         }
      }
      return match;
   }

   /**
    * Returns a completely new copy of the API loaded from the DB(s).  There
    * is no caching in this method.  This will cause the API to auto bootstrap 
    * if there are no collections and will store the api 
    */
   Api loadApi(Url url) throws Exception
   {
      Connection conn = null;
      Api api = null;
      //String apiUrl = null;

      try
      {
         conn = getConnection();
         if (conn != null)
         {

            String accountCode = null;
            String apiCode = null;

            String host = url.getHost();
            int numPeriods = host.length() - host.replace(".", "").length();
            if (numPeriods == 2)//if this is a three part host name hostKey.domain.com
            {
               accountCode = host.substring(0, host.indexOf("."));
            }

            String path = url.getPath();

            if (!J.empty(servletMapping))
            {
               String match = servletMapping.endsWith("/") ? servletMapping : (servletMapping + "/");

               if (path.indexOf(match) < 0)
               {
                  //caused if your servlet container is setup to route to something like *
                  //but you set a servletMapping on the Service/Snooze servlet probably something like "api/"
                  throw new ApiException(SC.SC_404_NOT_FOUND, "Servlet path mapping and Service.servletMapping paramters do not match.");
               }

               path = path.substring(path.indexOf(match) + match.length(), path.length());
            }
            while (path.startsWith("/"))
            {
               path = path.substring(1, path.length());
            }
            while (path.endsWith("/"))
            {
               path = path.substring(0, path.length() - 1);
            }

            String[] parts = path.split("\\/");

            for (int i = 0; i < parts.length; i++)
            {
               if (i == 0 && accountCode == null)
               {
                  accountCode = parts[i];
                  continue;
               }

               if (!parts[i].equalsIgnoreCase(accountCode))
               {
                  apiCode = parts[i];
                  break;
               }
            }

            if (J.empty(accountCode) || J.empty(apiCode))
            {
               return null;
            }

            String sql = "SELECT a.*, a.apiCode, n.accountCode FROM Api a JOIN Account n ON a.accountId = n.id WHERE (n.accountCode = ? AND n.accountCode = a.apiCode) OR (n.accountCode = ? AND  a.apiCode = ?) LIMIT 1";

            api = (Api) Sql.selectObject(conn, sql, new Api(), accountCode, accountCode, apiCode);

            if (api == null || api.getId() <= 0)
            {
               throw new ApiException(SC.SC_400_BAD_REQUEST, "Unable to find an API for url: '" + url + "'");
            }

            synchronized (this)
            {
               while (loadingApis.contains(api.getId()))
               {
                  try
                  {
                     this.wait();
                  }
                  catch (InterruptedException ex)
                  {

                  }
                  Api alreadyLoaded = getApi(api.getId());
                  if (alreadyLoaded != null)
                     return alreadyLoaded;
               }

               loadingApis.add(api.getId());
            }

            api = loadApi(api.getId());

         }
      }
      finally
      {
         synchronized (this)
         {
            if (api != null)
               loadingApis.remove(api.getId());

            notifyAll();
         }

         Sql.close(conn);
      }

      return api;
   }

   protected Api loadApi(long apiId) throws Exception
   {
      Connection conn = null;
      Api api = null;
      try
      {
         conn = getConnection();

         String sql = "SELECT a.*, a.apiCode, n.accountCode FROM Api a JOIN Account n ON a.accountId = n.id WHERE a.id = ? LIMIT 1";

         api = (Api) Sql.selectObject(conn, sql, new Api(), apiId);

         sql = "";
         sql += " SELECT d.* FROM Db d ";
         sql += " JOIN ApiDbs ad ON ad.dbId = d.id ";
         sql += " JOIN Api a ON ad.apiId = a.id ";
         sql += " WHERE a.id = ? ";

         List vars = new ArrayList();
         vars.add(api.getId());

         List<Db> dbs = Sql.selectObjects(conn, sql, Db.class, vars.toArray());
         api.setDbs(dbs);

         for (Db db : dbs)
         {
            db.bootstrapApi(api);
         }

         List<AclRule> acls = Sql.selectObjects(conn, "SELECT * FROM Acl WHERE apiId = ?", AclRule.class, api.getId());
         api.setAcls(acls);
         for (AclRule acl : acls)
         {
            acl.setPermissions(Sql.selectObjects(conn, "SELECT p.* FROM Permission p JOIN AclPermission ap WHERE ap.aclId = ?", Permission.class, acl.getId()));
            acl.setRoles(Sql.selectObjects(conn, "SELECT r.* FROM Role r JOIN AclRole ar WHERE ar.aclId = ?", Role.class, acl.getId()));
         }

         List<Endpoint> endpoints = Sql.selectObjects(conn, "SELECT * FROM Endpoint WHERE apiId = ?", Endpoint.class, api.getId());
         api.setEndpoints(endpoints);

         for (Endpoint endpoint : endpoints)
         {
            //endpoint.setPermissions(Sql.selectObjects(conn, "SELECT p.* FROM Permission p JOIN EndpointPermission ep WHERE ep.endpointId = ?", Permission.class, endpoint.getId()));
            //endpoint.setRoles(Sql.selectObjects(conn, "SELECT r.* FROM Role r JOIN EndpointRole er WHERE er.endpointId = ?", Role.class, endpoint.getId()));

            List<Action> actions = Sql.selectObjects(conn, "SELECT * FROM Action WHERE endpointId = ?", Action.class, endpoint.getId());
            for (Action action : actions)
            {
               loadAction(conn, action);
               endpoint.addAction(action);
            }

         }

         List<Action> actions = Sql.selectObjects(conn, "SELECT * FROM Action where endpointId IS NULL", Action.class);
         for (Action action : actions)
         {
            loadAction(conn, action);
            api.addAction(action);
         }
      }
      finally
      {
         Sql.close(conn);
      }

      return api;
   }

   void loadAction(Connection conn, Action action) throws Exception
   {
      Row handlerRow = Sql.selectRow(conn, "SELECT * FROM Handler WHERE id = ?", action.getHandlerId());

      AutoWire aw = new AutoWire();

      String cn = handlerRow.getString("className");
      if (!J.empty(cn))
      {
         aw.add("handler.className", cn);
      }

      String params = handlerRow.getString("params");
      if (!J.empty(params))
      {
         aw.add(params);
      }

      aw.load();
      Handler handler = aw.getBean(Handler.class);
      action.setHandler(handler);
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
