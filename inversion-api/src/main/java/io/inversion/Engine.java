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

    protected transient List<Operation> operations = new ArrayList();

    public Engine() {
        System.out.println("Engine() <>");
    }

    public Engine(Api... apis) {
        if (apis != null)
            for (Api api : apis)
                withApi(api);
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
            //new Configurator().configure(this, Config.getConfiguration());

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


            this.operations.addAll(buildOperations());

            System.out.println("\r\n--------------------------------------------");
            for (Operation op : this.operations) {
                System.out.println(op.getMethod() + " " + op.getDisplayPath());
            }
            System.out.println("\r\n--------------------------------------------");


//            //-- debug output
//            for (Api api : apis) {
//                System.out.println("\r\n--------------------------------------------");
//                System.out.println("API             " + api);
//
//                List<Operation> operations = api.getOperations();
//                Collections.sort(operations);
//                for (Operation op : operations) {
//
//                    String method = op.method;
//                    while (method.length() < 6)
//                        method += " ";
//
//                    System.out.println(method + " : " + op.name + " - " + op.displayPath + " - " + op.params);
//                }
//
////                for (Endpoint e : api.getEndpoints()) {
////                    System.out.println("  - ENDPOINT:   " + e);
////                }
////
////                List<String> collectionDebugs = new ArrayList<>();
////                for (Collection c : api.getCollections()) {
////                    if (c.getDb() != null && c.getDb().getEndpointPath() != null)
////                        collectionDebugs.add(c.getDb().getEndpointPath() + c.getName());
////                    else
////                        collectionDebugs.add(c.getName());
////                }
////                Collections.sort(collectionDebugs);
////                for (String coll : collectionDebugs) {
////                    System.out.println("  - COLLECTION: " + coll);
////                }
//            }
//            //-- end debug output


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
        if (url == null)
            throw new ApiException("Unable to service request with null url.");
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
        if (res.getRequest() == null)
            res.withRequest(req);
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

            Path urlPath = url.getPath();
            for (int i = 0; i < urlPath.size(); i++) {
                if (urlPath.isVar(i) || urlPath.isWildcard(i))
                    throw ApiException.new400BadRequest("URL {} is malformed.", url);
            }

            if (url.toString().contains("/favicon.ico")) {
                //-- browsers being a pain in the rear
                //throw ApiException.new404NotFound("The requested resource 'favicon.ico' could not be found.", req.getUrl().getOriginal());
                res.withStatus(Status.SC_404_NOT_FOUND);
                res.withJson(null);
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


            if (!matches(req.getMethod(), req.getUrl().getPath())) {
                log.error("A request url {} was incorrectly routed to an Engine that does not support the given context path.  The request will be rejected but this is probably a server configuration error.", url);
                throw ApiException.new400BadRequest("The requested URL has an unsupported context path.");
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

            matchRequest(req);


            if (req.isDebug()) {
                res.debug("");
                res.debug("");
                res.debug(">> request --------------");
                res.debug(req.getMethod() + ": " + url);
                res.debug("OPERATION: " + req.getOperation());

                ArrayListValuedHashMap<String, String> headers = req.getHeaders();
                for (String key : headers.keys()) {
                    res.debug(key + " " + Utils.implode(",", headers.get(key)));
                }
                res.debug("");

                List actionNames = new ArrayList();
                for (ActionMatch am : req.getActionMatches()) {
                    String name = am.action.getName();
                    if (name == null)
                        name = am.action.getClass().getSimpleName();
                    actionNames.add(name);
                }
                String msg = req.getMethod() + " " + url.getPath() + " [" + Utils.implode(",", actionNames) + "]";
                Chain.debug(msg);
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

                String orig = url.getOriginal();
                System.out.println(orig);


                throw ApiException.new404NotFound("No Endpoint found matching '{}:{}' Valid endpoints are: {}", req.getMethod(), url.getOriginal(), buff.toString());
            }

            List<ActionMatch> actions = req.getActionMatches();
            if (actions.size() == 0)
                throw ApiException.new404NotFound("No Actions are configured to handle your request.  Check your server configuration.");

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

            if (req.isDebug())
                ex.printStackTrace();

            Chain.debug("Uncaught Exception: " + Utils.getShortCause(ex));

            JSNode json = buildErrorJson(ex);
            res.withStatus(json.getString("status"));
            res.withError(ex);
            res.withJson(json);

            for (ApiListener listener : getApiListeners(req)) {
                try {
                    listener.afterError(req, res);
                } catch (Exception ex2) {
                    log.warn("Error notifying EngineListener.beforeError", ex);
                }
            }

        } finally {

            if (Chain.isRoot()) {
                exclude(req, res);
            }

            try {
                for (ApiListener listener : getApiListeners(req)) {
                    try {
                        listener.beforeFinally(req, res);
                    } catch (Exception ex) {
                        log.warn("Error notifying EngineListener.onFinally", ex);
                    }
                }
            } finally {
                if (chain != null)
                    Chain.pop();

                lastResponse = res;
            }
        }

        return chain;
    }

    List<Operation> getOperations() {
        return operations;
    }

    public void matchRequest(Request req) {

        Path                reqPath    = req.getUrl().getPath();
        Map<String, String> pathParams = new HashMap<>();


        for (Operation op : operations) {

            if (op.matches(req)) {
                req.withOperation(op);

                for (Parameter param : op.getParams()) {
                    if ("path".equalsIgnoreCase(param.getIn())) {
                        String name  = param.getKey();
                        String value = reqPath.get(param.getIndex());
                        pathParams.put(name, value);
                    }
                }
                req.withPathParams(pathParams);

                String method  = req.getMethod();
                Path   path    = req.getPath();
                Path   subpath = req.getSubpath();

                //this will get all actions specifically configured on the endpoint
                List<ActionMatch> actions = new ArrayList<>();

                for (Action action : req.getEndpoint().getActions()) {
                    Path actionPath = action.match(method, subpath);
                    if (actionPath != null) {
                        actions.add(new ActionMatch(actionPath, new Path(subpath), action));
                    }
                }

                //this matches for actions that can run across multiple endpoints.
                //this might be something like an authorization or logging action
                //that acts like a filter
                for (Action action : req.getApi().getActions()) {
                    Path actionPath = action.match(method, path);
                    if (actionPath != null) {
                        actions.add(new ActionMatch(actionPath, new Path(path), action));
                    }
                }

                Collections.sort(actions);
                req.withActionMatches(actions);

                return;
            }
        }

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

    public static JSNode buildErrorJson(Throwable ex) {
        String status  = "500";
        String message = "Internal Server Error";
        String error   = null;
        if (ex instanceof ApiException) {
            ApiException apiEx = ((ApiException) ex);
            status = apiEx.getStatus();
            message = status.substring(4, status.length());
            status = status.substring(0, 3);

            if ("500".equals(status)) {
                error = ex.getMessage() + "\r\n" + Utils.getShortCause(ex);
            } else {
                error = apiEx.getMessage();
                if (error != null && error.indexOf(" - ") < 20)
                    error = error.substring(error.indexOf(" - ") + 3);
            }
        }

        if (error == null && "500".equalsIgnoreCase(status)) {
            error = Utils.getShortCause(ex);
        }

        JSNode json = new JSNode("status", status, "message", message);
        if (error != null)
            json.put("error", error);

        return json;
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
        if (apiName == null)
            return null;
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
            api.startup(this);

        apis = newList;

        if (existingApi != null && existingApi != api) {
            existingApi.shutdown(this);
        }

        return this;
    }

    protected void startupApi(Api api) {
        if (started) {
            try {
                api.startup(this);
            } catch (Exception ex) {
                log.warn("Error starting api '" + api.getName() + "'", ex);
            }

            for (EngineListener listener : listeners) {
                try {
                    listener.onStartup(this, api);
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
                api.shutdown(this);
            } catch (Exception ex) {
                log.warn("Error shutting down api '" + api.getName() + "'", ex);
            }

            for (EngineListener listener : listeners) {
                try {
                    listener.onShutdown(this, api);
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

    protected static void exclude(Request req, Response res) {

        JSArray data = res.getStream();
        if (data == null)
            return;

        Set<String> includes = getXcludesSet(req.getUrl().getParam("include"));
        Set<String> excludes = getXcludesSet(req.getUrl().getParam("exclude"));

        if ((includes != null && includes.size() > 0) || (excludes != null && excludes.size() > 0)) {
            for (JSNode node : data.asNodeList()) {
                exclude(node, includes, excludes, null);
            }
        }
    }

    protected static void exclude(JSNode node, Set<String> includes, Set<String> excludes, String path) {
        for (String key : node.keySet()) {
            String attrPath = (path != null ? (path + "." + key) : key).toLowerCase();

            Object value = node.get(key);
            if (exclude(attrPath, includes, excludes)) {
                node.remove(key);
            } else {
                if (!(value instanceof JSNode))
                    continue;

                if (value instanceof JSArray) {
                    JSArray arr = (JSArray) value;
                    for (int i = 0; i < arr.size(); i++) {
                        if (arr.get(i) instanceof JSNode) {
                            exclude((JSNode) arr.get(i), includes, excludes, attrPath);
                        }
                    }
                } else {
                    exclude((JSNode) value, includes, excludes, attrPath);
                }
            }
        }
    }

    protected static boolean exclude(String path, Set<String> includes, Set<String> excludes) {
        boolean exclude = false;

        if (includes != null && includes.size() > 0)
            if (!find(includes, path, true))
                exclude = true;

        if (excludes != null && excludes.size() > 0)
            if (find(excludes, path, false))
                exclude = true;

        return exclude;
    }

    protected static boolean find(java.util.Collection<String> paths, String path, boolean matchStart) {
        boolean found = false;
        if (paths.contains(path)) {
            found = true;
        } else {
            for (String param : paths) {
                if (matchStart) {
                    if (param.startsWith(path + ".")) {
                        found = true;
                        break;
                    }
                }
                if (Utils.wildcardMatch(param, path))
                    found = true;
            }
        }
        //System.out.println("find(" + paths + ", " + path + ", " + matchStart + ") -> " + found);
        return found;
    }

    static Set getXcludesSet(String str) {
        if (str == null)
            return null;

        LinkedHashSet set = new LinkedHashSet();
        for (String path : Utils.explode(",", str.toLowerCase())) {
            int pipe = path.indexOf('|');
            if (pipe > -1) {
                String prefix = "";
                String props  = path;
                int    dot    = path.indexOf('.');
                if (dot > -1 && dot < pipe) {
                    prefix = path.substring(0, pipe);
                    prefix = prefix.substring(0, prefix.lastIndexOf('.') + 1);
                    props = path.substring(prefix.length());
                }
                for (String prop : Utils.explode("\\|", props)) {
                    set.add(prefix + prop);
                }
            } else {
                set.add(path);
            }
        }
        return set;
    }


    List<Operation> buildOperations() {
        List<Operation>                      operations = new ArrayList<>();
        ArrayListValuedHashMap<String, Path> allPaths   = buildRequestPaths();
        for (String method : allPaths.keySet()) {
            List<Path> requestPaths = allPaths.get(method);
            for (Path requestPath : requestPaths) {
                Operation op = buildOperation(method, requestPath);
                if (op != null)
                    operations.add(op);
            }
        }


        deduplicateOperationNames(operations);

        return operations;
    }

    void deduplicateOperationNames(List<Operation> operations) {
        for(Api api : apis){
            List<Operation> apiOps = new ArrayList<>();
            for(Operation op : operations){
                if(op.getApi() == api)
                    apiOps.add(op);
            }

            ArrayListValuedHashMap<String, Operation> map = new ArrayListValuedHashMap<>();
            apiOps.forEach(op -> map.put(op.getName(), op));

            for (String operationName : map.keySet()) {
                List<Operation> values = map.get(operationName);
                if (values.size() > 1) {
                    for (int i = 0; i < values.size(); i++) {
                        String name = values.get(i).getName() + (i + 1);
                        values.get(i).withName(name);
                    }
                }
            }
        }
    }

    Operation buildOperation(String method, Path requestPath) {

        Path operationPath = requestPath.copy();

        Path engineMatch = this.match(method, requestPath);
        if (engineMatch == null)
            return null;

        int       offset = 0;
        Operation op     = new Operation();
        op.withMethod(method);
        op.withOperationPath(operationPath);
        op.withEnginePathMatch(engineMatch.copy());

        offset = addParams(op, engineMatch, requestPath, offset, true);

        for (Api api : getApis()) {
            Path apiMatch = api.match(method, requestPath);
            if (apiMatch == null)
                continue;

            op.withApi(api);
            op.withApiMatchPath(apiMatch.copy());

            offset = addParams(op, apiMatch, requestPath, offset, true);

            for (Endpoint ep : api.getEndpoints()) {
                Path endpointMatch = ep.match(method, requestPath);
                if (endpointMatch == null)
                    continue;

                op.withEndpoint(ep);
                op.withEpMatchPath(endpointMatch.copy());


                //-- find the db with the most specific (longest) path match
                Db   winnerDb      = null;
                Path winnerDbMatch = null;
                for (Db db : api.getDbs()) {
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
                for (Action action : api.getActions()) {
                    Path actionMatch = action.match(method, requestPath);
                    if (actionMatch != null) {
                        op.withActionMatch(action, actionMatch.copy(), false);
                        addParams(op, actionMatch, requestPath, offset, false);
                    }
                }

                //-- consume the requestPath for Endpoint Actions to match.
                offset = addParams(op, endpointMatch, requestPath, offset, true);

                for (Action action : ep.getActions()) {
                    Path actionMatch = action.match(method, requestPath);
                    if (actionMatch != null) {
                        op.withActionMatch(action, actionMatch.copy(), true);
                        addParams(op, actionMatch, requestPath, offset, false);
                    }
                }

                break;
            }
            break;
        }


        //-- find the collection and relationship if applicable
        Db db = op.getDb();
        if (db != null && op.getCollection() == null) {
            String     collectionKey = op.getPathParam(Request.COLLECTION_KEY);
            Collection collection    = db.getCollection(collectionKey);
            if (collection != null) {
                op.withCollection(collection);
            }
        }
        Collection collection = op.getCollection();
        if (collection != null && op.getRelationship() == null) {
            String relationshipKey = op.getPathParam(Request.RELATIONSHIP_KEY);
            if (relationshipKey != null) {
                Relationship relationship = collection.getRelationship(relationshipKey);
                op.withRelationship(relationship);
            }
        }
        //--

        String name = buildOperationName(op);
        op.withName(name);


        return op;

    }

    protected int addParams(Operation op, Path matchedPath, Path requestPath, int offset, boolean consume) {
        for (int i = 0; i < matchedPath.size() && i < requestPath.size(); i++) {
            if (matchedPath.isVar(i)) {
                String key = matchedPath.getVarName(i);
                op.withParameter(new Parameter(key, i + offset));
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


    public ArrayListValuedHashMap<String, Path> buildRequestPaths() {
        ArrayListValuedHashMap<String, Path> raw      = enumeratePaths();
        ArrayListValuedHashMap<String, Path> filtered = new ArrayListValuedHashMap<String, Path>();
        for (String method : raw.keySet()) {
            List<Path> paths = raw.get(method);
            filtered.putAll(method, paths);
        }
        return filtered;
    }

    ArrayListValuedHashMap<String, Path> enumeratePaths() {
        ArrayListValuedHashMap<String, Path> paths = new ArrayListValuedHashMap<>();
        for (Rule.RuleMatcher engineMatcher : getIncludeMatchers()) {
            for (String method : engineMatcher.getMethods()) {
                for (Path enginePath : engineMatcher.getPaths()) {
                    for (Api api : apis) {
                        for (Rule.RuleMatcher apiMatcher : api.getIncludeMatchers()) {
                            if (!apiMatcher.hasMethod(method))
                                continue;
                            for (Path apiPath : apiMatcher.getPaths()) {
                                Path fullApiPath = Path.joinPaths(enginePath, apiPath);

                                if (fullApiPath == null)
                                    continue;

                                for (Endpoint ep : api.getEndpoints()) {
                                    for (Rule.RuleMatcher epMatcher : ep.getIncludeMatchers()) {
                                        if (!epMatcher.hasMethod(method))
                                            continue;
                                        for (Path epPath : epMatcher.getPaths()) {
                                            Path fullEpPath = Path.joinPaths(fullApiPath, epPath);

                                            if (fullEpPath == null)
                                                continue;

                                            List<Action> epActions  = ep.getActions();
                                            List<Action> allActions = new ArrayList(ep.getActions());
                                            for (Action action : api.actions) {
                                                if (!allActions.contains(action))
                                                    allActions.add(action);
                                            }
                                            Collections.sort(allActions);

                                            List<Path> allPathsForEndpoint = new ArrayList<>();

                                            for (Action action : allActions) {
                                                for (Rule.RuleMatcher actionMatcher : (List<Rule.RuleMatcher>) action.getIncludeMatchers()) {
                                                    if (!actionMatcher.hasMethod(method))
                                                        continue;
                                                    for (Path actionPath : actionMatcher.getPaths()) {

                                                        Path fullActionPath = null;
                                                        if (epActions.contains(action)) {
                                                            fullActionPath = Path.joinPaths(fullEpPath, actionPath);

                                                        } else {
                                                            if (actionPath.matches(epPath)) {
                                                                fullActionPath = Path.joinPaths(fullApiPath, actionPath);
                                                            }

                                                        }
                                                        if (fullActionPath == null)
                                                            continue;

                                                        allPathsForEndpoint.add(fullActionPath);
                                                        //paths.put(method, fullActionPath);
                                                    }
                                                }
                                            }
                                            allPathsForEndpoint = expandOptionalsAndFilterDuplicates(allPathsForEndpoint);
                                            allPathsForEndpoint = Endpoint.mergePaths(new ArrayList(), allPathsForEndpoint);
                                            paths.putAll(method, allPathsForEndpoint);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return paths;
    }

    public static List<Path> expandOptionalsAndFilterDuplicates(List<Path> paths) {
        List<Path> allPaths = new ArrayList();
        for (Path path : paths)
            allPaths.addAll(path.getSubPaths());

        for (int i = 0; i < allPaths.size(); i++) {
            for (int j = i + 1; j < allPaths.size(); j++) {
                Path p1 = allPaths.get(i);
                Path p2 = allPaths.get(j);

                if (p1.size() != p2.size())
                    continue;

                boolean same = true;

                for (int k = 0; same && k < p1.size(); k++) {
                    if (p1.isVar(k) && p2.isVar(k))
                        continue;
                    else if (p1.isWildcard(k) && p2.isWildcard(k))
                        continue;
                    else {
                        String part1 = p1.get(k);
                        String part2 = p2.get(k);
                        if (!part1.equalsIgnoreCase(part2)) {
                            same = false;
                        }
                    }
                }
                if (same) {
                    allPaths.remove(j);
                    j -= 1;
                }
            }
        }

        return allPaths;
    }

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
    /**
     * //        switch (function.toLowerCase()) {
     * //            case "get":
     * //                name = "get" + singular;
     * //                break;
     * //            case "list":
     * //                name = "find" + plural;
     * //                break;
     * //            case "related":
     * //                name = "findRelated" + Utils.capitalize(relationship.getRelated().getPluralDisplayName());
     * //                break;
     * //            case "post":
     * //                name = "create" + singular;
     * //                break;
     * //            case "batch_post":
     * //                name = "createMultiple" + plural;
     * //                break;
     * //            case "put":
     * //                name = "update" + singular;
     * //                break;
     * //            case "batch_put":
     * //                name = "updateMultiple" + plural;
     * //                break;
     * //            case "patch":
     * //                name = "patch" + plural;
     * //                break;
     * //            case "delete":
     * //                name = "delete" + singular;
     * //                break;
     * //            case "batch_delete":
     * //                name = "deleteMultiple" + plural;
     * //                break;
     * //            default:
     * //                throw new ApiException("Unknown function {}", function);
     * //        }
     *
     * @param op
     * @return
     */

    public String buildOperationName(Operation op) {

        String name = null;

        Collection collection = op.getCollection();
        String     epName     = op.getEndpoint().getName();

        String singular = Utils.capitalize(collection == null ? epName : collection.getSingularDisplayName());
        String plural   = Utils.capitalize(collection == null ? epName : collection.getPluralDisplayName());

        String method = op.getMethod().toUpperCase();

        if (op.hasAllParams("path", Request.COLLECTION_KEY, Request.RESOURCE_KEY, Request.RELATIONSHIP_KEY)) {

            switch (method){
                case "GET" :
                    name = "findRelated" + Utils.capitalize(op.getPathParam(Request.RELATIONSHIP_KEY));
                    break;
                case "POST" :
                case "PUT" :
                case "PATCH" :
                case "DELETE" :
                    name = "UNSUPPORTED";
            }

        } else if (op.hasAllParams("path", Request.COLLECTION_KEY, Request.RESOURCE_KEY)) {
            switch (method) {
                case "GET":
                    name = "get" + singular;
                    break;
                case "POST":
                    name = "createIdentified" + singular;
                    break;
                case "PUT":
                    name = "update" + singular;
                    break;
                case "PATCH":
                    name = "patch" + singular;
                    break;
                case "DELETE":
                    name = "delete" + singular;
            }

        } else if (op.hasAllParams("path", Request.COLLECTION_KEY)) {
            switch (method) {
                case "GET":
                    name = "find" + plural;
                    break;
                case "POST":
                    name = "create" + plural;
                    break;
                case "PUT":
                    name = "update" + plural;
                    break;
                case "PATCH":
                    name = "patch" + plural;
                case "DELETE":
                    name = "delete" + plural;
            }
        } else {
            switch (method) {
                case "GET":
                case "POST":
                case "PUT":
                case "PATCH":
                case "DELETE":
                    name = method.toLowerCase();
            }
        }

         for (int i = 0; i < op.operationPath.size(); i++) {
            if (op.getOperationPath().isVar(i))
                name += "By" + Utils.capitalize(op.getOperationPath().getVarName(i));
        }

        return name;
    }




}