/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
 * http://rocketpartners.io
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package io.rocketpartners.cloud.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.rocketpartners.cloud.action.sql.SqlDb.ConnectionLocal;
import io.rocketpartners.cloud.model.Action;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.ApiException;
import io.rocketpartners.cloud.model.ArrayNode;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.model.ObjectNode;
import io.rocketpartners.cloud.model.Request;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.model.Url;
import io.rocketpartners.cloud.utils.Configurator;
import io.rocketpartners.cloud.utils.English;
import io.rocketpartners.cloud.utils.Utils;

public class Service
{
   transient volatile boolean                started        = false;
   transient volatile boolean                starting       = false;
   transient volatile boolean                destroyed      = false;

   protected Logger                          log            = LoggerFactory.getLogger(getClass());
   protected Logger                          requestLog     = LoggerFactory.getLogger(getClass() + ".requests");

   protected List<Api>                       apis           = new Vector();

   protected ResourceLoader                  resourceLoader = null;

   protected Configurator                    configurator   = new Configurator();

   /**
    * Must be set to match your servlet path if your servlet is not 
    * mapped to /*
    */
   protected String                          servletMapping = null;

   /**
    * The runtime profile that will be used to load inversion[1-99]-$profile.properties files.
    * This is used so that you can ship common settings in inversion[1-99].properties files
    * that are loaded for all profiles and put custom settings in dev/stage/prod (for example)
    * profile specific settings files.
    */
   protected String                          profile        = null;

   /**
    * The path to inversion*.properties files
    */
   protected String                          configPath     = "";

   /**
    * The number of milliseconds between background reloads of the Api config
    */
   protected int                             configTimeout  = 10000;

   /**
    * Indicates that the supplied config files contain all the setup info and the Api
    * will not be reflectively configured as it otherwise would be.
    */
   protected boolean                         configFast     = false;
   protected boolean                         configDebug    = false;
   protected String                          configOut      = null;

   /**
    * The last response returned.  Not that useful in concurrent 
    * production environments but useful for writing test cases.
    */
   protected transient volatile Response     lastResponse   = null;

   protected transient List<ServiceListener> listeners      = new ArrayList();

   /**
    * Service reflects all request headers along with those supplied in <code>allowHeaders</code> as 
    * "Access-Control-Allow-Headers" response headers.  This is primarily a CROS security thing and you
    * probably won't need to customize this list. 
    */
   protected String                          allowedHeaders = "accept,accept-encoding,accept-language,access-control-request-headers,access-control-request-method,authorization,connection,Content-Type,host,user-agent,x-auth-token";

   public static interface ServiceListener
   {
      public void onStartup(Service service);

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

