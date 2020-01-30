package io.inversion.cloud.action.cosmosdb;

import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.Endpoint;
import io.inversion.cloud.model.EngineListener;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.service.Engine;
import io.inversion.cloud.service.spring.InversionApp;

public class MainCosmosApi
{
   public static void main(String[] args) throws Exception
   {
      Engine e = CosmosEngineFactory.engine();
      e.withEngineListener(new EngineListener()
         {

            @Override
            public void onFinally(Engine engine, Api api, Endpoint endpoint, Chain chain, Request req, Response res)
            {
               if (Chain.getDepth() <= 1)
               {
                  res.dump();
               }
            }

         });

      InversionApp.run(e);
   }
}
