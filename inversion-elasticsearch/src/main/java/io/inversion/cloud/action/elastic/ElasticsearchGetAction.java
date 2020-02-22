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
package io.inversion.cloud.action.elastic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inversion.cloud.model.Action;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.ApiException;
import io.inversion.cloud.model.Collection;
import io.inversion.cloud.model.JSArray;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.model.Status;
import io.inversion.cloud.service.Chain;

/**
 * Accepts RQL parameters and streams back the elastic query result to the http client
 * 
 * @author kfrankic
 *
 */
public class ElasticsearchGetAction extends Action<ElasticsearchGetAction>
{
   Logger  log           = LoggerFactory.getLogger(ElasticsearchGetAction.class);

   // The following properties can be assigned via snooze.properties
   int     maxRows       = 100;
   String  defaultSource = null;
   boolean isOneSrcArray = true;

   /**
    * @see io.inversion.cloud.service.Handler#run(io.inversion.cloud.model.Request, io.inversion.cloud.model.Response)
    */
   @Override
   public void run(Request req, Response res) throws Exception
   {

      Collection collection = findCollectionOrThrow404(req.getApi(), req.getChain(), req);
      ElasticsearchDb db = (ElasticsearchDb) collection.getDb();

      // examples...
      // http://gen2-dev-api.liftck.com:8103/api/lift/us/elastic/ad?w(name,wells)
      // http://gen2-dev-api.liftck.com:8103/api/lift/us/elastic/ad/suggest?suggestField=value

      // The path should include the Elastic index/type otherwise were gonna have a bad time.
      String path = req.getPath().toString();
      String[] paths = path != null ? path.split("/") : new String[]{};
      if (paths.length > 0 && paths[paths.length - 1].equals("suggest"))
      {
         handleAutoSuggestRequest(req, res, paths, req.removeParam("type"), db, collection);
      }
      else
      {
         handleRqlRequest(req, res, paths, req.getApiUrl() + req.getPath(), db, collection);
      }

   }

