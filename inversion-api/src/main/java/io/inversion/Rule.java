/*
 * c * // * Copyright (c) 2015-2018 Rocket Partners, LLC
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

/**
 * Matches against an HTTP method and URL path to determine if the object
 * should be included when processing the associated Request.
 * <p>
 * Matching relies heavily on variablized Path matching via {@link io.inversion.Path.match} 
 *
 */
public abstract class Rule<R extends Rule> implements Comparable<R> {

   protected final transient Logger log             = LoggerFactory.getLogger(getClass().getName());

   /**
    * The name used for configuration and debug purposes.
    */
   protected String                 name            = null;

   /**
    * Rules are always processed in sequence sorted by ascending order.
    */
   protected int                    order           = 1000;

   /**
    * Method/path combinations that would cause this Rule to be included in the relevant processing.
    */
   protected List<RuleMatcher>      includeMatchers = new ArrayList();

   /**
    * Method/path combinations that would cause this Rule to be excluded from the relevant processing.
    */
   protected List<RuleMatcher>      excludeMatchers = new ArrayList();

   /**
    * {@code JSNode} is used because it implements a case insensitive map without modifying the keys
    */
   protected transient JSNode       configMap       = new JSNode();
   protected String                 configStr       = null;

   transient boolean                lazyConfiged    = false;

   public void checkLazyConfig() {
      //-- reluctant lazy config defaultIncludes if no other 
      //-- includes/excludes have been configured by the user.

      if (!lazyConfiged) {
         synchronized (this) {
            if (!lazyConfiged) {
               lazyConfiged = true;

               if (getIncludePaths().size() == 0 && getExcludePaths().size() == 0) {
                  withIncludeOn(getDefaultIncludeMatch());
               }
            }
         }
      }
   }

   /**
    * Designed to allow subclasses to provide a default match behavior
    * of no configuration was provided by the developer.
    * 
    * @return
    */
   protected RuleMatcher getDefaultIncludeMatch() {
      return new RuleMatcher(null, "*");
   }

   /**
    * Check if the http method and path match this Rule.
    * 
    * @param method the HTTP method to match
    * @param path the concrete path to match
    * @return true if the http method and path are included and not excluded
    */
   public boolean matches(String method, String path) {
      return matches(method, new Path(path));
   }

   /**
    * Check if the http method and path match this Rule.
    * 
    * @param method the HTTP method to match
    * @param path the concrete path to match
    * @return true if the http method and path are included and not excluded
    */
   public boolean matches(String method, Path path) {
      return match(method, path) != null;
   }

   /**
    * Find the first ordered Path that satisfies this method/path match.
    * 
    * @param method the HTTP method to match
    * @param path the concrete path to match
    * @return the first includeMatchers path to match when method also matches, null if no matches or excluded
    */
   protected Path match(String method, Path path) {
      checkLazyConfig();

      for (RuleMatcher excluder : excludeMatchers) {
         if (excluder.methods.size() > 0 && !excluder.methods.contains(method))
            continue;

         for (Path excludePath : excluder.paths) {
            if (excludePath.matches(path)) {
               return null;
            }
         }
      }

      int includePathCount = 0;

      for (RuleMatcher includer : includeMatchers) {
         includePathCount += includer.paths.size();

         if (includer.methods.size() > 0 && !includer.methods.contains(method))
            continue;

         for (Path includePath : includer.paths) {
            if (includePath.matches(path)) {
               return includePath;
            }
         }
      }

      //-- path was not excluded but config did not supply any include paths
      //-- so this is an implicit * include.
      if (includePathCount == 0) {
         return new Path("*");
      }

      return null;
   }

   public List<Path> getIncludePaths() {
      Set paths = new LinkedHashSet();
      for (RuleMatcher includer : includeMatchers) {
         paths.addAll(includer.paths);
      }
      return new ArrayList(paths);
   }

   public List<Path> getExcludePaths() {
      Set paths = new LinkedHashSet();
      for (RuleMatcher excluder : excludeMatchers) {
         paths.addAll(excluder.paths);
      }
      return new ArrayList(paths);
   }

   public List<RuleMatcher> getIncludeMatchers() {
      return new ArrayList(includeMatchers);
   }

   public R withIncludeOn(RuleMatcher matcher) {
      includeMatchers.add(matcher);
      return (R) this;
   }

   /**
    * Select this Rule when any method and path match.
    * 
    * @param one or more comma separated http method names, can be null to match on any
    * @param paths each path can be one or more comma separated variableized Paths
    * @return this
    */
   public R withIncludeOn(String methods, String... paths) {
      withIncludeOn(methods, asPathsArray(paths));
      return (R) this;
   }

