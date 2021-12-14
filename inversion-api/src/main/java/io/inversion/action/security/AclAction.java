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
public class AclAction extends Action<AclAction> {
    protected final List<AclRule> aclRules = new ArrayList<>();

    public AclAction orRequireAllPerms(String ruleMatcherSpec, String permission1, String... permissionsN) {
        withAclRules(AclRule.requireAllPerms(ruleMatcherSpec, permission1, permissionsN));
        return this;
    }

    public AclAction orRequireOnePerm(String ruleMatcherSpec, String permission1, String... permissionsN) {
        withAclRules(AclRule.requireOnePerm(ruleMatcherSpec, permission1, permissionsN));
        return this;
    }

    public AclAction orRequireAllRoles(String ruleMatcherSpec, String role1, String... rolesN) {
        withAclRules(AclRule.requireAllRoles(ruleMatcherSpec, role1, rolesN));
        return this;
    }

    public AclAction orRequireOneRole(String ruleMatcherSpec, String role1, String... rolesN) {
        withAclRules(AclRule.requireOneRole(ruleMatcherSpec, role1, rolesN));
        return this;
    }

    public AclAction withAclRules(AclRule... acls) {
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
