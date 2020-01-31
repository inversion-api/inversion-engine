/*
 * Copyright (c) 2015-2020 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
