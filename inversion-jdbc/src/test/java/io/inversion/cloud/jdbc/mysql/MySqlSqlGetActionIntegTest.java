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
package io.inversion.cloud.jdbc.mysql;

import org.junit.jupiter.api.BeforeAll;

import io.inversion.cloud.action.rest.RestAction;
import io.inversion.cloud.jdbc.AbstractSqlGetActionIntegTest;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.service.Engine;

public class MySqlSqlGetActionIntegTest extends AbstractSqlGetActionIntegTest
{
   @BeforeAll
   public void beforeAll_initializeDb() throws Exception
   {
      db = "mysql";
      engine = new Engine().withApi(new Api("northwind") //
                                                        .withEndpoint("GET", "mysql/*", new RestAction())//
                                                        .withDb(MySqlUtils.bootstrapMySql(MySqlSqlGetActionIntegTest.class.getSimpleName())));

      engine.startup();
   }

}
