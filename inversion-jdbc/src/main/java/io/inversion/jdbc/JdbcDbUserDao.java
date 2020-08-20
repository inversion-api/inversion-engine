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
package io.inversion.jdbc;

import io.inversion.ApiException;
import io.inversion.User;
import io.inversion.action.security.AuthAction;
import io.inversion.action.security.AuthAction.JwtUserDao;
import io.inversion.utils.Config;
import io.inversion.utils.Rows;
import io.inversion.utils.Rows.Row;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.map.CaseInsensitiveMap;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Looks up a <code>User</code> from the configured <code>JdbcDb</code> supporting user/group/role permissioning.
 * <p>
 * Usage requires a password encryption "salt" value be
 * configured either explicitly or via a $name.salt
 * environment var or system prop.
 * <p>
 * In this model, Users, Groups, and Roles can all be
 * assigned Permissions.  Users can Groups can both
 * be assigned Roles and Users can be assigned to Groups.
 * This means users can be assigned permissions through
 * any one of the following relationship paths.
 * <p>
 * <ol>
 *  <li>user-to-permission
 *  <li>user-to-group-to-permission
 *  <li>user-to-role-to-permission
 *  <li>user-to-group-to-role-to-permission
 * </ol>
 * <p>
 * See users-h2.ddl for full underlying schema
 */
public class JdbcDbUserDao extends JwtUserDao {
    /**
     * Optional name param that is used for $name.salt
     * parameter configuration.
     */
    protected String name = null;
    protected JdbcDb db   = null;
    protected String salt = null;

    public JdbcDbUserDao() {

    }

    public JdbcDbUserDao(JdbcDb db) {
        withDb(db);
    }

    public static String strongHash(Object salt, String password) throws ApiException {
        try {
            int           iterationNb = 1000;
            MessageDigest digest      = MessageDigest.getInstance("SHA-512");
            digest.reset();
            digest.update(salt.toString().getBytes());
            byte[] input = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            for (int i = 0; i < iterationNb; i++) {
                digest.reset();
                input = digest.digest(input);
            }

            return Base64.encodeBase64String(input).trim();
        } catch (Exception ex) {
            throw new ApiException(ex);
        }
    }

