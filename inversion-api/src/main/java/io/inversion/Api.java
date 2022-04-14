/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
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
package io.inversion;

import io.inversion.utils.Path;
import io.inversion.utils.Task;
import io.inversion.utils.Utils;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Contains the Servers, Dbs, Collections, Endpoints and Actions that make up a REST API.
 */
public class Api {

    protected final transient Logger log = LoggerFactory.getLogger(getClass().getName());

    /**
     * Host and root path config
     */
    protected final List<Server> servers = new ArrayList<>();

    /**
     * The underlying data sources for the Api.
     */
    protected final List<Db> dbs = new ArrayList<>();

    /**
     * The Request HTTP method/path combinations that map to a distinct set of Actions.
     * <p>
     * A single Endpoint will be selected to run to service a Request.  Any additional
     * Path matching rules that exist on these Endpoint's Actions will be interpreted
     * as relative to the end of the selected Endpoint's Path match.
     */
    protected final List<Endpoint> endpoints = new ArrayList<>();

    /**
     * Actions that may be selected to run regardless of the matched Endpoint.
     * <p>
     * The Action's Path match statements will be considered relative to the Api's
     * base URL NOT relative to the selected Endpoint.
     */
    protected final List<Action> actions = new ArrayList<>();

    /**
     * The data objects being served by this API.  In a simple API these may map
     * one-to-one to, for example, database tables from a JdbcDb connecting to a
     * RDBMS such as MySql or SqlServer.
     */
    protected final List<Collection> collections = new ArrayList<>();


    /**
     * Listeners that receive callbacks on startup/shutdown/request/error.
     */
    protected final transient List<ApiListener> listeners = new ArrayList<>();

    protected transient Linker linker = new Linker(this);

    protected transient List<Op> ops = new ArrayList();

    protected           String  name     = null;
    transient protected String  hash     = null;
    protected           boolean debug    = false;
    protected           String  url      = null;
    transient volatile  boolean started  = false;
    transient volatile  boolean starting = false;
    transient           long    loadTime = 0;

    transient Engine engine = null;

    protected String version = "1";

    transient List<Runnable> delayedConfig = new ArrayList();

    public Api() {
    }

    public Api(String name) {
        withName(name);
    }

    public Api withIncludeOn(String ruleMatcherSpec) {
        withServer(new Server().withIncludeOn(ruleMatcherSpec));
        return this;
    }

    public Server getServer() {
        if (servers.size() == 0) {
            servers.add(new Server());
        }
        return servers.get(0);
    }

    public boolean isStarted() {
        return started;
    }

    synchronized Api startup(Engine engine) {

        if (this.engine != null && engine != this.engine) {
            this.engine.removeApi(this);
        }

        if (started || starting) //starting is an accidental recursion guard
            return this;

        this.engine = engine;
        starting = true;
        try {
            for (Db db : dbs) {
                db.startup(this);
            }

            removeExcludes();

            this.ops = Collections.unmodifiableList(buildOps());

            started = true;

            for (Runnable r : delayedConfig)
                r.run();

            for (ApiListener listener : listeners) {
                try {
                    listener.onStartup(engine, this);
                } catch (Exception ex) {
                    log.warn("Error notifying api startup listener: " + listener, ex);
                }
            }

            return this;
        } finally {
            starting = false;
        }
    }

    void shutdown(Engine engine) {

        if (!started)
            return;

        started = false;

        for (Db db : dbs) {
            db.shutdown(this);
        }

        for (ApiListener listener : listeners) {
            try {
                listener.onShutdown(engine, this);
            } catch (Exception ex) {
                log.warn("Error notifying api shutdown listener: " + listener, ex);
            }
        }
    }

    public void withDelayedConfig(Runnable r) {
        if (isStarted())
            r.run();
        else
            delayedConfig.add(r);
    }

    public void removeExcludes() {
        for (Db<Db> db : getDbs()) {
            for (Collection coll : db.getCollections()) {
                if (coll.isExclude()) {
                    db.removeCollection(coll);
                } else {
                    for (Property col : coll.getProperties()) {
                        if (col.isExclude())
                            coll.removeProperty(col);
                    }
                }

                for (Relationship rel : coll.getRelationships()) {
                    if (rel.isExclude()) {
                        coll.removeRelationship(rel);
                    }
                }
            }
        }
    }

    public String getHash() {
        return hash;
    }

