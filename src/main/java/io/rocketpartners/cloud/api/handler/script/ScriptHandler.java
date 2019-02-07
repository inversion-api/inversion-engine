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
package io.rocketpartners.cloud.api.handler.script;

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

import io.forty11.fusionvtl.directives.LayoutDirective;
import io.forty11.fusionvtl.directives.SaveDirective;
import io.forty11.fusionvtl.directives.SwitchDirective;
import io.rocketpartners.J;
import io.rocketpartners.rest.JS;
import io.rocketpartners.rest.JSArray;
import io.rocketpartners.rest.JSObject;
import io.rocketpartners.cloud.api.Action;
import io.rocketpartners.cloud.api.Api;
import io.rocketpartners.cloud.api.ApiException;
import io.rocketpartners.cloud.api.Chain;
import io.rocketpartners.cloud.api.Endpoint;
import io.rocketpartners.cloud.api.Handler;
import io.rocketpartners.cloud.api.Request;
import io.rocketpartners.cloud.api.Response;
import io.rocketpartners.cloud.api.SC;
import io.rocketpartners.cloud.api.handler.script.velocity.VelocityResourceLoader;
import io.rocketpartners.cloud.api.service.Service;
import net.jodah.expiringmap.ExpiringMap;

/**
 * @author tc-rocket
 *
 */
public class ScriptHandler implements Handler
{
   static ThreadLocal<ScriptHandler> scriptLocal        = new ThreadLocal();
   static ThreadLocal<Chain>         chainLocal         = new ThreadLocal();

   Logger                            log                = LoggerFactory.getLogger(ScriptHandler.class);
   String                            scriptsCollection  = "scripts";

   long                              cacheExpireSeconds = 60 * 30;
   Map<String, JSObject>             CACHE;

   boolean                           inited             = false;

   String                            scriptsDir         = "/WEB-INF/scripts";
   Map                               scriptTypes        = new LinkedHashMap();

   VelocityEngine                    velocity           = null;

   List<String>                      reservedNames      = new ArrayList(Arrays.asList("switch", "layout", "settings"));

   public ScriptHandler()
   {
      scriptTypes.put("js", "javascript");
      scriptTypes.put("vm", "velocity");
   }

   synchronized void init(Service service)
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
            Enumeration<URL> en = ScriptHandler.class.getClassLoader().getResources("META-INF/truffle/language");
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
         String content = null;

         for (String path : scripts.keySet())
         {
            JSObject script = scripts.get(path);
            String type = script.getString("type");

            List<String> parts = J.explode("/", path);
            String componentStr = parts.size() > 1 ? parts.get(parts.size() - 2) : parts.get(0);
            String actionStr = parts.size() > 1 ? parts.get(parts.size() - 1) : null;

            if (actionStr == null || actionStr.indexOf(".") > 0)
               actionStr = "";

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

               context.getBindings("js").putMember("content", content);

               context.eval("js", script.getString("script"));

               content = context.getBindings("js").getMember("content").asString();
            }
            else if ("velocity".equals(type))
            {
               VelocityContext context = (VelocityContext) contexts.get("velocity");

               if (context == null)
               {
                  context = new VelocityContext();
                  contexts.put("velocity", context);

                  context.put("method", req.getMethod());
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

               context.put("content", content);

               Template template = velocity.getTemplate(path);

               ByteArrayOutputStream baos = new ByteArrayOutputStream();
               BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(baos));

               template.merge(context, writer);

               writer.flush();
               writer.close();

               content = baos.toString();
               context.put("content", content);
            }
         }

         if (!J.empty(content) && J.empty(res.getText()) && (res.getJson() == null || res.getJson().getProperties().size() == 0))
         {
            boolean setText = true;
            if (content.startsWith("{") || content.startsWith("["))
            {
               try
               {
                  JSObject obj = JS.toJSObject(content);
                  res.setJson(obj);
                  setText = false;
               }
               catch (Exception ex)
               {

               }
            }

            if (setText)
            {
               res.setText(content.trim());
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

      if (parts.size() == 1)
         guesses.add(parts.get(0));
      
      // last chance, look for the file name 
      guesses.add(req.getEntityKey());

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

         parts = J.explode("/", path);

         List<JSObject> settings = new ArrayList();

         for (int i = 0; i < parts.size(); i++)
         {
            String base = i == 0 ? "" : J.implode("/", parts.subList(0, i - 1));
            path = J.implode("/", base, "settings");

            script = findScript(path);
            if (script != null)
            {
               settings.add(script);
            }
         }

         if (settings.size() > 0)
         {
            scripts.addAll(0, settings);
            paths.put(script, path);
         }

         for (int i = parts.size() - 1; i >= 0; i--)
         {
            String base = i == 0 ? "" : J.implode("/", parts.subList(0, i));
            path = J.implode("/", base, "layout");
            script = findScript(path);
            if (script != null)
            {
               scripts.add(script);
               paths.put(script, path);
            }
         }
      }

      LinkedHashMap ordered = new LinkedHashMap();
      for (JSObject aScript : scripts)
      {
         ordered.put(paths.get(aScript), aScript);
      }

      return ordered;
   }

   public static JSObject findScript(final String path) throws Exception
   {
      ScriptHandler handler = scriptLocal.get();
      Chain chain = chainLocal.get();

      //      if (handler.CACHE.containsKey(path))
      //         return handler.CACHE.get(path);

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

      JSObject script = null;

      for (String p : paths)
      {

         ext = handler.ext(p);
         InputStream is = chain.getService().getResource(J.implode("/", scriptsDir, p));
         if (is != null)
         {
            script = new JSObject("type", handler.scriptTypes.get(ext), "script", J.read(is));
            break;
         }
      }

      if (script == null && !J.empty(scriptsCollection))
      {
         String url = chain.getRequest().getApiUrl() + scriptsCollection + "?name=" + path;
         Response r = chain.getService().include(chain, "GET", url, null);
         if (r.getStatusCode() == 200)
         {
            JSArray dataArr = r.getJson().getArray("data");
            if (!dataArr.asList().isEmpty())
            {
               script =  dataArr.getObject(0);
            }
         }
      }

      handler.CACHE.put(path, script);

      return script;
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
