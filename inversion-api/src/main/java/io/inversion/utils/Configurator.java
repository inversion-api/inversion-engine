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

import java.io.ByteArrayInputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.configuration2.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inversion.Action;
import io.inversion.Api;
import io.inversion.ApiException;
import io.inversion.Collection;
import io.inversion.Db;
import io.inversion.Endpoint;
import io.inversion.Engine;
import io.inversion.Index;
import io.inversion.Property;
import io.inversion.Relationship;
import io.inversion.Rule;
import io.inversion.utils.Configurator.Encoder.Includer;
import io.inversion.utils.Configurator.Encoder.Namer;

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
 * <p>
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
 * @see io.inversion.Engine.startup()
 * @see Config
 * @see <a href="http://commons.apache.org/proper/commons-configuration/apidocs/org/apache/commons/configuration2/CombinedConfiguration.html">org.apache.commons.configuration2.CombinedConfiguration</a>
 */
public class Configurator
{
   static final Logger log            = LoggerFactory.getLogger(Configurator.class);

   static final String ROOT_BEAN_NAME = "inversion";

   /**
    * Wires up an Api at runtime by reflectively setting bean properties based on key/value configuration properties.
    * 
    * @param engine  the engine to be configured
    * @param configuration the name/value pairs used to wire up the Api's that will be added to <code>engine</code>.
    * 
    * @see io.inversion.Engine.startup()
    * @see Config
    */
   public synchronized void configure(Engine engine, Configuration configuration)
   {
      try
      {
         Properties props = new Properties();
         Iterator<String> keys = configuration.getKeys();
         while (keys.hasNext())
         {
            String key = keys.next();
            props.put(key, configuration.getString(key));
         }

         if (engine.getApis().size() > 0)
         {
            //-- DEPENDENCY INJECTION MODE
            //--
            //-- if there is already an API, don't load everything from scratch.
            //-- just override/set any keys from the config on the target objects 

            Encoder encoder = new Encoder();
            encoder.encode(new DefaultNamer(), new AllIncluder(), engine.getApis().toArray());

            //dump(encoder.props);

            Map<String, Object> beans = new HashMap();
            for (Object bean : encoder.names.keySet())
            {
               String key = encoder.names.get(bean);
               beans.put(key, bean);
            }

            for (String beanName : beans.keySet())
            {
               for (Object propKey : props.keySet())
               {
                  if (propKey.toString().startsWith(beanName + "."))
                  {
                     Object propVal = props.getProperty(propKey.toString());

                     Object bean = beans.get(beanName);
                     String fieldName = propKey.toString().substring(beanName.length() + 1);
                     try
                     {
                        Field field = Utils.getField(fieldName, bean.getClass());
                        if (field != null)
                        {
                           field.setAccessible(true);
                           propVal = cast((String) propKey, (String) propVal, field.getType(), field, beans);
                           field.set(bean, propVal);
                        }
                        else
                        {
                           log.warn("Unable to find field '" + fieldName + "' on bean '" + beanName + "'");
                        }
                     }
                     catch (Exception ex)
                     {
                        log.warn("Error setting property '" + propKey + "'", ex);
                     }
                  }
               }
            }
         }
         else
         {
            //-- FULL WIRING MODE

            Decoder w = new Decoder();
            w.putBean(ROOT_BEAN_NAME, engine);
            w.load(props);
            loadConfig(engine, props);
         }
      }
      catch (Exception e)
      {
         ApiException.throw500InternalServerError(e, "Error loading configuration.");
      }
   }

   //   public static Properties encode(Object... beans) throws Exception
   //   {
   //      Properties autoProps = Wirer.encode(new WirerSerializerNamer(), new WirerSerializerIncluder(), beans);
   //      return autoProps;
   //   }

