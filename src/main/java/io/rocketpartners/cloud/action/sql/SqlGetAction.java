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

import java.net.URLEncoder;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import io.rocketpartners.cloud.model.Action;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.ApiException;
import io.rocketpartners.cloud.model.Attribute;
import io.rocketpartners.cloud.model.Collection;
import io.rocketpartners.cloud.model.Column;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.model.Entity;
import io.rocketpartners.cloud.model.Relationship;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.model.Table;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.service.Request;
import io.rocketpartners.cloud.service.Response;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.JSArray;
import io.rocketpartners.cloud.utils.JSObject;
import io.rocketpartners.cloud.utils.Rows;
import io.rocketpartners.cloud.utils.Rows.Row;
import io.rocketpartners.cloud.utils.Sql;
import io.rocketpartners.cloud.utils.Utils;

public class SqlGetAction extends SqlAction
{
   protected int maxRows        = 100;

   Set reservedParams = new HashSet(Arrays.asList("includes", "excludes", "expands"));

   @Override
   public void service(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res) throws Exception
   {
      Connection conn = null;

      Set<String> includes = chain.getConfigSet("includes");
      includes.addAll(splitParam(req, "includes"));

      Set<String> excludes = chain.getConfigSet("excludes");
      excludes.addAll(splitParam(req, "excludes"));

      Set<String> expands = chain.getConfigSet("expands");
      expands.addAll(splitParam(req, "expands"));

      String dbName = (String) chain.get("db");
      SqlDb db = null;
      if (!Utils.empty(dbName))
      {
         db = (SqlDb) api.getDb(dbName);
      }
      else
      {
         db = (SqlDb) chain.getService().getDb(req.getApi(), req.getCollectionKey(), SqlDb.class);
      }

      conn = db.getConnection();

      Collection collection = null;
      try
      {
         // need to try catch this because getCollection throws an exception if the collection isn't found
         // but not having a collection isn't always an error in this handler because a previous handler 
         // like the SqlSuggestHandler or ScriptHandler may have set the "sql" chain param. 
         collection = req.getCollectionKey() != null ? req.getApi().getCollection(req.getCollectionKey(), SqlDb.class) : null;
      }
      catch (ApiException e)
      {
      }

      SqlQuery query = new SqlQuery(collection, req.getParams());

      Entity entity = collection != null ? collection.getEntity() : null;

      Table tbl = entity != null ? entity.getTable() : null;

      String sql = "";
      List params = new ArrayList();
      List sqlParams = (List) chain.get("sqlParams");
      if (sqlParams != null && !sqlParams.isEmpty())
      {
         params.addAll(sqlParams);
      }

      if (collection != null && entity != null && !Utils.empty(req.getSubCollectionKey()))
      {
         //-- this is an entity sub collection listing request
         //-- ${http://host/apipath}/collectionKey/entityKey/subCollectionKey

         for (Relationship rel : entity.getRelationships())
         {
            if (req.getSubCollectionKey().equals(rel.getName()))
            {
               collection = req.getApi().getCollection(rel.getRelated());

               if (rel.isManyToOne())
               {
                  String relTbl = query.quoteCol(rel.getFkCol1().getTable().getName());
                  String relFk = query.quoteCol(rel.getFkCol1().getName());

                  sql += " SELECT id FROM " + relTbl;
                  sql += " WHERE " + relFk + " = ? ";
               }
               else if (rel.isManyToMany())
               {
                  collection = req.getApi().getCollection(rel.getFkCol2().getPk().getTable());

                  String linkTblKey = query.quoteCol(rel.getFkCol1().getName());
                  String linkTblFk = query.quoteCol(rel.getFkCol2().getName());
                  String linkTbl = query.quoteCol(rel.getFkCol1().getTable().getName());

                  String pkCol = linkTbl + "." + linkTblKey;
                  String fkCol = linkTbl + "." + linkTblFk;

                  sql += "\r\n SELECT " + fkCol;
                  sql += "\r\n FROM " + linkTbl;
                  sql += "\r\n WHERE " + fkCol + " IS NOT NULL AND " + pkCol + " =  ? ";
               }

               List ids = Sql.selectList(conn, sql, req.getEntityKey());

               String newUrl = Service.buildLink(req, collection.getName(), Utils.implode(",", ids.toArray()), null);

               String queryStr = req.getQuery();
               if (!Utils.empty(queryStr))
               {
                  newUrl += "?" + queryStr;
               }

               Response included = service.include(chain, "GET", newUrl, null);

               res.setStatus(included.getStatus());
               res.setJson(included.getJson());

               return;
            }
         }
      }
      else if (entity != null && !Utils.empty(req.getCollectionKey()) && !Utils.empty(req.getEntityKey()))
      {
         String keyCol = entity.getKey().getName();

         //-- this is a request for one or more entities by ID
         //-- ${http://host/apipath}/collectionKey/entityKey[,entityKey2,entityKey3....,entityKeyN]

         String inClause = Sql.getInClauseStr(Utils.explode(",", Sql.check(req.getEntityKey())));

         sql += " SELECT * FROM " + query.quoteCol(tbl.getName());
         sql += " WHERE " + Sql.check(keyCol) + " IN (" + inClause + ") ";
      }
      else if (tbl != null)
      {
         //-- this is a listing request
         //-- ${http://host/apipath}/collectionKey
         //sql += " SELECT * FROM " + query.quoteCol(tbl.getName());
         sql += " SELECT * FROM " + tbl.getName();
      }

      //-- support for custom sql statements from a Rule
      //--

      String passedInSelect = (String) chain.remove("select");
      if (!Utils.empty(passedInSelect))
      {
         sql = passedInSelect.trim();
      }

      if (Utils.empty(sql))
      {
         throw new ApiException(SC.SC_404_NOT_FOUND, "Unable to map request to a db table or query. Please check your endpoint.");
      }

      //Replacer replacer = new Replacer(rql);

      Map rqlParams = req.getParams();

      //Stmt stmt = rql.createStmt(sql, collection != null ? collection.getEntity().getTable() : null, rqlParams, replacer);
      //stmt.setMaxRows(chain.getConfig("maxRows", maxRows)); //this is a default value

      query.withSelectSql(sql);

      if (query.page().getLimit() <= 0)
         query.page().limit(getMaxRows());

      sql = query.getPreparedStmt();

      if (includes.size() > 0)
      {
         includes = new HashSet<String>(query.getColValueKeys());
      }

      for (int i = 0; i < query.getNumValues(); i++)
      {
         String col = query.getColValue(i).getKey();
         String val = query.getColValue(i).getValue();
         params.add(cast(collection, col, val));
      }

      //-- end SQL construction for the primary query
      //--
      //--

      List<JSObject> results = null;

      if (collection != null && collection.getEntity().getKey() != null)
      {
         results = queryObjects(query, service, chain, action, req, res, db, conn, includes, excludes, expands, "", collection, sql, params);
      }
      else
      {
         results = getRows(chain, action, req, db, conn, includes, excludes, sql, params);
      }

      if (results.size() == 0 && req.getEntityKey() != null && req.getCollectionKey() != null)
      {
         res.setStatus(SC.SC_404_NOT_FOUND);
         res.setJson(null);
      }
      else
      {
         JSObject meta = new JSObject();
         JSArray data = new JSArray();

         JSObject wrapper = new JSObject("meta", meta, "data", data);
         res.setJson(wrapper);

         int rowCount = 1;
         if (req.getEntityKey() == null)
         {
            Integer c = (Integer) chain.get("rowCount");
            if (c != null)
               rowCount = c;
            else
               rowCount = -1;
         }
         else
         {
            //this should be 1
            rowCount = results.size();
         }

         meta.put("rowCount", rowCount);
         meta.put("pageSize", query.page().getLimit());

         //if (db.isCalcRowsFound())
         {
            meta.put("pageNum", query.page().getPageNum());
            int pages = (int) Math.ceil((double) rowCount / (double) query.page().getLimit());
            meta.put("pageCount", pages);
         }

         meta.put("created", Utils.formatIso8601(new Date()));

         for (JSObject js : results)
         {
            data.add(js);
         }
      }
   }

