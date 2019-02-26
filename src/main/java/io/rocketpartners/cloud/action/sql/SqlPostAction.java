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
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import io.rocketpartners.cloud.action.rest.RestPostAction;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.ApiException;
import io.rocketpartners.cloud.model.ArrayNode;
import io.rocketpartners.cloud.model.Attribute;
import io.rocketpartners.cloud.model.Collection;
import io.rocketpartners.cloud.model.Column;
import io.rocketpartners.cloud.model.Entity;
import io.rocketpartners.cloud.model.ObjectNode;
import io.rocketpartners.cloud.model.ObjectNode.Property;
import io.rocketpartners.cloud.model.Relationship;
import io.rocketpartners.cloud.model.Request;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.model.Url;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.utils.SqlUtils;
import io.rocketpartners.cloud.utils.Utils;

public class SqlPostAction extends RestPostAction
{
   protected String store(Request req, Collection collection, ObjectNode node) throws Exception
   {
      String href = node.getString("href");
      if (href != null && node.keySet().size() == 1)
         return href; //this object is empty except for the href...don't change anything

      Connection conn = ((SqlDb) collection.getDb()).getConnection();
      return store(conn, collection, node);
   }

   protected String store(Connection conn, Collection collection, ObjectNode parent) throws Exception
   {
      String parentId = storeEntity(conn, collection, parent); //this also stores oneToMany relationships

      storeManyTo(conn, collection, parentId, parent);

      // use the collection key from the request instead of the entity to support collection aliasing
      String href = Chain.buildLink(collection, parentId, null);//chain.getRequest().getApiUrl() + chain.getRequest().getCollectionKey() + "/" + parentId;

      return href;
   }

