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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.swing.SwingUtilities;

import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;

import io.inversion.Chain;
import io.inversion.Request;
import io.inversion.Response;

/**
 * An HttpClient wrapper designed specifically to run inside of an
 * Inversion Action that gives you easy or built in:
 * <ul>
 *  <li>retry support (built in)
 *  
 *  <li>asynchronous call support (built in)
 *  
 *  <li>header forwarding w/ whitelists and blacklists
 *  
 *  <li>lazy runtime host url construction through lookup of
 *      "${name}.url" in the environment
 *      
 *  <li>dynamic host url variables - any "${paramName}" tokens in
 *      the host url will be replaced with Chain.peek.getRequest().getUrl().getParam(paramName).
 *      
 *  <li>short circuit or transform Requests/Responses with a single
 *      override of <code>getResponse(Request)</code>
 *  <li>
 * </ul>    
 * 
 * 
 * <b>Transforming requests / responses<b>
 * <p> 
 * You can easily override the <code>getResponse(Request)</code> 
 * method to potentially short circuit calls or perform 
 * request/response transforms.  For example:
 * 
 * <pre>
 * protected RestClient client = new RestClient("myservice"){
 * 
 *  @Override
 *  protected Response getResponse(Request request)
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
 */
public class RestClient
{
   static Log                                       log              = LogFactory.getLog(RestClient.class);

   //-- config properties
   protected String                                 name             = null;
   protected String                                 url              = null;
   protected boolean                                remoteAsync      = true;
   protected boolean                                localAsync       = false;
   protected ArrayListValuedHashMap<String, String> forcedHeaders    = new ArrayListValuedHashMap();

   //https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers
   protected boolean                                forwardHeaders   = false;
   protected Set                                    whitelistHeaders = new HashSet(Arrays.asList("authorization", "cookie", "x-forwarded-host", " x-forwarded-proto"));
   protected Set                                    blacklistHeaders = new HashSet(Arrays.asList("content-length", "content-type", "content-encoding", "content-language", "content-location", "content-md5", "host"));

   protected boolean                                forwardParams    = false;
   protected Set<String>                            whitelistParams  = new HashSet();
   protected Set<String>                            blacklistParams  = new HashSet();

   //-- networking and pooling specific properties
   protected Executor                               pool             = null;
   protected int                                    poolMin          = 2;
   protected int                                    poolMax          = 100;
   protected int                                    queueMax         = 500;
   protected int                                    retryMax         = 5;
   protected int                                    totalRetryMax    = 50;

   protected int                                    retryTimeoutMin  = 10;
   protected int                                    retryTimeoutMax  = 1000;

   protected int                                    socketTimeout    = 30000;
   protected int                                    connectTimeout   = 30000;
   protected int                                    requestTimeout   = 30000;

   protected HttpClient                             httpClient       = null;

   protected Timer                                  timer            = null;

   public RestClient()
   {

   }

   public RestClient(String name)
   {
      withName(name);
   }

   public FutureResponse get(String path, Map<String, String> params)
   {
      return call("GET", path, params, null, -1, null);
   }

   public FutureResponse get(String path, String... queryStringOrNameValuePairs)
   {
      if (queryStringOrNameValuePairs != null && queryStringOrNameValuePairs.length == 1)
         return call("GET", path, Utils.parseQueryString(queryStringOrNameValuePairs[0]), (JSNode) null, -1, null);
      else
         return call("GET", path, Utils.addToMap(new HashMap(), queryStringOrNameValuePairs), null, -1, null);
   }

   public FutureResponse put(String path, JSNode body)
   {
      return call("PUT", path, null, body, -1, null);
   }

   public FutureResponse post(String path, JSNode body)
   {
      return call("POST", path, null, body, -1, null);
   }

   public FutureResponse patch(String path, JSNode body)
   {
      return call("PATCH", path, null, body, -1, null);
   }

   public FutureResponse delete(String path)
   {
      return call("DELETE", path, null, null, -1, null);
   }

