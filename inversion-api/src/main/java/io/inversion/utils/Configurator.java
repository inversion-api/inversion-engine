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
package io.inversion.utils;

import io.inversion.Collection;
import io.inversion.*;
import io.inversion.action.db.DbAction;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.configuration2.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.lang.reflect.*;
import java.util.*;

/**
 * Wires up an Api at runtime by reflectively setting bean properties based on key/value configuration properties.
 * <p>
 * Configurator works in two different modes:
 * <ol>
 *  <li><p>Dependency Injection Mode</p> - If the Engine already has an Api set on it, then it is assumed that a developer has at least partially wired up their own Api via code.
 *      In this case, the Configurator looks for named beans in the Api object graph(s) and sets any corresponding "beanName.propertyName" values from <code>configuration</code> on the bean.
 *      This is a great way to inject runtime dependencies such as database credentials.  If developers are hard core SpringBoot-ers, or are otherwise coding up their Inversion
 *      Api using some existing DI framework, they may find this capability to be redundant and prefer to use their own party DI which is totally fine.
 *
 *  <li><p>Full Wiring Mode</p> - when an Engine starts up and no Api's have been added to the Engine via code, then the Configurator does all of the work to fully instantiate/configure/bootstrap
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
 *          <li>NOTE: If you don't supply at least one "${myApiName}.class=io.inversion.Api" property in your <code>configuration</code>, a default api named "api" will be instantiated for you and the Configurator
 *              will assume all Db, Endpoints and Actions declared in the <code>configuration</code> belong to that single implicit Api.
 *
 *          <li>NOTE: If you have a single Api and you don't supply at least one "${myEndpointName}.class=io.inversion.Endpoint" property in our <code>configuration</code>, a default Endpoint named "endpoint"
 *              that matches on all HTTP methods and URL paths will be inferred by the Configurator.  If you declear multiple Apis, you must declare Endpoints if you want your Api to do anything.
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
 * @see Engine#startup()
 * @see Config
 * @see <a href="http://commons.apache.org/proper/commons-configuration/apidocs/org/apache/commons/configuration2/CombinedConfiguration.html">org.apache.commons.configuration2.CombinedConfiguration</a>
 */
@SuppressWarnings("unchecked")
public class Configurator {

    /**
     * If a bean property field name appears in this list, it will not be logged but replaced with "************"
     * in the output.
     * <p>
     * Put values here in lower case.
     */
    public static final Object[] MASKED_FIELDS = new Object[]{"pass", "password", "credentials", "secret", "secretkey"};

    public static final String MASK = "**************";

    static final Logger log = LoggerFactory.getLogger(Configurator.class);

    static final String ROOT_BEAN_NAME = "inversion";

