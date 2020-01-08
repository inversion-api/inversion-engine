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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.inversion.cloud.utils.Utils;

public class Path
{
   List<String> parts = null;
   List<String> lc    = null;

   public Path()
   {
      parts = new ArrayList();
      lc = new ArrayList();
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

   public String part(int idx)
   {
      if (idx < parts.size())
         return parts.get(idx);
      return null;
   }

   public String toString()
   {
      return Utils.implode("/", parts);
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

   public boolean startsWith(Path fullPath)
   {
      for (int i = 0; i < lc.size(); i++)
      {
         if (!fullPath.matches(i, lc.get(i)))
            return false;
      }
      return true;
   }

   public boolean matchFirst(String toMatch)
   {
      return matches(0, toMatch);
   }

   public boolean matchesLast(String toMatch)
   {
      return matches(size() - 1, toMatch);
   }

   public boolean matches(Path toMatch)
   {
      return matchesRest(0, toMatch);
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

   public boolean matches(int index, Path path)
   {
      if (index < path.size())
         return matches(index, path.lc.get(index));

      //matches a 'dir/*' to 'dir/'
      if (index >= path.size() && lc.get(lc.size() - 1).equals("*"))
         return true;

      return false;
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

   public void addPart(String part)
   {
      if (!Utils.empty(part))
      {
         parts.add(part);
         lc.add(part.toLowerCase());
      }
   }

   //   /**
   //    * Trims leading and trailing 
   //    * @param path
   //    * @return
   //    */
   //   public static String asPath(String path)
   //   {
   //      if (path != null)
   //      {
   //         path = path.trim().replaceAll("/+", "/");
   //
   //         if (path.startsWith("/"))
   //            path = path.substring(1, path.length());
   //
   //         if (path.endsWith("/"))
   //            path = path.substring(0, path.length() - 1);
   //      }
   //      
   //      if (Utils.empty(path))
   //      {
   //         path = null;
   //      }
   //
   //      return path;
   //   }

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
   }
}
