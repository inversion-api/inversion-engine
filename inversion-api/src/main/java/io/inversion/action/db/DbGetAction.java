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
import io.inversion.rql.Page;
import io.inversion.rql.Term;
import io.inversion.utils.JSArray;
import io.inversion.utils.JSNode;
import io.inversion.utils.Rows.Row;
import io.inversion.utils.Url;
import io.inversion.utils.Utils;
import org.apache.commons.collections4.KeyValue;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.keyvalue.DefaultKeyValue;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.util.*;

public class DbGetAction extends Action<DbGetAction> {

    protected int maxRows = 100;

    protected static boolean exclude(String path, Set<String> includes, Set<String> excludes) {
        boolean exclude = false;

        if (includes.size() > 0 || excludes.size() > 0) {
            path = path.toLowerCase();

            if (includes.size() > 0)
                if (!(path.endsWith("href") || path.endsWith(".href")))
                    if (!find(includes, path, true))
                        exclude = true;

            if (excludes != null && excludes.size() > 0)
                if (find(excludes, path, false))
                    exclude = true;
        }
        //System.out.println("exclude(" + path + ", " + includes + ", " + excludes + ") -> " + exclude);

        return exclude;
    }


    protected static String getForeignKey(Relationship rel, JSNode node) {

        String key = rel.getCollection().encodeJsonKey(node, rel.getFkIndex1());

//        if(key == null)
//            throw ApiException.new500InternalServerError("The foreign key '{}' could not be constructed from the data provided.", rel);
//        if(key != null)
//            return key;
//
//        if(key == null)
//            key = node.getString(rel.getName());
//
//        if(key == null)
//            key = node.findString("_links." + rel.getName() + ".href");
//
//        if(key != null && key.indexOf("/") > 0){
//            if(key.endsWith("/"))
//                key = key.substring(0, key.length() -1);
//
//            key = Utils.substringAfter(key, "/");
//        }

        return key;
    }


    protected static String getResourceKey(Collection collection, JSNode node) {
        String key = collection.encodeJsonKey(node);
        if(key == null)
            throw ApiException.new500InternalServerError("The primary key '{}' could not be constructed from the data provided.", collection.getPrimaryIndex());

        return key;
    }

    public static String stripTerms(String url, String... tokens) {
        Url u = new Url(url);
        u.clearParams(tokens);
        return u.toString();
    }

    protected static String stripTerm(String str, String startToken, char... endingTokens) {
        Set<Character> tokens = new HashSet<>();
        for (char c : endingTokens)
            tokens.add(c);

        while (true) {
            int start = str.toLowerCase().indexOf(startToken);

            //this makes sure the char before the start token is not a letter or number which would mean we are in the
            //middle of another token, not at the start of a token.
            while (start > 0 && (Character.isAlphabetic(str.charAt(start - 1)) || Character.isDigit(start - 1)))
                start = str.toLowerCase().indexOf(startToken, start + 1);

            if (start > -1) {
                String beginning = str.substring(0, start);

                int end = start + startToken.length() + 1;
                while (end < str.length()) {
                    char c = str.charAt(end);
                    if (tokens.contains(c))
                        break;
                    end += 1;
                }

                if (end == str.length())
                    str = beginning;
                else
                    str = beginning + str.substring(end + 1);
            } else {
                break;
            }
        }

        return str;
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
            if (ep.startsWith(path) && (ep.length() == path.length() || ep.charAt(path.length()) == '.')) {
                expand = true;
                break;
            }
        }

        //System.out.println("expand(" + expands + ", " + path + ") -> " + expand);

