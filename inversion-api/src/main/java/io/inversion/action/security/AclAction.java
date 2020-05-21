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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.inversion.Action;
import io.inversion.ApiException;
import io.inversion.Chain;
import io.inversion.Request;
import io.inversion.Response;
import io.inversion.utils.JSArray;
import io.inversion.utils.JSNode;
import io.inversion.utils.Utils;

/**
 * The AclAction secures an API by making sure that a requests matches one or 
 * more declared AclRules
 *
 * AclRules specify the roles and permissions that a user must have to access
 * specific method/path combinations and can also specify input/output
 * parameters that are either required or restricted
 *
 *
 *
 */
public class AclAction extends Action<AclAction>
{
   protected List<AclRule> aclRules = new ArrayList();

   public AclAction orRequireAllPerms(String httpMethods, String includePaths, String permission1, String... permissionsN)
   {
      withAclRules(AclRule.requireAllPerms(httpMethods, includePaths, permission1, permissionsN));
      return this;
   }

   public AclAction orRequireOnePerm(String httpMethods, String includePaths, String permission1, String... permissionsN)
   {
      withAclRules(AclRule.requireOnePerm(httpMethods, includePaths, permission1, permissionsN));
      return this;
   }

   public AclAction orRequireAllRoles(String httpMethods, String includePaths, String role1, String... rolesN)
   {
      withAclRules(AclRule.requireAllRoles(httpMethods, includePaths, role1, rolesN));
      return this;
   }

   public AclAction orRequireOneRole(String httpMethods, String includePaths, String role1, String... rolesN)
   {
      withAclRules(AclRule.requireOneRole(httpMethods, includePaths, role1, rolesN));
      return this;
   }

   public AclAction withAclRules(AclRule... acls)
   {
      for (AclRule acl : acls)
      {
         if (!aclRules.contains(acl))
         {
            aclRules.add(acl);
         }
      }

      Collections.sort(aclRules);
      return this;
   }

   @Override
   public void run(Request req, Response resp) throws ApiException
   {
      List<AclRule> matched = new ArrayList<>();
      boolean allowed = false;

      log.debug("Request Path: " + req.getPath());

      for (AclRule aclRule : aclRules)
      {
         if (aclRule.ruleMatches(req))
         {
            //log.debug("Matched AclAction: " + aclRule.getName());
            if (!aclRule.isAllow())
            {
               Chain.debug("AclAction: MATCH_DENY" + aclRule);

               allowed = false;
               break;
            }
            else
            {
               if (!aclRule.isInfo() && aclRule.isAllow())
               {
                  Chain.debug("AclAction: MATCH_ALLOW " + aclRule);
                  allowed = true;
               }
               else
               {
                  Chain.debug("AclAction: MATCH_INFO " + aclRule);
               }
            }

            matched.add(aclRule);
         }
      }

      if (!allowed)
      {
         Chain.debug("AclAction: NO_MATCH_DENY");
         ApiException.throw403Forbidden();
      }
   }
}
