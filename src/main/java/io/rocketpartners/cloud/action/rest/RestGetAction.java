package io.rocketpartners.cloud.action.rest;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.map.MultiKeyMap;

import io.rocketpartners.cloud.model.Action;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.ApiException;
import io.rocketpartners.cloud.model.ArrayNode;
import io.rocketpartners.cloud.model.Attribute;
import io.rocketpartners.cloud.model.Collection;
import io.rocketpartners.cloud.model.Column;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.model.Entity;
import io.rocketpartners.cloud.model.ObjectNode;
import io.rocketpartners.cloud.model.Relationship;
import io.rocketpartners.cloud.model.Request;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.model.Results;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.rql.Page;
import io.rocketpartners.cloud.rql.Parser;
import io.rocketpartners.cloud.rql.Term;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.Rows;
import io.rocketpartners.cloud.utils.Rows.Row;
import io.rocketpartners.cloud.utils.Utils;

public class RestGetAction extends Action<RestGetAction>
{

   protected int maxRows        = 100;

   protected Set reservedParams = new HashSet(Arrays.asList("includes", "excludes", "expands"));

   @Override
   public void run(Service service, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
   {
      Chain.debug("");
      Chain.debug("RestGetAction -> " + req.getUrl());

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

            collection = req.getApi().getCollection(rel.getRelated());
            String fkColName = rel.getFkCol1().getName();
            String fkAttrName = collection.getAttributeName(fkColName);
            newHref = Chain.buildLink(collection, null, null) + "?" + fkAttrName + "=" + req.getEntityKey();
         }
         else if (rel.isManyToMany())
         {
            //CONVERTS: http://localhost/northwind/source/employees/1/territories
            //TO THIS : http://localhost/northwind/source/territories/06897,19713

            Column toMatch = rel.getFkCol1();
            Column toRetrieve = rel.getFkCol2();

            Rows rows = toMatch.getTable().getDb().select(toMatch.getTable(), toMatch, toRetrieve, Arrays.asList(entityKey));
            if (rows.size() > 0)
            {
               List foreignKeys = new ArrayList();
               for (Row row : rows)
                  foreignKeys.add(row.get(toRetrieve.getName()));

               Collection relatedCollection = req.getApi().getCollection(rel.getFkCol2().getPk().getTable());
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
            //ONE_TO_MANY links are not written out referencing the linking entity
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
         Response included = service.get(newHref);
         res.withStatus(included.getStatus());
         res.withJson(included.getJson());
         return;
      }
      else if (req.getEntityKey() != null)
      {
         Attribute keyAttr = req.getCollection().getEntity().getKey();

         List<String> entityKeys = Utils.explode(",", req.getEntityKey());
         entityKeys.add(0, keyAttr.getName());

         Term term = Term.term(null, (entityKeys.size() == 2 ? "eq" : "in"), entityKeys.toArray());
         req.getUrl().withParam(term.toString(), null);
      }

      Results<ObjectNode> results = select(req, req.getCollection(), req.getUrl().getParams());

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

         int rowCount = results.getRowCount();
         if (rowCount >= 0)
            res.withRowCount(results.getRowCount());

         int offest = page.getOffset();
         int limit = page.getLimit();

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
                     next = replaceTerm(next, nextTerm);
                  }
                  res.withNext(next);
               }
               else if (results.size() == limit && (rowCount < 0 || (offest + limit) < rowCount))
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

