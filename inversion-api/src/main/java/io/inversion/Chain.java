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

import io.inversion.json.JSList;
import io.inversion.json.JSMap;
import io.inversion.json.JSNode;
import io.inversion.utils.*;
import io.inversion.utils.Utils;
import org.apache.commons.collections4.map.CaseInsensitiveMap;

import java.util.*;

public class Chain {

    public static final Set<String> APPEND_PARAMS = Collections.unmodifiableSet(Utils.add(new HashSet(), "include", "exclude", "collapse"));

    static          ThreadLocal<Stack<Chain>>          chainLocal         = new ThreadLocal<>();
    protected final Engine                             engine;
    protected final List<ActionMatch>                  actions            = new ArrayList<>();
    protected final Request                            request;
    protected final Response                           response;
    protected final CaseInsensitiveMap<String, Object> vars               = new CaseInsensitiveMap<>();
    protected       int                                next               = 0;
    protected       boolean                            canceled           = false;
    protected       User                               user               = null;
    protected       Chain                              parent             = null;
    protected       Set<String>                        pathParamsToRemove = new HashSet();

    private Chain(Engine engine, Request req, Response res) {
        this.engine = engine;
        this.request = req;
        this.response = res;
    }

    public static void resetAll() {
        chainLocal = new ThreadLocal<>();
    }

    protected static Stack<Chain> get() {
        Stack<Chain> stack = chainLocal.get();
        if (stack == null) {
            stack = new Stack<>();
            chainLocal.set(stack);
        }
        return stack;
    }

    public static int getDepth() {
        return get().size();
    }

    public static boolean isRoot() {
        Stack<Chain> stack = get();
        return stack.isEmpty() || stack.size() == 1;
    }

    public static Chain first() {
        Stack<Chain> stack = get();
        if (!stack.empty()) {
            return stack.get(0);
        }
        return null;
    }

    public static Chain top() throws ApiException {
        Stack<Chain> stack = get();
        if (!stack.empty())
            return stack.peek();
        throw ApiException.new500InternalServerError("Attempting to call Chain.top() when there is no Chain on the ThreadLocal.");
    }

    public static Chain peek() {
        Stack<Chain> stack = get();
        if (!stack.empty())
            return stack.peek();
        return null;
    }

    public static Chain push(Engine engine, Request req, Response res) {
        Chain child = new Chain(engine, req, res);

        Chain parent = peek();
        if (parent != null)
            child.setParent(parent);

        req.withChain(child);
        get().push(child);

        return child;
    }

    public static Chain pop() {
        return get().pop();
    }

    public static User getUser() {
        Chain chain = peek();
        if (chain != null) {
            do {
                if (chain.user != null)
                    return chain.user;
            }
            while ((chain = chain.parent) != null);
        }
        return null;
    }

    public static int size() {
        return get().size();
    }

    public static void debug(String format, Object... args) {

        if (format == null || format.trim().length() == 0)
            return;

        Stack<Chain> stack = get();
        if (stack.size() < 1) {
            return;
        }

        StringBuilder prefix = new StringBuilder("[" + stack.size() + "]: ");
        for (int i = 1; i < stack.size(); i++)
            prefix.append("   ");

        format = prefix.toString() + format;

        Chain root = stack.get(0);
        root.response.debug(format, args);
    }

    public static String buildLink(JSMap fromHere, Relationship toHere) {
        String link = null;
        if (toHere.isManyToOne()) {
            String fkval = null;
            if (toHere.getRelated().getResourceIndex().size() != toHere.getFkIndex1().size() && toHere.getFkIndex1().size() == 1) {
                fkval = toHere.getCollection().encodeKeyFromJsonNames(fromHere, toHere.getFkIndex1());
            } else {
                //this value is already an encoded resourceKey
                Object obj = fromHere.getValue(toHere.getFk1Col1().getJsonName());
                if (obj != null)
                    fkval = obj.toString();
            }

            if (fkval != null) {
                link = Chain.buildLink(toHere.getRelated(), fkval);
            }
        } else {
            //link = Chain.buildLink(req.getCollection(), resourceKey, rel.getName());
            String resourceKey = toHere.getCollection().encodeKeyFromJsonNames(fromHere);
            link = Chain.buildLink(toHere.getCollection(), resourceKey);
        }
        return link;
    }

    public static String buildLink(Collection collection) {
        return buildLink(collection, null);
    }

    public static String buildLink(Collection collection, String resourceKey) {
        return buildLink(collection, resourceKey, null);
    }

