/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
 * http://rocketpartners.io
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
package io.rocketpartners.cloud.action.security;

import java.security.MessageDigest;
import java.security.Permission;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.map.LRUMap;

import io.rocketpartners.cloud.action.sql.SqlDb;
import io.rocketpartners.cloud.model.Action;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.ApiException;
import io.rocketpartners.cloud.model.ArrayNode;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.model.ObjectNode;
import io.rocketpartners.cloud.model.Request;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.model.User;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.SqlUtils;
import io.rocketpartners.cloud.utils.Utils;

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

   protected SqlDb            db                      = null;

   protected boolean          shouldTrackRequestTimes = true;

   public AuthAction()
   {
      withOrder(100);
   }
   
   @Override
   public void run(Service service, Api api, Endpoint endpoint, Chain chain, Request req, Response resp) throws Exception
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
         token = token.trim().toLowerCase();
         if (token.startsWith("session "))
         {
            sessionKey = token.toLowerCase().substring(8, token.length()).trim();
         }
         else if (token.startsWith("basic "))
         {
            token = token.substring(token.indexOf(" ") + 1, token.length());
            token = new String(Base64.decodeBase64(token));
            username = token.substring(0, token.indexOf(":"));
            password = token.substring(token.indexOf(":") + 1, token.length());
         }
      }

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
         Connection conn = db.getConnection();

         User tempUser = getUser(conn, api, req.getTenantCode(), username, null);
         boolean authorized = false;
         if (tempUser != null)
         {
            long requestAt = tempUser.getRequestAt();
            int failedNum = tempUser.getFailedNum();
            if (failedNum < failedMax || now - requestAt > failedExp)
            {
               //only attempt to validate password and log the attempt 
               //if the user has failed login fewer than failedMax times
               String remoteAddr = req.getRemoteAddr();
               authorized = checkPassword(conn, tempUser, password);

               if (shouldTrackRequestTimes)
               {
                  String sql = "UPDATE User SET requestAt = ?, failedNum = ?, remoteAddr = ? WHERE id = ?";
                  SqlUtils.execute(conn, sql, now, authorized ? 0 : failedNum + 1, remoteAddr, tempUser.getId());
               }

               if (authorized)
               {
                  tempUser.withRequestAt(now);
                  tempUser.withRoles(getRoles(conn, req.getApi(), tempUser));
                  tempUser.withPermissions(getPermissions(conn, req.getApi(), tempUser));
                  if (!Utils.empty(authenticatedPerm))
                  {
                     tempUser.withPermissions(authenticatedPerm);
                  }

                  user = tempUser;
               }
            }
         }

         if (tempUser == null || !authorized)
            throw new ApiException(SC.SC_401_UNAUTHORIZED);

      }

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
            ObjectNode obj = new ObjectNode();
            obj.put("id", user.getId());
            obj.put("username", username);
            obj.put("displayname", user.getDisplayName());

            ArrayNode perms = new ArrayNode();
            for (String perm : user.getPermissions())
            {
               perms.add(perm);
            }
            obj.put("perms", perms);

            ArrayNode roles = new ArrayNode();
            for (String role : user.getRoles())
            {
               roles.add(role);
            }
            obj.put("roles", roles);

            resp.withJson(new ObjectNode("data", obj));
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
            String tenantCode = req.getTenantCode();

            Integer tenantId = (Integer) api.getCache("TENANT_ID_" + tenantCode);
            if (tenantId == null)
            {
               Connection conn = db.getConnection();

               Object tenant = SqlUtils.selectValue(conn, "SELECT id FROM Tenant WHERE tenantCode = ?", tenantCode);
               if (tenant == null)
                  throw new ApiException(SC.SC_404_NOT_FOUND);

               tenantId = Integer.parseInt(tenant + "");
               api.putCache("TENANT_ID_" + tenantCode, tenantId);
            }

            user.withTenantCode(tenantCode);
            user.withTenantId(tenantId);
         }
         req.withUser(user);
      }

   }

   protected User getUser(Connection conn, Api api, String tenantCode, String username, String accessKey) throws Exception
   {
      if (Utils.empty(username, accessKey))
         throw new ApiException(SC.SC_401_UNAUTHORIZED);

      if (api.isMultiTenant() && Utils.empty(tenantCode))
         throw new ApiException(SC.SC_401_UNAUTHORIZED);

      String sql = "";
      List params = new ArrayList();
      if (api.isMultiTenant())
      {
         sql += " SELECT DISTINCT u.*, t.id AS tenantId, t.tenantCode ";
         sql += " FROM User u   ";
         sql += " JOIN Tenant t ON t.tenantCode = ? ";
         params.add(tenantCode);
      }
      else
      {
         sql += " SELECT DISTINCT u.*";
         sql += " FROM User u   ";
      }

      sql += " WHERE (u.revoked IS NULL OR u.revoked != 1) ";

      if (!Utils.empty(username))
      {
         sql += " AND u.username = ? ";
         params.add(username);
      }
      else
      {
         sql += " AND u.accessKey = ? ";
         params.add(accessKey);
      }
      sql += " LIMIT 1 ";

      return SqlUtils.selectObject(conn, sql, User.class, params);
   }

   boolean checkPassword(Connection conn, User user, String password)
   {
      boolean matched = false;

      String savedHash = user.getPassword();
      String newHash = hashPassword(user.getId(), password);

      if (savedHash.equals(newHash))
      {
         matched = true;
      }
      else if (savedHash.equalsIgnoreCase(md5(password.getBytes())))
      {
         //this allows for manual password recovery by manually putting an
         //md5 of the password into the password col in the db
         matched = true;
      }

      return matched;
   }

   protected String[] getRoles(Connection conn, Api api, User user) throws Exception
   {
      String sql = "";
      sql += " SELECT DISTINCT r.name ";
      sql += " FROM Role r JOIN UserRole ur ON ur.roleId = r.id AND ur.userId = ?";
      List roles = SqlUtils.selectList(conn, sql, user.getId());
      return (String[]) roles.toArray(new String[roles.size()]);
   }

   protected String[] getPermissions(Connection conn, Api api, User user) throws Exception
   {
      String sql = "";
      sql += "\r\n SELECT DISTINCT name ";
      sql += "\r\n  FROM ";
      sql += "\r\n  ( ";
      sql += "\r\n    SELECT p.name ";
      sql += "\r\n    FROM Permission p";
      sql += "\r\n    JOIN UserPermission up ON p.id = up.permissionId";
      sql += "\r\n    WHERE up.userId = ? AND up.apiId = ? AND up.tenantId = ? ";
      sql += "\r\n                                                           ";
      sql += "\r\n    UNION";
      sql += "\r\n                                                           ";
      sql += "\r\n    SELECT p.name";
      sql += "\r\n    FROM Permission p";
      sql += "\r\n    JOIN GroupPermission gp ON p.id = gp.permissionId";
      sql += "\r\n    JOIN UserGroup ug ON ug.groupId = gp.groupId ";
      sql += "\r\n    WHERE ug.userId = ? and gp.apiId = ? AND gp.tenantId = ? ";
      sql += "\r\n  ) as perms";

      List args = Arrays.asList(user.getId(), api.getId(), user.getTenantId(), user.getId(), api.getId(), user.getTenantId());
      List<String> perms = SqlUtils.selectObjects(conn, sql, Permission.class, args);
      return perms.toArray(new String[perms.size()]);
   }

   public static String hashPassword(Object salt, String password) throws ApiException
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

   private String md5(byte[] byteArr)
   {
      try
      {
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

   //   void close(Connection conn)
   //   {
   //      if (conn != null)
   //      {
   //         try
   //         {
   //            conn.close();
   //         }
   //         catch (Exception ex)
   //         {
   //            ex.printStackTrace();
   //         }
   //      }
   //   }

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

   public AuthAction withDb(SqlDb db)
   {
      this.db = db;
      return this;
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

}
