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
package io.inversion.action.openapi;

import io.inversion.Collection;
import io.inversion.*;
import io.inversion.action.security.AuthScheme;
import io.inversion.utils.Path;
import io.inversion.utils.Task;
import io.inversion.utils.Utils;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariable;
import io.swagger.v3.oas.models.servers.ServerVariables;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.io.InputStream;
import java.util.*;

public class OpenAPISpecWriter implements OpenAPIWriter<OpenAPISpecWriter> {

    protected List<String> ignoredSuffixes = Utils.add(new ArrayList(), ".json", ".yaml", ".html", ".xml");


    protected String getDescription() {
        InputStream is = Utils.findInputStream(this, "description.md");
        if(is != null)
            return Utils.read(is);
        return "";
    }


    public OpenAPI writeOpenAPI(Request req, OpenAPI openApi) throws ApiException {

        List<Op> opsToDoc = new ArrayList(req.getApi().getOps());

        //-- remove any endpoints that end with the ignoredSuffixes
        opsToDoc.removeIf(o -> ignoredSuffixes.parallelStream().anyMatch(suffix -> o.getPath().toString().endsWith(suffix)));

        Map<Object, Schema> schemas = new LinkedHashMap();

        documentInfo(openApi, req);
        documentServers(openApi, opsToDoc, req);
        documentSchemas(openApi, req);
        documentPathItems(openApi, opsToDoc);
        documentOperations(openApi, opsToDoc, schemas);
        //documentSecurity(openApi, opsToDoc);

        removeInternalOps(openApi, opsToDoc);
        return openApi;
    }

    //TODO: change this so internal ops are not documented in the first place...
    //...requires related functions to internal endpoints be able to lazy create
    //their ops...need to think about this more
    protected void removeInternalOps(OpenAPI openApi, List<Op> ops) {
        for (Op op : ops) {
            if (op.isInternal()) {
                switch (op.getMethod().toUpperCase()) {
                    case "GET":
                        openApi.getPaths().get(op.getOperationPath()).setGet(null);
                        break;
                    case "POST":
                        openApi.getPaths().get(op.getOperationPath()).setPost(null);
                        break;
                    case "PUT":
                        openApi.getPaths().get(op.getOperationPath()).setPut(null);
                        break;
                    case "PATCH":
                        openApi.getPaths().get(op.getOperationPath()).setPatch(null);
                        break;
                    case "DELETE":
                        openApi.getPaths().get(op.getOperationPath()).setDelete(null);
                        break;
                }
            }
        }
    }

    protected void documentInfo(OpenAPI openApi, Request req) {
        Info info = openApi.getInfo();
        if (info == null) {
            info = new Info();
            openApi.setInfo(info);
        }

        if (info.getVersion() == null) {
            String version = req.getApi().getVersion();
            if (version == null)
                version = "0.1";
            info.setVersion(version);
        }
        if (info.getTitle() == null)
            info.setTitle(req.getApi().getName());

        if (info.getDescription() == null) {
            info.setDescription(getDescription());
        }
    }

    protected void documentServers(OpenAPI openApi, List<Op> ops, Request req) {
        if (openApi.getServers() == null) {
            List<Server> apiServers = new ArrayList();

            ArrayListValuedHashMap<io.inversion.Server, Path> serversMap = new ArrayListValuedHashMap<>();
            Api                                               api        = ops.get(0).getApi();
            for (io.inversion.Server server : api.getServers()) {
                serversMap.putAll(server, server.getAllIncludePaths());
            }

            for (io.inversion.Server server : serversMap.keySet()) {
                List<String> urls = server.getUrls();
                if (urls.size() == 0) {
                    String host = req.getUrl().toString();
                    host = host.substring(0, host.indexOf("/", 8));
                    urls.add(host);
                }
                for (String host : urls) {
                    List<Path> serverPaths = new ArrayList<>(new LinkedHashSet(serversMap.get(server)));
                    //Collections.sort(serverPaths);
                    for (Path path : serverPaths) {
                        Server apiServer = new Server();
                        apiServers.add(apiServer);

                        if (server.getDescription() != null) {
                            apiServer.setDescription(server.getDescription());
                        }

                        path = path.copy();
                        path.removeTrailingWildcard();
                        apiServer.setUrl(host + "/" + path);

                        if (server.getParams().size() > 0) {
                            ServerVariables variables = apiServer.getVariables();
                            if (variables == null) {
                                variables = new ServerVariables();
                                apiServer.setVariables(variables);
                            }
                            for (Param param : server.getParams()) {
                                String key  = param.getKey();
                                String desc = param.getDescription();

                                ServerVariable var = new ServerVariable();

                                String defaultValue = req.findParam(key, Param.In.SERVER_PATH);
                                if (!Utils.empty(defaultValue))
                                    var.setDefault(defaultValue);

                                if(!Utils.empty(desc))
                                    var.setDescription(desc);

                                variables.put(key, var);
                            }
                        }
                    }
                }


            }

            openApi.setServers(apiServers);
        }
    }

