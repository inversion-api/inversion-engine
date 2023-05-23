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
import io.inversion.json.JSList;
import io.inversion.json.JSMap;
import io.inversion.json.JSNode;
import io.inversion.query.Page;
import io.inversion.rql.Term;
import io.inversion.utils.*;
import io.inversion.utils.Utils;
import io.inversion.utils.KeyValue;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.keyvalue.DefaultKeyValue;
import org.apache.commons.collections4.map.MultiKeyMap;
import io.inversion.utils.ListMap;

import java.util.*;

public class DbGetAction<A extends DbGetAction> extends Action<A>  {

    protected int maxRows = 100;

    public DbGetAction() {
//        Param expand = new Param();
//        expand.withDescription("An optional comma separated lists of relationship names that should be expanded in the response. You can reference any number of nesting using 'dot' path notation.");
//        expand.withIn(Param.In.QUERY);
//        expand.withKey("expand");
//        withParam(expand);

        Param page = new Param();
        page.withDescription("An optional value used to compute the 'offset' of the first item returned as 'offset'='pageNumber'*'pageSize'.  If an 'offset' parameter is also supplied it will be used instead of the 'pageNumber' parameter.");
        page.withKey("pageNumber");
        page.withIn(Param.In.QUERY);
        withParam(page);

        Param size = new Param();
        size.withDescription("An optional number of items to return.  Unless overridden by other configuration the default value is '100'");
        size.withKey("pageSize");
        size.withIn(Param.In.QUERY);
        withParam(size);

        Param offset = new Param();
        offset.withDescription("An optional value used to compute the offset.  This value overrides the 'pageNumber' parameters.");
        offset.withKey("offset");
        offset.withIn(Param.In.QUERY);
        withParam(offset);


        Param sort = new Param();
        sort.withDescription("An optional comma separated list of json property names used to order the results.  Each property may optionally be prefixed with '-' to specify descending order.");
        sort.withKey("sort");
        sort.withIn(Param.In.QUERY);
        withParam(sort);

        //TODO build a somewhat real example from the collectin attributes
        //q.setExample("q=eq(jsonPropertyName,value1),in(anotherJsonProperty,value2)");
        Param  q    = new Param();
        String desc = "An RQL formatted filter statement that allows you to retrieve only the specific resources you require.  See 'Overview->Querying' for more documentation on available functions and syntax.";
        q.withDescription(desc);
        q.withKey("q");
        q.withIn(Param.In.QUERY);
        withParam(q);
    }

    /**
     * This task has been selected to run as part of the supplied operation, this
     * callback allows actions to perform any custom configuration on the op.
     * @param task
     * @param op
     */
    public void configureOp(Task task, Op op) {
        if(Op.OpFunction.FIND == op.getFunction())
            getParams().forEach(p -> op.withParam(p));
    }

    @Override
    protected List<RuleMatcher> getDefaultIncludeMatchers() {
        return Utils.asList(new RuleMatcher("GET", "{" + Request.COLLECTION_KEY + "}/[{" + Request.RESOURCE_KEY + "}]/[{" + Request.RELATIONSHIP_KEY + "}]"));
    }


    protected static String getForeignKey(Relationship rel, JSMap node) {
        Index idx = rel.getFkIndex1();
        if (idx.size() == 1 && node.get(idx.getJsonName(0)) != null)
            return node.getString(idx.getJsonName(0));

        String key = rel.getCollection().encodeKeyFromJsonNames(node, rel.getFkIndex1());
        return key;
    }


    protected static String getResourceKey(Collection collection, JSMap node) {
        String key = collection.encodeKeyFromJsonNames(node);
        if (key == null)
            throw ApiException.new500InternalServerError("The primary key '{}' could not be constructed from the data provided.", collection.getResourceIndex());
        return key;
    }

    public static String stripTerms(String url, String... tokens) {
        Url u = new Url(url);
        u.clearParams(tokens);
        return u.toString();
    }


    protected static String expandPath(String path, Object next) {
        if (Utils.empty(path))
            return next + "";
        else
            return path + "." + next;
    }

