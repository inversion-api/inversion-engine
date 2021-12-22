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
package io.inversion.action.misc;

import io.inversion.*;
import io.inversion.json.*;
import io.inversion.json.JSNode;
import io.inversion.utils.Utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

public class MockAction extends Action<MockAction> {
    protected JSNode  json          = null;
    protected String  jsonUrl       = null;
    protected int     statusCode    = -1;
    protected String  status        = null;
    protected boolean cancelRequest = true;

    public MockAction() {

    }

    public MockAction(JSNode json) {
        withJson(json);
    }

    public MockAction(String status, JSNode json) {
        withStatus(status);
        withJson(json);
    }

    public MockAction(String name) {
        this(null, name, null);
    }


    public MockAction(String ruleMatcherSpec, String name, JSNode json) {
        if (name != null && json == null)
            json = new JSMap("name", name);

        withName(name);
        withIncludeOn(ruleMatcherSpec);
        withJson(json);
    }

    @Override
    public void run(Request req, Response res) throws ApiException {
        if (statusCode > 0)
            res.withStatus(status);

        if (status != null)
            res.withStatus(status);
        else if (statusCode < 0)
            withStatus(Status.SC_200_OK);

        JSNode json = getJson();

        if (json != null) {
            if (json instanceof JSList)
                res.withRecords(json.asList());
            else
                res.withJson(json);
        }

        if (cancelRequest)
            req.getChain().cancel();
    }

    public MockAction withJson(JSNode json) {
        this.json = json;
        return this;
    }

    public String getJsonUrl() {
        return jsonUrl;
    }

    public MockAction withJsonUrl(String jsonUrl) {
        this.jsonUrl = jsonUrl;
        return this;
    }

    public JSNode getJson() {
        if (json == null && jsonUrl != null) {
            InputStream stream = null;
            try {
                stream = new URL(jsonUrl).openStream();
            } catch (Exception ex) {
                //intentionally empty
            }

            if (stream == null) {
                stream = getClass().getResourceAsStream(jsonUrl);
            }

            if (stream == null) {
                stream = getClass().getClassLoader().getResourceAsStream(jsonUrl);
            }

            if (stream == null) {
                try {
                    File f = new File(jsonUrl);
                    if (f.exists())
                        stream = new BufferedInputStream(new FileInputStream(jsonUrl));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            if (stream != null) {
                json = JSReader.asJSNode(Utils.read(stream));
            } else {
                throw ApiException.new500InternalServerError("Unable to locate jsonUrl '{}'. Please check your configuration", jsonUrl);
            }

        }

        return json;

    }

    public int getStatusCode() {
        return statusCode;
    }

    public MockAction wihtStatusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public MockAction withStatus(String status) {
        this.status = status;
        return this;
    }

    public boolean isCancelRequest() {
        return cancelRequest;
    }

    public MockAction withCancelRequest(boolean cancelRequest) {
        this.cancelRequest = cancelRequest;
        return this;
    }
}
