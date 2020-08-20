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
package io.inversion.action.db;

import io.inversion.Collection;
import io.inversion.*;
import io.inversion.rql.Term;
import io.inversion.utils.JSArray;
import io.inversion.utils.JSNode;
import io.inversion.utils.Rows.Row;
import io.inversion.utils.Utils;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;

import java.util.*;

public class DbPostAction<t extends DbPostAction> extends Action<t> {
    protected boolean collapseAll = false;

    /**
     * When true, forces PUTs to have an resourceKey in the URL
     */
    protected boolean strictRest     = false;
    protected boolean expandResponse = true;

    @Override
    public void run(Request req, Response res) throws ApiException {
        if (req.isMethod("PUT", "POST")) {
            upsert(req, res);
        } else if (req.isMethod("PATCH")) {
            patch(req, res);
        } else {
            throw ApiException.new400BadRequest("Method '%' is not supported by RestPostHandler");
        }

    }

    /**
     * Unlike upsert for POST/PUT, this method is specifically NOT recursive for patching
     * nested documents. It will only patch the parent collection/table.
     * <p>
     * TODO: add support for JSON patching...maybe
     *
     * @param req
     * @param res
     * @throws ApiException
     */
    public void patch(Request req, Response res) throws ApiException {
        JSNode body = req.getJson();
        if (body.isArray()) {
            if (!Utils.empty(req.getResourceKey())) {
                throw ApiException.new400BadRequest("You can't batch '{}' an array of objects to a specific resource url.  You must '{}' them to a collection.", req.getMethod(), req.getMethod());
            }
        } else {
            String href = body.getString("href");
            if (req.getResourceKey() != null) {
                if (href == null)
                    body.put("href", Utils.substringBefore(req.getUrl().toString(), "?"));
                else if (!req.getUrl().toString().startsWith(href))
                    throw ApiException.new400BadRequest("You are PATCHING-ing an resource with a different href property than the resource URL you are PATCHING-ing to.");
            }
        }

        List<String> resourceKeys = req.getCollection().getDb().patch(req.getCollection(), req.getJson().asNodeList());

        if (resourceKeys.size() > 0) {
            String location = Chain.buildLink(req.getCollection(), Utils.implode(",", resourceKeys), null);
            res.withHeader("Location", location);
        }

    }

    public void upsert(Request req, Response res) throws ApiException {
        if (strictRest) {
            if (req.isPost() && req.getResourceKey() != null)
                throw ApiException.new404NotFound("You are trying to POST to a specific resource url.  Set 'strictRest' to false to interpret PUT vs POST intention based on presense of 'href' property in passed in JSON");
            if (req.isPut() && req.getResourceKey() == null)
                throw ApiException.new404NotFound("You are trying to PUT to a collection url.  Set 'strictRest' to false to interpret PUT vs POST intention based on presense of 'href' property in passed in JSON");
        }

        Collection   collection   = req.getCollection();
        List<Change> changes      = new ArrayList<>();
        List         resourceKeys = new ArrayList<>();
        JSNode       obj          = req.getJson();

        if (obj == null)
            throw ApiException.new400BadRequest("You must pass a JSON body to the RestPostHandler");

        boolean     collapseAll = "true".equalsIgnoreCase(req.getChain().getConfig("collapseAll", this.collapseAll + ""));
        Set<String> collapses   = req.getChain().mergeEndpointActionParamsConfig("collapses");

        if (collapseAll || collapses.size() > 0) {
            obj = JSNode.parseJsonNode(obj.toString());
            collapse(obj, collapseAll, collapses, "");
        }

        if (obj instanceof JSArray) {
            if (!Utils.empty(req.getResourceKey())) {
                throw ApiException.new400BadRequest("You can't batch '{}' an array of objects to a specific resource url.  You must '{}' them to a collection.", req.getMethod(), req.getMethod());
            }
            resourceKeys = upsert(req, collection, (JSArray) obj);
        } else {
            String href = obj.getString("href");
            if (req.isPut() && href != null && req.getResourceKey() != null && !req.getUrl().toString().startsWith(href)) {
                throw ApiException.new400BadRequest("You are PUT-ing an resource with a different href property than the resource URL you are PUT-ing to.");
            }

            resourceKeys = upsert(req, collection, new JSArray(obj));
        }

        res.withChanges(changes);

        //-- take all of the hrefs and combine into a
        //-- single href for the "Location" header

        JSArray array = new JSArray();
        res.getJson().put("data", array);

        res.withStatus(Status.SC_201_CREATED);
        StringBuilder buff = new StringBuilder();
        for (int i = 0; i < resourceKeys.size(); i++) {
            String resourceKey = resourceKeys.get(i) + "";
            String href        = Chain.buildLink(collection, resourceKey, null);

            boolean added = false;

            if (!added) {
                array.add(new JSNode("href", href));
            }

            String nextId = href.substring(href.lastIndexOf("/") + 1, href.length());
            buff.append(",").append(nextId);
        }

        if (buff.length() > 0) {
            String location = Chain.buildLink(collection, buff.substring(1, buff.length()), null);
            res.withHeader("Location", location);
        }

    }

