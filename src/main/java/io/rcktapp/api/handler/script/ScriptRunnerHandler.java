/**
 * 
 */
package io.rcktapp.api.handler.script;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.app.event.EventCartridge;
import org.apache.velocity.app.event.implement.IncludeRelativePath;
import org.apache.velocity.runtime.RuntimeConstants;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.forty11.j.J;
import io.forty11.web.js.JS;
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
import io.rcktapp.api.handler.script.velocity.LayoutDirective;
import io.rcktapp.api.handler.script.velocity.SaveDirective;
import io.rcktapp.api.handler.script.velocity.SwitchDirective;
import io.rcktapp.api.handler.script.velocity.VelocityResourceLoader;
import io.rcktapp.api.service.Service;
import net.jodah.expiringmap.ExpiringMap;

/**
 * @author tc-rocket
 *
 */
public class ScriptRunnerHandler implements Handler
{
   static ThreadLocal<ScriptRunnerHandler> scriptLocal        = new ThreadLocal();
   static ThreadLocal<Chain>               chainLocal         = new ThreadLocal();

   Logger                                  log                = LoggerFactory.getLogger(ScriptRunnerHandler.class);
   String                                  scriptsCollection  = "scripts";

   long                                    cacheExpireSeconds = 60 * 30;
   Map<String, Object>                     CACHE;

   boolean                                 inited             = false;

   String                                  scriptsDir         = "/WEB-INF/scripts";
   Map                                     scriptTypes        = new HashMap();

   VelocityEngine                          velocity           = null;

   List<String>                            reservedNames      = new ArrayList(Arrays.asList("switch", "layout", "settings"));

   public ScriptRunnerHandler()
   {
      scriptTypes.put("js", "javascript");
      scriptTypes.put("vm", "velocity");
   }

   void init(Service service)
   {
      if (!inited)
      {
         inited = true;
         if (cacheExpireSeconds > 0)
         {
            CACHE = ExpiringMap.builder().maxSize(500).expiration(cacheExpireSeconds, TimeUnit.SECONDS).build();
         }

         //---------------------------------------------------------
         //-- Initialize Velocity Engine Support
         VelocityResourceLoader vrl = new VelocityResourceLoader();

         velocity = new VelocityEngine();
         
         velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, "snooze");
         velocity.setProperty("snooze.resource.loader.class", VelocityResourceLoader.class.getName());
         velocity.setProperty("userdirective", SwitchDirective.class.getName() + ", " + SaveDirective.class.getName() + ", " + LayoutDirective.class.getName());
         //velocity.setProperty("userdirective", SaveDirective.class.getName());
         //velocity.setProperty("userdirective", LayoutDirective.class.getName());
         velocity.init();

         //---------------------------------------------------------
         //-- Initialize JS Support through GraalVM

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
      scriptLocal.set(this);
      chainLocal.set(chain);

      if (!inited)
      {
         init(service);
      }

      LinkedHashMap<String, JSObject> scripts = findScripts(service, chain, req);
      if (scripts.size() > 0)
      {
         runScripts(service, api, endpoint, action, chain, req, res, scripts);
      }
   }

