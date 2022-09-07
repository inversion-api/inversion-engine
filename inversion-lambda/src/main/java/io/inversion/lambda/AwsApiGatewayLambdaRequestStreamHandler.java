/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
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
package io.inversion.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import io.inversion.Api;
import io.inversion.Engine;
import io.inversion.Request;
import io.inversion.Response;
import io.inversion.json.JSMap;
import io.inversion.json.JSNode;
import io.inversion.json.JSParser;
import io.inversion.Url;
import io.inversion.utils.Utils;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Adapter to run an Inversion API as an AWS Lambda behind an ApiGateway
 */

public class AwsApiGatewayLambdaRequestStreamHandler implements RequestStreamHandler {
    protected Engine engine = null;
    protected Api    api    = null;

    boolean debug = false;

    @SuppressWarnings("unchecked")
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {

        String input = Utils.read(new BufferedInputStream(inputStream));

        JSNode    responseBody = new JSMap();
        JSNode    config       = null;
        Exception ex           = null;

        try {
            JSNode json = JSParser.asJSNode(input);

            debug("Request Event");
            debug(json.toString(false));

            String method = json.getString("httpMethod");
            String host   = (String) json.find("headers.Host");
            String path   = (String) json.find("requestContext.path");
            Url    url    = new Url("http://" + host + path);

            String profile = path != null ? Utils.explode("/", path).get(0) : "";

            String proxyPath = (String) json.find("pathParameters.proxy");
            proxyPath = proxyPath != null ? proxyPath : "";

            String pathStr  = Utils.implode("/", path);
            String proxyStr = Utils.implode("/", proxyPath);

            String servletPath = "";

            if (pathStr.length() > proxyStr.length()) {
                servletPath = pathStr.substring(0, pathStr.length() - proxyStr.length());
            }

            config = new JSMap("method", method, "host", host, "path", path, "url", url.toString(), "profile", profile, "proxyPath", proxyPath, "servletPath", servletPath);

            if (engine == null) {
                synchronized (this) {
                    if (engine == null) {
                        engine = buildEngine(profile, servletPath);
                        engine.startup();
                    }
                }
            }


            ArrayListValuedHashMap<String, String> headers     = new ArrayListValuedHashMap<>();
            JSMap                                 jsonHeaders = json.getMap("headers");
            if(jsonHeaders != null)
                headers.putAll((Map<String, String>)headers);

            JSNode              jsonParams = json.getNode("queryStringParameters");
            Map<String, String> params     = jsonParams == null ? new HashMap<>() : (Map<String, String>) jsonParams;

            String body = json.getString("body");

            if (method.equals("POST") && body != null) {
                Map<String, String> postParams = Utils.parseQueryString(body);
                params.putAll(postParams);
            }

            Request  req = new Request(method, url.toString(), body, params, headers);
            Response res = new Response();

            engine.service(req, res);

            if (outputStream != null) {
                writeResponse(res, outputStream);
            }

        } catch (Exception e1) {
            ex = e1;
        } finally {
            if (ex != null) {
                if (config != null)
                    responseBody.putValue("config", config);

                responseBody.putValue("error", Utils.getShortCause(ex));

                responseBody.putValue("request", JSParser.asJSNode(input));

                JSNode responseJson = new JSMap();
                responseJson.putValue("isBase64Encoded", false);
                responseJson.putValue("statusCode", "500");
                responseJson.putValue("headers", new JSMap("Access-Control-Allow-Origin", "*"));

                responseJson.putValue("body", responseBody.toString());
                OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
                writer.write(responseJson.toString());
                writer.close();
            }
        }
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
     * @param configProfile the configuration runtime profile
     * @param servletPath   the servlet path
     * @return an Engine with an Api already set if one was supplied otherwise an empty Engine that will be configured via via Config/Wirer.
     * @see #buildApi
     */
    protected Engine buildEngine(String configProfile, String servletPath) {

        System.out.println("TODO FIX ME: " + servletPath);
        System.out.println("TODO FIX ME: " + configProfile);

        Engine engine = new Engine();
        engine.withConfigProfile(configProfile);

        if (api == null)
            api = buildApi(engine);

        if (api != null) {
            engine.withApi(api);
        }

        return engine;
    }

    /**
     * Optional subclass override hook to supply your own custom wired up Api.
     * <p>
     * If you don't set your <code>api</code> via <code>setApi()</code> and you don't override <code>buildApi()</code> to supply an Api
     * or otherwise wire your custom Api and Engine in an overridden buildEngine() method, you will need to define your Api in inversion.properties files for autowiring via Config/Wirer.
     *
     * @param engine the engine that will host the Api
     * @return null unless you override this method to construct an Api.
     * @see #buildEngine
     */
    protected Api buildApi(Engine engine) {
        System.out.println("You can override buildApi(engine) to wire up your api in code: " + engine);
        return null;
    }

    protected void writeResponse(Response res, OutputStream outputStream) throws IOException {
//        JSNode responseJson = new JSMap();
//
//        responseJson.put("isBase64Encoded", false);
//        responseJson.put("statusCode", res.getStatusCode());
//        JSNode headers = new JSMap();
//        responseJson.put("headers", headers);
//
//        asdfasdf
//        for (String key : res.getHeaders().keySet()) {
//            List          values = res.getHeaders().get(key);
//            StringBuilder buff   = new StringBuilder();
//            for (int i = 0; i < values.size(); i++) {
//                buff.append(values.get(i));
//                if (i < values.size() - 1)
//                    buff.append(",");
//            }
//            headers.put(key, buff.toString());
//        }
//
//        String output = res.getOutput();
//
//        responseJson.put("body", output);
//        OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
//        writer.write(responseJson.toString());
//        writer.close();
    }

    public void debug(String msg) {
        if (isDebug()) {
            System.out.println(msg);
        }
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
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