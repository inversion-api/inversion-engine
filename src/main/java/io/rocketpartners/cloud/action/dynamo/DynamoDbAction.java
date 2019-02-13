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
package io.rocketpartners.cloud.action.dynamo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.rocketpartners.cloud.model.Action;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.utils.Utils;

/**
 * @author tc-rocket
 *
 */
public abstract class DynamoDbAction<A extends DynamoDbAction> extends Action<A>
{
   static ObjectMapper mapper            = new ObjectMapper();

   /**
    * Used when "appendTenantIdToPk" is enabled for a table
    * The default value for this field is "::" so the primary key will look like this..
    * {tenant_id}::{pk_value}
    * 
    * Example:  1::4045551212
    */
   protected String    tenantIdDelimiter = "::";

   protected boolean isAppendTenantIdToPk(Chain chain, String collectionName)
   {
      return chain.getConfigSet("appendTenantIdToPk").contains(collectionName);
   }

   String addTenantIdToKey(Object tenantIdOrCode, String key)
   {
      return tenantIdOrCode + tenantIdDelimiter + key;
   }

   String removeTenantIdFromKey(Object tenantIdOrCode, String key)
   {
      if (key != null)
      {
         int preLength = (tenantIdOrCode + tenantIdDelimiter).length();
         return key.substring(preLength);
      }
      return key;
   }

   protected List splitToList(String csv)
   {
      List<String> l = new ArrayList<>();
      if (csv != null)
      {
         String[] arr = csv.split(",");
         for (String e : arr)
         {
            e = e.trim();
            if (!Utils.empty(e))
               l.add(e);
         }
      }
      return l;
   }

   static Object jsonStringToObject(String jsonStr) throws JsonParseException, JsonMappingException, IOException
   {
      return mapper.readValue(jsonStr, Object.class);
   }

   public void setTenantIdDelimiter(String tenantIdDelimiter)
   {
      this.tenantIdDelimiter = tenantIdDelimiter;
   }

}
