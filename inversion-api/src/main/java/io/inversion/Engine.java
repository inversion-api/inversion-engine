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
import io.inversion.action.db.DbAction;
import io.inversion.config.Codec;
import io.inversion.config.Config;
import io.inversion.config.Context;
import io.inversion.config.InversionNamer;
import io.inversion.rql.RqlParser;
import io.inversion.rql.Term;
import io.inversion.utils.*;
import ioi.inversion.utils.Utils;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.nio.file.PathMatcher;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Matches inbound Request Url paths to an Api Endpoint and executes associated Actions.
 */
public class Engine {

    static {
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("ROOT");
        logger.setLevel(Level.WARN);
    }

    protected final transient Logger log = LoggerFactory.getLogger(getClass().getName());

    final String name = "engine";

    /**
     * Optional override for the configPath sys/env prop used by Config to locate configuration property files
     *
     * @see Config#loadConfiguration(Object, String, String)
     */
    transient protected          String  configPath       = null;
    /**
     * Optional override for the sys/env prop used by Config to determine which profile specific configuration property files to load
     *
     * @see Config#loadConfiguration(Object, String, String)
     */
    transient protected          String  configProfile    = null;

    transient protected Context context = null;


    /**
     * Listeners that will receive Engine and Api lifecycle, request, and error callbacks.
     */
    protected final transient    List<EngineListener> listeners    = new ArrayList<>();
    /**
     * The last {@code Response} served by this Engine, primarily used for writing test cases.
     */
    protected transient volatile Response             lastResponse = null;
    /**
     * The {@code Api}s being service by this Engine
     */
    protected                    List<Api>            apis         = new Vector<>();

    protected final List<Action> filters = new ArrayList();

    /**
     * Base value for the CORS "Access-Control-Allow-Headers" response header.
     * <p>
     * Values from the request "Access-Control-Request-Header" header are concatenated
     * to this resulting in the final value of "Access-Control-Allow-Headers" sent in the response.
     * <p>
     * Unless you are really doing something specific with browser security you probably won't need to customize this list.
     */
    protected          String  corsAllowHeaders = "accept,accept-encoding,accept-language,access-control-request-headers,access-control-request-method,authorization,connection,content-type,host,user-agent,x-auth-token";

    transient volatile boolean started          = false;
    transient volatile boolean starting         = false;

    public Engine() {
        //System.out.println("Engine<>");
    }

    public Engine(Api... apis) {
        if (apis != null)
            for (Api api : apis)
                withApi(api);
    }

    /**
     * Convenient pre-startup hook for subclasses guaranteed to only be called once.
     * <p>
     * Called after <code>starting</code> has been set to true but before the {@code Wirer} is run or  any {@code Api}s have been started.
     */
    protected void startup0() {
        //implement me
    }

