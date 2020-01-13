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

package io.inversion.cloud.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

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

import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.model.Url;
import io.inversion.cloud.service.Chain;

/**
 * An HttpClient wrapper designed specifically to run inside of an 
 * Inversion Action that adds async and retry support and make it 
 * easy to proxy in-bound params and headers on to other services 
 * 
 * Designed to have host "url" set manually or discovered at runtime
 * out of the environment as "${name}.url=http://somehost.com"
 * 
 * This file was forked from the Inversion HttpUtils class to 
 * give authors options in tuning the underlying Apache HttpClient
 * configuration and thread pooling.
 * 
 * @author Wells Burke
 */
public class RestClient
{
   static Log                                       log             = LogFactory.getLog(RestClient.class);

   //-- config properties
   protected String                                 name            = null;
   protected String                                 url             = null;
   protected boolean                                remoteAsync     = true;
   protected boolean                                localAsync      = false;
   protected ArrayListValuedHashMap<String, String> forcedHeaders   = new ArrayListValuedHashMap();
   protected boolean                                forwardHeaders  = false;
   protected boolean                                forwardParams   = false;

   //-- networking and pooling specific properties
   protected Executor                               pool            = null;
   protected int                                    poolMin         = 2;
   protected int                                    poolMax         = 100;
   protected int                                    queueMax        = 500;
   protected int                                    retryMax        = 5;
   protected int                                    totalRetryMax   = 50;

   protected int                                    retryTimeoutMin = 10;
   protected int                                    retryTimeoutMax = 1000;

   protected int                                    socketTimeout   = 1000;
   protected int                                    connectTimeout  = 1000;
   protected int                                    requestTimeout  = 5000;

   protected HttpClient                             httpClient      = null;

   protected Timer                                  timer           = null;

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
      String url = getUrl();
      if (url != null && path != null)
      {
         url += (url.endsWith("/") ? "" : "/") + Utils.implode("/", Utils.explode("/", path));
      }
      else if (url == null && path != null)
      {
         url = path;
      }

      FutureResponse future = buildFuture(method, url, params, (body != null ? body.toString() : null), headers, retries);
      boolean localCall = future.isLocalRequest();

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

   protected FutureResponse buildFuture(String method, String url, Map<String, String> callParams, String body, ArrayListValuedHashMap<String, String> callHeaders, int retryAttempts)
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
                  if (request.getHeader(key) == null)
                     for (String header : inboundHeaders.get(key))
                        request.withHeader(key, header);
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
            Map<String, String> origionalParams = originalInboundRequest.getParams();
            if (origionalParams.size() > 0)
            {
               for (String key : origionalParams.keySet())
               {
                  if (request.getParam(key) == null)
                     request.withParam(key, origionalParams.get(key));
               }
            }
         }
      }

      final FutureResponse future = new FutureResponse(request)
         {
            public void run()
            {
               Response response = null;
               try
               {
                  response = getResponse(this);
               }
               finally
               {
                  // We had a successful response, so let's reset the retry count to give the best chance of success
                  if (response.getStatusCode() >= 200 && response.getStatusCode() <= 300)
                  {
                     debug("Resetting retry count");
                     resetRetryCount();
                  }

                  if (!response.isSuccess() //
                        && 404 != response.getStatusCode() //don't retry 404...it won't help
                        && getRetryCount() < request.getRetryAttempts() //
                        && getTotalRetries() < totalRetryMax) // since we resetRetryCount upon any successful response, this guards against a crazy large amount of retries with the TOTAL_MAX_RETRY_ATTEMPTS
                  {
                     incrementRetryCount();

                     long timeout = computeTimeout(this);
                     debug("retrying " + getTotalRetries() + "th attempt in " + timeout + "ms: " + getRetryCount());
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

   protected Response getResponse(FutureResponse future)
   {
      String url = future.request.getUrl().toString();
      Response response = new Response(url);

      try
      {
         //this is a local call...send it back to the engine
         if (future.isLocalRequest())
            return getLocalResponse(future);
         else
            return getRemoteResponse(future);
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

   protected Response getLocalResponse(FutureResponse future) throws Exception
   {
      String url = future.request.getUrl().toString();
      Response response = new Response(url);

      future.request.withEngine(future.chain.getEngine());
      future.chain.getEngine().service(future.request, response);
      return response;

   }

   protected Response getRemoteResponse(FutureResponse future) throws Exception
   {
      String m = future.request.getMethod();
      HttpRequestBase req = null;
      File tempFile = null;

      String url = future.request.getUrl().toString();
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

            if (future.getRetryFile() != null && future.getRetryFile().length() > 0)
            {
               long range = future.getRetryFile().length();
               future.request.getHeaders().remove("Range");
               future.request.getHeaders().put("Range", "bytes=" + range + "-");

               debug("RANGE REQUEST HEADER ** " + range);
            }
         }
         else if ("delete".equalsIgnoreCase(m))
         {
            if (future.request.getBody() != null)
            {
               req = new HttpDeleteWithBody(url);
            }
            else
            {
               req = new HttpDelete(url);
            }
         }

         for (String key : future.request.getHeaders().keySet())
         {
            List<String> values = future.request.getHeaders().get(key);
            for (String value : values)
            {
               req.setHeader(key, value);
               response.debug(key, value);
            }
         }
         if (future.request.getBody() != null && req instanceof HttpEntityEnclosingRequestBase)
         {
            response.debug("\r\n--request body--------");
            ((HttpEntityEnclosingRequestBase) req).setEntity(new StringEntity(future.request.getBody(), "UTF-8"));
         }

         RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(socketTimeout).setConnectTimeout(connectTimeout).setConnectionRequestTimeout(requestTimeout).build();
         req.setConfig(requestConfig);

         hr = h.execute(req);

         HttpEntity e = hr.getEntity();

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

         InputStream is = e.getContent();

         Url u = new Url(url);
         String fileName = u.getFile();
         if (fileName == null)
            fileName = Utils.slugify(u.toString());

         boolean skip = false;
         // if we have a retry file and it's length matches the Content-Range header's start and the Content-Range header's unit's are bytes use the existing file
         if (response.getStatusCode() == 404)
         {
            skip = true;
         }
         else if (future.getRetryFile() != null //
               && future.getRetryFile().length() == response.getContentRangeStart() //
               && "bytes".equalsIgnoreCase(response.getContentRangeUnit()))
         {
            tempFile = future.getRetryFile();
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

         if (!skip)
         {
            // stream to the temp file with append set to true (this is crucial for resumable downloads)
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

   public boolean isNetworkException(Exception ex)
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

   protected long computeTimeout(FutureResponse future)
   {
      long timeout = (retryTimeoutMin * future.getRetryCount() * future.getRetryCount()) + (int) (retryTimeoutMin * Math.random() * future.getRetryCount());
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

   public static class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase
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

   public String getUrl()
   {
      String key = getName() + ".url";
      return Utils.findSysEnvPropStr(key, url);
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

   public RestClient withForwardHeaders(boolean forwardHeaders)
   {
      this.forwardHeaders = forwardHeaders;
      return this;
   }

   public boolean isForwardParams()
   {
      return forwardParams;
   }

   public RestClient withForwardParams(boolean forwardParams)
   {
      this.forwardParams = forwardParams;
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

}