   void loadConfig(Engine engine, Properties properties) throws Exception
   {
      Decoder decoder = new Decoder()
         {
            //IMPORTANT IMPORTANT IMPORTANT
            // add special case exceptions here for cases where users may add unclean data
            // that should not be set directly on bean fields but should be passed the approperiate setter
            public boolean handleProp(Object bean, String prop, String value) throws Exception
            {
               //               if (bean instanceof Endpoint && "path".equalsIgnoreCase(value))
               //               {
               //                  //this is done as a special case because of the 
               //                  //special business logic in the setter
               //                  ((Endpoint) bean).withPath(value);
               //
               //                  return true;
               //               }

               return false;
            }

            @Override
            public void onLoad(String name, Object module, Map<String, Object> props) throws Exception
            {
               Field field = getField("name", module.getClass());
               if (field != null && field.get(module) == null)
                  field.set(module, name);
            }
         };
      decoder.load(properties);

      //-- this just loads the bare bones config supplied  
      //-- in inversion*.properties files by the users.  
      autoWireApi(decoder);

      for (Api api : decoder.getBeans(Api.class))
      {
         for (Db db : ((Api) api).getDbs())
         {
            db.startup(api);
         }
      }

      //-- this serializes out the object model that was bootsrapped off of the
      //-- configuration files.  At this point the db.startup() has been called
      //-- on all of the DBs and they configured collections on the Api.  
      Properties autoProps = new Encoder().encode(new DefaultNamer(), new DefaultIncluder(), decoder.getBeans(Api.class).toArray());
      autoProps.putAll(properties);
      
      for (Api api : decoder.getBeans(Api.class))
      {
         for (Db db : ((Api) api).getDbs())
         {
            db.shutdown(api);
         }
      }
      
      
      decoder.clear();
      decoder.load(autoProps);

      //dump(autoProps);

      autoWireApi(decoder);

      for (Api api : decoder.getBeans(Api.class))
      {
         engine.withApi(api);
      }

      for (String name : decoder.beans.keySet())
      {
         if (name.startsWith("_anonymous_"))
         {
            Object bean = decoder.beans.get(name);
            Field nameField = Utils.getField("name", bean.getClass());
            if (nameField != null)
               nameField.set(bean, null);
         }
      }

      for (Rule rule : decoder.getBeans(Rule.class))
      {
         rule.checkLazyConfig();
      }

   }

   void autoWireApi(Decoder decoder)
   {
      List<Api> apis = decoder.getBeans(Api.class);

      if (decoder.getBeans(Action.class).size() > 0)
      {
         if (apis.size() == 0)
         {
            decoder.putBean("api", new Api());
            apis = decoder.getBeans(Api.class);
         }

         if (decoder.getBeans(Endpoint.class).size() == 0)
         {
            decoder.putBean("endpoint", new Endpoint());
         }
      }

      for (Db db : decoder.getBeans(Db.class))
      {
         for (Collection collection : (List<Collection>) db.getCollections())
         {
            collection.withDb(db);
         }
      }

      if (apis.size() == 1)
      {
         List found = decoder.getBeans(Db.class);

         Api api = apis.get(0);

         if (api.getDbs().size() == 0)
            api.withDbs((Db[]) found.toArray(new Db[found.size()]));

         Set<Action> privateActions = new HashSet();
         found = decoder.getBeans(Endpoint.class);
         if (api.getEndpoints().size() == 0)
         {
            for (Endpoint ep : (List<Endpoint>) found)
            {
               api.withEndpoint(ep);
               privateActions.addAll(ep.getActions());
            }
         }

         found = decoder.getBeans(Action.class);
         if (api.getActions().size() == 0)
         {
            for (Action action : (List<Action>) found)
            {
               if (!privateActions.contains(action))
                  api.withAction(action);
            }
         }
      }
   }

   static class AllIncluder extends DefaultIncluder
   {
      public AllIncluder()
      {
         excludeTypes = new ArrayList(Arrays.asList(Logger.class));
      }
   }

   static class DefaultIncluder implements Includer
   {
      List<Field> excludes     = new ArrayList();                                                                 //TODO:  why was api.actions excluded?  //List<Field> excludes     =  Arrays.asList(Utils.getField("actions", Api.class));

