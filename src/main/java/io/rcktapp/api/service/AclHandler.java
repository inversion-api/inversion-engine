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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import io.forty11.j.J;
import io.forty11.web.js.JSArray;
import io.forty11.web.js.JSObject;
import io.rcktapp.api.Acl;
import io.rcktapp.api.Action;
import io.rcktapp.api.Api;
import io.rcktapp.api.ApiException;
import io.rcktapp.api.Chain;
import io.rcktapp.api.Endpoint;
import io.rcktapp.api.Handler;
import io.rcktapp.api.Request;
import io.rcktapp.api.Response;
import io.rcktapp.api.SC;

public class AclHandler implements Handler
{
   @Override
   public void service(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response resp) throws Exception
   {
      LinkedHashMap<String, Acl> matched = new LinkedHashMap();
      boolean allowed = false;

      for (Acl acl : api.getAcls())
      {
         if (acl.ruleMatches(req))
         {
            if (!acl.isAllow())
            {
               allowed = false;
               break;
            }
            else if (!acl.isInfo() && acl.isAllow())
            {
               allowed = true;
            }

            //we only keep one acl for a given include path.  This is not a perfect
            //scheme but in theory it would allow api designers to have different restricted/requireds 
            //for different path/permission levels.
            String key = acl.getIncludePaths().toString();
            if (!matched.containsKey(key))
            {
               matched.put(key, acl);
            }
         }
      }

      if (!allowed)
      {
         throw new ApiException(SC.SC_403_FORBIDDEN);
      }

      Set requires = new HashSet();
      Set restricts = new HashSet();

      for (Acl acl : matched.values())
      {
         requires.addAll(acl.getRequires());
         restricts.addAll(acl.getRestricts());
      }

      cleanParams(chain, req, restricts, requires);
      cleanJson(chain, req.getJson(), restricts, requires, false);

      try
      {
         chain.go();
      }
      finally
      {
         JSObject json = resp.getJson();
         if (json != null)
         {
            List toClean = json instanceof JSArray ? ((JSArray) json).asList() : Arrays.asList(json);
            for (Object parent : toClean)
            {
               if (parent instanceof JSObject)
               {
                  cleanJson(chain, (JSObject) parent, restricts, Collections.EMPTY_SET, true);
               }
            }
         }
      }
   }

   void cleanParams(Chain chain, Request req, Set<String> restricts, Set<String> requires)
   {
      for (String restricted : restricts)
      {
         restricted = restricted.toLowerCase();

         if (restricted.startsWith("query.") || restricted.startsWith("*."))
            restricted = restricted.substring(restricted.indexOf(".") + 1, restricted.length());

         if (restricted.indexOf(".") > 0)
            continue;

         for (String key : req.getParams().keySet())
         {
            String value = req.getParam(key);
            if (matches(restricted, key) || matches(restricted, value))
            {
               throw new ApiException(SC.SC_400_BAD_REQUEST, "Unknown or invalid query param '" + key + "=" + value + "'.");
            }
         }
      }

      for (String required : requires)
      {
         required = required.toLowerCase();

         if (required.startsWith("query.") || required.startsWith("*."))
            required = required.substring(required.indexOf(".") + 1, required.length());

         if (required.indexOf(".") > 0)
            continue;

         boolean found = false;
         for (String key : req.getParams().keySet())
         {
            if (matches(required, key))
            {
               String value = req.getParam(key);
               if (J.empty(value))
               {
                  value = getValue(chain, key);
                  if (value != null)
                     req.putParam(key, value);
               }

               if (value != null)
               {
                  found = true;
                  break;
               }
            }
         }

         if (!found)
         {
            String value = getValue(chain, required);
            if (value != null)
            {
               req.putParam(required, value);
               found = true;
            }
         }

         if (!found)
         {
            throw new ApiException(SC.SC_400_BAD_REQUEST, "Missing required param '" + required + "'");
         }
      }
   }

