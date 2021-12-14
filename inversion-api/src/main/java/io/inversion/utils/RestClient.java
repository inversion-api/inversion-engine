/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
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

package io.inversion.utils;

import io.inversion.*;
import io.inversion.config.Config;
import io.inversion.config.Context;
import ioi.inversion.utils.Utils;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.zip.GZIPOutputStream;

/**
 * An HttpClient wrapper designed specifically to run inside of an Inversion request Chain with some extra superpowers.
 * <p>
 * RestClient gives you easy or built in:
 * <ul>
 *  <li>request response listeners that operate on all requests/responses.  See RestClient.withRequestListener()/.withResponseListener()</li>
 *  <li>fluent asynchronous response handler api on FutureResponse for response specific handling.  See FutureResponse.onResponse()/onSuccess()/onFailure()
 *  <li>header forwarding w/ whitelists and blacklists
 *  <li>url query string param forwarding w/ whitelist and blacklists
 *  <li>lazy runtime host url construction through lookup of "{RestClient.name}.url" in the environment
 *  <li>dynamic host url variables - any "{paramName}" tokens in the host url will be replaced with Chain.peek.getRequest().getUrl().getParam(paramName).
 *  <li>change any request/response in an always thread safe way by overriding <code>doRequest(Request)</code>
 * </ul>
 *
 * <p>
 * <b>Intercepting and transforming requests and responses examples:</b>
 * <pre>
 *
 *      RestClient client = new RestClient().withRequestListener(req -&gt; return new Response())//-- edit the request or return your own response to short circuits the remote call
 *                                          .withResponseListener(res -&gt; res.setStatus(200))//-- edit anything you want about the response
 *
 *      //-- you can also override "doRequest" to control everything before and after the actual HttpClient call.
 *      client = new RestClient("myservice")
 *      {
 *          protected Response doRequest(Request request)
 *          {
 *              if(checkMyCondition(request))
 *              {
 *                  //-- short circuit the remote call "faking" a
 *                  //-- remote 200 response (the default status code for a Response)
 *                  return new Response(request.getUrl());
 *              }
 *              else
 *              {
 *                  doMyRequestTransformation(request);
 *                  Response response = super.doRequest(request);
 *                  doMyResponseTransformation(response);
 *                  return response;
 *              }
 *          }
 *      }
 *
 *      client.get("/some/relative/path")
 *          .onSuccess(response -@gt; System.out.println("Success:" + res.toString()))
 *          .onFailure(response -@gt; System.out.println("Failure:" + res.toString()))
 *          .onResponse(response -@gt; System.out.println(res.getStatus()));
 *
 *
 *      //-- instead of using the success/failure callbacks as above
 *      //-- you can wait for the async process to complete by calling 'get()'
 *
 *      FutureResponse future = client.post("/some/relative/path", new JSNode("hello", "world"));
 *      //-- request is asynchronously executing here
 *
 *      //-- the call to get() blocks indefinitely until the async execution completes
 *      //-- the fact that this method is called 'get()' is not related to HTTP get.
 *      Response response = future.get();
 *
 *
 *      //-- now do whatever you want with the Response
 *
 * </pre>
 */
public class RestClient {

    static final    Log                                    log                   = LogFactory.getLog(RestClient.class);

    /**
     * Headers that are always sent regardless of <code>forwardHeaders</code>, <code>includeForwardHeaders</code> and <code>excludeForwardHeaders</code> state.
     * <p>
     * These headers will overwrite any caller supplied or forwarded header with the same key, not simply appending to the value list.
     * <p>
     * This list is initially empty.
     */
    protected final ArrayListValuedHashMap<String, String> forcedHeaders         = new ArrayListValuedHashMap();

    protected final List<RequestListener>    requestListeners  = new ArrayList<>();

    protected final List<Consumer<Response>> responseListeners = new ArrayList<>();

    /**
     * The RestClient name that will be used for property decoding.
     *
     * @see Context
     */
    protected       String                   name              = null;

    /**
     * Optional base url that will be prepended to the url arg of any calls assuming that the url arg supplied is a relative path and not an absolute url.
     * Any {paramName} variables will be replaced with with values from the current Request Url.
     */
    protected String     url                = null;


    /**
     * Indicates the headers from the root inbound Request being handled on this Chain should be included on this request minus any <code>excludeForwardHeaders</code>.
     * <p>
     * Default value is true.
     */
    protected boolean    forwardHeaders     = true;

    /**
     * Forward these headers when forwardHeaders is true.
     *
     * @see #shouldForwardHeader(String)
     * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_header_fields">HTTP Header Fields</a>
     * @see <a href="https://w3c.github.io/trace-context">W3C Trace Context</a>
     * @see <a href="https://docs.amazonaws.cn/en_us/elasticloadbalancing/latest/application/load-balancer-request-tracing.html">Aws Request Tracing</a>
     * @see <a href="https://docs.microsoft.com/en-us/azure/azure-monitor/app/correlation">Azure Request Correlation</a>
     * @see <a href="https://github.com/dotnet/runtime/blob/master/src/libraries/System.Diagnostics.DiagnosticSource/src/HttpCorrelationProtocol.md">MS Http Corolation Protocol - Deprecated</a>
     */
    protected final Set                                    includeForwardHeaders = Utils.add(new TreeSet<String>(String.CASE_INSENSITIVE_ORDER)//
            , "authorization", "cookie" //-- can't login to downstream services if you don't forward the authorization
            , "x-forwarded-for", "x-forwarded-host", " x-forwarded-proto" //-- these are generic for servers hosted behind a reverse proxy/load balancer etc.
            , "x-request-id", "x-correlation-id" //-- common but not standard
            , "traceparent" //https://w3c.github.io/trace-context/
            , "request-id", "trace-id"  //deprecated but from correlation HTTP protocol, also called Request-Id standard: https://github.com/dotnet/runtime/blob/master/src/libraries/System.Diagnostics.DiagnosticSource/src/HttpCorrelationProtocol.md
            , "x-ms-request-id", "x-ms-request-root-id", "request-context"//-- these are usefull to azure app insights
            , "X-Amzn-Trace-Id" //-- for aws tracing @see https://docs.amazonaws.cn/en_us/elasticloadbalancing/latest/application/load-balancer-request-tracing.html
    );
    /**
     * Never forward these headers.
     *
     * @see #shouldForwardHeader(String)
     */
    protected final Set                                    excludeForwardHeaders = Utils.add(new TreeSet<String>(String.CASE_INSENSITIVE_ORDER) //
            , "content-length", "content-type", "content-encoding", "content-language", "content-location", "content-md5", "host");