   String storeEntity(Connection conn, Collection collection, ObjectNode parent) throws Exception
   {
      Api api = collection.getApi();
      Entity entity = collection.getEntity();

      LinkedHashMap vals = new LinkedHashMap();

      String idAttr = "id";
      String idCol = "id";
      if (collection.getEntity().getKey() != null)
      {
         idAttr = collection.getEntity().getKey().getName();
         idCol = collection.getEntity().getKey().getColumn().getName();
      }

      for (Attribute attr : entity.getAttributes())
      {
         String key = attr.getName();

         Property prop = parent.getProperty(key);
         if (prop != null)
         {
            String col = attr.getColumn().getName();
            Object value = prop.getValue();
            value = cast(attr.getColumn(), value);

            if ("null".equals((value + "").toLowerCase()))
            {
               value = null;
            }

            if (key.equals("href") && value != null && idAttr != null)
            {
               String id = value.toString();
               if (id.indexOf("/") > -1)
               {
                  id = id.substring(id.lastIndexOf("/") + 1, id.length());
                  value = id;
                  col = collection.getEntity().getKey().getColumn().getName();
               }
            }

            vals.put(col, value);
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

               if (value instanceof ObjectNode)
               {
                  ObjectNode child = (ObjectNode) value;
                  Collection fkCollection = api.getCollection(rel.getFkCol1().getPk().getTable());
                  String href = child.getString("href");

                  if (href == null || child.keySet().size() > 1)
                  {
                     try
                     {
                        href = store(conn, fkCollection, child);
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

      boolean insert = (strictRest && Chain.getRequest().isMethod("POST")) || !vals.containsKey(idCol);

      String id = null;
      if (insert)
      {
         Chain.debug("SqlPostHandler -> updateRow: (" + entity.getTable().getName(), "id", vals.get("id"), vals);
         try
         {
            SqlUtils.updateRow(conn, entity.getTable().getName(), idCol, vals.get(idCol) + "", vals);
         }
         catch (Exception ex)
         {
            ex.printStackTrace();
         }
         //changes.add(new Change("PUT", entity.getCollection().getName(), vals.get("id")));

         id = vals.get(idCol) + "";
      }
      else
      {
         Chain.debug("Sql.insertMap(" + entity.getTable().getName(), vals);
         id = SqlUtils.insertMap(conn, entity.getTable().getName(), vals) + "";
         //changes.add(new Change("POST", entity.getCollection().getName(), id));
      }

      String href = Chain.buildLink(entity.getCollection(), id, null);//.getName() + "/" + id;
      parent.put("href", href);

      return id;
   }

   void storeManyTo(Connection conn, Collection collection, Object parentId, ObjectNode parent) throws Exception
   {
      Api api = collection.getApi();
      Entity entity = collection.getEntity();

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

            if (!(arrayObj instanceof ArrayNode))
            {
               if (arrayObj instanceof ObjectNode)
               {
                  //this is the child collection "placeholder" that is used to bookmark
                  //non expanded child collections by GetHandler...ex:
                  //..., "children" : { "href" : "http://host.com/apicode/collection/entityKey/someChildCollection"}, ....
                  //thils shoudlbe ignored

                  ObjectNode js = (ObjectNode) arrayObj;
                  if (js.keySet().size() == 1 && js.containsKey("href"))
                     continue;
               }

               throw new ApiException(SC.SC_400_BAD_REQUEST, "Was expecting an array for relationship " + rel);
            }

            ArrayNode children = (ArrayNode) arrayObj;

            if (children.length() > 0)
            {
               for (Object childObj : children)
               {
                  if (childObj == null)
                     continue;

                  if (!(childObj instanceof ObjectNode))
                     throw new ApiException(SC.SC_400_BAD_REQUEST, "Child objects for relationships " + rel + " must be objects not arrays or primitives");

                  ObjectNode child = (ObjectNode) childObj;

                  child.put(rel.getFkCol1().getName(), parentId);
                  String href = store(conn, childEntity.getCollection(), child);

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
         String childKeyCol = m2m ? rel.getFkCol2().getName() : collection.getApi().getCollection(rel.getFkCol1().getTable()).getEntity().getKey().getName();
         String qmarks = SqlUtils.getQuestionMarkStr(relateds.get(rel).size());

         List args = new ArrayList(relateds.get(rel));
         args.add(0, parentId);

         if (m2o)
         {
            Column fk = rel.getFkCol1();

            String childPkCol = "`" + collection.getApi().getCollection(fk.getTable()).getEntity().getKey().getName() + "`";
            String fkCol = fk.getName();

            //this first statement assigns or reassigns any 
            //of the fk related entities to the parent
            String sql = "";
            sql += " UPDATE " + table;
            sql += " SET " + fkCol + " = ? ";
            sql += " WHERE " + childPkCol + " IN (" + qmarks + ")";

            Chain.debug(sql, args);
            SqlUtils.execute(conn, sql, args);

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

            //            chain.debug(sql, args);
            SqlUtils.execute(conn, sql, args);
         }
         else
         {
            String sql = "";
            sql += " INSERT IGNORE INTO " + table + " (" + parentKeyCol + ", " + childKeyCol + ") ";
            sql += " VALUES ( ?, ?)";

            //chain.debug(sql, relateds);

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
            //chain.debug(sql, args);
            SqlUtils.execute(conn, sql, args);
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

         //chain.debug(sql, args);
         SqlUtils.execute(conn, sql, args);
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
   public static void collapse(ObjectNode parent, boolean collapseAll, Set collapses, String path)
   {
      for (String key : (List<String>) new ArrayList(parent.keySet()))
      {
         Object value = parent.get(key);

         if (collapseAll || collapses.contains(nextPath(path, key)))
         {
            if (value instanceof ArrayNode)
            {
               ArrayNode children = (ArrayNode) value;
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

                  if (children.get(i) instanceof ArrayNode || !(children.get(i) instanceof ObjectNode))
                  {
                     children.remove(i);
                     i--;
                     continue;
                  }

                  ObjectNode child = children.getObject(i);
                  for (String key2 : (List<String>) new ArrayList(child.keySet()))
                  {
                     if (!key2.equalsIgnoreCase("href"))
                     {
                        child.remove(key2);
                     }
                  }

                  if (child.keySet().size() == 0)
                  {

                     children.remove(i);
                     i--;
                     continue;
                  }
               }
               if (children.length() == 0)
                  parent.remove(key);

            }
            else if (value instanceof ObjectNode)
            {
               ObjectNode child = (ObjectNode) value;
               for (String key2 : (List<String>) new ArrayList(child.keySet()))
               {
                  if (!key2.equalsIgnoreCase("href"))
                  {
                     child.remove(key2);
                  }
               }
               if (child.keySet().size() == 0)
                  parent.remove(key);
            }
         }
         else if (value instanceof ArrayNode)
         {
            ArrayNode children = (ArrayNode) value;
            for (int i = 0; i < children.length(); i++)
            {
               if (children.get(i) instanceof ObjectNode && !(children.get(i) instanceof ArrayNode))
               {
                  collapse(children.getObject(i), collapseAll, collapses, nextPath(path, key));
               }
            }
         }
         else if (value instanceof ObjectNode)
         {
            collapse((ObjectNode) value, collapseAll, collapses, nextPath(path, key));
         }

      }
   }

   Object cast(Column col, Object value)
   {
      return SqlUtils.cast(value, col.getType());
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