    protected static boolean shouldExpand(Set<String> expands, String path, Relationship rel) {
        boolean expand = false;
        path = path.length() == 0 ? rel.getName() : path + "." + rel.getName();
        path = path.toLowerCase();

        for (String ep : expands) {
            ep = ep.toLowerCase();
            if (ep.startsWith(path) && (ep.length() == path.length() || ep.charAt(path.length()) == '.')) {
                expand = true;
                break;
            }
        }

        //System.out.println("expand(" + expands + ", " + path + ") -> " + expand);

        return expand;
    }



    public void run(Request req, Response res) throws ApiException {
        if (req.getRelationshipKey() != null) {
            //-- all URLs with a subcollection key will be rewritten and
            //-- internally forwarded to the non-subcollection form.

            String       resourceKey = req.getResourceKey();
            Collection   collection  = req.getCollection();
            Relationship rel         = collection.getRelationship(req.getRelationshipKey());

            if (rel == null)
                throw ApiException.new400BadRequest("'{}' is not a valid relationship", req.getRelationshipKey());

            StringBuilder newHref = null;

            if (rel.isOneToMany()) {
                //-- CONVERTS: http://localhost/northwind/sql/orders/10395/orderdetails
                //-- TO THIS : http://localhost/northwind/sql/orderdetails?orderid=10395

                //-- CONVERTS: http://localhost/northwind/sql/collection/val1~val2/subcollection
                //-- TO THIS : http://localhost/northwind/sql/subcollection?col1=val1&col2=val2

                //TODO: need a compound key test case here
                Collection relatedCollection = rel.getRelated();
                newHref = new StringBuilder(Chain.buildLink(relatedCollection) + "?");
                Map<String, Object> resourceKeyRow = collection.decodeKeyToJsonNames(req.getResourceKey());

                if (rel.getFkIndex1().size() != collection.getResourceIndex().size() //
                        && rel.getFkIndex1().size() == 1)//assume the single fk prop is an encoded resourceKey
                {
                    String propName = rel.getFk1Col1().getJsonName();
                    newHref.append(propName).append("=").append(resourceKey);
                } else {
                    //TODO: test this change
                    Index fkIdx = rel.getFkIndex1();
                    Index pkIdx = collection.getResourceIndex();

                    for (int i = 0; i < fkIdx.size(); i++) {
                        Property fk     = fkIdx.getProperty(i);
                        String   pkName = pkIdx.getJsonName(i);
                        Object   pkVal  = resourceKeyRow.get(pkName);

                        if (pkVal == null)
                            throw ApiException.new400BadRequest("Missing parameter for foreign key property '{}'", fk.getJsonName());

                        newHref.append(fk.getJsonName()).append("=").append(pkVal).append("&");
                    }

                    newHref = new StringBuilder(newHref.substring(0, newHref.length() - 1));
                }

            } else if (rel.isManyToMany()) {
                //-- CONVERTS: http://localhost/northwind/source/employees/1/territories
                //-- TO THIS : http://localhost/northwind/source/territories/06897,19713

                List<KeyValue<String, String>> rows = getRelatedKeys(rel, rel.getFkIndex1(), rel.getFkIndex2(), Collections.singletonList(resourceKey));
                if (rows.size() > 0) {
                    List<String> foreignKeys = new ArrayList<>();
                    rows.forEach(k -> foreignKeys.add(k.getValue()));

                    Collection relatedCollection = rel.getRelated();
                    String     resourceKeys      = Utils.implode(",", foreignKeys.toArray());

                    newHref = new StringBuilder(Chain.buildLink(relatedCollection, resourceKeys));
                } else {
                    return;
                }
            }
//            else if(rel.isOneToOneParent() || rel.isOneToOneChild()){
//                //-- TODO: need to optimize for one to one relatinships, there is no need to requery the DB here.
//            }
            else {
                //-- The link was requested like this  : http://localhost/northwind/source/orderdetails/XXXXX/order
                //-- By default, the system would have written out : http://localhost/northwind/source/orders/YYYYY
                //-- We are going to have to re-query to get the FK value from the resource of the passed in link

                String url = req.getUrl().getOriginal();
                //String query = Utils.substringAfter(url, "?");

                url = Utils.substringBefore(url, "?");
                if (url.endsWith("/"))
                    url = url.substring(0, url.length() - 1);

                url = url.substring(0, url.lastIndexOf("/"));

                Response tempRes = res.getEngine().get(url).assertOk();
                JSMap   node    = tempRes.getFirstRecordAsMap();

                String link = Chain.buildLink(node, rel);
                newHref = new StringBuilder(link);

//                String fk = data.findString(rel.getName());
//                if(fk == null)
//                    fk = data.findString("_links." + rel.getName() + ".href");
//
//                if(fk != null && fk.startsWith("http")){
//                    newHref = new StringBuilder(fk);
//                }

                if (newHref == null)
                    throw ApiException.new500InternalServerError("Unable to locate foreign key value for relationship '{}'", rel.getName());


            }

            Map<String, String> params = req.getUrl().getParams();
            Utils.filter(params, Request.COLLECTION_KEY, Request.RESOURCE_KEY, Request.RELATIONSHIP_KEY);
            if (params.size() > 0) {
                String queryString = Url.toQueryString(params);
                if (!newHref.toString().contains("?"))
                    newHref.append("?");
                else
                    newHref.append("&");

                newHref.append(queryString);
            }

            Response included = req.getEngine().get(newHref.toString());
            res.withStatus(included.getStatus());
            res.withJson(included.getJson());
            return;
        } else if (req.getCollection() != null && !Utils.empty(req.getResourceKey())) {
            List<String> resourceKeys = Utils.explode(",", req.getResourceKey());
            Term         term         = Term.term(null, "_key", req.getCollection().getResourceIndex().getName(), resourceKeys.toArray());
            req.getUrl().withParams(term.toString(), null);
        }

        Results results = select(req, req.getCollection(), req.getApi());

        if (results.size() == 0 && req.getResourceKey() != null && req.getCollectionKey() != null) {
            res.withJson((JSNode)null);
            res.withStatus(Status.SC_404_NOT_FOUND);
        } else {
            //-- copy data into the response
            res.withRecords(results.getRows());

            if(res.data().size() > 0) {
                Collection coll = null;
                if (req.getOp().getFunction() == Op.OpFunction.RELATED) {
                    if (req.getRelationship() != null)
                        coll = req.getRelationship().getCollection();
                } else if (req.getOp().getFunction() == Op.OpFunction.FIND) {
                    if (req.getCollection() != null)
                        coll = req.getCollection();
                }
                if(coll != null){
                    String lastKey = coll.encodeKeyFromJsonNames((JSMap)res.data().last());
                    if(lastKey != null)
                        res.withLastKey(lastKey);
                }
            }

            //------------------------------------------------
            //-- setup all of the meta section

            Page page = results.getQuery().getPage();
            res.withPageSize(page.getPageSize());
            res.withPageNum(page.getPageNum());

            int offest = page.getOffset();
            int limit  = page.getLimit();

            int foundRows = results.getFoundRows();

            //if (foundRows < 0 && results.size() > 0 && offest <= 0 && results.size() < limit)
            if (foundRows < 0 && results.size() >= 0 && offest <= 0 && results.size() < limit)
                foundRows = results.size();

            if (foundRows >= 0) {
                res.withFoundRows(foundRows);
            }

            if (results.size() > 0 && results.size() >= limit) {
                if (req.getCollection() != null && req.getResourceKey() == null) {
                    List<Term> nextTerms = results.getNext();
                    if (nextTerms != null && !nextTerms.isEmpty()) {
                        String next = req.getUrl().getOriginal();
                        for (Term nextTerm : nextTerms) {
                            String toStrip = nextTerm.getToken();
                            next = stripTerms(next, toStrip);

                            if (!next.contains("?"))
                                next += "?";
                            if (!next.endsWith("?"))
                                next += "&";

                            next += nextTerm;
                        }
                        res.withNext(next);
                    } else if (results.size() == limit && (foundRows < 0 || (offest + limit) < foundRows)) {
                        String next = req.getUrl().getOriginal();
                        next = stripTerms(next, "offset", "page", "pageNum", "pageNumber", "after");

                        if (!next.contains("?"))
                            next += "?";
                        if (!next.endsWith("?"))
                            next += "&";

                        next += "pageNumber=" + (page.getPageNum() + 1);

                        res.withNext(next);
                    }
                }
            }
        }

    }

