/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
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
package io.inversion.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import io.inversion.json.JSMap;
import io.inversion.json.JSNode;
import io.inversion.json.JSParser;
import io.inversion.utils.Utils;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Simple handler to echo the request back
 */
public class EchoRequestStreamHandler implements RequestStreamHandler {
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        JSNode responseBody = new JSMap();
        JSNode responseJson = new JSMap();
        responseJson.putValue("isBase64Encoded", false);
        responseJson.putValue("statusCode", "200");
        responseJson.putValue("headers", new JSMap("Access-Control-Allow-Origin", "*"));
        try {
            String input = Utils.read(new BufferedInputStream(inputStream));
            context.getLogger().log(input);
            JSNode request = JSParser.asJSNode(input);
            responseBody.putValue("request", request);

        } catch (Exception ex) {
            responseBody.putValue("error", Utils.getShortCause(ex));
        }

        responseJson.putValue("body", responseBody.toString());
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        writer.write(responseJson.toString());
        writer.close();
    }
}