    protected void documentSchemas(OpenAPI openApi, Request req) {
        Components comps = openApi.getComponents();
        if (comps == null) {
            comps = new Components();
            openApi.setComponents(comps);
        }

        Map<String, Schema> schemas = comps.getSchemas();
        if (schemas == null) {
            schemas = new HashMap<>();
            comps.setSchemas(schemas);
        }

        documentErrorSchema(openApi);
    }

//    protected void documentSecurity(OpenAPI openApi, List<Op> opsToDoc) {
//        Components                  components      = openApi.getComponents();
//        Map<String, SecurityScheme> securitySchemes = new HashMap();
//
//        boolean allTheSame  = true;
//        String  lastSecReqs = null;
//
//        for (Operation doc : opsToDoc) {
//
//            if (doc.operation == null)
//                continue;
//
//            List<SecurityRequirement> opSecurity = new ArrayList();
//
//            for (Chain.ActionMatch match : doc.req.getActionMatches()) {
//                Action action = match.getAction();
//                if (action instanceof AuthAction) {
//                    List<AuthScheme> schemes = ((AuthAction) action).getAuthSchemes();
//
//
//                    for (AuthScheme ss : schemes) {
//                        if (ss instanceof BearerScheme) {
//                            String              name   = ss.getName();
//                            SecurityRequirement secReq = new SecurityRequirement();
//                            opSecurity.add(secReq);
//                            secReq.addList(name);
//
//                            if (!securitySchemes.containsKey(name)) {
//                                SecurityScheme oasSS = new SecurityScheme();
//                                oasSS.setDescription(getDescription(ss, null));
//                                oasSS.setType(SecurityScheme.Type.HTTP);
//                                oasSS.setScheme(ss.getScheme());
//                                oasSS.setBearerFormat(ss.getBarerFormat());
//                                securitySchemes.put(name, oasSS);
//                            }
//                        } else if (ss instanceof ApiKeyScheme) {
//
//                            SecurityRequirement secReq = new SecurityRequirement();
//                            opSecurity.add(secReq);
//
//                            for (Param param : ss.getParameters()) {
//                                String name = ss.getName() + "_" + param.getName();
//                                secReq.addList(name);
//
//                                if (!securitySchemes.containsKey(name)) {
//                                    SecurityScheme oasSS = new SecurityScheme();
//                                    oasSS.setDescription(getDescription(ss, param));
//                                    oasSS.setType(Type.APIKEY);
//                                    oasSS.setName(param.getName());
//
//                                    String in = param.getIn();
//                                    if (in.equalsIgnoreCase("header"))
//                                        oasSS.setIn(In.HEADER);
//                                    if (in.equalsIgnoreCase("query"))
//                                        oasSS.setIn(In.QUERY);
//                                    if (in.equalsIgnoreCase("cookie"))
//                                        oasSS.setIn(In.COOKIE);
//
//                                    oasSS.setDescription(getDescription(ss, param));
//                                    securitySchemes.put(name, oasSS);
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//
//            if (lastSecReqs == null) {
//                lastSecReqs = opSecurity.toString();
//            } else if (allTheSame) {
//                String newSecReqs = opSecurity.toString();
//                if (!lastSecReqs.equals(newSecReqs)) {
//                    allTheSame = false;
//                }
//            }
//            if (opSecurity.size() > 0)
//                doc.securityRequirements = opSecurity;
//        }
//
//        //-- don't overwrite something that a potentially overridden Operation construction method set.
//        Map oasSs = components.getSecuritySchemes();
//        if (oasSs == null)
//            oasSs = new LinkedHashMap();
//
//        for (String key : securitySchemes.keySet()) {
//            if (!oasSs.containsKey(key))
//                oasSs.put(key, securitySchemes.get(key));
//        }
//        components.setSecuritySchemes(oasSs);
//
//        if (allTheSame && opsToDoc.size() > 0) {
//            openApi.setSecurity(opsToDoc.get(0).securityRequirements);
//        } else {
//            for (Operation op : opsToDoc) {
//                if (op.operation == null)
//                    continue;
//                if (op.operation.getSecurity() == null)
//                    op.operation.setSecurity(op.securityRequirements);
//            }
//        }
//    }

