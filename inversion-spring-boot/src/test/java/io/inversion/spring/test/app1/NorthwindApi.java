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
package io.inversion.spring.test.app1;

import io.inversion.Action;
import io.inversion.Api;
import io.inversion.Request;
import io.inversion.Response;
import io.inversion.action.misc.MockAction;
import io.inversion.utils.JSNode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NorthwindApi {

    /**
     * Constructs a REST API that exposes database tables and REST collections.
     */
    @Bean
    public static Api buildApi() {
        return new Api().withName("test")

                .withEndpoint("GET", "mock1/*", new MockAction().withJson(new JSNode("hello_world", "from northwind api")))//
                .withEndpoint("GET", "custom1/*", new Action() {
                    @Override
                    public void doGet(Request req, Response res) {
                        res.data().add("action 1 value");
                    }
                }, new Action() {
                    @Override
                    public void doGet(Request req, Response res) {
                        res.data().add("action 2 value");
                    }
                });

    }

}
