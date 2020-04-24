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

import java.util.HashMap;
import java.util.Map;

import io.inversion.Action;
import io.inversion.Request;
import io.inversion.Response;
import io.inversion.utils.JSNode;

public class SetJsonPropertyAction extends Action<SetJsonPropertyAction>
{
   protected Map<String, Object> properties = new HashMap();
   protected boolean             recursive  = false;

   public void run(Request req, Response res) throws Exception
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
