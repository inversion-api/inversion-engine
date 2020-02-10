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
package io.inversion.cloud.action.security;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.map.LRUMap;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import io.inversion.cloud.model.Action;
import io.inversion.cloud.model.ApiException;
import io.inversion.cloud.model.JSArray;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.model.Status;
import io.inversion.cloud.model.User;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.utils.Utils;

public class AuthAction extends Action<AuthAction>
{
   long                       sessionExp              = 1000 * 60 * 30; //30 minute default timeput
   long                       sessionUpdate           = 1000 * 10;      //update a session every 10s to prevent spamming the cache with every request
   protected int              sessionMax              = 10000;

   protected int              failedMax               = 10;
   protected int              failedExp               = 1000 * 60 * 10; //10 minute timeout for failed password attemtps

   protected String           collection              = null;

   protected String           authenticatedPerm       = null;           // apply this perm to all authenticated users, allows ACL to target all authenticated users

   protected AuthSessionCache sessionCache            = null;

   protected UserDao          dao                     = null;

   protected boolean          shouldTrackRequestTimes = true;

   protected String           salt                    = "CHANGE_ME";

   public AuthAction()
   {
      withOrder(100);
   }

   @Override
   public void run(Request req, Response resp) throws Exception
   {
      //one time init
      if (sessionCache == null)
      {
         synchronized (this)
         {
            if (sessionCache == null)
               sessionCache = new LRUAuthSessionCache(sessionMax);
         }
      }

      User user = Chain.peek().getUser();

      if (user != null && !req.isDelete())
      {
         //the users is already logged in, have to let
         //deletes through because this could be a logout
         return;
      }

      String collection = getConfig("collection", this.collection);
      String authenticatedPerm = getConfig("authenticatedPerm", this.authenticatedPerm);
      long failedMax = Long.parseLong(getConfig("failedMax", this.failedMax + ""));
      long sessionExp = Long.parseLong(getConfig("sessionExp", this.sessionExp + ""));

      String accountCode = req.getApi().getAccountCode();
      String apiCode = req.getApi().getApiCode();
      String tenantCode = req.getTenantCode();

      //-- END CONFIG

      long now = System.currentTimeMillis();

      String username = null;
      String password = null;
      String sessionKey = null;
      boolean sessionReq = collection != null && collection.equalsIgnoreCase(req.getCollectionKey());

      String url = req.getUrl().toString().toLowerCase();
      while (url.endsWith("/"))
         url = url.substring(0, url.length() - 1);

      String token = req.getHeader("authorization");
      if (token == null)
         token = req.getHeader("x-auth-token");

      if (token != null)
      {
         token = token.trim();
         if (token.toLowerCase().startsWith("session "))
         {
            sessionKey = token.substring(8, token.length()).trim();
         }
         else if (token.toLowerCase().startsWith("basic "))
         {
            token = token.substring(token.indexOf(" ") + 1, token.length());
            token = new String(Base64.decodeBase64(token));
            username = token.substring(0, token.indexOf(":"));
            password = token.substring(token.indexOf(":") + 1, token.length());
         }
         else if (token != null && token.toLowerCase().startsWith("bearer "))
         {
            token = token.substring(token.indexOf(" ") + 1, token.length()).trim();
            DecodedJWT jwt = null;
            for (String secret : getJwtSecrets())
            {
               try
               {
                  JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret)).acceptLeeway(1).build();
                  //this will throw an exception if the signatures don't match
                  jwt = verifier.verify(token);
                  break;
               }
               catch (Exception ex)
               {
                  //-- this is not an error yet because there can be multiple signing keys in the list
                  //-- and this jwt may be using an older/different but still supported one in the list
                  //--
                  //-- multiple keys are supported so you can release keys with new signatures
                  //-- while supporting old signatures for some transition period.
               }
            }

            if (jwt == null)
               throw new ApiException(Status.SC_401_UNAUTHORIZED);

            user = createUserFromValidJwt(jwt);
         }
      }

      if (user == null)
      {

         if (req.isPost() && sessionReq && (Utils.empty(username, password)))
         {
            username = req.getJson().getString("username");
            password = req.getJson().getString("password");
         }

         if (sessionKey == null && Utils.empty(username, password))
         {
            username = req.removeParam("x-auth-username");
            password = req.removeParam("x-auth-password");
         }

         if (sessionKey == null && Utils.empty(username, password))
         {
            username = req.getHeader("username");
            password = req.getHeader("password");
         }

         if (sessionKey == null && Utils.empty(username, password))
         {
            username = req.removeParam("username");
            password = req.removeParam("password");
         }

         if (sessionReq && req.isDelete())
         {
            //this is a logout

            //delete to http[s]://{host}/{collection}/{sessionKey}
            if (sessionKey == null)
               sessionKey = url.substring(url.lastIndexOf("/") + 1, url.length());

            if (sessionKey == null)
               throw new ApiException(Status.SC_400_BAD_REQUEST, "Logout requires a session authroization or x-auth-token header");

            sessionCache.remove(sessionKey);
         }
         else if (!Utils.empty(username, password))
         {
            String salt = getSalt();
            if (salt == null)
            {
               log.warn("You must configure a salt value for password hashing.");
               throw new ApiException(Status.SC_500_INTERNAL_SERVER_ERROR);
            }

            user = dao.getUser(username, accountCode, apiCode, tenantCode);

            if (user != null)
            {
               String strongHash = strongHash(salt, password);
               String weakHash = weakHash(password);

               if (!(user.getPassword().equals(strongHash) || user.getPassword().equals(weakHash)))
                  user = null;
            }

            //         Connection conn = db.getConnection();
            //
            //         User tempUser = getUser(conn, api, req.getTenantCode(), username, null);
            //         boolean authorized = false;
            //         if (tempUser != null)
            //         {
            //            long requestAt = tempUser.getRequestAt();
            //            int failedNum = tempUser.getFailedNum();
            //            if (failedNum < failedMax || now - requestAt > failedExp)
            //            {
            //               //only attempt to validate password and log the attempt 
            //               //if the user has failed login fewer than failedMax times
            //               String remoteAddr = req.getRemoteAddr();
            //               authorized = checkPassword(conn, tempUser, password);
            //
            //               if (shouldTrackRequestTimes)
            //               {
            //                  String sql = "UPDATE User SET requestAt = ?, failedNum = ?, remoteAddr = ? WHERE id = ?";
            //                  SqlUtils.execute(conn, sql, now, authorized ? 0 : failedNum + 1, remoteAddr, tempUser.getId());
            //               }
            //
            //               if (authorized)
            //               {
            //                  tempUser.withRequestAt(now);
            //                  tempUser.withRoles(getRoles(conn, req.getApi(), tempUser));
            //                  tempUser.withPermissions(getPermissions(conn, req.getApi(), tempUser));
            //                  if (!Utils.empty(authenticatedPerm))
            //                  {
            //                     tempUser.withPermissions(authenticatedPerm);
            //                  }
            //
            //                  user = tempUser;
            //               }
            //            }
            //         }
            //
            //         if (tempUser == null || !authorized)
            //            throw new ApiException(SC.SC_401_UNAUTHORIZED);

         }
      }

      if (user == null)
      {
         if (sessionKey != null)
         {
            user = sessionCache.get(sessionKey);
            if (user != null && sessionExp > 0)
            {
               if (now - user.getRequestAt() > sessionExp)
               {
                  sessionCache.remove(sessionKey);
                  user = null;
               }
            }

            if (user == null)
               throw new ApiException(Status.SC_401_UNAUTHORIZED);
         }
      }

      if (user != null)
      {
         // update the session cache if this is a session request OR 
         // if the session update time has passed.
         boolean updateSessionCache = false;
         long previousReqTime = user.getRequestAt();
         if (now - previousReqTime > sessionUpdate)
            updateSessionCache = true;

         user.withRequestAt(now);
         Chain.peek().withUser(user);

         if (sessionReq && req.isPost())
         {
            sessionKey = req.getApi().getId() + "_" + newSessionId();
            updateSessionCache = true;

            resp.withHeader("x-auth-token", "Session " + sessionKey);
            JSNode obj = new JSNode();
            obj.put("id", user.getId());
            obj.put("username", username);
            obj.put("displayname", user.getDisplayName());

            JSArray perms = new JSArray();
            for (String perm : user.getPermissions())
            {
               perms.add(perm);
            }
            obj.put("perms", perms);

            JSArray roles = new JSArray();
            for (String role : user.getRoles())
            {
               roles.add(role);
            }
            obj.put("roles", roles);

            resp.withJson(new JSNode("data", obj));
         }

         if (updateSessionCache)
            sessionCache.put(sessionKey, user);
      }

      if (Chain.getUser() != null)
      {
         User loggedIn = Chain.getUser();
         if (api.isMultiTenant() && (req.getTenantCode() == null || !req.getTenantCode().equalsIgnoreCase(loggedIn.getTenantCode())))
            throw new ApiException(Status.SC_401_UNAUTHORIZED);
      }

      if (user == null && !sessionReq)
      {
         user = new User();
         user.withUsername("Anonymous");
         user.withRoles("guest");

         if (api.isMultiTenant())
         {
            //            String tenantCode = req.getTenantCode();
            //
            //            Integer tenantId = (Integer) api.getCache("TENANT_ID_" + tenantCode);
            //            if (tenantId == null)
            //            {
            //               Connection conn = dao.getConnection();
            //
            //               Object tenant = SqlUtils.selectValue(conn, "SELECT id FROM Tenant WHERE tenantCode = ?", tenantCode);
            //               if (tenant == null)
            //                  throw new ApiException(SC.SC_404_NOT_FOUND);
            //
            //               tenantId = Integer.parseInt(tenant + "");
            //               api.putCache("TENANT_ID_" + tenantCode, tenantId);
            //            }
            //
            //            user.withTenantCode(tenantCode);
            //            user.withTenantId(tenantId);
         }
         Chain.peek().withUser(user);
      }
   }

   User createUserFromValidJwt(DecodedJWT jwt)
   {
      User user = new User();
      user.withUsername(jwt.getSubject());

      Claim c = null;

      c = jwt.getClaim("groups");
      if (c != null && !c.isNull())
      {
         List<String> groups = c.asList(String.class);
         user.withRoles(groups.toArray(new String[groups.size()]));
      }

      c = jwt.getClaim("roles");
      if (c != null && !c.isNull())
      {
         List<String> roles = c.asList(String.class);
         user.withRoles(roles.toArray(new String[roles.size()]));
      }

      c = jwt.getClaim("tenantId");
      if (c != null && !c.isNull())
      {
         int tenantId = c.asInt();
         user.withTenantId(tenantId);
      }

      c = jwt.getClaim("tenantCode");
      if (c != null && !c.isNull())
      {
         String tenantCode = c.asString();
         user.withTenantCode(tenantCode);
      }

      addPermsToUser(user, jwt.getClaim("perms"));
      addPermsToUser(user, jwt.getClaim("actions"));

      return user;
   }

   void addPermsToUser(User user, Claim c)
   {
      if (c != null && !c.isNull())
      {
         List<String> perms = c.asList(String.class);
         user.withPermissions(perms.toArray(new String[perms.size()]));
      }
   }

   /**
    * Looks gwt signing secrets up as environment vars or sysprops.
    * 
    * Finds the most specific keys keys first
    * 
    * @param accountCode
    * @param apiCode
    * @param tenantCode
    * @return
    */
   List<String> getJwtSecrets()
   {
      Request req = Chain.peek().getRequest();
      String accountCode = req.getApi().getAccountCode();
      String apiCode = req.getApi().getApiCode();
      String tenantCode = req.getTenantCode();
      return getJwtSecrets(accountCode, apiCode, tenantCode);
   }

   List<String> getJwtSecrets(String accountCode, String apiCode, String tenantCode)
   {
      List secrets = new ArrayList();

      for (int i = 10; i >= 0; i--)
      {

         for (int j = 2; j >= 0; j--)
         {
            String key = (getCollectionName() != null ? getCollectionName() : "") + ".jwt" + (i == 0 ? "" : ("." + i));

            if (j > 0 && accountCode != null)
               key += "." + accountCode;

            if (j > 1 && apiCode != null)
               key += "." + apiCode;

            if (j > 2 && tenantCode != null)
               key += "." + tenantCode;

            key += ".secret";

            String secret = Utils.getSysEnvPropStr(key, null);
            if (secret != null)
            {
               secrets.add(secret);
            }
         }
      }

      return secrets;
   }

   public String signJwt(JWTCreator.Builder jwtBuilder) throws IllegalArgumentException, JWTCreationException, UnsupportedEncodingException
   {
      Request req = Chain.peek().getRequest();
      String accountCode = req.getApi().getAccountCode();
      String apiCode = req.getApi().getApiCode();
      String tenantCode = req.getTenantCode();

      return signJwt(jwtBuilder, accountCode, apiCode, tenantCode);
   }

   public String signJwt(JWTCreator.Builder jwtBuilder, String accountCode, String apiCode, String tenantCode) throws IllegalArgumentException, JWTCreationException, UnsupportedEncodingException
   {
      String secret = getJwtSecrets(accountCode, apiCode, tenantCode).get(0);
      return jwtBuilder.sign(Algorithm.HMAC256(secret));
   }

   public static String strongHash(Object salt, String password) throws ApiException
   {
      try
      {
         int iterationNb = 1000;
         MessageDigest digest = MessageDigest.getInstance("SHA-512");
         digest.reset();
         digest.update(salt.toString().getBytes());
         byte[] input = digest.digest(password.getBytes("UTF-8"));
         for (int i = 0; i < iterationNb; i++)
         {
            digest.reset();
            input = digest.digest(input);
         }

         String encoded = Base64.encodeBase64String(input).trim();
         return encoded;
      }
      catch (Exception ex)
      {
         throw new ApiException(Status.SC_500_INTERNAL_SERVER_ERROR);
      }
   }

   public static String weakHash(String password)
   {
      try
      {
         byte[] byteArr = password.getBytes();
         MessageDigest digest = MessageDigest.getInstance("MD5");
         digest.update(byteArr);
         byte[] bytes = digest.digest();

         String hex = (new HexBinaryAdapter()).marshal(bytes);

         return hex;
      }
      catch (Exception ex)
      {
         throw new RuntimeException(ex);
      }
   }

   protected String newSessionId()
   {
      String id = UUID.randomUUID().toString();
      id = id.replace("-", "");
      return id;
   }

   public AuthAction withSessionMax(int sessionMax)
   {
      this.sessionMax = sessionMax;
      return this;
   }

   public AuthAction withFailedMax(int failedMax)
   {
      this.failedMax = failedMax;
      return this;
   }

   public AuthAction withFailedExp(int failedExp)
   {
      this.failedExp = failedExp;
      return this;
   }

   public AuthAction withCollection(String collection)
   {
      this.collection = collection;
      return this;
   }

   public AuthAction withAuthenticatedPerm(String authenticatedPerm)
   {
      this.authenticatedPerm = authenticatedPerm;
      return this;
   }

   public AuthAction withSessionCache(AuthSessionCache sessionCache)
   {
      this.sessionCache = sessionCache;
      return this;
   }

   public AuthAction withShouldTrackRequestTimes(boolean shouldTrackRequestTimes)
   {
      this.shouldTrackRequestTimes = shouldTrackRequestTimes;
      return this;
   }

   public AuthAction withSessionExp(long sessionExp)
   {
      this.sessionExp = sessionExp;
      return this;
   }

   public AuthAction withDao(UserDao dao)
   {
      this.dao = dao;
      return this;
   }

   public AuthAction withSalt(String salt)
   {
      this.salt = salt;
      return this;
   }

   public String getSalt()
   {
      return Utils.getSysEnvPropStr(getCollectionName() + ".salt", salt);
   }

   class LRUAuthSessionCache implements AuthSessionCache
   {
      LRUMap map;

      public LRUAuthSessionCache(int sessionMax)
      {
         super();
         this.map = new LRUMap(sessionMax);;
      }

      @Override
      public User get(String sessionKey)
      {
         return (User) map.get(sessionKey);
      }

      @Override
      public void put(String sessionKey, User user)
      {
         map.put(sessionKey, user);
      }

      @Override
      public void remove(String sessionKey)
      {
         map.remove(sessionKey);
      }

   }

   public static interface UserDao
   {
      User getUser(String username, String accountCode, String apiCode, String tenantCode) throws Exception;
   }

}
