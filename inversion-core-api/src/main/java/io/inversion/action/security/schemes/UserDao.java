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

import io.inversion.ApiException;
import io.inversion.Request;
import io.inversion.User;
import java.util.List;

public interface UserDao {
    User getUserByUsernameAndPassword(Request req, String username, String password) throws ApiException;
    User getUserByApiKey(Request req, String apiKey) throws ApiException;

}
