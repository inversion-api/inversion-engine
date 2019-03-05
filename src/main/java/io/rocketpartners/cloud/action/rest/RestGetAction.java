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
import io.rocketpartners.cloud.model.Db;
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
import io.rocketpartners.cloud.utils.SqlUtils;
import io.rocketpartners.cloud.utils.Utils;

public class RestGetAction extends Action<RestGetAction>
{

   protected int maxRows        = 100;

   protected Set reservedParams = new HashSet(Arrays.asList("includes", "excludes", "expands"));

   @Override
   public void run(Service service, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
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
            throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Implementation logic error.");
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

      //todo process

      Results<ObjectNode> results = select(req, req.getCollection(), req.getUrl().getParams());

      if (results.size() == 0 && req.getEntityKey() != null && req.getCollectionKey() != null)
      {
         res.withStatus(SC.SC_404_NOT_FOUND);
      }
      else
      {
         Collection collection = req.getCollection();
         for (ObjectNode node : results.getRows())
         {
            res.withRecord(node);
         }

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
            mapToColumns(collection, term);
            terms.add(term);
         }
      }

      if (collection == null)
      {
         System.out.println("NULL COLLECTION WTF: " + req.getUrl());
      }

      Db db = collection.getDb();

      if (db == null)
      {
         System.out.println("NULL DB WTF: " + req.getUrl());
      }

      Results results = db.select(req, collection.getTable(), terms);

      Set<String> includes = req.getChain().getConfigSet("includes");
      includes.addAll(splitParam(req, "includes"));

      Set<String> expands = req.getChain().getConfigSet("expands");
      expands.addAll(splitParam(req, "expands"));

      Set<String> excludes = req.getChain().getConfigSet("excludes");
      excludes.addAll(splitParam(req, "excludes"));

      MultiKeyMap pkCache = new MultiKeyMap<>();

      for (int i = 0; i < results.size(); i++)
      {
         Map<String, Object> row = results.getRow(i);

         ObjectNode node = new ObjectNode(row);
         if (collection == null)
         {
            node = new ObjectNode(row);
         }
         else
         {
            String entityKey = req.getCollection().getTable().encodeKey(row);
            if (Utils.empty(entityKey))
               throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Unable to determine entity key for " + row);

            node = new ObjectNode();

            pkCache.put(collection, entityKey, node);

            //         for (String colName : (Set<String>) row.keySet())
            //         //for (Attribute attr : collection.getEntity().getAttributes())
            //         {
            //            //String attrName = attr.getName();
            //            //String colName = attr.getColumn().getName();
            //            //Object value = row.get(colName);
            //            Object value = row.get(colName);
            //            String attrName = colName;
            //
            //            if (colName.equalsIgnoreCase(keyAttr.getColumn().getName()))
            //            {
            //               String href = Service.buildLink(req, req.getCollectionKey(), value, null);
            //               if (include("href", includes, excludes, path))
            //               {
            //                  js.put("href", href);
            //               }
            //            }
            //
            //            if (include(attrName, includes, excludes, path))
            //            {
            //               js.put(attrName, value);
            //            }
            //         }

            for (Attribute attr : collection.getEntity().getAttributes())
            {
               String attrName = attr.getName();
               String colName = attr.getColumn().getName();
               Object val = row.remove(colName);
               node.put(attrName, val);
            }

            for (Relationship rel : collection.getEntity().getRelationships())
            {
               if (rel.isOneToMany()) //ONE_TO_MANY - Player.locationId -> Location.id
               {
                  Object fkval = node.remove(rel.getFkCol1().getName());
                  if (fkval != null)
                  {
                     String link = Chain.buildLink(rel.getRelated().getCollection(), fkval.toString(), null);
                     node.put(rel.getName(), link);
                  }
                  else
                  {
                     node.put(rel.getName(), null);
                  }
               }
               else
               {
                  String link = Chain.buildLink(req.getCollection(), entityKey, rel.getName());
                  node.put(rel.getName(), link);
               }
            }

            for (String key : row.keySet())
            {
               if (!key.equalsIgnoreCase("href") && !node.containsKey(key))
                  node.put(key, row.get(key));
            }

            String href = node.getString("href");
            if (Utils.empty(href))
            {
               href = Chain.buildLink(collection, entityKey, null);
               node.put("href", href);
            }
         }
         results.setRow(i, node);
      }

      if (expands.size() > 0)
      {
         expand(collection, "", results.getRows(), includes, excludes, expands, pkCache);
      }

      for (Term term : ((List<Term>) results.getNext()))
      {
         mapToAttributes(collection, term);
      }

