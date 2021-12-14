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

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.inversion.ApiException;
import io.inversion.Request;
import io.inversion.Response;
import io.inversion.User;
import io.inversion.action.security.AuthAction;
import io.inversion.action.security.AuthScheme;
import io.inversion.config.Config;

import java.io.UnsupportedEncodingException;
import java.util.*;

public class BearerScheme extends AuthScheme {

    RevokedTokenCache revokedTokenCache = null;

    public BearerScheme(){
        withName("bearerAuth");
        withType("http");
        withScheme("bearer");
        withBarerFormat("JWT");
    }

    @Override
    public User getUser(Request req, Response res) throws ApiException {

        String token = req.getHeader("Authorization");
        if(token == null)
            return null;

        token = token.trim();

        if(!token.toLowerCase().startsWith("bearer "))
            return null;

        token = token.substring(token.indexOf(" ") + 1);


        if (revokedTokenCache != null && revokedTokenCache.isRevoked(token))
            throw ApiException.new401Unauthroized();

        DecodedJWT jwt = null;
        for (String secret : getJwtSecrets(req)) {
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
                ex.printStackTrace();
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
     * Finds the most specific keys first.
     */
    protected List<String> getJwtSecrets(Request req) {

        //todo permute with path mapped vars not just api name and tenant

        String name = getName();
        String apiName = req.getApi().getName();
        String tenant = req.getUrl().getParam("tenant");


        LinkedHashSet<String> secrets = new LinkedHashSet<>();

        for (int i = 10; i >= 0; i--) {

            for (int j = 2; j >= 0; j--) {
                String key = (name != null ? name : "") + ".jwt" + (i == 0 ? "" : ("." + i));

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

        return new ArrayList(secrets);
    }

    public String signJwt(JWTCreator.Builder jwtBuilder, AuthAction action, String apiName, String tenant) throws IllegalArgumentException, JWTCreationException, UnsupportedEncodingException {
        //String secret = getJwtSecrets(action, apiName, tenant).get(0);
        //return jwtBuilder.sign(Algorithm.HMAC256(secret));
        return null;
    }

    public RevokedTokenCache getRevokedTokenCache() {
        return revokedTokenCache;
    }

    public BearerScheme withRevokedTokenCache(RevokedTokenCache revokedTokenCache) {
        this.revokedTokenCache = revokedTokenCache;
        return this;
    }

    public interface RevokedTokenCache {
        boolean isRevoked(String token);
    }

    public static class InMemoryRevokedTokenCache implements RevokedTokenCache {
        final Set<String> revoked = new HashSet<>();

        public void addRevokedToken(String token) {
            revoked.add(token.toLowerCase());
        }

        public boolean isRevoked(String token) {
            return revoked.contains(token);
        }
    }

}
