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
package io.rcktapp.rql.elastic;

public class ExistsQuery extends ElasticQuery
{

   private String field;

   public ExistsQuery(String field)
   {
      this.field = field;
      
      if (field.contains("."))
      {
         this.nestedPath = field.substring(0, field.lastIndexOf("."));
      }
   }

   /**
    * @return the field
    */
   public String getField()
   {
      return field;
   }

}
