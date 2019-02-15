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

import com.amazonaws.util.IOUtils;

import io.rocketpartners.cloud.model.ApiException;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.model.Url;
import io.rocketpartners.cloud.service.Request.Upload;
import io.rocketpartners.cloud.service.Request.Uploader;
import io.rocketpartners.cloud.utils.Utils;

public class Servlet extends HttpServlet
{
   Service service = new Service();

   public void destroy()
   {
      service.destroy();
   }

   public void init(ServletConfig config)
   {
      service.init();
   }

   public Service getService()
   {
      return service;
   }

   public void setService(Service service)
   {
      this.service = service;
   }

   @Override
   public void service(HttpServletRequest httpReq, HttpServletResponse httpResp) throws ServletException, IOException
   {

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

         Url url = new Url(urlstr);

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

         req = new Request(url, method, headers, params, body);
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
                           requestPath = IOUtils.toString(part.getInputStream());
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

         service.service(req, res);
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
