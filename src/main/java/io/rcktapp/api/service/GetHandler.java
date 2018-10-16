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

import java.net.URLEncoder;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.forty11.j.J;
import io.forty11.j.utils.DoubleKeyMap;
import io.forty11.j.utils.ListMap;
import io.forty11.sql.Rows;
import io.forty11.sql.Rows.Row;
import io.forty11.sql.Sql;
import io.forty11.utils.CaseInsensitiveSet;
import io.forty11.web.js.JSArray;
import io.forty11.web.js.JSObject;
import io.rcktapp.api.Action;
import io.rcktapp.api.Api;
import io.rcktapp.api.ApiException;
import io.rcktapp.api.Attribute;
import io.rcktapp.api.Chain;
import io.rcktapp.api.Collection;
import io.rcktapp.api.Db;
import io.rcktapp.api.Endpoint;
import io.rcktapp.api.Entity;
import io.rcktapp.api.Relationship;
import io.rcktapp.api.Request;
import io.rcktapp.api.Response;
import io.rcktapp.api.SC;
import io.rcktapp.api.Table;
import io.rcktapp.rql.RQL;
import io.rcktapp.rql.Replacer;
import io.rcktapp.rql.Stmt;

/**
 * <h2>Reserved URL Parameters</h2>
 * <p>
 * If any of the following terms conflict with resource property names that
 * the application developer wants to query/filter on, the q/filter parameter
 * should be used to disambiguate filter terms from these reserved terms
 * <ul>
 *   <li>expand        - the "expands" parameter can be used to expand a 
 *                       relationship in a GET request. It can be a comma 
 *                       separated lists of dot separated relationship 
 *                       paths ex: "businessHours,prop1.prop2,dog.owner"
 *   
 *   <li>sort          - comma separated list of fields to sort on, each 
 *                       optionally prefixed with a "-" to indicate descending order
 *                       
 *   <li>q || filter   - optional parameter to add filter conditions.  Handy 
 *                       when resources names conflict with reserved words 
 *                      
 *   <li>offset
 *   
 *   <li>
 *    
 *   <li>page
 *   
 *   <li>limit    || 
 *       pageSize || 
 *       page_size ||
 *       perPage
 *       
 * </li>
 * 
 * <p><h2>Supported Filter Query Language</h2>
 * <ul>
 *   <li> = equality
 *   <li> * - Wildcard equivelant to sql "%"
 *   <li> =,eq,ne,lt,le,gt,ge,in,out,and,or,(,)
 * </ul>
 * 
 * 
 * <hr>------------------------------------------------------------------</hr>
 * 
 * <h2>Resources From Web</h2>
 * 
 * <p>Standard REST Protocols</p>
 * <ul>
 *   <li>OData - OASIS standard - http://www.odata.org/
 *       WDB 5/12/15 opinion - very complicated for client developers 
 * </ul>
 * 
 * <p>Patters for Resource Linking
 * <ul>
 *   <li>https://stormpath.com/blog/linking-and-resource-expansion-rest-api-tips/
 *   <li>http://docs.stormpath.com/rest/product-guide/#link-expansion
 * </ul>
 * 
 * <p>Query Language Alternatives
 * <ul>
 *   <li>RQL - Resource Query Language 
 *       <ul>
 *           <li>http://dundalek.com/rql/
 *           <li>https://doc.apsstandard.org/2.1/spec/rql/
 *           <li>https://www.sitepen.com/blog/2010/11/02/resource-query-language-a-query-language-for-the-web-nosql/
 *       </ul>
 *   
 *   <li>FIQL - Feed Item Query Language
 *       <ul>
 *           <li>http://tools.ietf.org/html/draft-nottingham-atompub-fiql-00
 *           <li>http://stackoverflow.com/questions/16371610/rest-rql-java-implementation
 *       </ul>
 * </ul>
 * 
 * <p>Patterns for Paging
 * <ul>
 *   <li>http://dev.billysbilling.com/blog/How-to-make-your-API-better-than-the-REST
 * </ul>
 * 
 * For examples... 
 * @see io.rcktapp.api.service.TestRql
 * 
 * @param request
 * @param response
 * @throws Exception
 */
public class GetHandler extends RqlHandler
{
   int maxRows        = 100;

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

      Db db = chain.getService().getDb(req.getApi(), req.getCollectionKey());
      RQL rql = makeRql(chain);

      conn = service.getConnection(chain);

