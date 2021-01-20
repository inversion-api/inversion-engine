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
package io.inversion.openapi.v3;

import io.inversion.Action;
import io.inversion.ApiException;
import io.inversion.Request;
import io.inversion.Response;
import io.inversion.utils.JSNode;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;

/**
 * https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Getting-started
 * https://javadoc.io/doc/io.swagger.core.v3/swagger-models/latest/index.html
 * https://mvnrepository.com/artifact/io.swagger.core.v3
 * https://mvnrepository.com/artifact/io.swagger.core.v3/swagger-core
 * https://swagger.io/tools/open-source/open-source-integrations/
 * https://github.com/swagger-api/swagger-core
 * https://swagger.io/specification/
 */
public class OpenAPIAction<A extends OpenAPIAction> extends Action<A> {

    protected OpenAPIWriterFactory factory = new OpenAPIWriterFactory() {
    };


    public void doGet(Request req, Response res) throws ApiException {
        OpenAPI openApi = generateOpenApi(req);
        JSNode  json    = writeOpenAPI(openApi);
        res.withJson(json);


        try {
            if (req.getEndpointPath().toString().toLowerCase().endsWith(".yaml")) {
                openApi = Json.mapper().readValue(json.toString(), OpenAPI.class);
                res.withJson(null);
                res.withContentType("application/yaml");
                res.withText(Yaml.pretty(openApi));
            }
        } catch (Exception ex) {
            throw ApiException.new500InternalServerError(ex);
        }

    }

    public JSNode writeOpenAPI(OpenAPI openApi) {
        String json = Json.pretty(openApi);
        System.out.println(json);
        return JSNode.parseJsonNode(json);
    }

    public OpenAPI generateOpenApi(Request req) {
        OpenAPIWriter generator = factory.buildWriter();
        return generator.writeOpenAPI(req);
    }

    public interface OpenAPIWriterFactory {
        default OpenAPIWriter buildWriter() {
            return new OpenAPIWriter();
        }
    }
}
