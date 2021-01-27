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
import io.inversion.utils.JSArray;
import io.inversion.utils.JSNode;
import io.inversion.utils.Path;
import io.inversion.utils.Utils;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


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

    public interface OpenAPIWriterFactory {
        default OpenAPIWriter buildWriter() {
            return new OpenAPIWriter();
        }
    }

    protected String templateDir = null;
    protected String patchesDir = "patches";
    protected OpenAPIWriterFactory factory = new OpenAPIWriterFactory() {};

    public void doGet(Request req, Response res) throws ApiException {
        OpenAPI openApi = generateOpenApi(req);
        JSNode  json    = writeOpenAPI(req, openApi);
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

    /**
     * Override me to manually edit the OpenAPI pojo before it is serialized to JSON
     * OR to edit the JSNode model after it has been serialized.
     *
     * @param openApi
     * @return the JSNode representation of the OpenAPI JSON.
     */
    public JSNode writeOpenAPI(Request req, OpenAPI openApi) {
        String string = Json.pretty(openApi);
        JSNode json = JSNode.parseJsonNode(string);
        JSArray patches = findPatches(req);
        if(patches.size() > 0)
            json.patch(patches);
        return json;
    }

    public OpenAPI generateOpenApi(Request req) {
        OpenAPI openApi = new OpenAPI();
        String template = findTemplate(req);
        if(template != null){
            SwaggerParseResult result = new OpenAPIParser().readContents(template, null, null);
            openApi = result.getOpenAPI();
        }
        OpenAPIWriter generator = factory.buildWriter();
        return generator.writeOpenAPI(req, openApi);
    }


    public String findTemplate(Request req){
        if(templateDir != null) {
            Path path = req.getUrl().getPath();
            for (int i=path.size()-1; i>= 0; i--) {
                String pathString = path.subpath(0, i).toString();
                String templatePath = new Path(templateDir, pathString, "openapi.json").toString();
                InputStream stream = Utils.findInputStream(templatePath);
                if (stream == null) {
                    templatePath = new Path(templateDir, pathString, "openapi.yaml").toString();
                    stream = Utils.findInputStream(templatePath);
                }
                if (stream != null)
                    return Utils.read(stream);
            }
        }
        return null;
    }

    public JSArray findPatches(Request req){
        JSArray patches = new JSArray();

        if(patchesDir != null) {
            Path path = req.getUrl().getPath();
            for (int i=path.size()-1; i>= 0; i--) {
                String pathString = path.subpath(0, i).toString();
                for (int j = 0; j <= 10; j++) {
                    String patchPath = new Path(patchesDir, pathString, "openapi.patch" + (j == 0 ? "" : ("." + j)) + ".json").toString();
                    InputStream stream = Utils.findInputStream(patchPath);
                    if (stream != null) {
                        patches.add(JSNode.parseJsonNode(Utils.read(stream)));
                    }
                }
            }
        }
        return patches;
    }

    public String getTemplateDir() {
        return templateDir;
    }

    public OpenAPIAction withTemplateDir(String templateDir) {
        this.templateDir = templateDir;
        return this;
    }

    public String getPatchesDir() {
        return patchesDir;
    }

    public OpenAPIAction withPatchesDir(String patchesDir) {
        this.patchesDir = patchesDir;
        return this;
    }

    public OpenAPIWriterFactory getFactory() {
        return factory;
    }

    public OpenAPIAction withFactory(OpenAPIWriterFactory factory) {
        this.factory = factory;
        return this;
    }

}
