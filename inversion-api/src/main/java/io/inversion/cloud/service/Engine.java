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
package io.inversion.cloud.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;

import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inversion.cloud.model.Action;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.ApiException;
import io.inversion.cloud.model.ApiListener;
import io.inversion.cloud.model.Collection;
import io.inversion.cloud.model.Db;
import io.inversion.cloud.model.Endpoint;
import io.inversion.cloud.model.EngineListener;
import io.inversion.cloud.model.JSArray;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Path;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.model.Rule;
import io.inversion.cloud.model.Status;
import io.inversion.cloud.model.Url;
import io.inversion.cloud.service.Chain.ActionMatch;
import io.inversion.cloud.utils.Configurator;
import io.inversion.cloud.utils.Utils;

public class Engine extends Rule<Engine>
{
   protected transient volatile boolean     started        = false;
   protected transient volatile boolean     starting       = false;
   protected transient volatile boolean     destroyed      = false;

   protected final Logger                   log            = LoggerFactory.getLogger(getClass());
   protected final Logger                   requestLog     = LoggerFactory.getLogger(getClass() + ".requests");

   protected List<Api>                      apis           = new Vector();

   protected ResourceLoader                 resourceLoader = null;

   protected Configurator                   configurator   = new Configurator();

   /**
    * The runtime profile that will be used to load inversion[1-99]-$profile.properties files.
    * This is used so that you can ship common settings in inversion[1-99].properties files
    * that are loaded for all profiles and put custom settings in dev/stage/prod (for example)
    * profile specific settings files.
    */
   protected String                         profile        = null;

   /**
    * The path to inversion*.properties files
    */
   protected String                         configPath     = "";

   /**
    * The number of milliseconds between background reloads of the Api config
    */
   protected int                            configTimeout  = 10000;

   /**
    * Indicates that the supplied config files contain all the setup info and the Api
    * will not be reflectively configured as it otherwise would.
    */
   protected boolean                        configFast     = false;
   protected boolean                        configDebug    = false;
   protected String                         configOut      = null;

   /**
    * The last response returned.  Not that useful in concurrent 
    * production environments but useful for writing test cases.
    */
   protected transient volatile Response    lastResponse   = null;

   protected transient List<EngineListener> listeners      = new ArrayList();

   /**
    * Engine reflects all request headers along with those supplied in <code>allowHeaders</code> as 
    * "Access-Control-Allow-Headers" response headers.  This is primarily a CROS security thing and you
    * probably won't need to customize this list. 
    */
   protected String                         allowedHeaders = "accept,accept-encoding,accept-language,access-control-request-headers,access-control-request-method,authorization,connection,Content-Type,host,user-agent,x-auth-token";

   public Engine()
   {

   }

   public Engine(Api... apis)
   {
      if (apis != null)
         for (Api api : apis)
            withApi(api);
   }

   public void destroy()
   {
      destroyed = true;
   }

   /*
    * Designed to be overridden of subclasses to lazy load/configure
    * the service.
    */
   protected void startup0()
   {

   }

   public synchronized Engine startup()
   {
      if (started || starting) //initing is an accidental recursion guard
         return this;

      starting = true;
      try
      {
         startup0();

         //this will cause withApi and we don't want that to actually startup
         //-- the API until
         configurator.loadConfig(this);

         started = true;

         for (Api api : apis)
         {
            startupApi(api);
         }

         //todo need to include api version info here.
         //-- the following block is only debug output
         for (Api api : apis)
         {
            System.out.println(api.getName() + "--------------");

            for (Endpoint e : api.getEndpoints())
            {
               //System.out.println("  - ENDPOINT:   " + (!Utils.empty(e.getName()) ? ("name:" + e.getName() + " ") : "") + "path:" + e.getPath() + " includes:" + e.getIncludePaths() + " excludes:" + e.getExcludePaths());
            }

            List<String> strs = new ArrayList();
            for (Collection c : api.getCollections())
            {
               if (c.getDb() != null && c.getDb().getEndpointPath() != null)
                  strs.add(c.getDb().getEndpointPath() + c.getName());
               else
                  strs.add(c.getName());
            }
            Collections.sort(strs);
            for (String coll : strs)
            {
               System.out.println("  - COLLECTION: " + coll);
            }
         }
         //-- end debug output

         return this;
      }
      finally
      {
         starting = false;
      }
   }

