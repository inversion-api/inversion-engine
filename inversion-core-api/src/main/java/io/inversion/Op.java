package io.inversion;

import io.inversion.utils.Path;
import io.inversion.utils.Utils;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import java.util.*;
import java.util.regex.Pattern;

public final class Op implements Comparable<Op> {

    public enum OpFunction {GET, FIND, RELATED, POST, PUT, PATCH, DELETE, BATCH_POST, BATCH_PUT, BATCH_PATCH, BATCH_DELETE}

    String name        = null;
    String method      = null;
    Path   path        = null;
    String description = null;

    boolean    internal = false;
    OpFunction function = null;

    Engine engine = null;
    Api    api    = null;

    Endpoint endpoint          = null;
    Path     endpointPathMatch = null;

    Path actionPathMatch = null;

    //-- action,path/isEndpointAction
    List<Triple<Action, Path, Boolean>> actionPathMatches = new ArrayList();

    Db   db          = null;
    Path dbPathMatch = null;

    Collection   collection          = null;
    Path         collectionPathMatch = null;
    Relationship relationship        = null;

    List<Param> params = new ArrayList();

//    public Op copy() {
//        Op op = new Op();
//        op.method = method;
//        op.path = path.copy();
//        op.internal = internal;
//        op.function = function;
//        op.engine = engine;
//        op.api = api;
//        op.endpoint = endpoint;
//        op.endpointPathMatch = endpointPathMatch.copy();
//        op.actionPathMatches = new ArrayList<>(actionPathMatches);
//        op.db = db;
//        op.dbPathMatch = dbPathMatch.copy();
//        op.collection = collection;
//        op.collectionPathMatch = collectionPathMatch;
//        op.relationship = relationship;
//        op.params = new ArrayList<>(params);
//        return op;
//    }

    public Op() {

    }

    public boolean matches(Request req, Path path) {

        if (!getMethod().equalsIgnoreCase(req.getMethod()))
            return false;

        if (!getPath().matches(path))
            return false;

        if (!getEndpoint().matches(req.getMethod(), path))
            return false;

        for (Param param : getParams()) {
            if (param.in == Param.In.PATH) {
                for (Pattern regex : param.getPatterns()) {
                    if (!regex.matcher(path.get(param.getIndex())).matches())
                        return false;
                }
            }
        }

        return true;
    }

    public String getOperationPath() {
        return "/" + path;
    }

    /**
     * Indicates this operation should not be called from clients directly but may be used for internal calls.
     *
     * @return true if <code>internal</code> is true or either endpoint.isInternal() or collection.isInternal() are true
     */
    public boolean isInternal() {
        return internal || (endpoint == null ? false : endpoint.isInternal()) || (collection == null ? false : collection.isLinkTbl());
    }

    public Op withInternal(boolean internal) {
        this.internal = internal;
        return this;
    }

    public String toString() {
        LinkedHashMap props = new LinkedHashMap();
        props.put("name", getName());
        props.put("method", getMethod());
        props.put("path", getPath());
        props.put("collection", (collection != null ? collection.getName() : null));
        props.put("relationship", (relationship != null ? relationship.getName() : null));

        List<String> actStr = new ArrayList<>();
        for (Action action : getActions()) {
            actStr.add(getActionNameString(action));
        }
        props.put("actions", actStr);
        props.put("params", getParams());

        String str = props.toString();
        //str = str.replace("\r", "");
        //str = str.replace("\n", " ");
        return str;
    }

    String getActionNameString(Action action) {
        String name = action.getName();
        if (name == null) {
            name = action.getClass().getName();
            if (name.indexOf(".") > 0)
                name = name.substring(name.lastIndexOf(".") + 1);
        }
        return name;
    }

//    public Op copy() {
//        Op op = new Op();
//        op.api = api;
//        op.method = method;
//        op.serverPathMatch = serverPathMatch.copy();
//
//        op.endpointPathMatch = endpointPathMatch == null ? endpointPathMatch : endpointPathMatch.copy();
//        for (Triple<Action, Path, Boolean> apm : actionPathMatches)
//            op.actionPathMatches.add(new MutableTriple<>(apm.getLeft(), apm.getMiddle().copy(), apm.getRight()));
//
//        return op;
//    }