   List getRows(Chain chain, Action action, Request req, SqlDb db, Connection conn, Set includes, Set excludes, String sql, List params) throws Exception
   {
      List list = new ArrayList();

      sql = parseSql(sql, chain, action, req, db, null, null);

      List<Row> rows = selectRows(chain, db, conn, sql, params);

      for (int i = 0; i < rows.size(); i++)
      {
         JSObject o = new JSObject();
         list.add(o);

         Row row = rows.get(i);
         for (String col : (Set<String>) row.keySet())
         {
            if (include(col, includes, excludes))
            {
               o.put(col, row.get(col));
            }
         }
      }
      return list;
   }

   public Rows selectRows(Chain chain, SqlDb db, Connection conn, String sql, Object... vals) throws Exception
   {
      if (chain.isDebug())
      {
         chain.getResponse().debug("\r\n" + sql);
         if (vals != null && vals.length > 0)
         {
            chain.getResponse().debug("SQL Params: " + new ArrayList(Arrays.asList(vals)));
         }
      }

      Rows rows = Sql.selectRows(conn, sql, vals);
      if (chain.get("rowCount") == null)
      {
         if (db.isType("mysql"))
         {
            sql = "SELECT FOUND_ROWS()";
         }
         else
         {
            sql = "SELECT count(*) " + sql.substring(sql.indexOf("FROM "), sql.length());
            if (sql.indexOf("LIMIT ") > 0)
               sql = sql.substring(0, sql.indexOf("LIMIT "));

            if (sql.indexOf("ORDER BY ") > 0)
               sql = sql.substring(0, sql.indexOf("ORDER BY "));
         }
         int found = Sql.selectInt(conn, sql, vals);

         if (chain.isDebug())
         {
            chain.getResponse().debug("", sql + " -> " + found);
         }

         chain.put("rowCount", found);

      }
      return rows;
   }

