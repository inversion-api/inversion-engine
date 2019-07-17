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
import java.util.List;

import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.utils.Utils;

public class Endpoint extends Rule<Endpoint>
{
   protected String       path     = null;
   protected List<Action> actions  = new ArrayList();
   protected boolean      internal = false;

   public Endpoint()
   {

   }

   public Endpoint(String method, String path, String includePaths)
   {
      withMethod(method);
      withPath(path);
      withIncludePath(includePaths);
   }

   public String toString()
   {
      return "Endpoint: " + methods + " - '" + (path != null ? path : "/") + "' " + includePaths + " - " + excludePaths;
   }

   public boolean matches(String method, String path)
   {
      if (internal && Chain.getDepth() < 2)
      {
         return false;
      }

      if (!isMethod(method))
         return false;

      if (this.path != null && !path.toLowerCase().startsWith(this.path))
         return false;

      for (String includePath : includePaths)
      {
         if (pathMatches(includePath, path))
         {
            for (String excludePath : excludePaths)
            {
               if (pathMatches(excludePath, path))
               {
                  return false;
               }
            }
            return true;
         }
      }

      return this.path != null;
   }

   public Endpoint withApi(Api api)
   {
      if (this.api != api)
      {
         this.api = api;
         api.withEndpoint(this);
      }
      return this;
   }

   public String getPath()
   {
      return path;
   }

   public Endpoint withPath(String path)
   {
      if (path != null)
      {
         path = path.toLowerCase();
         path = Utils.implode("/", Utils.explode("/", path));

         if (!Utils.empty(path) && !path.endsWith("/"))
            path += "/";
      }

      if (Utils.empty(path))
      {
         path = null;
      }

      this.path = path;
      return this;
   }

   public Endpoint withInternal(boolean internal)
   {
      this.internal = internal;
      return this;
   }

   //   /**
   //    * @param classNames comma separated list of Handler classes that will be instantiated and passed to addHandler
   //    * @throws ClassNotFoundException 
   //    * @throws IllegalAccessException 
   //    * @throws InstantiationException 
   //    */
   //   public void setHandlerClass(String classNames) throws InstantiationException, IllegalAccessException, ClassNotFoundException
   //   {
   //      for (String name : Utils.explode(",", classNames))
   //      {
   //         addHandler((Handler) Class.forName(name).newInstance());
   //      }
   //   }
   //
   //   public void addHandler(Handler handler)
   //   {
   //      Action a = new Action();
   //      a.setHandler(handler);
   //      addAction(a);
   //   }

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

   public Endpoint withActions(List<Action> actions)
   {
      this.actions.clear();
      for (Action action : actions)
         withAction(action);

      return this;
   }

   public <T extends Action> Endpoint withAction(T action)
   {
      if (!actions.contains(action))
         actions.add(action);

      if (action.getApi() != getApi())
         action.withApi(getApi());

      Collections.sort(actions);

      return this;
   }

   public <T extends Action> Endpoint withAction(int order, T action)
   {
      action.withOrder(order);
      withAction(action);
      return this;
   }

   public Endpoint withAction(Action action, String methods, String includePaths)
   {
      action.withMethods(methods);
      action.withIncludePath(includePaths);
      withAction(action);
      return this;
   }

}
