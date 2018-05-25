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

package io.forty11.js;

import java.util.Iterator;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JS
{
   public static JSArray toJSArray(String json)
   {
      return ((JSArray) parse(json));
   }

   public static JSObject toJSObject(String json)
   {
      return ((JSObject) parse(json));
   }

   public static Object toObject(String json)
   {
      return parse(json);
   }

   static Object parse(String js)
   {
      try
      {
         ObjectMapper mapper = new ObjectMapper();
         JsonNode rootNode = mapper.readValue(js, JsonNode.class);

         Object parsed = map(rootNode);
         return parsed;
      }
      catch (Exception ex)
      {
         String mesg = "Error parsing JSON:" + ex.getMessage();

         if (!(ex instanceof JsonParseException))
         {
            mesg += "\r\nSource:" + js;
         }

         throw new RuntimeException("400 Bad Request: '" + js + "'");
      }
   }

   static Object map(JsonNode json)
   {
      if (json == null)
         return null;

      if (json.isNull())
         return null;

      if (json.isValueNode())
         return json.asText();//FIX

      if (json.isArray())
      {
         JSArray retVal = null;
         retVal = new JSArray();

         for (JsonNode child : json)
         {
            retVal.add(map(child));
         }

         return retVal;
      }
      else if (json.isObject())
      {
         JSObject retVal = null;
         retVal = new JSObject();

         Iterator<String> it = json.fieldNames();
         while (it.hasNext())
         {
            String field = it.next();
            JsonNode value = json.get(field);
            retVal.put(field, map(value));
         }
         return retVal;
      }

      throw new RuntimeException("unparsable json:" + json);
   }
}