    public Api withHash(String hash) {
        this.hash = hash;
        return this;
    }

    public List<Server> getServers() {
        return new ArrayList<>(servers);
    }

    public Api withServers(String... urls) {
        for (String url : urls) {
            Server server = new Server().withUrls(url);
            withServer(server);
        }
        return this;
    }

    public Api withServer(Server server) {
        if (!servers.contains(server)) {
            servers.add(server);
        }
        return this;
    }

    public Api withCollection(Collection coll) {
        //if (coll.isLinkTbl() || coll.isExclude())
        if (coll.isExclude())
            return this;

        if (!collections.contains(coll))
            collections.add(coll);

        return this;
    }

    public List<Collection> getCollections() {
        return Collections.unmodifiableList(collections);
    }

    public Collection getCollection(String name) {
        for (Collection coll : collections) {
            if (name.equalsIgnoreCase(coll.getName()))
                return coll;
        }
        return null;
    }

    public Db getDb(String name) {
        if (name == null)
            return null;

        for (Db db : dbs) {
            if (name.equalsIgnoreCase(db.getName()))
                return db;
        }
        return null;
    }

    /**
     * @return the dbs
     */
    public List<Db> getDbs() {
        return new ArrayList<>(dbs);
    }

    /**
     * @param dbs the dbs to set
     * @return this
     */
    public Api withDbs(Db... dbs) {
        for (Db db : dbs)
            withDb(db);

        return this;
    }

    public Api withDbs(List<Db> dbs) {
        for (Db db : dbs)
            withDb(db);

        return this;
    }

    public <T extends Db> Api withDb(Db<T> db) {
        if (!dbs.contains(db)) {
            dbs.add(db);

            for (Collection coll : db.getCollections()) {
                withCollection(coll);
            }
        }

        return this;
    }


    public String getName() {
        return name;
    }


    public Api withName(String name) {
        this.name = name;
        return this;
    }

