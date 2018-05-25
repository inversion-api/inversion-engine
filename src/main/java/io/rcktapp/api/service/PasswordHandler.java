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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.rcktapp.api.service;

import io.forty11.web.js.JSArray;
import io.forty11.web.js.JSObject;
import io.rcktapp.api.Action;
import io.rcktapp.api.Api;
import io.rcktapp.api.Chain;
import io.rcktapp.api.Endpoint;
import io.rcktapp.api.Handler;
import io.rcktapp.api.Request;
import io.rcktapp.api.Response;
import io.rcktapp.api.service.AuthHandler;
import io.rcktapp.api.service.Service;

public class PasswordHandler implements Handler
{
   String passwordField = "password";
   String collectionKey = "users";

   @Override
   public void service(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res) throws Exception
   {
      if (chain.getParent() != null)
      {
         // this must be a nested call to service.include so the outer call
         // is responsible for logging this change
         return;
      }
      
      String password = (String) req.getJson().remove(passwordField);
      if (password == null) {
         return;
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
            if (user.get("id") != null) {
               String encryptedPassword = AuthHandler.hashPassword(user.get("id"), password);
               JSObject body = new JSObject(passwordField, encryptedPassword, "href", user.getString("href"));
               String url = Service.buildLink(req, collectionKey, null, null);
               service.include(chain, "PUT", url, body.toString());
            }
         }
      }
   }

}
