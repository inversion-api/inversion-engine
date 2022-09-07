package io.inversion;/*
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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UserTest {

    ObjectMapper mapper = new ObjectMapper();

    @Test
    public void test_jackson_encoding()throws Exception{
        User user = new User();

        user.withProperty("permission", "perm1");
        user.withProperty("permission", "perm2");
        user.withProperty("group", "group1");
        user.withProperty("phone_number", "1111111111");

        assertEquals("[perm1, perm2]", user.getPermissions().toString());
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(user);

        System.out.println(json);

        User parsed = mapper.readValue(json, User.class);

        assertEquals("1111111111", parsed.getProperty("phone_number"));
        assertEquals("[perm1, perm2]", parsed.getPermissions().toString());

    }
}
