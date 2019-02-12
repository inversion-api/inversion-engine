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

public class Role
{
   //   public static String GUEST  = "guest";
   //   public static String MEMBER = "member";
   //   public static String ADMIN  = "admin";
   //   public static String OWNER  = "owner";

   String name  = null;
   int    level = -1;

   public Role()
   {

   }

   public Role(String role)
   {
      this.name = role;
   }

   public String getName()
   {
      return name;
   }

   public void setName(String role)
   {
      this.name = role.toLowerCase();
   }

   public int getLevel()
   {
      return level;
   }

   public void setLevel(int level)
   {
      this.level = level;
   }

   public int hashCode()
   {
      return name == null ? "null".hashCode() : name.hashCode();
   }

   public boolean equals(Object o)
   {
      return o == this || hashCode() == o.hashCode();
   }
}
