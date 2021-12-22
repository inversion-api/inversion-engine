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

import io.inversion.config.Context;
import io.inversion.utils.Path;
import io.inversion.utils.Utils;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class Server extends Rule<Server> {

    /**
     * The list of http(s) hosts not including a path component.
     * The path component, such as a servlet path, would be configured
     * through Server.includeOn()
     */
    List<String> urls = new ArrayList<>();

    transient List<UrlMatcher> urlMatchers = new ArrayList();

    class UrlMatcher {
        String protocol = null;
        int    port     = 0;
        Path   host     = null;

        public UrlMatcher(String url) {
            if (Utils.empty(url) || "*".equalsIgnoreCase(url)) {
                host = new Path("*");
            } else {
                Url u = new Url(url);
                protocol = u.getProtocol();
                port = u.getPort();
                host = new Path(u.getHost().replace('.', '/'));
            }
        }

        public boolean matches(Url url) {
            if (this.protocol != null && url.getProtocol() != null && !this.protocol.equalsIgnoreCase(url.getProtocol()))
                return false;

            if (this.port > 0 && url.getPort() > 0 && this.port != url.getPort())
                return false;

            if (this.host != null && url.getHost() != null && !this.host.matches(new Path(url.getHost().replace('.', '/'))))
                return false;

            return true;
        }
    }

    public Server() {

    }

    public Server(String... urls) {
        withUrls(urls);
    }

    public boolean matches(String method, Url url) {
        return match(method, url) != null;
    }


    public Path match(String method, Url url) {
        boolean match = urlMatchers.size() == 0;
        if(!match) {
            for (UrlMatcher matcher : urlMatchers) {
                if (matcher.matches(url)) {
                    match = true;
                    break;
                }
            }
        }
        if (!match)
            return null;

        Path matched = null;

        matched = super.match(method, url.getPath());
        if (matched != null) {
            Path host = new Path(url.getHost().replace('.', '/'));
            for (Param param : params) {
                if (Param.In.HOST == param.in) {
                    for (Pattern p : param.getPatterns()) {
                        if (!p.matcher(host.get(param.getIndex())).matches()) {
                            matched = null;
                            break;
                        }
                    }
                }
                else if(Param.In.SERVER_PATH == param.in){
                    for (Pattern p : param.getPatterns()) {
                        if (!p.matcher(url.getPath().get(param.getIndex())).matches()) {
                            matched = null;
                            break;
                        }
                    }
                }
            }
        }

        return matched;
    }

    public List<String> getUrls() {
        return new ArrayList(urls);
    }

    public Server withUrls(String... urls) {
        if (urls != null) {
            for (String url : urls) {
                if (Utils.empty(url)) {
                    url = "*";
                }
                if (!this.urls.contains(url))
                    this.urls.add(url);
            }
        }
        return this;
    }


    public void afterWiringComplete(Context context) {
        hook_wiringComplete_mergeUrlPaths();
        hook_wiringComplete_removeUrlPaths();
        hook_wiringComplete_applyParams();
        for (String url : urls) {
            urlMatchers.add(new UrlMatcher(url));
        }
    }

    protected void hook_wiringComplete_applyParams() {

        String      expected   = null;
        List<Param> hostParams = null;
        for (String url : urls) {
            Path hostPath = null;
            if (!Utils.empty(url) && (url.startsWith("http://") || url.startsWith("https://"))) {
                hostPath = new Path(new Url(url).getHost().replace('.', '/'));
            } else {
                hostPath = new Path();
            }
            hostParams = extractParams(Param.In.HOST, hostPath);
            if (expected == null)
                expected = hostParams.toString();
            else if (!expected.equalsIgnoreCase(hostParams.toString())) {
                throw ApiException.new500InternalServerError("All of the variables in a single Server's hosts and paths must match on name and position: '{}'", this.toString());
            }
        }

        expected = null;
        List<Param> pathParams = null;
        for (RuleMatcher matcher : getIncludeMatchers()) {
            Set<Path> includePaths = matcher.getPaths();
            for (Path path : includePaths) {
                pathParams = extractParams(Param.In.SERVER_PATH, path);
                if (expected == null)
                    expected = pathParams.toString();
                else if (!expected.equalsIgnoreCase(pathParams.toString())) {
                    throw ApiException.new500InternalServerError("All of the variables in a single Server's hosts and paths must match on name and position: '{}'", this.toString());
                }
            }
        }

        if (hostParams != null && hostParams.size() > 0)
            withParams(hostParams);

        if (pathParams != null && pathParams.size() > 0)
            withParams(pathParams);
    }


    protected void hook_wiringComplete_removeUrlPaths() {
        for (int i = 0; i < urls.size(); i++) {
            String url = urls.get(i);
            if (url.startsWith("http://") || url.startsWith("https://")) {
                int idx = url.indexOf('/', 8);
                if (idx > 0) {
                    url = url.substring(0, idx);
                    urls.set(i, url);
                }
            }
        }
    }

    protected void hook_wiringComplete_mergeUrlPaths() {

        ArrayListValuedHashMap<RuleMatcher, Path> updatedIncludePaths = new ArrayListValuedHashMap<>();
        ArrayListValuedHashMap<RuleMatcher, Path> updatedExcludePaths = new ArrayListValuedHashMap<>();
        for (String url : urls) {
            if (url.length() > 0) {

                if ("*".equalsIgnoreCase(url)) {

                } else {
                    Url  u       = new Url(url);
                    Path urlPath = u.getPath();

                    for (RuleMatcher matcher : getIncludeMatchers()) {
                        Set<Path> includePaths = matcher.getPaths();
                        for (Path path : includePaths) {
                            if (urlPath != null && urlPath.size() > 0) {
                                Path updatedPath = new Path(urlPath.toString(), path.toString());
                                updatedIncludePaths.put(matcher, updatedPath);
                            }
                        }
                        if (includePaths.size() == 0 && urlPath != null && urlPath.size() > 0) {
                            updatedIncludePaths.put(matcher, urlPath.copy());
                        }
                    }

                    for (RuleMatcher matcher : getExcludeMatchers()) {
                        Set<Path> excludePaths = matcher.getPaths();
                        for (Path path : excludePaths) {
                            if (urlPath != null && urlPath.size() > 0) {
                                Path updatedPath = new Path(urlPath.toString(), path.toString());
                                updatedExcludePaths.put(matcher, updatedPath);
                            }
                        }
                    }
                }
            }
        }

        for (RuleMatcher matcher : updatedIncludePaths.keySet()) {
            //-- WB 20211218
            //-- TODO: this array list wrapper should not be necessary to my eye but
            //-- if it is not here calling matcher.clearPaths() removes the elements
            //-- from the list that is returned from the ArrayListValuedHashMap.
            //-- I can't figure it out...
            List<Path> paths = new ArrayList<>(updatedIncludePaths.get(matcher));
            matcher.clearPaths();
            matcher.withPaths(paths);
        }

        for (RuleMatcher matcher : updatedExcludePaths.keySet()) {
            List<Path> paths = new ArrayList<>(updatedExcludePaths.get(matcher));
            matcher.clearPaths();
            matcher.withPaths(paths);
        }
    }

    protected List<Param> extractParams(Param.In in, Path path) {
        List<Param> params = new ArrayList();
        for (int i = 0; i < path.size(); i++) {
            //TODO: expand trivial regexes...not here...where...probably in Path.enumerate
            if (path.isVar(i)) {
                Param param = new Param()
                        .withRequired(true)
                        .withIndex(i)
                        .withIn(in)
                        .withKey(path.getVarName(i));

                String regex = path.getRegex(i);
                if (regex != null) {
                    param.withRegex(regex);
                }
                params.add(param);
            }
        }
        return params;
    }

}
