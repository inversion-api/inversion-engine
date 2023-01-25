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
public final class Api {

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

    protected transient Linker linker = new Linker();

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

            configureServers();
            configureOps();

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

            if(parentPropertyName != null)
                parentCollection.withOneToManyRelationship(parentPropertyName, childCollection, childFkProps);

            if(childPropertyName != null)
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

    public Api withDebug(boolean debug){
        this.debug = debug;
        return this;
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

    public Db matchDb(String method, Path requestPath) {
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

        return winnerDb;
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

    void configureServers() {

        System.out.println("\r\n--------------------------------------------");
        System.out.println("SERVERS: ");
        for (Server server : servers) {
            for(Server.ServerMatcher sm : server.getServerMatches()){
                System.out.println("  - " + sm);
            }
        }
    }

    List<Op> configureOps() {

        Map<String, Map<Endpoint, List<List<Path>>>> allPaths = generatePaths();
        List<Op>                                     ops      = new ArrayList<>();

        for (String method : allPaths.keySet()) {
            Map<Endpoint, List<List<Path>>> epMap = allPaths.get(method);
            for (Endpoint ep : epMap.keySet()) {
                List<List<Path>> groupedPaths = epMap.get(ep);

                for (List<Path> paths : groupedPaths) {
                    Op op = new Op();
                    op.withMethod(method);
                    op.withApi(this);
                    op.withEngine(engine);
                    paths.forEach(p -> op.withPath(p));
                    if (matchOp(ep, op)) {
                        ops.add(op);
                        //System.out.println(op.getMethod() + " - " + op.getPath() + " - " + op);
                    }
                }
            }
        }

        for (Op op : ops) {
            Task.buildTask(op.getActions(), "configureOp", op).go();
        }

        ops.removeIf(o -> o.getFunction() == null);
        ops.removeIf(o -> o.getPath().toString().indexOf("{_") > 0);
        ops.removeIf(o -> o.getActions().size() == 0);
        ops.removeIf(o -> o.getActions().parallelStream().allMatch(a -> a.isDecoration()));
        ops.removeIf(o -> o.getName() == null);

        Collections.sort(ops);
        deduplicateOperationNames(ops);

        System.out.println("\r\n--------------------------------------------");
        //System.out.println("\r\nOPERATIONS:");
        List<List> table = new ArrayList<>();

        List header = new ArrayList();
        table.add(header);
        Utils.add(header, "METHOD", "PATH", "OPERATION", "ENDPOINT", "COLLECTION", "ACTIONS", "PARAMS");

        for (Op op : ops) {
            List row = new ArrayList();
            table.add(row);

            List actionNames = new ArrayList();
            op.getActions().forEach(a -> actionNames.add(a.getName() != null ? a.getName() : a.getClass().getSimpleName()));
            Utils.add(row, op.getMethod(), op.getPath(), op.getName(), op.getEndpoint().getName(), op.getCollection() != null ? op.getCollection().getName() : null, actionNames, op.getParams());
        }
        System.out.println(Utils.printTable(table));
        System.out.println("\r\n--------------------------------------------");

        this.ops = Collections.unmodifiableList(ops);
        return new ArrayList(ops);
    }


    boolean matchOp(Endpoint ep, Op op) {

        String method          = op.getMethod();
        Path   requestPath     = op.getPath().copy();

        Path endpointMatch = ep.match(method, requestPath);
        if (endpointMatch == null)
            return false;

        op.withEndpoint(ep);
        op.withEndpointPathMatch(endpointMatch.copy());

        //-- pull out api action match paths before consuming the
        //-- request path for the endpoint action matches
        for (Action action : getActions()) {
            Path actionMatch = action.match(method, requestPath, true);
            if (actionMatch != null) {
                op.withActionMatch(action, actionMatch.copy(), false);
            }
        }

        //-- consume the endpoint part of the request path to relative match the actions
        for (int i = 0; i < endpointMatch.size(); i++) {
            if (endpointMatch.isOptional(i) || endpointMatch.isWildcard(i))
                break;
            requestPath.remove(0);
        }

        op.withActionPathMatch(requestPath.copy());

        for (Action action : ep.getActions()) {
            Path actionMatch = action.match(method, requestPath, true);
            if (actionMatch != null) {
                op.withActionMatch(action, actionMatch.copy(), true);
            }
        }

        return true;
    }


    Map<String, Map<Endpoint, List<List<Path>>>> generatePaths() {
        LinkedHashMap<String, Map<Endpoint, List<List<Path>>>> paths = new LinkedHashMap<>();
        Set<String> methods = new LinkedHashSet();
        Utils.add(methods, "GET", "POST", "PUT", "PATCH", "DELETE");
        for (String method : methods) {
            List<Endpoint> eps = new ArrayList(getEndpoints());
            Collections.sort(eps);
            for (Endpoint ep : eps) {
                List<Path> endpointPaths = new ArrayList();
                for (Rule.RuleMatcher epMatcher : ep.getIncludeMatchers()) {
                    if (!epMatcher.hasMethod(method))
                        continue;
                    for (Path epPath : epMatcher.getPaths()) {
                        Db db = matchDb(method, epPath);
                        List<Action> epActions  = ep.getActions();
                        List<Action> allActions = new ArrayList(ep.getActions());
                        for (Action action : getActions()) {
                            if (!allActions.contains(action))
                                allActions.add(action);
                        }
                        Collections.sort(allActions);

                        List<Path> allPathsForSingleEpMatcherPath = new ArrayList<>();
                        for (Action action : allActions) {
                            if(action.isDecoration())
                                continue;
                            for(Path fullActionPath : (List<Path>)action.getFullIncludePaths(this, db, method, epPath, epActions.contains(action))){
                                allPathsForSingleEpMatcherPath.add(fullActionPath);
                            }
                        }
                        allPathsForSingleEpMatcherPath = Path.filterDuplicates(allPathsForSingleEpMatcherPath);
                        allPathsForSingleEpMatcherPath.removeIf(p -> p.size() == 0);
                        endpointPaths.addAll(allPathsForSingleEpMatcherPath);
                    }
                }

                endpointPaths = Path.filterDuplicates(endpointPaths);
                endpointPaths = removeObscuredWildcards(endpointPaths);

                if(endpointPaths.size() > 0){
                    List<List<Path>> groupedPaths = groupPaths(endpointPaths);
                    Map<Endpoint, List<List<Path>>> epMap = paths.get(method);
                    if (epMap == null) {
                        epMap = new LinkedHashMap<>();
                        paths.put(method, epMap);
                    }
                    epMap.put(ep, groupedPaths);
                }
            }
        }
//        System.out.println("\r\nPATHS ------------------------");
//        for(String method : paths.keySet()){
//            Map<Endpoint, List<List<Path>>> pathSet = paths.get(method);
//            for(Endpoint ep : pathSet.keySet()){
//                System.out.println(method + " - " + ep.getAllIncludePaths());
//                for(Object o : pathSet.get(ep)){
//                    System.out.println("  - " + o);
//                }
//            }
//        }
//        System.out.println("END PATHS --------------------\r\n");

        return paths;
    }

    List<Path> removeObscuredWildcards(List<Path> paths){
        ArrayListValuedHashMap<String, Path> templates = new ArrayListValuedHashMap();
        for(Path path : paths){
            String template = path.getTemplate();
            templates.put(template, path);
        }
        List<String> sorted = new ArrayList(templates.keySet());
        Collections.sort(sorted);

        for(int i=1; i<sorted.size()-1; i++){
            String previous = sorted.get(i-1);
            String current = sorted.get(i);
            if(previous.endsWith("/*") && current.startsWith(previous.substring(0, previous.length()-1))){
                System.out.println("REMOVING: " + previous);
                templates.remove(previous);
            }
        }

        List<Path> newPaths = new ArrayList();
        for(String key : templates.keySet()){
            List<Path> ps = templates.get(key);
            newPaths.addAll(ps);
        }
        Collections.sort(newPaths);
        return newPaths;
    }

    /**
     * Groups into lists of compatible potentially variablized paths
     * based on bidirectional matching.
     *
     * @param paths
     * @return
     */
    List<List<Path>> groupPaths(List<Path> paths) {

        paths = Path.filterDuplicates(paths);

        //System.out.println(paths);

        List<List<Path>> groups   = new ArrayList<>();
        List<Path>       dynamics = new ArrayList();
        List<Path>       statics  = new ArrayList();
        List<Path>       unbounds = new ArrayList();

        for (Path p : paths) {
            if (p.hasVars()) {
                boolean unbound = false;
                for (int i = 0; i < p.size(); i++) {
                    String name = p.getVarName(i);
                    if (name != null && name.startsWith("_")) {
                        unbound = true;
                        break;
                    }
                }
                if (unbound)
                    unbounds.add(p);
                else
                    dynamics.add(p);
            } else
                statics.add(p);
        }

        statics.forEach(p -> groups.add(Utils.add(new ArrayList(), p)));
        dynamics.forEach(p -> groups.add(Utils.add(new ArrayList(), p)));

        for (Path unbound : unbounds) {
            for (List<Path> group : groups) {
                boolean matches = true;
                for (Path p : group) {
                    if (!p.matches(unbound, true)) {
                        matches = false;
                        break;
                    }
                }
                if (matches)
                    group.add(unbound);
            }
        }

        //System.out.println(groups);

        return groups;
    }


    void deduplicateOperationNames(List<Op> ops) {
        Collections.sort(ops);
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
