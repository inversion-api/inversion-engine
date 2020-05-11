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
package io.inversion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inversion.utils.JSNode;
import io.inversion.utils.Path;
import io.inversion.utils.Utils;

public abstract class Rule<R extends Rule> implements Comparable<Rule>
{
   protected final transient Logger log             = LoggerFactory.getLogger(getClass().getName());

   protected String                 name            = null;
   protected int                    order           = 1000;

   //   protected Set<String>            methods         = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
   //   protected List<Path>             excludePaths    = new ArrayList();
   //   protected List<Path>             includePaths    = new ArrayList();

   protected List<RuleMatcher>      includeMatchers = new ArrayList();
   protected List<RuleMatcher>      excludeMatchers = new ArrayList();

   /**
    * {@code JSNode} is used because it implements a case insensitive map without modifying the keys
    */
   protected transient JSNode       configMap       = new JSNode();
   protected String                 configStr       = null;

   static class RuleMatcher
   {
      protected Set<String> methods = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
      protected List<Path>  paths   = new ArrayList();

      RuleMatcher()
      {

      }

      RuleMatcher(String methods, String... paths)
      {
         this(methods, asPathsList(paths));
      }

      RuleMatcher(String methods, List<Path> paths)
      {
         withMethods(methods);
         withPaths(paths);
      }

      void withMethods(String... methods)
      {
         for (String method : Utils.explode(",", methods))
         {
            if ("*".equals(method))
            {
               this.methods.add("GET");
               this.methods.add("POST");
               this.methods.add("PUT");
               this.methods.add("PATCH");
               this.methods.add("DELETE");
               continue;
            }

            if ("read".equalsIgnoreCase(method))
            {
               this.methods.add("GET");
               continue;
            }

            if ("write".equalsIgnoreCase(method))
            {
               this.methods.add("POST");
               this.methods.add("PUT");
               this.methods.add("PATCH");
               this.methods.add("DELETE");
               continue;
            }

            this.methods.add(method.toUpperCase());
         }
      }

      void withPaths(List<Path> paths)
      {
         this.paths.addAll(paths);
      }

      public String toString()
      {
         StringBuffer buff = new StringBuffer();
         if (methods.size() == 0)
            buff.append("*");
         else
            buff.append(methods);

         buff.append(":");
         buff.append(paths);
         return buff.toString();
      }
   }

   public boolean matches(String method, String path)
   {
      return matches(method, new Path(path));
   }

   public boolean matches(String method, Path path)
   {
      return match(method, path) != null;
   }

   //   public Path match(String method, Path path)
   //   {
   //      if (isMethod(method))
   //      {
   //         return match(path);
   //      }
   //      return null;
   //   }

   boolean lazyConfiged = false;

   void checkLazyConfig()
   {
      //-- reluctant lazy config defaultIncludes if no other 
      //-- includes/excludes have been configured by the user.

      if (!lazyConfiged)
      {
         synchronized (this)
         {
            if (!lazyConfiged)
            {
               lazyConfiged = true;

               if (getIncludePaths().size() == 0 && getExcludePaths().size() == 0)
               {
                  withIncludePaths(getDefaultIncludes());
               }
            }
         }
      }
   }

   protected Path match(String method, Path path)
   {
      checkLazyConfig();

      for (RuleMatcher excluder : excludeMatchers)
      {
         if (excluder.methods.size() > 0 && !excluder.methods.contains(method))
            continue;

         for (Path excludePath : excluder.paths)
         {
            if (excludePath.matches(path))
            {
               return null;
            }
         }
      }

      int includePathCount = 0;

      for (RuleMatcher includer : includeMatchers)
      {
         includePathCount += includer.paths.size();
         
         if (includer.methods.size() > 0 && !includer.methods.contains(method))
            continue;

         for (Path includePath : includer.paths)
         {
            if (includePath.matches(path))
            {
               return includePath;
            }
         }
      }

      //-- path was not excluded but config did not supply any include paths
      //-- so this is an implicit * include.
      if (includePathCount == 0)
      {
         return new Path("*");
      }

      return null;
   }

   public Path getDefaultIncludes()
   {
      return new Path("*");
   }

   public List<Path> getIncludePaths()
   {
      Set paths = new LinkedHashSet();
      for (RuleMatcher includer : includeMatchers)
      {
         paths.addAll(includer.paths);
      }
      return new ArrayList(paths);
   }

   public List<Path> getExcludePaths()
   {
      Set paths = new LinkedHashSet();
      for (RuleMatcher excluder : excludeMatchers)
      {
         paths.addAll(excluder.paths);
      }
      return new ArrayList(paths);
   }

   public R withMethods(String... methods)
   {
      if (includeMatchers.size() == 0)
      {
         includeMatchers.add(new RuleMatcher());
      }

      RuleMatcher m = includeMatchers.get(0);
      m.withMethods(methods);

      return (R) this;
   }

   public R withIncludePaths(String... paths)
   {
      if (includeMatchers.size() == 0)
      {
         includeMatchers.add(new RuleMatcher());
      }

      RuleMatcher m = includeMatchers.get(0);
      m.withPaths(asPathsList(paths));

      return (R) this;
   }

   public R withIncludePaths(Path... paths)
   {
      if (includeMatchers.size() == 0)
      {
         includeMatchers.add(new RuleMatcher());
      }

      RuleMatcher m = includeMatchers.get(0);
      m.withPaths(Arrays.asList(paths));

      return (R) this;
   }

   public R withExcludePaths(String... paths)
   {
      if (excludeMatchers.size() == 0)
      {
         excludeMatchers.add(new RuleMatcher());
      }

      RuleMatcher m = excludeMatchers.get(0);
      m.withPaths(asPathsList(paths));

      return (R) this;
   }

   public R withExcludePaths(Path... paths)
   {
      if (excludeMatchers.size() == 0)
      {
         excludeMatchers.add(new RuleMatcher());
      }

      RuleMatcher m = excludeMatchers.get(0);
      m.withPaths(Arrays.asList(paths));

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

   public R withOrder(int order)
   {
      this.order = order;
      return (R) this;
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

   @Override
   public int compareTo(Rule a)
   {
      int compare = order == a.order ? 0 : order < a.order ? -1 : 1;
      return compare;
   }

   public String toString()
   {
      checkLazyConfig();

      StringBuffer buff = new StringBuffer();
      if (name != null)
         buff.append(name).append(" -");

      if (includeMatchers.size() > 0)
         buff.append(" includes: ").append(includeMatchers);

      if (excludeMatchers.size() > 0)
         buff.append(" exclude: ").append(excludeMatchers);

      return buff.toString();
   }

   R clearIncludePaths()
   {
      Set paths = new LinkedHashSet();
      for (RuleMatcher includer : includeMatchers)
      {
         includer.paths.clear();
      }
      return (R) this;
   }

   static List<Path> asPathsList(String... paths)
   {
      List<Path> pathsList = new ArrayList();
      for (String path : Utils.explode(",", paths))
      {
         pathsList.add(new Path(path));
      }
      return pathsList;
   }
}