    /**
     * Indicates the params from the root inbound Request being handled on this Chain should be included on this request minus any <code>excludeParams</code>.
     * <p>
     * Default value is false.
     */
    protected boolean    forwardParams      = false;

    /**
     * Forward these params when forwardParams is true.
     *
     * @see #shouldForwardParam(String)
     */
    protected final Set<String>                            includeParams         = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

    /**
     * Never forward these params.  Contains ["explain"] by default.
     *
     * @see #shouldForwardParam(String)
     */
    protected final Set<String>              excludeParams     = Utils.add(new TreeSet<String>(String.CASE_INSENSITIVE_ORDER), "explain");

    /**
     * Indicates that a request body should be gzipped and the content-encoding header should be sent with value "gzip".
     * <p>
     * Default value is true.
     */
    protected boolean    useCompression     = true;

    /**
     * If <code>useCompression</code> is true, anything over this size in bytes will be compressed.
     * <p>
     * Default value is 1024.
     */
    protected int        compressionMinSize = 1024;

    /**
     * Responses over this size will be written to a temp file that will be deleted
     * when the Response inputStream is closed (or Response is finalized which closes the stream)
     */
    protected long maxMemoryBuffer = 100 * 1024;


    /**
     * The thread pool executor used to make asynchronous requests.  The Executor will expand to
     * <code>threadsMax</code> worker threads.
     */
    protected Executor   executor           = null;

    /**
     * The number of background executor threads.
     * <p>
     * A value &lt; 1 will cause all tasks to execute synchronously on the calling thread meaning
     * the FutureResponse will always be complete upon return.
     * <p>
     * The default value is 0.
     */
    protected int threadsMax = 5;

    /**
     * Parameter for default HttpClient configuration
     * <p>
     * Default value is 30000ms
     *
     * @see org.apache.http.client.config.RequestConfig.Builder#setSocketTimeout(int)
     */
    protected int socketTimeout = 30000;

    /**
     * Parameter for default HttpClient configuration
     * <p>
     * Default value is 30000ms
     *
     * @see org.apache.http.client.config.RequestConfig.Builder#setConnectTimeout(int)
     */
    protected int connectTimeout = 30000;

    /**
     * Parameter for default HttpClient configuration
     * <p>
     * Default value is 30000ms
     *
     * @see org.apache.http.client.config.RequestConfig.Builder#setConnectionRequestTimeout(int)
     */
    protected int connectionRequestTimeout = 30000;


    /**
     * Parameter for default HttpClient configuration
     * <p>
     * Default value is 10
     *
     * @see org.apache.http.impl.client.HttpClientBuilder#setMaxConnPerRoute(int)
     */
    public int maxConPerRoute = 10;

    /**
     * Parameter for default HttpClient configuration
     * <p>
     * Default value is 50ms
     *
     * @see org.apache.http.impl.client.HttpClientBuilder#setMaxConnTotal(int)
     */
    public int maxConTotal = 50;


    /**
     * Parameter for default HttpClient configuration
     *
     * @see org.apache.http.impl.client.HttpClientBuilder#evictExpiredConnections()
     */
    public boolean evictExpiredConnections = true;

    /**
     * Parameter for default HttpClient configuration
     *
     * @see org.apache.http.impl.client.HttpClientBuilder#evictIdleConnections(long, TimeUnit)
     */
    public int evictIdleConnectionsAfterTimeMillis = -1;

    protected HttpClientBuilder httpClientBuilder = null;

    protected HttpClient httpClient         = null;

    public RestClient() {}

    /**
     * @param name the prefix used to look up property values from the environment if they have not already been wired
     */
    public RestClient(String name) {
        this();
        withName(name);
    }

    /**
     * Convenience overloading of {@link #call(String, String, Map, JSNode, ArrayListValuedHashMap)} to perform a GET request.
     *
     * @param fullUrlOrRelativePath may be a full url or relative to the {@link #url} property if set, can have a query string or not
     * @return a FutureResponse that will asynchronously resolve to a Response
     */
    public FutureResponse get(String fullUrlOrRelativePath) {
        return get(fullUrlOrRelativePath, (String) null);
    }

    /**
     * Convenience overloading of {@link #call(String, String, Map, JSNode, ArrayListValuedHashMap)} to perform a GET request.
     *
     * @param fullUrlOrRelativePath may be a full url or relative to the {@link #url} property if set, can have a query string or not
     * @param queryString           additional query string params in name=value@amp;name2=value2 style
     * @return a FutureResponse that will asynchronously resolve to a Response
     */
    public FutureResponse get(String fullUrlOrRelativePath, String queryString) {
        return call("GET", fullUrlOrRelativePath, Utils.parseQueryString(queryString), null, null);
    }

    /**
     * Convenience overloading of {@link #call(String, String, Map, JSNode, ArrayListValuedHashMap)} to perform a GET request.
     *
     * @param fullUrlOrRelativePath may be a full url or relative to the {@link #url} property if set, can have a query string or not
     * @param params                query strings passed in as a map
     * @return a FutureResponse that will asynchronously resolve to a Response
     */
    public FutureResponse get(String fullUrlOrRelativePath, Map<String, String> params) {
        return call("GET", fullUrlOrRelativePath, params, null, null);
    }

