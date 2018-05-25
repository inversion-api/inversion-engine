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

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import io.forty11.j.J;
import io.forty11.js.JSArray;
import io.forty11.js.JSObject;
import io.forty11.utils.DoubleKeyMap;
import io.forty11.web.Url;
import io.rcktapp.api.Api;
import io.rcktapp.api.ApiException;
import io.rcktapp.api.Chain;
import io.rcktapp.api.Handler;
import io.rcktapp.api.Request;
import io.rcktapp.api.Response;
import io.rcktapp.api.Rule;
import io.rcktapp.api.SC;
import io.rcktapp.api.User;

public class Service extends HttpServlet
{
   LinkedHashMap<String, Api> apis           = new LinkedHashMap();

   Map<String, Handler>       globalHandlers = new Hashtable();
   DoubleKeyMap               apiHandlers    = new DoubleKeyMap();

   List<String>               coorsHeaders   = new ArrayList();

   boolean                    debug          = true;

   public Service()
   {
      coorsHeaders.add("origin");
      coorsHeaders.add("accept");
      coorsHeaders.add("Content-Type");
      coorsHeaders.add("x-auth-token");
      coorsHeaders.add("authorization");
   }

   public Response doService(User user, String method, String url, String body) throws Exception
   {
      Api api = null;

      String apiUrl = null;
      ApiMatch match = findApi(url);
      if (match != null)
      {
         api = match.api;
         apiUrl = match.url;
      }

      if (match == null)
      {
         throw new ApiException(SC.SC_400_BAD_REQUEST, "No API found matching URL: \"" + url + "\"");
      }

      Request req = new Request(null, method.toUpperCase(), url, api, apiUrl);
      req.setBody(body);
      req.setUser(user);
     
      Response res = new Response();

      List<Rule> rules = matchRules(api, req);

      Chain chain = new Chain(this, api, rules, req, res);
      chain.go();

      return res;
   }

   @Override
   protected void service(HttpServletRequest httpReq, HttpServletResponse httpResp) throws ServletException, IOException
   {
      Api api = null;
      Response res = null;
      Request req = null;

      try
      {

         res = new Response(httpResp);

         //--
         //-- COORS header setup
         //--
         res.addHeader("Access-Control-Allow-Credentials", "true");
         //res.addHeader("Access-Control-Allow-Origin", req.getHeader("origin"));
         res.addHeader("Access-Control-Allow-Origin", "*");
         res.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE");

         Set<String> headers = new HashSet(this.coorsHeaders);

         Enumeration<String> eh = httpReq.getHeaderNames();
         while (eh.hasMoreElements())
         {
            String name = eh.nextElement();
            headers.add(name);
         }

         for (String header : headers)
         {
            res.addHeader("Access-Control-Allow-Headers", header);
         }
         //--
         //-- End COORS Header Setup

         String url = httpReq.getRequestURL().toString();

         String xfp = httpReq.getHeader("X-Forwarded-Proto");
         String xfh = httpReq.getHeader("X-Forwarded-Host");
         if (xfp != null || xfh != null)
         {
            Url u = new Url(url);
            if (xfp != null)
               u.setProtocol(xfp);

            if (xfh != null)
               u.setHost(xfh);

            url = u.toString();
         }

         if (!url.endsWith("/"))
            url = url + "/";

         String apiUrl = null;
         ApiMatch match = findApi(url);
         if (match != null)
         {
            api = match.api;
            apiUrl = match.url;
         }

         if (match == null)
         {
            throw new ApiException(SC.SC_400_BAD_REQUEST, "No API found matching URL: \"" + url + "\"");
         }

         req = new Request(httpReq, httpReq.getMethod(), url, api, apiUrl);

         if (isDebug(req))
         {
            res.debug("");
            res.debug("");
            res.debug(">> request --------------");
            res.debug(req.getMethod() + ": " + req.getUrl());
            Enumeration<String> e = httpReq.getHeaderNames();
            while (e.hasMoreElements())
            {
               String name = e.nextElement();
               String value = httpReq.getHeader(name);
               res.debug(name + " - " + value);
            }

            res.debug(req.getJson());
         }

         List<Rule> rules = matchRules(api, req);

         if (rules.size() == 0)
            throw new ApiException(SC.SC_400_BAD_REQUEST, "No rules found matching URL: \"" + url + "\"");

         if (isDebug(req))
         {
            String msg = "";
            for (Rule rule : rules)
            {
               msg += rule.getName() + ", ";
            }
            res.debug("CHAIN: " + msg);
         }

         Chain chain = new Chain(this, api, rules, req, res);
         chain.go();

      }
      catch (Throwable ex)
      {
         String status = SC.SC_500_INTERNAL_SERVER_ERROR;

         if (ex instanceof ApiException)
         {
            status = ((ApiException) ex).getStatus();
         }
         else
         {
            ex.printStackTrace();
         }

         res.setStatus(status);
         JSObject response = new JSObject("message", ex.getMessage());
         if (SC.SC_500_INTERNAL_SERVER_ERROR.equals(status))
            response.put("error", J.getShortCause(ex));

         res.setJson(response);
      }
      finally
      {
         try
         {
            writeResponse(req, res);
         }
         catch (Throwable ex)
         {
            ex.printStackTrace();
         }
      }
   }

