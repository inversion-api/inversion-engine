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

import io.inversion.json.JSFind;
import io.inversion.json.JSList;
import io.inversion.json.JSNode;
import io.inversion.json.JSReader;
import io.inversion.utils.Path;
import io.inversion.utils.Utils;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Request implements JSFind {

    public static final String COLLECTION_KEY   = "_collection";
    public static final String RESOURCE_KEY     = "_resource";
    public static final String RELATIONSHIP_KEY = "_relationship";

    long startAt = System.currentTimeMillis();
    long endAt   = -1;

    String                                 referrer   = null;
    String                                 remoteAddr = null;
    ArrayListValuedHashMap<String, String> headers    = new ArrayListValuedHashMap<>();

    boolean explain  = false;
    boolean internal = false;

    Engine     engine     = null;
    Chain      chain      = null;
    String     method     = null;
    Url        url        = null;
    Server     server     = null;
    Api        api        = null;
    Op         op         = null;
    Endpoint   endpoint   = null;
    Collection collection = null;
    Db         db         = null;

    Path serverPath     = null;
    Path operationPath  = null;
    Path endpointPath   = null;
    Path dbPath         = null;
    Path collectionPath = null;
    Path actionPath     = null;

    List<Chain.ActionMatch> actionMatches = new ArrayList();
    Map<String, String> pathParams = new HashMap<>();

    String body = null;
    JSNode json = null;

    Uploader uploader = null;


    public Request() {

    }

    public Request(String method, String url) {
        this(method, url, null, null);
    }

    public Request(String method, String url, String body) {
        withMethod(method);
        withUrl(url);
        withBody(body);
    }

    public Request(Engine engine, String method, String url, Object body) {
        withEngine(engine);
        withMethod(method);
        withUrl(url);
        if (body != null)
            withBody(body.toString());
    }

    public Request(String method, String url, String body, ArrayListValuedHashMap<String, String> headers) {
        this(method, url, body, null, headers);
    }

    public Request(String method, String url, String body, Map<String, String> params, ArrayListValuedHashMap<String, String> headers) {
        withMethod(method);
        withUrl(url);
        withBody(body);

        if (params != null) {
            this.url.withParams(params);
        }

        if (headers != null && headers.size() > 0)
            this.headers = new ArrayListValuedHashMap<>(headers);
    }

    public Request withUrl(String url) {
        Url u = new Url(url);

        String key = u.findKey("explain");
        if (key != null) {
            String explain = u.clearParams(key);
            if (Utils.empty(explain) || "true".equalsIgnoreCase(explain.trim()))
                withExplain(true);

            //-- makes the url.original look like it does not include the explain param;
            u = new Url(u.toString());
        }
        this.url = u;

        return this;
    }

    public long getStartAt() {
        return startAt;
    }

    public Request withStartAt(long startAt) {
        this.startAt = startAt;
        return this;
    }

    public long getEndAt() {
        return endAt;
    }

    public Request withEndAt(long endAt) {
        this.endAt = endAt;
        return this;
    }

    public long getDuration() {
        if (endAt < 1)
            return System.currentTimeMillis() - startAt;
        else
            return endAt - startAt;
    }

    public Request withMethod(String method) {
        this.method = method;
        return this;
    }

    public Request withHeaders(String key, String value) {
        this.headers.put(key, value);
        return this;
    }

    public Request withHeaders(Map<String, String> headers) {
        this.headers.putAll(headers);
        return this;
    }

    public Server getServer() {
        return server;
    }

    public Request withServer(Server server) {
        this.server = server;
        return this;
    }

    public Api getApi() {
        return api;
    }

    public Request withApi(Api api) {
        this.api = api;
        return this;
    }

    public Engine getEngine() {
        return engine;
    }

    public Request withEngine(Engine engine) {
        this.engine = engine;
        return this;
    }


    public boolean isInternal() {
        return internal;
    }

    public Request withInternal(boolean internal) {
        this.internal = internal;
        return this;
    }

    public Collection getCollection() {
        return collection;
    }

    public Request withCollection(Collection collection) {
        this.collection = collection;
        return this;
    }

    public Db getDb() {
        return db;
    }

    public Request withDb(Db db) {
        this.db = db;
        return this;
    }

    public Path getDbPath() {
        return dbPath;
    }

    public Request withDbPath(Path dbPath) {
        this.dbPath = dbPath;
        return this;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public Request withEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public Path getServerPath() {
        return serverPath;
    }

    public Request withServerPath(Path serverPath) {
        this.serverPath = serverPath;
        return this;
    }

    public Path getOperationPath() {
        return operationPath;
    }

    public Request withOperationPath(Path operationPath) {
        this.operationPath = operationPath;
        return this;
    }

    public Path getEndpointPath() {
        return endpointPath;
    }

    public Request withEndpointPath(Path endpointPath) {
        this.endpointPath = endpointPath;
        return this;
    }

    public Path getActionPath() {
        return actionPath;
    }

    public Request withActionPath(Path actionPath) {
        this.actionPath = actionPath;
        return this;
    }

    public Path getCollectionPath() {
        return collectionPath;
    }

    public Request withCollectionPath(Path collectionPath) {
        this.collectionPath = collectionPath;
        return this;
    }


    public Request withActionMatches(List<Chain.ActionMatch> actionMatches) {
        this.actionMatches.addAll(actionMatches);
        return this;
    }

    public Request withActionMatch(Chain.ActionMatch actionMatch) {
        this.actionMatches.add(actionMatch);
        return this;
    }


    public List<Chain.ActionMatch> getActionMatches() {
        return actionMatches;
    }

    public Request withPathParams(Map<String, String> pathParams) {
        pathParams.keySet().forEach(url::clearParams);
        pathParams.entrySet().stream().filter((e -> e.getValue() != null)).forEach(e -> url.withParam(e.getKey(), e.getValue()));

        return this;
    }

    public Map<String, String> getPathParams() {
        return this.pathParams;
    }

    public boolean isDebug() {
        String host = getUrl().getHost().toLowerCase();
        if ("127.0.0.1".equals(host))
            return true;

        if (getApi() != null)
            return getApi().isDebug();

        return false;
    }

    public boolean isExplain() {
        return explain;
    }

    public Request withExplain(boolean explain) {
        this.explain = explain;
        return this;
    }

    public String getBody() {
        return body;
    }

    public Request withBody(String body) {
        this.body = body;
        return this;
    }

    public JSNode getJson() throws ApiException {
        if (json != null)
            return json;

        String body = getBody();
        if (Utils.empty(body))
            return null;

        try {
            json = JSReader.asJSNode(body);
        } catch (Exception ex) {
            throw ApiException.new400BadRequest("Unparsable JSON body");
        }

        return json;
    }

    /**
     * Attempts to massage an inbound json body into an array.
     * <p>
     * This is useful so actions can treat all inbound requests as if they are arrays instead of having to check.
     * <p>
     * Conversion rules:
     * <ol>
     *   <li>if getBody() is a JSList return it.
     *   <li>if getBody() is a JSNode with a "data" array prop, return it
     *   <li>if getBody() is a JSNode with a "_embedded" array prop, return it
     *   <li>if getBody() is a JSNode wrap it in an array and return it.
     *   <li>if getBody() is not a JSNode and getBody() is null, return an empty array.
     * </ol>
     *
     * @return the JSON boty messaged into an array
     */
    public JSList getData() {
        JSNode node = getJson();
        if (node != null) {
            if (node instanceof JSList) {
                return (JSList) node;
            } else if (node.getValue("data") instanceof JSList) {
                return node.getList("data");
            } else if (node.getValue("_embedded") instanceof JSList) {
                return node.getList("_embedded");
            } else {
                return new JSList(node);
            }
        } else if (getBody() == null)
            return new JSList();
        return null;
    }

    //todo we should probably remove the ability for end users to modify the json?
    public Request withJson(JSNode json) {
        this.json = json;
        return this;
    }

    /**
     * @return the method
     */
    public String getMethod() {
        return method;
    }

    public boolean isMethod(String... methods) {
        for (String method : methods) {
            if (this.method.equalsIgnoreCase(method))
                return true;
        }
        return false;
    }

    public boolean isPut() {
        return "put".equalsIgnoreCase(method);
    }

    public boolean isPost() {
        return "post".equalsIgnoreCase(method);
    }

    public boolean isPatch() {
        return "patch".equalsIgnoreCase(method);
    }

    public boolean isGet() {
        return "get".equalsIgnoreCase(method);
    }

    public boolean isDelete() {
        return "delete".equalsIgnoreCase(method);
    }

    public String getReferrer() {
        return getHeader("referrer");
    }

    public String getHeader(String key) {
        key = key.toLowerCase();
        List<String> vals = headers.get(key);
        if (vals != null && vals.size() > 0)
            return vals.get(0);
        return null;
    }

    public void removeHeader(String key) {
        key = key.toLowerCase();
        headers.remove(key);
    }

    /**
     * @return the headers
     */
    public ArrayListValuedHashMap<String, String> getHeaders() {
        return headers;
    }

    public void withHeader(String key, String value) {
        key = key.toLowerCase();
        if (!headers.containsMapping(key, value))
            headers.put(key, value);
    }

    public Chain getChain() {
        return chain;
    }

    public Request withChain(Chain chain) {
        this.chain = chain;
        return this;
    }

    public Url getUrl() {
        return url;
    }


    /**
     * @return the collectionKey
     */
    public String getCollectionKey() {
        return url.getParam(COLLECTION_KEY);
    }

    /**
     * @return the resourceKey
     */
    public String getResourceKey() {
        return url.getParam(RESOURCE_KEY);
    }

    public String getRelationshipKey() {
        return url.getParam(RELATIONSHIP_KEY);
    }

    public Relationship getRelationship() {
        return op.getRelationship();
    }

    //TODO: should this be here
    public String getApiUrl() {
        return url.getProtocol() + "://" + url.getHost() + (url.getPort() > 0 ? ":" + url.getPort() : "") + "/" + getServerPath();
    }

    //TODO: should this be here
    public Path getPath() {
        return new Path(endpointPath.toString(), actionPath.toString());
    }

    //TODO: should this be here
    public Path getSubpath() {
        return actionPath;
    }


    public Op getOp() {
        return op;
    }

    public Request withOp(Op op) {
        this.op = op;
        return this;
    }

    public String getRemoteAddr() {
        String remoteAddr = getHeader("X-Forwarded-For");
        if (remoteAddr == null || remoteAddr.length() == 0 || "unknown".equalsIgnoreCase(remoteAddr)) {
            remoteAddr = getHeader("Proxy-Client-IP");
        }
        if (remoteAddr == null || remoteAddr.length() == 0 || "unknown".equalsIgnoreCase(remoteAddr)) {
            remoteAddr = getHeader("WL-Proxy-Client-IP");
        }
        if (remoteAddr == null || remoteAddr.length() == 0 || "unknown".equalsIgnoreCase(remoteAddr)) {
            remoteAddr = getHeader("HTTP_CLIENT_IP");
        }
        if (remoteAddr == null || remoteAddr.length() == 0 || "unknown".equalsIgnoreCase(remoteAddr)) {
            remoteAddr = getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (remoteAddr == null || remoteAddr.length() == 0 || "unknown".equalsIgnoreCase(remoteAddr)) {
            remoteAddr = this.remoteAddr;
        }

        return remoteAddr;
    }

    public Request withRemoteAddr(String remoteAddr) {
        this.remoteAddr = remoteAddr;
        return this;
    }

    public Uploader getUploader() {
        return uploader;
    }

    public Request withUploader(Uploader uploader) {
        this.uploader = uploader;
        return this;
    }


    public List<Upload> getUploads() {
        return uploader.getUploads();
    }


    public Validation validate(String propOrJsonPath) {
        return validate(propOrJsonPath, null);
    }

    public Validation validate(String propOrJsonPath, String customErrorMessage) {
        return new Validation(this, propOrJsonPath, customErrorMessage);
    }

}
