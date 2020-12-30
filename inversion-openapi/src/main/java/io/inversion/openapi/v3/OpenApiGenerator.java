package io.inversion.openapi.v3;

import com.github.curiousoddman.rgxgen.RgxGen;
import com.github.curiousoddman.rgxgen.iterators.StringIterator;
import io.inversion.*;
import io.inversion.Collection;
import io.inversion.utils.Path;
import io.inversion.utils.Utils;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.links.Link;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import org.apache.commons.collections.map.MultiKeyMap;

import java.lang.reflect.Method;
import java.util.*;

public class OpenApiGenerator {

    List<String> ignoredEndpointTokens = Utils.add(new ArrayList(), ".json", ".yaml", ".html", ".xml");
    OpenAPI openApi = new OpenAPI();


    /**
     * method,collection,endpoint,id
     */
    MultiKeyMap operationIds   = new MultiKeyMap();
    Map<String, Operation> operationIdMap = new HashMap<>();

    List<OpToDoc> opsToDoc = new ArrayList();

    Request req = null;

    protected String getDescription(){
        return Utils.read(Utils.findInputStream("description.md"));
    }

    public OpenAPI generateOpenApi(Request req) throws ApiException {
        return generateOpenApi0(req);
    }

    final OpenAPI generateOpenApi0(Request req) throws ApiException {

        this.req = req;
        openApi.setInfo(documentInfo(openApi, req));
        openApi.setServers(documentServers(openApi, req));

        Components comps = new Components();
        openApi.setComponents(comps);

//        Schema meta = new Schema();
//        meta.setType("object");
//        meta.addProperties("pageNum", newTypeSchema("integer"));
//        meta.addProperties("pageSize", newTypeSchema("integer"));
//        meta.addProperties("pageCount", newTypeSchema("integer"));
//        //"offset", "limit", "page", "pageNum", "pageSize", "after", "foundRows", "pageCount", "next"
//        comps.addSchemas("meta", meta);

        comps.addSchemas("_links", documentCollectionLinksSchema());

        for (Collection coll : req.getApi().getCollections()) {

            String singularName = coll.getSingularDisplayName();
            String pluralName = "Get" + coll.getPluralDisplayName() + "Result";

            Schema resourceSchema = documentResourceSchemas(openApi, req, coll);
            comps.addSchemas(singularName, resourceSchema);

            Schema newCollectionSchema = documentCollectionSchemas(openApi, req, coll);
            comps.addSchemas(pluralName, newCollectionSchema);
        }

        Api            api       = req.getApi();
        List<Endpoint> endpoints = api.getEndpoints();

        for(int pass = 1; pass<= 4; pass++) {
            for (Endpoint endpoint : endpoints) {

                if(endpoint.isInternal())
                    continue;

                for(String token : ignoredEndpointTokens)
                    if (endpoint.toString().contains(token))
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

                            if(collection.isLinkTbl() || collection.isExclude())
                                continue;

                            for (Rule.RuleMatcher collectionMatcher : collection.getIncludeMatchers()) {
                                for (Path collectionPath : collectionMatcher.getPaths()) {

                                    Path epColPath = new Path(endpointBasePath.toString(), collectionPath.toString());
                                    Path candidatePath = new Path();

                                    while (epColPath.size() > 0) {

                                        Path consumed = consumeToNext(epColPath, candidatePath);
                                        if (consumed.size() == 0)
                                            break;

                                        Path matchPath = new Path(candidatePath);

//                                        if (candidatePath.hasAllVars(Request.COLLECTION_KEY, Request.RELATIONSHIP_KEY, Request.RELATIONSHIP_KEY)) {
//                                            if(pass == 3) {
//                                                if (endpointMatcher.hasMethod("GET") && collectionMatcher.hasMethod("GET")) {
//                                                    for (Relationship rel : collection.getRelationships()) {
//                                                        queueOpToDoc("RELATED", matchPath, endpoint, collection, rel);
//                                                    }
//                                                }
//                                            }
//                                        } else if (candidatePath.hasAllVars(Request.COLLECTION_KEY, Request.RESOURCE_KEY)) {
//                                            if(pass ==2) {
//                                                queueOpToDoc("GET", matchPath, endpoint, collection, null);
//                                                queueOpToDoc("PUT", matchPath, endpoint, collection, null);
//                                                queueOpToDoc("PATCH", matchPath, endpoint, collection, null);
//                                                queueOpToDoc("DELETE", matchPath, endpoint, collection, null);
//                                            }
//                                        } else
                                            if (candidatePath.hasAllVars(Request.COLLECTION_KEY)) {
                                            if(pass == 1) {
                                                queueOpToDoc("LIST", matchPath, endpoint, collection, null);
                                                queueOpToDoc("POST", matchPath, endpoint, collection, null);
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


        for(OpToDoc opToDoc : opsToDoc){
            documentOp(opToDoc);
        }



        return openApi;
    }

    void queueOpToDoc(String operation, Path matchPath, Endpoint endpoint, Collection coll, Relationship rel){

        Path livePath = new Path(matchPath);

        livePath = fixCollectionKeyPathParam(livePath, coll);
        livePath = fixResourceKeyPathParam(livePath, coll);
        livePath = fixRelationshipKeyPathParam(livePath, rel);

        for(int i=0; i<livePath.size(); i++){

            String part = livePath.get(i);

            if(part.startsWith("["))
                part = part.substring(1, part.length());
            if(part.endsWith("]"))
                part = part.substring(0, part.length() -1);

            if(livePath.isVar(i)) {
                part = livePath.getVarName(i);

                String regex = livePath.getRegex(i);
                if(regex != null){
                    if(!Utils.isRegex(regex)){
                        part = regex;
                    }
                    else{
                        RgxGen         rgxGen        = new RgxGen(regex);
                        StringIterator uniqueStrings = rgxGen.iterateUnique();
                        part = uniqueStrings.next();
                    }

                }

                if(part == null)
                    part = "var" + i+1;
            }
            livePath.set(i, part);
        }

        Path apiPath = req.getApiPath();

        String url = req.getUrl().toString();
        url = Utils.substringBefore(url, "?");
        if(!url.endsWith("/"))
            url += "/";
        String host = url.substring(0, url.indexOf("/", 9) + 1);
        url = host;

        if(apiPath.size() > 0)
            url = url + apiPath + "/";

        url = url + livePath;

        String method = operation;
        if(method.equalsIgnoreCase("list") || method.equalsIgnoreCase("related"))
            method = "GET";

        Request docReq = new Request(method, url, null);

        req.getEngine().matchRequest(docReq);

        if(docReq.getEndpoint() == null || docReq.getActionMatches().size() == 0)
            return;

        OpToDoc opToDoc = new OpToDoc(operation, matchPath, docReq);
        opsToDoc.add(opToDoc);
    }

    class OpToDoc {
        String operationId = null;
        String operation = null;
        Path matchPath = null;
        String operationPath = null;
        Request req = null;

        public OpToDoc(String operation, Path matchPath, Request req) {
            this.operation = operation;
            this.matchPath = matchPath;
            this.req = req;
            operationPath = asOperationPath(matchPath);

            operationId = req.getMethod() + "-" + operationPath;
            operationId = operationId.replace("{", "");
            operationId = operationId.replace("}", "");
            operationId = operationId.replace("/", "-");
        }

        String asOperationPath(Path matchPath){

            Path opPath = new Path(matchPath);
            opPath = fixCollectionKeyPathParam(opPath, req.getCollection());
            opPath = fixResourceKeyPathParam(opPath, req.getCollection());
            opPath = fixRelationshipKeyPathParam(opPath, req.getRelationship());

            int numVars = 0;
            for(int i=0; i<opPath.size(); i++){

                if(opPath.isWildcard(0)) {
                    opPath.remove(i);
                    i--;
                    continue;
                }

                String part = opPath.get(i);

                if(opPath.isOptional(i)){
                    while(part.startsWith("["))
                        part = part.substring(1, part.length());
                    while(part.endsWith("]"))
                        part = part.substring(0, part.length()-1);
                }
                if(opPath.isVar(i)){
                    String varName = opPath.getVarName(i);
                    if(varName != null){
                        part = "{" + varName + "}";
                    }
                    else{
                        String regex = opPath.getRegex(i);
                        if(regex != null && !Utils.isRegex(regex)) {
                            part = regex;
                        }
                        else{
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



    void documentOp(OpToDoc opToDoc){

        try {
            String  operationId = opToDoc.operationId;
            String  operation   = opToDoc.operation;
            Path    matchPath   = opToDoc.matchPath;
            String   operationPath = opToDoc.operationPath;
            Request req         = opToDoc.req;

            PathItem pathItem      = openApi.getPaths() == null ? null : openApi.getPaths().get(operationPath);
            boolean newPathItem = false;
            if (pathItem == null) {
                pathItem = new PathItem();
                newPathItem = true;
            }

            Operation op = null;
            switch(operation.toLowerCase()){
                case "list" :
                    op = documentList(openApi, pathItem, operationPath, req);
                    break;
                case "get" :
                    op = documentGet(openApi, pathItem, operationPath, req);
                    break;
                case "related" :
                    op = documentRelated(openApi, pathItem, operationPath, req);
                    break;
                case "post" :
                    op = documentPost(openApi, pathItem, operationPath, req);
                    break;
                case "put" :
                    op = documentPut(openApi, pathItem, operationPath, req);
                    break;
                case "patch" :
                    op = documentPatch(openApi, pathItem, operationPath, req);
                    break;
                case "delete" :
                    op = documentDelete(openApi, pathItem, operationPath, req);
                    break;
            }

            if (op != null) {

                operationId = op.getOperationId() == null ? operationId : op.getOperationId();
                //op.setOperationId(operationId);

                switch (req.getMethod().toLowerCase()){
                    case "get" :
                        pathItem.setGet(op);
                        break;
                    case "post" :
                        pathItem.setPost(op);
                        break;
                    case "put" :
                        pathItem.setPut(op);
                        break;
                    case "patch" :
                        pathItem.setPatch(op);
                        break;
                    case "delete" :
                        pathItem.setDelete(op);
                        break;
                }

                if(newPathItem)
                    openApi.path(operationPath, pathItem);

                documentPathItem(openApi, pathItem, op, operationPath, matchPath, req);
                documentOperationTags(openApi, pathItem, op, operationPath, req);
            }
        }
        catch(Throwable ex){
            throw ApiException.new500InternalServerError(ex);
        }
    }

    public void documentPathItem(OpenAPI openApi, PathItem pathItem, Operation op, String operationPath, Path pathMatch, Request req){

        Path path = new Path(operationPath);

        for (int i=0;i<path.size(); i++) {
            String name = path.get(i);

            if (path.isVar(i)){
                name = path.getVarName(i);//--this looks wrong but is because of the different path encodings of OpenApi paths to Inversion paths: TODO: fix this difference probably in breaking change to Inversion syntax
                if(name == null)
                    name = path.getRegex(i);

                Schema schema = null;
                if(req.getCollection() != null){
                    Collection coll = req.getCollection();
                    Property prop = coll.getProperty(name);
                    if(prop != null){
                        String type = prop.getJsonType();
                        if(type.equalsIgnoreCase("number"))
                            schema = newTypeSchema("number");
                        if(type.equalsIgnoreCase("boolean"))
                            schema = newTypeSchema("boolean");
                    }
                }

                if(schema == null){
                    schema = newTypeSchema("string");
                    String regex = pathMatch.getRegex(i);
                    if(regex != null)
                        schema.setPattern(regex);
                }

                Parameter param = new Parameter();
                param.setName(name);
                param.setIn("path");
                param.setRequired(true);
                param.setSchema(schema);

                boolean hasParam = false;
                if(pathItem.getParameters() != null) {
                    for (Parameter p : pathItem.getParameters()) {
                        if (p.getName().equalsIgnoreCase(name)) {
                            hasParam = true;
                            break;
                        }
                    }
                }
                if(!hasParam)
                    pathItem.addParametersItem(param);
            }
        }
    }


    public void documentOperationTags(OpenAPI openApi, PathItem pathItem, Operation op, String operationPath, Request req){
        Collection collection = req.getCollection();
        if(collection != null){
            op.addTagsItem(beautifyTag(collection.getSingularDisplayName()));
        }
    }

    public Info documentInfo(OpenAPI api, Request req){
        Info info = new Info();
        String version = req.getApi().getVersion();
        if(version == null)
            version = "1";
        info.setVersion(version);
        info.setTitle(req.getApi().getName());
        info.setDescription(getDescription());
        return info;
    }

    public List<Server> documentServers(OpenAPI api, Request req){
        Server server = new Server();
        String url = req.getApiPath().toString();
        server.setUrl("/" + url);
        return Utils.add(new ArrayList(), server);
    }

    public Schema documentResourceSchemas(OpenAPI api, Request req, Collection coll){

        Schema schema = new Schema();
        schema.setType("object");

        List<String> requiredProps = new ArrayList<>();
        Index        primaryIndex  = coll.getPrimaryIndex();
        if (primaryIndex != null)
            requiredProps.addAll(primaryIndex.getJsonNames());

        //-- TODO filter excludes/includes
        for (Property prop : coll.getProperties()) {
            String name = prop.getJsonName();
            String type = prop.getType();

            Schema propSchema = newTypeSchema(prop.getJsonType());
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

        for(Relationship rel : coll.getRelationships()){
            links.addProperties(rel.getName(), newHrefSchema());
        }
        schema.addProperties("_links", links);
        return schema;
    }

    public Schema documentCollectionSchemas(OpenAPI openApi, Request req, Collection coll){
        String singularName = coll.getSingularDisplayName();


        Schema schema = new Schema();
        schema.addProperties("_links", newComponentRefSchema("_links") );

        schema.addProperties("page", newTypeSchema("number"));
        schema.addProperties("count", newTypeSchema("number"));
        schema.addProperties("total", newTypeSchema("number"));

        ArraySchema embedded = new ArraySchema();
        embedded.setItems(newComponentRefSchema(singularName));

        schema.addProperties("_embedded", embedded);

        return schema;
    }

    protected Schema documentCollectionLinksSchema(){
        Schema schema = new Schema();
        schema.addProperties("self", newHrefSchema());
        schema.addProperties("first", newHrefSchema());
        schema.addProperties("prev", newHrefSchema());
        schema.addProperties("next", newHrefSchema());
        schema.addProperties("last", newHrefSchema());
        return schema;
    }

    protected Operation documentGet(OpenAPI openApi, PathItem pathItem, String operationPath, Request req) {
        Collection coll = req.getCollection();
        if(coll == null)
            return  null;

        Operation op = new Operation();

        String description = "A specific " + coll.getSingularDisplayName() + " object";
        op.setDescription(description);

        ApiResponses resps = new ApiResponses();
        op.setResponses(resps);

        Schema resourceRef = newComponentRefSchema(coll.getSingularDisplayName());

        MediaType mt = new MediaType();
        mt.setSchema(resourceRef);

        Content content = new Content();
        content.addMediaType("application/json", mt);

        ApiResponse resp = new ApiResponse();
        resp.setContent(content);
        resp.setDescription(description);

        resps.addApiResponse("200", resp);

        return op;
    }

    public Operation documentList(OpenAPI openApi, PathItem pathItem, String operationPath, Request req) {

        Collection coll = req.getCollection();
        if(coll == null)
            return  null;


        Operation op = new Operation();

        String description = "A pageable list of all " + coll.getSingularDisplayName() + " resources the user has access to and also match any query parameters.  The list may be empty.";
        op.setDescription(description);

        ApiResponses resps = new ApiResponses();
        op.setResponses(resps);

        Schema refSchema = newComponentRefSchema("Get" + coll.getPluralDisplayName() + "Result");

        MediaType mt = new MediaType();
        mt.setSchema(refSchema);


        Parameter page = new Parameter();
        page.setSchema(newTypeSchema("string"));
        page.setDescription("The optional value used to compute the 'offset' of the first resource returned as 'offset'='page'*'limit'.  If an 'offset' parameter is also supplied it will be used instead of the 'page' parameter.");
        page.setName("page");
        page.setIn("query");
        op.addParametersItem(page);

        Parameter limit = new Parameter();
        limit.setDescription("The optional number of resources to return.  Unless overridden by other configuration the default value is '100'");
        limit.setSchema(newTypeSchema("string"));
        limit.setName("size");
        limit.setIn("query");
        op.addParametersItem(limit);

        Parameter sort = new Parameter();
        sort.setDescription("An optional comma separated list of json property names use to order the results.  Each property may optionally be prefixed with '-' to specify descending order.");
        sort.setSchema(newTypeSchema("string"));
        sort.setName("sort");
        sort.setIn("query");
        op.addParametersItem(sort);

//        Parameter offset = new Parameter();
//        offset.setDescription("The optional offset of the first resource to return.  This value will take priority over 'page' if both are supplied.");
//        offset.setSchema(newTypeSchema("string"));
//        offset.setName("offset");
//        offset.setIn("query");
//        op.addParametersItem(offset);


        Parameter q = new Parameter();
        String desc = "An RQL formatted filter statement that allows you to retrieve only the specific resources you require.  See 'Overview->Querying' for more documentation on available functions and syntax.";
        q.setDescription(desc);
        //q.setExample("q=eq(jsonPropertyName,value1),in(anotherJsonProperty,value2)");
        q.setSchema(newTypeSchema("string"));
        q.setName("q");
        q.setIn("query");
        op.addParametersItem(q);

        Parameter includes = new Parameter();
        includes.setDescription("An optional comma separated list of json properties to include in the response.  If this field is not supplied, then any field not listed in the 'excludes' parameter are returned.  When using the 'expands' parameter, you can use 'dot' path notation to reference inclusion of nested properties.");
        includes.setSchema(newTypeSchema("string"));
        includes.setName("includes");
        includes.setIn("query");
        op.addParametersItem(includes);

        Parameter excludes = new Parameter();
        excludes.setDescription("An optional comma separated list of json properties you specifically do not want to be included in the response. When using the 'expands' parameter, you can use 'dot' path notation to reference exclusion of nested properties.");
        excludes.setSchema(newTypeSchema("string"));
        excludes.setName("excludes");
        excludes.setIn("query");
        op.addParametersItem(excludes);

        Parameter expands = new Parameter();
        expands.setDescription("An optional comma separated lists of relationship names that should be expanded in the response. You can reference any number of nesting using 'dot' path notation.");
        expands.setSchema(newTypeSchema("string"));
        expands.setName("expands");
        expands.setIn("query");
        op.addParametersItem(expands);



        Parameter explain = new Parameter();
        desc = "When accessed via localhost (developer mode) only, when set to 'true', a detailed explanation of the Actions and underlying queries run will be added to the response.  The response will not be valid json.";
        explain.setDescription(desc);
        explain.setSchema(newTypeSchema("boolean"));
        explain.setName("explain");
        explain.setIn("query");
        op.addParametersItem(explain);

        Content content = new Content();
        content.addMediaType("application/json", mt);

        ApiResponse resp = new ApiResponse();
        resp.setContent(content);
        resp.setDescription(description);

        resps.addApiResponse("200", resp);

        return op;
    }


    public Operation documentRelated(OpenAPI openApi, PathItem pathItem, String operationPath, Request req) {

        Relationship rel = req.getRelationship();

        //TODO -- what about relationship cardinality and get vs list
        Operation op = new Operation();


        Collection parent = rel.getCollection();
        Collection related = rel.getRelated();

        String description = "Retrieves all of the " + related.getPluralDisplayName() + " related to the " + parent.getSingularDisplayName();
        op.setDescription(description);

        ApiResponses resps = new ApiResponses();
        op.setResponses(resps);


        Schema schema = new Schema();
        schema.set$ref("#/components/schemas/" + related.getSingularDisplayName());

        MediaType mt = new MediaType();
        mt.setSchema(schema);

        Content content = new Content();
        content.addMediaType("application/json", mt);

        ApiResponse resp = new ApiResponse();
        resp.setContent(content);
        resp.setDescription(description);

        resps.addApiResponse("200", resp);

        return op;
    }

    public Operation documentPost(OpenAPI openApi, PathItem pathItem, String operationPath, Request req) {

        Collection coll = req.getCollection();
        if(coll == null)
            return  null;

        Operation op = new Operation();

        String desc = "Creates a new " + coll.getSingularDisplayName() + " resource.";
        op.setDescription(desc);

        ApiResponses resps = new ApiResponses();
        op.setResponses(resps);

        Schema schema = new Schema();
        schema.set$ref("#/components/schemas/" + coll.getSingularDisplayName());

        MediaType mt = new MediaType();
        mt.setSchema(schema);

        Content content = new Content();
        content.addMediaType("application/json", mt);

        RequestBody requestBody = new RequestBody();
        requestBody.setContent(content);
        op.setRequestBody(requestBody);

        ApiResponse resp = new ApiResponse();
        resp.setContent(content);
        resp.setDescription(desc);

        resps.addApiResponse("201", resp);

        return op;
    }

    public Operation documentPut(OpenAPI openApi, PathItem pathItem, String operationPath, Request req) {

        Collection coll = req.getCollection();
        if(coll == null)
            return  null;

        Operation put = new Operation();

        String desc = "Updates an existing " + coll.getSingularDisplayName() + " resource.  Properties of the existing resource that are not supplied in the request body will not be updated.";
        put.setDescription(desc);

        ApiResponses resps = new ApiResponses();
        put.setResponses(resps);

        Schema schema = new Schema();
        schema.set$ref("#/components/schemas/" + coll.getSingularDisplayName());

        MediaType mt = new MediaType();
        mt.setSchema(schema);

        Content content = new Content();
        content.addMediaType("application/json", mt);

        RequestBody requestBody = new RequestBody();
        requestBody.setContent(content);
        put.setRequestBody(requestBody);

        ApiResponse resp = new ApiResponse();
        resp.setContent(content);
        resp.setDescription(desc);

        resps.addApiResponse("201", resp);

        return put;
    }

    public Operation documentPatch(OpenAPI openApi, PathItem pathItem, String operationPath, Request req) {
        return new Operation();
    }

    public Operation documentDelete(OpenAPI openApi, PathItem pathItem, String operationPath, Request req) {

        Collection coll = req.getCollection();
        if(coll == null)
            return  null;

        Operation op = new Operation();

        String desc = "Deletes an existing " + coll.getSingularDisplayName() + " resource.";
        op.setDescription(desc);

        ApiResponses resps = new ApiResponses();
        op.setResponses(resps);

        ApiResponse resp = new ApiResponse();
        resps.addApiResponse("204", resp);
        resp.setDescription(desc);

        return op;
    }


    /*
     * Replaces the var name which is "_collection" to the name of the collection
     * @param path
     * @param collection
     * @return
     */
     Path fixCollectionKeyPathParam(Path path, Collection coll){
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
    Path fixResourceKeyPathParam(Path path, Collection coll){
        path = new Path(path);


        for (int i = 0; i < path.size(); i++) {
            if (Request.RESOURCE_KEY.equalsIgnoreCase(path.getVarName(i))) {
                String varName = null;
                String regex = path.getRegex(i);
                Index pk = coll.getPrimaryIndex();
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


    Path fixRelationshipKeyPathParam(Path path, Relationship rel){
        path = new Path(path);
        if(rel == null)
            return path;

        for (int i = 0; i < path.size(); i++) {
            if (Request.RELATIONSHIP_KEY.equalsIgnoreCase(path.getVarName(i))) {
                path.set(i, rel.getName());
            }
        }
        return path;
    }

    Schema newTypeSchema(String type) {
        Schema schema = new Schema();
        schema.setType(type);
        return schema;
    }

    Schema newHrefSchema(){
        Schema href = new Schema();
        href.addProperties("href", newTypeSchema("string"));
        return href;
    }



    Schema newComponentRefSchema(String name){
        Schema schema = new Schema();
        schema.set$ref("#/components/schemas/" + name);
        return schema;
    }



    String beautifyTag(String str){
        str = str.replace("_", " ");

        StringBuffer buff = new StringBuffer();
        boolean wasLower = !Character.isUpperCase(str.charAt(0));

        for(int i=0 ;i<str.length(); i++){

            char c = str.charAt(i);
            boolean isUpper = Character.isUpperCase(c);

            if(c == ' '){
                wasLower = true;
                buff.append(" ");
            }
            else if(wasLower && isUpper){
                buff.append(" ");
                buff.append(c);
            }else{
                buff.append(c);
            }
            wasLower = !isUpper;
        }
        return buff.toString();
    }


    static Path consumeToNext(Path epColPath, Path candidatePath) {

        Path added = new Path();

        while (epColPath.size() > 0) {
            if (epColPath.isWildcard(0)) {
                epColPath.remove(0);
                return added;
            }

            boolean isStatic = epColPath.isStatic(0);
            String  varName  = isStatic ? null : epColPath.getVarName(0);
            String  part     = epColPath.remove(0);

            if(part.startsWith("["))
                part = part.substring(1, part.length());
            if(part.endsWith("]"))
                part = part.substring(0, part.length() -1);

            added.add(part);
            candidatePath.add(part);

            if (varName != null && Utils.in(varName, Request.COLLECTION_KEY, Request.RESOURCE_KEY, Request.RELATIONSHIP_KEY))
                return added;
        }

        return added;
    }
}
