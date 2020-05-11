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
package io.inversion;

import io.inversion.utils.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class User
{
   protected int         id          = 0;
   protected String      tenant      = null;
   protected String      username    = null;
   protected String      password    = null;

   protected String      displayName = null;

   protected Set<String> groups      = new HashSet();
   protected Set<String> roles       = new HashSet();
   protected Set<String> permissions = new HashSet();

   protected String      accessKey   = null;
   protected String      secretKey   = null;

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

   public List<String> getPermissions()
   {
      return new ArrayList(permissions);
   }

   public boolean hasPermissions(String... permissions)
   {
      if (permissions == null)
         return true;

      for (String permission : Utils.explode(",", permissions))
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
         for (String permission : Utils.explode(",", permissions))
         {
            if (!this.permissions.contains(permission))
               this.permissions.add(permission);
         }
      }

      return this;
   }

   public boolean hasGroups(String... groups)
   {
      if (groups == null)
         return true;

      for (String group : groups)
      {
         if (!this.groups.contains(group))
            return false;
      }
      return true;
   }

   public List<String> getGroups()
   {
      return new ArrayList(groups);
   }

   public User withGroups(String... groups)
   {
      if (groups != null)
      {
         for (String group : Utils.explode(",", groups))
         {
            if (!this.groups.contains(group))
               this.groups.add(group);
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

   public String getTenant()
   {
      return tenant;
   }

   public User withTenant(String tenant)
   {
      this.tenant = tenant;
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
