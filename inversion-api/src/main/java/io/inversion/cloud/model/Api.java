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
import java.util.Hashtable;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inversion.cloud.service.Engine;

public class Api
{
   protected Logger                         log         = LoggerFactory.getLogger(getClass());

   transient Engine                         engine      = null;

   transient volatile boolean               started     = false;
   transient volatile boolean               starting    = false;
   transient long                           loadTime    = 0;
   transient Hashtable                      cache       = new Hashtable();
   transient protected String               hash        = null;

   protected boolean                        debug       = false;

   protected int                            id          = 0;

   protected String                         name        = null;
   protected String                         accountCode = null;
   protected String                         apiCode     = null;
   protected boolean                        multiTenant = false;
   protected String                         url         = null;

   protected List<Db>                       dbs         = new ArrayList();
   protected List<Endpoint>                 endpoints   = new ArrayList();
   protected List<Action>                   actions     = new ArrayList();

   protected List<Collection>               collections = new ArrayList();

   protected transient List<EngineListener> listeners   = new ArrayList();

   public Api()
   {
   }

   public Api(String name)
   {
      withName(name);
      withApiCode(name);
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
            db.startup();
         }

         //removeExcludes();
         started = true;

         for (EngineListener listener : listeners)
         {
            try
            {
               listener.onStartup(engine, this);
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
      for (io.inversion.cloud.model.Collection col : getCollections())
      {
         if (col.isExclude() || col.getEntity().isExclude())
         {
            removeCollection(col);
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

      for (Db db : getDbs())
      {
         for (Table table : (List<Table>) db.getTables())
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

   public int getId()
   {
      return id;
   }

   public Api withId(int id)
   {
      this.id = id;
      return this;
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

   public Table findTable(String name)
   {
      for (Db db : dbs)
      {
         Table t = db.getTable(name);
         if (t != null)
            return t;
      }
      return null;
   }

   public Db findDb(String name)
   {
      if (name == null)
         return null;

      for (Db db : dbs)
      {
         if (name.equals(db.getName()))
            return db;
      }
      return null;
   }

   public Collection getCollection(String name)
   {
      if (name == null)
         return null;

      for (Collection collection : collections)
         if (name.equalsIgnoreCase(collection.getName()))
            return collection;
      return null;
   }

   public Collection getCollection(Table tbl)
   {
      for (Collection collection : collections)
      {
         if (collection.getTable() == tbl)
            return collection;
      }
      return null;
   }

   public Collection getCollection(Entity entity)
   {
      for (Collection collection : collections)
      {
         if (collection.getEntity() == entity)
            return collection;
      }
      return null;
   }

   public Entity getEntity(Table table)
   {
      for (Collection collection : collections)
      {
         if (collection.getTable() == table)
            return collection.getEntity();
      }

      return null;
   }

   public Collection makeCollection(Table table, String name)
   {
      Collection collection = new Collection(this, table, name);
      withCollection(collection);
      return collection;
   }

   public List<Collection> getCollections()
   {
      return new ArrayList(collections);
   }

   public Api withCollections(Collection... collections)
   {
      for (Collection collection : collections)
         withCollection(collection);

      return null;
   }

   /**
    * Bi-directional method also sets 'this' api on the collection
    * @param collection
    */
   public Api withCollection(Collection collection)
   {
      if (!collections.contains(collection))
         collections.add(collection);

      if (collection.getApi() != this)
         collection.withApi(this);

      return this;
   }

   public void removeCollection(Collection collection)
   {
      collections.remove(collection);
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
         dbs.add(db);

      if (db.getApi() != this)
         db.withApi(this);

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

   public Api withEndpoint(String methods, String pathExpression, Action... actions)
   {
      return withEndpoint(methods, pathExpression, null, null, actions);
   }

   public Api withEndpoint(String methods, String endpointPath, String collectionPaths, Action... actions)
   {
      return withEndpoint(methods, endpointPath, collectionPaths, null, actions);
   }

   public Api withEndpoint(String methods, String endpointPath, String collectionPaths, String name, Action... actions)
   {
      Endpoint endpoint = new Endpoint(methods, endpointPath, collectionPaths, name, actions);
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

         if (endpoint.getApi() != this)
            endpoint.withApi(this);
      }
      return this;
   }

   public List<Action> getActions()
   {
      return new ArrayList(actions);
   }

   public Api withActions(Action... actions)
   {
      for (Action action : actions)
         withAction(action);

      return this;
   }

   public Api withAction(Action action)
   {
      if (!actions.contains(action))
         actions.add(action);

      if (action.getApi() != this)
         action.withApi(this);

      return this;
   }

   public <T extends Action> T makeAction(T action)
   {
      return makeAction(action, null, null);
   }

   public <T extends Action> T makeAction(T action, String methods, String includePaths)
   {
      action.withMethods(methods);
      action.withIncludePaths(includePaths);

      withAction(action);

      return action;
   }

   public boolean isDebug()
   {
      return debug;
   }

   public void setDebug(boolean debug)
   {
      this.debug = debug;
   }

   public String getApiCode()
   {
      return apiCode != null ? apiCode : name;
   }

   public Api withApiCode(String apiCode)
   {
      this.apiCode = apiCode;
      return this;
   }

   public Api withAccountCode(String accountCode)
   {
      this.accountCode = accountCode;
      return this;
   }

   public String getAccountCode()
   {
      return accountCode != null ? accountCode : getApiCode();
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

   public boolean isMultiTenant()
   {
      return multiTenant;
   }

   public Api withMultiTenant(boolean multiTenant)
   {
      this.multiTenant = multiTenant;
      return this;
   }

   public Object putCache(Object key, Object value)
   {
      return cache.put(key, value);
   }

   public Object getCache(Object key)
   {
      return cache.get(key);
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

   public Api withEngineListener(EngineListener listener)
   {
      if (!listeners.contains(listener))
         listeners.add(listener);
      return this;
   }

   public Engine getEngine()
   {
      return engine;
   }

   public Api withEngine(Engine engine)
   {
      this.engine = engine;
      return this;
   }

}
