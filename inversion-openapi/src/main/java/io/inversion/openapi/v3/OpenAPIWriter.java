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
package io.inversion.openapi.v3;

import io.inversion.Collection;
import io.inversion.*;
import io.inversion.Operation;
import io.inversion.action.hateoas.HALAction;
import io.inversion.action.hateoas.LinksAction;
import io.inversion.action.security.AuthAction;
import io.inversion.action.security.AuthScheme;
import io.inversion.action.security.schemes.ApiKeyScheme;
import io.inversion.action.security.schemes.BearerScheme;
import io.inversion.utils.Path;
import io.inversion.utils.Utils;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

import java.util.*;

import static io.swagger.v3.oas.models.security.SecurityScheme.In;
import static io.swagger.v3.oas.models.security.SecurityScheme.Type;

public class OpenAPIWriter {

    protected List<String> ignoredEndpointTokens = Utils.add(new ArrayList(), ".json", ".yaml", ".html", ".xml");
    List<Operation> opsToDoc = new ArrayList();

    protected String getDescription() {
        return Utils.read(Utils.findInputStream("description.md"));
    }

    public OpenAPI writeOpenAPI(Request req, OpenAPI openApi) throws ApiException {
//
//        buildOpsToDoc(req);
//
//        documentInfo(openApi, req);
//        documentServers(openApi, req);
//        documentSchemas(openApi, req);
//        documentPathItems(openApi, opsToDoc);
//        documentOperations(openApi, opsToDoc);
//        documentSecurity(openApi, opsToDoc);
//
        return openApi;
    }

//    protected void documentInfo(OpenAPI openApi, Request req) {
//        Info info = openApi.getInfo();
//        if (info == null) {
//            info = new Info();
//            openApi.setInfo(info);
//        }
//
//        if (info.getVersion() == null) {
//            String version = req.getApi().getVersion();
//            if (version == null)
//                version = "1";
//            info.setVersion(version);
//        }
//        if (info.getTitle() == null)
//            info.setTitle(req.getApi().getName());
//
//        if(info.getDescription() == null){
//            info.setDescription(getDescription());
//        }
//    }
//
//    protected void documentServers(OpenAPI openApi, Request req) {
//        if (openApi.getServers() == null) {
//            Server server = new Server();
//            String url    = req.getApiPath().toString();
//            server.setUrl("/" + url);
//            openApi.setServers(Utils.add(new ArrayList(), server));
//        }
//    }
//
//    protected void documentSchemas(OpenAPI openApi, Request req) {
//
//        Components comps = openApi.getComponents();
//        if (comps == null) {
//            comps = new Components();
//            openApi.setComponents(comps);
//        }
//
//        Map<String, Schema> schemas = comps.getSchemas();
//        if (schemas == null) {
//            schemas = new HashMap<>();
//            comps.setSchemas(schemas);
//        }
//
//        documentErrorSchema(openApi);
//
////        //TODO: change this to HAL Links
////        //TODO: inspect optodoc to make sure we are using HAL
////        documentCollectionLinksSchema(openApi);
////
////        for (Collection coll : req.getApi().getCollections()) {
////            if(ignoreCollection(coll))
////                continue;
////
////            //TODO: only document collections with an optodoc
////            documentResourceSchemas(openApi, coll);
////            documentCollectionSchemas(openApi, coll);
////        }
//    }
//
//    protected void documentSecurity(OpenAPI openApi, List<Operation> opsToDoc) {
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
//                                oasSS.setType(Type.HTTP);
//                                oasSS.setScheme(ss.getScheme());
//                                oasSS.setBearerFormat(ss.getBarerFormat());
//                                securitySchemes.put(name, oasSS);
//                            }
//                        } else if (ss instanceof ApiKeyScheme) {
//
//                            SecurityRequirement secReq = new SecurityRequirement();
//                            opSecurity.add(secReq);
//
//                            for (io.inversion.Parameter param : ss.getParameters()) {
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
//            for (Operation opToDoc : opsToDoc) {
//                if (opToDoc.operation == null)
//                    continue;
//                if (opToDoc.operation.getSecurity() == null)
//                    opToDoc.operation.setSecurity(opToDoc.securityRequirements);
//            }
//        }
//    }
//
//    protected String getDescription(AuthScheme scheme, io.inversion.Parameter param) {
//        String schemeDesc = scheme.getDescription();
//        String paramDesc  = param == null ? null : param.getDescription();
//        if (schemeDesc == null && paramDesc == null)
//            return null;
//
//        if (schemeDesc != null && paramDesc != null)
//            return schemeDesc + " " + paramDesc;
//        if (schemeDesc != null)
//            return schemeDesc;
//        return paramDesc;
//    }
//
//
//    protected void documentErrorSchema(OpenAPI openApi) {
//        if (openApi.getComponents().getSchemas().get("error") != null)
//            return;
//
//        Schema schema = new Schema();
//        openApi.getComponents().addSchemas("error", schema);
//
//        schema.addProperties("status", newTypeSchema("string"));
//        schema.addProperties("message", newTypeSchema("string"));
//        schema.addProperties("error", newTypeSchema("string"));
//    }
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
//    protected String documentResponseSchema(OpenAPI openApi, Operation doc) {
//
//        String schemaName = doc.responseSchema;
//        if (openApi.getComponents().getSchemas().get(schemaName) != null)
//            return schemaName;
//
//        boolean isListing = Utils.in(doc.function.toLowerCase(Locale.ROOT), "list", "related");
//
//        Collection coll = doc.req.getCollection();
//        boolean useHal   = hasAction(doc, HALAction.class);
//        boolean useLinks = !useHal && hasAction(doc, LinksAction.class);
//
//
//        if(isListing){
//            Schema schema = new Schema();
//            openApi.getComponents().addSchemas(schemaName, schema);
//
//            if (useHal) {
//
//                documentCollectionLinksSchema(openApi);
//
//                schema.addProperties("_links", newComponentRefSchema("_links"));
//                schema.addProperties("page", newTypeSchema("number"));
//                schema.addProperties("size", newTypeSchema("number"));
//                schema.addProperties("total", newTypeSchema("number"));
//
//                ArraySchema embedded = new ArraySchema();
//
//                //TODO: what about the cases where you don't have a related GET???
//                embedded.setItems(newComponentRefSchema(documentResponseSchema(openApi, doc.resourceGet)));
//                schema.addProperties("_embedded", embedded);
//            } else if (useLinks) {
//                //TODO:
//            } else {
//                //TODO:
//            }
//        }
//        else{
//            Schema schema = new Schema();
//            openApi.getComponents().addSchemas(schemaName, schema);
//
//            if (useHal) {
//                Schema links = new Schema();
//                links.addProperties("self", newHrefSchema());
//
//                for (Relationship rel : coll.getRelationships()) {
//                    links.addProperties(rel.getName(), newHrefSchema());
//                }
//                schema.addProperties("_links", links);
//            } else if (useLinks) {
//                //TODO:
//            } else {
//                //TODO:
//            }
//
//
//            buildResourceSchema(openApi, doc, schema, false);
//
//        }
//
//
//        return schemaName;
//    }
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
//    protected void documentPathItems(OpenAPI openApi, List<Operation> opsToDoc) {
//
//        if (openApi.getPaths() == null)
//            openApi.setPaths(new Paths());//--prevents an NPE for uninitialized paths
//
//        for (Operation opToDoc : opsToDoc) {
//            String   path     = opToDoc.operationPath;
//            PathItem pathItem = openApi.getPaths().get(path);
//            if (pathItem == null) {
//                documentPathItem(openApi, opToDoc);
//            }
//        }
//    }
//
//    protected void documentPathItem(OpenAPI openApi, Operation opToDoc) {
//
//        PathItem pathItem = openApi.getPaths().get(opToDoc.operationPath);
//        if (pathItem == null) {
//            pathItem = new PathItem();
//            openApi.getPaths().addPathItem(opToDoc.operationPath, pathItem);
//        }
//
//
//        String  operationPath = opToDoc.operationPath;
//        Path    pathMatch     = opToDoc.matchPath;
//        Request req           = opToDoc.req;
//
//
//        Path path = new Path(operationPath);
//
//        for (int i = 0; i < path.size(); i++) {
//            String name = path.get(i);
//
//            if (path.isVar(i)) {
//                name = path.getVarName(i);//--this looks wrong but is because of the different path encodings of OpenApi paths to Inversion paths: TODO: fix this difference probably in breaking change to Inversion syntax
//                if (name == null)
//                    name = path.getRegex(i);
//
//                Schema schema = null;
//                if (req.getCollection() != null) {
//                    Collection coll = req.getCollection();
//                    Property   prop = coll.getProperty(name);
//                    if (prop != null) {
//                        String type = prop.getJsonType();
//                        if (type.equalsIgnoreCase("number"))
//                            schema = newTypeSchema("number");
//                        if (type.equalsIgnoreCase("boolean"))
//                            schema = newTypeSchema("boolean");
//                    }
//                }
//
//                if (schema == null) {
//                    schema = newTypeSchema("string");
//                    String regex = pathMatch.getRegex(i);
//                    if (regex != null)
//                        schema.setPattern(regex);
//                }
//
//                Parameter param = new Parameter();
//                param.setName(name);
//                param.setIn("path");
//                param.setRequired(true);
//                param.setSchema(schema);
//
//                boolean hasParam = false;
//                if (pathItem.getParameters() != null) {
//                    for (Parameter existing : pathItem.getParameters()) {
//                        if (name.equalsIgnoreCase(existing.getName()) && param.getIn().equalsIgnoreCase(existing.getIn())) {
//                            hasParam = true;
//                            break;
//                        }
//                    }
//                }
//                if (!hasParam)
//                    pathItem.addParametersItem(param);
//            }
//        }
//    }
//
//    protected void documentOperations(OpenAPI openApi, List<Operation> opsToDoc) {
//        for (Operation opToDoc : opsToDoc) {
//            documentOperation(openApi, opsToDoc, opToDoc);
//        }
//    }
//
//    protected void documentOperation(OpenAPI openApi, List<Operation> opsToDoc, Operation opToDoc) {
//        switch (opToDoc.function.toLowerCase()) {
//            case "get":
//                documentGet(openApi, opToDoc);
//                break;
//            case "list":
//                documentList(openApi, opToDoc);
//                break;
//            case "related":
//                documentRelated(openApi, opsToDoc, opToDoc);
//                break;
//            case "post":
//                documentPost(openApi, opToDoc);
//                break;
////            case "batch_post":
////                documentBatchPost(openApi, opToDoc);
////                break;
//            case "put":
//                documentPut(openApi, opToDoc);
//                break;
////            case "batch_put":
////                documentBatchPut(openApi, opToDoc);
////                break;
//            case "patch":
//                documentPatch(openApi, opToDoc);
//                break;
//            case "delete":
//                documentDelete(openApi, opToDoc);
//                break;
////            case "batch_delete":
////                documentBatchDelete(openApi, opToDoc);
////                break;
//        }
//
//        PathItem pathItem = openApi.getPaths().get(opToDoc.operationPath);
//        if (pathItem != null) {
//            io.swagger.v3.oas.models.Operation op = pathItem.getGet();
//        }
//
////            Collection collection = req.getCollection();
////            if (collection != null) {
////                op.addTagsItem(beautifyTag(collection.getSingularDisplayName()));
////            }
//    }
//
//    protected void documentGet(OpenAPI openApi, Operation opToDoc) {
//
//        if (opToDoc.req.getCollection() == null)
//            return;
//
//        Collection coll = opToDoc.req.getCollection();
//
//        String description = "A specific " + coll.getSingularDisplayName() + " object";
//        String schemaName  = documentResponseSchema(openApi, opToDoc);
//
//
//        io.swagger.v3.oas.models.Operation op = openApi.getPaths().get(opToDoc.operationPath).getGet();
//        if (op == null) {
//            op = buildOperation(opToDoc, description, null, "200", schemaName);
//            openApi.getPaths().get(opToDoc.operationPath).setGet(op);
//        }
//
//        withParams(op, opToDoc, "include", "exclude", "expand", "collapse");
//        withResponse(op, opToDoc, "404");
//    }
//
//    protected void documentList(OpenAPI openApi, Operation opToDoc) {
//
//        if (opToDoc.req.getCollection() == null)
//            return;
//
//        Collection coll = opToDoc.req.getCollection();
//
//        String description = "A pageable list of all " + coll.getSingularDisplayName() + " resources the user has access to and also match any query parameters.  The list may be empty.";
//        String schemaName  = documentResponseSchema(openApi, opToDoc);
//
//        io.swagger.v3.oas.models.Operation op = openApi.getPaths().get(opToDoc.operationPath).getGet();
//        if (op == null) {
//            op = buildOperation(opToDoc, description, null, "200", schemaName);
//            openApi.getPaths().get(opToDoc.operationPath).setGet(op);
//        }
//
//        withParams(op, opToDoc, "page", "size", "sort", "q", "include", "exclude", "expand");
//    }
//
//    protected void documentRelated(OpenAPI openApi, List<Operation> opsToDoc, Operation opToDoc) {
//
////        if (opToDoc.req.getCollection() == null)
////            return;
////
////        Collection parent = opToDoc.req.getCollection();
////        Collection related = opToDoc.req.getRelationship().getRelated();
////
////        String description = "Retrieves all of the " + related.getPluralDisplayName() + " related to the " + parent.getSingularDisplayName();
////        String schemaName = documentCollectionSchema(openApi, parent);
////
////        Operation op = openApi.getPaths().get(opToDoc.operationPath).getGet();
////        if(op == null){
////            op = buildOperation(opToDoc, description, "200", schemaName);
////            openApi.getPaths().get(opToDoc.operationPath).setGet(op);
////            withParams(op, opToDoc,"page", "size", "sort", "q", "include", "exclude", "expand");
////        }
////
////        //-- adds this relationship to the {collection}/{resource} endpoint parent
////        for(OpToDoc temp : opsToDoc){
////            if("GET".equalsIgnoreCase(temp.req.getMethod()) //
////             && temp.req.getCollection() == opToDoc.req.getCollection() //
////             && temp.req.getResourceKey() != null //
////             && temp.req.getRelationshipKey() == null){
////
////                //TODO: con't document releated if we can't find the actual endoint for the relateds
////
////
////
////                PathItem item = openApi.getPaths().get(temp.operationPath);
////                if(item != null){
////                    Operation parentGet = item.getGet();
////                    if(parentGet != null){
////                        ApiResponses resps = parentGet.getResponses();
////                        if(resps != null) {
////                            ApiResponse ok = resps.get("200");
////                            if (ok != null) {
////                                Link link = new Link();
////                                link.setOperationId(opToDoc.operationId);
////                                link.setDescription(opToDoc.req.getRelationship().getName());
////                                ok.link("link-" + opToDoc.operationId, link);
////                                //--TODO document path params
////                            }
////                        }
////                    }
////                }
////            }
////        }
//    }
//
//    protected void documentPost(OpenAPI openApi, Operation opToDoc) {
//
//        if (opToDoc.req.getCollection() == null)
//            return;
//
//        Collection coll = opToDoc.req.getCollection();
//
//        String description = "Creates a new " + coll.getSingularDisplayName() + " resource.";
//
//        String childSchema = documentRequestSchema(openApi, opToDoc);
//        Schema resSchema = openApi.getComponents().getSchemas().get(childSchema);
//
//        ArraySchema arr = new ArraySchema();
//        arr.setItems(newComponentRefSchema(childSchema));
//
//        ComposedSchema oneOf = new ComposedSchema();
//        oneOf.addOneOfItem(resSchema);
//        oneOf.addOneOfItem(arr);
//
//        io.swagger.v3.oas.models.Operation op = openApi.getPaths().get(opToDoc.operationPath).getPost();
//        if (op == null) {
//            op = buildOperation(opToDoc, description, null, "201", documentResponseSchema(openApi, opToDoc));
//            openApi.getPaths().get(opToDoc.operationPath).setPost(op);
//
//
//
//            RequestBody body = new RequestBody();
//            op.setRequestBody(body);
//
//            Content content = new Content();
//            body.setContent(content);
//
//            MediaType json = new MediaType();
//            json.setSchema(oneOf);
//            content.addMediaType("application/json", json);
//        }
//    }
//
//
//    protected void documentPut(OpenAPI openApi, Operation opToDoc) {
//
//        if (opToDoc.req.getCollection() == null)
//            return;
//
//        Collection coll = opToDoc.req.getCollection();
//
//        String description = "Updates an existing " + coll.getSingularDisplayName() + " resource.  Properties of the existing resource that are not supplied in the request body will not be updated.";
//
//
//        io.swagger.v3.oas.models.Operation op = openApi.getPaths().get(opToDoc.operationPath).getPost();
//        if (op == null) {
//            op = buildOperation(opToDoc, description, documentRequestSchema(openApi, opToDoc), "201", documentResponseSchema(openApi, opToDoc));
//            openApi.getPaths().get(opToDoc.operationPath).setPut(op);
//            withResponse(op, opToDoc, "404");
//        }
//
//        //--TODO: add this operation as a link to GET
//    }
//
//    protected void documentPatch(OpenAPI openApi, Operation opToDoc) {
//
//        if (true)
//            return;
//
//        //--TODO: implement me...make DbPatchAction first
//        //--TODO: add this operation as a link to GET
//    }
//
//    protected void documentDelete(OpenAPI openApi, Operation opToDoc) {
//
//        if (opToDoc.req.getCollection() == null)
//            return;
//
//        Collection coll        = opToDoc.req.getCollection();
//        String     description = "Deletes an existing " + coll.getSingularDisplayName() + " resource.";
//
//        io.swagger.v3.oas.models.Operation op = openApi.getPaths().get(opToDoc.operationPath).getPost();
//        if (op == null) {
//            op = buildOperation(opToDoc, description, null, "204", null);
//            openApi.getPaths().get(opToDoc.operationPath).setDelete(op);
//            withResponse(op, opToDoc, "404");
//        }
//    }
//
//    protected io.swagger.v3.oas.models.Operation buildOperation(Operation opToDoc, String description, String requestSchema, String status, String responseSchema) {
//        io.swagger.v3.oas.models.Operation op = new io.swagger.v3.oas.models.Operation().responses(new ApiResponses()).description(description);
//        opToDoc.operation = op;
//        op.setOperationId(opToDoc.operationId);
//
//        if(requestSchema != null){
//            RequestBody body = new RequestBody();
//            op.setRequestBody(body);
//
//            Content content = new Content();
//            body.setContent(content);
//
//            MediaType json = new MediaType();
//            json.setSchema(newComponentRefSchema(requestSchema));
//            content.addMediaType("application/json", json);
//        }
//
//
//        withResponse(op, opToDoc, status, null, responseSchema);
//
//        Collection collection = opToDoc.req.getCollection();
//        if (collection != null) {
//            op.addTagsItem(beautifyTag(collection.getSingularDisplayName()));
//        }
//
//        return op;
//    }
//
//    protected OpenAPIWriter withResponse(io.swagger.v3.oas.models.Operation op, Operation opToDoc, String status) {
//        String description = null;
//        String schemaName  = null;
//        return withResponse(op, opToDoc, status, description, schemaName);
//    }
//
//    protected OpenAPIWriter withResponse(io.swagger.v3.oas.models.Operation op, Operation opToDoc, String status, String description, String schemaName) {
//
//        if (description == null) {
//            switch (status) {
//                case "200":
//                    description = "OK";
//                    break;
//                case "201":
//                    description = "Created";
//                    break;
//                case "204":
//                    description = "No Content";
//                    break;
//                case "400":
//                    description = "Bad Request";
//                    break;
//                case "401":
//                    description = "Unauthorized";
//                    break;
//                case "403":
//                    description = "Forbidden";
//                    break;
//                case "404":
//                    description = "Not Found";
//                    break;
//                case "500":
//                    description = "Internal Server Error";
//                    break;
//            }
//        }
//
//        if (schemaName == null) {
//            if (status != null && "399".compareTo(status) < 0)
//                schemaName = "error";
//        }
//
//        if (op.getResponses() == null)
//            op.setResponses(new ApiResponses());
//
//        ApiResponse response = new ApiResponse();
//        if (description != null)
//            response.setDescription(description);
//
//        if (schemaName != null)
//            response.content(new Content().addMediaType("application/json",
//                    new MediaType().schema(newComponentRefSchema(schemaName))));
//
//        if (op.getResponses().get(status) == null) {
//            op.getResponses().addApiResponse(status, response);
//        }
//
//        return this;
//    }
//
//    protected OpenAPIWriter withParams(io.swagger.v3.oas.models.Operation op, Operation opToDoc, String... params) {
//
//        if (Utils.in("page", params)) {
//            Parameter page = new Parameter();
//            page.setSchema(newTypeSchema("string"));
//            page.setDescription("The optional value used to compute the 'offset' of the first resource returned as 'offset'='page'*'limit'.  If an 'offset' parameter is also supplied it will be used instead of the 'page' parameter.");
//            page.setName("page");
//            page.setIn("query");
//            if (!hasParameter(op, page))
//                op.addParametersItem(page);
//        }
//
//        if (Utils.in("size", params)) {
//            Parameter size = new Parameter();
//            size.setDescription("The optional number of resources to return.  Unless overridden by other configuration the default value is '100'");
//            size.setSchema(newTypeSchema("string"));
//            size.setName("size");
//            size.setIn("query");
//            if (!hasParameter(op, size))
//                op.addParametersItem(size);
//        }
//
//        if (Utils.in("sort", params)) {
//            Parameter sort = new Parameter();
//            sort.setDescription("An optional comma separated list of json property names use to order the results.  Each property may optionally be prefixed with '-' to specify descending order.");
//            sort.setSchema(newTypeSchema("string"));
//            sort.setName("sort");
//            sort.setIn("query");
//            if (!hasParameter(op, sort))
//                op.addParametersItem(sort);
//        }
//        if (Utils.in("q", params)) {
//            Parameter q    = new Parameter();
//            String    desc = "An RQL formatted filter statement that allows you to retrieve only the specific resources you require.  See 'Overview->Querying' for more documentation on available functions and syntax.";
//            q.setDescription(desc);
//            //TODO build a somewhat real example from the collectin attributes
//            //q.setExample("q=eq(jsonPropertyName,value1),in(anotherJsonProperty,value2)");
//            q.setSchema(newTypeSchema("string"));
//            q.setName("q");
//            q.setIn("query");
//            if (!hasParameter(op, q))
//                op.addParametersItem(q);
//        }
//        if (Utils.in("include", params)) {
//            Parameter includes = new Parameter();
//            includes.setDescription("An optional comma separated list of json properties to include in the response.  If this field is not supplied, then any field not listed in the 'excludes' parameter are returned.  When using the 'expands' parameter, you can use 'dot' path notation to reference inclusion of nested properties.");
////            includes.setSchema(newTypeSchema("string"));
////            includes.setName("include");
////            includes.setIn("query");
////            if (!hasParameter(op, includes))
////                op.addParametersItem(includes);
//
//            ArraySchema arr = new ArraySchema();
//            arr.setItems(newTypeSchema("string"));
//
//            includes.setSchema(arr);
//            includes.setStyle(Parameter.StyleEnum.PIPEDELIMITED);
//            includes.setName("include");
//            includes.setIn("query");
//            if (!hasParameter(op, includes))
//                op.addParametersItem(includes);
//
//        }
//        if (Utils.in("exclude", params)) {
//            Parameter excludes = new Parameter();
//            excludes.setDescription("An optional comma separated list of json properties you specifically do not want to be included in the response. When using the 'expands' parameter, you can use 'dot' path notation to reference exclusion of nested properties.");
//            excludes.setSchema(newTypeSchema("string"));
//            excludes.setName("exclude");
//            excludes.setIn("query");
//            if (!hasParameter(op, excludes))
//                op.addParametersItem(excludes);
//        }
//
//        if (Utils.in("expand", params)) {
//            Parameter expands = new Parameter();
//            expands.setDescription("An optional comma separated lists of relationship names that should be expanded in the response. You can reference any number of nesting using 'dot' path notation.");
//            expands.setSchema(newTypeSchema("string"));
//            expands.setName("expand");
//            expands.setIn("query");
//            if (!hasParameter(op, expands))
//                op.addParametersItem(expands);
//        }
//
//        return this;
//    }
//
//    boolean hasParameter(io.swagger.v3.oas.models.Operation op, Parameter param) {
//        if (op.getParameters() == null)
//            return false;
//        for (Parameter existing : op.getParameters()) {
//            if (param.getName().equalsIgnoreCase(existing.getName()) && param.getIn().equalsIgnoreCase(existing.getIn()))
//                return true;
//        }
//        return false;
//    }
//
//
//
//
//
//
//    protected Schema newTypeSchema(String type) {
//        Schema schema = new Schema();
//        schema.setType(type);
//        return schema;
//    }
//
//    protected Schema newHrefSchema() {
//        Schema schema = new Schema();
//        schema.addProperties("href", newTypeSchema("string"));
//        return schema;
//    }
//
//    protected Schema newComponentRefSchema(String nameOrRef) {
//        Schema schema = new Schema();
//        nameOrRef = getSchemaRef(nameOrRef);
//        schema.set$ref(nameOrRef);
//        return schema;
//    }
//
//    protected String getSchemaRef(String nameOrRef){
//        if (!nameOrRef.contains("/"))
//            nameOrRef = "#/components/schemas/" + nameOrRef;
//        return nameOrRef;
//    }
//
//    protected String beautifyTag(String str) {
//        str = str.replace("_", " ");
//
//        StringBuffer buff     = new StringBuffer();
//        boolean      wasLower = !Character.isUpperCase(str.charAt(0));
//
//        for (int i = 0; i < str.length(); i++) {
//
//            char    c       = str.charAt(i);
//            boolean isUpper = Character.isUpperCase(c);
//
//            if (c == ' ') {
//                wasLower = true;
//                buff.append(" ");
//            } else if (wasLower && isUpper) {
//                buff.append(" ");
//                buff.append(c);
//            } else {
//                buff.append(c);
//            }
//            wasLower = !isUpper;
//        }
//        return buff.toString();
//    }



}