    /**
     * Convenience overloading of {@link #call(String, String, Map, JSNode, ArrayListValuedHashMap)} to perform a GET request.
     *
     * @param fullUrlOrRelativePath     may be a full url or relative to the {@link #url} property if set, can have a query string or not
     * @param queryStringNameValuePairs additional query string name/value pairs
     * @return a FutureResponse that will asynchronously resolve to a Response
     */
    public FutureResponse get(String fullUrlOrRelativePath, String... queryStringNameValuePairs) {
        return call("GET", fullUrlOrRelativePath, Utils.addToMap(new HashMap<>(), queryStringNameValuePairs), null, null);
    }

    /**
     * Convenience overloading of {@link #call(String, String, Map, JSNode, ArrayListValuedHashMap)} to perform a POST request.
     *
     * @param fullUrlOrRelativePath may be a full url or relative to the {@link #url} property if set
     * @param body                  the optional JSON to post
     * @return a FutureResponse that will asynchronously resolve to a Response
     */
    public FutureResponse post(String fullUrlOrRelativePath, JSNode body) {
        return call("POST", fullUrlOrRelativePath, null, body, null);
    }

    /**
     * Convenience overloading of {@link #call(String, String, Map, JSNode, ArrayListValuedHashMap)} to perform a PUT request.
     *
     * @param fullUrlOrRelativePath may be a full url or relative to the {@link #url} property if set
     * @param body                  the optional JSON to put
     * @return a FutureResponse that will asynchronously resolve to a Response
     */
    public FutureResponse put(String fullUrlOrRelativePath, JSNode body) {
        return call("PUT", fullUrlOrRelativePath, null, body, null);
    }

    /**
     * Convenience overloading of {@link #call(String, String, Map, JSNode, ArrayListValuedHashMap)} to perform a PATCH request.
     *
     * @param fullUrlOrRelativePath may be a full url or relative to the {@link #url} property if set
     * @param body                  the optional JSON patch
     * @return a FutureResponse that will asynchronously resolve to a Response
     */
    public FutureResponse patch(String fullUrlOrRelativePath, JSNode body) {
        return call("PATCH", fullUrlOrRelativePath, null, body, null);
    }

    /**
     * Convenience overloading of {@link #call(String, String, Map, JSNode, ArrayListValuedHashMap)} to perform a DELETE request.
     *
     * @param fullUrlOrRelativePath may be a full url or relative to the {@link #url} property if set
     * @return a FutureResponse that will asynchronously resolve to a Response
     */
    public FutureResponse delete(String fullUrlOrRelativePath) {
        return call("DELETE", fullUrlOrRelativePath, null, null, null);
    }

    /**
     * Makes an HTTP request.
     *
     * @param method                the HTTP method to invoke
     * @param fullUrlOrRelativePath optional may be a full url or only additional relative path parts if the {@link #url} property if set, may contain a query string
     * @param params                optional additional query string params that will overwrite any that may be on url as composed from {@link #buildUrl(String)}
     * @param body                  optional json body
     * @param headers               headers that will always be sent regardless of {@link #includeForwardHeaders}, {@link #excludeForwardHeaders} but may be overwritten by {@link #forcedHeaders}
     * @return a FutureResponse that will asynchronously resolve to a Response
     */
    public FutureResponse call(String method, String fullUrlOrRelativePath, Map<String, String> params, JSNode body, ArrayListValuedHashMap<String, String> headers) {
        Request request = buildRequest(method, fullUrlOrRelativePath, params, body, headers);
        return call(request);
    }

    /**
     * Executes the Request as provided without modification ignoring forwardHeaders/forwardParams etc.
     * <p>
     * All of the other 'get/post/put/patch/delete/call' methods will use buildRequest() to construct a Request based on the configured
     * properties of this RestClient and optionally the data in Request on the top of the Chain if operating inside an Engine.
     * <p>
     * Those methods ultimately delegate to this method and no further modification of the Request is made from here out.
     *
     * @param request
     * @return
     */
    public FutureResponse call(Request request) {
        FutureResponse future = buildFuture(request);
        if(threadsMax < 1)
            future.run();
        else
            submit(future);
        return future;
    }

    /**
     * Builds a request with the supplied information merged with the url, query param, and header options configured
     * on this reset client and potentially pulled from the Chain.first() root caller request.
     *
     * @param method                - the http method
     * @param fullUrlOrRelativePath - a full url or a relative path that will be appended to this.url
     * @param params                - query params to pass
     * @param body                  - the request body to pass
     * @param headers               - request headers to pass
     * @return the configure request
     */
    public Request buildRequest(String method, String fullUrlOrRelativePath, Map<String, String> params, JSNode body, ArrayListValuedHashMap<String, String> headers) {

        String url         = buildUrl(fullUrlOrRelativePath);
        String queryString = StringUtils.substringAfter(url, "?");
        if (!Utils.empty(queryString)) {
            url = Utils.substringBefore(url, "?");
            Map newParams = Utils.parseQueryString(queryString);
            if (params != null) {
                //-- this makes sure any specifically provided name/value query string pairs
                //-- overwrite the ones from the url which would have been statically configured.
                newParams.putAll(params);
            }
            params = newParams;
        }

        Request request = new Request(method, url, (body == null ? null : body.toString()), params, headers);

        if (forwardHeaders) {
            Chain chain = Chain.first();//gets the root chain
            if (chain != null) {
                Request                                originalInboundRequest = chain.getRequest();
                ArrayListValuedHashMap<String, String> inboundHeaders         = originalInboundRequest.getHeaders();
                if (inboundHeaders != null) {
                    for (String key : inboundHeaders.keySet()) {
                        if (shouldForwardHeader(key)) {
                            if (request.getHeader(key) == null)
                                for (String header : inboundHeaders.get(key))
                                    request.withHeader(key, header);
                        }
                    }
                }
            }
        }

        if (forcedHeaders.size() > 0) {
            for (String key : forcedHeaders.keySet()) {
                request.removeHeader(key);
                for (String value : forcedHeaders.get(key))
                    request.withHeader(key, value);
            }
        }

        if (forwardParams) {
            Chain chain = Chain.first();
            if (chain != null) {
                Request             originalInboundRequest = chain.getRequest();
                Map<String, String> origionalParams        = new Url(originalInboundRequest.getUrl().getOriginal()).getParams();
                if (origionalParams.size() > 0) {
                    for (String key : origionalParams.keySet()) {
                        if (shouldForwardParam(key)) {
                            if (request.getUrl().getParam(key) == null)
                                request.getUrl().withParam(key, origionalParams.get(key));
                        }
                    }
                }
            }
        }

        return request;
    }

