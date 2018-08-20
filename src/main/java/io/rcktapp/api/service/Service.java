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

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.atteo.evo.inflector.English;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import io.forty11.j.J;
import io.forty11.j.utils.DoubleKeyMap;
import io.forty11.web.Url;
import io.forty11.web.js.JSArray;
import io.forty11.web.js.JSObject;
import io.rcktapp.api.Action;
import io.rcktapp.api.Api;
import io.rcktapp.api.ApiException;
import io.rcktapp.api.Chain;
import io.rcktapp.api.Db;
import io.rcktapp.api.Endpoint;
import io.rcktapp.api.Handler;
import io.rcktapp.api.Request;
import io.rcktapp.api.Response;
import io.rcktapp.api.SC;

public class Service extends HttpServlet
{
   Hashtable<Long, Api>           apis           = new Hashtable();

   Map<String, Handler>           globalHandlers = new Hashtable();
   DoubleKeyMap                   apiHandlers    = new DoubleKeyMap();

   List<String>                   corsHeaders    = new ArrayList();

   boolean                        debug          = true;

   Logger                         log            = LoggerFactory.getLogger(getClass());
   Logger                         requestLog     = LoggerFactory.getLogger(getClass() + ".requests");

   String                         servletMapping = null;

   int                            MIN_POOL_SIZE  = 3;
   int                            MAX_POOL_SIZE  = 10;

   DataSource                     ds             = null;
   Map<Db, ComboPooledDataSource> pools          = new HashMap();

   String                         driver         = null;
   String                         url            = null;
   String                         user           = null;
   String                         pass           = null;
   int                            poolMin        = MIN_POOL_SIZE;
   int                            poolMax        = MAX_POOL_SIZE;

   static
   {
      //initializes Log4J
      //new Logs();
   }

   public Service()
   {
      corsHeaders.add("origin");
      corsHeaders.add("accept");
      corsHeaders.add("Content-Type");
      corsHeaders.add("x-auth-token");
      corsHeaders.add("authorization");
   }

   @Override
   protected void service(HttpServletRequest httpReq, HttpServletResponse httpResp) throws ServletException, IOException
   {
      String method = httpReq.getMethod();

      if (method.equalsIgnoreCase("options"))
      {
         handlePreflightRequest(httpReq, httpResp);
         return;
      }

      if (httpReq.getRequestURI().indexOf("/favicon.ico") >= 0)
      {
         httpResp.setStatus(HttpServletResponse.SC_NOT_FOUND);
         return;
      }

      Api api = null;
      Response res = null;
      Request req = null;

      try
      {
         res = new Response(httpResp);

         //--
         //-- CORS header setup
         //--
         res.addHeader("Access-Control-Allow-Credentials", "true");
         //res.addHeader("Access-Control-Allow-Origin", req.getHeader("origin"));
         res.addHeader("Access-Control-Allow-Origin", "*");
         res.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE");

         Set<String> headers = new HashSet(this.corsHeaders);

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

         String urlstr = httpReq.getRequestURL().toString();

         if (!urlstr.endsWith("/"))
            urlstr = urlstr + "/";

         String query = httpReq.getQueryString();
         if (!J.empty(query))
         {
            urlstr += "?" + query;
         }

         Url url = new Url(urlstr);

         String xfp = httpReq.getHeader("X-Forwarded-Proto");
         String xfh = httpReq.getHeader("X-Forwarded-Host");
         if (xfp != null || xfh != null)
         {
            if (xfp != null)
               url.setProtocol(xfp);

            if (xfh != null)
               url.setHost(xfh);
         }

         //if (isDebug(req))
         {
            res.debug("");
            res.debug("");
            res.debug(">> request --------------");
            res.debug(method + ": " + url);
            Enumeration<String> e = httpReq.getHeaderNames();
            while (e.hasMoreElements())
            {
               String name = e.nextElement();
               String value = httpReq.getHeader(name);
               res.debug(name + " - " + value);
            }

            //res.debug(req.getJson());
         }

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

         req = new Request(httpReq, method, url, api, apiUrl);

         if (J.empty(req.getCollectionKey()))
         {
            throw new ApiException(SC.SC_400_BAD_REQUEST, "It looks like your collectionKey is empty.  You need at least one more part to your url request path.");
         }

         Endpoint endpoint = findEndpoint(api, req.getMethod(), req.getPath());

         if (endpoint == null)
         {
            //check to see if a non plural version of the collection endpoint 
            //was passed in, if it was redirect to the plural version
            if (redirectPlural(req, res))
               return;
         }

         if (endpoint == null)
         {
            String buff = "";
            for (Endpoint e : api.getEndpoints())
               buff += e.getMethods() + ": includePaths:" + e.getIncludePaths() + ": excludePaths" + e.getExcludePaths() + ",  ";

            throw new ApiException(SC.SC_400_BAD_REQUEST, "No endpoint found matching \"" + req.getMethod() + ": " + url + "\" Valid end points include: " + buff);
         }

         doService(this, null, match, endpoint, req, res);
         ConnectionLocal.commit();
      }
      catch (Throwable ex)
      {
         try
         {
            ConnectionLocal.rollback();
         }
         catch (Throwable t)
         {
            log.warn("Error rollowing back transaction", t);
         }

         String status = SC.SC_500_INTERNAL_SERVER_ERROR;

         if (ex instanceof ApiException)
         {
            if (req != null && req.isDebug())
            {
               log.error("Error in Service", ex);
            }

            status = ((ApiException) ex).getStatus();
            if (SC.SC_404_NOT_FOUND.equals(status))
            {
               //an endpoint could have match the url "such as GET * but then not 
               //known what to do with the URL because the collection was not pluralized
               if (redirectPlural(req, res))
                  return;
            }
         }
         else
         {
            log.error("Error in Service", ex);
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
            ConnectionLocal.close();
         }
         catch (Throwable t)
         {
            log.warn("Error closing connections", t);
         }

         try
         {
            writeResponse(req, res);
         }
         catch (Throwable ex)
         {
            log.error("Error in Service", ex);
         }
      }
   }