      List        excludeTypes = new ArrayList(Arrays.asList(Logger.class,                                        //don't care to persist info on loggers
            Action.class, Endpoint.class, Rule.class, Path.class));                                               //these are things that must be supplied by manual config so don't write them out.

      @Override
      public boolean include(Field field)
      {
         ///if (field.getAnnotation(Ignore.class) != null || Modifier.isTransient(field.getModifiers()))
         if (Modifier.isTransient(field.getModifiers()))
            return false;

         if (excludes.contains(field) || excludeTypes.contains(field.getType()))
            return false;

         Class c = field.getType();
         if (java.util.Collection.class.isAssignableFrom(c))
         {
            Type t = field.getGenericType();
            if (t instanceof ParameterizedType)
            {
               ParameterizedType pt = (ParameterizedType) t;
               if (pt.getActualTypeArguments()[0] instanceof TypeVariable)
               {
                  //can't figure out the type so consider it important
                  return true;
               }

               c = (Class) pt.getActualTypeArguments()[0];
            }

            boolean inc = !excludeTypes.contains(c);
            return inc;
         }
         else if (Properties.class.isAssignableFrom(c))
         {
            return true;
         }
         else if (JSNode.class.isAssignableFrom(c))
         {
            return true;
         }
         else if (Map.class.isAssignableFrom(c))
         {
            Type t = field.getGenericType();
            if (t instanceof ParameterizedType)
            {
               ParameterizedType pt = (ParameterizedType) t;
               Class keyType = (Class) pt.getActualTypeArguments()[0];
               Class valueType = (Class) pt.getActualTypeArguments()[1];

               return !excludeTypes.contains(keyType) && !excludeTypes.contains(valueType);
            }
            else
            {
               throw new RuntimeException("You need to parameterize this object: " + field);
            }
         }
         else
         {
            boolean inc = !excludeTypes.contains(c);

            if (!inc)
               System.out.println("skipping field: " + field);
            return inc;
         }
      }

   }

   static class DefaultNamer implements Namer
   {
      @Override
      public String getName(Object o) throws Exception
      {
         Object name = null;
         Class clazz = o.getClass();
         if (o instanceof Api)
         {
            name = ((Api) o).getName();
         }
         else if (o instanceof Db)// || o instanceof Action || o instanceof Endpoint)
         {
            name = Utils.getField("name", clazz).get(o);
         }
         else if (o instanceof Collection)
         {
            Collection t = (Collection) o;
            if (t.getDb() != null)
               name = t.getDb().getName() + ".collections." + t.getTableName();
         }
         else if (o instanceof Property)
         {
            Property col = (Property) o;
            if (col.getCollection() != null && col.getCollection().getDb() != null)
               name = col.getCollection().getDb().getName() + ".collections." + col.getCollection().getTableName() + ".properties." + col.getColumnName();
         }
         else if (o instanceof Index)
         {
            Index index = (Index) o;
            if (index.getCollection() != null && index.getCollection().getDb() != null)
               name = index.getCollection().getDb().getName() + ".collections." + index.getCollection().getTableName() + ".indexes." + index.getName();
         }
         else if (o instanceof Relationship)
         {
            Relationship rel = (Relationship) o;
            if (rel.getCollection() != null)
               name = getName(rel.getCollection()) + ".relationships." + rel.getName();
         }

         if (name == null)
            name = Utils.getProperty("name", o);

         if (name != null)
            return name.toString();

         return null;
      }
   }

   static class Decoder
   {
      Properties          props    = new Properties();
      TreeSet<String>     propKeys = new TreeSet<String>();

      Map<String, Object> beans    = new HashMap();

      //designed to be overridden
      public void onLoad(String name, Object bean, Map<String, Object> properties) throws Exception
      {

      }

      //designed to be overridden
      public boolean handleProp(Object bean, String prop, String value) throws Exception
      {
         return false;
      }

      public void clear()
      {
         props.clear();
         beans.clear();
         propKeys.clear();
      }

      public void add(Properties props)
      {
         this.props.putAll(props);
         this.propKeys.addAll((Set) props.keySet());
      }