   protected Results<ObjectNode> select(Request req, Collection collection, Map<String, String> params) throws Exception
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
            terms.add(term);
            mapToColumns(collection, term);
         }
      }

      Results results = collection.getDb().select(req, collection.getTable(), terms);

      if (results.size() > 0)
      {

         for (int i = 0; i < results.size(); i++)
         {
            //convert the map into an ObjectNode
            Map<String, Object> row = results.getRow(i);

            if (collection == null)
            {
               ObjectNode node = new ObjectNode(row);
               results.setRow(i, node);
            }
            else
            {
               ObjectNode node = new ObjectNode();
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
                     Object fkval = node.remove(rel.getFkCol1().getName());
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
                     node.put(key, row.get(key));
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
         expand(collection, results.getRows(), null, null, null);
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

   public void exclude(List<ObjectNode> nodes)
   {
      Set includes = Chain.peek().mergeEndpointActionParamsConfig("includes");
      Set excludes = Chain.peek().mergeEndpointActionParamsConfig("excludes");

      if (includes.size() > 0 || excludes.size() > 0)
      {
         for (ObjectNode node : nodes)
         {
            exclude(node, includes, excludes, null);
         }
      }
   }

   public void exclude(ObjectNode node, Set includes, Set excludes, String path)
   {
      for (String key : node.keySet())
      {
         String attrPath = path != null ? (path + "." + key) : key;
         if (!include(attrPath, includes, excludes))
         {
            node.remove(key);
         }
         else
         {
            Object value = node.get(key);

            if (value instanceof ArrayNode)
            {
               ArrayNode arr = (ArrayNode) value;
               for (int i = 0; i < arr.size(); i++)
               {
                  if (arr.get(i) instanceof ObjectNode)
                  {
                     exclude((ObjectNode) arr.get(i), includes, excludes, attrPath);
                  }
               }
            }
            else if (value instanceof ObjectNode)
            {
               exclude((ObjectNode) value, includes, excludes, attrPath);
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

   protected void expand(Collection collection, List<ObjectNode> parentObjs, Set expands, String expandsPath, MultiKeyMap pkCache) throws Exception
   {
      if (parentObjs.size() == 0)
         return;

      if (expands == null)
         expands = Chain.peek().mergeEndpointActionParamsConfig("expands");

      if (expandsPath == null)
         expandsPath = "";

      if (pkCache == null)
      {
         //------------------------------------------------
         // pkCache is used to make nested document expansion efficient
         //
         // the pkCache is used to map requested entities back to the right 
         // objects on the recursion stack and to keep track of entities
         // so you don't waste time requerying for things you have 
         // already retrieved.
         pkCache = new MultiKeyMap<>();

         for (ObjectNode node : parentObjs)
         {
            pkCache.put(collection, getEntityKey(node), node);
         }
      }

      for (Relationship rel : collection.getEntity().getRelationships())
      {
         if (expand(expands, expandsPath, rel))
         {
            //ONE_TO_MANY - Player.locationId -> Location.id
            //MANY_TO_ONE - Location.id <- Player.locationId  
            //MANY_TO_MANY, ex going from Category(id)->CategoryBooks(categoryId, bookId)->Book(id)

            final Collection relatedCollection = rel.getRelated().getCollection();
            Column toMatchCol = null;
            Column toRetrieveCol = null;

            Rows allChildEks = null;

            if (rel.isOneToMany())
            {
               toMatchCol = collection.getEntity().getKey().getColumn();
               toRetrieveCol = rel.getFkCol1();

               //NOTE: expands() is only getting the paired up related keys.  For a ONE_TO_MANY
               //relationship that data is already in the parent object you are trying to expand
               //so we don't need to query the db to find those relationships as we do for the 
               //MANY_TO relationships.
               //
               //However if you were to comment out the following block, the output of the algorithm
               //would be exactly the same you would just end up running an extra db query
               allChildEks = new Rows(Arrays.asList(toMatchCol.getName(), toRetrieveCol.getName()));
               for (ObjectNode parentObj : parentObjs)
               {
                  String parentEk = getEntityKey(parentObj);
                  String childEk = parentObj.getString(rel.getName());
                  if (childEk != null)
                  {
                     childEk = getEntityKey(childEk);
                     allChildEks.addRow(new Object[]{parentEk, childEk});
                  }
               }
            }
            else if (rel.isManyToOne())
            {
               toMatchCol = rel.getFkCol1();
               toRetrieveCol = rel.getEntity().getKey().getColumn();
            }
            else if (rel.isManyToMany())
            {
               toMatchCol = rel.getFkCol1();
               toRetrieveCol = rel.getFkCol2();
            }

            List toMatchEks = new ArrayList();
            for (ObjectNode parentObj : parentObjs)
            {
               String parentEk = getEntityKey(parentObj);
               if (!toMatchEks.contains(parentEk))
               {
                  if (parentObj.get(rel.getName()) instanceof ArrayNode)
                     throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Algorithm implementation error");

                  toMatchEks.add(parentEk);

                  if (rel.isOneToMany())
                  {
                     parentObj.remove(rel.getName());
                  }
                  else
                  {
                     parentObj.put(rel.getName(), new ArrayNode());
                  }
               }
            }

            List newChildEks = new ArrayList();
            allChildEks = allChildEks != null ? allChildEks : relatedCollection.getDb().select(toRetrieveCol.getTable(), toMatchCol, toRetrieveCol, toMatchEks);
            for (Row row : allChildEks)
            {
               String relatedEk = row.get(toRetrieveCol.getName()).toString();
               if (!pkCache.containsKey(relatedCollection, relatedEk))
               {
                  newChildEks.add(relatedEk);
               }
            }

            //this recursive call populates the pkCache
            List<ObjectNode> newChildObjs = recursiveGet(pkCache, relatedCollection, newChildEks, expandPath(expandsPath, rel.getName()));

            for (Row row : allChildEks)
            {
               String toMatchStr = row.getString(toMatchCol.getName()).toString();
               String toRetrieveStr = row.getString(toRetrieveCol.getName()).toString();

               ObjectNode parentObj = (ObjectNode) pkCache.get(collection, toMatchStr);
               ObjectNode childObj = (ObjectNode) pkCache.get(relatedCollection, toRetrieveStr);

               if (rel.isOneToMany())
               {
                  parentObj.put(rel.getName(), childObj);
               }
               else
               {
                  parentObj.getArray(rel.getName()).add(childObj);
               }
            }

            if (newChildObjs.size() > 0)
            {
               expand(relatedCollection, newChildObjs, expands, expandPath(expandsPath, rel.getName()), pkCache);
            }
         }
      }
   }

   protected List<ObjectNode> recursiveGet(MultiKeyMap pkCache, Collection collection, java.util.Collection entityKeys, String expandsPath) throws Exception
   {
      if (entityKeys.size() == 0)
         return Collections.EMPTY_LIST;

      String url = Chain.buildLink(collection, Utils.implode(",", entityKeys), null);

      //--
      //-- Nested param support
      //TODO: don't remember the use case here.  need to find and make a test case
      Map<String, String> params = Chain.getRequest().getParams();
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

      Response res = Chain.peek().getService().get(url);
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
         List<ObjectNode> nodes = (List<ObjectNode>) res.data().asList();

         for (ObjectNode node : nodes)
         {
            Object entityKey = getEntityKey((ObjectNode) node);
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

      if (obj instanceof ObjectNode)
         obj = ((ObjectNode) obj).get("href");

      String str = (String) obj;
      int idx = str.lastIndexOf('/');
      if (idx > 0)
         str = str.substring(idx + 1, str.length());
      return str;

   }

   protected static String replaceTerm(String url, Term term)
   {
      String op = term.hasToken("eq") ? term.getToken(0) : term.getToken();

      url = stripTerms(url, op);

      if (url.indexOf("?") < 0)
         url += "?";

      if (!url.endsWith("?") && !url.endsWith("&"))
         url += "&";

      url += term.toString();

      return url;
   }

   protected static void mapToColumns(Collection collection, Term term)
   {
      if (term.isLeaf() && !term.isQuoted())
      {
         String token = term.getToken();

         while (token.startsWith("-") || token.startsWith("+"))
            token = token.substring(1, token.length());

         Attribute attr = collection.getAttribute(token);
         if (attr != null)
         {
            String columnName = attr.getColumn().getName();

            if (term.getToken().startsWith("-"))
               columnName = "-" + columnName;

            term.withToken(columnName);
         }
      }
      else
      {
         for (Term child : term.getTerms())
         {
            mapToColumns(collection, child);
         }
      }
   }

   static void mapToAttributes(Collection collection, Term term)
   {
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

   protected static String stripTerms(String url, String... tokens)
   {
      for (String token : tokens)
      {
         url = url.replaceAll("(.*)(" + token + "=[^\\&]*\\&?)(.*)", "$1$3");
         url = url.replaceAll("(.*)(" + token + "\\([^\\&]*\\)\\&?)(.*)", "$1$3");
         url = url.replaceAll("(.*)(eq\\(" + token + "\\,[^\\&]*\\)\\&?)(.*)", "$1$3");
      }
      url = url.replace("&&", "&");
      return url;
   }

   protected static String expandPath(String path, Object next)
   {
      if (Utils.empty(path))
         return next + "";
      else
         return path + "." + next;
   }

   protected static boolean expand(Set<String> expands, String path, Relationship rel)
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

      return expand;
   }

   protected static boolean include(String path, Set<String> includes, Set<String> excludes)
   {
      if ("employee".equals(path))
         System.out.println("asdfasdf");

      boolean include = true;

      if (includes.size() == 0 && excludes.size() == 0)
      {
         include = true;
      }
      else
      {
         path = path.toLowerCase();

         if (includes != null && includes.size() > 0)
         {
            include = false;
            include = find(includes, path);
         }

         if (excludes != null && excludes.size() > 0 && find(excludes, path))
         {
            include = false;
         }
      }

      System.out.println("include(" + path + ") -> " + include);

      return include;
   }

   protected static boolean find(java.util.Collection<String> params, String path)
   {
      if (params.contains(path))
         return true;

      for (String param : params)
      {
         if (param.startsWith(path + "."))
            return true;

         if (Utils.wildcardMatch(param, path))
            return true;
      }
      return false;

   }

}
