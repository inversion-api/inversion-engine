/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * https://github.com/inversion-api
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
package io.inversion.cloud.action.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;

import io.inversion.cloud.model.Action;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.ApiException;
import io.inversion.cloud.model.Attribute;
import io.inversion.cloud.model.Change;
import io.inversion.cloud.model.Collection;
import io.inversion.cloud.model.Column;
import io.inversion.cloud.model.Endpoint;
import io.inversion.cloud.model.Index;
import io.inversion.cloud.model.JSArray;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Relationship;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.model.Results;
import io.inversion.cloud.model.SC;
import io.inversion.cloud.model.Table;
import io.inversion.cloud.rql.Term;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.service.Engine;
import io.inversion.cloud.utils.Rows.Row;
import io.inversion.cloud.utils.SqlUtils;
import io.inversion.cloud.utils.Utils;

public class RestPostAction extends Action<RestPostAction>
{
   protected boolean collapseAll    = false;
   protected boolean strictRest     = true;
   protected boolean expandResponse = true;

   public RestPostAction()
   {
      this(null);
   }

   public RestPostAction(String inludePaths)
   {
      this(inludePaths, null, null);
   }

   public RestPostAction(String inludePaths, String excludePaths, String config)
   {
      super(inludePaths, excludePaths, config);
      withMethods("PUT,POST");
   }

