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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class User
{
   protected int         id          = 0;
   protected String      username    = null;
   protected String      password    = null;

   protected String      displayName = null;

   protected Set<String> permissions = new HashSet();
   protected Set<String> roles       = new HashSet();

   protected String      accessKey   = null;
   protected String      secretKey   = null;

   protected int         tenantId    = 0;
   protected String      tenantCode  = null;

   /**
    * the time of the last request
    */
   protected long        requestAt   = -1;
   /**
    * the remote host of the last request
    */
   protected String      remoteAddr  = null;

   /**
    * the number of consecutive failed logins
    */
   protected int         failedNum   = 0;

   public User()
   {

   }

   public User(String username, String roles, String permissions)
   {
      withUsername(username);
      withRoles(roles);
      withPermissions(permissions);
   }

   public String getUsername()
   {
      return username;
   }

   public User withUsername(String username)
   {
      this.username = username;
      return this;
   }

   public int getId()
   {
      return id;
   }

   public User withId(int id)
   {
      this.id = id;
      return this;
   }

   public long getRequestAt()
   {
      return requestAt;
   }

   public User withRequestAt(long requestAt)
   {
      this.requestAt = requestAt;
      return this;
   }

   public String getRemoteAddr()
   {
      return remoteAddr;
   }

   public User withRemoteAddr(String remoteAddr)
   {
      this.remoteAddr = remoteAddr;
      return this;
   }

   public int getFailedNum()
   {
      return failedNum;
   }

   public User withFailedNum(int failedNum)
   {
      this.failedNum = failedNum;
      return this;
   }

   public String getAccessKey()
   {
      return accessKey;
   }

   public User withAccessKey(String accessKey)
   {
      this.accessKey = accessKey;
      return this;
   }

   public String getSecretKey()
   {
      return secretKey;
   }

   public User withSecretKey(String secretKey)
   {
      this.secretKey = secretKey;
      return this;
   }

   public String getPassword()
   {
      return password;
   }

   public User withPassword(String password)
   {
      this.password = password;
      return this;
   }

   public int getTenantId()
   {
      return tenantId;
   }

   public User withTenantId(int tenantId)
   {
      this.tenantId = tenantId;
      return this;
   }

   public List<String> getPermissions()
   {
      return new ArrayList(permissions);
   }

   public boolean hasPermissions(String... permissions)
   {
      if (permissions == null)
         return true;

      for (String permission : permissions)
      {
         if (!this.permissions.contains(permission))
            return false;
      }
      return true;
   }

   public User withPermissions(String... permissions)
   {
      if (permissions != null)
      {
         for (String permission : permissions)
         {
            if (!this.permissions.contains(permission))
               this.permissions.add(permission);
         }
      }

      return this;
   }

   public Set<String> getRoles()
   {
      return new HashSet(roles);
   }

   public boolean hasRoles(String... roles)
   {
      if (roles == null)
         return true;

      for (String role : roles)
      {
         if (!this.roles.contains(role))
            return false;
      }
      return true;
   }

   public User withRoles(String... roles)
   {
      if (roles != null)
      {
         for (String role : roles)
         {
            this.roles.add(role);
         }
      }

      return this;
   }

   public String getTenantCode()
   {
      return tenantCode;
   }

   public User withTenantCode(String tenantCode)
   {
      this.tenantCode = tenantCode;
      return this;
   }

   public String getDisplayName()
   {
      return displayName;
   }

   public User withDisplayName(String displayName)
   {
      this.displayName = displayName;
      return this;
   }

}
