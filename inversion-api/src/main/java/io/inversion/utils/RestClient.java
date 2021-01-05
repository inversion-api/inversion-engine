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
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.zip.GZIPOutputStream;

/**
 * An HttpClient wrapper designed specifically to run inside of an Inversion {@code io.inversion.Action} with some superpowers.
 * <p>
 * RestClient gives you easy or built in:
 * <ul>
 *  <li>fluent asynchronous response handler api
 *
 *  <li>automatic retry support
 *
 *  <li>header forwarding w/ whitelists and blacklists
 *
 *  <li>url query string param forwarding w/ whitelist and blacklists
 *
 *  <li>lazy runtime host url construction through lookup of
 *      "${name}.url" in the environment
 *
 *  <li>dynamic host url variables - any "${paramName}" tokens in
 *      the host url will be replaced with Chain.peek.getRequest().getUrl().getParam(paramName).
 *
 *  <li>short circuit remote calls or transform Requests/Responses with a single
 *      override of <code>doRequest(Request)</code>
 *
 * </ul>
 *
 * <p>
 * <b>Transforming requests / responses</b>
 * <p>
 * You can easily override the <code>doRequest(Request)</code> method to potentially short circuit calls or perform request/response transforms.
 * <p>
 * For example:
 * <pre>
 *
 *      protected RestClient client = new RestClient("myservice")
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
 *
 *                  Response response = super.getResponse(request);
 *
 *                  doMyResponseTransformation(response);
 *
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

    static final Log log = LogFactory.getLog(RestClient.class);
    /**
     * Always forward these headers.
     *
     * @see #shouldForwardHeader(String)
     * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_header_fields">HTTP Header Fields</a>
     * @see <a href="https://w3c.github.io/trace-context">W3C Trace Context</a>
     * @see <a href="https://docs.amazonaws.cn/en_us/elasticloadbalancing/latest/application/load-balancer-request-tracing.html">Aws Request Tracing</a>
     * @see <a href="https://docs.microsoft.com/en-us/azure/azure-monitor/app/correlation">Azure Request Correlation</a>
     * @see <a href="https://github.com/dotnet/runtime/blob/master/src/libraries/System.Diagnostics.DiagnosticSource/src/HttpCorrelationProtocol.md">MS Http Corolation Protocol - Deprecated</a>
     */
    protected final Set includeForwardHeaders = Utils.add(new TreeSet<String>(String.CASE_INSENSITIVE_ORDER)//
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
    protected final Set excludeForwardHeaders = Utils.add(new TreeSet<String>(String.CASE_INSENSITIVE_ORDER) //
            , "content-length", "content-type", "content-encoding", "content-language", "content-location", "content-md5", "host");
    /**
     * Headers that are always sent regardless of <code>forwardHeaders</code>, <code>includeForwardHeaders</code> and <code>excludeForwardHeaders</code> state.
     * <p>
     * These headers will overwrite any caller supplied or forwarded header with the same key, not append to the value list.
     * <p>
     * This list is initially empty.
     */
    protected final ArrayListValuedHashMap<String, String> forcedHeaders = new ArrayListValuedHashMap();
    /**
     * Always forward these params.
     *
     * @see #shouldForwardParam(String)
     */
    protected final Set<String>     includeParams = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    /**
     * Never forward these params.
     *
     * @see #shouldForwardParam(String)
     */
    protected final Set<String>     excludeParams = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    protected final List<RequestListener> requestListeners = new ArrayList<>();
    protected final List<Consumer<Response>> responseListeners = new ArrayList<>();
    /**
     * The RestClient name that will be for property decoding.
     *
     * @see Configurator
     */
    protected String name = null;
    /**
     * Indicates the headers from the root inbound Request being handled on this Chain should be included on this request minus any <code>excludeForwardHeaders</code>.
     * <p>
     * Default value is true.
     */
    protected boolean forwardHeaders = true;
    /**
     * Optional base url that will be prepended to the url arg of any calls assuming that the url arg supplied is a relative path and not an absolute url.
     */
    protected String url = null;
    /**
     * Indicates that a request body should be gzipped and the content-encoding header should be sent with value "gzip".
     * <p>
     * Default value is true.
     */
    protected boolean  useCompression     = true;
    /**
     * If <code>useCompression</code> is true, anything over this size in bytes will be compressed.
     * <p>
     * Default value is 1024.
     */
    protected int      compressionMinSize = 1024;
    /**
     * Indicates the params from the root inbound Request being handled on this Chain should be included on this request minus any blacklisted params.
     */
    protected boolean  forwardParams      = false;
    /**
     * The thread pool executor used to make asynchronous requests
     */
    protected Executor executor           = null;
    /**
     * The default maximum number of times to retry a request
     * <p>
     * The default value is zero meaning by default, failed requests will not be retried
     */
    protected int retryMax = 0;
    /**
     * The length of time before the first retry.
     * <p>
     * Incremental retries receive progressively more of a timeout up to <code>retryTimeoutMax</code>.
     *
     * @see #computeTimeout(Request, Response)
     */
    protected int retryTimeoutMin = 10;
    /**
     * The maximum amount of time to wait before a single retry.
     *
     * @see #computeTimeout(Request, Response)
     */
    protected int retryTimeoutMax = 1000;
    /**
     * Parameter for default HttpClient configuration
     *
     * @see org.apache.http.client.config.RequestConfig.Builder#setSocketTimeout(int)
     */
    protected int socketTimeout = 30000;
    /**
     * Parameter for default HttpClient configuration
     *
     * @see org.apache.http.client.config.RequestConfig.Builder#setConnectTimeout(int)
     */
    protected int connectTimeout = 30000;
    /**
     * Parameter for default HttpClient configuration
     *
     * @see org.apache.http.client.config.RequestConfig.Builder#setConnectionRequestTimeout(int)
     */
    protected int requestTimeout = 30000;
    /**
     * The underlying HttpClient use for all network comms.
     */
    protected HttpClient httpClient = null;

    /**
     * The number of background executor threads.
     * <p>
     * A value &lt; 1 will cause all tasks to execute synchronously on the calling thread meaning
     * the FutureResponse will always be complete upon return.
     * <p>
     * The default value is 5.
     */
    protected int threadsMax = 5;

    /**
     * The timer used it trigger retries.
     */
    Timer timer = null;

    public RestClient() {

    }

    /**
     * @param name the prefix used to look up property values from the environment if they have not already been wired
     */
    public RestClient(String name) {
        withName(name);
    }

    /**
     * Convenience overloading of {@link #call(String, String, Map, JSNode, int, ArrayListValuedHashMap)} to perform a GET request.
     *
     * @param fullUrlOrRelativePath may be a full url or relative to the {@link #url} property if set, can have a query string or not
     * @return a FutureResponse that will asynchronously resolve to a Response
     */
    public FutureResponse get(String fullUrlOrRelativePath) {
        return get(fullUrlOrRelativePath, (String) null);
    }

    /**
     * Convenience overloading of {@link #call(String, String, Map, JSNode, int, ArrayListValuedHashMap)} to perform a GET request.
     *
     * @param fullUrlOrRelativePath may be a full url or relative to the {@link #url} property if set, can have a query string or not
     * @param queryString           additional query string params in name=value@amp;name2=value2 style
     * @return a FutureResponse that will asynchronously resolve to a Response
     */
    public FutureResponse get(String fullUrlOrRelativePath, String queryString) {
        return call("GET", fullUrlOrRelativePath, Utils.parseQueryString(queryString), null, this.retryMax, null);
    }

    /**
     * Convenience overloading of {@link #call(String, String, Map, JSNode, int, ArrayListValuedHashMap)} to perform a GET request.
     *
     * @param fullUrlOrRelativePath may be a full url or relative to the {@link #url} property if set, can have a query string or not
     * @param params                query strings passed in as a map
     * @return a FutureResponse that will asynchronously resolve to a Response
     */
    public FutureResponse get(String fullUrlOrRelativePath, Map<String, String> params) {
        return call("GET", fullUrlOrRelativePath, params, null, this.retryMax, null);
    }

    /**
     * Convenience overloading of {@link #call(String, String, Map, JSNode, int, ArrayListValuedHashMap)} to perform a GET request.
     *
     * @param fullUrlOrRelativePath     may be a full url or relative to the {@link #url} property if set, can have a query string or not
     * @param queryStringNameValuePairs additional query string name/value pairs
     * @return a FutureResponse that will asynchronously resolve to a Response
     */
    public FutureResponse get(String fullUrlOrRelativePath, String... queryStringNameValuePairs) {
        return call("GET", fullUrlOrRelativePath, Utils.addToMap(new HashMap<>(), queryStringNameValuePairs), null, this.retryMax, null);
    }

    /**
     * Convenience overloading of {@link #call(String, String, Map, JSNode, int, ArrayListValuedHashMap)} to perform a POST request.
     *
     * @param fullUrlOrRelativePath may be a full url or relative to the {@link #url} property if set
     * @param body                  the optional JSON to post
     * @return a FutureResponse that will asynchronously resolve to a Response
     */
    public FutureResponse post(String fullUrlOrRelativePath, JSNode body) {
        return call("POST", fullUrlOrRelativePath, null, body, this.retryMax, null);
    }

    /**
     * Convenience overloading of {@link #call(String, String, Map, JSNode, int, ArrayListValuedHashMap)} to perform a PUT request.
     *
     * @param fullUrlOrRelativePath may be a full url or relative to the {@link #url} property if set
     * @param body                  the optional JSON to put
     * @return a FutureResponse that will asynchronously resolve to a Response
     */
    public FutureResponse put(String fullUrlOrRelativePath, JSNode body) {
        return call("PUT", fullUrlOrRelativePath, null, body, this.retryMax, null);
    }

    /**
     * Convenience overloading of {@link #call(String, String, Map, JSNode, int, ArrayListValuedHashMap)} to perform a PATCH request.
     *
     * @param fullUrlOrRelativePath may be a full url or relative to the {@link #url} property if set
     * @param body                  the optional JSON patch
     * @return a FutureResponse that will asynchronously resolve to a Response
     */
    public FutureResponse patch(String fullUrlOrRelativePath, JSNode body) {
        return call("PATCH", fullUrlOrRelativePath, null, body, this.retryMax, null);
    }

    /**
     * Convenience overloading of {@link #call(String, String, Map, JSNode, int, ArrayListValuedHashMap)} to perform a DELETE request.
     *
     * @param fullUrlOrRelativePath may be a full url or relative to the {@link #url} property if set
     * @return a FutureResponse that will asynchronously resolve to a Response
     */
    public FutureResponse delete(String fullUrlOrRelativePath) {
        return call("DELETE", fullUrlOrRelativePath, null, null, this.retryMax, null);
    }

    /**
     * Makes an HTTP request.
     *
     * @param method                the HTTP method to invoke
     * @param fullUrlOrRelativePath optional may be a full url or only additional relative path parts if the {@link #url} property if set, may contain a query string
     * @param params                optional additional query string params that will overwrite any that may be on url as composed from {@link #buildUrl(String)}
     * @param body                  optional json body
     * @param retryMax              how many times the client should retry if the Request is not successful, if less than zero then this.retriesMax is used
     * @param headers               headers that will always be sent regardless of {@link #includeForwardHeaders}, {@link #excludeForwardHeaders} but may be overwritten by {@link #forcedHeaders}
     * @return a FutureResponse that will asynchronously resolve to a Response
     */
    public FutureResponse call(String method, String fullUrlOrRelativePath, Map<String, String> params, JSNode body, int retryMax, ArrayListValuedHashMap<String, String> headers) {
        Request request = buildRequest(method, fullUrlOrRelativePath, params, body, headers, retryMax);
        return call(request);
    }

    /**
     * Executes the request as provided without modification.
     * <p>
     * All of the other 'get/post/put/patch/delete/call' methods will construct a Request based on the configured
     * properties of this RestClient and optionally the data in Request on the top of the Chain if operating inside an Engine.
     * <p>
     * Those methods ultimately delegate to this method and no further modification of the Request is made from here out.
     *
     * @param request
     * @return
     */
    public FutureResponse call(Request request){
        FutureResponse future = buildFuture(request);
        submit(future);
        return future;
    }

    /**
     * Builds a request with the supplied information merged with the url, query param, and header options configured
     * on this reset client and potentially pulled from the Chain request.
     *
     * @param method - the http method
     * @param fullUrlOrRelativePath - a full url or a relative path that will be appended to this.url
     * @param params - query params to pass
     * @param body - the request body to pass
     * @param headers - request headers to pass
     * @param retryMax - the number of times to retry this call if it fails.
     * @return the configure request
     */
    public Request buildRequest(String method, String fullUrlOrRelativePath, Map<String, String> params, JSNode body, ArrayListValuedHashMap<String, String> headers, int retryMax) {

        String url = buildUrl(fullUrlOrRelativePath);
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

        Request request = new Request(method, url, (body == null ? null : body.toString()), params, headers, retryMax > -1 ? retryMax : this.retryMax);

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
                Map<String, String> origionalParams        = originalInboundRequest.getUrl().getParams();
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
                if (shouldRetry(request, response)) {
                    request.incrementRetryCount();
                    long timeout = computeTimeout(request, response);
                    submitLater(this, timeout);
                } else {
                    response.withEndAt(System.currentTimeMillis());
                    setResponse(response);
                }
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
     *
     *          Response response = super.getResponse(request);
     *
     *          doMyResponseTransformation(response);
     *
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
        File            tempFile = null;

        String   url      = request.getUrl().toString();
        Response response = new Response(url);
        response.withRequest(request);

        for(RequestListener l : requestListeners){
            Response replacementResponse = l.onRequest(request);
            if(replacementResponse != null) {
                if(replacementResponse.getUrl() == null)
                    replacementResponse.withUrl(url);
                if(replacementResponse.getRequest() == null)
                    replacementResponse.withRequest(request);
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

                if (request.getRetryFile() != null && request.getRetryFile().length() > 0) {
                    long range = request.getRetryFile().length();
                    request.getHeaders().remove("Range");
                    request.getHeaders().put("Range", "bytes=" + range + "-");

                    log.debug("RANGE REQUEST HEADER ** " + range);
                }
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

            RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(socketTimeout).setConnectTimeout(connectTimeout).setConnectionRequestTimeout(requestTimeout).build();
            req.setConfig(requestConfig);

            hr = h.execute(req);

            response.withStatusMesg(hr.getStatusLine().toString());
            response.withStatusCode(hr.getStatusLine().getStatusCode());

            response.debug("-response headers -----");
            response.debug("status: " + response.getStatus());
            for (Header header : hr.getAllHeaders()) {
                response.debug("\r\n" + header.getName() + ": " + header.getValue());
                response.withHeader(header.getName(), header.getValue());
            }

            log.debug("RESPONSE CODE ** " + response.getStatusCode() + "   (" + response.getStatus() + ")");
            log.debug("CONTENT RANGE RESPONSE HEADER ** " + response.getHeader("Content-Range"));

            Url    u        = new Url(url);
            String fileName = u.getFile();
            if (fileName == null)
                fileName = Utils.slugify(u.toString());

            boolean skip = false;
            if (response.getStatusCode() == 404 //no amount of retries will make this request not found
                    || response.getStatusCode() == 204)//this status code indicates "no content" so we are done.
            {
                skip = true;
            }
            // if we have a retry file and it's length matches the Content-Range header's start and the Content-Range header's unit's are bytes use the existing file
            else if (request.getRetryFile() != null //
                    && request.getRetryFile().length() == response.getContentRangeStart() //
                    && "bytes".equalsIgnoreCase(response.getContentRangeUnit())) {
                tempFile = request.getRetryFile();
                log.debug("## Using existing file .. " + tempFile);
            } else if (response.getStatusCode() == 206) {
                // status code is 206 Partial Content, but we don't want to use the existing file for some reason, so abort this and force it to fail
                throw new Exception("Partial content without valid values, aborting this request");
            } else {
                if (fileName.length() < 3) {
                    // if fileName is only 2 characters long, createTempFile will blow up
                    fileName += "_ext";
                }

                tempFile = Utils.createTempFile(fileName);
                tempFile.deleteOnExit();
                log.debug("## Creating temp file .. " + tempFile);
            }

            HttpEntity e;
            if (!skip && (e = hr.getEntity()) != null) {
                // stream to the temp file with append set to true (this is crucial for resumable downloads)
                InputStream is = e.getContent();
                Utils.pipe(is, new FileOutputStream(tempFile, true));

                response.withFile(tempFile);

                if (response.getContentRangeSize() > 0 && tempFile.length() > response.getContentRangeSize()) {
                    // Something is wrong.. The server is saying the file should be X, but the actual file is larger than X, abort this
                    throw new Exception("Downloaded file is larger than the server says it should be, aborting this request");
                }
            }
        } catch (Exception ex) {
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

    public RestClient onRequest(RequestListener requestListener) {
        this.requestListeners.add(requestListener);
        return this;
    }

    public RestClient onResponse(Consumer<Response> responseListener) {
        this.responseListeners.add(responseListener);
        return this;
    }

    public static interface RequestListener{
        Response onRequest(Request request);
    }



    /**
     * Computes an incremental retry backoff time between retryTimeoutMin and retryTimeoutMax.
     * <p>
     * You can override this to provide your own timeout policy.
     *
     * @param request  the Request being made
     * @param response the Response being computed
     * @return the number of milliseconds to wait before retrying the Request
     */
    protected long computeTimeout(Request request, Response response) {
        long timeout = (retryTimeoutMin * request.getRetryCount() * request.getRetryCount()) + (int) (retryTimeoutMin * Math.random() * request.getRetryCount());
        if (retryTimeoutMax > 0 && timeout > retryTimeoutMax)
            timeout = retryTimeoutMax;

        return timeout;
    }

    /**
     * Determines if the Request should be retried.
     * <p>
     * You can override this to provide your own retry policy.
     * <p>
     * The default algorithm will only retry if the Request is not successful due to a network error and the current request.retryCount is less than reqest.retryMax.
     * <p>
     *
     * @param request  the Request being made
     * @param response the Response being computed
     * @return true if this request should be retried
     * @see #isNetworkException(Throwable)
     */
    protected boolean shouldRetry(Request request, Response response) {
        return response != null && //
                !response.isSuccess() //
                && request.getRetryCount() < request.getRetryMax() //
                && isNetworkException(response.getError());
    }

    boolean isNetworkException(Throwable ex) {
        if (ex == null)
            return false;

        return
                ex instanceof java.net.ConnectException //
                        || ex instanceof java.net.SocketTimeoutException //
                        //|| ex instanceof org.apache.http.conn.HttpHostConnectException //redundant to java.net.ConnectionException
                        || ex instanceof org.apache.http.conn.ConnectTimeoutException //
                        || ex instanceof org.apache.http.NoHttpResponseException //
                //|| ex instanceof java.net.NoRouteToHostException //
                //|| ex instanceof java.net.UnknownHostException //
                ;
    }

    synchronized void submit(FutureResponse future) {
        getExecutor().submit(future);
    }

    synchronized void submitLater(final FutureResponse future, long delay) {
        if (timer == null) {
            timer = new Timer();
        }

        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                submit(future);
            }
        }, delay);

    }

    /**
     * Factory method for building an HttpClient if one was not configured via withHttpClient.
     * <p>
     * The default HttpClient constructed here basically SSL cert and hostname verification assuming that you will be calling clients you control.
     * <p>
     * If you need to adopt a tighter security posture just override this method or call {@link #withHttpClient(HttpClient)} to supply your own HttpClient and security configuration.
     * <p>
     * Use of this method is wrapped by getHttpClient() which controls concurrency so this method should not be called more than once per instance if
     * an error is not thrown.
     *
     * @return an HttpClient that can be use to make concurrent asynchronous requests.
     * @throws Exception if HttpClient construction fails
     * @see <a href="http://literatejava.com/networks/ignore-ssl-certificate-errors-apache-httpclient-4-4/">Ignore SSL Certificate Errors</a>
     */
    protected HttpClient buildHttpClient() throws Exception {
        if (httpClient == null) {
            HttpClientBuilder b = HttpClientBuilder.create();

            // setup a Trust Strategy that allows all certificates.
            //
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, (arg0, arg1) -> true).build();
            b.setSslcontext(sslContext);

            // don't check Hostnames, either.
            //      -- use SSLConnectionSocketFactory.getDefaultHostnameVerifier(), if you don't want to weaken
            HostnameVerifier hostnameVerifier = SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

            // here's the special part:
            //      -- need to create an SSL Socket Factory, to use our weakened "trust strategy";
            //      -- and create a Registry, to register it.
            //
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
            //Registry<ConnectionSocketFactory> socketFactoryRegistry = ;

            // now, we create connection-manager using our Registry.
            //      -- allows multi-threaded use
            PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(RegistryBuilder.<ConnectionSocketFactory>create().register("http", PlainConnectionSocketFactory.getSocketFactory()).register("https", sslSocketFactory).build());

            b.setConnectionManager(connMgr);

            RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(socketTimeout).setConnectTimeout(connectTimeout).setConnectionRequestTimeout(requestTimeout).build();
            b.setDefaultRequestConfig(requestConfig);

            // finally, build the HttpClient;
            //      -- done!
            httpClient = b.build();
        }
        return httpClient;
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
            //-- have been set during wiring by the Configurator. This is here so RestClient can be used outside
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

        return protocol + ":" + url;
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

    public int getRetryMax() {
        return retryMax;
    }

    public RestClient withRetryMax(int retryMax) {
        this.retryMax = retryMax;
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

    public int getRequestTimeout() {
        return requestTimeout;
    }

    public RestClient withRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
        return this;
    }

    public RestClient withHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
        return this;
    }

    public HttpClient getHttpClient() {
        if (httpClient == null) {
            synchronized (this) {
                if (httpClient == null) {
                    try {
                        httpClient = buildHttpClient();
                    } catch (Exception ex) {
                        Utils.rethrow(ex);
                    }
                }
            }
        }
        return httpClient;
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

    public RestClient withExecutor(Executor executor) {
        this.executor = executor;
        return this;
    }

    /**
     * Build an executor if one was not wired in.
     * <p>
     * You can dependency inject your Executor or override this method to provide advanced customizations.
     * <p>
     * As a convenience RestClient.threadsMax is configured on the default executor.
     * @return a default new Executor.
     */
    protected Executor buildExecutor() {
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

    public int getRetryTimeoutMin() {
        return retryTimeoutMin;
    }

    public RestClient withRetryTimeoutMin(int retryTimeoutMin) {
        this.retryTimeoutMin = retryTimeoutMin;
        return this;
    }

    public int getRetryTimeoutMax() {
        return retryTimeoutMax;
    }

    public RestClient getRetryTimeoutMax(int retryTimeoutMax) {
        this.retryTimeoutMax = retryTimeoutMax;
        return this;
    }

    public int getThreadsMax() {
        return threadsMax;
    }

    public RestClient withThreadsMax(int threadsMax) {
        this.threadsMax = threadsMax;
        if(executor != null)
            executor.withThreadsMax(threadsMax);
        return this;
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
