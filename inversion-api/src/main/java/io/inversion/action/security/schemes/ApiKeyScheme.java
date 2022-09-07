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
package io.inversion.action.security.schemes;

import io.inversion.Param;
import io.inversion.Request;
import io.inversion.action.security.AuthScheme;

import javax.naming.OperationNotSupportedException;
import java.util.List;

public abstract class ApiKeyScheme extends AuthScheme {

    protected Param.In in  = Param.In.HEADER;
    protected String   key = "X-API-KEY";

    public ApiKeyScheme() {
        withType(AuthSchemeType.apiKey);
    }

    public synchronized List<Param> getParams(){
        if(super.getParams().size() == 0){
            Param p = new Param();
            p.withIn(in);
            p.withKey(key);
            withParam(p);
        }
        return super.getParams();
    }

    protected String getApiKey(Request req){
        return req.findParam(key, in);
    }

    public Param.In getIn() {
        return in;
    }

    public ApiKeyScheme withIn(Param.In in) {
        this.in = in;
        return this;
    }

    public String getKey() {
        return key;
    }

    public ApiKeyScheme withKey(String key) {
        this.key = key;
        return this;
    }
}
