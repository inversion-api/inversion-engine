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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import io.rocketpartners.cloud.model.Action;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.ApiException;
import io.rocketpartners.cloud.model.Attribute;
import io.rocketpartners.cloud.model.Change;
import io.rocketpartners.cloud.model.Collection;
import io.rocketpartners.cloud.model.Column;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.model.Entity;
import io.rocketpartners.cloud.model.Relationship;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.service.Request;
import io.rocketpartners.cloud.service.Response;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.utils.JS;
import io.rocketpartners.utils.JSArray;
import io.rocketpartners.utils.JSObject;
import io.rocketpartners.utils.JSObject.Property;
import io.rocketpartners.utils.Sql;
import io.rocketpartners.utils.Url;
import io.rocketpartners.utils.Utils;

public class SqlPostAction extends SqlAction
{
   boolean collapseAll    = false;
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

      Connection conn = ((SqlDb) chain.getService().getDb(req.getApi(), req.getCollectionKey(), SqlDb.class)).getConnection();

      List<String> hrefs = new ArrayList();

      Collection collection = req.getApi().getCollection(req.getCollectionKey(), SqlDb.class);
      Entity entity = collection.getEntity();

      JSObject obj = req.getJson();

      if (obj == null)
         throw new ApiException(SC.SC_400_BAD_REQUEST, "You must pass a JSON body to the PostHandler");

      boolean collapseAll = "true".equalsIgnoreCase(chain.getConfig("collapseAll", this.collapseAll + ""));
      Set<String> collapses = chain.getConfigSet("collapses");
      collapses.addAll(splitParam(req, "collapses"));

      if (collapseAll || collapses.size() > 0)
      {
         obj = JS.toJSObject(obj.toString());
         collapse(obj, collapseAll, collapses, "");
      }

