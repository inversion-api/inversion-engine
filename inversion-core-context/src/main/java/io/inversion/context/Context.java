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
package io.inversion.context;

import io.inversion.context.codec.CollectionCodec;
import io.inversion.context.codec.MapCodec;
import io.inversion.context.codec.PrimitiveCodec;
import io.inversion.utils.Utils;
import io.inversion.utils.ListMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wires up an Api at runtime by reflectively setting bean properties based on key/value configuration properties.
 * <p>
 * Wirer works in two different modes:
 * <ol>
 *  <li><p>Dependency Injection Mode</p> - If the Engine already has an Api set on it, then it is assumed that a developer has at least partially wired up their own Api via code.
 *      In this case, the Wirer looks for named beans in the Api object graph(s) and sets any corresponding "beanName.propertyName" values from <code>configuration</code> on the bean.
 *      This is a great way to inject runtime dependencies such as database credentials.  If developers are hard core SpringBoot-ers, or are otherwise coding up their Inversion
 *      Api using some existing DI framework, they may find this capability to be redundant and prefer to use their own party DI which is totally fine.
 *
 *  <li><p>Full Wiring Mode</p> - when an Engine starts up and no Api's have been added to the Engine via code, then the Wirer does all of the work to fully instantiate/configure/bootstrap
 *      the Api object model based on data found in the <code>configuration</code>.  Here is an outline how Full Wiring Mode works.
 *      <ul>
 *          <li>All beans with key/value properties "${beanName}.class=className" are instantiated and all "${beanName}.${propertyName}" values are set.
 *
 *          <li><code>startup()</code> is called on all Db objects effectively bootstrapping the full Api model.
 *
 *          <li>The now populated object graph is re-serialized to name/value property pairs in memory.
 *
 *          <li>All instantiated objects to this point are thrown away.
 *
 *          <li>All of the values from <code>configuration</code> are merged down on top of the in memory pairs.  This allows the configuration author to overwrite any of the bean properties set during the Db.startup()
 *              default bootstrapping.
 *
 *          <li>The merged properties model is decoded and the full Api(s) object model/graph that is set on the Engine.
 *
 *          <li>When the Engine calls startup() on the Apis (right after configure(Engine) returns), the Apis will call startup() on their Dbs.
 *              In the first pass, above, the Dbs had empty configurations so calling Db.startup() caused the Dbs to reflectively inspect their underlying data source and create Collections to represent underlying tables etc.
 *              These Collections were serialized out and then instantiated and set on the new copies of the Db in the final wiring above.  Now when Db.startup() is called, the Db has Collection(s) already
 *              set on them and the Db will skip the reflective bootstrapping phase.
 *
 *          <li>NOTE: If you don't supply at least one "${myApiName}.class=io.inversion.Api" property in your <code>configuration</code>, a default api named "api" will be instantiated for you and the Wirer
 *              will assume all Db, Endpoints and Actions declared in the <code>configuration</code> belong to that single implicit Api.
 *
 *          <li>NOTE: If you have a single Api and you don't supply at least one "${myEndpointName}.class=io.inversion.Endpoint" property in our <code>configuration</code>, a default Endpoint named "endpoint"
 *              that matches on all HTTP methods and URL paths will be inferred by the Wirer.  If you declear multiple Apis, you must declare Endpoints if you want your Api to do anything.
 *
 *          <li>NOTE: If you only have a single Api, all Dbs, Endpoints, and global Actions will be set on the Api.
 *              If you have more than one Api in your <code>configuration</code>, you must assign Dbs, Endpoints, and global Actions to the appropriate Api.
 *              A "global" Action is one that is not explicitly assigned to an Endpoint but is instead assigned directly to the Api and can then be selected to run across requests to multiple different Endpoints.
 *      </ul>
 * </ol>
 *
 * <p>
 * Here is an example minimal configuration for Full Wiring Mode that will produce a fully running REST API for the underlying data source.
 * These name/value pairs can come from any combination of property sources loaded into <code>configuration</code>.
 * <pre>
 *      myAction.class=io.inversion.db.DbAction
 *      myDb.class=io.inversion.jdbc.JdbcDb
 *      myDb.driver=${YOUR JDBC DRIVER CLASS NAME}
 *      myDb.url=${YOUR JDBC URL}
 *      myDb.user=${YOUR JDBC USERNAME}
 *      myDb.pass=${YOUR JDBC PASSWORD}
 * </pre>
 *
 * <p>
 * By default, the <code>configuration</code> is going to the global default CombinedConfiguration from Config.
 *
 * @see <a href="http://commons.apache.org/proper/commons-configuration/apidocs/org/apache/commons/configuration2/CombinedConfiguration.html">org.apache.commons.configuration2.CombinedConfiguration</a>
 */
