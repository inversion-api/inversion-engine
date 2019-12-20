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

import io.inversion.cloud.model.Action;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.JSArray;
import io.inversion.cloud.model.Endpoint;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.service.Engine;

public class PasswordAction extends Action<PasswordAction>
{
   String passwordField = "password";

   @Override
   public void run(Engine engine, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
   {
      if (chain.getParent() != null)
      {
         // this must be a nested call to service.include so the outer call
         // is responsible for logging this change
         return;
      }

      JSNode json = req.getJson();

      if (json == null)
         return;

      if (json instanceof JSArray)
         return;

      String password = (String) json.remove(passwordField);
      if (password == null)
      {
         return;
      }
      else
      {
         json.put(passwordField, "ENCRYPTING...");
      }

      try
      {
         chain.go();
      }
      finally
      {
         JSNode js = res.getJson().getNode("data");
         if (js instanceof JSArray && ((JSArray) js).length() == 1)
         {
            JSNode user = (JSNode) ((JSArray) js).get(0);
            if (user.get("id") != null)
            {
               String encryptedPassword = AuthAction.strongHash(user.get("id"), password);
               JSNode body = new JSNode(passwordField, encryptedPassword, "href", user.getString("href"));
               String url = Chain.buildLink(req.getCollection(), user.get("id"), null);
               engine.put(url, body.toString());
            }
         }
      }
   }

}
