/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.cloud.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.inversion.cloud.service.Chain;
import io.inversion.cloud.utils.Utils;

/**
 * @author wells
 */
public abstract class Action<A extends Action> extends Rule<A>
{
   protected String comment = null;

   public Action()
   {

   }

   public Action(String includePaths)
   {
      withIncludePaths(includePaths);
   }

   public Action(String includePaths, String excludePaths, String config)
   {
      withIncludePaths(includePaths);
      withExcludePaths(excludePaths);
      withConfig(config);
   }

   public void run(Request req, Response res) throws Exception
   {
      if (req.isGet())
         doGet(req, res);
      else if (req.isPost())
         doPost(req, res);
      else if (req.isPut())
         doPut(req, res);
      else if (req.isPatch())
         doPatch(req, res);
      else if (req.isDebug())
         doDelete(req, res);
   }

   public void doGet(Request req, Response res) throws Exception
   {
      throw new ApiException(Status.SC_501_NOT_IMPLEMENTED, "Either exclude GET requests for this Action in your Api configuration or override run() or doGet().");
   }

   public void doPost(Request req, Response res) throws Exception
   {
      throw new ApiException(Status.SC_501_NOT_IMPLEMENTED, "Either exclude POST requests for this Action in your Api configuration or override run() or doPost().");
   }

   public void doPut(Request req, Response res) throws Exception
   {
      throw new ApiException(Status.SC_501_NOT_IMPLEMENTED, "Either exclude PUT requests for this Action in your Api configuration or override run() or doPut().");
   }

   public void doPatch(Request req, Response res) throws Exception
   {
      throw new ApiException(Status.SC_501_NOT_IMPLEMENTED, "Either exclude PATCH requests for this Action in your Api configuration or override run() or doPatch().");
   }

   public void doDelete(Request req, Response res) throws Exception
   {
      throw new ApiException(Status.SC_501_NOT_IMPLEMENTED, "Either exclude DELETE requests for this Action in your Api configuration or override run() or doDelete().");
   }

   @Override
   public A withApi(Api api)
   {
      if (this.api != api)
      {
         this.api = api;
         //intentionally not bidirectional because actions set 
         //directly on the Api are not private to an endpoint but
         //matched against all requests.
         // api.withAction(this);
      }
      return (A) this;
   }

   public String getComment()
   {
      return comment;
   }

   public A withComment(String comment)
   {
      this.comment = comment;
      return (A) this;
   }

   public static List<JSNode> find(Object parent, String... paths)
   {
      List<JSNode> found = new ArrayList();
      for (String apath : paths)
      {
         for (String path : (List<String>) Utils.explode(",", apath))
         {
            find(parent, found, path, ".");
         }
      }
      return found;
   }

   public static void find(Object parent, List<JSNode> found, String targetPath, String currentPath)
   {
      if (parent instanceof JSArray)
      {
         for (Object child : (JSArray) parent)
         {
            if (child instanceof JSNode)
               find(child, found, targetPath, currentPath);
         }
      }
      else if (parent instanceof JSNode)
      {
         if (!found.contains(parent) && Utils.wildcardMatch(targetPath, currentPath))
         {
            found.add((JSNode) parent);
         }

         for (String key : ((JSNode) parent).keySet())
         {
            Object child = ((JSNode) parent).get(key);
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
      else if ("tenantId".equalsIgnoreCase(key))
      {
         if (Chain.getUser() != null)
            return Chain.getUser().getTenantId() + "";
      }
      else if ("tenantCode".equalsIgnoreCase(key))
      {
         if (Chain.getUser() != null)
            return Chain.getUser().getTenantCode();
      }
      else if ("userId".equalsIgnoreCase(key))
      {
         if (Chain.getUser() != null)
            return Chain.getUser().getId() + "";
      }
      else if ("username".equalsIgnoreCase(key))
      {
         if (Chain.getUser() != null)
            return Chain.getUser().getUsername();
      }

      Object val = chain.get(key);
      if (val != null)
         return val.toString();
      return null;
   }

   public static String nextPath(String path, String next)
   {
      return Utils.empty(path) ? next : path + "." + next;
   }

   public String toString()
   {
      if (name != null)
         return name;

      String cn = getClass().getSimpleName();
      if (Utils.empty(cn))
         cn = getClass().getName();

      return cn;
   }
}
