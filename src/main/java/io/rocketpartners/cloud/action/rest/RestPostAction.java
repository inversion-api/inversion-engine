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
package io.rocketpartners.cloud.action.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.qos.logback.classic.net.SyslogAppender;
import io.rocketpartners.cloud.model.Action;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.ApiException;
import io.rocketpartners.cloud.model.ArrayNode;
import io.rocketpartners.cloud.model.Attribute;
import io.rocketpartners.cloud.model.Change;
import io.rocketpartners.cloud.model.Collection;
import io.rocketpartners.cloud.model.Column;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.model.ObjectNode;
import io.rocketpartners.cloud.model.Relationship;
import io.rocketpartners.cloud.model.Request;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.SqlUtils;
import io.rocketpartners.cloud.utils.Utils;

public class RestPostAction extends Action<RestPostAction>
{
   protected boolean collapseAll    = false;
   protected boolean strictRest     = true;
   protected boolean expandResponse = true;

   protected String upsert(Request req, Collection collection, ObjectNode node) throws Exception
   {
      Map<String, Object> mapped = new HashMap();
      Set copied = new HashSet();

      if(collection == null)
         System.out.println("can't post to a null collection");
      
      for (Attribute attr : collection.getEntity().getAttributes())
      {
         String attrName = attr.getName();
         
         if(collection.getEntity().getRelationship(attrName) != null)
            continue;
         
         String colName = attr.getColumn().getName();
         if (node.containsKey(attrName))
         {
            copied.add(attrName.toLowerCase());
            copied.add(colName.toLowerCase());

            Object attrValue = node.get(attrName);
            Object colValue = collection.getDb().cast(attr, attrValue);
            mapped.put(colName, colValue);
         }
      }

      for (Relationship rel : collection.getEntity().getRelationships())
      {
         //TODO recursively upsert children first and collapse nested objects back into just an href
      }

      for (Relationship rel : collection.getEntity().getRelationships())
      {
         String attrName = rel.getName();
         copied.add(attrName.toLowerCase());

         if (!node.containsKey(attrName))
            continue;

         if (rel.isOneToMany()) //ONE_TO_MANY - Player.locationId -> Location.id
         {
            Object attrValue = node.remove(attrName);

            if (attrValue instanceof String)
            {
               String attrStr = (String) attrValue;
               if (attrStr.indexOf("/") > 0 && !attrStr.endsWith("/"))
                  attrStr = attrStr.substring(attrStr.lastIndexOf("/") + 1, attrStr.length());

               attrValue = attrStr;
            }
            else if (attrValue != null)
            {
               throw new ApiException("implementation error");
            }

            //TODO: work on compound key support here, need test case
            Column fkCol = rel.getFk1Col1();
            String fkColName = fkCol.getName();
            copied.add(fkColName.toLowerCase());

            Object colValue = attrValue == null ? null : collection.getDb().cast(fkCol.getType(), attrValue);
            mapped.put(fkColName, colValue);
         }
         else
         {
            //TODO
         }
      }

      for (String key : node.keySet())
      {
         if (!copied.contains(key.toLowerCase()) && !key.equalsIgnoreCase("href"))
         {
            //these fields were posted and may map to table columns but they are not defined as attributes.
            //this is ok for some dynamic backends like dynamo but will cause a problem for others like sql rdbmss.
            mapped.put(key, node.get(key));
         }
      }

      return collection.getDb().upsert(collection.getTable(), mapped);
   }