    protected Results select(Request req, Collection collection, Api api) throws ApiException {
        Results results;

        if (collection == null) {
            Db db = api.getDb((String) Chain.peek().get("db"));

            if (db == null) {
                List<Db> dbs = api.getDbs();
                for (Db candidate : dbs) {
                    if (candidate.matches(req.getMethod(), req.getPath())) {
                        db = candidate;
                        break;
                    }
                }
            }

            if (db == null)
                throw ApiException.new400BadRequest("Unable to find collection for url '{}'", req.getUrl());

            results = db.select(null, req.getUrl().getParams());
        } else {
            results = collection.getDb().select(collection, req.getUrl().getParams());
        }

        if (results.size() > 0) {
            if (collection != null)
                expand(req, collection, (List<JSMap>) results.getRows(), null, null, null);
        }

        return results;
    }


    /**
     * This is more complicated than it seems like it would need to be because
     * it attempts to retrieve all values of a relationship at a time for the whole
     * document.  It does not run a recursive query for each resource and each relationship
     * which could mean hundreds and hundreds of queries per document.  This should
     * result in number of queries proportional to the number of expands terms that does
     * not increase with the number of results at any level of the expansion.
     *
     * @param request     the request being serviced
     * @param collection  the collection being queried
     * @param parentObjs  the records that were just selected
     * @param expands     the definition of which properties should be expanded
     * @param expandsPath the path we are currently on
     * @param pkCache     a cache of things already looked up
     */
    protected void expand(Request request, Collection collection, List<JSMap> parentObjs, Set expands, String expandsPath, MultiKeyMap pkCache) {
        if (parentObjs.size() == 0)
            return;

        if (expands == null) {
            String expandsStr = request.getUrl().getParam("expand");
            if (expandsStr == null)
                return;

            expands = new LinkedHashSet(Utils.explode(",", expandsStr));
        }

        if (expandsPath == null)
            expandsPath = "";

        for (Relationship rel : collection.getRelationships()) {
            boolean shouldExpand = shouldExpand(expands, expandsPath, rel);

            //System.out.println("should expand " + Chain.getDepth() + " -> " + rel + " -> " + shouldExpand);

            if (shouldExpand) {
                if (pkCache == null) {
                    //------------------------------------------------
                    // pkCache is used to make nested document expansion efficient
                    //
                    // the pkCache is used to map requested entities back to the right
                    // objects on the recursion stack and to keep track of entities
                    // so you don't waste time requerying for things you have
                    // already retrieved.
                    pkCache = new MultiKeyMap();

                    for (JSMap node : parentObjs) {
                        pkCache.put(collection, getResourceKey(collection, node), node);
                    }
                }

                //ONE_TO_MANY - Location.id <- Player.locationId
                //MANY_TO_ONE - Player.locationId -> Location.id (one playe
                //MANY_TO_MANY, ex going from Category(id)->CategoryBooks(categoryId, bookId)->Book(id)

                final Collection relatedCollection = rel.getRelated();
                //            Column toMatchCol = null;
                //            Column toRetrieveCol = null;

                Index          idxToMatch    = null;
                Index          idxToRetrieve = null;
                List<KeyValue> relatedEks    = null;

                if (rel.isManyToOne()) {
                    idxToMatch = collection.getResourceIndex();
                    idxToRetrieve = rel.getFkIndex1();

                    //NOTE: expands() is only getting the paired up related keys.  For a MANY_TO_ONE
                    //relationship that data is already in the parent object you are trying to expand
                    //so we don't need to query the db to find those relationships as we do for the
                    //MANY_TO relationships.
                    //
                    //However if you were to comment out the following block, the output of the algorithm
                    //would be exactly the same you would just end up running an extra db query

                    relatedEks = new ArrayList<>();
                    for (JSMap parentObj : parentObjs) {
                        String parentEk = getResourceKey(collection, parentObj);
                        String childEk  = getForeignKey(rel, parentObj);
                        if (childEk != null) {
                            relatedEks.add(new KeyValue(parentEk, childEk));
                        }
                    }
                } else if (rel.isOneToMany()) {
                    idxToMatch = rel.getFkIndex1();
                    idxToRetrieve = rel.getRelated().getResourceIndex();
                } else if (rel.isManyToMany()) {
                    idxToMatch = rel.getFkIndex1();
                    idxToRetrieve = rel.getFkIndex2();
                } else if(rel.isOneToOneParent()){
                    relatedEks = new ArrayList<>();
                    for (JSMap parentObj : parentObjs) {
                        String parentEk = getResourceKey(collection, parentObj);
                        String childEk  = parentEk; //TODO: this will not work if the columns are not in the same order
                        if (childEk != null) {
                            relatedEks.add(new KeyValue(parentEk, childEk));
                        }
                    }
                }
                else if(rel.isOneToOneChild()){
                    relatedEks = new ArrayList<>();
                    for (JSMap parentObj : parentObjs) {
                        String parentEk = getResourceKey(collection, parentObj);
                        String childEk  = parentEk; //TODO: this will not work if the columns are not in the same order
                        if (childEk != null) {
                            relatedEks.add(new KeyValue(parentEk, childEk));
                        }
                    }
                }


                if (relatedEks == null) {
                    List toMatchEks = new ArrayList<>();
                    for (JSMap parentObj : parentObjs) {
                        String parentEk = getResourceKey(collection, parentObj);
                        if (!toMatchEks.contains(parentEk)) {
                            if (parentObj.get(rel.getName()) instanceof JSList)
                                throw ApiException.new500InternalServerError("This relationship seems to have already been expanded.");//-- this is an implementation logic error. If it ever happens...FIX IT.

                            toMatchEks.add(parentEk);

                            if (rel.isManyToOne()) {
                                parentObj.remove(rel.getName());
                            } else {
                                parentObj.put(rel.getName(), new JSList());
                            }
                        }
                    }
                    relatedEks = getRelatedKeys(rel, idxToMatch, idxToRetrieve, toMatchEks);
                }

                List                          unfetchedChildEks = new ArrayList<>();
                ListMap<String, String> fkCache           = new ListMap<>();

                for (KeyValue<String, String> row : relatedEks) {
                    //the values in the many_to_many link table may have different names than the target columns so you have to
                    //use the index not the name to build the child resource key.

                    String parentEk  = row.getKey();
                    String relatedEk = row.getValue();

                    fkCache.put(relatedEk, parentEk);

                    if (!pkCache.containsKey(relatedCollection, relatedEk)) {
                        unfetchedChildEks.add(relatedEk);
                    }
                }

                //this recursive call populates the pkCache
                List<JSMap> newChildObjs = recursiveGet(pkCache, relatedCollection, unfetchedChildEks, expandPath(expandsPath, rel.getName()));

                for (KeyValue<String, String> row : relatedEks) {
                    String parentEk  = row.getKey();
                    String relatedEk = row.getValue();

                    JSNode parentObj = (JSNode) pkCache.get(collection, parentEk);
                    JSNode childObj  = (JSNode) pkCache.get(relatedCollection, relatedEk);

                    if (rel.isManyToOne() || rel.isOneToOneParent() || rel.isOneToOneChild()) {
                        parentObj.put(rel.getName(), childObj);
                    } else {
                        if (childObj != null) {
                            parentObj.getList(rel.getName()).add(childObj);
                        }
                    }
                }

                if (newChildObjs.size() > 0) {
                    expand(request, relatedCollection, newChildObjs, expands, expandPath(expandsPath, rel.getName()), pkCache);
                }
            }
        }
    }