   public void service(Service service, Chain chain, Rule rule, Request req, Response res) throws Exception
   {
      throw new ApiException(SC.SC_400_BAD_REQUEST);
   }

   public static class ApiMatch
   {
      public Api    api = null;
      public String url = null;

      public ApiMatch(Api api, String url)
      {
         this.api = api;
         this.url = url;
      }

   }

   public ApiMatch findApi(String url) throws ApiException
   {
      url = url.toLowerCase();
      for (Api a : apis.values())
      {
         for (String apiUrl : a.getUrls())
         {
            apiUrl = apiUrl.toLowerCase();

            if (url.startsWith(apiUrl) || //
                  (apiUrl.startsWith("//") && url.substring(url.indexOf("/"), url.length()).startsWith(apiUrl)))
            {
               if (apiUrl.startsWith("//"))
               {
                  apiUrl = url.substring(0, url.indexOf("/")) + apiUrl;
               }

               return new ApiMatch(a, apiUrl);
            }
         }
      }
      return null;
   }

   public List<Rule> matchRules(Api api, Request req)
   {
      boolean allMeta = true;

      Map<Float, Rule> matches = new HashMap();

      for (Rule rule : api.getRules())
      {
         if (rule.matches(req))
         {
            Rule existing = matches.get(rule.getOrder());
            if (existing == null || existing.getPriority() <= rule.getPriority())
            {
               if (!rule.isMeta())
                  allMeta = false;

               matches.put(rule.getOrder(), rule);
            }
         }
      }

      if (allMeta)//this should turn into  404
         return Collections.EMPTY_LIST;

      List rules = new ArrayList(matches.values());
      Collections.sort(rules);

      return rules;
   }

   void writeResponse(Request request, Response res) throws Exception
   {
      String method = request != null ? request.getMethod() : null;
      String format = request != null ? request.getParam("format") : null;

      HttpServletResponse http = res.getHttpResp();

      http.setStatus(res.getStatusCode());

      res.debug("\r\n<< response -------------\r\n");
      res.debug(res.getStatusCode());

      OutputStream out = http.getOutputStream();
      try
      {
         for (String key : res.getHeaders().keySet())
         {
            List values = res.getHeaders().get(key);
            StringBuffer buff = new StringBuffer();
            for (int i = 0; i < values.size(); i++)
            {
               buff.append(values.get(i));
               if (i < values.size() - 1)
                  buff.append(",");
            }
            http.setHeader(key, buff.toString());
            res.debug(key + " " + buff);
         }
         if ("OPTIONS".equals(method))
         {
            //
         }
         else if (res.getJson() != null)
         {
            if ("csv".equalsIgnoreCase(format))
            {
               JSObject arr = res.getJson();
               if (!(arr instanceof JSArray))
               {
                  arr = new JSArray(arr);
               }

               byte[] bytes = toCsv((JSArray) arr).getBytes();

               res.addHeader("Content-Length", bytes.length + "");
               res.debug("Content-Length " + bytes.length + "");
               //http.setContentType("application/json; charset=utf-8");
               //http.setCharacterEncoding("UTF-8");
               http.setContentType("text/csv");

               out.write(bytes);
               res.debug(bytes);
            }
            else
            {
               byte[] bytes = res.getJson().toString().getBytes();

               res.addHeader("Content-Length", bytes.length + "");
               res.debug("Content-Length " + bytes.length + "");
               //http.setContentType("application/json; charset=utf-8");
               //http.setCharacterEncoding("UTF-8");
               http.setContentType("application/json");

               out.write(bytes);
               res.debug(bytes);
            }
         }
         else if (res.getText() != null)
         {
            byte[] bytes = res.getText().getBytes();
            http.setContentType("text/text");
            out.write(bytes);
            //debug.write(bytes);

         }
      }
      finally
      {
         out.flush();
         out.close();
      }

      res.debug("\r\n-- done -----------------\r\n");

      if (debug)
      {
         System.out.println(res.getDebug());
      }
   }

