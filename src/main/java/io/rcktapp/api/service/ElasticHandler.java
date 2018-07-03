/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
 * http://rocketpartners.io
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
package io.rcktapp.api.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.forty11.web.Web;
import io.forty11.web.js.JS;
import io.forty11.web.js.JSArray;
import io.forty11.web.js.JSObject;
import io.rcktapp.api.Action;
import io.rcktapp.api.Api;
import io.rcktapp.api.ApiException;
import io.rcktapp.api.Chain;
import io.rcktapp.api.Endpoint;
import io.rcktapp.api.Handler;
import io.rcktapp.api.Request;
import io.rcktapp.api.Response;
import io.rcktapp.api.SC;
import io.rcktapp.rql.RQL;
import io.rcktapp.rql.elasticsearch.QueryDsl;

/**
 * Accepts RQL parameters and streams back the elastic query result to the http client
 * 
 * @author kfrankic
 *
 */
public class ElasticHandler implements Handler
{
   Logger                      log           = LoggerFactory.getLogger(ElasticHandler.class);

   // The following properties can be assigned via snooze.properties
   String                      elasticURL    = "";
   int                         maxRows       = 100;
   String                      defaultSource = null;
   boolean                     isOneSrcArray = true;

   static Map<Integer, String> SC_MAP        = new HashMap<>();
   static
   {
      SC_MAP.put(400, SC.SC_400_BAD_REQUEST);
      SC_MAP.put(401, SC.SC_401_UNAUTHORIZED);
      SC_MAP.put(403, SC.SC_403_FORBIDDEN);
      SC_MAP.put(404, SC.SC_404_NOT_FOUND);
   }