    /**
     * Wires up an Api at runtime by reflectively setting bean properties based on key/value configuration properties.
     *
     * @param engine        the engine to be configured
     * @param configuration the name/value pairs used to wire up the Api's that will be added to <code>engine</code>.
     * @see Engine#startup()
     * @see Config
     */
    public synchronized void configure(Engine engine, Configuration configuration) {
        try {
            Map<String, String>       configProps = new HashMap();
            Iterator<String> keys  = configuration.getKeys();
            while (keys.hasNext()) {
                String key = keys.next();
                configProps.put(key, configuration.getString(key));
            }
            dump("all config properties", configProps);


            // decode everything into a map
            //   -- will not write out ".class" properties.
            // wire in all new objects from supplied properties
            // bootstrap api
            // remove any props that were applied in the first pass.
            // apply remaining user supplied props to make sure they override bootstrapping

            Encoder primaryEncoder = new Encoder();
            primaryEncoder.encode(engine);
            dump("properties found by encoding initial model", primaryEncoder.props);

            //-- reverse the encoder bean-to-name map to be used for decoding
            Map<String, Object> beans = new HashMap(primaryEncoder.namesToObjects);

            //-- wires in all config properties to the existing model
            //-- including instantiating any beans that were not part of the initial model
            Decoder primaryDecoder = new Decoder();
            primaryDecoder.namesToObjects.putAll(primaryEncoder.namesToObjects);
            primaryDecoder.decode(configProps);

            dump("properties applied in primary decoding", primaryDecoder.applied);

            //-- this is a shortcut bootstrapping options for
            //-- apis configured primarily through configuration
            if (primaryDecoder.findBeans(Api.class).size() == 0)
                primaryDecoder.namesToObjects.put("api", new Api());

            for (Api api : (List<Api>) primaryDecoder.findBeans(Api.class))
                if (!engine.getApis().contains(api))
                    engine.withApi(api);

            for (Api api : engine.getApis()) {
                if (api.getDbs().size() > 0 //
                    && api.getActions().size() == 0
                    && api.getEndpoints().size() == 0)
                {
                    Action action = new DbAction();
                    Endpoint ep = new Endpoint().withAction(action);

                    if(primaryDecoder.getBean("endpoint") == null)
                    {
                        primaryDecoder.namesToObjects.put("endpoint", ep);
                        ep.withName("endpoint");
                    }
                    if(primaryDecoder.getBean("action") == null)
                    {
                        primaryDecoder.namesToObjects.put("action", action);
                        action.withName("acton");
                    }
                    api.withEndpoint(ep);
                }
            }
            //-- end config shortcut bootstrapping

            //-- this will cause the Dbs to reflect their data sources and create Collections etc.
            for (Api api : primaryDecoder.getBeans(Api.class))
                for (Db db : api.getDbs())
                    db.startup(api);


            //-- now we re encode because we need to know the names of all the beans in case
            //-- some properties were manually overridden
            Encoder secondaryEncoder = new Encoder();
            secondaryEncoder.encode(engine);

            dump("re-encoded properties after db startup", secondaryEncoder.props);

            //-- reverse the encoder bean-to-name map to be used for decoding
            beans = new HashMap(secondaryEncoder.namesToObjects);

            //-- remove all the props that were set in the
            //-- primary decoding so you don't apply them twice
            for (String key : primaryDecoder.applied.keySet())
                configProps.remove(key);

            Decoder secondaryDecoder = new Decoder();
            secondaryDecoder.namesToObjects.putAll(beans);
            secondaryDecoder.decode(configProps);

            for (Rule rule : secondaryDecoder.getBeans(Rule.class)) {
                rule.checkLazyConfig();
            }

            dump("properties applied on second decoding after db startup", secondaryDecoder.applied);

//            log.info("-- applied configuration properties ---------------------------");
//            Map<String, String> applied = new HashMap(primaryDecoder.applied);
//            applied.putAll(seconderyDecoder.applied);
//
//            for (String key : Decoder.sort(applied.keySet())) {
//                String value = applied.get(key);
//
//                log.info("   > " + maskOutput(key, value));
//            }
//            log.info("-- end applied configuration properties -----------------------");


        } catch (Exception e) {
            if(e instanceof ApiException)
                throw (ApiException)e;

            e.printStackTrace();
            throw ApiException.new500InternalServerError(e, "Error loading configuration.");
        }
    }


    static class Encoder {

        static List<Field> excludeFields = new ArrayList<>();
        static List excludeTypes = Utils.asList(Logger.class, Configurator.class);
        static MultiKeyMap<Object, Object> defaults = new MultiKeyMap();

        /**
         * Encoding an object of this type will simply involve recording calling toString().
         */
        private static final Set<Class<?>> STRINGIFIED_TYPES = new HashSet<>();
        static
        {
            STRINGIFIED_TYPES.add(Boolean.class);
            STRINGIFIED_TYPES.add(Character.class);
            STRINGIFIED_TYPES.add(Byte.class);
            STRINGIFIED_TYPES.add(Short.class);
            STRINGIFIED_TYPES.add(Integer.class);
            STRINGIFIED_TYPES.add(Long.class);
            STRINGIFIED_TYPES.add(Float.class);
            STRINGIFIED_TYPES.add(Double.class);
            STRINGIFIED_TYPES.add(Void.class);
            STRINGIFIED_TYPES.add(String.class);
            STRINGIFIED_TYPES.add(Path.class);
        }

        Set<Object>         encoded  = new HashSet();
        Map<String, String> props          = new HashMap();
        Map<Object, String> objectsToNames = new HashMap<>();
        Map<String, Object> namesToObjects = new HashMap<>();


        public String encode(Object object) throws Exception {
            return encode0(object);
        }

