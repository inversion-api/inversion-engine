package io.rocketpartners.cloud.service;

import io.rocketpartners.cloud.model.Action;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.model.ObjectNode;

public class MockActionA extends Action<Action>
{

   @Override
   public void run(Service service, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
   {
      if (req.isMethod("get"))
      {
         res.withRecord(new ObjectNode("primaryKey", 1, "firstName", "tester1", "className", getClass().getSimpleName()));
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
