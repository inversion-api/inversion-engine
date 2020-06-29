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
package io.inversion;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import io.inversion.utils.JSArray;
import io.inversion.utils.JSNode;
import io.inversion.utils.Path;
import io.inversion.utils.Url;
import io.inversion.utils.Utils;

public class Request {

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

   protected int                                    retryMax       = 0;
   int                                              retryCount     = 0;
   File                                             retryFile;

   boolean                                          explain        = false;

   public Request() {

   }

   public Request(String method, String url) {
      this(method, url, null, null, -1);
   }

   public Request(String method, String url, String body) {
      withMethod(method);
      withUrl(url);
      withBody(body);
   }

   public Request(Engine engine, String method, String url, Object body) {
      withEngine(engine);
      withMethod(method);
      withUrl(url);
      if (body != null)
         withBody(body.toString());
   }

   public Request(String method, String url, Map<String, String> headers, Map<String, String> params, String body) {
      withMethod(method);
      withUrl(url);
      withBody(body);

      if (headers != null) {
         for (String key : headers.keySet())
            withHeader(key, headers.get(key));
      }

      if (params != null) {
         this.url.withParams(params);
      }
   }

   public Request(String method, String url, String body, ArrayListValuedHashMap<String, String> headers, int retryAttempts) {
      this(method, url, body, null, headers, retryAttempts);
   }

   public Request(String method, String url, String body, Map<String, String> params, ArrayListValuedHashMap<String, String> headers, int retryMax) {
      withMethod(method);
      withUrl(url);
      withBody(body);

      if (params != null) {
         this.url.withParams(params);
      }

      if (headers != null && headers.size() > 0)
         this.headers = new ArrayListValuedHashMap(headers);

      if (retryMax > -1)
         this.retryMax = retryMax;
   }

   public Engine getEngine() {
      return engine;
   }

   public Request withEngine(Engine service) {
      this.engine = service;
      return this;
   }

   public Request withUrl(String url) {
      Url u = new Url(url);

      String key = u.findKey("explain");
      if (key != null) {
         String explain = u.clearParams(key);
         if (Utils.empty(explain) || "true".equalsIgnoreCase(explain.trim()))
            withExplain(true);

         //-- makes the url.original look like it does not include the explain param;
         u = new Url(u.toString());
      }
      this.url = u;

      return this;
   }

   public Request withMethod(String method) {
      this.method = method;
      return this;
   }

   public Request withHeaders(String key, String value) {
      this.headers.put(key, value);
      return this;
   }

   public Request withHeaders(Map<String, String> headers) {
      this.headers.putAll(headers);
      return this;
   }

   public Collection getCollection() {
      return collection;
   }

   /**
    * @return true if any of the <code>collectionKeys</code> case insensitive match <code>collectoinKey</code>
    */
   public boolean hasCollectionKey(String... collectionKeys) {
      String collectionKey = getCollectionKey();
      if (collectionKey != null) {
         for (int i = 0; collectionKeys != null && i < collectionKeys.length; i++) {
            String key = collectionKeys[i];
            if (key != null && key.equalsIgnoreCase(collectionKey)) {
               return true;
            }
         }
      }

      return false;
   }

   public Request withCollection(Collection collection) {
      this.collection = collection;
      return this;
   }

   public Request withCollection(Collection collection, Path collectionPath) {
      this.collection = collection;
      this.collectionPath = collectionPath;
      return this;
   }

   public Endpoint getEndpoint() {
      return endpoint;
   }

   public Request withEndpoint(Endpoint endpoint, Path endpointPath) {
      this.endpoint = endpoint;
      this.endpointPath = endpointPath;
      return this;
   }

   public boolean isDebug() {
      String url = getUrl().toString();
      if (url.indexOf("://localhost/") > 0)
         return true;

      if (url.indexOf("://127.0.0.1/") > 0)
         return true;

      if (getApi() != null)
         return getApi().isDebug();

      return false;
   }

   public boolean isExplain() {
      return explain;
   }

   public Request withExplain(boolean explain) {
      this.explain = explain;
      return this;
   }

   public String getBody() {
      return body;
   }

   public Request withBody(String body) {
      this.body = body;
      return this;
   }

