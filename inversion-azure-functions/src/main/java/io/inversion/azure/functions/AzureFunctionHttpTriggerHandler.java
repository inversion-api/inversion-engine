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

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.HttpResponseMessage.Builder;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import io.inversion.Api;
import io.inversion.Engine;
import io.inversion.Request;
import io.inversion.Response;
import io.inversion.rql.RqlTokenizer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter to run an Inversion API as an Azure Function
 * <p>
 * TODO: multipart post is not yet supported but could be implemented: https://stackoverflow.com/questions/54473126/azure-functions-how-to-use-the-multiparthttpservletrequest-class-from-the-de
 *
 * @see Similar functionality for AWS Lambdas is provided by io.inversion.lambda.AwsApiGatewayLambdaRequestStreamHandler
 * @see https://docs.microsoft.com/en-us/azure/azure-functions/functions-create-first-azure-function-azure-cli?tabs=bash%2Cbrowser&pivots=programming-language-java
 */
public class AzureFunctionHttpTriggerHandler {
    protected Engine engine = null;
    protected Api    api    = null;

    @FunctionName("HttpTrigger-Java")
    public HttpResponseMessage run(@HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.OPTIONS, HttpMethod.DELETE}, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request, final ExecutionContext context) {
        try {
            if (engine == null) {
                synchronized (this) {
                    if (engine == null) {
                        engine = buildEngine(request, context);
                    }
                }
            }

            Request  req = buildRequest(request);
            Response res = engine.service(req, new Response()).getResponse();

            return buildHttpResponseMessage(request, req, res);
        } catch (Exception ex) {
            ex.printStackTrace();
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage()).build();
        }
    }

    protected Request buildRequest(HttpRequestMessage<Optional<String>> request) {
        String method = request.getHttpMethod().toString();
        String url    = request.getUri().toString();

        if (url.indexOf("?") > 0)
            url = url.substring(0, url.indexOf("?"));

        if (!url.endsWith("/"))
            url = url + "/";

        Map safeParams = new HashMap();

        for (String key : request.getQueryParameters().keySet()) {
            boolean skip = false;

            if (key.indexOf("_") > 0) {
                //-- RQL expressions with tokens that start with an "_" are not for public use at this time.
                List illegals = new RqlTokenizer(key).stream().filter(s -> s.startsWith("_")).collect(Collectors.toList());
                if (illegals.size() > 0) {
                    skip = true;
                }
            }

            if (!skip) {
                String val = request.getQueryParameters().get(key);
                safeParams.put(key, val);
            }
        }

        String body = request.getBody().isPresent() ? request.getBody().get() : null;

        Request req = new Request(method, url, request.getHeaders(), safeParams, body);
        return req;
    }

    protected HttpResponseMessage buildHttpResponseMessage(HttpRequestMessage<Optional<String>> azReq, Request req, Response res) throws Exception {
        Builder builder = azReq.createResponseBuilder(HttpStatus.valueOf(res.getStatusCode()));

        for (String key : res.getHeaders().keySet()) {
            List         values = res.getHeaders().get(key);
            StringBuffer buff   = new StringBuffer();
            for (int i = 0; i < values.size(); i++) {
                buff.append(values.get(i));
                if (i < values.size() - 1)
                    buff.append(",");
            }
            builder.header(key, buff.toString());
            res.debug(key + " " + buff);
        }
        if ("OPTIONS".equals(req.getMethod())) {
            //
        } else {
            String contentType = res.getContentType();
            builder.header("Content-Type", contentType);

            String output = res.getOutput();
            builder.body(output);
        }

        return builder.build();
    }

    /**
     * Optional subclass override hook to allow for advanced Engine configuration.
     * <p>
     * Simple embeddings can leave this alone.  Complex embeddings can either set the engine via setEngine() or override this method to construct/configure as needed.
     * <p>
     * This default implementation constructs an Engine with the supplied configProfile and adds <code>api</code> to it if <code>api</code> is not null.
     * <p>
     * If <code>api</code> is null, it calls buildApi() which by default does nothing and is itself designed as an override hook.
     *
     * @return an Engine with an Api already set if one was supplied otherwise an empty Engine that will be configured via via Confg/Configurator.
     * @see #buildApi()
     */
    protected Engine buildEngine(HttpRequestMessage<Optional<String>> request, final ExecutionContext context) {
        Engine engine = new Engine();

        if (api == null)
            api = buildApi(request, context);

        if (api != null) {
            engine.withApi(api);
        }

        return engine;
    }

    /**
     * Optional subclass override hook to supply your own custom wired up Api.
     * <p>
     * If you don't set your <code>api</code> via <code>setApi()</code> and you don't override <code>buildApi()</code> to supply an Api
     * or otherwise wire your custom Api and Engine in an overridden buildEngine() method, you will need to define your Api in inversion.properties files for autowiring via Confg/Configurator.
     *
     * @return null unless you override this method to construct an Api.
     * @see #buildEngine
     */
    protected Api buildApi(HttpRequestMessage<Optional<String>> request, final ExecutionContext context) {
        return null;
    }

    public Engine getEngine() {
        return engine;
    }

    public void setEngine(Engine engine) {
        this.engine = engine;
    }

    public Api getApi() {
        return api;
    }

    public void setApi(Api api) {
        this.api = api;
    }


}