   public FutureResponse call(String method, String path, Map<String, String> params, JSNode body, int retries, ArrayListValuedHashMap<String, String> headers)
   {
      String url = buildUrl();
      if (url != null && path != null)
      {
         url += (url.endsWith("/") ? "" : "/") + Utils.implode("/", Utils.explode("/", path));
      }
      else if (url == null && path != null)
      {
         url = path;
      }

      Request request = buildRequest(method, url, params, (body != null ? body.toString() : null), headers, retries);
      FutureResponse future = buildFuture(request);
      boolean localCall = future.request.isLocalRequest();

      if ((localCall && !localAsync) || (!localCall && !remoteAsync))
      {
         future.run();
      }
      else
      {
         submit(future);
      }

      return future;
   }

   protected Request buildRequest(String method, String url, Map<String, String> callParams, String body, ArrayListValuedHashMap<String, String> callHeaders, int retryAttempts)
   {
      Request request = new Request(method, url, body, callParams, callHeaders, retryAttempts);

      if (forcedHeaders.size() > 0)
      {
         for (String key : forcedHeaders.keySet())
         {
            request.removeHeader(key);
            for (String value : forcedHeaders.get(key))
               request.withHeader(key, value);
         }
      }

      if (forwardHeaders)
      {
         Chain chain = Chain.first();//gets the root chain
         if (chain != null)
         {
            Request originalInboundRequest = chain.getRequest();
            ArrayListValuedHashMap<String, String> inboundHeaders = originalInboundRequest.getHeaders();
            if (inboundHeaders != null)
            {
               for (String key : inboundHeaders.keySet())
               {
                  if (forwardHeader(key))
                  {
                     if (request.getHeader(key) == null)
                        for (String header : inboundHeaders.get(key))
                           request.withHeader(key, header);
                  }
               }
            }
         }
      }

      if (forwardParams)
      {
         Chain chain = Chain.first();
         if (chain != null)
         {
            Request originalInboundRequest = chain.getRequest();
            Map<String, String> origionalParams = originalInboundRequest.getUrl().getParams();
            if (origionalParams.size() > 0)
            {
               for (String key : origionalParams.keySet())
               {
                  if (forwardParam(key))
                  {
                     if (request.getUrl().getParam(key) == null)
                        request.getUrl().withParam(key, origionalParams.get(key));
                  }
               }
            }
         }
      }

      return request;
   }

   protected FutureResponse buildFuture(Request request)
   {
      final FutureResponse future = new FutureResponse(request)
         {
            public void run()
            {
               Response response = null;
               try
               {
                  response = getResponse(request);
               }
               finally
               {
                  // We had a successful response, so let's reset the retry count to give the best chance of success
                  if (response.getStatusCode() >= 200 && response.getStatusCode() <= 300)
                  {
                     debug("Resetting retry count");
                     request.resetRetryCount();
                  }

                  if (!response.isSuccess() //
                        && 404 != response.getStatusCode() //don't retry 404...it won't help
                        && request.getRetryCount() < request.getRetryAttempts() //
                        && request.getTotalRetries() < totalRetryMax) // since we resetRetryCount upon any successful response, this guards against a crazy large amount of retries with the TOTAL_MAX_RETRY_ATTEMPTS
                  {
                     request.incrementRetryCount();

                     long timeout = computeTimeout(request);
                     debug("retrying " + request.getTotalRetries() + "th attempt in " + timeout + "ms: " + request.getRetryCount());
                     submitLater(this, timeout);
                  }
                  else
                  {
                     if (!response.isSuccess() && response.getError() != null && !(isNetworkException(response.getError())))
                     {
                        log.warn("Error in RestClient: '" + request.getMethod() + " " + url + "' ", response.getError());
                     }

                     setResponse(response);
                  }
               }
            }
         };

      return future;
   }

