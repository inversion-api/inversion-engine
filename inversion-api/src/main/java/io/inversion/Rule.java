/*
 * c * // * Copyright (c) 2015-2018 Rocket Partners, LLC
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

import io.inversion.context.Context;
import io.inversion.json.JSMap;
import io.inversion.utils.Path;
import io.inversion.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Matches against an HTTP method and URL path to determine if the object
 * should be included when processing the associated Request.
 * <p>
 * Matching relies heavily on variablized Path matching via {@link Path#matches(String)}
 */
public abstract class Rule<R extends Rule> implements Comparable<R> {

    public static final SortedSet<String> ALL_METHODS = Collections.unmodifiableSortedSet(new TreeSet<String>(Utils.asSet("GET", "POST", "PUT", "PATCH", "DELETE")));

    protected final transient Logger            log             = LoggerFactory.getLogger(getClass().getName());
    /**
     * Method/path combinations that would cause this Rule to be included in the relevant processing.
     */
    protected final           List<RuleMatcher> includeMatchers = new ArrayList<>();
    /**
     * Method/path combinations that would cause this Rule to be excluded from the relevant processing.
     */
    protected final           List<RuleMatcher> excludeMatchers = new ArrayList<>();
    /**
     * {@code JSNode} is used because it implements a case insensitive map without modifying the keys
     */
    protected final transient JSMap             configMap       = new JSMap();
    /**
     * The name used for configuration and debug purposes.
     */
    protected                 String            name            = null;
    /**
     * Rules are always processed in sequence sorted by ascending order.
     */
    protected                 int               order           = 1000;

    /**
     * An optional querystring that will be applied to every request processed.
     * This is useful to force specific params on different endpoints/actions etc.
     */
    //protected String query = null;

    protected String includeOn = null;

    protected String excludeOn = null;

    protected String description = null;

    protected List<Param> params = new ArrayList();

    transient boolean lazyConfiged = false;

    static List<Path> asPathsList(String... paths) {
        List<Path> pathsList = new ArrayList<>();
        for (String path : Utils.explode(",", paths)) {
            pathsList.add(new Path(path));
        }
        if (pathsList.size() == 0)
            pathsList.add(new Path("*"));
        return pathsList;
    }

    static Path[] asPathsArray(String... paths) {
        List<Path> list = asPathsList(paths);
        return list.toArray(new Path[0]);

    }

    public void afterWiringComplete(Context context) {
        checkLazyConfig();
    }

    protected void checkLazyConfig() {
        //-- reluctant lazy config defaultIncludes if no other
        //-- includes/excludes have been configured by the user.

        if (!lazyConfiged) {
            synchronized (this) {
                if (!lazyConfiged) {
                    lazyConfiged = true;
                    doLazyConfig();
                }
            }
        }
    }

    protected void doLazyConfig() {
        if (includeOn != null)
            withIncludeOn(includeOn);

        if (excludeOn != null)
            withExcludeOn(excludeOn);

        if (includeMatchers.size() == 0 && excludeMatchers.size() == 0) {

            List<RuleMatcher> matchers = getDefaultIncludeMatchers();
            for (RuleMatcher m : matchers)
                withIncludeOn(m);
        }
    }

    /**
     * Designed to allow subclasses to provide a default match behavior
     * of no configuration was provided by the developer.
     *
     * @return the default include match "*","*"
     */
    protected List<RuleMatcher> getDefaultIncludeMatchers() {
        return Utils.asList(new RuleMatcher(null, "*"));
    }

    /**
     * Check if the http method and path match this Rule.
     *
     * @param method the HTTP method to match
     * @param path   the concrete path to match
     * @return true if the http method and path are included and not excluded
     */
    public boolean matches(String method, String path) {
        return matches(method, new Path(path));
    }

    /**
     * Check if the http method and path match this Rule.
     *
     * @param method the HTTP method to match
     * @param path   the concrete path to match
     * @return true if the http method and path are included and not excluded
     */
    public boolean matches(String method, Path path) {
        return match(method, path) != null;
    }

