package io.rocketpartners.cloud.service;

import io.rocketpartners.cloud.model.Action;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.JsonArray;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.model.JsonMap;
import io.rocketpartners.cloud.model.Request;
import io.rocketpartners.cloud.model.Response;

public class MockAction extends Action<MockAction>
{
   JsonMap json = null;

   public MockAction()
   {
      
   }
   
   public MockAction(String name)
   {
      this(null, null, name, new JsonMap("name", name));
   }

   public MockAction(String methods, String includePaths, String name)
   {
      this(methods, includePaths, name, null);
   }

   public MockAction(String methods, String includePaths, String name, JsonMap json)
   {
      withMethods(methods);
      withIncludePaths(includePaths);
      withName(name);
      withJson(json);
   }

   @Override
   public void run(Engine engine, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
   {
      if (json != null)
      {
         if (json instanceof JsonArray)
            res.withData((JsonArray) json);
         else
            res.withData(new JsonArray(json));
      }
   }

   public MockAction withJson(JsonMap json)
   {
      this.json = json;
      return null;
   }

}
