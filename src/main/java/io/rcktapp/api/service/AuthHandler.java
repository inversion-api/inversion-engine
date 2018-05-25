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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.rcktapp.api.service;

import java.security.MessageDigest;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.map.LRUMap;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.mchange.v2.c3p0.ComboPooledDataSource;

import io.forty11.j.J;
import io.forty11.js.JSArray;
import io.forty11.js.JSObject;
import io.forty11.sql.Rows.Row;
import io.forty11.sql.Sql;
import io.rcktapp.api.Api;
import io.rcktapp.api.ApiException;
import io.rcktapp.api.Chain;
import io.rcktapp.api.Db;
import io.rcktapp.api.Handler;
import io.rcktapp.api.Response;
import io.rcktapp.api.Rule;
import io.rcktapp.api.SC;
import io.rcktapp.api.User;

public class AuthHandler implements Handler
{
   long   sessionExp = 1000 * 60 * 30; //30 minute default timeput
   int    sessionMax = 10000;

   int    failedMax  = 10;
   int    failedExp  = 1000 * 60 * 10; //10 minute timeout for failed password attemtps

   String collection = null;

   LRUMap sessions   = null;

   Db     db         = null;

   @Override
   public void service(Service service, Chain chain, Rule rule, io.rcktapp.api.Request req, Response resp) throws Exception
   {
      if(req.getUser() != null)
         return;
      
      boolean authorized = false;
      String debug = null;

      try
      {
         //one time init
         if (sessions == null)
         {
            synchronized (this)
            {
               if (sessions == null)
                  sessions = new LRUMap(sessionMax);
            }
         }

         //--
         //-- APPLY RULE SPECIFIC CONFIG
         //--  
         String collection = this.collection;
         long failedMax = this.failedMax;
         long sessionExp = this.sessionExp;

         JSObject config = rule.getConfig();
         if (config != null)
         {
            if (config.containsKey("collection"))
               collection = config.getString("collection");

            if (config.containsKey("failedMax"))
            {
               failedMax = Long.parseLong(config.getString("failedMax"));
            }

            if (config.containsKey("sessionExp"))
            {
               sessionExp = Long.parseLong(config.getString("sessionExp"));
            }
         }
         //-- END CONFIG

         Connection conn = null;
         long now = System.currentTimeMillis();

         try
         {
            String url = req.getUrl().toLowerCase();
            while (url.endsWith("/"))
               url = url.substring(0, url.length() - 1);

            String token = req.getHeader("authorization");
            if (token == null)
               token = req.getHeader("x-auth-token");

            String sessionKey = null;
            if (token != null && token.toLowerCase().startsWith("session "))
            {
               sessionKey = token.toLowerCase().substring(8, token.length()).trim();
            }

            if (collection != null && collection.equalsIgnoreCase(req.getCollectionKey()))
            {
               //this is a logout
               if (req.isDelete() && rule.hasMethod("delete"))
               {
                  //delete to http[s]://{host}/{collection}/{sessionKey}
                  if (sessionKey == null)
                     sessionKey = url.substring(url.lastIndexOf("/") + 1, url.length());

                  if (sessionKey == null)
                     throw new ApiException(SC.SC_400_BAD_REQUEST, "Logout requires a session authroization or x-auth-token header");

                  sessions.remove(sessionKey);
                  authorized = true;
               }
               //this is a login
               else if (req.isPost() && rule.hasMethod("post"))
               {
                  conn = db.getDs().getConnection();

                  String username = null;
                  String password = null;

                  if (token != null)
                  {
                     token = token.trim();
                     if (token.toLowerCase().startsWith("basic "))
                     {
                        token = token.substring(token.indexOf(" ") + 1, token.length());
                        token = new String(Base64.decodeBase64(token));
                        username = token.substring(0, token.indexOf(":"));
                        password = token.substring(token.indexOf(":") + 1, token.length());
                     }
                  }
                  else if (req.getJson() != null)
                  {
                     username = req.getJson().getString("username");
                     password = req.getJson().getString("password");
                  }

                  username = username != null ? username : req.getParam("username");
                  if (username != null && username.length() > 255)
                     username = username.substring(0, 255);

                  password = password != null ? password : req.getParam("password");

                  if (username == null || password == null)
                  {
                     throw new ApiException(SC.SC_400_BAD_REQUEST);
                  }

                  User user = getUser(conn, req.getApi(), username, null);

                  if (user != null)
                  {
                     long requestAt = user.getRequestAt();
                     int failedNum = user.getFailedNum();
                     if (failedNum < failedMax || now - requestAt > failedExp)
                     {
                        //only attempt to validate password and log the attempt 
                        //if the user has failed login fewer than failedMax times
                        String remoteAddr = req.getHttpServletRequest().getRemoteAddr();
                        authorized = checkPassword(conn, user, password);

                        String sql = "UPDATE User SET requestAt = ?, failedNum = ?, remoteAddr = ? WHERE id = ?";
                        Sql.execute(conn, sql, now, authorized ? 0 : failedNum + 1, remoteAddr, user.getId());
                     }
                  }

                  if (user == null || !authorized)
                     throw new ApiException(SC.SC_401_UNAUTHORIZED);

                  user.setRequestAt(now);
                  req.setUser(user);

                  Row rolesAndPerms = getRolesAndPerms(conn, req.getApi(), user);
                  String roles = rolesAndPerms.getString("roles");
                  if (roles != null)
                     user.setRoles(Arrays.asList(roles.split(",")));

                  String perms = rolesAndPerms.getString("perms");
                  if (perms != null)
                     user.setPerms(Arrays.asList(perms.split(",")));

                  sessionKey = req.getApi().getId() + "_" + newSessionId();//
                  sessions.put(sessionKey, user);

                  debug = "create session: " + sessionKey;

                  //resp.getHttpResp().setHeader("x-auth-token", "Session " + sessionKey);

                  JSObject obj = new JSObject();
                  //obj.put("x-auth-token", "Session " + session);
                  obj.put("username", username);
                  obj.put("headers", new JSArray(new JSObject("header", "x-auth-token", "value", "session " + sessionKey)));
                  obj.put("perms", new JSArray(user.getPerms()));
                  obj.put("roles", new JSArray(user.getRoles()));
                  resp.setJson(new JSObject("data", obj));
               }
            }
            //request other than a login/logout so we have to validate the session or the barer token
            else if (rule.hasMethod(req.getMethod()))
            {
               if (sessionKey != null)
               {
                  User user = (User) sessions.get(sessionKey);
                  debug = "lookup session: " + sessionKey + " " + user;
                  if (user != null)
                  {
                     if (sessionExp > 0 && now - user.getRequestAt() > sessionExp)
                     {
                        debug += " SESSION EXPIRED";
                        sessions.remove(token);
                        user = null;
                     }
                  }
                  if (user == null)
                  {
                     throw new ApiException(SC.SC_401_UNAUTHORIZED);
                  }
                  else
                  {
                     user.setRequestAt(now);
                     req.setUser(user);
                     authorized = true;
                  }
               }
               else if (token != null && token.toLowerCase().startsWith("bearer "))
               {
                  token = token.substring(token.indexOf(" ") + 1, token.length()).trim();

                  JWT jwt = JWT.decode(token);

                  String accessKey = jwt.getSubject();

                  conn = db.getDs().getConnection();
                  User user = getUser(conn, req.getApi(), null, accessKey);

                  if (user == null)
                     throw new ApiException(SC.SC_401_UNAUTHORIZED);

                  authorized = true;

                  String secretKey = (String) user.getSecretKey();

                  JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secretKey)).acceptLeeway(1).build();

                  //this will throw an exception if the signatures don't match
                  DecodedJWT verified = verifier.verify(token);

                  Claim c = jwt.getClaim("perms");
                  List<String> perms = c != null ? c.asList(String.class) : null;

                  user = new User();
                  user.setUsername(jwt.getSubject());
                  user.setPerms(perms);
                  req.setUser(user);
               }
               //            else
               //            {
               //               throw new ApiException(SC.SC_400_BAD_REQUEST, "Unknown authroization scheme");
               //            }
            }
         }
         finally
         {
            Sql.close(conn);
         }
      }
      catch (Exception ex)
      {
         authorized = false;
         if (ex instanceof ApiException)
            throw ((ApiException) ex);
         else
            throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR);
      }
      finally
      {
         System.out.println("AuthHandler: " + req.getMethod() + " " + authorized + " - " + req.getUrl() +  "  - " + debug);
      }
   }

   User getUser(Connection conn, Api api, String username, String accessKey) throws Exception
   {
      String sql = "";
      sql += " SELECT u.* ";
      sql += " FROM User u ";
      sql += " WHERE u.orgId = ? ";
      if (username != null)
      {
         sql += " AND u.username = ? ";
      }
      else if (accessKey != null)
      {
         sql += " AND u.accessKey = ?";
      }
      else
      {
         throw new ApiException(SC.SC_400_BAD_REQUEST);
      }
      sql += " AND u.revoked IS NULL OR u.revoked != 1 LIMIT 1";

      User user = null;
      Row row = Sql.selectRow(conn, sql, api.getOrgId(), username != null ? username : accessKey);
      if (row != null)
      {
         user = new User();
         user.setUsername(row.getString("username"));
         user.setPassword(row.getString("password"));
         user.setAccessKey(row.getString("accessKey"));
         user.setSecretKey(row.getString("secretKey"));
         user.setRequestAt(row.getLong("requestAt"));
         user.setFailedNum(row.getInt("failedNum"));
      }
      return user;
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
         //this allows for manual password recovery by manually putting anfa
         //md5 of the password into the password col in the db
         matched = true;
      }

      return matched;
   }

   Row getRolesAndPerms(Connection conn, Api api, User user) throws Exception
   {
      String sql = "";
      sql += "SELECT GROUP_CONCAT(DISTINCT perm order by perm) as perms, ";
      sql += "    (SELECT GROUP_CONCAT(DISTINCT role order by role) FROM Role r JOIN UserRole ur ON ur.roleId = r.id AND ur.userId = ?) as roles ";
      sql += "FROM ";
      sql += "( ";
      sql += "   SELECT p.perm";
      sql += "   FROM Permission p";
      sql += "   JOIN UserPermission up ON p.id = up.permissionId";
      sql += "   WHERE up.userId = ? AND up.apiId = ?";
      sql += "                                                          ";
      sql += "   UNION";
      sql += "                                                          ";
      sql += "   SELECT p.perm";
      sql += "   FROM Permission p";
      sql += "   JOIN GroupPermission gp ON p.id = gp.permissionId";
      sql += "   JOIN UserGroup ug ON ug.groupId = gp.groupId ";
      sql += "   WHERE ug.userId = ? and gp.apiId = ?";
      sql += ") as perms";

      return Sql.selectRow(conn, sql, user.getId(), user.getId(), api.getId(), user.getId(), api.getId());
   }

   private String hashPassword(Object salt, String password) throws ApiException
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

   void close(Connection conn)
   {
      if (conn != null)
      {
         try
         {
            conn.close();
         }
         catch (Exception ex)
         {
            ex.printStackTrace();
         }
      }
   }

   protected String newSessionId()
   {
      String id = UUID.randomUUID().toString();
      id = id.replace("-", "");
      return id;
   }

}
