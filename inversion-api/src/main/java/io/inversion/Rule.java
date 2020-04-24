/*
 * // * Copyright (c) 2015-2018 Rocket Partners, LLC
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inversion.cloud.utils.Utils;

public abstract class Rule<R extends Rule> implements Comparable<Rule>
{
   protected final transient Logger log          = LoggerFactory.getLogger(getClass().getName());

   protected String                 name         = null;
   protected int                    order        = 1000;

   protected Set<String>            methods      = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
   protected List<Path>             excludePaths = new ArrayList();
   protected List<Path>             includePaths = new ArrayList();

   /**
    * JSMap is used because it implements a case insensitive map without modifying the keys
    */
   protected transient JSNode       configMap    = new JSNode();
   protected String                 configStr    = null;

   public boolean matches(String method, String path)
   {
      return matches(method, new Path(path));
   }

   public boolean matches(String method, Path path)
   {
      return match(method, path) != null;
   }

   public Path match(String method, Path path)
   {
      if (isMethod(method))
      {
         return match(path);
      }
      return null;
   }

   protected Path match(Path path)
   {
      //-- reluctant lazy config defaultIncludes if no other 
      //-- includes/excludes have been configured by the user.
      if (includePaths.size() == 0 && excludePaths.size() == 0)
      {
         synchronized (this)
         {
            if (includePaths.size() == 0 && excludePaths.size() == 0)
            {
               includePaths.add(getDefaultIncludes());
            }
         }
      }

      Path included = null;
      boolean excluded = false;

      if (includePaths.size() == 0)
      {
         if (excludePaths.size() == 0 || path.size() == 0)
            included = new Path("*");
      }
      else
      {
         for (Path includePath : includePaths)
         {
            if (includePath.matches(path))
            {
               included = includePath;
               break;
            }
         }
      }

      if (included != null && path.size() > 0)
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

      if (excluded)
         return null;

      return included;
   }

   public Rule clearIncludePaths()
   {
      includePaths.clear();
      return this;
   }

   public Rule clearExcludePaths()
   {
      excludePaths.clear();
      return this;
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

   public Path getDefaultIncludes()
   {
      return new Path("*");
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

   @Override
   public int compareTo(Rule a)
   {
      int compare = order == a.order ? 0 : order < a.order ? -1 : 1;
      return compare;
   }

   public String toString()
   {
      return System.identityHashCode(this) + " - " + name;
   }

}
