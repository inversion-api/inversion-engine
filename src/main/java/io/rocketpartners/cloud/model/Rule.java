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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.rocketpartners.cloud.utils.Utils;

public abstract class Rule<R extends Rule> implements Comparable<Rule>
{
   protected Api                  api          = null;

   protected String               name         = null;
   protected int                  order        = 1000;

   protected Set<String>          methods      = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

   protected List<Path>           excludePaths = new ArrayList();
   protected List<Path>           includePaths = new ArrayList();

   /**
    * ObjectNode is used because it implements a case insensitive map without modifying the keys
    */
   protected transient ObjectNode configMap    = new ObjectNode();
   protected String               configStr    = null;

   @Override
   public int compareTo(Rule a)
   {
      return order <= a.order ? -1 : 1;
   }

   public String toString()
   {
      return System.identityHashCode(this) + " - " + name;
   }

   public abstract R withApi(Api api);
   //   {
   //      this.api = api;
   //      api.addEndpoint(this);
   //   }

   public Api getApi()
   {
      return api;
   }

   public boolean matches(String method, Path path)
   {
      boolean included = false;
      boolean excluded = false;

      if (isMethod(method))
      {
         if (includePaths.size() == 0)
         {
            included = true;
         }
         else
         {
            for (Path includePath : includePaths)
            {
               if (includePath.matches(path))
               {
                  included = true;
                  break;
               }
            }
         }

         if (included)
         {
            for (Path excludePath : excludePaths)
            {
               if (excludePath.matches(path))
               {
                  excluded = true;
                  break;
               }
            }
         }
      }
      return included && !excluded;

   }

   public boolean isMethod(String... methods)
   {
      if (this.methods.size() == 0)
         return true;

      for (String method : methods)
      {
         if (method != null && this.methods.contains(method))
            return true;
      }
      return false;
   }

   public List<String> getMethods()
   {
      return new ArrayList(methods);
   }

   public R withMethods(String... methods)
   {
      if (methods == null)
         return (R) this;

      for (String method : Utils.explode(",", methods))
      {
         if (!this.methods.contains(method))
            this.methods.add(method);
      }
      return (R) this;
   }

   public List<Path> getIncludePaths()
   {
      return new ArrayList(includePaths);
   }

   public R withIncludePaths(String... paths)
   {
      if (paths != null)
      {
         for (String path : Utils.explode(",", paths))
         {
            includePaths.add(new Path(path));
         }
      }
      return (R) this;
   }

   public R withIncludePaths(Path... paths)
   {
      if (paths != null)
      {
         for (Path path : paths)
         {
            includePaths.add(path);
         }
      }
      return (R) this;
   }

   public List<Path> getExcludePaths()
   {
      return new ArrayList(excludePaths);
   }

   public R withExcludePaths(String... paths)
   {
      if (paths != null)
      {
         for (String path : Utils.explode(",", paths))
         {
            excludePaths.add(new Path(path));
         }
      }
      return (R) this;
   }

   public R withExcludePaths(Path... paths)
   {
      for (Path path : paths)
      {
         excludePaths.add(path);
      }
      return (R) this;
   }

   public String getName()
   {
      return name;
   }

   public R withName(String name)
   {
      this.name = name;
      return (R) this;
   }

   public int getOrder()
   {
      return order;
   }

   public Set<String> getConfigKeys()
   {
      return new HashSet(configMap.keySet());
   }

   public String getConfig(String key)
   {
      return (String) configMap.get(key);
   }

   public String getConfig(String key, String defaultValue)
   {
      String value = configMap.getString(key);
      if (Utils.empty(value))
         value = defaultValue;

      return value;
   }

   public R withConfig(String queryString)
   {
      try
      {
         if (queryString != null)
         {
            configStr = configStr == null ? queryString : configStr + "&" + queryString;

            Map<String, String> parsed = Utils.parseQueryString(queryString);
            configMap.putAll(parsed);
         }
      }
      catch (Exception ex)
      {
         ex.printStackTrace();
      }
      return (R) this;
   }

   public R withOrder(int order)
   {
      this.order = order;
      return (R) this;
   }

}
