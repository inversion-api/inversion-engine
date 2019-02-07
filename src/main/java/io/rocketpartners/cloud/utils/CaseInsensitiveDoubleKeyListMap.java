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
package io.rocketpartners.cloud.utils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.CaseInsensitiveMap;

public class CaseInsensitiveDoubleKeyListMap
{
   Map<Object, CaseInsensitiveListMap> root = new CaseInsensitiveMap();

   public Object put(Object key1, Object key2, Object value)
   {
      CaseInsensitiveListMap key2Map = root.get(key1);
      if (key2Map == null)
      {
         key2Map = new CaseInsensitiveListMap();
         root.put(key1, key2Map);
      }

      return key2Map.put(key2, value);
   }

   public List get(Object key1, Object key2)
   {
      CaseInsensitiveListMap key2Map = root.get(key1);
      if (key2Map != null)
      {
         return key2Map.get(key2);
      }

      return Collections.EMPTY_LIST;
   }
}