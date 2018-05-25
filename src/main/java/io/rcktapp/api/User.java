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
package io.rcktapp.api;

import java.util.Set;

import io.forty11.utils.CaseInsensitiveSet;

public class User
{
   int                        id         = 0;
   String                     username   = null;
   String                     password   = null;
   CaseInsensitiveSet<String> perms      = new CaseInsensitiveSet<String>();
   CaseInsensitiveSet<String> roles      = new CaseInsensitiveSet<String>();

   String                     accessKey  = null;
   String                     secretKey  = null;

   /**
    * the time of the last request
    */
   long                       requestAt  = -1;
   /**
    * the remote host of the last request
    */
   String                     remoteAddr = null;

   /**
    * the number of consecutive failed logins
    */
   int                        failedNum  = 0;

   public User()
   {
      roles.add(Role.MEMBER);
   }

   public User(String username, java.util.Collection<String> roles, java.util.Collection<String> perms)
   {
      this.username = username;
      if (perms != null)
         this.perms.addAll(perms);

      if (roles != null)
         this.roles.add(roles);
   }

   public String getUsername()
   {
      return username;
   }

   public void setUsername(String username)
   {
      this.username = username;
   }

   public Set<String> getPerms()
   {
      return perms;
   }

   public void setPerms(java.util.Collection<String> perms)
   {
      this.perms.clear();
      this.perms.addAll(perms);
   }

   public boolean hasPerm(String perm)
   {
      if (perm == null)
         return false;

      return perms.contains(perm);
   }

   public Set<String> getRoles()
   {
      return roles;
   }

   public void setRoles(java.util.Collection<String> roles)
   {
      this.roles.clear();
      this.roles.addAll(roles);
   }

   public boolean hasRole(String role)
   {
      if (role == null)
         return false;

      return roles.contains(role);
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

}