    /**
     * README README README README
     * <p>
     * Algorithm:
     * <p>
     * Step 1: Upsert all <code>nodes</code> in this generation...meaning not recursively including
     * key values for all many-to-one foreign keys but excluding all one-to-many and many-to-many
     * key changes...non many-to-one relationships involve modifying other tables that have foreign
     * keys back to this collection's table, not the direct modification of the single table
     * underlying this collection.
     * <p>
     * Step 2: For each relationship POST back through the "front door".  This is the primary
     * recursion that enables nested documents to submitted all at once by client.  Putting
     * this step first ensure that all new objects are POSTed, with their newly created hrefs
     * placed back in the JSON prior to any PUTs that depend on relationship keys to exist.
     * <p>
     * Step 3: PKs generated for child documents which are actually relationship parents, are set
     * as foreign keys back on the parent json (which is actully the one-to-many child)
     * <p>
     * Step 4: Find the key values for all new/kept one-to-many and many-to-many relationships
     * <p>
     * Step 5.1 Upsert all of those new/kept relationships and create the RQL queries needed find
     * all relationships NOT in the upserts.
     * <p>
     * Step 5.2 Null out all now invalid many-to-one foreign keys back
     * and delete all now invalid many-to-many relationships rows.
     *
     * @param req
     * @param collection
     * @param nodes
     * @return
     * @throws ApiException
     */
    protected List<String> upsert(Request req, Collection collection, JSArray nodes) throws ApiException {
        //--
        //--
        //-- Step 1. Upsert this generation including many-to-one relationships where the fk is known
        //--

        System.out.println("UPSERT: " + collection.getName() + ":\r\n" + nodes);

        List<String> returnList = collection.getDb().upsert(collection, nodes.asList());
        for (int i = 0; i < nodes.length(); i++) {
            //-- new records need their newly assigned id/href assigned back on them
            if (nodes.getNode(i).get("href") == null) {
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
        for (Relationship rel : collection.getRelationships()) {
            Relationship inverse    = rel.getInverse();
            List         childNodes = new ArrayList<>();

            for (JSNode node : nodes.asNodeList()) {
                Object value = node.get(rel.getName());
                if (value instanceof JSArray) //this is a one-to-many or many-to-many
                {
                    JSArray childArr = ((JSArray) value);
                    for (int i = 0; i < childArr.size(); i++) {
                        //-- removals will be handled in the next section, not this recursion
                        Object child = childArr.get(i);
                        if (child == null)
                            continue;

                        if (child instanceof String) {
                            //-- this was passed in as an href reference, not as an object
                            if (rel.isOneToMany()) {
                                //-- the child inverse of this is a many-to-one that modifies the child row.
                                child = new JSArray(child);
                                childArr.set(i, child);
                            } else {
                                //-- don't do anything..the many-to-many section below will update this
                            }
                        }

                        if (child instanceof JSNode) {
                            JSNode childNode = (JSNode) child;
                            if (rel.isOneToMany()) {
                                //-- this generations one-to-many, are the next generation's many-to-ones
                                //-- the child generation receives an implicity relationship via nesting
                                //-- under the parent, have to set the inverse prop on the child so
                                //-- its many-to-one FK gets set to this parent.

                                childNode.put(inverse.getName(), node.getString("href"));
                            }
                            childNodes.add(childNode);

                        }
                    }
                } else if (value instanceof JSNode) {
                    //-- this must be a many-to-one...the FK is in this generation, not the child
                    JSNode childNode = ((JSNode) value);
                    childNodes.add(childNode);
                }
            }

            if (childNodes.size() > 0) {
                String   path = Chain.buildLink(rel.getRelated(), null, null);
                Response res  = req.getEngine().post(path, new JSArray(childNodes));
                if (!res.isSuccess() || res.getData().length() != childNodes.size()) {
                    res.rethrow();
                    //throw new ApiException(Status.SC_400_BAD_REQUEST, res.getErrorContent());
                }

                //-- now get response URLS and set them BACK on the source from this generation
                JSArray data = res.getData();
                for (int i = 0; i < data.length(); i++) {
                    String childHref = ((JSNode) data.get(i)).getString("href");
                    ((JSNode) childNodes.get(i)).put("href", childHref);
                }
            }
        }

        //--
        //--
        //-- Step 3. sets foreign keys on json parent entities..this happens
        //-- when a JSON parent is actually a ONE_TO_MANY child.
        //--
        //-- ...important for when
        //-- new ONE_TO_MANY entities are passed in...they won't have an href
        //-- on the initial record submit..the recursion has to happen to
        //-- give them an href
        //--
        //-- TODO: can optimize this to not upsert if the key was available
        //-- in the first pass
        for (Relationship rel : collection.getRelationships()) {
            List<Map> updatedRows = new ArrayList<>();
            if (rel.isManyToOne())//this means we have a FK to the related element's PK
            {
                for (JSNode node : nodes.asNodeList()) {
                    Map primaryKey         = collection.getDb().getKey(collection, node);
                    Map foreignResourceKey = collection.getDb().getKey(rel.getRelated(), node.get(rel.getName()));

                    Index foreignIdx        = rel.getFkIndex1();
                    Index relatedPrimaryIdx = rel.getRelated().getPrimaryIndex();

                    Object docChild = node.get(rel.getName());
                    if (docChild instanceof JSNode) {
                        Map updatedRow = new HashMap<>();
                        updatedRows.add(updatedRow);
                        updatedRow.putAll(primaryKey);

                        if (foreignIdx.size() != relatedPrimaryIdx.size() && foreignIdx.size() == 1) {
                            //-- the fk is an resourceKey not a one-to-one column mapping to the primary composite key
                            updatedRow.put(foreignIdx.getProperty(0).getColumnName(), rel.getRelated().encodeResourceKey(foreignResourceKey));
                        } else {
                            Map foreignKey = collection.getDb().mapTo(foreignResourceKey, rel.getRelated().getPrimaryIndex(), rel.getFkIndex1());
                            updatedRow.putAll(foreignKey);
                        }
                    }
                }

                if (updatedRows.size() > 0) {
                    //-- don't need to "go back through the front door and PATCH to the engine
                    //-- here because we are updating our own collection.
                    //-- TODO...make sure of above statement.
                    collection.getDb().doPatch(collection, updatedRows);
                }
            }
        }

        //--
        //--
        //-- Step 4: Now find all key values to keep for one-to-many and many-to-many relationships
        //-- ... this step just collects them...then next steps updates new and removed relationships
        //--

        MultiKeyMap keepRels = new MultiKeyMap<>(); //-- relationship, table, parentKey, list of childKeys

        for (Relationship rel : collection.getRelationships()) {
            if (rel.isManyToOne())//these were handled in step 1 and 2
                continue;

            for (JSNode node : nodes.asNodeList()) {
                if (!node.hasProperty(rel.getName()) || node.get(rel.getName()) instanceof String)
                    continue;//-- this property was not passed back in...if it is string it is the link to expand the relationship

                String href = node.getString("href");

                if (href == null)
                    throw ApiException.new500InternalServerError("The child href should not be null at this point, this looks like an algorithm error.");

                Collection parentTbl = collection;
                Row        parentPk  = parentTbl.decodeResourceKey(href);
                Map        parentKey = collection.getDb().mapTo(parentPk, parentTbl.getPrimaryIndex(), rel.getFkIndex1());

                keepRels.put(rel, parentKey, new ArrayList<>());//there may not be any child nodes...this has to be added here so it will be in the loop later

                JSArray childNodes = node.getArray(rel.getName());

                for (int i = 0; childNodes != null && i < childNodes.length(); i++) {
                    Object childHref = childNodes.get(i);
                    childHref = childHref instanceof JSNode ? ((JSNode) childHref).get("href") : childHref;

                    if (!Utils.empty(childHref)) {
                        String childEk = (String) Utils.last(Utils.explode("/", childHref.toString()));
                        Row    childPk = rel.getRelated().decodeResourceKey(childEk);

                        if (rel.isOneToMany()) {
                            ((ArrayList) keepRels.get(rel, parentKey)).add(childPk);
                        } else if (rel.isManyToMany()) {
                            Map childFk = collection.getDb().mapTo(childPk, rel.getRelated().getPrimaryIndex(), rel.getFkIndex2());
                            ((ArrayList) keepRels.get(rel, parentKey)).add(childFk);
                        }
                    }

                }
            }
        }

        //--
        //-- Step 5 -
        //--   1. upsert all new and kept relationships
        //--   2. null out all now invalid many-to-one foreign keys
        //--      AND delete all now invalid many-to-many rows
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

        for (MultiKey mkey : (Set<MultiKey>) keepRels.keySet()) {
            Relationship rel       = (Relationship) mkey.getKey(0);
            Map          parentKey = (Map) mkey.getKey(1);
            List<Map>    childKeys = (List) keepRels.get(rel, parentKey);

            List upserts = new ArrayList<>();

            //-- this set will contain the columns we need to update/delete outdated relationships
            Set includesKeys = new HashSet();
            includesKeys.addAll(parentKey.keySet());
            includesKeys.addAll(rel.getRelated().getPrimaryIndex().getColumnNames());

            Term childNot = Term.term(null, "not");
            Term childOr  = Term.term(childNot, "or");

            for (Map childKey : childKeys) {
                Map upsert = new HashMap<>();
                upsert.putAll(parentKey);
                upsert.putAll(childKey);
                upserts.add(upsert);

                includesKeys.addAll(childKey.keySet());
                childOr.withTerm(asTerm(childKey));
            }

            //-- TODO: I don't think you need to do this...the recursive generation already did it...
            Collection coll = rel.isOneToMany() ? rel.getRelated() : rel.getFk1Col1().getCollection();
            if (rel.isOneToMany()) {
                //TODO: go through front door?
                log.debug("updating relationship: " + rel + " -> " + coll + " -> " + upserts);
                coll.getDb().doPatch(coll, upserts);
            } else if (rel.isManyToMany()) {
                //TODO: go through front door?
                log.debug("updating relationship: " + rel + " -> " + coll + " -> " + upserts);
                coll.getDb().doUpsert(coll, upserts);
            }

            //-- now find all relationships that are NOT in the group that we just upserted
            //-- they need to be nulled out if many-to-one and deleted if many-to-many

            Map<String, String> queryTerms = new HashMap<>();
            queryTerms.put("limit", "100");
            queryTerms.put("includes", Utils.implode(",", includesKeys));

            for (Object parentKeyProp : parentKey.keySet()) {
                queryTerms.put(parentKeyProp.toString(), parentKey.get(parentKeyProp).toString());
            }

            if (childOr.size() > 0) {
                queryTerms.put(childNot.toString(), null);
            }

            String next = Chain.buildLink(coll);
            while (true) {
                log.debug("...looking for one-to-many and many-to-many foreign keys: " + rel + " -> " + queryTerms);

                Response toUnlink = req.getEngine().get(next, queryTerms).assertOk();

                if (toUnlink.data().length() == 0)
                    break;

                if (rel.isOneToMany()) {
                    for (JSNode node : toUnlink.data().asNodeList()) {
                        for (String prop : rel.getFkIndex1().getJsonNames()) {
                            node.put(prop, null);
                        }
                    }
                    req.getEngine().patch(Chain.buildLink(coll), toUnlink.data());

                }
                //TODO: put back in support for many to many rels recursing through engine
                else if (rel.isManyToMany()) {
                    List resourceKeys = new ArrayList<>();
                    for (JSNode node : toUnlink.data().asNodeList()) {
                        resourceKeys.add(Utils.substringAfter(node.getString("href"), "/"));
                    }

                    String url = Chain.buildLink(coll) + "/" + Utils.implode(",", resourceKeys);
                    req.getEngine().delete(url);
                }

                if (toUnlink.data().size() < 100)
                    break;
            }
        }

        return returnList;
    }

    //   String getHref(Object hrefOrNode)
    //   {
    //      if (hrefOrNode instanceof JSNode)
    //         hrefOrNode = ((JSNode) hrefOrNode).get("href");
    //
    //      if (hrefOrNode instanceof String)
    //         return (String) hrefOrNode;
    //
    //      return null;
    //   }

    //   Map getKey(Collection table, Object node)
    //   {
    //      if (node instanceof JSNode)
    //         node = ((JSNode) node).getString("href");
    //
    //      if (node instanceof String)
    //         return table.decodeResourceKey((String) node);
    //
    //      return null;
    //   }

    //   Map mapTo(Map srcRow, Index srcCols, Index destCols)
    //   {
    //      if (srcCols.size() != destCols.size() && destCols.size() == 1)
    //      {
    //         //when the foreign key is only one column but the related primary key is multiple
    //         //columns, encode the FK as an resourceKey.
    //         String resourceKey = Collection.encodeResourceKey(srcRow, srcCols);
    //
    //         for(Object key : srcRow.keySet())
    //            srcRow.remove(key);
    //
    //         srcRow.put(destCols.getProperty(0).getColumnName(), resourceKey);
    //      }
    //      else
    //      {
    //         if (srcCols.size() != destCols.size())
    //            throw ApiException.new500InternalServerError("Unable to map from index '{}' to '{}'", srcCols.toString(), destCols);
    //
    //         if (srcRow == null)
    //            return Collections.EMPTY_MAP;
    //
    //         if (srcCols != destCols)
    //         {
    //            for (int i = 0; i < srcCols.size(); i++)
    //            {
    //               String key = srcCols.getProperty(i).getColumnName();
    //               Object value = srcRow.remove(key);
    //               srcRow.put(destCols.getProperty(i).getColumnName(), value);
    //            }
    //         }
    //      }
    //      return srcRow;
    //   }

    Term asTerm(Map row) {
        Term t = null;
        for (Object key : row.keySet()) {
            Object value = row.get(key);

            if (t == null) {
                t = Term.term(null, "eq", key, value);
            } else {
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
     * object in the case of a ONE_TO_MANY relationship).
     *
     * This is intended to be used as a reciprocal to GetHandler "expands" when
     * a client does not want to scrub their json model before posting changes to
     * the parent document back to the parent collection.
     */
    public static void collapse(JSNode parent, boolean collapseAll, Set collapses, String path) {
        for (String key : (List<String>) new ArrayList(parent.keySet())) {
            Object value = parent.get(key);

            if (collapseAll || collapses.contains(nextPath(path, key))) {
                if (value instanceof JSArray) {
                    JSArray children = (JSArray) value;
                    if (children.length() == 0)
                        parent.remove(key);

                    for (int i = 0; i < children.length(); i++) {
                        if (children.get(i) == null) {
                            children.remove(i);
                            i--;
                            continue;
                        }

                        if (children.get(i) instanceof JSArray || !(children.get(i) instanceof JSNode)) {
                            children.remove(i);
                            i--;
                            continue;
                        }

                        JSNode child = children.getNode(i);
                        for (String key2 : (List<String>) new ArrayList(child.keySet())) {
                            if (!key2.equalsIgnoreCase("href")) {
                                child.remove(key2);
                            }
                        }

                        if (child.keySet().size() == 0) {

                            children.remove(i);
                            i--;
                            continue;
                        }
                    }
                    if (children.length() == 0)
                        parent.remove(key);

                } else if (value instanceof JSNode) {
                    JSNode child = (JSNode) value;
                    for (String key2 : (List<String>) new ArrayList(child.keySet())) {
                        if (!key2.equalsIgnoreCase("href")) {
                            child.remove(key2);
                        }
                    }
                    if (child.keySet().size() == 0)
                        parent.remove(key);
                }
            } else if (value instanceof JSArray) {
                JSArray children = (JSArray) value;
                for (int i = 0; i < children.length(); i++) {
                    if (children.get(i) instanceof JSNode && !(children.get(i) instanceof JSArray)) {
                        collapse(children.getNode(i), collapseAll, collapses, nextPath(path, key));
                    }
                }
            } else if (value instanceof JSNode) {
                collapse((JSNode) value, collapseAll, collapses, nextPath(path, key));
            }

        }
    }

    public static String nextPath(String path, String next) {
        return Utils.empty(path) ? next : path + "." + next;
    }

    public boolean isCollapseAll() {
        return collapseAll;
    }

    public DbPostAction withCollapseAll(boolean collapseAll) {
        this.collapseAll = collapseAll;
        return this;
    }

    public boolean isStrictRest() {
        return strictRest;
    }

    public DbPostAction withStrictRest(boolean strictRest) {
        this.strictRest = strictRest;
        return this;
    }

    public boolean isExpandResponse() {
        return expandResponse;
    }

    public DbPostAction withExpandResponse(boolean expandResponse) {
        this.expandResponse = expandResponse;
        return this;
    }

}