    public static String buildLink(Collection collection, String resourceKey, String relationshipKey) {
        Request req       = top().getRequest();
        Request linkedReq = req.getApi().getLinker().buildRequest(req, null, "GET", collection, resourceKey, relationshipKey, null);
        //if (linkedReq == null)
        //    throw ApiException.new400BadRequest("Unable to find a valid operation.");

        if (linkedReq == null)
            return null;

        Url    url = linkedReq.getUrl();
        String str = url.toString();
        //System.out.println(str);
        return str;

//        Request req = top().getRequest();
//
//
//        if(subCollectionKey != null)
//        Operation op = req.getApi().findOperation(req, null, null, resourceKey, relationship
//
//
//        Request req = top().getRequest();
//
//        String collectionKey = collection.getName();
//
//        if (req.getCollection() == collection)
//            collectionKey = req.getCollectionKey();
//
//        StringBuilder url = new StringBuilder(Utils.empty(req.getApiUrl()) ? "" : req.getApiUrl());
//
//        if (!Utils.endsWith(url, "/"))
//            url.append("/");
//
//        if (req.getCollection() != null && (collection == req.getCollection() || collection.getDb() == req.getCollection().getDb()))//
//        {
//            //going after the same collection...so must be going after the same endpoint
//            //so get the endpoint path from the current request and ame sure it is on the url.
//
//            Path epp = req.getEndpointPath();
//
//            if (epp != null && epp.size() > 0) {
//                url.append(epp).append("/");
//            }
//        } else if (collection.getDb().getEndpointPath() != null) {
//            Path epP = collection.getDb().getEndpointPath();
//
//            for (int i = 0; i < epP.size(); i++) {
//                if (epP.isWildcard(i))
//                    break;
//
//                if (epP.isVar(i)) {
//                    String value;
//                    String name = epP.getVarName(i);
//                    switch (name.toLowerCase()) {
//                        case "_collection":
//                            value = collection.getName();
//                            break;
//                        case "_resource":
//                            value = resourceKey + "";
//                            break;
//                        case "_relationship":
//                            value = subCollectionKey;
//                            break;
//                        default:
//                            value = req.getUrl().getParam(name);
//                    }
//                    if (value == null)
//                        throw ApiException.new500InternalServerError("Unable to determine path for link to collection '{}', resource '{}', relationship '{}'", collection.getName(), resourceKey + "", subCollectionKey + "");
//
//                    url.append(epP.get(i)).append("/");
//                } else {
//                    url.append(epP.get(i)).append("/");
//                }
//            }
//
//            url.append(collection.getDb().getEndpointPath()).append("/");
//        }
//
//        if (!Utils.empty(collectionKey)) {
//            if (!Utils.endsWith(url, "/"))
//                url.append("/");
//
//            url.append(collectionKey);
//        }
//
//        if (!Utils.empty(resourceKey)){
//            if (!Utils.endsWith(url, "/"))
//                url.append("/");
//            url.append(resourceKey.toString());
//        }
//
//
//        if (!Utils.empty(subCollectionKey)){
//            if (!Utils.endsWith(url, "/"))
//                url.append("/");
//            url.append(subCollectionKey);
//        }
//
//        if (req.getApi().getUrl() != null && !Utils.startsWith(url, req.getApi().getUrl())) {
//            String newUrl = req.getApi().getUrl();
//            while (newUrl.endsWith("/"))
//                newUrl = newUrl.substring(0, newUrl.length() - 1);
//
//            url = new StringBuilder(newUrl).append(url.substring(url.indexOf("/", 8)));
//        }
//
//        if (req.getApi() != null) {
//            if (Utils.empty(req.getApi().getUrl())) {
//                String proto = req.getHeader("x-forwarded-proto");
//                if (!Utils.empty(proto)) {
//                    url = new StringBuilder(proto).append(url.substring(url.indexOf(":")));
//                }
//            }
//        }
//        return new Url(url.toString()).toString();
    }

    public Chain withUser(User user) {
        this.user = user;
        return this;
    }

    public Chain getParent() {
        return parent;
    }

    public void setParent(Chain parent) {
        this.parent = parent;
    }

    public void put(String key, Object value) {
        vars.put(key, value);
    }

    public boolean isDebug() {
        if (parent != null)
            return parent.isDebug();

        return request.isDebug();
    }

    /**
     * Storage for chain steps to communicate with each other.
     *
     * @param key the name of the value to retrieve
     * @return the value if it exists otherwise null
     */
    public Object get(String key) {
        if (vars.containsKey(key))
            return vars.get(key);

        Object value = request.getUrl().getParam(key);
        if (value != null)
            return value;

        if (parent != null)
            return parent.get(key);

        return null;
    }

