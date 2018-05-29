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
package io.rcktapp.api.service;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.forty11.j.J;
import io.forty11.sql.Sql;
import io.forty11.web.js.JSArray;
import io.rcktapp.api.Action;
import io.rcktapp.api.Api;
import io.rcktapp.api.ApiException;
import io.rcktapp.api.Chain;
import io.rcktapp.api.Collection;
import io.rcktapp.api.Endpoint;
import io.rcktapp.api.Handler;
import io.rcktapp.api.Request;
import io.rcktapp.api.Response;
import io.rcktapp.api.SC;

public class SuggestHandler implements Handler
{
   public void service(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res) throws Exception
   {
      String properties = req.getParam("field"); //change this to be a comma separated list of collection.property,collection.property...
      String value = req.getParam("value");

      if (J.empty(properties))
         return;

      if (J.empty(value))
         value = "";

      Connection conn = null;
      try
      {
         Integer tenantId = req.getUser().getTenantId();

         List<String> propertyList = Arrays.asList(properties.split(","));
         List<Collection> collections = new ArrayList<>();
         List<String> columns = new ArrayList<>();

         for (String raw : propertyList)
         {
            if (raw.indexOf(".") == -1)
            {
               throw new ApiException(SC.SC_400_BAD_REQUEST, "Autosuggest requests must be formatted as 'collection.property,collection.property' - One of your requested collections did not include a property!");
            }

            Collection collection = api.getCollection(raw.substring(0, raw.indexOf(".")));
            collections.add(collection);
            columns.add(raw.substring(raw.indexOf(".") + 1, raw.length()));
         }

         if (collections.size() != columns.size())
         {
            throw new ApiException(SC.SC_400_BAD_REQUEST, "Autosuggest requests must be formatted as 'collection.property,collection.property' - One of your requested collections did not include a property!");
         }

         try
         {
            value = Sql.check(value);
         }
         catch (Exception ex)
         {
            value = "";
         }

         String sql = "";
         
         String firstColumn = columns.get(0);

         for (int i = 0; i < collections.size(); i++)
         {
            String collection = collections.get(i).getEntity().getTable().getName();
            String column = columns.get(i);
            sql += "SELECT DISTINCT " + column + " FROM " + collection + " WHERE " + column + " LIKE '%" + value + "%' AND " + column + " != ''";
            if (tenantId != null)
               sql += " AND tenantId=" + tenantId;
            
            if (i + 1 < collections.size())
               sql += " UNION ";
            else if (i + 1 == collections.size())
               sql += " ORDER BY " + firstColumn;
         }

         conn = ((Snooze) service).getConnection(req.getApi(), null);

         //make the sql do a distinct union of all the collection.property values passed in.
         //if the api is multi tenant, and the collection has a tenant property that include tenant=? in the query
         List<String> list = Sql.selectList(conn, sql);

         if (list.size() > 1)
         {
            int next = 0;
            for (int i = 1; i < list.size(); i++)
            {
               if (list.get(i).toLowerCase().startsWith(value.toLowerCase()))
               {
                  String val = list.remove(i);
                  list.add(next++, val);
               }
            }
         }

         JSArray arr = new JSArray(list.toArray());
         res.setJson(arr);
      }
      finally
      {
         Sql.close(conn);
      }

   }
}
