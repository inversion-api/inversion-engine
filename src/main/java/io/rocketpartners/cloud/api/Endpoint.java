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
package io.rocketpartners.cloud.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.rocketpartners.J;

public class Endpoint extends Rule
{
   protected String       path    = null;
   protected List<Action> actions = new ArrayList();

   public boolean matches(String method, String path)
   {
      if (!methods.contains(method))
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

   public void setApi(Api api)
   {
      this.api = api;
      api.addEndpoint(this);
   }

   public String getPath()
   {
      return path;
   }

   public void setPath(String path)
   {
      if (path != null)
      {
         path = path.toLowerCase();
         path = J.implode("/", J.explode("/", path));

         if (!J.empty(path) && !path.endsWith("/"))
            path += "/";
      }

      if (J.empty(path))
      {
         path = null;
      }

      this.path = path;
   }

   /**
    * @param classNames comma separated list of Handler classes that will be instantiated and passed to addHandler
    * @throws ClassNotFoundException 
    * @throws IllegalAccessException 
    * @throws InstantiationException 
    */
   public void setHandlerClass(String classNames) throws InstantiationException, IllegalAccessException, ClassNotFoundException
   {
      for (String name : J.explode(",", classNames))
      {
         addHandler((Handler) Class.forName(name).newInstance());
      }
   }

   public void addHandler(Handler handler)
   {
      Action a = new Action();
      a.setHandler(handler);
      addAction(a);
   }

   public List<Action> getActions(Request req)
   {
      List<Action> filtered = new ArrayList();
      for (Action a : actions)
      {
         if (a.matches(req.getMethod(), req.getPath()))
            filtered.add(a);
      }

      Collections.sort(filtered);
      return filtered;
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

      Collections.sort(actions);
   }

}
