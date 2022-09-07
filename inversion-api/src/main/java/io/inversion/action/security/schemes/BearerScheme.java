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
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.inversion.*;
import io.inversion.action.security.AuthScheme;
import io.inversion.config.Config;
import io.inversion.json.JSList;
import io.inversion.json.JSMap;
import io.inversion.json.JSNode;
import io.inversion.json.JSParser;
import io.inversion.utils.Utils;
import org.apache.commons.collections.map.LRUMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

public class BearerScheme extends HttpAuthScheme {

    public static final String CONTEXT_KEY_API_NAME = "__API_NAME";

    protected final transient Logger log = LoggerFactory.getLogger(getClass().getName());

    protected transient LRUMap secretCache = null;

    public enum In {header}

    protected Param.In     in                  = Param.In.HEADER;
    protected String       requestHeaderKey    = "Authorization";
    protected String       barerFormat         = "JWT";
    protected String       requiredTokenPrefix = "bearer ";
    protected List<String> contextKeys         = new ArrayList();
    protected String       secretKeyPrefix     = "jwt";
    protected String       secretKeyPostfix    = "secret";
    protected int          secretCacheSize     = 100;


    public BearerScheme() {
        withHttpScheme(HttpAuthScheme.HttpScheme.bearer);
    }

    public String getBarerFormat() {
        return barerFormat;
    }

    public AuthScheme withBarerFormat(String barerFormat) {
        this.barerFormat = barerFormat;
        return this;
    }

    @Override
    public User getUser(Request req, Response res) throws ApiException {
        String token = req.findParam(requestHeaderKey, in);
        if (token == null)
            return null;

        token = token.trim();

        if (!token.toLowerCase().startsWith(requiredTokenPrefix))
            return null;

        token = token.substring(requiredTokenPrefix.length()).trim();

        DecodedJWT jwt = null;
        for (String secret : findSecrets(secretKeyPrefix, secretKeyPostfix, getContextValues(req))) {
            jwt = decodeJWT(token, secret);
            if (jwt != null)
                break;
        }

        User user = null;
        if (jwt != null)
            user = buildUser(jwt);

        return user;
    }


    public List<String> getContextValues(Request req) {
        List<String> values = new ArrayList<>();
        for (String contextKey : contextKeys)
            values.add(getContextValue(req, contextKey));
        return values;
    }

    public String getContextValue(Request req, String key) {
        if (CONTEXT_KEY_API_NAME.equalsIgnoreCase(key))
            return req.getApi().getName();

        String value = req.getUrl().getParam(key);
        return value;
    }


    public User buildUser(DecodedJWT jwt) {
        User   user    = new User();
        String payload = jwt.getPayload();
        payload = new String(Base64.getDecoder().decode(payload.getBytes()));
        JSNode node = JSParser.asJSNode(payload);
        for (String key : node.keySet()) {
            Object value = node.getValue(key);
            user.withProperty(key, value);
        }

        return user;
    }


    /**
     * A list of scoping strings from most specific to the request to least specific to the request.
     * <p>
     * For example: ["{apiName}, "{tenant}"]
     * <p>
     * [{keyPrefix}.]apiName.tenant[.10-1][.{keyPostfix}]
     * {keyPrefix}.apiName[.10-1].{keyPostfix}
     *
     * @param contextValues
     * @return
     */
    List<String> findSecrets(String secretKeyPrefix, String secretKeyPostfix, List<String> contextValues) {

        if (secretCache == null) {
            synchronized (this) {
                if (secretCache == null) {
                    secretCache = new LRUMap(secretCacheSize);
                }
            }
        }

        String       cacheKey = secretKeyPrefix + secretKeyPostfix + contextValues.toString();
        List<String> secrets  = (List<String>) secretCache.get(cacheKey);

        if (secrets != null)
            return secrets;

        secrets = new ArrayList<>();
        for (int i = contextValues.size() - 1; i >= 0; i--) {
            for (int j = 10; j >= -1; j--) {
                List parts = new ArrayList();
                parts.add(secretKeyPrefix);
                for (int k = 0; k <= i; k++)
                    parts.add(contextValues.get(k));
                if (j > -1)
                    parts.add(j);
                parts.add(secretKeyPostfix);

                String key    = Utils.implode(".", parts);
                String secret = getSecret(key);

                if (!Utils.empty(secret))
                    secrets.add(secret);
            }
        }

        if (secrets.size() > 0)
            secretCache.put(cacheKey, secrets);

        return secrets;
    }

