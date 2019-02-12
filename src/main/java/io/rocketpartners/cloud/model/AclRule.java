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

import io.rocketpartners.cloud.service.Request;
import io.rocketpartners.utils.J;

public class AclRule extends Rule
{
   boolean          allow       = true;
   boolean          info        = false;

   List<Permission> permissions = new ArrayList();
   List<Role>       roles       = new ArrayList();

   List<String>     restricts   = new ArrayList();
   List<String>     requires    = new ArrayList();

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
   public void setApi(Api api)
   {
      this.api = api;
      api.addAclRule(this);
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

   public void setPermissions(String permissions)
   {
      this.permissions.clear();
      for (String permission : J.explode(",", permissions))
         addPermission(permission);
   }

   public void addPermission(Permission permission)
   {
      if (!permissions.contains(permission))
         permissions.add(permission);
   }

   public void addPermission(String permission)
   {
      addPermission(new Permission(permission));
   }

   public ArrayList<Role> getRoles()
   {
      return new ArrayList(roles);
   }

   public void setRoles(List<Role> roles)
   {
      this.roles.clear();
      for (Role role : roles)
         addRole(role);
   }

   public void addRole(Role role)
   {
      if (!roles.contains(role))
         roles.add(role);
   }

   public void setRestricts(List<String> restricts)
   {
      this.restricts.clear();
      for (String restrict : restricts)
         addRestrict(restrict);
   }

   public void addRestrict(String restrict)
   {
      if (!restricts.contains(restrict))
         restricts.add(restrict);
   }

   public void setRequires(List<String> requires)
   {
      this.requires.clear();
      for (String require : requires)
      {
         addRequire(require);
      }
   }

   public void addRequire(String require)
   {
      requires.add(require);
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

   public void setAllow(boolean allow)
   {
      this.allow = allow;
   }

   public boolean isInfo()
   {
      return info;
   }

   public void setInfo(boolean info)
   {
      this.info = info;
   }

}