    FutureResponse buildFuture(Request request) {
        final FutureResponse future = new FutureResponse(request) {

            public void run() {
                Response response = doRequest(request);
                response.withEndAt(System.currentTimeMillis());
                setResponse(response);
            }
        };

        return future;
    }

    /**
     * The work of executing the remote call is done here.
     * <p>
     * Override this method to intercept the remote call and change anything about the Request or Response that you want.
     * <p>
     * All of the Request url/header/param configuration has already been done on the Request.
     * <p>
     * You don't need to do anything related to threading here.
     * This method is already executing asynchronously within the Executor's thread pool.
     * Simply handle/transform the Request/Response as desired.
     * Simply returning a Response will cause the FutureResponse to transition to done and allow calls blocking on FutureResponse.get() to receive the Response.
     * <p>
     * Overriding this method can be really helpful when you what your RestClient calling algorithm to say clean,
     * hiding some of the Request/Response customization details or otherwise need to make sure Requests/Responses are always handled in a specific way.
     * <p>
     * A typical override of this method might look like the following:
     * <pre>
     * protected RestClient client = new RestClient("myservice"){
     *
     *  protected Response doRequest(Request request)
     *  {
     *      if(checkMyCondition(request))
     *      {
     *          //-- short circuit the remote call "faking" a
     *          //-- remote 200 response (the default status code for a Response)
     *          return new Response(request.getUrl());
     *      }
     *      else
     *      {
     *          doMyRequestTransformation(request);
     *          Response response = super.doRequest(request);
     *          doMyResponseTransformation(response);
     *          return response;
     *      }
     *  }
     * }
     * </pre>
     *
     * @param request the request to make
     * @return a Response containing the server response data or error data.  Will not be null.
     */
    protected Response doRequest(Request request) {
        return doRequest0(request);
    }

