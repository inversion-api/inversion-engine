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
package io.inversion.script;

import io.inversion.*;
import io.inversion.script.velocity.LayoutDirective;
import io.inversion.script.velocity.SaveDirective;
import io.inversion.script.velocity.SwitchDirective;
import io.inversion.script.velocity.VelocityResourceLoader;
import io.inversion.utils.JSArray;
import io.inversion.utils.JSNode;
import io.inversion.utils.Path;
import io.inversion.utils.Utils;
import net.jodah.expiringmap.ExpiringMap;
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

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class ScriptAction extends Action<ScriptAction> {
    static final ThreadLocal<ScriptAction> scriptLocal = new ThreadLocal<>();
    static final ThreadLocal<Chain>        chainLocal  = new ThreadLocal<>();

    protected final Logger log = LoggerFactory.getLogger(ScriptAction.class);
    final Map<String, String> scriptTypes = new LinkedHashMap<>();
    String scriptsCollection = "scripts";
    long                cacheExpireSeconds = 60 * 30;
    Map<String, JSNode> CACHE;
    boolean inited = false;
    String scriptsDir = "/WEB-INF/scripts";
    VelocityEngine velocity = null;

    //List<String> reservedNames = new ArrayList<String>(Arrays.asList("switch", "layout", "settings"));

    public ScriptAction() {
        scriptTypes.put("js", "javascript");
        scriptTypes.put("vm", "velocity");
    }

    public static JSNode findScript(final String path) {
        if (path == null)
            return null;

        ScriptAction handler = scriptLocal.get();
        Chain        chain   = chainLocal.get();

        //      if (handler.CACHE.containsKey(path))
        //         return handler.CACHE.get(path);

        String ext = path.indexOf(".") > 0 ? path.substring(path.lastIndexOf(".") + 1).toLowerCase() : null;
        if (ext != null && !handler.scriptTypes.containsKey(ext)) {
            return null;
        }

        String scriptsDir        = handler.scriptsDir;
        String scriptsCollection = handler.scriptsCollection;

        List<String> paths = new ArrayList<>();
        if (ext != null) {
            paths.add(path);
        } else {
            for (Object e : handler.scriptTypes.keySet())
                paths.add(path + "." + e);
        }

        JSNode script = null;

        for (String p : paths) {

            ext = handler.ext(p);
            URL url = chain.getEngine().getResource(Utils.implode("/", scriptsDir, p));
            if (url != null) {
                try {
                    script = new JSNode("type", handler.scriptTypes.get(ext), "script", Utils.read(url.openStream()));
                } catch (IOException e) {
                    //ignore
                }
                break;
            }
        }

        if (script == null && !Utils.empty(scriptsCollection)) {
            String   url = chain.getRequest().getApiUrl() + scriptsCollection + "?name=" + path;
            Response r   = chain.getEngine().get(url);
            if (r.getStatusCode() == 200) {
                JSArray dataArr = r.getJson().getArray("data");
                if (!dataArr.isEmpty()) {
                    script = dataArr.getNode(0);
                }
            }
        }

        handler.CACHE.put(path, script);

        return script;
    }

    synchronized void init() {
        if (!inited) {
            inited = true;
            if (cacheExpireSeconds > 0) {
                CACHE = ExpiringMap.builder().maxSize(500).expiration(cacheExpireSeconds, TimeUnit.SECONDS).build();
            }

            //---------------------------------------------------------
            //-- Initialize Velocity Engine Support
            //VelocityResourceLoader vrl = new VelocityResourceLoader();

            velocity = new VelocityEngine();

            velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, "inversion");
            velocity.setProperty("inversion.resource.loader.class", VelocityResourceLoader.class.getName());
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
            try {
                Enumeration<URL> en = ScriptAction.class.getClassLoader().getResources("META-INF/truffle/language");
                while (en.hasMoreElements()) {
                    URL           u          = en.nextElement();
                    URLConnection connection = u.openConnection();
                    URI           uri        = ((JarURLConnection) connection).getJarFileURL().toURI();
                    try {
                        Paths.get(uri);
                    } catch (FileSystemNotFoundException fsnfe) {
                        log.info("Init : Attempting to create file system for " + uri);
                        Map<String, String> env = new HashMap<>();
                        env.put("create", "true");
                        FileSystems.newFileSystem(uri, env);
                    }
                }
            } catch (Exception e) {
                log.error("Error initializing the javascript language file system", e);
            }
        }
    }

    @Override
    public void run(Request req, Response res) throws ApiException {
        scriptLocal.set(this);
        chainLocal.set(req.getChain());

        if (!inited) {
            init();
        }

        LinkedHashMap<String, JSNode> scripts = findScripts(req);
        if (scripts.size() > 0) {
            runScripts(req, res, scripts);
        }
    }

    void runScripts(Request req, Response res, LinkedHashMap<String, JSNode> scripts) throws ApiException {
        Map<String, Object> contexts = new HashMap<>();

        try {
            String content = null;

            for (String path : scripts.keySet()) {
                JSNode script  = scripts.get(path);
                String type    = script.getString("type");
                String caseVar = null;

                if (path.toLowerCase().endsWith("switch")) {
                    Path reqPath    = req.getSubpath();
                    Path switchPath = new Path(path);

                    if (reqPath.size() > 0) {
                        caseVar = reqPath.get(reqPath.size() - 1);

                        if (switchPath.size() > 1 && switchPath.get(switchPath.size() - 2).equalsIgnoreCase(caseVar))
                            caseVar = null;
                    }
                }

                if ("javascript".equals(type)) {
                    Context context = (Context) contexts.get("javascript");

                    if (context == null) {
                        context = Context.create("js");
                        contexts.put("javascript", context);

                        Value bindings = context.getBindings("js");

                        bindings.putMember("engine", req.getEngine());
                        bindings.putMember("api", req.getApi());
                        bindings.putMember("endpoint", req.getEndpoint());
                        bindings.putMember("action", this);
                        bindings.putMember("chain", req.getChain());
                        bindings.putMember("req", req);
                        bindings.putMember("res", res);
                        bindings.putMember("scriptJson", script.getString("script"));
                        bindings.putMember("case", caseVar);
                        bindings.putMember("util", new Util());

                        for (String key : req.getUrl().getParams().keySet()) {
                            if (!bindings.hasMember(key))
                                bindings.putMember(key, req.getUrl().getParam(key));
                        }
                    }

                    context.getBindings("js").putMember("content", content);

                    context.eval("js", script.getString("script"));

                    content = context.getBindings("js").getMember("content").asString();
                } else if ("velocity".equals(type)) {
                    VelocityContext context = (VelocityContext) contexts.get("velocity");

                    if (context == null) {
                        context = new VelocityContext();
                        contexts.put("velocity", context);

                        context.put("method", req.getMethod());
                        context.put("engine", req.getEngine());
                        context.put("api", req.getApi());
                        context.put("endpoint", req.getEndpoint());
                        context.put("action", this);
                        context.put("chain", req.getChain());
                        context.put("req", req);
                        context.put("res", res);
                        context.put("scriptJson", script);
                        context.put("case", caseVar);
                        context.put("util", new Util());

                        for (String key : req.getUrl().getParams().keySet()) {
                            if (!context.containsKey(key))
                                context.put(key, req.getUrl().getParam(key));
                        }

                        EventCartridge ec = new EventCartridge();
                        ec.addEventHandler(new IncludeRelativePath());
                        ec.attachToContext(context);
                    }

                    context.put("content", content);

                    Template template = velocity.getTemplate(path);

                    ByteArrayOutputStream baos   = new ByteArrayOutputStream();
                    BufferedWriter        writer = new BufferedWriter(new OutputStreamWriter(baos));

                    template.merge(context, writer);

                    writer.flush();
                    writer.close();

                    content = baos.toString();
                    context.put("content", content);
                }
            }

            if (content != null && !Utils.empty(content))// && Utils.empty(res.getText()) && (res.getJson() == null || res.getJson().getProperties().size() == 0))
            {
                boolean setText = true;
                if (content.startsWith("{") || content.startsWith("[")) {
                    try {
                        JSNode obj = JSNode.parseJsonNode(content);
                        res.withJson(obj);
                        setText = false;
                    } catch (Exception ex) {
                        //intentionally empty
                    }
                }

                if (setText) {
                    res.withText(content.trim());
                }
            }
        } catch (Exception ex) {
            throw ApiException.new500InternalServerError(ex);
        } finally {
            for (Object context : contexts.values()) {
                if (context instanceof Context)
                    ((Context) context).close();
            }
        }
    }

    public LinkedHashMap<String, JSNode> findScripts(Request req) throws ApiException {
        Map<JSNode, String> paths   = new HashMap<>();
        List<JSNode>        scripts = new ArrayList<>();

        String subpath = req.getSubpath().toString();

        List<String> parts = Utils.explode("/", subpath);

        JSNode script = null;
        String path   = null;

        List<String> guesses = new ArrayList<>();
        if (parts.size() > 1) {
            if (ext(parts.get(1)) == null) {
                guesses.add(Utils.implode("/", parts.get(0), "switch"));
            }
            guesses.add(Utils.implode("/", parts.get(0), parts.get(1)));
        }

        if (parts.size() == 0 || ext(parts.get(0)) == null) {
            if (parts.size() > 0)
                guesses.add(Utils.implode("/", parts.get(0), "switch"));
            guesses.add("switch");
        }

        if (parts.size() == 1)
            guesses.add(parts.get(0));

        // last chance, look for the file name
        if (req.getResourceKey() != null)
            guesses.add(req.getResourceKey());

        for (String guess : guesses) {
            path = guess;
            script = findScript(path);
            if (script != null)
                break;
        }

        if (script != null) {
            scripts.add(script);
            paths.put(script, path);

            parts = Utils.explode("/", path);

            List<JSNode> settings = new ArrayList<>();

            for (int i = 0; i < parts.size(); i++) {
                String base = i == 0 ? "" : Utils.implode("/", parts.subList(0, i - 1));
                path = Utils.implode("/", base, "settings");

                script = findScript(path);
                if (script != null) {
                    settings.add(script);
                }
            }

            if (settings.size() > 0) {
                scripts.addAll(0, settings);
                paths.put(script, path);
            }

            for (int i = parts.size() - 1; i >= 0; i--) {
                String base = i == 0 ? "" : Utils.implode("/", parts.subList(0, i));
                path = Utils.implode("/", base, "layout");
                script = findScript(path);
                if (script != null) {
                    scripts.add(script);
                    paths.put(script, path);
                }
            }
        }

        LinkedHashMap<String, JSNode> ordered = new LinkedHashMap<>();
        for (JSNode aScript : scripts) {
            ordered.put(paths.get(aScript), aScript);
        }

        return ordered;
    }

    String ext(String fileName) {
        if (fileName.indexOf(".") > 0) {
            String ext = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
            if (scriptTypes.containsKey(ext))
                return ext;
        }
        return null;
    }

    //   ObjectNode findScriptJson(Engine service, String scriptName, Chain chain, Request req) throws Exception
    //   {
    //      String noScriptCache = req.removeParam("noScriptCache");
    //      String clearScriptCache = req.removeParam("clearScriptCache");
    //      if (clearScriptCache != null && CACHE != null)
    //      {
    //         CACHE.clear();
    //      }
    //
    //      ObjectNode scriptJson = null;
    //      if (CACHE != null && noScriptCache == null)
    //      {
    //         scriptJson = (ObjectNode) CACHE.get(scriptName);
    //      }
    //      if (scriptJson == null)
    //      {
    //         String ext = scriptName.indexOf(".") > 0 ? scriptName.substring(scriptName.lastIndexOf(".") + 1, scriptName.length()).toLowerCase() : null;
    //         if (scriptName.indexOf("../") < 0 && scriptTypes.containsKey(ext))
    //         {
    //            InputStream is = service.getResource(scriptsDir + "/" + scriptName);
    //            if (is != null)
    //            {
    //               return new ObjectNode("type", scriptTypes.get(ext), "script", Utils.read(is));
    //            }
    //         }
    //
    //         String url = req.getApiUrl() + scriptsCollection + "?name=" + scriptName;
    //         Response r = chain.getEngine().include(chain, "GET", url, null);
    //         if (r.getStatusCode() == 200)
    //         {
    //            ArrayNode dataArr = r.getJson().getArray("data");
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

    public String getScriptsCollection() {
        return scriptsCollection;
    }

    public void setScriptsCollection(String scriptsCollection) {
        this.scriptsCollection = scriptsCollection;
    }

    public String getScriptsDir() {
        return scriptsDir;
    }

    public ScriptAction withScriptsDir(String scriptsDir) {
        this.scriptsDir = scriptsDir;
        return this;
    }

    public long getCacheExpireSeconds() {
        return cacheExpireSeconds;
    }

    public void setCacheExpireSeconds(long cacheExpireSeconds) {
        this.cacheExpireSeconds = cacheExpireSeconds;
    }

    public ScriptAction withCacheExpireSeconds(long cacheExpireSeconds) {
        setCacheExpireSeconds(cacheExpireSeconds);
        return this;
    }

    public static class Util {
        public void throwApiException(String status, String message) {
            throw ApiException.new500InternalServerError(status, message);
        }

        public void throwBadRequest(String message) {
            throw ApiException.new400BadRequest(message);
        }

        public void throwNotFound(String message) {
            throw ApiException.new404NotFound(message);
        }

        public void throwServerError(String message) {
            throw ApiException.new500InternalServerError(message);
        }

        public List<Object> list(Object obj) {
            List<Object> l = new ArrayList<>();

            if (obj instanceof Map) {
                l.addAll(((Map) obj).values());
            } else if (obj instanceof Collection) {
                l.addAll((Collection) obj);
            } else if (obj != null) {
                l.add(obj);
            }

            return l;
        }

    }

}
