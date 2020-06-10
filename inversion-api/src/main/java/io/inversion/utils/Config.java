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
package io.inversion.utils;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.Iterator;

import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.EnvironmentConfiguration;
import org.apache.commons.configuration2.SystemConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;

import io.inversion.ApiException;

/**
 * Global configuration properties access through a singleton wrapper around a Commons Configuration CompositeConfiguration object.
 * <p>
 * The name/value pairs found in configuration are used by the Configurator to reflectively set bean properties on Apis, Endpoints, Actions, Dbs and other Api model objects during Engine startup.  
 * <p>
 * Automatic/reflective parameter setting via the Configurator is the preferred way to do runtime dependency injection.  
 * You can however directly use this classes Config.getXYX() methods to look up key/value pairs at runtime.  
 * <p>
 * You can access and modify the underlying CompositeConfiguration object to change where properties are pulled form.
 * <p>
 * By default, listed in order of priority, properties are merged from:
 * <ol>
 *  <li>a "${user.dir}/.env" properties file if one exists.
 *  <li>system properties
 *  <li>environment variables
 *  <li>${configPath}/inversion[-][0-100]-${configProfile}.properties files if they exist
 *  <li>${configPath}/inversion-${configProfile}[-][0-100].properties files if they exist
 *  <li>${configPath}/inversion[-][0-100].properties files if they exist
 * </ol>
 * 
 * <p>For example:
 * <ul>
 *   <li>if key "foo" is found in "${user.dir}/.env" the corresponding value will always be returned no matter what other source have key "foo"
 *   <li>if key "bar" exists in inversion-${configProfile}.properties and inversion.properties, the value from inversion-${configProfile}.properties will be returned
 *   <li>if key "abc" is environment and inversion-${configProfile}.properties, the value from the environment will be returned
 *   <li>if key "xyz" is in inversion.properties and inversion-10.properties, the value from inversion-10.properties will be returned
 * </ul>
 * <p>
 * If you have a custom configuration data source, such as a database or secrets key vault, you can add those to the FRONT of the 
 * CompositeConfiguration via <code>Config.getConfiguration().addConfigurationFirst(MY_CONFIG_OBJECT)</code> to make sure those values
 * are given the highest priority.  Use <code>CompositeConfiguration.addConfiguration</code> to give your custom props lowest priority.
 * 
 * @see io.inversion.utils.Configurator
 * @see <a href="http://commons.apache.org/proper/commons-configuration/apidocs/org/apache/commons/configuration2/CombinedConfiguration.html">org.apache.commons.configuration2.CombinedConfiguration</a>
 */
public class Config
{
   static CompositeConfiguration configuration = null;

   private Config()
   {

   }

   public static synchronized boolean hasConfiguration()
   {
      return configuration != null;
   }

   /**
    * If <code>configuration</code> is null, <code>loadConfiguration</code> is called
    * to lazy load the default config. 
    * 
    * @return the system wide CompositeConfiguration
    */
   public static synchronized CompositeConfiguration getConfiguration()
   {
      if (configuration == null)
      {
         loadConfiguration(null, null);
      }
      return configuration;
   }

   /**
    * Sets the system wide CompositeConfiguration.
    * <p>
    * Generally, you don't need to explicitly call this, as accessing any of this 
    * classes getters will cause the default configuration to be loaded via <code>loadConfiguration()</code>
    * if <code>configuration</code> is null.
    */
   public static synchronized void setConfiguration(CompositeConfiguration configuration)
   {
      Config.configuration = configuration;
   }

   /**
    * Nulls out the system wide CompositeConfiguration, same as <code>setConfiguration(null)</code>
    */
   public static synchronized void clearConfiguration()
   {
      configuration = null;
   }