    Response doRequest0(Request request) {

        String          m        = request.getMethod();
        HttpRequestBase req      = null;

        String   url      = request.getUrl().toString();
        Response response = new Response(url);
        response.withJson(null);
        response.withRequest(request);

        for (RequestListener l : requestListeners) {
            Response replacementResponse = l.onRequest(request);
            if (replacementResponse != null) {
                if (replacementResponse.getUrl() == null)
                    replacementResponse.withUrl(url);
                if (replacementResponse.getRequest() == null)
                    replacementResponse.withRequest(request);

                return replacementResponse;
            }
        }

        try {
            HttpClient   h = getHttpClient();
            HttpResponse hr;

            response.debug("--request header------");
            response.debug(m + " " + url);

            if ("post".equalsIgnoreCase(m)) {
                req = new HttpPost(url);
            }
            if ("put".equalsIgnoreCase(m)) {
                req = new HttpPut(url);
            } else if ("get".equalsIgnoreCase(m)) {
                req = new HttpGet(url);


            } else if ("delete".equalsIgnoreCase(m)) {
                if (request.getBody() != null) {
                    req = new HttpDeleteWithBody(url);
                } else {
                    req = new HttpDelete(url);
                }
            } else if ("patch".equalsIgnoreCase(m)) {
                req = new HttpPatch(url);
            }

            for (String key : request.getHeaders().keySet()) {
                List<String> values = request.getHeaders().get(key);
                for (String value : values) {
                    req.setHeader(key, value);
                    response.debug(key, value);
                }
            }
            if (request.getBody() != null && req instanceof HttpEntityEnclosingRequestBase) {
                response.debug("\r\n--request body--------");

                byte[] bytes = request.getBody().getBytes(StandardCharsets.UTF_8);

                if (useCompression && bytes.length >= compressionMinSize) {
                    req.setHeader("Content-Encoding", "gzip");
                    ByteArrayOutputStream obj  = new ByteArrayOutputStream();
                    GZIPOutputStream      gzip = new GZIPOutputStream(obj);
                    gzip.write(bytes);
                    gzip.flush();
                    gzip.close();
                    bytes = obj.toByteArray();
                }

                ((HttpEntityEnclosingRequestBase) req).setEntity(new ByteArrayEntity(bytes));
            }

            if (Utils.empty(request.getHeader("Accept-Encoding"))) {
                req.setHeader("Accept-Encoding", "gzip");
            }

            hr = h.execute(req);

            response.withStatusMesg(hr.getStatusLine().toString());
            response.withStatusCode(hr.getStatusLine().getStatusCode());

            response.debug("-response headers -----");
            response.debug("status: " + response.getStatus());
            for (Header header : hr.getAllHeaders()) {
                response.debug("\r\n" + header.getName() + ": " + header.getValue());
                response.withHeader(header.getName(), header.getValue());
            }

            HttpEntity e = hr.getEntity();
            if (e != null) {
                InputStream  is         = e.getContent();
                StreamBuffer tempBuffer = new StreamBuffer();
                tempBuffer.withBufferSize(getMaxMemoryBuffer());
                Utils.pipe(is, tempBuffer);
                response.withStream(tempBuffer);

                long expectedLength = e.getContentLength();
                if(expectedLength > 0 && tempBuffer.getLength() != expectedLength){
                    throw new ApiException("Content-Length header does not match received payload size.");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            response.withError(ex);
            response.withStatus(Status.SC_500_INTERNAL_SERVER_ERROR);
        } finally {
            if (req != null) {
                try {
                    req.releaseConnection();
                } catch (Exception ex) {
                    log.info("Exception trying to release the request connection", ex);
                }
            }
        }

        return response;
    }

    /**
     * Requests listeners can modify the Request.  If they return null, request processing/execution
     * will continue.  If they return a Response, no additional RequestListeners will be notified
     * and the supplied Response will be used instead of actually making the remote response.  In this
     * case all response listeners on this class or the FutureResponse will still be notified.
     * @param requestListener
     * @return
     */
    public RestClient onRequest(RequestListener requestListener) {
        this.requestListeners.add(requestListener);
        return this;
    }

    public RestClient onResponse(Consumer<Response> responseListener) {
        this.responseListeners.add(responseListener);
        return this;
    }

    synchronized void submit(FutureResponse future) {
        getExecutor().submit(future);
    }




    /**
     * Constructs the url for the RestClient to call.
     * <p>
     * The host / base of the url can come from:
     * <ol>
     *   <li><code>callerSuppliedFullUrlOrRelativePath</code> if it starts with 'http://' or 'https://'
     *   <li>a <code>${this.name}.url</code> Config property
     *   <li><code>this.url</code>
     * </ol>
     * <p>
     * After the url is found/concatenated, any ":paramName" tokens found in the string are replaced with any URL variables
     * form the current Inversion request via <code>Chain.top().getRequest().getParam(paramName)</code>.
     * This allows outbound urls to be dynamically constructed based on the inbound url should that use case arise.  This
     * function is a counterpart to Path matching and even allows for optional components.
     * <p>
     * For example: this.url = "http://localhost:8080/api/:_collection/[:_resource]/[:_relationship]
     *
     * @param callerSuppliedFullUrlOrRelativePath string url path
     * @return the url to call
     */
    String buildUrl(String callerSuppliedFullUrlOrRelativePath) {
        String url = callerSuppliedFullUrlOrRelativePath;

        if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
            //do nothing, the caller passed in a full url
        } else {
            url = url != null ? url : "";

            //-- generally "Config.getString(getName() + ".url")" should be redundant because the prop should
            //-- have been set during wiring by the Wirer. This is here so RestClient can be used outside
            //-- of a running Engine...like in test and such...or just generally as a utility.
            String prefix = this.url != null ? this.url : Config.getString(getName() + ".url");
            if (!Utils.empty(prefix)) {
                if (url.length() > 0 && !url.startsWith("/") && !prefix.endsWith("/"))
                    url = prefix + "/" + url;
                else
                    url = prefix + url;
            }
        }

        if (url == null)
            throw ApiException.new500InternalServerError("Unable to determine url for RestClient.buildUrl().  Either pass the desired url in on your call or set configuration property {}.url=${url}.", getName());

        String protocol = "";
        if ((url.startsWith("http://") || url.startsWith("https://"))) {
            protocol = url.substring(0, url.indexOf(":"));
            url = url.substring(url.indexOf(":") + 1);
        }


        if (Chain.peek() != null && url.indexOf(':') > 0) {
            Request request = Chain.top().getRequest();
            url = request.buildPath(url);
        }

        if (url.endsWith("/"))
            url = url.substring(0, url.length() - 1);

        if(!Utils.empty(protocol))
            return protocol + ":" + url;
        else
            return url;
    }

    public RestClient withUrl(String url) {
        this.url = url;
        return this;
    }

    public ArrayListValuedHashMap<String, String> getForcedHeaders() {
        return forcedHeaders;
    }

    public RestClient withForcedHeader(String name, String value) {
        forcedHeaders.put(name, value);
        return this;
    }

    public RestClient withForcedHeaders(String... headers) {
        for (int i = 0; i < headers.length - 1; i += 2) {
            withForcedHeader(headers[i], headers[i + 1]);
        }
        return this;
    }

    public RestClient withForwardedHeaders(boolean forwardHeaders) {
        this.forwardHeaders = forwardHeaders;
        return this;
    }

    public RestClient withForwardedParams(boolean forwardParams) {
        this.forwardParams = forwardParams;
        return this;
    }

    public String getName() {
        return name;
    }

    public RestClient withName(String name) {
        this.name = name;
        return this;
    }

    public boolean isUseCompression() {
        return useCompression;
    }

    public RestClient withUseCompression(boolean useCompression) {
        this.useCompression = useCompression;
        return this;
    }

    public int getCompressionMinSize() {
        return compressionMinSize;
    }

    public RestClient withCompressionMinSize(int compressionMinSize) {
        this.compressionMinSize = compressionMinSize;
        return this;
    }


    public RestClient withHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
        return this;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public RestClient withSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
        return this;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public RestClient withConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public int getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    public RestClient withConnectionRequestTimeout(int connectionRequestTimeout) {
        this.connectionRequestTimeout = connectionRequestTimeout;
        return this;
    }

    public int getMaxConPerRoute() {
        return maxConPerRoute;
    }

    public RestClient withMaxConPerRoute(int maxConPerRoute) {
        this.maxConPerRoute = maxConPerRoute;
        return this;
    }

    public int getMaxConTotal() {
        return maxConTotal;
    }

    public RestClient withMaxConTotal(int maxConTotal) {
        this.maxConTotal = maxConTotal;
        return this;
    }

    public boolean isEvictExpiredConnections() {
        return evictExpiredConnections;
    }

    public RestClient withEvictExpiredConnections(boolean evictExpiredConnections) {
        this.evictExpiredConnections = evictExpiredConnections;
        return this;
    }

    public int getEvictIdleConnectionsAfterTimeMillis() {
        return evictIdleConnectionsAfterTimeMillis;
    }

    public RestClient withEvictIdleConnectionsAfterTimeMillis(int evictIdleConnectionsAfterTimeMillis) {
        this.evictIdleConnectionsAfterTimeMillis = evictIdleConnectionsAfterTimeMillis;
        return this;
    }

    public HttpClient getHttpClient() {
        if (httpClient == null) {
            synchronized (this) {
                if (httpClient == null) {
                    try {
                        httpClient = buildHttpClient(getHttpClientBuilder());
                    } catch (Exception ex) {
                        Utils.rethrow(ex);
                    }
                }
            }
        }
        return httpClient;
    }

    protected synchronized HttpClient buildHttpClient(HttpClientBuilder builder) throws Exception {
        return builder.build();
    }

    public RestClient withHttpClientBuilder(HttpClientBuilder httpClientBuilder) {
        this.httpClientBuilder = httpClientBuilder;
        return this;
    }

    public HttpClientBuilder getHttpClientBuilder() {
        if (httpClientBuilder == null) {
            synchronized (this) {
                if (httpClientBuilder == null) {
                        httpClientBuilder = buildDefaultHttpClientBuilder();
                }
            }
        }
        return httpClientBuilder;
    }


    public synchronized HttpClientBuilder buildDefaultHttpClientBuilder(){

        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();

        httpClientBuilder.setMaxConnTotal(maxConTotal);
        httpClientBuilder.setMaxConnPerRoute(maxConPerRoute);
        if(evictExpiredConnections)
            httpClientBuilder.evictExpiredConnections();

        if(evictIdleConnectionsAfterTimeMillis > 0)
            httpClientBuilder.evictIdleConnections(evictIdleConnectionsAfterTimeMillis, TimeUnit.MILLISECONDS);

        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(socketTimeout).setConnectTimeout(connectTimeout).setConnectionRequestTimeout(connectionRequestTimeout).build();
        httpClientBuilder.setDefaultRequestConfig(requestConfig);

        return httpClientBuilder;
    }



    public RestClient withExecutor(Executor executor) {
        this.executor = executor;
        return this;
    }

    /**
     * @return lazy constructs <code>executor</code> if necessary.
     */
    public Executor getExecutor() {
        if (executor == null) {
            synchronized (this) {
                if (executor == null) {
                    executor = buildExecutor();
                }
            }
        }
        return executor;
    }

    /**
     * Build an executor if one was not wired in.
     * <p>
     * You can dependency inject your Executor or override this method to provide advanced customizations.
     * <p>
     * As a convenience RestClient.threadsMax is configured on the default executor.
     *
     * @return a default new Executor.
     */
    protected synchronized Executor buildExecutor() {
        return new Executor().withThreadsMax(threadsMax);
    }

    public boolean isForwardHeaders() {
        return forwardHeaders;
    }

    protected boolean shouldForwardHeader(String headerKey) {
        if (headerKey == null)
            return false;

        headerKey = headerKey.trim();

        return forwardHeaders //
                && (includeForwardHeaders.size() == 0 || includeForwardHeaders.contains(headerKey)) //
                && (!excludeForwardHeaders.contains(headerKey));
    }

    public RestClient withForwardHeaders(boolean forwardHeaders) {
        this.forwardHeaders = forwardHeaders;
        return this;
    }

    public Set<String> getIncludeForwardHeaders() {
        return new HashSet(includeForwardHeaders);
    }

    public RestClient withIncludeForwardHeaders(String... headerKeys) {
        for (int i = 0; headerKeys != null && i < headerKeys.length; i++)
            includeForwardHeaders.add(headerKeys[i]);
        return this;
    }

    public RestClient removeIncludeForwardHeader(String headerKey) {
        if (headerKey != null)
            includeForwardHeaders.remove(headerKey);
        return this;
    }

    public Set getExcludeForwardHeaders() {
        return new HashSet(excludeForwardHeaders);
    }

    public RestClient withExcludeForwardHeaders(String... headerKeys) {
        for (int i = 0; headerKeys != null && i < headerKeys.length; i++)
            excludeForwardHeaders.add(headerKeys[i]);
        return this;
    }

    public RestClient removeExcludeForwardHeader(String headerKey) {
        if (headerKey != null)
            excludeForwardHeaders.remove(headerKey);
        return this;
    }

    public boolean isForwardParams() {
        return forwardParams;
    }

    protected boolean shouldForwardParam(String param) {
        return forwardParams //
                && !param.startsWith("_")//
                && !param.equalsIgnoreCase("explain")//
                && !param.equalsIgnoreCase("debug")//
                && (includeParams.size() == 0 || includeParams.contains(param)) //
                && (!excludeParams.contains(param));
    }

    public RestClient withForwardParams(boolean forwardParams) {
        this.forwardParams = forwardParams;
        return this;
    }

    public Set getIncludeParams() {
        return new HashSet(includeParams);
    }

    public RestClient withIncludeParams(String... paramNames) {
        for (int i = 0; paramNames != null && i < paramNames.length; i++)
            includeParams.add(paramNames[i]);
        return this;
    }

    public RestClient removeIncludeParam(String param) {
        if (param != null)
            includeParams.remove(param);
        return this;
    }

    public Set getExcludeParams() {
        return new HashSet(excludeParams);
    }

    public RestClient withExcludeParams(String... paramNames) {
        for (int i = 0; paramNames != null && i < paramNames.length; i++)
            excludeParams.add(paramNames[i]);
        return this;
    }

    public RestClient removeExcludeParam(String param) {
        if (param != null)
            excludeParams.remove(param);
        return this;
    }

    public long getMaxMemoryBuffer() {
        return maxMemoryBuffer;
    }

    public RestClient withMaxMemoryBuffer(long maxMemoryBuffer) {
        this.maxMemoryBuffer = maxMemoryBuffer;
        return this;
    }

    public int getThreadsMax() {
        return threadsMax;
    }

    public RestClient withThreadsMax(int threadsMax) {
        this.threadsMax = threadsMax;
        if (executor != null)
            executor.withThreadsMax(threadsMax);
        return this;
    }

    public interface RequestListener {
        Response onRequest(Request request);
    }

    static class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase {

        static final String methodName = "DELETE";

        public HttpDeleteWithBody(final String url) {
            super();
            setURI(URI.create(url));
        }

        @Override
        public String getMethod() {
            return methodName;
        }
    }

    /**
     * An asynchronous thread pool task runner.
     * <p>
     * The number of threads in the pool will be expanded up to <code>threadsMax</code> and down to
     * <code>thredsMin</code> based on the size of of the queue.  Up to <code>queueMax</code> tasks
     * can occupy the queue before caller will start to be blocked having to wait for queue space to
     * clear up.
     * <p>
     * You can completely disable asynchronous execution by setting <code>threadsMax</code> to zero.
     * That will ensure that tasks will always execute synchronously in the calling thread and will
     * be completed by the time <code>submit</code> returns.
     */
    public static class Executor {

        final     LinkedList<RunnableFuture> queue        = new LinkedList();
        final     Vector<Thread>             threads      = new Vector();
        final     String                     threadPrefix = "executor";
        /**
         * The thread pool will be dynamically contracted to this minimum number of worker threads as the queue length shrinks.
         */
        protected int                        threadsMin   = 1;
        /**
         * The thread pool will by dynamically expanded up to this max number of worker threads as the queue length grows.
         * <p>
         * If this number is less than 1, then tasks will be executed synchronously in the calling thread, not asynchronously.
         */
        protected int                        threadsMax   = 5;
        protected int                        queueMax     = 500;

        public Executor() {

        }

        public synchronized Future submit(final Runnable task) {
            return submit(new RunnableFuture() {

                boolean started = false;
                boolean canceled = false;
                boolean done = false;

                @Override
                public void run() {
                    try {
                        if (canceled || done)
                            return;

                        started = true;
                        task.run();
                    } finally {
                        synchronized (this) {
                            done = true;
                            notifyAll();
                        }
                    }
                }

                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    canceled = true;
                    return !started;
                }

                @Override
                public boolean isCancelled() {
                    return canceled;
                }

                @Override
                public boolean isDone() {
                    return false;
                }

                @Override
                public Object get() throws InterruptedException, ExecutionException {
                    synchronized (this) {
                        while (!done) {
                            wait();
                        }
                    }
                    return null;
                }

                @Override
                public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                    synchronized (this) {
                        while (!done) {
                            wait(unit.toMillis(timeout));
                        }
                    }
                    return null;
                }

            });
        }

