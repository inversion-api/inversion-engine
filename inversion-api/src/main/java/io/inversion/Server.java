/*
 * Copyright (c) 2015-2021 Rocket Partners, LLC
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

import io.inversion.utils.Path;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public final class Server {// extends Rule<Server> {

    String name = null;

    LinkedHashMap<Url, ServerMatcher> urls = new LinkedHashMap();

    boolean documented = true;

    public Server() {
    }

    public Server(String... urls) {
        withUrls(urls);
    }

    public Server(Url... urls) {
        withUrls(urls);
    }

    public String getName() {
        return name;
    }

    public Server withName(String name) {
        this.name = name;
        return this;
    }

    public String toString(){
        return urls.keySet().toString();
    }

    public static class ServerMatcher {
        Url url;
        String protocol;
        Path   host;
        Path   path;
        int port = 0;
        public ServerMatcher(Url url){
            this.url = url;
            protocol = url.getProtocol();
            host = url.getHost() != null ? new Path(url.getHost().replace(".", "/")) : null;
            port = url.getPort();
            path = url.getPath();
            if(path == null)
                path = new Path("*");
            if(!path.endsWithWildcard())
                path.add("*");
        }

        public String toString(){
            if(host != null && host.size() > 0){
                return url.toString();
            }
            if(port > 0){
                return "/" + path + ":" + port;
            }
            else{
                return "/" + path;
            }

        }

        public boolean match(Url url){
            if(protocol != null && !protocol.equals("//") && !protocol.equals(url.getProtocol()))
                return false;
            if(host != null && url.getHostAsPath() != null && !host.matches(url.getHostAsPath()))
                return false;
            if(port > 0 && url.getPort() > 0 && port != url.getPort())
                return false;
            if(path != null && !path.matches(url.getPath()))
                return false;

            return true;
        }

        public String getProtocol() {
            return protocol;
        }

        public Path getHost() {
            if(host == null)
                return new Path();
            return new Path(host);
        }

        public Path getPath() {
            return new Path(path);
        }

        public int getPort() {
            return port;
        }
    }

    public ServerMatcher match(Url url){
        for(ServerMatcher matcher : urls.values()){
            if(matcher.match(url)){
                return matcher;
            }
        }
        return null;
    }

    public Server withUrls(String... urls) {
        for(String url : urls){
            withUrls(new Url(url));
        }
        return this;
    }

    public Server withUrls(Url... urls) {
        for(Url url : urls){
            this.urls.put(url, new ServerMatcher(url));
        }
        return this;
    }

    public List<Url> getUrls(){
        return new ArrayList(urls.keySet());
    }

    public boolean isDocumented() {
        return documented;
    }

    public Server withDocumented(boolean documented) {
        this.documented = documented;
        return this;
    }

    public List<ServerMatcher> getServerMatches(){
        return new ArrayList<>(this.urls.values());
    }

//    /**
//     * The list of http(s) hosts not including a path component.
//     * The path component, such as a servlet path, would be configured
//     * through Server.includeOn()
//     */
//    List<String> urls = new ArrayList<>();
//
//    transient List<UrlMatcher> urlMatchers = new ArrayList();
//

