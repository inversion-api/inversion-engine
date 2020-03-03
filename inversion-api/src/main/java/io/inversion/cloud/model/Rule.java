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

import io.inversion.cloud.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public abstract class Rule<R extends Rule> implements Comparable<Rule>
{
   protected Logger           log          = LoggerFactory.getLogger(getClass().getName());

   protected String           name         = null;
   protected int              order        = 1000;

   protected Set<String>      methods      = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

   protected List<Path>       excludePaths = new ArrayList();
   protected List<Path>       includePaths = new ArrayList();

   /**
    * JSMap is used because it implements a case insensitive map without modifying the keys
    */
   protected transient JSNode configMap    = new JSNode();
   protected String           configStr    = null;

   @Override
   public int compareTo(Rule a)
   {
      return order <= a.order ? -1 : 1;
   }

   public String toString()
   {
      return System.identityHashCode(this) + " - " + name;
   }

   public boolean matches(String method, Path path)
   {
      if (isMethod(method))
      {
         return matchesPath(path);
      }
      return false;
   }

   public boolean matchesPath(Path path)
   {
      boolean included = false;
      boolean excluded = false;

      if (includePaths.size() == 0)
      {
         if (excludePaths.size() == 0 || path.size() == 0)
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

      if (included && path.size() > 0)
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

      return included && !excluded;
   }

   public boolean isMethod(String... methods)
   {
      if (this.methods.size() == 0)
         return true;

      if (this.methods.contains("*"))
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
         if ("*".equals(method))
         {
            withMethods("GET,PUT,POST,DELETE,PATCH");
            continue;
         }

         if ("read".equalsIgnoreCase(method))
         {
            withMethods("GET");
            continue;
         }

         if ("write".equalsIgnoreCase(method))
         {
            withMethods("PUT,POST,DELETE,PATCH");
            continue;
         }

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
