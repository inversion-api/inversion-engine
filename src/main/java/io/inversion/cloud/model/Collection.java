/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
 * https://github.com/inversion-api
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
package io.inversion.cloud.model;

import java.util.ArrayList;
import java.util.List;

public class Collection extends Rule<Collection>
{
   protected Entity       entity  = null;
   protected List<String> aliases = new ArrayList();
   protected boolean      exclude = false;

   public Collection()
   {

   }

   public Collection(Api api, Table table, String name)
   {
      withName(name);
      withApi(api);
      withTable(table);
   }

   public Collection(Table table)
   {
      withTable(table);
   }

   public boolean isMethod(String... methods)
   {
      return this.methods.size() == 0 || super.isMethod(methods);
   }

   public boolean matches(String method, Path path)
   {
      if (exclude)
         return false;

      return super.matches(method, path);
   }

   public Collection withTable(Table table)
   {
      entity = new Entity(this, table);
      if (name == null)
      {
         name = table.getName();
      }
      return this;
   }

   public boolean isExclude()
   {
      return exclude || entity.isExclude();
   }

   public Collection withExclude(boolean exclude)
   {
      this.exclude = exclude;
      return this;
   }

   public Attribute getAttribute(String name)
   {
      return entity.getAttribute(name);
   }

   public Relationship getRelationship(String name)
   {
      return entity.getRelationship(name);
   }

   public String getAttributeName(String columnName)
   {
      for (Attribute attr : entity.getAttributes())
      {
         if (attr.getColumn().getName().equalsIgnoreCase(columnName))
            return attr.getName();
      }
      return columnName;
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
   public Collection withApi(Api api)
   {
      if (api != null && this.api != api)
      {
         this.api = api;
         api.withCollection(this);
      }
      return this;
   }

   /**
    * @return the entity
    */
   public Entity getEntity()
   {
      return entity;
   }

   public Db getDb()
   {
      return getEntity().getTable().getDb();
   }

   public Table getTable()
   {
      return getEntity().getTable();
   }

   /**
    * @param entity the entity to set
    */
   public Collection withEntity(Entity entity)
   {
      this.entity = entity;
      return this;
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
   public Collection withName(String name)
   {
      //System.out.println("Collection.withName(" + name + ")");
      this.name = name;
      return this;
   }

   public List<String> getAliases()
   {
      return new ArrayList(aliases);
   }

   public Collection withAliases(List<String> aliases)
   {
      this.aliases.clear();
      for (String alias : aliases)
         withAlias(alias);
      return this;
   }

   public Collection withAlias(String alias)
   {
      if (!aliases.contains(alias))
         aliases.add(alias);
      return this;
   }

}
