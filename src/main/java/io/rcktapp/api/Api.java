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
package io.rcktapp.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;

import io.forty11.j.J;

public class Api extends Dto
{
   String                            name        = null;
   LinkedHashMap<String, String>     handlers    = new LinkedHashMap();
   LinkedHashMap<String, Db>         dbs         = new LinkedHashMap();
   List<Endpoint>                    endpoints   = new ArrayList();
   List<Action>                      actions     = new ArrayList();
   LinkedHashMap<String, Collection> collections = new LinkedHashMap();
   List<AclRule>                     acls        = new ArrayList();

   String                            apiCode     = null;
   String                            accountCode = null;

   boolean                           multiTenant = false;

   transient long                    loadTime    = 0;

   boolean                           reloadable  = false;
   boolean                           debug       = false;

   String                            url         = null;

   transient Hashtable               cache       = new Hashtable();

   public Api()
   {
   }

   public Api(String name)
   {
      this.name = name;
   }

   public Table findTable(String name)
   {
      for (Db db : dbs.values())
      {
         Table t = db.getTable(name);
         if (t != null)
            return t;
      }
      return null;
   }

   public Db findDb(String collection)
   {
      Collection c = getCollection(collection);
      if (c != null)
         return c.getEntity().getTable().getDb();

      return null;
   }

   public Entity getEntity(Table tbl)
   {
      for (Collection col : collections.values())
      {
         if (col.getEntity().getTable() == tbl)
            return col.getEntity();
      }
      return null;
   }

   public Attribute getAttribute(Column col)
   {
      for (Collection c : collections.values())
      {
         for (Attribute a : c.getEntity().getAttributes())
         {
            if (a.getColumn() == col)
               return a;
         }
      }
      return null;
   }

   public Collection getCollection(Entity entity)
   {
      for (Collection collection : collections.values())
      {
         if (entity.equals(collection.getEntity()))
            return collection;
      }
      return null;
   }

   public Collection getCollection(String name)
   {
      for (Collection col : collections.values())
      {
         if (name.equalsIgnoreCase(col.getName()))
            return col;

         for (String alias : col.getAliases())
            if (name.equalsIgnoreCase(alias))
               return col;

      }

      return null;
   }

   public Collection getCollection(Table tbl)
   {
      for (Collection col : collections.values())
      {
         if (col.getEntity().getTable().getName().equals(tbl.getName()))
            return col;
      }
      return null;
   }

   public List<Collection> getCollections()
   {
      return new ArrayList(collections.values());
   }

   public List<Entity> getEntities()
   {
      List<Entity> entities = new ArrayList();
      for (Collection col : collections.values())
      {
         entities.add(col.getEntity());
      }
      return entities;
   }

   public List<Relationship> getRelationships()
   {
      List<Relationship> rels = new ArrayList();
      for (Collection col : collections.values())
      {
         rels.addAll(col.getEntity().getRelationships());
      }
      return rels;
   }

   public List<Attribute> getAttributes()
   {
      List<Attribute> attrs = new ArrayList();
      for (Collection col : collections.values())
      {
         attrs.addAll(col.getEntity().getAttributes());
      }
      return attrs;
   }

   //   /**
   //    * @return the collections
   //    */
   //   public List<Collection> getCollections()
   //   {
   //      return collections;
   //   }

   //   /**
   //    * @param collections the collections to set
   //    */
   //   public void setCollections(List<Collection> collections)
   //   {
   //      this.collections = new ArrayList(collections.values());
   //   }
   //
   public void addCollection(Collection collection)
   {
      collections.put(collection.getName().toLowerCase(), collection);
   }

   public void removeCollection(Collection collection)
   {
      collections.remove(collection.getName().toLowerCase());
   }

   /**
    * @return the dbs
    */
   public List<Db> getDbs()
   {
      return new ArrayList(dbs.values());
   }

   /**
       * @param dbs the dbs to set
       */
   public void setDbs(List<Db> dbs)
   {
      for (Db db : dbs)
      {
         addDb(db);
      }
   }

   public void addDb(Db db)
   {
      String name = db.getName() != null ? db.getName() : "db";
      dbs.put(name.toLowerCase(), db);
      if (db.getApi() != this)
         db.setApi(this);
   }

   public Db getDb(String name)
   {
      return dbs.get(name.toLowerCase());
   }

   public void updateDbMap()
   {
      // This is needed because the Db objects coming into addDb don't have a name prop yet
      // during auto-wiring.. this is called just after auto-wiring to update the keys in the map
      List<Db> dbList = new ArrayList<>(dbs.values());
      this.dbs.clear();
      this.setDbs(dbList);
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
      if (actions.contains(action))
         actions.add(action);

      if (action.getApi() != this)
         action.setApi(this);
   }

   public void addAclRule(AclRule acl)
   {
      if (!acls.contains(acl))
      {
         acls.add(acl);
         Collections.sort(acls);
      }

      if (acl.getApi() != this)
         acl.setApi(this);
   }

   public void setAclRules(List<AclRule> acls)
   {
      this.acls.clear();
      for (AclRule acl : acls)
         addAclRule(acl);
   }

   public List<AclRule> getAclRules()
   {
      return new ArrayList(acls);
   }

   public String getName()
   {
      return name;
   }

   public void setName(String name)
   {
      this.name = name;
   }

   public void addHandler(String name, String clazz)
   {
      try
      {
         handlers.put(name.toLowerCase(), clazz);
      }
      catch (Exception ex)
      {
         throw new ApiException("Unable to add handler \"" + clazz + "\". Reason: " + J.getShortCause(ex));
      }
   }

   public void setHandlers(String handlers)
   {
      if (handlers == null)
         return;

      String[] parts = handlers.split(",");
      for (int i = 0; i < parts.length - 1; i += 2)
      {
         addHandler(parts[i].trim(), parts[i + 1].trim());
      }
   }

   public String getHandler(String name)
   {
      return handlers.get(name.toLowerCase());
   }

   public boolean isReloadable()
   {
      return reloadable;
   }

   public void setReloadable(boolean reloadable)
   {
      this.reloadable = reloadable;
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

}
