/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
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
import java.util.List;
import java.util.Map;

import io.inversion.cloud.utils.Utils;

/**
 * A case insensitive utility abstraction for working with
 * forward slash based paths /like/you/find/in/urls.
 * 
 * 
 * {collection:books|customers}/{entity:[0-9a-fA-F]{1,8}}
 * [] -> optional
 * 
 * {name:regex}
 * ${name:regex}
 * :name
 * 
 * localhost://mycontainermap/{
 * 
 * 
 * 
 */

public class Path
{
   List<String> parts = new ArrayList();
   List<String> lc    = new ArrayList();

   public Path()
   {

   }
   
   public Path(Path path)
   {
      parts.addAll(path.parts);
      lc.addAll(path.lc);
   }

   public Path(String... path)
   {
      parts = Utils.explode("/", path);
      lc = new ArrayList(parts.size());
      for (int i = 0; i < parts.size(); i++)
      {
         lc.add(parts.get(i).toLowerCase());
      }
   }

   public Path(List<String> parts)
   {
      this.parts = new ArrayList(parts);
      this.lc = new ArrayList(parts.size());
      for (int i = 0; i < parts.size(); i++)
      {
         lc.add(parts.get(i).toLowerCase());
      }
   }

   public List<String> parts()
   {
      return new ArrayList(parts);
   }

   public String first()
   {
      if (parts.size() > 0)
         return parts.get(0);
      return null;
   }

   public String last()
   {
      if (parts.size() > 0)
         return parts.get(parts.size() - 1);
      return null;
   }

   public String get(int idx)
   {
      if (idx < parts.size())
         return parts.get(idx);
      return null;
   }


   public void add(String part)
   {
      if (!Utils.empty(part))
      {
         parts.add(part);
         lc.add(part.toLowerCase());
      }
   }

   public String remove(int index)
   {
      if (index < parts.size())
      {
         lc.remove(index);
         return parts.remove(index);
      }
      return null;
   }
   
   
   public String toString()
   {
      return Utils.implode("/", parts);
   }

   public boolean equals(Object o)
   {
      return o instanceof Path && o.toString().equals(toString());
   }

   public Path subpath(int fromIndex, int toIndex)
   {
      Path subpath = new Path(parts.subList(fromIndex, toIndex));
      return subpath;
   }

   public boolean isRegex()
   {
      for (String part : parts)
      {
         if (part.indexOf("*") > -1 || part.indexOf("{") > -1)
            return true;
      }
      return false;
   }

   public int size()
   {
      return parts.size();
   }

   public boolean isOptional(int index)
   {
      String part = get(index);
      if (part != null && (part.startsWith("[") || part.equalsIgnoreCase("*")))
      {
         return true;
      }
      return false;
   }

   public boolean isVariable(int index)
   {
      String part = get(index);

      if (part.startsWith("["))
         part = part.substring(1);

      if (part.equals("*") || part.startsWith("{") || part.startsWith("$") || part.startsWith(":"))
      {
         return true;
      }

      return false;
   }

   public boolean matchesLast(String toMatch)
   {
      return matches(size() - 1, toMatch);
   }

   public boolean matches(Path toMatch)
   {
      return matchesRest(0, toMatch);
   }

   public boolean matches(List toMatch)
   {
      return matchesRest(0, new Path(toMatch));
   }

   public boolean matchesRest(int matchFrom, Path toMatch)
   {
      for (int i = 0; i < lc.size(); i++)
      {
         String myPart = lc.get(i);

         if ("*".equals(myPart) && i == lc.size() - 1)
            return true;

         if (i + matchFrom >= toMatch.size())
         {
            if (myPart.equals("*"))
               return true;
            else
               return false;
         }

         String theirPart = toMatch.lc.get(i + matchFrom);

         if (i == lc.size() - 1 && myPart.equals("*"))
         {
            return true;
         }
         if (myPart.indexOf("*") > -1)
         {
            if (!Utils.wildcardMatch(myPart, theirPart))
               return false;
         }
         else
         {
            if (!myPart.equals(theirPart))
               return false;
         }
      }
      return toMatch.size() - matchFrom == lc.size();
   }

