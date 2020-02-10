/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
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
package io.inversion.cloud.model;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import io.inversion.cloud.rql.Term;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.service.Engine;
import io.inversion.cloud.utils.HttpUtils;
import io.inversion.cloud.utils.Utils;

public class Request
{
   Chain                                  chain                  = null;

   String                                 referrer               = null;
   String                                 remoteAddr             = null;
   ArrayListValuedHashMap<String, String> headers                = new ArrayListValuedHashMap();

   Url                                    url                    = null;
   String                                 method                 = null;

   Engine                                 engine                 = null;
   Api                                    api                    = null;
   Path                                   apiPath                = null;
   String                                 apiCode                = null;
   String                                 tenantCode             = null;

   Path                                   endpointPath           = null;
   Endpoint                               endpoint               = null;

   Collection                             collection             = null;
   String                                 collectionKey          = null;
   String                                 entityKey              = null;
   String                                 subCollectionKey       = null;

   String                                 body                   = null;
   JSNode                                 json                   = null;

   boolean                                browse                 = false;

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

   public Request(Engine engine, String method, String url, Object body)
   {
      withEngine(engine);
      withMethod(method);
      withUrl(url);
      if (body != null)
         withBody(body.toString());
   }

   public Request(String method, String url, Map<String, String> headers, Map<String, String> params, String body)
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
         this.url.withParams(params);
      }
   }

   public Request(String method, String url, String body, ArrayListValuedHashMap<String, String> headers, int retryAttempts)
   {
      this(method, url, body, null, headers, retryAttempts);
   }

   public Request(String method, String url, String body, Map<String, String> params, ArrayListValuedHashMap<String, String> headers, int retryAttempts)
   {
      withMethod(method);
      withUrl(url);
      withBody(body);

      if (params != null)
      {
         this.url.withParams(params);
      }

      if (headers != null && headers.size() > 0)
         this.headers = new ArrayListValuedHashMap(headers);

      if (retryAttempts > 0)
         this.retryAttempts = retryAttempts;
   }

   public Response go()
   {
      if (engine != null)
      {
         return engine.service(this, new Response()).getResponse();
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

   public Engine getEngine()
   {
      return engine;
   }

   public Request withEngine(Engine service)
   {
      this.engine = service;
      return this;
   }

   public Request withUrl(String url)
   {
      this.url = new Url(url);
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

   public Collection getCollection()
   {
      return collection;
   }

   /**
    * @return true if any of the <code>collectionKeys</code> case insensitive match <code>collectoinKey</code>
    */
   public boolean hasCollectionKey(String... collectionKeys)
   {
      if (collectionKey != null)
      {
         for (int i = 0; collectionKeys != null && i < collectionKeys.length; i++)
         {
            String key = collectionKeys[i];
            if (key != null && key.equalsIgnoreCase(collectionKey))
            {
               return true;
            }
         }
      }

      return false;
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
      String str = getParam("explain");
      boolean explain = isDebug() && !Utils.empty(str) && !"false".equalsIgnoreCase(str.trim());
      return explain;
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

   public JSNode getJson() throws ApiException
   {
      if (json != null)
         return json;

      String body = getBody();
      if (Utils.empty(body))
         return null;

      try
      {
         json = JSNode.parseJsonNode(body);
      }
      catch (Exception ex)
      {
         throw new ApiException(Status.SC_400_BAD_REQUEST, "Unparsable JSON body");
      }

      return json;
   }

   /**
    * Attempts to massage an inbound json body into an array
    * according to:
    * 1. if getBody() is a JSArray return it.
    * 1. if getBody() is a JSNode with a "data" array prop, return it
    * 1. if getBody() is a JSNode wrap it in an array and return it.
    * 1. if getBody() is not a JSNode and getBody() is null, return
    *     an empty array.
    *
    *
    * @return
    */
   public JSArray getData()
   {
      JSNode node = getJson();
      if (node != null)
      {
         if (node instanceof JSArray)
         {
            return (JSArray) node;
         }
         else if (node.get("data") instanceof JSArray)
         {
            return node.getArray("data");
         }
         else
         {
            return new JSArray(node);
         }
      }
      else if (getBody() == null)
         return new JSArray();
      return null;
   }

   //todo we should probably remove the ability for end users to modify the json?
   public Request withJson(JSNode json)
   {
      this.json = json;
      return this;
   }

   public String getParam(String name)
   {
      return url.getParam(name);
   }

   public Request withParam(String name, String value)
   {
      url.withParam(name, value);
      return this;
   }

   public Map<String, String> getParams()
   {
      Map<String, String> params = url.getParams();
      return params;
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

   public boolean isPatch()
   {
      return "patch".equalsIgnoreCase(method);
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

   public void removeHeader(String key)
   {
      headers.remove(key);
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

   /**
    * Returns the URL path with the apiPath subtracted from the beginning
    */
   public Path getPath()
   {
      Path path = url.getPath();

      int startIdx = apiPath == null ? 0 : apiPath.size();
      path = path.subpath(startIdx, path.size());

      return path;
   }

   public Path getSubpath()
   {
      Path subpath = getPath();
      Path ep = this.endpointPath;

      int startIdx = ep == null ? 0 : ep.size();

      subpath = subpath.subpath(startIdx, subpath.size());

      return subpath;
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

   public String getApiUrl()
   {
      String apiUrl = url.getProtocol() + "://" + url.getHost() + (url.getPort() > 0 ? ":" + url.getPort() : "") + "/" + apiPath;
      return apiUrl;
   }

   public Path getApiPath()
   {
      return apiPath;
   }

   public Request withApiPath(Path apiPath)
   {
      this.apiPath = apiPath;
      return this;
   }

   public Path getEndpointPath()
   {
      return endpointPath;
   }

   public Request withEndpointPath(Path endpointPath)
   {
      this.endpointPath = endpointPath;
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

   public Validation validate(String propOrJsonPath)
   {
      return validate(propOrJsonPath, null);
   }

   public Validation validate(String propOrJsonPath, String customErrorMessage)
   {
      return new Validation(this, propOrJsonPath, customErrorMessage);
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