        /**
         * Puts <code>task</code> into the queue to be run unless <code>threadsMax</code> is less than one in which case
         * the task is immediately run synchronously in stead of asynchronously.
         *
         * @param task the task to run
         * @return the task submitted
         */
        public synchronized RunnableFuture submit(RunnableFuture task) {
            if (getThreadsMax() < 1) {
                task.run();
            } else {
                put(task);
                checkStartThread();
            }
            return task;
        }

        synchronized boolean checkStartThread() {
            if (queue.size() > 0 && threads.size() < threadsMax) {
                Thread t = new Thread(this::processQueue, threadPrefix + " worker");
                t.setDaemon(true);
                threads.add(t);
                t.start();
                return true;
            }
            return false;
        }

        synchronized boolean checkEndThread() {
            if (queue.size() == 0 && threads.size() > threadsMin) {
                threads.remove(Thread.currentThread());
                return true;
            }
            return false;
        }

        int queued() {
            synchronized (queue) {
                return queue.size();
            }
        }

        void put(RunnableFuture task) {
            synchronized (queue) {
                while (queue.size() >= queueMax) {
                    try {
                        queue.wait();
                    } catch (Exception ex) {
                        //intentionally empty
                    }
                }
                queue.add(task);
                queue.notifyAll();
            }
        }

