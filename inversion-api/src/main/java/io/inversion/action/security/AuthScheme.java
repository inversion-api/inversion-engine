/*
 * Copyright (c) 2015-2021 Rocket Partners, LLC
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
package io.inversion.action.security;

import io.inversion.*;

import java.util.ArrayList;
import java.util.List;

public abstract class AuthScheme {

    String name = null;
    String type = null;   //http,apiKey,openIdConnect,oauth2
    String scheme = null;  //basic,bearer
    String barerFormat = "JWT";
    String description = null;

    List<Parameter> parameters = new ArrayList();

    public abstract User getUser(Request req, Response res) throws ApiException;

    public String getName() {
        return name;
    }

    public AuthScheme withName(String name) {
        this.name = name;
        return this;
    }

    public String getType() {
        return type;
    }

    public AuthScheme withType(String type) {
        this.type = type;
        return this;
    }

    public String getScheme() {
        return scheme;
    }

    public AuthScheme withScheme(String scheme) {
        this.scheme = scheme;
        return this;
    }

    public String getBarerFormat() {
        return barerFormat;
    }

    public AuthScheme withBarerFormat(String barerFormat) {
        this.barerFormat = barerFormat;
        return this;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public AuthScheme withParameters(List<Parameter> parameters) {
        this.parameters = parameters;
        return this;
    }

    public AuthScheme withParameter(Parameter parameter) {
        if(parameter != null && !parameters.contains(parameter))
            this.parameters.add(parameter);
        return this;
    }

    public String getDescription() {
        return description;
    }

    public AuthScheme withDescription(String description) {
        this.description = description;
        return this;
    }
}
