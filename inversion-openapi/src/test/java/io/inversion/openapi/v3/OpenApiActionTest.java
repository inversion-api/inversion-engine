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
package io.inversion.openapi.v3;

import io.inversion.Api;
import io.inversion.Endpoint;
import io.inversion.action.db.DbAction;
import io.inversion.jdbc.JdbcDb;
import io.inversion.spring.InversionMain;

public class OpenApiActionTest {

    static Api buildApi() {
        Api api = new Api().withName("northwind")

                .withDb(new JdbcDb("h2", //
                        "org.h2.Driver", //
                        "jdbc:h2:mem:swaggertest;IGNORECASE=TRUE;DB_CLOSE_DELAY=-1", //
                        "sa", //
                        "", //
                        JdbcDb.class.getResource("northwind-h2.ddl").toString()))
                .withEndpoint(new Endpoint("*", "openapi.json", new OpenApiAction()))
                //.withEndpoint(new Endpoint("*", "v1/*", dbAction))
                .withEndpoint(new Endpoint("*", "v1/:tenant/*", new DbAction()))
        ;
        return api;
    }

    public static void main(String[] args) {
        InversionMain.run(buildApi());
    }


//    @Test
//    public void generateNorthwindSwagger1() throws Exception {
//        DbAction dbAction = new DbAction();
//
//
//        Engine   e = new Engine(api);
//        Response r = e.get("/northwind/openapi.json");
//        r.dump();
//    }

}