   void runScripts(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res, LinkedHashMap<String, JSObject> scripts) throws Exception
   {
      Map<String, Object> contexts = new HashMap();

      try
      {
         for (String path : scripts.keySet())
         {
            JSObject script = scripts.get(path);
            String type = script.getString("type");

            if ("javascript".equals(type))
            {
               Context context = (Context) contexts.get("javascript");

               if (context == null)
               {
                  context = Context.create("js");
                  contexts.put("javascript", context);

                  Value bindings = context.getBindings("js");
                  bindings.putMember("service", service);
                  bindings.putMember("api", api);
                  bindings.putMember("endpoint", endpoint);
                  bindings.putMember("action", action);
                  bindings.putMember("chain", chain);
                  bindings.putMember("req", req);
                  bindings.putMember("res", res);
                  bindings.putMember("scriptJson", script.getString("script"));
                  bindings.putMember("util", new Util());
                  bindings.putMember("js", new JS());
               }
               context.eval("js", script.getString("script"));
            }
            else if ("velocity".equals(type))
            {
               VelocityContext context = (VelocityContext) contexts.get("velocity");

               if (context == null)
               {
                  context = new VelocityContext();
                  contexts.put("velocity", context);
                  context.put("service", service);
                  context.put("api", api);
                  context.put("endpoint", endpoint);
                  context.put("action", action);
                  context.put("chain", chain);
                  context.put("req", req);
                  context.put("res", res);
                  context.put("scriptJson", script);
                  context.put("util", new Util());

                  EventCartridge ec = new EventCartridge();
                  ec.addEventHandler(new IncludeRelativePath());
                  ec.attachToContext(context);
               }

               Template template = velocity.getTemplate(path);

               ByteArrayOutputStream baos = new ByteArrayOutputStream();
               BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(baos));

               template.merge(context, writer);

               writer.flush();
               writer.close();

               String output = baos.toString();

               output = output.trim();

               boolean setText = true;
               if (output.startsWith("{") || output.startsWith("["))
               {
                  try
                  {
                     JSObject obj = JS.toJSObject(output);
                     res.setJson(obj);
                     setText = false;
                  }
                  catch (Exception ex)
                  {

                  }
               }

               if (setText)
               {
                  res.setText(output);
               }
            }

         }
      }
      finally
      {
         for (Object context : contexts.values())
         {
            if (context instanceof Context)
               ((Context) context).close();
         }
      }

   }

   public LinkedHashMap<String, JSObject> findScripts(Service service, Chain chain, Request req) throws Exception
   {
      Map<JSObject, String> paths = new HashMap();
      List<JSObject> scripts = new ArrayList();

      String subpath = req.getSubpath();

      List<String> parts = J.explode("/", subpath);

      JSObject script = null;
      String path = null;

      List<String> guesses = new ArrayList();
      if (parts.size() > 1)
      {
         if (ext(parts.get(1)) == null)
         {
            guesses.add(J.implode("/", parts.get(0), "switch"));
         }
         guesses.add(J.implode("/", parts.get(0), parts.get(1)));
      }

      if (ext(parts.get(0)) == null)
         guesses.add("switch");
      else
         guesses.add(parts.get(0));

      for (String guess : guesses)
      {
         path = guess;
         script = findScript(path);
         if (script != null)
            break;
      }

      if (script != null)
      {
         scripts.add(script);
         paths.put(script, path);

         //         List<JSObject> settings = new ArrayList();
         //         for (int i = 0; i < parts.size(); i++)
         //         {
         //            String base = i == 0 ? "" : J.implode("/", parts.subList(0, i - 1));
         //            path = J.implode("/", base, "settings");
         //
         //            script = findScript(path);
         //            if (script != null)
         //            {
         //               settings.add(script);
         //            }
         //         }
         //
         //         if (settings.size() > 0)
         //         {
         //            scripts.addAll(0, settings);
         //            paths.put(script, path);
         //         }
         //
         //         for (int i = parts.size() - 1; i >= 0; i--)
         //         {
         //            String base = i == 0 ? "" : J.implode("/", parts.subList(0, i - 1));
         //            path = J.implode("/", base, "layout");
         //            script = findScript(path);
         //            if (script != null)
         //            {
         //               scripts.add(script);
         //               paths.put(script, path);
         //            }
         //         }
      }

      LinkedHashMap ordered = new LinkedHashMap();
      for (JSObject aScript : scripts)
      {
         ordered.put(paths.get(aScript), aScript);
      }

      return ordered;
   }

   public static JSObject findScript(String path) throws Exception
   {
      ScriptRunnerHandler handler = scriptLocal.get();
      Chain chain = chainLocal.get();

      String ext = path.indexOf(".") > 0 ? path.substring(path.lastIndexOf(".") + 1, path.length()).toLowerCase() : null;
      if (ext != null && !handler.scriptTypes.containsKey(ext))
      {
         return null;
      }

      String scriptsDir = chain.getConfig("scriptsDir", handler.scriptsDir);
      String scriptsCollection = chain.getConfig("scriptsCollection", handler.scriptsCollection);

      List<String> exts = new ArrayList();
      if (ext != null)
         exts.add(ext);
      else
         exts.addAll(handler.scriptTypes.keySet());

      List<String> paths = new ArrayList();
      if (ext != null)
      {
         paths.add(path);
      }
      else
      {
         for (Object e : handler.scriptTypes.keySet())
            paths.add(path + "." + e);
      }

      for (String p : paths)
      {
         ext = handler.ext(p);
         InputStream is = chain.getService().getResource(J.implode("/", scriptsDir, p));
         if (is != null)
         {
            return new JSObject("type", handler.scriptTypes.get(ext), "script", J.read(is));
         }

      }

      String url = chain.getRequest().getApiUrl() + scriptsCollection + "?name=" + path;
      Response r = chain.getService().include(chain, "GET", url, null);
      if (r.getStatusCode() == 200)
      {
         JSArray dataArr = r.getJson().getArray("data");
         if (!dataArr.asList().isEmpty())
         {
            return dataArr.getObject(0);
         }
      }

      return null;
   }

   String ext(String fileName)
   {
      if (fileName.indexOf(".") > 0)
      {
         String ext = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length()).toLowerCase();
         if (scriptTypes.containsKey(ext))
            return ext;
      }
      return null;
   }

   //   JSObject findScriptJson(Service service, String scriptName, Chain chain, Request req) throws Exception
   //   {
   //      String noScriptCache = req.removeParam("noScriptCache");
   //      String clearScriptCache = req.removeParam("clearScriptCache");
   //      if (clearScriptCache != null && CACHE != null)
   //      {
   //         CACHE.clear();
   //      }
   //
   //      JSObject scriptJson = null;
   //      if (CACHE != null && noScriptCache == null)
   //      {
   //         scriptJson = (JSObject) CACHE.get(scriptName);
   //      }
   //      if (scriptJson == null)
   //      {
   //         String ext = scriptName.indexOf(".") > 0 ? scriptName.substring(scriptName.lastIndexOf(".") + 1, scriptName.length()).toLowerCase() : null;
   //         if (scriptName.indexOf("../") < 0 && scriptTypes.containsKey(ext))
   //         {
   //            InputStream is = service.getResource(scriptsDir + "/" + scriptName);
   //            if (is != null)
   //            {
   //               return new JSObject("type", scriptTypes.get(ext), "script", J.read(is));
   //            }
   //         }
   //
   //         String url = req.getApiUrl() + scriptsCollection + "?name=" + scriptName;
   //         Response r = chain.getService().include(chain, "GET", url, null);
   //         if (r.getStatusCode() == 200)
   //         {
   //            JSArray dataArr = r.getJson().getArray("data");
   //            if (!dataArr.asList().isEmpty())
   //            {
   //               scriptJson = dataArr.getObject(0);
   //            }
   //         }
   //         if (scriptJson != null && CACHE != null)
   //         {
   //            CACHE.put(scriptName, scriptJson);
   //         }
   //      }
   //
   //      return scriptJson;
   //   }

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
