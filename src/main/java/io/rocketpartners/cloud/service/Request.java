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
package io.rocketpartners.cloud.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.ApiException;
import io.rocketpartners.cloud.model.Collection;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.model.Url;
import io.rocketpartners.cloud.model.User;
import io.rocketpartners.cloud.rql.Term;
import io.rocketpartners.cloud.service.Service.ApiMatch;
import io.rocketpartners.cloud.utils.JSArray;
import io.rocketpartners.cloud.utils.JSObject;
import io.rocketpartners.cloud.utils.Utils;

public class Request
{
   Service                                service                = null;

   Url                                    url                    = null;

   String                                 apiUrl                 = null;
   Api                                    api                    = null;
   Endpoint                               endpoint               = null;

   String                                 apiCode                = null;
   String                                 tenantCode             = null;

   User                                   user                   = null;

   String                                 referrer               = null;

   String                                 method                 = null;

   String                                 path                   = null;

   String                                 remoteAddr             = null;

   /**
    * The path minus any Endpoint.path prefix
    */
   String                                 subpath                = null;

   String                                 collectionKey          = null;
   String                                 entityKey              = null;
   String                                 subCollectionKey       = null;

   ArrayListValuedHashMap<String, String> headers                = new ArrayListValuedHashMap();
   JSObject                               params                 = new JSObject();

   String                                 body                   = null;
   JSObject                               json                   = null;

   boolean                                browse                 = false;

   boolean                                explain                = false;

   public Uploader                        uploader               = null;

   int                                    retryAttempts;
   static final int                       DEFAULT_RETRY_ATTEMPTS = 5;

   public Request(String method, String url)
   {
      this(method, url, null, null);
   }

   public Request(String method, String url, String body, List<String> headers)
   {
      this(method, url, body, headers, DEFAULT_RETRY_ATTEMPTS);
   }

   public Request(String method, String url, String body, List<String> headers, int retryAttempts)
   {

   }

   public int getRetryAttempts()
   {
      return retryAttempts;
   }

   public void setRetryAttempts(int retryAttempts)
   {
      this.retryAttempts = retryAttempts;
   }

   public Request(ApiMatch match)
   {
      withUrl(match.reqUrl);
      withApi(match);
   }

   public Request(String method, String url, String body)
   {
      withMethod(method);
      withUrl(url);
      withBody(body);
   }

   public Request(Service service, String method, String url, Object body)
   {
      withService(service);
      withMethod(method);
      withUrl(url);
      withBody(body.toString());
   }

   public Request(Url url, String method, Map headers, Map params, String body)
   {
      withMethod(method);
      withUrl(url);
      withParams(params);
      withHeaders(headers);
      withBody(body);
   }

   public Response go()
   {
      String url = this.url.toString();
      if (url.indexOf("?") < 0)
         url += "?";
      else if (!url.endsWith("&"))
         url += "&";

      List<String> keys = new ArrayList(params.keySet());
      for (int i = 0; i < keys.size(); i++)
      {
         String key = keys.get(i);
         String value = params.getString(key);

         url += key;

         if (!Utils.empty(value))
            url += "=" + value;

         if (i < keys.size() - 1)
            url += "&";
      }

      if (service != null)
      {
         return service.service(method, url, body);
      }
      else
      {
         //Web.rest()
         return null;
      }
   }

   public Service getService()
   {
      return service;
   }

   public Request withService(Service service)
   {
      this.service = service;
      return this;
   }

   public Collection getCollection()
   {
      if (api != null && collectionKey != null)
         return api.getCollection(collectionKey);

      return null;
   }

   public Request withUrl(String url)
   {
      this.url = new Url(url);
      return this;
   }

   public Request withUrl(Url url)
   {
      this.url = url;
      String query = url.getQuery();
      if (!Utils.empty(query))
      {
         this.params.putAll(Url.parseQuery(query));
      }
      return this;
   }

   public Request withMethod(String method)
   {
      this.method = method;
      return this;
   }

   public Request withHeaders(String key, String value)
   {
      this.headers.put(key, value);
      return this;
   }

   public Request withHeaders(Map<String, String> headers)
   {
      this.headers.putAll(headers);
      return this;
   }

   public Request withParams(Map<String, String> params)
   {
      //      this.params.putAll(params);
      //
      //      boolean explain = this.params.containsKey("explain") && !((String) this.params.remove("explain")).equalsIgnoreCase("false");
      //      this.explain = this.explain || explain;
      //      return this;

      for (String key : params.keySet())
      {
         withParam(key, params.get(key));
      }
      return this;
   }