   public boolean isStarted()
   {
      return started;
   }

   public void shutdown()
   {
      for (Api api : getApis())
      {
         removeApi(api);
      }

      for (EngineListener listener : listeners)
      {
         try
         {
            listener.onShutdown(this);
         }
         catch (Exception ex)
         {
            ex.printStackTrace();
         }
      }

      Chain.resetAll();
   }

   public Engine withEngineListener(EngineListener listener)
   {
      if (!listeners.contains(listener))
         listeners.add(listener);
      return this;
   }

   public Response get(String url)
   {
      return service("GET", url, (String) null);
   }

   public Response get(String url, Map<String, String> params)
   {
      return service("GET", url, null, params);
   }

   public Response get(String url, List queryTerms)
   {
      if (queryTerms != null && queryTerms.size() > 0)
      {
         Map<String, String> params = new HashMap();
         queryTerms.forEach(key -> params.put(key.toString(), null));
         return service("GET", url, null, params);
      }
      else
      {
         return service("GET", url, null, null);
      }
   }

   public Response put(String url, Object body)
   {
      return service("PUT", url, (body != null ? body.toString() : null));
   }

   public Response post(String url, Object body)
   {
      return service("POST", url, (body != null ? body.toString() : null));
   }

   public Response put(String url, JSNode body)
   {
      return service("PUT", url, body.toString());
   }

   public Response post(String url, JSNode body)
   {
      return service("POST", url, body.toString());
   }

   public Response put(String url, String body)
   {
      return service("PUT", url, body);
   }

   public Response post(String url, String body)
   {
      return service("POST", url, body);
   }

   public Response delete(String url)
   {
      return service("DELETE", url, (String) null);
   }

   public Response delete(String url, JSArray hrefs)
   {
      return service("DELETE", url, hrefs.toString());
   }

   public Response service(String method, String url)
   {
      return service(method, url, null);
   }

   public Request request(String method, String url, String body)
   {
      Request req = new Request(method, url, body);
      req.withEngine(this);

      return req;
   }

   /**
    * @return the last response serviced by this Engine.
    */
   public Response response()
   {
      return lastResponse;
   }

   public Response service(String method, String url, String body)
   {
      return service(method, url, body, null);
   }

   public Response service(String method, String url, String body, Map<String, String> params)
   {
      Request req = new Request(method, url, body);
      req.withEngine(this);

      if (params != null)
      {
         for (String key : params.keySet())
         {
            req.getUrl().withParam(key, params.get(key));
         }
      }

      Response res = new Response();

      service(req, res);
      return res;
   }

   public Response forward(String method, String url)
   {
      Request req = new Request(method, url, null);
      req.withEngine(this);

      Response res = Chain.peek().getResponse();

      service(req, res);
      return res;
   }

