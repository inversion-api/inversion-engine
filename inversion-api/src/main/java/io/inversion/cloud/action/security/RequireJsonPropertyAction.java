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
package io.inversion.cloud.action.security;

import io.inversion.cloud.model.*;
import io.inversion.cloud.utils.Utils;

import java.util.HashSet;
import java.util.Set;

public class RequireJsonPropertyAction extends Action<RequireJsonPropertyAction>
{
   protected Set<String> properties = new HashSet();
   protected boolean     recursive  = false;

   public void run(Request req, Response res) throws Exception
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
      for (String prop : properties)
      {
         for (JSNode aNode : node.asNodeList())
         {
            if (Utils.empty(aNode.get(prop)))
               ApiException.throw400BadRequest("Required property '%s' appears to be missing from your JSON body.",  prop);
         }
      }
   }

   public RequireJsonPropertyAction withProperty(String name)
   {
      if (name != null)
         properties.add(name.toLowerCase());
      return this;
   }

   public RequireJsonPropertyAction withRecursive(boolean recursive)
   {
      this.recursive = recursive;
      return this;
   }
}