   /**
    * Converts a request from RQL into an Elastic query that is then sent to ElasticSearch.
    * The Elastic response is converted into the snooze format.
    * @param req
    * @param res
    * @param paths
    * @throws Exception
    */
   private void handleRqlRequest(Request req, Response res, String[] paths, String apiUrl, ElasticsearchDb db, Collection table) throws Exception
   {
      if (req.getParam("source") == null && defaultSource != null)
      {
         req.withParam("source", defaultSource);
      }

      Collection collection = req.getCollectionKey() != null ? req.getCollection() : null;//getApi().getCollection(req.getCollectionKey(), ElasticsearchDb.class) : null;

      ElasticsearchQuery elasticQ = new ElasticsearchQuery(collection, req.getParams());

      //      Integer wantedPage = null;
      //      if (req.getParam("wantedpage") != null)
      //      {
      //         wantedPage = elasticRQL.toInt("wantedpage", req.removeParam("wantedpage"));
      //         if (wantedPage < elasticRQL.toInt("pagenum", req.getParam("pagenum")))
      //         {
      //            // remove the start param as it wont be used.
      //            req.removeParam("start");
      //            req.removeParam("prevstart");
      //         }
      //      }
      //
      //      ElasticQuery dsl = elasticRQL.toQueryDsl(req.getParams());
      //      dsl.getStmt().setMaxRows(maxRows);
      //
      //      if (wantedPage != null)
      //      {
      //         // wantedPage < MAX query size, get that page.
      //         if (wantedPage * dsl.getStmt().pagesize <= ElasticRql.MAX_NORMAL_ELASTIC_QUERY_SIZE)
      //         {
      //            // to directly load the page, set pagenum = wantedPage
      //            dsl.getStmt().pagenum = wantedPage;
      //         }
      //         else
      //         {
      //            // wantedPage > 10k, start traversal at the last page BEFORE the MAX query index
      //            if (wantedPage < dsl.getStmt().pagenum)
      //            {
      //               int newPageNum = 1;
      //               while (newPageNum * dsl.getStmt().pagesize <= ElasticRql.MAX_NORMAL_ELASTIC_QUERY_SIZE)
      //               {
      //                  newPageNum++;
      //               }
      //
      //               dsl.getStmt().pagenum = newPageNum - 1;
      //            }
      //         }
      //      }

      String json = elasticQ.getJson().toString();

      // uses a http client to deliver the dsl/json as the payload...
      // ... the elastic response is then piped into the Response 'res'
      // and returned to the client 
      // HttpClient httpClient = Web.getHttpClient();

      List<String> headers = new ArrayList<String>();
      String url = buildSearchUrlAndHeaders(table, paths, headers);

      res.debug(url, json, headers);

      Response r = null;//HttpUtils.rest("post", url, json, headers, 0).get(ElasticsearchDb.maxRequestDuration, TimeUnit.SECONDS);

      if (r.isSuccess())
      {

         // TODO how do we want to handle a failed elastic result?

         JSNode jsObj = JSNode.parseJsonNode(r.getContent());

         int totalHits = Integer.parseInt(jsObj.getNode("hits").getProperty("total").getValue().toString());
         JSArray hits = jsObj.getNode("hits").getArray("hits");

         //         boolean isAll = paths[paths.length - 1].toLowerCase().equals("no-type");
         //         boolean isOneSrcArr = (isOneSrcArray && dsl.getSources() != null && dsl.getSources().size() == 1) ? true : false;
         //
         //         ArrayNode data = createDataJsArray(isAll, isOneSrcArr, hits, dsl);
         JSArray data = new JSArray();
         //
         //         // if the query contains a wantedPage and it differs from the pagenum 
         //         // loop until pagenum==wantedPage.  Use the query, and only adjust the 
         //         // 'from' for each request.  While this method maybe more verbose, it 
         //         // will do half the work than re-invoking the this handler as it will 
         //         // not have to query the 'prev' value multiple times.
         //         int pageNum = dsl.getStmt().pagenum;
         //         List<String> sortList = dsl.getOrder() != null ? dsl.getOrder().getOrderAsStringList() : new ArrayList<String>();
         //         while (wantedPage != null && wantedPage != pageNum)
         //         {
         //            // get the last object
         //            ObjectNode lastHit = data.getObject(data.length() - 1);
         //
         //            // get that object's 'sort' values
         //            String startStr = srcObjectFieldsToStringBySortList(lastHit, sortList);
         //
         //            // update 'search after' starting position on the dsl
         //            dsl.setSearchAfter(new ArrayList<String>(Arrays.asList(startStr.split(","))));
         //            json = mapper.writeValueAsString(dsl.toDslMap());
         //
         //            r = Web.post(url, json, headers, 0).get(ElasticDb.maxRequestDuration, TimeUnit.SECONDS);
         //            jsObj = Utils.toObjectNode(r.getContent());
         //            hits = jsObj.getObject("hits").getArray("hits");
         //
         //            data = createDataJsArray(isAll, isOneSrcArr, hits, dsl);
         //
         //            pageNum++;
         //         }
         //
         //         ObjectNode meta = buildMeta(dsl.getStmt().pagesize, pageNum, totalHits, apiUrl, dsl, (data.length() > 0 ? data.get(data.length() - 1) : null), url, headers);

         //ObjectNode wrapper = new ObjectNode("meta", meta, "data", data);
         JSNode wrapper = new JSNode("meta", new JSNode(), "data", data);
         res.withJson(wrapper);

      }
      else
      {
         res.debug("", "Elastic Error Response", r.getErrorContent());

         throw new ApiException(r.hasStatus(db.allowedFailResponseCodes) ? r.getStatus() : Status.SC_500_INTERNAL_SERVER_ERROR);
      }

   }

