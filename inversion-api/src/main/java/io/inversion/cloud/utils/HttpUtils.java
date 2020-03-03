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

import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.model.Url;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
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
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 
 * @author Wells Burke
 *
 */
public class HttpUtils
{
   static Log       log                      = LogFactory.getLog(HttpUtils.class);

   static final int DEFAULT_TIMEOUT          = 30000;

   static final int POOL_MIN                 = 2;
   static final int POOL_MAX                 = 100;
   static final int QUEUE_MAX                = 500;
   static final int DEFAULT_RETRY_ATTEMPTS   = 5;
   static final int TOTAL_MAX_RETRY_ATTEMPTS = 50;

   static Executor  pool                     = null;
   static Timer     timer                    = null;

   public static FutureResponse get(String url)
   {
      return rest("GET", url, null, null, -1);
   }

   public static FutureResponse put(String url, String body)
   {
      return rest("PUT", url, body, null, -1);
   }

   public static FutureResponse post(String url, String body)
   {
      return rest("POST", url, body, null, -1);
   }

   public static FutureResponse delete(String url)
   {
      return rest("DELETE", url, null, null, -1);
   }

   public static FutureResponse rest(String method, String url, String body, int retryAttempts, String... headers)
   {
      ArrayListValuedHashMap h = new ArrayListValuedHashMap();
      for (int i = 0; i < headers.length - 1; i += 2)
      {
         h.put(headers[i], headers[i + 1]);
      }

      return rest(method, url, body, h, retryAttempts);
   }

   public static FutureResponse rest(String method, String url, String body, ArrayListValuedHashMap<String, String> headers, int retryAttempts)
   {
      return rest(new Request(method, url, body, headers, retryAttempts));
   }

   public static FutureResponse rest(final Request request)
   {
      final FutureResponse future = new FutureResponse()
         {
            public void run()
            {
               String m = request.getMethod();
               String url = request.getUrl().toString();
               ArrayListValuedHashMap<String, String> headers = request.getHeaders();

               if (headers == null)
               {
                  headers = new ArrayListValuedHashMap();
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

                     if (this.getRetryFile() != null && this.getRetryFile().length() > 0)
                     {
                        long range = this.getRetryFile().length();
                        headers.remove("Range");
                        headers.put("Range", "bytes=" + range + "-");

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

                  for (String key : headers.keySet())
                  {
                     List<String> values = headers.get(key);
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
                     this.resetRetryCount();
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
                  else if (this.getRetryFile() != null && this.getRetryFile().length() == response.getContentRangeStart() && "bytes".equalsIgnoreCase(response.getContentRangeUnit()))
                  {
                     tempFile = this.getRetryFile();
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
                  if (retryable && this.getRetryCount() < request.getRetryAttempts() && !response.isSuccess() && this.getTotalRetries() < TOTAL_MAX_RETRY_ATTEMPTS)
                  {
                     this.incrementRetryCount();

                     long timeout = (1000 * this.getRetryCount() * this.getRetryCount()) + (int) (1000 * Math.random() * this.getRetryCount());

                     debug("retrying: " + this.getRetryCount() + " - " + timeout + " - " + url);

                     // Set this for possible resumable download on the next try
                     if (this.getRetryFile() == null && response.getStatusCode() == 200)
                     {
                        this.setRetryFile(response.getFile());
                     }

                     submitLater(this, timeout);
                     return;
                  }
                  else
                  {
                     if (!response.isSuccess() && response.getError() != null && !(isNetworkException(response.getError())))
                     {
                        log.warn("Error in Web.rest() . " + m + " : " + url, response.getError());
                     }

                     setResponse(response);
                  }
               }
            }
         };

      submit(future);
      return future;

   }

   public static boolean isNetworkException(Throwable ex)
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

   static synchronized void submit(FutureResponse future)
   {
      if (pool == null)
         pool = new Executor(POOL_MIN, POOL_MAX, QUEUE_MAX);

      pool.submit(future);
   }

   static synchronized void submitLater(final FutureResponse future, long delay)
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
   public static synchronized HttpClient getHttpClient() throws Exception
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

      RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(DEFAULT_TIMEOUT).setConnectTimeout(DEFAULT_TIMEOUT).setConnectionRequestTimeout(DEFAULT_TIMEOUT).build();
      b.setDefaultRequestConfig(requestConfig);

      // finally, build the HttpClient;
      //      -- done!
      HttpClient client = b.build();

      return client;
   }

   private static void debug(Object obj)
   {
      if (log.isDebugEnabled())
      {
         log.debug(obj);
      }
   }

   public static interface ResponseHandler
   {
      public void onResponse(Response response) throws Exception;
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
