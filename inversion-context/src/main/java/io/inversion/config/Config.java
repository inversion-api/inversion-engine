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
package io.inversion.config;


import ioi.inversion.utils.Utils;
import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.*;

/**
 * Global configuration properties access through a singleton wrapper around a Commons Configuration CompositeConfiguration object.
 * <p>
 * The name/value pairs found in configuration are used by the Wirer to reflectively set bean properties on Apis, Endpoints, Actions, Dbs and other Api model objects during Engine startup.
 * <p>
 * Automatic/reflective parameter setting via the Wirer is the preferred way to do runtime dependency injection.
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
 * @see Context
 * @see <a href="http://commons.apache.org/proper/commons-configuration/apidocs/org/apache/commons/configuration2/CombinedConfiguration.html">org.apache.commons.configuration2.CombinedConfiguration</a>
 */
public class Config {
    static CompositeConfiguration configuration = null;

    public static final List<String> CONFIG_PATH_KEYS    = Collections.unmodifiableList(Arrays.asList("inversion.configPath", "configPath"));
    public static final List<String> CONFIG_PROFILE_KEYS = Collections.unmodifiableList(Arrays.asList("inversion.configProfile", "inversion.profile", "spring.profiles.active", "configProfile", "profile"));
    public static final String       CONFIG_FILE_NAME    = "inversion.properties";
    public static final int          CONFIG_FILE_COUNTER = 100;
    public static final String       CONFIG_SOURCE_PROP  = "$inversion_config_source";

    private Config() {

    }

    public static String findConfigPath() {
        for (String key : CONFIG_PATH_KEYS) {
            String val = Utils.getSysEnvProp(key);
            if (val != null)
                return val;
        }
        return null;
    }

    public static String findConfigProfile() {
        for (String key : CONFIG_PROFILE_KEYS) {
            String val = Utils.getSysEnvProp(key);
            if (val != null)
                return val;
        }
        return null;
    }

    public static synchronized boolean hasConfiguration() {
        return configuration != null;
    }

    /**
     * Sets the system wide CompositeConfiguration.
     * <p>
     * Generally, you don't need to explicitly call this, as accessing any of this
     * classes getters will cause the default configuration to be loaded via <code>loadConfiguration()</code>
     * if <code>configuration</code> is null.
     *
     * @param configuration the configuration to use
     */
    public static synchronized void setConfiguration(CompositeConfiguration configuration) {
        Config.configuration = configuration;
    }

    /**
     * Nulls out the system wide CompositeConfiguration, same as <code>setConfiguration(null)</code>
     */
    public static synchronized void clearConfiguration() {
        configuration = null;
    }

    /**
     * If <code>configuration</code> is null, <code>loadConfiguration</code> is called
     * to lazy load the default config.
     *
     * @return the system wide CompositeConfiguration
     */
    public static synchronized CompositeConfiguration getConfiguration() {
        if(configuration == null)
            loadConfiguration(null, null, null);
        return configuration;
    }


