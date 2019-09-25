package io.rocketpartners.cloud.service;

import io.rocketpartners.cloud.model.Action;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.model.JSNode;
import io.rocketpartners.cloud.model.Request;
import io.rocketpartners.cloud.model.Response;

public class MockActionA extends Action<MockActionA>
{
   
   public MockActionA()
   {
      
   }
   
   
   public MockActionA(String methods, String includePaths)
   {
      withMethods(methods);
      withIncludePaths(includePaths);
   }

   @Override
   public void run(Engine engine, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
   {
      if (req.isMethod("get"))
      {
         res.withRecord(new JSNode("primaryKey", 1, "firstName", "tester1", "className", getClass().getSimpleName()));
      }
      else if (req.isMethod("put", "post"))
      {

      }
      else if (req.isMethod("delete"))
      {

      }
   }

   public String toString()
   {
      return getClass().getSimpleName();
   }

}
