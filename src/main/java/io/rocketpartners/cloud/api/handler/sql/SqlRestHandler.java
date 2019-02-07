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
package io.rocketpartners.cloud.api.handler.sql;

import io.rocketpartners.cloud.api.Action;
import io.rocketpartners.cloud.api.Api;
import io.rocketpartners.cloud.api.Chain;
import io.rocketpartners.cloud.api.Endpoint;
import io.rocketpartners.cloud.api.Handler;
import io.rocketpartners.cloud.api.Request;
import io.rocketpartners.cloud.api.Response;
import io.rocketpartners.cloud.api.service.Service;

public class SqlRestHandler implements Handler
{
   SqlGetHandler    get    = new SqlGetHandler();
   SqlDeleteHandler delete = new SqlDeleteHandler();
   SqlPostHandler   post   = new SqlPostHandler();

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