@SuppressWarnings("unchecked")
public class Context {

    /**
     * If a bean property field name appears in this list, it will not be logged but replaced with "************"
     * in the output.
     * <p>
     * Put values here in lower case.
     */
    public static final String[]  MASKED_FIELDS       = new String[]{"pass", "password", "credentials", "secret", "secretkey"};
    static final        Pattern[] MASKED_FIELDS_REGEX = new Pattern[MASKED_FIELDS.length];
    public static final String    MASK                = "**************";

    static {
        for (int i = 0; i < MASKED_FIELDS.length; i++) {
            MASKED_FIELDS_REGEX[i] = Pattern.compile(MASKED_FIELDS[i], Pattern.CASE_INSENSITIVE);
        }
    }

    static Logger log = LoggerFactory.getLogger(Context.class);

    String nameRegex = "^[a-zA-Z0-9_]*$";

    ListMap<Class, Codec> codecs  = new ListMap();
    Encoder                              encoder = new Encoder();
    Decoder                              decoder = new Decoder();
    Namer                                namer   = null;

    Map<Class, Codec>               codecCache   = new HashMap();
    IdentityHashMap<Object, String> beansToNames = new IdentityHashMap();
    TreeMap<String, Object>         namesToBeans = new TreeMap();
    TreeMap<String, String>         properties   = new TreeMap<>();

    public Context() {
        withCodec(new PrimitiveCodec());
        withCodec(new CollectionCodec());
        withCodec(new MapCodec());
    }

    public void clear() {
        codecCache.clear();
        beansToNames.clear();
        namesToBeans.clear();
        properties.clear();
    }

    public Set<String> getNames() {
        return new HashSet(namesToBeans.keySet());
    }

    public String getName(Object bean) {
        return beansToNames.get(bean);
    }

    public boolean hasName(String name) {
        return namesToBeans.containsKey(name);
    }

    public Object getBean(String name) {
        return namesToBeans.get(name);
    }

    public void putBean(String name, Object bean) {
        beansToNames.put(bean, name);
        namesToBeans.put(name, bean);
    }

    public <T> List<T> getBeans(Class<T> type) {
        List matches = new ArrayList<>();
        for (Object bean : beansToNames.keySet()) {
            Class beanClass = bean.getClass();
            if (type.isAssignableFrom(beanClass))
                matches.add(bean);
        }
        return (List<T>) matches;
    }

    public Context withProperties(Map<String, String> properties) {
        this.properties.putAll(properties);
        return this;
    }

    public Context withProperty(String key, String value) {
        this.properties.put(key, value);
        return this;
    }

    public Map<String, String> getProperties() {
        return new TreeMap<>(properties);
    }

    public String getProperty(String key) {
        return properties.get(key);
    }

    public synchronized LinkedHashMap<String, String> wire(Map<String, String> configuration, Object... beans) {
        LinkedHashMap                 primaryEncoderProps   = encode(beans);
        LinkedHashMap<String, String> appliedPrimaryDecoder = decode(configuration);

        List<Listener> listeners = getBeans(Listener.class);
        listeners.forEach(l -> l.wiringComplete(this));

        return appliedPrimaryDecoder;
    }

    public synchronized LinkedHashMap<String, String> encode(Object... beans) {
        LinkedHashMap<String, String> primaryEncoderProps = encoder.encode(this, beans);
        dump("properties found by encoding initial model", primaryEncoderProps);
        return primaryEncoderProps;
    }

