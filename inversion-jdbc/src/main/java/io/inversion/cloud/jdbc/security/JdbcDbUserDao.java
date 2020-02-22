/*
 * Copyright (c) 2015-2020 Rocket Partners, LLC
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
package io.inversion.cloud.jdbc.security;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.inversion.cloud.action.security.AuthAction.UserDao;
import io.inversion.cloud.jdbc.db.JdbcDb;
import io.inversion.cloud.jdbc.utils.JdbcUtils;
import io.inversion.cloud.model.User;
import io.inversion.cloud.utils.Rows;
import io.inversion.cloud.utils.Rows.Row;

public class JdbcDbUserDao implements UserDao
{
   JdbcDb db = null;

   public JdbcDbUserDao()
   {

   }

   public JdbcDbUserDao(JdbcDb db)
   {
      withDb(db);
   }

   public JdbcDbUserDao withDb(JdbcDb db)
   {
      this.db = db;
      return this;
   }

   public JdbcDb getDb()
   {
      return db;
   }

   public User getUser(String username, String apiCode, String tenantCode) throws Exception
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
         user = JdbcUtils.selectObject(conn, sql, User.class, username);

         if (user != null)
         {
            Rows rows = findGRP(conn, user.getId(), apiCode, tenantCode);
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
   Rows findGRP(Connection conn, int userId, String apiCode, String tenantCode) throws Exception
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

      sql += "\r\n     AND ((p.apiCode is null OR p.apiCode = ?) AND (p.tenantCode is null OR p.tenantCode = ?))";
      sql += "\r\n     AND ((u.apiCode is null OR u.apiCode = ?) AND (u.tenantCode is null OR u.tenantCode = ?))";
      vals.addAll(Arrays.asList(apiCode, tenantCode, apiCode, tenantCode));

      //-- user -> group -> permission
      sql += "\r\n                                                           ";
      sql += "\r\n    UNION";
      sql += "\r\n                                                           ";
      sql += "\r\n    SELECT 'permission' as type, p.name, 'user->group->permission' as via";
      sql += "\r\n    FROM Permission p";
      sql += "\r\n    JOIN GroupPermission g ON p.id = g.permissionId";
      sql += "\r\n    JOIN UserGroup u ON u.groupId = g.groupId ";
      sql += "\r\n    WHERE u.userId = ?";
      sql += "\r\n     AND ((p.apiCode is null OR p.apiCode = ?) AND (p.tenantCode is null OR p.tenantCode = ?))";
      sql += "\r\n     AND ((g.apiCode is null OR g.apiCode = ?) AND (g.tenantCode is null OR g.tenantCode = ?))";
      sql += "\r\n     AND ((u.apiCode is null OR u.apiCode = ?) AND (u.tenantCode is null OR u.tenantCode = ?))";

      vals.addAll(Arrays.asList(userId, apiCode, tenantCode, apiCode, tenantCode, apiCode, tenantCode));

      //-- user -> role -> permission
      sql += "\r\n                                                           ";
      sql += "\r\n    UNION";
      sql += "\r\n                                                           ";
      sql += "\r\n    SELECT 'permission' as type, p.name, 'user->role->permission' as via";
      sql += "\r\n    FROM Permission p";
      sql += "\r\n    JOIN RolePermission r ON p.id = r.permissionId";
      sql += "\r\n    JOIN UserRole u ON u.roleId = r.roleId ";
      sql += "\r\n    WHERE u.userId = ?";
      sql += "\r\n     AND ((p.apiCode is null OR p.apiCode = ?) AND (p.tenantCode is null OR p.tenantCode = ?))";
      sql += "\r\n     AND ((r.apiCode is null OR r.apiCode = ?) AND (r.tenantCode is null OR r.tenantCode = ?))";
      sql += "\r\n     AND ((u.apiCode is null OR u.apiCode = ?) AND (u.tenantCode is null OR u.tenantCode = ?))";

      vals.addAll(Arrays.asList(userId, apiCode, tenantCode, apiCode, tenantCode, apiCode, tenantCode));

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

      sql += "\r\n     AND ((p.apiCode is null OR p.apiCode = ?) AND (p.tenantCode is null OR p.tenantCode = ?))";
      sql += "\r\n     AND ((r.apiCode is null OR r.apiCode = ?) AND (r.tenantCode is null OR r.tenantCode = ?))";
      sql += "\r\n     AND ((g.apiCode is null OR g.apiCode = ?) AND (g.tenantCode is null OR g.tenantCode = ?))";
      sql += "\r\n     AND ((u.apiCode is null OR u.apiCode = ?) AND (u.tenantCode is null OR u.tenantCode = ?))";

      vals.addAll(Arrays.asList(userId, apiCode, tenantCode, apiCode, tenantCode, apiCode, tenantCode, apiCode, tenantCode));

      //-- user -> group
      sql += "\r\n                                                           ";
      sql += "\r\n    UNION";
      sql += "\r\n                                                           ";
      sql += "\r\n    SELECT 'group' as type, g.name, '' as via";
      sql += "\r\n    FROM `Group` g";
      sql += "\r\n    JOIN UserGroup u ON g.id = u.groupId";
      sql += "\r\n    WHERE u.userId = ?";
      sql += "\r\n     AND ((u.apiCode is null OR u.apiCode = ?) AND (u.tenantCode is null OR u.tenantCode = ?))";
      vals.addAll(Arrays.asList(userId, apiCode, tenantCode));

      //-- user -> role
      sql += "\r\n                                                           ";
      sql += "\r\n    UNION";
      sql += "\r\n                                                           ";
      sql += "\r\n    SELECT 'role' as type, r.name, '' as via";
      sql += "\r\n    FROM Role r";
      sql += "\r\n    JOIN UserRole u ON r.id = u.roleId";
      sql += "\r\n    WHERE u.userId = ?";
      sql += "\r\n     AND ((u.apiCode is null OR u.apiCode = ?) AND (u.tenantCode is null OR u.tenantCode = ?))";
      vals.addAll(Arrays.asList(userId, apiCode, tenantCode));

      sql += " ) as q ORDER BY type, name, via";

      return JdbcUtils.selectRows(conn, sql, vals);
   }

}