      public void add(String propsStr)
      {
         Properties props = new Properties();
         try
         {
            props.load(new ByteArrayInputStream(propsStr.getBytes()));
         }
         catch (Exception ex)
         {
            Utils.rethrow(ex);
         }
         add(props);
      }

      public void add(String key, String value)
      {
         props.put(key, value);
         propKeys.add(key);
      }

      public void load(Properties props) throws Exception
      {
         add(props);
         load();
      }

      String getProperty(String name)
      {
         String value = System.getProperty(name);
         if (value != null)
         {
            //System.out.p("using syso prop for key: " + name);
         }
         else
         {
            value = System.getenv(name);
            if (value != null)
            {
               //log.info("using env var for key: " + name);
            }
            else
            {
               value = props.getProperty(name);
            }
         }
         return value;
      }

      List<String> getKeys(String beanName)
      {
         Set<String> keys = new HashSet<String>();
         String beanPrefix = beanName + ".";
         SortedSet<String> keySet = propKeys.tailSet(beanPrefix);
         for (String key : keySet)
         {
            if (!key.startsWith(beanPrefix))
            {
               break;
            }
            if (!(key.endsWith(".class") || key.endsWith(".className")))
            {
               if (!keys.contains(beanName))
                  keys.add(key);
            }
         }

         for (Object p : System.getProperties().keySet())
         {
            String key = (String) p;
            if (key.startsWith(beanPrefix) && !(key.endsWith(".class") || key.endsWith(".className")))
            {
               if (!keys.contains(beanName))
                  keys.add(key);
            }
         }

         for (Object p : System.getenv().keySet())
         {
            String key = (String) p;
            if (key.startsWith(beanPrefix) && !(key.endsWith(".class") || key.endsWith(".className")))
            {
               if (!keys.contains(beanName))
                  keys.add(key);
            }
         }

         return new ArrayList(keys);
      }

      /**
       * Sorts based on the number of "." characters first and then
       * based on the string value.
       * 
       * @param keys
       * @return
       */
      public static List<String> sort(java.util.Collection keys)
      {
         List<String> sorted = new ArrayList(keys);
         Collections.sort(sorted, new Comparator<String>()
            {
               @Override
               public int compare(String o1, String o2)
               {
                  int count1 = o1.length() - o1.replace(".", "").length();
                  int count2 = o2.length() - o2.replace(".", "").length();
                  if (count1 != count2)
                     return count1 > count2 ? 1 : -1;

                  return o1.compareTo(o2);
               }
            });

         return sorted;
      }