   private boolean matches(int index, String part)
   {
      if (index > -1 && index < lc.size())
      {
         String myPart = lc.get(index);
         if (myPart.indexOf("*") > -1)
         {
            return Utils.wildcardMatch(myPart, part);
         }
         else
         {
            return lc.get(index).equals(part);
         }

      }
      return false;
   }


   public Path extract(Map params, Path toMatch)
   {
      Path matchedPath = new Path();

      for (int i = 0; this.parts != null && i < this.size() && i < parts.size(); i++)
      {
         String myPart = toMatch.get(i);

         if (myPart.equals("*") || myPart.startsWith("["))
            break;

         String theirPart = toMatch.remove(0);
         matchedPath.add(theirPart);

         if (myPart.startsWith(":"))
         {
            String name = myPart.substring(1).trim();
            params.put(name, theirPart);
         }
         else if (myPart.startsWith("{"))
         {
            int colon = myPart.indexOf(":");
            if (colon > 0)
            {
               String name = myPart.substring(1, colon).trim();
               params.put(name, theirPart);
            }
         }
         else if (!myPart.equalsIgnoreCase(theirPart))
         {
            System.out.println("unmatced part:");
            //            ApiException.throw500InternalServerError("Attempting to extract values from an unmatched path: '%s', '%s'", this.parts.toString(), toMatch.toString());
         }

      }

      return null;

      //      try
      //      {
      //         if (wildcardPath.indexOf("{") > -1)
      //         {
      //            List<String> regexParts = Utils.explode("/", wildcardPath);
      //            List<String> pathParts = Utils.explode("/", path);
      //
      //            if (pathParts.size() > regexParts.size() && !regexParts.get(regexParts.size() - 1).endsWith("*"))
      //               return false;
      //
      //            boolean optional = false;
      //
      //            for (int i = 0; i < regexParts.size(); i++)
      //            {
      //               String matchPart = regexParts.get(i);
      //
      //               while (matchPart.startsWith("[") && matchPart.endsWith("]"))
      //               {
      //                  matchPart = matchPart.substring(1, matchPart.length() - 1);
      //                  optional = true;
      //               }
      //
      //               if (pathParts.size() == i)
      //               {
      //                  if (optional)
      //                     return true;
      //                  return false;
      //               }
      //
      //               String pathPart = pathParts.get(i);
      //
      //               if (matchPart.startsWith("{"))
      //               {
      //                  int colonIdx = matchPart.indexOf(":");
      //                  if (colonIdx < 0)
      //                     colonIdx = 0;
      //
      //                  String regex = matchPart.substring(colonIdx + 1, matchPart.lastIndexOf("}")).trim();
      //
      //                  Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
      //                  Matcher matcher = pattern.matcher(pathPart);
      //
      //                  if (!matcher.matches())
      //                     return false;
      //               }
      //               else if (!Utils.wildcardMatch(matchPart, pathPart))
      //               {
      //                  return false;
      //               }
      //
      //               if (matchPart.endsWith("*") && i == regexParts.size() - 1)
      //                  return true;
      //            }
      //
      //            return true;
      //         }
      //         else
      //         {
      //            if (!wildcardPath.endsWith("*") && !wildcardPath.endsWith("/") && path != null && path.endsWith("/"))
      //               wildcardPath += "/";
      //            return Utils.wildcardMatch(wildcardPath, path);
      //         }
      //      }
      //      catch (NullPointerException npe)
      //      {
      //         npe.printStackTrace();
      //      }
      //      catch (Exception ex)
      //      {
      //         //intentionally ignore
      //      }
      //
      //      return false;
   }
}
