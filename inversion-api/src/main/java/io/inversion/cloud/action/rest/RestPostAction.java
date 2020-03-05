/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.cloud.action.rest;

import io.inversion.cloud.model.Collection;
import io.inversion.cloud.model.*;
import io.inversion.cloud.model.Rows.Row;
import io.inversion.cloud.rql.Term;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.utils.Utils;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;

import java.util.*;

public class RestPostAction extends Action<RestPostAction>
{
   protected boolean collapseAll    = false;

   /**
    * When true, forces PUTs to have an entityKey in the URL
    */
   protected boolean strictRest     = false;
   protected boolean expandResponse = true;

   public RestPostAction()
   {
      withMethods("PUT,POST,PATCH");
   }

   @Override
   public void run(Request req, Response res) throws Exception
   {
      if (strictRest)
      {
         if (req.isPost() && req.getEntityKey() != null)
            ApiException.throw404NotFound("You are trying to POST to a specific entity url.  Set 'strictRest' to false to interpret PUT vs POST intention based on presense of 'href' property in passed in JSON");
         if (req.isPut() && req.getEntityKey() == null)
            ApiException.throw404NotFound("You are trying to PUT to a collection url.  Set 'strictRest' to false to interpret PUT vs POST intention based on presense of 'href' property in passed in JSON");
      }

      Collection collection = req.getCollection();
      List<Change> changes = new ArrayList();
      List entityKeys = new ArrayList();
      JSNode obj = req.getJson();

      if (obj == null)
         ApiException.throw400BadRequest("You must pass a JSON body to the PostHandler");

      boolean collapseAll = "true".equalsIgnoreCase(req.getChain().getConfig("collapseAll", this.collapseAll + ""));
      Set<String> collapses = req.getChain().mergeEndpointActionParamsConfig("collapses");

      if (collapseAll || collapses.size() > 0)
      {
         obj = JSNode.parseJsonNode(obj.toString());
         collapse(obj, collapseAll, collapses, "");
      }

      if (obj instanceof JSArray)
      {
         if (!Utils.empty(req.getEntityKey()))
         {
            ApiException.throw400BadRequest("You can't batch '%s' an array of objects to a specific resource url.  You must '%s' them to a collection.", req.getMethod(), req.getMethod());
         }
         entityKeys = upsert(req, collection, (JSArray) obj);
      }
      else
      {
         String href = obj.getString("href");
         if (req.isPut() && href != null && req.getEntityKey() != null && !req.getUrl().toString().startsWith(href))
         {
            ApiException.throw400BadRequest("You are PUT-ing an entity with a different href property than the entity URL you are PUT-ing to.");
         }

         entityKeys = upsert(req, collection, new JSArray(obj));
      }

      res.withChanges(changes);

      //-- take all of the hrefs and combine into a 
      //-- single href for the "Location" header

      JSArray array = new JSArray();
      res.getJson().put("data", array);

      res.withStatus(Status.SC_201_CREATED);
      StringBuffer buff = new StringBuffer("");
      for (int i = 0; i < entityKeys.size(); i++)
      {
         String entityKey = entityKeys.get(i) + "";
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
    *
    * Step 1: Upsert all <code>nodes</code> in this generation...meaning not recursively including
    *         key values for all one-to-many foreign keys but excluding all many-to-one and many-to-many
    *         key changes...those many-to-x relationships involve modifying other tables, the many to many 
    *         link tables and the many-to-one table that have foreign keys back to this collections 
    *         table, not the direct modification of the single table underlying this collection.
    *         
    * Step 2: For each relationship POST back through the "front door".  This is the primary
    *         recursion that enables nested documents to submitted all at once by client.  Putting
    *         this step first ensure that all new objects are POSTed, with their newly created hrefs
    *         placed back in the JSON prior to any PUTs that depend on relationship keys to exist.
   
    * Step 3: Set child foreign keys back on parent generation.       
    *        
    * Step 4: Find the key values for all new/kept many-to-one and many-to-many relationships
    *
    * Step 5.1 Upsert all of those new/kept relationships and create the RQL queries needed find
    *          all relationships NOT in the upserts.
    * 
    * Step 5.2 Null out all now invalid many-to-one foreign keys back to these notes
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
      //-- Step 1. Upsert this generation including one to many relationships where the fk is known
      //--
      List<Map> upsertMaps = new ArrayList();
      for (JSNode node : nodes.asNodeList())
      {
         Map<String, Object> mapped = new HashMap();
         upsertMaps.add(mapped);

         String href = node.getString("href");
         if (href != null)
         {
            Row decodedKey = collection.decodeKey(href);
            mapped.putAll(decodedKey);
         }

         HashSet copied = new HashSet();
         for (Property attr : collection.getProperties())
         {
            String attrName = attr.getJsonName();

            //skip relationships first, all relationships will be upserted first
            //to make sure child foreign keys are created
            if (collection.getRelationship(attrName) != null)
               continue;

            String colName = attr.getColumnName();
            if (node.containsKey(attrName))
            {
               copied.add(attrName.toLowerCase());
               copied.add(colName.toLowerCase());

               Object attrValue = node.get(attrName);
               Object colValue = collection.getDb().cast(attr, attrValue);
               mapped.put(colName, colValue);
            }
         }
         for (Relationship rel : collection.getRelationships())
         {
            copied.add(rel.getName().toLowerCase());

            if (rel.isOneToMany() && node.hasProperty(rel.getName()))
            {
               for (String colName : rel.getFkIndex1().getColumnNames())
               {
                  copied.add(colName.toLowerCase());
               }

               Map foreignKey = mapTo(getKey(rel.getRelated(), node.get(rel.getName())), rel.getRelated().getPrimaryIndex(), rel.getFkIndex1());
               mapped.putAll(foreignKey);
            }
         }

         //-- this pulls in any properties that were supplied in the submitted document 
         //-- but are unknown to the collection/table.  This is necessary to support
         //-- document stores like dynamo/elastic where all columns are not necessarily
         //-- known.
         for (String key : node.keySet())
         {
            if (!copied.contains(key.toLowerCase()))
            {
               if (!key.equals("href"))
                  mapped.put(key, node.get(key));
            }
         }

      }
      List returnList = collection.getDb().upsert(collection, upsertMaps);
      for (int i = 0; i < nodes.length(); i++)
      {
         //-- new records need their newly assigned id/href assigned back on them
         if (nodes.getNode(i).get("href") == null)
         {
            String newHref = Chain.buildLink(collection, returnList.get(i) + "", null);
            nodes.getNode(i).put("href", newHref);
         }
      }

      //--
      //--
      //-- Step 2. recurse by relationship in batch
      //-- 
      //-- THIS IS THE ONLY RECURSION IN THE ALGORITHM.  IT IS NOT DIRECTLY RECURSIVE. IT
      //-- SENDS THE "CHILD GENERATION" AS A POST BACK TO THE ENGINE WHICH WOULD LAND AT
      //-- THE ACTION (MAYBE THIS ONE) THAT HANDLES THE UPSERT FOR THAT CHILD COLLECTION
      //-- AND ITS DESCENDANTS.
      for (Relationship rel : collection.getRelationships())
      {
         Relationship inverse = rel.getInverse();
         List childNodes = new ArrayList();

         for (JSNode node : (List<JSNode>) ((JSArray) nodes).asList())
         {
            Object value = node.get(rel.getName());
            if (value instanceof JSArray) //this is a many-to-*
            {
               JSArray childArr = ((JSArray) value);
               for (int i = 0; i < childArr.size(); i++)
               {
                  //-- removals will be handled in the next section, not this recursion
                  Object child = childArr.get(i);
                  if (child == null)
                     continue;

                  if (child instanceof String)
                  {
                     //-- this was passed in as an href reference, not as an object
                     if (rel.isManyToOne())
                     {
                        //-- the child inverse of this a one-to-many that modifies the child row.  
                        child = new JSArray(child);
                        childArr.set(i, child);
                     }
                     else
                     {
                        //-- don't do anything..the many-to-many section below will update this
                     }
                  }

                  if (child instanceof JSNode)
                  {
                     JSNode childNode = (JSNode) child;
                     if (rel.isManyToOne())
                     {
                        //-- this generations many-to-one, are the next generation's one-to-manys
                        //-- the child generation receives an implicity relationship via nesting
                        //-- under the parent, have to set the inverse prop on the child so 
                        //-- its one-to-many FK gets set to this parent.

                        childNode.put(inverse.getName(), node.getString("href"));
                     }
                     childNodes.add(childNode);

                  }
               }
            }
            else if (value instanceof JSNode)
            {
               //-- this must be a one-to-many...the FK is in this generation, not the child
               JSNode childNode = ((JSNode) value);
               childNodes.add(childNode);
            }
         }

         if (childNodes.size() > 0)
         {
            String path = Chain.buildLink(rel.getRelated(), null, null);
            Response res = req.getEngine().post(path, new JSArray(childNodes).toString());
            if (!res.isSuccess() || res.getData().length() != childNodes.size())
            {
               res.rethrow();
               //throw new ApiException(Status.SC_400_BAD_REQUEST, res.getErrorContent());
            }

            //-- now get response URLS and set them BACK on the source from this generation
            JSArray data = res.getData();
            for (int i = 0; i < data.length(); i++)
            {
               String childHref = ((JSNode) data.get(i)).getString("href");
               ((JSNode) childNodes.get(i)).put("href", childHref);
            }
         }
      }

      //--
      //--
      //-- Step 3. sets foreign keys on parent entities
      //-- 
      //-- ...important for when 
      //-- new child entities are passed in...they won't have an href 
      //-- on the initial record submit..the recursion has to happen to 
      //-- give them an href
      //--
      //-- TODO: can optimize this to not upsert if the key was available
      //-- in the first pass
      for (Relationship rel : collection.getRelationships())
      {
         List<Map> updatedRows = new ArrayList();
         if (rel.isOneToMany())
         {
            //GOAL: set the value of the FK on this one record..then done

            for (JSNode node : nodes.asNodeList())
            {
               Map primaryKey = getKey(collection, node);
               Map foreignKey = mapTo(getKey(rel.getRelated(), node.get(rel.getName())), rel.getRelated().getPrimaryIndex(), rel.getFkIndex1());

               if (foreignKey.size() > 0)
               {
                  Map updatedRow = new HashMap();
                  updatedRow.putAll(primaryKey);
                  updatedRow.putAll(foreignKey);
                  updatedRows.add(updatedRow);
               }
               else
               {
                  //this FK value was not provided by the caller
               }
            }

            if (updatedRows.size() > 0)
            {
               collection.getDb().update(collection, updatedRows);
            }
         }
      }

      //--
      //--
      //-- Step 4: Now find all key values to keep for many-to-* relationships
      //-- ... this step just collects them...then next steps updates new and removed relationships
      //--

      MultiKeyMap keepRels = new MultiKeyMap<>(); //-- relationship, table, parentKey, list of childKeys

      for (Relationship rel : collection.getRelationships())
      {
         if (rel.isOneToMany())//these were handled above
            continue;

         for (JSNode node : nodes.asNodeList())
         {
            if (!node.hasProperty(rel.getName()) || node.get(rel.getName()) instanceof String)
               continue;//-- this property was not passed back in...if it is string it is the link to expand the relationship

            String href = node.getString("href");

            if (href == null)
               ApiException.throw500InternalServerError("The child href should not be null at this point, this looks like an algorithm error.");

            Collection parentTbl = collection;
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
                  Row childPk = rel.getRelated().decodeKey(childEk);

                  if (rel.isManyToOne())
                  {
                     ((ArrayList) keepRels.get(rel, parentKey)).add(childPk);
                  }
                  else if (rel.isManyToMany())
                  {
                     Map childFk = mapTo(childPk, rel.getRelated().getPrimaryIndex(), rel.getFkIndex2());
                     ((ArrayList) keepRels.get(rel, parentKey)).add(childFk);
                  }
               }

            }
         }
      }

