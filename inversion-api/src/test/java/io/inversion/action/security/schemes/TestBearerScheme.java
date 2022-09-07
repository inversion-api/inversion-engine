/*
 * Copyright (c) 2015-2022 Rocket Partners, LLC
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

package io.inversion.action.security.schemes;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.inversion.User;
import io.inversion.utils.Utils;
import org.junit.jupiter.api.Test;

public class TestBearerScheme {

    @Test
    public void test_findSecrets(){

        BearerScheme bs = new BearerScheme();

        bs.findSecrets("jwt", "secret", Utils.asList("api", "account", "workspace", "profile"));

    }

    @Test
    public void test_buildJWT(){

        User user = new User();

        user.withUsername("test");
        user.withRoles("role1");

        BearerScheme bs = new BearerScheme();
        String token = bs.buildToken(user, "12345");

        System.out.println(token);

        DecodedJWT jwt = bs.decodeJWT(token, "12345");
        user = bs.buildUser(jwt);

        System.out.println(user.getUsername());

    }
}
