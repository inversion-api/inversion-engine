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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class for parsing and working with HTTP(S) URLs.
 * <p>
 * Not for use with non HTTP(S) urls.
 * <p>
 * A number of different utility methods are provided to make it simple to find or remove different query string keys.
 */
public class Url {
    /**
     * The url string as supplied to the constructor, with 'http://localhost/' prepended if the constructor url arg did not contain a host
     */
    protected final String original;

    /**
     * The url protocol, either http or https
     */
    protected String protocol = "http";

    /**
     * The url host.
     */
    protected String host = null;

    /**
     * The url port number if a custom port was provided
     */
    protected int port = 0;

    /**
     * The part of the url after the host/port before the query string.
     */
    protected Path path = null;

    /**
     * A case insensitive map of query string name/value pairs that preserves iteration order
     * <p>
     * Implementation Node: <code>params</code>is not actually JSON, JSNode is used as the map
     * implementation here simply because it is an affective case insensitive map that preserves
     * the original key case key iteration order.
     */
    protected JSNode params = new JSNode();

    /**
     * Parses <code>url</code> into its protocol, host, port, path and query string param parts.
     * <p>
     * If <code>url</code> does not start with "http://" or "https://" then "http://localhost/" will be prepended.
     *
     * @param url url string
     */
    public Url(String url) {
        String path;

        if (url.indexOf(":/") > 0 && !url.contains("://"))
            url = url.replaceAll(":/", "://");

        url = url.replace("&amp;", "&");
        if (!(url.startsWith("http://") || url.startsWith("https://")))
            url = "http://localhost" + (!url.startsWith("/") ? "/" : "") + url;

        original = url;

        try {
            int queryIndex = url.indexOf('?');
            if (queryIndex >= 0) {
                String query = url.substring(queryIndex + 1);
                url = url.substring(0, queryIndex);

                withQueryString(query);
            }

            //replace slashes after stripping off query to leave query as it was found
            url = url.replace('\\', '/');

            int potocolEnd = url.indexOf("://");
            if (potocolEnd < 0) {
                path = url;
            } else {
                //-- parse a full url
                protocol = url.substring(0, url.indexOf(':'));

                int hostStart = url.indexOf('/') + 2;
                int hostEnd   = url.indexOf(':', hostStart);

                //--this is probably ah file url like file://c:/
                //--so don't cound this colon
                //if(hostEnd - hostStart <= 1)
                //   hostEnd = url.indexOf(':', hostEnd + 1);
                if (hostEnd < 0 || hostEnd > url.indexOf('/', hostStart)) {
                    hostEnd = url.indexOf('/', hostStart);
                }
                if (hostEnd < 0) {
                    url += "/";
                    hostEnd = url.indexOf('/', hostStart);
                }

                host = url.substring(hostStart, hostEnd);

                String rest = url.substring(hostEnd);

                if (rest.indexOf(':') > -1) {
                    int nextColon = rest.indexOf(':');
                    int nextSlash = rest.indexOf('/');

                    if (nextColon < nextSlash) {
                        String portString = rest.substring(nextColon + 1, nextSlash);
                        port = Integer.parseInt(portString);
                        rest = rest.substring(nextSlash);
                    }
                }

                path = rest;
            }

            if (path.length() == 0) {
                path = "/";
            } else if (path.charAt(0) != '/') {
                path = '/' + url;
            }

            if (!Utils.empty(path))
                this.path = new Path(path);

        } catch (Exception ex) {
            System.err.println("Error parsing url \"" + url + "\"");
            ex.printStackTrace();
        }
    }

    /**
     * Creates a UTF-8 url encoded query string, not including a leading "?" with key value pairs separated by a '&amp;'
     *
     * @param params the key/value pairs to encode
     * @return a UTF-8 url encoded query string
     * @see java.net.URLEncoder#encode(String, String)
     */
    public static String toQueryString(Map params) {
        StringBuilder query = new StringBuilder();
        if (params != null) {
            for (Object o : params.keySet()) {
                String key = o.toString();
                try {
                    if (params.get(key) != null) {
                        query.append(URLEncoder.encode(key, "UTF-8")).append("=").append(URLEncoder.encode(params.get(key).toString(), "UTF-8")).append("&");
                    } else {
                        query.append(URLEncoder.encode(key, "UTF-8")).append("&");
                    }

                } catch (UnsupportedEncodingException e) {
                    Utils.rethrow(e);
                }
            }
            if (query.length() > 0)
                query = new StringBuilder(query.substring(0, query.length() - 1));
        }

        String str = query.toString();
        str = str.replace("%28", "(");
        str = str.replace("%29", ")");
        str = str.replace("%2C", ",");

        return str;
    }

    /**
     * Generates a string string representation of this url with any query string parameters URL encoded and port number included only if it differs from the standard protocol port.
     *
     * @return the string representation of this url
     */
    public String toString() {
        String url = protocol + "://" + host;

        if (port > 0) {
            if (!((port == 80 && "http".equalsIgnoreCase(protocol)) || (port == 443 && "https".equalsIgnoreCase(protocol))))
                url += ":" + port;
        }

        if (path != null && path.size() > 0) {
            url += "/" + path;
        }

        if (params.size() > 0) {
            while (url.endsWith("/"))
                url = url.substring(0, url.length() - 1);

            url += "?";
            url += toQueryString(params.asMap());
        }

        return url;
    }

