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
package io.inversion.cloud.jdbc;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;

import io.inversion.cloud.action.rest.AbstractRestDeleteActionIntegTest;
import io.inversion.cloud.action.rest.RestAction;
import io.inversion.cloud.jdbc.db.JdbcDb.ConnectionLocal;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.service.Engine;

public abstract class AbstractSqlDeleteActionIntegTest extends AbstractRestDeleteActionIntegTest
{
   public AbstractSqlDeleteActionIntegTest(String dbType)
   {
      super(dbType);
   }

   @BeforeEach
   public void initializeDb() throws Exception
   {
      if (engine != null)
      {
         engine.shutdown();
         engine = null;
         Chain.resetAll();
         ConnectionLocal.resetAll();
      }

      engine = new Engine().withApi(new Api("northwind") //
                                                        .withEndpoint("*", dbType + "/*", new RestAction())//
                                                        .withDb(JdbcDbFactory.buildDb(dbType, getClass().getSimpleName())));
      engine.startup();
   }

   @AfterAll
   public void finalizeDb() throws Exception
   {
      if (engine != null)
      {
         engine.shutdown();
         engine = null;
         Chain.resetAll();
         ConnectionLocal.resetAll();
      }
   }
}
