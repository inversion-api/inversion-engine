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
package io.rocketpartners.cloud.service.lambda;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import io.rocketpartners.cloud.model.JSNode;
import io.rocketpartners.cloud.model.Request;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.model.Url;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.service.Engine;
import io.rocketpartners.cloud.utils.Utils;

/**
 * @author wells
 */
public class ApiGatewayRequestStreamHandler implements RequestStreamHandler
{
   Engine engine = null;
   boolean debug   = false;

   public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException
   {
      runRequest(inputStream, outputStream, context);
   }

   public Chain runRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException
   {
      Chain chain = null;

      String input = Utils.read(new BufferedInputStream(inputStream));

      JSNode responseBody = new JSNode();
      JSNode config = null;
      Exception ex = null;

      try
      {
         JSNode json = JSNode.parseJsonNode(input);

         debug("Request Event");
         debug(json.toString(false));

         String method = json.getString("httpMethod");
         String host = (String) json.find("headers.Host");
         String path = (String) json.find("requestContext.path");
         Url url = new Url("http://" + host + path);

         String profile = path != null ? Utils.explode("/", path).get(0) : "";

         String proxyPath = (String) json.find("pathParameters.proxy");
         proxyPath = proxyPath != null ? proxyPath : "";

         String pathStr = Utils.implode("/", path);
         String proxyStr = Utils.implode("/", proxyPath);

         String servletPath = "";

         if (pathStr.length() > proxyStr.length())
         {
            servletPath = pathStr.substring(0, pathStr.length() - proxyStr.length());
         }

         config = new JSNode("method", method, "host", host, "path", path, "url", url.toString(), "profile", profile, "proxyPath", proxyPath, "servletPath", servletPath);

         if (engine == null)
         {
            synchronized (this)
            {
               if (engine == null)
               {
                  engine = buildEngine(profile, servletPath);
                  engine.startup();
               }
            }
         }

         Response res = null;
         Request req = null;

         Map headers = new HashMap();
         JSNode jsonHeaders = json.getNode("headers");
         if (jsonHeaders != null)
            headers = jsonHeaders.asMap();

         Map params = new HashMap();
         JSNode jsonParams = json.getNode("queryStringParameters");
         if (jsonParams != null)
         {
            params = jsonParams.asMap();
         }

         String body = json.getString("body");

         if (method.equals("POST") && body != null)
         {
            Map<String, String> postParams = Utils.parseQueryString(body);
            params.putAll(postParams);
         }

         req = new Request(url.toString(), method, headers, params, body);
         res = new Response();

         chain = engine.service(req, res);

         if (outputStream != null)
         {
            writeResponse(res, outputStream);
         }

      }
      catch (Exception e1)
      {
         ex = e1;
      }
      finally
      {
         if (ex != null)
         {
            if (config != null)
               responseBody.put("config", config);

            responseBody.put("error", Utils.getShortCause(ex));

            responseBody.put("request", JSNode.parseJsonNode(input));

            JSNode responseJson = new JSNode();
            responseJson.put("isBase64Encoded", false);
            responseJson.put("statusCode", "500");
            responseJson.put("headers", new JSNode("Access-Control-Allow-Origin", "*"));

            responseJson.put("body", responseBody.toString());
            OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
            writer.write(responseJson.toString());
            writer.close();
         }
      }

      return chain;
   }

   /**
    * This method is here as a hook for sub classes to override.
    * @return
    */
   protected Engine buildEngine(String profile, String servletPath)
   {
      Engine engine = new Engine();

      if (!Utils.empty(profile))
         engine.withProfile(profile);

      if (!Utils.empty(servletPath))
         engine.withServletMapping(servletPath);

      return engine;
   }

   protected void writeResponse(Response res, OutputStream outputStream) throws IOException
   {
      JSNode responseJson = new JSNode();

      responseJson.put("isBase64Encoded", false);
      responseJson.put("statusCode", res.getStatusCode());
      JSNode headers = new JSNode();
      responseJson.put("headers", headers);

      for (String key : res.getHeaders().keySet())
      {
         List values = res.getHeaders().get(key);
         StringBuffer buff = new StringBuffer();
         for (int i = 0; i < values.size(); i++)
         {
            buff.append(values.get(i));
            if (i < values.size() - 1)
               buff.append(",");
         }
         headers.put(key, buff.toString());
      }

      String output = res.getOutput();

      responseJson.put("body", output);
      OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
      writer.write(responseJson.toString());
      writer.close();
   }

   public void debug(String msg)
   {
      if (isDebug())
      {
         System.out.println(msg);
      }
   }

   public boolean isDebug()
   {
      return debug;
   }

   public void setDebug(boolean debug)
   {
      this.debug = debug;
   }

}