        String encode0(Object object) throws Exception {
            try {

                if (object == null)
                    return null;

                if (STRINGIFIED_TYPES.contains(object.getClass()))
                    return object + "";

                if(encoded.contains(object))
                    return objectsToNames.get(object);

                encoded.add(object); //recursion guard

                final String name = getName(object);

                List<Field> fields = Utils.getFields(object.getClass());

                if (!defaults.containsKey(object.getClass())) {
                    Object clean        = null;
                    try
                    {
                        for (Field field : fields) {
                            if (!include(field))
                                continue;

                            if(clean == null)
                                clean = object.getClass().getDeclaredConstructor().newInstance();

                            try {
                                Object defaultValue = field.get(clean);
                                String encodedDefault = new Encoder().encode(defaultValue);
                                defaults.put(object.getClass(), field.getName(), encodedDefault);
                            } catch (Exception ex) {
                                log.debug("Unable to determine default value for {}: ", field, ex);
                                Object defaultValue = field.get(clean);
                            }
                        }
                    }
                    catch(Exception ex)//-- probably no empty constructor
                    {
                        //put this here so future encoders won't try to load defaults
                        defaults.put(object.getClass(), "__none", "__none");
                    }
                }

                for (Field field : fields) {

                    try {
                        if (!include(field))
                            continue;

                        Object value    = field.get(object);
                        String fieldKey = name + "." + field.getName();

                        if (value != null) {
                            if (value.getClass().isArray())
                                value = Utils.asList(value);

                            if (value instanceof java.util.Collection) {
                                if (((java.util.Collection) value).size() == 0)
                                    continue;

                                List values = new ArrayList<>();
                                for (Object child : ((java.util.Collection) value)) {
                                    String childKey = encode0(child);
                                    values.add(childKey);
                                }

                                String encodedProp = Utils.implode(",", values);
                                Object defaultProp = defaults.get(object.getClass(), field.getName());

                                if (!Utils.equal(encodedProp, defaultProp))
                                    props.put(fieldKey, encodedProp);

                            } else if (value instanceof Map) {
                                Map map = (Map) value;
                                if (map.size() == 0)
                                    continue;

                                for (Object mapKey : map.keySet()) {
                                    String encodedKey   = encode0(mapKey);
                                    String encodedValue = encode0(map.get(mapKey));
                                    props.put(fieldKey + "." + encodedKey, encodedValue);
                                }
                            } else {
                                if (STRINGIFIED_TYPES.contains(value.getClass())) {
                                    Object defaultVal = defaults.get(object.getClass(), field.getName());
                                    if (defaultVal != null && defaultVal.equals(value))
                                        continue;
                                } else if (!include(field))
                                    continue;

                                String encodedProp = encode0(value);
                                Object defaultProp = defaults.get(object.getClass(), field.getName());

                                if (!Utils.equal(encodedProp, defaultProp))
                                    props.put(fieldKey, encodedProp);
                            }
                        }
//                    } catch (Exception fieldEx) {
//                        //log.warn("Skipping field encoding due to error: " + field, fieldEx);
//                    }
                    } finally{

                    }
                }

                return name;
            } catch(ApiException ex){
               throw ex;
            }  catch (Exception ex) {
                throw new ApiException(ex, Status.SC_500_INTERNAL_SERVER_ERROR, "Error encoding class {}", object.getClass().getName());
            }
        }

        String getName(Object object) throws Exception {

            if (objectsToNames.containsKey(object))
                return objectsToNames.get(object);

            Object name  = null;
            Class  clazz = object.getClass();
            if (object instanceof Engine) {
                name = ((Engine) object).getName();
                if(name == null)
                    name = "engine";
            } else if (object instanceof Api) {
                name = ((Api) object).getName();
            } else if (object instanceof Db) {
                name = Utils.getField("name", clazz).get(object);
            } else if (object instanceof Collection) {
                Collection coll = (Collection) object;
                if (coll.getDb() != null)
                    name = coll.getDb().getName() + ".collections." + coll.getTableName();
            } else if (object instanceof Property) {
                Property col = (Property) object;
                if (col.getCollection() != null && col.getCollection().getDb() != null)
                    name = col.getCollection().getDb().getName() + ".collections." + col.getCollection().getTableName() + ".properties." + col.getColumnName();
            } else if (object instanceof Index) {
                Index index = (Index) object;
                if (index.getCollection() != null && index.getCollection().getDb() != null)
                    name = index.getCollection().getDb().getName() + ".collections." + index.getCollection().getTableName() + ".indexes." + index.getName();
            } else if (object instanceof Relationship) {
                Relationship rel = (Relationship) object;
                if (rel.getCollection() != null)
                    name = getName(rel.getCollection()) + ".relationships." + rel.getName();
            }

            if (name == null) {
                try {
                    Method getter = Utils.getMethod(object.getClass(), "get" + name);
                    if (getter != null) {
                        name = getter.invoke(object);
                    }
                } catch (Throwable ex) {
                    throw new ApiException(ex, Status.SC_500_INTERNAL_SERVER_ERROR, "Unable to determine name for object '{}'", object);
                }
            }

            if(name == null){
                Field nameField = Utils.getField("name", object.getClass());
                if (nameField != null) {
                    name = nameField.get(object);
                }
            }

            if(name == null || name.toString().trim().length() == 0){
                name = "_anonymous_" + object.getClass().getSimpleName() + "_" + name + "_" + objectsToNames.size();
            }

            String nameStr = name.toString();
            //if(namesToObjects.containsKey(nameStr))
            //    throw new ApiException("You have a configuration error.  Multiple objects have been given the name {}.  All object names are required to be unique if they are not null.", nameStr);

            namesToObjects.put(nameStr, object);
            objectsToNames.put(object, nameStr);

            return nameStr;
        }