    protected List<KeyValue<String, String>> getRelatedKeys(Relationship rel, Index idxToMatch, Index idxToRetrieve, List<String> toMatchEks) throws ApiException {
        if (idxToMatch.getCollection() != idxToRetrieve.getCollection())
            throw ApiException.new400BadRequest("You can only retrieve related index keys from the same Collection.");

        List<KeyValue<String, String>> related = new ArrayList<>();

        LinkedHashSet columns = new LinkedHashSet();
        columns.addAll(idxToMatch.getColumnNames());
        columns.addAll(idxToRetrieve.getColumnNames());

        Term termKeys = Term.term(null, "_key", idxToMatch.getName(), toMatchEks);
        Term includes = Term.term(null, "include", columns);
        Term sort     = Term.term(null, "sort", columns);
        Term notNull  = Term.term(null, "nn", columns);

        String   link = Chain.buildLink(idxToRetrieve.getCollection());
        Response res  = Chain.peek().getEngine().get(link, Arrays.asList(termKeys, includes, sort, notNull)).assertOk();

        for (JSNode node : res.data().asMapList()) {
            List idxToMatchVals = new ArrayList<>();

            for (String property : idxToMatch.getJsonNames()) {
                Object propVal = node.get(property);

                if (propVal instanceof String) {
                    propVal = Utils.substringAfter(propVal.toString(), "/");
                    if (((String) propVal).contains("~")) {
                        idxToMatchVals.addAll(Utils.explode("~", (String) propVal));
                        continue;
                    }
                }

                idxToMatchVals.add(propVal);
            }

            List idxToRetrieveVals = new ArrayList<>();
            for (String property : idxToRetrieve.getJsonNames()) {
                Object propVal = node.get(property);

                propVal = Utils.substringAfter(propVal.toString(), "/");
                if (((String) propVal).contains("~")) {
                    idxToRetrieveVals.addAll(Utils.explode("~", (String) propVal));
                    continue;
                }

                idxToRetrieveVals.add(propVal);
            }

            String parentEk  = Collection.encodeKey(idxToMatchVals);
            String relatedEk = Collection.encodeKey(idxToRetrieveVals);

            related.add(new KeyValue<>(parentEk, relatedEk));
        }

        return related;
    }

