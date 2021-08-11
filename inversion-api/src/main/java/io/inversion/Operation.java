package io.inversion;

import io.inversion.utils.Path;
import io.inversion.utils.Utils;

import java.util.*;
import java.util.regex.Pattern;

public class Operation implements Comparable<Operation> {

    protected String name               = null;
    protected String function           = null;
    protected String method             = null;
    protected Path   operationMatchPath = null;
    protected Path   displayPath        = null;
    protected Path   apiMatchPath       = null;
    protected Path   epMatchPath        = null;
    protected Path   collMatchPath      = null;

    protected Api                    api          = null;
    protected Endpoint               endpoint     = null;
    protected Collection             collection   = null;
    protected Relationship           relationship = null;
    protected Map<String, Parameter> params       = new HashMap();

    protected Map properties = new HashMap();

    //protected  Operation resourceGet = null;
    //protected  Operation relatedList = null;

    String error = null;

    public Operation() {

    }

    public Operation(Api api, String function, String method, Path operationPath, Path apiMatchPath, Path epMatchPath, Path collMatchPath, Endpoint endpoint, Collection collection, Relationship relationship) {
        this.api = api;
        this.function = function;
        this.method = method;
        this.apiMatchPath = new Path(apiMatchPath);
        this.epMatchPath = new Path(epMatchPath);
        this.collMatchPath = collMatchPath == null ? null : new Path(collMatchPath);
        this.endpoint = endpoint;
        this.collection = collection;
        this.relationship = relationship;
        this.operationMatchPath = new Path(operationPath);
        this.displayPath = new Path(operationPath);

        int offset = 0;
        int max    = operationPath.size();
        prepPath(this.displayPath, offset, max);
        prepPath(new Path(apiMatchPath), offset, max);


        for (int i = 0; i < apiMatchPath.size() && !apiMatchPath.isOptional(i) && !apiMatchPath.isWildcard(i); i++)
            offset += 1;

        prepPath(new Path(epMatchPath), offset, max);

        if(collection != null){
            Path dbPathMatch = collection.getDb() == null ? null : collection.getDb().getEndpointPath();
            if (dbPathMatch != null)
                prepPath(new Path(dbPathMatch), offset, max);
        }


        for (int i = 0; i < epMatchPath.size() && !epMatchPath.isOptional(i) && !epMatchPath.isWildcard(i); i++)
            offset += 1;

        if(collMatchPath != null)
            prepPath(new Path(collMatchPath), offset, max);


        String epName = endpoint.getName() != null ? endpoint.getName() : ((api.getEndpoints().indexOf(endpoint) + 1) + "");

        String singular = Utils.capitalize(collection == null ? epName : collection.getSingularDisplayName());
        String plural   = Utils.capitalize(collection == null ? epName : collection.getPluralDisplayName());

        switch (function.toLowerCase()) {
            case "get":
                name = "get" + singular;
                break;
            case "list":
                name = "find" + plural;
                break;
            case "related":
                name = "findRelated" + Utils.capitalize(relationship.getRelated().getPluralDisplayName());
                break;
            case "post":
                name = "create" + singular;
                break;
            case "batch_post":
                name = "createMultiple" + plural;
                break;
            case "put":
                name = "update" + singular;
                break;
            case "batch_put":
                name = "updateMultiple" + plural;
                break;
            case "patch":
                name = "patch" + plural;
                break;
            case "delete":
                name = "delete" + singular;
                break;
            case "batch_delete":
                name = "deleteMultiple" + plural;
                break;
            default:
                throw new ApiException("Unknown function {}", function);
        }

        for (int i = 0; i < displayPath.size(); i++) {
            if (displayPath.isVar(i))
                name += "By" + Utils.capitalize(displayPath.getVarName(i));
        }
    }

    public List<Parameter> getParams(int pathIndex) {
        List<Parameter> found = new ArrayList();
        for (Parameter p : getParams().values()) {
            if (pathIndex != p.getIndex())
                continue;
            found.add(p);
        }
        return found;
    }

    public List<String> getParamKeys(int pathIndex) {
        List<String> found = new ArrayList();
        for (Parameter p : getParams().values()) {
            if (pathIndex != p.getIndex())
                continue;
            found.add(p.getKey());
        }
        if (found.size() > 1)
            Collections.sort(found);
        return found;
    }

    public int getPathParamCount(){
        int params = 0;
        for(int i=0;i< operationMatchPath.size();i++)
            params +=1;
        return params;
    }

