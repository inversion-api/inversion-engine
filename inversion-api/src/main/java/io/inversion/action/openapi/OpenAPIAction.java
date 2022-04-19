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
package io.inversion.action.openapi;

import io.inversion.ApiException;
import io.inversion.Request;
import io.inversion.Response;
import io.inversion.action.misc.FileAction;
import io.inversion.json.JSList;
import io.inversion.json.JSNode;
import io.inversion.json.JSReader;
import io.inversion.utils.Path;
import io.inversion.utils.Utils;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.io.File;
import java.io.InputStream;


/**
 * https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Getting-started
 * https://javadoc.io/doc/io.swagger.core.v3/swagger-models/latest/index.html
 * https://mvnrepository.com/artifact/io.swagger.core.v3
 * https://mvnrepository.com/artifact/io.swagger.core.v3/swagger-core
 * https://swagger.io/tools/open-source/open-source-integrations/
 * https://github.com/swagger-api/swagger-core
 * https://swagger.io/specification/
 */
public class OpenAPIAction<A extends OpenAPIAction> extends FileAction<A> {

    protected String            templateBaseDir = "openapi/templates";
    protected String            patchesBaseDir  = "openapi/patches";
    protected String            outputBaseDir   = "openapi/output";
    protected OpenAPISpecWriter writer          = new OpenAPISpecWriter();

    protected boolean copyToOutputDirInDevMode = true;

    public OpenAPIAction() {
        withFiles("rapidoc.html", "openapi.json", "openapi.yml");
    }


    @Override
    public void doGet(Request req, Response res) throws ApiException {
        boolean filterMode = req.getOp() == null;
        String  file       = req.getUrl().getPath().last();
        if (canServe(file)) {
            if ("openapi.json".equalsIgnoreCase(file) || "openapi.yaml".equalsIgnoreCase(file)) {
                serveOpenApi(req, res, file);
                if (filterMode)
                    req.getChain().cancel();
            } else {
                super.serveFile(req, res);
            }
        }
    }

    void writeFile(Request req, Response res, String baseDir, String file, String content){
        String filePath = null;
        try {
            String apiName = req.getApi().getName();
            if (apiName == null)
                apiName = "UNNAMED";

            filePath = baseDir + "/" + apiName + "/" + file;
            filePath = filePath.replace("//", "/");

            File f = new File(filePath);
            f.getParentFile().mkdirs();

            Utils.write(f, content);
            log.warn("Writing debug file: {}", filePath);
        }
        catch(Exception ex){
            log.warn("Error writing out debug file to disk: " + filePath, ex);
        }
    }

    public void serveOpenApi(Request req, Response res, String file) {
        if (req.getApi() == null && !req.getEngine().matchApi(req))
            return;

        try {

            File debug = new File("./openapi.json");
            if(debug.exists()){
                JSNode json = (JSNode)JSReader.parseJson(debug.toURI().toURL().openStream());
                res.withJson(json);
                return;
            }



            OpenAPI openApi = generateOpenApi(req);
            JSNode  json    = writeOpenAPI(req, openApi);
            openApi = Json.mapper().readValue(json.toString(), OpenAPI.class);

            if (file.toLowerCase().endsWith(".yaml")) {
                res.withJson(null);
                res.withContentType("application/yaml");
                res.withText(Yaml.pretty(openApi));
            }
            else{
                res.withJson(json);
            }
            if(req.isDebug()){
                writeFile(req, res, outputBaseDir, "openapi.json", json.toString());
                writeFile(req, res, outputBaseDir, "openapi.yaml", Yaml.pretty(openApi));
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
        String  string  = Json.pretty(openApi);
        JSNode  json    = JSReader.asJSNode(string);
        JSList patches = findPatches(req);
        if (patches.size() > 0)
            json.patch(patches);
        return json;
    }

    public OpenAPI generateOpenApi(Request req) {
        OpenAPI openApi  = new OpenAPI();
        String  template = findTemplate(req);
        if (template != null) {
            SwaggerParseResult result = new OpenAPIParser().readContents(template, null, null);
            openApi = result.getOpenAPI();
        }
        return writer.writeOpenAPI(req, openApi);
    }


    public String findTemplate(Request req) {
        if (templateBaseDir != null) {
            Path path = req.getUrl().getPath();
            for (int i = path.size() - 1; i >= 0; i--) {
                String      pathString   = path.subpath(0, i).toString();
                String      templatePath = new Path(templateBaseDir, pathString, "openapi.json").toString();
                InputStream stream       = Utils.findInputStream(this, templatePath);
                if (stream == null) {
                    templatePath = new Path(templateBaseDir, pathString, "openapi.yaml").toString();
                    stream = Utils.findInputStream(this, templatePath);
                }
                if (stream != null)
                    return Utils.read(stream);
            }
        }
        return null;
    }

    public JSList findPatches(Request req) {
        JSList patches = new JSList();

        if (patchesBaseDir != null) {
            Path path = req.getUrl().getPath();
            for (int i = path.size() - 1; i >= 0; i--) {
                String pathString = path.subpath(0, i).toString();
                for (int j = 0; j <= 10; j++) {
                    String      patchPath = new Path(patchesBaseDir, pathString, "openapi.patch" + (j == 0 ? "" : ("." + j)) + ".json").toString();
                    InputStream stream    = Utils.findInputStream(this, patchPath);
                    if (stream != null) {
                        patches.add(JSReader.asJSNode(Utils.read(stream)));
                    }
                }
            }
        }
        return patches;
    }

    public String getTemplateBaseDir() {
        return templateBaseDir;
    }

    public OpenAPIAction withTemplateBaseDir(String templateBaseDir) {
        this.templateBaseDir = templateBaseDir;
        return this;
    }

    public String getPatchesBaseDir() {
        return patchesBaseDir;
    }

    public OpenAPIAction withPatchesBaseDir(String patchesBaseDir) {
        this.patchesBaseDir = patchesBaseDir;
        return this;
    }

    public String getOutputBaseDir() {
        return outputBaseDir;
    }

    public OpenAPIAction withOutputBaseDir(String outputBaseDir) {
        this.outputBaseDir = outputBaseDir;
        return this;
    }

    public OpenAPISpecWriter getWriter() {
        return writer;
    }

    public OpenAPIAction withWriter(OpenAPISpecWriter writer) {
        this.writer = writer;
        return this;
    }
}
