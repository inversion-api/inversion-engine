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

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.KeyValue;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.keyvalue.DefaultKeyValue;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import io.inversion.cloud.model.Action;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.ApiException;
import io.inversion.cloud.model.JSArray;
import io.inversion.cloud.model.Attribute;
import io.inversion.cloud.model.Collection;
import io.inversion.cloud.model.Column;
import io.inversion.cloud.model.Db;
import io.inversion.cloud.model.Endpoint;
import io.inversion.cloud.model.Entity;
import io.inversion.cloud.model.Index;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Relationship;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.model.Results;
import io.inversion.cloud.model.SC;
import io.inversion.cloud.model.Table;
import io.inversion.cloud.model.Url;
import io.inversion.cloud.rql.Page;
import io.inversion.cloud.rql.Parser;
import io.inversion.cloud.rql.Term;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.service.Engine;
import io.inversion.cloud.utils.Rows;
import io.inversion.cloud.utils.Rows.Row;
import io.inversion.cloud.utils.Utils;

public class RestGetAction extends Action<RestGetAction>
{

   protected int maxRows        = 100;

   /**
    * These params are specifically NOT passed to the Query for parsing.  These are either dirty worlds like sql injection tokens or the are used by actions themselves 
    */
   protected Set reservedParams = new HashSet(Arrays.asList("select", "insert", "update", "delete", "drop", "union", "truncate", "exec", "explain", /*"includes",*/ "excludes", "expands"));

   public RestGetAction()
   {
      this(null);
   }

   public RestGetAction(String inludePaths)
   {
      this(inludePaths, null, null);
   }

   public RestGetAction(String inludePaths, String excludePaths, String config)
   {
      super(inludePaths, excludePaths, config);
      withMethods("GET");
   }

