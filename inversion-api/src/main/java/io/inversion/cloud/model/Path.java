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
import java.util.regex.Pattern;

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

   public int size()
   {
      return parts.size();
   }

   public String toString()
   {
      return Utils.implode("/", parts);
   }

   public boolean equals(Object o)
   {
      if (o == null)
         return false;

      return o.toString().equals(toString());
   }

   public Path subpath(int fromIndex, int toIndex)
   {
      Path subpath = new Path(parts.subList(fromIndex, toIndex));
      return subpath;
   }

   public boolean isWildcard(int idx)
   {
      return "*".equals(get(idx));
   }

   public boolean isVar(int idx)
   {
      String part = get(idx);
      if (part != null)
      {
         if (part.startsWith("["))
            part = part.substring(1).trim();

         char c = part.charAt(0);
         return c == '$' || c == ':' || c == '{';
      }
      return false;
   }

   public String getVar(int idx)
   {
      String part = get(idx);
      if (part != null)
      {
         if (part.startsWith("["))
            part = part.substring(1, part.length() - 1).trim();

         int colon = part.indexOf(":");
         if (colon == 0)
            return part.substring(1).trim();
         else if (part.startsWith("{") && colon > 1)
            return part.substring(1, colon).trim();
      }
      return null;
   }

   public boolean matches(String toMatch)
   {
      return matches(new Path(toMatch));
   }

   public boolean matches(Path toMatch)
   {
      Path matchedPath = new Path();

      if (size() < toMatch.size() && !"*".equals(last()))
      {
         return false;
      }

      for (int i = 0; i < size(); i++)
      {
         String myPart = get(i);

         if (i == size() - 1 && myPart.equals("*"))
            return true;

         boolean optional = myPart.startsWith("[") && myPart.endsWith("]");

         if (i == toMatch.size())
         {
            if (optional)
               return true;
            return false;
         }

         if (optional)
            myPart = myPart.substring(1, myPart.length() - 1);

         String theirPart = toMatch.get(i);
         matchedPath.add(theirPart);

         if (myPart.startsWith(":"))
         {
            continue;
         }
         else if ((myPart.startsWith("{") || myPart.startsWith("${")) && myPart.endsWith("}"))
         {
            int nameStart = myPart.indexOf("{") + 1;
            int endName = myPart.indexOf(":");
            if (endName < 0)
               endName = myPart.length() - 1;

            String name = myPart.substring(nameStart, endName).trim();

            if (endName < myPart.length() - 1)
            {
               String regex = myPart.substring(endName + 1, myPart.length() - 1);
               Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
               if (!pattern.matcher(theirPart).matches())
               {
                  return false;
               }
            }
         }
         else if (!myPart.equalsIgnoreCase(theirPart))
         {
            return false;
         }

      }

      return true;
   }

   public Path extract(Map params, Path toMatch)
   {
      return extract(params, toMatch, false);
   }

   /**
    * If <code>greedy</code> is true, the match will consume
    * through matching optional path parts.  False indicates a 
    * reluctant match where the match finishes as soon as the first
    * optional or wildcard is hit.  A greedy match will still
    * not include a trailing wildcard.  
    * 
    * 
    * @param params
    * @param toMatch
    * @param greedy
    * @return
    */
   public Path extract(Map params, Path toMatch, boolean greedy)
   {
      Path matchedPath = new Path();

      boolean restOptional = false;
      int i = 0;
      int nextOptional = 0;
      for (i = 0; i < size() && toMatch.size() > 0; i++)
      {
         String myPart = get(i);

         boolean partOptional = myPart.startsWith("[") && myPart.endsWith("]");

         if (partOptional)
         {
            restOptional = true;
            myPart = myPart.substring(1, myPart.length() - 1);
         }

         if (myPart.equals("*"))
            break;

         String theirPart = null;

         if (greedy || !restOptional)
         {
            theirPart = toMatch.remove(0);
            matchedPath.add(theirPart);
         }
         else
         {
            theirPart = toMatch.get(nextOptional++);
         }

         if (myPart.startsWith(":"))
         {
            String name = myPart.substring(1).trim();
            params.put(name, theirPart);
         }
         else if ((myPart.startsWith("{") || myPart.startsWith("${")) && myPart.endsWith("}"))
         {
            int nameStart = myPart.indexOf("{") + 1;
            int endName = myPart.indexOf(":");
            if (endName < 0)
               endName = myPart.length() - 1;

            String name = myPart.substring(nameStart, endName).trim();

            if (endName < myPart.length() - 1)
            {
               String regex = myPart.substring(endName + 1, myPart.length() - 1);
               Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
               if (!pattern.matcher(theirPart).matches())
               {
                  ApiException.throw500InternalServerError("Attempting to extract values from an unmatched path: '%s', '%s'", this.parts.toString(), toMatch.toString());
               }
            }

            params.put(name, theirPart);
         }
         else if (!myPart.equalsIgnoreCase(theirPart))
         {
            ApiException.throw500InternalServerError("Attempting to extract values from an unmatched path: '%s', '%s'", this.parts.toString(), toMatch.toString());
         }
      }

      //null out any trailing vars
      for (i = i; i < size(); i++)
      {
         String var = getVar(i);
         if (var != null)
            params.put(var, null);
      }

      return matchedPath;
   }
}
