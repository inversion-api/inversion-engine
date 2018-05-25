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
import java.util.List;

import io.forty11.j.J;
import io.forty11.js.JS;
import io.forty11.js.JSObject;
import io.forty11.utils.CaseInsensitiveSet;

public class Rule extends Dto implements Comparable<Rule>
{
   public static float        DEFAULT_ORDER    = 1000;
   public static float        DEFAULT_PRIORITY = 0;

   CaseInsensitiveSet<String> methodsSet       = null;
   CaseInsensitiveSet<String> pathsSet         = null;
   List<String>               permsList        = null;
   CaseInsensitiveSet<String> rolesSet         = null;

   Org                        org              = null;

   //Api                        api              = null;

   String                     name             = null;

   String                     methods          = null;

   String                     paths            = null;

   boolean                    negate           = false;

   boolean                    meta             = false;

   boolean                    terminate        = false;

   float                      order            = DEFAULT_ORDER;

   float                      priority         = DEFAULT_PRIORITY;

   String                     handler          = null;

   //requires a User object to have these perms
   String                     perms            = null;

   String                     roles            = null;

   String                     status           = SC.SC_200_OK;

   boolean                    deny             = false;

   String                     params           = null;

   JSObject                   config           = null;

   /**
    * A query string of overrideable name/value pairs
    */
   String                     defaults         = null;

   /**
    * A query string of overrideable name/value pairs
    */
   String                     absolutes        = null;

   /**
    * A select statement that GetHandler will use as
    * its seed select statement
    */
   String                     select           = null;

   /**
    * A sql statement used by GetHandler,DeleteHandler,
    * PostPutHandler to filter the entity 
    * ids that are about to be operated on.  Enables
    * resource level security.  
    * 
    *  @see io.rcktapp.api.service.SqlHandler.filterIds()
    */
   String                     filter           = null;

   //   /**
   //    * Tells GetHandler that it should treat the 
   //    * results of of its query as if were of 
   //    * this type.  Useful when you have a rule
   //    * that is not mapped to a specific collection
   //    * path and uses a <code>select</code> property
   //    * to get the rows but you still want to 
   //    * preserve relationships in the output
   //    */
   //   String                collection       = null;
   //
   //   /**
   //    * Specifically only include these properties
   //    * in the output
   //    */
   //   String                includes         = null;
   //
   //   /**
   //    * Include all properties but these in the output
   //    */
   //   String                excludes         = null;
   //
   //   /**
   //    * Tells GetHandler to expand these relationships
   //    * for example person.addresses would cause all
   //    * related address entites to be included in the 
   //    * output document
   //    */
   //   String                expands          = null;
   //
   //   /**
   //    * 
   //    */
   //   String                format           = null;
   //
   //   /**
   //    * Tells GetHandler that these query params
   //    * should not be parsed as RQL
   //    */
   //   String                ignores          = null;

   public Rule()
   {

   }

   public Rule(String name)
   {
      setName(name);
   }

   public Rule(String name, String method, String path, String handler)
   {
      setName(name);
      setMethods(method);
      setPaths(paths);
      setHandler(handler);
   }

   public boolean matches(Request req) throws ApiException
   {
      boolean matches = true;

      if (!J.empty(methods) && !hasMethod(req.getMethod()))
      {
         matches = false;
      }
      else if (!J.empty(paths) && !hasPath(req.getPath()))
      {
         matches = false;
      }

      if (negate)
         matches = !matches;

      return matches;
   }

   public boolean isAuthroized(Request req)
   {
      if (deny)
         return false;

      if ((!J.empty(perms)) && req.getUser() == null)
         return false;

      Api api = req.getApi();
      User user = req.getUser();

      //ex guest.get, member.post
      if (!allowRole(user) && !api.allowRole(user, req.getMethod()))
         return false;

      if (!J.empty(perms))
      {
         for (String perm : allPerms())
         {
            if (!user.hasPerm(perm))
               return false;
         }
      }

      return true;
   }

