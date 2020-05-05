/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
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
package io.inversion.action.security;

import java.util.List;

import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import io.inversion.Action;
import io.inversion.ApiException;
import io.inversion.Request;
import io.inversion.Response;
import io.inversion.utils.JSArray;
import io.inversion.utils.JSNode;
import io.inversion.utils.Utils;

public class RequireJsonPropertyAction<T extends RequireJsonPropertyAction> extends Action<T>
{
   protected ArrayListValuedHashMap<String, String> properties = new ArrayListValuedHashMap();
   protected boolean                                recursive  = false;

   public void run(Request req, Response res) throws ApiException
   {
      JSNode json = req.getJson();
      if (json == null)
         return;

      if (recursive)
      {
         for (JSNode node : json.findAllNodes("**/*"))
         {
            checkProperties(node);
         }
      }
      else
      {
         checkProperties(json);
      }
   }

   protected void checkProperties(JSNode node)
   {
      for (String jsonPath : properties.keySet())
      {
         List<String> props = properties.get(jsonPath);

         JSArray found = node.findAll(jsonPath);

         for (JSNode obj : found.asNodeList())
         {
            for (String prop : props)
            {
               checkProperty(obj, jsonPath, prop);
            }
         }
      }
   }

   protected void checkProperty(JSNode node, String jsonPath, String property)
   {
      Object value = node.get(property);
      if (Utils.empty(value))
         ApiException.throw400BadRequest("Required property '{}'.'{}' appears to be missing from your JSON body.", jsonPath, property);
   }

   public RequireJsonPropertyAction withProperty(String jsonPath, String property)
   {
      properties.put(jsonPath, property);
      return this;
   }

   public RequireJsonPropertyAction withRecursive(boolean recursive)
   {
      this.recursive = recursive;
      return this;
   }
}
