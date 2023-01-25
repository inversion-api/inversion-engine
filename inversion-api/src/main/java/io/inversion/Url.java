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
package io.inversion;


import io.inversion.json.JSMap;
import io.inversion.json.JSParser;
import io.inversion.rql.Rql;
import io.inversion.rql.Term;
import io.inversion.utils.Path;
import io.inversion.utils.Utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

/**
 * Utility class for parsing and working with HTTP(S) URLs.
 * <p>
 * Not for use with non HTTP(S) urls.
 * <p>
 * A number of different utility methods are provided to make it simple to find or remove different query string keys.
 */
public final class Url {
    /**
     * The url string as supplied to the constructor, with 'http://localhost/' prepended if the constructor url arg did not contain a host
     */
    private String original;

    /**
     * The url protocol, either http or https
     */
    private String protocol = "http";

    /**
     * The url host.
     */
    private String hostAsString = null;

    private Path hostAsPath = null;

    /**
     * The url port number if a custom port was provided
     */
    private int port = 0;

    /**
     * The part of the url after the host/port before the query string.
     */
    private Path path = null;

    /**
     * A case insensitive map of query string name/value pairs that preserves iteration order
     * <p>
     * Implementation Node: <code>params</code>is not actually JSON, JSMap is used as the map
     * implementation here simply because it is an affective case insensitive map that preserves
     * the original key case key iteration order.
     */
    private JSMap params = new JSMap();


    public Url copy(){
        Url url = new Url();
        url.original = original;
        url.protocol = protocol;
        url.hostAsString = hostAsString;
        url.hostAsPath = hostAsPath == null ? null : new Path(hostAsPath);
        url.port = port;
        url.path = path == null ? null : new Path(path);
        url.params = params.size() == 0 ? url.params : JSParser.asJSMap(params.toString());
        return url;
    }

    private Url(){

    }

    /**
     * Parses <code>url</code> into its protocol, host, port, path and query string param parts.
     * <p>
     * If <code>url</code> does not start with "http://" or "https://" then "http://127.0.0.1" will be prepended.
     *
     * @param url url string
     */
    public Url(String url) {

        String origionalUrl = url;

        try {

            //-- chopping out querystring is easy so start there
            int queryIndex = url.indexOf('?');
            if (queryIndex >= 0) {
                String query = url.substring(queryIndex + 1);
                url = url.substring(0, queryIndex);
                withQueryString(query);
            }


            if(url.startsWith("//") || url.startsWith("http:/") || url.startsWith("https:/")){

                if(url.startsWith("//")) {
                    withProtocol("//");
                }
                else if(url.startsWith("http://")){
                    withProtocol("http");
                    url = url.substring(5);
                }
                else if(url.startsWith("https://")){
                    withProtocol("https");
                    url = url.substring(6);
                }
                else{
                    throw Utils.ex("Your requested URL '{}' is malformed.", origionalUrl);
                }

                //-- trim off any type extra "/" characters
                while(url.startsWith("/"))
                    url = url.substring(1);

                int slash = url.indexOf("/");
                int colon = url.indexOf(":");

                if(colon > 0 && (slash < 0 || colon < slash)){
                    withHost(url.substring(0, colon));
                    url = url.substring(colon);
                }
                else if(slash > 0){
                    withHost(url.substring(0, slash));
                    url = url.substring(slash);
                }

                if(url.startsWith(":")){
                    slash = url.indexOf("/");
                    if(slash > 0){
                        withPort(Integer.parseInt(url.substring(1, slash)));
                        url = url.substring(slash);
                    }
                    else{
                        withPort(Integer.parseInt(url.substring(1)));
                        url = "";
                    }
                }
            }

            while(url.startsWith("/"))
                url = url.substring(1);

            if(url.length() > 0){
                Path path = new Path(url);
                for(int i=0; i<path.size(); i++){
                    if(path.get(i).startsWith(".")){
                        throw Utils.ex("Your requested URL '{}' is malformed.", origionalUrl);
                    }
                }
                withPath(path);
            }
        } catch (Exception ex) {
            throw Utils.ex(ex, "Your requested URL '{}' is malformed.", origionalUrl);
        } finally {
            this.original = toString();
        }
    }

