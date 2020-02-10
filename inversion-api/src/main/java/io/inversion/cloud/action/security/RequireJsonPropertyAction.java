/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.cloud.action.security;

import java.util.HashSet;
import java.util.Set;

import io.inversion.cloud.model.Action;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.ApiException;
import io.inversion.cloud.model.Endpoint;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.model.SC;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.service.Engine;
import io.inversion.cloud.utils.Utils;

public class RequireJsonPropertyAction extends Action<RequireJsonPropertyAction>
{
   protected Set<String> properties = new HashSet();
   protected boolean     recursive  = false;

   public RequireJsonPropertyAction()
   {
      this(null, null, null);
   }

   public RequireJsonPropertyAction(String inludePaths)
   {
      this(inludePaths, null, null);
   }

   public RequireJsonPropertyAction(String inludePaths, String excludePaths, String config)
   {
      super(inludePaths, excludePaths, config);
   }

   public void run(Engine engine, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
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
               throw new ApiException(SC.SC_400_BAD_REQUEST, "Required property '" + prop + "' appears to be missing from your JSON body.");
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