   public synchronized Service startup()
   {
      if (started || starting) //initing is an accidental recursion guard
         return this;

      starting = true;
      try
      {
         startup0();

         configurator.loadConfig(this);

         for (Api api : apis)
         {
            api.startup();
         }

         for (ServiceListener listener : listeners)
         {
            try
            {
               listener.onStartup(Service.this);
            }
            catch (Exception ex)
            {
               log.warn("Error notifying listener init()", ex);
            }
         }

         //         for (Api api : apis)
         //         {
         //            System.out.println(api.getApiCode() + "--------------");
         //
         //            for (Endpoint e : api.getEndpoints())
         //            {
         //               System.out.println("  - ENDPOINT:   " + e.getPath() + " - " + e.getIncludePaths() + " - " + e.getExcludePaths());
         //            }
         //
         //            List<String> strs = new ArrayList();
         //            for (Collection c : api.getCollections())
         //            {
         //               if (c.getDb().getCollectionPath() != null)
         //                  strs.add(c.getDb().getCollectionPath() + c.getName());
         //               else
         //                  strs.add(c.getName());
         //            }
         //            Collections.sort(strs);
         //            for (String coll : strs)
         //            {
         //               System.out.println("  - COLLECTION: " + coll);
         //            }
         //
         //         }

         started = true;
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

   public Service withListener(ServiceListener listener)
   {
      if (!listeners.contains(listener))
         listeners.add(listener);
      return this;
   }

   public Response get(String url)
   {
      return service("GET", url, (String) null);
   }

   public Response put(String url, Object body)
   {
      return service("PUT", url, (body != null ? body.toString() : null));
   }

   public Response post(String url, Object body)
   {
      return service("POST", url, (body != null ? body.toString() : null));
   }

   public Response put(String url, ObjectNode body)
   {
      return service("PUT", url, body.toString());
   }

   public Response post(String url, ObjectNode body)
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

   public Response delete(String url, ArrayNode hrefs)
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
      req.withService(this);

      return req;
   }

   /**
    * @return the last response serviced by this Service.
    */
   public Response response()
   {
      return lastResponse;
   }

   public Response service(String method, String url, String body)
   {
      Request req = new Request(method, url, body);
      req.withService(this);

      Response res = new Response();

      service(req, res);
      return res;
   }

   public Response forward(String method, String url)
   {
      Request req = new Request(method, url, null);
      req.withService(this);

      Response res = Chain.getResponse();

      service(req, res);
      return res;
   }

   public Chain service(Request req, Response res)
   {
      if (!started)
         startup();

      Chain chain = Chain.push(this, req, res);

      try
      {
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
            res.withStatus(SC.SC_200_OK);
            return chain;
         }

         if (req.getUrl().toString().indexOf("/favicon.ico") >= 0)
         {
            res.withStatus(SC.SC_404_NOT_FOUND);
            return chain;
         }

         String xfp = req.getHeader("X-Forwarded-Proto");
         String xfh = req.getHeader("X-Forwarded-Host");
         if (xfp != null || xfh != null)
         {
            if (xfp != null)
               req.getUrl().withProtocol(xfp);

            if (xfh != null)
               req.getUrl().withHost(xfh);
         }

         Url url = req.getUrl();

         String urlPath = url.getPath();
         List<String> parts = Utils.explode("/", urlPath);

         List<String> apiPath = new ArrayList();

         if (!Utils.empty(servletMapping))
         {
            for (String servletPathPart : Utils.explode("/", servletMapping))
            {
               if (!servletPathPart.equalsIgnoreCase(parts.get(0)))
               {
                  //the inbound URL does not match the expected servletMapping
                  //this may be becuse you are localhost testing...going to 
                  //optomistically skip 
                  break;
               }
               apiPath.add(servletPathPart);
               parts.remove(0);
            }
         }

         if (parts.size() > 0)
         {
            for (Api a : apis)
            {
               if (!((apis.size() == 1 && a.getApiCode() == null) || (parts.get(0).equalsIgnoreCase(a.getApiCode()))))
                  continue;

               req.withApi(a);

               if (parts.get(0).equalsIgnoreCase((a.getApiCode())))
               {
                  apiPath.add(parts.remove(0));
               }

               if (a.isMultiTenant() && parts.size() > 0)
               {
                  String tenantCode = parts.remove(0);
                  apiPath.add(tenantCode);
                  req.withTenantCode(tenantCode);
               }

               req.withApiPath(Utils.implode("/", apiPath) + "/");

               String remainingPath = (Utils.implode("/", parts) + "/"); //find the endpoint that matches the fewest path segments
               for (int i = 0; i <= parts.size(); i++)
               {
                  String endpointPath = i == 0 ? "" : (Utils.implode("/", parts.subList(0, i)) + "/");

                  for (Endpoint e : a.getEndpoints())
                  {
                     if (e.matches(req.getMethod(), endpointPath) //
                           && e.matches(req.getMethod(), remainingPath))
                     {
                        req.withEndpointPath(endpointPath);
                        req.withEndpoint(e);

                        if (i < parts.size())
                        {
                           String collectionKey = parts.get(i);

                           req.withCollectionKey(collectionKey);
                           i += 1;

                           for (io.rocketpartners.cloud.model.Collection collection : a.getCollections())
                           {
                              if (collectionKey.equalsIgnoreCase(collection.getName())//
                                    && (collection.getIncludePaths().size() > 0 //
                                          || collection.getExcludePaths().size() > 0))
                              {
                                 if (collection.matches(req.getMethod(), endpointPath))
                                 {
                                    req.withCollection(collection);
                                    break;
                                 }
                              }
                           }

                           if (req.getCollection() == null)
                           {
                              for (io.rocketpartners.cloud.model.Collection collection : a.getCollections())
                              {
                                 if (collectionKey.equalsIgnoreCase(collection.getName()) //
                                       && collection.getIncludePaths().size() == 0 //
                                       && collection.getExcludePaths().size() == 0)
                                 {
                                    req.withCollection(collection);
                                    break;
                                 }
                              }
                           }

                        }
                        if (i < parts.size())
                        {
                           req.withEntityKey(parts.get(i));
                           i += 1;
                        }
                        if (i < parts.size())
                        {
                           req.withSubCollectionKey(parts.get(i));
                        }
                        break;
                     }
                  }

                  if (req.getEndpoint() != null)
                     break;
               }
            }
         }

         //---------------------------------

         if (req.getEndpoint() == null || req.getUrl().getHost().equals("localhost"))
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
            throw new ApiException(SC.SC_404_NOT_FOUND, "No API found matching URL: \"" + req.getUrl() + "\"");
         }

         if (req.getEndpoint() == null)
         {
            //check to see if a non plural version of the collection endpoint 
            //was passed in, if it was redirect to the plural version
            if (redirectPlural(req, res))
               return chain;
         }

         if (req.getEndpoint() == null)
         {
            String buff = "";
            for (Endpoint e : req.getApi().getEndpoints())
               buff += e.getMethods() + " path: " + e.getPath() + " : includePaths:" + e.getIncludePaths() + ": excludePaths" + e.getExcludePaths() + ",  ";

            throw new ApiException(SC.SC_404_NOT_FOUND, "No endpoint found matching \"" + req.getMethod() + ": " + req.getUrl() + "\" Valid end points include: " + buff);
         }

         //         if (Utils.empty(req.getCollectionKey()))
         //         {
         //            throw new ApiException(SC.SC_400_BAD_REQUEST, "It looks like your collectionKey is empty.  You need at least one more part to your url request path.");
         //         }

         //this will get all actions specifically configured on the endpoint
         List<Action> actions = req.getEndpoint().getActions(req);

         //this matches for actions that can run across multiple endpoints.
         //this might be something like an authorization or logging action
         //that acts like a filter
         for (Action a : req.getApi().getActions())
         {
            //http://host/{apipath}/{endpointpath}/{subpath}
            //since these actions were not assigned to 
            if (a.matches(req.getMethod(), req.getPath()))
               actions.add(a);
         }

         if (actions.size() == 0)
            throw new ApiException(SC.SC_404_NOT_FOUND, "No Actions are configured to handle your request.  Check your server configuration.");

         Collections.sort(actions);

         chain.withActions(actions).go();

         ConnectionLocal.commit();

         return chain;
      }
      catch (Throwable ex)
      {
         try
         {
            ConnectionLocal.rollback();
         }
         catch (Throwable t)
         {
            log.warn("Error rollowing back transaction", t);
         }

         String status = SC.SC_500_INTERNAL_SERVER_ERROR;

         if (ex instanceof ApiException)
         {
            if (req != null && req.isDebug())
            {
               log.error("Error in Service", ex);
            }

            status = ((ApiException) ex).getStatus();
            if (SC.SC_404_NOT_FOUND.equals(status))
            {
               //an endpoint could have match the url "such as GET * but then not 
               //known what to do with the URL because the collection was not pluralized
               if (redirectPlural(req, res))
                  return chain;
            }
         }
         else
         {
            log.error("Error in Service", ex);
         }

         res.withStatus(status);
         ObjectNode response = new ObjectNode("message", ex.getMessage());
         if (SC.SC_500_INTERNAL_SERVER_ERROR.equals(status))
            response.put("error", Utils.getShortCause(ex));

         res.withJson(response);
      }
      finally
      {
         try
         {
            ConnectionLocal.close();
         }
         catch (Throwable t)
         {
            log.warn("Error closing connections", t);
         }

         try
         {
            writeResponse(req, res);
         }
         catch (Throwable ex)
         {
            log.error("Error in Service", ex);
         }

         Chain.pop();
         lastResponse = res;
      }