    public Api withVersion(String version) {
        this.version = version;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public long getLoadTime() {
        return loadTime;
    }

    public void setLoadTime(long loadTime) {
        this.loadTime = loadTime;
    }

    public Endpoint getEndpoint(String name) {
        for (Endpoint ep : endpoints) {
            if (name.equalsIgnoreCase(ep.getName()))
                return ep;
        }
        return null;
    }

    public List<Endpoint> getEndpoints() {
        return new ArrayList<>(endpoints);
    }

    public Api removeEndpoint(Endpoint ep) {
        endpoints.remove(ep);
        return this;
    }

    public Api withEndpoint(Action action1, Action... actions) {
        Endpoint endpoint = new Endpoint(action1);
        endpoint.withActions(actions);
        withEndpoint(endpoint);
        return this;
    }

    public Api withEndpoint(String ruleMatcherSpec, Action... actions) {
        Endpoint endpoint = new Endpoint(ruleMatcherSpec, actions);
        withEndpoint(endpoint);
        return this;
    }

    public Api withEndpoint(Endpoint... endpoints) {
        for (Endpoint endpoint : endpoints) {
            if (!this.endpoints.contains(endpoint)) {
                boolean inserted = false;
                for (int i = 0; i < this.endpoints.size(); i++) {
                    if (endpoint.getOrder() < this.endpoints.get(i).getOrder()) {
                        this.endpoints.add(i, endpoint);
                        inserted = true;
                        break;
                    }
                }

                if (!inserted)
                    this.endpoints.add(endpoint);

                endpoint.withApi(this);
            }
        }
        return this;
    }

    /**
     * Creates a ONE_TO_MANY Relationship from the parent to child collection and the inverse MANY_TO_ONE from the child to the parent.
     * The Relationship object along with the required Index objects are created.
     * <p>
     * If parentPropertyName is null, the ONE_TO_MANY relationship will not be crated.
     * <p>
     * If childPropertyName is null, the MANY_TO_ONE relationship will not be created.
     * <p>
     * If both parentPropertyName and childPropertyName are null, nothing will be performed, this will be a noop.
     * <p>
     * This configuration does not occur until after the Api has been started so that underlying Collections/Properties don't have to exist.
     *
     * @param parentCollectionName the name of the parent collection
     * @param parentPropertyName   the name of the json property for the parent that references the children (optional)
     * @param childCollectionName  the target child collection name
     * @param childPropertyName    the name of hte json property for the child that references the parent (optional)
     * @param childFkProps         names of the existing Properties that make up the foreign key
     * @return this
     * @see Collection#withOneToManyRelationship(String, Collection, String...)
     * @see Collection#withManyToOneRelationship(String, Collection, String...)
     */
    public Api withRelationship(String parentCollectionName, String parentPropertyName, String childCollectionName, String childPropertyName, String... childFkProps) {

        withDelayedConfig(() -> {
            Collection parentCollection = getCollection(parentCollectionName);
            Collection childCollection  = getCollection(childCollectionName);

            if (parentCollection == null || childCollection == null)
                throw ApiException.new500InternalServerError("You have specified a relationship between collections that don't exist in the Api after startup during delayed config: '{}' and, '{}'", parentCollectionName, childCollectionName);

            parentCollection.withOneToManyRelationship(parentPropertyName, childCollection, childFkProps);
            childCollection.withManyToOneRelationship(childPropertyName, parentCollection, childFkProps);
        });

        return this;
    }

//    /**
//     * Creates a ONE_TO_MANY Relationship from the parent to child collection and the inverse MANY_TO_ONE from the child to the parent.
//     * The Relationship object along with the required Index objects are created.
//     * <p>
//     * For collections backed by relational data sources (like a SQL db) the length of <code>childFkProps</code> will generally match the
//     * length of <code>parentCollections</code> primary index.  If the two don't match, then <code>childFkProps</code> must be 1.  In this
//     * case, the compound primary index of parentCollection will be encoded as an resourceKey in the single child table property.
//     *
//     * @param parentCollection   the collection to add the relationship to
//     * @param parentPropertyName the name of the json property for the parent that references the child
//     * @param childCollection    the target child collection
//     * @param childPropertyName  the name of hte json property for the child that references the parent
//     * @param childFkProps       Properties that make up the foreign key
//     * @return this
//     */
//    public Api withRelationship(Collection parentCollection, String parentPropertyName, Collection childCollection, String childPropertyName, Property... childFkProps) {
//        parentCollection.withOneToManyRelationship(parentPropertyName, childCollection, childPropertyName, childFkProps);
//        return this;
//    }

    public Action getAction(String name) {
        for (Action action : actions) {
            if (name.equalsIgnoreCase(action.getName()))
                return action;
        }
        return null;
    }

    public List<Action> getActions() {
        return new ArrayList<>(actions);
    }

    /**
     * Add Action(s) may be selected to run across multiple Endpoints.
     *
     * @param actions actions to match and conditionally run across all Requests
     * @return this
     */
    public synchronized Api withActions(Action... actions) {
        for (Action action : actions)
            if (!this.actions.contains(action))
                this.actions.add(action);
        return this;
    }

    public Api withAction(Action action) {
        if (!this.actions.contains(action))
            this.actions.add(action);
        return this;
    }

    public Engine getEngine() {
        return engine;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public String getUrl() {
        return url;
    }

    public Api withUrl(String url) {
        this.url = url;
        return this;
    }

    public Linker getLinker() {
        return linker;
    }

    public Api withLinker(Linker linker) {
        this.linker = linker;
        if (linker.getApi() != this) ;
        linker.withApi(this);
        return this;
    }

    public Api withApiListener(ApiListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
        return this;
    }

    public List<ApiListener> getApiListeners() {
        return Collections.unmodifiableList(listeners);
    }

    /**
     * Listener that can be registered with an {@code Api} to receive lifecycle,
     * per request and per error callback notifications.
     */
    public interface ApiListener {
        default void onStartup(Engine engine, Api api) {
            //implement me
        }

        default void onShutdown(Engine engine, Api api) {
            //implement me
        }

        default void onAfterRequest(Request req, Response res) {
            //implement me
        }

        default void onAfterError(Request req, Response res) {
            //implement me
        }

        default void onBeforeFinally(Request req, Response res) {
            //implement me
        }
    }


    public List<Op> getOps() {
        return ops;
    }

    public Op getOp(String name) {
        for (Op op : ops) {
            if (name.equalsIgnoreCase(op.getName()))
                return op;
        }
        return null;
    }


    List<Op> buildOps() {

        if (servers.size() == 0)
            servers.add(new Server());

        List<Op>                             operations = new ArrayList<>();
        ArrayListValuedHashMap<String, Path> allPaths   = buildRequestPaths();
        for (String method : allPaths.keySet()) {
            List<Path> requestPaths = allPaths.get(method);
            for (Path requestPath : requestPaths) {

                //-- there is a template variable here that was not bound by an action.
                if (requestPath.toString().indexOf("{_") < -1)
                    continue;

                List<Op> ops = buildOp(method, requestPath);
                operations.addAll(ops);
            }
        }

        operations.removeIf(o -> o.getActions().size() == 0);
        operations.removeIf(o -> o.getActions().parallelStream().allMatch(a -> a.isDecoration()));

        for (int i = 0; i < operations.size(); i++) {
            Op o = operations.get(i);
            if (o.getRelationship() != null) {
                boolean found = false;
                for (Op candiate : operations) {
                    //TODO: probably need to distinguish between GET and FIND methods here
                    if ("GET".equalsIgnoreCase(candiate.getMethod())) {
                        if (candiate.getCollection() == o.getRelationship().getRelated()) {
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    operations.remove(i);
                    i--;
                }
            }
        }

        operations.removeIf(op -> setOpFunctionAndName(op) == null);


        deduplicateOperationNames(operations);
        Collections.sort(operations);

        for (Op op : operations) {
            List<Rule> rules = new ArrayList();
            Utils.add(rules, op.getEngine(), op.getApi(), op.getEndpoint(), op.getCollection());
            rules.addAll(op.getActions());
            Task.buildTask(rules, "hook_configureOp", op).go();
        }


        ArrayListValuedHashMap<Server, Path> servers = new ArrayListValuedHashMap();
        for (Server server : getServers()) {
            servers.putAll(server, server.getAllIncludePaths());
        }

        System.out.println("\r\n--------------------------------------------");

        System.out.println("SERVERS: ");
        for (Server server : servers.keySet()) {
            List<Path> serverPaths = new ArrayList(new LinkedHashSet(servers.get(server)));

            for (Path serverPath : serverPaths) {
                List<String> serverUrls = server.getUrls();
                if (serverUrls.size() == 0)
                    serverUrls.add("127.0.0.1");

                for (String host : serverUrls) {
                    System.out.println(" - " + host + "/" + serverPath);
                }
            }
        }

        System.out.println("\r\nOPERATIONS:");

        List<Op> apiOps = new ArrayList(operations);
        Collections.sort(apiOps);

        List<List> table = new ArrayList<>();
        for(Op op : apiOps){
            List row = new ArrayList();
            table.add(row);
            Utils.add(row, op.getMethod(), op.getPath(), op.getName());
        }

        System.out.println(Utils.printTable(table));
        System.out.println("\r\n--------------------------------------------");


        return operations;
    }

//    public Op findOperation(Op.OpFunction function, String... pathParams){
//        for(Op op : ops){
//            if(function != null && function != op.getFunction())
//                continue;
//
//            for(String )
//        }
//    }

    List<Op> buildOp(String method, Path requestPath) {

        Path     requestPathCopy = requestPath.copy();
        List<Op> ops             = new ArrayList<>();

        requestPath = requestPathCopy.copy();
        int offset = 0;

        for (Endpoint ep : getEndpoints()) {
            requestPath = requestPathCopy.copy();

            Op op = new Op();
            op.withEngine(engine);
            op.withApi(this);
            op.withMethod(method);
            op.withPath(requestPath.copy());

            Path endpointMatch = ep.match(method, requestPath);
            if (endpointMatch == null)
                continue;

            op.withEndpoint(ep);
            op.withEndpointPathMatch(endpointMatch.copy());

            //-- find the db with the most specific (longest) path match
            Db   winnerDb      = null;
            Path winnerDbMatch = null;
            for (Db db : getDbs()) {
                Path dbMatch = db.match(method, requestPath);
                if (dbMatch != null) {
                    if (winnerDbMatch == null || dbMatch.size() > winnerDbMatch.size()) {
                        winnerDb = db;
                        winnerDbMatch = dbMatch;
                    }
                }
            }
            if (winnerDb != null) {
                op.withDb(winnerDb);
                op.withDbMatchPath(winnerDbMatch);
                addParams(op, winnerDbMatch, requestPath, offset, false);
            }

            //-- pull out api action match paths before consuming the
            //-- request path for the endpoint action matches
            for (Action action : getActions()) {
                Path actionMatch = action.match(method, requestPath, true);
                if (actionMatch != null) {
                    op.withActionMatch(action, actionMatch.copy(), false);
                    addParams(op, actionMatch, requestPath, offset, false);
                }
            }

            //-- consume the requestPath for Endpoint Actions to match.
            offset = addParams(op, endpointMatch, requestPath, offset, true);

            op.withActionPathMatch(requestPath.copy());

            for (Action action : ep.getActions()) {
                Path actionMatch = action.match(method, requestPath, true);
                if (actionMatch != null) {
                    op.withActionMatch(action, actionMatch.copy(), true);
                    addParams(op, actionMatch, requestPath, offset, false);
                }
            }

            List<Op> fixed = new ArrayList();
            fixed.add(op);
            Task.buildTask(op.getActions(), "hook_enumerateOps", fixed).go();
            ops.addAll(fixed);
        }
        return ops;
    }

    public void assignDefaultCollections(Op op) {
        String collectionKey = op.getPathParamValue("_collection");
        if (collectionKey == null) {

        }
    }


    public ArrayListValuedHashMap<String, Path> buildRequestPaths() {

        ArrayListValuedHashMap<String, Path> paths   = new ArrayListValuedHashMap<>();
        Set<String>                          methods = new HashSet();
        Utils.add(methods, "GET", "POST", "PUT", "PATCH", "DELETE");
        for (String method : methods) {
            for (Endpoint ep : getEndpoints()) {
                for (Rule.RuleMatcher epMatcher : ep.getIncludeMatchers()) {
                    if (!epMatcher.hasMethod(method))
                        continue;
                    for (Path epPath : epMatcher.getPaths()) {

                        List<Action> epActions  = ep.getActions();
                        List<Action> allActions = new ArrayList(ep.getActions());
                        for (Action action : getActions()) {
                            if (!allActions.contains(action))
                                allActions.add(action);
                        }
                        Collections.sort(allActions);

                        List<Path> allPathsForEndpoint = new ArrayList<>();
                        allPathsForEndpoint.add(epPath.copy());

                        for (Action action : allActions) {
                            for (Rule.RuleMatcher actionMatcher : (List<Rule.RuleMatcher>) action.getIncludeMatchers()) {
                                if (!actionMatcher.hasMethod(method))
                                    continue;
                                for (Path actionPath : actionMatcher.getPaths()) {
                                    Path fullActionPath = null;
                                    if (epActions.contains(action)) {
                                        fullActionPath = Path.joinPaths(epPath, actionPath, true);

                                    } else {
                                        if (actionPath.matches(epPath, true)) {
                                            fullActionPath = Path.joinPaths(epPath, actionPath, false);
                                        }
                                    }
                                    if (fullActionPath == null)
                                        continue;

                                    allPathsForEndpoint.add(fullActionPath);
                                    //paths.put(method, fullActionPath);
                                }
                            }
                        }
                        allPathsForEndpoint = Path.expandOptionals(allPathsForEndpoint);
                        allPathsForEndpoint = Path.materializeTrivialRegexes(allPathsForEndpoint);
                        allPathsForEndpoint = Path.filterDuplicates(allPathsForEndpoint);
                        allPathsForEndpoint = Path.mergePaths(new ArrayList(), allPathsForEndpoint);
                        paths.putAll(method, allPathsForEndpoint);
                    }
                }
            }
        }


        for (String key : new ArrayList<String>(paths.keySet())) {
            List<Path> ps = new ArrayList(paths.get(key));
            for (Path p : ps) {
                if (p.endsWithWildcard()) {
                    p.removeTrailingWildcard();
                }
                if (p.size() == 0)
                    paths.removeMapping(key, p);
            }
        }

        //-- endpoints could have produced identical paths. Filter out duplicates
        ArrayListValuedHashMap<String, Path> merged = new ArrayListValuedHashMap<>();
        for (String method : paths.keySet()) {
            List<Path> before = paths.get(method);
            List<Path> after  = Path.filterDuplicates(before);
            merged.putAll(method, after);
        }
        paths = merged;


        System.out.println("\r\nPATHS ------------------------");
        List<String> methodsList = new ArrayList(paths.keySet());
        Collections.sort(methodsList);
        for (String method : methodsList) {
            List<Path> pathList = new ArrayList(paths.get(method));
            for (Path path : pathList) {
                System.out.println(method + " " + path);
            }
        }
        System.out.println("END PATHS --------------------\r\n");

        return paths;
    }

    protected int addParams(Op op, Path matchedPath, Path requestPath, int offset, boolean consume) {

        for (int i = 0; i < matchedPath.size() && i < requestPath.size(); i++) {
            if (matchedPath.isVar(i)) {
                String key   = matchedPath.getVarName(i);
                Param  param = new Param(key, i + offset);
                param.withRequired(true);
                String regex = matchedPath.getRegex(i);
                if (regex != null)
                    param.withRegex(regex);
                op.withParam(param);
            }
        }

        if (consume) {
            for (int i = 0; i < matchedPath.size(); i++) {
                if (matchedPath.isOptional(i) || matchedPath.isWildcard(i))
                    break;
                requestPath.remove(0);
                offset += 1;
            }
        }
        return offset;
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

    public String setOpFunctionAndName(Op op) {

        String name = null;

        Collection collection  = op.getCollection();
        String     defaultName = op.getEndpoint().getName();
        if (defaultName == null) {
            defaultName = op.getPath().last();
            defaultName = cleanNamePart(defaultName);
            defaultName = Utils.beautifyName(defaultName);
        }

        String singular = Utils.capitalize(collection == null ? defaultName : collection.getSingularDisplayName());
        String plural   = Utils.capitalize(collection == null ? defaultName : collection.getPluralDisplayName());

        String method = op.getMethod().toUpperCase();

        if (op.hasParams(Param.In.PATH, Request.COLLECTION_KEY, Request.RESOURCE_KEY, Request.RELATIONSHIP_KEY)) {

            switch (method) {
                case "GET":
                    name = "find" + plural + "Related" + Utils.capitalize(cleanNamePart(op.getPathParamValue(Request.RELATIONSHIP_KEY)));
                    op.function = Op.OpFunction.RELATED;
                    break;
                case "POST":
                case "PUT":
                case "PATCH":
                case "DELETE":
                    return null;//-- unsupported operations
            }

        } else if (op.hasParams(Param.In.PATH, Request.COLLECTION_KEY, Request.RESOURCE_KEY)) {
            switch (method) {
                case "GET":
                    name = "get" + singular;
                    op.function = Op.OpFunction.GET;
                    break;
//                case "POST":
//                    name = "createIdentified" + singular;
//                    op.function = Op.OpFunction.POST;
//                    break;
                case "PUT":
                    name = "update" + singular;
                    op.function = Op.OpFunction.PUT;
                    break;
                case "PATCH":
                    name = "patch" + singular;
                    op.function = Op.OpFunction.PATCH;
                    break;
                case "DELETE":
                    name = "delete" + singular;
                    op.function = Op.OpFunction.DELETE;
                    break;
            }

        } else if (op.hasParams(Param.In.PATH, Request.COLLECTION_KEY)) {
            switch (method) {
                case "GET":
                    name = "find" + plural;
                    op.function = Op.OpFunction.FIND;
                    break;
                case "POST":
                    name = "create" + singular;
                    op.function = Op.OpFunction.POST;
                    break;
                case "PUT":
                    name = "batchUpdate" + plural;
                    op.function = Op.OpFunction.BATCH_PUT;
                    break;
                case "PATCH":
                    name = "batchPatch" + plural;
                    op.function = Op.OpFunction.BATCH_PATCH;
                    break;
                case "DELETE":
                    name = "batchDelete" + plural;
                    op.function = Op.OpFunction.BATCH_DELETE;
                    break;
            }
        } else {
            switch (method) {
                case "GET":
                case "POST":
                case "PUT":
                case "PATCH":
                case "DELETE":
                    name = Utils.beautifyName(method.toLowerCase() + " " + defaultName);
                    op.function = Op.OpFunction.valueOf(method);
                    break;
            }
        }

        for (int i = 0; i < op.getPath().size(); i++) {
            if (op.getPath().isVar(i))
                name += "By" + Utils.capitalize(cleanNamePart(op.getPath().getVarName(i)));
        }

        op.withName(name);
        return name;
    }


    void deduplicateOperationNames(List<Op> ops) {
        ArrayListValuedHashMap<String, Op> map = new ArrayListValuedHashMap<>();
        ops.forEach(op -> map.put(op.getName(), op));

        for (String operationName : map.keySet()) {
            List<Op> values = map.get(operationName);
            if (values.size() > 1) {
                for (int i = 0; i < values.size(); i++) {
                    String name = values.get(i).getName() + (i + 1);
                    values.get(i).withName(name);
                }
            }
        }
    }


}
