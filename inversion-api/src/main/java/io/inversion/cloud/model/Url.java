/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.inversion.cloud.utils.Utils;

/**
 * Url utilities
 * 
 * @author wells
 */
public class Url
{
   protected String original = null;
   protected String protocol = "http";
   protected String host     = null;
   protected int    port     = 0;
   protected Path   path     = null;
   protected JSNode params   = new JSNode();

   public Url copy()
   {
      Url url = new Url();
      url.original = original;
      url.protocol = protocol;
      url.host = host;
      url.port = port;
      url.path = path;
      url.params.putAll(params);
      return url;
   }

   private Url()
   {

   }

   public Url(String url)
   {
      String path = null;

      if (url.indexOf(":/") > 0 && url.indexOf("://") < 0)
         url = url.replaceAll(":/", "://");

      url = url.replace("&amp;", "&");
      if (!(url.startsWith("http://") || url.startsWith("https://")))
         url = "http://localhost" + (!url.startsWith("/") ? "/" : "") + url;

      original = url;

      try
      {
         int queryIndex = url.indexOf('?');
         if (queryIndex >= 0)
         {
            String query = url.substring(queryIndex + 1, url.length());
            url = url.substring(0, queryIndex);

            withQuery(query);
         }

         //replace slashes after stripping off query to leave query as it was found
         url = url.replace('\\', '/');

         int potocolEnd = url.indexOf("://");
         if (potocolEnd < 0)
         {
            path = url;
         }
         else
         {
            //-- parse a full url
            protocol = url.substring(0, url.indexOf(':'));

            int hostStart = url.indexOf('/') + 2;
            int hostEnd = url.indexOf(':', hostStart);

            //--this is probably ah file url like file://c:/
            //--so don't cound this colon
            //if(hostEnd - hostStart <= 1)
            //   hostEnd = url.indexOf(':', hostEnd + 1);
            if (hostEnd < 0 || hostEnd > url.indexOf('/', hostStart))
            {
               hostEnd = url.indexOf('/', hostStart);
            }
            if (hostEnd < 0)
            {
               url += "/";
               hostEnd = url.indexOf('/', hostStart);
            }

            host = url.substring(hostStart, hostEnd);

            String rest = url.substring(hostEnd, url.length());

            if (rest.indexOf(':') > -1)
            {
               int nextColon = rest.indexOf(':');
               int nextSlash = rest.indexOf('/');

               if (nextColon < nextSlash)
               {
                  String portString = rest.substring(nextColon + 1, nextSlash);
                  port = Integer.parseInt(portString);
                  rest = rest.substring(nextSlash, rest.length());
               }
            }

            path = rest;
         }

         if (path == null || path.length() == 0)
         {
            path = "/";
         }
         else if (path.charAt(0) != '/')
         {
            path = '/' + url;
         }

         if (!Utils.empty(path))
            this.path = new Path(path);

      }
      catch (Exception ex)
      {
         System.err.println("Error parsing url \"" + url + "\"");
         ex.printStackTrace();
      }
   }

   public String toString()
   {
      String url = protocol + "://" + host;

      if (port > 0)
      {
         if (!((port == 80 && "http".equalsIgnoreCase(protocol)) || (port == 443 && "https".equalsIgnoreCase(protocol))))
            url += ":" + port;
      }

      if (path != null && path.size() > 0)
      {
         url += "/" + path;
      }

      if (params.size() > 0)
      {
         while (url.endsWith("/"))
            url = url.substring(0, url.length() - 1);

         url += "?";
         url += toQueryString(params.asMap());
      }

      return url;
   }

   public boolean equals(Object obj)
   {
      if (obj instanceof Url)
      {
         return toString().equals(((Url) obj).toString());
      }
      return false;
   }

   public String getDomain()
   {
      String domain = host;

      if (domain.lastIndexOf('.') > domain.indexOf('.'))
      {
         domain = domain.substring(domain.indexOf('.') + 1, domain.length());
      }
      return domain;
   }

   public String getQuery()
   {
      if (params.size() == 0)
         return "";

      return toQueryString(params.asMap());
   }

   public String getHost()
   {
      return host;
   }

   public Url withHost(String host)
   {
      this.host = host;
      return this;
   }

   public int getPort()
   {
      return port;
   }

   public Url withPort(int port)
   {
      this.port = port;
      return this;
   }

   public String getProtocol()
   {
      return protocol;
   }

   public Url withProtocol(String protocol)
   {
      this.protocol = protocol;
      return this;
   }

   public Path getPath()
   {
      return path;
   }

   public Url withPath(Path path)
   {
      this.path = path;
      return this;
   }

   public String getFile()
   {
      if (path != null)
         return path.last();

      return null;
   }

   public Url withQuery(String query)
   {
      params = new JSNode();
      params.putAll(Utils.parseQueryString(query));
      return this;
   }

   public Url withParams(Map<String, String> params)
   {
      if (params != null)
         this.params.putAll(params);
      return this;
   }

   public Url withParam(String name, String value)
   {
      params.put(name, value);
      return this;
   }

   public Url withParams(String... nvpairs)
   {
      if (nvpairs != null)
      {
         for (int i = 0; i < nvpairs.length - 1; i = i + 2)
            withParam(nvpairs[i], nvpairs[i + 1]);

         if (nvpairs.length % 2 == 1)
            withParam(nvpairs[nvpairs.length - 1], null);
      }
      return this;
   }

   /**
    * Replaces any param that has <code>key<code> as a whole 
    * word case insensitive match in its key.
    * 
    * @param key
    * @return
    */
   public void replaceParam(String key, String value)
   {
      for (String existing : (List<String>) new ArrayList(params.keySet()))
      {
         if (Utils.containsToken(key, existing))
         {
            params.remove(existing);
         }
      }

      withParam(key, value);
   }

   /**
    * Removes any param that has one of <code>tokens<code> as a whole 
    * word case insensitive match in the key.
    * 
    * @param key
    * @return
    */
   public String clearParams(String... tokens)
   {
      String oldValue = null;
      for (int i = 0; i < tokens.length; i++)
      {
         for (String existing : (List<String>) new ArrayList(params.keySet()))
         {
            if (Utils.containsToken(tokens[i], existing))
            {
               String removed = (String) params.remove(existing);
               if (oldValue == null)
                  oldValue = removed;
            }
         }
      }

      return oldValue;
   }

   public String findKey(String token)
   {
      for (String key : (List<String>) new ArrayList(params.keySet()))
      {
         if (Utils.containsToken(token, key))
         {
            return key;
         }
      }
      return null;
   }

   public String getParam(String param)
   {
      return (String) params.get(param);
   }

   public Map<String, String> getParams()
   {
      return params.asMap();
   }

   public String getOriginal()
   {
      return original;
   }

   public static String toQueryString(Map<String, String> params)
   {
      String query = "";
      if (params != null)
      {
         for (String key : params.keySet())
         {
            try
            {
               if (params.get(key) != null)
               {
                  query += URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(params.get(key), "UTF-8") + "&";
               }
               else
               {
                  query += URLEncoder.encode(key, "UTF-8") + "&";
               }

            }
            catch (UnsupportedEncodingException e)
            {
               Utils.rethrow(e);
            }
         }
         if (query.length() > 0)
            query = query.substring(0, query.length() - 1);
      }
      return query;
   }

}