   public Chain service(Request req, Response res)
   {
      if (!started)
         startup();

      Chain chain = null;

      try
      {
         chain = Chain.push(this, req, res);
         req.withEngine(this);
         req.withChain(chain);
         res.withChain(chain);

         //--
         //-- CORS header setup
         //--
         String allowedHeaders = new String(this.allowedHeaders);
         String corsRequestHeader = req.getHeader("Access-Control-Request-Header");
         if (corsRequestHeader != null)
         {
            List<String> headers = Arrays.asList(corsRequestHeader.split(","));
            for (String h : headers)
            {
               h = h.trim();
               allowedHeaders = allowedHeaders.concat(h).concat(",");
            }
         }
         res.withHeader("Access-Control-Allow-Origin", "*");
         res.withHeader("Access-Control-Allow-Credentials", "true");
         res.withHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE");
         res.withHeader("Access-Control-Allow-Headers", allowedHeaders);

         //--
         //-- End CORS Header Setup

         if (req.isMethod("options"))
         {
            //this is a CORS preflight request. All of hte work was done bove
            res.withStatus(Status.SC_200_OK);
            return chain;
         }

         Url url = req.getUrl();

         if (url.toString().indexOf("/favicon.ico") >= 0)
         {
            res.withStatus(Status.SC_404_NOT_FOUND);
            return chain;
         }

         String xfp = req.getHeader("X-Forwarded-Proto");
         String xfh = req.getHeader("X-Forwarded-Host");
         if (xfp != null || xfh != null)
         {
            if (xfp != null)
               url.withProtocol(xfp);

            if (xfh != null)
               url.withHost(xfh);
         }

         Path parts = new Path(url.getPath());
         String method = req.getMethod();

         Map<String, String> pathParams = new HashMap();

         Path containerPath = match(method, parts);

         if (containerPath == null)
            ApiException.throw400BadRequest("Somehow a request was routed to your Engine with an unsupported containerPath. This is a configuration error.");

         if (containerPath != null)
            containerPath = containerPath.extract(pathParams, parts);

         Path afterContainerPath = new Path(parts);
         Path afterApiPath = null;
         Path afterEndpointPath = null;

         for (Api api : apis)
         {
            Path apiPath = api.match(method, parts);

            if (apiPath != null)
            {
               apiPath = apiPath.extract(pathParams, parts);
               req.withApi(api, apiPath);

               afterApiPath = new Path(parts);

               for (Endpoint endpoint : api.getEndpoints())
               {
                  if (Chain.getDepth() < 2 && endpoint.isInternal())
                     continue;

                  Path endpointPath = endpoint.match(req.getMethod(), parts);

                  if (endpointPath != null)
                  {
                     endpointPath = endpointPath.extract(pathParams, parts);
                     req.withEndpoint(endpoint, endpointPath);

                     afterEndpointPath = new Path(parts);

                     for (Collection collection : api.getCollections())
                     {
                        Db<Db> db = collection.getDb();
                        if (db != null && db.getEndpointPath() != null && !db.getEndpointPath().matches(endpointPath))
                           continue;

                        Path collectionPath = collection.match(method, parts);
                        if (collectionPath != null)
                        {
                           collectionPath = collectionPath.extract(pathParams, parts, true);
                           req.withCollection(collection, collectionPath);

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

         if (req.getEndpoint() == null || req.isDebug())
         {
            res.debug("");
            res.debug("");
            res.debug(">> request --------------");
            res.debug(req.getMethod() + ": " + url);

            ArrayListValuedHashMap<String, String> headers = req.getHeaders();
            for (String key : headers.keys())
            {
               res.debug(key + " " + Utils.implode(",", headers.get(key)));
            }
            res.debug("");
         }

         if (req.getApi() == null)
         {
            ApiException.throw400BadRequest("No API found matching URL: '%s'", url);
         }

         //         if (req.getEndpoint() == null)
         //         {
         //            //check to see if a non plural version of the collection endpoint 
         //            //was passed in, if it was redirect to the plural version
         //            if (redirectPlural(req, res))
         //               return chain;
         //         }

         if (req.getEndpoint() == null)
         {
            String buff = "";
            for (Endpoint e : req.getApi().getEndpoints())
               buff += e.getMethods() + " path: " + e.getIncludePaths() + " : includePaths:" + e.getIncludePaths() + ": excludePaths" + e.getExcludePaths() + ",  ";

            ApiException.throw404NotFound("No Endpoint found matching '%s' : '%s' Valid endpoints include %s", req.getMethod(), url, buff);
         }

         //this will get all actions specifically configured on the endpoint
         List<ActionMatch> actions = new ArrayList();

         for (Action action : req.getEndpoint().getActions())
         {
            Path actionPath = action.match(method, afterEndpointPath);
            if (actionPath != null)
            {
               actions.add(new ActionMatch(actionPath, new Path(afterEndpointPath), action));
            }
         }

         //this matches for actions that can run across multiple endpoints.
         //this might be something like an authorization or logging action
         //that acts like a filter
         for (Action action : req.getApi().getActions())
         {
            Path actionPath = action.match(method, afterApiPath);
            if (actionPath != null)
            {
               actions.add(new ActionMatch(actionPath, new Path(afterApiPath), action));
            }
         }

         if (actions.size() == 0)
            ApiException.throw404NotFound("No Actions are configured to handle your request.  Check your server configuration.");

         Collections.sort(actions);

         //-- appends info to chain.debug that can be used for debugging an d
         //-- for test cases to validate what actually ran
         if (req.isDebug())
         {
            Chain.debug("Endpoint: " + req.getEndpoint());
            Chain.debug("Actions: " + actions);
         }

         run(chain, actions);

         Exception listenerEx = null;
         for (ApiListener listener : getApiListeners(req))
         {
            try
            {
               listener.afterRequest(req, res);
            }
            catch (Exception ex)
            {
               if (listenerEx == null)
                  listenerEx = ex;
            }
         }
         if (listenerEx != null)
            throw listenerEx;

         return chain;
      }
      catch (Throwable ex)
      {
         String status = Status.SC_500_INTERNAL_SERVER_ERROR;

         if (ex instanceof ApiException)
         {
            if (req != null && req.isDebug() && ((ApiException) ex).getStatus().startsWith("5"))
            {
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
         }
         else
         {
            ex = Utils.getCause(ex);
            if (Chain.getDepth() == 1)
               log.error("Non ApiException was caught in Engine.", ex);
         }

         res.withStatus(status);
         String message = ex.getMessage();
         JSNode response = new JSNode("message", message);
         if (Status.SC_500_INTERNAL_SERVER_ERROR.equals(status))
            response.put("error", Utils.getShortCause(ex));

         res.withError(ex);
         res.withJson(response);

         for (ApiListener listener : getApiListeners(req))
         {
            try
            {
               listener.afterError(req, res);
            }
            catch (Exception ex2)
            {
               log.warn("Error notifying EngineListner.beforeError", ex);
            }

         }

      }
      finally
      {
         for (ApiListener listener : getApiListeners(req))
         {
            try
            {
               listener.beforeFinally(req, res);
            }
            catch (Exception ex)
            {
               log.warn("Error notifying EngineListner.onFinally", ex);
            }
         }

         try
         {
            writeResponse(req, res);
         }
         catch (Throwable ex)
         {
            log.error("Error writing response.", ex);
         }

         Chain.pop();
         lastResponse = res;
      }

      return chain;
   }

   public static void applyPathParams(Map<String, String> pathParams, Url url, JSNode json)
   {
      pathParams.keySet().forEach(param -> url.clearParams(param));
      pathParams.keySet().forEach(param -> {
         if (pathParams.get(param) != null)
         {
            url.withParam(param, pathParams.get(param));
         }
      });

      if (json != null)
      {
         json.asList().forEach(n -> {
            if (n instanceof JSNode && !((JSNode) n).isArray())
            {
               pathParams.keySet().forEach(param -> {
                  if (pathParams.get(param) != null)
                  {
                     ((JSNode) n).put(param, pathParams.get(param));
                  }
               });
            }
         });
      }
   }

   LinkedHashSet<ApiListener> getApiListeners(Request req)
   {
      LinkedHashSet listeners = new LinkedHashSet();
      if (req.getApi() != null)
      {
         listeners.addAll(req.getApi().getApiListeners());
      }
      listeners.addAll(this.listeners);

      return listeners;
   }

   /**
    * This is specifically pulled out so you can mock Engine invocations
    * in test cases.
    * 
    * @param chain
    * @param actions
    * @throws Exception
    */
   protected void run(Chain chain, List<ActionMatch> actions) throws Exception
   {
      chain.withActions(actions).go();
   }

   protected void writeResponse(Request req, Response res) throws Exception
   {
      boolean debug = req != null && req.isDebug();
      boolean explain = req != null && req.isExplain();

      String method = req != null ? req.getMethod() : null;

      if ("OPTIONS".equals(method))
      {
         //
      }
      else
      {
         if (debug)
         {
            res.debug("\r\n<< response -------------\r\n");
            res.debug(res.getStatusCode());
         }

         String output = res.getText();
         if (output != null)
         {
            if (res.getContentType() == null)
            {
               if (output.indexOf("<html") > -1)
                  res.withContentType("text/html");
               else
                  res.withContentType("text/text");
            }
         }
         else if (!Utils.empty(res.getRedirect()))
         {
            res.withHeader("Location", res.getRedirect());
            res.withStatus(Status.SC_308_PERMANENT_REDIRECT);
         }
         else if (output == null && res.getJson() != null)
         {
            output = res.getJson().toString();

            if (res.getContentType() == null)
               res.withContentType("application/json");
         }

         if (debug)
         {
            for (String key : res.getHeaders().keySet())
            {
               List values = res.getHeaders().get(key);
               StringBuffer buff = new StringBuffer();
               for (int i = 0; i < values.size(); i++)
               {
                  buff.append(values.get(i));
                  if (i < values.size() - 1)
                     buff.append(",");
               }
               res.debug(key + " " + buff);
            }

            res.debug("\r\n-- done -----------------\r\n");
         }

         res.out(output);

         if (explain)
         {
            res.withOutput(res.getDebug());
         }
      }
   }

   //   boolean redirectPlural(Request req, Response res)
   //   {
   //      String collection = req.getCollectionKey();
   //      if (!Utils.empty(collection))
   //      {
   //         String plural = Pluralizer.plural(collection);
   //         if (!plural.equals(collection))
   //         {
   //            String path = req.getPath().toString();
   //            path = path.replaceFirst(collection, plural);
   //            Endpoint rightEndpoint = findEndpoint(req.getApi(), req.getMethod(), path);
   //            if (rightEndpoint != null)
   //            {
   //               String redirect = req.getUrl().toString();
   //               //redirect = req.getHttpServletRequest().getRequest
   //               redirect = redirect.replaceFirst("\\/" + collection, "\\/" + plural);
   //
   //               res.withRedirect(redirect);
   //               return true;
   //            }
   //         }
   //      }
   //      return false;
   //   }

   //   Endpoint findEndpoint(Api api, String method, String pathStr)
   //   {
   //      Path path = new Path(pathStr);
   //      for (Endpoint endpoint : api.getEndpoints())
   //      {
   //         if (endpoint.match(method, path) != null)
   //            return endpoint;
   //      }
   //      return null;
   //   }

   public List<Api> getApis()
   {
      return new ArrayList(apis);
   }

   /*
   Gets all apis of the same apiName
    */
   public synchronized List<Api> findApis(String apiName)
   {
      return apis.stream().filter(api -> apiName.equalsIgnoreCase(api.getName())).collect(Collectors.toList());
   }

   public synchronized Api getApi(String apiName)
   {
      //only one api will have a name version pair so return the first one.
      for (Api api : apis)
      {
         if (apiName.equalsIgnoreCase(api.getName()))
            return api;

      }
      return null;
   }

   public synchronized Engine withApi(Api api)
   {
      if (apis.contains(api))
         return this;

      List<Api> newList = new ArrayList(apis);

      Api existingApi = getApi(api.getName());
      if (existingApi != null && existingApi != api)
      {
         newList.remove(existingApi);
         newList.add(api);
      }
      else if (existingApi == null)
      {
         newList.add(api);
      }

      if (existingApi != api && isStarted())
         api.startup();

      apis = newList;

      if (existingApi != null && existingApi != api)
      {
         existingApi.shutdown();
      }

      return this;
   }

   protected void startupApi(Api api)
   {
      if (started)
      {
         try
         {
            api.startup();
         }
         catch (Exception ex)
         {
            log.warn("Error starting api '" + api.getName() + "'", ex);
         }

         for (EngineListener listener : listeners)
         {
            try
            {
               listener.onStartup(api);
            }
            catch (Exception ex)
            {
               log.warn("Error starting api '" + api.getName() + "'", ex);
            }
         }
      }
   }

   /**
    * Removes the api, notifies EngineListeners and calls api.shutdown()
    * @param api
    */
   public synchronized void removeApi(Api api)
   {
      List newList = new ArrayList(apis);
      newList.remove(api);
      apis = newList;

      shutdownApi(api);
   }

   protected void shutdownApi(Api api)
   {
      if (api.isStarted())
      {
         try
         {
            api.shutdown();
         }
         catch (Exception ex)
         {
            log.warn("Error shutting down api '" + api.getName() + "'", ex);
         }

         for (EngineListener listener : listeners)
         {
            try
            {
               listener.onShutdown(api);
            }
            catch (Exception ex)
            {
               log.warn("Error shutting down api '" + api.getName() + "'", ex);
            }
         }

      }
   }

   public String getProfile()
   {
      return Utils.getSysEnvPropStr("inversion.profile", profile);
   }

   public Engine withProfile(String profile)
   {
      this.profile = profile;
      return this;
   }

   public String getConfigPath()
   {
      return configPath;
   }

   public Engine withConfigPath(String configPath)
   {
      this.configPath = configPath;
      return this;
   }

   public Configurator getConfigurator()
   {
      return configurator;
   }

   public Engine withConfigurator(Configurator configurator)
   {
      this.configurator = configurator;
      return this;
   }

   //   public Path getServletMapping()
   //   {
   //      return containerPaths;
   //   }
   //
   //   public Engine withServletMapping(String servletMapping)
   //   {
   //      if (servletMapping != null)
   //         this.containerPaths = new Path(servletMapping);
   //      else
   //         this.containerPaths = null;
   //
   //      return this;
   //   }

   public Engine withAllowHeaders(String allowedHeaders)
   {
      this.allowedHeaders = allowedHeaders;
      return this;
   }

   public interface ResourceLoader
   {
      InputStream getResource(String name);
   }

   public ResourceLoader getResourceLoader()
   {
      return resourceLoader;
   }

   public Engine withResourceLoader(ResourceLoader resourceLoader)
   {
      this.resourceLoader = resourceLoader;
      return this;
   }

   public InputStream getResource(String name)
   {
      try
      {
         InputStream stream = null;
         if (resourceLoader != null)
            stream = resourceLoader.getResource(name);

         if (stream == null)
         {
            File file = new File(System.getProperty("user.dir"), name);
            if (file.exists())
               stream = new BufferedInputStream(new FileInputStream(file));
         }

         if (stream == null)
         {
            stream = getClass().getClassLoader().getResourceAsStream(name);
         }

         return stream;
      }
      catch (Exception ex)
      {
         throw new RuntimeException(ex);
      }
   }

   public boolean isConfigFast()
   {
      return configFast;
   }

   public Engine withConfigFast(boolean configFast)
   {
      this.configFast = configFast;
      return this;
   }

   public boolean isConfigDebug()
   {
      return configDebug;
   }

   public Engine withConfigDebug(boolean configDebug)
   {
      this.configDebug = configDebug;
      return this;
   }

   public String getConfigOut()
   {
      return configOut;
   }

   public Engine withConfigOut(String configOut)
   {
      this.configOut = configOut;
      return this;
   }

   public int getConfigTimeout()
   {
      return configTimeout;
   }

   public Engine withConfigTimeout(int configTimeout)
   {
      this.configTimeout = configTimeout;
      return this;
   }

}