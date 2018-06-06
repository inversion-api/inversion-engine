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

public class Permission extends Dto
{
   long   apiId = 0;
   String name  = null;

   public String getName()
   {
      return name;
   }
   
   public String toString()
   {
      return "Permission " + getId() + " - " + getName();
   }

   public void setName(String permission)
   {
      this.name = permission.toLowerCase();
   }

   public long getApiId()
   {
      return apiId;
   }

   public void setApiId(long apiId)
   {
      this.apiId = apiId;
   }

   public int hashCode()
   {
      return (apiId + "_" + name).hashCode();
   }

   public boolean equals(Object o)
   {
      return o == this || hashCode() == o.hashCode();
   }

}
