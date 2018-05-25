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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.rcktapp.api;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import io.forty11.j.J;

public abstract class Rule extends Dto implements Comparable<Rule>
{
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

   public static boolean pathMatches(String wildcardPath, String path)
   {
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

      return J.wildcardMatch(wildcardPath, path);
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

   public void setMethods(List<String> methods)
   {
      this.methods.clear();
      this.methods.addAll(methods);
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
         config.putAll(Request.parse(queryString));
         //config.load(new ByteArrayInputStream(propertiesString.getBytes()));
      }
      catch (Exception ex)
      {
         ex.printStackTrace();
      }
   }

}
