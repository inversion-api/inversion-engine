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

import io.inversion.cloud.service.Chain;
import io.inversion.cloud.service.Engine;
import io.inversion.cloud.utils.HttpUtils;
import io.inversion.cloud.utils.Utils;

public class Request
{
   protected Chain                                  chain          = null;

   protected String                                 referrer       = null;
   protected String                                 remoteAddr     = null;
   protected ArrayListValuedHashMap<String, String> headers        = new ArrayListValuedHashMap();

   protected Url                                    url            = null;
   protected String                                 method         = null;

   protected Engine                                 engine         = null;
   protected Api                                    api            = null;
   protected Path                                   apiPath        = null;
   protected String                                 tenant         = null;

   protected Path                                   endpointPath   = null;
   protected Endpoint                               endpoint       = null;

   protected Collection                             collection     = null;
   protected Path                                   collectionPath = null;

   protected String                                 body           = null;
   protected JSNode                                 json           = null;

   protected Uploader                               uploader       = null;

   protected int                                    retryAttempts  = -1;

   public Request()
   {

   }

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
      String collectionKey = getCollectionKey();
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

   public Request withCollection(Collection collection, Path collectionPath)
   {
      this.collection = collection;
      this.collectionPath = collectionPath;
      return this;
   }

   public Endpoint getEndpoint()
   {
      return endpoint;
   }

   public Request withEndpoint(Endpoint endpoint, Path endpointPath)
   {
      this.endpoint = endpoint;
      this.endpointPath = endpointPath;
      return this;
   }

   public boolean isDebug()
   {
      String url = getUrl().toString();
      if (url.indexOf("://localhost/") > 0)
         return true;

      if (url.indexOf("://127.0.0.1/") > 0)
         return true;

      if (getApi() != null)
         return getApi().isDebug();

      return false;
   }

   public boolean isExplain()
   {
      String str = url.getParam("explain");
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
         ApiException.throw400BadRequest("Unparsable JSON body");
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

   /**
    * @return the collectionKey
    */
   public String getCollectionKey()
   {
      return url.getParam("collection");
   }

   /**
    * @return the entityKey
    */
   public String getEntityKey()
   {
      return url.getParam("entity");
   }

   public String getRelationshipKey()
   {
      return url.getParam("relationship");
   }

   public Request withApi(Api api, Path apiPath)
   {
      this.api = api;
      this.apiPath = apiPath;
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

   public Path getEndpointPath()
   {
      return endpointPath;
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

   public List<Upload> getUploads()
   {
      return uploader.getUploads();
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

}
