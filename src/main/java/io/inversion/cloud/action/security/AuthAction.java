/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package io.inversion.cloud.action.security;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
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

import io.inversion.cloud.action.sql.SqlDb;
import io.inversion.cloud.model.Action;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.ApiException;
import io.inversion.cloud.model.Endpoint;
import io.inversion.cloud.model.JSArray;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.model.SC;
import io.inversion.cloud.model.User;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.service.Engine;
import io.inversion.cloud.utils.Rows;
import io.inversion.cloud.utils.Rows.Row;
import io.inversion.cloud.utils.SqlUtils;
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
   public void run(Engine engine, Api api, Endpoint endpoint, Chain chain, Request req, Response resp) throws Exception
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

      User user = req.getUser();

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
                  jwt = verifier.verify(token);
                  break;
               }
               catch (Exception ex)
               {
               }

               if (jwt == null)
                  throw new ApiException(SC.SC_401_UNAUTHORIZED);
            }

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
               throw new ApiException(SC.SC_400_BAD_REQUEST, "Logout requires a session authroization or x-auth-token header");

            sessionCache.remove(sessionKey);
         }
         else if (!Utils.empty(username, password))
         {
            String salt = getSalt();
            if (salt == null)
            {
               log.warn("You must configure a salt value for password hashing.");
               throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR);
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
               throw new ApiException(SC.SC_401_UNAUTHORIZED);
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
         req.withUser(user);

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

      if (req.getUser() != null)
      {
         User loggedIn = req.getUser();
         if (api.isMultiTenant() && (req.getTenantCode() == null || !req.getTenantCode().equalsIgnoreCase(loggedIn.getTenantCode())))
            throw new ApiException(SC.SC_401_UNAUTHORIZED);
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
         req.withUser(user);
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
            String key = (getName() != null ? getName() : "") + ".jwt" + (i == 0 ? "" : ("." + i));

            if (j > 0 && accountCode != null)
               key += "." + accountCode;

            if (j > 1 && apiCode != null)
               key += "." + apiCode;

            if (j > 2 && tenantCode != null)
               key += "." + tenantCode;

            key += ".secret";

            String secret = Utils.findSysEnvPropStr(key, null);
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
         throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR);
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

   public SqlDbUserDao withSalt(String salt)
   {
      this.salt = salt;
      return null;
   }

   public String getSalt()
   {
      return Utils.findSysEnvPropStr(getName() + ".salt", salt);
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

   public static class SqlDbUserDao implements UserDao
   {
      SqlDb db = null;

      public SqlDbUserDao()
      {

      }

      public SqlDbUserDao(SqlDb db)
      {
         withDb(db);
      }

      public SqlDbUserDao withDb(SqlDb db)
      {
         this.db = db;
         return this;
      }

      public SqlDb getDb()
      {
         return db;
      }

      public User getUser(String username, String accountCode, String apiCode, String tenantCode) throws Exception
      {
         Connection conn = null;
         User user = null;
         try
         {
            List params = new ArrayList();
            String sql = "";

            if (username != null)
            {
               params.add(username);
               sql += " SELECT DISTINCT u.*";
               sql += " FROM User u   ";
               sql += " WHERE (u.revoked IS NULL OR u.revoked != 1) ";
               sql += " AND u.username = ? ";
               //sql += " AND (u.password = ? OR u.password = ?)";
               sql += " LIMIT 1 ";
            }
            //            else
            //            {
            //               params.add(apiCode);
            //               sql += " SELECT DISTINCT u.*";
            //               sql += " FROM User u   ";
            //               sql += " JOIN ApiKey a ON a.userId = u.id";
            //               sql += " WHERE (u.revoked IS NULL OR u.revoked != 1) ";
            //               sql += " AND (a.revoked IS NULL OR a.revoked != 1) ";
            //               sql += " AND a.accessKey = ? ";
            //               sql += " AND (a.secretKey = ? OR a.secretKey = ?)";
            //               sql += " LIMIT 1 ";
            //            }

            conn = db.getConnection();
            user = SqlUtils.selectObject(conn, sql, User.class, username);

            if (user != null)
            {
               Rows rows = findGRP(conn, user.getId(), accountCode, apiCode, tenantCode);
               if (rows == null)
               {
                  //-- there is a users with the given username but the don't have any association to this accountCoce/apiCode/tenantCode
                  user = null;
               }
               else
               {
                  populateGRP(user, rows);
               }
            }
         }
         finally
         {
            conn.close();
         }

         return user;
      }

      void populateGRP(User user, Rows rows)
      {
         for (Row row : rows)
         {
            String type = row.getString("type");
            String name = row.getString("name");
            if (name != null)
            {
               switch (type)
               {
                  case "group":
                     user.withGroups(name);
                     break;
                  case "role":
                     user.withRoles(name);
                     break;
                  case "permission":
                     user.withPermissions(name);
                     break;
               }
            }
         }
      }

      /**
       * user -> permission
       * user -> group -> permission
       * user -> role -> permission
       * user -> group -> role -> permission

       * UserPermission
       * GroupPermission
       * RolePermission
       * UserGroup
       * UserRole
       * GroupRole
       *
       *
       *
       */
      Rows findGRP(Connection conn, int userId, String accountCode, String apiCode, String tenantCode) throws Exception
      {
         List vals = new ArrayList();

         String sql = "";

         sql += "SELECT * FROM (";

         //-- user -> permission
         sql += "\r\n    SELECT 'permission' as type, p.name, 'user->permission' as via";
         sql += "\r\n    FROM Permission p";
         sql += "\r\n    JOIN UserPermission u ON p.id = u.permissionId";
         sql += "\r\n    WHERE u.userId = ?";
         vals.add(userId);

         sql += "\r\n     AND ((p.accountCode is null OR p.accountCode = ? ) AND (p.apiCode is null OR p.apiCode = ?) AND (p.tenantCode is null OR p.tenantCode = ?))";
         sql += "\r\n     AND ((u.accountCode is null OR u.accountCode = ? ) AND (u.apiCode is null OR u.apiCode = ?) AND (u.tenantCode is null OR u.tenantCode = ?))";
         vals.addAll(Arrays.asList(accountCode, apiCode, tenantCode, accountCode, apiCode, tenantCode));

         //-- user -> group -> permission
         sql += "\r\n                                                           ";
         sql += "\r\n    UNION";
         sql += "\r\n                                                           ";
         sql += "\r\n    SELECT 'permission' as type, p.name, 'user->group->permission' as via";
         sql += "\r\n    FROM Permission p";
         sql += "\r\n    JOIN GroupPermission g ON p.id = g.permissionId";
         sql += "\r\n    JOIN UserGroup u ON u.groupId = g.groupId ";
         sql += "\r\n    WHERE u.userId = ?";
         sql += "\r\n     AND ((p.accountCode is null OR p.accountCode = ? ) AND (p.apiCode is null OR p.apiCode = ?) AND (p.tenantCode is null OR p.tenantCode = ?))";
         sql += "\r\n     AND ((g.accountCode is null OR g.accountCode = ? ) AND (g.apiCode is null OR g.apiCode = ?) AND (g.tenantCode is null OR g.tenantCode = ?))";
         sql += "\r\n     AND ((u.accountCode is null OR u.accountCode = ? ) AND (u.apiCode is null OR u.apiCode = ?) AND (u.tenantCode is null OR u.tenantCode = ?))";

         vals.addAll(Arrays.asList(userId, accountCode, apiCode, tenantCode, accountCode, apiCode, tenantCode, accountCode, apiCode, tenantCode));

         //-- user -> role -> permission
         sql += "\r\n                                                           ";
         sql += "\r\n    UNION";
         sql += "\r\n                                                           ";
         sql += "\r\n    SELECT 'permission' as type, p.name, 'user->role->permission' as via";
         sql += "\r\n    FROM Permission p";
         sql += "\r\n    JOIN RolePermission r ON p.id = r.permissionId";
         sql += "\r\n    JOIN UserRole u ON u.roleId = r.roleId ";
         sql += "\r\n    WHERE u.userId = ?";
         sql += "\r\n     AND ((p.accountCode is null OR p.accountCode = ? ) AND (p.apiCode is null OR p.apiCode = ?) AND (p.tenantCode is null OR p.tenantCode = ?))";
         sql += "\r\n     AND ((r.accountCode is null OR r.accountCode = ? ) AND (r.apiCode is null OR r.apiCode = ?) AND (r.tenantCode is null OR r.tenantCode = ?))";
         sql += "\r\n     AND ((u.accountCode is null OR u.accountCode = ? ) AND (u.apiCode is null OR u.apiCode = ?) AND (u.tenantCode is null OR u.tenantCode = ?))";

         vals.addAll(Arrays.asList(userId, accountCode, apiCode, tenantCode, accountCode, apiCode, tenantCode, accountCode, apiCode, tenantCode));

         //-- user -> group -> role -> permission
         sql += "\r\n                                                           ";
         sql += "\r\n    UNION";
         sql += "\r\n                                                           ";
         sql += "\r\n    SELECT 'permission' as type, p.name, 'user->group->role->permission' as via";
         sql += "\r\n    FROM Permission p";
         sql += "\r\n    JOIN RolePermission r ON p.id = r.permissionId";
         sql += "\r\n    JOIN GroupRole g ON r.roleID = g.roleId";
         sql += "\r\n    JOIN UserGroup u ON g.groupId = u.groupId";
         sql += "\r\n    WHERE u.userId = ?";

         sql += "\r\n     AND ((p.accountCode is null OR p.accountCode = ? ) AND (p.apiCode is null OR p.apiCode = ?) AND (p.tenantCode is null OR p.tenantCode = ?))";
         sql += "\r\n     AND ((r.accountCode is null OR r.accountCode = ? ) AND (r.apiCode is null OR r.apiCode = ?) AND (r.tenantCode is null OR r.tenantCode = ?))";
         sql += "\r\n     AND ((g.accountCode is null OR g.accountCode = ? ) AND (g.apiCode is null OR g.apiCode = ?) AND (g.tenantCode is null OR g.tenantCode = ?))";
         sql += "\r\n     AND ((u.accountCode is null OR u.accountCode = ? ) AND (u.apiCode is null OR u.apiCode = ?) AND (u.tenantCode is null OR u.tenantCode = ?))";

         vals.addAll(Arrays.asList(userId, accountCode, apiCode, tenantCode, accountCode, apiCode, tenantCode, accountCode, apiCode, tenantCode, accountCode, apiCode, tenantCode));

         //-- user -> group
         sql += "\r\n                                                           ";
         sql += "\r\n    UNION";
         sql += "\r\n                                                           ";
         sql += "\r\n    SELECT 'group' as type, g.name, '' as via";
         sql += "\r\n    FROM `Group` g";
         sql += "\r\n    JOIN UserGroup u ON g.id = u.groupId";
         sql += "\r\n    WHERE u.userId = ?";
         sql += "\r\n     AND ((u.accountCode is null OR u.accountCode = ? ) AND (u.apiCode is null OR u.apiCode = ?) AND (u.tenantCode is null OR u.tenantCode = ?))";
         vals.addAll(Arrays.asList(userId, accountCode, apiCode, tenantCode));

         //-- user -> role
         sql += "\r\n                                                           ";
         sql += "\r\n    UNION";
         sql += "\r\n                                                           ";
         sql += "\r\n    SELECT 'role' as type, r.name, '' as via";
         sql += "\r\n    FROM Role r";
         sql += "\r\n    JOIN UserRole u ON r.id = u.roleId";
         sql += "\r\n    WHERE u.userId = ?";
         sql += "\r\n     AND ((u.accountCode is null OR u.accountCode = ? ) AND (u.apiCode is null OR u.apiCode = ?) AND (u.tenantCode is null OR u.tenantCode = ?))";
         vals.addAll(Arrays.asList(userId, accountCode, apiCode, tenantCode));

         sql += " ) as q ORDER BY type, name, via";

         return SqlUtils.selectRows(conn, sql, vals);
      }

   }

}