    /**
     * Checks url equality based on type and toString equality
     *
     * @return true if <code>url</code> is a Url with a matching toString
     */
    public boolean equals(Object url) {
        if (url instanceof Url) {
            return toString().equals(url.toString());
        }
        return false;
    }

    public String getDomain() {
        String domain = host;

        if (domain.lastIndexOf('.') > domain.indexOf('.')) {
            domain = domain.substring(domain.indexOf('.') + 1);
        }
        return domain;
    }

    /**
     * Generates a URL encode query string for <code>params</code>
     *
     * @return a URL encoded query string if <code>params.size() is @gt; 0</code> else empty string
     * @see #toQueryString(Map)
     */
    public String getQueryString() {
        if (params.size() == 0)
            return "";

        return toQueryString(params.asMap());
    }

    public String getHost() {
        return host;
    }

    public Url withHost(String host) {
        this.host = host;
        return this;
    }

    public int getPort() {
        return port;
    }

    public Url withPort(int port) {
        this.port = port;
        return this;
    }

    public String getProtocol() {
        return protocol;
    }

    public Url withProtocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public Path getPath() {
        return path;
    }

    public Url withPath(Path path) {
        this.path = path;
        return this;
    }

    /**
     * Gets the last url path part if it exists
     *
     * @return path.last() if it exists otherwise null
     */
    public String getFile() {
        if (path != null)
            return path.last();

        return null;
    }

    /**
     * Parses <code>queryString</code> and replace <code>params</code>
     *
     * @param queryString query prams to add
     * @return this
     */
    public Url withQueryString(String queryString) {
        params = new JSNode(Utils.parseQueryString(queryString));
        return this;
    }

    /**
     * Adds all key/value pairs from <code>newParams</code> to <code>params</code>
     * overwriting any exiting keys on a case insensitive basis
     *
     * @param newParams name/value pairs to add to the query string params map
     * @return this
     */
    public Url withParams(Map<String, String> newParams) {
        if (newParams != null)
            this.params.putAll(newParams);
        return this;
    }

    /**
     * Adds name/value to <code>params</code> overwriting any preexisting key/value pair
     * on a key case insensitive basis.
     *
     * @param name  the key to add or overwrite, may not be null
     * @param value the value, may be null
     * @return this
     */
    public Url withParam(String name, String value) {
        params.put(name, value);
        return this;
    }

    public Url withParams(String... nvpairs) {
        if (nvpairs != null) {
            for (int i = 0; i < nvpairs.length - 1; i = i + 2)
                withParam(nvpairs[i], nvpairs[i + 1]);

            if (nvpairs.length % 2 == 1)
                withParam(nvpairs[nvpairs.length - 1], null);
        }
        return this;
    }

    /**
     * Replaces any existing param that has <code>key</code> as a whole word case insensitive substring in its key.
     * <p>
     * If the key/value pair <code>"eq(dog,Fido)" = null</code> is in the map <code>replaceParam("DOG", "Fifi")</code>
     * would cause it to be removed and the pair "DOG" / "Fifi" would be added.
     *
     * @param key   the key to add overwrite, may not be null, also used a a regex whole world case insensitive token to search for other keys to remove.
     * @param value the value, may be null
     * @see Utils#containsToken
     */
    public void replaceParam(String key, String value) {
        for (String existing : params.keySet()) {
            if (Utils.containsToken(key, existing)) {
                params.remove(existing);
            }
        }

        withParam(key, value);
    }

    /**
     * Removes any param that has one of <code>tokens</code> as a whole word case insensitive substring in the key.
     *
     * @param tokens string tokens when found in a param key will cause them to be removed
     * @return the first value found that contained any one of <code>tokens</code>
     * @see Utils#containsToken
     */
    public String clearParams(String... tokens) {
        String oldValue = null;
        for (String token : tokens) {
            for (String existing : params.keySet()) {
                if (Utils.containsToken(token, existing)) {
                    String removed = (String) params.remove(existing);
                    if (oldValue == null)
                        oldValue = removed;
                }
            }
        }

        return oldValue;
    }

    /**
     * Finds a key that has any one of <code>tokens</code> as a whole word case insensitive substring
     *
     * @param tokens substrings to search params.keySet() for
     * @return the first param key that has anyone of <code>tokens</code> as a whole word case insensitive substring
     */
    public String findKey(String... tokens) {
        for (String token : tokens) {
            for (String key : params.keySet()) {
                if (Utils.containsToken(token, key)) {
                    return key;
                }
            }
        }
        return null;
    }

    /**
     * Finds the value associated with <code>findKey(tokens)</code>
     *
     * @param tokens substrings to search params.keySet() for
     * @return the value for the first param key that has anyone of <code>tokens</code> as a whole word case insensitive substring
     * @see #findKey(String...)
     */
    public String findKeyValue(String... tokens) {
        String key = findKey(tokens);
        if (key != null)
            return params.getString(key);

        return null;
    }

    /**
     * Gets the param value with <code>key</code> based on a case insensitive match.
     *
     * @param key the key to get
     * @return the param value for <code>key</code> based on a case insensitive match.
     */
    public String getParam(String key) {
        return (String) params.get(key);
    }

    /**
     * @return a new case insensitive order preserving map copy of <code>params</code>
     */
    public Map<String, String> getParams() {
        return (Map<String, String>) params.asMap();
    }

    /**
     * @return the url string used in constructing this Url.
     */
    public String getOriginal() {
        return original;
    }

}