      /**
       * Four step process
       * 1. Instantiate all beans
       * 2. Set primitiave types on all beans
       * 3. Set object types on all beans
       * 4. Path compression 
       * 
       * @throws Exception
       */
      public void load() throws Exception
      {
         HashMap<String, Map> loaded = new LinkedHashMap();

         //FIRST STEP
         // - instantiate all beans

         for (Object p : props.keySet())
         {
            String key = (String) p;
            if (key.endsWith(".class") || key.endsWith(".className"))
            {
               String name = key.substring(0, key.lastIndexOf("."));
               String cn = (String) props.get(key);
               Object obj = null;
               try
               {
                  obj = Class.forName(cn).newInstance();
               }
               catch (Exception ex)
               {
                  System.err.println("Error instantiating class: '" + cn + "'");
                  throw ex;
               }

               loaded.put(name, new HashMap());
               beans.put(name, obj);
               //System.out.println(name + "->" + cn);
            }
            if (key.lastIndexOf(".") < 0)
            {
               beans.put(key, cast0(props.getProperty(key)));
            }
         }

         List<String> keys = new ArrayList(beans.keySet());
         keys = sort(keys);

         //LOOP THROUGH TWICE.  
         // - First loop, set atomic props
         // - Second loop, set bean props

         for (int i = 0; i <= 1; i++)
         {
            boolean isFirstPassSoLoadOnlyPrimitives = i == 0;

            for (String beanName : keys)
            {
               Object obj = beans.get(beanName);
               List beanKeys = getKeys(beanName);
               for (Object p : beanKeys)
               {
                  String key = (String) p;

                  if (key.endsWith(".class") || key.endsWith(".className"))
                     continue;

                  if ((key.startsWith(beanName + ".") && key.lastIndexOf(".") == beanName.length()))
                  {
                     String prop = key.substring(key.lastIndexOf(".") + 1, key.length());
                     String value = getProperty(key);

                     if (value != null)
                        value = value.trim();

                     //if (value != null && (value.length() == 0 || "null".equals(value)))
                     if ("null".equals(value))
                     {
                        value = null;
                     }

                     boolean valueIsBean = (!(value.equals("") || value.equals("null")) && (beans.containsKey(value) || beans.containsKey(Utils.explode(",", value).get(0))));

                     if (isFirstPassSoLoadOnlyPrimitives && valueIsBean)
                     {
                        continue;
                     }
                     else if (!isFirstPassSoLoadOnlyPrimitives && !valueIsBean)
                     {
                        continue;
                     }

                     if (handleProp(obj, prop, value))
                     {
                        //do nothing, already handled
                     }
                     else
                     {
                        Field field = getField(prop, obj.getClass());
                        if (field != null)
                        {
                           Class type = field.getType();

                           if (beans.containsKey(value) && type.isAssignableFrom(beans.get(value).getClass()))
                           {
                              field.set(obj, beans.get(value));
                           }
                           else if (java.util.Collection.class.isAssignableFrom(type))
                           {
                              java.util.Collection list = (java.util.Collection) cast(key, value, type, field, beans);
                              ((java.util.Collection) field.get(obj)).addAll(list);
                           }
                           else if (Map.class.isAssignableFrom(type))
                           {
                              Map map = (Map) cast(key, value, type, null, beans);
                              ((Map) field.get(obj)).putAll(map);
                           }
                           else
                           {
                              field.set(obj, cast(key, value, type, null, beans));
                           }
                        }
                        else
                        {
                           System.out.println("Can't map: " + key + " = " + value);
                        }

                     }
                  }
               }
            }
         }

         //THIRD STEP
         // - Perform implicit setters based on nested paths of keys

         for (String beanName : keys)
         {
            Object obj = beans.get(beanName);
            int count = beanName.length() - beanName.replace(".", "").length();
            if (count > 0)
            {
               String parentKey = beanName.substring(0, beanName.lastIndexOf("."));
               String propKey = beanName.substring(beanName.lastIndexOf(".") + 1);
               if (beans.containsKey(parentKey))
               {
                  //               Object parent = beans.get(parentKey);
                  //               System.out.println(parent);
               }
               else if (count > 1)
               {
                  String mapKey = propKey;
                  propKey = parentKey.substring(parentKey.lastIndexOf(".") + 1);
                  parentKey = parentKey.substring(0, parentKey.lastIndexOf("."));

                  Object parent = beans.get(parentKey);
                  if (parent != null)
                  {
                     Field field = getField(propKey, parent.getClass());
                     if (field != null)
                     {
                        if (Map.class.isAssignableFrom(field.getType()))
                        {
                           Map map = (Map) field.get(parent);
                           if (!map.containsKey(mapKey))
                              map.put(mapKey, obj);
                        }
                        else if (java.util.Collection.class.isAssignableFrom(field.getType()))
                        {
                           //System.err.println("You should consider adding " + parent.getClass().getName() + ".with" + propKey + "in camel case singular or plural form");//a settinger to accomodate property: " + beanName);
                           java.util.Collection list = (java.util.Collection) field.get(parent);
                           if (!list.contains(obj))
                           {
                              list.add(obj);
                           }
                        }
                        else
                        {
                           System.err.println("Unable to set nested value: '" + beanName + "'");
                        }
                     }
                     else
                     {
                        System.err.println("Field is not a mapped: " + beanName + " - " + field);
                     }
                  }
                  else
                  {
                     System.err.println("Missing parent for map compression: " + beanName);
                  }
               }
            }
         }

         for (String name : loaded.keySet())
         {
            Object bean = beans.get(name);
            Map loadedPros = loaded.get(name);
            onLoad(name, bean, loadedPros);
         }

      }

