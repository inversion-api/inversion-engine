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
import java.util.List;

import io.forty11.j.J;
import io.forty11.sql.Sql;
import io.forty11.utils.CaseInsensitiveSet;
import io.forty11.web.js.JSArray;
import io.forty11.web.js.JSObject;
import io.rcktapp.api.Action;
import io.rcktapp.api.Api;
import io.rcktapp.api.ApiException;
import io.rcktapp.api.Chain;
import io.rcktapp.api.Endpoint;
import io.rcktapp.api.Request;
import io.rcktapp.api.Response;
import io.rcktapp.api.SC;
import io.rcktapp.rql.RQL;

public class SuggestHandler extends RqlHandler
{
   CaseInsensitiveSet<String> whitelist    = new CaseInsensitiveSet<>();
   int                        maxResults   = 100;

   String                     propertyProp = "property";
   String                     searchProp   = "search";

   public void service(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res) throws Exception
   {
      String properties = req.getParam(propertyProp);

      if (J.empty(properties))
         throw new ApiException(SC.SC_400_BAD_REQUEST, "Missing query param '" + propertyProp + "' which should be a comma separated list of collection.property names to query");

      String value = req.getParam(searchProp);
      if (J.empty(value))
      {
         value = "";
      }
      else
      {
         value = value.trim();
         value = value.replace("`", "");
         value = value.replace("\'", "");
         value = value.replace("\"", "");
      }

      RQL rql = makeRql(chain);

      String sql = "";
      sql += " SELECT DISTINCT value FROM (";

      List<String> propertyList = J.explode(",", properties);
      for (int i = 0; i < propertyList.size(); i++)
      {
         String prop = propertyList.get(i);

         if (!whitelist.contains(prop))
            throw new ApiException(SC.SC_400_BAD_REQUEST, "One of the properties you requested is not in the SuggestHandler whitelist, please edit your query and try again");

         if (prop.indexOf(".") < 0)
            throw new ApiException(SC.SC_400_BAD_REQUEST, "All autosuggest requests must have both a collection and property name, error was thrown due to: " + prop);

         String tableName = Sql.check(api.getCollection(prop.substring(0, prop.indexOf("."))).getEntity().getTable().getName());
         String column = Sql.check(prop.substring(prop.indexOf(".") + 1, prop.length()));

         sql += " SELECT DISTINCT " + rql.asCol(column) + " AS value FROM " + rql.asCol(tableName) + " WHERE " + rql.asCol(column) + " LIKE '%" + Sql.check(value) + "%' AND " + column + " != ''";

         //TODO: this would come for free and be more secure if we used RQL to construct the sql
         if (api.isMultiTenant() && api.findTable(tableName).getCol("tenantId") != null)
            sql += " AND tenantId=" + req.getUser().getTenantId();

         if (i + 1 < propertyList.size())
            sql += " UNION ";
      }
      sql += " ) v ";
      sql += " ORDER BY value LIMIT " + maxResults;

      Connection conn = ((Snooze) service).getConnection(req.getApi(), null);
      List<String> list = Sql.selectList(conn, sql);

      //puts results that start with 'value' at the begining of the
      //results regardless of their canonical sort order
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

      //this should matche the get handler format
      //TODO would be pageable if we use RQL to construct the query
      JSObject meta = new JSObject();
      meta.put("rowCount", list.size());
      meta.put("pageNum", 1);
      meta.put("pageSize", maxResults);
      meta.put("pageCount", 1);

      res.setJson(new JSObject("meta", meta, "data", new JSArray(list.toArray())));
   }

   public List<String> getWhitelist()
   {
      return new ArrayList(whitelist);
   }

   public void setWhitelist(java.util.Collection<String> whitelist)
   {
      this.whitelist.clear();
      this.whitelist.addAll(whitelist);
   }

}