    void prepPath(Path path, int offset, int max) {

        for (int i = 0; i < path.size() && !path.isWildcard(i) && offset + i < max; i++) {
            String part = path.get(i);
            if (path.isVar(i)) {

                String varName = path.getVarName(i);
                part = varName;

                String regex = path.getRegex(i);

                if (Request.COLLECTION_KEY.equalsIgnoreCase(varName)) {
                    if (regex != null && !Utils.isRegex(regex))
                        part = regex;
                    else if (collection != null)
                        part = collection.getName();
                    else
                        part = "{" + varName + "}";

                    if (regex == null && collection != null)
                        regex = collection.getName();
                } else if (Request.RESOURCE_KEY.equalsIgnoreCase(varName) && collection != null) {
                    Index pk = collection.getPrimaryIndex();
                    if (pk != null && pk.size() == 1) {
                        part = pk.getJsonName(0);
                        regex = regex != null ? regex : pk.getProperty(0).getRegex();
                    } else {
                        part = collection.getName() + "Id";
                        for (int j = 0; j < 10; j++) {
                            if (collection.getProperty(varName) == null)
                                break;
                            part = "_" + varName;
                        }
                    }
                    part = "{" + part + "}";
                } else if (Request.RELATIONSHIP_KEY.equalsIgnoreCase(varName)) {
                    if (relationship != null)
                        part = relationship.getName();
                    else if (regex != null && !Utils.isRegex(regex))
                        part = regex;
                    else
                        part = "{" + varName + "}";

                    if (regex == null && relationship != null)
                        regex = relationship.getName();
                } else {
                    part = "{" + varName + "}";
                }

                Parameter param = params.get(varName);

                if (param == null)
                    param = new Parameter();
                else if (param.getIndex() != offset + i)
                    error = "Your Api configuration references parameter '" + param.getName() + "' at multiple places in the Url.  I parameter name can be referenced in multiple pieces of config but can only exist in one logical place in a single operation path.";

                param.withIndex(offset + i);
                param.withKey(varName);
                param.withRequired(true);
                param.withIn("query");
                param.withRegex(regex);

                params.put(varName, param);
            }

            if (part.startsWith("[")) {
                part = part.substring(1);
            }
            if (part.endsWith("]"))
                part = part.substring(0, part.length() - 1);

            path.set(i, part);
        }
    }

    public boolean matches(Request req) {

        if (!req.isMethod(this.method))
            return false;

        Path reqPath = req.getUrl().getPath();
        if (operationMatchPath.matches(reqPath)) {

            for (Parameter param : params.values()) {

                switch (param.getIn().toLowerCase()) {
                    case "query":
                        int index = param.getIndex();
                        if (index >= reqPath.size())
                            return false;
                        String value = reqPath.get(index);
                        for (Pattern p : param.getPatterns()) {
                            if (!p.matcher(value).matches())
                                return false;
                        }
                }
            }
            return true;
        }
        return false;
    }


    public String toString() {
        return method + " - " + displayPath + " - " + params;
    }

    @Override
    public int compareTo(Operation o) {
        int val = o == null ? 1 : operationMatchPath.toString().compareTo(o.operationMatchPath.toString());

        if (val == 0) {
            int method1 = methodAsInt(method);
            int method2 = methodAsInt(o.method);
            val = method1 < method2 ? -1 : 1;
        }
        return val;
    }

    int methodAsInt(String method) {
        switch (method.toLowerCase()) {
            case "get":
                return 0;
            case "post":
                return 1;
            case "put":
                return 2;
            case "patch":
                return 3;
            case "delete":
                return 4;
        }
        return 0;
    }

    public String getName() {
        return name;
    }

    public Operation withName(String name) {
        this.name = name;
        return this;
    }

    public String getFunction() {
        return function;
    }

    public Operation withFunction(String function) {
        this.function = function;
        return this;
    }

    public String getMethod() {
        return method;
    }

    public Operation withMethod(String method) {
        this.method = method;
        return this;
    }

    public Path getOperationMatchPath() {
        return operationMatchPath;
    }

    public Operation withOperationPath(Path operationPath) {
        this.operationMatchPath = operationPath;
        return this;
    }

    public Path getDisplayPath() {
        return displayPath;
    }

    public Operation withDisplayPath(Path displayPath) {
        this.displayPath = displayPath;
        return this;
    }

    public Path getApiMatchPath() {
        return apiMatchPath;
    }

    public Operation withApiMatchPath(Path apiMatchPath) {
        this.apiMatchPath = apiMatchPath;
        return this;
    }

    public Path getEpMatchPath() {
        return epMatchPath;
    }

    public Operation withEpMatchPath(Path epMatchPath) {
        this.epMatchPath = epMatchPath;
        return this;
    }

    public Path getCollMatchPath() {
        return collMatchPath;
    }

    public Operation withCollMatchPath(Path collMatchPath) {
        this.collMatchPath = collMatchPath;
        return this;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public Operation withEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public Collection getCollection() {
        return collection;
    }

    public Operation withCollection(Collection collection) {
        this.collection = collection;
        return this;
    }

    public Relationship getRelationship() {
        return relationship;
    }

    public Operation withRelationship(Relationship relationship) {
        this.relationship = relationship;
        return this;
    }

    public Map<String, Parameter> getParams() {
        return params;
    }

    public Operation withParams(Map<String, Parameter> params) {
        this.params = params;
        return this;
    }

    public Map getProperties() {
        return properties;
    }

    public Operation withProperties(Map properties) {
        this.properties = properties;
        return this;
    }

    public String getError() {
        return error;
    }

    public Operation withError(String error) {
        this.error = error;
        return this;
    }

    public Api getApi() {
        return api;
    }

    public Operation withApi(Api api) {
        this.api = api;
        return this;
    }
}