      try
      {
         if (obj instanceof JSArray)
         {
            if (!Utils.empty(req.getEntityKey()))
               throw new ApiException(SC.SC_400_BAD_REQUEST, "You can't batch " + req.getMethod() + " an array of objects to a specific resource url.  You must " + req.getMethod() + " them to a collection.");

            for (JSObject child : (List<JSObject>) ((JSArray) obj).getObjects())
            {
               String href = store(chain, conn, changes, entity, child);
               hrefs.add(href);
            }
         }
         else
         {
            String href = obj.getString("href");
            if (req.isPut() && href != null && req.getEntityKey() != null && !req.getUrl().toString().startsWith(href))
            {
               throw new ApiException(SC.SC_400_BAD_REQUEST, "You are PUT-ing an entity with a different href property than the entity URL you are PUT-ing to.");
            }

            href = store(chain, conn, changes, entity, obj);
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
                  else
                  {
                     System.out.println("what?");
                  }
               }
               else
               {
                  System.out.println("what?");
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

   String store(Chain chain, Connection conn, List<Change> changes, Entity entity, JSObject parent) throws Exception
   {
      String href = parent.getString("href");
      if (href != null && parent.keys().size() == 1)
         return href; //this object is empty except for the href...don't change anything

      String parentId = storeEntity(chain, conn, changes, entity, parent); //this also stores oneToMany relationships

      storeManyTo(chain, conn, changes, parentId, entity, parent);

      // use the collection key from the request instead of the entity to support collection aliasing
      href = chain.getRequest().getApiUrl() + chain.getRequest().getCollectionKey() + "/" + parentId;

      return href;
   }

   String storeEntity(Chain chain, Connection conn, List<Change> changes, Entity entity, JSObject parent) throws Exception
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
                        href = store(chain, conn, changes, fkCollection.getEntity(), child);
                     }
                     catch (Exception ex)
                     {
                        ex.printStackTrace();
                        throw ex;
                     }
                  }
                  value = new Url(href).getFile();
               }
            }

            if ("null".equals((value + "").toLowerCase()) || Utils.empty(value))
            {
               value = null;
            }

            vals.put(colName, value);
         }
      }

      String id = null;
      if (vals.containsKey("id"))
      {
         if (vals.size() > 1)
         {
            chain.debug("Sql.updateRow(`" + entity.getTable().getName() + "`", "id", vals.get("id"), vals);
            Sql.updateRow(conn, "`" + entity.getTable().getName() + "`", "id", vals.get("id") + "", vals);
            changes.add(new Change("PUT", entity.getCollection().getName(), vals.get("id")));
         }
         id = vals.get("id") + "";
      }
      else
      {
         chain.debug("Sql.insertMap(`" + entity.getTable().getName() + "`", vals);
         id = Sql.insertMap(conn, "`" + entity.getTable().getName() + "`", vals) + "";
         changes.add(new Change("POST", entity.getCollection().getName(), id));
      }

      String href = chain.getRequest().getApiUrl() + entity.getCollection().getName() + "/" + id;
      parent.put("href", href);

      return id;
   }

   void storeManyTo(Chain chain, Connection conn, List<Change> changes, Object parentId, Entity entity, JSObject parent) throws Exception
   {
      ArrayListValuedHashMap<Relationship, String> relateds = new ArrayListValuedHashMap();

      // holds Relationships when an empty array was posted
      List<Relationship> emptyRelateds = new ArrayList<>();

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
                     continue;
               }

               throw new ApiException(SC.SC_400_BAD_REQUEST, "Was expecting an array for relationship " + rel);
            }

            JSArray children = (JSArray) arrayObj;

            if (children.length() > 0)
            {
               for (Object childObj : children.getObjects())
               {
                  if (childObj == null)
                     continue;

                  if (!(childObj instanceof JSObject))
                     throw new ApiException(SC.SC_400_BAD_REQUEST, "Child objects for relationships " + rel + " must be objects not arrays or primitives");

                  JSObject child = (JSObject) childObj;

                  child.put(rel.getFkCol1().getName(), parentId);
                  String href = store(chain, conn, changes, childEntity, child);

                  String childId = href.substring(href.lastIndexOf("/") + 1, href.length());
                  relateds.put(rel, childId);
               }
            }
            else
            {
               emptyRelateds.add(rel);
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
         String childKeyCol = m2m ? rel.getFkCol2().getName() : chain.getRequest().getApi().getCollection(rel.getFkCol1().getTable()).getEntity().getKey().getName();
         String qmarks = Sql.getQuestionMarkStr(relateds.get(rel).size());

         List args = new ArrayList(relateds.get(rel));
         args.add(0, parentId);

         if (m2o)
         {
            Column fk = rel.getFkCol1();

            String childPkCol = "`" + chain.getRequest().getApi().getCollection(fk.getTable()).getEntity().getKey().getName() + "`";
            String fkCol = fk.getName();

            //this first statement assigns or reassigns any 
            //of the fk related entities to the parent
            String sql = "";
            sql += " UPDATE " + table;
            sql += " SET " + fkCol + " = ? ";
            sql += " WHERE " + childPkCol + " IN (" + qmarks + ")";

            chain.debug(sql, args);
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

            chain.debug(sql, args);
            Sql.execute(conn, sql, args);
         }
         else
         {
            String sql = "";
            sql += " INSERT IGNORE INTO " + table + " (" + parentKeyCol + ", " + childKeyCol + ") ";
            sql += " VALUES ( ?, ?)";

            chain.debug(sql, relateds);

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
            chain.debug(sql, args);
            Sql.execute(conn, sql, args);
         }
      }

      // now handle the empty arrays, we just need to delete all the related 
      for (Relationship rel : emptyRelateds)
      {
         boolean m2o = rel.getType().equals("MANY_TO_ONE");
         boolean nullable = (m2o && rel.getFkCol1().isNullable());
         String table = "`" + rel.getFkCol1().getTable().getName() + "`";
         String parentKeyCol = rel.getFkCol1().getName();
         String sql;

         if (nullable)
         {
            sql = "UPDATE " + table + " SET " + parentKeyCol + " = NULL WHERE " + parentKeyCol + " = ? ";
         }
         else
         {
            sql = "DELETE FROM " + table + " WHERE " + parentKeyCol + " = ? ";
         }

         List args = new ArrayList();
         args.add(parentId);

         chain.debug(sql, args);
         Sql.execute(conn, sql, args);
      }
   }

   /*
    * Collapses nested objects so that relationships can be preserved but the fields
    * of the nested child objects are not saved (except for FKs back to the parent 
    * object in the case of a MANY_TO_ONE relationship).
    * 
    * This is intended to be used as a reciprocal to GetHandler "expands" when
    * a client does not want to scrub their json model before posting changes to
    * the parent document back to the parent collection.
    */
   public static void collapse(JSObject parent, boolean collapseAll, Set collapses, String path)
   {
      for (String key : (List<String>) new ArrayList(parent.keys()))
      {
         Object value = parent.get(key);

         if (collapseAll || collapses.contains(nextPath(path, key)))
         {
            if (value instanceof JSArray)
            {
               JSArray children = (JSArray) value;
               if (children.length() == 0)
                  parent.remove(key);

               for (int i = 0; i < children.length(); i++)
               {
                  if (children.get(i) == null)
                  {
                     children.remove(i);
                     i--;
                     continue;
                  }

                  if (children.get(i) instanceof JSArray || !(children.get(i) instanceof JSObject))
                  {
                     children.remove(i);
                     i--;
                     continue;
                  }

                  JSObject child = children.getObject(i);
                  for (String key2 : (List<String>) new ArrayList(child.keys()))
                  {
                     if (!key2.equalsIgnoreCase("href"))
                     {
                        child.remove(key2);
                     }
                  }

                  if (child.keys().size() == 0)
                  {

                     children.remove(i);
                     i--;
                     continue;
                  }
               }
               if (children.length() == 0)
                  parent.remove(key);

            }
            else if (value instanceof JSObject)
            {
               JSObject child = (JSObject) value;
               for (String key2 : (List<String>) new ArrayList(child.keys()))
               {
                  if (!key2.equalsIgnoreCase("href"))
                  {
                     child.remove(key2);
                  }
               }
               if (child.keys().size() == 0)
                  parent.remove(key);
            }
         }
         else if (value instanceof JSArray)
         {
            JSArray children = (JSArray) value;
            for (int i = 0; i < children.length(); i++)
            {
               if (children.get(i) instanceof JSObject && !(children.get(i) instanceof JSArray))
               {
                  collapse(children.getObject(i), collapseAll, collapses, nextPath(path, key));
               }
            }
         }
         else if (value instanceof JSObject)
         {
            collapse((JSObject) value, collapseAll, collapses, nextPath(path, key));
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
