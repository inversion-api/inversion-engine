package io.inversion.cloud.service;

import io.inversion.cloud.model.Action;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.Endpoint;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;

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
