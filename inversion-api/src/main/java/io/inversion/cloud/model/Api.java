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
package io.inversion.cloud.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Api extends Rule<Api>
{
   protected final Logger                log         = LoggerFactory.getLogger(getClass());

   transient volatile boolean            started     = false;
   transient volatile boolean            starting    = false;
   transient protected String            hash        = null;
   transient long                        loadTime    = 0;

   protected boolean                     debug       = false;

   protected String                      name        = null;
   protected String                      url         = null;

   protected List<Db>                    dbs         = new ArrayList();
   protected List<Endpoint>              endpoints   = new ArrayList();
   protected List<Action>                actions     = new ArrayList();
   protected List<Collection>            collections = new ArrayList();

   protected transient List<ApiListener> listeners   = new ArrayList();

   //protected Path pathMatch = new Path("${api.name}/${api.version}/${tenant}/blah/blah2/*");
   //protected Path pathMatch = new Path("$}/${api.version}/${tenant}/blah/blah2/*");

   public Api()
   {
   }

   public Api(String name)
   {
      withName(name);
   }

   public Path getDefaultIncludes()
   {
      List parts = new ArrayList();
      if (name != null)
      {
         parts.add(name);
      }
      parts.add("*");
      return new Path(parts);
   }

   public synchronized Api startup()
   {
      if (started || starting) //starting is an accidental recursion guard
         return this;

      starting = true;
      try
      {
         for (Db db : dbs)
         {
            db.startup(this);
         }

         removeExcludes();

         started = true;

         for (ApiListener listener : listeners)
         {
            try
            {
               listener.onStartup(this);
            }
            catch (Exception ex)
            {
               log.warn("Error notifing api startup listener: " + listener, ex);
            }
         }

         return this;
      }
      finally
      {
         starting = false;
      }
   }

   public boolean isStarted()
   {
      return started;
   }

   public void shutdown()
   {
      for (Db db : dbs)
      {
         db.shutdown();
      }
   }

   public void removeExcludes()
   {
      for (Db db : getDbs())
      {
         for (Collection coll : (List<Collection>) db.getCollections())
         {
            if (coll.isExclude())
            {
               db.removeCollection(coll);
            }
            else
            {
               for (Property col : coll.getProperties())
               {
                  if (col.isExclude())
                     coll.removeProperty(col);
               }
            }

            for (Relationship rel : coll.getRelationships())
            {
               if (rel.isExclude())
               {
                  coll.removeRelationship(rel);
               }
            }
         }
      }
   }

   public String getHash()
   {
      return hash;
   }

   public Api withHash(String hash)
   {
      this.hash = hash;
      return this;
   }

   public Api withCollection(Collection coll)
   {
      if (coll.isLinkTbl() || coll.isExclude())
         return this;

      if (!collections.contains(coll))
         collections.add(coll);

      return this;
   }

   public List<Collection> getCollections()
   {
      return Collections.unmodifiableList(collections);
   }

   public Collection getCollection(String name)
   {
      for (Collection coll : collections)
      {
         if (name.equalsIgnoreCase(coll.getName()) //
               || name.equalsIgnoreCase(coll.getTableName()))
            return coll;
      }
      return null;
   }

   public Db getDb(String name)
   {
      if (name == null)
         return null;

      for (Db db : dbs)
      {
         if (name.equalsIgnoreCase(db.getName()))
            return db;
      }
      return null;
   }

   /**
    * @return the dbs
    */
   public List<Db> getDbs()
   {
      return new ArrayList(dbs);
   }

   /**
       * @param dbs the dbs to set
       */
   public Api withDbs(Db... dbs)
   {
      for (Db db : dbs)
         withDb(db);

      return this;
   }

   public Api withDb(Db db)
   {
      if (!dbs.contains(db))
      {
         dbs.add(db);

         for (Collection coll : (List<Collection>) db.getCollections())
         {
            withCollection(coll);
         }
      }

      return this;
   }

   public long getLoadTime()
   {
      return loadTime;
   }

   public void setLoadTime(long loadTime)
   {
      this.loadTime = loadTime;
   }

   public List<Endpoint> getEndpoints()
   {
      return new ArrayList(endpoints);
   }

   public Api withEndpoint(String methods, String includePaths, Action... actions)
   {
      Endpoint endpoint = new Endpoint(methods, includePaths, actions);
      withEndpoint(endpoint);
      return this;
   }

   public Api withEndpoints(Endpoint... endpoints)
   {
      for (Endpoint endpoint : endpoints)
         withEndpoint(endpoint);

      return this;
   }

   public Api withEndpoint(Endpoint endpoint)
   {
      if (!endpoints.contains(endpoint))
      {
         boolean inserted = false;
         for (int i = 0; i < endpoints.size(); i++)
         {
            if (endpoint.getOrder() < endpoints.get(i).getOrder())
            {
               endpoints.add(i, endpoint);
               inserted = true;
               break;
            }
         }

         if (!inserted)
            endpoints.add(endpoint);
      }
      return this;
   }

   public List<Action> getActions()
   {
      return new ArrayList(actions);
   }

   public Api withActions(String methods, String includePaths, Action... actions)
   {
      for (Action action : actions)
         withAction(methods, includePaths, action);

      return this;
   }

   public Api withActions(Action... actions)
   {
      for (Action action : actions)
         withAction(action);

      return this;
   }

   public Api withAction(String methods, String includePaths, Action action)
   {
      action.withMethods(methods);
      action.withIncludePaths(includePaths);

      if (!actions.contains(action))
         actions.add(action);

      return this;
   }

   public Api withAction(Action action)
   {
      if (!actions.contains(action))
         actions.add(action);

      return this;
   }

   public boolean isDebug()
   {
      return debug;
   }

   public void setDebug(boolean debug)
   {
      this.debug = debug;
   }

   public String getName()
   {
      return name;
   }

   public Api withName(String name)
   {
      this.name = name;
      return this;
   }

   public String getUrl()
   {
      return url;
   }

   public Api withUrl(String url)
   {
      this.url = url;
      return this;
   }

   public Api withApiListener(ApiListener listener)
   {
      if (!listeners.contains(listener))
         listeners.add(listener);
      return this;
   }

   public List<ApiListener> getApiListeners()
   {
      return Collections.unmodifiableList(listeners);
   }

}