    protected String getSecret(String key) {
        return Config.getString(key);
    }


    public String buildToken(Request req, User user) {
        List<String> secrets = findSecrets(secretKeyPrefix, secretKeyPostfix, getContextValues(req));
        if (secrets.size() > 0)
            return buildToken(user, secrets.get(0));
        return null;
    }

    public String buildToken(User user, String secret) {
        try {
            JWTCreator.Builder builder = JWT.create();
            String             jwt     = null;

            long expires = System.currentTimeMillis() + 1000 * 60 * 15;
            builder.withExpiresAt(new Date(expires));
            builder.withSubject(user.getUsername());

            ObjectMapper mapper = new ObjectMapper();
            String       json   = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(user);
            JSMap        claims = JSParser.asJSMap(json);

            for (String name : claims.keySet()) {
                Object value = claims.get(name);
                if (value == null) {
                    continue;
                }
                if (value instanceof JSList) {
                    builder.withArrayClaim(name, (String[]) ((JSList) value).asList().toArray(new String[((JSList) value).size()]));
                } else {
                    builder.withClaim(name, String.valueOf(value));
                }
            }
            jwt = signJWT(builder, secret);
            return jwt;
        } catch (Exception ex) {
            throw ApiException.new500InternalServerError("Error creating JWT", ex.getMessage());
        }

    }


    public String signJWT(JWTCreator.Builder builder, String secret) throws IllegalArgumentException, JWTCreationException, UnsupportedEncodingException {
        return builder.sign(Algorithm.HMAC256(secret));
    }

    public DecodedJWT decodeJWT(String token, String secret) {
        try {
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret)).acceptLeeway(1).build();
            return verifier.verify(token);
        } catch (Exception ex) {
            //-- this is not an error yet because there can be multiple signing keys in the list
            //-- and this jwt may be using an older/different but still supported one in the list
            log.info("Error decoding jwt:", ex.getMessage());
        }
        return null;
    }

    public String getRequestHeaderKey() {
        return requestHeaderKey;
    }

    public BearerScheme withRequestHeaderKey(String requestHeaderKey) {
        this.requestHeaderKey = requestHeaderKey;
        return this;
    }

    public String getRequiredTokenPrefix() {
        return requiredTokenPrefix;
    }

    public BearerScheme withRequiredTokenPrefix(String requiredTokenPrefix) {
        this.requiredTokenPrefix = requiredTokenPrefix;
        return this;
    }

    public List<String> getContextKeys() {
        return contextKeys;
    }

    public BearerScheme withContextKeys(String... contextKeys) {
        for (String contextKey : contextKeys) {
            this.contextKeys.add(contextKey);
        }
        return this;
    }

    public BearerScheme withContextKeys(List<String> contextKeys) {
        this.contextKeys = contextKeys;
        return this;
    }

    public String getSecretKeyPrefix() {
        return secretKeyPrefix;
    }

    public BearerScheme withSecretKeyPrefix(String secretKeyPrefix) {
        this.secretKeyPrefix = secretKeyPrefix;
        return this;
    }

    public String getSecretKeyPostfix() {
        return secretKeyPostfix;
    }

    public BearerScheme withSecretKeyPostfix(String secretKeyPostfix) {
        this.secretKeyPostfix = secretKeyPostfix;
        return this;
    }

}