      public void putBean(String key, Object bean)
      {
         beans.put(key, bean);
      }

      public Object getBean(String key)
      {
         return beans.get(key);
      }

      public <T> List<T> getBeans(Class<T> type)
      {
         List found = new ArrayList();
         for (Object bean : beans.values())
         {
            if (type.isAssignableFrom(bean.getClass()))
               found.add(bean);
         }
         return found;
      }

      public <T> T getBean(Class<T> type)
      {
         for (Object bean : beans.values())
         {
            if (type.isAssignableFrom(bean.getClass()))
               return (T) bean;
         }
         return null;
      }

      public List findBeans(Class type)
      {
         List matches = new ArrayList();
         for (Object bean : beans.values())
         {
            if (type.isAssignableFrom(bean.getClass()))
               matches.add(bean);
         }
         return matches;
      }

      public static Field getField(String fieldName, Class clazz)
      {
         if (fieldName == null || clazz == null)
         {
            return null;
         }

         Field[] fields = clazz.getDeclaredFields();
         for (int i = 0; i < fields.length; i++)
         {
            if (fields[i].getName().equals(fieldName))
            {
               Field field = fields[i];
               field.setAccessible(true);
               return field;
            }
         }

         if (clazz.getSuperclass() != null && !clazz.equals(clazz.getSuperclass()))
         {
            return getField(fieldName, clazz.getSuperclass());
         }

         return null;
      }

      protected Object cast0(String str)
      {
         if ("true".equalsIgnoreCase(str))
            return true;

         if ("false".equalsIgnoreCase(str))
            return true;

         if (str.matches("\\d+"))
            return Integer.parseInt(str);

         return str;
      }

   }

   static class Encoder
   {
      Properties                  props    = null;
      Map<Object, String>         names    = null;
      MultiKeyMap<String, String> defaults = null;

      static interface Namer
      {
         public String getName(Object o) throws Exception;
      }

      static interface Includer
      {
         public boolean include(Field field);
      }

      public Properties encode(Namer namer, Includer includer, Object... objects) throws Exception
      {
         props = new Properties();
         names = new HashMap();
         defaults = new MultiKeyMap();

         for (Object object : objects)
         {
            encode(object, props, namer, includer, names, defaults);
         }
         return props;
      }

      static String encode(Object object, Properties props, Namer namer, Includer includer, Map<Object, String> names, MultiKeyMap defaults) throws Exception
      {
         try
         {
            if (WRAPPER_TYPES.contains(object.getClass()))
               return object + "";

            String name = getName(object, namer, names);

            if (props.containsKey(name + ".class"))
               return name;

            if (name != null)
               props.put(name + ".class", object.getClass().getName());

            List<Field> fields = Utils.getFields(object.getClass());

            if (!defaults.containsKey(object.getClass()))
            {
               for (Field field : fields)
               {
                  if (Modifier.isTransient(field.getModifiers()))
                     continue;

                  if (Modifier.isStatic(field.getModifiers()))
                     continue;

                  if (Modifier.isFinal(field.getModifiers()))
                     continue;

                  try
                  {
                     Object clean = object.getClass().newInstance();
                     Object defaultValue = field.get(clean);

                     if (defaultValue != null && WRAPPER_TYPES.contains(defaultValue.getClass()))
                        defaults.put(object.getClass(), field.getName(), defaultValue);
                  }
                  catch (Exception ex)
                  {
                     // ex.printStackTrace();
                  }
               }
            }

            for (Field field : fields)
            {
               if (Modifier.isTransient(field.getModifiers()))
                  continue;

               if (Modifier.isStatic(field.getModifiers()))
                  continue;

               if (!includer.include(field))
                  continue;

               Object value = field.get(object);

               String fieldKey = name + "." + field.getName();
               if (value != null)
               {
                  if (value.getClass().isArray())
                     value = Arrays.asList(value);

                  if (value instanceof java.util.Collection)
                  {
                     if (((java.util.Collection) value).size() == 0)
                        continue;

                     List values = new ArrayList();
                     for (Object child : ((java.util.Collection) value))
                     {
                        String childKey = encode(child, props, namer, includer, names, defaults);
                        values.add(childKey);
                     }
                     if (name != null)
                        props.put(fieldKey, Utils.implode(",", values));
                  }
                  else if (value instanceof Map)
                  {
                     Map map = (Map) value;
                     if (map.size() == 0)
                        continue;

                     for (Object mapKey : map.keySet())
                     {
                        String encodedKey = encode(mapKey, props, namer, includer, names, defaults);
                        String encodedValue = encode(map.get(mapKey), props, namer, includer, names, defaults);
                        if (name != null)
                           props.put(fieldKey + "." + encodedKey, encodedValue);
                     }
                  }
                  else
                  {
                     if (WRAPPER_TYPES.contains(value.getClass()))
                     {
                        Object defaultVal = defaults.get(object.getClass(), field.getName());
                        if (defaultVal != null && defaultVal.equals(value))
                           continue;
                     }
                     else if (!includer.include(field))
                        continue;

                     if (name != null)
                        props.put(fieldKey, encode(value, props, namer, includer, names, defaults));
                  }
               }

            }
            return name;
         }
         catch (Exception ex)
         {
            System.err.println("Error encoding " + object.getClass().getName() + " - " + ex.getMessage());
            ex.printStackTrace();
            throw ex;
         }
      }

