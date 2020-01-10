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
import java.util.List;
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
 * This file was forked from the Inversion HttpUtils class to 
 * give authors options in tuning the underlying Apache HttpClient
 * configuration and thread pooling.
 * 
 * @author Wells Burke
 */
public class RestClient
{
   static Log                                       log            = LogFactory.getLog(RestClient.class);

   //-- endpoing properties
   protected String                                 name           = null;
   protected String                                 version        = "1";
   protected String                                 url            = null;
   protected ArrayListValuedHashMap<String, String> forcedHeaders  = new ArrayListValuedHashMap();
   protected boolean                                forwardHeaders = false;

   //-- networking and pooling specific properties
   protected Executor                               pool           = null;
   protected int                                    poolMin        = 2;
   protected int                                    poolMax        = 100;
   protected int                                    queueMax       = 500;
   protected int                                    retryMax       = 5;
   protected int                                    totalRetryMax  = 50;

   protected int                                    socketTimeout  = 30000;
   protected int                                    connectTimeout = 30000;
   protected int                                    requestTimeout = 30000;

   protected HttpClient                             httpClient     = null;

   protected Timer                                  timer          = null;

   public static ArrayListValuedHashMap asHeaderMap(String... keyValueList)
   {
      ArrayListValuedHashMap headers = new ArrayListValuedHashMap();
      addToHeaderMap(headers, keyValueList);
      return headers;
   }

   public static void addToHeaderMap(ArrayListValuedHashMap headers, String... keyValueList)
   {
      for (int i = 0; i < keyValueList.length - 1; i += 2)
         headers.put(keyValueList[i], keyValueList[i + 1]);
   }

   public RestClient()
   {

   }

   public RestClient(String name)
   {
      withName(name);
   }

   public RestClient(String name, String version)
   {
      withName(name);
      withVersion(version);
   }

   public FutureResponse call(String method, String path, String query, JSNode body, int retries)
   {
      return call(method, path, query, body, retries, null);
   }

   public FutureResponse call(String method, String path, String query, JSNode body, int retries, ArrayListValuedHashMap<String, String> headers)
   {
      String url = getUrl();
      if (path != null)
      {
         url += (url.endsWith("/") ? "" : "/") + Utils.implode("/", Utils.explode("/", path));
      }

      if (query != null)
      {
         url += (url.indexOf("?") < 0 ? "?" : "&") + query;
      }

      final String fullUrl = url;

      if (!fullUrl.startsWith("http"))
      {
         //TODO: this internal 'shortcut' is not actually multi threaded
         FutureResponse future = new FutureResponse()
            {
               @Override
               public void run()
               {
                  Response response = Chain.peek().getEngine().service(method, fullUrl, body == null ? null : body.toString());
                  setResponse(response);
               }
            };
         future.run();
         return future;
      }
      else
      {
         FutureResponse future = buildFuture(method, url, (body != null ? body.toString() : null), headers, retries);
         submit(future);
         return future;
      }
   }

   public FutureResponse get(String path, String query)
   {
      return call("GET", path, query, null, -1);
   }

   public FutureResponse put(String path, JSNode body)
   {
      return call("PUT", path, null, body, -1);
   }

   public FutureResponse post(String path, JSNode body)
   {
      return call("POST", path, null, body, -1);
   }

   public FutureResponse delete(String path)
   {
      return call("DELETE", path, null, null, -1);
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

   public String getName()
   {
      return name;
   }

   public RestClient withName(String name)
   {
      this.name = name;
      return this;
   }

   public RestClient withVersion(String version)
   {
      this.version = version;
      return this;
   }

   public String getVersion()
   {
      return version;
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

   protected FutureResponse buildFuture(String method, String url, String body, ArrayListValuedHashMap<String, String> callHeaders, int retryAttempts)
   {
      //we are going to change outboundHeaders so make a copy.
      callHeaders = callHeaders != null ? new ArrayListValuedHashMap(callHeaders) : new ArrayListValuedHashMap();

      if (forcedHeaders.size() > 0)
      {
         for (String key : forcedHeaders.keySet())
         {
            callHeaders.remove(key);
            callHeaders.putAll(key, forcedHeaders.get(key));
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
                  if (!callHeaders.containsKey(key))
                     callHeaders.putAll(key, inboundHeaders.get(key));
               }
            }
         }
      }

      Request request = new Request(method, url, body, callHeaders, retryAttempts);

      final FutureResponse future = new FutureResponse(request)
         {
            public void run()
            {
               doCall(this);
            }
         };

      return future;

   }

