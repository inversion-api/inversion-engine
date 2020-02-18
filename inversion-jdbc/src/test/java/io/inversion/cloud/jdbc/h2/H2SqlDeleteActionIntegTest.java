/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
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
package io.inversion.cloud.jdbc.h2;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import io.inversion.cloud.action.rest.AbstractRestDeleteActionIntegTest;
import io.inversion.cloud.action.rest.RestAction;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.service.Engine;

@TestInstance(Lifecycle.PER_CLASS)
public class H2SqlDeleteActionIntegTest extends AbstractRestDeleteActionIntegTest
{

   @BeforeAll
   public void beforeEach_reinitializeDb() throws Exception
   {
      if (engine != null)
      {
         engine.getApi("northwind").getDb("h2").shutdown();
      }

      db = "h2";
      engine = new Engine().withApi(new Api("northwind") //
                                                        .withEndpoint("*", "h2/*", new RestAction())//
                                                        .withDb(H2Utils.bootstrapH2(H2SqlDeleteActionIntegTest.class.getSimpleName())));

      engine.startup();
   }

}