   public boolean allowRole(User user)
   {
      if (user == null)
         return hasRole(Role.GUEST);

      for (String role : user.getRoles())
      {
         if (hasRole(role))
            return true;
      }
      return false;
   }

   public boolean hasMethod(String val)
   {
      if (methodsSet == null)
      {
         methodsSet = toSet(methods);
      }
      return methodsSet.contains(val.toLowerCase());
   }

   public boolean hasPath(String val)
   {
      if (pathsSet == null)
      {
         pathsSet = toSet(paths);
      }

      for (String wildcard : pathsSet)
      {
         if (J.wildcardMatch(wildcard, val))
            return true;
      }

      return false;
   }

   CaseInsensitiveSet toSet(String string)
   {
      CaseInsensitiveSet set = new CaseInsensitiveSet();

      if (string == null)
         return set;

      String[] parts = string.toLowerCase().split(",");
      for (int i = 0; i < parts.length; i++)
      {
         String part = parts[i].trim();
         if (!J.empty(part))
            set.add(part);
      }
      return set;
   }

   @Override
   public int compareTo(Rule o)
   {
      if (order == o.order)
         return priority > o.priority ? 1 : -1;

      return order > o.order ? 1 : -1;
   }

   public Org getOrg()
   {
      return org;
   }

   public void setOrg(Org org)
   {
      this.org = org;
   }

   public boolean isNegate()
   {
      return negate;
   }

   public void setNegate(boolean negate)
   {
      this.negate = negate;
   }

   public float getOrder()
   {
      return order;
   }

   public void setOrder(float order)
   {
      this.order = order;
   }

   public float getPriority()
   {
      return priority;
   }

   public void setPriority(float priority)
   {
      this.priority = priority;
   }

   public String getStatus()
   {
      return status;
   }

   public void setStatus(String status)
   {
      this.status = status;
   }

   public String getMethods()
   {
      return methods;
   }

   public void setMethods(String methods)
   {
      this.methods = methods;
   }

   public String getPaths()
   {
      return paths;
   }

   public void setPaths(String paths)
   {
      this.paths = paths;
   }

   public String getHandler()
   {
      return handler;
   }

   public void setHandler(String handler)
   {
      this.handler = handler;
   }

   public List<String> allPerms()
   {
      if (permsList == null)
      {
         permsList = new ArrayList(toSet(perms));
      }
      return permsList;
   }

   public boolean hasRole(String role)
   {
      if (rolesSet == null)
      {
         rolesSet = toSet(roles);
      }
      return rolesSet.contains(role);
   }

   public void setRoles(String roles)
   {
      this.roles = roles;
   }

   public void setPerms(String perms)
   {
      permsList = null;
      this.perms = perms;
   }

   public boolean isTerminate()
   {
      return terminate;
   }

   public void setTerminate(boolean terminate)
   {
      this.terminate = terminate;
   }

   public JSObject getConfig()
   {
      if (params != null && config == null)
      {
         config = (JSObject) JS.toObject(params);
      }

      return config;
   }

   public void setConfig(JSObject config)
   {
      this.config = config;
   }

   public String getName()
   {
      return name;
   }

   public void setName(String name)
   {
      this.name = name;
   }

   public String getDefaults()
   {
      return defaults;
   }

   public void setDefaults(String defaults)
   {
      this.defaults = defaults;
   }

   public String getAbsolutes()
   {
      return absolutes;
   }

   public void setAbsolutes(String absolutes)
   {
      this.absolutes = absolutes;
   }

   public String getSelect()
   {
      return select;
   }

   public void setSelect(String select)
   {
      this.select = select;
   }

   public String getFilter()
   {
      return filter;
   }

   public void setFilter(String filter)
   {
      this.filter = filter;
   }

   public boolean isMeta()
   {
      return meta;
   }

   public void setMeta(boolean meta)
   {
      this.meta = meta;
   }

   public String getParams()
   {
      return params;
   }

   public void setParams(String params)
   {
      this.params = params;
   }

}