   @Override
   public void run(Engine engine, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
   {
      if (strictRest)
      {
         if (req.isPost() && req.getEntityKey() != null)
            throw new ApiException(SC.SC_404_NOT_FOUND, "You are trying to POST to a specific entity url.  Set 'strictRest' to false to interpret PUT vs POST intention based on presense of 'href' property in passed in JSON");
         if (req.isPut() && req.getEntityKey() == null)
            throw new ApiException(SC.SC_404_NOT_FOUND, "You are trying to PUT to a collection url.  Set 'strictRest' to false to interpret PUT vs POST intention based on presense of 'href' property in passed in JSON");
      }

      Collection collection = req.getCollection();
      List<Change> changes = new ArrayList();
      List<String> entityKeys = new ArrayList();
      JSNode obj = req.getJson();

      if (obj == null)
         throw new ApiException(SC.SC_400_BAD_REQUEST, "You must pass a JSON body to the PostHandler");

      boolean collapseAll = "true".equalsIgnoreCase(chain.getConfig("collapseAll", this.collapseAll + ""));
      Set<String> collapses = chain.mergeEndpointActionParamsConfig("collapses");

      if (collapseAll || collapses.size() > 0)
      {
         obj = JSNode.parseJsonNode(obj.toString());
         collapse(obj, collapseAll, collapses, "");
      }

      if (obj instanceof JSArray)
      {
         if (!Utils.empty(req.getEntityKey()))
         {
            throw new ApiException(SC.SC_400_BAD_REQUEST, "You can't batch " + req.getMethod() + " an array of objects to a specific resource url.  You must " + req.getMethod() + " them to a collection.");
         }
         entityKeys = upsert(req, collection, (JSArray) obj);
      }
      else
      {
         String href = obj.getString("href");
         if (req.isPut() && href != null && req.getEntityKey() != null && !req.getUrl().toString().startsWith(href))
         {
            throw new ApiException(SC.SC_400_BAD_REQUEST, "You are PUT-ing an entity with a different href property than the entity URL you are PUT-ing to.");
         }

         entityKeys = upsert(req, collection, new JSArray(obj));
      }

      res.withChanges(changes);

      //-- take all of the hrefs and combine into a 
      //-- single href for the "Location" header

      JSArray array = new JSArray();
      res.getJson().put("data", array);

      res.withStatus(SC.SC_201_CREATED);
      StringBuffer buff = new StringBuffer("");
      for (int i = 0; i < entityKeys.size(); i++)
      {
         String entityKey = entityKeys.get(i);

         String href = Chain.buildLink(collection, entityKey, null);

         boolean added = false;

         if (!added)
         {
            array.add(new JSNode("href", href));
         }

         String nextId = href.substring(href.lastIndexOf("/") + 1, href.length());
         buff.append(",").append(nextId);
      }

      if (buff.length() > 0)
      {
         String location = Chain.buildLink(collection, buff.substring(1, buff.length()), null);
         res.withHeader("Location", location);
      }

   }

   /**
    * README README README README
    * 
    * Algorithm:
    * Step 1: For each relationship POST back through the "front door".  This is the primary
    *         recursion that enables nested documents to submitted all at once by client.  Putting
    *         this step first ensure that all new objects are POSTed, with their newly created hrefs
    *         placed back in the JSON prior to any PUTs that depend on relationship keys to exist.
    *
    * Step 2: Upsert all <code>nodes</code> in this generation...meaning not recursively including
    *         key values for all one-to-many foreign keys but excluding all many-to-one and many-to-many
    *         key changes...those many-to-x relationships involve modifying other tables, the many to many 
    *         link tables and the many-to-one table that have foreign keys back to this collections 
    *         table, not the direct modification of the single table underlying this collection.
    *        
    * Step 3: Find the key values for all new/kept many-to-one and many-to-many relationships
    *
    * Step 4.1 Upsert all of those new/kept relationships and create the RQL queries needed find
    *          all relationships NOT in the upserts.
    * 
    * Step 4.2 Null out all now invalid many-to-one foreign keys back to these notes
    *          and delete all now invalid many-to-many relationships rows.
    *   
    * @param req
    * @param collection
    * @param nodes
    * @return
    * @throws Exception
    */
   protected List<String> upsert(Request req, Collection collection, JSArray nodes) throws Exception
   {
      //--
      //--
      //-- Step 1. recurse by relationship in batch batch
      //-- 
      //-- THIS IS THE ONLY RECURSION IN THE ALGORITHM.  IT IS NOT DIRECTLY RECURSIVE. IT
      //-- SENDS THE "CHILD GENERATION" AS A POST BACK TO THE ENGINE WHICH WOULD LAND AT
      //-- AND ACTION (MAYGE THIS ONE) THAT HANDLES THE UPSERT FOR THAT CHILD GENERATION
      //-- AND ITS DESCENDANTS.
      for (Relationship rel : collection.getEntity().getRelationships())
      {
         List childNodes = new ArrayList();

         for (JSNode node : (List<JSNode>) ((JSArray) nodes).asList())
         {
            Object value = node.get(rel.getName());
            if (value instanceof JSArray)
            {
               for (Object child : ((JSArray) value).asList())
               {
                  if (child instanceof JSNode)
                  {
                     childNodes.add(child);
                  }
               }
            }
            else if (value instanceof JSNode)
            {
               childNodes.add((JSNode) value);
            }
         }

         if (childNodes.size() > 0)
         {
            String path = Chain.buildLink(rel.getRelated().getCollection(), null, null);

            Response res = req.getEngine().post(path, new JSArray(childNodes).toString());
            res.dump();

            if (!res.isSuccess() || res.data().length() != childNodes.size())
            {
               throw new ApiException(SC.SC_400_BAD_REQUEST, res.getErrorContent());
            }

            //now get response URLS and set them BACK on the source from this generation
            JSArray data = res.data();
            for (int i = 0; i < data.length(); i++)
            {
               String childHref = ((JSNode) data.get(i)).getString("href");
               ((JSNode) childNodes.get(i)).put("href", childHref);
            }
         }
      }

      //--
      //--
      //-- Step 2. now upsert this generation including one to many relationships
      //--

      List<Map> upsertMaps = new ArrayList();
      for (JSNode node : nodes.asArrayList())
      {
         Map<String, Object> mapped = new HashMap();
         upsertMaps.add(mapped);

         for (Attribute attr : collection.getEntity().getAttributes())
         {
            String attrName = attr.getName();

            //skip relationships first, all relationships will be upserted first
            //to make sure child foreign keys are created
            if (collection.getEntity().getRelationship(attrName) != null)
               continue;

            String colName = attr.getColumn().getName();
            if (node.containsKey(attrName))
            {
               //copied.add(attrName.toLowerCase());
               //copied.add(colName.toLowerCase());

               Object attrValue = node.get(attrName);
               Object colValue = collection.getDb().cast(attr, attrValue);
               mapped.put(colName, colValue);
            }
         }
         for (Relationship rel : collection.getEntity().getRelationships())
         {
            if (rel.isOneToMany() && node.hasProperty(rel.getName()))
            {
               Column column = rel.getFk1Col1();
               String colName = column.getName();

               Object child = node.get(rel.getName());
               Object value = child instanceof String ? child : child instanceof JSNode ? ((JSNode) child).get("href") : null;

               if (!Utils.empty(value))
               {
                  value = Utils.last(Utils.explode("/", value.toString()));
               }
               Object colValue = collection.getDb().cast(column, value);
               mapped.put(colName, colValue);
            }
         }

      }
      List returnList = collection.getDb().upsert(collection.getTable(), upsertMaps);
      for (int i = 0; i < nodes.length(); i++)
      {
         //-- new records need their newly assigned id/href assigned back on them
         if (nodes.getNode(i).get("href") == null)
         {
            nodes.getNode(i).put("href", Chain.buildLink(collection, returnList.get(i) + "", null));
         }
      }

      //--
      //--
      //-- Step 3: Now find all key values to keep for many-to-* relationships
      //-- ... this step just collects them...then next steps updates new and removed relationships
      //--

      MultiKeyMap keepRels = new MultiKeyMap<>(); //-- relationship, parentKey, list of child keys

      for (Relationship rel : collection.getEntity().getRelationships())
      {
         if (rel.isOneToMany())
            continue;

         for (JSNode node : nodes.asArrayList())
         {
            if (!node.hasProperty(rel.getName()) || (node.get(rel.getName()) instanceof String))
               continue;//-- this property was not passed back in...if it is string it is the link to expand the relationship

            String href = node.getString("href");

            if (href == null)
               throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "The child href should not be null at this point, this looks like an algorithm error.");

            Table parentTbl = collection.getEntity().getTable();
            Row parentPk = parentTbl.decodeKey(href);
            Map parentKey = mapTo(parentPk, parentTbl.getPrimaryIndex(), rel.getFkIndex1());
            keepRels.put(rel, parentKey, new ArrayList());//there may not be any child nodes...this has to be added here so it will be in the loop later

            JSArray childNodes = node.getArray(rel.getName());
            for (int i = 0; childNodes != null && i < childNodes.length(); i++)
            {
               Object childHref = childNodes.get(i);
               childHref = childHref instanceof JSNode ? ((JSNode) childHref).get("href") : childHref;

               if (!Utils.empty(childHref))
               {
                  String childEk = (String) Utils.last(Utils.explode("/", childHref.toString()));
                  Row childPk = rel.getRelated().getTable().decodeKey(childEk);

                  if (rel.isManyToOne())
                  {
                     ((ArrayList) keepRels.get(rel, parentKey)).add(childPk);
                  }
                  else if (rel.isManyToMany())
                  {
                     Map childFk = mapTo(childPk, rel.getRelated().getTable().getPrimaryIndex(), rel.getFkIndex2());
                     ((ArrayList) keepRels.get(rel, parentKey)).add(childFk);
                  }
               }
            }
         }
      }

      //--
      //-- Step 4 - 
      //--   1. upsert all new and kept relationships
      //--   2. null out all now invalid many-to-one relationships
      //--      AND delete all now invalid many-to-many relationships
      //--
      //--   To update/delete all now invlaid relationships, we are going to construct
      //--   an RQL query to find all relationships that are NOT in the list we upserted
      //--   in step 4.1
      //--   
      //--   The RQL might look like this
      //-- 
      //--   or( 
      //--        and(eq(parentFkX, nodeX.href), not(or(eq(childPk1.1, child.href1.1), eq(childPk1.2, child.href1.2), eq(childPk1.3, child.href1.3)))),
      //--        and(eq(parentFKY, nodeY.href), not(or(eq(childPk2.1, child.href2.1), eq(childPk2.2, child.href2.2)))),
      //--        and(eq(parentFKY, nodeY.href), not(or(eq(childPk2.1, child.href2.1), eq(childPk2.2, child.href2.2)))),
      //--     )
      //--
      Term findOr = Term.term(null, "or");
      for (MultiKey mkey : (Set<MultiKey>) keepRels.keySet())
      {
         Relationship rel = (Relationship) mkey.getKey(0);
         Map parentKey = (Map) mkey.getKey(1);
         List<Map> childKeys = (List) keepRels.get(rel, parentKey);

         List upserts = new ArrayList();

         //-- this set will contain the columns we need to update/delete outdated relationships
         Set includesKeys = new HashSet();
         includesKeys.addAll(parentKey.keySet());

         Term childNot = Term.term(null, "not");
         Term childOr = Term.term(childNot, "or");

         for (Map childKey : childKeys)
         {
            Map upsert = new HashMap();
            upsert.putAll(parentKey);
            upsert.putAll(childKey);
            upserts.add(upsert);

            includesKeys.addAll(childKey.keySet());
            childOr.withTerm(asTerm(childKey));
         }

         Table table = rel.isManyToOne() ? rel.getRelated().getTable() : rel.getFk1Col1().getTable();
         log.debug("updating relationship: " + rel + " -> " + table + " -> " + upserts);
         table.getDb().upsert(table, upserts);

         //-- now find all relationships that are NOT in the group that we just upserted
         //-- they need to be nulled out if many-to-one and deleted if many-to-many

         if (childOr.size() > 0)
            findOr.withTerm(Term.term(findOr, "and", asTerm(parentKey), childNot));
         else
            findOr.withTerm(asTerm(parentKey));

         if (findOr.size() == 1)
            findOr = findOr.getTerm(0);

         List queryTerms = new ArrayList(Arrays.asList(Term.term(null, "includes", includesKeys), Term.term(null, "limit", 100), findOr));

         while (true)
         {
            log.debug("...looking for many-to-* outdated relationships: " + rel + " -> " + queryTerms);

            Results<Row> results = table.getDb().select(table, queryTerms);

            if (results.size() <= 0)
               break;

            if (rel.isManyToOne())
            {
               for (Row row : results.getRows())
               {
                  for (Column col : rel.getFkIndex1().getColumns())
                  {
                     row.put(col.getName(), null);
                  }
               }

               log.debug("...nulling out many-to-one outdated relationships foreign keys: " + rel + " -> " + table + " -> " + results.getRows());
               table.getDb().upsert(table, results.getRows());
            }
            else if (rel.isManyToMany())
            {
               log.debug("...deleting outdated many-to-many relationships rows: " + rel + " -> " + table + " -> " + results.getRows());
               table.getDb().delete(table, table.getPrimaryIndex(), results.getRows());
            }

            if (results.size() < 100)
               break;
         }
      }

      return returnList;
   }