    public synchronized LinkedHashMap<String, String> decode(Map<String, String> configuration) {
        TreeMap<String, String> configProps = filterConfigProps(configuration);

        //-- wires in all config properties to the existing model
        //-- including instantiating any beans that were not part of the initial model
        LinkedHashMap<String, String> appliedPrimaryDecoder = decoder.decode(this, configProps);
        dump("properties applied in primary decoding", appliedPrimaryDecoder);
        return appliedPrimaryDecoder;
    }


    public String makeName(Object object) {
        try {
            String name = getName(object);
            if (name != null)
                return name;

            name = namer != null ? namer.name(this, object) : name;

            if (name == null) {
                Field nameField = null;
                try{
                    nameField = Utils.getField("name", object.getClass());
                }
                catch(Exception ex){
                    System.err.println("Unable to make name for " + object.getClass());
                    ex.printStackTrace();
                }

                if (nameField != null) {
                    Object n = nameField.get(object);
                    if (n != null)
                        name = n.toString();
                }
            }

            if (name == null) {
                Method getter = null;
                try {
                    getter = Utils.getMethod(object.getClass(), "getName");
                    if (getter != null && getter.getParameterCount() == 0) {
                        Object n = getter.invoke(object);
                        if (n != null)
                            name = n.toString();
                    }
                } catch (Throwable ex) {
                    throw Utils.ex(ex, "Unable to determine name for class '{}' with getName method {}", object.getClass(), getter);
                }
            }

            if (name == null || name.trim().length() == 0) {
                List ofType = getBeans(object.getClass());
                int  i      = ofType.size();
                do {
                    i += 1;
                    String simpleName = object.getClass().getSimpleName();
                    simpleName = simpleName.replaceAll("[^A-Za-z0-9]", "_");
                    name = "_anonymous_" + simpleName + "_" + i;
                }
                while (namesToBeans.containsKey(name));
            }

            if (!isValidName(name)) {
                name = name.replace(" ", "");
                if (!isValidName(name))
                    throw Utils.ex("You have an invalid object name in your configuration: '{}'.  Object names must match the regex '{}'", name, nameRegex);
            }

            if (hasName(name))
                throw Utils.ex("You have an invalid object name in your configuration: '{}'.  Multiple objects have been given the name '{}'.  All object names are required to be unique if they are not null.", object.getClass().getName(), name);

            putBean(name, object);
            return name;

        } catch (Exception ex) {
            throw Utils.ex(ex);
        }
    }

    public boolean isValidName(String s) {
        return s.matches(nameRegex);
    }


    public String getNameRegex() {
        return nameRegex;
    }

    public Context withNameRegex(String nameRegex) {
        this.nameRegex = nameRegex;
        return this;
    }

    public Namer getNamer() {
        return namer;
    }

    public Context withNamer(Namer namer) {
        this.namer = namer;
        return this;
    }

    public Encoder getEncoder() {
        return encoder;
    }

    public Context withEncoder(Encoder encoder) {
        this.encoder = encoder;
        return this;
    }

    public Decoder getDecoder() {
        return decoder;
    }

    public Context withDecoder(Decoder decoder) {
        this.decoder = decoder;
        return this;
    }

    public ListMap<Class, Codec> getCodecs() {
        return codecs;
    }

    public Context withCodec(Codec codec) {
        if (codec == null)
            return this;

        codecCache.clear();
        for (Class type : codec.getTypes()) {
            codecs.put(type, codec);
        }
        return this;
    }

    public Codec getCodec(Class type) {

        if(Codec.class.isAssignableFrom(type)){
            try {
                return (Codec)type.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw Utils.ex("Unable to instantiate class {} as a codec.  You are probably missing a no arg constructor in your class.", type);
            }
        }


        Class inType = type;
        Codec codec  = codecCache.get(type);
        if (codec == null) {
            List<Codec> matches = codecs.get(type);
            if (matches != null && matches.size() > 0) {
                codec = matches.get(0);
                codecCache.put(inType, codec);
                return codec;
            }

            //-- no codec was found for the direct class.
            //-- check for interface compatibility.
            if (codec == null) {
                for (Class clazz : codecs.keySet()) {
                    if (clazz.isAssignableFrom(inType)) {
                        codec = codecs.get(clazz).get(0);
                        codecCache.put(inType, codec);
                        return codec;
                    }
                }
            }

            //-- nothing found for the passed in type or its interfaces, check all parent classes
            while (type != null && type.getSuperclass() != null && !type.getSuperclass().getName().equals(Object.class.getName())) {
                matches = codecs.get(type);
                if (matches != null && matches.size() > 0) {
                    codec = matches.get(0);
                    codecCache.put(inType, codec);
                    return codec;
                }
                type = type.getSuperclass();
            }
        }

        return codec;
    }


