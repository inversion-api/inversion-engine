/*
 * Copyright (c) 2015-2018 Inversion.org, LLC
 * https://github.com/inversion-api
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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
               String encryptedPassword = AuthAction.hashPassword(user.get("id"), password);
               JSNode body = new JSNode(passwordField, encryptedPassword, "href", user.getString("href"));
               String url = Chain.buildLink(req.getCollection(), user.get("id"), null);
               engine.put(url, body.toString());
            }
         }
      }
   }

}