   void cleanJson(Chain chain, JSObject json, Set<String> restricts, Set<String> requires, boolean silent)
   {
      if (json != null)
      {
         List objs = json instanceof JSArray ? ((JSArray) json).asList() : Arrays.asList(json);

         for (String path : restricts)
         {
            List<JSObject> found = new ArrayList();

            String parentPath = (path.lastIndexOf(".") < 0 ? "" : path.substring(0, path.lastIndexOf("."))).toLowerCase();
            String targetProp = path.lastIndexOf(".") < 0 ? path : path.substring(path.lastIndexOf(".") + 1, path.length());

            if (parentPath.startsWith("query."))
               continue;
            if (parentPath.startsWith("*."))
               parentPath = "*." + parentPath;
            if (!parentPath.startsWith("body."))
               parentPath = "body." + parentPath;

            for (Object parent : objs)
            {
               find(parent, found, parentPath, "body.");
            }

            for (JSObject target : found)
            {
               target.remove(targetProp);
               if (!silent)
               {
                  if (target.containsKey(targetProp))
                     throw new ApiException(SC.SC_400_BAD_REQUEST, "Unknown or invalid JSON property '" + path + "'.");
               }
            }
         }

         for (String path : requires)
         {
            List<JSObject> found = new ArrayList();

            String parentPath = (path.lastIndexOf(".") < 0 ? "" : path.substring(0, path.lastIndexOf("."))).toLowerCase();
            String targetProp = path.lastIndexOf(".") < 0 ? path : path.substring(path.lastIndexOf(".") + 1, path.length());

            if (parentPath.startsWith("query."))
               continue;
            if (parentPath.startsWith("*."))
               parentPath = "*." + parentPath;
            if (!parentPath.startsWith("body."))
               parentPath = "body." + parentPath;

            for (Object parent : objs)
            {
               find(parent, found, parentPath, "body.");
            }

            for (JSObject target : found)
            {
               if (target.keys().size() == 1 && target.containsKey("href"))
               {
                  //this is posting some type of reference not a json body, this 
                  //should not result in the entity being written
                  continue;
               }

               Object value = target.get(targetProp);
               if (value == null)
               {
                  value = getValue(chain, targetProp);

                  if (value != null)
                     target.put(targetProp, value);
                  else
                     throw new ApiException(SC.SC_400_BAD_REQUEST, "Required property '" + path + "' is missing from JSON body");

               }
            }
         }
      }
   }

   void find(Object parent, List<JSObject> found, String targetPath, String currentPath)
   {
      if (parent instanceof JSArray)
      {
         for (Object child : ((JSArray) parent).asList())
         {
            if (child instanceof JSObject)
               find(child, found, targetPath, currentPath);
         }
      }
      else if (parent instanceof JSObject)
      {
         if (J.wildcardMatch(targetPath, currentPath))
         {
            found.add((JSObject) parent);
         }

         for (String key : ((JSObject) parent).keySet())
         {
            Object child = ((JSObject) parent).get(key);
            String nextPath = currentPath == null || currentPath.length() == 0 ? key : currentPath + key.toLowerCase() + ".";
            find(child, found, targetPath, nextPath);
         }
      }
   }

   String getValue(Chain chain, String key)
   {
      if ("apiId".equalsIgnoreCase(key))
      {
         return chain.getRequest().getApi().getId() + "";
      }
      else if ("apiCode".equalsIgnoreCase(key))
      {
         return chain.getRequest().getApi().getApiCode();
      }
      else if ("accountId".equalsIgnoreCase(key))
      {
         return chain.getRequest().getApi().getAccountId() + "";
      }
      else if ("accountCode".equalsIgnoreCase(key))
      {
         return chain.getRequest().getApi().getAccountCode();
      }
      else if ("tenantId".equalsIgnoreCase(key))
      {
         if (chain.getRequest().getUser() != null)
            return chain.getRequest().getUser().getTenantId() + "";
      }
      else if ("tenantCode".equalsIgnoreCase(key))
      {
         if (chain.getRequest().getUser() != null)
            return chain.getRequest().getUser().getTenantCode();
      }
      else if ("userId".equalsIgnoreCase(key))
      {
         if (chain.getRequest().getUser() != null)
            return chain.getRequest().getUser().getId() + "";
      }
      else if ("username".equalsIgnoreCase(key))
      {
         if (chain.getRequest().getUser() != null)
            return chain.getRequest().getUser().getUsername();
      }

      Object val = chain.get(key);
      if (val != null)
         return val.toString();
      return null;
   }

   boolean matches(String restricted, String value)
   {
      return value.toLowerCase().matches(".*\\b" + restricted.toLowerCase() + "\\b.*");
   }
}
