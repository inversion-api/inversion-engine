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
package io.inversion.cosmosdb;

import io.inversion.Api;
import io.inversion.Chain;
import io.inversion.Engine;
import io.inversion.Engine.EngineListener;
import io.inversion.Request;
import io.inversion.Response;
import io.inversion.spring.InversionMain;

public class MainCosmosApi
{
   public static void main(String[] args) throws Exception
   {
      Engine e = CosmosDbFactory.buildEngine();
      e.withEngineListener(new EngineListener()
         {

            @Override
            public void beforeFinally(Request req, Response res)
            {
               if (Chain.getDepth() <= 1)
               {
                  res.dump();
               }
            }

            @Override
            public void onStartup(Api api)
            {
            }

            @Override
            public void onShutdown(Api api)
            {
            }

            @Override
            public void afterRequest(Request req, Response res)
            {
            }

            @Override
            public void afterError(Request req, Response res)
            {
            }

            @Override
            public void onStartup(Engine engine)
            {
            }

            @Override
            public void onShutdown(Engine engine)
            {
            }

         });

      InversionMain.run(e);
   }
}
