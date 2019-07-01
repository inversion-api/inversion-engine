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
package io.rocketpartners.cloud.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.Utils;

public class Api
{
   transient volatile boolean  started     = false;
   transient volatile boolean  starting    = false;
   transient long              loadTime    = 0;
   transient Hashtable         cache       = new Hashtable();
   transient protected String  hash        = null;

   protected transient Service service     = null;

   protected boolean           debug       = false;

   protected int               id          = 0;

   protected String            apiCode     = null;
   protected boolean           multiTenant = false;
   protected String            url         = null;

   protected List<Db>          dbs         = new ArrayList();
   protected List<Endpoint>    endpoints   = new ArrayList();
   protected List<Action>      actions     = new ArrayList();
   protected List<AclRule>     aclRules    = new ArrayList();

   protected List<Collection>  collections = new ArrayList();

   public Api()
   {
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
      for (io.rocketpartners.cloud.model.Collection col : getCollections())
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

   public void setService(Service service)
   {
      if (this.service != service)
      {
         this.service = service;
         service.addApi(this);
      }
   }

   public Service getService()
   {
      return service;
   }

   public int getId()
   {
      return id;
   }

   public void setId(int id)
   {
      this.id = id;
   }
   
   public Api withId(int id)
   {
      setId(id);
      return this;
   }

   public String getHash()
   {
      return hash;
   }

   public void setHash(String hash)
   {
      this.hash = hash;
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

   //   public Collection getCollection(String name)
   //   {
   //      return getCollection(name, null);
   //   }
   //
   ////   public Collection getCollection(String name, Class dbClass) throws ApiException
   //   {
   //      for (Collection collection : collections)
   //      {
   //         if (collection.getName().equalsIgnoreCase(name))
   //         {
   //            if (dbClass == null || dbClass.isAssignableFrom(collection.getDb().getClass()))
   //               return collection;
   //         }
   //      }
   //
   //      for (Collection collection : collections)
   //      {
   //         // This loop is done separately from the one above to allow 
   //         // collections to have precedence over aliases
   //         for (String alias : collection.getAliases())
   //         {
   //            if (name.equalsIgnoreCase(alias))
   //            {
   //               if (dbClass == null || dbClass.isAssignableFrom(collection.getDb().getClass()))
   //                  return collection;
   //            }
   //         }
   //      }
   //
   //      if (dbClass != null)
   //      {
   //         throw new ApiException(SC.SC_404_NOT_FOUND, "Collection '" + name + "' configured with Db class '" + dbClass.getSimpleName() + "' could not be found");
   //      }
   //      else
   //      {
   //         throw new ApiException(SC.SC_404_NOT_FOUND, "Collection '" + name + "' could not be found");
   //      }
   //   }

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

   public List<Collection> getCollections()
   {
      return new ArrayList(collections);
   }

   public void setCollections(List<Collection> collections)
   {
      this.collections.clear();
      for (Collection collection : collections)
         withCollection(collection);
   }

   public Collection withCollection(Table table, String name)
   {
      Collection collection = new Collection(this, table, name);
      return collection;
   }

   /**
    * Bi-directional method also sets 'this' api on the collection
    * @param collection
    */
   public Api withCollection(Collection collection)
   {
      //      if ("null".equals(collection.getName() + ""))
      //         System.out.println("asdfs");

      //      System.out.println(collection.getName());

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
   public void setDbs(List<Db> dbs)
   {
      for (Db db : dbs)
         addDb(db);
   }

   public void addDb(Db db)
   {
      if (!dbs.contains(db))
         dbs.add(db);

      if (db.getApi() != this)
         db.withApi(this);
   }

   public <T extends Db> T withDb(T db)
   {
      addDb(db);
      return db;
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

   public void setEndpoints(List<Endpoint> endpoints)
   {
      this.endpoints.clear();
      for (Endpoint endpoint : endpoints)
         withEndpoint(endpoint);
   }

   public Api withEndpoint(Endpoint endpoint)
   {
      if (!endpoints.contains(endpoint))
         endpoints.add(endpoint);

      if (endpoint.getApi() != this)
         endpoint.withApi(this);

      return this;
   }

   public Endpoint withEndpoint(String method, String includePaths)
   {
      return withEndpoint(method, null, includePaths);
   }

   public Endpoint withEndpoint(String method, String path, String includePaths)
   {
      Endpoint endpoint = new Endpoint().withMethods(method).withPath(path).withIncludePaths(includePaths);
      withEndpoint(endpoint);
      return endpoint;
   }

   public Endpoint withEndpoint(Action action, String method, String includePaths)
   {
      return withEndpoint(action, method, null, includePaths);
   }

   public Endpoint withEndpoint(Action action, String method, String path, String includePaths)
   {
      Endpoint endpoint = new Endpoint().withMethods(method).withPath(path).withIncludePaths(includePaths);
      endpoint.withAction(action);
      withEndpoint(endpoint);
      return endpoint;
   }

   public <T extends Action> T withAction(T action)
   {
      return withAction(action, null, null);
   }

   public <T extends Action> T withAction(T action, String methods, String includePaths)
   {
      for (String method : Utils.explode(",", methods))
      {
         action.withMethods(method);
      }

      for (String path : Utils.explode(",", includePaths))
      {
         action.withIncludePaths(path);
      }

      if (!actions.contains(action))
         actions.add(action);

      if (action.getApi() != this)
         action.withApi(this);

      return action;
   }

   public List<Action> getActions()
   {
      return new ArrayList(actions);
   }

   public void setActions(List<Action> actions)
   {
      this.actions.clear();
      for (Action action : actions)
         withAction(action);
   }

   public void addAclRule(AclRule acl)
   {
      if (!aclRules.contains(acl))
      {
         aclRules.add(acl);
         Collections.sort(aclRules);
      }

      if (acl.getApi() != this)
         acl.withApi(this);
   }

   public Api withAclRule(AclRule acl)
   {
      addAclRule(acl);
      return this;
   }

   public void setAclRules(List<AclRule> acls)
   {
      this.aclRules.clear();
      for (AclRule acl : acls)
         addAclRule(acl);
   }

   public List<AclRule> getAclRules()
   {
      return new ArrayList(aclRules);
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
      return apiCode;
   }

   public Api withApiCode(String apiCode)
   {
      this.apiCode = apiCode;
      return this;
   }

   public boolean isMultiTenant()
   {
      return multiTenant;
   }

   public void setMultiTenant(boolean multiTenant)
   {
      this.multiTenant = multiTenant;
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

}