      public static String getName(Object object, Namer namer, Map<Object, String> names) throws Exception
      {
         if (names.containsKey(object))
            return names.get(object);

         if (namer != null)
         {
            String name = namer.getName(object);
            if (name != null)
            {
               names.put(object, name);
               return name;
            }
         }

         String name = "";

         Field nameField = Utils.getField("name", object.getClass());
         if (nameField != null)
         {
            name = nameField.get(object) + "";
         }

         name = "_anonymous_" + object.getClass().getSimpleName() + "_" + name + "_" + names.size();
         names.put(object, name);
         return name;
      }

      private static final Set<Class<?>> WRAPPER_TYPES = getWrapperTypes();

      private static Set<Class<?>> getWrapperTypes()
      {
         Set<Class<?>> ret = new HashSet<Class<?>>();
         ret.add(Boolean.class);
         ret.add(Character.class);
         ret.add(Byte.class);
         ret.add(Short.class);
         ret.add(Integer.class);
         ret.add(Long.class);
         ret.add(Float.class);
         ret.add(Double.class);
         ret.add(Void.class);
         ret.add(String.class);
         ret.add(Path.class);
         return ret;
      }
   }

   static <T> T cast(String key, String stringVal, Class<T> type, Field field, Map<String, Object> beans) throws Exception
   {
      if (String.class.isAssignableFrom(type))
      {
         return (T) stringVal;
      }
      else if (Path.class.isAssignableFrom(type))
      {
         return (T) new Path(stringVal);
      }
      else if (boolean.class.isAssignableFrom(type))
      {
         stringVal = stringVal.toLowerCase();
         return (T) (Boolean) (stringVal.equals("true") || stringVal.equals("t") || stringVal.equals("1"));
      }
      else if (byte.class.isAssignableFrom(type))
      {
         return (T) (Byte) Byte.parseByte(stringVal);
      }
      else if (char.class.isAssignableFrom(type))
      {
         return (T) (Character) stringVal.charAt(0);
      }
      else if (int.class.isAssignableFrom(type))
      {
         return (T) (Integer) Integer.parseInt(stringVal);
      }
      else if (long.class.isAssignableFrom(type))
      {
         return (T) (Long) Long.parseLong(stringVal);
      }
      else if (float.class.isAssignableFrom(type))
      {
         return (T) (Float) Float.parseFloat(stringVal);
      }
      else if (double.class.isAssignableFrom(type))
      {
         return (T) (Double) Double.parseDouble(stringVal);
      }
      else if (type.isArray() || java.util.Collection.class.isAssignableFrom(type))
      {
         Class subtype = null;
         if (type.isArray())
         {
            subtype = getArrayElementClass(type);
         }

         if (subtype == null && field != null)
         {
            subtype = (Class) ((((ParameterizedType) field.getGenericType()).getActualTypeArguments())[0]);
         }

         java.util.Collection list = java.util.Set.class.isAssignableFrom(type) ? new HashSet() : new ArrayList();
         String[] parts = stringVal.split(",");
         for (String part : parts)
         {
            part = part.trim();

            Object val = beans.containsKey(part) ? beans.get(part) : part;

            if (val != null && subtype != null && !subtype.isAssignableFrom(val.getClass()))
               val = cast(key, val + "", subtype, null, beans);

            list.add(val);
         }

         if (type.isArray())
            return (T) list.toArray((Object[]) Array.newInstance(subtype, list.size()));

         return (T) list;
      }
      else if (Map.class.isAssignableFrom(type))
      {
         Map map = new HashMap();
         String[] parts = stringVal.split(",");
         for (String part : parts)
         {
            Object val = beans.get(part);
            map.put(part, val);
         }
         return (T) map;
      }
      else
      {
         Object o = beans.get(stringVal);
         if (o != null && type.isAssignableFrom(o.getClass()))
            return (T) o;
      }

      if (stringVal != null)
         throw new RuntimeException("Error setting '" + key + "=" + stringVal + "'.  You must add support for type " + type + " into the Configurator");

      return null;
   }

