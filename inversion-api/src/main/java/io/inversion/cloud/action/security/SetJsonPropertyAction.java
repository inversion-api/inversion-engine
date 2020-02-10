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

import java.util.HashMap;
import java.util.Map;

import io.inversion.cloud.model.Action;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.ApiException;
import io.inversion.cloud.model.Endpoint;
import io.inversion.cloud.model.JSArray;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.model.SC;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.service.Engine;
import io.inversion.cloud.utils.Utils;

public class SetJsonPropertyAction extends Action<SetJsonPropertyAction>
{
   protected Map<String, Object> properties = new HashMap();
   protected boolean             recursive  = false;
   

   public SetJsonPropertyAction()
   {
      this(null);
   }

   public SetJsonPropertyAction(String inludePaths)
   {
      this(inludePaths, null, null);
   }

   public SetJsonPropertyAction(String inludePaths, String excludePaths, String config)
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
            setProperties(node);
         }
      }
      else
      {
         setProperties(json);
      }
   }

   protected void setProperties(JSNode node)
   {
      for (String prop : properties.keySet())
      {
         for (JSNode aNode : node.asNodeList())
         {
            Object value = properties.get(prop);

            if (value instanceof JSNode)// you have to clone JSNodes so you don't end up with multiples of the same objects in the doc.
               value = JSNode.parseJson(value.toString());

            aNode.put(prop, value);
         }
      }
   }

   public SetJsonPropertyAction withProperty(String name, Object value)
   {
      properties.put(name, value);
      return this;
   }

   public SetJsonPropertyAction withRecursive(boolean recursive)
   {
      this.recursive = recursive;
      return this;
   }
}
