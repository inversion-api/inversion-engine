/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
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
package io.inversion.action.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.inversion.*;
import io.inversion.action.security.AuthAction.JwtUserDao.RevokedTokenCache;
import io.inversion.utils.Config;
import io.inversion.utils.JSArray;
import io.inversion.utils.JSNode;
import io.inversion.utils.Utils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.map.LRUMap;

import java.io.UnsupportedEncodingException;
import java.util.*;

public class AuthAction extends Action<AuthAction> {
    public static final int AUTH_ACTION_DEFAULT_ORDER_IS_100 = 100;

    protected String collection = null;

    protected String authenticatedPerm = null; // apply this perm to all authenticated users, allows ACL to target all authenticated users

    protected SessionDao sessionDao = null;

    protected UserDao userDao = null;

    public AuthAction() {
        withOrder(AUTH_ACTION_DEFAULT_ORDER_IS_100);
    }

    @Override
    public void run(Request req, Response resp) throws ApiException {
        User user = Chain.getUser();

        if (user != null && !req.isDelete()) {
            //the users is already logged in, have to let
            //deletes through because this could be a logout
            return;
        }

        String apiName = req.getApi().getName();
        String tenant  = req.getUrl().getParam("tenant");

        //-- END CONFIG

        long now = System.currentTimeMillis();

        String username = null;
        String password = null;
        //String sessionKey = null;
        boolean sessionReq = collection != null && collection.equalsIgnoreCase(req.getCollectionKey());

        String url = req.getUrl().toString().toLowerCase();
        while (url.endsWith("/"))
            url = url.substring(0, url.length() - 1);

        String token = req.getHeader("authorization");
        if (token == null)
            token = req.getHeader("x-auth-token");

        if (token != null) {
            token = token.trim();

            if (token.toLowerCase().startsWith("bearer ")) {
                token = token.substring(token.indexOf(" ") + 1).trim();
                user = userDao.getUser(this, token, apiName, tenant);
            } else if (token.toLowerCase().startsWith("basic ")) {
                token = token.substring(token.indexOf(" ") + 1);
                token = new String(Base64.decodeBase64(token));
                username = token.substring(0, token.indexOf(":"));
                password = token.substring(token.indexOf(":") + 1);

                user = userDao.getUser(this, username, password, apiName, tenant);
            } else if (token.toLowerCase().startsWith("session ")) {
                if (sessionDao == null)
                    throw ApiException.new400BadRequest("AuthAction has not been configured to support session authorization");

                token = token.substring(8).trim();

                if (sessionReq && req.isDelete()) {
                    //the supplied authorization and the resourceKey in the url
                    //must match on a delete.
                    String resourceKey = req.getResourceKey();
                    if (!Utils.equal(token, resourceKey))
                        throw ApiException.new401Unauthroized("Logout requires a session authroization or x-auth-token header that matches the url resourceKey");

                    sessionDao.delete(token);
                    return;
                }

                user = sessionDao.get(token);
            } else {
                throw ApiException.new400BadRequest("Authorization token format must be bearer,basic or session. {} ", token);
            }

            if (user == null)
                throw ApiException.new401Unauthroized();

        } else {
            if (req.isPost() && sessionReq) {
                username = req.getJson().getString("username");
                password = req.getJson().getString("password");
            }

            if (Utils.empty(username, password)) {
                username = req.getHeader("x-auth-username");
                password = req.getHeader("x-auth-password");
            }

            if (Utils.empty(username, password)) {
                username = req.getHeader("username");
                password = req.getHeader("password");
            }

            if (Utils.empty(username, password)) {
                username = req.getUrl().clearParams("username");
                password = req.getUrl().clearParams("password");
            }

            if (!Utils.empty(username, password)) {
                user = userDao.getUser(this, username, password, apiName, tenant);

                if (user == null)
                    throw ApiException.new401Unauthroized();
            }
        }

        if (user == null)//by here, we know that no credentials were provided
        {
            if (sessionReq)
                throw ApiException.new401Unauthroized();

            user = userDao.getGuest(apiName, tenant);
        }

        if (user == null //
                || (tenant != null && !tenant.equalsIgnoreCase(user.getTenant()))) {
            throw ApiException.new401Unauthroized();
        }

        user.withRequestAt(now);
        Chain.peek().withUser(user);

        if (sessionDao != null && sessionReq && req.isPost()) {
            String sessionKey = sessionDao.post(user);

            resp.withHeader("x-auth-token", "Session " + sessionKey);
            JSNode obj = new JSNode();
            obj.put("id", user.getId());
            obj.put("username", username);
            obj.put("displayname", user.getDisplayName());

            JSArray perms = new JSArray();
            for (String perm : user.getPermissions()) {
                perms.add(perm);
            }
            obj.put("perms", perms);

            JSArray roles = new JSArray();
            for (String role : user.getRoles()) {
                roles.add(role);
            }
            obj.put("roles", roles);

            resp.withJson(new JSNode("data", obj));
        }
    }