   static Class getArrayElementClass(Class arrayClass) throws ClassNotFoundException
   {
      Class subtype = null;
      String typeStr = arrayClass.toString();

      if (typeStr.startsWith("class [Z"))
      {
         subtype = boolean.class;
      }
      else if (typeStr.startsWith("class [B"))
      {
         subtype = byte.class;
      }
      else if (typeStr.startsWith("class [C"))
      {
         subtype = char.class;
      }
      else if (typeStr.startsWith("class [I"))
      {
         subtype = int.class;
      }
      else if (typeStr.startsWith("class [J"))
      {
         subtype = long.class;
      }
      else if (typeStr.startsWith("class [F"))
      {
         subtype = float.class;
      }
      else if (typeStr.startsWith("class [D"))
      {
         subtype = double.class;
      }
      else //if (typeStr.startsWith("class ["))
      {
         subtype = Class.forName(typeStr.substring(typeStr.indexOf("[") + 2, typeStr.indexOf(";")));
      }
      return subtype;
   }

   void dump(Properties autoProps)
   {
      //      if (!Utils.empty(configOut))
      //      {
      //         String fileName = "./" + configOut.trim();
      //
      //         File file = new File(fileName);
      //
      //         log.info("writing merged config file to: '" + file.getCanonicalPath() + "'");
      //
      //         file.getParentFile().mkdirs();
      //         BufferedWriter out = new BufferedWriter(new FileWriter(file));

      //properties are sorted based on the number of "." segments they contain so that "shallow"
      //depth properties can be set before deeper depth properties.
      Properties sorted = new Properties()
         {
            public Enumeration keys()
            {
               Vector v = new Vector(Decoder.sort(keySet()));
               return v.elements();
            }
         };

      sorted.putAll(autoProps);
      autoProps = sorted;

      //         //autoProps.store(out, "");
      //
      //         //               for (String key : AutoWire.sort(autoProps.keySet()))
      //         //               {
      //         //                  String value = autoProps.getProperty(key);
      //         //                  if (shouldMask(key))
      //         //                     value = "###############";
      //         //               }
      //         out.flush();
      //         out.close();

      List<String> keys = Decoder.sort(autoProps.keySet());//new ArrayList(autoProps.keySet());
      Collections.sort(keys);
      log.info("-- merged user supplied configuration -------------------------");
      for (String key : keys)
      {
         String value = autoProps.getProperty(key);

         //         if (shouldMask(key))
         //            value = "###############";

         log.info(" > " + key + "=" + value);
      }
      log.info("-- end merged user supplied configuration ---------------------");

      //      }
   }

}