   /**
    * Converts a request into an Elastic auto-completion search used for obtaining 
    * suggested values.  The request path is expected to end with 'suggest'.  The 
    * path format should look like the following:
    * .../elastic/indexY/suggest/varX=value
    * The default number of suggestions returned is 'maxRows'.  That value can be adjusted
    * via the 'pageSize' parameter.
    * @param req
    * @param res
    * @param paths
    */
   private void handleAutoSuggestRequest(Request req, Response res, String[] paths, String type, ElasticsearchDb db, Collection table) throws Exception
   {

      int size = req.getParam("pagesize") != null ? Integer.parseInt(req.removeParam("pagesize")) : maxRows;

      // remove tenantId before looping over the params to ensure tenantId is not used as the field
      String tenantId = null;
      JSNode context = null;
      if (req.getApi().isMultiTenant())
      {
         tenantId = req.removeParam("tenantId");
         context = new JSNode("tenantid", tenantId); // elastic expects "tenantid" to be all lowercase 
      }

      String field = null;
      String value = null;

      for (Map.Entry<String, String> entry : req.getParams().entrySet())
      {
         field = entry.getKey();
         value = entry.getValue();
      }

      JSNode completion = null;
      JSNode autoSuggest = null;
      JSNode payload = null;

      if (type == null || (type != null && !type.equals("wildcard")))
      {
         completion = new JSNode("field", field, "skip_duplicates", true, "size", size);
         autoSuggest = new JSNode("prefix", value, "completion", completion);
         payload = new JSNode("_source", new JSArray(field), "suggest", new JSNode("auto-suggest", autoSuggest));

      }
      else
      {
         // use regex completion (slightly slower...~20ms vs 2ms).  Regex searches must be done in lowercase.
         completion = new JSNode("field", field, "skip_duplicates", true, "size", size);
         autoSuggest = new JSNode("regex", ".*" + value.toLowerCase() + ".*", "completion", completion);
         payload = new JSNode("_source", new JSArray(field), "suggest", new JSNode("auto-suggest", autoSuggest));
      }

      if (context != null)
      {
         completion.put("context", context);
      }

      List<String> headers = new ArrayList<String>();
      String url = buildSearchUrlAndHeaders(table, paths, headers);

      res.debug(url + "?pretty", payload.toString(), headers);

      Response r = null;//HttpUtils.post(url + "?pretty", payload.toString(), headers, 0).get(ElasticsearchDb.maxRequestDuration, TimeUnit.SECONDS);

      if (r.isSuccess())
      {
         JSNode jsObj = JSNode.parseJsonNode(r.getContent());
         JSNode auto = (JSNode) jsObj.getNode("suggest").getArray("auto-suggest").get(0);
         JSArray resultArray = new JSArray();
         for (JSNode obj : (List<JSNode>) auto.getArray("options").asList())
         {
            if (context != null)
            {
               resultArray.add(obj.getNode("_source").getNode(field).get("input"));
            }
            else
            {
               resultArray.add(obj.getNode("_source").get(field));
            }
         }

         // do a wildcard search of no type was defined.
         if (resultArray.length() == 0 && type == null)
         {
            if (req.getApi().isMultiTenant())
            {
               req.withParam("tenantId", tenantId);
            }
            handleAutoSuggestRequest(req, res, paths, "wildcard", db, table);
         }
         else
         {
            JSNode data = new JSNode("field", field, "results", resultArray);
            JSNode meta = buildMeta(resultArray.length(), 1, resultArray.length(), null, null, null, null, null);
            res.withJson(new JSNode("meta", meta, "data", data));
         }
      }
      else
      {
         throw new ApiException(r.hasStatus(db.allowedFailResponseCodes) ? r.getStatus(): Status.SC_500_INTERNAL_SERVER_ERROR);
      }

   }

   private String buildSearchUrlAndHeaders(Collection table, String[] paths, List<String> headers)
   {
      //      String indexAndType = null;
      //      // paths[0] should be 'elastic' ... otherwise this handled wouldn't be invoked
      //      if (paths.length < 3)
      //      {
      //         // indexAndType = "/" + paths[1] + "/" + paths[1] + "/";
      //         indexAndType = "/" + table.getName() + "/_doc/";
      //      }
      //      // if the type is of 'no-type', dont' include it 
      //      else if (paths[2].toLowerCase().equals("no-type"))
      //      {
      //         indexAndType = "/" + table.getName() + "/";
      //      }
      //      else
      //         indexAndType = "/" + table.getName() + "/" + paths[2] + "/";
      //
      //      headers.add("Content-Type");
      //      headers.add("application/json");
      //
      //      return ElasticsearchDb.getUrl() + indexAndType + "_search";

      return null;
   }

