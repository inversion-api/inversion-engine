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
package io.rcktapp.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.map.CaseInsensitiveMap;

import io.forty11.j.J;
import io.forty11.web.Url;
import io.forty11.web.js.JS;
import io.forty11.web.js.JSArray;
import io.forty11.web.js.JSObject;

public class Request
{
   Url                url              = null;
   HttpServletRequest request          = null;

   String             apiUrl           = null;
   Api                api              = null;

   String             accountCode      = null;
   String             apiCode          = null;
   String             tenantCode       = null;

   User               user             = null;

   String             referrer         = null;

   String             method           = null;

   String             path             = null;

   String             collectionKey    = null;
   String             entityKey        = null;
   String             subCollectionKey = null;

   String             body             = null;
   JSObject           json             = null;

   boolean            browse           = false;

   boolean            explain          = false;

   CaseInsensitiveMap params           = new CaseInsensitiveMap();

   public Request(HttpServletRequest req, String method, Url url, Api api, String apiUrl)
   {
      this.request = req;
      this.api = api;
      this.apiUrl = apiUrl;
      this.accountCode = api.getAccountCode();
      this.apiCode = api.getApiCode();

      this.method = method;

      this.url = url;

      if (apiUrl != null)
      {
         if (api.isMultiTenant())
         {
            List<String> p = J.explode("/", apiUrl);
            tenantCode = p.get(p.size() - 1);
         }

         String urlStr = url.toString();
         if (urlStr.indexOf("?") > 0)
            urlStr = urlStr.substring(0, urlStr.indexOf("?"));

         path = urlStr.substring(apiUrl.length(), urlStr.length());
         while (path.startsWith("/"))
            path = path.substring(1, path.length());

         String[] parts = path.split("/");

         if (!path.endsWith("/"))
            path = path + "/";

         int idx = 0;
         if (parts.length > idx)
            collectionKey = parts[idx++];

         if (collectionKey == null)
            throw new ApiException(SC.SC_400_BAD_REQUEST, "Your request is missing a collection key");

         if (parts.length > idx)
            entityKey = parts[idx++];

         if (parts.length > idx)
            subCollectionKey = parts[idx++];
      }

      //-- copy params
      if (req != null)
      {
         Enumeration<String> enumer = req.getParameterNames();
         while (enumer.hasMoreElements())
         {
            String key = enumer.nextElement();
            String val = req.getParameter(key);
            params.put(key, val);
         }
      }
      else
      {
         String query = url.getQuery();
         if (!J.empty(query))
         {
            params.putAll(Url.parseQuery(query));
         }
      }

      if (params.containsKey("explain"))
      {
         explain = !(params.get("explain") + "").trim().equalsIgnoreCase("false");
         params.remove("explain");
      }

      //--

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
      return explain;
   }

   public static String readBody(HttpServletRequest request) throws ApiException
   {
      if (request == null)
         return null;

      StringBuilder stringBuilder = new StringBuilder();
      BufferedReader bufferedReader = null;

      try
      {
         InputStream inputStream = request.getInputStream();
         if (inputStream != null)
         {
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            char[] charBuffer = new char[128];
            int bytesRead = -1;
            while ((bytesRead = bufferedReader.read(charBuffer)) > 0)
            {
               stringBuilder.append(charBuffer, 0, bytesRead);
            }
         }
         else
         {
            stringBuilder.append("");
         }
      }
      catch (Exception ex)
      {
         throw new ApiException(SC.SC_400_BAD_REQUEST, "Unable to read request body", ex);
      }
      finally
      {
         if (bufferedReader != null)
         {
            try
            {
               bufferedReader.close();
            }
            catch (IOException ex)
            {
               //throw ex;
            }
         }
      }

      return stringBuilder.toString();
   }

   public String getBody()
   {
      if (body != null)
         return body;

      body = readBody(request);
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
      if (J.empty(body))
         return null;

      json = JS.toJSObject(body);
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
      return new CaseInsensitiveMap(params);
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
    * @return the request
    */
   public HttpServletRequest getHttpServletRequest()
   {
      return request;
   }

   /**
    * @return the method
    */
   public String getMethod()
   {
      return method;
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
      if (request == null)
         return null;

      return request.getHeader(header);
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

   public String getAccountCode()
   {
      return accountCode;
   }

   public void setAccountCode(String accountCode)
   {
      this.accountCode = accountCode;
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

}