   public JSNode getJson() throws ApiException {
      if (json != null)
         return json;

      String body = getBody();
      if (Utils.empty(body))
         return null;

      try {
         json = JSNode.parseJsonNode(body);
      } catch (Exception ex) {
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
   public JSArray getData() {
      JSNode node = getJson();
      if (node != null) {
         if (node instanceof JSArray) {
            return (JSArray) node;
         } else if (node.get("data") instanceof JSArray) {
            return node.getArray("data");
         } else {
            return new JSArray(node);
         }
      } else if (getBody() == null)
         return new JSArray();
      return null;
   }

   //todo we should probably remove the ability for end users to modify the json?
   public Request withJson(JSNode json) {
      this.json = json;
      return this;
   }

   /**
    * @return the method
    */
   public String getMethod() {
      return method;
   }

   public boolean isMethod(String... methods) {
      for (String method : methods) {
         if (this.method.equalsIgnoreCase(method))
            return true;
      }
      return false;
   }

   public boolean isPut() {
      return "put".equalsIgnoreCase(method);
   }

   public boolean isPost() {
      return "post".equalsIgnoreCase(method);
   }

   public boolean isPatch() {
      return "patch".equalsIgnoreCase(method);
   }

   public boolean isGet() {
      return "get".equalsIgnoreCase(method);
   }

   public boolean isDelete() {
      return "delete".equalsIgnoreCase(method);
   }

   public String getReferrer() {
      return getHeader("referrer");
   }

   public String getHeader(String key) {
      List<String> vals = headers.get(key);
      if (vals != null && vals.size() > 0)
         return vals.get(0);
      return null;
   }

   public void removeHeader(String key) {
      headers.remove(key);
   }

   /**
    * @return the headers
    */
   public ArrayListValuedHashMap<String, String> getHeaders() {
      return headers;
   }

   public void withHeader(String key, String value) {
      if (!headers.containsMapping(key, value))
         headers.put(key, value);
   }

   public Api getApi() {
      return api;
   }

   public Chain getChain() {
      return chain;
   }

   public Request withChain(Chain chain) {
      this.chain = chain;
      return this;
   }

   public Url getUrl() {
      return url;
   }

   /**
    * Returns the URL path with the apiPath subtracted from the beginning
    */
   public Path getPath() {
      Path path = url.getPath();

      int startIdx = apiPath == null ? 0 : apiPath.size();
      path = path.subpath(startIdx, path.size());

      return path;
   }

   public Path getSubpath() {
      Path subpath = getPath();
      Path ep = this.endpointPath;

      int startIdx = ep == null ? 0 : ep.size();

      subpath = subpath.subpath(startIdx, subpath.size());

      return subpath;
   }

   /**
    * @return the collectionKey
    */
   public String getCollectionKey() {
      return url.getParam("collection");
   }

   /**
    * @return the resourceKey
    */
   public String getResourceKey() {
      return url.getParam("resource");
   }

   public String getRelationshipKey() {
      return url.getParam("relationship");
   }

   public Request withApi(Api api, Path apiPath) {
      this.api = api;
      this.apiPath = apiPath;
      return this;
   }

   public String getApiUrl() {
      String apiUrl = url.getProtocol() + "://" + url.getHost() + (url.getPort() > 0 ? ":" + url.getPort() : "") + "/" + apiPath;
      return apiUrl;
   }

   public Path getApiPath() {
      return apiPath;
   }

   public Path getEndpointPath() {
      return endpointPath;
   }

   public String getRemoteAddr() {
      String remoteAddr = getHeader("X-Forwarded-For");
      if (remoteAddr == null || remoteAddr.length() == 0 || "unknown".equalsIgnoreCase(remoteAddr)) {
         remoteAddr = getHeader("Proxy-Client-IP");
      }
      if (remoteAddr == null || remoteAddr.length() == 0 || "unknown".equalsIgnoreCase(remoteAddr)) {
         remoteAddr = getHeader("WL-Proxy-Client-IP");
      }
      if (remoteAddr == null || remoteAddr.length() == 0 || "unknown".equalsIgnoreCase(remoteAddr)) {
         remoteAddr = getHeader("HTTP_CLIENT_IP");
      }
      if (remoteAddr == null || remoteAddr.length() == 0 || "unknown".equalsIgnoreCase(remoteAddr)) {
         remoteAddr = getHeader("HTTP_X_FORWARDED_FOR");
      }
      if (remoteAddr == null || remoteAddr.length() == 0 || "unknown".equalsIgnoreCase(remoteAddr)) {
         remoteAddr = this.remoteAddr;
      }

      return remoteAddr;
   }

   public Request withRemoteAddr(String remoteAddr) {
      this.remoteAddr = remoteAddr;
      return this;
   }

   public Uploader getUploader() {
      return uploader;
   }

   public Request withUploader(Uploader uploader) {
      this.uploader = uploader;
      return this;
   }

   public Validation validate(String propOrJsonPath) {
      return validate(propOrJsonPath, null);
   }

   public Validation validate(String propOrJsonPath, String customErrorMessage) {
      return new Validation(this, propOrJsonPath, customErrorMessage);
   }

   public boolean isLocalRequest() {
      String url = getUrl().toString();
      return chain != null && !(url.startsWith("http:") || url.startsWith("https://"));
   }

   public int getRetryMax() {
      return retryMax;
   }

   public Request withRetryMax(int retryMax) {
      this.retryMax = retryMax;
      return this;
   }

   public int getRetryCount() {
      return retryCount;
   }

   public void incrementRetryCount() {
      this.retryCount++;
   }

   public File getRetryFile() {
      return retryFile;
   }

   public void setRetryFile(File retryFile) {
      this.retryFile = retryFile;
   }

   public List<Upload> getUploads() {
      return uploader.getUploads();
   }

   /**
    * Implemented by different runtimes, for example a servlet vs a lambda, to enable different file upload mechanisms. 
    */
   public interface Uploader {

      public List<Upload> getUploads();
   }

   public static class Upload {

      String      fileName    = null;
      long        fileSize    = 0;
      String      requestPath = null;
      InputStream inputStream = null;

      public Upload(String fileName, long fileSize, String requestPath, InputStream inputStream) {
         super();
         this.fileName = fileName;
         this.fileSize = fileSize;
         this.requestPath = requestPath;
         this.inputStream = inputStream;
      }

      public String getFileName() {
         return fileName;
      }

      public void setFileName(String fileName) {
         this.fileName = fileName;
      }

      public long getFileSize() {
         return fileSize;
      }

      public void setFileSize(long fileSize) {
         this.fileSize = fileSize;
      }

      public String getRequestPath() {
         return requestPath;
      }

      public void setRequestPath(String requestPath) {
         this.requestPath = requestPath;
      }

      public InputStream getInputStream() {
         return inputStream;
      }

      public void setInputStream(InputStream inputStream) {
         this.inputStream = inputStream;
      }

   }

   /**
    * Utility designed to make it easy to validate request properties or request body
    * json values while you are retrieving them.  
    *
    * <h3>Required (Not Null)</h3>
    *
    * To ensure a field is not null, use the required() method:
    * <ul>
    *     <li>String nameFirst = request.validate("nameFirst", "A first name is required").required().asString();</li>
    * </ul>
    *
    * <h3>Comparison</h3>
    *
    * To validate a number is greater than 5, then return its value:
    * <ul>
    * <li>int myParam = request.validate("myParamName", "optional_custom_error_message").gt(5).asInt();
    * </ul>
    *
    * @see Request#validate(String)
    * @see Request#validate(String,String)
    *
    */
   public static class Validation {

      Object value              = null;
      String customErrorMessage = null;
      String propOrPath         = null;

      public Validation(Request req, String propOrPath, String customErrorMessage) {
         value = req.getUrl().getParam(propOrPath);
         if (value == null && req.getJson() != null)
            value = req.getJson().find(propOrPath);

         this.propOrPath = null;
         this.customErrorMessage = customErrorMessage;
      }

      public Validation(Response res, String jsonPath, String customErrorMessage) {
         this.value = res.find(jsonPath);
         this.propOrPath = null;
         this.customErrorMessage = customErrorMessage;
      }

      /**
       * If there are any <code>childProps</code> they must exist on the JSNode
       * found at <code>pathOrProp</code>.  If <code>childProps</code> are null/empty
       * then  <code>pathOrProp</code> must not be null.
       * 
       * @return
       * @throws ApiException 400 if the referenced validation is null.
       */
      public Validation required(String... childProps) {
         if (Utils.empty(value))
            fail("Required field '" + propOrPath + "' is missing.");

         if (childProps != null && value instanceof JSNode && !((JSNode) value).isArray()) {
            for (String childProp : childProps) {
               if (Utils.empty(((JSNode) value).get(childProp))) {
                  fail("Required field '" + propOrPath + "." + childProp + "' is missing.");
               }
            }
         }

         return this;
      }

      public Validation matches(String regex) {
         if (value == null)
            return this;

         if (!value.toString().matches(regex))
            fail("Field '" + propOrPath + "' does not match the required pattern.");

         return this;
      }

      public Validation in(Object... possibleValues) {
         if (value == null)
            return this;

         if (!Utils.in(value, possibleValues))
            fail("Field '" + propOrPath + "' is not one of the possible values.");

         return this;
      }

      public Validation out(Object... excludedValues) {
         if (value == null)
            return this;

         if (Utils.in(value, excludedValues))
            fail("Field '" + propOrPath + "' has a restricted value.");

         return this;
      }

      protected int compareTo(Object compareTo) {
         Object value = this.value;

         if (compareTo instanceof Number) {
            try {
               value = Double.parseDouble(value.toString());
               compareTo = Double.parseDouble(compareTo.toString());
            } catch (Exception ex) {
               //ignore numeric type conversion error.
            }
         }

         return ((Comparable) value).compareTo(compareTo);
      }

      public Validation gt(Object compareTo) {
         if (value == null)
            return this;

         if (compareTo(compareTo) < 1)
            fail("Field '" + propOrPath + "' is less than the required value.");

         return this;
      }

      public Validation ge(Object compareTo) {
         if (value == null)
            return this;

         if (compareTo(compareTo) < 0)
            fail("Field '" + propOrPath + "' is less than the required value.");

         return this;
      }

      public Validation lt(Object compareTo) {
         if (value == null)
            return this;

         if (compareTo(compareTo) > -1)
            fail("Field '" + propOrPath + "' is greater than the required value.");

         return this;
      }

      public Validation le(Object compareTo) {
         if (value == null)
            return this;

         if (compareTo(compareTo) > 0)
            fail("Field '" + propOrPath + "' is greater than the required value.");

         return this;
      }

      public Validation eq(Object compareTo) {
         if (value == null)
            return this;

         if (compareTo(compareTo) != 0)
            fail("Field '" + propOrPath + "' is not equal to the required value.");

         return this;
      }

      public Validation ne(Object compareTo) {
         if (value == null)
            return this;

         if (compareTo(compareTo) != 0)
            fail("Field '" + propOrPath + "' is equal to a restricted value.");

         return this;
      }

      public Validation length(int max) {
         if (value == null)
            return this;

         if (value.toString().length() > max)
            fail("Field '" + propOrPath + "' is longer than the max allowed length of '" + max + "'.");

         return this;
      }

      public Validation length(int min, int max) {
         if (value == null)
            return this;

         int length = value.toString().length();

         if (length > max)
            fail("Field '" + propOrPath + "' is longer than the maximum allowed length of '" + max + "'.");

         if (length < min)
            fail("Field '" + propOrPath + "' is shorter than the minimum allowed length of '" + max + "'.");

         return this;
      }

      public Validation minMax(Number min, Number max) {
         if (value == null)
            return this;

         max(max);
         min(min);
         return this;
      }

      public Validation max(Number max) {
         if (value == null)
            return this;

         if (Double.parseDouble(max.toString()) < Double.parseDouble(value.toString()))
            fail("Field '" + propOrPath + "' is greater than the required maximum of '" + max + "'.");

         return this;
      }

      public Validation min(Number min) {
         if (value == null)
            return this;

         if (Double.parseDouble(min.toString()) > Double.parseDouble(value.toString()))
            fail("Field '" + propOrPath + "' is less than the required minimum of '" + min + "'.");

         return this;
      }

      public Object value() {
         return value;
      }

      public JSNode asNode() {
         if (value == null)
            return null;

         if (value instanceof String)
            value = JSNode.parseJson(value.toString());

         return ((JSNode) value);
      }

      public JSArray asArray() {
         if (value == null)
            return null;

         if (value instanceof String)
            value = JSNode.parseJsonArray(value.toString());

         return ((JSArray) value);
      }

      public String asString() {
         if (value == null)
            return null;

         return value.toString();
      }

      public int asInt() {
         if (value == null)
            return -1;

         try {
            return Integer.parseInt(value + "");
         } catch (Exception ex) {
            fail("Field '" + propOrPath + "' must be an integer.");
         }

         return -1;
      }

      public double asDouble() {
         if (value == null)
            return -1;

         try {
            return Double.parseDouble(value + "");
         } catch (Exception ex) {
            fail("Field '" + propOrPath + "' must be an number.");
         }

         return -1;
      }

      public boolean asBoolean() {
         try {
            return Boolean.parseBoolean(value + "");
         } catch (Exception ex) {
            fail("Field '" + propOrPath + "' must be a boolean.");
         }

         return false;
      }

      /**
       * Throws an ApiException 400 using the provided custom error message.  If a custom error message
       * was not provided, a default error message is utilized.
       * @param defaultErrorMessage
       * @throws ApiException
       */
      protected void fail(String defaultErrorMessage) {
         String message = customErrorMessage != null ? customErrorMessage : defaultErrorMessage;
         ApiException.throw400BadRequest(message);
      }
   }

}
