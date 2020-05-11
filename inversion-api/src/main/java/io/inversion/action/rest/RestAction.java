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
package io.inversion.action.rest;

import io.inversion.Action;
import io.inversion.Request;
import io.inversion.Response;

public class RestAction extends Action<RestAction>
{
   protected RestGetAction    getAction    = new RestGetAction();
   protected RestPostAction   postAction   = new RestPostAction();
   protected RestPutAction    putAction    = new RestPutAction();
   protected RestPatchAction  patchAction  = new RestPatchAction();
   protected RestDeleteAction deleteAction = new RestDeleteAction();

   @Override
   public void run(Request req, Response res) throws Exception
   {
      if (req.isMethod("GET"))
      {
         getAction.run(req, res);
      }
      else if (req.isMethod("POST"))
      {
         postAction.run(req, res);
      }
      else if (req.isMethod("PUT"))
      {
         putAction.run(req, res);
      }
      else if (req.isMethod("PATCH"))
      {
         patchAction.run(req, res);
      }

      else if (req.isMethod("DELETE"))
      {
         deleteAction.run(req, res);
      }
   }

   public RestGetAction getGetAction()
   {
      return getAction;
   }

   public RestAction withGetAction(RestGetAction getAction)
   {
      this.getAction = getAction;
      return this;
   }

   public RestPostAction getPostAction()
   {
      return postAction;
   }

   public RestAction withPostAction(RestPostAction postAction)
   {
      this.postAction = postAction;
      return this;
   }

   public RestPutAction getPutAction()
   {
      return putAction;
   }

   public RestAction withPutAction(RestPutAction putAction)
   {
      this.putAction = putAction;
      return this;
   }

   public RestPatchAction getPatchAction()
   {
      return patchAction;
   }

   public RestAction withPatchAction(RestPatchAction patchAction)
   {
      this.patchAction = patchAction;
      return this;
   }

   public RestDeleteAction getDeleteAction()
   {
      return deleteAction;
   }

   public RestAction withDeleteAction(RestDeleteAction deleteAction)
   {
      this.deleteAction = deleteAction;
      return this;
   }

}
