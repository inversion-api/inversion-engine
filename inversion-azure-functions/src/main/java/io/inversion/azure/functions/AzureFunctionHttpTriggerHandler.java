/*
 * Copyright (c) 2015-2020 Rocket Partners, LLC
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
package io.inversion.azure.functions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpResponseMessage.Builder;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import io.inversion.Engine;
import io.inversion.Request;
import io.inversion.Response;
import io.inversion.rql.RqlTokenizer;

/**
 * Adapter to run an Inversion API as an Azure Function
 *
 * TODO: multipart post is not yet supported but could be implemented: https://stackoverflow.com/questions/54473126/azure-functions-how-to-use-the-multiparthttpservletrequest-class-from-the-de 
 *  
 * @see Similar functionality for AWS Lambdas is provided by io.inversion.lambda.AwsApiGatewayLambdaRequestStreamHandler
 * @see https://docs.microsoft.com/en-us/azure/azure-functions/functions-create-first-azure-function-azure-cli?tabs=bash%2Cbrowser&pivots=programming-language-java
 */
public class AzureFunctionHttpTriggerHandler
{
   Engine engine = null;

   @FunctionName("HttpTrigger-Java")
   public HttpResponseMessage run(@HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.OPTIONS, HttpMethod.DELETE}, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request, final ExecutionContext context)
   {
      try
      {
         if (engine == null)
         {
            synchronized (this)
            {
               if (engine == null)
               {
                  engine = buildEngine(request, context);
               }
            }
         }

         Request req = buildRequest(request);
         Response res = engine.service(req, new Response()).getResponse();

         return buildHttpResponseMessage(request, req, res);
      }
      catch (Exception ex)
      {
         ex.printStackTrace();
         return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage()).build();
      }
   }

   protected Request buildRequest(HttpRequestMessage<Optional<String>> request)
   {
      String method = request.getHttpMethod().toString();
      String url = request.getUri().toString();

      if (url.indexOf("?") > 0)
         url = url.substring(0, url.indexOf("?"));

      if (!url.endsWith("/"))
         url = url + "/";

      Map safeParams = new HashMap();

      for (String key : request.getQueryParameters().keySet())
      {
         boolean skip = false;

         if (key.indexOf("_") > 0)
         {
            //-- RQL expressions with tokens that start with an "_" are not for public use at this time.
            List illegals = new RqlTokenizer(key).stream().filter(s -> s.startsWith("_")).collect(Collectors.toList());
            if (illegals.size() > 0)
            {
               skip = true;
            }
         }

         if (!skip)
         {
            String val = request.getQueryParameters().get(key);
            safeParams.put(key, val);
         }
      }

      String body = request.getBody().isPresent() ? request.getBody().get() : null;

      Request req = new Request(method, url, request.getHeaders(), safeParams, body);
      return req;
   }

   protected HttpResponseMessage buildHttpResponseMessage(HttpRequestMessage<Optional<String>> azReq, Request req, Response res) throws Exception
   {
      Builder builder = azReq.createResponseBuilder(HttpStatus.valueOf(res.getStatusCode()));

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
         builder.header(key, buff.toString());
         res.debug(key + " " + buff);
      }
      if ("OPTIONS".equals(req.getMethod()))
      {
         //
      }
      else
      {
         String contentType = res.getContentType();
         builder.header("Content-Type", contentType);

         String output = res.getOutput();
         builder.body(output);
      }

      return builder.build();
   }

   /**
    * Override this method to build your Engine and wire up your Api. 
    * <p>
    * The engine you create will be cached and reused across all calls to this Azure Function instance.
    * 
    * @param request
    * @param context
    * @return the Inversion Engine to service requests
    */
   protected Engine buildEngine(HttpRequestMessage<Optional<String>> request, final ExecutionContext context)
   {
      return new Engine();
   }
}