//
//    public boolean matches(String method, Url url) {
//        return match(method, url) != null;
//    }
//
//
//    public Path match(String method, Url url) {
//
//
//        checkLazyConfig();
//
//        boolean match = urlMatchers.size() == 0;
//        if (!match) {
//            for (UrlMatcher matcher : urlMatchers) {
//                if (matcher.matches(url)) {
//                    match = true;
//                    break;
//                }
//            }
//        }
//        if (!match)
//            return null;
//
//        Path matched = null;
//
//        matched = super.match(method, url.getPath());
//        if (matched != null) {
//            Path host = new Path(url.getHost().replace('.', '/'));
//            for (Param param : params) {
//                if (Param.In.HOST == param.in) {
//                    for (Pattern p : param.getPatterns()) {
//                        if (!p.matcher(host.get(param.getIndex())).matches()) {
//                            matched = null;
//                            break;
//                        }
//                    }
//                } else if (Param.In.SERVER_PATH == param.in) {
//                    for (Pattern p : param.getPatterns()) {
//                        if (!p.matcher(url.getPath().get(param.getIndex())).matches()) {
//                            matched = null;
//                            break;
//                        }
//                    }
//                }
//            }
//        }
//
//        return matched;
//    }
//
//    public List<String> getUrls() {
//        return new ArrayList(urls);
//    }
//
//    public Server withUrls(String... urls) {
//        if (urls != null) {
//            for (String url : urls) {
//                if (Utils.empty(url)) {
//                    url = "*";
//                }
//                if (!this.urls.contains(url))
//                    this.urls.add(url);
//            }
//        }
//        return this;
//    }
//
//    protected void doLazyConfig() {
//        super.doLazyConfig();
//        lazyConfig_mergeUrlPaths();
//        lazyConfig_removeUrlPaths();
//        lazyConfig_applyParams();
//        for (String url : urls) {
//            urlMatchers.add(new UrlMatcher(url));
//        }
//    }
//
//    protected void lazyConfig_applyParams() {
//
//        String      expected   = null;
//        List<Param> hostParams = null;
//        for (String url : urls) {
//            Path hostPath = null;
//            if (!Utils.empty(url) && (url.startsWith("http://") || url.startsWith("https://"))) {
//                hostPath = new Path(new Url(url).getHost().replace('.', '/'));
//            } else {
//                hostPath = new Path();
//            }
//            hostParams = extractParams(Param.In.HOST, hostPath);
//            if (expected == null)
//                expected = hostParams.toString();
//            else if (!expected.equalsIgnoreCase(hostParams.toString())) {
//                throw ApiException.new500InternalServerError("All of the variables in a single Server's hosts and paths must match on name and position: '{}'", this.toString());
//            }
//        }
//
//        expected = null;
//        List<Param> pathParams = null;
//        for (RuleMatcher matcher : getIncludeMatchers()) {
//            Set<Path> includePaths = matcher.getPaths();
//            for (Path path : includePaths) {
//                pathParams = extractParams(Param.In.SERVER_PATH, path);
//                if (expected == null)
//                    expected = pathParams.toString();
//                else if (!expected.equalsIgnoreCase(pathParams.toString())) {
//                    throw ApiException.new500InternalServerError("All of the variables in a single Server's hosts and paths must match on name and position: '{}'", this.toString());
//                }
//            }
//        }
//
//        if (hostParams != null && hostParams.size() > 0)
//            withParams(hostParams);
//
//        if (pathParams != null && pathParams.size() > 0)
//            withParams(pathParams);
//    }
//
//
//    protected void lazyConfig_removeUrlPaths() {
//        for (int i = 0; i < urls.size(); i++) {
//            String url = urls.get(i);
//            if (url.startsWith("http://") || url.startsWith("https://")) {
//                int idx = url.indexOf('/', 8);
//                if (idx > 0) {
//                    url = url.substring(0, idx);
//                    urls.set(i, url);
//                }
//            }
//        }
//    }
//
//    protected void lazyConfig_mergeUrlPaths() {
//        ArrayListValuedHashMap<RuleMatcher, Path> updatedIncludePaths = new ArrayListValuedHashMap<>();
//        ArrayListValuedHashMap<RuleMatcher, Path> updatedExcludePaths = new ArrayListValuedHashMap<>();
//        for (String url : urls) {
//            if (url.length() > 0) {
//
//                if ("*".equalsIgnoreCase(url)) {
//
//                } else {
//                    Url  u       = new Url(url);
//                    Path urlPath = u.getPath();
//
//                    for (RuleMatcher matcher : getIncludeMatchers()) { //don't call getIncludeMatcher() becuase it will recurse the initialziation
//                        Set<Path> includePaths = matcher.getPaths();
//                        for (Path path : includePaths) {
//                            if (urlPath != null && urlPath.size() > 0) {
//                                Path updatedPath = new Path(urlPath.toString(), path.toString());
//                                updatedIncludePaths.put(matcher, updatedPath);
//                            }
//                        }
//                        if (includePaths.size() == 0 && urlPath != null && urlPath.size() > 0) {
//                            updatedIncludePaths.put(matcher, urlPath.copy());
//                        }
//                    }
//
//                    for (RuleMatcher matcher : getExcludeMatchers()) {
//                        Set<Path> excludePaths = matcher.getPaths();
//                        for (Path path : excludePaths) {
//                            if (urlPath != null && urlPath.size() > 0) {
//                                Path updatedPath = new Path(urlPath.toString(), path.toString());
//                                updatedExcludePaths.put(matcher, updatedPath);
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        for (RuleMatcher matcher : updatedIncludePaths.keySet()) {
//            //-- WB 20211218
//            //-- TODO: this array list wrapper should not be necessary to my eye but
//            //-- if it is not here calling matcher.clearPaths() removes the elements
//            //-- from the list that is returned from the ArrayListValuedHashMap.
//            //-- I can't figure it out...
//            List<Path> paths = new ArrayList<>(updatedIncludePaths.get(matcher));
//            matcher.clearPaths();
//            matcher.withPaths(paths);
//            System.out.println(matcher);
//        }
//
//        for (RuleMatcher matcher : updatedExcludePaths.keySet()) {
//            List<Path> paths = new ArrayList<>(updatedExcludePaths.get(matcher));
//            matcher.clearPaths();
//            matcher.withPaths(paths);
//        }
//    }
//
//    protected List<Param> extractParams(Param.In in, Path path) {
//        List<Param> params = new ArrayList();
//        for (int i = 0; i < path.size(); i++) {
//            //TODO: expand trivial regexes...not here...where...probably in Path.enumerate
//            if (path.isVar(i)) {
//                Param param = new Param()
//                        .withRequired(true)
//                        .withIndex(i)
//                        .withIn(in)
//                        .withKey(path.getVarName(i));
//
//                String regex = path.getRegex(i);
//                if (regex != null) {
//                    param.withRegex(regex);
//                }
//                params.add(param);
//            }
//        }
//        return params;
//    }
//
//    public List<UrlMatcher> getUrlMatchers(){
//        return urlMatchers;
//    }
//
//    class UrlMatcher {
//        String protocol = null;
//        int    port     = 0;
//        Path   host     = null;
//
//        public String toString(){
//            return Utils.addToMap(new LinkedHashMap(), "protocol", protocol, "port", port, "host", host).toString();
//        }
//
//        public UrlMatcher(String url) {
//            if (Utils.empty(url) || "*".equalsIgnoreCase(url)) {
//                host = new Path("*");
//            } else {
//                Url u = new Url(url);
//                protocol = u.getProtocol();
//                port = u.getPort();
//                host = new Path(u.getHost().replace('.', '/'));
//            }
//        }
//
//        public boolean matches(Url url) {
//            if (this.protocol != null && url.getProtocol() != null && !this.protocol.equalsIgnoreCase(url.getProtocol()))
//                return false;
//
//            if (this.port > 0 && url.getPort() > 0 && this.port != url.getPort())
//                return false;
//
//            if (this.host != null && url.getHost() != null && !this.host.matches(new Path(url.getHost().replace('.', '/'))))
//                return false;
//
//            return true;
//        }
//    }

}
