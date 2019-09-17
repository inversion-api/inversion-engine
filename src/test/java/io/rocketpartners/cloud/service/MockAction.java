package io.rocketpartners.cloud.service;

import io.rocketpartners.cloud.model.Action;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.ArrayNode;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.model.ObjectNode;
import io.rocketpartners.cloud.model.Request;
import io.rocketpartners.cloud.model.Response;

public class MockAction extends Action<MockAction>
{
   ObjectNode json = null;

   public MockAction(String name)
   {
      this(null, null, name, new ObjectNode("name", name));
   }

   public MockAction(String methods, String includePaths, String name)
   {
      this(methods, includePaths, name, null);
   }

   public MockAction(String methods, String includePaths, String name, ObjectNode json)
   {
      withMethods(methods);
      withIncludePaths(includePaths);
      withName(name);
      withJson(json);
   }

   @Override
   public void run(Service service, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
   {
      if (json != null)
      {
         if (json instanceof ArrayNode)
            res.withData((ArrayNode) json);
         else
            res.withData(new ArrayNode(json));
      }
   }

   public MockAction withJson(ObjectNode json)
   {
      this.json = json;
      return null;
   }

}
