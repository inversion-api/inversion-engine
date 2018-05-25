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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.rcktapp.api.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.forty11.web.Web;
import io.forty11.web.js.JS;
import io.forty11.web.js.JSArray;
import io.forty11.web.js.JSObject;
import io.rcktapp.api.Action;
import io.rcktapp.api.Api;
import io.rcktapp.api.Chain;
import io.rcktapp.api.Endpoint;
import io.rcktapp.api.Handler;
import io.rcktapp.api.Request;
import io.rcktapp.api.Response;
import io.rcktapp.api.SC;
import io.rcktapp.rql.RQL;
import io.rcktapp.rql.Stmt;
import io.rcktapp.rql.elasticsearch.QueryDsl;

/**
 * Accepts RQL parameters and streams back the elastic query result to the http client
 * 
 * @author kfrankic
 *
 */
public class ElasticHandler implements Handler
{
   String elasticURL = "";  // assigned via snooze.properties

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
         handleRqlRequest(req, res, paths);
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
   private void handleRqlRequest(Request req, Response res, String[] paths) throws Exception
   {
      // handle RQL request

      QueryDsl dsl = new RQL("elastic").toQueryDsl(req.getParams());

      ObjectMapper mapper = new ObjectMapper();

      String json = mapper.writeValueAsString(dsl.toDslMap());

      // uses a http client to deliver the dsl/json as the payload...
      // ... the elastic response is then piped into the Response 'res'
      // and returned to the client 
      // HttpClient httpClient = Web.getHttpClient();

      List<String> headers = new ArrayList<String>();
      String url = buildSearchUrlAndHeaders(paths, headers);

      res.debug(url, json, headers);

      Web.Response r = Web.post(url, json, headers).get();

      if (r.isSuccess())
      {

         // TODO how do we want to handle a failed elastic result?

         JSObject jsObj = JS.toJSObject(r.getContent());

         int totalHits = Integer.parseInt(jsObj.getObject("hits").getProperty("total").getValue().toString());
         JSArray hits = jsObj.getObject("hits").getArray("hits");

         Stmt stmt = dsl.getStmt();
         JSObject meta = buildMeta(stmt.pagesize, stmt.pagenum, totalHits);
         JSArray data = new JSArray();
         
         boolean isAll = paths[paths.length-1].toLowerCase().equals("no-type");

         for (JSObject obj : (List<JSObject>) hits.asList())
         {
            JSObject src = obj.getObject("_source");

            // for 'all' requests, add the _meta
            if (isAll) {
               JSObject src_meta = new JSObject();
               src_meta.put("index", obj.get("_index"));
               src_meta.put("type", obj.get("_type"));
               src.put("_meta", src_meta);
            }
            
            data.add(src);
         }
         

         JSObject wrapper = new JSObject("meta", meta, "data", data);
         res.setJson(wrapper);

      }
      else
         res.setStatus(SC.SC_500_INTERNAL_SERVER_ERROR);

   }

   /**
    * Converts a request into an Elastic auto-completion search used for obtaining 
    * suggested values.  The request path is expected to end with 'suggest'.  The 
    * path format should look like the following:
    * .../elastic/indexY/suggest/varX=value
    * The default number of suggestions returned is 15.  That value can be adjusted
    * via the 'pageSize' parameter.
    * @param req
    * @param res
    * @param paths
    */
   private void handleAutoSuggestRequest(Request req, Response res, String[] paths, String type)
   {

      int size = req.getParam("pagesize") != null ? Integer.parseInt(req.removeParam("pagesize")) : 15;

      Map.Entry<String, String> prefixEntry = null;

      for (Map.Entry<String, String> entry : req.getParams().entrySet())
      {
         prefixEntry = entry;
      }

      JSObject completion = null;
      JSObject autoSuggest = null;
      JSObject payload = null;

      if (type == null || (type != null && !type.equals("wildcard")))
      {
         // use prefix completion
         completion = new JSObject("field", prefixEntry.getKey(), "skip_duplicates", true, "size", size);
         autoSuggest = new JSObject("prefix", prefixEntry.getValue(), "completion", completion);
         payload = new JSObject("_source", new JSArray(prefixEntry.getKey()), "suggest", new JSObject("auto-suggest", autoSuggest));

      }
      else
      {
         // use regex completion (slightly slower...~20ms vs 2ms).  Regex searches must be done in lowercase.
         completion = new JSObject("field", prefixEntry.getKey(), "skip_duplicates", true, "size", size);
         autoSuggest = new JSObject("regex", ".*" + prefixEntry.getValue().toLowerCase() + ".*", "completion", completion);
         payload = new JSObject("_source", new JSArray(prefixEntry.getKey()), "suggest", new JSObject("auto-suggest", autoSuggest));
      }

      List<String> headers = new ArrayList<String>();
      String url = buildSearchUrlAndHeaders(paths, headers);

      res.debug(url + "?pretty", payload.toString(), headers);

      Web.Response r = Web.post(url + "?pretty", payload.toString(), headers).get();

      if (r.isSuccess())
      {
         JSObject jsObj = JS.toJSObject(r.getContent());
         JSObject auto = (JSObject) jsObj.getObject("suggest").getArray("auto-suggest").get(0);
         JSArray resultArray = new JSArray();
         for (JSObject obj : (List<JSObject>) auto.getArray("options").asList())
         {
            resultArray.add(obj.getObject("_source").get(prefixEntry.getKey()));
         }

         // do a wildcard search of no type was defined.
         if (resultArray.length() == 0 && type == null)
         {
            handleAutoSuggestRequest(req, res, paths, "wildcard");
         }
         else
         {
            JSObject data = new JSObject("field", prefixEntry.getKey(), "results", resultArray);
            JSObject meta = buildMeta(resultArray.length(), 1, resultArray.length());
            res.setJson(new JSObject("meta", meta, "data", data));
         }
      }
      else
         res.setStatus(SC.SC_500_INTERNAL_SERVER_ERROR);
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
      else if (paths[2].toLowerCase().equals("no-type")) {
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
   private JSObject buildMeta(int size, int pageNum, int totalHits)
   {
      JSObject meta = new JSObject();

      int pagesize = size < 0 ? 10 : size;

      // Converting elastic paging 
      meta.put("rowCount", totalHits);

      if (pageNum == -1)
         meta.put("pageNum", 1);
      else
         meta.put("pageNum", pageNum);

      meta.put("pageSize", pagesize);

      int pages = (int) Math.ceil((double) totalHits / (double) pagesize);
      meta.put("pageCount", pages);

      return meta;
   }

}