    /**
     * Runs the {@code Wirer} and calls <code>startupApi</code> for each Api.
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
        long start = System.currentTimeMillis();

        starting = true;
        try {
            startup0();

            if (!Config.hasConfiguration())
                Config.loadConfiguration(this, getConfigPath(), getConfigProfile());

            if(context == null){
                context = new Context();
                context.withNamer(new InversionNamer());

                context.withCodec(new Codec(Path.class){
                    @Override
                    public Object fromString(Class clazz, String encoded) {
                        return new Path(encoded);
                    }
                });
                context.withCodec(new Codec(JSNode.class){
                    @Override
                    public String toString(Object jsNode){
                        return ((JSNode)jsNode).toString(false);
                    }
                    @Override
                    public Object fromString(Class clazz, String encoded) {
                        return JSNode.parseJson(encoded);
                    }
                });
                context.withCodec(new Codec(Rule.RuleMatcher.class){
                    @Override
                    public Object fromString(Class clazz, String encoded) { return new Rule.RuleMatcher(encoded); }
                });

                Map<String, String> properties = Config.getProperties();
                Map<String, String> firstPassApplied = context.wire(properties, this);

                autowire(context);

                //-- remove props that were previously applied so configed classes don't get re-instantiated etc.
                //properties.entrySet().removeIf(entry -> firstPassApplied.containsKey(entry.getKey()));
                properties.entrySet().removeIf(entry -> entry.getKey().toLowerCase().endsWith(".class") || entry.getKey().toLowerCase().endsWith(".classname"));
                Map<String, String> secondPassApplied = context.wire(properties, this);
            }

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


            System.out.println("...ENGINE STARTED IN: " + (System.currentTimeMillis() - start) + "ms");
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
            shutdownApi(api);
        }

        for (EngineListener listener : listeners) {
            try {
                listener.onShutdown(this);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        started = false;
        starting = false;

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

            Url url = req.getUrl();

            if (req.isMethod("options")) {
                //this is a CORS preflight request. All of the work was done above
                res.withStatus(Status.SC_200_OK);
                return chain;
            }

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

            for (Action filter : filters) {
                Path path = req.getUrl().getPath().copy();
                Path match = filter.match(req.getMethod(), path);
                if (match != null) {
                    chain.withAction(new ActionMatch(match, path, filter));
                }
            }

            final Chain finalChain = chain;
            chain.withAction(new ActionMatch(null, null, new Action() {
                @Override
                public void run(Request req, Response res) throws ApiException {
                    service0(finalChain, req, res);
                }
            }));
            //-- causes filters to run then the above anon action the runs the rest of the match/serve process after the filters run.
            chain.go();

            Exception listenerEx = null;
            for (ApiListener listener : getApiListeners(req)) {
                try {
                    listener.onAfterRequest(req, res);
                } catch (Exception ex) {
                    if (listenerEx == null)
                        listenerEx = ex;
                }
            }
            if (listenerEx != null)
                throw listenerEx;

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
                    listener.onAfterError(req, res);
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
                        listener.onBeforeFinally(req, res);
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

    void service0(Chain chain, Request req, Response res) throws ApiException {

        Url url = req.getUrl();
        if (!matchRequest(req)) {
            String requestUrl = req.getUrl().getOriginal();
            throw ApiException.new400BadRequest("No API or Endpoint was found matching your request '{}':'{}'", req.getMethod(), requestUrl);
        }

        if (req.isDebug()) {
            res.debug("");
            res.debug("");
            res.debug(">> request --------------");
            res.debug(req.getMethod() + ": " + url);
            res.debug("OPERATION: " + req.getOp());

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


    public boolean matchApi(Request req){

        if(req.getApi() != null)
            return true;

        Path reqPath = req.getUrl().getPath();
        Path                remainder       = reqPath.copy();
        Path                serverPathMatch = null;
        Path                serverPath      = null;
        Server              server          = null;
        Api                 api             = null;
        Map<String, String> pathParams = new HashMap();

        for (Api a : getApis()) {
            for (Server serv : a.getServers()) {
                serverPathMatch = serv.match(req.getMethod(), req.getUrl());
                if (serverPathMatch == null)
                    continue;
                server = serv;
                api = a;
                serverPath = serverPathMatch.extract(pathParams, remainder);
                break;
            }
            if (api != null) {
                break;
            }
        }
        if(api != null && server != null){
            req.withApi(api);
            req.withServer(server);
            req.withServerPath(serverPath);
            req.withPathParams(pathParams);
            req.withOperationPath(remainder);
            return true;
        }
        else{
            return false;
        }
    }

    boolean matchRequest(Request req) {

        if(!matchApi(req))
            return false;

        Api api = req.getApi();
        if(api == null)
            return false;

        Map<String, String> pathParams = new HashMap<>();
        Path remainder = req.getOperationPath().copy();

        if (api != null) {
            for (Op op : api.getOps()) {
                if (op.matches(req, remainder)) {
                    //TODO: need to revalidate for exclude rules...or remove the concept
                    req.withOp(op);
                    req.withEndpoint(op.getEndpoint());
                    req.withDb(op.getDb());
                    req.withCollection(op.getCollection());

                    Path dbPath         = op.getDbPathMatch() != null ? op.getDbPathMatch().extract(pathParams, remainder.copy()) : null;
                    Path endpointPath   = op.getEndpointPathMatch().extract(pathParams, remainder);
                    Path collectionPath = op.getCollectionPathMatch() != null ? op.getCollectionPathMatch().extract(pathParams, remainder.copy()) : null;

                    req.withEndpointPath(endpointPath);
                    req.withActionPath(remainder);
                    req.withDbPath(dbPath);
                    req.withCollectionPath(collectionPath);
                    req.withPathParams(pathParams);

//                pathParams.clear();
//                for(Parameter param : op.getParameters()){
//                    if(param.getIn().equalsIgnoreCase("path")){
//                        String name = param.getKey();
//                        String value = reqPath.get(param.getIndex());
//                        pathParams.put(name, value);
//                    }
//                }


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

                    System.out.println("SELECTING OPERATION: " + op.getMethod() + " " + op.getPath());

                    return true;
                }
            }
        }
        return false;
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

        if (api.isStarted() && api.getEngine() != null)
            api.getEngine().removeApi(api);

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

    public Context getContext() {
        return context;
    }

    public Engine withContext(Context context) {
        this.context = context;
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

    public Engine withFilters(Action... filters) {
        for (Action filter : filters) {
            if(filters != null) {
                if (!this.filters.contains(filter)) {
                    this.filters.add(filter);
                }
            }
        }
        Collections.sort(this.filters);
        return this;
    }

    public List<Action> getFilters(){
        return Collections.unmodifiableList(filters);
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

    protected void autowire(Context context) {
        //-- SHORTCUT BOOTSTRAPPING
        //--
        //--
        //-- this is a shortcut bootstrapping options for
        //-- apis configured primarily through configuration
        if (context.getBeans(Api.class).size() == 0) {
            Api api = new Api();
            context.putBean("api", api);
        }

        //-- assign all Apis to the engine
        for (Api api : context.getBeans(Api.class)) {
            if (!getApis().contains(api))
                withApi(api);
        }

        //-- if you have a single Api, you don't have to explicitly assign endpoints/dbs/actions to the Api
        Api singleApi = getApis().size() == 1 ? getApis().get(0) : null;
        if (singleApi != null) {

            //-- assign all dbs to single api
            for (Db db : context.getBeans(Db.class)) {
                singleApi.withDb(db);
            }

            //-- assign all endpoints to the single Api
            for (Endpoint ep : context.getBeans(Endpoint.class)) {
                singleApi.withEndpoint(ep);
            }

            //-- make sure the Api has an endpoint
            if (singleApi.getEndpoints().size() == 0) {
                Endpoint ep = new Endpoint().withName("endpoint");
                singleApi.withEndpoint(ep);
                context.putBean("endpoint", ep);
            }

            //-- assign all unassigned actions to the endpoint if there is one endpoint
            //-- or to the Api if there are multiple endpoints
            boolean        assignToEndpoint = false;
            List<Endpoint> endpoints        = context.getBeans(Endpoint.class);
            if (endpoints.size() == 1 && endpoints.get(0).getActions().size() == 0)
                assignToEndpoint = true;

            for (Action action : context.getBeans(Action.class)) {
                boolean assigned = false;

                for (Endpoint ep : singleApi.getEndpoints()) {
                    if (ep.getActions().contains(action)) {
                        assigned = true;
                        break;
                    }
                }

                if (!assigned && getFilters().contains(action))
                    assigned = true;

                if (!assigned && singleApi.getActions().contains(action))
                    assigned = true;

                if (!assigned) {
                    if (assignToEndpoint)
                        endpoints.get(0).withAction(action);
                    else
                        singleApi.withAction(action);
                }
            }
        }

        //-- AFTER potentially assigning unassigned objects to single api above
        //--  1. make sure every api has a server
        //--  2. again, make sure every Api has an endpoint
        //--  3. add a DbAction if there are no other actions
        for (Api api : getApis()) {

            if (api.getServers().size() == 0)
                api.withServer(new Server());

            //-- give all APIs a default endpoint if they don't have one
            if (api.getEndpoints().size() == 0) {
                Endpoint ep = new Endpoint();
                api.withEndpoint(ep);
            }

            if (api.getDbs().size() > 0 && api.getActions().size() == 0) {
                boolean hasAction = false;
                for (Endpoint ep : api.getEndpoints()) {
                    if (ep.getActions().size() > 0) {
                        hasAction = true;
                        break;
                    }
                }
                if (hasAction == false) {
                    Action dbAction = new DbAction();
                    if (api.getEndpoints().size() == 1)
                        api.getEndpoints().get(0).withAction(dbAction);
                    else
                        api.withAction(dbAction);
                }
            }
        }
        //--
        //--
        //--
        //-- END SHORTCUT BOOTSTRAPPING

        //--
        //-- this will cause the Dbs to reflect their data sources and create Collections etc.
        for (Api api : getApis())
            for (Db db : api.getDbs())
                db.startup(api);
    }

}