    protected String getDescription(AuthScheme scheme, Param param) {
        String schemeDesc = scheme.getDescription();
        String paramDesc  = param == null ? null : param.getDescription();
        if (schemeDesc == null && paramDesc == null)
            return null;

        if (schemeDesc != null && paramDesc != null)
            return schemeDesc + " " + paramDesc;
        if (schemeDesc != null)
            return schemeDesc;
        return paramDesc;
    }


    protected void documentErrorSchema(OpenAPI openApi) {
        if (openApi.getComponents().getSchemas().get("error") != null)
            return;

        Schema schema = new Schema();
        openApi.getComponents().addSchemas("error", schema);

        schema.addProperties("status", newTypeSchema("string", null));
        schema.addProperties("message", newTypeSchema("string", null));
        schema.addProperties("error", newTypeSchema("string", null));
    }
//
//    protected void documentCollectionLinksSchema(OpenAPI openApi) {
//        if (openApi.getComponents().getSchemas().get("_links") != null)
//            return;
//
//        Schema schema = new Schema();
//        openApi.getComponents().addSchemas("_links", schema);
//
//        schema.addProperties("self", newHrefSchema());
//        schema.addProperties("first", newHrefSchema());
//        schema.addProperties("prev", newHrefSchema());
//        schema.addProperties("next", newHrefSchema());
//        schema.addProperties("last", newHrefSchema());
//        schema.addProperties("after", newHrefSchema());
//    }
//
//
//
//    protected String documentRequestSchema(OpenAPI openApi, Operation doc) {
//        String schemaName = doc.requestSchema;
//
//        if (openApi.getComponents().getSchemas().get(schemaName) != null)
//            return schemaName;
//
//        Schema schema = new Schema();
//        openApi.getComponents().addSchemas(schemaName, schema);
//        buildResourceSchema(openApi, doc, schema, true);
//
//        return schemaName;
//    }
//

    //
//
//
//    protected Schema buildResourceSchema(OpenAPI openApi, Operation doc, Schema schema, boolean isRequest) {
//
//        Collection coll = doc.req.getCollection();
//
//        if (coll.getSchemaRef() != null) {
//            return newComponentRefSchema(coll.getSchemaRef());
//
//        } else {
//
//            if (coll.getDescription() != null) {
//                schema.setDescription(coll.getDescription());
//            }
//            schema.setType("object");
//
//            List<String> requiredProps = new ArrayList<>();
//            Index        primaryIndex  = coll.getPrimaryIndex();
//            if (primaryIndex != null)
//                requiredProps.addAll(primaryIndex.getJsonNames());
//
//
//            //-- TODO filter excludes/includes
//            for (Property prop : coll.getProperties()) {
//                String name = prop.getJsonName();
//                String type = prop.getJsonType();
//
//                Schema propSchema = newTypeSchema(type);
//                if (prop.getDescription() != null)
//                    propSchema.setDescription(prop.getDescription());
//
////                if (coll.getPrimaryIndex() == null || coll.getPrimaryIndex().getProperties().contains(prop)) {
////                    propSchema.setReadOnly(true);
////                }
//
//                if (prop.isNullable()) {
//                    propSchema.setNullable(true);
//                }
//
//                schema.addProperties(name, propSchema);
//
//                if (prop.isRequired() && !requiredProps.contains(name))
//                    requiredProps.add(name);
//            }
//            schema.setRequired(requiredProps);
//
//
//            for(Relationship rel : coll.getRelationships()){
//                for(Operation temp : opsToDoc){
//                    if(temp.function.equalsIgnoreCase(doc.function) && temp.req.getCollection() == rel.getRelated()){
//
//                        String childSchema = isRequest ? documentRequestSchema(openApi, temp) : documentResponseSchema(openApi, temp);
//
//                        if(rel.isManyToOne()){
//                            schema.addProperties(rel.getName(), newComponentRefSchema(childSchema));
//                        }
//                        else{
//                            ArraySchema arr = new ArraySchema();
//                            arr.setItems(newComponentRefSchema(childSchema));
//                            schema.addProperties(rel.getName(), arr);
//                        }
//
//                        break;
//                    }
//                }
//            }
//
//            return schema;
//        }
//    }
//
//    protected Schema buildCollectionSchema(OpenAPI openApi, Operation doc) {
//
//        Schema schema = new Schema();
//        Collection coll = doc.req.getCollection();
//        boolean useHal   = hasAction(doc, HALAction.class);
//        boolean useLinks = !useHal && hasAction(doc, LinksAction.class);
//
//        if (useHal) {
//
//            documentCollectionLinksSchema(openApi);
//
//            schema.addProperties("_links", newComponentRefSchema("_links"));
//            schema.addProperties("page", newTypeSchema("number"));
//            schema.addProperties("size", newTypeSchema("number"));
//            schema.addProperties("total", newTypeSchema("number"));
//
//            ArraySchema embedded = new ArraySchema();
//
//            //TODO: what about the cases where you don't have a related GET???
//            embedded.setItems(newComponentRefSchema(documentRequestSchema(openApi, doc.resourceGet)));
//            schema.addProperties("_embedded", embedded);
//        } else if (useLinks) {
//            //TODO:
//        } else {
//            //TODO:
//        }
//        return schema;
//    }
//
//
//
//    protected boolean hasAction(Operation doc, Class... targets) {
//        for (Chain.ActionMatch match : doc.req.getActionMatches()) {
//            for (Class target : targets) {
//                if (target.isAssignableFrom(match.getAction().getClass()))
//                    return true;
//            }
//        }
//        return false;
//    }
//
//
    protected void documentPathItems(OpenAPI openApi, List<Op> opsToDoc) {

        if (openApi.getPaths() == null)
            openApi.setPaths(new Paths());//--prevents an NPE for uninitialized paths

        for (Op op : opsToDoc) {
            String   path     = op.getOperationPath();
            PathItem pathItem = openApi.getPaths().get(path);
            if (pathItem == null) {
                documentPathItem(openApi, op);
            }
        }
    }

