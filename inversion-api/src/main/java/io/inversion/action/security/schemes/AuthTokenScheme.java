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

import io.inversion.ApiException;
import io.inversion.Request;

import io.inversion.Response;
import io.inversion.User;
import io.inversion.action.security.AuthScheme;

public class AuthTokenScheme extends AuthScheme {

    protected TokenDao tokenDao = null;

    public AuthTokenScheme(){
        withName("tokenAuth");
    }

    @Override
    public User getUser(Request req, Response res) throws ApiException {
        return null;
    }

    public TokenDao getTokenDao() {
        return tokenDao;
    }

    public AuthTokenScheme withTokenDao(TokenDao tokenDao) {
        this.tokenDao = tokenDao;
        return this;
    }

    public interface TokenDao {
        User get(String sessionKey);

        String post(User user);

        void put(String sessionKey, User user);

        void delete(String sessionKey);
    }

//    public static class InMemoryTokenDao implements TokenDao {
//        protected long sessionExp    = 1000 * 60 * 30; //30 minute default timeput
//        protected long sessionUpdate = 1000 * 10;      //update a session every 10s to prevent spamming the cache with every request
//        protected int  sessionMax    = 10000;
//
//        protected Map<String, User> cache;
//
//        protected InMemoryTokenDao() {
//
//        }
//
//        public InMemoryTokenDao(int sessionMax) {
//            this.cache = new LRUMap<>(sessionMax);
//        }
//
//        @Override
//        public User get(String sessionKey) {
//            long now = System.currentTimeMillis();
//
//            User user = doGet(sessionKey);
//
//            if (sessionExp > 0) {
//                long lastRequest = user.getRequestAt();
//                if (now - lastRequest > sessionExp) {
//                    delete(sessionKey);
//                    throw ApiException.new401Unauthroized("Your API Token has expired.");
//                } else if (now - lastRequest > sessionUpdate) {
//                    put(sessionKey, user);
//                }
//            }
//
//            return cache.get(sessionKey);
//        }
//    }
}
