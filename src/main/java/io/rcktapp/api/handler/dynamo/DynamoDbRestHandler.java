package io.rcktapp.api.handler.dynamo;

import io.rcktapp.api.Action;
import io.rcktapp.api.Api;
import io.rcktapp.api.ApiException;
import io.rcktapp.api.Chain;
import io.rcktapp.api.Endpoint;
import io.rcktapp.api.Handler;
import io.rcktapp.api.Request;
import io.rcktapp.api.Response;
import io.rcktapp.api.SC;
import io.rcktapp.api.service.Service;

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