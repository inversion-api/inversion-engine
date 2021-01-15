package io.inversion.openapi.v3;

import com.github.curiousoddman.rgxgen.RgxGen;
import com.github.curiousoddman.rgxgen.iterators.StringIterator;
import io.inversion.*;
import io.inversion.utils.Path;
import io.inversion.utils.Utils;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.links.Link;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import org.apache.commons.collections.map.MultiKeyMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenAPIWriter {

    protected List<String> ignoredEndpointTokens = Utils.add(new ArrayList(), ".json", ".yaml", ".html", ".xml");

    List<OpToDoc>          opsToDoc       = new ArrayList();

    protected String getDescription() {
        return Utils.read(Utils.findInputStream("description.md"));
    }

    public OpenAPI writeOpenAPI(Request req) throws ApiException {
        return writeOpenAPI0(req);
    }

    final OpenAPI writeOpenAPI0(Request req) throws ApiException {

        buildOpsToDoc(req);

        OpenAPI openApi = new OpenAPI();
        documentInfo(openApi, req);
        documentServers(openApi, req);
        documentSchemas(openApi, req);
        documentPathItems(openApi);
        documentOperations(openApi, opsToDoc);

        return openApi;
    }

    protected void documentInfo(OpenAPI openApi, Request req) {
        Info info = new Info();
        openApi.setInfo(info);

        String version = req.getApi().getVersion();
        if (version == null)
            version = "1";
        info.setVersion(version);
        info.setTitle(req.getApi().getName());
        info.setDescription(getDescription());
    }

    protected void documentServers(OpenAPI openApi, Request req) {
        Server server = new Server();
        String url    = req.getApiPath().toString();
        server.setUrl("/" + url);
        openApi.setServers(Utils.add(new ArrayList(), server));
    }

    protected void documentSchemas(OpenAPI openApi, Request req) {

        openApi.setComponents(new Components());//-- prevents NPE

        documentErrorSchema(openApi);
        documentCollectionLinksSchema(openApi);

        for (Collection coll : req.getApi().getCollections()) {
            if(ignoreCollection(coll))
                continue;
            if(coll.getSchemaRef() == null)
                documentResourceSchemas(openApi, coll);

            documentCollectionSchemas(openApi, coll);
        }
    }

    protected void documentErrorSchema(OpenAPI openApi){
        Schema schema = new Schema();
        openApi.getComponents().addSchemas("error", schema);

        schema.addProperties("status", newTypeSchema("string"));
        schema.addProperties("message", newTypeSchema("string"));
        schema.addProperties("error", newTypeSchema("string"));
    }

    protected void documentCollectionLinksSchema(OpenAPI openApi) {
        Schema schema = new Schema();
        openApi.getComponents().addSchemas("_links", schema);

        schema.addProperties("self", newHrefSchema());
        schema.addProperties("first", newHrefSchema());
        schema.addProperties("prev", newHrefSchema());
        schema.addProperties("next", newHrefSchema());
        schema.addProperties("last", newHrefSchema());
        schema.addProperties("after", newHrefSchema());
    }

    protected void documentResourceSchemas(OpenAPI openApi, Collection coll) {

        Schema schema = new Schema();
        openApi.getComponents().addSchemas(coll.getSingularDisplayName(), schema);

        if(coll.getDescription() != null){
            schema.setDescription(coll.getDescription());
        }
        schema.setType("object");

        List<String> requiredProps = new ArrayList<>();
        Index        primaryIndex  = coll.getPrimaryIndex();
        if (primaryIndex != null)
            requiredProps.addAll(primaryIndex.getJsonNames());

        //-- TODO filter excludes/includes
        for (Property prop : coll.getProperties()) {
            String name = prop.getJsonName();
            String type = prop.getJsonType();

            Schema propSchema = newTypeSchema(type);
            if(prop.getDescription() != null)
                propSchema.setDescription(prop.getDescription());

            //TODO...this condition may not be true for non autoincrement PKs
            if (coll.getPrimaryIndex().getProperties().contains(prop)) {
                propSchema.setReadOnly(true);
            }

            if (prop.isNullable()) {
                propSchema.setNullable(true);
            }

            schema.addProperties(name, propSchema);

            if (prop.isRequired() && !requiredProps.contains(name))
                requiredProps.add(name);
        }
        schema.setRequired(requiredProps);

        Schema links = new Schema();
        links.addProperties("self", newHrefSchema());

        for (Relationship rel : coll.getRelationships()) {
            links.addProperties(rel.getName(), newHrefSchema());
        }
        schema.addProperties("_links", links);

    }

    protected void documentCollectionSchemas(OpenAPI openApi, Collection coll) {

        Schema schema = new Schema();
        openApi.getComponents().addSchemas("Get" + coll.getPluralDisplayName() + "Result", schema);

        schema.addProperties("_links", newComponentRefSchema("_links"));
        schema.addProperties("page", newTypeSchema("number"));
        schema.addProperties("size", newTypeSchema("number"));
        schema.addProperties("total", newTypeSchema("number"));

        ArraySchema embedded = new ArraySchema();

        embedded.setItems(newComponentRefSchema(coll));
        schema.addProperties("_embedded", embedded);
    }

    protected void documentPathItems(OpenAPI openApi) {

        openApi.setPaths(new Paths());//--prevents an NPE for uninitialized paths

        for (OpToDoc opToDoc : opsToDoc) {
            String   path     = opToDoc.operationPath;
            PathItem pathItem = openApi.getPaths().get(path);
            if (pathItem == null) {
                documentPathItem(openApi, opToDoc);
            }
        }
    }

    protected void documentPathItem(OpenAPI openApi, OpToDoc opToDoc) {

        PathItem pathItem = new PathItem();
        openApi.getPaths().addPathItem(opToDoc.operationPath, pathItem);

        String  operationPath = opToDoc.operationPath;
        Path    pathMatch     = opToDoc.matchPath;
        Request req           = opToDoc.req;


        Path path = new Path(operationPath);

        for (int i = 0; i < path.size(); i++) {
            String name = path.get(i);

            if (path.isVar(i)) {
                name = path.getVarName(i);//--this looks wrong but is because of the different path encodings of OpenApi paths to Inversion paths: TODO: fix this difference probably in breaking change to Inversion syntax
                if (name == null)
                    name = path.getRegex(i);

                Schema schema = null;
                if (req.getCollection() != null) {
                    Collection coll = req.getCollection();
                    Property   prop = coll.getProperty(name);
                    if (prop != null) {
                        String type = prop.getJsonType();
                        if (type.equalsIgnoreCase("number"))
                            schema = newTypeSchema("number");
                        if (type.equalsIgnoreCase("boolean"))
                            schema = newTypeSchema("boolean");
                    }
                }

                if (schema == null) {
                    schema = newTypeSchema("string");
                    String regex = pathMatch.getRegex(i);
                    if (regex != null)
                        schema.setPattern(regex);
                }

                Parameter param = new Parameter();
                param.setName(name);
                param.setIn("path");
                param.setRequired(true);
                param.setSchema(schema);

                pathItem.addParametersItem(param);
            }
        }
    }

    protected void documentOperations(OpenAPI openApi, List<OpToDoc> opsToDoc) {
        for (OpToDoc opToDoc : opsToDoc) {
            documentOperation(openApi, opToDoc);
        }
    }

    protected void documentOperation(OpenAPI openApi, OpToDoc opToDoc) {
            switch (opToDoc.function.toLowerCase()) {
                case "list":
                    documentList(openApi, opToDoc);
                    break;
                case "get":
                    documentGet(openApi, opToDoc);
                    break;
                case "related":
                    documentRelated(openApi, opToDoc);
                    break;
                case "post":
                    documentPost(openApi, opToDoc);
                    break;
                case "put":
                    documentPut(openApi, opToDoc);
                    break;
                case "patch":
                    documentPatch(openApi, opToDoc);
                    break;
                case "delete":
                    documentDelete(openApi, opToDoc);
                    break;
            }

            PathItem pathItem = openApi.getPaths().get(opToDoc.operationPath);
            if(pathItem != null){
                Operation op = pathItem.getGet();
            }

//            Collection collection = req.getCollection();
//            if (collection != null) {
//                op.addTagsItem(beautifyTag(collection.getSingularDisplayName()));
//            }
    }

    protected void documentGet(OpenAPI openApi, OpToDoc opToDoc) {

        if (opToDoc.req.getCollection() == null)
            return;

        Collection coll = opToDoc.req.getCollection();

        String description = "A specific " + coll.getSingularDisplayName() + " object";
        String schemaName = coll.getSingularDisplayName();

        Operation op = buildOperation(opToDoc, description, "200", schemaName);
        openApi.getPaths().get(opToDoc.operationPath).setGet(op);

        withParams(op, opToDoc,"include", "exclude", "expand", "collapse");
        withResponse(op, opToDoc, "404");
    }

    protected void documentList(OpenAPI openApi, OpToDoc opToDoc) {

        if (opToDoc.req.getCollection() == null)
            return;

        Collection coll = opToDoc.req.getCollection();

        String description = "A pageable list of all " + coll.getSingularDisplayName() + " resources the user has access to and also match any query parameters.  The list may be empty.";
        String schemaName = "Get" + coll.getPluralDisplayName() + "Result";

        Operation op = buildOperation(opToDoc, description, "200", schemaName);
        openApi.getPaths().get(opToDoc.operationPath).setGet(op);

        withParams(op, opToDoc,"page", "size", "sort", "q", "include", "exclude", "expand");
    }

    protected void documentRelated(OpenAPI openApi, OpToDoc opToDoc) {

        if (opToDoc.req.getCollection() == null)
            return;

        Collection parent = opToDoc.req.getCollection();
        Collection related = opToDoc.req.getRelationship().getRelated();

        String description = "Retrieves all of the " + related.getPluralDisplayName() + " related to the " + parent.getSingularDisplayName();
        String schemaName = "Get" + parent.getPluralDisplayName() + "Result";

        Operation op = buildOperation(opToDoc, description, "200", schemaName);
        openApi.getPaths().get(opToDoc.operationPath).setGet(op);

        withParams(op, opToDoc,"page", "size", "sort", "q", "include", "exclude", "expand");

        //-- adds this relationship to the {collection}/{resource} endpoint parent
        for(OpToDoc temp : opsToDoc){
            if("GET".equalsIgnoreCase(temp.req.getMethod()) //
             && temp.req.getCollection() == opToDoc.req.getCollection() //
             && temp.req.getResourceKey() != null //
             && temp.req.getRelationshipKey() == null){

                PathItem item = openApi.getPaths().get(temp.operationPath);
                if(item != null){
                    Operation parentGet = item.getGet();
                    if(parentGet != null){
                        ApiResponses resps = parentGet.getResponses();
                        if(resps != null) {
                            ApiResponse ok = resps.get("200");
                            if (ok != null) {
                                Link link = new Link();
                                link.setOperationId(opToDoc.operationId);
                                link.setDescription(opToDoc.req.getRelationship().getName());
                                ok.link("link-" + opToDoc.operationId, link);
                                //--TODO document path params
                            }
                        }
                    }
                }
            }
        }
    }

    protected void documentPost(OpenAPI openApi, OpToDoc opToDoc) {

        if (opToDoc.req.getCollection() == null)
            return;

        Collection coll = opToDoc.req.getCollection();

        String description = "Creates a new " + coll.getSingularDisplayName() + " resource.";
        String schemaName = coll.getSingularDisplayName();

        Operation op = buildOperation(opToDoc, description, "201", schemaName);
        openApi.getPaths().get(opToDoc.operationPath).setPost(op);
    }

    protected void documentPut(OpenAPI openApi, OpToDoc opToDoc) {

        if (opToDoc.req.getCollection() == null)
            return;

        Collection coll = opToDoc.req.getCollection();

        String description = "Updates an existing " + coll.getSingularDisplayName() + " resource.  Properties of the existing resource that are not supplied in the request body will not be updated.";
        String schemaName = coll.getSingularDisplayName();

        Operation op = buildOperation(opToDoc, description, "201", schemaName);
        openApi.getPaths().get(opToDoc.operationPath).setPut(op);

        withResponse(op, opToDoc, "404");

        //--TODO: add this operation as a link to GET
    }

    protected void documentPatch(OpenAPI openApi, OpToDoc opToDoc) {

        if (true)
            return;

        //--TODO: implement me...make DbPatchAction first
        //--TODO: add this operation as a link to GET
    }

    protected void documentDelete(OpenAPI openApi, OpToDoc opToDoc) {

        if (opToDoc.req.getCollection() == null)
            return;

        Collection coll = opToDoc.req.getCollection();
        String description = "Deletes an existing " + coll.getSingularDisplayName() + " resource.";

        Operation op = buildOperation(opToDoc, description, "204", null);
        withResponse(op,opToDoc, "404");

        openApi.getPaths().get(opToDoc.operationPath).setDelete(op);
    }

    protected Operation buildOperation(OpToDoc opToDoc, String description, String status, String schemaName){
        Operation op = new Operation().responses(new ApiResponses()).description(description);
        op.setOperationId(opToDoc.operationId);
        withResponse(op, opToDoc, status, null, schemaName);

        Collection collection = opToDoc.req.getCollection();
        if (collection != null) {
            op.addTagsItem(beautifyTag(collection.getSingularDisplayName()));
        }

        return op;
    }

    protected OpenAPIWriter withResponse(Operation op, OpToDoc opToDoc, String status){
        String description = null;
        String schemaName = null;
        return withResponse(op, opToDoc, status, description, schemaName);
    }

    protected OpenAPIWriter withResponse(Operation op, OpToDoc opToDoc, String status, String description, String schemaName){

        if(description == null){
            switch(status){
                case "200" :
                    description = "OK";
                    break;
                case "201" :
                    description = "Created";
                    break;
                case "204" :
                    description = "No Content";
                    break;
                case "400" :
                    description = "Bad Request";
                    break;
                case "401" :
                    description = "Unauthorized";
                    break;
                case "403" :
                    description = "Forbidden";
                    break;
                case "404" :
                    description = "Not Found";
                    break;
                case "500" :
                    description = "Internal Server Error";
                    break;
            }
        }

        if(schemaName == null){
            if(status != null && "399".compareTo(status) < 0)
                schemaName = "error";
        }

        if(op.getResponses() == null)
            op.setResponses(new ApiResponses());

        ApiResponse response = new ApiResponse();
        if(description != null)
            response.setDescription(description);

        if(schemaName != null)
            response.content(new Content().addMediaType("application/json",
                    new MediaType().schema(newComponentRefSchema(schemaName))));

        op.getResponses().addApiResponse(status, response);

        return this;
    }

    protected OpenAPIWriter withParams(Operation op, OpToDoc opToDoc, String... params) {

        if(Utils.in("page", params)) {
            Parameter page = new Parameter();
            page.setSchema(newTypeSchema("string"));
            page.setDescription("The optional value used to compute the 'offset' of the first resource returned as 'offset'='page'*'limit'.  If an 'offset' parameter is also supplied it will be used instead of the 'page' parameter.");
            page.setName("page");
            page.setIn("query");
            op.addParametersItem(page);
        }

        if(Utils.in("size", params)) {
            Parameter size = new Parameter();
            size.setDescription("The optional number of resources to return.  Unless overridden by other configuration the default value is '100'");
            size.setSchema(newTypeSchema("string"));
            size.setName("size");
            size.setIn("query");
            op.addParametersItem(size);
        }

        if(Utils.in("sort", params)) {
            Parameter sort = new Parameter();
            sort.setDescription("An optional comma separated list of json property names use to order the results.  Each property may optionally be prefixed with '-' to specify descending order.");
            sort.setSchema(newTypeSchema("string"));
            sort.setName("sort");
            sort.setIn("query");
            op.addParametersItem(sort);
        }
        if(Utils.in("q", params)) {
            Parameter q    = new Parameter();
            String    desc = "An RQL formatted filter statement that allows you to retrieve only the specific resources you require.  See 'Overview->Querying' for more documentation on available functions and syntax.";
            q.setDescription(desc);
            //TODO build a somewhat real example from the collectin attributes
            //q.setExample("q=eq(jsonPropertyName,value1),in(anotherJsonProperty,value2)");
            q.setSchema(newTypeSchema("string"));
            q.setName("q");
            q.setIn("query");
            op.addParametersItem(q);
        }
        if(Utils.in("include", params)) {
            Parameter includes = new Parameter();
            includes.setDescription("An optional comma separated list of json properties to include in the response.  If this field is not supplied, then any field not listed in the 'excludes' parameter are returned.  When using the 'expands' parameter, you can use 'dot' path notation to reference inclusion of nested properties.");
            includes.setSchema(newTypeSchema("string"));
            includes.setName("include");
            includes.setIn("query");
            op.addParametersItem(includes);
        }
        if(Utils.in("exclude", params)) {
            Parameter excludes = new Parameter();
            excludes.setDescription("An optional comma separated list of json properties you specifically do not want to be included in the response. When using the 'expands' parameter, you can use 'dot' path notation to reference exclusion of nested properties.");
            excludes.setSchema(newTypeSchema("string"));
            excludes.setName("exclude");
            excludes.setIn("query");
            op.addParametersItem(excludes);
        }

        if(Utils.in("expand", params)) {
            Parameter expands = new Parameter();
            expands.setDescription("An optional comma separated lists of relationship names that should be expanded in the response. You can reference any number of nesting using 'dot' path notation.");
            expands.setSchema(newTypeSchema("string"));
            expands.setName("expand");
            expands.setIn("query");
            op.addParametersItem(expands);
        }

        return this;
    }


    protected void buildOpsToDoc(Request req) {

        Api            api       = req.getApi();
        List<Endpoint> endpoints = api.getEndpoints();

        for (int pass = 1; pass <= 4; pass++) {
            for (Endpoint endpoint : endpoints) {

                if (ignoreEndpoint(endpoint))
                    continue;

                for (Rule.RuleMatcher endpointMatcher : endpoint.getIncludeMatchers()) {
                    List<Path> paths = endpointMatcher.getPaths();
                    for (Path endpointPath : paths) {
                        Path endpointBasePath = new Path();
                        for (int i = 0; i < endpointPath.size(); i++) {
                            if (endpointPath.isOptional(i) || endpointPath.isWildcard(i))
                                break;
                            endpointBasePath.add(endpointPath.get(i));
                        }

                        for (Collection collection : api.getCollections()) {

                            if(ignoreCollection(collection))
                                continue;

                            for (Rule.RuleMatcher collectionMatcher : collection.getIncludeMatchers()) {
                                for (Path collectionPath : collectionMatcher.getPaths()) {

                                    Path epColPath     = new Path(endpointBasePath.toString(), collectionPath.toString());
                                    Path candidatePath = new Path();

                                    while (epColPath.size() > 0) {

                                        Path consumed = consumeToNext(epColPath, candidatePath);
                                        if (consumed.size() == 0)
                                            break;

                                        Path matchPath = new Path(candidatePath);

                                        if (candidatePath.hasAllVars(Request.COLLECTION_KEY, Request.RELATIONSHIP_KEY, Request.RELATIONSHIP_KEY)) {
                                            if (pass == 3) {
                                                if (endpointMatcher.hasMethod("GET") && collectionMatcher.hasMethod("GET")) {
                                                    for (Relationship rel : collection.getRelationships()) {
                                                        queueOpToDoc("RELATED", matchPath, req, endpoint, collection, rel);
                                                    }
                                                }
                                            }
                                        } else if (candidatePath.hasAllVars(Request.COLLECTION_KEY, Request.RESOURCE_KEY)) {
                                            if (pass == 2) {
                                                queueOpToDoc("GET", matchPath, req, endpoint, collection, null);
                                                queueOpToDoc("PUT", matchPath, req, endpoint, collection, null);
                                                queueOpToDoc("PATCH", matchPath, req, endpoint, collection, null);
                                                queueOpToDoc("DELETE", matchPath, req, endpoint, collection, null);
                                            }
                                        } else if (candidatePath.hasAllVars(Request.COLLECTION_KEY)) {
                                            if (pass == 1) {
                                                queueOpToDoc("LIST", matchPath, req, endpoint, collection, null);
                                                queueOpToDoc("POST", matchPath, req, endpoint, collection, null);
                                            }
                                        } else {
                                            //TODO document these additional eps? ...but what do they do?
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    protected void queueOpToDoc(String function, Path matchPath, Request req, Endpoint endpoint, Collection coll, Relationship rel) {

        Path livePath = new Path(matchPath);

        livePath = fixCollectionKeyPathParam(livePath, coll);
        livePath = fixResourceKeyPathParam(livePath, coll);
        livePath = fixRelationshipKeyPathParam(livePath, rel);

        for (int i = 0; i < livePath.size(); i++) {

            String part = livePath.get(i);

            if (part.startsWith("["))
                part = part.substring(1, part.length());
            if (part.endsWith("]"))
                part = part.substring(0, part.length() - 1);

            if (livePath.isVar(i)) {
                part = livePath.getVarName(i);

                String regex = livePath.getRegex(i);
                if (regex != null) {
                    if (!Utils.isRegex(regex)) {
                        part = regex;
                    } else {
                        RgxGen         rgxGen        = new RgxGen(regex);
                        StringIterator uniqueStrings = rgxGen.iterateUnique();
                        part = uniqueStrings.next();
                    }

                }

                if (part == null)
                    part = "var" + i + 1;
            }
            livePath.set(i, part);
        }

        Path apiPath = req.getApiPath();

        String url = req.getUrl().toString();
        url = Utils.substringBefore(url, "?");
        if (!url.endsWith("/"))
            url += "/";
        String host = url.substring(0, url.indexOf("/", 9) + 1);
        url = host;

        if (apiPath.size() > 0)
            url = url + apiPath + "/";

        url = url + livePath;

        String method = function;
        if (method.equalsIgnoreCase("list") || method.equalsIgnoreCase("related"))
            method = "GET";

        Request docReq = new Request(method, url, null);

        req.getEngine().matchRequest(docReq);

        if (docReq.getEndpoint() == null || docReq.getActionMatches().size() == 0)
            return;

        OpToDoc opToDoc = new OpToDoc(function, matchPath, docReq);
        opsToDoc.add(opToDoc);
    }

    protected boolean ignoreEndpoint(Endpoint endpoint) {
        if (endpoint.isInternal())
            return true;

        for (String token : ignoredEndpointTokens) {
            if (endpoint.toString().contains(token))
                return true;
        }

        return false;
    }

    protected boolean ignoreCollection(Collection collection){
        if (collection.isLinkTbl() || collection.isExclude())
                return true;
        return false;
    }

    /*
     * Replaces the var name which is "_collection" to the name of the collection
     * @param path
     * @param collection
     * @return
     */
    protected Path fixCollectionKeyPathParam(Path path, Collection coll) {
        path = new Path(path);

        for (int i = 0; i < path.size(); i++) {
            //TODO: should we pull from the regex instead of collection.getName()?
            if (Request.COLLECTION_KEY.equalsIgnoreCase(path.getVarName(i))) {
                path.set(i, coll.getName());
            }
        }
        return path;
    }

    /*
     * Replace the var name which is "_resource" to the name of the actual key field
     * @param path
     * @param collection
     * @return
     */
    protected Path fixResourceKeyPathParam(Path path, Collection coll) {
        path = new Path(path);


        for (int i = 0; i < path.size(); i++) {
            if (Request.RESOURCE_KEY.equalsIgnoreCase(path.getVarName(i))) {
                String varName = null;
                String regex   = path.getRegex(i);
                Index  pk      = coll.getPrimaryIndex();
                if (pk.size() == 1) {
                    varName = pk.getPropertyName(0);
                    regex = regex != null ? regex : pk.getProperty(0).getRegex();
                } else {
                    varName = "id";
                    for (int j = 0; j < 10; j++) {
                        if (coll.getProperty(varName) == null)
                            break;
                        varName = "_" + varName;
                    }
                }
                String var = regex == null ? (":" + varName) : ("{" + varName + ":" + regex + "}");
                path.set(i, var);
            }
        }
        return path;
    }

    protected Path fixRelationshipKeyPathParam(Path path, Relationship rel) {
        path = new Path(path);
        if (rel == null)
            return path;

        for (int i = 0; i < path.size(); i++) {
            if (Request.RELATIONSHIP_KEY.equalsIgnoreCase(path.getVarName(i))) {
                path.set(i, rel.getName());
            }
        }
        return path;
    }

    protected Schema newTypeSchema(String type) {
        Schema schema = new Schema();
        schema.setType(type);
        return schema;
    }

    protected Schema newHrefSchema() {
        Schema schema = new Schema();
        schema.addProperties("href", newTypeSchema("string"));
        return schema;
    }

    protected Schema newComponentRefSchema(String nameOrRef) {
        Schema schema = new Schema();
        if(!nameOrRef.contains("/"))
            nameOrRef = "#/components/schemas/" + nameOrRef;
        schema.set$ref(nameOrRef);
        return schema;
    }

    protected Schema newComponentRefSchema(Collection coll){
        Schema schema = new Schema();
        String ref = coll.getSchemaRef();
        if(ref == null){
            ref = coll.getSingularDisplayName();
        }

        if(!ref.contains("/"))
            ref = "#/components/schemas/" + ref;
        schema.set$ref(ref);
        return schema;
    }


    protected String beautifyTag(String str) {
        str = str.replace("_", " ");

        StringBuffer buff     = new StringBuffer();
        boolean      wasLower = !Character.isUpperCase(str.charAt(0));

        for (int i = 0; i < str.length(); i++) {

            char    c       = str.charAt(i);
            boolean isUpper = Character.isUpperCase(c);

            if (c == ' ') {
                wasLower = true;
                buff.append(" ");
            } else if (wasLower && isUpper) {
                buff.append(" ");
                buff.append(c);
            } else {
                buff.append(c);
            }
            wasLower = !isUpper;
        }
        return buff.toString();
    }

    protected Path consumeToNext(Path epColPath, Path candidatePath) {

        Path added = new Path();

        while (epColPath.size() > 0) {
            if (epColPath.isWildcard(0)) {
                epColPath.remove(0);
                return added;
            }

            boolean isStatic = epColPath.isStatic(0);
            String  varName  = isStatic ? null : epColPath.getVarName(0);
            String  part     = epColPath.remove(0);

            if (part.startsWith("["))
                part = part.substring(1, part.length());
            if (part.endsWith("]"))
                part = part.substring(0, part.length() - 1);

            added.add(part);
            candidatePath.add(part);

            if (varName != null && Utils.in(varName, Request.COLLECTION_KEY, Request.RESOURCE_KEY, Request.RELATIONSHIP_KEY))
                return added;
        }

        return added;
    }

    class OpToDoc {
        String operationId = null;
        String function    = null;
        Path   matchPath   = null;
        String  operationPath = null;
        Request req           = null;

        public OpToDoc(String function, Path matchPath, Request req) {
            this.function = function;
            this.matchPath = matchPath;
            this.req = req;
            operationPath = asOperationPath(matchPath);

            operationId = req.getMethod() + "-" + operationPath;
            operationId = operationId.replace("{", "");
            operationId = operationId.replace("}", "");
            operationId = operationId.replace("/", "-");
        }

        String asOperationPath(Path matchPath) {

            Path opPath = new Path(matchPath);
            opPath = fixCollectionKeyPathParam(opPath, req.getCollection());
            opPath = fixResourceKeyPathParam(opPath, req.getCollection());
            opPath = fixRelationshipKeyPathParam(opPath, req.getRelationship());

            int numVars = 0;
            for (int i = 0; i < opPath.size(); i++) {

                if (opPath.isWildcard(0)) {
                    opPath.remove(i);
                    i--;
                    continue;
                }

                String part = opPath.get(i);

                if (opPath.isOptional(i)) {
                    while (part.startsWith("["))
                        part = part.substring(1, part.length());
                    while (part.endsWith("]"))
                        part = part.substring(0, part.length() - 1);
                }
                if (opPath.isVar(i)) {
                    String varName = opPath.getVarName(i);
                    if (varName != null) {
                        part = "{" + varName + "}";
                    } else {
                        String regex = opPath.getRegex(i);
                        if (regex != null && !Utils.isRegex(regex)) {
                            part = regex;
                        } else {
                            numVars += 1;
                            part = "{var" + numVars + "}";
                        }
                    }
                }
                opPath.set(i, part);
            }
            return "/" + opPath;
        }
    }
}