   public Request withParam(String key, String value)
   {
      this.params.put(key, value);
      return this;
   }

   public Request withApi(ApiMatch match)
   {
      this.api = match.api;
      this.endpoint = match.endpoint;
      this.method = match.method;
      this.url = match.reqUrl;
      this.apiUrl = match.apiUrl;
      this.path = match.apiPath;
      this.subpath = match.apiPath;

      this.apiCode = api.getApiCode();

      if (this.path.indexOf("?") > 0)
         this.path = this.path.substring(0, this.path.indexOf("?"));

      if (this.subpath.indexOf("?") > 0)
         this.subpath = this.subpath.substring(0, this.subpath.indexOf("?"));

      if (apiUrl != null)
      {
         if (api.isMultiTenant())
         {
            List<String> p = Utils.explode("/", apiUrl);
            tenantCode = p.get(p.size() - 1);
         }

         String urlStr = url.toString();
         if (urlStr.indexOf("?") > 0)
            urlStr = urlStr.substring(0, urlStr.indexOf("?"));

         //         path = urlStr.substring(apiUrl.length(), urlStr.length());
         //         while (path.startsWith("/"))
         //            path = path.substring(1, path.length());

         List<String> parts = Utils.explode("/", path);//path.split("/");

         //         if (!path.endsWith("/"))
         //            path = path + "/";

         if (endpoint != null)
         {
            String endpointPath = endpoint.getPath();
            if (endpointPath != null)
            {
               List<String> epPath = Utils.explode("/", endpointPath);
               for (int i = 0; i < epPath.size(); i++)
               {
                  if (parts.get(0).equalsIgnoreCase(epPath.get(i)))
                  {
                     parts.remove(0);
                  }
                  else
                  {
                     throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "The endpoint.path is not match the start of apiPath but it should have");
                  }
               }
            }

            for (String wildcard : endpoint.getIncludePaths())
            {
               if (Endpoint.pathMatches(wildcard, path))
               {
                  List<String> matchParts = Utils.explode("/", wildcard);
                  List<String> pathParts = Utils.explode("/", path);
                  for (int i = 0; i < matchParts.size(); i++)
                  {
                     if (i >= pathParts.size())
                        break;

                     String matchPart = matchParts.get(i);

                     while (matchPart.startsWith("[") && matchPart.endsWith("]"))
                     {
                        matchPart = matchPart.substring(1, matchPart.length() - 1);
                     }

                     if (matchPart.startsWith("{") && matchPart.endsWith("}"))
                     {
                        int end = matchPart.indexOf(":");
                        end = end > 0 ? end : matchPart.lastIndexOf("}");
                        if (end < 0)
                           end = matchPart.length() - 1;

                        String key = matchPart.substring(1, end).trim();
                        if (key.length() > 0)
                        {
                           String value = pathParts.get(i);
                           params.put(key, value);
                        }
                     }
                  }
               }
            }

            subpath = Utils.implode("/", parts) + "/";

            int idx = 0;
            if (parts.size() > idx)
               collectionKey = parts.get(idx++);

            if (collectionKey == null)
               throw new ApiException(SC.SC_400_BAD_REQUEST, "Your request is missing a collection key");

            if (parts.size() > idx)
               entityKey = parts.get(idx++);

            if (parts.size() > idx)
               subCollectionKey = parts.get(idx++);
         }
      }
      return this;
   }

   public boolean isDebug()
   {
      if (getUrl().toString().indexOf("://localhost") > 0)
         return true;

      if (getApi() != null)
         return getApi().isDebug();

      return false;
   }

   public boolean isExplain()
   {
      return isDebug() && explain;
   }

   public String getBody()
   {
      return body;
   }

   public Request withBody(String body)
   {
      this.body = body;
      return this;
   }

   public JSObject getJson() throws ApiException
   {
      if (json != null)
         return json;

      String body = getBody();
      if (Utils.empty(body))
         return null;

      try
      {
         json = Utils.parseJsonObject(body);
      }
      catch (Exception ex)
      {
         throw new ApiException(SC.SC_400_BAD_REQUEST, "Unparsable JSON body");
      }
      prune(json);

      return json;
   }

   public void putParam(String name, String value)
   {
      params.put(name, value);
   }

   public Map<String, String> getParams()
   {
      return (Map<String, String>) params.asMap();
   }

   public String removeParam(String param)
   {
      return (String) params.remove(param);
   }

   public void clearParams()
   {
      params.clear();
   }

   /**
    * @return the method
    */
   public String getMethod()
   {
      return method;
   }

   public boolean isMethod(String... methods)
   {
      for (String method : methods)
      {
         if (this.method.equalsIgnoreCase(method))
            return true;
      }
      return false;
   }

   public boolean isPut()
   {
      return "put".equalsIgnoreCase(method);
   }

   public boolean isPost()
   {
      return "post".equalsIgnoreCase(method);
   }

   public boolean isGet()
   {
      return "get".equalsIgnoreCase(method);
   }

   public boolean isDelete()
   {
      return "delete".equalsIgnoreCase(method);
   }

   public String getReferrer()
   {
      return getHeader("referrer");
   }

   public String getHeader(String key)
   {
      List<String> vals = headers.get(key);
      if (vals != null && vals.size() > 0)
         return vals.get(0);
      return null;
   }

   /**
    * @return the headers
    */
   public ArrayListValuedHashMap<String, String> getHeaders()
   {
      return headers;
   }

   public void withHeader(String key, String value)
   {
      if (!headers.containsMapping(key, value))
         headers.put(key, value);
   }

   public String getParam(String key)
   {
      String val = (String) params.get(key);
      return val;
   }

   public Api getApi()
   {
      return api;
   }

   public Url getUrl()
   {
      return url;
   }

   public String getPath()
   {
      return path;
   }

   public String getQuery()
   {
      return url.getQuery();
   }

   /**
    * @return the collectionKey
    */
   public String getCollectionKey()
   {
      return collectionKey;
   }

   /**
    * @return the entityKey
    */
   public String getEntityKey()
   {
      return entityKey;
   }

   public Request withCollectionKey(String collectionKey)
   {
      this.collectionKey = collectionKey;
      return this;
   }

   public Request withEntityKey(String entityKey)
   {
      this.entityKey = entityKey;
      return this;
   }

   public String getApiUrl()
   {
      return apiUrl;
   }

   public Request withApiUrl(String apiUrl)
   {
      this.apiUrl = apiUrl;
      return this;
   }

   public User getUser()
   {
      return user;
   }

   public Request withUser(User user)
   {
      this.user = user;
      return this;
   }

   public String getApiCode()
   {
      return apiCode;
   }

   public Request withApiCode(String apiCode)
   {
      this.apiCode = apiCode;
      return this;
   }

   public String getTenantCode()
   {
      return tenantCode;
   }

   public Request withTenantCode(String tenantCode)
   {
      this.tenantCode = tenantCode;
      return this;
   }

   public String getSubCollectionKey()
   {
      return subCollectionKey;
   }

   public Request withSubCollectionKey(String subCollectionKey)
   {
      this.subCollectionKey = subCollectionKey;
      return this;
   }

   public String getSubpath()
   {
      return subpath;
   }

   public Request withSubpath(String subpath)
   {
      this.subpath = subpath;
      return this;
   }

   public String getRemoteAddr()
   {
      String remoteAddr = getHeader("X-Forwarded-For");
      if (remoteAddr == null || remoteAddr.length() == 0 || "unknown".equalsIgnoreCase(remoteAddr))
      {
         remoteAddr = getHeader("Proxy-Client-IP");
      }
      if (remoteAddr == null || remoteAddr.length() == 0 || "unknown".equalsIgnoreCase(remoteAddr))
      {
         remoteAddr = getHeader("WL-Proxy-Client-IP");
      }
      if (remoteAddr == null || remoteAddr.length() == 0 || "unknown".equalsIgnoreCase(remoteAddr))
      {
         remoteAddr = getHeader("HTTP_CLIENT_IP");
      }
      if (remoteAddr == null || remoteAddr.length() == 0 || "unknown".equalsIgnoreCase(remoteAddr))
      {
         remoteAddr = getHeader("HTTP_X_FORWARDED_FOR");
      }
      if (remoteAddr == null || remoteAddr.length() == 0 || "unknown".equalsIgnoreCase(remoteAddr))
      {
         remoteAddr = this.remoteAddr;
      }

      return remoteAddr;
   }

   public Request withRemoteAddr(String remoteAddr)
   {
      this.remoteAddr = remoteAddr;
      return this;
   }

   public Uploader getUploader()
   {
      return uploader;
   }

   public Request withUploader(Uploader uploader)
   {
      this.uploader = uploader;
      return this;
   }

   public static interface Uploader
   {
      public List<Upload> getUploads();
   }

   public static class Upload
   {
      String      fileName    = null;
      long        fileSize    = 0;
      String      requestPath = null;
      InputStream inputStream = null;

      public Upload(String fileName, long fileSize, String requestPath, InputStream inputStream)
      {
         super();
         this.fileName = fileName;
         this.fileSize = fileSize;
         this.requestPath = requestPath;
         this.inputStream = inputStream;
      }

      public String getFileName()
      {
         return fileName;
      }

      public void setFileName(String fileName)
      {
         this.fileName = fileName;
      }

      public long getFileSize()
      {
         return fileSize;
      }

      public void setFileSize(long fileSize)
      {
         this.fileSize = fileSize;
      }

      public String getRequestPath()
      {
         return requestPath;
      }

      public void setRequestPath(String requestPath)
      {
         this.requestPath = requestPath;
      }

      public InputStream getInputStream()
      {
         return inputStream;
      }

      public void setInputStream(InputStream inputStream)
      {
         this.inputStream = inputStream;
      }

   }

   public List<Upload> getUploads()
   {
      return uploader.getUploads();
   }

   /**
    * Removes all empty objects from the tree
    */
   boolean prune(Object parent)
   {
      if (parent instanceof JSArray)
      {
         JSArray arr = ((JSArray) parent);
         for (int i = 0; i < arr.length(); i++)
         {
            if (prune(arr.get(i)))
            {
               arr.remove(i);
               i--;
            }
         }
         return arr.length() == 0;
      }
      else if (parent instanceof JSObject)
      {
         boolean prune = true;
         JSObject js = (JSObject) parent;
         for (String key : js.keySet())
         {
            Object child = js.get(key);
            prune &= prune(child);
         }

         if (prune)
         {
            for (String key : js.keySet())
            {
               js.remove(key);
            }
         }

         return prune;
      }
      else
      {
         return parent == null;
      }
   }

   public Request withTerm(String token, Object... terms)
   {
      withParam(Term.term(null, token, terms).toString(), null);
      return this;
   }

   //   public RequestBuilder pop()
   //   {
   //      return getParent();
   //   }
   //   public RequestBuilder if()
   //   {
   //      Term or = term(this, "if");
   //      return or;
   //   }
   //
   //   public RequestBuilder and()
   //   {
   //      Term or = term(this, "and");
   //      return or;
   //   }
   //
   //   public RequestBuilder or()
   //   {
   //      Term or = term(this, "or");
   //      return or;
   //   }

   public Request val(String value)
   {
      return withTerm(value);
   }

   public Request offset(int offset)
   {
      return withTerm("offset", offset);
   }

   public Request limit(int limit)
   {
      return withTerm("limit", limit);
   }

   public Request page(int page)
   {
      return withTerm("page", page);
   }

   public Request pageNum(int pageNum)
   {
      return withTerm("pageNum", pageNum);
   }

   public Request pageSize(int pageSize)
   {
      return withTerm("pageSize", pageSize);
   }

   public Request order(String... order)
   {
      return withTerm("order", (Object[]) order);
   }

   public Request eq(String field, String value)
   {
      return withTerm("eq", field, value);
   }

   public Request ne(String field, String value)
   {
      return withTerm("ne", field, value);
   }

   public Request nn(String field)
   {
      return withTerm("nn", field);
   }

   public Request n(String field)
   {
      return withTerm("n", field);
   }

   public Request like(String field, String value)
   {
      return withTerm("like", field, value);
   }

   public Request w(String field, String value)
   {
      return withTerm("w", field, value);
   }

   public Request sw(String field, String value)
   {
      return withTerm("sw", field, value);
   }

   public Request lt(String field, String value)
   {
      return withTerm("lt", field, value);
   }

   public Request le(String field, String value)
   {
      return withTerm("le", field, value);
   }

   public Request gt(String field, String value)
   {
      return withTerm("gt", field, value);
   }

   public Request ge(String field, String value)
   {
      return withTerm("ge", field, value);
   }

   public Request in(String field, String... values)
   {
      return withTerm("in", field, values);
   }

   public Request out(String field, String... values)
   {
      return withTerm("out", field, values);
   }

   public Request w(String field, String... values)
   {
      return withTerm("w", field, values);
   }

   public Request wo(String field, String... values)
   {
      return withTerm("wo", field, values);
   }

   public Request emp(String field, String... values)
   {
      return withTerm("emp", field, values);
   }

   public Request nemp(String field, String... values)
   {
      return withTerm("nemp", field, values);
   }
}