   /**
    * @see io.rcktapp.api.Handler#service(io.rcktapp.api.service.Service, io.rcktapp.api.Api, io.rcktapp.api.Endpoint, io.rcktapp.api.Action, io.rcktapp.api.Chain, io.rcktapp.api.Request, io.rcktapp.api.Response)
    */
   @Override
   public void service(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res) throws Exception
   {

      // examples...
      // http://gen2-dev-api.liftck.com:8103/api/lift/us/elastic/ad?w(name,wells)
      // http://gen2-dev-api.liftck.com:8103/api/lift/us/elastic/ad/suggest?suggestField=value

      // The path should include the Elastic index/type otherwise were gonna have a bad time.
      String[] paths = req.getPath().split("/");
      if (paths.length > 0 && paths[paths.length - 1].equals("suggest"))
      {
         handleAutoSuggestRequest(req, res, paths, req.removeParam("type"));
      }
      else
      {
         handleRqlRequest(req, res, paths, req.getApiUrl() + req.getPath());
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
   private void handleRqlRequest(Request req, Response res, String[] paths, String apiUrl) throws Exception
   {
      if (req.getParam("source") == null && defaultSource != null)
      {
         req.putParam("source", defaultSource);
      }

      QueryDsl dsl = new RQL("elastic").toQueryDsl(req.getParams());
      dsl.getStmt().setMaxRows(maxRows);

      ObjectMapper mapper = new ObjectMapper();

      String json = mapper.writeValueAsString(dsl.toDslMap());

      // uses a http client to deliver the dsl/json as the payload...
      // ... the elastic response is then piped into the Response 'res'
      // and returned to the client 
      // HttpClient httpClient = Web.getHttpClient();

      List<String> headers = new ArrayList<String>();
      String url = buildSearchUrlAndHeaders(paths, headers);

      res.debug(url, json, headers);

      Web.Response r = Web.post(url, json, headers, 0).get(10, TimeUnit.SECONDS);

      if (r.isSuccess())
      {

         // TODO how do we want to handle a failed elastic result?

         JSObject jsObj = JS.toJSObject(r.getContent());

         int totalHits = Integer.parseInt(jsObj.getObject("hits").getProperty("total").getValue().toString());
         JSArray hits = jsObj.getObject("hits").getArray("hits");

         JSObject lastSource = null;
         if (hits.length() > 0)
            lastSource = hits.getObject(hits.length() - 1).getObject("_source");

         JSObject meta = buildMeta(dsl.getStmt().pagesize, dsl.getStmt().pagenum, totalHits, apiUrl, dsl, lastSource, url, headers);
         JSArray data = new JSArray();

         boolean isAll = paths[paths.length - 1].toLowerCase().equals("no-type");

         JSArray oneSrcArray = new JSArray();
         boolean isOneSrcArr = (isOneSrcArray && dsl.getSources() != null && dsl.getSources().size() == 1) ? true : false;

         for (JSObject obj : (List<JSObject>) hits.asList())
         {
            JSObject src = obj.getObject("_source");

            // for 'all' requests, add the _meta
            if (isAll)
            {
               JSObject src_meta = new JSObject();
               src_meta.put("index", obj.get("_index"));
               src_meta.put("type", obj.get("_type"));
               src.put("_meta", src_meta);
            }

            // if there is only one source, convert the return data into an array of values for that source.
            if (isOneSrcArr)
            {
               oneSrcArray.add(src.get(dsl.getSources().get(0)));
            }
            else
               data.add(src);
         }

         if (isOneSrcArr)
         {
            // if 'oneSrcArray' has values, 'data' will be empty.
            JSObject oneSrcObj = new JSObject(dsl.getSources().get(0), oneSrcArray);
            data.add(oneSrcObj);
         }

         JSObject wrapper = new JSObject("meta", meta, "data", data);
         res.setJson(wrapper);

      }
      else
      {
         String status = SC_MAP.get(r.getCode());
         throw new ApiException(status != null ? status : SC.SC_500_INTERNAL_SERVER_ERROR);
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
   private void handleAutoSuggestRequest(Request req, Response res, String[] paths, String type) throws Exception
   {

      int size = req.getParam("pagesize") != null ? Integer.parseInt(req.removeParam("pagesize")) : maxRows;

      // remove tenantId before looping over the params to ensure tenantId is not used as the field
      String tenantId = null;
      JSObject context = null;
      if (req.getApi().isMultiTenant())
      {
         tenantId = req.removeParam("tenantId");
         context = new JSObject("tenantid", tenantId); // elastic expects "tenantid" to be all lowercase 
      }

      String field = null;
      String value = null;

      for (Map.Entry<String, String> entry : req.getParams().entrySet())
      {
         field = entry.getKey();
         value = entry.getValue();
      }

      JSObject completion = null;
      JSObject autoSuggest = null;
      JSObject payload = null;

      if (type == null || (type != null && !type.equals("wildcard")))
      {
         completion = new JSObject("field", field, "skip_duplicates", true, "size", size);
         autoSuggest = new JSObject("prefix", value, "completion", completion);
         payload = new JSObject("_source", new JSArray(field), "suggest", new JSObject("auto-suggest", autoSuggest));

      }
      else
      {
         // use regex completion (slightly slower...~20ms vs 2ms).  Regex searches must be done in lowercase.
         completion = new JSObject("field", field, "skip_duplicates", true, "size", size);
         autoSuggest = new JSObject("regex", ".*" + value.toLowerCase() + ".*", "completion", completion);
         payload = new JSObject("_source", new JSArray(field), "suggest", new JSObject("auto-suggest", autoSuggest));
      }

      if (context != null)
      {
         completion.put("context", context);
      }

      List<String> headers = new ArrayList<String>();
      String url = buildSearchUrlAndHeaders(paths, headers);

      res.debug(url + "?pretty", payload.toString(), headers);

      Web.Response r = Web.post(url + "?pretty", payload.toString(), headers, 0).get(10, TimeUnit.SECONDS);

      if (r.isSuccess())
      {
         JSObject jsObj = JS.toJSObject(r.getContent());
         JSObject auto = (JSObject) jsObj.getObject("suggest").getArray("auto-suggest").get(0);
         JSArray resultArray = new JSArray();
         for (JSObject obj : (List<JSObject>) auto.getArray("options").asList())
         {
            if (context != null)
            {
               resultArray.add(obj.getObject("_source").getObject(field).get("input"));
            }
            else
            {
               resultArray.add(obj.getObject("_source").get(field));
            }
         }

         // do a wildcard search of no type was defined.
         if (resultArray.length() == 0 && type == null)
         {
            if (req.getApi().isMultiTenant())
            {
               req.putParam("tenantId", tenantId);
            }
            handleAutoSuggestRequest(req, res, paths, "wildcard");
         }
         else
         {
            JSObject data = new JSObject("field", field, "results", resultArray);
            JSObject meta = buildMeta(resultArray.length(), 1, resultArray.length(), null, null, null, null, null);
            res.setJson(new JSObject("meta", meta, "data", data));
         }
      }
      else
      {
         String status = SC_MAP.get(r.getCode());
         throw new ApiException(status != null ? status : SC.SC_500_INTERNAL_SERVER_ERROR);
      }

   }

   private String buildSearchUrlAndHeaders(String[] paths, List<String> headers)
   {
      String indexAndType = null;
      // paths[0] should be 'elastic' ... otherwise this handled wouldn't be invoked
      if (paths.length < 3)
      {
         indexAndType = "/" + paths[1] + "/" + paths[1] + "/";
      }
      // if the type is of 'no-type', dont' include it 
      else if (paths[2].toLowerCase().equals("no-type"))
      {
         indexAndType = "/" + paths[1] + "/";
      }
      else
         indexAndType = "/" + paths[1] + "/" + paths[2] + "/";

      headers.add("Content-Type");
      headers.add("application/json");

      return elasticURL + indexAndType + "_search";
   }

   /**
    * 'meta' should be included with every snooze response. 
    * @param size
    * @param pageNum
    * @param totalHits
    * @return
    */
   private JSObject buildMeta(int size, int pageNum, int totalHits, String apiUrl, QueryDsl dsl, JSObject jsonSource, String elasticUrl, List<String> headers)
   {
      JSObject meta = new JSObject();

      pageNum = (pageNum == -1) ? 1 : pageNum;
      size = size < 0 ? maxRows : size;

      // Converting elastic paging 
      meta.put("rowCount", totalHits);

      meta.put("pageNum", pageNum);

      meta.put("pageSize", size);

      int pages = (int) Math.ceil((double) totalHits / (double) size);
      meta.put("pageCount", pages);

      meta.put("prev", null);
      meta.put("next", null);

      // Create urls to obtain the next and previous searches. 
      // Urls need to include as much detail as possible such as
      // pagenum, size, count, sorting
      if (apiUrl != null)
      {
         // remove the trailing '/'
         if (apiUrl.endsWith("/"))
         {
            apiUrl = apiUrl.substring(0, apiUrl.length() - 1);
         }

         List<String> sortList = dsl.getOrder().getOrderAsStringList();

         // add the original query back onto the url
         List<String> rqlQuery = new ArrayList<String>();
         for (int i = 0; i < dsl.getStmt().where.size(); i++)
         {
            if (!dsl.getStmt().where.get(i).getSrc().contains("tenantid"))
            {
               rqlQuery.add(dsl.getStmt().where.get(i).getSrc());
            }
         }

         String url = apiUrl //
               + "?" + String.join("&", rqlQuery) // 
               + "&sort=" + String.join(",", sortList) //
               + "&pageSize=" + size;

         // the start values for the 'next' search should be pulled from the lastHit object using the sort order to obtain the correct fields
         String startString = null;
         if (jsonSource != null)
            startString = srcObjectFieldsToStringBySortList(jsonSource, sortList);
         String prevStartString = null;

         // the start values for the 'previous' search need to be pulled from a separate query.
         if (!dsl.isSearchAfterNull())
         {
            try
            {
               dsl.getOrder().reverseOrdering();
               ObjectMapper mapper = new ObjectMapper();
               String json = mapper.writeValueAsString(dsl.toDslMap());
               Web.Response r = Web.post(elasticUrl, json, headers, 0).get(10, TimeUnit.SECONDS);

               if (r.isSuccess())
               {
                  JSObject jsObj = JS.toJSObject(r.getContent());
                  JSArray hits = jsObj.getObject("hits").getArray("hits");
                  JSObject prevLastHit = hits.getObject(hits.length() - 1);

                  prevStartString = srcObjectFieldsToStringBySortList(prevLastHit.getObject("_source"), sortList);

                  if (pageNum - 1 == 1)
                     // the first page only requires the original rql query because there is no 'search after' that 
                     // would retrieve the proper values.
                     meta.put("prev", (pageNum == 1) ? null : url + "&pageNum=" + (pageNum - 1));
                  else
                     meta.put("prev", (pageNum == 1) ? null : url + "&pageNum=" + (pageNum - 1) + "&start=" + prevStartString);
               }
            }
            catch (Exception e)
            {
               // TODO something
               log.error("error! ", e);
            }
         }

         if (pages != pageNum)
            meta.put("next", url + "&pageNum=" + (pageNum + 1) + "&start=" + startString + "&prevStart=" + prevStartString);

      }

      return meta;
   }

   private String srcObjectFieldsToStringBySortList(JSObject sourceObj, List<String> sortList)
   {

      List<String> list = new ArrayList<String>();

      for (String field : sortList)
      {
         if (sourceObj.get(field) != null)
            list.add(sourceObj.get(field).toString().toLowerCase());
         else
            list.add("[NULL]");
      }

      return String.join(",", list);
   }

}