    /**
     * Creates a UTF-8 url encoded query string, not including a leading "?" with key value pairs separated by a '&amp;'
     *
     * @param params the key/value pairs to encode
     * @return a UTF-8 url encoded query string
     * @see java.net.URLEncoder#encode(String, String)
     */
    public static String toQueryString(Map<String, String> params) {

        StringBuilder query = new StringBuilder();

        List<String> qs = new ArrayList();
        for (String key : params.keySet()) {
            if (key.indexOf("(") > 0)
                qs.add(key);
        }

        if (qs.size() > 0) {
            params = new LinkedHashMap<>(params);
            for (String q : qs) {
                params.remove(q);
            }
            params.put("q", Utils.implode(",", qs));
        }

        for (String key : params.keySet()) {
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

        String str = query.toString();
        str = str.replace("%28", "(");
        str = str.replace("%29", ")");
        str = str.replace("%2C", ",");

        return str;
    }

    public static String encode(String str) {
        try {
            str = URLEncoder.encode(str, "UTF-8");
            str = str.replace("%2C", ",");
            str = str.replace("%7E", "~");
            return str;
        } catch (Exception ex) {
            throw Utils.ex(ex);
        }
    }

    /**
     * Generates a string string representation of this url with any query string parameters URL encoded and port number included only if it differs from the standard protocol port.
     *
     * @return the string representation of this url
     */
    public String toString() {
        String url = protocol;
        if(!protocol.equals("//"))
            url += "://";

        url += hostAsString != null ? hostAsString : "127.0.0.1";

        if (port > 0) {
            if (!((port == 80 && "http".equalsIgnoreCase(protocol)) || (port == 443 && "https".equalsIgnoreCase(protocol))))
                url += ":" + port;
        }
        else if(hostAsString == null){
            url += ":8080";
        }

        if (path != null && path.size() > 0) {
            url += "/" + path;
        }

        if (params.size() > 0) {
            while (url.endsWith("/"))
                url = url.substring(0, url.length() - 1);

            url += "?";
            url += toQueryString((Map<String, String>) params);
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
        String domain = hostAsString;

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

        return toQueryString((Map<String, String>) params);
    }

    public String getHost() {
        return hostAsString;
    }

    public Url withHost(String host) {
        this.hostAsString = host;
        this.hostAsPath = new Path(hostAsString.replace('.', '/'));
        return this;
    }

    public Path getHostAsPath(){
        return hostAsPath;
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
        if(path == null)
            return new Path();

        return new Path(path);
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
        withParams(Utils.parseQueryString(queryString));
        return this;
    }

    /**
     * Adds all key/value pairs from <code>newParams</code> to <code>params</code>
     * overwriting any exiting keys on a case insensitive basis
     *
     * @param params name/value pairs to add to the query string params map
     * @return this
     */
    public Url withParams(Map<String, String> params) {
        if (params != null) {
            for (String key : params.keySet()) {
                withParam(key, params.get(key));
            }
        }

        return this;
    }

    public Url withParam(Term term){
        params.put(term.toString(), null);
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

        int paren = name.indexOf("(");
        if (paren > 0) {
            String func = fixLegacyParamName(name.substring(0, paren));
            if (Utils.in(func, "page", "size", "sort", "include", "exclude", "expand", "collapse")) {
                value = name.substring(paren + 1, name.lastIndexOf(")"));
                name = func;
            }
        } else {
            name = fixLegacyParamName(name);
        }

        if (!Utils.empty(name)) {
            if ("q".equalsIgnoreCase(name)) {
                value = "and(" + value + ")";
                Term term = Rql.parse(value);
                for (Term child : term.getTerms())
                    withParam(child.toString(), null);
            } else {
                params.put(name, value);
            }
        }
        return this;
    }

    String fixLegacyParamName(String name) {
        switch (name.toLowerCase()) {
            case "pagenum":
                return "page";
            case "limit":
                return "size";
            case "order":
                return "sort";
            case "includes":
                return "include";
            case "excludes":
                return "exclude";
            case "expands":
                return "expand";
            case "collapses":
                return "collapse";
        }
        return name;
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

//    /**
//     * Replaces any existing param that has <code>key</code> as a whole word case insensitive substring in its key.
//     * <p>
//     * If the key/value pair <code>"eq(dog,Fido)" = null</code> is in the map <code>replaceParam("DOG", "Fifi")</code>
//     * would cause it to be removed and the pair "DOG" / "Fifi" would be added.
//     *
//     * @param key   the key to add overwrite, may not be null, also used a a regex whole world case insensitive token to search for other keys to remove.
//     * @param value the value, may be null
//     * @see Utils#containsToken
//     */
//    public void replaceParam(String key, String value) {
//        for (String existing : new ArrayList<>(params.keySet())) {
//            if (Utils.containsToken(key, existing)) {
//                params.remove(existing);
//            }
//        }
//
//        withParam(key, value);
//    }

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
            for (String key : new LinkedHashSet<>(params.keySet())) {
                String value = (String) params.get(key);
                if (Utils.containsToken(token, key)) {
                    String removed = (String) params.remove(key);
                    if (oldValue == null)
                        oldValue = removed;
                }
            }
        }

        return oldValue;
    }

    public void clearParams() {
        params.clear();
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
            return (String)params.get(key);

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
        return (Map<String, String>) params;
    }

    /**
     * @return the url string used in constructing this Url.
     */
    public String getOriginal() {
        return original;
    }

}