   /**
    * 'meta' should be included with every snooze response. 
    * @param size
    * @param pageNum
    * @param totalHits
    * @return
    */
   private JSNode buildMeta(int size, int pageNum, int totalHits, String apiUrl, ElasticsearchQuery dsl, Object sources, String elasticUrl, List<String> headers)
   {
      JSNode meta = new JSNode();

      //      pageNum = (pageNum == -1) ? 1 : pageNum;
      //      int prevPageNum = pageNum - 1;
      //      int nextPageNum = pageNum + 1;
      //      size = size < 0 ? maxRows : size;
      //
      //      // Converting elastic paging 
      //      meta.put("foundRows", totalHits);
      //
      //      meta.put("pageNum", pageNum);
      //
      //      meta.put("pageSize", size);
      //
      //      int pages = (int) Math.ceil((double) totalHits / (double) size);
      //      meta.put("pageCount", pages);
      //
      //      meta.put("prev", null);
      //      meta.put("next", null);
      //
      //      // Create urls to obtain the next and previous searches. 
      //      // Urls need to include as much detail as possible such as
      //      // pagenum, size, count, sorting
      //      if (apiUrl != null)
      //      {
      //         // remove the trailing '/'
      //         if (apiUrl.endsWith("/"))
      //         {
      //            apiUrl = apiUrl.substring(0, apiUrl.length() - 1);
      //         }
      //
      //         List<String> sortList = dsl.getOrder() != null ? dsl.getOrder().getOrderAsStringList() : new ArrayList<String>();
      //
      //         // add the original query back onto the url
      //         List<String> rqlQuery = new ArrayList<String>();
      //         for (int i = 0; i < dsl.getStmt().where.size(); i++)
      //         {
      //            if (!dsl.getStmt().where.get(i).getSrc().contains("tenantid"))
      //            {
      //               rqlQuery.add(dsl.getStmt().where.get(i).getSrc());
      //            }
      //         }
      //
      //         String url = apiUrl //
      //               + "?" + String.join("&", rqlQuery) // 
      //               + "&pageSize=" + size;
      //
      //         if (sortList.size() > 0)
      //            url = url + "&sort=" + String.join(",", sortList);
      //
      //         if (dsl.getSources() != null)
      //            url = url + "&source=" + String.join(",", dsl.getSources());
      //
      //         // 'prev' & 'next' can easily be determined if pageSize*pageNum < 10k
      //         if ((size * prevPageNum <= ElasticRql.MAX_NORMAL_ELASTIC_QUERY_SIZE) && (prevPageNum > 0))
      //            meta.put("prev", url + "&pageNum=" + prevPageNum);
      //
      //         if ((size * nextPageNum <= ElasticRql.MAX_NORMAL_ELASTIC_QUERY_SIZE) && (pages > pageNum))
      //            meta.put("next", url + "&pageNum=" + nextPageNum);
      //
      //         // if next is still null & the size > 10k
      //         if (meta.get("next") == null && ((size * nextPageNum) > ElasticRql.MAX_NORMAL_ELASTIC_QUERY_SIZE))
      //         {
      //            // the start values for the 'next' search should be pulled from the lastHit object using the sort order to obtain the correct fields
      //            String startString = null;
      //            if (sources != null)
      //               startString = srcObjectFieldsToStringBySortList(sources, sortList);
      //
      //            if (prevPageNum == 1)
      //               // the first page only requires the original rql query because there is no 'search after' that 
      //               // would retrieve the proper values.
      //               meta.put("prev", (pageNum == 1) ? null : url + "&pageNum=" + prevPageNum);
      //
      //            // prevStart was set as a req param, use that value instead of running another query
      //            else if (dsl.getPreviousStart() != null)
      //            {
      //               meta.put("prev", (pageNum == 1) ? null : url + "&pageNum=" + prevPageNum + "&start=" + dsl.getPreviousStart());
      //            }
      //
      //            else
      //            {
      //               String prevStartString = null;
      //
      //               // the start values for the 'previous' search need to be pulled from a separate query.
      //               if ((size * prevPageNum > (ElasticRql.MAX_NORMAL_ELASTIC_QUERY_SIZE - 1)))
      //               {
      //                  try
      //                  {
      //                     dsl.getOrder().reverseOrdering();
      //                     ObjectMapper mapper = new ObjectMapper();
      //                     String json = mapper.writeValueAsString(dsl.toDslMap());
      //                     Web.Response r = Web.post(elasticUrl, json, headers, 0).get(ElasticDb.maxRequestDuration, TimeUnit.SECONDS);
      //
      //                     if (r.isSuccess())
      //                     {
      //                        ObjectNode jsObj = Utils.toObjectNode(r.getContent());
      //                        ArrayNode hits = jsObj.getObject("hits").getArray("hits");
      //                        ObjectNode prevLastHit = hits.getObject(hits.length() - 1);
      //
      //                        prevStartString = srcObjectFieldsToStringBySortList(prevLastHit.getObject("_source"), sortList);
      //
      //                        meta.put("prev", (pageNum == 1) ? null : url + "&pageNum=" + prevPageNum + "&start=" + prevStartString);
      //                     }
      //                  }
      //                  catch (Exception e)
      //                  {
      //                     log.error("error determining the meta 'prev' url ", e);
      //                  }
      //               }
      //            }
      //
      //            if (pages > pageNum)
      //            {
      //               meta.put("next", url + "&pageNum=" + nextPageNum + "&start=" + startString + "&prevStart=" + dsl.getSearchAfterAsString());
      //            }
      //         }
      //
      //      }

      return meta;
   }