    protected List<JSMap> recursiveGet(MultiKeyMap pkCache, Collection collection, java.util.Collection resourceKeys, String expandsPath) throws ApiException {
        if (resourceKeys.size() == 0)
            return Collections.EMPTY_LIST;

        String url = Chain.buildLink(collection, Utils.implode(",", resourceKeys));

        //      //--
        //      //-- Nested param support
        //      //TODO: don't remember the use case here.  need to find and make a test case
        //      Map<String, String> params = Chain.top().getRequest().getParams();
        //      String lcPath = expandsPath.toLowerCase();
        //      for (String key : params.keySet())
        //      {
        //         String lcKey = key.toLowerCase();
        //
        //         if (reservedParams.contains(lcKey))
        //            continue;
        //
        //         if (lcKey.matches(".*\\b" + lcPath.replace(".", "\\.") + ".*"))
        //         {
        //            String value = params.get(key);
        //            lcKey = key.replaceAll("\\b" + (lcPath + "\\."), "");
        //
        //            if (url.indexOf("?") < 0)
        //               url += "?";
        //            url += URLEncoder.encode(lcKey, "UTF-8");
        //            if (!Utils.empty(value))
        //               url += "=" + URLEncoder.encode(value, "UTF-8");
        //         }
        //      }

        Response res = Chain.peek().getEngine().get(url);
        int      sc  = res.getStatusCode();
        if (sc == 401 || sc == 403)//unauthorized || forbidden
            return null;

        if (sc == 404) {
            return Collections.EMPTY_LIST;
        } else if (sc == 500) {
            res.rethrow();
        } else if (sc == 200) {
            List<JSMap> nodes = res.data().asMapList();

            for (JSMap node : nodes) {
                Object resourceKey = getResourceKey(collection, node);
                if (pkCache.containsKey(collection, resourceKey)) {
                    throw ApiException.new500InternalServerError("The requested resource has already been retrieved.");//-- logic error...fix me if found.
                }

                pkCache.put(collection, resourceKey, node);
            }
            return nodes;
        }

        res.rethrow();
        return null;
    }

    public int getMaxRows() {
        return maxRows;
    }

    //   protected static boolean include(String path, Set<String> includes, Set<String> excludes)
    //   {
    //      boolean include = true;
    //
    //      if (includes.size() == 0 && excludes.size() == 0)
    //      {
    //         include = true;
    //      }
    //      else
    //      {
    //         path = path.toLowerCase();
    //
    //         if (includes != null && includes.size() > 0)
    //         {
    //            include = false;
    //            include = find(includes, path, true);
    //         }
    //
    //         if (excludes != null && excludes.size() > 0 && find(excludes, path, true))
    //         {
    //            include = false;
    //         }
    //      }
    //
    //      System.out.println("include(" + path + ", " + includes + ", " + excludes + ") -> " + include);
    //
    //      return include;
    //   }

    public DbGetAction withMaxRows(int maxRows) {
        this.maxRows = maxRows;
        return this;
    }

}