   protected void doCall(FutureResponse future)
   {
      String m = future.request.getMethod();
      String url = future.request.getUrl().toString();

      ArrayListValuedHashMap<String, String> outboundHeaders = new ArrayListValuedHashMap();

      if (future.request.getHeaders() != null)
      {
         outboundHeaders.putAll(future.request.getHeaders());
      }

      boolean retryable = true;

      Response response = new Response(url);
      HttpRequestBase req = null;
      File tempFile = null;

      try
      {
         int timeout = 30000;

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
               outboundHeaders.remove("Range");
               outboundHeaders.put("Range", "bytes=" + range + "-");

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

         for (String key : outboundHeaders.keySet())
         {
            List<String> values = outboundHeaders.get(key);
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

         RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(timeout).setConnectTimeout(timeout).setConnectionRequestTimeout(timeout).build();
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

         // We had a successful response, so let's reset the retry count to give the best chance of success
         if (response.getStatusCode() >= 200 && response.getStatusCode() <= 300)
         {
            debug("Resetting retry count");
            future.resetRetryCount();
         }

         Url u = new Url(url);
         String fileName = u.getFile();
         if (fileName == null)
            fileName = Utils.slugify(u.toString());

         // if we have a retry file and it's length matches the Content-Range header's start and the Content-Range header's unit's are bytes use the existing file
         if (response.getStatusCode() == 404)
         {
            retryable = false; // do not allow this to retry on a 404
            return; //will go to finally block
         }
         else if (future.getRetryFile() != null && future.getRetryFile().length() == response.getContentRangeStart() && "bytes".equalsIgnoreCase(response.getContentRangeUnit()))
         {
            tempFile = future.getRetryFile();
            debug("## Using existing file .. " + tempFile);
         }
         else if (response.getStatusCode() == 206)
         {
            // status code is 206 Partial Content, but we don't want to use the existing file for some reason, so abort this and force it to fail
            retryable = false; // do not allow this to retry
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

         // stream to the temp file with append set to true (this is crucial for resumable downloads)
         Utils.pipe(is, new FileOutputStream(tempFile, true));

         response.withFile(tempFile);

         if (response.getContentRangeSize() > 0 && tempFile.length() > response.getContentRangeSize())
         {
            // Something is wrong.. The server is saying the file should be X, but the actual file is larger than X, abort this
            retryable = false; // do not allow this to retry
            throw new Exception("Downloaded file is larger than the server says it should be, aborting this request");
         }

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

         // If this is a retryable response, submit it later
         // Since we resetRetryCount upon any successful response, we are still guarding against a crazy large amount of retries with the TOTAL_MAX_RETRY_ATTEMPTS
         if (retryable && future.getRetryCount() < future.request.getRetryAttempts() && !response.isSuccess() && future.getTotalRetries() < totalRetryMax)
         {
            future.incrementRetryCount();

            long timeout = (1000 * future.getRetryCount() * future.getRetryCount()) + (int) (1000 * Math.random() * future.getRetryCount());

            debug("retrying: " + future.getRetryCount() + " - " + timeout + " - " + url);

            // Set this for possible resumable download on the next try
            if (future.getRetryFile() == null && response.getStatusCode() == 200)
            {
               future.setRetryFile(response.getFile());
            }

            submitLater(future, timeout);
            return;
         }
         else
         {
            if (!response.isSuccess() && response.getError() != null && !(isNetworkException(response.getError())))
            {
               log.warn("Error in Web.rest() . " + m + " : " + url, response.getError());
            }

            future.setResponse(response);
         }
      }
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

}
