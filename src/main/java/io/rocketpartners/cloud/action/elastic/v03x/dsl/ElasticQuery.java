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
package io.rocketpartners.cloud.action.elastic.v03x.dsl;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Does nothing special other than a base class to extend from
 * @author kfrankic
 *
 */
public abstract class ElasticQuery
{
   // identifies a nested path
   @JsonIgnore
   protected String nestedPath;
   
   public String getNestedPath() {
      return nestedPath;
   }
}
