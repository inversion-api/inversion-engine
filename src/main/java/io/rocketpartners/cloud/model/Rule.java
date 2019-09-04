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

   protected List<String>         excludePaths = new ArrayList();
   protected List<String>         includePaths = new ArrayList();

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

   public static boolean pathMatches(String wildcardPath, String path)
   {
      try
      {
         if (wildcardPath.indexOf("{") > -1)
         {
            List<String> regexParts = Utils.explode("/", wildcardPath);
            List<String> pathParts = Utils.explode("/", path);

            if (pathParts.size() > regexParts.size() && !regexParts.get(regexParts.size() - 1).endsWith("*"))
               return false;

            boolean optional = false;

            for (int i = 0; i < regexParts.size(); i++)
            {
               String matchPart = regexParts.get(i);

               while (matchPart.startsWith("[") && matchPart.endsWith("]"))
               {
                  matchPart = matchPart.substring(1, matchPart.length() - 1);
                  optional = true;
               }

               if (pathParts.size() == i)
               {
                  if (optional)
                     return true;
                  return false;
               }

               String pathPart = pathParts.get(i);

               if (matchPart.startsWith("{"))
               {
                  int colonIdx = matchPart.indexOf(":");
                  if (colonIdx < 0)
                     colonIdx = 0;

                  String regex = matchPart.substring(colonIdx + 1, matchPart.lastIndexOf("}")).trim();

                  Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                  Matcher matcher = pattern.matcher(pathPart);

                  if (!matcher.matches())
                     return false;
               }
               else if (!Utils.wildcardMatch(matchPart, pathPart))
               {
                  return false;
               }

               if (matchPart.endsWith("*") && i == regexParts.size() - 1)
                  return true;
            }

            return true;
         }
         else
         {
            if (!wildcardPath.endsWith("*") && !wildcardPath.endsWith("/") && path != null && path.endsWith("/"))
               wildcardPath += "/";
            return Utils.wildcardMatch(wildcardPath, path);
         }
      }
      catch (NullPointerException npe)
      {
         npe.printStackTrace();
      }
      catch (Exception ex)
      {
         //intentionally ignore
      }

      return false;
      //      wildcardPath = Utils.path(wildcardPath.replace('\\', '/'));
      //      path = Utils.path(path.replace('\\', '/'));
      //
      //      if (!wildcardPath.endsWith("*") && !wildcardPath.endsWith("/"))
      //         wildcardPath += "/";
      //
      //      if (!path.endsWith("*") && !path.endsWith("/"))
      //         path += "/";
      //
      //      if (wildcardPath.startsWith("/"))
      //         wildcardPath = wildcardPath.substring(1, wildcardPath.length());
      //
      //      if (path.startsWith("/"))
      //         path = path.substring(1, path.length());

   }

   public boolean matches(String method, String path)
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
            for (String includePath : includePaths)
            {
               if (pathMatches(includePath, path))
               {
                  included = true;
                  break;
               }
            }
         }

         if (included)
         {
            for (String excludePath : excludePaths)
            {
               if (pathMatches(excludePath, path))
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

   public List<String> getIncludePaths()
   {
      return new ArrayList(includePaths);
   }

   public R withIncludePaths(String... paths)
   {
      if (paths != null)
      {
         for (String path : Utils.explode(",", paths))
         {
            if (!includePaths.contains(path))
               includePaths.add(path);
         }
      }
      return (R) this;
   }

   public List<String> getExcludePaths()
   {
      return new ArrayList(excludePaths);
   }

   public R withExcludePaths(String... paths)
   {
      if (paths != null)
      {
         for (String path : Utils.explode(",", paths))
         {
            if (!excludePaths.contains(path))
               excludePaths.add(path);
         }
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
