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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.rcktapp.api.service;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.forty11.j.J;
import io.forty11.js.JSArray;
import io.forty11.js.JSObject;
import io.forty11.sql.Rows.Row;
import io.forty11.sql.Sql;
import io.forty11.utils.CaseInsensitiveSet;
import io.forty11.utils.DoubleKeyListMap;
import io.forty11.utils.DoubleKeyMap;
import io.rcktapp.api.ApiException;
import io.rcktapp.api.Attribute;
import io.rcktapp.api.Chain;
import io.rcktapp.api.Col;
import io.rcktapp.api.Collection;
import io.rcktapp.api.Entity;
import io.rcktapp.api.Handler;
import io.rcktapp.api.Relationship;
import io.rcktapp.api.Request;
import io.rcktapp.api.Response;
import io.rcktapp.api.Rule;
import io.rcktapp.api.SC;
import io.rcktapp.api.Tbl;
import io.rcktapp.api.service.RQL.Replacer;
import io.rcktapp.api.service.RQL.Stmt;

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
 * 
 *
 * @param request
 * @param response
 * @throws Exception
 */
public class GetHandler implements Handler
{
   int MAX_RESULTS = 1000;

   @Override
   public void service(Service service, Chain chain, Rule rule, Request req, Response res) throws Exception
   {
      Connection conn = null;
      try
      {
         Set expands = splitParam(req, "expands");
         Set includes = new LinkedHashSet();
         for (String include : splitParam(req, "includes"))
         {
            if (include.startsWith("'") && include.endsWith("'"))
               include = include.substring(1, include.length() - 1);

            if (include.startsWith("\"") && include.endsWith("\""))
               include = include.substring(1, include.length() - 1);

            includes.add(include);
         }

         Set excludes = splitParam(req, "excludes");

         conn = ((Snooze) service).getConnection(req.getApi(), req.getCollectionKey());

         Collection collection = req.getCollectionKey() != null ? req.getApi().getCollection(req.getCollectionKey()) : null;
         Entity entity = collection != null ? collection.getEntity() : null;

         Tbl tbl = entity != null ? entity.getTbl() : null;

         boolean listing = true;
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
                     String relTbl = rel.getFkCol1().getTbl().getName();
                     String relFk = rel.getFkCol1().getName();

                     sql += " SELECT * FROM " + relTbl;
                     sql += " WHERE " + relFk + " = ? ";

                     params.add(req.getEntityKey());
                  }
                  else if (rel.isManyToMany())
                  {
                     collection = req.getApi().getCollection(rel.getFkCol2().getPk().getTbl());

                     String targetTbl = rel.getFkCol2().getPk().getTbl().getName();
                     String targetTblKey = rel.getFkCol2().getPk().getName();

                     String linkTblFk = rel.getFkCol2().getName();
                     String linkTblKey = rel.getFkCol1().getName();
                     String linkTbl = rel.getFkCol1().getTbl().getName();

                     sql += "\r\n SELECT " + targetTbl + ".* FROM " + targetTbl;
                     sql += "\r\n  JOIN " + linkTbl + " ON " + linkTbl + "." + linkTblFk + " = " + targetTbl + "." + targetTblKey;
                     sql += "\r\n WHERE " + linkTbl + "." + linkTblKey + " =  ? ";

                     params.add(req.getEntityKey());
                  }

                  break;
               }
            }
         }
         else if (entity != null && !J.empty(req.getCollectionKey()) && !J.empty(req.getEntityKey()))
         {
            String keyCol = entity.getKey().getName();

            //-- this is a single entity request of the for
            //-- ${http://host/apipath}/collectionKey/entityKey

            sql += " SELECT * FROM " + Sql.check(tbl.getName());

            if (req.getEntityKey().indexOf(',') > 0)
            {
               Sql.check(req.getEntityKey()); //SQL injection check
               String[] ids = req.getEntityKey().split(",");
               String inClause = Sql.getInClauseStr(Arrays.asList(ids));

               sql += " WHERE " + Sql.check(keyCol) + " IN (" + inClause + ") ";
            }
            else
            {
               //this is the only time we are going after a single resource
               listing = false;

               sql += " WHERE " + Sql.check(keyCol) + " = ? ";
               params.add(req.getEntityKey());
            }
         }
         else if (tbl != null)
         {
            //-- this is a listing request
            //-- ${http://host/apipath}/collectionKey

            sql += " SELECT * FROM " + Sql.check(tbl.getName());

         }

         //         if ("true".equalsIgnoreCase(req.getParam("distinct")) && includes.size() > 0)
         //         {
         //            String sel = " SELECT DISTINCT ";
         //            for (Object field : includes)
         //            {
         //               sel += " " + field + ",";
         //            }
         //            sel = sel.substring(0, sel.length() - 1) + " ";
         //
         //            sql = sql.replaceFirst("SELECT \\* ", sel);
         //         }

         Collection reqCol = collection;

         //-- support for custom sql statements from a Rule
         //--
         JSObject config0 = rule.getConfig();

         String passedInSelect = (String) chain.get("select");
         if (!J.empty(passedInSelect))
         {
            sql = passedInSelect;
         }

         Replacer r = new Replacer();
         Stmt stmt = RQL.toSql(sql, req.getParams(), r);
         sql = stmt.sql;
         if (includes.size() > 0)
         {
            includes = new CaseInsensitiveSet<String>(stmt.cols.keySet());
         }

         for (int i = 0; i < r.cols.size(); i++)
         {
            String col = r.cols.get(i);
            String val = r.vals.get(i);

            params.add(convert(collection, col, val));
         }

         //-- end SQL construction for the primary query
         //--
         //--

         LinkedHashMap results = null;

         if (collection != null)
         {
            results = getObjects(conn, req, res, new DoubleKeyMap(), new DoubleKeyListMap(), new DoubleKeyListMap(), includes, excludes, expands, "", collection, sql, null, params);
         }
         else
         {
            results = getRows(conn, req, res, includes, excludes, sql, params);
         }

         if (results.size() == 0 && req.getEntityKey() != null && req.getCollectionKey() == null)
         {
            res.setStatus(SC.SC_404_NOT_FOUND);
            res.setJson(null);
         }
         else
         {
            if (listing)
            {
               JSArray arr = new JSArray();
               for (Object key : results.keySet())
               {
                  JSObject obj = (JSObject) results.get(key);
                  arr.add(obj);
               }
               res.setJson(arr);

               if (stmt.pagenum >= 0 || stmt.rowcount != null)
               {
                  //                  sql = "SELECT count(*) " + sql.substring(sql.indexOf("FROM "), sql.length());
                  //                  if (sql.indexOf("LIMIT ") > 0)
                  //                     sql = sql.substring(0, sql.indexOf("LIMIT "));
                  //
                  //                  if (sql.indexOf("ORDER BY ") > 0)
                  //                     sql = sql.substring(0, sql.indexOf("ORDER BY "));

                  int count = Sql.selectInt(conn, "SELECT FOUND_ROWS()");

                  JSObject wrapper = new JSObject();
                  wrapper.put(stmt.rowcount != null ? stmt.rowcount : "rowCount", count);
                  
                  if(stmt.pagenum > 0)
                  {
                     wrapper.put("pageNum", stmt.pagenum);
                     wrapper.put("pageSize", stmt.limit + "");
                     int pages = (int) Math.ceil((double) count / (double) stmt.limit);
                     wrapper.put("pageCount", pages);
                     wrapper.put("data", arr);
                  }
                  res.setJson(wrapper);
               }

            }
            else if (results.size() == 1)
            {
               for (Object key : results.keySet())
               {
                  res.setJson((JSObject) results.get(key));
                  break;
               }
            }
            else
            {
               throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "This request has multiple results but only a single object was expected");
            }
         }
      }
      finally
      {
         Sql.close(conn);
      }
   }

   static LinkedHashMap getRows(Connection conn, Request req, Response res, Set includes, Set excludes, String sql, List params) throws Exception
   {
      LinkedHashMap map = new LinkedHashMap();
      List<Row> rows = Sql.selectRows(conn, sql, params);

      for (int i = 0; i < rows.size(); i++)
      {
         JSObject o = new JSObject();
         map.put(i + 1, o);

         Row row = rows.get(i);
         for (String col : (Set<String>) row.keySet())
         {
            if (include(col, includes, excludes))
            {
               o.put(col, row.get(col));
            }
         }
      }
      return map;
   }

   /**
    * Going to have to keep a traversed map.  If expand is true but we have already
    * traversed an item, then we won't re-expand, only print the href link
    *
    * TODO: this would be much more efficient if you took an a list and queried
    * for the related children all at the same time.
    */
   static LinkedHashMap getObjects(Connection conn, Request req, Response res, DoubleKeyMap jsonPKIdnetityCache, DoubleKeyListMap jsonFKListCache, DoubleKeyListMap visitedKeysCache, Set includes, Set excludes, Set expands, String path, Collection collection, String inSql, String mtmCacheKey, List params) throws Exception
   {

      LinkedHashMap results = new LinkedHashMap();

      if (collection.getEntity().getKey() == null)
      {
         return results;
      }

      String keyCol = collection.getEntity().getKey().getCol().getName();

      if (params != null)
      {
         log(inSql + " - " + params);
      }
      else
      {
         log(inSql);
      }

      List<Row> rows = params != null && params.size() > 0 ? Sql.selectRows(conn, inSql, params.toArray()) : Sql.selectRows(conn, inSql);
      List<Row> newRows = new ArrayList();

      for (Row row : rows)
      {
         Object key = row.get(keyCol);
         if (jsonPKIdnetityCache.containsKey(collection, key))
         {
            results.put(row, jsonPKIdnetityCache.get(collection, key));
         }
         else
         {
            results.put(row, new JSObject());
            newRows.add(row);
         }
      }

      Attribute keyAttr = collection.getEntity().getKey();
      Entity entity = collection.getEntity();

      for (Row row : newRows)
      {
         Object key = row.get(keyCol);

         JSObject js = (JSObject) results.get(row);
         jsonPKIdnetityCache.put(collection, key, js);

         for (String attrName : (Set<String>) row.keySet())//Attribute attr : collection.getEntity().getAttributes())
         {
            Object value = row.get(attrName);

            //TODO: update returned property names to match collection attribute names
            //THESE WILL ALWAYS BE THE SAME UNTIL API PERSISTANCE & MODIFICATION IS IMPLEMENTED

            //            String attrName = attr.getName();
            //            String colName = attr.getCol().getName();

            if (include(attrName, includes, excludes, path))
            {
               js.put(attrName, value);
            }

            if (attrName.equalsIgnoreCase(keyAttr.getCol().getName()))
            {
               if (include("href", includes, excludes, path))
               {
                  String href = Service.buildLink(req, collection.getName(), value, null);
                  js.put("href", href);
               }
            }
         }

         //-- Create a foreign key cache for MANY_TO_ONE lookups by 
         //-- aggregating the reciprocal ONE_TO_MANTY relationships
         //--
         //-- The values added to this cache will be used by 
         //-- PREVIOUS RECURSIVE GENERATIONS to build the list of 
         //-- related entities
         for (Relationship rel : collection.getEntity().getRelationships())
         {
            if (rel.isOneToMany())
            {
               //-- if this were a column such as "Category.parentId"
               //-- this will collect the "child categories" list
               //-- for each Category.id
               Col fkCol = rel.getFkCol1();
               Object fkVal = row.get(rel.getFkCol1().getName());
               if (fkVal != null)
               {
                  jsonFKListCache.put(fkCol, fkVal, js);
               }
            }
         }

         //-- MANY_TO_MANY that use a relationship table don't 
         //-- have the foreign key value in an entity recored
         //-- to do the "reverse cache building" like is done
         //-- above for MANY_TO_ONE.  Instead the SQL retrieves
         //-- appends a single extra meta column to the results
         //-- that is the FK of the previous recursive generation row.
         if (mtmCacheKey != null)
         {
            //log("MTM Caching: " + mtmCacheKey + " - " + key + " - " + js);
            Object key1Val = row.get(mtmCacheKey);
            jsonFKListCache.put(mtmCacheKey, key1Val, js);
         }

      }

      for (Relationship rel : collection.getEntity().getRelationships())
      {
         Collection relatedCollection = req.getApi().getCollection(rel.getRelated().getTbl());
         if (rel.isManyToMany())
            relatedCollection = req.getApi().getCollection(rel.getFkCol2().getPk().getTbl());

         if (!include(rel.getName(), includes, excludes, path))
            continue;

         if (!expand(expands, path, rel))
         {
            for (Map row : rows)
            {
               JSObject js = (JSObject) results.get(row);
               if (js.getProperty(rel.getName()) != null)
                  continue;

               if (rel.isManyToOne())
               {
                  Object key = row.get(keyCol);
                  String href = Service.buildLink(req, collection.getName(), key, rel.getName());
                  //js.put(rel.getName(), new JSObject("@noexpand-many-to-one:" + path, href));
                  js.put(rel.getName(), new JSObject("href" + path, href));
               }
               else if (rel.isManyToMany())
               {
                  Object key = row.get(keyCol);
                  String href = Service.buildLink(req, collection.getName(), key, rel.getName());
                  //js.put(rel.getName(), new JSObject("@noexpand-many-to-many:" + path, href));
                  js.put(rel.getName(), new JSObject("href" + path, href));
               }
               else if (rel.isOneToMany())
               {
                  Object fk = row.get(rel.getFkCol1().getName());

                  if (fk != null)
                  {
                     JSObject relatedJs = (JSObject) jsonPKIdnetityCache.get(relatedCollection, fk);
                     if (relatedJs != null)
                     {
                        js.put(rel.getName(), relatedJs);
                     }
                     else
                     {
                        String href = Service.buildLink(req, relatedCollection.getName(), fk, null);
                        //js.put(rel.getName(), new JSObject("@noexpand-one-to-many:" + path, href));
                        js.put(rel.getName(), new JSObject("href" + path, href));
                     }
                  }
               }
            }
         }
         else
         {
            if (!rel.isManyToMany())
            {
               LinkedHashSet keys = new LinkedHashSet();

               String ownKeyCol = null;
               String relKeyCol = null;
               String relTbl = null;

               if (rel.isManyToOne())
               {
                  ownKeyCol = rel.getFkCol1().getPk().getName();
                  relKeyCol = rel.getFkCol1().getName();
                  relTbl = rel.getFkCol1().getTbl().getName();
               }
               else
               //ONE_TO_MANY
               {
                  ownKeyCol = rel.getFkCol1().getName();
                  relKeyCol = rel.getFkCol1().getPk().getName();
                  relTbl = rel.getFkCol1().getPk().getTbl().getName();
               }

               for (Map row : rows)
               {
                  Object pk = row.get(ownKeyCol);
                  if (pk != null)
                     keys.add(pk);
               }

               List visitedPks = visitedKeysCache.get(relTbl, relKeyCol);
               List notVisited = new ArrayList(keys);
               notVisited.removeAll(visitedPks);
               Collections.sort(notVisited);
               for (Object pk : notVisited)
               {
                  visitedKeysCache.put(relTbl, relKeyCol, pk);
               }

               if (notVisited.size() > 0)
               {
                  String sql = "";
                  sql += " SELECT * FROM " + relTbl;
                  sql += " WHERE " + relKeyCol + " IN (" + Sql.getQuestionMarkStr(notVisited.size()) + ")";

                  //-- this call will load the caches, the
                  //-- return values are not considered.
                  String nextPath = expandPath(path, rel.getName());
                  getObjects(conn, req, res, jsonPKIdnetityCache, jsonFKListCache, visitedKeysCache, includes, excludes, expands, nextPath, relatedCollection, sql, null, notVisited);
               }

               for (Map row : newRows)
               {
                  JSObject thisJs = (JSObject) results.get(row);

                  if (thisJs.getProperty(rel.getName()) != null)
                     continue;

                  JSObject valueJs = null;

                  if (rel.isManyToOne())
                  {
                     List<JSObject> relatedJs = jsonFKListCache.get(rel.getFkCol1(), row.get(ownKeyCol));
                     JSArray jsArr = new JSArray();
                     for (JSObject relObj : relatedJs)
                     {
                        jsArr.add(relObj);
                     }
                     valueJs = jsArr;
                  }
                  else
                  {
                     valueJs = (JSObject) jsonPKIdnetityCache.get(relatedCollection, row.get(ownKeyCol));
                  }

                  thisJs.put(rel.getName(), valueJs);

               }
            }
            else
            {
               String ownPkCol = rel.getFkCol1().getPk().getName();
               String linkTbl = rel.getFkCol1().getTbl().getName();
               String linkTblRelFk = rel.getFkCol2().getName();
               String relTbl = rel.getFkCol2().getPk().getTbl().getName();
               String relPkCol = rel.getFkCol2().getPk().getName();

               String cacheKey = "__MTM_" + linkTbl + "." + rel.getFkCol1().getName();

               String sql = "";
               sql += " SELECT " + relTbl + ".*, " + linkTbl + "." + rel.getFkCol1().getName() + " AS '" + cacheKey + "' FROM " + relTbl;
               sql += "\r\n  JOIN " + linkTbl + " ON " + linkTbl + "." + linkTblRelFk + " = " + relTbl + "." + relPkCol;
               sql += "\r\n WHERE " + linkTbl + "." + rel.getFkCol1().getName() + " IN ";

               List keys = new ArrayList();

               for (Map row : rows)
               {
                  Object pk = row.get(ownPkCol);
                  if (pk != null)
                     keys.add(pk);
               }
               Collections.sort(keys);

               sql += "(" + Sql.getInClauseStr(keys) + ")";

               //-- this call will load the caches, the
               //-- return values are not considered.
               String nextPath = expandPath(path, rel.getName());
               getObjects(conn, req, res, jsonPKIdnetityCache, jsonFKListCache, visitedKeysCache, includes, excludes, expands, nextPath, relatedCollection, sql, cacheKey, null);

               for (Map row : newRows)
               {
                  JSObject thisJs = (JSObject) results.get(row);

                  if (thisJs.getProperty(rel.getName()) != null)
                     continue;

                  List<JSObject> relatedJs = jsonFKListCache.get(cacheKey, row.get(ownPkCol));

                  //log("MTM cache found:" + cacheKey + " - " + row.get(ownPkCol) + " - " + relatedJs);

                  if (relatedJs != null)
                  {
                     JSArray jsArr = new JSArray();
                     for (JSObject relObj : relatedJs)
                     {
                        jsArr.add(relObj);
                     }
                     thisJs.put(rel.getName(), jsArr);
                  }
               }
            }
         }
      }

      return results;
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
         if (ep.startsWith(path))
         {
            expand = true;
            break;
         }
      }

      return expand;
   }

   static boolean include(String attr, Set includes, Set excludes)
   {
      return include(attr, includes, excludes, null);
   }

   static boolean include(String attr, Set includes, Set excludes, String path)
   {
      if (includes.size() == 0 && excludes.size() == 0)
         return true;

      String key = (path != null && path.length() > 0 ? (path + "." + attr) : attr).toLowerCase();

      if (includes != null && includes.size() > 0 && !includes.contains(key))
         return false;

      if (excludes != null && excludes.contains(key))
         return false;

      return true;
   }

   public LinkedHashSet<String> splitParam(Request req, String key)
   {
      LinkedHashSet map = new LinkedHashSet();
      String param = req.getParam(key);
      if (!J.empty(param))
      {
         String[] arr = param.split(",");
         for (String e : arr)
         {
            e = e.trim().toLowerCase();
            if (!J.empty(e))
               map.add(e);
         }
      }

      return map;
   }

   Object convert(Collection coll, String col, Object val)
   {
      String type = null;

      if (coll != null && coll.getEntity().getTbl().getCol(col) != null)
      {
         try
         {
            type = coll.getEntity().getTbl().getCol(col).getType();
         }
         catch (Exception ex)
         {
            ex.printStackTrace();
         }
      }

      if (type == null && (col.endsWith("date") || col.endsWith("at")))
      {
         type = "date";
      }

      if (type == null && ("true".equalsIgnoreCase(val + "") || "false".equalsIgnoreCase(val + "")))
      {
         type = "boolean";
      }

      if (type != null)
      {
         type = type.toLowerCase();

         if (type.indexOf("string") >= 0 || type.indexOf("char") >= 0)
         {
            //do nothing
         }
         else if (type.indexOf("date") >= 0)
         {
            val = J.date(val + "");
         }
         else if (type.indexOf("timestamp") >= 0)
         {
            val = new Timestamp(J.date(val + "").getTime());;
         }
         else if (type.indexOf("bool") > 0)
         {
            val = Boolean.parseBoolean(val + "");
         }
         else if (type.indexOf("integ") > 0)
         {
            val = Integer.parseInt(val + "");
         }
         else if (type.indexOf("float") > 0)
         {
            val = Float.parseFloat(val + "");
         }
      }

      if (val instanceof String)
      {
         String str = (String) val;
         if (str.startsWith("'") && str.endsWith("'") && str.length() > 1)
            str = str.substring(1, str.length() - 1);
         val = str;
      }

      return val;
   }

   static void log(Object... msg)
   {
      for (Object o : msg)
      {
         System.out.println(o);
      }
      System.out.println(" ");
   }
}