   List<JSObject> queryObjects(SqlQuery query, Service service, Chain chain, Action action, Request req, Response res, SqlDb db, Connection conn, Set includes, Set excludes, Set expands, String path, Collection collection, String inSql, List params) throws Exception
   {
      List<JSObject> results = new ArrayList();

      if (collection.getEntity().getKey() == null)
      {
         return results;
      }

      String keyCol = collection.getEntity().getKey().getColumn().getName();

      List<Row> rows = params != null && params.size() > 0 ? selectRows(chain, db, conn, inSql, params.toArray()) : selectRows(chain, db, conn, inSql);

      Attribute keyAttr = collection.getEntity().getKey();
      //Entity entity = collection.getEntity();

      MultiKeyMap pkCache = new MultiKeyMap();

      for (Row row : rows)
      {
         Object key = row.get(keyCol);

         JSObject js = new JSObject();
         results.add(js);

         pkCache.put(collection, key, js);

         for (String colName : (Set<String>) row.keySet())
         //for (Attribute attr : collection.getEntity().getAttributes())
         {
            //String attrName = attr.getName();
            //String colName = attr.getColumn().getName();
            //Object value = row.get(colName);
            Object value = row.get(colName);
            String attrName = colName;

            if (collection != null)
            {
               attrName = collection.getAttributeName(attrName);
            }

            if (colName.equalsIgnoreCase(keyAttr.getColumn().getName()))
            {
               String href = Service.buildLink(req, req.getCollectionKey(), value, null);
               if (include("href", includes, excludes, path))
               {
                  js.put("href", href);
               }
            }

            if (include(attrName, includes, excludes, path))
            {
               js.put(attrName, value);
            }
         }
      }

      expand(query, chain, conn, req.getApi(), collection, path, results, includes, excludes, expands, pkCache);

      return results;
   }

