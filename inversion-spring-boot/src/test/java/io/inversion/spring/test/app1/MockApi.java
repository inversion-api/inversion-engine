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

import io.inversion.Api;
import io.inversion.action.misc.MockAction;
import io.inversion.json.JSMap;
import io.inversion.json.JSNode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MockApi {
    @Bean
    public static Api buildMockApi1() {
        return new Api()//
                .withName("secondApi")//
                .withEndpoint("GET", new MockAction().withJson(new JSMap("hello_world", "from mock api one")));
    }

    @Bean
    public static Api buildMockApi2() {
        return new Api()//
                .withName("thirdApi")//
                .withEndpoint("GET", new MockAction().withJson(new JSMap("hello_world", "from mock api two")));
    }

}
