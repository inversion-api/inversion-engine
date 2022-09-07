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
package io.inversion;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.inversion.utils.Utils;

import java.lang.reflect.Field;
import java.util.*;

public class User {

    protected String    id       = null;
    //    protected String tenant      = null;
    protected String username = null;
//    protected String password    = null;
//    protected String displayName = null;
//    protected String accessKey   = null;
//    protected String secretKey   = null;

    protected final Set<String> groups      = new HashSet();
    protected final Set<String> roles       = new HashSet();
    protected final Set<String> permissions = new HashSet();
    protected final Set<String> scopes      = new HashSet();

    protected final Map<String, Object> properties = new HashMap();


//    /**
//     * the time of the last request
//     */
//    protected long   requestAt  = -1;
//    /**
//     * the remote host of the last request
//     */
//    protected String remoteAddr = null;
//
//    /**
//     * the number of consecutive failed logins
//     */
//    protected int failedNum = 0;

    public User() {

    }

    public User(String username, String roles, String permissions) {
        withUsername(username);
        withRoles(roles);
        withPermissions(permissions);
    }

    @JsonAnySetter
    public User withProperty(String name, Object value) {

        String lc = name.toLowerCase();
        if (value instanceof List && Utils.in(name, "group", "role", "permission", "scope", "groups", "roles", "permissions", "scopes")) {
            List list = (List) value;
            for (Object listVal : list) {
                withProperty(name, listVal);
            }
            return this;
        }

        switch (lc) {
            case "group":
            case "groups":
                withGroups((String) value);
                return this;
            case "role":
            case "roles":
                withRoles((String) value);
                return this;
            case "permission":
            case "permissions":
                withPermissions((String) value);
                return this;
            case "scope":
            case "scopes":
                withScopes((String) value);
                return this;
        }

        Field f = Utils.getField(name, this.getClass());
        if (f != null) {
            try {
                f.set(this, value);
                return this;
            } catch (Exception ex) {
                Utils.rethrow(ex);
            }
        }
        properties.put(name, value);
        return this;
    }

    @JsonAnyGetter
    public Map<String, Object> getProperties() {
        return properties;
    }

    public Object getProperty(String name) {
        Field f = Utils.getField(name, this.getClass());
        if (f != null) {
            try {
                return f.get(this);
            } catch (Exception ex) {
                Utils.rethrow(ex);
            }
        }
        return properties.get(name);
    }


    public String getUsername() {
        return username;
    }

    public User withUsername(String username) {
        this.username = username;
        return this;
    }

    public String getId() {
        return id;
    }

    public User withId(String id) {
        this.id = id;
        return this;
    }

//    public long getRequestAt() {
//        return requestAt;
//    }
//
//    public User withRequestAt(long requestAt) {
//        this.requestAt = requestAt;
//        return this;
//    }
//
//    public String getRemoteAddr() {
//        return remoteAddr;
//    }
//
//    public User withRemoteAddr(String remoteAddr) {
//        this.remoteAddr = remoteAddr;
//        return this;
//    }
//
//    public int getFailedNum() {
//        return failedNum;
//    }
//
//    public User withFailedNum(int failedNum) {
//        this.failedNum = failedNum;
//        return this;
//    }

//    public String getAccessKey() {
//        return accessKey;
//    }
//
//    public User withAccessKey(String accessKey) {
//        this.accessKey = accessKey;
//        return this;
//    }
//
//    public String getSecretKey() {
//        return secretKey;
//    }
//
//    public User withSecretKey(String secretKey) {
//        this.secretKey = secretKey;
//        return this;
//    }
//
//    public String getPassword() {
//        return password;
//    }
//
//    public User withPassword(String password) {
//        this.password = password;
//        return this;
//    }

    public List<String> getPermissions() {
        return new ArrayList(permissions);
    }

    public boolean hasPermissions(String... permissions) {
        if (permissions == null)
            return true;

        for (String permission : Utils.explode(",", permissions)) {
            if (!this.permissions.contains(permission))
                return false;
        }
        return true;
    }

    public User withPermissions(String... permissions) {
        if (permissions != null) {
            this.permissions.addAll(Utils.explode(",", permissions));
        }

        return this;
    }

    public boolean hasGroups(String... groups) {
        if (groups == null)
            return true;

        for (String group : groups) {
            if (!this.groups.contains(group))
                return false;
        }
        return true;
    }

    public List<String> getGroups() {
        return new ArrayList(groups);
    }

    public User withGroups(String... groups) {
        if (groups != null) {
            this.groups.addAll(Utils.explode(",", groups));
        }

        return this;
    }

    public Set<String> getRoles() {
        return new HashSet(roles);
    }

    public boolean hasRoles(String... roles) {
        if (roles == null)
            return true;

        for (String role : roles) {
            if (!this.roles.contains(role))
                return false;
        }
        return true;
    }

    public User withRoles(String... roles) {
        if (roles != null) {
            Collections.addAll(this.roles, roles);
        }

        return this;
    }

//    public String getTenant() {
//        return tenant;
//    }
//
//    public User withTenant(String tenant) {
//        this.tenant = tenant;
//        return this;
//    }

//    public String getDisplayName() {
//        return displayName;
//    }
//
//    public User withDisplayName(String displayName) {
//        this.displayName = displayName;
//        return this;
//    }


    public List<String> getScopes() {
        return new ArrayList(scopes);
    }

    public boolean hasScope(String... scopes) {
        if (scopes == null)
            return true;

        for (String permission : Utils.explode(",", scopes)) {
            if (!this.scopes.contains(scopes))
                return false;
        }
        return true;
    }

    public User withScopes(String... scopes) {
        if (scopes != null) {
            this.scopes.addAll(Utils.explode(",", scopes));
        }

        return this;
    }


}
