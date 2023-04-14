/*
 * Copyright (c) 2015-2021 Rocket Partners, LLC
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

import io.inversion.*;
import io.inversion.action.security.AuthScheme;
import io.inversion.utils.Utils;

import java.util.List;

public class ApiKeyScheme extends AuthScheme {

    public ApiKeyScheme() {
        withType(AuthSchemeType.apiKey);
    }

    protected UserDao userDao = null;

    @Override
    public User getUser(Request req, Response res) throws ApiException {

        List<Param> params = getParams();
        if (params.size() == 1) {
            Param  apiKeyParam = params.get(0);
            String apiKey      = req.findParam(apiKeyParam.getKey(), apiKeyParam.getIn());
            if (apiKey != null) {
                return userDao.getUserByApiKey(req, apiKey);
            }
        } else {
            Param usernameParam = params.get(0).getKey().toLowerCase().indexOf("pass") < 0 ? params.get(0) : params.get(1);
            Param passwordParam = params.get(0).getKey().toLowerCase().indexOf("pass") >= 0 ? params.get(0) : params.get(1);

            String username = req.findParam(usernameParam.getKey(), usernameParam.getIn());
            String password = req.findParam(passwordParam.getKey(), passwordParam.getIn());

            if (username != null && password != null) {
                return userDao.getUserByUsernameAndPassword(req, username, password);
            }
        }
        return null;
    }

    public UserDao getUserDao() {
        return userDao;
    }

    public ApiKeyScheme withUserDao(UserDao userDao) {
        this.userDao = userDao;
        return this;
    }

}
