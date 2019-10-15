/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
 * https://github.com/inversion-api
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
package io.inversion.cloud.action.security;

import java.util.ArrayList;
import java.util.List;

import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Rule;
import io.inversion.cloud.utils.Utils;

public class AclRule extends Rule<AclRule>
{
   protected boolean      allow                   = true;
   protected boolean      info                    = false;

   protected List<String> permissions             = new ArrayList();
   protected List<String> roles                   = new ArrayList();

   protected List<String> restricts               = new ArrayList();
   protected List<String> requires                = new ArrayList();

   protected boolean      allRolesMustMatch       = true;
   protected boolean      allPermissionsMustMatch = true;

   public static AclRule allowAny(String name, String methods, String includePaths, String excludePaths)
   {
      AclRule rule = new AclRule(methods, includePaths, excludePaths, null);
      rule.withName(name);
      return rule;
   }

   public static AclRule allowIfUserHasAllPermissions(String name, String methods, String includePaths, String excludePaths, String... permissions)
   {
      AclRule rule = new AclRule(methods, includePaths, excludePaths, null);
      rule.withName(name);
      rule.withPermissions(permissions);
      return rule;
   }

   public static AclRule allowIfUserHasAnyPermissions(String name, String methods, String includePaths, String excludePaths, String... permissions)
   {
      AclRule rule = new AclRule(methods, includePaths, excludePaths, null);
      rule.withName(name);
      rule.withPermissions(permissions);
      return rule;
   }

   public AclRule()
   {
      super();
   }

   //   public AclRule(String methods)
   //   {
   //      super();
   //      withMethods(methods);
   //   }

   public AclRule(String name, String methods, String includePaths, String... permissions)
   {
      withName(name);
      withMethods(methods);
      withIncludePaths(includePaths);
      withPermissions(permissions);
   }

   //   public AclRule(String methods, String includePaths, String excludePaths, String config)
   //   {
   //      withMethods(methods);
   //      withIncludePaths(includePaths);
   //      withExcludePaths(excludePaths);
   //      withConfig(config);
   //   }

   @Override
   public AclRule withApi(Api api)
   {
      if (this.api != api)
      {
         this.api = api;
      }
      return (AclRule) this;
   }

   public boolean ruleMatches(Request req)
   {
      if (!matches(req.getMethod(), req.getPath()))
         return false;

      //short cut 
      if (req.getUser() == null && (roles.size() > 0 || permissions.size() > 0))
         return false;

      int matches = 0;
      for (String requiredRole : roles)
      {
         boolean matched = req.getUser().hasRoles(requiredRole);

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

      boolean hasRoles = roles.size() == 0 //
            || (allRolesMustMatch && matches == roles.size())//
            || (!allRolesMustMatch && matches > 0);

      if (hasRoles)//no need to check perms if does not have roles
      {
         for (String requiredPerm : permissions)
         {
            boolean matched = req.getUser().hasPermissions(requiredPerm);

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
      }

      boolean hasPermissions = permissions.size() == 0 //
            || (allPermissionsMustMatch && matches == permissions.size())//
            || (!allPermissionsMustMatch && matches > 0);

      return hasRoles && hasPermissions;
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

   @Override
   public String toString()
   {
      return name + " - methods:" + methods + " includesPaths:" + includePaths + " excludesPaths:" + excludePaths + " roles:" + roles + " permissions:" + permissions + " allow:" + allow + " info:" + info;
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
