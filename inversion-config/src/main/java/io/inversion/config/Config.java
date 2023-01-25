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


import io.inversion.utils.Utils;
import org.apache.commons.configuration2.*;
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
 * @see <a href="http://commons.apache.org/proper/commons-configuration/apidocs/org/apache/commons/configuration2/CombinedConfiguration.html">org.apache.commons.configuration2.CombinedConfiguration</a>
 */
public class Config {

    public static final List<String> CONFIG_PATH_KEYS    = Collections.unmodifiableList(Arrays.asList("{appName}.configPath", "configPath"));
    public static final List<String> CONFIG_PROFILE_KEYS = Collections.unmodifiableList(Arrays.asList("{appName}.configProfile", "{appName}.profile", "spring.profiles.active", "configProfile", "profile"));
    public static final int          CONFIG_FILE_COUNTER = 100;

    private Config() {

    }

    Object                 root;
    String                 configApp;
    String                 configProfile;
    String                 configPath;
    CompositeConfiguration configuration = null;


    public static Config getConfig(String appName) {
        return getConfig(appName, null, null, null);
    }

    public static Config getConfig(String appName, String configPath) {
        return getConfig(appName, configPath, null, null);
    }

    public static Config getConfig(String appName, String configPath, String configProfile) {
        return getConfig(appName, configPath, configProfile, null);
    }

    public static Config getConfig(String configApp, String configPath, String configProfile, Object root) {

        Config config = new Config();
        if (configApp == null) {
            configApp = Utils.getSysEnvProp("configApp");
        }

        if (configProfile == null) {
            configProfile = findConfigProfile(configApp);
        }

        if (configPath == null)
            configPath = findConfigPath(configApp);

        config.configApp = configApp;
        config.configProfile = configProfile;
        config.configPath = configPath;
        config.root = root != null ? root : config;

        return config;
    }


    static String findConfigPath(String appName) {
        for (String key : CONFIG_PATH_KEYS) {
            key = key.replace("{appName}", appName);
            String val = Utils.getSysEnvProp(key);
            if (val != null)
                return val;
        }
        return null;
    }

    static String findConfigProfile(String appName) {
        for (String key : CONFIG_PROFILE_KEYS) {
            key = key.replace("{appName}", appName);
            String val = Utils.getSysEnvProp(key);
            if (val != null)
                return val;
        }
        return null;
    }

//    public static synchronized boolean hasConfiguration() {
//        return configuration != null;
//    }