    public Object remove(String key) {
        if (vars.containsKey(key))
            return vars.remove(key);

        return get(key);
    }

    public void go() throws ApiException {
        boolean root = next == 0;
        try {
            //TODO: this is not correct, the Params in the op should have a source and get applied from the op params based on the source not being an action
            if (root)
                applyRuleParams(getRequest().getUrl(), getServer(), getEndpoint());


            while (next()) {
                //-- intentionally empty
            }

        } finally {
            if (root) {
                filterPathParams(response.getJson());
            }
        }
    }

    /**
     * Recursively removes any url path params that appear as properties in the json
     *
     * @param json the json node to clean
     * @return this
     */
    public Chain filterPathParams(JSNode json) {
        if (json != null && request.pathParams.size() > 0) {
            json.streamAll()
                    .filter(node -> node instanceof JSNode && !(node instanceof JSList))
                    .forEach(node -> {
                        pathParamsToRemove.forEach(key -> ((JSNode) node).removeValues(key));
                    });
        }
        return this;
    }

    public Chain skipNext() {
        next += 1;
        return this;
    }

    public Action getNext() {
        if (hasNext())
            return actions.get(next).action;
        return null;
    }

    public boolean next() throws ApiException {
        if (!isCanceled() && next < actions.size()) {
            ActionMatch actionMatch = actions.get(next);
            next += 1;

            Map<String, String> pathParams = new HashMap<>();
            if (actionMatch.path != null) {
                actionMatch.rule.extract(pathParams, new Path(actionMatch.path));
                applyPathParams(pathParams, request.getUrl(), request.getJson());
            }
            applyRuleParams(request.getUrl(), actionMatch.action);
            actionMatch.action.run(request, response);
            return true;
        }
        return false;
    }

//    public Chain withPathParams(Map<String, String> pathParams){
//        this.pathParams.putAll(pathParams);
//        return this;
//    }

    void applyPathParams(Map<String, String> pathParamsToAdd, Url url, JSNode json) {
        pathParamsToAdd.keySet().forEach(url::clearParams);
        pathParamsToAdd.entrySet().stream().filter((e -> e.getValue() != null)).forEach(e -> url.withParam(e.getKey(), e.getValue()));

        if (json != null) {
            json.stream()
                    .filter(node -> node instanceof JSMap)
                    .forEach(node -> pathParamsToAdd.entrySet().stream().filter((e -> e.getValue() != null && !e.getKey().startsWith("_"))).forEach(e -> ((JSNode) node).putValue(e.getKey(), e.getValue())));
        }

        pathParamsToRemove.addAll(pathParamsToAdd.keySet());
    }


    public Chain applyRuleParams(Url url, Rule... rules) {
        for (Rule rule : rules) {
            if (rule == null)
                continue;
            String query = rule.getQuery();
            if (query != null) {
                Url temp = new Url("http://127.0.0.1?" + query);
                for (String name : temp.getParams().keySet()) {
                    String value = temp.getParam(name);
                    if (APPEND_PARAMS.contains(name.toLowerCase())) {
                        String previous = url.getParam(name);
                        if (previous != null)
                            value = value + "," + previous;
                    }
                    url.withParam(name, value);
                }
            }
        }
        return this;
    }

    public boolean hasNext() {
        return !isCanceled() && next < actions.size();
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void cancel() {
        this.canceled = true;
    }

    public Engine getEngine() {
        return engine;
    }

    public Api getApi() {
        return request.getApi();
    }

    public Server getServer() {
        return request.getServer();
    }

    public Endpoint getEndpoint() {
        return request.getEndpoint();
    }

    public List<ActionMatch> getActions() {
        return new ArrayList<>(actions);
    }

    public Chain withActions(List<ActionMatch> actions) {
        for (ActionMatch action : actions) {
            withAction(action);
        }
        return this;
    }

    public Chain withAction(ActionMatch action) {
        if (action != null && !actions.contains(action))
            actions.add(action);

        return this;
    }

    public Request getRequest() {
        return request;
    }

    public Response getResponse() {
        return response;
    }

    public static class ActionMatch implements Comparable<ActionMatch> {
        final Path   rule;
        final Path   path;
        final Action action;

        public ActionMatch(Path rule, Path path, Action action) {
            super();
            this.rule = rule;
            this.path = path;
            this.action = action;
        }

        @Override
        public int compareTo(ActionMatch o) {
            return action.compareTo(o.action);
        }

        public String toString() {
            return rule + " " + path + " " + action;
        }

        public Path getRule() {
            return rule;
        }

        public Path getPath() {
            return path;
        }

        public Action getAction() {
            return action;
        }
    }

}
