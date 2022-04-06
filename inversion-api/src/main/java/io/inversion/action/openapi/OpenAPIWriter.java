package io.inversion.action.openapi;

import io.inversion.*;
import io.inversion.utils.Task;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public interface OpenAPIWriter<T extends OpenAPIWriter> {

    default void hook_documentOp(Task docChain, OpenAPI openApi, List<Op> ops, Op op, Map<Object, Schema> schemas) {
        Operation operation = null;
        switch (op.getFunction()) {
            case GET:
                operation = documentOpGet(docChain, openApi, ops, op, schemas);
                break;
            case FIND:
                operation = documentOpFind(docChain, openApi, ops, op, schemas);
                break;
            case RELATED:
                operation = documentOpRelated(docChain, openApi, ops, op, schemas);
                break;
            case POST:
                operation = documentOpPost(docChain, openApi, ops, op, schemas);
                break;
//            case "batch_post":
//                documentBatchPost(openApi, op);
//                break;
            case PUT:
                operation = documentOpPut(docChain, openApi, ops, op, schemas);
                break;
//            case "batch_put":
//                documentBatchPut(openApi, op);
//                break;
//            case PATCH:
//                documentPatch(openApi, op);
//                break;
            case DELETE:
                operation = documentOpDelete(docChain, openApi, ops, op, schemas);
                break;
//            case "batch_delete":
//                documentBatchDelete(openApi, op);
//                break;
        }

        if (operation != null) {
            if (op.getCollection() != null) {
                String tag  = beautifyTag(op.getCollection().getSingularDisplayName());
                List   tags = operation.getTags();
                if (tags != null && !tags.contains(tag))
                    operation.addTagsItem(tag);
            }
        }
    }

    default Operation documentOpGet(Task docChain, OpenAPI openApi, List<Op> ops, Op op, Map<Object, Schema> schemas) {
        Operation operation = openApi.getPaths().get(op.getOperationPath()).getGet();
        if (operation == null) {
            String description = getDescription(op);
            String schemaName  = documentResponseSchema(docChain, openApi, ops, op, schemas);
            operation = buildOperation(op, description, null, schemaName, "200", "404");
            openApi.getPaths().get(op.getOperationPath()).setGet(operation);
        }
        documentQueryParams(docChain, openApi, operation, op);
        return operation;
    }

    default Operation documentOpFind(Task docChain, OpenAPI openApi, List<Op> ops, Op op, Map<Object, Schema> schemas) {
        Operation operation = openApi.getPaths().get(op.getOperationPath()).getGet();
        if (operation == null) {
            String description    = getDescription(op);
            String responseSchema = documentResponseSchema(docChain, openApi, ops, op, schemas);
            operation = buildOperation(op, description, null, responseSchema, "200");
            openApi.getPaths().get(op.getOperationPath()).setGet(operation);
        }
        documentQueryParams(docChain, openApi, operation, op);
        return operation;
    }


    default Operation documentOpRelated(Task docChain, OpenAPI openApi, List<Op> ops, Op op, Map<Object, Schema> schemas) {
        Operation operation = openApi.getPaths().get(op.getOperationPath()).getGet();
        if (operation == null) {
            String description    = getDescription(op);
            String responseSchema = documentResponseSchema(docChain, openApi, ops, op, schemas);
            operation = buildOperation(op, description, null, responseSchema, "200");
            openApi.getPaths().get(op.getOperationPath()).setGet(operation);
        }

        documentQueryParams(docChain, openApi, operation, op);

        //-- adds this relationship to the {collection}/{resource} endpoint parent
//        for(op temp : opsToDoc){
//            if("GET".equalsIgnoreCase(temp.req.getMethod()) //
//             && temp.req.getCollection() == op.getCollection() //
//             && temp.req.getResourceKey() != null //
//             && temp.req.getRelationshipKey() == null){
//
//                //TODO: con't document releated if we can't find the actual endoint for the relateds
//
//
//
//                PathItem item = openApi.getPaths().get(temp.operationPath);
//                if(item != null){
//                    Operation parentGet = item.getGet();
//                    if(parentGet != null){
//                        ApiResponses resps = parentGet.getResponses();
//                        if(resps != null) {
//                            ApiResponse ok = resps.get("200");
//                            if (ok != null) {
//                                Link link = new Link();
//                                link.setOperationId(op.getName());
//                                link.setDescription(op.req.getRelationship().getName());
//                                ok.link("link-" + op.getName(), link);
//                                //--TODO document path params
//                            }
//                        }
//                    }
//                }
//            }
        //     }
        return operation;
    }


    default void removeReadOnlyProperties(OpenAPI openApi, Collection coll, String schemaName) {
        Schema schema = openApi.getComponents().getSchemas().get(schemaName);
        if (schema != null) {
            for (Property prop : coll.getProperties()) {
                if (prop.isReadOnly()) {
                    schema.getProperties().remove(prop.getName());
                }
            }
        }
    }

    default Operation documentOpPost(Task docChain, OpenAPI openApi, List<Op> ops, Op op, Map<Object, Schema> schemas) {
        Operation operation = openApi.getPaths().get(op.getOperationPath()).getPost();
        if (operation == null) {
            String description   = getDescription(op);
            String requestSchema = documentRequestSchema(docChain, openApi, ops, op, schemas);

            Collection coll = op.getRelationship() != null ? op.getRelationship().getRelated() : op.getCollection();
            removeReadOnlyProperties(openApi, coll, requestSchema);

            String responseSchema = documentResponseSchema(docChain, openApi, ops, op, schemas);
            operation = buildOperation(op, description, requestSchema, responseSchema, "201");
            openApi.getPaths().get(op.getOperationPath()).setPost(operation);
        }
        return operation;
    }


    default Operation documentOpPut(Task docChain, OpenAPI openApi, List<Op> ops, Op op, Map<Object, Schema> schemas) {
        Operation operation = openApi.getPaths().get(op.getOperationPath()).getPost();
        if (operation == null) {
            String description    = getDescription(op);
            String requestSchema  = documentRequestSchema(docChain, openApi, ops, op, schemas);
            String responseSchema = documentResponseSchema(docChain, openApi, ops, op, schemas);
            operation = buildOperation(op, description, requestSchema, responseSchema, "201", "404");
            openApi.getPaths().get(op.getOperationPath()).setPut(operation);
        }

        //--TODO: add this operation as a link to GET

        return operation;
    }

    default Operation documentOpPatch(Task docChain, OpenAPI openApi, List<Op> ops, Op op, Map<Object, Schema> schemas) {
        return null;
        //--TODO: implement me...make DbPatchAction first
        //--TODO: add this operation as a link to GET
    }

    default Operation documentOpDelete(Task docChain, OpenAPI openApi, List<Op> ops, Op op, Map<Object, Schema> schemas) {
        Operation operation = openApi.getPaths().get(op.getOperationPath()).getPost();
        if (operation == null) {
            String description = getDescription(op);
            operation = buildOperation(op, description, null, null, "204");
            openApi.getPaths().get(op.getOperationPath()).setDelete(operation);
            addResponse(operation, op, "404");
        }

        return operation;
    }


    default String documentRequestSchema(Task docChain, OpenAPI openApi, List<Op> ops, Op op, Map<Object, Schema> schemas) {
        return documentResourceSchema(docChain, openApi, ops, op, true, schemas);
    }

    default String documentResponseSchema(Task docChain, OpenAPI openApi, List<Op> ops, Op op, Map<Object, Schema> schemas) {

        return documentResourceSchema(docChain, openApi, ops, op, false, schemas);
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
    }

    default String documentResourceSchema(Task docChain, OpenAPI openApi, List<Op> ops, Op op, boolean request, Map<Object, Schema> schemas) {

        if (op.getCollection() == null) {
            System.out.println("NO COLLECTION: " + op.getName());
            return "unknown";
        }

        Collection coll = op.getCollection();
        //String     schemaName = coll.getSingularDisplayName() + (request ? "Request" : "Response");
        String schemaName = op.getName() + (request ? "Request" : "Response");
        Schema schema     = openApi.getComponents().getSchemas().get(schemaName);

        if (schema != null)
            return schemaName;

        if (coll.getSchemaRef() != null) {
            schema = newComponentRefSchema(coll.getSchemaRef());
            openApi.getComponents().addSchemas(schemaName, schema);

        } else {
            schema = new Schema();
            openApi.getComponents().addSchemas(schemaName, schema);


            if (coll.getDescription() != null) {
                schema.setDescription(coll.getDescription());
            }
            schema.setType("object");

            List<String> requiredProps = new ArrayList<>();
            Index        primaryIndex  = coll.getResourceIndex();

            boolean requirePk = request && op.getFunction().toString().toLowerCase().startsWith("batch");//&& !op.getMethod().equalsIgnoreCase("GET")

//            if (primaryIndex != null && !hidePk)
//                requiredProps.addAll(primaryIndex.getJsonNames());

            //-- TODO filter excludes/includes
            for (Property prop : coll.getProperties()) {

                if(!prop.isDocumented())
                    continue;

                String name = prop.getJsonName();
                String type = prop.getJsonType();

                if(primaryIndex != null && primaryIndex.getProperties().contains(prop)){
                    if(requirePk)
                        requiredProps.add(name);
                }

                Schema propSchema = newTypeSchema(type);
                if (prop.getDescription() != null)
                    propSchema.setDescription(prop.getDescription());

                if (prop.isNullable()) {
                    propSchema.setNullable(true);
                }

                schema.addProperties(name, propSchema);

                if (prop.isRequired() && !requiredProps.contains(name))
                    requiredProps.add(name);
            }
            schema.setRequired(requiredProps);


            for (Relationship rel : coll.getRelationships()) {
                for (Op temp : ops) {
                    if (temp.getFunction() == op.getFunction() && temp.getCollection() == rel.getRelated()) {

                        String childSchema = request ? documentRequestSchema(docChain, openApi, ops, temp, schemas) : documentResponseSchema(docChain, openApi, ops, temp, schemas);

                        if (rel.isManyToOne()) {
                            schema.addProperties(rel.getName(), newComponentRefSchema(childSchema));
                        } else {
                            ArraySchema arr = new ArraySchema();
                            arr.setItems(newComponentRefSchema(childSchema));
                            schema.addProperties(rel.getName(), arr);
                        }

                        break;
                    }
                }
            }

            schemas.put(op, schema);
        }
        return schemaName;
    }

    default Operation buildOperation(Op op, String description, String requestSchema, String responseSchema, String... statuses) {
        Operation operation = new Operation().responses(new ApiResponses()).description(description);
        operation.setOperationId(op.getName());
        if (requestSchema != null) {
            RequestBody body = new RequestBody();
            operation.setRequestBody(body);

            Content content = new Content();
            body.setContent(content);

            MediaType json = new MediaType();
            json.setSchema(newComponentRefSchema(requestSchema));
            content.addMediaType("application/json", json);
        }

        if (statuses != null) {
            for (String status : statuses) {
                addResponse(operation, op, status, null, responseSchema);
            }
        }

        Collection collection = op.getCollection();
        if (collection != null) {
            operation.addTagsItem(beautifyTag(collection.getSingularDisplayName()));
        }

        return operation;
    }

    default String getDescription(Op op) {

        if (op.getCollection() == null)
            return "";

        Collection coll = op.getCollection();
        //GET, FIND, RELATED, POST, PUT, PATCH, DELETE, BATCH_POST, BATCH_PUT, BATCH_PATCH, BATCH_DELETE

        switch (op.getFunction()) {
            case GET:
                return "Retrieve a specific " + coll.getSingularDisplayName() + " object. (" + op.getName() + ")";
            case FIND:
                return "A pageable list of all " + coll.getSingularDisplayName() + " resources the user has access to and also match any query parameters.  The list may be empty. (" + op.getName() + ")";
            case RELATED:
                return "Retrieves all of the " + op.getRelationship().getRelated().getPluralDisplayName() + " related to the " + op.getRelationship().getRelated().getSingularDisplayName() + ". (" + op.getName() + ")";
            case POST:
                return "Creates a new " + coll.getSingularDisplayName() + " resource. (" + op.getName() + ")";
            case PUT:
                return "Updates an existing " + coll.getSingularDisplayName() + " resource.  Properties of the existing resource that are not supplied in the request body will not be updated. (" + op.getName() + ")";
            case PATCH:
                return "";
            case DELETE:
                return "";
            case BATCH_POST:
                return "";
            case BATCH_PUT:
                return "";
            case BATCH_PATCH:
                return "";
            case BATCH_DELETE:
                return op.getName() + ": " + "Deletes an existing " + coll.getSingularDisplayName() + " resource. (" + op.getName() + ")";
        }
        return "";
    }


    default OpenAPIWriter addResponse(Operation operation, Op op, String status) {
        String description = null;
        String schemaName  = null;
        return addResponse(operation, op, status, description, schemaName);
    }

    default OpenAPIWriter addResponse(Operation operation, Op op, String status, String description, String schemaName) {

        if (description == null) {
            switch (status) {
                case "200":
                    description = "OK";
                    break;
                case "201":
                    description = "Created";
                    break;
                case "204":
                    description = "No Content";
                    break;
                case "400":
                    description = "Bad Request";
                    break;
                case "401":
                    description = "Unauthorized";
                    break;
                case "403":
                    description = "Forbidden";
                    break;
                case "404":
                    description = "Not Found";
                    break;
                case "500":
                    description = "Internal Server Error";
                    break;
            }
        }

        if (schemaName == null) {
            if (status != null && "399".compareTo(status) < 0)
                schemaName = "error";
        }

        if (operation.getResponses() == null)
            operation.setResponses(new ApiResponses());

        ApiResponse response = new ApiResponse();
        if (description != null)
            response.setDescription(description);

        if (schemaName != null)
            response.content(new Content().addMediaType("application/json",
                    new MediaType().schema(newComponentRefSchema(schemaName))));

        if (operation.getResponses().get(status) == null) {
            operation.getResponses().addApiResponse(status, response);
        }

        return this;
    }

    default void documentQueryParams(Task docChain, OpenAPI openApi, Operation operation, Op op) {
        for (Param param : op.getParams()) {
            if (param.getIn() == Param.In.QUERY) {
                documentParam(docChain, openApi, operation, op, param);
            }
        }
    }


    default void documentParam(Task docTask, OpenAPI openApi, Operation operation, Op op, Param param) {
        PathItem pi = openApi.getPaths().get(op.getOperationPath());
        if (pi == null)
            return;

        Parameter parameter = new Parameter();
        parameter.setSchema(newTypeSchema(param.getType()));
        parameter.setDescription(param.getDescription());
        parameter.setName(param.getKey());
        parameter.setIn(param.getIn().toString().toLowerCase());
        if (!hasParam(operation, parameter))
            operation.addParametersItem(parameter);
    }


    default boolean hasParam(Operation operation, Parameter param) {
        if (operation.getParameters() == null)
            return false;
        for (Parameter existing : operation.getParameters()) {
            if (param.getName().equalsIgnoreCase(existing.getName()) && param.getIn().equalsIgnoreCase(existing.getIn()))
                return true;
        }
        return false;
    }

    default Schema newTypeSchema(String type) {
        Schema schema = new Schema();
        schema.setType(type);
        return schema;
    }

    default Schema newHrefSchema() {
        Schema schema = new Schema();
        schema.addProperties("href", newTypeSchema("string"));
        return schema;
    }

    default Schema newComponentRefSchema(String nameOrRef) {
        Schema schema = new Schema();
        nameOrRef = getSchemaRef(nameOrRef);
        schema.set$ref(nameOrRef);
        return schema;
    }

    default String getSchemaRef(String nameOrRef) {
        if (!nameOrRef.contains("/"))
            nameOrRef = "#/components/schemas/" + nameOrRef;
        return nameOrRef;
    }

    default String beautifyTag(String str) {
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

    default Op findOp(List<Op> ops, Op.OpFunction fun, Collection collection) {
        for (Op op : ops) {
            if (op.getFunction() == fun && op.getCollection() == collection)
                return op;
        }
        return null;
    }


}
