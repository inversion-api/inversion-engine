/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.cloud.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import io.inversion.cloud.model.ApiException;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Request.Upload;
import io.inversion.cloud.model.Request.Uploader;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.model.SC;
import io.inversion.cloud.utils.Utils;

public class Servlet extends HttpServlet
{
   public static class ServletLocal
   {
      static ThreadLocal<HttpServletRequest>  request  = new ThreadLocal();
      static ThreadLocal<HttpServletResponse> response = new ThreadLocal();

      public static void set(HttpServletRequest req, HttpServletResponse res)
      {
         request.set(req);
         response.set(res);
      }

      public static void setRequest(HttpServletRequest req)
      {
         request.set(req);
      }

      public static void setResponse(HttpServletResponse res)
      {
         response.set(res);
      }

      public static HttpServletRequest getRequest()
      {
         return request.get();
      }

      public static HttpServletResponse getResponse()
      {
         return response.get();
      }
   }

   Engine engine = null;//new Engine();

   public void destroy()
   {
      engine.destroy();
   }

   public void init(ServletConfig config)
   {
      engine.startup();
   }

   public Engine getEngine()
   {
      return engine;
   }

   public void setEngine(Engine engine)
   {
      this.engine = engine;
   }

   @Override
   public void service(HttpServletRequest httpReq, HttpServletResponse httpResp) throws ServletException, IOException
   {
      ServletLocal.set(httpReq, httpResp);

      Response res = null;
      Request req = null;

      try
      {
         String method = httpReq.getMethod();
         String urlstr = httpReq.getRequestURL().toString();

         if (!urlstr.endsWith("/"))
            urlstr = urlstr + "/";

         String query = httpReq.getQueryString();
         if (!Utils.empty(query))
         {
            urlstr += "?" + query;
         }

         Map headers = new HashMap();
         Enumeration<String> headerEnum = httpReq.getHeaderNames();
         while (headerEnum.hasMoreElements())
         {
            String key = headerEnum.nextElement();
            String val = httpReq.getHeader(key);
            headers.put(key, val);
         }

         Map params = new HashMap();
         Enumeration<String> paramsEnumer = httpReq.getParameterNames();
         while (paramsEnumer.hasMoreElements())
         {
            String key = paramsEnumer.nextElement();
            String val = httpReq.getParameter(key);
            params.put(key, val);
         }

         String body = readBody(httpReq);

         req = new Request(method, urlstr, headers, params, body);
         req.withRemoteAddr(httpReq.getRemoteAddr());

         req.withUploader(new Uploader()
            {
               @Override
               public List<Upload> getUploads()
               {
                  try
                  {
                     String fileName = null;
                     long fileSize = 0;
                     String requestPath = null;
                     InputStream inputStream = null;

                     for (Part part : httpReq.getParts())
                     {
                        if (part.getName() == null)
                        {
                           continue;
                        }
                        if (part.getName().equals("file"))
                        {
                           inputStream = part.getInputStream();
                           fileName = part.getSubmittedFileName();
                           fileSize = part.getSize();
                        }
                        else if (part.getName().equals("requestPath"))
                        {
                           requestPath = Utils.read(part.getInputStream());
                           if (requestPath.indexOf("/") == 0)
                              requestPath = requestPath.substring(1);
                        }
                     }

                     List uploads = new ArrayList();

                     if (inputStream != null)
                     {
                        uploads.add(new Upload(fileName, fileSize, requestPath, inputStream));
                     }
                     return uploads;
                  }
                  catch (Exception ex)
                  {
                     Utils.rethrow(ex);
                  }
                  return null;
               }
            });

         res = new Response();

         engine.service(req, res);
         writeResponse(req, res, httpResp);
      }
      catch (Exception ex)
      {
         ex.printStackTrace();
         httpResp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }
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

   void writeResponse(Request req, Response res, HttpServletResponse http) throws Exception
   {
      String method = req != null ? req.getMethod() : null;

      http.setStatus(res.getStatusCode());

      OutputStream out = http.getOutputStream();
      try
      {
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
            http.setHeader(key, buff.toString());
            res.debug(key + " " + buff);
         } ;
         if ("OPTIONS".equals(method))
         {
            //
         }
         else
         {
            String contentType = res.getContentType();
            byte[] bytes = res.getOutput().getBytes();

            http.setContentType(contentType);
            res.withHeader("Content-Length", bytes.length + "");
            res.debug("Content-Length " + bytes.length + "");

            out.write(bytes);
         }
      }
      finally
      {
         out.flush();
         out.close();
      }
   }
}