      //--
      //-- Step 5 - 
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

      for (MultiKey mkey : (Set<MultiKey>) keepRels.keySet())
      {
         Relationship rel = (Relationship) mkey.getKey(0);
         Map parentKey = (Map) mkey.getKey(1);
         List<Map> childKeys = (List) keepRels.get(rel, parentKey);

         List upserts = new ArrayList();

         //-- this set will contain the columns we need to update/delete outdated relationships
         Set includesKeys = new HashSet();
         includesKeys.addAll(parentKey.keySet());
         includesKeys.addAll(rel.getRelated().getPrimaryIndex().getColumnNames());

         Term findOr = Term.term(null, "or");
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

         //-- TODO: I don't think you need to do this...the recursive generation already did it...
         Collection coll = rel.isManyToOne() ? rel.getRelated() : rel.getFk1Col1().getCollection();
         if (rel.isManyToOne())
         {
            log.debug("updating relationship: " + rel + " -> " + coll + " -> " + upserts);
            coll.getDb().update(coll, upserts);
         }
         else if (rel.isManyToMany())
         {
            log.debug("updating relationship: " + rel + " -> " + coll + " -> " + upserts);
            coll.getDb().upsert(coll, upserts);
         }

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

            Results<Row> results = coll.getDb().select(coll, queryTerms);

            if (results.size() <= 0)
               break;

            if (rel.isManyToOne())
            {
               for (Row row : results.getRows())
               {
                  for (int i = 0; i < rel.getFkIndex1().size(); i++)
                  {
                     Property col = rel.getFkIndex1().getColumn(i);
                     row.put(col.getColumnName(), null);
                  }
               }

               log.debug("...nulling out many-to-one outdated relationships foreign keys: " + rel + " -> " + coll + " -> " + results.getRows());
               coll.getDb().update(coll, results.getRows());
            }
            else if (rel.isManyToMany())
            {
               log.debug("...deleting outdated many-to-many relationships rows: " + rel + " -> " + coll + " -> " + results.getRows());
               Rows rows = new Rows();
               rows.addAll(results.getRows());
               coll.getDb().delete(coll, rows);
            }

            if (results.size() < 100)
               break;
         }
      }

      return returnList;
   }

   String getHref(Object hrefOrNode)
   {
      if (hrefOrNode instanceof JSNode)
         hrefOrNode = ((JSNode) hrefOrNode).get("href");

      if (hrefOrNode instanceof String)
         return (String) hrefOrNode;

      return null;
   }

   Map getKey(Collection table, Object node)
   {
      if (node instanceof JSNode)
         node = ((JSNode) node).getString("href");

      if (node instanceof String)
         return table.decodeKey((String) node);

      return null;
   }

   Map mapTo(Map srcRow, Index srcCols, Index destCols)
   {
      if (srcCols.size() != destCols.size())
         ApiException.throw500InternalServerError("Unable to map from index '%s' to '%s'", srcCols.toString(), destCols);

      if (srcRow == null)
         return Collections.EMPTY_MAP;

      if (srcCols != destCols)
      {
         for (int i = 0; i < srcCols.size(); i++)
         {
            String key = srcCols.getColumn(i).getColumnName();
            Object value = srcRow.remove(key);
            srcRow.put(destCols.getColumn(i).getColumnName(), value);
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