   protected Response getResponse(Request request)
   {
      String url = request.getUrl().toString();
      Response response = new Response(url);

      try
      {
         //this is a local call...send it back to the engine
         if (request.isLocalRequest())
            return getLocalResponse(request);
         else
            return getRemoteResponse(request);
      }
      catch (Exception ex)
      {
         response.withError(ex);
         if (isNetworkException(ex))
         {
            log.debug("Network exception " + ex.getClass().getName() + " - " + ex.getMessage() + " - " + url);
         }
         else
         {
            log.warn("Exception in rest call. " + url, ex);
         }
      }

      return response;
   }

   protected Response getLocalResponse(Request request) throws Exception
   {
      String url = request.getUrl().toString();
      Response response = new Response(url);

      request.withEngine(request.getChain().getEngine());
      request.getChain().getEngine().service(request, response);
      return response;

   }

   protected Response getRemoteResponse(Request request) throws Exception
   {
      String m = request.getMethod();
      HttpRequestBase req = null;
      File tempFile = null;

      String url = request.getUrl().toString();
      Response response = new Response(url);

      try
      {
         HttpClient h = getHttpClient();
         HttpResponse hr = null;

         response.debug("--request header------");
         response.debug(m + " " + url);

         if ("post".equalsIgnoreCase(m))
         {
            req = new HttpPost(url);
         }
         if ("put".equalsIgnoreCase(m))
         {
            req = new HttpPut(url);
         }
         else if ("get".equalsIgnoreCase(m))
         {
            req = new HttpGet(url);

            if (request.getRetryFile() != null && request.getRetryFile().length() > 0)
            {
               long range = request.getRetryFile().length();
               request.getHeaders().remove("Range");
               request.getHeaders().put("Range", "bytes=" + range + "-");

               debug("RANGE REQUEST HEADER ** " + range);
            }
         }
         else if ("delete".equalsIgnoreCase(m))
         {
            if (request.getBody() != null)
            {
               req = new HttpDeleteWithBody(url);
            }
            else
            {
               req = new HttpDelete(url);
            }
         }
         else if ("patch".equalsIgnoreCase(m))
         {
            req = new HttpPatch(url);
         }

         for (String key : request.getHeaders().keySet())
         {
            List<String> values = request.getHeaders().get(key);
            for (String value : values)
            {
               req.setHeader(key, value);
               response.debug(key, value);
            }
         }
         if (request.getBody() != null && req instanceof HttpEntityEnclosingRequestBase)
         {
            response.debug("\r\n--request body--------");
            ((HttpEntityEnclosingRequestBase) req).setEntity(new StringEntity(request.getBody(), "UTF-8"));
         }

         RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(socketTimeout).setConnectTimeout(connectTimeout).setConnectionRequestTimeout(requestTimeout).build();
         req.setConfig(requestConfig);

         hr = h.execute(req);

         response.withStatusMesg(hr.getStatusLine().toString());
         response.withStatusCode(hr.getStatusLine().getStatusCode());

         response.debug("-response headers -----");
         response.debug("status: " + response.getStatus());
         for (Header header : hr.getAllHeaders())
         {
            response.debug("\r\n" + header.getName() + ": " + header.getValue());
            response.withHeader(header.getName(), header.getValue());
         }

         debug("RESPONSE CODE ** " + response.getStatusCode() + "   (" + response.getStatus() + ")");
         debug("CONTENT RANGE RESPONSE HEADER ** " + response.getHeader("Content-Range"));

         Url u = new Url(url);
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
               && "bytes".equalsIgnoreCase(response.getContentRangeUnit()))
         {
            tempFile = request.getRetryFile();
            debug("## Using existing file .. " + tempFile);
         }
         else if (response.getStatusCode() == 206)
         {
            // status code is 206 Partial Content, but we don't want to use the existing file for some reason, so abort this and force it to fail
            throw new Exception("Partial content without valid values, aborting this request");
         }
         else
         {
            if (fileName.length() < 3)
            {
               // if fileName is only 2 characters long, createTempFile will blow up
               fileName += "_ext";
            }

            tempFile = Utils.createTempFile(fileName);
            tempFile.deleteOnExit();
            debug("## Creating temp file .. " + tempFile);
         }

         HttpEntity e = null;
         if (!skip && (e = hr.getEntity()) != null)
         {
            // stream to the temp file with append set to true (this is crucial for resumable downloads)
            InputStream is = e.getContent();
            Utils.pipe(is, new FileOutputStream(tempFile, true));

            response.withFile(tempFile);

            if (response.getContentRangeSize() > 0 && tempFile.length() > response.getContentRangeSize())
            {
               // Something is wrong.. The server is saying the file should be X, but the actual file is larger than X, abort this
               throw new Exception("Downloaded file is larger than the server says it should be, aborting this request");
            }
         }
      }
      finally
      {
         if (req != null)
         {
            try
            {
               req.releaseConnection();
            }
            catch (Exception ex)
            {
               log.info("Exception trying to release the request connection", ex);
            }
         }
      }