        RunnableFuture take() {
            RunnableFuture t;
            synchronized (queue) {
                while (queue.size() == 0) {
                    try {
                        queue.wait();
                    } catch (InterruptedException ex) {
                        //intentionally empty
                    }
                }

                t = queue.removeFirst();
                queue.notifyAll();
            }
            return t;
        }

        void processQueue() {
            try {
                while (!checkEndThread()) {
                    do {
                        RunnableFuture task = take();
                        task.run();
                    } while (queue.size() > 0);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        public int getThreadsMin() {
            return threadsMin;
        }

        public Executor withThreadsMin(int threadsMin) {
            this.threadsMin = threadsMin;
            return this;
        }

        public int getThreadsMax() {
            return threadsMax;
        }

        public Executor withThreadsMax(int threadsMax) {
            this.threadsMax = threadsMax;
            return this;
        }

        public int getQueueMax() {
            return queueMax;
        }

        public Executor withQueueMax(int queueMax) {
            this.queueMax = queueMax;
            return this;
        }
    }

    /**
     * A RunnableFuture that blocks on get() until the execution of the Request has returned the Response.
     * <p>
     * Here are some example uses:
     * <pre>
     *
     *     client.get("/some/relative/path")
     *          .onSuccess(response -@gt; System.out.println("Success:" + res.toString()))
     *          .onFailure(response -@gt; System.out.println("Failure:" + res.toString()))
     *          .onResponse(response -@gt; System.out.println("I get called on success or failure: " + res.getStatus()));
     *
     *
     *      //-- instead of using the success/failure callbacks as above
     *      //-- you can wait for the async process to complete by calling 'get()'
     *
     *      FutureResponse future = client.post("/some/relative/path", new JSNode("hello", "world"));
     *
     *      //-- request is asynchronously executing now
     *
     *      //-- the call to get() blocks indefinitely until the async execution completes
     *      //-- the fact that this method is called 'get()' is not related to HTTP get.
     *
     *      Response response = future.get();
     *
     *
     *      //-- if you want to guarantee that your thread will not be indefinitely blocked
     *      //-- you can use get(long timeout, TimeUnit units) to wait no more than the specified time
     *
     *      future = client.get("/some/other/path");
     *      response = future.get(100, TimeUnit.MILLISECONDS);
     *
     *      if(response == null)
     *      {
     *           System.out.println("the http request still has not completed");
     *      }
     *
     *
     * </pre>
     */
    public abstract class FutureResponse implements RunnableFuture<Response> {

        final List<Consumer<Response>> successListeners  = new ArrayList<>();
        final List<Consumer<Response>> failureListeners  = new ArrayList<>();
        final List<Consumer<Response>> responseListeners = new ArrayList<>();
        final Request                  request;
        Response response = null;

        FutureResponse(Request request) {
            this.request = request;
        }

        /**
         * Registers a success callback.
         * <p>
         * If the isDone() is already true the handler will be called synchronously right away.
         *
         * @param handler the listener to notify on success
         * @return this
         */
        public FutureResponse onSuccess(Consumer<Response> handler) {
            boolean done;
            synchronized (this) {
                done = isDone();
                if (!done) {
                    successListeners.add(handler);
                }
            }

            if (done && isSuccess()) {
                try {
                    handler.accept(response);
                } catch (Throwable ex) {
                    log.error("Error handling onSuccess", ex);
                }
            }

            return this;
        }

        /**
         * Registers a failure callback.
         * <p>
         * If the isDone() is already true the handler will be called synchronously right away.
         *
         * @param handler the listener to notify on failure
         * @return this
         */
        public FutureResponse onFailure(Consumer<Response> handler) {
            boolean done;
            synchronized (this) {
                done = isDone();
                if (!done) {
                    failureListeners.add(handler);
                }
            }

            if (done && !isSuccess()) {
                try {
                    handler.accept(response);
                } catch (Throwable ex) {
                    log.error("Error handling onFailure", ex);
                }
            }

            return this;
        }

        /**
         * Registers a listener to be notified regardless of success or failure status.
         * <p>
         * If the isDone() is already true the handler will be called synchronously right away.
         *
         * @param handler the listener to notify when the Response has arrived
         * @return this
         */
        public FutureResponse onResponse(Consumer<Response> handler) {
            boolean done;
            synchronized (this) {
                done = isDone();
                if (!done) {
                    responseListeners.add(handler);
                }
            }

            if (done) {
                try {
                    handler.accept(response);
                } catch (Throwable ex) {
                    log.error("Error handling onResponse", ex);
                }
            }

            return this;
        }

        void setResponse(Response response) {
            synchronized (this) {
                this.response = response;

                //notify all of the RestClient global listeners first.
                for (Consumer<Response> h : RestClient.this.responseListeners) {
                    h.accept(response);
                }

                if (isSuccess()) {
                    for (Consumer<Response> h : successListeners) {
                        try {
                            h.accept(response);
                        } catch (Throwable ex) {
                            log.error("Error handling success callbacks in setResponse", ex);
                        }
                    }
                } else {
                    for (Consumer<Response> h : failureListeners) {
                        try {
                            h.accept(response);
                        } catch (Throwable ex) {
                            log.error("Error handling failure callbacks in setResponse", ex);
                        }
                    }
                }

                for (Consumer<Response> h : responseListeners) {
                    try {
                        h.accept(response);
                    } catch (Throwable ex) {
                        log.error("Error handling callbacks in setResponse", ex);
                    }
                }

                notifyAll();
            }
        }

        /**
         * Blocks indefinitely until <code>response</code> is not null.
         *
         * @return the response
         */
        @Override
        public Response get() {
            while (response == null) {
                synchronized (this) {
                    if (response == null) {
                        try {
                            wait();
                        } catch (InterruptedException ex) {
                            //ignore
                        }
                    }
                }
            }

            return response;
        }

        /**
         * Blocks until the arrival of the response just like get() but will return null after
         * the specified timeout if the response has not arrived.
         *
         * @return the response or null if the call has not asynchronously completed
         */
        @Override
        public Response get(long timeout, TimeUnit unit) throws TimeoutException {
            long start = System.currentTimeMillis();
            while (response == null) {
                synchronized (this) {
                    if (response == null) {
                        try {
                            timeout = TimeUnit.MILLISECONDS.convert(timeout, unit);
                            timeout -= System.currentTimeMillis() - start;

                            if (timeout < 1)
                                break;

                            wait(timeout);
                        } catch (InterruptedException e) {
                            //ignore
                        }
                    }
                }
            }

            return response;
        }

        /**
         * @return true if the response is not null and response.isSuccess()
         */
        public boolean isSuccess() {
            return response != null && response.isSuccess();
        }

        /**
         * @return the Request being run.
         */
        public Request getRequest() {
            return request;
        }

        /**
         * @return false
         */
        @Override
        public boolean isCancelled() {
            return false;
        }

        /**
         * This does nothing.
         *
         * @return false
         */
        @Override
        public boolean cancel(boolean arg0) {
            return false;
        }

        /**
         * @return true when response is not null.
         */
        @Override
        public boolean isDone() {
            return response != null;
        }

    }

}
