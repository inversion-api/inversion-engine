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
import io.inversion.cloud.rql.Page;
import io.inversion.cloud.rql.RqlParser;
import io.inversion.cloud.rql.Term;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.utils.Utils;
import org.apache.commons.collections4.KeyValue;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.keyvalue.DefaultKeyValue;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.util.*;

public class RestGetAction extends Action<RestGetAction>
{

   protected int maxRows        = 100;

   /**
    * These params are specifically NOT passed to the Query for parsing.  These are either dirty worlds like sql injection tokens or the are used by actions themselves 
    */
   protected Set reservedParams = new HashSet(Arrays.asList("select", "insert", "update", "delete", "drop", "union", "truncate", "exec", "explain", /*"includes",*/ "excludes", "expands"));

   public RestGetAction()
   {
      withMethods("GET");
   }

   @Override
   public void run(Request req, Response res) throws Exception
   {
      if (req.getRelationshipKey() != null)
      {
         //-- all URLs with a subcollection key will be rewritten and  
         //-- internally forwarded to the non-subcollection form.

         String entityKey = req.getEntityKey();
         Collection collection = req.getCollection();
         Relationship rel = collection.getRelationship(req.getRelationshipKey());

         if (rel == null)
            ApiException.throw404NotFound("'{}' is not a valid relationship", req.getRelationshipKey());

         String newHref = null;

         if (rel.isOneToMany())
         {
            //-- CONVERTS: http://localhost/northwind/sql/orders/10395/orderdetails
            //-- TO THIS : http://localhost/northwind/sql/orderdetails?orderid=10395

            //-- CONVERTS: http://localhost/northwind/sql/collection/val1~val2/subcollection
            //-- TO THIS : http://localhost/northwind/sql/subcollection?col1=val1&col2=val2

            //TODO: need a compound key test case here
            Collection relatedCollection = rel.getRelated();
            newHref = Chain.buildLink(relatedCollection, null, null) + "?";
            Row entityKeyRow = collection.decodeKey(req.getEntityKey());

            if (rel.getFkIndex1().size() != collection.getPrimaryIndex().size() //
                  && rel.getFkIndex1().size() == 1)//assume the single fk prop is an encoded entityKey
            {
               String propName = rel.getFk1Col1().getJsonName();
               newHref += propName + "=" + entityKey;
            }
            else
            {
               //TODO: test this change
               Index fkIdx = rel.getFkIndex1();
               Index pkIdx = collection.getPrimaryIndex();

               for (int i = 0; i < fkIdx.size(); i++)
               {
                  Property fk = fkIdx.getProperty(i);
                  String pkName = pkIdx.getPropertyName(i);
                  Object pkVal = entityKeyRow.get(pkName);

                  if (pkVal == null)
                     ApiException.throw400BadRequest("Missing parameter for foreign key property '{}'", fk.getJsonName());

                  newHref += fk.getJsonName() + "=" + pkVal + "&";
               }

               newHref = newHref.substring(0, newHref.length() - 1);
            }

         }
         else if (rel.isManyToMany())
         {
            //-- CONVERTS: http://localhost/northwind/source/employees/1/territories
            //-- TO THIS : http://localhost/northwind/source/territories/06897,19713

            List<KeyValue> rows = getRelatedKeys(rel, rel.getFkIndex1(), rel.getFkIndex2(), Arrays.asList(entityKey));
            if (rows.size() > 0)
            {
               List foreignKeys = new ArrayList();
               rows.forEach(k -> foreignKeys.add(k.getValue()));

               Collection relatedCollection = rel.getRelated();
               String entityKeys = Utils.implode(",", foreignKeys.toArray());

               newHref = Chain.buildLink(relatedCollection, entityKeys, null);
            }
            else
            {
               return;
            }
         }
         else
         {
            //-- The link was requested like this  : http://localhost/northwind/source/orderdetails/XXXXX/order
            //-- The system would have written out : http://localhost/northwind/source/orders/YYYYY
            throw new UnsupportedOperationException("FIX ME IF FOUND...implementation logic error.");
         }

         String query = req.getUrl().getQuery();

         if (query != null)
         {
            if (newHref.indexOf("?") < 0)
               newHref += "?";
            else
               newHref += "&";

            newHref += query;
         }

         Response included = req.getEngine().get(newHref);
         res.withStatus(included.getStatus());
         res.withJson(included.getJson());
         return;
      }
      else if (!Utils.empty(req.getCollection()) && !Utils.empty(req.getEntityKey()))
      {
         List<String> entityKeys = Utils.explode(",", req.getEntityKey());
         Term term = Term.term(null, "_key", req.getCollection().getPrimaryIndex().getName(), entityKeys.toArray());
         req.getUrl().withParams(term.toString(), null);
      }

      Results<JSNode> results = select(req, req.getCollection(), req.getUrl().getParams(), req.getApi());

      if (results.size() == 0 && req.getEntityKey() != null && req.getCollectionKey() != null)
      {
         res.withStatus(Status.SC_404_NOT_FOUND);
      }
      else
      {
         //-- copy data into the response
         res.withRecords(results.getRows());

         //------------------------------------------------
         //-- setup all of the meta section

         Page page = results.getQuery().getPage();
         res.withPageSize(page.getPageSize());
         res.withPageNum(page.getPageNum());

         int offest = page.getOffset();
         int limit = page.getLimit();

         int foundRows = results.getFoundRows();

         if (foundRows < 0 && results.size() > 0 && offest <= 0 && results.size() < limit)
            foundRows = results.size();

         if (foundRows >= 0)
         {
            res.withFoundRows(foundRows);
         }

         if (results.size() > 0)
         {
            if (req.getCollection() != null && req.getEntityKey() == null)
            {
               List<Term> nextTerms = results.getNext();
               if (nextTerms != null && !nextTerms.isEmpty())
               {
                  String next = req.getUrl().getOriginal();
                  for (Term nextTerm : nextTerms)
                  {
                     String toStrip = nextTerm.getToken();
                     next = stripTerms(next, toStrip);

                     if (next.indexOf("?") < 0)
                        next += "?";
                     if (!next.endsWith("?"))
                        next += "&";

                     next += nextTerm;
                  }
                  res.withNext(next);
               }
               else if (results.size() == limit && (foundRows < 0 || (offest + limit) < foundRows))
               {
                  String next = req.getUrl().getOriginal();

                  next = stripTerms(next, "offset", "page", "pageNum");

                  if (next.indexOf("?") < 0)
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

   protected Results<JSNode> select(Request req, Collection collection, Map<String, String> params, Api api) throws Exception
   {
      //------------------------------------------------
      // Normalize all of the params and convert attribute
      // names to column names.
      List<Term> terms = new ArrayList();

      if (params.size() > 0)
      {
         RqlParser parser = new RqlParser();
         for (String paramName : params.keySet())
         {
            String termStr = null;
            String paramValue = params.get(paramName);

            if (Utils.empty(paramValue) && paramName.indexOf("(") > -1)
            {
               termStr = paramName;
            }
            else
            {
               termStr = "eq(" + paramName + "," + paramValue + ")";
            }
            Term term = parser.parse(termStr);

            if (term.hasToken("eq") && reservedParams.contains(term.getToken(0)))
               continue;

            if (term.hasToken("eq") && term.getTerm(0).hasToken("includes"))
            {
               //THIS IS AN OPTIMIZATION...the rest action can pull stuff OUT of the results based on
               //dotted path expressions.  If you don't use dotted path expressions the includes values
               //can be used to limit the sql select clause...however if any of the columns are actually
               //dotted paths, don't pass on to the Query the extra stuff will be removed by the rest action.
               boolean dottedInclude = false;
               for (int i = 1; i < term.size(); i++)
               {
                  String str = term.getToken(i);
                  if (str.indexOf(".") > -1)
                  {
                     dottedInclude = true;
                     break;
                  }
               }
               if (dottedInclude)
                  continue;

               //TODO: need test cases 
               for (Term child : term.getTerms())
               {
                  if (child.hasToken("href") && collection != null)
                  {
                     term.removeTerm(child);

                     Index pk = collection.getPrimaryIndex();
                     for (int i = 0; i < pk.size(); i++)
                     {
                        Property c = pk.getProperty(i);
                        boolean includesPkCol = false;
                        for (Term col : term.getTerms())
                        {
                           if (col.hasToken(c.getColumnName()))
                           {
                              includesPkCol = true;
                              break;
                           }
                        }
                        if (!includesPkCol)
                           term.withTerm(Term.term(term, c.getColumnName()));
                     }
                     break;
                  }
               }
            }

            if (collection != null)
            {
               terms.addAll(collection.getDb().mapToColumns(collection, term));
            }
            else
            {
               terms.add(term);
            }
         }
      }

      //-- this sort is not strictly necessary but it makes the order of terms in generated
      //-- query text dependable so you can write better tests.
      Collections.sort(terms);

      Results results = null;

      if (collection == null)
      {
         Db db = api.getDb((String) Chain.peek().get("db"));

         if (db == null)
            ApiException.throw400BadRequest("Unable to find collection for url '{}'", req.getUrl());

         results = db.select(null, terms);
      }
      else
      {
         results = collection.getDb().select(collection, terms);
      }

      if (results.size() > 0)
      {

         for (int i = 0; i < results.size(); i++)
         {
            //convert the map into a JSMap
            Map<String, Object> row = results.getRow(i);

            if (collection == null)
            {
               JSNode node = new JSNode(row);
               results.setRow(i, node);
            }
            else
            {
               JSNode node = new JSNode();
               results.setRow(i, node);

               String entityKey = req.getCollection().encodeKey(row);

               if (!Utils.empty(entityKey))
               {
                  //------------------------------------------------
                  //next turn all relationships into links that will 
                  //retrieve the related entities
                  for (Relationship rel : collection.getRelationships())
                  {
                     String link = null;
                     if (rel.isManyToOne())
                     {
                        String fkval = null;
                        if (rel.getRelated().getPrimaryIndex().size() != rel.getFkIndex1().size() && rel.getFkIndex1().size() == 1)//this value is already an encoded entityKey
                        {
                           Object obj = row.get(rel.getFk1Col1().getColumnName());
                           if (obj != null)
                              fkval = obj.toString();
                        }
                        else
                        {
                           fkval = Collection.encodeKey(row, rel.getFkIndex1());
                        }

                        if (fkval != null)
                        {
                           link = Chain.buildLink(rel.getRelated(), fkval.toString(), null);
                        }
                     }
                     else
                     {
                        link = Chain.buildLink(req.getCollection(), entityKey, rel.getName());
                     }
                     node.put(rel.getName(), link);
                  }

                  //------------------------------------------------
                  // finally make sure the entity key is encoded as
                  // the href
                  String href = node.getString("href");
                  if (Utils.empty(href))
                  {
                     href = Chain.buildLink(collection, entityKey, null);
                     node.putFirst("href", href);
                  }
               }

               //------------------------------------------------
               //copy over defined attributes first, if the select returned 
               //extra columns they will be copied over last
               for (Property attr : collection.getProperties())
               {
                  String attrName = attr.getJsonName();
                  String colName = attr.getColumnName();

                  boolean rowHas = row.containsKey(colName);
                  if (entityKey != null || rowHas)
                  {
                     //-- if the entityKey was null don't create
                     //-- empty props for fields that were not 
                     //-- returned from the db
                     Object val = row.remove(colName);
                     if (!node.containsKey(attrName))
                        node.put(attrName, val);
                  }
               }

               //------------------------------------------------
               // next, if the db returned extra columns that 
               // are not mapped to attributes, just straight copy them
               for (String key : row.keySet())
               {
                  if (!key.equalsIgnoreCase("href") && !node.containsKey(key))
                  {
                     Object value = row.get(key);
                     node.put(key, value);
                  }
               }

            }

         }
         if (collection != null)
            expand(req, collection, results.getRows(), null, null, null);
         exclude(results.getRows());

      } // end if results.size() > 0

      //------------------------------------------------
      //the "next" params come from the db encoded with db col names
      //have to convert them to their attribute equivalents
      for (Term term : ((List<Term>) results.getNext()))
      {
         mapToAttributes(collection, term);
      }

      return results;
   }

   protected void exclude(List<JSNode> nodes)
   {
      Set includes = Chain.peek().mergeEndpointActionParamsConfig("includes");
      Set excludes = Chain.peek().mergeEndpointActionParamsConfig("excludes");

      if (includes.size() > 0 || excludes.size() > 0)
      {
         for (JSNode node : nodes)
         {
            exclude(node, includes, excludes, null);
         }
      }
   }

   protected void exclude(JSNode node, Set includes, Set excludes, String path)
   {
      for (String key : node.keySet())
      {
         String attrPath = path != null ? (path + "." + key) : key;
         if (exclude(attrPath, includes, excludes))
         {
            node.remove(key);
         }
         else
         {
            Object value = node.get(key);

            if (value instanceof JSArray)
            {
               JSArray arr = (JSArray) value;
               for (int i = 0; i < arr.size(); i++)
               {
                  if (arr.get(i) instanceof JSNode)
                  {
                     exclude((JSNode) arr.get(i), includes, excludes, attrPath);
                  }
               }
            }
            else if (value instanceof JSNode)
            {
               exclude((JSNode) value, includes, excludes, attrPath);
            }
         }
      }
   }

   protected static boolean exclude(String path, Set<String> includes, Set<String> excludes)
   {
      boolean exclude = false;

      if (includes.size() > 0 || excludes.size() > 0)
      {
         path = path.toLowerCase();

         if (includes != null && includes.size() > 0)
         {
            if (path.endsWith("href") || path.endsWith(".href"))
               exclude = false;

            else if (!find(includes, path, true))
               exclude = true;
         }

         if (excludes != null && excludes.size() > 0)
         {
            if (find(excludes, path, false))
               exclude = true;
         }
      }

      //System.out.println("exclude(" + path + ", " + includes + ", " + excludes + ") -> " + exclude);

      return exclude;
   }

   /**
    * This is more complicated than it seems like it would need to be because 
    * it attempts to retrieve all values of a relationship at a time for the whole 
    * document.  It does not run a recursive query for each entity and each relationship
    * which could mean hundreds and hundreds of queries per document.  This should
    * result in number of queries proportional to the number of expands terms that does
    * not increase with the number of results at any level of the expansion.
    */

   protected void expand(Request request, Collection collection, List<JSNode> parentObjs, Set expands, String expandsPath, MultiKeyMap pkCache) throws Exception
   {
      if (parentObjs.size() == 0)
         return;

      if (expands == null)
         expands = Chain.peek().mergeEndpointActionParamsConfig("expands");

      if (expandsPath == null)
         expandsPath = "";

      for (Relationship rel : collection.getRelationships())
      {
         boolean shouldExpand = shouldExpand(expands, expandsPath, rel);

         //System.out.println("should expand " + Chain.getDepth() + " -> " + rel + " -> " + shouldExpand);

         if (shouldExpand)
         {
            if (pkCache == null)
            {
               //------------------------------------------------
               // pkCache is used to make nested document expansion efficient
               //
               // the pkCache is used to map requested entities back to the right 
               // objects on the recursion stack and to keep track of entities
               // so you don't waste time requerying for things you have 
               // already retrieved.
               pkCache = new MultiKeyMap()
                  {
                     //               public Object put(Object key1, Object key2, Object value)
                     //               {
                     //                  System.out.println("PUTPUTPUTPUTPUTPUTPUTPUT:  " + key1 + ", " + key2);
                     //                  return super.put(key1, key2, value);
                     //               }
                     //
                     //               public Object get(Object key1, Object key2)
                     //               {
                     //                  Object value =  super.get(key1, key2);
                     //                  String str = (value + "").replace("\r", "").replace("\n", "");
                     //                  System.out.println("GETGETGETGETGETGETGETGET: " + key1 + ", " + key2 + " -> " + value);
                     //                  return value;
                     //               }
                  };

               for (JSNode node : parentObjs)
               {
                  pkCache.put(collection, getEntityKey(node), node);
               }
            }

            //ONE_TO_MANY - Location.id <- Player.locationId  
            //MANY_TO_ONE - Player.locationId -> Location.id (one playe
            //MANY_TO_MANY, ex going from Category(id)->CategoryBooks(categoryId, bookId)->Book(id)

            final Collection relatedCollection = rel.getRelated();
            //            Column toMatchCol = null;
            //            Column toRetrieveCol = null;

            Index idxToMatch = null;
            Index idxToRetrieve = null;
            List<KeyValue> relatedEks = null;

            if (rel.isManyToOne())
            {
               idxToMatch = collection.getPrimaryIndex();
               idxToRetrieve = rel.getFkIndex1();

               //NOTE: expands() is only getting the paired up related keys.  For a MANY_TO_ONE
               //relationship that data is already in the parent object you are trying to expand
               //so we don't need to query the db to find those relationships as we do for the 
               //MANY_TO relationships.
               //
               //However if you were to comment out the following block, the output of the algorithm
               //would be exactly the same you would just end up running an extra db query

               List cols = new ArrayList();
               //idxToMatch.getColumns().forEach(c -> cols.add(c.getName()));
               //idxToRetrieve.getColumns().forEach(c -> cols.add(c.getName()));

               cols.addAll(idxToMatch.getJsonNames());
               cols.addAll(idxToRetrieve.getJsonNames());

               relatedEks = new ArrayList();
               for (JSNode parentObj : parentObjs)
               {
                  String parentEk = getEntityKey(parentObj);
                  String childEk = parentObj.getString(rel.getName());
                  if (childEk != null)
                  {
                     childEk = getEntityKey(childEk);
                     relatedEks.add(new DefaultKeyValue(parentEk, childEk));
                  }
               }
            }
            else if (rel.isOneToMany())
            {
               //               idxToMatch = rel.getFkIndex1();
               //               idxToRetrieve = rel.getRelated().getTable().getPrimaryIndex();//Entity().getKey().getColumn();

               idxToMatch = rel.getFkIndex1();
               idxToRetrieve = rel.getRelated().getPrimaryIndex();

            }
            else if (rel.isManyToMany())
            {
               idxToMatch = rel.getFkIndex1();
               idxToRetrieve = rel.getFkIndex2();
            }

            if (relatedEks == null)
            {
               List toMatchEks = new ArrayList();
               for (JSNode parentObj : parentObjs)
               {
                  String parentEk = getEntityKey(parentObj);
                  if (!toMatchEks.contains(parentEk))
                  {
                     if (parentObj.get(rel.getName()) instanceof JSArray)
                        ApiException.throw500InternalServerError("Algorithm implementation error...this relationship seems to have already been expanded.");

                     toMatchEks.add(parentEk);

                     if (rel.isManyToOne())
                     {
                        parentObj.remove(rel.getName());
                     }
                     else
                     {
                        parentObj.put(rel.getName(), new JSArray());
                     }
                  }
               }
               relatedEks = getRelatedKeys(rel, idxToMatch, idxToRetrieve, toMatchEks);
            }

            List unfetchedChildEks = new ArrayList();
            ListValuedMap<String, String> fkCache = new ArrayListValuedHashMap<>();

            for (KeyValue<String, String> row : relatedEks)
            {
               //the values in the many_to_many link table may have different names than the target columns so you have to 
               //use the index not the name to build the child entity key.

               String parentEk = row.getKey();
               String relatedEk = row.getValue();

               fkCache.put(relatedEk, parentEk);

               if (!pkCache.containsKey(relatedCollection, relatedEk))
               {
                  unfetchedChildEks.add(relatedEk);
               }
            }

            //this recursive call populates the pkCache
            List<JSNode> newChildObjs = recursiveGet(pkCache, relatedCollection, unfetchedChildEks, expandPath(expandsPath, rel.getName()));

            for (KeyValue<String, String> row : relatedEks)
            {
               String parentEk = row.getKey();
               String relatedEk = row.getValue();

               JSNode parentObj = (JSNode) pkCache.get(collection, parentEk);
               JSNode childObj = (JSNode) pkCache.get(relatedCollection, relatedEk);

               if (rel.isManyToOne())
               {
                  parentObj.put(rel.getName(), childObj);
               }
               else
               {
                  if (childObj != null)
                  {
                     parentObj.getArray(rel.getName()).add(childObj);
                  }
               }
            }

            if (newChildObjs.size() > 0)
            {
               expand(request, relatedCollection, newChildObjs, expands, expandPath(expandsPath, rel.getName()), pkCache);
            }
         }
      }
   }

   protected List<KeyValue> getRelatedKeys(Relationship rel, Index idxToMatch, Index idxToRetrieve, List<String> toMatchEks) throws Exception
   {
      if (idxToMatch.getCollection() != idxToRetrieve.getCollection())
         ApiException.throw400BadRequest("You can only retrieve related index keys from the same Collection.");

      List<KeyValue> related = new ArrayList<>();

      LinkedHashSet columns = new LinkedHashSet();
      columns.addAll(idxToMatch.getColumnNames());
      columns.addAll(idxToRetrieve.getColumnNames());

      Term termKeys = Term.term(null, "_key", idxToMatch.getName(), toMatchEks);
      Term includes = Term.term(null, "includes", columns);
      Term sort = Term.term(null, "sort", columns);
      Term notNull = Term.term(null, "nn", columns);

      String link = Chain.buildLink(idxToRetrieve.getCollection());
      Response res = Chain.peek().getEngine().get(link, Arrays.asList(termKeys, includes, sort, notNull)).assertOk();

      for (JSNode node : res.data().asNodeList())
      {
         List idxToMatchVals = new ArrayList();

         for (String property : idxToMatch.getJsonNames())
         {
            Object propVal = node.get(property);

            if (propVal instanceof String)
            {
               propVal = Utils.substringAfter(propVal.toString(), "/");
               if (((String) propVal).indexOf("~") > -1)
               {
                  idxToMatchVals.addAll(Utils.explode("~", (String) propVal));
                  continue;
               }
            }

            idxToMatchVals.add(propVal);
         }

         List idxToRetrieveVals = new ArrayList();
         for (String property : idxToRetrieve.getJsonNames())
         {
            Object propVal = node.get(property);

            propVal = Utils.substringAfter(propVal.toString(), "/");
            if (((String) propVal).indexOf("~") > -1)
            {
               idxToRetrieveVals.addAll(Utils.explode("~", (String) propVal));
               continue;
            }

            idxToRetrieveVals.add(propVal);
         }

         String parentEk = Collection.encodeKey(idxToMatchVals);
         String relatedEk = Collection.encodeKey(idxToRetrieveVals);

         related.add(new DefaultKeyValue(parentEk, relatedEk));
      }

      //      Results obj = idxToMatch.getProperty(0).getCollection().getDb().select(idxToRetrieve.getCollection(), );
      //      List<Map> rows = obj.getRows();
      //      for (Map row : rows)
      //      {
      //         List idxToMatchVals = new ArrayList();
      //         idxToMatch.getColumnNames().forEach(column -> idxToMatchVals.add(row.get(column)));
      //
      //         List idxToRetrieveVals = new ArrayList();
      //         idxToRetrieve.getColumnNames().forEach(column -> idxToRetrieveVals.add(row.get(column)));
      //
      //         String parentEk = Collection.encodeKey(idxToMatchVals);
      //         String relatedEk = Collection.encodeKey(idxToRetrieveVals);
      //
      //         related.add(new DefaultKeyValue(parentEk, relatedEk));
      //      }

      return related;
   }

   protected List<JSNode> recursiveGet(MultiKeyMap pkCache, Collection collection, java.util.Collection entityKeys, String expandsPath) throws Exception
   {
      if (entityKeys.size() == 0)
         return Collections.EMPTY_LIST;

      String url = Chain.buildLink(collection, Utils.implode(",", entityKeys), null);

      //      //--
      //      //-- Nested param support
      //      //TODO: don't remember the use case here.  need to find and make a test case
      //      Map<String, String> params = Chain.peek().getRequest().getParams();
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
      int sc = res.getStatusCode();
      if (sc == 401 || sc == 403)//unauthorized || forbidden
         return null;

      if (sc == 404)
      {
         return Collections.EMPTY_LIST;
      }
      else if (sc == 500)
      {
         res.rethrow();
      }
      else if (sc == 200)
      {
         List<JSNode> nodes = (List<JSNode>) res.getData().asList();

         for (JSNode node : nodes)
         {
            Object entityKey = getEntityKey((JSNode) node);
            if (pkCache.containsKey(collection, entityKey))
            {
               ApiException.throw500InternalServerError("FIX ME IF FOUND.  Algorithm Implementation Error");
               return null;
            }

            pkCache.put(collection, entityKey, node);
         }
         return nodes;
      }

      res.rethrow();
      return null;
   }

   public int getMaxRows()
   {
      return maxRows;
   }

   public RestGetAction withMaxRows(int maxRows)
   {
      this.maxRows = maxRows;
      return this;
   }

   //-------------------------------------------------------------------------------------
   //-------------------------------------------------------------------------------------
   //-Static Utils -----------------------------------------------------------------------
   //-------------------------------------------------------------------------------------

   protected static String getEntityKey(Object obj)
   {
      if (obj == null)
         return null;

      if (obj instanceof JSNode)
         obj = ((JSNode) obj).get("href");

      String str = (String) obj;
      int idx = str.lastIndexOf('/');
      if (idx > 0)
         str = str.substring(idx + 1, str.length());
      return str;

   }

   static void mapToAttributes(Collection collection, Term term)
   {
      if (collection == null)
         return;

      if (term.isLeaf() && !term.isQuoted())
      {
         String token = term.getToken();

         Property attr = collection.findProperty(token);
         if (attr != null)
            term.withToken(attr.getJsonName());
      }
      else
      {
         for (Term child : term.getTerms())
         {
            mapToAttributes(collection, child);
         }
      }
   }

   public static String stripTerms(String url, String... tokens)
   {
      Url u = new Url(url);

      for (int i = 0; tokens != null && i < tokens.length; i++)
      {
         String token = tokens[i];
         if (token != null)
         {
            u.clearParams(token);
         }
      }

      return u.toString();
   }

   protected static String stripTerm(String str, String startToken, char... endingTokens)
   {
      Set tokens = new HashSet();
      for (char c : endingTokens)
         tokens.add(c);

      while (true)
      {
         int start = str.toLowerCase().indexOf(startToken);

         //this makes sure the char before the start token is not a letter or number which would mean we are in the
         //middle of another token, not at the start of a token.
         while (start > 0 && (Character.isAlphabetic(str.charAt(start - 1)) || Character.isDigit(start - 1)))
            start = str.toLowerCase().indexOf(startToken, start + 1);

         if (start > -1)
         {
            String beginning = str.substring(0, start);

            int end = start + startToken.length() + 1;
            while (end < str.length())
            {
               char c = str.charAt(end);
               if (tokens.contains(c))
                  break;
               end += 1;
            }

            if (end == str.length())
               str = beginning;
            else
               str = beginning + str.substring(end + 1, str.length());
         }
         else
         {
            break;
         }
      }

      return str;
   }

   protected static String expandPath(String path, Object next)
   {
      if (Utils.empty(path))
         return next + "";
      else
         return path + "." + next;
   }

   protected static boolean shouldExpand(Set<String> expands, String path, Relationship rel)
   {
      boolean expand = false;
      path = path.length() == 0 ? rel.getName() : path + "." + rel.getName();
      path = path.toLowerCase();

      for (String ep : expands)
      {
         if (ep.startsWith(path) && (ep.length() == path.length() || ep.charAt(path.length()) == '.'))
         {
            expand = true;
            break;
         }
      }

      //System.out.println("expand(" + expands + ", " + path + ") -> " + expand);

      return expand;
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

   protected static boolean find(java.util.Collection<String> params, String path, boolean matchStart)
   {
      boolean rtval = false;

      if (params.contains(path))
      {
         rtval = true;
      }
      else
      {
         for (String param : params)
         {
            if (matchStart)
            {
               if (param.startsWith(path + "."))
               {
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

}
