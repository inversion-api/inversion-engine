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
package io.inversion.cloud.jdbc.rql;

import io.inversion.cloud.action.rest.RestAction;
import io.inversion.cloud.jdbc.db.JdbcDb;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.service.Engine;

public class H2RqlIntegTest extends H2RqlUnitTest
{
   public H2RqlIntegTest() throws Exception
   {
      urlPrefix = "/northwind/h2/";

      engine = new Engine();
      engine.withApi(new Api("northwind")//
                                         .withEndpoint("GET", "h2/*", new RestAction())//
                                         .withDb(new JdbcDb("db", //
                                                            "org.h2.Driver", //
                                                            "jdbc:h2:mem:northwind" + System.currentTimeMillis() + ";DB_CLOSE_DELAY=-1", //
                                                            "sa", //
                                                            "", //
                                                            JdbcDb.class.getResource("northwind-h2.ddl").toString()))//
      );
   }
}
