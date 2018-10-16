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

import java.util.ArrayList;
import java.util.List;

import io.forty11.j.J;
import io.forty11.sql.Sql;
import io.forty11.utils.CaseInsensitiveSet;
import io.rcktapp.api.Action;
import io.rcktapp.api.Api;
import io.rcktapp.api.ApiException;
import io.rcktapp.api.Chain;
import io.rcktapp.api.Db;
import io.rcktapp.api.Endpoint;
import io.rcktapp.api.Request;
import io.rcktapp.api.Response;
import io.rcktapp.api.SC;
import io.rcktapp.rql.RQL;

public class SuggestHandler extends RqlHandler
{
   CaseInsensitiveSet<String> whitelist    = new CaseInsensitiveSet<>();

   String                     propertyProp = "property";
   String                     searchProp   = "value";
   String                     tenantCol    = "tenantId";

   public void service(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res) throws Exception
   {
      String properties = req.removeParam(propertyProp);

      if (J.empty(properties))
         throw new ApiException(SC.SC_400_BAD_REQUEST, "Missing query param '" + propertyProp + "' which should be a comma separated list of collection.property names to query");

      String value = req.removeParam(searchProp);
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

      Db db = chain.getService().getDb(req.getApi(), req.getCollectionKey());
      RQL rql = makeRql(db);

      String sql = "";
      sql += " SELECT DISTINCT " + searchProp;
      sql += " \r\n FROM (";

      List<String> propertyList = J.explode(",", properties);
      for (int i = 0; i < propertyList.size(); i++)
      {
         String prop = propertyList.get(i);

         if (!whitelist.contains(prop))
            throw new ApiException(SC.SC_400_BAD_REQUEST, "One of the properties you requested is not in the SuggestHandler whitelist, please edit your query or your config and try again");

         if (prop.indexOf(".") < 0)
            throw new ApiException(SC.SC_400_BAD_REQUEST, "Query param '" + propertyProp + "' must be of the form '" + propertyProp + "=collection.property[,collection.property...]");

         String tableName = Sql.check(api.getCollection(prop.substring(0, prop.indexOf("."))).getEntity().getTable().getName());
         String column = Sql.check(prop.substring(prop.indexOf(".") + 1, prop.length()));

         sql += " \r\nSELECT DISTINCT " + rql.asCol(column) + " AS " + searchProp + " FROM " + rql.asCol(tableName) + " WHERE " + rql.asCol(column) + " LIKE '%" + Sql.check(value) + "%' AND " + rql.asCol(column) + " != ''";

         if (api.isMultiTenant() && api.findTable(tableName).getCol(tenantCol) != null)
            sql += " AND " + rql.asCol(tenantCol) + "=" + req.getUser().getTenantId();

         if (i + 1 < propertyList.size())
            sql += " \r\nUNION ";
      }
      sql += " \r\n ) as v ";
      sql += " \r\n ORDER BY CASE WHEN " + searchProp + " LIKE '" + Sql.check(value) + "%' THEN 0 ELSE 1 END, " + searchProp;

      
      // removing the tenantId here so the Get Handler won't add an additional where clause to the sql we are sending it
      req.removeParam("tenantId");
      chain.put("select", sql);
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