      Collection collection = req.getCollectionKey() != null ? req.getApi().getCollection(req.getCollectionKey()) : null;
      Entity entity = collection != null ? collection.getEntity() : null;

      Table tbl = entity != null ? entity.getTable() : null;

      String sql = "";
      List params = new ArrayList();

      if (!J.empty(req.getCollectionKey()) && !J.empty(req.getEntityKey()) && !J.empty(req.getSubCollectionKey()))
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
                  String relTbl = rql.asCol(rel.getFkCol1().getTable().getName());
                  String relFk = rql.asCol(rel.getFkCol1().getName());

                  sql += " SELECT id FROM " + relTbl;
                  sql += " WHERE " + relFk + " = ? ";
               }
               else if (rel.isManyToMany())
               {
                  collection = req.getApi().getCollection(rel.getFkCol2().getPk().getTable());

                  String linkTblKey = rql.asCol(rel.getFkCol1().getName());
                  String linkTblFk = rql.asCol(rel.getFkCol2().getName());
                  String linkTbl = rql.asCol(rel.getFkCol1().getTable().getName());

                  String pkCol = linkTbl + "." + linkTblKey;
                  String fkCol = linkTbl + "." + linkTblFk;

                  sql += "\r\n SELECT " + fkCol;
                  sql += "\r\n FROM " + linkTbl;
                  sql += "\r\n WHERE " + fkCol + " IS NOT NULL AND " + pkCol + " =  ? ";
               }

               List ids = Sql.selectList(conn, sql, req.getEntityKey());

               String newUrl = Service.buildLink(req, collection.getName(), J.implode(",", ids.toArray()), null);

               String query = req.getQuery();
               if (!J.empty(query))
               {
                  newUrl += "?" + query;
               }

               Response included = service.include(chain, "GET", newUrl, null);

               res.setStatus(included.getStatus());
               res.setJson(included.getJson());

               return;
            }
         }
      }
      else if (entity != null && !J.empty(req.getCollectionKey()) && !J.empty(req.getEntityKey()))
      {
         String keyCol = entity.getKey().getName();

         //-- this is a request for one or more entities by ID
         //-- ${http://host/apipath}/collectionKey/entityKey[,entityKey2,entityKey3....,entityKeyN]

         String inClause = Sql.getInClauseStr(J.explode(",", Sql.check(req.getEntityKey())));

         sql += " SELECT * FROM " + rql.asCol(tbl.getName());
         sql += " WHERE " + Sql.check(keyCol) + " IN (" + inClause + ") ";
      }
      else if (tbl != null)
      {
         //-- this is a listing request
         //-- ${http://host/apipath}/collectionKey
         sql += " SELECT * FROM " + rql.asCol(tbl.getName());
      }

      //-- support for custom sql statements from a Rule
      //--

      String passedInSelect = (String) chain.remove("select");
      if (!J.empty(passedInSelect))
      {
         sql = passedInSelect;
      }

      if (J.empty(sql))
      {
         throw new ApiException(SC.SC_404_NOT_FOUND, "Unable to map request to a db table or query. Please check your endpoint.");
      }

      Replacer replacer = new Replacer();

      Map rqlParams = req.getParams();

      Stmt stmt = rql.toSql(sql, collection != null ? collection.getEntity().getTable() : null, rqlParams, replacer);
      stmt.setMaxRows(maxRows); //this is a default value

      sql = stmt.toSql();

      if (includes.size() > 0)
      {
         includes = new CaseInsensitiveSet<String>(stmt.cols.keySet());
      }

      for (int i = 0; i < replacer.cols.size(); i++)
      {
         String col = replacer.cols.get(i);
         String val = replacer.vals.get(i);

         params.add(cast(collection, col, val));
      }

      //-- end SQL construction for the primary query
      //--
      //--

      List<JSObject> results = null;

      if (collection != null && collection.getEntity().getKey() != null)
      {
         results = queryObjects(rql, service, chain, action, req, res, db, conn, includes, excludes, expands, "", collection, sql, params);
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
         meta.put("pageNum", stmt.pagenum);
         meta.put("pageSize", stmt.limit + "");
         int pages = (int) Math.ceil((double) rowCount / (double) stmt.limit);
         meta.put("pageCount", pages);

         for (JSObject js : results)
         {
            data.add(js);
         }
      }

   }

   List getRows(Chain chain, Action action, Request req, Db db, Connection conn, Set includes, Set excludes, String sql, List params) throws Exception
   {
      List list = new ArrayList();

      sql = parseSql(sql, chain, action, req, db, null, null);

      List<Row> rows = selectRows(chain, conn, sql, params);

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

   public Rows selectRows(Chain chain, Connection conn, String sql, Object... vals) throws Exception
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
         sql = "SELECT FOUND_ROWS()";
         //TODO "SELECT FOUND_ROWS() is MySQL specific
         //         if(!mysql)
         //         {
         //            sql = "SELECT count(*) " + sql.substring(sql.indexOf("FROM "), sql.length());
         //            if (sql.indexOf("LIMIT ") > 0)
         //               sql = sql.substring(0, sql.indexOf("LIMIT "));
         //
         //            if (sql.indexOf("ORDER BY ") > 0)
         //               sql = sql.substring(0, sql.indexOf("ORDER BY "));   
         //         }

         int found = Sql.selectInt(conn, sql);

         if (chain.isDebug())
         {
            chain.getResponse().debug("", sql + " -> " + found);
         }

         chain.put("rowCount", found);

      }
      return rows;
   }

   List<JSObject> queryObjects(RQL rql, Service service, Chain chain, Action action, Request req, Response res, Db db, Connection conn, Set includes, Set excludes, Set expands, String path, Collection collection, String inSql, List params) throws Exception
   {
      List<JSObject> results = new ArrayList();

      if (collection.getEntity().getKey() == null)
      {
         return results;
      }

      String keyCol = collection.getEntity().getKey().getColumn().getName();

      List<Row> rows = params != null && params.size() > 0 ? selectRows(chain, conn, inSql, params.toArray()) : selectRows(chain, conn, inSql);

      Attribute keyAttr = collection.getEntity().getKey();
      //Entity entity = collection.getEntity();

      DoubleKeyMap pkCache = new DoubleKeyMap();

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

      expand(rql, chain, conn, req.getApi(), collection, path, results, includes, excludes, expands, pkCache);

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
            if (!J.empty(value))
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

   protected void expand(RQL rql, Chain chain, Connection conn, Api api, Collection collection, String path, List<JSObject> parentObjs, Set includes, Set excludes, Set expands, DoubleKeyMap pkCache) throws Exception
   {
      if (parentObjs.size() == 0)
         return;

      for (Relationship rel : collection.getEntity().getRelationships())
      {
         Collection childCollection = api.getCollection(rel.getRelated().getTable());
         if (rel.isManyToMany())
            childCollection = api.getCollection(rel.getFkCol2().getPk().getTable());

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

                  expand(rql, chain, conn, api, childCollection, expandPath(path, rel.getName()), childObjs, includes, excludes, expands, pkCache);
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
               sql += " SELECT " + rql.asCol(childPkCol) + " FROM " + rql.asCol(relTbl);
               sql += " WHERE " + rql.asCol(childFkCol) + " IN (" + Sql.getQuestionMarkStr(parentIds.size()) + ")";

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
                  expand(rql, chain, conn, api, childCollection, expandPath(path, rel.getName()), childObjs, includes, excludes, expands, pkCache);
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

               String sql = " SELECT " + rql.asCol(linkTblParentFkCol) + ", " + rql.asCol(linkTblChildFkCol) + //
                     " FROM " + rql.asCol(linkTbl) + //
                     " WHERE " + rql.asCol(linkTblChildFkCol) + " IS NOT NULL " + //
                     " AND " + rql.asCol(linkTblParentFkCol) + " IN(" + Sql.getQuestionMarkStr(parentIds.size()) + ") ";

               if (chain.getRequest().isDebug())
               {
                  chain.getResponse().debug(sql);
                  if (parentIds.size() > 0)
                  {
                     chain.getResponse().debug(parentIds);
                  }
               }

               Rows childRows = Sql.selectRows(conn, sql, parentIds);

               ListMap parentLists = new ListMap();
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

               expand(rql, chain, conn, api, childCollection, expandPath(path, rel.getName()), childObjs, includes, excludes, expands, pkCache);
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

   static String expandPath(String path, Object next)
   {
      if (J.empty(path))
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
         if (J.wildcardMatch(pattern, lc))
            return true;

         if (pattern.startsWith("*") || pattern.startsWith("."))
            if (J.wildcardMatch(pattern, "." + lc))
               return true;
      }
      return false;

   }

}