      return chain;
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
         res.debug("\r\n<< response -------------\r\n");
         res.debug(res.getStatusCode());

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
            res.withStatus(SC.SC_302_FOUND);
         }
         else if (output == null && res.getJson() != null)
         {
            output = res.getJson().toString();

            if (res.getContentType() == null)
               res.withContentType("application/json");
         }

         ObjectNode headers = new ObjectNode();
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

         res.out(output);

         res.debug("\r\n-- done -----------------\r\n");

         //         if (debug)
         //         {
         //            requestLog.info(res.getDebug());
         //         }

         if (explain)
         {
            res.withOutput(res.getDebug());
         }
      }
   }

   boolean redirectPlural(Request req, Response res)
   {
      String collection = req.getCollectionKey();
      if (!Utils.empty(collection))
      {
         String plural = English.plural(collection);
         if (!plural.equals(collection))
         {
            String path = req.getPath();
            path = path.replaceFirst(collection, plural);
            Endpoint rightEndpoint = findEndpoint(req.getApi(), req.getMethod(), path);
            if (rightEndpoint != null)
            {
               String redirect = req.getUrl().toString();
               //redirect = req.getHttpServletRequest().getRequest
               redirect = redirect.replaceFirst("\\/" + collection, "\\/" + plural);

               res.withRedirect(redirect);
               return true;
            }
         }
      }
      return false;
   }

   Endpoint findEndpoint(Api api, String method, String path)
   {
      for (Endpoint endpoint : api.getEndpoints())
      {
         if (endpoint.matches(method, path))
            return endpoint;
      }
      return null;
   }

   public List<Api> getApis()
   {
      return new ArrayList(apis);
   }

   public Api withApi(String apiCode)
   {
      Api api = new Api();
      api.withApiCode(apiCode);
      addApi(api);
      return api;
   }

   public Service withApi(Api api)
   {
      addApi(api);
      return this;
   }

   public synchronized void addApi(Api api)
   {
      if (apis.contains(api))
         return;

      List<Api> newList = new ArrayList(apis);

      Api existingApi = getApi(api.getApiCode());
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

      api.withService(this);
   }

   public synchronized void removeApi(Api api)
   {
      List newList = new ArrayList(apis);
      newList.remove(api);
      apis = newList;

      api.shutdown();
   }

   public synchronized Api getApi(String apiCode)
   {
      for (Api api : apis)
      {
         if (apiCode.equalsIgnoreCase(api.getApiCode()))
            return api;
      }
      return null;
   }

   public String getProfile()
   {
      return profile;
   }

   public void setProfile(String profile)
   {
      this.profile = profile;
   }

   public String getConfigPath()
   {
      return configPath;
   }

   public void setConfigPath(String configPath)
   {
      this.configPath = configPath;
   }

   public Configurator getConfigurator()
   {
      return configurator;
   }

   public void setConfigurator(Configurator configurator)
   {
      this.configurator = configurator;
   }

   public String getServletMapping()
   {
      return servletMapping;
   }

   public void setServletMapping(String servletMapping)
   {
      this.servletMapping = servletMapping;
   }

   public Service withServletMapping(String servletMapping)
   {
      setServletMapping(servletMapping);
      return this;
   }

   public void setAllowHeaders(String allowedHeaders)
   {
      this.allowedHeaders = allowedHeaders;
   }

   public interface ResourceLoader
   {
      InputStream getResource(String name);
   }

   public ResourceLoader getResourceLoader()
   {
      return resourceLoader;
   }

   public void setResourceLoader(ResourceLoader resourceLoader)
   {
      this.resourceLoader = resourceLoader;
   }

   public InputStream getResource(String name)
   {
      if (resourceLoader != null)
         return resourceLoader.getResource(name);

      return getClass().getClassLoader().getResourceAsStream(name);
   }

   public boolean isConfigFast()
   {
      return configFast;
   }

   public void setConfigFast(boolean configFast)
   {
      this.configFast = configFast;
   }

   public boolean isConfigDebug()
   {
      return configDebug;
   }

   public void setConfigDebug(boolean configDebug)
   {
      this.configDebug = configDebug;
   }

   public String getConfigOut()
   {
      return configOut;
   }

   public void setConfigOut(String configOut)
   {
      this.configOut = configOut;
   }

   public int getConfigTimeout()
   {
      return configTimeout;
   }

   public void setConfigTimeout(int configTimeout)
   {
      this.configTimeout = configTimeout;
   }

}
