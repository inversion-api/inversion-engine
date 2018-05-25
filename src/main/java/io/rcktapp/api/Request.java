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
package io.rcktapp.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import io.forty11.j.J;
import io.forty11.js.JS;
import io.forty11.js.JSObject;

public class Request
{
   String              apiUrl           = null;
   Api                 api              = null;

   User                user             = null;

   String              referrer         = null;

   HttpServletRequest  request          = null;

   String              method           = null;

   String              url              = null;
   String              path             = null;
   String              query            = null;

   String              collectionKey    = null;
   String              entityKey        = null;
   String              subCollectionKey = null;

   String              body             = null;
   JSObject            json             = null;

   boolean             browse           = false;

   Map<String, String> params           = new HashMap();

   public Request(HttpServletRequest req, String method, String inUrl, Api api, String apiUrl)
   {
      this.request = req;
      this.api = api;
      this.apiUrl = apiUrl;

      this.method = method;//req.getMethod();

      url = inUrl;

      if (url.indexOf('?') > 0)
      {
         query = url.substring(url.indexOf('?') + 1, url.length());
         url = url.substring(0, url.indexOf('?'));
      }
      else if (req != null)
      {
         query = req.getQueryString();
      }

      if (apiUrl != null)
      {
         path = url.substring(apiUrl.length(), url.length());
         while (path.startsWith("/"))
            path = path.substring(1, path.length());

         String[] parts = path.split("/");

         if (!path.endsWith("/"))
            path = path + "/";

         if (parts.length > 0)
            collectionKey = parts[0];

         if (parts.length > 1)
            entityKey = parts[1];

         if (parts.length > 2)
            subCollectionKey = parts[2];
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
      else if (query != null)
      {
         String q = query;
         if (q.startsWith("?"))
            q = q.substring(1, q.length());

         String[] pairs = q.split("&");
         for (String pair : pairs)
         {
            int idx = pair.indexOf("=");
            params.put(URLDecoder.decode(pair.substring(0, idx)), URLDecoder.decode(pair.substring(idx + 1)));
         }
      }
   }

   //--

   //   public void replaceParams(String query)
   //   {
   //      params
   //   }
   //   
   //   public void appendParams(String query)
   //   {
   //      
   //   }

   Map parse(String query)
   {
      try
      {
         Map params = new HashMap();
         String[] pairs = query.split("&");
         for (String pair : pairs)
         {
            int idx = pair.indexOf("=");
            if (idx > 0)
            {
               String key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
               String value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
               params.put(key, value);
            }
         }

         return params;
      }
      catch (Exception ex)
      {
         throw new ApiException(SC.SC_400_BAD_REQUEST, "Unable to parse query string \"" + query + "\" - " + ex.getMessage());
      }

   }
   //   public void withQuery(String query)
   //   {
   //      withQuery(query, true);
   //   }
   //
   //   public void withQuery(String query, boolean overwrite)
   //   {
   //      try
   //      {
   //         if (this.query == null)
   //            this.query = "";
   //
   //         if (overwrite)
   //         {
   //            query = this.query + "&" + query;
   //         }
   //         else
   //         {
   //            query = query + "&" + this.query;
   //         }
   //
   //         setQuery(query);
   //      }
   //      catch (Exception ex)
   //      {
   //         J.rethrow(ex);
   //      }
   //   }

   public static String readBody(HttpServletRequest request) throws ApiException
   {
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

   public JSObject getJson() throws ApiException
   {
      if (json != null)
         return json;

      String body = getBody();
      if (J.empty(body))
         return null;

      json = JS.toJSObject(body);

      return json;
   }

   public void putParam(String name, String value)
   {
      params.put(name, value);
   }

   public Map getParams()
   {
      return new HashMap(params);
   }

   public void removeParam(String param)
   {
      params.remove(param);
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
      if(request == null)
         return null;
      
      return request.getHeader(header);
   }

   public String getParam(String key)
   {
      String val = params.get(key);
      if (val == null)
      {
         for (String pk : params.keySet())
         {
            if (pk.equalsIgnoreCase(key))
               return params.get(pk);
         }
      }
      return val;
   }

   public Api getApi()
   {
      return api;
   }

   public String getUrl()
   {
      return url;
   }

   public String getPath()
   {
      return path;
   }

   public String getQuery()
   {
      return query;
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

   /**
    * @return the subCollectionKey
    */
   public String getSubCollectionKey()
   {
      return subCollectionKey;
   }

   public void setCollectionKey(String collectionKey)
   {
      this.collectionKey = collectionKey;
   }

   public void setEntityKey(String entityKey)
   {
      this.entityKey = entityKey;
   }

   public void setSubCollectionKey(String subCollectionKey)
   {
      this.subCollectionKey = subCollectionKey;
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

   public void setBody(String body)
   {
      this.body = body;
   }

   public void requireAll(String... params) throws ApiException
   {
      for (String param : params)
      {
         if (!this.params.containsKey(param))
         {
            throw new ApiException(SC.SC_400_BAD_REQUEST, "Missing a required parameter '" + param + "'");
         }
      }
   }

   public void requireOne(String... params) throws ApiException
   {
      boolean found = false;
      for (String param : params)
      {
         if (this.params.containsKey(param))
         {
            found = true;
            break;
         }
      }
      if (!found)
      {
         throw new ApiException(SC.SC_400_BAD_REQUEST, "One of " + params + " is required parameter.");
      }
   }

}
