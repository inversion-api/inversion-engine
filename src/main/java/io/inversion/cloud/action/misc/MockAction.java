/*
 * Copyright (c) 2015-2019 Inversion.org, LLC
 * https://github.com/inversion-api
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
package io.inversion.cloud.action.misc;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

import io.inversion.cloud.model.Action;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.ApiException;
import io.inversion.cloud.model.Endpoint;
import io.inversion.cloud.model.JSArray;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.model.SC;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.service.Engine;
import io.inversion.cloud.utils.Utils;

public class MockAction extends Action<MockAction>
{
   protected JSNode json          = null;
   protected String  jsonUrl       = null;
   protected int     statusCode    = 200;
   protected String  status        = SC.SC_200_OK;
   protected boolean cancelRequest = true;

   public MockAction()
   {

   }

   public MockAction(String name)
   {
      this(null, null, name, new JSNode("name", name));
   }

   public MockAction(String methods, String includePaths, String name)
   {
      this(methods, includePaths, name, null);
   }

   public MockAction(String methods, String includePaths, String name, JSNode json)
   {
      withMethods(methods);
      withIncludePaths(includePaths);
      withName(name);
      withJson(json);
   }

   @Override
   public void run(Engine engine, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
   {
      res.withStatus(status);
      res.withStatusCode(statusCode);

      JSNode json = getJson();

      if (json != null)
      {
         if (json instanceof JSArray)
            res.withData((JSArray) json);
         else
            res.withJson(json);
      }

      if (cancelRequest)
         chain.cancel();
   }

   public MockAction withJson(JSNode json)
   {
      this.json = json;
      return null;
   }

   public String getJsonUrl()
   {
      return jsonUrl;
   }

   public MockAction withJsonUrl(String jsonUrl)
   {
      this.jsonUrl = jsonUrl;
      return this;
   }

   public JSNode getJson()
   {
      if (json == null && jsonUrl != null)
      {
         InputStream stream = null;
         try
         {
            stream = new URL(jsonUrl).openStream();
         }
         catch (Exception ex)
         {
         }

         if (stream == null)
         {
            stream = getClass().getResourceAsStream(jsonUrl);
         }

         if (stream == null)
         {
            stream = getClass().getClassLoader().getResourceAsStream(jsonUrl);
         }

         if (stream == null)
         {
            try
            {
               File f = new File(jsonUrl);
               if (f.exists())
                  stream = new BufferedInputStream(new FileInputStream(jsonUrl));
            }
            catch (Exception ex)
            {
               ex.printStackTrace();
            }
         }

         if (stream != null)
         {
            json = JSNode.parseJsonNode(Utils.read(stream));
         }
         else
         {
            throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Unable to locate jsonUrl '" + jsonUrl + "'. Please check your configuration");
         }

      }

      return json;

   }

   public int getStatusCode()
   {
      return statusCode;
   }

   public MockAction StatusCode(int statusCode)
   {
      this.statusCode = statusCode;
      return this;
   }

   public String getStatus()
   {
      return status;
   }

   public MockAction withStatus(String status)
   {
      this.status = status;
      return this;
   }

   public boolean isCancelRequest()
   {
      return cancelRequest;
   }

   public MockAction withCancelRequest(boolean cancelRequest)
   {
      this.cancelRequest = cancelRequest;
      return this;
   }
}
