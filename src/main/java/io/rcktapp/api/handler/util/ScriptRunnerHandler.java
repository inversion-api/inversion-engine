/**
 * 
 */
package io.rcktapp.api.handler.util;

import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.forty11.j.J;
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

   Logger              log                = LoggerFactory.getLogger(ScriptRunnerHandler.class);

   String              scriptsCollection  = "scripts";

   long                cacheExpireSeconds = 60 * 30;
   Map<String, Object> CACHE;

   boolean             inited             = false;

   String              scriptsDir         = "/WEB-INF/scripts";
   Map                 scriptTypes        = new HashMap();

   public ScriptRunnerHandler()
   {
      scriptTypes.put("js", "javascript");
   }

   void init()
   {
      if (!inited)
      {
         inited = true;
         if (cacheExpireSeconds > 0)
         {
            CACHE = ExpiringMap.builder().maxSize(500).expiration(cacheExpireSeconds, TimeUnit.SECONDS).build();
         }

         /*
          * Initialize the Zip File System if needed.  This is needed to be able to run in a Fat Jar.
          * Without this code the javascript library we are using threw an error in..
          * com.oracle.truffle.polyglot.LanguageCache.collectLanguages(LanguageCache.java:282)
          * The reason is that you can't use the Paths.get method if the File system hasn't been created yet.
          * See.. https://stackoverflow.com/questions/25032716/getting-filesystemnotfoundexception-from-zipfilesystemprovider-when-creating-a-p
          */
         try
         {
            Enumeration<URL> en = ScriptRunnerHandler.class.getClassLoader().getResources("META-INF/truffle/language");
            while (en.hasMoreElements())
            {
               URL u = en.nextElement();
               URLConnection connection = u.openConnection();
               URI uri = ((JarURLConnection) connection).getJarFileURL().toURI();
               try
               {
                  Paths.get(uri);
               }
               catch (FileSystemNotFoundException fsnfe)
               {
                  log.info("Init : Attempting to create file system for " + uri);
                  Map<String, String> env = new HashMap<>();
                  env.put("create", "true");
                  FileSystem fs = FileSystems.newFileSystem(uri, env);
               }
            }
         }
         catch (Exception e)
         {
            log.error("Error initializing the javascript language file system", e);
         }
      }
   }

   @Override
   public void service(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res) throws Exception
   {
      init();

      // find script
      String scriptName = req.getEntityKey();
      JSObject scriptJson = findScriptJson(service, scriptName, chain, req);

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

   JSObject findScriptJson(Service service, String scriptName, Chain chain, Request req) throws Exception
   {
      String noScriptCache = req.removeParam("noScriptCache");
      String clearScriptCache = req.removeParam("clearScriptCache");
      if (clearScriptCache != null && CACHE != null)
      {
         CACHE.clear();
      }

      JSObject scriptJson = null;
      if (CACHE != null && noScriptCache == null)
      {
         scriptJson = (JSObject) CACHE.get(scriptName);
      }
      if (scriptJson == null)
      {
         String ext = scriptName.indexOf(".") > 0 ? scriptName.substring(scriptName.lastIndexOf(".") + 1, scriptName.length()).toLowerCase() : null;
         if (scriptName.indexOf("../") < 0 && scriptTypes.containsKey(ext))
         {
            InputStream is = service.getResource(scriptsDir + "/" + scriptName);
            if (is != null)
            {
               return new JSObject("type", scriptTypes.get(ext), "script", J.read(is));
            }
         }

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
