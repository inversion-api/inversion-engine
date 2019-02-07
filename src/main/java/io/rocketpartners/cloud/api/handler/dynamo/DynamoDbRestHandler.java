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
package io.rocketpartners.cloud.api.handler.dynamo;

import io.rocketpartners.cloud.api.Action;
import io.rocketpartners.cloud.api.Api;
import io.rocketpartners.cloud.api.ApiException;
import io.rocketpartners.cloud.api.Chain;
import io.rocketpartners.cloud.api.Endpoint;
import io.rocketpartners.cloud.api.Handler;
import io.rocketpartners.cloud.api.Request;
import io.rocketpartners.cloud.api.Response;
import io.rocketpartners.cloud.api.SC;
import io.rocketpartners.cloud.api.service.Service;

/**
 * 
 * @author tc-rocket
 *
 */
public class DynamoDbRestHandler implements Handler
{
   DynamoDbGetHandler    get    = new DynamoDbGetHandler();
   DynamoDbDeleteHandler delete = new DynamoDbDeleteHandler();
   DynamoDbPostHandler   post   = new DynamoDbPostHandler();

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
      else
      {
         throw new ApiException(SC.SC_400_BAD_REQUEST, "This handler only supports GET, PUT, POST and DELETE requests");
      }
   }

   public void setGet(DynamoDbGetHandler get)
   {
      this.get = get;
   }

   public void setDelete(DynamoDbDeleteHandler delete)
   {
      this.delete = delete;
   }

   public void setPost(DynamoDbPostHandler post)
   {
      this.post = post;
   }

}
