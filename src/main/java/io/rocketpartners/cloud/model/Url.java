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
package io.rocketpartners.cloud.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.rocketpartners.cloud.utils.Utils;

public class Url implements Cloneable
{
   protected String     original = null;
   protected String     protocol = "http";
   protected String     host     = null;
   protected int        port     = 0;
   protected String     path     = null;
   protected String     query    = null;
   protected ObjectNode params   = new ObjectNode();

   public Url copy()
   {
      Url u = new Url();
      u.original = original;
      u.protocol = protocol;
      u.host = host;
      u.port = port;
      u.path = path;
      u.query = query;
      if (params.size() > 0)
         Utils.parseObjectNode(params.toString());

      return u;
   }

   private Url()
   {

   }

   public Url(String url)
   {
      this(url, null);
   }

   public Url(String parent, String child)
   {
      if (Utils.empty(parent) && Utils.empty(child))
         throw new ApiException("Can't construct an empty url");

      if (Utils.empty(child))
      {
         parse(parent);
      }
      else if (Utils.empty(parent) || (child.startsWith("http://") || child.startsWith("https://")))
      {
         parse(child);
      }
      else
      {
         if (!parent.endsWith("/"))
         {
            parent += "/";
         }
         while (child.startsWith("/"))
            child = child.substring(1, child.length());

         parse(parent + child);
      }
   }

   //   public Url(Url parent, String url)
   //   {
   //      parse(parent, url);
   //   }
   //
   //   public Url(String parent, String url)
   //   {
   //      this(new Url(parent), url);
   //   }

   protected void parse(String url)
   {
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
            query = url.substring(queryIndex + 1, url.length());
            url = url.substring(0, queryIndex);

            withQuery(query);
            //this.params = new ObjectNode(Utils.parseQueryString(query));
         }

         //replace slashes after stripping off query to leave query as it was found
         url = url.replace('\\', '/');

         int potocolEnd = url.indexOf("://");
         if (potocolEnd < 0)
         {
            //            if (parent != null)
            //            {
            //               protocol = parent.protocol;
            //               host = parent.host;
            //               port = parent.port;
            //            }

            if (url.length() == 0 || url.charAt(0) == '/')
            {
               //-- absolute path form parent
               path = url;
            }
            else
            {
               //               //-- path relative to parent
               //               if (parent != null)
               //               {
               //                  String parentUri = parent.path;
               //                  if (parentUri.charAt(parentUri.length() - 1) != '/')
               //                  {
               //                     if (parentUri.lastIndexOf('/') >= 0)
               //                     {
               //                        //chop off the file to make it path
               //                        //realtive not file relative
               //                        parentUri = parentUri.substring(0, parentUri.lastIndexOf('/') + 1);
               //                     }
               //                     else
               //                     {
               //                        parentUri += '/';
               //                     }
               //                  }
               //
               //                  if (url.charAt(0) == '/')
               //                  {
               //                     url = url.substring(1, url.length());
               //                  }
               //
               //                  path = parentUri + url;
               //
               //               }
               //               else
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

      if (params.size() > 0)
      {
         while (url.endsWith("/"))
            url = url.substring(0, url.length() - 1);

         List<String> keys = new ArrayList(params.keySet());
         for (int i = 0; i < keys.size(); i++)
         {
            String key = keys.get(i);
            String value = params.getString(key);

            if (i == 0)
               url += "?";
            else if (!key.startsWith("&"))
               url += "&";

            url += key;

            if (value != null)
               url += "=" + value;
         }
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
      //      if (port == 0)
      //      {
      //         if ("http".equalsIgnoreCase(protocol))
      //            return 80;
      //         if ("https".equalsIgnoreCase(protocol))
      //            return 443;
      //      }
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

   public String getQuery()
   {
      return query;
   }

   public Url withQuery(String query)
   {
      this.query = query;
      params = new ObjectNode();
      if (query != null)
      {
         params.putAll(Utils.parseQueryString(query));
      }
      return this;
   }

   public String getPath()
   {
      return path;
   }

   public Url withPath(String path)
   {
      this.path = path;
      return this;
   }

   public Url addPath(String path)
   {
      this.path = Utils.implode("/", this.path, path);
      return this;
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
            params.put(nvpairs[i], nvpairs[i + 1]);

         if (nvpairs.length % 2 == 1)
            params.put(nvpairs[nvpairs.length - 1], null);
      }
      return this;
   }

   public String getParam(String param)
   {
      return (String) params.get(param);
   }

   public Map<String, String> getParams()
   {
      return params.asMap();
   }

   public String removeParam(String param)
   {
      return (String) params.remove(param);
   }

   public void clearParams()
   {
      params.clear();
   }

   public String getOriginal()
   {
      return original;
   }

   public Url withOriginal(String url)
   {
      this.original = url;
      return this;
   }

}
