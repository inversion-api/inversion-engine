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

   public Endpoint(String method, String basePathStr, String includeRelativeSubPathsStr, Action... actions)
   {
      withMethods(method);
      withPath(basePathStr);
      withIncludePaths(includeRelativeSubPathsStr);

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

   public boolean matches(String method, String toMatch)
   {
      return matches(method, new Path(toMatch));
   }

   public boolean matches(String method, Path toMatch)
   {
      if (internal && Chain.getDepth() < 2)
      {
         return false;
      }

      boolean included = false;
      boolean excluded = false;

      if (isMethod(method))
      {
         int index = 0;
         for (index = 0; path != null && index < path.size(); index++)
         {
            if (!path.matches(index, toMatch))
               return false;
         }

         if (includePaths.size() == 0)
         {
            included = true;
         }
         else
         {
            for (Path includePath : includePaths)
            {
               if (includePath.matchesRest(index, toMatch))
               {
                  included = true;
                  break;
               }
            }
         }

         if (included && toMatch.size() > index)
         {
            for (Path excludePath : excludePaths)
            {
               if (excludePath.matchesRest(index, toMatch))
               {
                  excluded = true;
                  break;
               }
            }
         }
      }
      return included && !excluded;

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

      if (action.getApi() != getApi())
         action.withApi(getApi());

      return this;
   }

}
