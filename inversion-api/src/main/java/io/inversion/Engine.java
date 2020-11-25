/*
 * Copyright (c) 2015-2020 Rocket Partners, LLC
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

import ch.qos.logback.classic.Level;
import io.inversion.Api.ApiListener;
import io.inversion.Chain.ActionMatch;
import io.inversion.rql.RqlParser;
import io.inversion.rql.Term;
import io.inversion.utils.*;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Matches inbound Request Url paths to an Api Endpoint and executes associated Actions.
 */
public class Engine extends Rule<Engine> {

    static {
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("ROOT");
        logger.setLevel(Level.WARN);
    }

    /**
     * Listeners that will receive Engine and Api lifecycle, request, and error callbacks.
     */
    protected final transient    List<EngineListener> listeners        = new ArrayList<>();
    /**
     * The last {@code Response} served by this Engine, primarily used for writing test cases.
     */
    protected transient volatile Response             lastResponse     = null;
    /**
     * The {@code Api}s being service by this Engine
     */
    protected                    List<Api>            apis             = new Vector<>();
    /**
     * Base value for the CORS "Access-Control-Allow-Headers" response header.
     * <p>
     * Values from the request "Access-Control-Request-Header" header are concatenated
     * to this resulting in the final value of "Access-Control-Allow-Headers" sent in the response.
     * <p>
     * Unless you are really doing something specific with browser security you probably won't need to customize this list.
     */
    protected                    String               corsAllowHeaders = "accept,accept-encoding,accept-language,access-control-request-headers,access-control-request-method,authorization,connection,content-type,host,user-agent,x-auth-token";
    /**
     * Optional override for the configPath sys/env prop used by Config to locate configuration property files
     *
     * @see Config#loadConfiguration(String, String)
     */
    protected                    String               configPath       = null;
    /**
     * Optional override for the sys/env prop used by Config to determine which profile specific configuration property files to load
     *
     * @see Config#loadConfiguration(String, String)
     */
    protected                    String               configProfile    = null;
    transient volatile           boolean              started          = false;
    transient volatile           boolean              starting         = false;

    public Engine() {

    }

    public Engine(Api... apis) {
        if (apis != null)
            for (Api api : apis)
                withApi(api);
    }

    public static void applyPathParams(Map<String, String> pathParams, Url url, JSNode json) {
        pathParams.keySet().forEach(url::clearParams);
        pathParams.entrySet().stream().filter((e -> e.getValue() != null)).forEach(e -> url.withParam(e.getKey(), e.getValue()));

        if (json != null) {
            json.stream()
                    .filter(node -> node instanceof JSNode && !(node instanceof JSArray))
                    .forEach(node -> pathParams.entrySet().stream().filter((e -> e.getValue() != null)).forEach(e -> ((JSNode) node).put(e.getKey(), e.getValue())));
        }
    }

    /**
     * Convenient pre-startup hook for subclasses guaranteed to only be called once.
     * <p>
     * Called after <code>starting</code> has been set to true but before the {@code Configurator} is run or  any {@code Api}s have been started.
     */
    protected void startup0() {
        //implement me
    }

    /**
     * Runs the {@code Configurator} and calls <code>startupApi</code> for each Api.
     * <p>
     * An Engine can only be started once.
     * Any calls to <code>startup</code> after the initial call will not have any affect.
     *
     * @return this Engine
     */
    public synchronized Engine startup() {
        if (started || starting) //accidental recursion guard
            return this;

        System.out.println("STARTING ENGINE...");

        starting = true;
        try {
            startup0();

            if (!Config.hasConfiguration()) {
                Config.loadConfiguration(getConfigPath(), getConfigProfile());
            }
            new Configurator().configure(this, Config.getConfiguration());

            started = true;

            boolean hasApi = false;
            for (Api api : apis) {
                hasApi = true;

                if (api.getEndpoints().size() == 0)
                    throw ApiException.new500InternalServerError("CONFIGURATION ERROR: You have configured an Api without any Endpoints.");

                startupApi(api);
            }
            if (!hasApi)
                throw ApiException.new500InternalServerError("CONFIGURATION ERROR: You don't have any Apis configured.");

            //-- debug output
            for (Api api : apis) {
                System.out.println("\r\n--------------------------------------------");
                System.out.println("API             " + api);

                for (Endpoint e : api.getEndpoints()) {
                    System.out.println("  - ENDPOINT:   " + e);
                }

                List<String> collectionDebugs = new ArrayList<>();
                for (Collection c : api.getCollections()) {
                    if (c.getDb() != null && c.getDb().getEndpointPath() != null)
                        collectionDebugs.add(c.getDb().getEndpointPath() + c.getName());
                    else
                        collectionDebugs.add(c.getName());
                }
                Collections.sort(collectionDebugs);
                for (String coll : collectionDebugs) {
                    System.out.println("  - COLLECTION: " + coll);
                }
            }
            //-- end debug output

            return this;
        } finally {
            starting = false;
        }
    }

