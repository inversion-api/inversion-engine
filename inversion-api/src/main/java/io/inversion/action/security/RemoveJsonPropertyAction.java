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

import java.util.HashSet;
import java.util.Set;

import io.inversion.Action;
import io.inversion.Request;
import io.inversion.Response;
import io.inversion.utils.JSNode;

public class RemoveJsonPropertyAction extends Action<RemoveJsonPropertyAction>
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
            removeProperties(node);
         }
      }
      else
      {
         removeProperties(json);
      }
   }

   protected void removeProperties(JSNode node)
   {
      for (String prop : properties)
      {
         for (JSNode aNode : node.asNodeList())
         {
            aNode.remove(prop);
         }
      }
   }

   public RemoveJsonPropertyAction withProperty(String name)
   {
      if (name != null)
         properties.add(name.toLowerCase());
      return this;
   }

   public RemoveJsonPropertyAction withRecursive(boolean recursive)
   {
      this.recursive = recursive;
      return this;
   }
}
