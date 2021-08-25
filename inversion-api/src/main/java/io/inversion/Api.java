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

//import com.github.curiousoddman.rgxgen.RgxGen;
//import com.github.curiousoddman.rgxgen.iterators.StringIterator;

import io.inversion.utils.Path;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.util.*;

/**
 * Contains the Collections, Endpoints and Actions that make up a REST API.
 */
public class Api extends Rule<Api> {

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

    protected final transient List<Operation> operations = new ArrayList();


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

    @Override
    protected RuleMatcher getDefaultIncludeMatch() {
        List<String> parts = new ArrayList<>();
        if (name != null) {
            parts.add(name);
        }
        parts.add("*");
        return new RuleMatcher(null, new Path(parts));
    }

    public boolean isStarted() {
        return started;
    }

    synchronized Api startup(Engine engine) {
        if (started || starting) //starting is an accidental recursion guard
            return this;

        this.engine = engine;
        starting = true;
        try {
            for (Db db : dbs) {
                db.startup(this);
            }

            removeExcludes();

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

    public List<Endpoint> getEndpoints() {
        return new ArrayList<>(endpoints);
    }

    public Api withEndpoint(String methods, String includePaths, Action... actions) {
        Endpoint endpoint = new Endpoint(methods, includePaths, actions);
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

            if (parentPropertyName != null)
                parentCollection.withOneToManyRelationship(parentPropertyName, childCollection, childFkProps);

            if (childPropertyName != null)
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

        default void afterRequest(Request req, Response res) {
            //implement me
        }

        default void afterError(Request req, Response res) {
            //implement me
        }

        default void beforeFinally(Request req, Response res) {
            //implement me
        }
    }

//    public List<Operation> getOperations() {
//        if (operations.size() == 0) {
//            synchronized (this) {
//                if (operations.size() == 0)
//                    operations.addAll(buildOperations());
//            }
//        }
//        return operations;
//    }


    /**
     * LIST
     * GET /books
     * ListBooksRequest    - ListBooksResponse
     * <p>
     * GET
     * GET /book/:id
     * GetBookRequest      - GetBookResponse
     * <p>
     * RELATED
     * GET /book/:id/author
     * RelatedAuthorsRequest - ListAuthorsResponse
     * <p>
     * POST
     * POST /books
     * CreateBookRequest - CreateBookResponse
     * <p>
     * BATCH_POST
     * POST /books
     * CreateBooksBatchRequest - CreateBooksBatchResponse
     * <p>
     * BATCH_PUT
     * PUT /books
     * UpdateBooksBatchRequest - UpdateBooksBatchResponse
     * <p>
     * PUT
     * PUT /books/:id
     * UpdateBookRequest - UpdateBookResponse
     * <p>
     * PATCH
     * PATCH /books/:id
     * PatchBookRequest  - PatchBookResponse
     * <p>
     * DELETE
     * DELETE /books/:id
     * DeleteBookRequest - DeleteBookResponse
     * <p>
     * BATCH_DELETE
     * DELETE /books/
     * DeleteBooksBatchRequest - DeleteBooksBatchResponse
     */
    List<Operation> buildOperations() {
        //List<Operation> operations = generateCandidateOperations();
        //filterValidOperations(operations);
        //deduplicateOperationIds(operations);
        //return operations;
        return null;
    }






//    List<Operation> generateCandidateOperations() {
//        List<Operation> operations = new ArrayList();
//
//        for (Rule.RuleMatcher apiMatcher : getIncludeMatchers()) {
//            for (Path apiPath : apiMatcher.getPaths()) {
//
//                Path candidateApiPath = new Path();
//
//                for (int i = 0; i < apiPath.size() && !(apiPath.isWildcard(i) || apiPath.isOptional(i)); i++) {
//                    String part = apiPath.get(i);
//                    candidateApiPath.add(part);
//                }
//
//                for (Endpoint endpoint : getEndpoints()) {
//                    for (Rule.RuleMatcher epMatcher : endpoint.getIncludeMatchers()) {
//                        for (Path epPath : epMatcher.getPaths()) {
//
//                            if (!epPath.hasAllVars(Request.COLLECTION_KEY) && (!epPath.isWildcard(epPath.size() - 1) || getCollections().size() == 0)) {
//
//                                for (Path candidateEpPath : epPath.getSubPaths()) {
//
//                                    Path candidatePath = new Path(candidateApiPath.toString(), candidateEpPath.toString());
//                                    operations.add(new Operation(this, "GET", "GET", candidatePath, apiPath, epPath, null, endpoint, null, null));
//                                    operations.add(new Operation(this, "POST", "POST", candidatePath, apiPath, epPath, null, endpoint, null, null));
//                                    operations.add(new Operation(this, "PUT", "PUT", candidatePath, apiPath, epPath, null, endpoint, null, null));
//                                    operations.add(new Operation(this, "PATCH", "PATCH", candidatePath, apiPath, epPath, null, endpoint, null, null));
//                                    operations.add(new Operation(this, "DELETE", "DELETE", candidatePath, apiPath, epPath, null, endpoint, null, null));
//                                }
//                            } else {
//
//                                Path candidateEpPath = new Path();
//
//                                for (int i = 0; i < epPath.size() && !(epPath.isWildcard(i) || epPath.isOptional(i)); i++) {
//                                    String part = epPath.get(i);
//                                    candidateEpPath.add(part);
//                                }
//
//                                for (Collection coll : getCollections()) {
//                                    for (Rule.RuleMatcher collMatcher : coll.getIncludeMatchers()) {
//                                        for (Path collPath : collMatcher.getPaths()) {
//
//                                            Path candidateCollPath = new Path();
//
//                                            for (int i = 0; i < collPath.size(); i++) {
//                                                if (collPath.isWildcard(i) && i == collPath.size() - 1)
//                                                    break;
//
//                                                String part = collPath.get(i);
//                                                candidateCollPath.add(part);
//
//                                                //TODO iterate over subpaths here
//                                                Path candidatePath = new Path(candidateApiPath.toString(), candidateEpPath.toString(), candidateCollPath.toString());
//
//                                                if (candidatePath.hasAllVars(Request.COLLECTION_KEY, Request.RESOURCE_KEY, Request.RELATIONSHIP_KEY)) {
//                                                    for (Relationship rel : coll.getRelationships()) {
//                                                        operations.add(new Operation(this, "RELATED", "GET", candidatePath, apiPath, epPath, collPath, endpoint, coll, rel));
//                                                    }
//                                                } else if (candidatePath.hasAllVars(Request.COLLECTION_KEY, Request.RESOURCE_KEY)) {
//                                                    operations.add(new Operation(this, "GET", "GET", candidatePath, apiPath, epPath, collPath, endpoint, coll, null));
//                                                    operations.add(new Operation(this, "PUT", "PUT", candidatePath, apiPath, epPath, collPath, endpoint, coll, null));
//                                                    operations.add(new Operation(this, "PATCH", "PATCH", candidatePath, apiPath, epPath, collPath, endpoint, coll, null));
//                                                    operations.add(new Operation(this, "DELETE", "DELETE", candidatePath, apiPath, epPath, collPath, endpoint, coll, null));
//
//                                                } else if (candidatePath.hasAllVars(Request.COLLECTION_KEY)) {
//                                                    operations.add(new Operation(this, "LIST", "GET", candidatePath, apiPath, epPath, collPath, endpoint, coll, null));
//                                                    operations.add(new Operation(this, "POST", "POST", candidatePath, apiPath, epPath, collPath, endpoint, coll, null));
//
//                                                }
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        return operations;
//    }


//    List<Operation> generateCandidateOperations(){
//        List<Operation> operations = new ArrayList();
//
//        for (Rule.RuleMatcher apiMatcher : getIncludeMatchers()) {
//            for (Path apiPath : apiMatcher.getPaths()) {
//
//                Path candidateApiPath = new Path();
//
//                for (int i = 0; i < apiPath.size() && !(apiPath.isWildcard(i) || apiPath.isOptional(i)); i++) {
//                    String part = apiPath.get(i);
//                    candidateApiPath.add(part);
//                }
//
//                for (Endpoint endpoint : getEndpoints()) {
//                    for (Rule.RuleMatcher epMatcher : endpoint.getIncludeMatchers()) {
//                        for (Path epPath : epMatcher.getPaths()) {
//
//                            if(!epPath.hasAllVars(Request.COLLECTION_KEY) && (!epPath.isWildcard(epPath.size()-1) || getCollections().size() == 0)){
//
//                                for(Path candidateEpPath : epPath.getSubPaths()){
//
//                                    Path candidatePath = new Path(candidateApiPath.toString(), candidateEpPath.toString());
//                                    operations.add(new Operation(this, "GET", "GET", candidatePath, apiPath, epPath, null, endpoint, null, null));
//                                    operations.add(new Operation(this, "POST", "POST", candidatePath, apiPath, epPath, null, endpoint, null, null));
//                                    operations.add(new Operation(this,"PUT", "PUT", candidatePath, apiPath, epPath, null, endpoint, null, null));
//                                    operations.add(new Operation(this,"PATCH", "PATCH", candidatePath, apiPath, epPath, null, endpoint, null, null));
//                                    operations.add(new Operation(this,"DELETE", "DELETE", candidatePath, apiPath, epPath, null, endpoint, null, null));
//                                }
//                            }
//                            else {
//
//                                Path candidateEpPath = new Path();
//
//                                for (int i = 0; i < epPath.size() && !(epPath.isWildcard(i) || epPath.isOptional(i)); i++) {
//                                    String part = epPath.get(i);
//                                    candidateEpPath.add(part);
//                                }
//
//                                for (Collection coll : getCollections()) {
//                                    for (Rule.RuleMatcher collMatcher : coll.getIncludeMatchers()) {
//                                        for (Path collPath : collMatcher.getPaths()) {
//
//                                            Path candidateCollPath = new Path();
//
//                                            for (int i = 0; i < collPath.size(); i++) {
//                                                if (collPath.isWildcard(i) && i == collPath.size() - 1)
//                                                    break;
//
//                                                String part = collPath.get(i);
//                                                candidateCollPath.add(part);
//
//                                                //TODO iterate over subpaths here
//                                                Path candidatePath = new Path(candidateApiPath.toString(), candidateEpPath.toString(), candidateCollPath.toString());
//
//                                                if (candidatePath.hasAllVars(Request.COLLECTION_KEY, Request.RESOURCE_KEY, Request.RELATIONSHIP_KEY)) {
//                                                    for (Relationship rel : coll.getRelationships()) {
//                                                        operations.add(new Operation(this,"RELATED", "GET", candidatePath, apiPath, epPath, collPath, endpoint, coll, rel));
//                                                    }
//                                                } else if (candidatePath.hasAllVars(Request.COLLECTION_KEY, Request.RESOURCE_KEY)) {
//                                                    operations.add(new Operation(this,"GET", "GET", candidatePath, apiPath, epPath, collPath, endpoint, coll, null));
//                                                    operations.add(new Operation(this,"PUT", "PUT", candidatePath, apiPath, epPath, collPath, endpoint, coll, null));
//                                                    operations.add(new Operation(this,"PATCH", "PATCH", candidatePath, apiPath, epPath, collPath, endpoint, coll, null));
//                                                    operations.add(new Operation(this,"DELETE", "DELETE", candidatePath, apiPath, epPath, collPath, endpoint, coll, null));
//
//                                                } else if (candidatePath.hasAllVars(Request.COLLECTION_KEY)) {
//                                                    operations.add(new Operation(this,"LIST", "GET", candidatePath, apiPath, epPath, collPath, endpoint, coll, null));
//                                                    operations.add(new Operation(this,"POST", "POST", candidatePath, apiPath, epPath, collPath, endpoint, coll, null));
//
//                                                }
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        return operations;
//    }

    void filterValidOperations(List<Operation> operations) {
        List<Operation> validOps = new ArrayList();
        for (Operation op : operations) {

            Map<String, String> pathParams = new HashMap();

            Path operationMatchPath = new Path(op.operationPath);
            Path examplePath        = materialziePath(op.operationPath);

            Path match = match(op.method, examplePath);
            if (match != null) {
                for (int i = 0; i < match.size() && !match.isOptional(i) && !match.isWildcard(i); i++) {
                    examplePath.remove(0);
                    operationMatchPath.remove(0);
                }


                match = op.endpoint.match(op.method, examplePath);
                if (match != null) {

                    if (op.getCollection() != null) {
                        Path dbPath = op.collection.getDb() == null ? null : op.collection.getDb().getEndpointPath();
                        if (dbPath != null && !dbPath.matches(examplePath))
                            continue;
                    }


                    List<Action> actions = new ArrayList();
                    for (Action action : getActions()) {
                        if (action.match(op.method, operationMatchPath) != null)
                            actions.add(action);
                    }
                    for (int i = 0; i < match.size() && !match.isOptional(i) && !match.isWildcard(i); i++) {
                        examplePath.remove(0);
                        operationMatchPath.remove(0);
                    }


                    for (Action action : op.endpoint.getActions()) {
                        if (action.match(op.method, operationMatchPath) != null)
                            actions.add(action);
                    }
                    Collections.sort(actions);


                    if (op.collection == null || op.collection.match(op.method, examplePath) != null) {
                        if (op.error != null)
                            throw new ApiException(op.error);

                        if (actions.size() == 0) {
                            //throw new ApiException("Operation '{}' does not have any eligible actions to run.", op);
                        }

                        validOps.add(op);
                    }
                }
            }
        }

        operations.clear();
        operations.addAll(validOps);
    }


    Path materialziePath(Path livePath) {
        livePath = new Path(livePath);
        for (int i = 0; i < livePath.size(); i++) {
            String part = livePath.get(i);
            if (livePath.isVar(i)) {
                part = livePath.getVarName(i);
                String regex = livePath.getRegex(i);
                if (regex != null)
                    part = generateMatch(regex);
            }
            if (part.startsWith("["))
                part = part.substring(1);
            if (part.endsWith("]"))
                part = part.substring(0, part.length() - 1);

            livePath.set(i, part);
        }
        return livePath;
    }


    String generateMatch(String regex) {
//        RgxGen         rgxGen        = new RgxGen(regex);
//        StringIterator uniqueStrings = rgxGen.iterateUnique();
//        String         match         = uniqueStrings.next();
//        if (match.length() == 0)
//            match = uniqueStrings.next();
//        return match;
        return null;
    }


    void deduplicateOperationIds(List<Operation> operation) {
        ArrayListValuedHashMap<String, Operation> map = new ArrayListValuedHashMap<>();
        operation.forEach(op -> map.put(op.name, op));

        for (String operationId : map.keySet()) {
            List<Operation> values = map.get(operationId);
            if (values.size() > 1) {
                for (int i = 0; i < values.size(); i++)
                    values.get(i).name += (i + 1);
            }
        }
    }

}