   Map mapTo(Map srcRow, Index srcCols, Index destCols)
   {
      if (srcCols != destCols)
      {
         for (int i = 0; i < srcCols.size(); i++)
         {
            String key = srcCols.getColumn(i).getName();
            Object value = srcRow.remove(key);
            srcRow.put(destCols.getColumn(i).getName(), value);
         }
      }

      return srcRow;
   }

   Term asTerm(Map row)
   {
      Term t = null;
      for (Object key : row.keySet())
      {
         Object value = row.get(key);

         if (t == null)
         {
            t = Term.term(null, "eq", key, value);
         }
         else
         {
            if (!t.hasToken("and"))
               t = Term.term(null, "and", t);

            t.withTerm(Term.term(t, "eq", key, value));
         }
      }
      return t;
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
   public static void collapse(JSNode parent, boolean collapseAll, Set collapses, String path)
   {
      for (String key : (List<String>) new ArrayList(parent.keySet()))
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

                  if (children.get(i) instanceof JSArray || !(children.get(i) instanceof JSNode))
                  {
                     children.remove(i);
                     i--;
                     continue;
                  }

                  JSNode child = children.getObject(i);
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
            else if (value instanceof JSNode)
            {
               JSNode child = (JSNode) value;
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
         else if (value instanceof JSArray)
         {
            JSArray children = (JSArray) value;
            for (int i = 0; i < children.length(); i++)
            {
               if (children.get(i) instanceof JSNode && !(children.get(i) instanceof JSArray))
               {
                  collapse(children.getObject(i), collapseAll, collapses, nextPath(path, key));
               }
            }
         }
         else if (value instanceof JSNode)
         {
            collapse((JSNode) value, collapseAll, collapses, nextPath(path, key));
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