    TreeMap<String, String> filterConfigProps(Map<String, String> configuration) {
        Set<String> keepPrefixes = new TreeSet();
        for (String name : getNames()) {
            if (!name.startsWith("_anonymous_")) {
                keepPrefixes.add(name + ".");
            }
        }

        Map<String, String> tempConfigProps = new HashMap();
        List<String>        excludes        = Arrays.asList("java.", "javax.");
        for (String key : configuration.keySet()) {
            boolean skip = false;
            //System.out.println(key);
            if (!key.contains("."))
                skip = true;
            if (key.indexOf(".") != key.lastIndexOf("."))
                skip = true;

            if (!skip) {
                for (String exclude : excludes) {
                    if (key.startsWith(exclude)) {
                        skip = true;
                        break;
                    }
                }
            }

            if (!skip) {
                if ((key.endsWith(".class") || key.endsWith(".className")) && key.indexOf(".") == key.lastIndexOf(".")) {

                    String name = key.substring(0, key.indexOf("."));
                    if (!isValidName(name)) {
                        skip = true;
                        log.warn("Ignoring configuration property with an invalid name '{}'", name);
                    }
                    if (!skip)
                        keepPrefixes.add(key.substring(0, key.indexOf(".") + 1));
                }

                if (!skip)
                    tempConfigProps.put(key, configuration.get(key));
            }
        }

        TreeMap<String, String> configProps = new TreeMap();
        for (String key : tempConfigProps.keySet()) {
            String prefix = key.substring(0, key.indexOf(".") + 1);
            if (keepPrefixes.contains(prefix)) {
                configProps.put(key, tempConfigProps.get(key));
            }
        }
        dump("all config properties", configProps);
        return configProps;
    }


    public static void dump(String title, Map<String, String> properties) {
        dump(title, properties, null);
    }

    public static void dump(String title, Map<String, String> properties, String outputFilePath) {

        try {
            PrintStream fileOut = null;
            if (outputFilePath != null) {
                File file = new File(outputFilePath);
                //System.out.println(file.getCanonicalPath());
                fileOut = new PrintStream(new FileOutputStream(file));
            }


            List<String> keys = Decoder.sort(properties.keySet());

            String startTitle = "-- START: " + title + " -";
            String endTitle   = "--";

            while (startTitle.length() < 80)
                startTitle += "-";
            while (endTitle.length() < 80)
                endTitle += "-";


            log("\r\n" + startTitle);
            //System.out.println("\r\n" + startTitle);
            for (String key : keys) {

                //if (key.startsWith("_anonymous_"))
                //    continue;

                String value = properties.get(key);

                //if (value != null && value.startsWith("_anonymous_"))
                //    continue;

                if (fileOut != null)
                    fileOut.println(key + " = " + value);

                log("   > " + maskOutput(key, value));
                //System.out.println("   > " + maskOutput(key, value));
            }
            log(endTitle + "\r\n");
            //System.out.println(endTitle + "\r\n");

            if (fileOut != null) {
                fileOut.flush();
                fileOut.close();
            }

        } catch (Exception ex) {
            Utils.rethrow(ex);
        }
    }

    static void log(String msg) {
        log.debug(msg);
        //System.out.println(msg);
    }

    public static String maskOutput(String key, String value) {
        String field = Utils.substringAfter(key, ".").toLowerCase();
        for (int i = 0; i < MASKED_FIELDS_REGEX.length; i++) {
            Matcher m = MASKED_FIELDS_REGEX[i].matcher(field);
            if (m.find()) {
                value = MASK;
                break;
            }
        }
        return key + " = " + value;
    }

}
