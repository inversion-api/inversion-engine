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
package io.rocketpartners.cloud.action.sql;

import io.rocketpartners.cloud.action.rest.RestGetAction;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.ApiException;
import io.rocketpartners.cloud.model.Collection;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.model.Entity;
import io.rocketpartners.cloud.model.Request;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.model.Table;
import io.rocketpartners.cloud.rql.Query.QueryResults;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.Utils;

public class SqlGetAction extends RestGetAction<SqlGetAction>
{
   @Override
   public QueryResults runQuery(Service service, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
   {
      Collection collection = null;
      try
      {
         collection = req.getCollection();
      }
      catch (ApiException e)
      {
         // need to try catch this because getCollection throws an exception if the collection isn't found
         // but not having a collection isn't always an error in this handler because a previous handler 
         // like the SqlSuggestHandler or ScriptHandler may have set the "sql" chain param. 
      }

      String dbname = (String) chain.get("db");
      SqlDb db = (SqlDb) (collection != null ? collection.getDb() : api.getDb(dbname));
      if (db == null)
      {
         throw new ApiException(SC.SC_404_NOT_FOUND, "Unable to map request to a db table or query. Please check your endpoint.");
      }

      Entity entity = collection != null ? collection.getEntity() : null;
      Table tbl = entity != null ? entity.getTable() : null;

      String sql = (String) chain.remove("select");
      if (Utils.empty(sql))
         sql = " SELECT * FROM " + tbl.getName();

      SqlQuery query = new SqlQuery(collection, req.getParams());
      query.withSelectSql(sql);
      query.withDb(db);

      return query.runQuery();

   }
}