    public Path getActionPathMatch() {
        return actionPathMatch;
    }

    public Op withActionPathMatch(Path actionPathMatch) {
        this.actionPathMatch = actionPathMatch;
        return this;
    }

    public Op withActionMatch(Action action, Path actionMatchPath, Boolean isEpAction) {
        actionPathMatches.add(new MutableTriple<Action, Path, Boolean>(action, actionMatchPath, isEpAction));

        int offset = 0;
        if(isEpAction){
            for(int i=0; i<endpointPathMatch.size(); i++){
                if(endpointPathMatch.isOptional(i) || endpointPathMatch.isWildcard(i))
                    break;
                offset += 1;
            }
        }
        for(int i=0; i<actionMatchPath.size(); i++){
            if(offset + i >= path.size())
                break;
            if(actionMatchPath.isVar(i)){
                Param p = new Param();
                p.withIn(Param.In.PATH);
                p.withIndex(i + offset);
                p.withKey(actionMatchPath.getVarName(i));
                String regex = actionMatchPath.getRegex(i);
                if(regex != null)
                    p.withRegex(regex);
                withParam(p);
            }
        }


        Collections.sort(actionPathMatches, new Comparator<Triple<Action, Path, Boolean>>() {
            @Override
            public int compare(Triple<Action, Path, Boolean> o1, Triple<Action, Path, Boolean> o2) {
                return o1.getLeft().compareTo(o2.getLeft());
            }
        });

        return this;
    }

    public List<Action> getActions() {
        List<Action> actions = new ArrayList<>();
        for (Triple<Action, Path, Boolean> actionPathMatch : actionPathMatches) {
            actions.add(actionPathMatch.getLeft());
        }
        return actions;
    }

    public boolean hasParams(Param.In in, String... keys) {
        for (String key : keys) {
            boolean found = false;
            for (Param param : params) {
                if ((in == null || in == param.getIn())
                        && key.equalsIgnoreCase(param.getKey())) {
                    found = true;
                    break;
                }
            }
            if (!found)
                return false;
        }
        return true;
    }

    public String getPathParamValue(String key) {
        Path operationPath = getPath();
        for (Param param : params) {
            if (Param.In.PATH == param.getIn() && key.equalsIgnoreCase(param.getKey())) {
                String value = operationPath.get(param.getIndex());
                return value;
            }
        }
        return null;
    }

    public Op withParam(Param param) {

        for (Param p : getParams()) {
            if (p.getKey().equalsIgnoreCase(param.getKey())) {
                if (p.in == Param.In.HOST || p.in == Param.In.SERVER_PATH || p.in == Param.In.PATH) {
                    if (p.in != param.in || p.index != param.index)
                        throw ApiException.new500InternalServerError("You have a configuration error.  You can not have a path param with the same key at different locations.");

                    if (p.getName() == null && param.getName() != null)
                        p.withName(param.getName());

                    if (p.getDescription() == null && param.getDescription() != null)
                        p.withDescription(param.getDescription());

                    param.getRegexs().forEach(r -> p.withRegex(r));

                    if (param.isRequired())
                        p.withRequired(true);

                    return this;
                }
            }
        }
        params.add(param);
        return this;
    }

    public List<Param> getParams() {
        return new ArrayList(params);
    }

    public void removeParam(Param param) {
        params.remove(param);
    }

    public List<Param> getPathParams(int pathIndex) {
        List<Param> found = new ArrayList();
        for (Param p : params) {
            if (p.in != Param.In.PATH || pathIndex != p.getIndex())
                continue;
            found.add(p);
        }
        return found;
    }

    public Param getParam(Param.In in, String key) {
        for (Param p : params) {
            if (p.in == in && key.equalsIgnoreCase(p.getKey()))
                return p;
        }
        return null;
    }

