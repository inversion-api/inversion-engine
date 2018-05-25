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
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import io.forty11.j.J;
import io.forty11.sql.Sql;
import io.forty11.utils.ListMap;
import io.forty11.web.Url;
import io.forty11.web.js.JSArray;
import io.forty11.web.js.JSObject;
import io.forty11.web.js.JSObject.Property;
import io.rcktapp.api.Action;
import io.rcktapp.api.Api;
import io.rcktapp.api.ApiException;
import io.rcktapp.api.Attribute;
import io.rcktapp.api.Chain;
import io.rcktapp.api.Change;
import io.rcktapp.api.Collection;
import io.rcktapp.api.Column;
import io.rcktapp.api.Endpoint;
import io.rcktapp.api.Entity;
import io.rcktapp.api.Handler;
import io.rcktapp.api.Relationship;
import io.rcktapp.api.Request;
import io.rcktapp.api.Response;
import io.rcktapp.api.SC;

public class PostHandler implements Handler
{
   boolean strictRest     = false;
   boolean expandResponse = true;

   @Override
   public void service(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res) throws Exception
   {
      if (strictRest)
      {
         if (req.isPost() && req.getEntityKey() != null)
            throw new ApiException(SC.SC_404_NOT_FOUND, "You are trying to POST to a specific entity url.  Set 'strictRest' to false interprent PUT vs POST intention based on presense of 'href' property in passed in JSON");
         if (req.isPut() && req.getEntityKey() == null)
            throw new ApiException(SC.SC_404_NOT_FOUND, "You are trying to PUT to a collection url.  Set 'strictRest' to false interprent PUT vs POST intention based on presense of 'href' property in passed in JSON");
      }

      List<Change> changes = new ArrayList();

      Connection conn = chain.getService().getConnection(req.getApi(), req.getCollectionKey());

      List<String> hrefs = new ArrayList();

      Collection collection = req.getApi().getCollection(req.getCollectionKey());
      Entity entity = collection.getEntity();

      JSObject obj = req.getJson();

      try
      {
         if (obj instanceof JSArray)
         {
            if (!J.empty(req.getEntityKey()))
               throw new ApiException(SC.SC_400_BAD_REQUEST, "You can't batch " + req.getMethod() + " an array of objects to a specific resource url.  You must " + req.getMethod() + " them to a collection.");

            for (JSObject child : (List<JSObject>) ((JSArray) obj).getObjects())
            {
               String href = store(conn, req, changes, entity, child);
               hrefs.add(href);
            }
         }
         else
         {
            String href = obj.getString("href");
            if (href != null && req.getEntityKey() != null && !req.getUrl().toString().startsWith(href))
            {
               throw new ApiException(SC.SC_400_BAD_REQUEST, "You are PUT-ing an entity with a different href property than the entity URL you are PUT-ing to.");
            }

            href = store(conn, req, changes, entity, obj);
            hrefs.add(href);
         }

         res.addChanges(changes);

         //-- take all of the hrefs and combine into a 
         //-- single href for the "Location" header

         JSArray array = new JSArray();
         res.getJson().put("data", array);

         res.setStatus(SC.SC_201_CREATED);
         StringBuffer buff = new StringBuffer(hrefs.get(0));
         for (int i = 0; i < hrefs.size(); i++)
         {
            String href = hrefs.get(i);

            boolean added = false;
            if (expandResponse)
            {
               Response resp = service.include(chain, "GET", href, null);
               if (resp != null)
               {
                  JSObject js = resp.getJson();
                  if (js != null)
                  {
                     js = js.getObject("data");
                     if (js instanceof JSArray && ((JSArray) js).length() == 1)
                     {
                        array.add(((JSArray) js).get(0));
                        added = true;
                     }
                  }
               }
            }

            if (!added)
            {
               array.add(new JSObject("href", href));
            }

            String nextId = href.substring(href.lastIndexOf("/") + 1, href.length());
            buff.append(",").append(nextId);
         }

         res.addHeader("Location", buff.toString());
      }
      finally
      {
         // don't do this anymore, connection will be committed/rollbacked and closed in the Service class
         //Sql.close(conn);
      }

   }

