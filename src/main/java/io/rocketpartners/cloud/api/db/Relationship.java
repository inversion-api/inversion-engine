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
package io.rocketpartners.cloud.api.db;

import io.rocketpartners.cloud.api.Entity;

//public class Relationship<C extends Column<Table<Db, Column<Table, Column>, Index>, Column<Table, Column>>> extends io.rocketpartners.db.Relationship<C>
//public class Relationship extends io.rocketpartners.db.Relationship<Column<Table,Column<Table, Column>>>
//public class Relationship extends io.rocketpartners.db.Relationship<Column<Table,Column<Table, Column>>>
public class Relationship extends io.rocketpartners.db.Relationship<Column>
{
   public static final String REL_MANY_TO_MANY = "MANY_TO_MANY";
   public static final String REL_ONE_TO_MANY  = "ONE_TO_MANY";
   public static final String REL_MANY_TO_ONE  = "MANY_TO_ONE";

   Entity                     entity           = null;
   Entity                     related          = null;

   boolean                    exclude          = false;

   public boolean isExcluded()
   {
      if (exclude)
         return true;

      if (entity != null && entity.isExclude())
         return true;

      if (related != null && related.isExclude())
         return true;

      if (fkCol1 != null && ((Column) fkCol1).isExclude())
         return true;

      if (fkCol2 != null && ((Column) fkCol2).isExclude())
         return true;

      return exclude;
   }

   public boolean isExclude()
   {
      return exclude;
   }

   public void setExclude(boolean exclude)
   {
      this.exclude = exclude;
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
    * @return the related
    */
   public Entity getRelated()
   {
      return related;
   }

   /**
    * @param related the related to set
    */
   public void setRelated(Entity related)
   {
      this.related = related;
   }

}