    public AuthAction withCollection(String collection) {
        this.collection = collection;
        return this;
    }

    public AuthAction withAuthenticatedPerm(String authenticatedPerm) {
        this.authenticatedPerm = authenticatedPerm;
        return this;
    }

    public AuthAction withSessionDao(SessionDao sessionDao) {
        this.sessionDao = sessionDao;
        return this;
    }

    public AuthAction withUserDao(UserDao dao) {
        this.userDao = dao;
        return this;
    }

    public UserDao getUserDao() {
        return userDao;
    }

    public interface SessionDao {
        User get(String sessionKey);

        String post(User user);

        void put(String sessionKey, User user);

        void delete(String sessionKey);
    }

    public interface UserDao {
        User getUser(AuthAction action, String jwt, String apiName, String tenant) throws ApiException;

        User getUser(AuthAction action, String username, String password, String apiName, String tenant) throws ApiException;

        default User getGuest(String apiName, String tenant) {
            User user = new User();
            user.withUsername("Anonymous");
            user.withRoles("guest");
            user.withTenant(tenant);
            return user;
        }
    }

    public static class InMemorySessionDao implements SessionDao {
        protected long sessionExp    = 1000 * 60 * 30; //30 minute default timeput
        protected long sessionUpdate = 1000 * 10;      //update a session every 10s to prevent spamming the cache with every request
        protected int  sessionMax    = 10000;

        protected Map<String, User> cache;

        protected InMemorySessionDao() {

        }

        public InMemorySessionDao(int sessionMax) {
            this.cache = new LRUMap(sessionMax);
        }

        @Override
        public User get(String sessionKey) {
            long now = System.currentTimeMillis();

            User user = doGet(sessionKey);

            if (sessionExp > 0) {
                long lastRequest = user.getRequestAt();
                if (now - lastRequest > sessionExp) {
                    delete(sessionKey);
                    throw ApiException.new401Unauthroized("The session has expired.");
                } else if (now - lastRequest > sessionUpdate) {
                    put(sessionKey, user);
                }
            }

            return cache.get(sessionKey);
        }

        /**
         * Override me to change out map/cache implementation
         *
         * @param sessionKey the session id of the user
         * @return the users if found
         */
        protected User doGet(String sessionKey) {
            return cache.get(sessionKey);
        }

        @Override
        public String post(User user) {
            String sessionKey = newSessionId();
            put(sessionKey, user);
            return sessionKey;
        }

        @Override
        public void put(String sessionKey, User user) {
            doPut(sessionKey, user);
        }

        /**
         * Override me to change out map/cache implementation
         *
         * @param sessionKey the users session id
         * @param user       the user to store against the sessionKey
         */
        protected void doPut(String sessionKey, User user) {
            cache.put(sessionKey, user);
        }

        @Override
        public void delete(String sessionKey) {
            doDelete(sessionKey);
        }

        /**
         * Override me to change out map/cache implementation
         *
         * @param sessionKey the session to end
         */
        protected void doDelete(String sessionKey) {
            cache.remove(sessionKey);
        }

        protected String newSessionId() {
            String id = UUID.randomUUID().toString();
            id = id.replace("-", "");
            return id;
        }

        public SessionDao withSessionUpdate(long sessionUpdate) {
            this.sessionUpdate = sessionUpdate;
            return this;
        }

        public SessionDao withSessionMax(int sessionMax) {
            this.sessionMax = sessionMax;
            return this;
        }

        public SessionDao withSessionExp(long sessionExp) {
            this.sessionExp = sessionExp;
            return this;
        }

    }

    public static class InMemoryRevokedTokenCache implements RevokedTokenCache {
        final Set<String> revoked = new HashSet();

        public void addRevokedToken(String token) {
            revoked.add(token.toLowerCase());
        }