        return expand;
    }

    protected static boolean find(java.util.Collection<String> params, String path, boolean matchStart) {
        boolean rtval = false;

        if (params.contains(path)) {
            rtval = true;
        } else {
            for (String param : params) {
                if (matchStart) {
                    if (param.startsWith(path + ".")) {
                        rtval = true;
                        break;
                    }
                }

                if (Utils.wildcardMatch(param, path))
                    rtval = true;
            }
        }

        //System.out.println("find(" + params + ", " + path + ", " + matchStart + ") -> " + path);

        return rtval;

    }

    @Override
    public void run(Request req, Response res) throws ApiException {
        if (req.getRelationshipKey() != null) {
            //-- all URLs with a subcollection key will be rewritten and
            //-- internally forwarded to the non-subcollection form.

            String       resourceKey = req.getResourceKey();
            Collection   collection  = req.getCollection();
            Relationship rel         = collection.getRelationship(req.getRelationshipKey());

            if (rel == null)
                throw ApiException.new404NotFound("'{}' is not a valid relationship", req.getRelationshipKey());

            StringBuilder newHref = null;

            if (rel.isOneToMany()) {
                //-- CONVERTS: http://localhost/northwind/sql/orders/10395/orderdetails
                //-- TO THIS : http://localhost/northwind/sql/orderdetails?orderid=10395

                //-- CONVERTS: http://localhost/northwind/sql/collection/val1~val2/subcollection
                //-- TO THIS : http://localhost/northwind/sql/subcollection?col1=val1&col2=val2

                //TODO: need a compound key test case here
                Collection relatedCollection = rel.getRelated();
                newHref = new StringBuilder(Chain.buildLink(relatedCollection, null, null) + "?");
                Row resourceKeyRow = collection.decodeJsonKey(req.getResourceKey());

                if (rel.getFkIndex1().size() != collection.getPrimaryIndex().size() //
                        && rel.getFkIndex1().size() == 1)//assume the single fk prop is an encoded resourceKey
                {
                    String propName = rel.getFk1Col1().getJsonName();
                    newHref.append(propName).append("=").append(resourceKey);
                } else {
                    //TODO: test this change
                    Index fkIdx = rel.getFkIndex1();
                    Index pkIdx = collection.getPrimaryIndex();

                    for (int i = 0; i < fkIdx.size(); i++) {
                        Property fk     = fkIdx.getProperty(i);
                        String pkName = pkIdx.getPropertyName(i);
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

                    newHref = new StringBuilder(Chain.buildLink(relatedCollection, resourceKeys, null));
                } else {
                    return;
                }
            } else {
                //-- The link was requested like this  : http://localhost/northwind/source/orderdetails/XXXXX/order
                //-- The system would have written out : http://localhost/northwind/source/orders/YYYYY
                //throw new UnsupportedOperationException("FIX ME IF FOUND...implementation logic error.");

                String url = req.getUrl().getOriginal();
                //String query = Utils.substringAfter(url, "?");

                url = Utils.substringBefore(url, "?");
                if(url.endsWith("/"))
                    url = url.substring(0, url.length()-1);

                url = url.substring(0, url.lastIndexOf("/"));

                Response tempRes = res.getEngine().get(url).assertOk();
                JSNode data = tempRes.getData();
                if(data instanceof JSArray)
                    data = (JSNode)((JSArray)data).get(0);

                String link = Chain.buildLink(data, rel);
                newHref = new StringBuilder(link);

//                String fk = data.findString(rel.getName());
//                if(fk == null)
//                    fk = data.findString("_links." + rel.getName() + ".href");
//
//                if(fk != null && fk.startsWith("http")){
//                    newHref = new StringBuilder(fk);
//                }

                if(newHref == null)
                    throw ApiException.new500InternalServerError("Unable to locate foreign key value for relationship '{}'", rel.getName());


            }

            String query = req.getUrl().getQueryString();

            if (query != null) {
                if (!newHref.toString().contains("?"))
                    newHref.append("?");
                else
                    newHref.append("&");

                newHref.append(query);
            }

            Response included = req.getEngine().get(newHref.toString());
            res.withStatus(included.getStatus());
            res.withJson(included.getJson());
            return;
        } else if (req.getCollection() != null && !Utils.empty(req.getResourceKey())) {
            List<String> resourceKeys = Utils.explode(",", req.getResourceKey());
            Term         term         = Term.term(null, "_key", req.getCollection().getPrimaryIndex().getName(), resourceKeys.toArray());
            req.getUrl().withParams(term.toString(), null);
        }

        Results results = select(req, req.getCollection(), req.getApi());

        if (results.size() == 0 && req.getResourceKey() != null && req.getCollectionKey() != null) {
            res.withStatus(Status.SC_404_NOT_FOUND);
        } else {
            //-- copy data into the response
            res.withRecords(results.getRows());

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

            if (results.size() > 0) {
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

                        next = stripTerms(next, "offset", "page", "pageNum");

                        if (!next.contains("?"))
                            next += "?";
                        if (!next.endsWith("?"))
                            next += "&";

                        next += "pageNum=" + (page.getPageNum() + 1);

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
                if (dbs.size() == 1) {
                    db = dbs.get(0);
                } else {
                    for (Db candidate : dbs) {
                        if (candidate.getEndpointPath() != null && candidate.getEndpointPath().matches(req.getEndpointPath())) {
                            db = candidate;
                            break;
                        }
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
                expand(req, collection, (List<JSNode>) results.getRows(), null, null, null);

            exclude(results.getRows());
        }

        return results;
    }

    protected void exclude(List<JSNode> nodes) {
        Set<String> includes = Chain.peek().mergeEndpointActionParamsConfig("includes");
        Set<String> excludes = Chain.peek().mergeEndpointActionParamsConfig("excludes");

        if (includes.size() > 0 || excludes.size() > 0) {
            for (JSNode node : nodes) {
                exclude(node, includes, excludes, null);
            }
        }
    }

    //-------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------
    //-Static Utils -----------------------------------------------------------------------
    //-------------------------------------------------------------------------------------

    protected void exclude(JSNode node, Set<String> includes, Set<String> excludes, String path) {
        for (String key : node.keySet()) {
            String attrPath = path != null ? (path + "." + key) : key;
            if (exclude(attrPath, includes, excludes)) {
                node.remove(key);
            } else {
                Object value = node.get(key);

                if (value instanceof JSArray) {
                    JSArray arr = (JSArray) value;
                    for (int i = 0; i < arr.size(); i++) {
                        if (arr.get(i) instanceof JSNode) {
                            exclude((JSNode) arr.get(i), includes, excludes, attrPath);
                        }
                    }
                } else if (value instanceof JSNode) {
                    exclude((JSNode) value, includes, excludes, attrPath);
                }
            }
        }
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
    protected void expand(Request request, Collection collection, List<JSNode> parentObjs, Set expands, String expandsPath, MultiKeyMap pkCache) {
        if (parentObjs.size() == 0)
            return;

        if (expands == null)
            expands = Chain.peek().mergeEndpointActionParamsConfig("expands");

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

                    for (JSNode node : parentObjs) {
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
                    idxToMatch = collection.getPrimaryIndex();
                    idxToRetrieve = rel.getFkIndex1();

                    //NOTE: expands() is only getting the paired up related keys.  For a MANY_TO_ONE
                    //relationship that data is already in the parent object you are trying to expand
                    //so we don't need to query the db to find those relationships as we do for the
                    //MANY_TO relationships.
                    //
                    //However if you were to comment out the following block, the output of the algorithm
                    //would be exactly the same you would just end up running an extra db query

                    relatedEks = new ArrayList<>();
                    for (JSNode parentObj : parentObjs) {
                        String parentEk = getResourceKey(collection, parentObj);
                        String childEk  = getForeignKey(rel, parentObj);
                        if (childEk != null) {
                            relatedEks.add(new DefaultKeyValue(parentEk, childEk));
                        }
                    }
                } else if (rel.isOneToMany()) {
                    idxToMatch = rel.getFkIndex1();
                    idxToRetrieve = rel.getRelated().getPrimaryIndex();
                } else if (rel.isManyToMany()) {
                    idxToMatch = rel.getFkIndex1();
                    idxToRetrieve = rel.getFkIndex2();
                }

                if (relatedEks == null) {
                    List toMatchEks = new ArrayList<>();
                    for (JSNode parentObj : parentObjs) {
                        String parentEk = getResourceKey(collection, parentObj);
                        if (!toMatchEks.contains(parentEk)) {
                            if (parentObj.get(rel.getName()) instanceof JSArray)
                                throw ApiException.new500InternalServerError("This relationship seems to have already been expanded.");//-- this is an implementation logic error. If it ever happens...FIX IT.

                            toMatchEks.add(parentEk);

                            if (rel.isManyToOne()) {
                                parentObj.remove(rel.getName());
                            } else {
                                parentObj.put(rel.getName(), new JSArray());
                            }
                        }
                    }
                    relatedEks = getRelatedKeys(rel, idxToMatch, idxToRetrieve, toMatchEks);
                }

                List                          unfetchedChildEks = new ArrayList<>();
                ListValuedMap<String, String> fkCache           = new ArrayListValuedHashMap<>();

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
                List<JSNode> newChildObjs = recursiveGet(pkCache, relatedCollection, unfetchedChildEks, expandPath(expandsPath, rel.getName()));

                for (KeyValue<String, String> row : relatedEks) {
                    String parentEk  = row.getKey();
                    String relatedEk = row.getValue();

                    JSNode parentObj = (JSNode) pkCache.get(collection, parentEk);
                    JSNode childObj  = (JSNode) pkCache.get(relatedCollection, relatedEk);

                    if (rel.isManyToOne()) {
                        parentObj.put(rel.getName(), childObj);
                    } else {
                        if (childObj != null) {
                            parentObj.getArray(rel.getName()).add(childObj);
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
        Term includes = Term.term(null, "includes", columns);
        Term sort     = Term.term(null, "sort", columns);
        Term notNull  = Term.term(null, "nn", columns);

        String   link = Chain.buildLink(idxToRetrieve.getCollection());
        Response res  = Chain.peek().getEngine().get(link, Arrays.asList(termKeys, includes, sort, notNull)).assertOk();

        for (JSNode node : res.data().asNodeList()) {
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

            related.add(new DefaultKeyValue<>(parentEk, relatedEk));
        }

        return related;
    }

    protected List<JSNode> recursiveGet(MultiKeyMap pkCache, Collection collection, java.util.Collection resourceKeys, String expandsPath) throws ApiException {
        if (resourceKeys.size() == 0)
            return Collections.EMPTY_LIST;

        String url = Chain.buildLink(collection, Utils.implode(",", resourceKeys), null);

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
            List<JSNode> nodes = (List<JSNode>) res.getData().asList();

            for (JSNode node : nodes) {
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
