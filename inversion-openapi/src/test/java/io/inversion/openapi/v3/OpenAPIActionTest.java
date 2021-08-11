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
import io.inversion.Collection;
import io.inversion.Endpoint;
import io.inversion.Parameter;
import io.inversion.action.db.DbAction;
import io.inversion.action.misc.FileAction;
import io.inversion.action.hateoas.HALAction;
import io.inversion.action.misc.MockAction;
import io.inversion.action.security.AuthAction;
import io.inversion.action.security.schemes.ApiKeyScheme;
import io.inversion.action.security.schemes.BearerScheme;
import io.inversion.jdbc.JdbcDb;
import io.inversion.spring.main.InversionMain;

public class OpenAPIActionTest {

    static Api buildApi() {
        Api api = new Api().withName("northwind")
                .withIncludeOn("northwind/v1/:tenant/*")
                .withDb(new JdbcDb("h2", //
                        "org.h2.Driver", //
                        //"jdbc:h2:mem:swaggertest;IGNORECASE=TRUE;DB_CLOSE_DELAY=-1", //
                        "jdbc:h2:mem:swaggertest;DB_CLOSE_DELAY=-1", //
                        "sa", //
                        "", //
                        JdbcDb.class.getResource("northwind-h2.ddl").toString()))
                .withEndpoint(new Endpoint("*", "openapi.json,openapi.yaml", new OpenAPIAction()))
                .withEndpoint(new Endpoint("*", "rapidoc.html", new FileAction()))

//                .withEndpoint(new Endpoint("*", "auth.json", new FileAction()))
//                .withEndpoint(new Endpoint("*", "test/*", new HALAction(), new MockAction()))
//                .withCollection(new Collection().withName("auths").withSchemaRef("http://localhost:8080/northwind/v1/us/auth.json").withProperty("id", "number").withIndex("pk", "primary", true, "id"));

                .withEndpoint(new Endpoint("*", "*", new HALAction() //
                        //,new AuthAction().withAuthScheme(new BearerScheme().withDescription("this is a JWT."))
                        //                .withAuthScheme(new ApiKeyScheme().withParameter(new Parameter("username", "username", "query", false))
                        //                        .withParameter(new Parameter("password", "password", "query", false)))//
                        , new DbAction()))

        ;
        return api;
    }
//
//    static Api buildApi() {
//        Api api = new Api().withName("ledgerApi")
//                .withIncludeOn("{programCode}/v1/:tenantCode/ledger/*")//
//                .withDb(new JdbcDb("ledger"))
//                .withEndpoint(new Endpoint("*", "openapi.json,openapi.yaml", new OpenAPIAction()))
//                .withEndpoint(new Endpoint("*", "rapidoc.html", new FileAction()))
//                .withEndpoint(new Endpoint("*", "*", new HALAction(), new DbAction()))
//                ;
//        return api;
//    }


    public static void main(String[] args) {
        InversionMain.run(buildApi());
    }
}