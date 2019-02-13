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

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import io.rocketpartners.cloud.utils.JS;
import io.rocketpartners.cloud.utils.JSObject;
import io.rocketpartners.cloud.utils.Utils;

/**
 * Simple handler to echo the request back 
 * @author wells
 */
public class EchoRequestStreamHandler implements RequestStreamHandler
{
   public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException
   {
      JSObject responseBody = new JSObject();
      JSObject responseJson = new JSObject();
      responseJson.put("isBase64Encoded", false);
      responseJson.put("statusCode", "200");
      responseJson.put("headers", new JSObject("Access-Control-Allow-Origin", "*"));
      try
      {
         String input = Utils.read(new BufferedInputStream(inputStream));
         context.getLogger().log(input);
         JSObject request = JS.toJSObject(input);
         responseBody.put("request", request);

      }
      catch (Exception ex)
      {
         responseBody.put("error", Utils.getShortCause(ex));
      }

      responseJson.put("body", responseBody.toString());
      OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
      writer.write(responseJson.toString());
      writer.close();
   }
}