      return results;
   }

   /**
    * This is more complicated that it seems like it would need to be because 
    * it attempts to retrieve all values of a relationship at a time for the whole 
    * document.  It does not run a recursive query for each entity and each relationship
    * which could mean hundreds and hundreds of queries per document.  This should
    * result in number of queries proportional to the number of expands terms that does
    * not increase with the number of results at any level of the expansion.
    */
   protected void expand(Collection collection, String path, List<ObjectNode> parentObjs, Set includes, Set excludes, Set expands, MultiKeyMap pkCache) throws Exception
   {
      System.out.println(path);

      if (parentObjs.size() == 0)
         return;

      for (Relationship rel : collection.getEntity().getRelationships())
      {
         if (expand(expands, path, rel))
         {
            //ONE_TO_MANY - Player.locationId -> Location.id
            //MANY_TO_ONE - Location.id <- Player.locationId  
            //MANY_TO_MANY, ex going from Category(id)->CategoryBooks(categoryId, bookId)->Book(id)

            final Collection relatedCollection = rel.getRelated().getCollection();
            Column toMatchCol = null;
            Column toRetrieveCol = null;

            if (rel.isOneToMany())
            {
               //TODO: WE ACTUALLY DON'T NEED TO DO THIS DB ROUNDTRIP.  WE HAVE ALL OF THE IDS ALREAD
               toMatchCol = collection.getEntity().getKey().getColumn();
               toRetrieveCol = rel.getFkCol1();
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
                     throw new ApiException("Implementation error");

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
            Rows allChildEks = relatedCollection.getDb().select(toRetrieveCol.getTable(), toMatchCol, toRetrieveCol, toMatchEks);
            for (Row row : allChildEks)
            {
               String relatedEk = row.get(toRetrieveCol.getName()).toString();
               if (!pkCache.containsKey(relatedCollection, relatedEk))
               {
                  newChildEks.add(relatedEk);
               }
            }

            //this recursive call populates the pkCache
            List<ObjectNode> newChildObjs = getNestedObjects(pkCache, relatedCollection, newChildEks, includes, excludes, expandPath(path, rel.getName()));

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

            expand(relatedCollection, expandPath(path, rel.getName()), newChildObjs, includes, excludes, expands, pkCache);

         }
      }
   }

   protected String getEntityKey(Object obj)
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

   protected List<ObjectNode> getNestedObjects(MultiKeyMap pkCache, Collection collection, java.util.Collection ids, Set includes, Set excludes, String path) throws Exception
   {
      if (ids.size() == 0)
         return Collections.EMPTY_LIST;

      String url = Chain.buildLink(collection, SqlUtils.getInClauseStr(ids).replaceAll(" ", ""), null);

      //--
      //-- Nested param support
      Map<String, String> params = Chain.getRequest().getParams();
      String lcPath = path.toLowerCase();
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
         return Collections.EMPTY_LIST;

      if (sc == 500)
      {
         throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, res.getText());
      }

      if (sc == 200)
      {
         for (Object obj : res.data().asList())
         {
            Object entityKey = getEntityKey((ObjectNode) obj);
            if (pkCache.containsKey(entityKey))
               System.out.println("why is this already here???");
            pkCache.put(collection, entityKey, obj);
         }
         return res.data().asList();
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

      //System.out.println("expand(" + path + ") -> " + expand);

      return expand;
   }

   protected static boolean include(String attr, Set<String> includes, Set<String> excludes)
   {
      return include(attr, includes, excludes, null);
   }

   protected static boolean include(String attr, Set<String> includes, Set<String> excludes, String path)
   {
      if (includes.size() == 0 && excludes.size() == 0)
         return true;

      String key = (path != null && path.length() > 0 ? (path + "." + attr) : attr).toLowerCase();

      if (includes != null && includes.size() > 0)
      {
         return find(includes, key);
      }

      if (excludes != null && excludes.size() > 0 && find(excludes, key))
      {
         return false;
      }

      return true;
   }

   protected static boolean find(java.util.Collection<String> haystack, String needle)
   {
      //      if(needle.equalsIgnoreCase("adcompleters.ad"))
      //         System.out.println("asdf");
      String lc = needle.toLowerCase();
      if (haystack.contains(needle) || haystack.contains(lc))
         return true;

      for (String pattern : haystack)
      {
         pattern = pattern.toLowerCase();
         if (Utils.wildcardMatch(pattern, lc))
            return true;

         if (pattern.startsWith("*") || pattern.startsWith("."))
            if (Utils.wildcardMatch(pattern, "." + lc))
               return true;
      }
      return false;

   }

}
