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
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.rocketpartners.cloud.utils.Url;
import io.rocketpartners.cloud.utils.Utils;

public abstract class Rule<R extends Rule> implements Comparable<Rule>
{
   Api          api          = null;

   String       name         = null;
   int          order        = 1000;

   Set<String>  methods      = new HashSet();

   List<String> excludePaths = new ArrayList();
   List<String> includePaths = new ArrayList();

   Properties   config       = new Properties();

   @Override
   public int compareTo(Rule a)
   {
      return order <= a.order ? -1 : 1;
   }

   public String toString()
   {
      return System.identityHashCode(this) + " - " + name;
   }

   public abstract void setApi(Api api);
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
                     continue;

                  String regex = matchPart.substring(colonIdx + 1, matchPart.lastIndexOf("}"));

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
      //      wildcardPath = J.path(wildcardPath.replace('\\', '/'));
      //      path = J.path(path.replace('\\', '/'));
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

      if (methods.size() == 0 || methods.contains(method))
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

   public List<String> getMethods()
   {
      return new ArrayList(methods);
   }

   public void setMethods(String methods)
   {
      setMethods(Utils.explode(",", methods));
   }

   public void setMethods(List<String> methods)
   {
      this.methods.clear();
      for (String method : methods)
         addMethod(method);
   }

   public void addMethod(String method)
   {
      if (!methods.contains(method))
         methods.add(method);
   }

   public List<String> getIncludePaths()
   {
      return new ArrayList(includePaths);
   }

   public void setIncludePaths(List<String> includePaths)
   {
      this.includePaths.clear();
      for (String includePath : includePaths)
         addIncludePath(includePath);
   }

   public void addIncludePath(String includePath)
   {
      if (!includePaths.contains(includePath))
         includePaths.add(includePath);
   }

   public List<String> getExcludePaths()
   {
      return new ArrayList(excludePaths);
   }

   public void setExcludePaths(List<String> excludePaths)
   {
      this.excludePaths.clear();
      for (String excludePath : excludePaths)
         addExcludePath(excludePath);
   }

   public void addExcludePath(String excludePath)
   {
      if (!excludePaths.contains(excludePath))
         excludePaths.add(excludePath);
   }

   public String getName()
   {
      return name;
   }

   public void setName(String name)
   {
      this.name = name;
   }

   public int getOrder()
   {
      return order;
   }

   public void setOrder(int order)
   {
      this.order = order;
   }

   public String getConfig(String key)
   {
      return (String) config.get(key);
   }

   public String getConfig(String key, String defaultValue)
   {
      return config.getProperty(key, defaultValue);
   }

   public void setConfig(String queryString)
   {
      try
      {
         config.clear();
         config.putAll(Url.parseQuery(queryString));
         //config.load(new ByteArrayInputStream(propertiesString.getBytes()));
      }
      catch (Exception ex)
      {
         ex.printStackTrace();
      }
   }

   public R withExcludePaths(String... paths)
   {
      for (String path : Utils.explode(",", paths))
         addIncludePath(path);

      return (R) this;
   }

   public R withIncludePaths(String... paths)
   {
      for (String path : Utils.explode(",", paths))
         addIncludePath(path);

      return (R) this;
   }

   public R withMethods(String... methods)
   {
      for (String method : Utils.explode(",", methods))
         addMethod(method);

      return (R) this;
   }

   public R withOrder(int order)
   {
      setOrder(order);
      return (R) this;
   }
}