        static boolean excludeField(Field field) {

            if(field.getName().equals("name"))//this is implicit in the property key
                return true;

            if(field.getName().indexOf("$") > -1)
                return true;

            if (Modifier.isStatic(field.getModifiers()))
                return true;

            if (Modifier.isTransient(field.getModifiers()))
                return true;

            if (Modifier.isPrivate(field.getModifiers()))
                return true;

            if (excludeFields.contains(field) || excludeTypes.contains(field.getType()))
                return true;

            return false;
        }

        static boolean excludeType(Class type) {
            boolean exclude = excludeTypes.contains(type);
            if (!exclude && type.getName().indexOf("org.springframework") > -1)
                exclude = true;

            return exclude;
        }

        static boolean include(Field field) {

            if (excludeField(field))
                return false;

            Class c = field.getType();
            if (java.util.Collection.class.isAssignableFrom(c)) {
                Type t = field.getGenericType();
                if (t instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType) t;
                    if (pt.getActualTypeArguments()[0] instanceof TypeVariable) {
                        //can't figure out the type so consider it important
                        return true;
                    } else if (pt.getActualTypeArguments()[0] instanceof ParameterizedType) {
                        //TODO: is this the right decision
                        return false;
                    }

                    c = (Class) pt.getActualTypeArguments()[0];
                }

                boolean inc = !excludeType(c);
                return inc;
            } else if (Properties.class.isAssignableFrom(c)) {
                return true;
            } else if (JSNode.class.isAssignableFrom(c)) {
                return true;
            } else if (Map.class.isAssignableFrom(c)) {
                Type t = field.getGenericType();
                if (t instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType) t;
                    if (!(pt.getActualTypeArguments()[0] instanceof Class)
                            || !(pt.getActualTypeArguments()[1] instanceof Class)) {
                        System.out.println("Skipping : " + field);
                        return false;
                    }

                    Class keyType   = (Class) pt.getActualTypeArguments()[0];
                    Class valueType = (Class) pt.getActualTypeArguments()[1];

                    return !excludeType(keyType) && !excludeType(valueType);
                } else {
                    throw new RuntimeException("You need to parameterize this object: " + field);
                }
            } else {
                boolean inc = !excludeType(c);
                return inc;
            }
        }
    }

    static class Decoder {

        final Map<String, String> props          = new HashMap();
        final TreeSet<String>     propKeys       = new TreeSet<>();
        final Map<String, Object> namesToObjects = new HashMap<>();
        final Map<String, String> applied        = new HashMap();

        /**
         * Sorts based on the number of "." characters first and then
         * based on the string value.
         *
         * @param keys the keys to sort
         * @return the sorted list of keys
         */
        public static List<String> sort(java.util.Collection keys) {
            List<String> sorted = new ArrayList(keys);
            sorted.sort((o1, o2) -> {
                int count1 = o1.length() - o1.replace(".", "").length();
                int count2 = o2.length() - o2.replace(".", "").length();
                if (count1 != count2)
                    return count1 > count2 ? 1 : -1;

                return o1.compareTo(o2);
            });

            return sorted;
        }

        public void add(Map props) {
            this.props.putAll(props);
            this.propKeys.addAll((Set) props.keySet());
        }

        public void add(String propsStr) {
            Properties props = new Properties();
            try {
                props.load(new ByteArrayInputStream(propsStr.getBytes()));
            } catch (Exception ex) {
                Utils.rethrow(ex);
            }
            add(props);
        }

        public void add(String key, String value) {
            props.put(key, value);
            propKeys.add(key);
        }

        public void decode(Map<String, String> props) throws Exception {
            add(props);
            decode();
        }

        List<String> getKeys(String beanName) {
            Set<String>       keys       = new HashSet<>();
            String            beanPrefix = beanName + ".";
            SortedSet<String> keySet     = propKeys.tailSet(beanPrefix);
            for (String key : keySet) {
                if (!key.startsWith(beanPrefix)) {
                    break;
                }
                if (!(key.endsWith(".class") || key.endsWith(".className"))) {
                    if (!keys.contains(beanName))
                        keys.add(key);
                }
            }
            return new ArrayList(keys);
        }

        /**
         * Four step process
         * 1. Instantiate all beans
         * 2. Set primitiave types on all beans
         * 3. Set object types on all beans
         * 4. Path compression
         *
         * @throws Exception when configuration fails
         */
        public void decode() throws Exception {
            HashMap<String, Map> loaded = new LinkedHashMap();

            //FIRST STEP
            // - instantiate all beans

            for (Object p : props.keySet()) {
                String key = (String) p;
                if (key.endsWith(".class") || key.endsWith(".className")) {
                    String name = key.substring(0, key.lastIndexOf("."));
                    String cn   = (String) props.get(key);
                    Object obj;

                    applied.put(key, cn);

                    if (namesToObjects.containsKey(name))
                        obj = namesToObjects.get(name);
                    else {
                        try {
                            obj = Class.forName(cn).getDeclaredConstructor().newInstance();
                            Field nameField = Utils.getField("name", obj.getClass());
                            if(nameField != null)
                                nameField.set(obj, name);
                        } catch (Exception ex) {
                            System.err.println("Error instantiating class: '" + cn + "'");
                            throw ex;
                        }
                        namesToObjects.put(name, obj);
                    }
                    loaded.put(name, new HashMap<>());
                }
                if (key.lastIndexOf(".") < 0) {
                    namesToObjects.put(key, cast0(props.get(key)));
                }
            }

            List<String> keys = new ArrayList(namesToObjects.keySet());
            keys = sort(keys);

            //LOOP THROUGH TWICE.
            // - First loop, set atomic props
            // - Second loop, set bean props

            for (int i = 0; i <= 1; i++) {
                boolean isFirstPassSoLoadOnlyPrimitives = i == 0;

                for (String beanName : keys) {
                    Object bean     = namesToObjects.get(beanName);
                    List   beanKeys = getKeys(beanName);
                    for (Object p : beanKeys) {
                        String key = (String) p;

                        if (key.endsWith(".class") || key.endsWith(".className"))
                            continue;

                        //make sure this only has a single "."
                        if ((key.startsWith(beanName + ".") && key.lastIndexOf(".") == beanName.length())) {
                            String prop     = key.substring(key.lastIndexOf(".") + 1);
                            String valueStr = props.get(key);

                            if (valueStr != null)
                                valueStr = valueStr.trim();

                            //if (value != null && (value.length() == 0 || "null".equals(value)))
                            if ("null".equalsIgnoreCase(valueStr)) {
                                valueStr = null;
                            }

                            Object value = valueStr;
                            if (!Utils.empty(valueStr) && namesToObjects.containsKey(valueStr)) {
                                value = namesToObjects.get(valueStr);
                            }

                            boolean valueIsBean = (!(value == null || valueStr.equals("") || valueStr.equals("null")) && (namesToObjects.containsKey(value) || namesToObjects.containsKey(Utils.explode(",", valueStr).get(0))));

                            if (isFirstPassSoLoadOnlyPrimitives && valueIsBean) {
                                continue;
                            } else if (!isFirstPassSoLoadOnlyPrimitives && !valueIsBean) {
                                continue;
                            }

                            Field field = Utils.getField(prop, bean.getClass());
                            if (field != null) {
                                applied.put(key, valueStr);
                                Class type = field.getType();
                                if (value == null || type.isAssignableFrom(value.getClass())) {
                                    if(value == null){
                                        if(valueStr == null || valueStr.trim().equals("") || valueStr.trim().toLowerCase().equals("null"))
                                            field.set(bean, null);
                                        else
                                            log.warn("Unable to determine value for property '{}={}'", prop, valueStr);
                                    } else {
                                        field.set(bean, value);
                                    }

                                } else if (java.util.Collection.class.isAssignableFrom(type)) {
                                    java.util.Collection list = (java.util.Collection) cast(key, valueStr, type, field, namesToObjects);
                                    ((java.util.Collection) field.get(bean)).addAll(list);
                                } else if (Map.class.isAssignableFrom(type)) {
                                    Map map = (Map) cast(key, valueStr, type, null, namesToObjects);
                                    ((Map) field.get(bean)).putAll(map);
                                } else {
                                    field.set(bean, cast(key, valueStr, type, null, namesToObjects));
                                }
                            } else {
                                log.debug("Can't map: " + maskOutput(key, value + ""));
                            }
                        }
                    }
                }
            }

            //THIRD STEP
            // - Perform implicit setters based on nested paths of keys

            for (String beanName : keys) {
                Object obj   = namesToObjects.get(beanName);
                int    count = beanName.length() - beanName.replace(".", "").length();
                if (count > 0) {
                    String parentKey = beanName.substring(0, beanName.lastIndexOf("."));
                    String propKey   = beanName.substring(beanName.lastIndexOf(".") + 1);
                    if (namesToObjects.containsKey(parentKey)) {
                        //               Object parent = beans.get(parentKey);
                        //               System.out.println(parent);
                    } else if (count > 1) {
                        String mapKey = propKey;
                        propKey = parentKey.substring(parentKey.lastIndexOf(".") + 1);
                        parentKey = parentKey.substring(0, parentKey.lastIndexOf("."));

                        Object parent = namesToObjects.get(parentKey);
                        if (parent != null) {
                            Field field = Utils.getField(propKey, parent.getClass());
                            if (field != null) {
                                if (Map.class.isAssignableFrom(field.getType())) {
                                    Map map = (Map) field.get(parent);
                                    if (!map.containsKey(mapKey))
                                        map.put(mapKey, obj);
                                } else if (java.util.Collection.class.isAssignableFrom(field.getType())) {
                                    //System.err.println("You should consider adding " + parent.getClass().getName() + ".with" + propKey + "in camel case singular or plural form");//a settinger to accomodate property: " + beanName);
                                    java.util.Collection list = (java.util.Collection) field.get(parent);
                                    if (!list.contains(obj)) {
                                        list.add(obj);
                                    }
                                } else {
                                    log.warn("Unable to set nested value: '" + beanName + "'");
                                }
                            } else {
                                log.warn("Field is not a mapped: '" + beanName + "'");
                            }
                        } else {
                            log.debug("Missing parent for map compression: '" + beanName + "'");
                        }
                    }
                }
            }
        }

        public void putBean(String key, Object bean) {
            namesToObjects.put(key, bean);
        }

        public Object getBean(String key) {
            return namesToObjects.get(key);
        }

        public <T> List<T> getBeans(Class<T> type) {
            List found = new ArrayList<>();
            for (Object bean : namesToObjects.values()) {
                if (type.isAssignableFrom(bean.getClass()))
                    found.add(bean);
            }
            return found;
        }

        public <T> T getBean(Class<T> type) {
            for (Object bean : namesToObjects.values()) {
                if (type.isAssignableFrom(bean.getClass()))
                    return (T) bean;
            }
            return null;
        }

        public List findBeans(Class type) {
            List matches = new ArrayList<>();
            for (Object bean : namesToObjects.values()) {
                if (type.isAssignableFrom(bean.getClass()))
                    matches.add(bean);
            }
            return matches;
        }

        protected Object cast0(String str) {
            if ("true".equalsIgnoreCase(str))
                return true;

            if ("false".equalsIgnoreCase(str))
                return true;

            if (str.matches("\\d+")){
                try{
                    return Integer.parseInt(str);
                }
                catch(Exception ex) {
                    try {
                        return Long.parseLong(str);
                    }
                    catch(Exception ex2) {
                        //OK must be a really huge number
                    }
                }
            }
            return str;
        }
    }




    static <T> T cast(String key, String stringVal, Class<T> type, Field field, Map<String, Object> beans) throws Exception {
        if (String.class.isAssignableFrom(type)) {
            return (T) stringVal;
        } else if (Path.class.isAssignableFrom(type)) {
            return (T) new Path(stringVal);
        } else if (boolean.class.isAssignableFrom(type)) {
            stringVal = stringVal.toLowerCase();
            return (T) (Boolean) (stringVal.equals("true") || stringVal.equals("t") || stringVal.equals("1"));
        } else if (byte.class.isAssignableFrom(type)) {
            return (T) (Byte) Byte.parseByte(stringVal);
        } else if (char.class.isAssignableFrom(type)) {
            return (T) (Character) stringVal.charAt(0);
        } else if (int.class.isAssignableFrom(type)) {
            return (T) (Integer) Integer.parseInt(stringVal);
        } else if (long.class.isAssignableFrom(type)) {
            return (T) (Long) Long.parseLong(stringVal);
        } else if (float.class.isAssignableFrom(type)) {
            return (T) (Float) Float.parseFloat(stringVal);
        } else if (double.class.isAssignableFrom(type)) {
            return (T) (Double) Double.parseDouble(stringVal);
        } else if (type.isArray() || java.util.Collection.class.isAssignableFrom(type)) {
            Class subtype = null;
            if (type.isArray()) {
                subtype = getArrayElementClass(type);
            }

            if (subtype == null && field != null) {
                subtype = (Class) ((((ParameterizedType) field.getGenericType()).getActualTypeArguments())[0]);
            }

            java.util.Collection list  = java.util.Set.class.isAssignableFrom(type) ? new HashSet() : new ArrayList<>();
            String[]             parts = stringVal.split(",");
            for (String part : parts) {
                part = part.trim();

                Object val = beans.getOrDefault(part, part);

                if (val != null && subtype != null && !subtype.isAssignableFrom(val.getClass()))
                    val = cast(key, val + "", subtype, null, beans);

                list.add(val);
            }

            if (type.isArray())
                return (T) list.toArray((Object[]) Array.newInstance(subtype, list.size()));

            return (T) list;
        } else if (Map.class.isAssignableFrom(type)) {
            Map      map   = new HashMap<>();
            String[] parts = stringVal.split(",");
            for (String part : parts) {
                Object val = beans.get(part);
                map.put(part, val);
            }
            return (T) map;
        } else {
            Object o = beans.get(stringVal);
            if (o != null && type.isAssignableFrom(o.getClass()))
                return (T) o;
        }

        if (stringVal != null)
            throw new RuntimeException("Error setting '" + key + "=" + stringVal + "'.  You must add support for type " + type + " into the Configurator");

        return null;
    }

    static Class getArrayElementClass(Class arrayClass) throws ClassNotFoundException {
        Class  subtype;
        String typeStr = arrayClass.toString();

        if (typeStr.startsWith("class [Z")) {
            subtype = boolean.class;
        } else if (typeStr.startsWith("class [B")) {
            subtype = byte.class;
        } else if (typeStr.startsWith("class [C")) {
            subtype = char.class;
        } else if (typeStr.startsWith("class [I")) {
            subtype = int.class;
        } else if (typeStr.startsWith("class [J")) {
            subtype = long.class;
        } else if (typeStr.startsWith("class [F")) {
            subtype = float.class;
        } else if (typeStr.startsWith("class [D")) {
            subtype = double.class;
        } else //if (typeStr.startsWith("class ["))
        {
            subtype = Class.forName(typeStr.substring(typeStr.indexOf("[") + 2, typeStr.indexOf(";")));
        }
        return subtype;
    }

    static void dump(String title, Map<String, String>  properties) {

        List<String> keys = Decoder.sort(properties.keySet());

        log.debug("-- START: " + title + " ----------------------------------------------");
        //System.out.println("-- START: " + title + " ----------------------------------------------");
        for (String key : keys) {

            if(key.startsWith("_anonymous_"))
                continue;

            String value = properties.get(key);

            if(value != null && value.startsWith("_anonymous_"))
                continue;

            log.debug("   > " + maskOutput(key, value));
            //System.out.println("   > " + maskOutput(key, value));
        }
        log.debug("-- END: " + title + " ----------------------------------------------");
        //System.out.println("-- END: " + title + " ----------------------------------------------");

    }

    static String maskOutput(String key, String value)
    {
        String field = Utils.substringAfter(key, ".").toLowerCase();
        if(Utils.in(field, MASKED_FIELDS))
            value = MASK;
        return key + " = " + value;
    }


}