   /**
    * This method is designed to be called by handlers who want to "go back through the front door"
    * for additional functionality.
    */
   public Response include(Chain parent, String method, String url, String body) throws Exception
   {
      Api api = null;
      String apiUrl = null;
      ApiMatch match = findApi(new Url(url));
      if (match != null)
      {
         api = match.api;
         apiUrl = match.url;
      }

      if (match == null)
      {
         throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "No api found matching " + method + " " + url);
      }

      Request req = new Request(null, method, new Url(url), api, apiUrl);
      req.setUser(parent.getRequest().getUser());
      req.setBody(body);

      Response res = new Response(null);
      Endpoint endpoint = findEndpoint(api, req.getMethod(), req.getPath());

      if (endpoint == null)
      {
         throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "No endpoint found matching " + method + " " + url);
      }

      try
      {
         doService(this, parent, match, endpoint, req, res);
      }
      catch (Throwable ex)
      {
         String status = SC.SC_500_INTERNAL_SERVER_ERROR;

         if (ex instanceof ApiException)
         {
            log.error("Error in Service", ex);
            status = ((ApiException) ex).getStatus();
         }
         else
         {
            log.error("Error in Service", ex);
         }

         res.setStatus(status);
         JSObject response = new JSObject("message", ex.getMessage());
         if (SC.SC_500_INTERNAL_SERVER_ERROR.equals(status))
            response.put("error", J.getShortCause(ex));

         res.setJson(response);
      }
      finally
      {
         parent.getResponse().addChanges(res.getChanges());
      }

      return res;
   }

   protected void doService(Service service, Chain parent, ApiMatch match, Endpoint endpoint, Request req, Response res) throws Exception
   {
      //this will get all actions specifically configured on the endpoint
      List<Action> actions = endpoint.getActions(req);

      //this matches for actions that can run across multiple endpoints.
      //this might be something like an authorization or logging action
      //that acts like a filter
      for (Action a : match.api.getActions())
      {
         if (a.matches(req.getMethod(), req.getPath()))
            actions.add(a);
      }
      Collections.sort(actions);

      //TODO: filter all request params for security -- "restrict" && "require"

      Chain chain = new Chain(this, match.api, endpoint, actions, req, res);
      chain.setParent(parent);
      chain.go();
   }

   private void handlePreflightRequest(HttpServletRequest httpReq, HttpServletResponse httpResp)
   {
      String allowedHeaders = "authorization,accept-language,origin,host,access-control-request-headers,connection,access-control-request-method,x-auth-token,accept-encoding,accept,Content-Type,user-agent";
      String corsRequestHeader = httpReq.getHeader("Access-Control-Request-Header");
      if (corsRequestHeader != null)
      {
         List<String> headers = Arrays.asList(corsRequestHeader.split(","));
         for (String h : headers)
         {
            h = h.trim();
            allowedHeaders = allowedHeaders.concat(h).concat(",");
         }
      }
      httpResp.addHeader("Access-Control-Allow-Origin", "*");
      httpResp.addHeader("Access-Control-Allow-Credentials", "true");
      httpResp.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE");
      httpResp.addHeader("Access-Control-Allow-Headers", allowedHeaders);
      httpResp.setStatus(200);
      return;
   }

   boolean redirectPlural(Request req, Response res) throws IOException
   {
      String collection = req.getCollectionKey();
      if (!J.empty(collection))
      {
         String plural = English.plural(collection);
         if (!plural.equals(collection))
         {
            String path = req.getPath();
            path = path.replaceFirst(collection, plural);
            Endpoint rightEndpoint = findEndpoint(req.getApi(), req.getMethod(), path);
            if (rightEndpoint != null)
            {
               String redirect = req.getHttpServletRequest().getRequestURI();
               //redirect = req.getHttpServletRequest().getRequest
               redirect = redirect.replaceFirst("\\/" + collection, "\\/" + plural);

               String queryString = req.getHttpServletRequest().getQueryString();
               if (!J.empty(queryString))
               {
                  redirect += "?" + queryString;
               }

               res.getHttpResp().sendRedirect(redirect);
               return true;
            }
         }
      }
      return false;
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

   public ApiMatch findApi(Url url) throws Exception
   {
      String accountCode = null;

      String path = url.getPath() + "";

      String host = url.getHost();
      if (host.indexOf(".") != host.lastIndexOf("."))//if this is a three part host name hostKey.domain.com
      {
         accountCode = host.substring(0, host.indexOf("."));
      }

      for (Api a : apis.values())
      {
         String fullPath = "/" + a.getAccountCode() + "/" + a.getApiCode() + "/";
         String halfPath = "/" + a.getApiCode() + "/";

         if (!J.empty(servletMapping))
         {
            fullPath = "/" + servletMapping + fullPath;
            halfPath = "/" + servletMapping + halfPath;
         }

         if ((accountCode == null && path.startsWith(fullPath)) || //  form: https://host.com/[${servletPath}]/${accountCode}/${apiCode}/
               (accountCode != null && accountCode.equals(a.getAccountCode()) && path.startsWith(fullPath)) || //form: https://host.com/[${servletPath}]/${accountCode}/${apiCode}/
               (accountCode != null && accountCode.equals(a.getAccountCode()) && path.startsWith(halfPath)) || //https://${accountCode}.host.com/[${servletPath}]/${apiCode}/
               (a.getAccountCode().equalsIgnoreCase(a.getApiCode()) && path.startsWith(halfPath))) //http/host.com/[${servletPath}]/${accountCode} ONLY when apiCode and accountCode are the same thing
         {

            if (path.startsWith(fullPath))
            {
               path = fullPath;
            }
            else
            {
               path = halfPath;
            }

            String apiUrl = url.toString();
            int idx = apiUrl.indexOf(path);
            apiUrl = apiUrl.substring(0, idx + path.length());

            if (a.isMultiTenant())
            {
               String u = url.toString();
               int start = apiUrl.length();
               int end = u.indexOf('/', start + 1);
               if (end < 0)
                  end = u.length();
               String tenantId = u.substring(start, end);

               if (!apiUrl.endsWith("/"))
                  apiUrl += "/";
               apiUrl += tenantId;
            }

            if (!apiUrl.endsWith("/"))
               apiUrl += "/";

            return new ApiMatch(a, apiUrl);
         }
      }

      return null;

   }

   Endpoint findEndpoint(Api api, String method, String path)
   {
      for (Endpoint endpoint : api.getEndpoints())
      {
         if (endpoint.matches(method, path))
            return endpoint;
      }
      return null;
   }

   void writeResponse(Request req, Response res) throws Exception
   {
      boolean debug = req != null && req.isDebug();
      boolean explain = req != null && req.isExplain();

      String method = req != null ? req.getMethod() : null;
      String format = req != null ? req.getParam("format") : null;

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
         else if (res.getText() != null)
         {
            byte[] bytes = res.getText().getBytes();
            http.setContentType("text/text");

            if (debug)
            {
               res.debug(bytes);
            }
            else
            {
               out.write(bytes);
            }
            res.debug(bytes);
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

               out(req, res, out, bytes);

            }
            else
            {
               byte[] bytes = res.getJson().toString().getBytes();

               res.addHeader("Content-Length", bytes.length + "");
               res.debug("Content-Length " + bytes.length + "");
               //http.setContentType("application/json; charset=utf-8");
               //http.setCharacterEncoding("UTF-8");
               http.setContentType("application/json");

               out(req, res, out, bytes);
            }
         }

         res.debug("\r\n-- done -----------------\r\n");

         if (debug)
         {
            requestLog.info(res.getDebug());
         }

         if (explain)
         {
            out.write(res.getDebug().getBytes());
         }

      }
      finally
      {
         out.flush();
         out.close();
      }
   }

   void out(Request req, Response res, OutputStream out, byte[] bytes) throws Exception
   {
      res.debug(bytes);

      if (req == null || !req.isExplain())
         out.write(bytes);
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

      if (req.getApi().getUrl() != null && !url.startsWith(req.getApi().getUrl()))
      {
         String newUrl = req.getApi().getUrl();
         while (newUrl.endsWith("/"))
            newUrl = newUrl.substring(0, newUrl.length() - 1);

         url = newUrl + url.substring(url.indexOf("/", 8));
      }
      else
      {
         String proto = req.getHeader("x-forwarded-proto");
         if (!J.empty(proto))
         {
            url = proto + url.substring(url.indexOf(':'), url.length());
         }
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
         globalHandlers.put(name, handler);
      }
      catch (Exception ex)
      {
         throw new ApiException("Unknown handler \"" + name + "\". " + J.getShortCause(ex));
      }
   }

   public synchronized Api getApi(long id)
   {
      return apis.get(id);
   }

   public synchronized void addApi(Api api)
   {
      Hashtable apisClone = new Hashtable(apis);

      long id = api.getId();
      if (id <= 0)
         id = api.hashCode();

      apisClone.put(id, api);
      apis = apisClone;
   }

   public synchronized void removeApi(Api api)
   {
      Hashtable apisClone = new Hashtable(apis);

      long id = api.getId();
      if (id <= 0)
         id = api.hashCode();

      apisClone.remove(id);
      apis = apisClone;
   }

   public Collection<Api> getApis()
   {
      return apis.values();
   }

   public Api getApi(String name)
   {
      for (Api api : apis.values())
      {
         if (name.equalsIgnoreCase(api.getName()))
            return api;
      }

      return null;
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

   /**
    * Cleans/normalizes a path strings
    */
   public static String path(String path)
   {
      path = J.path(path.replace('\\', '/'));

      if (!path.endsWith("*") && !path.endsWith("/"))
         path += "/";

      if (path.startsWith("/"))
         path = path.substring(1, path.length());

      return path;
   }

   public String getServletMapping()
   {
      return servletMapping;
   }

   public void setServletMapping(String servletMapping)
   {
      this.servletMapping = servletMapping;
   }

   public Connection getConnection() throws Exception
   {
      if (ds != null)
      {
         return ds.getConnection();
      }
      return null;
   }

   public Connection getConnection(Chain chain) throws Exception
   {
      Db db = chain.getService().getDb(chain.getApi(), chain.getRequest().getCollectionKey());
      return chain.getService().getConnection(db);
   }

   public Connection getConnection(Api api) throws ApiException
   {
      return getConnection(api, null);
   }

   public Connection getConnection(Api api, String collectionKey) throws ApiException
   {
      return getConnection(getDb(api, collectionKey));
   }

   public Connection getConnection(Db db) throws ApiException
   {
      try
      {
         Connection conn = ConnectionLocal.getConnection(db);
         if (conn == null)
         {
            ComboPooledDataSource pool = pools.get(db);

            if (pool == null)
            {
               synchronized (this)
               {
                  pool = pools.get(db);

                  if (pool == null)
                  {
                     String driver = db.getDriver();
                     String url = db.getUrl();
                     String user = db.getUser();
                     String password = db.getPass();
                     int minPoolSize = db.getPoolMin();
                     int maxPoolSize = db.getPoolMax();

                     minPoolSize = Math.max(MIN_POOL_SIZE, minPoolSize);
                     maxPoolSize = Math.min(maxPoolSize, MAX_POOL_SIZE);

                     pool = new ComboPooledDataSource();
                     pool.setDriverClass(driver);
                     pool.setJdbcUrl(url);
                     pool.setUser(user);
                     pool.setPassword(password);
                     pool.setMinPoolSize(minPoolSize);
                     pool.setMaxPoolSize(maxPoolSize);

                     pools.put(db, pool);
                  }
               }
            }

            conn = pool.getConnection();
            conn.setAutoCommit(false);

            ConnectionLocal.putConnection(db, conn);
         }

         return conn;
      }
      catch (Exception ex)
      {
         log.error("Unable to get DB connection", ex);
         throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Unable to get DB connection", ex);
      }
   }

   public Db getDb(Api api, String collectionKey) throws ApiException
   {
      Db db = null;

      if (collectionKey != null)
      {
         db = api.findDb(collectionKey);
      }

      if (db == null)
      {
         if (api.getDbs() == null || api.getDbs().size() == 0)
            throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "There are no database connections configured for this API.");
         db = api.getDbs().get(0);
      }

      return db;
   }

   static class ConnectionLocal
   {
      static ThreadLocal<Map<Db, Connection>> connections = new ThreadLocal();

      public static Map<Db, Connection> getConnections()
      {
         return connections.get();
      }

      public static Connection getConnection(Db db)
      {
         Map<Db, Connection> conns = connections.get();
         if (conns == null)
         {
            conns = new HashMap();
            connections.set(conns);
         }

         return conns.get(db);
      }

      public static void putConnection(Db db, Connection connection)
      {
         Map<Db, Connection> conns = connections.get();
         if (conns == null)
         {
            conns = new HashMap();
            connections.set(conns);
         }
         conns.put(db, connection);
      }

      public static void commit() throws Exception
      {
         Exception toThrow = null;
         Map<Db, Connection> conns = connections.get();
         if (conns != null)
         {
            for (Db db : (List<Db>) new ArrayList(conns.keySet()))
            {
               Connection conn = conns.get(db);
               try
               {
                  conn.commit();
               }
               catch (Exception ex)
               {
                  if (toThrow != null)
                     toThrow = ex;
               }
            }
         }

         if (toThrow != null)
            throw toThrow;
      }

      public static void rollback() throws Exception
      {
         Exception toThrow = null;
         Map<Db, Connection> conns = connections.get();
         if (conns != null)
         {
            for (Db db : (List<Db>) new ArrayList(conns.keySet()))
            {
               Connection conn = conns.get(db);
               try
               {
                  conn.rollback();
               }
               catch (Exception ex)
               {
                  if (toThrow != null)
                     toThrow = ex;
               }
            }
         }

         if (toThrow != null)
            throw toThrow;
      }

      public static void close() throws Exception
      {
         Exception toThrow = null;
         Map<Db, Connection> conns = connections.get();
         if (conns != null)
         {
            for (Db db : (List<Db>) new ArrayList(conns.keySet()))
            {
               Connection conn = conns.get(db);
               try
               {
                  conn.close();
               }
               catch (Exception ex)
               {
                  if (toThrow != null)
                     toThrow = ex;
               }
            }
         }

         connections.remove();

         if (toThrow != null)
            throw toThrow;
      }
   }

}