    /**
     * Removes all Apis and notifies listeners.onShutdown
     */
    public void shutdown() {
        for (Api api : getApis()) {
            removeApi(api);
        }

        for (EngineListener listener : listeners) {
            try {
                listener.onShutdown(this);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        Chain.resetAll();
    }

    /**
     * Convenience overloading of {@code #service(Request, Response)} to run a REST GET Request on this Engine.
     * <p>
     * IMPORTANT: This method does not make an external HTTP request, it runs the request on this Engine.
     * If you want to make an external HTTP request see {@link io.inversion.utils.RestClient}.
     * <p>
     * GET requests for a specific resource should return 200 of 404.
     * GET requests with query string search conditions should return 200 even if the search did not yield any results.
     *
     * @param url the url that will be serviced by this Engine
     * @return the Response generated by handling the Request
     * @see #service(Request, Response)
     * @see io.inversion.action.db.DbGetAction
     */
    public Response get(String url) {
        return service("GET", url, null);
    }

    /**
     * Convenience overloading of {@code #service(Request, Response)} to run a REST GET Request on this Engine.
     * <p>
     * IMPORTANT: This method does not make an external HTTP request, it runs the request on this Engine.
     * If you want to make an external HTTP request see {@link io.inversion.utils.RestClient}.
     * <p>
     * GET requests for a specific resource should return 200 of 404.
     * GET requests with query string search conditions should return 200 even if the search did not yield any results.
     *
     * @param url    the url that will be serviced by this Engine
     * @param params additional key/value pairs to add to the url query string
     * @return the Response generated by handling the Request
     * @see #service(Request, Response)
     * @see io.inversion.action.db.DbGetAction
     */
    public Response get(String url, Map<String, String> params) {
        return service("GET", url, null, params);
    }

    /**
     * Convenience overloading of {@code #service(Request, Response)} to run a REST GET Request on this Engine.
     * <p>
     * IMPORTANT: This method does not make an external HTTP request, it runs the request on this Engine.
     * If you want to make an external HTTP request see {@link io.inversion.utils.RestClient}.
     * <p>
     * GET requests for a specific resource should return 200 of 404.
     * GET requests with query string search conditions should return 200 even if the search did not yield any results.
     *
     * @param url        the url that will be serviced by this Engine
     * @param queryTerms additional keys (no values) to add to the url query string
     * @return the Response generated by handling the Request
     * @see #service(Request, Response)
     * @see io.inversion.action.db.DbGetAction
     */
    public Response get(String url, List queryTerms) {
        if (queryTerms != null && queryTerms.size() > 0) {
            Map<String, String> params = new HashMap<>();
            queryTerms.stream().filter(Objects::nonNull).forEach(key -> params.put(key.toString(), null));
            return service("GET", url, null, params);
        } else {
            return service("GET", url, null, null);
        }
    }

    /**
     * Convenience overloading of {@code #service(Request, Response)} to run a REST POST Request on this Engine.
     * <p>
     * IMPORTANT: This method does not make an external HTTP request, it runs the request on this Engine.
     * If you want to make an external HTTP request see {@link io.inversion.utils.RestClient}.
     * <p>
     * Successful POSTs that create a new resource should return a 201.
     *
     * @param url  the url that will be serviced by this Engine
     * @param body the JSON body to POST which will be stringified first
     * @return the Response generated by handling the Request
     * @see #service(Request, Response)
     * @see io.inversion.action.db.DbPostAction
     */
    public Response post(String url, JSNode body) {
        return service("POST", url, body.toString());
    }

    /**
     * Convenience overloading of {@code #service(Request, Response)} to run a REST PUT Request on this Engine.
     * <p>
     * IMPORTANT: This method does not make an external HTTP request, it runs the request on this Engine.
     * If you want to make an external HTTP request see {@link io.inversion.utils.RestClient}.
     * <p>
     * Successful PUTs that update an existing resource should return a 204.
     * If the PUT references a resource that does not exist, a 404 will be returned.
     *
     * @param url  the url that will be serviced by this Engine
     * @param body the JSON body to POST which will be stringified first
     * @return the Response generated by handling the Request
     * @see #service(Request, Response)
     * @see io.inversion.action.db.DbPutAction
     */
    public Response put(String url, JSNode body) {
        return service("PUT", url, body.toString());
    }

    /**
     * Convenience overloading of {@code #service(Request, Response)} to run a REST PATCH Request on this Engine.
     * <p>
     * IMPORTANT: This method does not make an external HTTP request, it runs the request on this Engine.
     * If you want to make an external HTTP request see {@link io.inversion.utils.RestClient}.
     * <p>
     * Successful PATCHes that update an existing resource should return a 204.
     * If the PATCH references a resource that does not exist, a 404 will be returned.
     *
     * @param url  the url for a specific resource that should be PATCHed that will be serviced by this Engine
     * @param body the JSON body to POST which will be stringified first
     * @return the Response generated by handling the Request
     * @see #service(Request, Response)
     * @see io.inversion.action.db.DbPatchAction
     */
    public Response patch(String url, JSNode body) {
        return service("PATCH", url, body.toString());
    }

    /**
     * Convenience overloading of {@code #service(Request, Response)} to run a REST DELETE Request on this Engine.
     * <p>
     * IMPORTANT: This method does not make an external HTTP request, it runs the request on this Engine.
     * If you want to make an external HTTP request see {@link io.inversion.utils.RestClient}.
     *
     * @param url the url of the resource to be DELETED
     * @return the Response generated by handling the Request with status 204 if the delete was successful or 404 if the resource was not found
     * @see #service(Request, Response)
     * @see io.inversion.action.db.DbDeleteAction
     */
    public Response delete(String url) {
        return service("DELETE", url, null);
    }

    /**
     * Convenience overloading of {@code #service(Request, Response)} to run a REST DELETE Request on this Engine.
     * <p>
     * IMPORTANT: This method does not make an external HTTP request, it runs the request on this Engine.
     * If you want to make an external HTTP request see {@link io.inversion.utils.RestClient}.
     *
     * @param url   the url of the resource to be DELETED
     * @param hrefs the hrefs of the resource to delete
     * @return the Response generated by handling the Request with status 204 if the delete was successful or 404 if the resource was not found
     * @see #service(Request, Response)
     * @see io.inversion.action.db.DbDeleteAction
     */
    public Response delete(String url, JSArray hrefs) {
        return service("DELETE", url, hrefs.toString());
    }

    /**
     * Convenience overloading of {@code #service(Request, Response)}
     * <p>
     * IMPORTANT: This method does not make an external HTTP request, it runs the request on this Engine.
     * If you want to make an external HTTP request see {@link io.inversion.utils.RestClient}.
     *
     * @param method the http method of the requested operation
     * @param url    the url that will be serviced by this Engine
     * @return the Response generated by handling the Request
     * @see #service(Request, Response)
     */
    public Response service(String method, String url) {
        return service(method, url, null);
    }

    /**
     * Convenience overloading of {@code #service(Request, Response)}
     * <p>
     * IMPORTANT: This method does not make an external HTTP request, it runs the request on this Engine.
     * If you want to make an external HTTP request see {@link io.inversion.utils.RestClient}.
     *
     * @param method the http method of the requested operation
     * @param url    the url that will be serviced by this Engine.
     * @param body   a stringified JSON body presumably to PUT/POST/PATCH
     * @return the Response generated by handling the Request
     * @see #service(Request, Response)
     */
    public Response service(String method, String url, String body) {
        return service(method, url, body, null);
    }

    /**
     * Convenience overloading of {@code #service(Request, Response)}
     * <p>
     * IMPORTANT: This method does not make an external HTTP request, it runs the request on this Engine.
     * If you want to make an external HTTP request see {@link io.inversion.utils.RestClient}.
     *
     * @param method the http method of the requested operation
     * @param url    the url that will be serviced by this Engine.
     * @param body   a stringified JSON body presumably to PUT/POST/PATCH
     * @param params additional key/value pairs to add to the url query string
     * @return the Response generated by handling the Request
     * @see #service(Request, Response)
     */
    public Response service(String method, String url, String body, Map<String, String> params) {
        Request req = new Request(method, url, body);
        req.withEngine(this);

        if (params != null) {
            for (String key : params.keySet()) {
                req.getUrl().withParam(key, params.get(key));
            }
        }

        Response res = new Response();

        service(req, res);
        return res;
    }

    /**
     * The main entry point for processing a Request and generating Response content.
     * <p>
     * This method is designed to be called by integrating runtimes such as {@code EngineServlet} or by {@code Action}s that
     * need to make recursive calls to the Engine when performing composite operations.
     * <p>
     * The host and port component of the Request url are ignored assuming that this Engine instance is supposed to be servicing the request.
     * The url does not have to start with "http[s]://".  If it does not, urls that start with "/" or not are handled the same.
     * <p>
     * All of the following would be processed the same way:
     * <ul>
     *   <li>https://library.com/v1/library/books?ISBN=1234567890
     *   <li>https://library.com:8080/v1/library/books?ISBN=1234567890
     *   <li>https://localhost/v1/library/books?ISBN=1234567890
     *   <li>/v1/library/books?ISBN=1234567890
     *   <li>v1/library/books?ISBN=1234567890
     * </ul>
     *
     * @param req the api Request
     * @param res the api Response
     * @return the Chain representing all of the actions executed in populating the Response
     */
    public Chain service(Request req, Response res) {
        Chain chain = null;

        try {
            if (!started)
                startup();

            chain = Chain.push(this, req, res);
            req.withEngine(this);
            req.withChain(chain);
            res.withChain(chain);

            //--
            //-- CORS header setup
            //--
            String allowedHeaders    = this.corsAllowHeaders;
            String corsRequestHeader = req.getHeader("Access-Control-Request-Header");
            if (corsRequestHeader != null) for (String h : corsRequestHeader.split(",")) {
                h = h.trim();
                allowedHeaders = allowedHeaders.concat(h).concat(",");
            }
            res.withHeader("Access-Control-Allow-Origin", "*");
            res.withHeader("Access-Control-Allow-Credentials", "true");
            res.withHeader("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
            res.withHeader("Access-Control-Allow-Headers", allowedHeaders);

            //--
            //-- End CORS Header Setup

            if (req.isMethod("options")) {
                //this is a CORS preflight request. All of the work was done above
                res.withStatus(Status.SC_200_OK);
                return chain;
            }

            Url url = req.getUrl();

            if (url.toString().contains("/favicon.ico")) {
                res.withStatus(Status.SC_404_NOT_FOUND);
                return chain;
            }

            String xfp = req.getHeader("X-Forwarded-Proto");
            String xfh = req.getHeader("X-Forwarded-Host");
            if (xfp != null || xfh != null) {
                if (xfp != null)
                    url.withProtocol(xfp);

                if (xfh != null)
                    url.withHost(xfh);
            }


            //-- remove any RQL terms that functions with leading "_" as these are internal/restricted
            if (Chain.getDepth() < 2) {
                Map<String, String> urlParams = req.getUrl().getParams();
                for (String key : urlParams.keySet()) {

                    if (key.indexOf("_") > 0) {
                        List<Term> illegals = RqlParser.parse(key, urlParams.get(key)).stream().filter(t -> !t.isLeaf() && t.getToken().startsWith("_")).collect(Collectors.toList());
                        if (illegals.size() > 0) {
                            req.getUrl().clearParams(key);
                        }
                    }
                }
            }

            String method = req.getMethod();

            Path                parts      = new Path(url.getPath());
            Map<String, String> pathParams = new HashMap<>();

            Path containerPath = match(method, parts);

            if (containerPath == null)
                throw ApiException.new400BadRequest("Somehow a request was routed to your Engine with an unsupported containerPath. This is a configuration error.");

            if (containerPath != null)
                containerPath.extract(pathParams, parts);

            Path afterApiPath      = null;
            Path afterEndpointPath = null;

            for (Api api : apis) {
                Path apiPath = api.match(method, parts);

                if (apiPath != null) {
                    apiPath = apiPath.extract(pathParams, parts);
                    req.withApi(api, apiPath);

                    afterApiPath = new Path(parts);

                    for (Endpoint endpoint : api.getEndpoints()) {
                        //-- endpoints marked as internal can not be directly called by external
                        //-- clients, they can only be called by a recursive call to Engine.service
                        if (Chain.getDepth() < 2 && endpoint.isInternal())
                            continue;

                        Path endpointPath = endpoint.match(req.getMethod(), parts);

                        if (endpointPath != null) {
                            endpointPath = endpointPath.extract(pathParams, parts);
                            req.withEndpoint(endpoint, endpointPath);

                            afterEndpointPath = new Path(parts);

                            for (Collection collection : api.getCollections()) {
                                Db db = collection.getDb();
                                if (db != null && db.getEndpointPath() != null && !db.getEndpointPath().matches(endpointPath))
                                    continue;

                                Path collectionPath = collection.match(method, parts);
                                if (collectionPath != null) {
                                    collectionPath = collectionPath.extract(pathParams, parts, true);
                                    req.withCollection(collection, collectionPath);

                                    if (db != null && db.getEndpointPath() != null)
                                        db.getEndpointPath().extract(pathParams, afterApiPath, true);

                                    break;
                                }
                            }
                            break;
                        }
                    }
                    break;
                }
            }

            applyPathParams(pathParams, url, req.getJson());

            //---------------------------------

            if (req.getEndpoint() == null || req.isDebug()) {
                res.debug("");
                res.debug("");
                res.debug(">> request --------------");
                res.debug(req.getMethod() + ": " + url);

                ArrayListValuedHashMap<String, String> headers = req.getHeaders();
                for (String key : headers.keys()) {
                    res.debug(key + " " + Utils.implode(",", headers.get(key)));
                }
                res.debug("");
            }

            if (req.getApi() == null) {
                throw ApiException.new400BadRequest("No API found matching URL: '{}'", url);
            }

            if (req.getEndpoint() == null) {
                StringBuilder buff = new StringBuilder();
                for (Endpoint e : req.getApi().getEndpoints()) {
                    if (!e.isInternal())
                        buff.append(e.toString()).append(" | ");
                }

                throw ApiException.new404NotFound("No Endpoint found matching '{}:{}' Valid endpoints are: {}", req.getMethod(), url, buff.toString());
            }

            //this will get all actions specifically configured on the endpoint
            List<ActionMatch> actions = new ArrayList<>();

            for (Action action : req.getEndpoint().getActions()) {
                Path actionPath = action.match(method, afterEndpointPath);
                if (actionPath != null) {
                    actions.add(new ActionMatch(actionPath, new Path(afterEndpointPath), action));
                }
            }

            //this matches for actions that can run across multiple endpoints.
            //this might be something like an authorization or logging action
            //that acts like a filter
            for (Action action : req.getApi().getActions()) {
                Path actionPath = action.match(method, afterApiPath);
                if (actionPath != null) {
                    actions.add(new ActionMatch(actionPath, new Path(afterApiPath), action));
                }
            }

            if (actions.size() == 0)
                throw ApiException.new404NotFound("No Actions are configured to handle your request.  Check your server configuration.");

            Collections.sort(actions);

            //-- appends info to chain.debug that can be used for debugging an d
            //-- for test cases to validate what actually ran
            if (req.isDebug()) {
                Chain.debug("Endpoint: " + req.getEndpoint());
                Chain.debug("Actions: " + actions);
            }

            run(chain, actions);

            Exception listenerEx = null;
            for (ApiListener listener : getApiListeners(req)) {
                try {
                    listener.afterRequest(req, res);
                } catch (Exception ex) {
                    if (listenerEx == null)
                        listenerEx = ex;
                }
            }
            if (listenerEx != null)
                throw listenerEx;

            return chain;
        } catch (Throwable ex) {
            String status = Status.SC_500_INTERNAL_SERVER_ERROR;

            if (ex instanceof ApiException) {
                if (req != null && req.isDebug() && ((ApiException) ex).getStatus().startsWith("5")) {
                    log.error("Error in Engine", ex);
                }

                status = ((ApiException) ex).getStatus();
                //            if (Status.SC_404_NOT_FOUND.equals(status))
                //            {
                //               //an endpoint could have match the url "such as GET * but then not
                //               //known what to do with the URL because the collection was not pluralized
                //               if (redirectPlural(req, res))
                //                  return chain;
                //            }
            } else {
                ex = Utils.getCause(ex);
                if (Chain.getDepth() == 1)
                    log.error("Non ApiException was caught in Engine.", ex);
            }

            res.withStatus(status);
            String message  = ex.getMessage();
            JSNode response = new JSNode("message", message);
            if (Status.SC_500_INTERNAL_SERVER_ERROR.equals(status))
                response.put("error", Utils.getShortCause(ex));

            res.withError(ex);
            res.withJson(response);

            for (ApiListener listener : getApiListeners(req)) {
                try {
                    listener.afterError(req, res);
                } catch (Exception ex2) {
                    log.warn("Error notifying EngineListener.beforeError", ex);
                }

            }

        } finally {
            for (ApiListener listener : getApiListeners(req)) {
                try {
                    listener.beforeFinally(req, res);
                } catch (Exception ex) {
                    log.warn("Error notifying EngineListener.onFinally", ex);
                }
            }

            try {
                writeResponse(req, res);
            } catch (Throwable ex) {
                log.error("Error writing response.", ex);
            }

            if (chain != null)
                Chain.pop();

            lastResponse = res;
        }

        return chain;
    }

    /**
     * This is specifically pulled out so you can mock Engine invocations
     * in test cases.
     *
     * @param chain   the Chain of the running Request
     * @param actions the Actions that should be run to service the Request
     * @throws ApiException when something goes wrong
     */
    void run(Chain chain, List<ActionMatch> actions) throws ApiException {
        chain.withActions(actions).go();
    }

    void writeResponse(Request req, Response res) throws ApiException {
        boolean debug   = req != null && req.isDebug();
        boolean explain = req != null && req.isExplain();

        String method = req != null ? req.getMethod() : null;

        if (!"OPTIONS".equals(method)) {
            if (debug) {
                res.debug("\r\n<< response -------------\r\n");
                res.debug(res.getStatusCode() + "");
            }

            if (!Utils.empty(res.getRedirect())) {
                res.withHeader("Location", res.getRedirect());
                res.withStatus(Status.SC_308_PERMANENT_REDIRECT);
            } else {
                String output      = res.getContent();
                String contentType = res.getContentType();
                if (output != null && contentType == null) {
                    if (res.getJson() != null)
                        contentType = "application/json";
                    else if (output.contains("<html"))
                        contentType = "text/html";
                    else
                        contentType = "text/text";

                    res.withContentType(contentType);
                }
                res.out(output);
            }

            if (debug) {
                for (String key : res.getHeaders().keySet()) {
                    List          values = res.getHeaders().get(key);
                    StringBuilder buff   = new StringBuilder();
                    for (int i = 0; i < values.size(); i++) {
                        buff.append(values.get(i));
                        if (i < values.size() - 1)
                            buff.append(",");
                    }
                    res.debug(key + " " + buff);
                }

                res.debug("\r\n-- done -----------------\r\n");
            }

            if (explain) {
                res.withOutput(res.getDebug());
            }
        }
    }

    public boolean isStarted() {
        return started;
    }

    /**
     * Registers <code>listener</code> to receive Engine, Api, request and error callbacks.
     *
     * @param listener the listener to add
     * @return this
     */
    public Engine withEngineListener(EngineListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
        return this;
    }

    LinkedHashSet<ApiListener> getApiListeners(Request req) {
        LinkedHashSet<ApiListener> listeners = new LinkedHashSet<>();
        if (req.getApi() != null) {
            listeners.addAll(req.getApi().getApiListeners());
        }
        listeners.addAll(this.listeners);

        return listeners;
    }

    public List<Api> getApis() {
        return new ArrayList<>(apis);
    }

    public synchronized Api getApi(String apiName) {
        //only one api will have a name version pair so return the first one.
        for (Api api : apis) {
            if (apiName.equalsIgnoreCase(api.getName()))
                return api;

        }
        return null;
    }

    public synchronized Engine withApi(Api api) {
        if (apis.contains(api))
            return this;

        List<Api> newList = new ArrayList<>(apis);

        Api existingApi = getApi(api.getName());
        if (existingApi != null && existingApi != api) {
            newList.remove(existingApi);
            newList.add(api);
        } else if (existingApi == null) {
            newList.add(api);
        }

        if (existingApi != api && isStarted())
            api.startup();

        apis = newList;

        if (existingApi != null && existingApi != api) {
            existingApi.shutdown();
        }

        return this;
    }

    protected void startupApi(Api api) {
        if (started) {
            try {
                api.startup();
            } catch (Exception ex) {
                log.warn("Error starting api '" + api.getName() + "'", ex);
            }

            for (EngineListener listener : listeners) {
                try {
                    listener.onStartup(api);
                } catch (Exception ex) {
                    log.warn("Error starting api '" + api.getName() + "'", ex);
                }
            }
        }
    }

    /**
     * Removes the api, notifies EngineListeners and calls api.shutdown().
     *
     * @param api the api to be removed
     */
    public synchronized void removeApi(Api api) {
        List<Api> newList = new ArrayList<>(apis);
        newList.remove(api);
        apis = newList;
        shutdownApi(api);
    }

    protected void shutdownApi(Api api) {
        if (api.isStarted()) {
            try {
                api.shutdown();
            } catch (Exception ex) {
                log.warn("Error shutting down api '" + api.getName() + "'", ex);
            }

            for (EngineListener listener : listeners) {
                try {
                    listener.onShutdown(api);
                } catch (Exception ex) {
                    log.warn("Error shutting down api '" + api.getName() + "'", ex);
                }
            }

        }
    }

    public Engine withAllowHeaders(String allowHeaders) {
        this.corsAllowHeaders = allowHeaders;
        return this;
    }

    /**
     * @return the last response serviced by this Engine.
     */
    public Response getLastResponse() {
        return lastResponse;
    }

    public URL getResource(String name) {
        try {
            URL url = getClass().getClassLoader().getResource(name);
            if (url == null) {
                File file = new File(System.getProperty("user.dir"), name);
                if (file.exists())
                    url = file.toURI().toURL();
            }

            return url;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public String getConfigPath() {
        return configPath;
    }

    public Engine withConfigPath(String configPath) {
        this.configPath = configPath;
        return this;
    }

    public String getConfigProfile() {
        return configProfile;
    }

    public Engine withConfigProfile(String configProfile) {
        this.configProfile = configProfile;
        return this;
    }

    /**
     * Receives {@code Engine} and {@code Api} lifecycle,
     * per request and per error callback notifications.
     */
    public interface EngineListener extends ApiListener {

        /**
         * Notified when the Engine is starting prior to accepting
         * any requests which allows listeners to perform additional configuration.
         *
         * @param engine the Engine starting
         */
        default void onStartup(Engine engine) {
            //implement me
        }

        /**
         * Notified when the Engine is shutting down and has stopped receiving requests
         * allowing listeners to perform any resource cleanup.
         *
         * @param engine the Engine stopping
         */
        default void onShutdown(Engine engine) {
            //implement me
        }
    }

}