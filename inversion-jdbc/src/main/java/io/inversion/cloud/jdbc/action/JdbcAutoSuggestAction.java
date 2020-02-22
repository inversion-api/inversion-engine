/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.cloud.jdbc.action;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.inversion.cloud.jdbc.db.JdbcDb;
import io.inversion.cloud.jdbc.utils.JdbcUtils;
import io.inversion.cloud.model.Action;
import io.inversion.cloud.model.ApiException;
import io.inversion.cloud.model.Collection;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.model.Status;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.utils.Utils;

public class JdbcAutoSuggestAction extends Action<JdbcAutoSuggestAction>
{
   protected HashSet<String> whitelist    = new HashSet();

   protected String          propertyProp = "property";
   protected String          searchProp   = "value";
   protected String          tenantCol    = "tenantId";

   public void run(Request req, Response res) throws Exception
   {
      String propertyProp = req.getChain().getConfig("propertyProp", this.propertyProp);
      String searchProp = req.getChain().getConfig("searchProp", this.searchProp);
      String tenantCol = req.getChain().getConfig("tenantCol", this.tenantCol);

      String whitelistStr = req.getChain().getConfig("whitelist", null);
      Set<String> whitelist = this.whitelist;
      if (whitelistStr != null)
      {
         whitelist = new HashSet(Utils.explode(",", whitelistStr.toLowerCase()));
      }

      String properties = req.removeParam(propertyProp);

      if (Utils.empty(properties))
         throw new ApiException(Status.SC_400_BAD_REQUEST, "Missing query param '" + propertyProp + "' which should be a comma separated list of collection.property names to query");

      if (!properties.contains("."))
         throw new ApiException(Status.SC_400_BAD_REQUEST, "Query param '" + propertyProp + "' must be in the format '{collection}.{property}'");

      String value = req.removeParam(searchProp);
      if (Utils.empty(value))
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

      List<String> propertyList = Utils.explode(",", properties);
      String firstProp = propertyList.get(0);
      String collectionKey = firstProp.substring(0, firstProp.indexOf("."));

      Collection collection = req.getApi().getCollection(collectionKey);//getApi().getCollection(collectionKey, SqlDb.class);
      if (collection == null)
         throw new ApiException(Status.SC_404_NOT_FOUND, "Collection '" + collectionKey + "' could not be found");

      String sql = "";
      sql += " SELECT DISTINCT " + searchProp;
      sql += " \r\n FROM (";

      for (int i = 0; i < propertyList.size(); i++)
      {
         String prop = propertyList.get(i);

         if (!whitelist.contains(prop.toLowerCase()))
            throw new ApiException(Status.SC_400_BAD_REQUEST, "One of the properties you requested is not in the SuggestHandler whitelist, please edit your query or your config and try again");

         if (prop.indexOf(".") < 0)
            throw new ApiException(Status.SC_400_BAD_REQUEST, "Query param '" + propertyProp + "' must be of the form '" + propertyProp + "=collection.property[,collection.property...]");

         collectionKey = prop.substring(0, prop.indexOf("."));

         String tableName = JdbcUtils.check(collection.getTableName());
         String column = JdbcUtils.check(prop.substring(prop.indexOf(".") + 1, prop.length()));

         sql += " \r\nSELECT DISTINCT " + column + " AS " + searchProp + " FROM " + tableName + " WHERE " + column + " LIKE '%" + JdbcUtils.check(value) + "%' AND " + column + " != ''";

         if (req.getApi().isMultiTenant() && req.getApi().getCollection(tableName).getProperty(tenantCol) != null)
            sql += " AND " + tenantCol + "=" + Chain.getUser().getTenantId();

         if (i + 1 < propertyList.size())
            sql += " \r\nUNION ";
      }
      sql += " \r\n ) as v ";
      sql += " \r\n ORDER BY CASE WHEN " + searchProp + " LIKE '" + JdbcUtils.check(value) + "%' THEN 0 ELSE 1 END, " + searchProp;

      // removing the tenantId here so the Get Handler won't add an additional where clause to the sql we are sending it
      req.removeParam("tenantId");

      JdbcDb db = (JdbcDb) collection.getDb();
      req.getChain().put("db", db.getName());
      req.getChain().put("select", sql);
   }

   public List<String> getWhitelist()
   {
      return new ArrayList<String>(whitelist);
   }

   public void setWhitelist(java.util.Collection<String> whitelist)
   {
      this.whitelist.clear();
      for (String entry : whitelist)
      {
         String lowercaseEntry = entry.toLowerCase();
         this.whitelist.add(lowercaseEntry);
      }
   }

   public JdbcAutoSuggestAction withWhitelist(java.util.Collection<String> whitelist)
   {
      setWhitelist(whitelist);
      return this;
   }

   public JdbcAutoSuggestAction withWhitelist(String... whitelist)
   {
      this.whitelist.clear();
      for (String whitelistEntry : Utils.explode(",", whitelist))
      {
         this.whitelist.add(whitelistEntry.toLowerCase());
      }

      return this;
   }

}
