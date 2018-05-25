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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.rcktapp.api;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import io.forty11.j.J;
import io.forty11.utils.CaseInsensitiveSet;

public class Api extends Dto
{
   long                              orgId       = 0;
   String                            name        = null;
   Set                               perms       = new CaseInsensitiveSet<String>();

   ArrayList<String>                 urls        = new ArrayList();
   LinkedHashMap<String, String>     handlers    = new LinkedHashMap();
   LinkedHashMap<String, Db>         dbs         = new LinkedHashMap();
   LinkedHashMap<String, Rule>       rules       = new LinkedHashMap();
   LinkedHashMap<String, Collection> collections = new LinkedHashMap();

   transient long                    loadTime    = 0;

   public Api()
   {
   }

   public Api(String name)
   {
      this.name = name;
   }

   public Db findDb(String collection)
   {
      Collection c = getCollection(collection);
      if (c != null)
         return c.getEntity().getTbl().getDb();

      return null;
   }

   public Entity getEntity(Tbl tbl)
   {
      for (Collection col : collections.values())
      {
         if (col.getEntity().getTbl() == tbl)
            return col.getEntity();
      }
      return null;
   }

   public Attribute getAttribute(Col col)
   {
      for (Collection c : collections.values())
      {
         for (Attribute a : c.getEntity().getAttributes())
         {
            if (a.getCol() == col)
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
         if (col.getName().equalsIgnoreCase(name))
            return col;
      }

      //throw new ApiException(SC.SC_404_NOT_FOUND, "Unknown collection \"" + name + "\"");
      return null;
   }

   public Collection getCollection(Tbl tbl)
   {
      for (Collection col : collections.values())
      {
         if (col.getEntity().getTbl().getName().equals(tbl.getName()))
            return col;
      }
      return null;
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
      dbs.put(db.getName().toLowerCase(), db);
   }

   public Db getDb(String name)
   {
      return dbs.get(name.toLowerCase());
   }

   public long getLoadTime()
   {
      return loadTime;
   }

   public void setLoadTime(long loadTime)
   {
      this.loadTime = loadTime;
   }

   public void addRule(Rule rule)
   {
      rules.put(rule.getName(), rule);
   }

   public Rule getRule(String name)
   {
      return rules.get(name.toLowerCase());
   }

   public List<Rule> getRules()
   {
      return new ArrayList(rules.values());
   }

   public void setRules(List<Rule> rules)
   {
      for (Rule rule : rules)
         addRule(rule);
   }

   public String getName()
   {
      return name;
   }

   public void setName(String name)
   {
      this.name = name;
   }

   public long getOrgId()
   {
      return orgId;
   }

   public void setOrgId(long orgId)
   {
      this.orgId = orgId;
   }

   public ArrayList<String> getUrls()
   {
      return urls;
   }

   public void setUrls(String urls)
   {
      if (urls == null)
         return;

      String[] parts = urls.split(",");
      for (int i = 0; i < parts.length; i++)
      {
         addUrl(parts[i]);
      }
   }

   public void setUrls(List<String> urls)
   {
      for (String url : urls)
      {
         addUrl(url);
      }
   }

   public void addUrl(String url)
   {
      if (!url.endsWith("/") && url.indexOf("*") < 0 && url.indexOf("?") < 0)
         url = url + "/";

      if (!urls.contains(url))
      {
         urls.add(url);
      }
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

   public boolean allowRole(User user, String perm)
   {
      if (user == null)
         return allowRole(Role.GUEST, perm);

      for (String role : user.getRoles())
      {
         if (allowRole(role, perm))
            return true;
      }
      return false;
   }

   public boolean allowRole(String role, String perm)
   {
      return perms.contains(role + "." + perm);
   }

}