    /**
     * Find the first ordered Path that satisfies this method/path match.
     *
     * @param method the HTTP method to match
     * @param path   the concrete path to match
     * @return the first includeMatchers path to match when method also matches, null if no matches or excluded
     */
    public Path match(String method, Path path) {
        return match(method, path, false);
    }
    public Path match(String method, Path path, boolean bidirectional) {
        checkLazyConfig();

        for (RuleMatcher excluder : excludeMatchers) {
            if (excluder.methods.size() > 0 && !excluder.methods.contains(method))
                continue;

            for (Path excludePath : excluder.paths) {
                if (excludePath.matches(path, bidirectional)) {
                    return null;
                }
            }
        }

        int includePathCount = 0;

        for (RuleMatcher includer : includeMatchers) {
            includePathCount += includer.paths.size();

            if (includer.methods.size() > 0 && !includer.methods.contains(method))
                continue;

            for (Path includePath : includer.paths) {
                if (includePath.matches(path, bidirectional)) {
                    return includePath;
                }
            }
        }

        //-- path was not excluded but config did not supply any include paths
        //-- so this is an implicit * include.
        if (includePathCount == 0) {
            return new Path("*");
        }

        return null;
    }

    public List<String> getAllIncludeMethods() {
        checkLazyConfig();
        Set methods = new LinkedHashSet();
        for (RuleMatcher includer : includeMatchers) {
            methods.addAll(includer.methods);
        }
        return new ArrayList(methods);
    }

    public List<Path> getAllIncludePaths() {
        checkLazyConfig();
        Set paths = new LinkedHashSet();
        for (RuleMatcher includer : includeMatchers) {
            paths.addAll(includer.paths);
        }
        return new ArrayList(paths);
    }

    public List<Path> getAllExcludePaths() {
        checkLazyConfig();
        Set paths = new LinkedHashSet();
        for (RuleMatcher excluder : excludeMatchers) {
            paths.addAll(excluder.paths);
        }
        return new ArrayList(paths);
    }

    public List<RuleMatcher> getIncludeMatchers() {
        checkLazyConfig();
        return new ArrayList(includeMatchers);
    }

    public R withIncludeOn(RuleMatcher matcher) {
        includeMatchers.add(matcher);
        return (R) this;
    }

//    /**
//     * Select this Rule when any method and path match.
//     *
//     * @param methods or more comma separated http method names, can be null to match on any
//     * @param paths   each path can be one or more comma separated variableized Paths
//     * @return this
//     */
//    public R withIncludeOn(String methods, String paths) {
//        withIncludeOn(new RuleMatcher(methods, paths));
//        return (R) this;
//    }

    public R withIncludeOn(String spec) {
        withIncludeOn(new RuleMatcher(spec));
        return (R) this;
    }

//    /**
//     * Don't select this Rule when any method and path match.
//     *
//     * @param methods or more comma separated http method names, can be null to match on any
//     * @param paths   each path can be one or more comma separated variableized Paths
//     * @return this
//     */
//    public R withExcludeOn(String methods, String paths) {
//        withExcludeOn(new RuleMatcher(methods, paths));
//        return (R) this;
//    }

    /**
     * Don't select this Rule when RuleMatcher matches
     *
     * @param matcher the method/path combo to exclude
     * @return this
     */
    public R withExcludeOn(RuleMatcher matcher) {
        excludeMatchers.add(matcher);
        return (R) this;
    }

    public R withExcludeOn(String spec) {
        withExcludeOn(new RuleMatcher(spec));
        return (R) this;
    }

    public List<RuleMatcher> getExcludeMatchers() {
        checkLazyConfig();
        return new ArrayList(excludeMatchers);
    }

    public String getName() {
        return name;
    }

    public R withName(String name) {
        this.name = name;
        return (R) this;
    }

    public Rule withDescription(String description) {
        this.description = description;
        return this;
    }

    public String getDescription() {
        return this.description;
    }


    public int getOrder() {
        return order;
    }

    public R withOrder(int order) {
        this.order = order;
        return (R) this;
    }

//    public R withQuery(String query) {
//        this.query = query;
//        return (R) this;
//    }
//
//    public String getQuery() {
//        return query;
//    }

    @Override
    public int compareTo(Rule a) {
        int compare = Integer.compare(getOrder(), a.getOrder());
        return compare;
    }

    public List<Param> getParams() {
        return params;
    }

    public R withParams(List<Param> params) {
        params.forEach(p -> withParam(p));
        return (R) this;
    }

    public R withParam(Param param) {
        if (!params.contains(param)) {
            params.add(param);
        }
        return (R) this;
    }