    /**
     * Creates a new CompositeConfiguration with individual Configuration objects loaded with key/value pairs
     * from sources as described in the class comment above.
     * <p>
     * If <code>configPath</code> is null, it will be looked up via Utils.findProperty with the following keys:
     * <ol>
     *   <li>"inversion.configPath"
     *   <li>"configPath"
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
     * @param inConfigPath    the path use to locate 'inversion.properties' files via <code>getResource</code>
     * @param inConfigProfile the runtime profile used to load some inversion[-${configProfile}][-0-100].properties files and not others.
     * @see Utils#findSysEnvProp(String...)
     */
    public static synchronized CompositeConfiguration loadConfiguration(Object caller, String inConfigPath, String inConfigProfile) {
//
//        if (Config.configuration != null)
//            return Config.configuration;
//
//        if (Config.configPath != null && inConfigPath != null && !Config.configPath.equalsIgnoreCase(inConfigPath))
//            throw new ApiException("The configPath has already been set");
//
//        if (Config.configProfile != null && inConfigProfile != null && !Config.configProfile.equalsIgnoreCase(inConfigProfile))
//            throw new ApiException("The configProfile has already been set");

        String configPath = !Utils.empty(inConfigPath) ? inConfigPath : findConfigPath();
        String configProfile = !Utils.empty(inConfigProfile) ? inConfigProfile : findConfigProfile();

        Configurations         configs       = new Configurations();
        CompositeConfiguration configuration = new CompositeConfiguration();

        System.out.println("LOADING CONFIGURATION...");
        System.out.println("  - configPath    : " + configPath);
        System.out.println("  - configProfile : " + configProfile);

        try {
            URL url = findUrl(caller, ".env");
            if (url != null) {
                System.out.println("  - loading file  : " + url);
                configuration.addConfiguration(configs.properties(url));
            }

            //configuration.addConfiguration(new SystemConfiguration());
            //configuration.addConfiguration(new EnvironmentConfiguration());

            String usePath = configPath != null ? configPath : "";

            if (usePath.length() > 0 && !(usePath.endsWith("/") || usePath.endsWith("\\")))
                usePath += "/";

            List<URL> files = findFiles(caller, CONFIG_FILE_NAME, CONFIG_FILE_COUNTER, configPath, configProfile);

            for (URL u : files) {
                System.out.println("  - loading file  : " + u);
                PropertiesConfiguration fileConfig = configs.properties(u);
                if (!fileConfig.containsKey(CONFIG_SOURCE_PROP))
                    fileConfig.setProperty(CONFIG_SOURCE_PROP, u.toString());

                configuration.addConfiguration(fileConfig);
            }


        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        Config.configuration = configuration;
        return Config.configuration;
    }

    static List<URL> findFiles(Object caller, String file) {
        return findFiles(caller, file, 1, findConfigPath(), findConfigProfile());
    }

    static List<URL> findFiles(Object caller, String file, int maxCounter, String inConfigPath, String inConfigProfile) {

        //List<URL> files = new ArrayList();

        String prefix = file.substring(0, file.lastIndexOf('.'));
        String suffix = file.substring(file.lastIndexOf('.'));

        String configPath = !Utils.empty(inConfigPath) ? inConfigPath : findConfigPath();
        String configProfile = !Utils.empty(inConfigProfile) ? inConfigProfile : findConfigProfile();

        String configPathPrefix = Utils.implode("/", Utils.explode("/", inConfigPath, prefix));

        if (configPath != null && configPath.startsWith("/"))
            configPathPrefix = "/" + configPathPrefix;

        List<URL> defaultFiles = new ArrayList<>();
        for (int i = 0; i < maxCounter; i++) {
            String fileName = configPathPrefix + (i > 0 ? i : "") + suffix;
            URL    url      = findUrl(caller, fileName);

            if (url == null) {
                fileName = configPathPrefix + "-" + (i > 0 ? i : "") + suffix;
                url = findUrl(caller, fileName);
            }

            if (url != null) {
                defaultFiles.add(url);
            }
        }

        List<URL> profileFiles = new ArrayList<>();
        if (configProfile != null) {
            for (int i = 0; i < maxCounter; i++) {
                String fileName = configPathPrefix + (i > 0 ? i : "") + "-" + configProfile + suffix;
                URL    url      = findUrl(caller, fileName);
                if (url == null) {
                    fileName = configPathPrefix + "-" + (i > 0 ? i : "") + "-" + configProfile + suffix;
                    url = findUrl(caller, fileName);
                }
                if (url == null) {
                    fileName = configPathPrefix + "-" + configProfile + (i > 0 ? i : "") + suffix;
                    url = findUrl(caller, fileName);
                }
                if (url == null) {
                    fileName = configPathPrefix + "-" + configProfile + "-" + (i > 0 ? i : "") + suffix;
                    url = findUrl(caller, fileName);
                }

                if (url != null) {
                    profileFiles.add(url);
                }
            }
        }

        Collections.reverse(defaultFiles);
        Collections.reverse(profileFiles);
        List<URL> files = new ArrayList<>();
        files.addAll(profileFiles);
        files.addAll(defaultFiles);


        return files;
    }

    /**
     * Attempts to locate a resource URL for <code>name</code> via the ClassLoader or as a file path relative to ${user.dir}.
     *
     * @param name a string identifier for the resource to find
     * @return a url
     */
    static URL findUrl(Object caller, String name) {
        try {

            Object ctx = caller != null ? caller : Config.class;

            URL url = caller.getClass().getResource(name);
            if(url == null && !name.startsWith("/"))
                url = ctx.getClass().getResource("/" + name);

            if (url == null) {
                File file = new File(System.getProperty("user.dir"), name);
                if (file.exists())
                    url = file.toURI().toURL();
            }

            return url;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static TreeSet<String> getKeys() {
        TreeSet<String> set = new TreeSet<>();
        Configuration config = getConfiguration();
        Iterator<String> keys = config.getKeys();
        String key;
        while(keys.hasNext()){
            key = keys.next();
            set.add(key);
        }
        return set;
    }

    public static TreeMap<String, String> getProperties() {
        TreeMap<String, String> map = new TreeMap<>();
        Configuration config = getConfiguration();
        Iterator<String> keys = config.getKeys();
        String key;
        while(keys.hasNext()){
            key = keys.next();
            map.put(key, config.getString(key));
        }
        return map;
    }

    public static Object getProperty(String key) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getProperty(key);
    }

    public static boolean getBoolean(String key) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getBoolean(key);
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getBoolean(key, defaultValue);
    }

    public static Boolean getBoolean(String key, Boolean defaultValue) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getBoolean(key, defaultValue);
    }

