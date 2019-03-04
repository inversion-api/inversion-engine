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
package io.rocketpartners.cloud.model;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import io.rocketpartners.cloud.rql.Term;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.HttpUtils;
import io.rocketpartners.cloud.utils.Utils;

public class Request
{
   Chain                                  chain                  = null;

   String                                 referrer               = null;
   String                                 remoteAddr             = null;
   ArrayListValuedHashMap<String, String> headers                = new ArrayListValuedHashMap();

   Url                                    url                    = null;
   String                                 method                 = null;
   String                                 path                   = null;

   Service                                service                = null;
   Api                                    api                    = null;
   String                                 apiPath                = null;
   String                                 apiCode                = null;
   String                                 tenantCode             = null;

   String                                 endpointPath           = null;
   Endpoint                               endpoint               = null;

   User                                   user                   = null;

   /**
    * The path minus any Endpoint.path prefix
    */
   String                                 subpath                = null;

   Collection                             collection             = null;
   String                                 collectionKey          = null;
   String                                 entityKey              = null;
   String                                 subCollectionKey       = null;

   //JSObject                               params                 = new JSObject();

   String                                 body                   = null;
   ObjectNode                             json                   = null;

   boolean                                browse                 = false;

   boolean                                explain                = false;

   public Uploader                        uploader               = null;

   static final int                       DEFAULT_RETRY_ATTEMPTS = 1;
   int                                    retryAttempts          = DEFAULT_RETRY_ATTEMPTS;

   public Request(String method, String url)
   {
      this(method, url, null, null, -1);
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
      if (body != null)
         withBody(body.toString());
   }

   public Request(String url, String method, Map<String, String> headers, Map<String, String> params, String body)
   {
      withMethod(method);
      withUrl(url);
      withBody(body);

      if (headers != null)
      {
         for (String key : headers.keySet())
            withHeader(key, headers.get(key));
      }

      if (params != null)
      {
         for (String key : params.keySet())
            this.url.withParam(key, params.get(key));
      }
   }

   public Request(String method, String url, String body, ArrayListValuedHashMap<String, String> headers, int retryAttempts)
   {
      withMethod(method);
      withUrl(url);
      withBody(body);
      this.headers = headers;
      if (retryAttempts > 0)
         this.retryAttempts = retryAttempts;
   }

   public Response go()
   {
      if (service != null)
      {
         return service.service(this, new Response()).getResponse();
      }
      else
      {
         return HttpUtils.rest(getMethod(), getUrl().toString(), getBody(), headers, -1).get();
      }

      //      String url = this.url.toString();
      //      if (url.indexOf("?") < 0)
      //         url += "?";
      //      else if (!url.endsWith("&"))
      //         url += "&";
      //
      //      List<String> keys = new ArrayList(params.keySet());
      //      for (int i = 0; i < keys.size(); i++)
      //      {
      //         String key = keys.get(i);
      //         String value = params.getString(key);
      //
      //         url += key;
      //
      //         if (!Utils.empty(value))
      //            url += "=" + value;
      //
      //         if (i < keys.size() - 1)
      //            url += "&";
      //      }
      //
      //      if (service != null)
      //      {
      //         return service.service(method, url, body);
      //      }
      //      else
      //      {
      //         //Web.rest()
      //         return null;
      //      }
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

   public Request withUrl(String url)
   {
      this.url = new Url(url);
      return this;
   }
   //
   //   public Request withUrl(Url url)
   //   {
   //      this.url = url;
   //      String query = url.getQuery();
   //      if (!Utils.empty(query))
   //      {
   //         this.params.putAll(Utils.parseQueryString(query));
   //      }
   //      return this;
   //   }

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

   //   public Request withParams(Map<String, String> params)
   //   {
   //      //      this.params.putAll(params);
   //      //
   //      //      boolean explain = this.params.containsKey("explain") && !((String) this.params.remove("explain")).equalsIgnoreCase("false");
   //      //      this.explain = this.explain || explain;
   //      //      return this;
   //
   //      
   //      
   //      for (String key : params.keySet())
   //      {
   //         withParam(key, params.get(key));
   //      }
   //      return this;
   //   }
   //
   //   public Request withParam(String key, String value)
   //   {
   //      this.params.put(key, value);
   //      return this;
   //   }

   public Collection getCollection()
   {
      return collection;
   }

   public Request withCollection(Collection collection)
   {
      this.collection = collection;
      return this;
   }

   public Endpoint getEndpoint()
   {
      return endpoint;
   }

   public Request withEndpoint(Endpoint endpoint)
   {
      this.endpoint = endpoint;
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

   public ObjectNode getJson() throws ApiException
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

   public String getParam(String name)
   {
      return url.getParam(name);
   }

   public void putParam(String name, String value)
   {
      url.withParam(name, value);
   }

   public Map<String, String> getParams()
   {
      return url.getParams();
   }

   public String removeParam(String param)
   {
      return url.removeParam(param);
   }

   public void clearParams()
   {
      url.clearParams();
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

   public Api getApi()
   {
      return api;
   }

   public Chain getChain()
   {
      return chain;
   }

   public Request withChain(Chain chain)
   {
      this.chain = chain;
      return this;
   }

   public Url getUrl()
   {
      return url;
   }

   public String getPath()
   {
      return path;
   }

   public Request withPath(String path)
   {
      this.path = path;
      return this;
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

   public Request withApi(Api api)
   {
      this.api = api;
      return this;
   }

   public String getApiPath()
   {
      return apiPath;
   }

   public String getApiUrl()
   {
      String apiUrl = url.getProtocol() + "://" + url.getHost() + (url.getPort() > 0 ? ":" + url.getPort() : "") + "/" + apiPath;
      return apiUrl;
   }

   public Request withApiPath(String apiUrl)
   {
      this.apiPath = apiUrl;
      return this;
   }

   public String getEndpointPath()
   {
      return endpointPath;
   }

   public Request withEndpointPath(String endpointPath)
   {
      this.endpointPath = endpointPath;
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
      return api.getApiCode();
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

   public int getRetryAttempts()
   {
      return retryAttempts;
   }

   public void setRetryAttempts(int retryAttempts)
   {
      this.retryAttempts = retryAttempts;
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
      if (parent instanceof ArrayNode)
      {
         ArrayNode arr = ((ArrayNode) parent);
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
      else if (parent instanceof ObjectNode)
      {
         boolean prune = true;
         ObjectNode js = (ObjectNode) parent;
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
      url.withParam(Term.term(null, token, terms).toString(), null);
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