   protected List<JSObject> fetchObjects(Chain chain, Collection collection, java.util.Collection ids, Set includes, Set excludes, String path) throws Exception
   {
      if (ids.size() == 0)
         return Collections.EMPTY_LIST;

      String url = Service.buildLink(chain.getRequest(), collection.getName(), Sql.getInClauseStr(ids).replaceAll(" ", ""), null);

      //--
      //-- Nested param support
      Map<String, String> params = chain.getRequest().getParams();
      String lcPath = path.toLowerCase();
      for (String key : params.keySet())
      {
         String lcKey = key.toLowerCase();

         if (reservedParams.contains(lcKey))
            continue;

         if (lcKey.matches(".*\\b" + lcPath.replace(".", "\\.") + ".*"))
         {
            String value = params.get(key);
            lcKey = key.replaceAll("\\b" + (lcPath + "\\."), "");

            if (url.indexOf("?") < 0)
               url += "?";
            url += URLEncoder.encode(lcKey, "UTF-8");
            if (!Utils.empty(value))
               url += "=" + URLEncoder.encode(value, "UTF-8");
         }
      }

      Response res = chain.getService().include(chain, "GET", url, null);
      int sc = res.getStatusCode();
      if (sc == 401 || sc == 403)//unauthorized || forbidden
         return null;

      if (sc == 404)
         return Collections.EMPTY_LIST;

      if (sc == 500)
      {
         throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, res.getText());
      }

      if (sc == 200)
      {
         Object arr = res.getJson().get("data");
         if (arr instanceof JSArray)
         {
            List<JSObject> objs = ((JSArray) arr).asList();
            for (JSObject obj : objs)
            {
               for (String key : (Set<String>) obj.asMap().keySet())
               {
                  if (!include(key, includes, excludes, path))
                     obj.remove(key);
               }
            }

            return objs;
         }
      }

      throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Unknow repose code \"" + sc + "\" or body type from nested query.");
   }

   protected void expand(SqlQuery query, Chain chain, Connection conn, Api api, Collection collection, String path, List<JSObject> parentObjs, Set includes, Set excludes, Set expands, MultiKeyMap pkCache) throws Exception
   {
      if (parentObjs.size() == 0)
         return;

      for (Relationship rel : collection.getEntity().getRelationships())
      {
         Collection childCollection = api.getCollection(rel.getRelated().getTable());
         if (rel.isManyToMany())
            childCollection = api.getCollection(rel.getFkCol2().getPk().getTable());

         Column c = rel.getFkCol1();

         if (!include(rel.getName(), includes, excludes, path))
            continue;

         if (!expand(expands, path, rel))
         {
            for (JSObject js : parentObjs)
            {
               if (js.getProperty(rel.getName()) != null)
                  continue;

               String keyProp = collection.getEntity().getKey().getName();

               if (rel.isOneToMany())
               {
                  Object fk = js.get(rel.getFkCol1().getName());

                  if (fk != null)
                  {
                     String href = Service.buildLink(chain.getRequest(), childCollection.getName(), fk, null);
                     js.put(rel.getName(), new JSObject("href", href));
                  }
                  else
                  {
                     js.put(rel.getName(), null);
                  }
               }
               else
               {
                  Object key = js.get(keyProp);
                  String href = Service.buildLink(chain.getRequest(), chain.getRequest().getCollectionKey(), key, rel.getName());
                  js.put(rel.getName(), new JSObject("href", href));
               }
            }
         }
         else
         {
            if (rel.isOneToMany()) //ONE_TO_MANY - Player.locationId -> Location.id
            {
               String parentFkCol = rel.getFkCol1().getName();
               String childPkCol = rel.getFkCol1().getPk().getName();

               //find all the fks you need to query for
               List childIds = new ArrayList();
               for (JSObject parentObj : parentObjs)
               {
                  //TODO
                  Object childId = parentObj.get(parentFkCol);
                  if (childId != null && !pkCache.containsKey(childCollection, childId))
                     childIds.add(childId);
               }

               //now get them
               List<JSObject> childObjs = fetchObjects(chain, childCollection, childIds, includes, excludes, expandPath(path, rel.getName()));
               if (childObjs != null)
               {
                  for (JSObject childObj : childObjs)
                  {
                     Object childId = childObj.get(childPkCol);
                     if (!pkCache.containsKey(childCollection, childId))
                        pkCache.put(childCollection, childId, childObj);
                  }

                  //now hook the new fk objects back in
                  for (JSObject parentObj : parentObjs)
                  {
                     //TODO
                     Object childId = parentObj.get(parentFkCol);
                     if (childId != null)
                     {
                        JSObject childObj = (JSObject) pkCache.get(childCollection, childId);
                        if (childObj != null)
                        {
                           parentObj.put(rel.getName(), childObj);
                        }
                     }
                  }

                  expand(query, chain, conn, api, childCollection, expandPath(path, rel.getName()), childObjs, includes, excludes, expands, pkCache);
               }
            }
            else if (rel.isManyToOne()) //MANY_TO_ONE - Location.id <- Player.locationId
            {
               String relTbl = childCollection.getEntity().getTable().getName();

               String parentPkCol = rel.getFkCol1().getPk().getName();
               String childFkCol = rel.getFkCol1().getName();
               String childPkCol = childCollection.getEntity().getKey().getColumn().getName();

               List parentIds = new ArrayList();
               for (JSObject parentObj : parentObjs)
               {
                  parentIds.add(parentObj.get(parentPkCol));
                  if (!(parentObj.get(rel.getName()) instanceof JSArray))
                     parentObj.put(rel.getName(), new JSArray());
               }

               String sql = "";
               sql += " SELECT " + query.asCol(childPkCol) + " FROM " + query.asCol(relTbl);
               sql += " WHERE " + query.asCol(childFkCol) + " IN (" + Sql.getQuestionMarkStr(parentIds.size()) + ")";

               if (chain.getRequest().isDebug())
               {
                  chain.getResponse().debug(sql);
                  if (parentIds.size() > 0)
                  {
                     chain.getResponse().debug(parentIds);
                  }
               }

               List thoseIds = Sql.selectList(conn, sql, parentIds);

               List<JSObject> childObjs = fetchObjects(chain, childCollection, thoseIds, includes, excludes, expandPath(path, rel.getName()));

               if (childObjs != null)
               {
                  for (JSObject childObj : childObjs)
                  {
                     Object childId = childObj.get(childPkCol);
                     if (!pkCache.containsKey(childCollection, childId))
                        pkCache.put(childCollection, childId, childObj);

                     Object childFkVal = childObj.get(childFkCol);
                     if (childFkVal != null)
                     {
                        JSObject parentObj = (JSObject) pkCache.get(collection, childFkVal);
                        if (parentObj != null)
                        {
                           JSArray array = (JSArray) parentObj.get(rel.getName());
                           array.add(childObj);
                        }
                     }
                  }
                  expand(query, chain, conn, api, childCollection, expandPath(path, rel.getName()), childObjs, includes, excludes, expands, pkCache);
               }
            }
            else //many-to-many
            {
               //ex going from Category(id)->CategoryBooks(categoryId, bookId)->Book(id)

               String parentListProp = rel.getName();
               String parentPkCol = rel.getFkCol1().getPk().getName();
               String linkTbl = rel.getFkCol1().getTable().getName();
               String linkTblParentFkCol = rel.getFkCol1().getName();
               String linkTblChildFkCol = rel.getFkCol2().getName();
               String childPkCol = rel.getFkCol2().getPk().getName();

               List parentIds = new ArrayList();
               for (JSObject parentObj : parentObjs)
               {
                  parentIds.add(parentObj.get(parentPkCol));

                  if (!(parentObj.get(rel.getName()) instanceof JSArray))
                     parentObj.put(rel.getName(), new JSArray());
               }

               String sql = " SELECT " + query.asCol(linkTblParentFkCol) + ", " + query.asCol(linkTblChildFkCol) + //
                     " FROM " + query.asCol(linkTbl) + //
                     " WHERE " + query.asCol(linkTblChildFkCol) + " IS NOT NULL " + //
                     " AND " + query.asCol(linkTblParentFkCol) + " IN(" + Sql.getQuestionMarkStr(parentIds.size()) + ") ";

               if (chain.getRequest().isDebug())
               {
                  chain.getResponse().debug(sql);
                  if (parentIds.size() > 0)
                  {
                     chain.getResponse().debug(parentIds);
                  }
               }

               Rows childRows = Sql.selectRows(conn, sql, parentIds);

               ArrayListValuedHashMap parentLists = new ArrayListValuedHashMap();
               Set childIds = new HashSet();
               for (Row row : childRows)
               {
                  Object parentPk = row.get(linkTblParentFkCol);
                  Object childPk = row.get(linkTblChildFkCol);

                  parentLists.put(parentPk, childPk);
                  if (!pkCache.containsKey(childCollection, childPk))
                     childIds.add(childPk);
               }

               List<JSObject> childObjs = fetchObjects(chain, childCollection, childIds, includes, excludes, expandPath(path, rel.getName()));
               for (JSObject childObj : childObjs)
               {
                  Object childId = childObj.get(childPkCol);
                  if (!pkCache.containsKey(childCollection, childId))
                     pkCache.put(childCollection, childId, childObj);
               }

               for (Row childRow : childRows)
               {
                  JSObject parentObj = (JSObject) pkCache.get(collection, childRow.get(linkTblParentFkCol));
                  JSObject childObj = (JSObject) pkCache.get(childCollection, childRow.get(linkTblChildFkCol));

                  JSArray array = (JSArray) parentObj.get(parentListProp);
                  array.add(childObj);
               }

               expand(query, chain, conn, api, childCollection, expandPath(path, rel.getName()), childObjs, includes, excludes, expands, pkCache);
            }
         }
      }

      if (chain.getParent() == null)
      {
         for (Relationship rel : collection.getEntity().getRelationships())
         {
            if (rel.isOneToMany())
            {
               for (JSObject parentObj : parentObjs)
               {
                  String key = rel.getFkCol1().getName();
                  parentObj.remove(key);
               }
            }
         }
      }
   }

   public SqlGetAction withMaxRows(int maxRows)
   {
      this.maxRows = maxRows;
      return this;
   }

   static String expandPath(String path, Object next)
   {
      if (Utils.empty(path))
         return next + "";
      else
         return path + "." + next;
   }

   static boolean expand(Set<String> expands, String path, Relationship rel)
   {
      boolean expand = false;
      path = path.length() == 0 ? rel.getName() : path + "." + rel.getName();
      path = path.toLowerCase();

      for (String ep : expands)
      {
         if (ep.startsWith(path) && (ep.length() == path.length() || ep.charAt(path.length()) == '.'))
         {
            expand = true;
            break;
         }
      }

      return expand;
   }

   static boolean include(String attr, Set<String> includes, Set<String> excludes)
   {
      return include(attr, includes, excludes, null);
   }

   static boolean include(String attr, Set<String> includes, Set<String> excludes, String path)
   {
      if (includes.size() == 0 && excludes.size() == 0)
         return true;

      String key = (path != null && path.length() > 0 ? (path + "." + attr) : attr).toLowerCase();

      if (includes != null && includes.size() > 0)
      {
         return find(includes, key);
      }

      if (excludes != null && excludes.size() > 0 && find(excludes, key))
      {
         return false;
      }

      return true;
   }

   static boolean find(java.util.Collection<String> haystack, String needle)
   {
      //      if(needle.equalsIgnoreCase("adcompleters.ad"))
      //         System.out.println("asdf");
      String lc = needle.toLowerCase();
      if (haystack.contains(needle) || haystack.contains(lc))
         return true;

      for (String pattern : haystack)
      {
         pattern = pattern.toLowerCase();
         if (Utils.wildcardMatch(pattern, lc))
            return true;

         if (pattern.startsWith("*") || pattern.startsWith("."))
            if (Utils.wildcardMatch(pattern, "." + lc))
               return true;
      }
      return false;

   }

   public int getMaxRows()
   {
      return maxRows;
   }

   public void setMaxRows(int maxRows)
   {
      this.maxRows = maxRows;
   }

}
