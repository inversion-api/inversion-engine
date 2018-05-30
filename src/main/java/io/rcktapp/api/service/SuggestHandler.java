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
import java.util.Arrays;
import java.util.List;

import io.forty11.j.J;
import io.forty11.sql.Sql;
import io.forty11.web.js.JSArray;
import io.rcktapp.api.Action;
import io.rcktapp.api.Api;
import io.rcktapp.api.ApiException;
import io.rcktapp.api.Chain;
import io.rcktapp.api.Endpoint;
import io.rcktapp.api.Handler;
import io.rcktapp.api.Request;
import io.rcktapp.api.Response;
import io.rcktapp.api.SC;

public class SuggestHandler implements Handler
{

   List<String> whitelist = null;

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
         boolean isMultiTenant = api.isMultiTenant();
         List<String> propertyList = Arrays.asList(properties.split(","));

         try
         {
            value = Sql.check(value);
         }
         catch (Exception ex)
         {
            value = "";
         }

         String sql = "SELECT DISTINCT value FROM (";

         for (int i = 0; i < propertyList.size(); i++)
         {
            String prop = propertyList.get(0);

            if (whitelist != null)
            {
               if (!whitelist.contains(prop))
                  throw new ApiException(SC.SC_400_BAD_REQUEST, "One of the properties you requested to autosuggest is not in the whitelist, please edit your request and try again");
            }
            if (prop.indexOf(".") < 0)
               throw new ApiException(SC.SC_400_BAD_REQUEST, "All autosuggest requests must have both a collection and property name, error was thrown due to: " + prop);
            
            String tableName = api.getCollection(prop.substring(0, prop.indexOf("."))).getEntity().getTable().getName();
            String column = prop.substring(prop.indexOf(".") + 1, prop.length());

            sql += "SELECT DISTINCT " + column + " AS value FROM " + tableName + " WHERE " + column + " LIKE '%" + value + "%' AND " + column + " != ''";
            if (isMultiTenant && api.findTable(tableName).getCol("tenantId") != null)
               sql += " AND tenantId=" + req.getUser().getTenantId();

            if (i + 1 < propertyList.size())
               sql += " UNION ";
         }

         sql += ") v ORDER BY value";

         conn = ((Snooze) service).getConnection(req.getApi(), null);

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
