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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.rocketpartners.cloud.api;

import java.util.ArrayList;
import java.util.List;

public class User
{
   int              id          = 0;
   String           username    = null;
   String           password    = null;
   
   String           displayName    = null;

   List<Permission> permissions = new ArrayList();
   List<Role>       roles       = new ArrayList();

   String           accessKey   = null;
   String           secretKey   = null;

   int              tenantId    = 0;
   String           tenantCode  = null;

   /**
    * the time of the last request
    */
   long             requestAt   = -1;
   /**
    * the remote host of the last request
    */
   String           remoteAddr  = null;

   /**
    * the number of consecutive failed logins
    */
   int              failedNum   = 0;

   public User()
   {

   }

   public User(String username, java.util.Collection<Role> roles, java.util.Collection<Permission> perms)
   {
      this.username = username;
      setRoles(roles);
      setPermissions(permissions);
   }

   public String getUsername()
   {
      return username;
   }

   public void setUsername(String username)
   {
      this.username = username;
   }

   public int getId()
   {
      return id;
   }

   public void setId(int id)
   {
      this.id = id;
   }

   public long getRequestAt()
   {
      return requestAt;
   }

   public void setRequestAt(long requestAt)
   {
      this.requestAt = requestAt;
   }

   public String getRemoteAddr()
   {
      return remoteAddr;
   }

   public void setRemoteAddr(String remoteAddr)
   {
      this.remoteAddr = remoteAddr;
   }

   public int getFailedNum()
   {
      return failedNum;
   }

   public void setFailedNum(int failedNum)
   {
      this.failedNum = failedNum;
   }

   public String getAccessKey()
   {
      return accessKey;
   }

   public void setAccessKey(String accessKey)
   {
      this.accessKey = accessKey;
   }

   public String getSecretKey()
   {
      return secretKey;
   }

   public void setSecretKey(String secretKey)
   {
      this.secretKey = secretKey;
   }

   public String getPassword()
   {
      return password;
   }

   public void setPassword(String password)
   {
      this.password = password;
   }

   public int getTenantId()
   {
      return tenantId;
   }

   public void setTenantId(int tenantId)
   {
      this.tenantId = tenantId;
   }

   public List<Permission> getPermissions()
   {
      return permissions;
   }

   public void setPermissions(java.util.Collection<Permission> permissions)
   {
      this.permissions.clear();
      if (permissions != null)
         this.permissions.addAll(permissions);
   }

   public List<Role> getRoles()
   {
      return roles;
   }

   public void setRoles(java.util.Collection<Role> roles)
   {
      this.roles.clear();
      if (roles != null)
         this.roles.addAll(roles);
   }

   public String getTenantCode()
   {
      return tenantCode;
   }

   public void setTenantCode(String tenantCode)
   {
      this.tenantCode = tenantCode;
   }

   public String getDisplayName()
   {
      return displayName;
   }

   public void setDisplayName(String displayName)
   {
      this.displayName = displayName;
   }
   
   

}