   private String srcObjectFieldsToStringBySortList(Object sourceObj, List<String> sortList)
   {

      List<String> list = new ArrayList<String>();

      for (String field : sortList)
      {
         if (sourceObj instanceof JSNode && ((JSNode) sourceObj).get(field) != null)
         {
            list.add(((JSNode) sourceObj).get(field).toString().toLowerCase());
         }
         else if (sourceObj instanceof String)
         {
            list.add((String) sourceObj);
         }
         else
            list.add("[NULL]");
      }

      return String.join(",", list);
   }

   private JSArray createDataJsArray(boolean isAll, boolean isOneSrcArr, JSArray hits, ElasticsearchQuery dsl)
   {
      JSArray data = new JSArray();

      //      for (ObjectNode obj : (List<ObjectNode>) hits.asList())
      //      {
      //         ObjectNode src = obj.getObject("_source");
      //
      //         // for 'all' requests, add the _meta
      //         if (isAll)
      //         {
      //            ObjectNode src_meta = new ObjectNode();
      //            src_meta.put("index", obj.get("_index"));
      //            src_meta.put("type", obj.get("_type"));
      //            src.put("_meta", src_meta);
      //         }
      //
      //         // if there is only one source, convert the return data into an array of values for that source.
      //         if (isOneSrcArr)
      //         {
      //            data.add(src.get(dsl.getSources().get(0)));
      //         }
      //         else
      //            data.add(src);
      //      }

      return data;
   }

   private Collection findCollectionOrThrow404(Api api, Chain chain, Request req) throws Exception
   {
      Collection collection = req.getCollection();//api.getCollection(req.getCollectionKey(), ElasticsearchDb.class);

      if (collection == null)
      {
         throw new ApiException(Status.SC_404_NOT_FOUND, "An elastic table is not configured for this collection key, please edit your query or your config and try again.");
      }

      if (!(collection.getDb() instanceof ElasticsearchDb))
      {
         throw new ApiException(Status.SC_500_INTERNAL_SERVER_ERROR, "Bad server configuration. The endpoint is hitting the elastic handler, but this collection is not related to a elasticdb");
      }

      return collection;

   }

}