    public String toString() {
        StringBuilder buff = new StringBuilder(getClass().getSimpleName());

        if (name != null) {
            buff.append(":").append(name);
        }

        if (includeMatchers.size() > 0 || excludeMatchers.size() > 0) {
            buff.append(" -");
            if (includeMatchers.size() > 0)
                buff.append(" includes: ").append(includeMatchers);

            if (excludeMatchers.size() > 0)
                buff.append(" exclude: ").append(excludeMatchers);
        }
        return buff.toString();
    }


//    static List<RuleMatcher> parseRuleMatcher(String methodsAndOrPaths) {
//        List<RuleMatcher> matchers = new ArrayList<>();
//        String[]          parts    = methodsAndOrPaths.split("\\|");
//        for (int i = 0; i < parts.length; i++) {
//            if (parts.length - i == 1) {
//                //there is not another matched pair so parse both methods and paths from this single string
//
//                List<String> methodsList = new ArrayList();
//                List<String> pathsList   = new ArrayList();
//
//                for (String part : parts[i].split(",")) {
//                    part = part.trim();
//                    if (Utils.in(part.toLowerCase(), "get", "post", "put", "patch", "delete"))
//                        methodsList.add(part);
//                    else
//                        pathsList.add(part);
//                }
//
//                String methods = methodsList.size() == 0 ? "*" : Utils.implode(",", methodsList);
//                String paths   = pathsList.size() == 0 ? "*" : Utils.implode(",", pathsList);
//                matchers.add(new RuleMatcher(methods, paths));
//            } else {
//                matchers.add(new RuleMatcher(parts[i], parts[i + 1]));
//                i++;
//            }
//        }
//        return matchers;
//    }

    public static class RuleMatcher {

        protected final TreeSet<String>   methods = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        protected final LinkedHashSet<Path> paths   = new LinkedHashSet<>();

        public RuleMatcher() {
        }

        public RuleMatcher(String spec) {
            parse(this, spec);
        }


        public static void parse(RuleMatcher matcher, String spec) {
            if (spec == null)
                return;

            spec = spec.trim();
            List<String> parts = Utils.explode(",", spec);
            for (String part : parts) {
                if (ALL_METHODS.contains(part.toUpperCase()))
                    matcher.withMethods(part);
                else
                    matcher.withPaths(new Path(part));
            }
        }

        public String toString() {
            return Utils.implode(",", methods, paths);
        }

        public int hashCode() {
            return toString().toLowerCase().hashCode();
        }


        @Override
        public boolean equals(Object o) {
            return o instanceof RuleMatcher && Utils.equal(this.toString(), o.toString());
        }

        public RuleMatcher(String methods, String... paths) {
            this(methods, asPathsList(paths));
        }

        public RuleMatcher(String methods, Path path) {
            withMethods(methods);
            withPaths(path);
        }

        public RuleMatcher(String methods, List<Path> paths) {
            withMethods(methods);
            withPaths(paths);
        }

        public RuleMatcher clearPaths() {
            paths.clear();
            return this;
        }

        public RuleMatcher clearMethods() {
            methods.clear();
            return this;
        }

        public boolean hasMethod(String method) {
            return methods.size() == 0 || methods.contains(method);
        }


        public void withMethods(String... methods) {
            for (String method : Utils.explode(",", methods)) {
                if (method.equals("*"))
                    continue;
                method = method.toUpperCase();
                if (ALL_METHODS.contains(method)) {
                    this.methods.add(method);
                }
            }
        }

        public RuleMatcher withPaths(Path... paths) {
            for (int i = 0; paths != null && i < paths.length; i++) {
                if (paths[i] != null)
                    this.paths.add(paths[i]);
            }
            return this;
        }

        public RuleMatcher withPaths(List<Path> paths) {
            for (Path p : paths)
                withPaths(p);
            return this;
        }

        public SortedSet<String> getMethods() {
            if (methods.size() == 0)
                return ALL_METHODS;
            return Collections.unmodifiableSortedSet(methods);
        }

        public LinkedHashSet<Path> getPaths() {
            if (this.paths.size() == 0) {
                return Utils.add(new LinkedHashSet(), new Path("*"));
            }
            return new LinkedHashSet(paths);
        }
    }
}
