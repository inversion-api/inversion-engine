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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package io.rocketpartners.cloud.model;

import java.util.ArrayList;
import java.util.List;

import io.rocketpartners.cloud.utils.Utils;

public class AclRule extends Rule<AclRule>
{
   protected boolean          allow       = true;
   protected boolean          info        = false;

   protected List<Permission> permissions = new ArrayList();
   protected List<Role>       roles       = new ArrayList();

   protected List<String>     restricts   = new ArrayList();
   protected List<String>     requires    = new ArrayList();

   public AclRule()
   {
      super();
   }

   public AclRule(String... methods)
   {
      super();
      withMethods(methods);
   }

   public boolean ruleMatches(Request req)
   {
      if (!matches(req.getMethod(), req.getPath()))
         return false;

      //short cut 
      if (req.getUser() == null && (roles.size() > 0 || permissions.size() > 0))
         return false;

      boolean hasRole = false;
      boolean hasPerms = false;

      if (roles.size() == 0)
      {
         hasRole = true;
      }
      else
      {
         for (Role requiredRole : getRoles())
         {
            for (Role userRole : req.getUser().getRoles())
            {
               if (userRole.getLevel() >= requiredRole.getLevel())
               {
                  hasRole = true;
                  break;
               }
            }
            if (hasRole)
               break;
         }
      }

      if (permissions.size() == 0)
      {
         hasPerms = true;
      }
      else
      {
         List rest = new ArrayList(permissions);
         rest.removeAll(req.getUser().getPermissions());
         hasPerms = (rest.size() == 0);
      }

      return hasRole && hasPerms;
   }

   @Override
   public AclRule withApi(Api api)
   {
      if (this.api != api)
      {
         this.api = api;
         api.addAclRule(this);
      }
      return this;
   }

   public ArrayList<Permission> getPermissions()
   {
      return new ArrayList(permissions);
   }

   //   public void setPermissions(List<Permission> permissions)
   //   {
   //      this.permissions.clear();
   //      for (Permission permission : permissions)
   //         addPermission(permission);
   //   }

   public AclRule withPermissions(String permissions)
   {
      this.permissions.clear();
      for (String permission : Utils.explode(",", permissions))
         withPermission(permission);

      return this;
   }

   public AclRule withPermission(Permission permission)
   {
      if (!permissions.contains(permission))
         permissions.add(permission);

      return this;
   }

   public AclRule withPermission(String permission)
   {
      withPermission(new Permission(permission));
      return this;
   }

   public ArrayList<Role> getRoles()
   {
      return new ArrayList(roles);
   }

   public AclRule withRoles(List<Role> roles)
   {
      for (Role role : roles)
         withRole(role);

      return this;
   }

   public AclRule withRole(Role role)
   {
      if (!roles.contains(role))
         roles.add(role);

      return this;
   }

   public AclRule withRestricts(List<String> restricts)
   {
      for (String restrict : restricts)
         withRestrict(restrict);

      return this;
   }

   public AclRule withRestricts(String... restricts)
   {
      for (String restrict : Utils.explode(",", restricts))
         withRestrict(restrict);

      return this;
   }

   public AclRule withRestrict(String restrict)
   {
      if (!restricts.contains(restrict))
         restricts.add(restrict);

      return this;
   }

   public AclRule withRequires(List<String> requires)
   {
      this.requires.clear();
      for (String require : requires)
      {
         withRequire(require);
      }
      return this;
   }

   public AclRule withRequires(String... requires)
   {
      for (String require : Utils.explode(",", requires))
         withRequire(require);

      return this;
   }

   public AclRule withRequire(String require)
   {
      requires.add(require);

      return this;
   }

   public List<String> getRestricts()
   {
      return new ArrayList(restricts);
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
      if (name != null)
      {
         return super.toString();
      }

      return System.identityHashCode(this) + " - " + permissions + " - " + includePaths + " - " + methods;
   }

}
