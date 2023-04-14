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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.inversion.utils.Utils;

import java.lang.reflect.Field;
import java.util.*;

public class User {

    protected String issuer   = null;
    protected String account  = null;
    protected String subject = null;

    protected Set<String> audiences      = new LinkedHashSet();
    protected Set<String> groups      = new LinkedHashSet();
    protected Set<String> roles       = new LinkedHashSet();
    protected Set<String> permissions = new LinkedHashSet();
    protected Set<String> scopes      = new LinkedHashSet();


    protected final Map<String, Object> claims = new HashMap();


    public User() {

    }

    public User(String username, String roles, String permissions) {
        setSubject(username);
        withRoles(roles);
        withPermissions(permissions);
    }

    @JsonAnyGetter
    public Map<String, Object> getClaims() {
        return claims;
    }

    @JsonAnySetter
    public User withClaim(String name, Object value) {

        if("iss".equalsIgnoreCase(name))
            name = "issuer";
        else if("aud".equalsIgnoreCase(name))
            name = "audiences";
        else if("sub".equalsIgnoreCase(name))
            name = "subject";
        else if("scp".equalsIgnoreCase(name))
            name = "scopes";
        else if("rol".equalsIgnoreCase(name))
            name = "roles";
        else if("prm".equalsIgnoreCase(name))
            name = "permissions";

        String lc = name.toLowerCase();
        if (value instanceof List && Utils.in(name, "audience", "group", "role", "permission", "scope", "audiences", "groups", "roles", "permissions", "scopes")) {
            java.util.Collection list = (java.util.Collection) value;
            for (Object listVal : list) {
                withClaim(name, listVal);
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
            case "audiences":
                withAudiences((String) value);
                return this;
        }

        Field f = Utils.getField(name, this.getClass());
        if (f != null) {
            try {
                f.set(this, value.toString());
                return this;
            } catch (Exception ex) {
                Utils.rethrow(ex);
            }
        }
        claims.put(name, value);
        return this;
    }

    public Object getClaim(String name) {

        if("iss".equalsIgnoreCase(name))
            name = "issuer";
        else if("aud".equalsIgnoreCase(name))
            name = "audiences";
        else if("sub".equalsIgnoreCase(name))
            name = "subject";
        else if("scp".equalsIgnoreCase(name))
            name = "scopes";
        else if("rol".equalsIgnoreCase(name))
            name = "roles";
        else if("prm".equalsIgnoreCase(name))
            name = "permissions";

        Field f = Utils.getField(name, this.getClass());
        if (f != null) {
            try {
                Object claim =  f.get(this);
                if(claim instanceof Set)
                    claim = toString((Set)claim);
                return claim;
            } catch (Exception ex) {
                Utils.rethrow(ex);
            }
        }
        return claims.get(name);
    }


    @JsonProperty("iss")
    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    @JsonProperty("sub")
    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getAccount() {
        return account;
    }

    public User withAccount(String account) {
        this.account = account;
        return this;
    }

//-- Audience ----------------------------------------------------------------------------------------------------------

    @JsonProperty("aud")
    public String getAudiences() {
        return toString(audiences);
    }

    public void setAudiences(String audiences) {
        this.audiences = fromString(audiences);
    }

    public boolean hasAudience(String... audiences) {
        if (audiences == null)
            return true;

        for (String audience : Utils.explode(",", audiences)) {
            if (!this.audiences.contains(audience))
                return false;
        }
        return true;
    }

    public User withAudiences(String... audiences) {
        if (audiences != null) {
            this.audiences.addAll(Utils.explode(",", audiences));
        }

        return this;
    }



    //-- Permissions ----------------------------------------------------------------------------------------------------------

    @JsonProperty("prm")
    public String getPermissions() {
        return toString(permissions);
    }

    public void setPermissions(String permissions) {
        this.permissions = fromString(permissions);
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

    //-- Groups ----------------------------------------------------------------------------------------------------------

    @JsonIgnore
    public String getGroups() {
        return toString(groups);
    }

    public void setGroups(String groups) {
        this.groups = fromString(groups);
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

    public User withGroups(String... groups) {
        if (groups != null) {
            this.groups.addAll(Utils.explode(",", groups));
        }

        return this;
    }

    //-- Roles ----------------------------------------------------------------------------------------------------------

    @JsonProperty("rol")
    public String getRoles() {
        return toString(roles);
    }

    public void setRoles(String roles) {
        this.roles = fromString(roles);
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

    //-- Scopes ----------------------------------------------------------------------------------------------------------

    public String getScopes() {
        return toString(scopes);
    }

    @JsonProperty("scp")
    public void setScopes(String scopes) {
        this.scopes = fromString(scopes);
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


    //-- Encoding Utils ----------------------------------------------------------------------------------------------------------


    String toString(Set<String> c) {
        StringBuilder builder = new StringBuilder();
        c.forEach(str -> builder.append(str).append(" "));
        return builder.toString().trim();
    }

    LinkedHashSet<String> fromString(String str) {
        LinkedHashSet set = new LinkedHashSet<>();
        for (String part : str.split(" ")) {
            set.add(part.trim());
        }
        return set;
    }

}
