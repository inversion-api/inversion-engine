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

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.KeyValue;

import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.ApiException;
import io.rocketpartners.cloud.model.ArrayNode;
import io.rocketpartners.cloud.model.Collection;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.model.Entity;
import io.rocketpartners.cloud.model.ObjectNode;
import io.rocketpartners.cloud.model.Request;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.SqlUtils;
import io.rocketpartners.cloud.utils.Utils;

public class SqlDeleteAction extends SqlAction
{
   boolean      allowBatchDelete = true;

   List<String> batchAllow       = new ArrayList();
   List<String> batchDeny        = new ArrayList();

   @Override
   public void run(Service service, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
   {
      boolean rootDelete = chain.get("batch.delete.sqls") == null;
      if (rootDelete)
      {
         chain.put("batch.delete.sqls", new ArrayList());
         chain.put("batch.delete.args", new ArrayList());
      }

      List sqls = (List) chain.get("batch.delete.sqls");
      List args = (List) chain.get("batch.delete.args");

      try
      {
         String entityKey = req.getEntityKey();

         if (req.getJson() != null)
         {
            if (!Utils.empty(entityKey))
               throw new ApiException(SC.SC_400_BAD_REQUEST, "You can't DELETE to an entity key in the url and also include a JSON body.");

            ObjectNode obj = req.getJson();
            if (!(obj instanceof ArrayNode))
            {
               throw new ApiException(SC.SC_400_BAD_REQUEST, "The JSON body to a DELETE must be an array that contains string urls.");
            }

            List<String> urls = new ArrayList();

            for (Object o : (ArrayNode) obj)
            {
               if (!(o instanceof String))
                  throw new ApiException(SC.SC_400_BAD_REQUEST, "The JSON body to a DELETE must be an array that contains string urls.");

               String url = (String) o;

               String path = req.getUrl().toString();
               if (path.indexOf("?") > 0)
                  path = path.substring(0, path.indexOf("?") - 1);

               if (!url.toLowerCase().startsWith(path.toLowerCase()))
               {
                  throw new ApiException(SC.SC_400_BAD_REQUEST, "All delete request must be for the collection in the original request: '" + path + "'");
               }
               urls.add((String) o);
            }

            for (String url : urls)
            {
               Response r = service.delete(url);
               if (r.getStatusCode() != 200)
               {
                  throw new ApiException("Nested delete url: " + url + " failed!");
               }
            }

            //         JSObject deletedKeyJs = new JSObject();
            //         deletedKeyJs.put("deletedKeys", new JSArray(deletedKeys));
            //         res.setJson(deletedKeyJs);
         }
         else
         {
            String collection = req.getCollectionKey().toLowerCase();

            if (!Utils.empty(entityKey) || //
                  (allowBatchDelete && //
                        ((batchAllow.isEmpty() && batchDeny.isEmpty()) || //
                              (!batchAllow.isEmpty() && batchAllow.contains(collection)) || (batchAllow.isEmpty() && !batchDeny.contains(collection)))))
            {
               findIds(chain, req, res, sqls, args);
            }
            else
            {
               throw new ApiException(SC.SC_400_BAD_REQUEST, "Batch deletes are not allowed for this collection");
            }
         }

         if (rootDelete)
         {
            delete(chain, sqls, args);
         }
      }
      finally
      {
         chain.remove("batch.delete.sqls");
         chain.remove("batch.delete.args");
      }
   }

   void delete(Chain chain, List<String> sqls, List<Object> args) throws Exception
   {
      String sql = "";

      if (sqls.size() == 1)
      {
         sql = sqls.get(0);
      }
      else
      {
         for (int i = 0; i < sqls.size(); i++)
         {
            sql += "\r\n" + sqls.get(i);
            if (i < sqls.size() - 1)
               sql += "\r\n UNION ";
         }
         sql = sql.trim();
      }

      Request req = chain.getRequest();
      Collection collection = req.getCollection();//getApi().getCollection(req.getCollectionKey(), SqlDb.class);
      Entity entity = collection.getEntity();

      SqlDb db = (SqlDb) req.getCollection().getDb();// chain.getService().getDb(req.getApi(), req.getCollectionKey(), SqlDb.class);
      Connection conn = db.getConnection();

      SqlQuery query = new SqlQuery(collection);

      String table = query.quoteCol(entity.getTable().getName());
      String idCol = query.quoteCol(entity.getKey().getColumn().getName());

      List ids = SqlUtils.selectList(conn, sql, args);
      if (ids.size() > 0)
      {
         String idstr = SqlUtils.check(Utils.implode(",", ids));
         SqlUtils.execute(conn, "DELETE FROM " + table + " WHERE " + idCol + " IN (" + idstr + ")");
      }

      for (Object id : ids)
      {
         chain.getResponse().withChange("DELETE", collection.getName(), Long.parseLong(id + ""));
      }

   }

   void findIds(Chain chain, Request req, Response res, List sqls, List args) throws Exception
   {
      try
      {
         SqlDb db = ((SqlDb)req.getCollection().getDb());
         //SqlRql rql = (SqlRql) Rql.getRql(db.getType());

         Collection collection = req.getCollection();//getApi().getCollection(req.getCollectionKey(), SqlDb.class);

         Entity entity = collection.getEntity();
         String entityKey = req.getEntityKey();

         //String table = rql.asCol(entity.getTable().getName());

         Map params = req.getParams();
         String keyAttr = collection.getEntity().getKey().getName();
         if (!Utils.empty(entityKey))
         {
            params.put("in(`" + keyAttr + "`," + entityKey + ")", null);
         }

         SqlQuery query = new SqlQuery(collection, params);

         String sql = "SELECT " + query.asCol(collection.getEntity().getKey().getColumn().getName()) + " FROM " + query.asCol(entity.getTable().getName());

         query.withSelectSql(sql);

         //query.page().setMaxRows(0);

         sql = query.getPreparedStmt();

         sql = sql.replaceAll("SQL_CALC_FOUND_ROWS", "");

         if (sql.toLowerCase().indexOf(" where ") < 0)
            throw new ApiException(SC.SC_400_BAD_REQUEST, "You can't delete from a table without a where clause or an individual ID.");

         sqls.add(sql);

         for (int i = 0; i < query.getNumValues(); i++)
         {
            KeyValue<String, String> pair = query.getColValue(i);
            String col = pair.getKey();
            String val = pair.getValue();
            args.add(cast(collection, col, val));
         }
      }
      catch (Exception e)
      {
         throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, e.getMessage(), e);
      }

   }
}
