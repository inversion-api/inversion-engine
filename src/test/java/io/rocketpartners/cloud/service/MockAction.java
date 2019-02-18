package io.rocketpartners.cloud.service;

import io.rocketpartners.cloud.model.Action;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.utils.JSObject;

public class MockAction extends Action<Action>
{

   @Override
   public void run(Service service, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
   {
      if (req.isMethod("get"))
      {
         res.withRecord(new JSObject("primaryKey", 1, "firstName", "tester1"));
      }
      else if (req.isMethod("put", "post"))
      {

      }
      else if (req.isMethod("delete"))
      {

      }
   }

}
