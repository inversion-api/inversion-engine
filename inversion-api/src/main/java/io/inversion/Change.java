/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion;

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
