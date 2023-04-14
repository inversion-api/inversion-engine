/*
 * Copyright (c) 2015-2022 Rocket Partners, LLC
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

package io.inversion.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.inversion.utils.Utils;

public class JSNodeDeserializer extends StdDeserializer<JSNode> {

    public JSNodeDeserializer() {
        this(null);
    }

    public JSNodeDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public JSNode deserialize(JsonParser jp, DeserializationContext ctxt) {
        try {
            return JSParser.parseJson(jp, null);
        } catch (Exception ex) {
            Utils.rethrow(ex);
            return null;
        }
    }
}