    public static String weakHash(String password) {
        try {
            byte[]        byteArr = password.getBytes();
            MessageDigest digest  = MessageDigest.getInstance("MD5");
            digest.update(byteArr);
            byte[] bytes = digest.digest();
            return (new HexBinaryAdapter()).marshal(bytes);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    protected boolean checkPassword(String actual, String supplied) {
        String salt = getSalt();
        if (salt == null) {
            throw ApiException.new500InternalServerError("You must configure a salt value for password hashing.");
        }

        String strongHash = strongHash(salt, supplied);
        String weakHash   = weakHash(supplied);

        return actual.equals(strongHash) || actual.equals(weakHash);
    }

    public User getUser(AuthAction action, String username, String suppliedPassword, String apiName, String tenant) throws ApiException {
        User user = null;
        try {
            String sql    = "";

            if (username != null) {
                sql += " SELECT DISTINCT u.*";
                sql += " FROM User u   ";
                sql += " WHERE (u.revoked IS NULL OR u.revoked != 1) ";
                sql += " AND u.username = ? ";
                sql += " LIMIT 1 ";
            }

            //this connection is managed by JdbcConnectionLocal
            Connection conn = db.getConnection();

            Row userRow = JdbcUtils.selectRow(conn, sql, username);
            if (userRow != null) {
                CaseInsensitiveMap<String, Object> map = new CaseInsensitiveMap<>(userRow);

                String actualPassword = (String) map.get("password");
                if (checkPassword(actualPassword, suppliedPassword)) {
                    user = new User();
                    user.withId(Integer.parseInt(map.get("id") + ""))//
                            .withUsername((String) map.get("username"))//
                            .withAccessKey((String) map.get("accessKey"))//
                            .withTenant((String) map.get("tenant"));//
                }
                if (user != null) {
                    Rows rows = findGRP(conn, user.getId(), apiName, tenant);
                    if (rows == null || rows.size() == 0) {
                        //-- there is a users with the given username but the don't have any association to this apiName/tenant
                        user = null;
                    } else {
                        populateGRP(user, rows);
                    }
                }
            }
        } catch (Exception ex) {
            throw ApiException.new500InternalServerError(ex);
        }

        return user;
    }

    void populateGRP(User user, Rows rows) {
        for (Row row : rows) {
            String type = row.getString("type");
            String name = row.getString("name");
            if (name != null) {
                switch (type) {
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
     * user -@gt; permission
     * user -@gt; group -@gt; permission
     * user -@gt; role -@gt; permission
     * user -@gt; group -@gt; role -@gt; permission
     * <p>
     * UserPermission
     * GroupPermission
     * RolePermission
     * UserGroup
     * UserRole
     * GroupRole
     */
    protected Rows findGRP(Connection conn, int userId, String api, String tenant) throws Exception {
        List<Object> values = new ArrayList<>();

        String sql = "";

        sql += "SELECT * FROM (";

        //-- user -> permission
        sql += "\r\n    SELECT 'permission' as type, p.name, 'user->permission' as via";
        sql += "\r\n    FROM Permission p";
        sql += "\r\n    JOIN UserPermission u ON p.id = u.permissionId";
        sql += "\r\n    WHERE u.userId = ?";
        values.add(userId);

        sql += "\r\n     AND ((p.api is null OR p.api = ?) AND (p.tenant is null OR p.tenant = ?))";
        sql += "\r\n     AND ((u.api is null OR u.api = ?) AND (u.tenant is null OR u.tenant = ?))";
        values.addAll(Arrays.asList(api, tenant, api, tenant));

        //-- user -> group -> permission
        sql += "\r\n                                                           ";
        sql += "\r\n    UNION";
        sql += "\r\n                                                           ";
        sql += "\r\n    SELECT 'permission' as type, p.name, 'user->group->permission' as via";
        sql += "\r\n    FROM Permission p";
        sql += "\r\n    JOIN GroupPermission g ON p.id = g.permissionId";
        sql += "\r\n    JOIN UserGroup u ON u.groupId = g.groupId ";
        sql += "\r\n    WHERE u.userId = ?";
        sql += "\r\n     AND ((p.api is null OR p.api = ?) AND (p.tenant is null OR p.tenant = ?))";
        sql += "\r\n     AND ((g.api is null OR g.api = ?) AND (g.tenant is null OR g.tenant = ?))";
        sql += "\r\n     AND ((u.api is null OR u.api = ?) AND (u.tenant is null OR u.tenant = ?))";

        values.addAll(Arrays.asList(userId, api, tenant, api, tenant, api, tenant));

        //-- user -> role -> permission
        sql += "\r\n                                                           ";
        sql += "\r\n    UNION";
        sql += "\r\n                                                           ";
        sql += "\r\n    SELECT 'permission' as type, p.name, 'user->role->permission' as via";
        sql += "\r\n    FROM Permission p";
        sql += "\r\n    JOIN RolePermission r ON p.id = r.permissionId";
        sql += "\r\n    JOIN UserRole u ON u.roleId = r.roleId ";
        sql += "\r\n    WHERE u.userId = ?";
        sql += "\r\n     AND ((p.api is null OR p.api = ?) AND (p.tenant is null OR p.tenant = ?))";
        sql += "\r\n     AND ((r.api is null OR r.api = ?) AND (r.tenant is null OR r.tenant = ?))";
        sql += "\r\n     AND ((u.api is null OR u.api = ?) AND (u.tenant is null OR u.tenant = ?))";

        values.addAll(Arrays.asList(userId, api, tenant, api, tenant, api, tenant));

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

        sql += "\r\n     AND ((p.api is null OR p.api = ?) AND (p.tenant is null OR p.tenant = ?))";
        sql += "\r\n     AND ((r.api is null OR r.api = ?) AND (r.tenant is null OR r.tenant = ?))";
        sql += "\r\n     AND ((g.api is null OR g.api = ?) AND (g.tenant is null OR g.tenant = ?))";
        sql += "\r\n     AND ((u.api is null OR u.api = ?) AND (u.tenant is null OR u.tenant = ?))";

        values.addAll(Arrays.asList(userId, api, tenant, api, tenant, api, tenant, api, tenant));

        //-- user -> group
        sql += "\r\n                                                           ";
        sql += "\r\n    UNION";
        sql += "\r\n                                                           ";
        sql += "\r\n    SELECT 'group' as type, g.name, '' as via";
        sql += "\r\n    FROM `Group` g";
        sql += "\r\n    JOIN UserGroup u ON g.id = u.groupId";
        sql += "\r\n    WHERE u.userId = ?";
        sql += "\r\n     AND ((u.api is null OR u.api = ?) AND (u.tenant is null OR u.tenant = ?))";
        values.addAll(Arrays.asList(userId, api, tenant));

        //-- user -> role
        sql += "\r\n                                                           ";
        sql += "\r\n    UNION";
        sql += "\r\n                                                           ";
        sql += "\r\n    SELECT 'role' as type, r.name, '' as via";
        sql += "\r\n    FROM Role r";
        sql += "\r\n    JOIN UserRole u ON r.id = u.roleId";
        sql += "\r\n    WHERE u.userId = ?";
        sql += "\r\n     AND ((u.api is null OR u.api = ?) AND (u.tenant is null OR u.tenant = ?))";
        values.addAll(Arrays.asList(userId, api, tenant));

        sql += " ) as q ORDER BY type, name, via";

        System.out.println(sql + " -> " + values);
        return JdbcUtils.selectRows(conn, sql, values);
    }

    public JdbcDbUserDao withDb(JdbcDb db) {
        this.db = db;
        return this;
    }

    public JdbcDb getDb() {
        return db;
    }

    public void setDb(JdbcDb db) {
        this.db = db;
    }

    public JdbcDbUserDao withSalt(String salt) {
        this.salt = salt;
        return this;
    }

    public String getSalt() {
        return Config.getString(getName() + ".salt", salt);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
