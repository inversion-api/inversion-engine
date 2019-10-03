package io.inversion.cloud.action.rest;

import io.inversion.cloud.model.Action;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.Endpoint;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.service.Engine;

public class RestAction extends Action<RestAction>
{
   protected RestGetAction    get    = new RestGetAction();
   protected RestDeleteAction delete = new RestDeleteAction();
   protected RestPostAction   post   = new RestPostAction();

   public RestAction()
   {
      this(null);
   }

   public RestAction(String inludePaths)
   {
      this(inludePaths, null, null);
   }

   public RestAction(String inludePaths, String excludePaths, String config)
   {
      super(inludePaths, excludePaths, config);
   }

   @Override
   public void run(Engine engine, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
   {
      if (req.isMethod("GET"))
      {
         get.run(engine, api, endpoint, chain, req, res);
      }
      else if (req.isMethod("POST", "PUT"))
      {
         post.run(engine, api, endpoint, chain, req, res);
      }
      else if (req.isMethod("DELETE"))
      {
         delete.run(engine, api, endpoint, chain, req, res);
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
