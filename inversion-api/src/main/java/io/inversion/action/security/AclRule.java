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
package io.inversion.action.security;

import java.util.ArrayList;
import java.util.List;

import io.inversion.Chain;
import io.inversion.Request;
import io.inversion.Rule;
import io.inversion.utils.Utils;

public class AclRule extends Rule<AclRule>
{
   protected boolean allow = true;
   protected boolean info  = false;

   protected List<String> permissions = new ArrayList();
   protected List<String> roles       = new ArrayList();

   protected List<String> restricts               = new ArrayList();
   protected List<String> requires                = new ArrayList();

   protected boolean      allRolesMustMatch       = false;
   protected boolean      allPermissionsMustMatch = false;

   public static AclRule allowAll(String methods, String includePaths)
   {
      AclRule rule = new AclRule(null, methods, includePaths, null);
      return rule;
   }

   public static AclRule requireAllPerms(String methods, String includePaths, String permission1, String... permissionsN)
   {
      AclRule rule = new AclRule(null, null, includePaths, permission1, permissionsN);
      rule.withAllPermissionsMustMatch(true);
      return rule;
   }

   public static AclRule requireOnePerm(String methods, String includePaths, String permission1, String... permissionsN)
   {
      AclRule rule = new AclRule(null, null, includePaths, permission1, permissionsN);
      rule.withAllPermissionsMustMatch(false);
      return rule;
   }

   public static AclRule requireAllRoles(String methods, String includePaths, String role1, String... rolesN)
   {
      AclRule rule = new AclRule(null, null, includePaths, null);
      rule.withAllRolesMustMatch(true);
      rule.withRoles(role1);
      rule.withRoles(rolesN);
      return rule;
   }

   public static AclRule requireOneRole(String methods, String includePaths, String role1, String... rolesN)
   {
      AclRule rule = new AclRule(null, null, includePaths, null);
      rule.withAllRolesMustMatch(false);
      rule.withRoles(role1);
      rule.withRoles(rolesN);
      return rule;
   }

   public AclRule()
   {
      super();
   }

   public AclRule(String name, String methods, String includePaths, String permission1, String... permissionsN)
   {
      withName(name);
      withMethods(methods);
      withIncludePaths(includePaths);

      if (permission1 != null)
         withPermissions(permission1);

      if (permissionsN != null)
         withPermissions(permissionsN);
   }

   public boolean ruleMatches(Request req)
   {
      if (match(req.getMethod(), req.getPath()) == null)
         return false;

      //short cut 
      if (Chain.getUser() == null && (roles.size() > 0 || permissions.size() > 0))
         return false;

      int matches = 0;
      for (String requiredRole : roles)
      {
         boolean matched = Chain.getUser().hasRoles(requiredRole);

         if (matched)
         {
            matches += 1;
            if (!allRolesMustMatch)
            {
               break;
            }
         }
         else
         {
            if (allRolesMustMatch)
            {
               break;
            }
         }
      }

      for (String requiredPerm : permissions)
      {
         boolean matched = Chain.getUser().hasPermissions(requiredPerm);

         if (matched)
         {
            matches += 1;
            if (!allPermissionsMustMatch)
            {
               break;
            }
         }
         else
         {
            if (allPermissionsMustMatch)
            {
               break;
            }
         }
      }

      boolean hasRoles = roles.size() == 0 //
            || (allRolesMustMatch && matches == roles.size())//
            || (!allRolesMustMatch && matches > 0);

      boolean hasPermissions = permissions.size() == 0 //
            || (allPermissionsMustMatch && matches == permissions.size())//
            || (!allPermissionsMustMatch && matches > 0);

      return hasRoles || hasPermissions;
   }

   public ArrayList<String> getRoles()
   {
      return new ArrayList(roles);
   }

   public AclRule withRoles(String... roles)
   {
      if (roles != null)
      {
         for (String role : Utils.explode(",", roles))
         {
            if (!this.roles.contains(role))
               this.roles.add(role);
         }
      }
      return this;
   }

   public ArrayList<String> getPermissions()
   {
      return new ArrayList(permissions);
   }

   public AclRule withPermissions(String... permissions)
   {
      if (permissions != null)
      {
         for (String permission : Utils.explode(",", permissions))
         {
            if (!this.permissions.contains(permission))
               this.permissions.add(permission);
         }
      }
      return this;
   }

   public AclRule withRestricts(String... restricts)
   {
      if (restricts != null)
      {
         for (String restrict : Utils.explode(",", restricts))
         {
            if (!this.restricts.contains(restrict))
               this.restricts.add(restrict);
         }
      }
      return this;
   }

   public List<String> getRestricts()
   {
      return new ArrayList(restricts);
   }

   public AclRule withRequires(String... requires)
   {
      if (requires != null)
      {
         for (String require : Utils.explode(",", requires))
         {
            if (!this.requires.contains(require))
               this.requires.add(require);
         }
      }
      return this;
   }

   public List<String> getRequires()
   {
      return new ArrayList(requires);
   }

   public boolean isAllow()
   {
      return allow;
   }

   public AclRule withAllow(boolean allow)
   {
      this.allow = allow;
      return this;
   }

   public boolean isInfo()
   {
      return info;
   }

   public AclRule withInfo(boolean info)
   {
      this.info = info;
      return this;
   }

   public boolean isAllRolesMustMatch()
   {
      return allRolesMustMatch;
   }

   public AclRule withAllRolesMustMatch(boolean allRolesMustMatch)
   {
      this.allRolesMustMatch = allRolesMustMatch;
      return this;
   }

   public boolean isAllPermissionsMustMatch()
   {
      return allPermissionsMustMatch;
   }

   public AclRule withAllPermissionsMustMatch(boolean allPermissionsMustMatch)
   {
      this.allPermissionsMustMatch = allPermissionsMustMatch;
      return this;
   }

}