   String store(Connection conn, Request req, List<Change> changes, Entity entity, JSObject parent) throws Exception
   {
      String href = parent.getString("href");
      if (href != null && parent.keys().size() == 1)
         return href; //this object is empty except for the href...don't change anything

      String parentId = storeEntity(conn, req, changes, entity, parent); //this also stores oneToMany relationships

      storeManyTo(conn, req, changes, parentId, entity, parent);

      href = req.getApiUrl() + entity.getCollection().getName() + "/" + parentId;

      return href;
   }

   String storeEntity(Connection conn, Request req, List<Change> changes, Entity entity, JSObject parent) throws Exception
   {
      Api api = entity.getCollection().getApi();

      LinkedHashMap vals = new LinkedHashMap();

      for (Attribute attr : entity.getAttributes())
      {
         String key = attr.getName();

         if (key.equals("id"))
            key = "href";

         String col = attr.getColumn().getName();

         Property prop = parent.getProperty(key);
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
            vals.put(col, cast(attr.getColumn(), value));
         }
      }

      for (Relationship rel : entity.getRelationships())
      {
         if (rel.getType().equals("ONE_TO_MANY"))
         {
            String colName = rel.getFkCol1().getName();

            Property prop = parent.getProperty(colName);
            Object value = null;

            if (prop != null)
            {
               value = prop.getValue();
            }
            else
            {
               prop = parent.getProperty(rel.getName());
               if (prop != null)
                  value = prop.getValue();
               else
                  continue;

               if (value instanceof JSObject)
               {
                  JSObject child = (JSObject) value;
                  Collection fkCollection = api.getCollection(rel.getFkCol1().getPk().getTable());
                  String href = child.getString("href");

                  if (href == null || child.keySet().size() > 1)
                  {
                     try
                     {
                        href = store(conn, req, changes, fkCollection.getEntity(), child);
                     }
                     catch (Exception ex)
                     {
                        ex.printStackTrace();
                     }
                  }
                  value = new Url(href).getFile();
               }
            }

            if ("null".equals((value + "").toLowerCase()) || J.empty(value))
            {
               value = null;
            }

            vals.put(colName, value);
         }
      }

      String id = null;
      if (vals.containsKey("id"))
      {
         Sql.updateRow(conn, entity.getTable().getName(), "id", vals.get("id") + "", vals);
         changes.add(new Change("PUT", entity.getCollection().getName(), vals.get("id")));
         id = vals.get("id") + "";
      }
      else
      {
         id = Sql.insertMap(conn, entity.getTable().getName(), vals) + "";
         changes.add(new Change("POST", entity.getCollection().getName(), id));
      }

      String href = req.getApiUrl() + entity.getCollection().getName() + "/" + id;
      parent.put("href", href);

