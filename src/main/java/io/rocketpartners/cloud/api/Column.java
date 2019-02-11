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

public class Column<T extends Table, C extends Column> extends io.rocketpartners.db.Column<T, C>
{
   boolean exclude = false;

   public Column(T table, int number, String name, String type, boolean nullable)
   {
      super(table, number, name, type, nullable);
   }

   public boolean isExclude()
   {
      return exclude;
   }

   public void setExclude(boolean exclude)
   {
      this.exclude = exclude;
   }
}