    public int getPathParamCount() {
        int params = 0;
        //-- TODO this is not correct, see Linker
        for (int i = 0; i < path.size(); i++)
            params += 1;
        return params;
    }

    @Override
    public int compareTo(Op o) {
        int val = o == null ? 1 : path.toString().compareTo(o.path.toString());

        if (val == 0) {
            int func1 = functionAsInt(getFunction());
            int func2 = functionAsInt(o.getFunction());
            val = func1 < func2 ? -1 : 1;
        }
        return val;
    }

    public static int functionAsInt(Op.OpFunction func) {
        if (func == null)
            return 0;
        switch (func) {
            case GET:
                return 1;
            case FIND:
                return 2;
            case RELATED:
                return 3;
            case POST:
                return 4;
            case PUT:
                return 5;
            case PATCH:
                return 6;
            case DELETE:
                return 7;
            case BATCH_POST:
                return 8;
            case BATCH_PUT:
                return 9;
            case BATCH_PATCH:
                return 10;
            case BATCH_DELETE:
                return 11;
        }
        throw ApiException.new500InternalServerError("Unsupported OpFunction: " + func);
    }

    public String getName() {

        if (name != null)
            return name;

        Collection collection  = getCollection();

        int pathLength = getPath().size();
        String     defaultName = getPath().isVar(pathLength -1) ? getPath().getVarName(pathLength -1) : getPath().last();

        if (getEndpoint().getName() != null) {
            defaultName = getEndpoint().getName() + Utils.capitalize(defaultName);
        }
        defaultName = cleanNamePart(defaultName);
        defaultName = Utils.beautifyName(defaultName);

        if(defaultName.endsWith("*") && defaultName.length() > 1)
            defaultName = defaultName.substring(0, defaultName.length()-1);


        String singular = Utils.capitalize(collection == null ? defaultName : collection.getSingularDisplayName());
        String plural   = Utils.capitalize(collection == null ? defaultName : collection.getPluralDisplayName());

        String builtName = null;

        OpFunction function = getFunction();
        if (function != null) {
            switch (function) {
                case GET:
                    builtName = "get" + singular;
                    break;
                case FIND:
                    builtName =  "find" + plural;
                    break;
                case RELATED:
                    builtName =  "find" + plural + "Related" + Utils.capitalize(cleanNamePart(getPathParamValue(Request.RELATIONSHIP_KEY)));
                    break;
                case POST:
                    builtName =  "create" + singular;
                    break;
                case PUT:
                    builtName =  "update" + singular;
                    break;
                case PATCH:
                    builtName =  "patch" + singular;
                    break;
                case DELETE:
                    builtName =  "delete" + singular;
                    break;
                case BATCH_POST:
                    builtName =  null;
                    break;
                case BATCH_PUT:
                    builtName =  "batchUpdate" + plural;
                    break;
                case BATCH_PATCH:
                    builtName =  "batchPatch" + plural;
                    break;
                case BATCH_DELETE:
                    builtName =  "batchDelete" + plural;
                    break;
            }

            if(builtName != null){
                Path p = getPath();
                for(int i=0; i<p.size(); i++){
                    if(p.isVar(i)){
                        String key = p.getVarName(i);
                        builtName += "By" + Utils.capitalize(key);
                    }
                }
            }
        }
        return builtName;
    }


    static String cleanNamePart(String part) {
        if (part == null)
            part = "Unknown";
        part = part.replace("{", "");
        part = part.replace("}", "");
        part = part.replace(".", " ");
        part = part.replace("_", "");
        return part;
    }

    public Op withName(String name) {
        this.name = name;
        return this;
    }

    public String getMethod() {
        return method;
    }

    public Op withMethod(String method) {
        this.method = method;
        return this;
    }

