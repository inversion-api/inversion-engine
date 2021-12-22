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

import io.inversion.*;

import java.util.*;

public class AuthAction extends Action<AuthAction> {
    public static final int AUTH_ACTION_DEFAULT_ORDER_IS_100 = 100;

    protected String collection = null;

    protected String authenticatedPerm = null; // apply this perm to all authenticated users, allows ACL to target all authenticated users

    //

    //protected UserDao userDao = null;


    List<AuthScheme> schemes = new ArrayList();




    public AuthAction() {
        withOrder(AUTH_ACTION_DEFAULT_ORDER_IS_100);
    }

    @Override
    public void run(Request req, Response resp) throws ApiException {
        User user = Chain.getUser();

        if(user != null)
            return;

        for(AuthScheme scheme : schemes){
            user = scheme.getUser(req, resp);
            if(user != null)
                break;
        }

        if(user == null)
            throw ApiException.new401Unauthroized();
//
//        String apiName = req.getApi().getName();
//        String tenant  = req.getUrl().getParam("tenant");
//
//        //-- END CONFIG
//
//        long now = System.currentTimeMillis();
//
//        String username = null;
//        String password = null;
//        //String sessionKey = null;
//        boolean sessionReq = collection != null && collection.equalsIgnoreCase(req.getCollectionKey());
//
//        String url = req.getUrl().toString().toLowerCase();
//        while (url.endsWith("/"))
//            url = url.substring(0, url.length() - 1);
//
//        String token = req.getHeader("authorization");
//        if (token == null)
//            token = req.getHeader("x-auth-token");
//
//        if (token != null) {
//            token = token.trim();
//
//            if (token.toLowerCase().startsWith("bearer ")) {
//                token = token.substring(token.indexOf(" ") + 1).trim();
//                user = userDao.getUser(this, token, apiName, tenant);
//            } else if (token.toLowerCase().startsWith("basic ")) {
//                token = token.substring(token.indexOf(" ") + 1);
//                token = new String(Base64.decodeBase64(token));
//                username = token.substring(0, token.indexOf(":"));
//                password = token.substring(token.indexOf(":") + 1);
//
//                user = userDao.getUser(this, username, password, apiName, tenant);
//            } else if (token.toLowerCase().startsWith("session ")) {
//                if (sessionDao == null)
//                    throw ApiException.new400BadRequest("AuthAction has not been configured to support session authorization");
//
//                token = token.substring(8).trim();
//
//                if (sessionReq && req.isDelete()) {
//                    //the supplied authorization and the resourceKey in the url
//                    //must match on a delete.
//                    String resourceKey = req.getResourceKey();
//                    if (!Utils.equal(token, resourceKey))
//                        throw ApiException.new401Unauthroized("Logout requires a session authroization or x-auth-token header that matches the url resourceKey");
//
//                    sessionDao.delete(token);
//                    return;
//                }
//
//                user = sessionDao.get(token);
//            } else {
//                throw ApiException.new400BadRequest("Authorization token format must be bearer,basic or session. {} ", token);
//            }
//
//            if (user == null)
//                throw ApiException.new401Unauthroized();
//
//        } else {
//            if (req.isPost() && sessionReq) {
//                username = req.getJson().getString("username");
//                password = req.getJson().getString("password");
//            }
//
//            if (Utils.empty(username, password)) {
//                username = req.getHeader("x-auth-username");
//                password = req.getHeader("x-auth-password");
//            }
//
//            if (Utils.empty(username, password)) {
//                username = req.getHeader("username");
//                password = req.getHeader("password");
//            }
//
//            if (Utils.empty(username, password)) {
//                username = req.getUrl().clearParams("username");
//                password = req.getUrl().clearParams("password");
//            }
//
//            if (!Utils.empty(username, password)) {
//                user = userDao.getUser(this, username, password, apiName, tenant);
//
//                if (user == null)
//                    throw ApiException.new401Unauthroized();
//            }
//        }
//
//        if (user == null)//by here, we know that no credentials were provided
//        {
//            if (sessionReq)
//                throw ApiException.new401Unauthroized();
//
//            user = userDao.getGuest(apiName, tenant);
//        }
//
//        if (user == null //
//                || (tenant != null && !tenant.equalsIgnoreCase(user.getTenant()))) {
//            throw ApiException.new401Unauthroized();
//        }
//
//        user.withRequestAt(now);
//        Chain.peek().withUser(user);
//
//        if (sessionDao != null && sessionReq && req.isPost()) {
//            String sessionKey = sessionDao.post(user);
//
//            resp.withHeader("x-auth-token", "Session " + sessionKey);
//            JSNode obj = new JSMap();
//            obj.put("id", user.getId());
//            obj.put("username", username);
//            obj.put("displayname", user.getDisplayName());
//
//            JSList perms = new JSList();
//            for (String perm : user.getPermissions()) {
//                perms.add(perm);
//            }
//            obj.put("perms", perms);
//
//            JSList roles = new JSList();
//            for (String role : user.getRoles()) {
//                roles.add(role);
//            }
//            obj.put("roles", roles);
//
//            resp.withJson(new JSMap("data", obj));
//        }
    }

//    public AuthAction withCollection(String collection) {
//        this.collection = collection;
//        return this;
//    }
//
//    public AuthAction withAuthenticatedPerm(String authenticatedPerm) {
//        this.authenticatedPerm = authenticatedPerm;
//        return this;
//    }
//
//    public AuthAction withSessionDao(SessionDao sessionDao) {
//        this.sessionDao = sessionDao;
//        return this;
//    }
//
//    public AuthAction withUserDao(UserDao dao) {
//        this.userDao = dao;
//        return this;
//    }
//
//    public UserDao getUserDao() {
//        return userDao;
//    }



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



//    /**
//     * Override me to change out map/cache implementation
//     *
//     * @param sessionKey the session id of the user
//     * @return the users if found
//     */
//    protected User doGet(String sessionKey) {
//        return cache.get(sessionKey);
//    }
//
//    @Override
//    public String post(User user) {
//        String sessionKey = newSessionId();
//        put(sessionKey, user);
//        return sessionKey;
//    }
//
//    @Override
//    public void put(String sessionKey, User user) {
//        doPut(sessionKey, user);
//    }
//
//    /**
//     * Override me to change out map/cache implementation
//     *
//     * @param sessionKey the users session id
//     * @param user       the user to store against the sessionKey
//     */
//    protected void doPut(String sessionKey, User user) {
//        cache.put(sessionKey, user);
//    }
//
//    @Override
//    public void delete(String sessionKey) {
//        doDelete(sessionKey);
//    }
//
//    /**
//     * Override me to change out map/cache implementation
//     *
//     * @param sessionKey the session to end
//     */
//    protected void doDelete(String sessionKey) {
//        cache.remove(sessionKey);
//    }
//
//    protected String newSessionId() {
//        String id = UUID.randomUUID().toString();
//        id = id.replace("-", "");
//        return id;
//    }
//
//    public SessionDao withSessionUpdate(long sessionUpdate) {
//        this.sessionUpdate = sessionUpdate;
//        return this;
//    }
//
//    public SessionDao withSessionMax(int sessionMax) {
//        this.sessionMax = sessionMax;
//        return this;
//    }
//
//    public SessionDao withSessionExp(long sessionExp) {
//        this.sessionExp = sessionExp;
//        return this;
//    }

    public List<AuthScheme> getAuthSchemes() {
        return schemes;
    }

    public AuthAction withAuthSchemes(List<AuthScheme> schemes) {
        schemes.forEach(this::withAuthScheme);
        return this;
    }

    public AuthAction withAuthScheme(AuthScheme scheme) {
        if(scheme != null && !schemes.contains(scheme))
            schemes.add(scheme);
        return this;
    }
}
