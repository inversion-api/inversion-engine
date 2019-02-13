/*
 * Copyright (c) 2016-2019 Rocket Partners, LLC
 * http://rocketpartners.io
 * 
 * Copyright 2008-2016 Wells Burke
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
package io.rocketpartners.cloud.utils;

import java.io.Serializable;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class Url implements Serializable
{
   public static Map<String, String> parseQuery(String query)
   {
      Map params = new HashMap();
      try
      {
         while (query.startsWith("?") || query.startsWith("&") || query.startsWith("="))
         {
            query = query.substring(1);
         }

         if (query.length() > 0)
         {
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
               else
               {
                  params.put(URLDecoder.decode(pair, "UTF-8"), null);
               }
            }
         }
      }
      catch (Exception ex)
      {
         Utils.rethrow(ex);
      }
      return params;
   }

   protected String              protocol = "http";
   protected String              host     = null;
   protected int                 port     = 0;
   protected String              path     = null;
   protected String              query    = null;
   protected Map<String, String> params   = new HashMap();

   public Url(String url)
   {
      this((Url) null, url);
   }

   public Url(Url parent, String url)
   {
      parse(parent, url);
   }

   public Url(String parent, String url)
   {
      this(new Url(parent), url);
   }

   public Url(String protocol, String host, int port, String path, String query)
   {
      super();
      this.protocol = protocol;
      this.host = host;
      this.port = port;
      this.path = path;
      this.query = query;
      if (!Utils.empty(query))
         this.params = parseQuery(query);
   }

   protected void parse(Url parent, String url)
   {
      if (url.indexOf(":/") > 0 && url.indexOf("://") < 0)
         url = url.replaceAll(":/", "://");

      url = url.replace("&amp;", "&");

      try
      {
         int queryIndex = url.indexOf('?');
         if (queryIndex >= 0)
         {
            query = url.substring(queryIndex + 1, url.length());
            url = url.substring(0, queryIndex);

            this.params = parseQuery(query);
         }

         //replace slashes after stripping off query to leave query as it was found
         url = url.replace('\\', '/');

         int potocolEnd = url.indexOf("://");
         if (potocolEnd < 0)
         {
            if (parent != null)
            {
               protocol = parent.protocol;
               host = parent.host;
               port = parent.port;
            }

            if (url.length() == 0 || url.charAt(0) == '/')
            {
               //-- absolute path form parent
               path = url;
            }
            else
            {
               //-- path relative to parent
               if (parent != null)
               {
                  String parentUri = parent.path;
                  if (parentUri.charAt(parentUri.length() - 1) != '/')
                  {
                     if (parentUri.lastIndexOf('/') >= 0)
                     {
                        //chop off the file to make it path
                        //realtive not file relative
                        parentUri = parentUri.substring(0, parentUri.lastIndexOf('/') + 1);
                     }
                     else
                     {
                        parentUri += '/';
                     }
                  }

                  if (url.charAt(0) == '/')
                  {
                     url = url.substring(1, url.length());
                  }

                  path = parentUri + url;

               }
               else
               {
                  path = url;
               }
            }
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
         while (path.contains("//"))
         {
            path = path.replace("//", "/");
         }
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

      if (!Utils.empty(path))
      {
         if (!path.startsWith("/"))
            url += "/";

         url += path;
      }

      if (!Utils.empty(query))
      {
         if (!query.startsWith("?"))
            url += "?";
         url += query;
      }

      return url;
   }

   /**
    * @return the url without any querystring
    */
   public Url getBase()
   {
      return new Url(this.protocol, this.host, this.port, this.path, null);
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

   public String getHost()
   {
      return host;
   }

   public void setHost(String host)
   {
      this.host = host;
   }

   public int getPort()
   {
      if (port == 0)
      {
         if ("http".equalsIgnoreCase(protocol))
            return 80;
         if ("https".equalsIgnoreCase(protocol))
            return 443;
      }
      return port;
   }

   public void setPort(int port)
   {
      this.port = port;
   }

   public String getProtocol()
   {
      return protocol;
   }

   public void setProtocol(String protocol)
   {
      this.protocol = protocol;
   }

   public String getQuery()
   {
      return query;
   }

   public void setQuery(String query)
   {
      this.query = query;
   }

   public String getPath()
   {
      return path;
   }

   public void setPath(String path)
   {
      this.path = path;
   }

   public String getFile()
   {
      if (path != null && !path.endsWith("/"))
      {
         if (path.lastIndexOf('/') > -1)
            return path.substring(path.lastIndexOf('/') + 1, path.length());
         return path;

      }

      return null;
   }

   public Map<String, String> getParams()
   {
      return params;
   }

   public void setParams(Map<String, String> params)
   {
      this.params = params;
   }

}