    /**
     * Sets the system wide CompositeConfiguration.
     * <p>
     * Generally, you don't need to explicitly call this, as accessing any of this
     * classes getters will cause the default configuration to be loaded via <code>loadConfiguration()</code>
     * if <code>configuration</code> is null.
     *
     * @param configuration the configuration to use
     */
    public synchronized void setConfiguration(CompositeConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Nulls out the system wide CompositeConfiguration, same as <code>setConfiguration(null)</code>
     */
    public synchronized void clearConfiguration() {
        this.configuration = null;
    }

    /**
     * If <code>configuration</code> is null, <code>loadConfiguration</code> is called
     * to lazy load the default config.
     *
     * @return the system wide CompositeConfiguration
     */
    public synchronized CompositeConfiguration getConfiguration() {
        if (configuration == null)
            configuration = loadConfiguration();
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
     * @see Utils#findSysEnvProp(String...)
     */
    synchronized CompositeConfiguration loadConfiguration() {

        Configurations         configs       = new Configurations();
        CompositeConfiguration configuration = new CompositeConfiguration();

        System.out.println("LOADING CONFIGURATION...");
        System.out.println("  - configApp     : " + configApp);
        System.out.println("  - configPath    : " + configPath);
        System.out.println("  - configProfile : " + configProfile);

        try {

            configuration.addConfiguration(new SystemConfiguration());
            configuration.addConfiguration(new EnvironmentConfiguration());

            List<String> names = new ArrayList();
            if(configProfile != null)
                names.add("." + configApp + "-" + configProfile + ".env");
            names.add("." + configApp + ".env");
            names.add(".env");

            for(URL url : findUrls(root, names)){
                PropertiesConfiguration props = configs.properties(url);
                props.addProperty("@@source", url + "");
                configuration.addConfiguration(props);
            }

            for (URL url : findFiles(configApp + ".properties", CONFIG_FILE_COUNTER)) {
                PropertiesConfiguration props = configs.properties(url);
                props.addProperty("@@source", url + "");
                configuration.addConfiguration(props);
            }

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        for(int i=0; i<configuration.getNumberOfConfigurations(); i++){
            Configuration config = configuration.getConfiguration(i);
            if(config instanceof SystemConfiguration){
                System.out.println("  - config source : System Properties");
            }
            else if(config instanceof EnvironmentConfiguration){
                System.out.println("  - config source : Environment Variables");
            }
            else{
                String source = config.getString("@@source");
                if(source != null)
                    System.out.println("  - config source : " + source);
            }
        }

        this.configuration = configuration;
        return configuration;
    }

    List<URL> findFiles(String file, int maxCounter) {

        String prefix = file.substring(0, file.lastIndexOf('.'));
        String suffix = file.substring(file.lastIndexOf('.'));

        String configPathPrefix = Utils.implode("/", Utils.explode("/", configPath, prefix));

        if (configPath != null && configPath.startsWith("/"))
            configPathPrefix = "/" + configPathPrefix;

        List<URL> defaultFiles = new ArrayList<>();
        for (int i = 0; i < maxCounter; i++) {
            String fileName = configPathPrefix + (i > 0 ? i : "") + suffix;
            URL    url      = findUrl(root, fileName);

            if (url == null) {
                fileName = configPathPrefix + "-" + (i > 0 ? i : "") + suffix;
                url = findUrl(root, fileName);
            }

            if (url != null) {
                defaultFiles.add(url);
            }
        }

        List<URL> profileFiles = new ArrayList<>();
        if (configProfile != null) {
            for (int i = 0; i < maxCounter; i++) {
                String fileName = configPathPrefix + (i > 0 ? i : "") + "-" + configProfile + suffix;
                URL    url      = findUrl(root, fileName);
                if (url == null) {
                    fileName = configPathPrefix + "-" + (i > 0 ? i : "") + "-" + configProfile + suffix;
                    url = findUrl(root, fileName);
                }
                if (url == null) {
                    fileName = configPathPrefix + "-" + configProfile + (i > 0 ? i : "") + suffix;
                    url = findUrl(root, fileName);
                }
                if (url == null) {
                    fileName = configPathPrefix + "-" + configProfile + "-" + (i > 0 ? i : "") + suffix;
                    url = findUrl(root, fileName);
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

    public static List<URL> findUrls(Object caller, List<String> names) {
        List<URL> found = new ArrayList<>();
        for(String name : names){
            URL url = findUrl(caller, name);
            if(url != null){
                found.add(url);
            }
        }
        return found;
    }

    /**
     * Attempts to locate a resource URL for <code>name</code> via the ClassLoader or as a file path relative to ${user.dir}.
     *
     * @param name a string identifier for the resource to find
     * @return a url
     */
    public static URL findUrl(Object caller, String name) {
        try {

            Object ctx = caller != null ? caller : Config.class;

            URL url = caller.getClass().getResource(name);
            if (url == null && !name.startsWith("/"))
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

    public TreeSet<String> getKeys() {
        TreeSet<String>  set    = new TreeSet<>();
        Configuration    config = getConfiguration();
        Iterator<String> keys   = config.getKeys();
        String           key;
        while (keys.hasNext()) {
            key = keys.next();
            set.add(key);
        }
        return set;
    }

    public TreeMap<String, String> getProperties() {
        TreeMap<String, String> map    = new TreeMap<>();
        Configuration           config = getConfiguration();
        Iterator<String>        keys   = config.getKeys();
        String                  key;
        while (keys.hasNext()) {
            key = keys.next();
            map.put(key, config.getString(key));
        }
        return map;
    }

    public Object getProperty(String key) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getProperty(key);
    }

    public boolean getBoolean(String key) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getBoolean(key);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getBoolean(key, defaultValue);
    }

    public Boolean getBoolean(String key, Boolean defaultValue) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getBoolean(key, defaultValue);
    }

    public byte getByte(String key) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getByte(key);
    }

    public byte getByte(String key, byte defaultValue) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getByte(key, defaultValue);
    }

    public Byte getByte(String key, Byte defaultValue) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getByte(key, defaultValue);
    }

    public double getDouble(String key) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getDouble(key);
    }

    public double getDouble(String key, double defaultValue) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getDouble(key, defaultValue);
    }

    public Double getDouble(String key, Double defaultValue) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getDouble(key, defaultValue);
    }

    public float getFloat(String key) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getFloat(key);
    }

    public float getFloat(String key, float defaultValue) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getFloat(key, defaultValue);
    }

    public Float getFloat(String key, Float defaultValue) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getFloat(key, defaultValue);
    }

    public int getInt(String key) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getInt(key);
    }

    public int getInt(String key, int defaultValue) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getInt(key, defaultValue);
    }

    public Integer getInteger(String key, Integer defaultValue) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getInteger(key, defaultValue);
    }

    public long getLong(String key) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getLong(key);
    }

    public long getLong(String key, long defaultValue) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getLong(key, defaultValue);
    }

    public Long getLong(String key, Long defaultValue) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getLong(key, defaultValue);
    }

    public short getShort(String key) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getShort(key);
    }

    public short getShort(String key, short defaultValue) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getShort(key, defaultValue);
    }

    public Short getShort(String key, Short defaultValue) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getShort(key, defaultValue);
    }

    public BigDecimal getBigDecimal(String key) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getBigDecimal(key);
    }

    public BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getBigDecimal(key, defaultValue);
    }

    public BigInteger getBigInteger(String key) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getBigInteger(key);
    }

    public BigInteger getBigInteger(String key, BigInteger defaultValue) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getBigInteger(key, defaultValue);
    }

    public String getString(String key) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getString(key);
    }

    public String getString(String key, String defaultValue) {
        getConfiguration();//lazy loads default config if necessary
        return configuration.getString(key, defaultValue);
    }

}