      return response;
   }

   public boolean isNetworkException(Throwable ex)
   {
      return ex instanceof org.apache.http.conn.HttpHostConnectException //
            || ex instanceof org.apache.http.conn.ConnectTimeoutException //
            || ex instanceof org.apache.http.NoHttpResponseException //
            || ex.getClass().getName().startsWith("java.net")
      //|| ex instanceof java.net.ConnectException //
      //|| ex instanceof java.net.NoRouteToHostException //
      //|| ex instanceof java.net.SocketTimeoutException //
      //|| ex instanceof java.net.UnknownHostException //
      ;
   }

   protected long computeTimeout(Request request)
   {
      long timeout = (retryTimeoutMin * request.getRetryCount() * request.getRetryCount()) + (int) (retryTimeoutMin * Math.random() * request.getRetryCount());
      if (retryTimeoutMax > 0 && timeout > retryTimeoutMax)
         timeout = retryTimeoutMax;

      return timeout;
   }

   synchronized void submit(FutureResponse future)
   {
      if (pool == null)
         pool = new Executor(poolMin, poolMax, queueMax);

      pool.submit(future);
   }

   synchronized void submitLater(final FutureResponse future, long delay)
   {
      if (timer == null)
      {
         timer = new Timer();
      }

      timer.schedule(new TimerTask()
         {
            @Override
            public void run()
            {
               submit(future);
            }
         }, delay);

   }

   /**
    * @see http://literatejava.com/networks/ignore-ssl-certificate-errors-apache-httpclient-4-4/
    * @return
    * @throws Exception
    */
   public synchronized HttpClient getHttpClient() throws Exception
   {
      if (httpClient == null)
      {
         HttpClientBuilder b = HttpClientBuilder.create();

         // setup a Trust Strategy that allows all certificates.
         //
         SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy()
            {
               public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException
               {
                  return true;
               }
            }).build();
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
         PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(RegistryBuilder.<ConnectionSocketFactory> create().register("http", PlainConnectionSocketFactory.getSocketFactory()).register("https", sslSocketFactory).build());

         b.setConnectionManager(connMgr);

         RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(socketTimeout).setConnectTimeout(connectTimeout).setConnectionRequestTimeout(requestTimeout).build();
         b.setDefaultRequestConfig(requestConfig);

         // finally, build the HttpClient;
         //      -- done!
         httpClient = b.build();
      }
      return httpClient;
   }

   private void debug(Object obj)
   {
      if (log.isDebugEnabled())
      {
         log.debug(obj);
      }
   }

   static class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase
   {

      static final String methodName = "DELETE";

      @Override
      public String getMethod()
      {
         return methodName;
      }

      public HttpDeleteWithBody(final String url)
      {
         super();
         setURI(URI.create(url));
      }
   }

   /**
    * Finds and composes the URL for the remote host.
    * <p>
    * If "${this.name}.url" is found in the environment it is 
    * used, otherwise this.url is used.  
    * <p>
    * After the base string is located, any "${paramName}" tokens
    * found in the string are replaced with any URL variables
    * form the current Inversion request via 
    * <code>Chain.peek().getRequest().getParam(paramName)</code> 
    * 
    * @return
    */
   public String buildUrl()
   {
      String propKey = getName() + ".url";
      String url = Utils.getSysEnvPropStr(propKey, this.url);

      if (Chain.peek() != null && url.indexOf('$') > 0)
      {
         Request request = Chain.peek().getRequest();

         StringBuffer buff = new StringBuffer("");
         Pattern p = Pattern.compile("\\$\\{([^\\}]*)\\}");
         Matcher m = p.matcher(url);
         while (m.find())
         {
            String key = m.group(1);
            String param = request.getUrl().getParam(key);
            if (param == null)//replacement value was not there
               param = "${" + key + "}";

            String value = Matcher.quoteReplacement(param);
            m.appendReplacement(buff, value);
         }
         m.appendTail(buff);
         return buff.toString();
      }

      return url;
   }

   public RestClient withUrl(String url)
   {
      this.url = url;
      return this;
   }

   public ArrayListValuedHashMap<String, String> getForcedHeaders()
   {
      return forcedHeaders;
   }

   public RestClient withForcedHeader(String name, String value)
   {
      forcedHeaders.put(name, value);
      return this;
   }

   public RestClient withForcedHeaders(String... headers)
   {
      for (int i = 0; i < headers.length - 1; i += 2)
      {
         withForcedHeader(headers[i], headers[i + 1]);
      }
      return this;
   }

   public RestClient withForwardedHeaders(boolean forwardHeaders)
   {
      this.forwardHeaders = forwardHeaders;
      return this;
   }

   public RestClient withForwardedParams(boolean forwardParams)
   {
      this.forwardParams = forwardParams;
      return this;
   }

   public String getName()
   {
      return name;
   }

   public RestClient withName(String name)
   {
      this.name = name;
      return this;
   }

   public int getPoolMin()
   {
      return poolMin;
   }

   public RestClient withPoolMin(int poolMin)
   {
      this.poolMin = poolMin;
      return this;
   }

   public int getPoolMax()
   {
      return poolMax;
   }

   public RestClient withPoolMax(int poolMax)
   {
      this.poolMax = poolMax;
      return this;
   }

   public int getQueueMax()
   {
      return queueMax;
   }

   public RestClient withQueueMax(int queueMax)
   {
      this.queueMax = queueMax;
      return this;
   }

   public int getRetryMax()
   {
      return retryMax;
   }

   public RestClient withRetryMax(int retryMax)
   {
      this.retryMax = retryMax;
      return this;
   }

   public int getTotalRetryMax()
   {
      return totalRetryMax;
   }

   public RestClient withTotalRetryMax(int totalRetryMax)
   {
      this.totalRetryMax = totalRetryMax;
      return this;
   }

   public int getSocketTimeout()
   {
      return socketTimeout;
   }

   public RestClient withSocketTimeout(int socketTimeout)
   {
      this.socketTimeout = socketTimeout;
      return this;
   }

   public int getConnectTimeout()
   {
      return connectTimeout;
   }

   public RestClient withConnectTimeout(int connectTimeout)
   {
      this.connectTimeout = connectTimeout;
      return this;
   }

   public int getRequestTimeout()
   {
      return requestTimeout;
   }

   public RestClient withRequestTimeout(int requestTimeout)
   {
      this.requestTimeout = requestTimeout;
      return this;
   }

   public RestClient withHttpClient(HttpClient httpClient)
   {
      this.httpClient = httpClient;
      return this;
   }

   public Executor getPool()
   {
      return pool;
   }

   public RestClient withPool(Executor pool)
   {
      this.pool = pool;
      return this;
   }

   public boolean isRemoteAsync()
   {
      return remoteAsync;
   }

   public RestClient withRemoteAsync(boolean remoteAsync)
   {
      this.remoteAsync = remoteAsync;
      return this;
   }

   public boolean isLocalAsync()
   {
      return localAsync;
   }

   public RestClient withLocalAcync(boolean localAsync)
   {
      this.localAsync = localAsync;
      return this;
   }

   public boolean isForwardHeaders()
   {
      return forwardHeaders;
   }

   public boolean forwardHeader(String headerKey)
   {
      return forwardHeaders //
            && (whitelistHeaders.size() == 0 || whitelistHeaders.contains(headerKey.toLowerCase())) //
            && (!blacklistHeaders.contains(headerKey.toLowerCase()));
   }

   public RestClient withForwardHeaders(boolean forwardHeaders)
   {
      this.forwardHeaders = forwardHeaders;
      return this;
   }

   public Set getWhitelistHeaders()
   {
      return new HashSet(whitelistHeaders);
   }

   public RestClient withWhitelistedHeaders(String... headerKeys)
   {
      for (int i = 0; headerKeys != null && i < headerKeys.length; i++)
         whitelistHeaders.add(headerKeys[i].toLowerCase());
      return this;
   }

   public RestClient removeWhitelistHeader(String headerKey)
   {
      if (headerKey != null)
         whitelistHeaders.remove(headerKey.toString());
      return this;
   }

   public boolean isForwardParams()
   {
      return forwardParams;
   }

   public boolean forwardParam(String param)
   {
      return forwardParams //
            && (whitelistParams.size() == 0 || whitelistParams.contains(param.toLowerCase())) //
            && (!blacklistParams.contains(param.toLowerCase()));
   }

   public RestClient withForwardParams(boolean forwardParams)
   {
      this.forwardParams = forwardParams;
      return this;
   }

   public Set getWhitelistParams()
   {
      return new HashSet(whitelistParams);
   }

   public RestClient withWhitelistedParams(String... paramNames)
   {
      for (int i = 0; paramNames != null && i < paramNames.length; i++)
         whitelistParams.add(paramNames[i].toLowerCase());
      return this;
   }

   public RestClient removeWhitelistParam(String param)
   {
      if (param != null)
         whitelistParams.remove(param.toString());
      return this;
   }

   public int getRetryTimeoutMin()
   {
      return retryTimeoutMin;
   }

   public RestClient withRetryTimeoutMin(int retryTimeoutMin)
   {
      this.retryTimeoutMin = retryTimeoutMin;
      return this;
   }

   public int getRetryTimeoutMax()
   {
      return retryTimeoutMax;
   }

   public RestClient getRetryTimeoutMax(int retryTimeoutMax)
   {
      this.retryTimeoutMax = retryTimeoutMax;
      return this;
   }

   public static abstract class FutureResponse implements RunnableFuture<Response>
   {
      Log                   log        = LogFactory.getLog(getClass());

      long                  createdAt  = System.currentTimeMillis();

      Request               request    = null;
      Response              response   = null;
      List<ResponseHandler> onSuccess  = new ArrayList();
      List<ResponseHandler> onFailure  = new ArrayList();
      List<ResponseHandler> onResponse = new ArrayList();

      public FutureResponse()
      {

      }

      public FutureResponse(Request request)
      {
         this.request = request;
      }

      public FutureResponse onSuccess(ResponseHandler handler)
      {
         boolean done = false;
         synchronized (this)
         {
            done = isDone();
            if (!done)
            {
               onSuccess.add(handler);
            }
         }

         if (done && isSuccess())
         {
            try
            {
               handler.onResponse(response);
            }
            catch (Throwable ex)
            {
               log.error("Error handling onSuccess", ex);
            }
         }

         return this;
      }

      public FutureResponse onFailure(ResponseHandler handler)
      {
         boolean done = false;
         synchronized (this)
         {
            done = isDone();
            if (!done)
            {
               onFailure.add(handler);
            }
         }

         if (done && !isSuccess())
         {
            try
            {
               handler.onResponse(response);
            }
            catch (Throwable ex)
            {
               log.error("Error handling onFailure", ex);
            }
         }

         return this;
      }

      public FutureResponse onResponse(ResponseHandler handler)
      {
         boolean done = false;
         synchronized (this)
         {
            done = isDone();
            if (!done)
            {
               onResponse.add(handler);
            }
         }

         if (done)
         {
            try
            {
               handler.onResponse(response);
            }
            catch (Throwable ex)
            {
               log.error("Error handling onResponse", ex);
            }
         }

         return this;
      }

      public void setResponse(Response response)
      {
         synchronized (this)
         {
            this.response = response;

            if (isSuccess())
            {
               for (ResponseHandler h : onSuccess)
               {
                  try
                  {
                     h.onResponse(response);
                  }
                  catch (Throwable ex)
                  {
                     log.error("Error handling success callbacks in setResponse", ex);
                  }
               }
            }
            else
            {
               for (ResponseHandler h : onFailure)
               {
                  try
                  {
                     h.onResponse(response);
                  }
                  catch (Throwable ex)
                  {
                     log.error("Error handling failure callbacks in setResponse", ex);
                  }
               }
            }

            for (ResponseHandler h : onResponse)
            {
               try
               {
                  h.onResponse(response);
               }
               catch (Throwable ex)
               {
                  log.error("Error handling callbacks in setResponse", ex);
               }
            }

            notifyAll();
         }
      }

      @Override
      public boolean cancel(boolean arg0)
      {
         return false;
      }

      @Override
      public Response get()
      {
         if (SwingUtilities.isEventDispatchThread())
         {
            String msg = "Blocking on the Swing thread. Your code is blocking the UI by calling FutureResponse.get() on the Swing event dispatch thread.  You should consider moving your call into a background thread.";
            Exception ex = new Exception();
            ex.fillInStackTrace();
            log.warn(msg, ex);
         }

         while (response == null)
         {
            synchronized (this)
            {
               if (response == null)
               {
                  try
                  {
                     wait();
                  }
                  catch (Exception ex)
                  {

                  }
               }
            }
         }

         return response;
      }

      public boolean isSuccess()
      {
         if (response != null && response.getError() == null && response.getStatusCode() >= 200 && response.getStatusCode() < 300)
            return true;

         return false;
      }

      public Response get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
      {
         if (SwingUtilities.isEventDispatchThread())
         {
            String msg = "Blocking on the Swing thread. Your code is blocking the UI by calling FutureResponse.get() on the Swing event dispatch thread.  You should consider moving your call into a background thread.";
            Exception ex = new Exception();
            ex.fillInStackTrace();
            log.warn(msg, ex);
         }

         timeout = TimeUnit.MILLISECONDS.convert(timeout, unit);
         while (response == null)
         {
            synchronized (this)
            {
               if (response == null)
               {
                  wait(timeout);
                  if (response == null)
                     throw new TimeoutException(timeout + " millisecond timeout reached");
               }
            }
         }

         return response;
      }

      public Request getRequest()
      {
         return request;
      }

      @Override
      public boolean isCancelled()
      {
         return false;
      }

      @Override
      public boolean isDone()
      {
         return response != null;
      }

      public long getCreatedAt()
      {
         return createdAt;
      }

   }

   public static interface ResponseHandler
   {
      public void onResponse(Response response) throws Exception;
   }

   /**
    * A thread pool executor that will expand the number of threads up to <code>poolMax</code>
    * with a queue depth of <code>queueMax</code>.  If more than <code>queueMax</code> jobs
    * are submitted, the submit() will block until the queue has room.
    */
   public static class Executor
   {
      int                        poolMin  = 1;
      int                        poolMax  = 3;

      long                       queueMax = Integer.MAX_VALUE;

      LinkedList<RunnableFuture> queue    = new LinkedList();
      Vector<Thread>             threads  = new Vector();

      boolean                    daemon   = true;

      String                     poolName = "Executor";

      boolean                    shutdown = false;
      long                       delay    = 1000;

      static Timer               timer    = null;

      public Executor(int poolMin, int poolMax, long queueMax)
      {
         this(poolMin, poolMax, queueMax, true, "Executor");
      }

      public Executor(int poolMin, int poolMax, long queueMax, boolean daemon, String poolName)
      {
         this.poolMin = Math.max(this.poolMin, poolMin);
         this.poolMax = poolMax;
         this.queueMax = queueMax;
         this.daemon = daemon;
         this.poolName = poolName;
      }

      public synchronized Future submit(final Runnable task)
      {
         return submit(new RunnableFuture()
            {
               boolean started  = false;
               boolean canceled = false;
               boolean done     = false;

               @Override
               public void run()
               {
                  try
                  {
                     if (canceled || done)
                        return;

                     started = true;
                     task.run();
                  }
                  finally
                  {
                     synchronized (this)
                     {
                        done = true;
                        notifyAll();
                     }
                  }
               }

               @Override
               public boolean cancel(boolean mayInterruptIfRunning)
               {
                  canceled = true;
                  return !started;
               }

               @Override
               public boolean isCancelled()
               {
                  return canceled;
               }

               @Override
               public boolean isDone()
               {
                  return false;
               }

               @Override
               public Object get() throws InterruptedException, ExecutionException
               {
                  synchronized (this)
                  {
                     while (!done)
                     {
                        wait();
                     }
                  }
                  return null;
               }

               @Override
               public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
               {
                  synchronized (this)
                  {
                     while (!done)
                     {
                        wait(unit.toMillis(timeout));
                     }
                  }
                  return null;
               }

            });
      }

      public synchronized RunnableFuture submit(RunnableFuture task)
      {
         put(task);
         checkStartThread();
         return task;
      }

      public synchronized RunnableFuture submit(final RunnableFuture task, long delay)
      {
         getTimer().schedule(new TimerTask()
            {
               @Override
               public void run()
               {
                  Thread t = new Thread(new Runnable()
                     {
                        @Override
                        public void run()
                        {
                           submit(task);
                        }
                     });
                  t.setDaemon(daemon);
                  t.start();
               }
            }, delay);

         return task;
      }

      static synchronized Timer getTimer()
      {
         if (timer == null)
            timer = new Timer("Executor timer");

         return timer;
      }

      synchronized boolean checkStartThread()
      {
         if (queue.size() > 0 && threads.size() < poolMax)
         {
            Thread t = new Thread(new Runnable()
               {
                  public void run()
                  {
                     processQueue();
                  }
               }, poolName + " worker");
            t.setDaemon(daemon);
            threads.add(t);
            t.start();
            return true;
         }
         return false;
      }

      synchronized boolean checkEndThread()
      {
         if (queue.size() == 0 && threads.size() > poolMin)
         {
            threads.remove(Thread.currentThread());
            return true;
         }
         return false;
      }

      int queued()
      {
         synchronized (queue)
         {
            return queue.size();
         }
      }

      void put(RunnableFuture task)
      {
         synchronized (queue)
         {
            while (queue.size() >= queueMax)
            {
               try
               {
                  queue.wait();
               }
               catch (Exception ex)
               {

               }
            }
            queue.add(task);
            queue.notifyAll();
         }
      }

      RunnableFuture take()
      {
         RunnableFuture t = null;
         synchronized (queue)
         {
            while (queue.size() == 0)
            {
               try
               {
                  queue.wait();
               }
               catch (InterruptedException ex)
               {

               }
            }

            t = queue.removeFirst();
            queue.notifyAll();
         }
         return t;
      }

      void processQueue()
      {
         try
         {
            while (true && !shutdown && !checkEndThread())
            {
               do
               {
                  RunnableFuture task = take();
                  task.run();
               }
               while (queue.size() > 0);

               //            if (!shutdown)
               //            {
               //               Thread.sleep(delay);
               //            }
               //
               //            if (queue.size() == 0)
               //               break;
            }
         }
         catch (Exception ex)
         {
            ex.printStackTrace();
         }
      }

   }

}
