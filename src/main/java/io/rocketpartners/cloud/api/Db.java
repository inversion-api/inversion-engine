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

import org.atteo.evo.inflector.English;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.rocketpartners.db.Index;

public abstract class Db<D extends Db, T extends Table, C extends Column, I extends Index> extends io.rocketpartners.db.Db
{
   protected Api    api       = null;

   protected Logger log       = LoggerFactory.getLogger(getClass());
   boolean          bootstrap = true;

   public Db()
   {
   }

   public Db(String name)
   {
      this.name = name;
   }

   public abstract void bootstrapApi() throws Exception;

   public void startup()
   {
   }

   public void shutdown()
   {
   }

   protected String makeRelationshipName(Relationship rel)
   {
      String name = null;
      String type = rel.getType();
      if (type.equals(Relationship.REL_ONE_TO_MANY))
      {
         name = rel.getFkCol1().getName();
         if (name.toLowerCase().endsWith("id") && name.length() > 2)
         {
            name = name.substring(0, name.length() - 2);
         }
      }
      else if (type.equals(Relationship.REL_MANY_TO_ONE))
      {
         name = rel.getRelated().getCollection().getName();//.getTbl().getName();
         if (!name.endsWith("s"))
            name = English.plural(name);
      }
      else if (type.equals(Relationship.REL_MANY_TO_MANY))
      {
         name = rel.getFkCol2().getPk().getTable().getName();
         if (!name.endsWith("s"))
            name = English.plural(name);
      }

      name = Character.toLowerCase(name.charAt(0)) + name.substring(1, name.length());

      return name;
   }

   protected String lowercaseAndPluralizeString(String collectionName)
   {
      collectionName = Character.toLowerCase(collectionName.charAt(0)) + collectionName.substring(1, collectionName.length());

      if (!collectionName.endsWith("s"))
         collectionName = English.plural(collectionName);

      return collectionName;
   }

   public void setApi(Api api)
   {
      this.api = api;
      api.addDb(this);
   }

   public Api getApi()
   {
      return api;
   }

   public boolean isBootstrap()
   {
      return bootstrap;
   }

   public void setBootstrap(boolean bootstrap)
   {
      this.bootstrap = bootstrap;
   }

}
