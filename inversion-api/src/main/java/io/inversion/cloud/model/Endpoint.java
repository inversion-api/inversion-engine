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

import io.inversion.cloud.service.Chain;
import io.inversion.cloud.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Endpoint extends Rule<Endpoint>
{
   protected Path         path     = null;
   protected List<Action> actions  = new ArrayList();
   protected boolean      internal = false;

   public Endpoint()
   {

   }

   public Endpoint(String method, String pathExpression, Action... actions)
   {
      this(method, pathExpression, null, actions);
   }

   public Endpoint(String method, String endpointPath, String collectionPaths, Action... actions)
   {
      if (!Utils.empty(endpointPath) && !Utils.empty(collectionPaths))
      {
         Path ep = new Path(endpointPath);
         if (ep.matchesLast("*"))
         {
            endpointPath = ep.subpath(0, ep.size() - 1).toString();
         }
      }

      withMethods(method);
      withPath(endpointPath);
      withIncludePaths(collectionPaths);

      if (actions != null)
      {
         for (Action action : actions)
            withAction(action);
      }
   }

   public String toString()
   {
      if (name != null)
         return name;

      return (!Utils.empty(name) ? name + " " : "") + methods + " '/" + (!Utils.empty(path) ? path : "") + "' " + includePaths + " - " + excludePaths;
   }

   public boolean matches(String method, String fullPath)
   {
      Path path = new Path(fullPath);
      for (int i = 0; i <= path.size(); i++)
      {
         Path endpointPath = path.subpath(0, i);
         Path collectionPath = path.subpath(i, path.size());
         if (matches(method, endpointPath, collectionPath))
            return true;
      }
      return false;
   }

   public boolean matches(String method, String endpointPath, String collectionPath)
   {
      return matches(method, new Path(endpointPath), new Path(collectionPath));
   }

   public boolean matches(String method, Path endpointPath, Path collectionPath)
   {
      if (internal && Chain.getDepth() < 2)
      {
         return false;
      }

      if (isMethod(method))
      {
         if (path == null || !path.matches(endpointPath))
         {
            if (!path.matches(endpointPath))
               return false;
         }

         if (collectionPath.size() > 0 && includePaths.size() == 0 && excludePaths.size() == 0)
            return false;

         return super.matchesPath(collectionPath);
      }
      return false;
   }

   public Path getPath()
   {
      return path;
   }

   public Endpoint withPath(String path)
   {
      return withPath(new Path(path));
   }

   public Endpoint withPath(Path path)
   {
      if (path.isRegex())
      {
         Path newPath = new Path();
         List<String> pathParts = path.parts();
         for (int i = 0; i < pathParts.size(); i++)
         {
            String part = pathParts.get(i);
            if (part.indexOf("*") > -1 || part.indexOf("[") > -1 || part.indexOf("{") > -1)
            {
               Path subPath = new Path();

               for (int j = i; j < pathParts.size(); j++)
               {
                  subPath.addPart(pathParts.get(j));
               }
               withIncludePaths(subPath);

               break;
            }
            else
            {
               newPath.addPart(part);
            }
         }

         path = newPath;
      }

      this.path = path;

      return this;
   }

   public Endpoint withInternal(boolean internal)
   {
      this.internal = internal;
      return this;
   }

   public List<Action> getActions(Request req)
   {
      List<Action> filtered = new ArrayList();
      for (Action a : actions)
      {
         if (a.matches(req.getMethod(), req.getSubpath()))
            filtered.add(a);
      }

      Collections.sort(filtered);
      return filtered;
   }

   public List<Action> getActions()
   {
      return new ArrayList(actions);
   }

   public Endpoint withActions(Action... actions)
   {
      for (Action action : actions)
         withAction(action);

      return this;
   }

   public Endpoint withAction(Action action)
   {
      if (actions.contains(action))
         return this;

      boolean inserted = false;
      for (int i = 0; i < actions.size(); i++)
      {
         if (action.getOrder() < actions.get(i).getOrder())
         {
            actions.add(i, action);
            inserted = true;
            break;
         }
      }

      if (!inserted)
         actions.add(action);

      return this;
   }

}