      return id;
   }

   void storeManyTo(Connection conn, Request req, List<Change> changes, Object parentId, Entity entity, JSObject parent) throws Exception
   {
      ListMap<Relationship, String> relateds = new ListMap();

      for (Relationship rel : entity.getRelationships())
      {
         if (!rel.getType().equals("ONE_TO_MANY"))
         {
            Entity childEntity = rel.getRelated();

            Object arrayObj = parent.get(rel.getName());

            if (arrayObj == null)
               continue;

            if (!(arrayObj instanceof JSArray))
            {
               if (arrayObj instanceof JSObject)
               {
                  //this is the child collection "placeholder" that is used to bookmark
                  //non expanded child collections by GetHandler...ex:
                  //..., "children" : { "href" : "http://host.com/apicode/collection/entityKey/someChildCollection"}, ....
                  //thils shoudlbe ignored

                  JSObject js = (JSObject) arrayObj;
                  if (js.keys().size() == 1 && js.containsKey("href"))
                     return;
               }

               throw new ApiException(SC.SC_400_BAD_REQUEST, "Was expecting an array for relationship " + rel);
            }

            JSArray children = (JSArray) arrayObj;

            for (Object childObj : children.getObjects())
            {
               if (childObj == null)
                  continue;

               if (!(childObj instanceof JSObject))
                  throw new ApiException(SC.SC_400_BAD_REQUEST, "Child objects for relationships " + rel + " must be objects not arrays or primitives");

               JSObject child = (JSObject) childObj;

               child.put(rel.getFkCol1().getName(), parentId);
               String href = store(conn, req, changes, childEntity, child);

               String childId = href.substring(href.lastIndexOf("/") + 1, href.length());
               relateds.put(rel, childId);
            }
         }
      }

      //Now previously related objects NOT in 
      //the supplied list must be unlinked
      for (Relationship rel : relateds.keySet())
      {
         boolean m2m = rel.getType().equals("MANY_TO_MANY");
         boolean m2o = rel.getType().equals("MANY_TO_ONE");

         String table = "`" + rel.getFkCol1().getTable().getName() + "`";
         String parentKeyCol = rel.getFkCol1().getName();
         String childKeyCol = m2m ? rel.getFkCol2().getName() : req.getApi().getCollection(rel.getFkCol1().getTable()).getEntity().getKey().getName();
         String qmarks = Sql.getQuestionMarkStr(relateds.get(rel).size());

         List args = new ArrayList(relateds.get(rel));
         args.add(0, parentId);

         if (m2o)
         {
            Column fk = rel.getFkCol1();

            String childPkCol = "`" + req.getApi().getCollection(fk.getTable()).getEntity().getKey().getName() + "`";
            String fkCol = fk.getName();

            //this first statement assigns or reassigns any 
            //of the fk related entities to the parent
            String sql = "";
            sql += " UPDATE " + table;
            sql += " SET " + fkCol + " = ? ";
            sql += " WHERE " + childPkCol + " IN (" + qmarks + ")";

            Sql.execute(conn, sql, args);

            //these next statemets delete now removed relationshiops
            if (fk.isNullable())
            {
               //set the fk to null where the fk field is nullable
               sql = "";
               sql += " UPDATE " + table + " ";
               sql += " SET " + fkCol + " = NULL ";
               sql += " WHERE " + fkCol + " = ? ";
               sql += " AND " + childPkCol + " NOT IN (" + qmarks + ")";
            }
            else
            {
               //delete the row if the fk is not nullable
               //this would be a dependent child that the data model
               //says should not exist outside of the relationship
               //with the parent
               sql = "";
               sql += " DELETE FROM " + table + " ";
               sql += " WHERE " + fkCol + " = ? ";
               sql += " AND " + childPkCol + " NOT IN (" + qmarks + ")";
            }

            Sql.execute(conn, sql, args);
         }
         else
         {
            String sql = "";
            sql += " INSERT IGNORE INTO " + table + " (" + parentKeyCol + ", " + childKeyCol + ") ";
            sql += " VALUES ( ?, ?)";

            PreparedStatement stmt = conn.prepareStatement(sql);
            for (String childId : relateds.get(rel))
            {
               stmt.setObject(1, parentId);
               stmt.setObject(2, childId);
               stmt.addBatch();
            }
            stmt.executeBatch();
            stmt.close();

            sql = "";
            sql += "DELETE FROM " + table + " WHERE " + parentKeyCol + " = ? AND " + childKeyCol + " NOT IN (" + qmarks + ")";
            Sql.execute(conn, sql, args);

         }
      }

   }

   Object cast(Column col, Object value)
   {
      return Sql.cast(value, col.getType());
      //      
      //      if (J.empty(value))
      //         return null;
      //
      //      String type = col.getType().toUpperCase();
      //
      //      if ((type.equals("BIT") || type.equals("BOOLEAN")) && !(value instanceof Boolean))
      //      {
      //         if (value instanceof String)
      //         {
      //            String str = ((String) value).toLowerCase();
      //            value = str.startsWith("t") || str.startsWith("1");
      //         }
      //      }
      //      if ((type.startsWith("DATE") || type.startsWith("TIME")) && !(value instanceof Date))
      //      {
      //         value = J.date(value.toString());
      //      }
      //
      //      return value;
   }

}
