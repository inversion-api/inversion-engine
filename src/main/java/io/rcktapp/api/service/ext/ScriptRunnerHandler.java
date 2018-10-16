/**
 * 
 */
package io.rcktapp.api.service.ext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import io.forty11.web.js.JSArray;
import io.forty11.web.js.JSObject;
import io.rcktapp.api.Action;
import io.rcktapp.api.Api;
import io.rcktapp.api.ApiException;
import io.rcktapp.api.Chain;
import io.rcktapp.api.Endpoint;
import io.rcktapp.api.Handler;
import io.rcktapp.api.Request;
import io.rcktapp.api.Response;
import io.rcktapp.api.SC;
import io.rcktapp.api.service.Service;
import net.jodah.expiringmap.ExpiringMap;

/**
 * @author tc-rocket
 *
 */
public class ScriptRunnerHandler implements Handler
{
   String              scriptsCollection  = "scripts";

   long                cacheExpireSeconds = 60 * 30;
   Map<String, Object> CACHE;

   boolean             inited             = false;

   void init()
   {
      if (!inited)
      {
         inited = true;
         if (cacheExpireSeconds > 0)
         {
            CACHE = ExpiringMap.builder().maxSize(500).expiration(cacheExpireSeconds, TimeUnit.SECONDS).build();
         }
      }
   }

   @Override
   public void service(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res) throws Exception
   {
      init();

      // find script
      String scriptName = req.getEntityKey();
      JSObject scriptJson = findScriptJson(scriptName, chain, req);

      // execute script
      if (scriptJson != null)
      {
         String scriptType = scriptJson.getString("type");
         if (scriptType.equalsIgnoreCase("javascript"))
         {
            executeJavascript(scriptJson, service, api, endpoint, action, chain, req, res);
         }
         else
         {
            throw new ApiException(SC.SC_400_BAD_REQUEST, "The script name '" + scriptName + "' has an unsupported type of '" + scriptType + "'.");
         }
      }

      // chain continue
      chain.go();

   }

   void executeJavascript(JSObject scriptJson, Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res)
   {
      String language = "js";
      String script = scriptJson.getString("script");

      Context context = Context.create(language);
      Value bindings = context.getBindings(language);
      bindings.putMember("service", service);
      bindings.putMember("api", api);
      bindings.putMember("endpoint", endpoint);
      bindings.putMember("action", action);
      bindings.putMember("chain", chain);
      bindings.putMember("req", req);
      bindings.putMember("res", res);
      bindings.putMember("scriptJson", scriptJson);
      bindings.putMember("util", new Util());

      context.eval(language, script);

      context.close();
   }

   JSObject findScriptJson(String scriptName, Chain chain, Request req) throws Exception
   {
      JSObject scriptJson = null;
      if (CACHE != null)
      {
         scriptJson = (JSObject) CACHE.get(scriptName);
      }
      if (scriptJson == null)
      {
         String url = req.getApiUrl() + scriptsCollection + "?name=" + scriptName;
         Response r = chain.getService().include(chain, "GET", url, null);
         if (r.getStatusCode() == 200)
         {
            JSArray dataArr = r.getJson().getArray("data");
            if (!dataArr.asList().isEmpty())
            {
               scriptJson = dataArr.getObject(0);
            }
         }
         if (scriptJson != null && CACHE != null)
         {
            CACHE.put(scriptName, scriptJson);
         }
      }

      return scriptJson;
   }

   public String getScriptsCollection()
   {
      return scriptsCollection;
   }

   public void setScriptsCollection(String scriptsCollection)
   {
      this.scriptsCollection = scriptsCollection;
   }

   public long getCacheExpireSeconds()
   {
      return cacheExpireSeconds;
   }

   public void setCacheExpireSeconds(long cacheExpireSeconds)
   {
      this.cacheExpireSeconds = cacheExpireSeconds;
   }

   public static class Util
   {

      public void throwApiException(String status, String message)
      {
         throw new ApiException(status, message);
      }

      public void throwBadRequest(String message)
      {
         throw new ApiException(SC.SC_400_BAD_REQUEST, message);
      }

      public void throwNotFound(String message)
      {
         throw new ApiException(SC.SC_404_NOT_FOUND, message);
      }

      public void throwServerError(String message)
      {
         throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, message);
      }

      public List<Object> list(Object obj)
      {
         List<Object> l = new ArrayList<Object>();

         if (obj instanceof Map)
         {
            l.addAll(((Map) obj).values());
         }
         else if (obj instanceof Collection)
         {
            l.addAll((Collection) obj);
         }
         else if (obj != null)
         {
            l.add(obj);
         }

         return l;
      }

   }

}
