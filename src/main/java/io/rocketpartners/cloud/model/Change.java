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
package io.rocketpartners.cloud.model;

public class Change
{
   protected String method        = null;
   protected String collectionKey = null;
   protected Object entityKey     = null;

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

   public Change withMethod(String method)
   {
      this.method = method;
      return this;
   }

   public String getCollectionKey()
   {
      return collectionKey;
   }

   public Change withCollectionKey(String collectionKey)
   {
      this.collectionKey = collectionKey;
      return this;
   }

   public Object getEntityKey()
   {
      return entityKey;
   }

   public Change withEntityKey(Object entityKey)
   {
      this.entityKey = entityKey;
      return this;
   }

}
