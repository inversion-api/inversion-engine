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
package io.rocketpartners.cloud.api;

import java.util.ArrayList;
import java.util.List;

import io.rocketpartners.cloud.api.db.Relationship;

public class Collection extends Dto
{
   Api          api     = null;
   Entity       entity  = null;
   String       name    = null;
   List<String> aliases = new ArrayList();

   boolean      exclude = false;

   public boolean isExclude()
   {
      return exclude || entity.isExclude();
   }

   public void setExclude(boolean exclude)
   {
      this.exclude = exclude;
   }

   public Attribute getAttribute(String name)
   {
      return entity.getAttribute(name);
   }

   public Relationship getRelationship(String name)
   {
      return entity.getRelationship(name);
   }

   public String toString()
   {
      return name;
   }

   /**
    * @return the api
    */
   public Api getApi()
   {
      return api;
   }

   /**
    * Bi-directional method that also adds 'this' collection to the api assuming
    * the entity and it's table have been set. 
    * @param api the api to set
    */
   public void setApi(Api api)
   {
      if(api != null && this.api != api)
      {
         this.api = api;
         api.addCollection(this);
      }
   }

   /**
    * @return the entity
    */
   public Entity getEntity()
   {
      return entity;
   }

   /**
    * @param entity the entity to set
    */
   public void setEntity(Entity entity)
   {
      this.entity = entity;
   }

   /**
    * @return the name
    */
   public String getName()
   {
      return name;
   }

   /**
    * @param name the name to set
    */
   public void setName(String name)
   {
      this.name = name;
   }

   public List<String> getAliases()
   {
      return new ArrayList(aliases);
   }

   public void setAliases(List<String> aliases)
   {
      this.aliases.clear();
      for (String alias : aliases)
         addAlias(alias);
   }

   public void addAlias(String alias)
   {
      if (!aliases.contains(alias))
         aliases.add(alias);
   }

}
