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

import io.inversion.*;
import io.inversion.utils.Path;
import io.inversion.utils.Utils;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Getting-started
 * https://javadoc.io/doc/io.swagger.core.v3/swagger-models/latest/index.html
 * https://mvnrepository.com/artifact/io.swagger.core.v3
 * https://mvnrepository.com/artifact/io.swagger.core.v3/swagger-core
 * https://swagger.io/tools/open-source/open-source-integrations/
 * https://github.com/swagger-api/swagger-core
 * https://swagger.io/specification/
 */
public class OpenApiAction<A extends OpenApiAction> extends Action<A> {

    public OpenApiAction() {

    }


    public void doGet(Request req, Response res) throws ApiException {
        OpenAPI openApi = generateOpenApi(req);
        res.withText(Json.pretty(openApi));
    }

    protected OpenAPI generateOpenApi(Request req){
        OpenApiGenerator generator = new OpenApiGenerator();
        return generator.generateOpenApi(req);
    }


}
