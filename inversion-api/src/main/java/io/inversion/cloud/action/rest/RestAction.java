/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
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
package io.inversion.cloud.action.rest;

import io.inversion.cloud.model.Action;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;

public class RestAction extends Action<RestAction>
{
   protected RestGetAction    get    = new RestGetAction();
   protected RestDeleteAction delete = new RestDeleteAction();
   protected RestPostAction   post   = new RestPostAction();

   @Override
   public void run(Request req, Response res) throws Exception
   {
      if (req.isMethod("GET"))
      {
         get.run(req, res);
      }
      else if (req.isMethod("POST", "PUT", "PATCH"))
      {
         post.run(req, res);
      }
      else if (req.isMethod("DELETE"))
      {
         delete.run(req, res);
      }
   }

   public RestGetAction getGet()
   {
      return get;
   }

   public RestAction withGet(RestGetAction get)
   {
      this.get = get;
      return this;
   }

   public RestDeleteAction getDelete()
   {
      return delete;
   }

   public RestAction withDelete(RestDeleteAction delete)
   {
      this.delete = delete;
      return this;
   }

   public RestPostAction getPost()
   {
      return post;
   }

   public RestAction withPost(RestPostAction post)
   {
      this.post = post;
      return this;
   }

}
