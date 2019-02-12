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
package io.rocketpartners.cloud.handler.dynamo;

import io.rocketpartners.cloud.model.Action;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.ApiException;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.service.Request;
import io.rocketpartners.cloud.service.Response;
import io.rocketpartners.cloud.service.Service;

/**
 * 
 * @author tc-rocket
 *
 */
public class DynamoDbRestAction<T extends DynamoDbRestAction> extends Action<T>
{
   DynamoDbGetAction    get    = new DynamoDbGetAction();
   DynamoDbDeleteAction delete = new DynamoDbDeleteAction();
   DynamoDbPostAction   post   = new DynamoDbPostAction();

   @Override
   public void service(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res) throws Exception
   {
      if (req.isMethod("get"))
      {
         get.service(service, api, endpoint, action, chain, req, res);
      }
      else if (req.isMethod("delete"))
      {
         delete.service(service, api, endpoint, action, chain, req, res);
      }
      else if (req.isMethod("post", "put"))
      {
         post.service(service, api, endpoint, action, chain, req, res);
      }
      else
      {
         throw new ApiException(SC.SC_400_BAD_REQUEST, "This handler only supports GET, PUT, POST and DELETE requests");
      }
   }

   public void setGet(DynamoDbGetAction get)
   {
      this.get = get;
   }

   public void setDelete(DynamoDbDeleteAction delete)
   {
      this.delete = delete;
   }

   public void setPost(DynamoDbPostAction post)
   {
      this.post = post;
   }

}
