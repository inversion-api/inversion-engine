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
import io.inversion.utils.JSNode;
import io.inversion.utils.Utils;

import java.io.*;

/**
 * Simple handler to echo the request back
 */
public class EchoRequestStreamHandler implements RequestStreamHandler {
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        JSNode responseBody = new JSNode();
        JSNode responseJson = new JSNode();
        responseJson.put("isBase64Encoded", false);
        responseJson.put("statusCode", "200");
        responseJson.put("headers", new JSNode("Access-Control-Allow-Origin", "*"));
        try {
            String input = Utils.read(new BufferedInputStream(inputStream));
            context.getLogger().log(input);
            JSNode request = JSNode.parseJsonNode(input);
            responseBody.put("request", request);

        } catch (Exception ex) {
            responseBody.put("error", Utils.getShortCause(ex));
        }

        responseJson.put("body", responseBody.toString());
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
        writer.write(responseJson.toString());
        writer.close();
    }
}