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

public class Change
{
   String method        = null;
   String collectionKey = null;
   Object entityKey     = null;

   public Change(String method, String collectionKey, Object entityKey)
   {
      super();
      this.method = method;
      this.collectionKey = collectionKey;
      this.entityKey = entityKey;
   }

   public String getMethod()
   {
      return method;
   }

   public void setMethod(String method)
   {
      this.method = method;
   }

   public String getCollectionKey()
   {
      return collectionKey;
   }

   public void setCollectionKey(String collectionKey)
   {
      this.collectionKey = collectionKey;
   }

   public Object getEntityKey()
   {
      return entityKey;
   }

   public void setEntityKey(Object entityKey)
   {
      this.entityKey = entityKey;
   }

}