   @Override
   public void run(Service service, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
   {
      if (strictRest)
      {
         if (req.isPost() && req.getEntityKey() != null)
            throw new ApiException(SC.SC_404_NOT_FOUND, "You are trying to POST to a specific entity url.  Set 'strictRest' to false interprent PUT vs POST intention based on presense of 'href' property in passed in JSON");
         if (req.isPut() && req.getEntityKey() == null)
            throw new ApiException(SC.SC_404_NOT_FOUND, "You are trying to PUT to a collection url.  Set 'strictRest' to false interprent PUT vs POST intention based on presense of 'href' property in passed in JSON");
      }

      Collection collection = req.getCollection();
      List<Change> changes = new ArrayList();
      List<String> entityKeys = new ArrayList();
      ObjectNode obj = req.getJson();

      if (obj == null)
         throw new ApiException(SC.SC_400_BAD_REQUEST, "You must pass a JSON body to the PostHandler");

      boolean collapseAll = "true".equalsIgnoreCase(chain.getConfig("collapseAll", this.collapseAll + ""));
      Set<String> collapses = chain.mergeEndpointActionParamsConfig("collapses");

      if (collapseAll || collapses.size() > 0)
      {
         obj = Utils.parseJsonObject(obj.toString());
         collapse(obj, collapseAll, collapses, "");
      }

      try
      {
         if (obj instanceof ArrayNode)
         {
            if (!Utils.empty(req.getEntityKey()))
               throw new ApiException(SC.SC_400_BAD_REQUEST, "You can't batch " + req.getMethod() + " an array of objects to a specific resource url.  You must " + req.getMethod() + " them to a collection.");

            for (ObjectNode child : (List<ObjectNode>) ((ArrayNode) obj).asList())
            {
               String href = upsert(req, collection, child);
               entityKeys.add(href);
            }
         }
         else
         {
            String href = obj.getString("href");
            if (req.isPut() && href != null && req.getEntityKey() != null && !req.getUrl().toString().startsWith(href))
            {
               throw new ApiException(SC.SC_400_BAD_REQUEST, "You are PUT-ing an entity with a different href property than the entity URL you are PUT-ing to.");
            }

            href = upsert(req, collection, obj);
            entityKeys.add(href);
         }

         res.withChanges(changes);

         //-- take all of the hrefs and combine into a 
         //-- single href for the "Location" header

         ArrayNode array = new ArrayNode();
         res.getJson().put("data", array);

         res.withStatus(SC.SC_201_CREATED);
         StringBuffer buff = new StringBuffer("");
         for (int i = 0; i < entityKeys.size(); i++)
         {
            String entityKey = entityKeys.get(i);

            String href = Chain.buildLink(collection, entityKey, null);

            boolean added = false;
            //            if (expandResponse)
            //            {
            //               Response resp = service.get(href);
            //               if (resp != null)
            //               {
            //                  ObjectNode js = resp.getJson();
            //                  if (js != null)
            //                  {
            //                     js = js.getNode("data");
            //                     if (js instanceof ArrayNode && ((ArrayNode) js).length() == 1)
            //                     {
            //                        array.add(((ArrayNode) js).get(0));
            //                        added = true;
            //                     }
            //                  }
            //                  else
            //                  {
            //                     System.out.println("what?");
            //                  }
            //               }
            //               else
            //               {
            //                  System.out.println("what?");
            //               }
            //            }

            if (!added)
            {
               array.add(new ObjectNode("href", href));
            }

            String nextId = href.substring(href.lastIndexOf("/") + 1, href.length());
            buff.append(",").append(nextId);
         }

         if(buff.length() > 0)
         {
            String location = Chain.buildLink(collection, buff.substring(1, buff.length()), null);
            res.withHeader("Location", location);
         }
        
      }
      finally
      {
         // don't do this anymore, connection will be committed/rollbacked and closed in the Service class
         //SqlUtils.close(conn);
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
      //      if (Utils.empty(value))
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
      //         value = Utils.date(value.toString());
      //      }
      //
      //      return value;
   }

   public boolean isCollapseAll()
   {
      return collapseAll;
   }

   public RestPostAction withCollapseAll(boolean collapseAll)
   {
      this.collapseAll = collapseAll;
      return this;
   }

   public boolean isStrictRest()
   {
      return strictRest;
   }

   public RestPostAction withStrictRest(boolean strictRest)
   {
      this.strictRest = strictRest;
      return this;
   }

   public boolean isExpandResponse()
   {
      return expandResponse;
   }

   public RestPostAction withExpandResponse(boolean expandResponse)
   {
      this.expandResponse = expandResponse;
      return this;
   }

}
