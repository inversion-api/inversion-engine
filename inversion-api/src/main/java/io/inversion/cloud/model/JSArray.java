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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.inversion.cloud.utils.Utils;

public class JSArray extends JSNode implements Iterable
{
   List objects = new ArrayList();

   public JSArray(Object... objects)
   {
      if (objects != null && objects.length == 1 && objects[0].getClass().isArray())
      {
         objects = (Object[]) objects[0];
      }
      else if (objects != null && objects.length == 1 && java.util.Collection.class.isAssignableFrom(objects[0].getClass()))
      {
         objects = ((java.util.Collection) objects[0]).toArray();
      }

      for (int i = 0; objects != null && i < objects.length; i++)
         add(objects[i]);
   }

   public boolean isArray()
   {
      return true;
   }

   public Object get(int index)
   {
      if (index >= objects.size())
         return null;
      return objects.get(index);
   }

   public Set<String> keySet()
   {
      //TODO make more efficient!!!!
      LinkedHashSet set = new LinkedHashSet();
      for (int i = 0; i < objects.size(); i++)
      {
         set.add(i + "");
      }
      return set;
   }

   public JSNode getNode(int index)
   {
      return (JSNode) get(index + "");
   }

   @Override
   public Object get(Object index)
   {
      return get(Integer.parseInt(index.toString().trim()));
   }

   public Object set(int index, Object o)
   {
      while (objects.size() < index + 1)
         objects.add(null);

      return objects.set(index, o);
   }

   @Override
   public Object put(String index, Object value)
   {
      return set(Integer.parseInt(index.trim()), value);
   }

   public Object put(int index, Object value)
   {
      return set(index, value);
   }

   public Object remove(int index)
   {
      return objects.remove(index);
   }

   @Override
   public Object remove(Object index)
   {
      return remove(Integer.parseInt(index.toString().trim()));
   }

   public void addAll(JSArray array)
   {
      objects.addAll(array.asList());
   }

   public void add(int index, Object object)
   {
      objects.add(index, object);
   }

   public void add(Object object)
   {
      objects.add(object);
   }

   public String getString(int index)
   {
      return (String) get(index);
   }

   public JSNode getObject(int index)
   {
      return (JSNode) get(index);
   }

   public void setObject(int index, Object o)
   {
      objects.set(index, o);
   }

   public JSArray getArray(int index)
   {
      return (JSArray) get(index);
   }

   public boolean contains(Object object)
   {
      return objects.contains(object);
   }

   @Override
   public boolean isEmpty()
   {
      return objects.isEmpty();
   }

   @Override
   public void clear()
   {
      objects.clear();
   }

   @Override
   public int size()
   {
      return objects.size();
   }

   public int length()
   {
      return objects.size();
   }

   @Override
   public Iterator iterator()
   {
      return asList().iterator();
   }

   @Override
   public Collection values()
   {
      return asList();
   }

   /**
    * This is an exhaustive recursive optimizer to find the minimum
    * patches required for the diff.
    * 
    * TODO: how can this algo be optimized...pretty poor big as is
    */

   @Override
   protected JSArray diff(JSNode diffAgainst, String path, JSArray patches)
   {
      return diff(diffAgainst, path, patches, 0, 0);
   }

   protected JSArray diff(JSNode diffAgainst, String path, JSArray patches, int myStart, int theirStart)
   {
      JSArray otherArray = (JSArray) diffAgainst;

      if (otherArray.size() > size())//we have to remove some of ours
      {
         JSArray copy = new JSArray();
         copy.objects.addAll(otherArray.objects);

         int numToRemove = otherArray.size() - size();
         for (int i = 0; i < numToRemove; i++)
         {
            int minRemovalIdx = -1;
            JSArray minRemovalPatches = null;

            for (int j = copy.size() - 1; j >= 0; j--)
            {
               Object o = copy.remove(j);
               JSArray tempRemovedPatches = diff(copy, path, new JSArray());
               copy.add(j, o);

               if (minRemovalPatches == null || minRemovalPatches.size() > tempRemovedPatches.size())
               {
                  minRemovalIdx = j;
                  minRemovalPatches = tempRemovedPatches;
               }
            }
            copy.remove(minRemovalIdx);
            patches.add(new JSNode("op", "remove", "path", path + "." + minRemovalIdx));
         }

         diffAgainst = copy;

      }
      else if (otherArray.size() < size())//we have to add some of ours to them
      {
         JSArray copy = new JSArray();
         copy.objects.addAll(otherArray.objects);

         int numToAdd = size() - otherArray.size();

         for (int i = 0; i < numToAdd; i++)
         {
            JSArray minAddPatches = null;

            int copyIdx = theirStart;
            int myIdx = myStart;

            for (int j = myIdx; j < size(); j++)
            {
               for (int k = copyIdx; k <= copy.size(); k++)
               {
                  copy.add(k, get(j));
                  
                  JSArray tempAddPatches = diff(copy, path, new JSArray(), j + 1, k + 1);
                  if (minAddPatches == null || minAddPatches.size() > tempAddPatches.size())
                  {
                     minAddPatches = tempAddPatches;
                     myIdx = j;
                     copyIdx = k;
                  }
                  copy.remove(k);
               }
            }

            copy.add(copyIdx, get(myIdx));

            JSNode patch = new JSNode("op", "add", "path", path + "." + copyIdx, "value", get(myIdx));

            System.out.println(patch.toString(false));
            patches.add(patch);
            myIdx += 1;
            copyIdx += 1;
         }

         diffAgainst = copy;
      }

      return diff0(diffAgainst, path, patches);
   }

   /**
    * Overridden to reverse sort the array keys to generate patches in reverse
    * sequen
    * 
    * @param diffAgainst
    * @param path
    * @param patches
    * @return
    */
   protected JSArray diff0(JSNode diffAgainst, String path, JSArray patches)
   {
      return diff0(diffAgainst, path, patches, 0);
   }

   protected JSArray diff0(JSNode diffAgainst, String path, JSArray patches, int diffFrom)
   {
      //      JSArray otherArray = (JSArray) diffAgainst;
      //
      //      List<String> myKeys = new ArrayList(keySet());
      //      List<String> theirKeys = new ArrayList(otherArray.keySet());
      //
      //      if (myKeys.size() < theirKeys.size())
      //      {
      //         //-- if I have fewer keys than <code>diffAgainst</code> then 
      //         //-- you have to reverse the order of the keys to avoid generating
      //         //-- a patch list that when applied creates an ArrayIndexOutOfBoundsException.
      //         //-- meaning you can remove element 1 then 2 then 3 from an array of 
      //         //-- length 3 but you can remove element 3 then 2 then 1.
      //         Collections.reverse(myKeys);
      //         Collections.reverse(theirKeys);
      //      }

      for (int i = size() - 1; i >= diffFrom; i--)
      {
         String nextPath = Utils.implode(".", path, i);

         Object myVal = get(i);
         Object theirVal = diffAgainst.get(i);

         diff(nextPath, myVal, theirVal, patches);
      }

      //      for (String key : theirKeys)
      //      {
      //         Object myVal = get(key);
      //         Object theirVal = otherArray.get(key);
      //
      //         if (myVal == null && theirVal != null)
      //            patches.add(new JSNode("op", "remove", "path", Utils.implode(".", path, key)));
      //      }

      for (int i = size(); i < diffAgainst.size(); i++)
      {
         patches.add(new JSNode("op", "remove", "path", path + "." + i));
      }

      return patches;
   }

}