    protected void documentPathItem(OpenAPI openApi, Op op) {

        PathItem pathItem = openApi.getPaths().get(op.getPath());
        if (pathItem == null) {
            pathItem = new PathItem();
            openApi.getPaths().addPathItem(op.getOperationPath(), pathItem);
        }


        String operationPath = op.getOperationPath();
        Path   pathMatch     = op.getPath();
        //Request req           = op.req;


        Path path = new Path(operationPath);

        for (int i = 0; i < path.size(); i++) {
            String name = path.get(i);

            if (path.isVar(i)) {
                name = path.getVarName(i);//--this looks wrong but is because of the different path encodings of OpenApi paths to Inversion paths: TODO: fix this difference probably in breaking change to Inversion syntax
                if (name == null)
                    name = path.getRegex(i);

                Schema schema = null;
                if (op.getCollection() != null) {
                    Collection coll = op.getCollection();
                    Property   prop = coll.getProperty(name);
                    if (prop != null) {
                        String type = prop.getJsonType();
                        if (type.equalsIgnoreCase("number"))
                            schema = newTypeSchema("number", null);
                        if (type.equalsIgnoreCase("boolean"))
                            schema = newTypeSchema("boolean", null);
                    }
                }

                if (schema == null) {
                    schema = newTypeSchema("string", null);
                    String regex = pathMatch.getRegex(i);
                    if (regex != null)
                        schema.setPattern(regex);
                }

                Parameter param = new Parameter();
                param.setName(name);
                param.setIn("path");
                param.setRequired(true);
                param.setSchema(schema);

                boolean hasParam = false;
                if (pathItem.getParameters() != null) {
                    for (Parameter existing : pathItem.getParameters()) {
                        if (name.equalsIgnoreCase(existing.getName()) && param.getIn().equalsIgnoreCase(existing.getIn())) {
                            hasParam = true;
                            break;
                        }
                    }
                }
                if (!hasParam)
                    pathItem.addParametersItem(param);
            }
        }
    }

    protected void documentOperations(OpenAPI openApi, List<Op> ops, Map<Object, Schema> schemas) {

        for (Op op : ops) {
            List<OpenAPIWriter> writers = new ArrayList();
            writers.add(this);

            List actions = new ArrayList(op.getActions());
            actions.removeIf(o -> !(o instanceof OpenAPIWriter));

            writers.addAll(actions);

            Task.buildTask(writers, "hook_documentOp", openApi, ops, op, schemas).go();
        }
    }

    public Operation hook_documentOp(Task docChain, OpenAPI openApi, List<Op> ops, Op op, Map<Object, Schema> schemas) {
        return OpenAPIWriter.super.hook_documentOp(docChain, openApi, ops, op, schemas);
//        PathItem pathItem = openApi.getPaths().get(op.getPath());
//        if (pathItem != null) {
//            Operation op = pathItem.getGet();
//        }
//
//            Collection collection = req.getCollection();
//            if (collection != null) {
//                op.addTagsItem(beautifyTag(collection.getSingularDisplayName()));
//            }
    }


}
