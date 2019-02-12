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

public class Api
{
   protected String           name        = null;
   boolean                    debug       = false;

   protected int              id          = 0;
   protected int              accountId   = 0;

   protected String           apiCode     = null;
   protected String           accountCode = null;
   protected boolean          multiTenant = false;
   protected String           url         = null;

   protected List<Db>         dbs         = new ArrayList();
   protected List<Endpoint>   endpoints   = new ArrayList();
   protected List<Action>     actions     = new ArrayList();
   protected List<AclRule>    aclRules    = new ArrayList();

   protected List<Collection> collections = new ArrayList();

   transient long             loadTime    = 0;
   protected String           hash        = null;

   transient Hashtable        cache       = new Hashtable();

   public Api()
   {
   }

   public Api(String name)
   {
      this.name = name;
   }

   public void startup()
   {
      for (Db db : dbs)
      {
         db.startup();
      }
   }

   public void shutdown()
   {
      for (Db db : dbs)
      {
         db.shutdown();
      }
   }

   public String getName()
   {
      return name;
   }

   public void setName(String name)
   {
      this.name = name;
   }

   public int getId()
   {
      return id;
   }

   public void setId(int id)
   {
      this.id = id;
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

   public Db findDb(String collection, Class dbClass)
   {
      Collection c = getCollection(collection, dbClass);
      if (c != null)
         return c.getEntity().getTable().getDb();

      return null;
   }

   public Collection getCollection(String name)
   {
      return getCollection(name, null);
   }

   public Collection getCollection(String name, Class dbClass) throws ApiException
   {
      for (Collection collection : collections)
      {
         if (collection.getName().equalsIgnoreCase(name))
         {
            if (dbClass == null || dbClass.isAssignableFrom(collection.getEntity().getTable().getDb().getClass()))
               return collection;
         }
      }

      for (Collection collection : collections)
      {
         // This loop is done separately from the one above to allow 
         // collections to have precedence over aliases
         for (String alias : collection.getAliases())
         {
            if (name.equalsIgnoreCase(alias))
            {
               if (dbClass == null || dbClass.isAssignableFrom(collection.getEntity().getTable().getDb().getClass()))
                  return collection;
            }
         }
      }

      if (dbClass != null)
      {
         throw new ApiException(SC.SC_404_NOT_FOUND, "Collection '" + name + "' configured with Db class '" + dbClass.getSimpleName() + "' could not be found");
      }
      else
      {
         throw new ApiException(SC.SC_404_NOT_FOUND, "Collection '" + name + "' could not be found");
      }
   }

   public Collection getCollection(Table tbl)
   {
      for (Collection collection : collections)
      {
         if (collection.getEntity().getTable() == tbl)
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
         if (collection.getEntity().getTable() == table)
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
         addCollection(collection);
   }

   /**
    * Bi-directional method also sets 'this' api on the collection
    * @param collection
    */
   public void addCollection(Collection collection)
   {
      //      if ("null".equals(collection.getName() + ""))
      //         System.out.println("asdfs");

      //      System.out.println(collection.getName());

      if (!collections.contains(collection))
         collections.add(collection);

      if (collection.getApi() != this)
         collection.setApi(this);
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
         db.setApi(this);
   }

   // public void addHandler(String name, String clazz)
   // {
   //    try
   //    {
   //       handlers.put(name.toLowerCase(), clazz);
   //    }
   //    catch (Exception ex)
   //    {
   //       throw new ApiException("Unable to add handler \"" + clazz + "\". Reason: " + J.getShortCause(ex));
   //    }
   // }
   //
   // public void setHandlers(String handlers)
   // {
   //    if (handlers == null)
   //       return;
   //
   //    String[] parts = handlers.split(",");
   //    for (int i = 0; i < parts.length - 1; i += 2)
   //    {
   //       addHandler(parts[i].trim(), parts[i + 1].trim());
   //    }
   // }

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
         addEndpoint(endpoint);
   }

   public void addEndpoint(Endpoint endpoint)
   {
      if (!endpoints.contains(endpoint))
         endpoints.add(endpoint);

      if (endpoint.getApi() != this)
         endpoint.setApi(this);
   }

   public List<Action> getActions()
   {
      return new ArrayList(actions);
   }

   public void setActions(List<Action> actions)
   {
      this.actions.clear();
      for (Action action : actions)
         addAction(action);
   }

   public void addAction(Action action)
   {
      if (!actions.contains(action))
         actions.add(action);

      if (action.getApi() != this)
         action.setApi(this);
   }

   public void addAclRule(AclRule acl)
   {
      if (!aclRules.contains(acl))
      {
         aclRules.add(acl);
         Collections.sort(aclRules);
      }

      if (acl.getApi() != this)
         acl.setApi(this);
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

   public void setApiCode(String apiCode)
   {
      this.apiCode = apiCode;
   }

   public String getAccountCode()
   {
      return accountCode;
   }

   public void setAccountCode(String accountCode)
   {
      this.accountCode = accountCode;
   }

   public boolean isMultiTenant()
   {
      return multiTenant;
   }

   public void setMultiTenant(boolean multiTenant)
   {
      this.multiTenant = multiTenant;
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

   public void setUrl(String url)
   {
      this.url = url;
   }

   public int getAccountId()
   {
      return accountId;
   }

   public void setAccountId(int accountId)
   {
      this.accountId = accountId;
   }

}