   public String toCsv(JSArray arr) throws Exception
   {
      StringBuffer buff = new StringBuffer();

      LinkedHashSet<String> keys = new LinkedHashSet();

      for (int i = 0; i < arr.length(); i++)
      {
         JSObject obj = (JSObject) arr.get(i);
         if (obj != null)
         {
            for (String key : obj.keys())
            {
               Object val = obj.get(key);
               if (!(val instanceof JSArray) && !(val instanceof JSObject))
                  keys.add(key);
            }
         }
      }

      CSVPrinter printer = new CSVPrinter(buff, CSVFormat.DEFAULT);

      List<String> keysList = new ArrayList(keys);
      for (String key : keysList)
      {
         printer.print(key);
      }
      printer.println();

      for (int i = 0; i < arr.length(); i++)
      {
         for (String key : keysList)
         {
            Object val = ((JSObject) arr.get(i)).get(key);
            if (val != null)
            {
               printer.print(val);
            }
            else
            {
               printer.print("");
            }
         }
         printer.println();
      }
      printer.flush();
      printer.close();

      return buff.toString();
   }

   public static String buildLink(Request req, String collectionKey, Object entityKey, String subCollectionKey)
   {
      String url = req.getApiUrl();

      if (!J.empty(collectionKey))
      {
         if (!url.endsWith("/"))
            url += "/";

         url += collectionKey;
      }

      if (!J.empty(entityKey))
         url += "/" + entityKey;

      if (!J.empty(subCollectionKey))
         url += "/" + subCollectionKey;

      String proto = req.getHeader("x-forwarded-proto");
      if (!J.empty(proto))
      {
         url = proto + url.substring(url.indexOf(':'), url.length());
      }

      return url;

   }

   public Handler getHandler(Api api, String name)
   {
      try
      {
         String clazz = null;

         //first see if it is cached
         Handler h = (Handler) apiHandlers.get(api.getName(), name);
         if (h == null && api.getHandler(name) != null)
         {
            //ok, it is not cached but it is a registred short name to class name
            clazz = api.getHandler(name);
            h = (Handler) Class.forName(clazz).newInstance();
            apiHandlers.put(api.getName(), name, h);
            return h;
         }

         //so it is not a registered api handler, maybe it is a global handler
         h = globalHandlers.get(name);

         if (h == null && name.indexOf(".") > 0)
         {
            //nope, so maybe it is just a class name
            h = (Handler) Class.forName(name).newInstance();
            apiHandlers.put(api.getName(), name, h);
         }

         return h;
      }
      catch (Exception ex)
      {
         throw new ApiException("Unknown handler \"" + name + "\". " + J.getShortCause(ex));
      }
   }

   /**
    * Adds a global handler useable by all APIs
    * @param name
    * @param clazz
    */
   public void addHandler(String name, Handler handler)
   {
      try
      {
         globalHandlers.put(name, handler);//(Handler) Class.forName(clazz).newInstance());
      }
      catch (Exception ex)
      {
         throw new ApiException("Unknown handler \"" + name + "\". " + J.getShortCause(ex));
      }
   }

   public void addApi(Api api)
   {
      log("addApi(" + api.getName() + ")");
      List urls = api.getUrls();
      if (urls == null || urls.size() == 0)
         log(" - NO URLS FOR API");
      else
         for (Object url : urls)
         {
            log(" - " + url);
         }

      apis.put(api.getName().toLowerCase(), api);
   }

   public Collection<Api> getApis()
   {
      return apis.values();
   }

   public Api getApi(String name)
   {
      return apis.get(name.toLowerCase());
   }

   public boolean isDebug(Request req)
   {
      if (debug || req.getUrl().indexOf("://localhost") > 0)
         return true;

      return false;
   }

   public void setDebug(boolean debug)
   {
      this.debug = debug;
   }

   int first(String str, char... chars)
   {
      int first = -1;
      for (char c : chars)
      {
         int idx = str.indexOf(c);
         if (first < 0)
            first = idx;
         else if (idx < first)
            first = idx;

      }
      return first;
   }

}
