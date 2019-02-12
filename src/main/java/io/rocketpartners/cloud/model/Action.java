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
import java.util.LinkedHashSet;
import java.util.List;

import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.service.Request;
import io.rocketpartners.cloud.service.Response;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.utils.J;
import io.rocketpartners.utils.JSArray;
import io.rocketpartners.utils.JSObject;

/**
 * @author wells
 */
public class Action<A extends Action> extends Rule<A>
{
   protected String comment = null;

   public void run(Service service, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
   {
      service(service, api, endpoint, this, chain, req, res);
   }

   public void service(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res) throws Exception
   {

   }

   @Override
   public void setApi(Api api)
   {
      this.api = api;
      api.addAction(this);
   }

   public String getComment()
   {
      return comment;
   }

   public void setComment(String comment)
   {
      this.comment = comment;
   }

   public A withComment(String comment)
   {
      setComment(comment);
      return (A) this;
   }

   public static List<JSObject> find(Object parent, String... paths)
   {
      List<JSObject> found = new ArrayList();
      for (String apath : paths)
      {
         for (String path : (List<String>) J.explode(",", apath))
         {
            find(parent, found, path, ".");
         }
      }
      return found;
   }

   public static void find(Object parent, List<JSObject> found, String targetPath, String currentPath)
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
         if (!found.contains(parent) && J.wildcardMatch(targetPath, currentPath))
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

   public static String getValue(Chain chain, String key)
   {
      //      if ("apiId".equalsIgnoreCase(key))
      //      {
      //         return chain.getRequest().getApi().getId() + "";
      //      }
      //      else 
      if ("apiCode".equalsIgnoreCase(key))
      {
         return chain.getRequest().getApi().getApiCode();
      }
      //      else if ("accountId".equalsIgnoreCase(key))
      //      {
      //         return chain.getRequest().getApi().getAccountId() + "";
      //      }
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

   public static LinkedHashSet<String> splitParam(Request req, String key)
   {
      LinkedHashSet map = new LinkedHashSet();
      String param = req.getParam(key);
      if (!J.empty(param))
      {
         String[] arr = param.split(",");
         for (String e : arr)
         {
            e = e.trim().toLowerCase();
            if (!J.empty(e))
               map.add(e);
         }
      }

      return map;
   }

   public static String nextPath(String path, String next)
   {
      return J.empty(path) ? next : path + "." + next;
   }

}