   @Override
   public void run(Engine engine, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
   {
      if (req.getSubCollectionKey() != null)
      {
         String entityKey = req.getEntityKey();
         Collection collection = req.getCollection();
         Entity entity = collection.getEntity();
         Relationship rel = entity.getRelationship(req.getSubCollectionKey());

         if (rel == null)
            throw new ApiException(SC.SC_404_NOT_FOUND, "'" + req.getSubCollectionKey() + "' is not a valid relationship");

         String newHref = null;

         if (rel.isManyToOne())
         {
            //CONVERTS: http://localhost/northwind/sql/orders/10395/orderdetails
            //TO THIS : http://localhost/northwind/sql/orderdetails?orderid=10395

            //CONVERTS: http://localhost/northwind/sql/collection/val1~val2/subcollection
            //TO THIS : http://localhost/northwind/sql/subcollection?col1=val1&col2=val2

            //TODO: need a compound key test case here
            Collection relatedCollection = rel.getRelated().getCollection();

            newHref = Chain.buildLink(relatedCollection, null, null) + "?";

            Row entityKeyRow = collection.getTable().decodeKey(req.getEntityKey());

            //maps query string parameter names for the main tables pk to the related tables fk
            Index fkIdx = rel.getFkIndex1();
            for (Column fk : fkIdx.getColumns())
            {
               String pkName = fk.getPk().getName();
               Object pkVal = entityKeyRow.get(pkName);

               if (pkVal == null)
                  throw new ApiException(SC.SC_400_BAD_REQUEST, "Missing parameter for foreign key column '" + fk + "'");

               newHref += fk.getName() + "=" + pkVal + "&";
            }

            newHref = newHref.substring(0, newHref.length() - 1);
         }
         else if (rel.isManyToMany())
         {
            //CONVERTS: http://localhost/northwind/source/employees/1/territories
            //TO THIS : http://localhost/northwind/source/territories/06897,19713

            List<KeyValue> rows = getRelatedKeys(rel.getFkIndex1(), rel.getFkIndex2(), Arrays.asList(entityKey));
            if (rows.size() > 0)
            {
               //TODO need to escape values (~',) in this string and add test case
               List foreignKeys = new ArrayList();
               rows.forEach(k -> foreignKeys.add(k.getValue()));

               Collection relatedCollection = rel.getRelated().getCollection();
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
            //The link was requested like this  : http://localhost/northwind/source/orderdetails/XXXXX/order
            //The system would have written out : http://localhost/northwind/source/orders/YYYYY
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

         //TODO: forward better symentec here?
         Response included = engine.get(newHref);
         res.withStatus(included.getStatus());
         res.withJson(included.getJson());
         return;
      }
      else if (req.getCollection() != null && req.getEntityKey() != null)
      {
         List<String> entityKeys = Utils.explode(",", req.getEntityKey());
         Term term = Term.term(null, "_key", req.getCollection().getEntity().getTable().getPrimaryIndex().getName(), entityKeys.toArray());
         req.getUrl().withParams(term.toString(), null);
      }

      Results<JSNode> results = select(req, req.getCollection(), req.getParams(), api);

      if (results.size() == 0 && req.getEntityKey() != null && req.getCollectionKey() != null)
      {
         res.withStatus(SC.SC_404_NOT_FOUND);
      }
      else
      {
         //copy data into the response
         res.withRecords(results.getRows());

         //------------------------------------------------
         //setup all of the meta section

         Page page = results.getQuery().page();
         res.withPageSize(page.getPageSize());
         res.withPageNum(page.getPageNum());

         int offest = page.getOffset();
         int limit = page.getLimit();

         int foundRows = results.getFoundRows();

         if (foundRows < 0 && results.size() > 0 && offest <= 0 && results.size() < limit)
            foundRows = results.size();

         if (foundRows >= 0)
         {
            //  Chain.peek().put("foundRows", foundRows);
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
         Parser parser = new Parser();
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

                     Index pk = collection.getTable().getPrimaryIndex();
                     for (Column c : pk.getColumns())
                     {
                        boolean includesPkCol = false;
                        for (Term col : term.getTerms())
                        {
                           if (col.hasToken(c.getName()))
                           {
                              includesPkCol = true;
                              break;
                           }
                        }
                        if (!includesPkCol)
                           term.withTerm(Term.term(term, c.getName()));
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

      Results results = null;

      if (collection == null)
      {
         Db db = api.findDb((String) Chain.peek().get("db"));

         if (db == null)
            throw new ApiException(SC.SC_400_BAD_REQUEST, "Unable to find collection for url '" + req.getUrl() + "'");

         results = db.select(null, terms);
      }
      else
      {
         results = collection.getDb().select(collection.getTable(), terms);
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

               String entityKey = req.getCollection().getTable().encodeKey(row);

               if (Utils.empty(entityKey))
                  throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Unable to determine entity key for " + row);

               //------------------------------------------------
               //copy over defined attributes first, if the select returned 
               //extra columns they will be copied over last
               for (Attribute attr : collection.getEntity().getAttributes())
               {
                  String attrName = attr.getName();
                  String colName = attr.getColumn().getName();
                  Object val = row.remove(colName);
                  node.put(attrName, val);
               }

               //------------------------------------------------
               //next turn all relationships into links that will 
               //retrieve the related entities
               for (Relationship rel : collection.getEntity().getRelationships())
               {
                  String link = null;
                  if (rel.isOneToMany())
                  {
                     //Object fkval = node.remove(rel.getFk1Col1().getName());
                     Object fkval = node.get(rel.getFk1Col1().getName());
                     if (fkval != null)
                     {
                        link = Chain.buildLink(rel.getRelated().getCollection(), fkval.toString(), null);
                     }
                  }
                  else
                  {
                     link = Chain.buildLink(req.getCollection(), entityKey, rel.getName());
                  }
                  node.put(rel.getName(), link);
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

               //------------------------------------------------
               // finally make sure the entity key is encoded as
               // the href
               String href = node.getString("href");
               if (Utils.empty(href))
               {
                  href = Chain.buildLink(collection, entityKey, null);
                  node.put("href", href);
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

   public void exclude(List<JSNode> nodes)
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

   public void exclude(JSNode node, Set includes, Set excludes, String path)
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

   /**
    * This is more complicated that it seems like it would need to be because 
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

      for (Relationship rel : collection.getEntity().getRelationships())
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

            //ONE_TO_MANY - Player.locationId -> Location.id
            //MANY_TO_ONE - Location.id <- Player.locationId  
            //MANY_TO_MANY, ex going from Category(id)->CategoryBooks(categoryId, bookId)->Book(id)

            final Collection relatedCollection = rel.getRelated().getCollection();
            //            Column toMatchCol = null;
            //            Column toRetrieveCol = null;

            Index idxToMatch = null;
            Index idxToRetrieve = null;
            List<KeyValue> relatedEks = null;

            if (rel.isOneToMany())
            {
               idxToMatch = collection.getEntity().getTable().getPrimaryIndex();
               idxToRetrieve = rel.getFkIndex1();

               //NOTE: expands() is only getting the paired up related keys.  For a ONE_TO_MANY
               //relationship that data is already in the parent object you are trying to expand
               //so we don't need to query the db to find those relationships as we do for the 
               //MANY_TO relationships.
               //
               //However if you were to comment out the following block, the output of the algorithm
               //would be exactly the same you would just end up running an extra db query

               List cols = new ArrayList();
               idxToMatch.getColumns().forEach(c -> cols.add(c.getName()));
               idxToRetrieve.getColumns().forEach(c -> cols.add(c.getName()));

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
            else if (rel.isManyToOne())
            {
               //               idxToMatch = rel.getFkIndex1();
               //               idxToRetrieve = rel.getRelated().getTable().getPrimaryIndex();//Entity().getKey().getColumn();

               idxToMatch = rel.getFkIndex1();
               idxToRetrieve = rel.getRelated().getTable().getPrimaryIndex();

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
                        throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Algorithm implementation error...this relationship seems to have already been expanded.");

                     toMatchEks.add(parentEk);

                     if (rel.isOneToMany())
                     {
                        parentObj.remove(rel.getName());
                     }
                     else
                     {
                        parentObj.put(rel.getName(), new JSArray());
                     }
                  }
               }
               relatedEks = getRelatedKeys(idxToMatch, idxToRetrieve, toMatchEks);
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

               if (rel.isOneToMany())
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

   protected List<KeyValue> getRelatedKeys(Index idxToMatch, Index idxToRetrieve, List<String> toMatchEks) throws Exception
   {
      if (idxToMatch.getTable() != idxToRetrieve.getTable())
         throw new ApiException(SC.SC_400_BAD_REQUEST, "You can only retrieve corolated index keys from the same table.");
      List<KeyValue> related = new ArrayList<>();

      List columns = new ArrayList();
      idxToMatch.getColumns().forEach(c -> columns.add(c.getName()));
      idxToRetrieve.getColumns().forEach(c -> columns.add(c.getName()));

      Term termKeys = Term.term(null, "_key", idxToMatch.getName(), toMatchEks);
      Term includes = Term.term(null, "includes", columns);
      Term sort = Term.term(null, "sort", columns);
      Term notNull = Term.term(null, "nn", columns);

      Rows rows = ((Rows) idxToMatch.getColumn(0).getTable().getDb().select(idxToRetrieve.getTable(), Arrays.asList(termKeys, includes, sort, notNull)).getRows());
      for (Row row : rows)
      {
         List keyParts = row.asList();
         String parentEk = Table.encodeKey(keyParts.subList(0, idxToMatch.getColumns().size()));
         String relatedEk = Table.encodeKey(keyParts.subList(idxToMatch.getColumns().size(), keyParts.size()));

         related.add(new DefaultKeyValue(parentEk, relatedEk));
      }

      return related;
   }

   protected List<JSNode> recursiveGet(MultiKeyMap pkCache, Collection collection, java.util.Collection entityKeys, String expandsPath) throws Exception
   {
      if (entityKeys.size() == 0)
         return Collections.EMPTY_LIST;

      String url = Chain.buildLink(collection, Utils.implode(",", entityKeys), null);

      //--
      //-- Nested param support
      //TODO: don't remember the use case here.  need to find and make a test case
      Map<String, String> params = Chain.peek().getRequest().getParams();
      String lcPath = expandsPath.toLowerCase();
      for (String key : params.keySet())
      {
         String lcKey = key.toLowerCase();

         if (reservedParams.contains(lcKey))
            continue;

         if (lcKey.matches(".*\\b" + lcPath.replace(".", "\\.") + ".*"))
         {
            String value = params.get(key);
            lcKey = key.replaceAll("\\b" + (lcPath + "\\."), "");

            if (url.indexOf("?") < 0)
               url += "?";
            url += URLEncoder.encode(lcKey, "UTF-8");
            if (!Utils.empty(value))
               url += "=" + URLEncoder.encode(value, "UTF-8");
         }
      }

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
         if (res.getError() != null)
            throw res.getError();
         else
            throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, res.getText());
      }
      else if (sc == 200)
      {
         List<JSNode> nodes = (List<JSNode>) res.data().asList();

         for (JSNode node : nodes)
         {
            Object entityKey = getEntityKey((JSNode) node);
            if (pkCache.containsKey(collection, entityKey))
               throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "FIX ME IF FOUND.  Algorithm Implementation Error");

            pkCache.put(collection, entityKey, node);
         }
         return nodes;
      }

      throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Unknow repose code \"" + sc + "\" or body type from nested query.");
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
         String attrName = collection.getAttributeName(token);
         if (attrName != null)
            term.withToken(attrName);
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
      Parser p = new Parser();

      Map<String, String> params = u.getParams();
      for (String key : params.keySet())
      {
         Term t = p.parse(key);

         for (String token : tokens)
         {
            if (key.equalsIgnoreCase(token) //
                  || (t.hasToken("eq") && token.equalsIgnoreCase(t.getToken(0))) //
                  || t.getToken().equalsIgnoreCase(token)//
            )
            {
               u.removeParam(key);
               break;
            }
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

   protected static boolean exclude(String path, Set<String> includes, Set<String> excludes)
   {
      boolean exclude = false;

      if (includes.size() > 0 || excludes.size() > 0)
      {
         path = path.toLowerCase();

         if (includes != null && includes.size() > 0)
         {
            if (!find(includes, path, true))
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
