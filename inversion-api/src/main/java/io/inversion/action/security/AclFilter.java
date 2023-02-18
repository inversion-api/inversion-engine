/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
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
package io.inversion.action.security;

import io.inversion.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The AclAction secures an API by making sure that a requests matches one or
 * more declared AclRules
 * <p>
 * AclRules specify the roles and permissions that a user must have to access
 * specific method/path combinations and can also specify input/output
 * parameters that are either required or restricted
 */
public class AclFilter extends Filter<AclFilter> {
    protected final List<AclRule> aclRules = new ArrayList<>();

    public AclFilter orRequireAllPerms(String permissions, String... includedOn) {
        withAclRules(AclRule.requireAllPerms(permissions, includedOn));
        return this;
    }

    public AclFilter orRequireOnePerm(String permissions, String... includedOn) {
        withAclRules(AclRule.requireOnePerm(permissions, includedOn));
        return this;
    }

    public AclFilter orRequireAllRoles(String roles, String... includedOn) {
        withAclRules(AclRule.requireAllRoles(roles, includedOn));
        return this;
    }

    public AclFilter orRequireOneRole(String roles, String... includedOn) {
        withAclRules(AclRule.requireOneRole(roles, includedOn));
        return this;
    }

    public AclFilter orRequireAllScopes(String scopes, String... includedOn) {
        withAclRules(AclRule.requireAllRoles(scopes, includedOn));
        return this;
    }

    public AclFilter orRequireOneScope(String scopes, String... includedOn) {
        withAclRules(AclRule.requireOneRole(scopes, includedOn));
        return this;
    }

    public AclFilter withAclRules(AclRule... acls) {
        for (AclRule acl : acls) {
            if (!aclRules.contains(acl)) {
                aclRules.add(acl);
            }
        }

        Collections.sort(aclRules);
        return this;
    }

    public List<AclRule> getAclRules(){
        return new ArrayList<>(aclRules);
    }

    @Override
    public void run(Request req, Response resp) throws ApiException {

        log.debug("Request Path: " + req.getUrl().getPath());
        boolean allowed = false;

        for (AclRule aclRule : aclRules) {
            if (aclRule.ruleMatches(req)) {
                //log.debug("Matched AclAction: " + aclRule.getName());
                if (!aclRule.isAllow()) {
                    Chain.debug("AclAction: MATCH_DENY" + aclRule);

                    allowed = false;
                    break;
                } else {
                    if (!aclRule.isInfo() && aclRule.isAllow()) {
                        Chain.debug("AclAction: MATCH_ALLOW " + aclRule);
                        allowed = true;
                        break;
                    } else {
                        Chain.debug("AclAction: MATCH_INFO " + aclRule);
                    }
                }
            }
        }

        if (!allowed) {
            Chain.debug("AclAction: NO_MATCH_DENY");
            throw ApiException.new403Forbidden();
        }
    }
}
