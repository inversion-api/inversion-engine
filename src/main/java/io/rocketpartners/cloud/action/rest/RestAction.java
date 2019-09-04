package io.rocketpartners.cloud.action.rest;

import io.rocketpartners.cloud.model.Action;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.model.Request;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.service.Service;

public class RestAction<T extends RestAction> extends Action<T>
{
   protected RestGetAction    get    = new RestGetAction();
   protected RestDeleteAction delete = new RestDeleteAction();
   protected RestPostAction   post   = new RestPostAction();

   @Override
   public void run(Service service, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
   {
      if (req.isMethod("GET"))
      {
         get.run(service, api, endpoint, chain, req, res);
      }
      else if (req.isMethod("POST", "PUT"))
      {
         post.run(service, api, endpoint, chain, req, res);
      }
      else if (req.isMethod("DELETE"))
      {
         delete.run(service, api, endpoint, chain, req, res);
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
