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

import io.rcktapp.api.Action;
import io.rcktapp.api.Api;
import io.rcktapp.api.Chain;
import io.rcktapp.api.Endpoint;
import io.rcktapp.api.Handler;
import io.rcktapp.api.Request;
import io.rcktapp.api.Response;

public class RestHandler implements Handler
{
   GetHandler    get    = new GetHandler();
   DeleteHandler delete = new DeleteHandler();
   PostHandler   post   = new PostHandler();

   @Override
   public void service(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res) throws Exception
   {
      String method = req.getMethod();
      if ("GET".equalsIgnoreCase(method))
      {
         get.service(service, api, endpoint, action, chain, req, res);
      }
      else if ("DELETE".equalsIgnoreCase(method))
      {
         delete.service(service, api, endpoint, action, chain, req, res);
      }
      else if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method))
      {
         post.service(service, api, endpoint, action, chain, req, res);
      }
   }
}