   /**
    * Select this Rule when any method and path match.
    * 
    * @param one or more comma separated http method names, can be null to match on any
    * @param paths each path can be one or more comma separated variableized Paths
    * @return this
    */
   public R withIncludeOn(String methods, Path... paths) {
      if (includeMatchers.size() == 0) {
         includeMatchers.add(new RuleMatcher());
      }

      RuleMatcher m = includeMatchers.get(0);
      m.withMethods(methods);
      if (paths != null && paths.length > 0) {
         m.withPaths(Arrays.asList(paths));
      }

      return (R) this;
   }

   /**
    * Don't select this Rule when any method and path match.
    * 
    * @param one or more comma separated http method names, can be null to match on any
    * @param paths each path can be one or more comma separated variableized Paths
    * @return this
    */
   public R withExcludeOn(String methods, String... paths) {
      withExcludeOn(methods, asPathsArray(paths));
      return (R) this;
   }

   /**
    * Don't select this Rule when any method and path match.
    * 
    * @param one or more comma separated http method names, can be null to match on any
    * @param paths each path can be one or more comma separated variableized Paths
    * @return this
    */
   public R withExcludeOn(RuleMatcher matcher) {
      excludeMatchers.add(matcher);
      return (R) this;
   }

   public R withExcludeOn(String methods, Path... paths) {
      if (excludeMatchers.size() == 0) {
         excludeMatchers.add(new RuleMatcher());
      }

      RuleMatcher m = excludeMatchers.get(0);
      m.withMethods(methods);
      if (paths != null && paths.length > 0) {
         m.withPaths(Arrays.asList(paths));
      }

      return (R) this;
   }

   public List<RuleMatcher> getExcludeMatchers() {
      return new ArrayList(excludeMatchers);
   }

   public String getName() {
      return name;
   }

   public R withName(String name) {
      this.name = name;
      return (R) this;
   }

   public int getOrder() {
      return order;
   }

   public R withOrder(int order) {
      this.order = order;
      return (R) this;
   }

   public Set<String> getConfigKeys() {
      return new HashSet(configMap.keySet());
   }

   public String getConfig(String key) {
      return (String) configMap.get(key);
   }

   public String getConfig(String key, String defaultValue) {
      String value = configMap.getString(key);
      if (Utils.empty(value))
         value = defaultValue;

      return value;
   }

   public R withConfig(String queryString) {
      try {
         if (queryString != null) {
            configStr = configStr == null ? queryString : configStr + "&" + queryString;

            Map<String, String> parsed = Utils.parseQueryString(queryString);
            configMap.putAll(parsed);
         }
      } catch (Exception ex) {
         ex.printStackTrace();
      }
      return (R) this;
   }

   @Override
   public int compareTo(Rule a) {
      int compare = order == a.order ? 0 : order < a.order ? -1 : 1;
      return compare;
   }

   R clearIncludeRuleMatchers() {
      includeMatchers.clear();
      return (R) this;
   }

   R clearExcludeRuleMatchers() {
      excludeMatchers.clear();
      return (R) this;
   }

   public String toString() {
      StringBuffer buff = new StringBuffer();
      if (name != null)
         buff.append(name).append(" -");

      if (includeMatchers.size() > 0)
         buff.append(" includes: ").append(includeMatchers);

      if (excludeMatchers.size() > 0)
         buff.append(" exclude: ").append(excludeMatchers);

      return buff.toString();
   }

   static List<Path> asPathsList(String... paths) {
      List<Path> pathsList = new ArrayList();
      for (String path : Utils.explode(",", paths)) {
         pathsList.add(new Path(path));
      }
      return pathsList;
   }

   static Path[] asPathsArray(String... paths) {
      List<Path> list = asPathsList(paths);
      return list.toArray(new Path[list.size()]);

   }

   public static class RuleMatcher {

      protected Set<String> methods = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
      protected List<Path>  paths   = new ArrayList();

      public RuleMatcher() {

      }

      public RuleMatcher(String methods, String... paths) {
         this(methods, asPathsList(paths));
      }

      public RuleMatcher(String methods, Path path) {
         withMethods(methods);
         withPaths(path);
      }

      public RuleMatcher(String methods, List<Path> paths) {
         withMethods(methods);
         withPaths(paths);
      }

      public void withMethods(String... methods) {
         for (String method : Utils.explode(",", methods)) {
            if ("*".equals(method)) {
               this.methods.add("GET");
               this.methods.add("POST");
               this.methods.add("PUT");
               this.methods.add("PATCH");
               this.methods.add("DELETE");
               continue;
            }

            if ("read".equalsIgnoreCase(method)) {
               this.methods.add("GET");
               continue;
            }

            if ("write".equalsIgnoreCase(method)) {
               this.methods.add("POST");
               this.methods.add("PUT");
               this.methods.add("PATCH");
               this.methods.add("DELETE");
               continue;
            }

            this.methods.add(method.toUpperCase());
         }
      }

      public void withPaths(Path... paths) {
         this.paths.addAll(Arrays.asList(paths));
      }

      public void withPaths(List<Path> paths) {
         this.paths.addAll(paths);
      }

      public String toString() {
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

}
