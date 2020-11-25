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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Contains the Collections, Endpoints and Actions that make up a REST API.
 */
public class Api extends Rule<Api> {

    /**
     * An optional api version identifier
     */
    protected String version = null;

    /**
     * The underlying data sources for the Api.
     */
    protected final           List<Db>          dbs         = new ArrayList<>();

    /**
     * The Request HTTP method/path combinations that map to a distinct set of Actions.
     * <p>
     * A single Endpoint will be selected to run to service a Request.  Any additional
     * Path matching rules that exist on these Endpoint's Actions will be interpreted
     * as relative to the end of the selected Endpoint's Path match.
     */
    protected final           List<Endpoint>    endpoints   = new ArrayList<>();

    /**
     * Actions that may be selected to run regardless of the matched Endpoint.
     * <p>
     * The Action's Path match statements will be considered relative to the Api's
     * base URL NOT relative to the selected Endpoint.
     */
    protected final           List<Action>      actions     = new ArrayList<>();

    /**
     * The data objects being served by this API.  In a simple API these may map
     * one-to-one to, for example, database tables from a JdbcDb connecting to a
     * RDBMS such as MySql or SqlServer.
     */
    protected final           List<Collection>  collections = new ArrayList<>();

    /**
     * Listeners that receive callbacks on startup/shutdown/request/error.
     */
    protected final transient List<ApiListener> listeners   = new ArrayList<>();
    transient protected       String            hash        = null;
    protected                 boolean           debug       = false;
    protected                 String            url         = null;
    transient volatile        boolean           started     = false;
    transient volatile        boolean           starting    = false;
    transient                 long              loadTime    = 0;

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

    synchronized Api startup() {
        if (started || starting) //starting is an accidental recursion guard
            return this;

        starting = true;
        try {
            for (Db db : dbs) {
                db.startup(this);
            }

            removeExcludes();

            started = true;

            for (ApiListener listener : listeners) {
                try {
                    listener.onStartup(this);
                } catch (Exception ex) {
                    log.warn("Error notifing api startup listener: " + listener, ex);
                }
            }

            return this;
        } finally {
            starting = false;
        }
    }

    public boolean isStarted() {
        return started;
    }

    void shutdown() {
        for (Db db : dbs) {
            db.shutdown(this);
        }
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

    public <T extends Db> Api withDb(Db<T> db) {
        if (!dbs.contains(db)) {
            dbs.add(db);

            for (Collection coll : db.getCollections()) {
                withCollection(coll);
            }
        }

        return this;
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
     * This method takes String instead of actual Collections and Properties as a convenience to people hand wiring up an Api.
     * The referenced Collections and Properties actually have to exist already or you will get a NPE.
     *
     * @param parentCollectionName the name of the parent collection
     * @param parentPropertyName   the name of the json property for the parent that references the child
     * @param childCollectionName  the target child collection name
     * @param childPropertyName    the name of hte json property for the child that references the parent
     * @param childFkProps         names of the existing Properties that make up the foreign key
     * @return this
     */
    public Api withRelationship(String parentCollectionName, String parentPropertyName, String childCollectionName, String childPropertyName, String... childFkProps) {
        Collection parentCollection = getCollection(parentCollectionName);
        Collection childCollection  = getCollection(childCollectionName);

        parentCollection.withOneToManyRelationship(parentPropertyName, childCollection, childPropertyName, childFkProps);
        return this;
    }

    /**
     * Creates a ONE_TO_MANY Relationship from the parent to child collection and the inverse MANY_TO_ONE from the child to the parent.
     * The Relationship object along with the required Index objects are created.
     * <p>
     * For collections backed by relational data sources (like a SQL db) the length of <code>childFkProps</code> will generally match the
     * length of <code>parentCollections</code> primary index.  If the two don't match, then <code>childFkProps</code> must be 1.  In this
     * case, the compound primary index of parentCollection will be encoded as an resourceKey in the single child table property.
     *
     * @param parentCollection   the collection to add the relationship to
     * @param parentPropertyName the name of the json property for the parent that references the child
     * @param childCollection    the target child collection
     * @param childPropertyName  the name of hte json property for the child that references the parent
     * @param childFkProps       Properties that make up the foreign key
     * @return this
     */
    public Api withRelationship(Collection parentCollection, String parentPropertyName, Collection childCollection, String childPropertyName, Property... childFkProps) {
        parentCollection.withOneToManyRelationship(parentPropertyName, childCollection, childPropertyName, childFkProps);
        return this;
    }

    public List<Action> getActions() {
        return new ArrayList<>(actions);
    }

    /**
     * Add Action(s) may be selected to run across multiple Endpoints.
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

    public String getVersion() {
        return version;
    }

    public Api withVersion(String version){
        this.version = version;
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
        default void onStartup(Api api) {
            //implement me
        }

        default void onShutdown(Api api) {
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

}
