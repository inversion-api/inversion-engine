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

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.KeyValue;
import org.apache.commons.collections4.keyvalue.DefaultKeyValue;

public class UrlBuilder
{

   String                       protocol = null;
   String                       host     = null;
   Integer                      port     = null;
   String                       path     = null;

   List<KeyValue<String, String>> query    = new ArrayList();

   public UrlBuilder()
   {

   }

   public UrlBuilder(String url)
   {
      this(new Url(url));
   }

   public UrlBuilder(Url url)
   {
      protocol = url.getProtocol();
      host = url.getHost();
      port = url.getPort();
      path = url.getPath();
   }

   public UrlBuilder(String protocol, String host, Integer port, String path, Object... params)
   {
      this.protocol = protocol;
      this.host = host;
      this.port = port;
      this.path = path;

      List plist = null;
      if (params != null)
      {
         plist = Arrays.asList(params);
         if (plist.size() > 0 && plist.get(0) instanceof Collection)
         {
            plist = new ArrayList((Collection) plist.get(0));
         }
      }

      for (int i = 0; plist != null && i < plist.size(); i += 2)
      {
         query.add(new DefaultKeyValue(plist.get(i) + "", plist.get(i + 1) == null ? null : (plist.get(i + 1) + "")));
      }
   }

   public String toString()
   {
      return toUrl().toString();
   }

   public String getHost()
   {
      return host;
   }

   public UrlBuilder withHost(String host)
   {
      host = host.replace("/", "");
      this.host = host;
      return this;
   }

   public Integer getPort()
   {
      return port;
   }

   public UrlBuilder withPort(Integer port)
   {
      this.port = port;
      return this;
   }

   public String getProtocol()
   {
      return protocol;
   }

   public UrlBuilder withProtocol(String protocol)
   {
      this.protocol = protocol;
      return this;
   }

   public String getPath()
   {
      return path;
   }

   public UrlBuilder withPath(String path)
   {
      this.path = path;
      if (!path.startsWith("/"))
         path += "/";

      return this;
   }

   public UrlBuilder addPath(String dir)
   {
      if (path == null)
         path = "/";

      if (!path.startsWith("/"))
         path = "/" + path;

      if (!path.endsWith("/"))
         path += "/";

      while (dir.startsWith("/"))
         dir = dir.substring(1);

      path += dir;

      if (!path.endsWith("/"))
         path += "/";

      return this;
   }

   /**
    * Parses queryString and adds the nvpairs to query.
    */
   public UrlBuilder withQuery(String queryString)
   {
      Map<String, String> params = Url.parseQuery(queryString);
      for (String key : params.keySet())
      {
         query.add(new DefaultKeyValue(key, params.get(key)));
      }
      return this;
   }

   public UrlBuilder withParam(String name, String value)
   {
      try
      {
         query.add(new DefaultKeyValue(URLEncoder.encode(name, "UTF-8"), value != null ? URLEncoder.encode(value, "UTF-8") : null));
      }
      catch (Exception ex)
      {
         Utils.rethrow(ex);
      }
      return this;
   }

   public Url toUrl()
   {
      String queryStr = null;
      if (query != null && query.size() > 0)
      {
         queryStr = "";
         for (int i = 0; i < query.size(); i++)
         {
            KeyValue pair = query.get(i);
            if (Utils.empty(pair.getValue()))
               queryStr += pair.getKey();
            else
               queryStr += pair.getKey() + "=" + pair.getValue();

            if (i < query.size() - 1)
               queryStr += "&";
         }
      }

      Url u = new Url(this.protocol, this.host, this.port, this.path, queryStr);
      return u;

   }

}
