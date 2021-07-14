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

import io.inversion.utils.Path;

import java.util.*;
import java.util.regex.Pattern;

public class Linker {

    protected String name = null;
    protected Api    api  = null;

    public Linker() {
    }

    public Linker(Api api) {
        this.api = api;
    }

    //TODO add support for header params
    //TODO add support for requests to other servers...?
    public Request buildRequest(Request req, String function, String method, Collection collection, String resourceKey, String relationshipKey, Map<String, String> mustMatch) {

        List<Request> matches = new ArrayList();

        if (resourceKey != null || relationshipKey != null || collection != null) {
            if (mustMatch == null)
                mustMatch = new LinkedHashMap<>();

            if (collection != null && !mustMatch.containsKey(Request.COLLECTION_KEY))
                mustMatch.put(Request.COLLECTION_KEY, collection.getName());

            if (resourceKey != null && !mustMatch.containsKey(Request.RESOURCE_KEY))
                mustMatch.put(Request.RESOURCE_KEY, resourceKey);

            if (relationshipKey != null && !mustMatch.containsKey(Request.RELATIONSHIP_KEY))
                mustMatch.put(Request.RELATIONSHIP_KEY, relationshipKey);
        }

        for (Operation op : api.getOperations()) {

            Path path = new Path(op.getOperationMatchPath());
            System.out.println(path);

            if (function != null && !function.equalsIgnoreCase(op.getFunction()))
                continue;

            if (method != null && !method.equalsIgnoreCase(op.getMethod()))
                continue;

            if (collection != null && collection != op.getCollection())
                continue;


            boolean match = true;
            if (mustMatch != null) {
                for (String key : mustMatch.keySet()) {
                    String value = mustMatch.get(key);
                    if (value == null)
                        throw new ApiException("A path mapped variable may not be null.");
                    boolean found = false;
                    for (int i = 0; i < path.size(); i++) {
                        if (path.isVar(i)) {
                            List<Parameter> params = op.getParams(i);
                            for (Parameter param : params) {
                                if (key.equalsIgnoreCase(param.getKey())) {
                                    boolean regexMatch = true;
                                    for (Pattern pattern : param.getPatterns()) {
                                        if (!pattern.matcher(value).matches()) {
                                            regexMatch = false;
                                            break;
                                        }
                                    }
                                    if (regexMatch) {
                                        path.set(i, value);
                                        found = true;
                                        break;
                                    }
                                }
                            }
                        }
                        if (found)
                            break;
                    }
                    if (!found) {
                        match = false;
                        break;
                    }
                }
            }
            if (!match)
                continue;

            //-- now match any remaining unmatched path vars
            for (int i = 0; i < path.size(); i++) {
                boolean found = !path.isVar(i);
                if (!found && req != null) {
                    List<Parameter> params = op.getParams(i);
                    for (Parameter param : params) {
                        String value = req.getUrl().getParam(param.getKey());
                        if (value != null) {
                            boolean regexMatch = true;
                            for (Pattern pattern : param.getPatterns()) {
                                if (!pattern.matcher(value).matches()) {
                                    regexMatch = false;
                                    break;
                                }
                            }
                            if (regexMatch) {
                                path.set(i, value);
                                found = true;
                                break;
                            }
                        }
                    }
                    if (!found) {
                        match = false;
                        break;
                    }
                }
                if (!match)
                    break;
            }
            if (!match)
                continue;

            Request linkedReq = new Request();
            linkedReq.withOperation(op);
            String  url       = "/" + path.toString();

            if (req != null) {
                String host = req.getUrl().getProtocol() + "://" + req.getUrl().getHost() + (req.getUrl().getPort() > 0 ? ":" + req.getUrl().getPort() : "");
                url = host + url;
            }
            linkedReq.withUrl(url);
            matches.add(linkedReq);

        }
        if (matches.size() > 0) {
            //-- return the match with the greatest number of satisfied pathParam matches
            //-- TODO: need test cases
            if (matches.size() > 1) {
                Collections.sort(matches, new Comparator<Request>() {
                    @Override
                    public int compare(Request o1, Request o2) {
                        int s1 = o1.getOperation().getPathParamCount();
                        int s2 = o2.getOperation().getPathParamCount();
                        return s1 == s2 ? 0 : (s1 > s2 ? 1 : -1);
                    }
                });
            }
            return matches.get(matches.size() - 1);
        }

        return null;
    }

    public String getName() {
        return name;
    }

    public Linker withName(String name) {
        this.name = name;
        return this;
    }

    public Api getApi() {
        return api;
    }

    public Linker withApi(Api api) {
        this.api = api;
        if (api.getLinker() != this)
            api.withLinker(this);
        return this;
    }
}
