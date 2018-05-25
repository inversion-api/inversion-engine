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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.rcktapp.api.Api;
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
import io.forty11.j.J;
import io.forty11.js.JSArray;
import io.forty11.js.JSObject;
import io.forty11.js.JSObject.Property;
import io.forty11.sql.Sql;
import io.forty11.utils.DoubleKeyListMap;
import io.forty11.utils.ListMap;

public class PostHandler implements Handler
{

   @Override
   public void service(Service service, Chain chain, Rule rule, Request req, Response res) throws Exception
   {
      if (req.isPost() && req.getEntityKey() != null)
      {
         res.setStatus(SC.SC_404_NOT_FOUND);
         return;
      }
      //      else if(req.isPut())
      //      {
      //         if (req.getEntityKey() == null)
      //         {
      //            res.setStatus(SC.SC_404_NOT_FOUND);
      //         }
      //         else if (subCollectionKey != null)
      //         {
      //            res.setStatus(SC.SC_404_NOT_FOUND);
      //         }
      //      }

      Connection conn = null;
      try
      {
         conn = ((Snooze) service).getConnection(req.getApi(), req.getCollectionKey());

         List<String> hrefs = new ArrayList();

         Collection collection = req.getApi().getCollection(req.getCollectionKey());
         Entity entity = collection.getEntity();

         JSObject obj = req.getJson();

         boolean wasAutoCommit = conn.getAutoCommit();
         if (wasAutoCommit)
            conn.setAutoCommit(false);

         try
         {
            if (obj instanceof JSArray)
            {

               JSArray arrRes = new JSArray();
               res.setJson(arrRes);
               for (JSObject child : (List<JSObject>) ((JSArray) obj).getObjects())
               {
                  String href = store(conn, req, entity, child);
                  hrefs.add(href);

                  JSObject js = new JSObject();
                  js.put("href", href);

                  arrRes.add(js);
               }
            }
            else
            {
               String href = store(conn, req, entity, obj);
               res.getJson().put("href", href);

               hrefs.add(href);
            }

            conn.commit();
         }
         catch (Exception ex)
         {
            conn.rollback();
            J.rethrow(ex);
         }
         finally
         {
            if (wasAutoCommit)
               conn.setAutoCommit(true);
         }

         //-- take all of the hrefs and combine into a 
         //-- single href for the "Location" header
         res.setStatus(SC.SC_201_CREATED);
         StringBuffer buff = new StringBuffer(hrefs.get(0));
         for (int i = 1; i < hrefs.size(); i++)
         {
            String nextId = hrefs.get(i);
            nextId = nextId.substring(nextId.lastIndexOf("/") + 1, nextId.length());
            buff.append(",").append(nextId);
         }

         res.addHeader("Location", buff.toString());
      }
      finally
      {
         Sql.close(conn);
      }

   }

   String store(Connection conn, Request req, Entity entity, JSObject obj) throws Exception
   {
      Collection collection = entity.getCollection();
      Api api = collection.getApi();

      Object key = storeEntity(conn, req, entity, obj);

      storeManyToOne(conn, req, key, entity, obj);

      makeManyToManyRows(conn, req, key, entity, obj);

      String href = req.getApiUrl() + collection.getName() + "/" + key;

      return href;
   }

   void storeManyToOne(Connection conn, Request req, Object newPk, Entity entity, JSObject obj) throws Exception
   {
      DoubleKeyListMap relateds = new DoubleKeyListMap();
      Map<String, String> cols = new HashMap();

      List mtmRows = new ArrayList();
      for (Relationship rel : entity.getRelationships())
      {
         if (rel.getType().equals("MANY_TO_ONE"))
         {
            Tbl tbl = rel.getFkCol1().getTbl();

            Object temp = obj.get(rel.getName());
            if (!(temp instanceof JSArray))
               continue;

            JSArray related = (JSArray) temp;
            if (related == null)
               continue;

            for (JSObject o : ((List<JSObject>) related.getObjects()))
            {
               if (o.keys().size() == 0)
                  continue;

               if (!cols.containsKey(tbl.getName()))
               {
                  cols.put(tbl.getName(), rel.getFkCol1().getName());
               }

               String relName = rel.getName();
               String fk = rel.getFkCol1().getName();
               if (o.getProperty(fk) == null)
                  ((JSObject) o).put(fk, newPk);

               Entity e = entity.getCollection().getApi().getCollection(tbl).getEntity();
               String id = store(conn, req, e, (JSObject) o);

               id = id.substring(id.lastIndexOf("/") + 1, id.length());
               relateds.put(tbl.getName(), newPk, id);
            }
         }
      }

      //Now previously related objects NOT in 
      //the supplied list must be unlinked
      for (Object table : relateds.keySet())
      {
         ListMap rels = relateds.get(table);

         for (Object id : rels.keySet())
         {
            List relIds = rels.get(id);

            //TODO: this should be a delete if the fk field is not nullable 
            //or is a cascading delete
            String sql = "";
            sql += " UPDATE `" + table + "` ";
            sql += " SET " + cols.get(table) + " = NULL ";
            sql += " WHERE " + cols.get(table) + " = " + id;
            sql += " AND id NOT IN (" + Sql.getInClauseStr(relIds) + ")";

            Sql.execute(conn, sql);
         }
      }
   }