   /**
    * Creates a new CompositeConfiguration with individual Configuration objects loaded with key/value pairs
    * from sources as described in the class comment above.
    * <p> 
    * If <code>configPath</code> is null, it will be looked up via Utils.findProperty with the following keys:
    * <ol>
    *   <li>"inversion.configPath"
    *   </li>"configPath"
    * </ol> 
    * 
    * <p>
    * If <code>configProfile</code> is null, it will be looked up via Utils.findProperty with the following keys:
    * <ol>
    *   <li>"inversion.configProfile"
    *   <li>"inversion.profile"
    *   <li>"spring.profiles.active"
    *   <li>"configProfile"
    *   <li>"profile"
    * </ol>
    * 
    * 
    * @param configPath  the path use to locate 'inversion.properties' files via <code>getResource</code>
    * @param configProfile  the runtime profile used to load some inversion-${configProfile}-[0-100].properties files and not others.
    * 
    * @see #getResource(String)
    * @see Utils.findProperty
    */
   public static synchronized void loadConfiguration(String configPath, String configProfile)
   {
      configPath = configPath != null ? configPath : Utils.findProperty("inversion.configPath", "configPath");
      configProfile = configProfile != null ? configProfile : Utils.findProperty("inversion.configProfile", "inversion.profile", "spring.profiles.active", "configProfile", "profile");

      Configurations configs = new Configurations();
      CompositeConfiguration configuration = new CompositeConfiguration();

      try
      {
         URL url = getResource(".env");
         if (url != null)
         {
            configuration.addConfiguration(configs.properties(url));
         }

         configuration.addConfiguration(new SystemConfiguration());
         configuration.addConfiguration(new EnvironmentConfiguration());

         configPath = configPath != null ? configPath : "";

         if (configPath.length() > 0 && !(configPath.endsWith("/") || configPath.endsWith("\\")))
            configPath += "/";

         if (configProfile != null)
         {
            for (int i = 100; i >= -1; i--)
            {
               String fileName = null;

               fileName = configPath + "inversion" + (i < 0 ? "" : i) + "-" + configProfile + ".properties";
               url = getResource(fileName);

               if (url == null)
               {
                  fileName = configPath + "inversion" + "-" + (i < 0 ? "" : i) + "-" + configProfile + ".properties";
                  url = getResource(fileName);
               }
               if (url == null)
               {
                  fileName = configPath + "inversion" + "-" + configProfile + (i < 0 ? "" : i) + ".properties";
                  url = getResource(fileName);
               }
               if (url == null)
               {
                  fileName = configPath + "inversion" + "-" + configProfile + "-" + (i < 0 ? "" : i) + ".properties";
                  url = getResource(fileName);
               }

               if (url != null)
               {
                  configuration.addConfiguration(configs.properties(url));
               }
            }
         }

         for (int i = 100; i >= -1; i--)
         {
            String fileName = configPath + "inversion" + (i < 0 ? "" : i) + ".properties";
            url = getResource(fileName);

            if (url == null)
            {
               fileName = configPath + "inversion" + "-" + (i < 0 ? "" : i) + ".properties";
               url = getResource(fileName);
            }

            if (url != null)
            {
               configuration.addConfiguration(configs.properties(url));
            }
         }
      }
      catch (Exception ex)
      {
         ApiException.throw500InternalServerError(ex);
      }

      Config.configuration = configuration;
   }

   /**
    * Attempts to locate a resource URL for <code>name</code> via the ClassLoader or as a file path relative to ${user.dir}.
    * 
    * @param name
    * @return
    */
   protected static URL getResource(String name)
   {
      try
      {
         URL url = null;

         url = Config.class.getClassLoader().getResource(name);
         if (url == null)
         {
            File file = new File(System.getProperty("user.dir"), name);
            if (file.exists())
               url = file.toURI().toURL();
         }

         return url;
      }
      catch (Exception ex)
      {
         throw new RuntimeException(ex);
      }
   }

   public static Iterator<String> getKeys()
   {
      getConfiguration();//lazy loads default config if necessary
      return configuration.getKeys();
   }

   public static Object getProperty(String key)
   {
      getConfiguration();//lazy loads default config if necessary
      return configuration.getProperty(key);
   }

   public static boolean getBoolean(String key)
   {
      getConfiguration();//lazy loads default config if necessary
      return configuration.getBoolean(key);
   }

   public static boolean getBoolean(String key, boolean defaultValue)
   {
      getConfiguration();//lazy loads default config if necessary
      return configuration.getBoolean(key, defaultValue);
   }