    public static byte getByte(String key) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getByte(key);
    }

    public static byte getByte(String key, byte defaultValue) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getByte(key, defaultValue);
    }

    public static Byte getByte(String key, Byte defaultValue) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getByte(key, defaultValue);
    }

    public static double getDouble(String key) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getDouble(key);
    }

    public static double getDouble(String key, double defaultValue) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getDouble(key, defaultValue);
    }

    public static Double getDouble(String key, Double defaultValue) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getDouble(key, defaultValue);
    }

    public static float getFloat(String key) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getFloat(key);
    }

    public static float getFloat(String key, float defaultValue) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getFloat(key, defaultValue);
    }

    public static Float getFloat(String key, Float defaultValue) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getFloat(key, defaultValue);
    }

    public static int getInt(String key) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getInt(key);
    }

    public static int getInt(String key, int defaultValue) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getInt(key, defaultValue);
    }

    public static Integer getInteger(String key, Integer defaultValue) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getInteger(key, defaultValue);
    }

    public static long getLong(String key) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getLong(key);
    }

    public static long getLong(String key, long defaultValue) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getLong(key, defaultValue);
    }

    public static Long getLong(String key, Long defaultValue) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getLong(key, defaultValue);
    }

    public static short getShort(String key) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getShort(key);
    }

    public static short getShort(String key, short defaultValue) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getShort(key, defaultValue);
    }

    public static Short getShort(String key, Short defaultValue) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getShort(key, defaultValue);
    }

    public static BigDecimal getBigDecimal(String key) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getBigDecimal(key);
    }

    public static BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getBigDecimal(key, defaultValue);
    }

    public static BigInteger getBigInteger(String key) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getBigInteger(key);
    }

    public static BigInteger getBigInteger(String key, BigInteger defaultValue) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getBigInteger(key, defaultValue);
    }

    public static String getString(String key) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getString(key);
    }

    public static String getString(String key, String defaultValue) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getString(key, defaultValue);
    }

}