    public OpFunction getFunction() {
        if (function != null)
            return function;

        String method = getMethod().toUpperCase();

        if (hasParams(Param.In.PATH, Request.COLLECTION_KEY, Request.RESOURCE_KEY, Request.RELATIONSHIP_KEY)) {

            switch (method) {
                case "GET":
                    return Op.OpFunction.RELATED;
            }
            return null;

        } else if (hasParams(Param.In.PATH, Request.COLLECTION_KEY, Request.RESOURCE_KEY)) {
            switch (method) {
                case "GET":
                    return Op.OpFunction.GET;
                case "PUT":
                    return Op.OpFunction.PUT;
                case "PATCH":
                    return Op.OpFunction.PATCH;
                case "DELETE":
                    return Op.OpFunction.DELETE;
            }
            return null;

        } else if (hasParams(Param.In.PATH, Request.COLLECTION_KEY)) {
            switch (method) {
                case "GET":
                    return Op.OpFunction.FIND;
                case "POST":
                    return Op.OpFunction.POST;
                case "PUT":
                    return Op.OpFunction.BATCH_PUT;
                case "PATCH":
                    return Op.OpFunction.BATCH_PATCH;
                case "DELETE":
                    return Op.OpFunction.BATCH_DELETE;
            }
            return null;
        } else {
            switch (method) {
                case "GET":
                case "POST":
                case "PUT":
                case "PATCH":
                case "DELETE":
                    return Op.OpFunction.valueOf(method);
            }
            return null;
        }
    }

    public Op withFunction(OpFunction function) {
        this.function = function;
        return this;
    }

    public Path getPath() {
        return path;
    }

    public Op withPath(Path path) {

        if (this.path == null)
            this.path = path.copy();

        if (this.path.size() != path.size())
            throw ApiException.new500InternalServerError("Paths sizes are different '{}', '{}'", this.path, path);

        for (int i = 0; i < path.size(); i++) {
            boolean isVar = path.isVar(i);

            if (isVar) {
                Param  param = new Param(path.getVarName(i), i);
                String regex = path.getRegex(i);
                if (regex != null)
                    param.withRegex(regex);
                withParam(param);
            }

            if (this.path.isVar(i) && !path.isVar(i)) {
                this.path.set(i, path.get(i));
            } else if (this.path.isVar(i) && isVar) {
                String currentName = this.path.getVarName(i);
                String newName     = this.path.getVarName(i);
                if (currentName.startsWith("_") && !newName.startsWith("_"))
                    this.path.set(i, newName);
            }
        }

        return this;
    }

    public Path getEndpointPathMatch() {
        return endpointPathMatch;
    }

    public Op withEndpointPathMatch(Path endpointPathMatch) {
        this.endpointPathMatch = endpointPathMatch;
        return this;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public Op withEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
        return this;
    }


    public Collection getCollection() {
        return collection;
    }

    public Op withCollection(Collection collection) {
        this.collection = collection;
        return this;
    }

    public Relationship getRelationship() {
        return relationship;
    }

    public Op withRelationship(Relationship relationship) {
        this.relationship = relationship;
        return this;
    }


    public Api getApi() {
        return api;
    }

    public Op withApi(Api api) {
        this.api = api;
        return this;
    }

    public Path getDbPathMatch() {
        return dbPathMatch;
    }

    public Op withDbMatchPath(Path dbMatchPath) {
        this.dbPathMatch = dbMatchPath;
        return this;
    }

    public Db getDb() {
        return db;
    }

    public Op withDb(Db db) {
        this.db = db;
        return this;
    }

    public Engine getEngine() {
        return engine;
    }

    public Op withEngine(Engine engine) {
        this.engine = engine;
        return this;
    }


    public Op withDbPathMatch(Path dbPathMatch) {
        this.dbPathMatch = dbPathMatch;
        return this;
    }

    public Path getCollectionPathMatch() {
        return collectionPathMatch;
    }

    public Op withCollectionPathMatch(Path collectionPathMatch) {
        this.collectionPathMatch = collectionPathMatch;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Op withDescription(String description) {
        this.description = description;
        return this;
    }

    public List<Triple<Action, Path, Boolean>> getActionPathMatches() {
        return new ArrayList(actionPathMatches);
    }

    public boolean isEpAction(Action action){
        for(Triple<Action, Path, Boolean> match : actionPathMatches){
            if(match.getLeft() == action && match.getRight())
                return true;
        }
        return false;
    }

}