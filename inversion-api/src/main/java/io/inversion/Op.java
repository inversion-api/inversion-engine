package io.inversion;

import io.inversion.utils.Path;
import io.inversion.utils.Utils;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import java.util.*;
import java.util.regex.Pattern;

public class Op implements Comparable<Op> {

    public enum OpFunction {GET, FIND, RELATED, POST, PUT, PATCH, DELETE, BATCH_POST, BATCH_PUT, BATCH_PATCH, BATCH_DELETE}

    String name   = null;
    String method = null;
    Path   path   = null;

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

    public Op copy() {
        Op op = new Op();
        op.method = method;
        op.path = path.copy();
        op.internal = internal;
        op.function = function;
        op.engine = engine;
        op.api = api;
        op.endpoint = endpoint;
        op.endpointPathMatch = endpointPathMatch.copy();
        op.actionPathMatches = new ArrayList<>(actionPathMatches);
        op.db = db;
        op.dbPathMatch = dbPathMatch.copy();
        op.collection = collection;
        op.collectionPathMatch = collectionPathMatch;
        op.relationship = relationship;
        op.params = new ArrayList<>(params);
        return op;
    }

    public Op() {

    }

    public boolean matches(Request req, Path path) {

        if(!getMethod().equalsIgnoreCase(req.getMethod()))
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
        for(Action action : getActions()){
            actStr.add(getActionNameString(action));
        }
        props.put("actions", actStr);
        props.put("params", getParams());
        return props.toString();
    }

    String getActionNameString(Action action){
        String name = action.getName();
        if(name == null){
            name = action.getClass().getName();
            if(name.indexOf(".") > 0)
                name = name .substring(name.lastIndexOf(".") + 1);
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

        //-- updates the vars in the path for optimal display
        if (path != null)
            withPath(path);
        return this;
    }

    public List<Param> getParams() {
        return params;
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


    //    public List<String> getParamKeys(int pathIndex) {
//        List<String> found = new ArrayList();
//        for (Param p : params) {
//            if (pathIndex != p.getIndex())
//                continue;
//            found.add(p.getKey());
//        }
//        if (found.size() > 1)
//            Collections.sort(found);
//        return found;
//    }
//
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
            int func1 = functionAsInt(function);
            int func2 = functionAsInt(o.function);
            val = func1 < func2 ? -1 : 1;
        }
        return val;
    }

    public static int functionAsInt(Op.OpFunction func) {
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
        return name;
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
        return function;
    }

    public Op withFunction(OpFunction function) {
        this.function = function;
        return this;
    }

    public Path getPath() {
        return path;
    }

    public Op withPath(Path requestPath) {
        this.path = requestPath.copy();

        for (int i = 0; i < this.path.size(); i++) {
            if (!this.path.isVar(i))
                continue;

            if (this.path.isVar(i) && !this.path.getVarName(i).startsWith("_"))
                continue;

            List<Param> params = getPathParams(i);
            Param       winner = null;
            for (Param param : params) {
                if (param.getKey() == null)
                    continue;
                if (winner == null)
                    winner = param;
                else if (winner.getKey().startsWith("_") && !param.getKey().startsWith("_"))
                    winner = param;
            }
            if (winner != null)
                this.path.set(i, "{" + winner.getKey() + "}");
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
}