   void makeManyToManyRows(Connection conn, Request req, Object newPk, Entity entity, JSObject obj) throws Exception
   {
      String sql = null;
      try
      {
         List<Object[]> mtmRows = new ArrayList();
         for (Relationship rel : entity.getRelationships())
         {
            if (rel.getType().equals("MANY_TO_MANY"))
            {
               Object fk2PropVal = obj.get(rel.getName());

               if (fk2PropVal == null)
                  continue;

               List fk2Vals = new ArrayList();

               //TODO: PUT child objects that have more than an href property

               //TODO: IMPORTANT - must delete M2M relationships that are not present here

               if (fk2PropVal instanceof JSArray)
               {
                  JSArray arr = (JSArray) fk2PropVal;
                  for (JSObject jso : (List<JSObject>) arr.getObjects())
                  {
                     if (jso instanceof JSArray)
                        throw new ApiException(SC.SC_400_BAD_REQUEST, "Found a array but was expecting an object for property '" + rel.getName() + "'");

                     if (jso.keys().size() == 0) //ignore an empty object
                        continue;

                     String id = (String) jso.get("href");
                     if (id == null)
                     {
                        Tbl tbl = rel.getFkCol2().getPk().getTbl();
                        Entity e = entity.getCollection().getApi().getCollection(tbl).getEntity();
                        id = store(conn, req, e, (JSObject) jso);
                     }

                     id = id.substring(id.lastIndexOf("/") + 1, id.length());

                     fk2Vals.add(id);
                  }
               }
               //            else if (!J.empty(fk2PropVal))
               //            {
               //               String str = fk2PropVal.toString();
               //               str = str.substring(str.lastIndexOf("/") + 1, str.length());
               //               fk2Vals = Arrays.asList(str.split(","));
               //            }

               for (Object fk : fk2Vals)
               {
                  Map row = new HashMap();
                  row.put(rel.getFkCol1().getName(), newPk);
                  row.put(rel.getFkCol2().getName(), fk);

                  mtmRows.add(new Object[]{rel.getFkCol1().getTbl().getName(), row});
               }
            }
         }

         ListMap<String, Map> mtms = new ListMap();
         for (Object[] mtmRel : mtmRows)
         {
            mtms.put((String) mtmRel[0], (Map) mtmRel[1]);
         }

         for (String table : mtms.keySet())
         {
            List<Map> rows = mtms.get(table);
            sql = "";
            List keys = new ArrayList(rows.get(0).keySet());
            sql += " INSERT IGNORE INTO " + table + " (" + Sql.getColumnStr(keys) + ") ";
            sql += " VALUES (" + Sql.getQuestionMarkStr(keys.size()) + ")";

            PreparedStatement stmt = conn.prepareStatement(sql);
            for (Map row : rows)
            {
               for (int i = 0; i < keys.size(); i++)
               {
                  stmt.setObject(i + 1, row.get(keys.get(i)));
               }
               stmt.addBatch();
            }
            int[] added = stmt.executeBatch();
            stmt.close();
         }
      }
      catch (Exception ex)
      {
         if (ex instanceof SQLException)
            J.rethrow(ex.getMessage() + " - SQL = " + sql, ex);

         J.rethrow(ex);
      }
   }

   String storeEntity(Connection conn, Request req, Entity entity, JSObject obj) throws Exception
   {
      Api api = entity.getCollection().getApi();

      LinkedHashMap vals = new LinkedHashMap();

      for (Attribute attr : entity.getAttributes())
      {
         String key = attr.getName();

         if (key.equals("id"))
            key = "href";

         String col = attr.getCol().getName();

         Property prop = obj.getProperty(key);
         if (prop != null)
         {
            Object value = prop.getValue();
            if ("null".equals((value + "").toLowerCase()))
            {
               value = null;
            }

            if (key.equals("href") && value != null)
            {
               String id = (String) value;
               id = id.substring(id.lastIndexOf("/") + 1, id.length());
               value = id;
            }
            vals.put(col, convert(attr.getCol(), value));
         }
      }

      for (Relationship rel : entity.getRelationships())
      {
         if (rel.getType().equals("ONE_TO_MANY"))
         {
            String colName = rel.getFkCol1().getName();

            Property prop = obj.getProperty(colName);
            Object value = null;

            if (prop != null)
            {
               value = prop.getValue();
            }
            else
            {
               prop = obj.getProperty(rel.getName());
               if (prop != null)
                  value = prop.getValue();

               if (!J.empty(value))
               {
                  Collection fkCollection = api.getCollection(rel.getFkCol1().getTbl());
                  String baseRef = req.getApiUrl() + fkCollection.getName();

                  String href = value.toString();
                  String key = href.substring(baseRef.length(), href.length());

                  while (key.endsWith("/"))
                     key = key.substring(0, key.length() - 1);

                  while (key.startsWith("/"))
                     key = key.substring(1, key.length());

                  value = key;

                  if (!href.startsWith(baseRef) || key.indexOf("/") > -1)
                  {
                     String msg = "Invalid reference \"" + href + "\" for relationship " + rel.getHint();
                     throw new ApiException(SC.SC_400_BAD_REQUEST, msg);
                  }
               }
            }

            if ("null".equals((value + "").toLowerCase()) || J.empty(value))
            {
               value = null;
            }

            vals.put(colName, value);
         }
      }

      if (vals.containsKey("id"))
      {
         Sql.updateRow(conn, entity.getTbl().getName(), "id", (String) vals.get("id"), vals);
         return vals.get("id") + "";
      }
      else
      {
         return Sql.insertMap(conn, entity.getTbl().getName(), vals) + "";
      }
   }

   Object convert(Col col, Object value)
   {
      if (J.empty(value))
         return null;

      String type = col.getType().toUpperCase();

      if ((type.equals("BIT") || type.equals("BOOLEAN")) && !(value instanceof Boolean))
      {
         if (value instanceof String)
         {
            String str = ((String) value).toLowerCase();
            value = str.startsWith("t") || str.startsWith("1");
         }
      }
      if ((type.startsWith("DATE") || type.startsWith("TIME")) && !(value instanceof Date))
      {
         if("0".equals(value + ""))
            return 0;
         
         value = J.date(value.toString());
      }

      return value;
   }

}
