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
import java.util.List;
import java.util.Map;

import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.ApiException;
import io.rocketpartners.cloud.model.Collection;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.model.User;
import io.rocketpartners.cloud.service.Service.ApiMatch;
import io.rocketpartners.cloud.utils.CaseInsensitiveLookupMap;
import io.rocketpartners.cloud.utils.JS;
import io.rocketpartners.cloud.utils.JSArray;
import io.rocketpartners.cloud.utils.JSObject;
import io.rocketpartners.cloud.utils.Url;
import io.rocketpartners.cloud.utils.Utils;

public class Request
{
   Url                      url              = null;
   //HttpServletRequest       request          = null;

   String                   apiUrl           = null;
   Api                      api              = null;
   Endpoint                 endpoint         = null;

   String                   apiCode          = null;
   String                   tenantCode       = null;

   User                     user             = null;

   String                   referrer         = null;

   String                   method           = null;

   String                   path             = null;

   String                   remoteAddr       = null;

   /**
    * The path minus any Endpoint.path prefix
    */
   String                   subpath          = null;

   String                   collectionKey    = null;
   String                   entityKey        = null;
   String                   subCollectionKey = null;

   CaseInsensitiveLookupMap headers          = new CaseInsensitiveLookupMap();
   CaseInsensitiveLookupMap params           = new CaseInsensitiveLookupMap();
   String                   body             = null;
   JSObject                 json             = null;

   boolean                  browse           = false;

   boolean                  explain          = false;

   public Uploader          uploader         = null;

   public Request(ApiMatch match)
   {
      setUrl(match.reqUrl);
      setApiMatch(match);
   }

   public Request(Url url, String method, Map headers, Map params, String body)
   {
      setMethod(method);
      setUrl(url);
      addParams(params);
      addHeaders(headers);
      setBody(body);
   }

   public Collection getCollection()
   {
      if (api != null && collectionKey != null)
         return api.getCollection(collectionKey);

      return null;
   }

   public void setUrl(Url url)
   {
      this.url = url;
      String query = url.getQuery();
      if (!Utils.empty(query))
      {
         this.params.putAll(Url.parseQuery(query));
      }
   }

   public void setMethod(String method)
   {
      this.method = method;
   }

   public void addHeaders(Map<String, String> headers)
   {
      this.headers.putAll(headers);
   }

   public void addParams(Map params)
   {
      this.params.putAll(params);

      boolean explain = this.params.containsKey("explain") && !((String) this.params.remove("explain")).equalsIgnoreCase("false");
      this.explain = this.explain || explain;
   }

   public void setApiMatch(ApiMatch match)
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

   public void setBody(String body)
   {
      this.body = body;
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
         json = JS.toJSObject(body);
      }
      catch (Exception ex)
      {
         throw new ApiException(SC.SC_400_BAD_REQUEST, "Unparsable JSON body");
      }
      prune(json);

      return json;
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

   public void putParam(String name, String value)
   {
      params.put(name, value);
   }

   public Map<String, String> getParams()
   {
      return new CaseInsensitiveLookupMap(params);
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

   public String getHeader(String header)
   {
      return (String) headers.get(header);
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

   public void setCollectionKey(String collectionKey)
   {
      this.collectionKey = collectionKey;
   }

   public void setEntityKey(String entityKey)
   {
      this.entityKey = entityKey;
   }

   public String getApiUrl()
   {
      return apiUrl;
   }

   public void setApiUrl(String apiUrl)
   {
      this.apiUrl = apiUrl;
   }

   public User getUser()
   {
      return user;
   }

   public void setUser(User user)
   {
      this.user = user;
   }

   public String getApiCode()
   {
      return apiCode;
   }

   public void setApiCode(String apiCode)
   {
      this.apiCode = apiCode;
   }

   public String getTenantCode()
   {
      return tenantCode;
   }

   public void setTenantCode(String tenantCode)
   {
      this.tenantCode = tenantCode;
   }

   public String getSubCollectionKey()
   {
      return subCollectionKey;
   }

   public void setSubCollectionKey(String subCollectionKey)
   {
      this.subCollectionKey = subCollectionKey;
   }

   public String getSubpath()
   {
      return subpath;
   }

   public void setSubpath(String subpath)
   {
      this.subpath = subpath;
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

   public void setRemoteAddr(String remoteAddr)
   {
      this.remoteAddr = remoteAddr;
   }

   public Uploader getUploader()
   {
      return uploader;
   }

   public void setUploader(Uploader uploader)
   {
      this.uploader = uploader;
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

}
