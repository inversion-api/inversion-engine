/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
 * http://rocketpartners.io
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
package io.rocketpartners.cloud.action.security;

import io.rocketpartners.cloud.model.Action;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.service.Request;
import io.rocketpartners.cloud.service.Response;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.JSArray;
import io.rocketpartners.cloud.utils.JSObject;

public class PasswordAction extends Action<PasswordAction>
{
   String passwordField = "password";

   @Override
   public void run(Service service, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
   {
      if (chain.getParent() != null)
      {
         // this must be a nested call to service.include so the outer call
         // is responsible for logging this change
         return;
      }

      JSObject json = req.getJson();

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
         JSObject js = res.getJson().getObject("data");
         if (js instanceof JSArray && ((JSArray) js).length() == 1)
         {
            JSObject user = (JSObject) ((JSArray) js).get(0);
            if (user.get("id") != null)
            {
               String encryptedPassword = AuthAction.hashPassword(user.get("id"), password);
               JSObject body = new JSObject(passwordField, encryptedPassword, "href", user.getString("href"));
               String url = Service.buildLink(req, req.getCollectionKey(), user.get("id"), null);
               service.put(url, body.toString());
            }
         }
      }
   }

}
