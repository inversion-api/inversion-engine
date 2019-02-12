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
package io.rocketpartners.cloud.handler.sql;

import java.util.ArrayList;
import java.util.List;

import io.rocketpartners.cloud.model.Action;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.ApiException;
import io.rocketpartners.cloud.model.Collection;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.rql.Rql;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.service.Request;
import io.rocketpartners.cloud.service.Response;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.utils.CaseInsensitiveSet;
import io.rocketpartners.utils.J;
import io.rocketpartners.utils.Sql;

public class SqlSuggestHandler extends SqlHandler
{
   protected CaseInsensitiveSet<String> whitelist    = new CaseInsensitiveSet<>();

   protected String                     propertyProp = "property";
   protected String                     searchProp   = "value";
   protected String                     tenantCol    = "tenantId";

   public void service(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res) throws Exception
   {
      String propertyProp = chain.getConfig("propertyProp", this.propertyProp);
      String searchProp = chain.getConfig("searchProp", this.searchProp);
      String tenantCol = chain.getConfig("tenantCol", this.tenantCol);

      String whitelistStr = chain.getConfig("whitelist", null);
      CaseInsensitiveSet<String> whitelist = this.whitelist;
      if (whitelistStr != null)
      {
         whitelist = new CaseInsensitiveSet(J.explode(",", whitelistStr));
      }

      String properties = req.removeParam(propertyProp);

      if (J.empty(properties))
         throw new ApiException(SC.SC_400_BAD_REQUEST, "Missing query param '" + propertyProp + "' which should be a comma separated list of collection.property names to query");

      if (!properties.contains("."))
         throw new ApiException(SC.SC_400_BAD_REQUEST, "Query param '" + propertyProp + "' must be in the format '{collection}.{property}'");

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

      String sql = "";
      sql += " SELECT DISTINCT " + searchProp;
      sql += " \r\n FROM (";

      List<String> propertyList = J.explode(",", properties);
      String firstProp = propertyList.get(0);
      String collectionKey = firstProp.substring(0, firstProp.indexOf("."));

      Collection collection = req.getApi().getCollection(collectionKey, SqlDb.class);
      if (collection == null)
         throw new ApiException(SC.SC_404_NOT_FOUND, "Collection '" + collectionKey + "' could not be found");

      SqlDb db = (SqlDb) collection.getEntity().getTable().getDb();
      
      SqlQuery query = ((SqlRql) Rql.getRql(db.getType())).buildQuery(collection.getEntity().getTable(), req.getParams()); 

      for (int i = 0; i < propertyList.size(); i++)
      {
         String prop = propertyList.get(i);

         if (!whitelist.contains(prop))
            throw new ApiException(SC.SC_400_BAD_REQUEST, "One of the properties you requested is not in the SuggestHandler whitelist, please edit your query or your config and try again");

         if (prop.indexOf(".") < 0)
            throw new ApiException(SC.SC_400_BAD_REQUEST, "Query param '" + propertyProp + "' must be of the form '" + propertyProp + "=collection.property[,collection.property...]");

         collectionKey = prop.substring(0, prop.indexOf("."));

         String tableName = Sql.check(api.getCollection(collectionKey, SqlDb.class).getEntity().getTable().getName());
         String column = Sql.check(prop.substring(prop.indexOf(".") + 1, prop.length()));

         sql += " \r\nSELECT DISTINCT " + query.asCol(column) + " AS " + searchProp + " FROM " + query.asCol(tableName) + " WHERE " + query.asCol(column) + " LIKE '%" + Sql.check(value) + "%' AND " + query.asCol(column) + " != ''";

         if (api.isMultiTenant() && api.findTable(tableName).getColumn(tenantCol) != null)
            sql += " AND " + query.asCol(tenantCol) + "=" + req.getUser().getTenantId();

         if (i + 1 < propertyList.size())
            sql += " \r\nUNION ";
      }
      sql += " \r\n ) as v ";
      sql += " \r\n ORDER BY CASE WHEN " + searchProp + " LIKE '" + Sql.check(value) + "%' THEN 0 ELSE 1 END, " + searchProp;

      // removing the tenantId here so the Get Handler won't add an additional where clause to the sql we are sending it
      req.removeParam("tenantId");

      chain.put("db", db.getName());
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