   public static Boolean getBoolean(String key, Boolean defaultValue)
   {
      getConfiguration();//lazy loads default config if necessary
      return configuration.getBoolean(key, defaultValue);
   }

   public static byte getByte(String key)
   {
      getConfiguration();//lazy loads default config if necessary
      return configuration.getByte(key);
   }

   public static byte getByte(String key, byte defaultValue)
   {
      getConfiguration();//lazy loads default config if necessary
      return configuration.getByte(key, defaultValue);
   }

   public static Byte getByte(String key, Byte defaultValue)
   {
      getConfiguration();//lazy loads default config if necessary
      return configuration.getByte(key, defaultValue);
   }

   public static double getDouble(String key)
   {
      getConfiguration();//lazy loads default config if necessary
      return configuration.getDouble(key);
   }

   public static double getDouble(String key, double defaultValue)
   {
      getConfiguration();//lazy loads default config if necessary
      return configuration.getDouble(key, defaultValue);
   }

   public static Double getDouble(String key, Double defaultValue)
   {
      getConfiguration();//lazy loads default config if necessary
      return configuration.getDouble(key, defaultValue);
   }

   public static float getFloat(String key)
   {
      getConfiguration();//lazy loads default config if necessary
      return configuration.getFloat(key);
   }

   public static float getFloat(String key, float defaultValue)
   {
      getConfiguration();//lazy loads default config if necessary
      return configuration.getFloat(key, defaultValue);
   }

   public static Float getFloat(String key, Float defaultValue)
   {
      getConfiguration();//lazy loads default config if necessary
      return configuration.getFloat(key, defaultValue);
   }

   public static int getInt(String key)
   {
      getConfiguration();//lazy loads default config if necessary
      return configuration.getInt(key);
   }

   public static int getInt(String key, int defaultValue)
   {
      getConfiguration();//lazy loads default config if necessary
      return configuration.getInt(key, defaultValue);
   }

   public static Integer getInteger(String key, Integer defaultValue)
   {
      getConfiguration();//lazy loads default config if necessary
      return configuration.getInteger(key, defaultValue);
   }

   public static long getLong(String key)
   {
      getConfiguration();//lazy loads default config if necessary
      return configuration.getLong(key);
   }

   public static long getLong(String key, long defaultValue)
   {
      getConfiguration();//lazy loads default config if necessary
      return configuration.getLong(key, defaultValue);
   }

   public static Long getLong(String key, Long defaultValue)
   {
      getConfiguration();//lazy loads default config if necessary
      return configuration.getLong(key, defaultValue);
   }

   public static short getShort(String key)
   {
      getConfiguration();//lazy loads default config if necessary
      return configuration.getShort(key);
   }

   public static short getShort(String key, short defaultValue)
   {
      getConfiguration();//lazy loads default config if necessary
      return configuration.getShort(key, defaultValue);
   }

   public static Short getShort(String key, Short defaultValue)
   {
      getConfiguration();//lazy loads default config if necessary
      return configuration.getShort(key, defaultValue);
   }

   public static BigDecimal getBigDecimal(String key)
   {
      getConfiguration();//lazy loads default config if necessary
      return configuration.getBigDecimal(key);
   }

   public static BigDecimal getBigDecimal(String key, BigDecimal defaultValue)
   {
      getConfiguration();//lazy loads default config if necessary
      return configuration.getBigDecimal(key, defaultValue);
   }

   public static BigInteger getBigInteger(String key)
   {
      getConfiguration();//lazy loads default config if necessary
      return configuration.getBigInteger(key);
   }

   public static BigInteger getBigInteger(String key, BigInteger defaultValue)
   {
      getConfiguration();//lazy loads default config if necessary
      return configuration.getBigInteger(key, defaultValue);
   }

   public static String getString(String key)
   {
      getConfiguration();//lazy loads default config if necessary
      return configuration.getString(key);
   }

   public static String getString(String key, String defaultValue)
   {
      getConfiguration();//lazy loads default config if necessary
      return configuration.getString(key, defaultValue);
   }

}