        public boolean isRevoked(String token) {
            return revoked.contains(token);
        }
    }

    public static class JwtUserDao implements UserDao {
        RevokedTokenCache revokedTokenCache = new InMemoryRevokedTokenCache();

        public User getUser(AuthAction action, String username, String password, String apiName, String tenant) throws ApiException {
            throw ApiException.new403Forbidden();
        }

        public User getUser(AuthAction action, String token, String apiName, String tenant) throws ApiException {
            if (revokedTokenCache != null && revokedTokenCache.isRevoked(token))
                throw ApiException.new401Unauthroized();

            DecodedJWT jwt = null;
            for (String secret : getJwtSecrets(action, apiName, tenant)) {
                try {
                    JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret)).acceptLeeway(1).build();
                    //this will throw an exception if the signatures don't match
                    jwt = verifier.verify(token);
                    break;
                } catch (Exception ex) {
                    //-- this is not an error yet because there can be multiple signing keys in the list
                    //-- and this jwt may be using an older/different but still supported one in the list
                    //--
                    //-- multiple keys are supported so you can release keys with new signatures
                    //-- while supporting old signatures for some transition period.
                }
            }

            if (jwt == null)
                throw ApiException.new401Unauthroized();

            return createUserFromValidJwt(jwt);
        }

        protected User createUserFromValidJwt(DecodedJWT jwt) {
            User user = new User();
            user.withUsername(jwt.getSubject());

            Claim c;

            c = jwt.getClaim("groups");
            if (c != null && !c.isNull()) {
                List<String> groups = c.asList(String.class);
                user.withRoles(groups.toArray(new String[0]));
            }

            c = jwt.getClaim("roles");
            if (c != null && !c.isNull()) {
                List<String> roles = c.asList(String.class);
                user.withRoles(roles.toArray(new String[0]));
            }

            c = jwt.getClaim("tenantId");//legacy support
            if (c != null && !c.isNull()) {
                String tenant = c.asString();
                user.withTenant(tenant);
            }

            c = jwt.getClaim("tenantCode");//legacy support
            if (c != null && !c.isNull()) {
                String tenant = c.asString();
                user.withTenant(tenant);
            }

            c = jwt.getClaim("tenant");
            if (c != null && !c.isNull()) {
                String tenant = c.asString();
                user.withTenant(tenant);
            }

            addPermsToUser(user, jwt.getClaim("perms"));
            addPermsToUser(user, jwt.getClaim("actions"));

            return user;
        }

        protected void addPermsToUser(User user, Claim c) {
            if (c != null && !c.isNull()) {
                List<String> perms = c.asList(String.class);
                user.withPermissions(perms.toArray(new String[0]));
            }
        }

        /**
         * Looks gwt signing secrets up as environment vars or sysprops.
         * <p>
         * Finds the most specific keys keys first.
         *
         * @param action  the action requesting the secrets
         * @param apiName the name of the parent api
         * @param tenant  the tenant if there is one
         * @return any jwt secrets found
         */
        protected List<String> getJwtSecrets(AuthAction action, String apiName, String tenant) {
            List secrets = new ArrayList<>();

            for (int i = 10; i >= 0; i--) {

                for (int j = 2; j >= 0; j--) {
                    String key = (action.getName() != null ? action.getName() : "") + ".jwt" + (i == 0 ? "" : ("." + i));

                    if (j > 1 && apiName != null)
                        key += "." + apiName;

                    if (j > 2 && tenant != null)
                        key += "." + tenant;

                    key += ".secret";

                    String secret = Config.getString(key);
                    if (secret != null) {
                        secrets.add(secret);
                    }
                }
            }

            return secrets;
        }

        public String signJwt(JWTCreator.Builder jwtBuilder, AuthAction action, String apiName, String tenant) throws IllegalArgumentException, JWTCreationException, UnsupportedEncodingException {
            String secret = getJwtSecrets(action, apiName, tenant).get(0);
            return jwtBuilder.sign(Algorithm.HMAC256(secret));
        }

        public RevokedTokenCache getRevokedTokenCache() {
            return revokedTokenCache;
        }

        public JwtUserDao withRevokedTokenCache(RevokedTokenCache revokedTokenCache) {
            this.revokedTokenCache = revokedTokenCache;
            return this;
        }

        public interface RevokedTokenCache {
            boolean isRevoked(String token);
        }

    }

}
