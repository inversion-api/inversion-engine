/*
 * Copyright (c) 2015-2020 Rocket Partners, LLC
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
package io.inversion.openapi.v3;

import io.inversion.*;
import io.inversion.utils.Path;
import io.inversion.utils.Utils;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;

import java.util.List;

/**
 * https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Getting-started
 * https://javadoc.io/doc/io.swagger.core.v3/swagger-models/latest/index.html
 * https://mvnrepository.com/artifact/io.swagger.core.v3
 * https://mvnrepository.com/artifact/io.swagger.core.v3/swagger-core
 * https://swagger.io/tools/open-source/open-source-integrations/
 * https://github.com/swagger-api/swagger-core
 * https://swagger.io/specification/
 */
public class OpenApiAction<A extends OpenApiAction> extends Action<A> {

    public OpenApiAction() {

    }

    public OpenApiAction(String methods, String... includePaths) {
        super(methods, includePaths);
    }

    public void doGet(Request req, Response res) throws ApiException {

        OpenAPI openApi = new OpenAPI();

        Api            api       = req.getApi();
        List<Endpoint> endpoints = api.getEndpoints();
        for (Endpoint endpoint : endpoints) {

            //don't document this swagger producing endpoint
            if (endpoint.matches("GET", "openapi.json"))
                continue;

            for (RuleMatcher endpointMatcher : endpoint.getIncludeMatchers()) {
                List<Path> paths = endpointMatcher.getPaths();
                for (Path endpointPath : paths) {
                    Path endpointBasePath = new Path();
                    for (int i = 0; i < endpointPath.size(); i++) {
                        if (endpointPath.isOptional(i) || endpointPath.isWildcard(i))
                            break;
                        endpointBasePath.add(endpointPath.get(i));
                    }

                    for (Collection collection : api.getCollections()) {

                        for (RuleMatcher collectionMatcher : collection.getIncludeMatchers()) {
                            for (Path collectionPath : collectionMatcher.getPaths()) {
                                Path epColPath = new Path(endpointBasePath.toString(), collectionPath.toString());

                                //TODO if endoint or collection path won't match the combine path
                                //continue;

                                //now we have to create permutations for all optional variants
                                boolean hasCollectionKey = false;
                                boolean hasResourceKey   = false;
                                Path    candidatePath    = new Path();
                                for (int i = 0; i < epColPath.size(); i++) {
                                    String part = epColPath.get(i);
                                    if (epColPath.isStatic(i)) {
                                        candidatePath.add(epColPath.get(i));
                                    } else if (epColPath.isVar(i)) {
                                        String varName = epColPath.getVarName(i);
                                        String regex   = epColPath.getRegex(i);

                                        if (Request.COLLECTION_KEY.equalsIgnoreCase(varName)) {

                                            hasCollectionKey = true;

                                            //this should not be a regex...maybe should check for these chars \.[]{}()<>*+-=!?^$|
                                            //https://stackoverflow.com/questions/14134558/list-of-all-special-characters-that-need-to-be-escaped-in-a-regex
                                            candidatePath.add(regex);

                                            //consume any additional static and non collection vars
                                            for (int j = i + 1; j < epColPath.size(); j++) {
                                                if (epColPath.isWildcard(j)) {
                                                    break;
                                                } else if (epColPath.isStatic(j)) {
                                                    i++;
                                                    candidatePath.add(epColPath.get(j));
                                                } else if (epColPath.isVar(j)) {
                                                    String nextVar = epColPath.getVarName(j);
                                                    if (Utils.in(nextVar, Request.COLLECTION_KEY, Request.RESOURCE_KEY, Request.RELATIONSHIP_KEY))
                                                        break;

                                                    i++;
                                                    candidatePath.add(nextVar);
                                                }
                                            }

                                            PathItem pathItem = new PathItem();

                                            if (endpointMatcher.hasMethod("GET") && collectionMatcher.hasMethod("GET")) {
                                                pathItem.setGet(new Operation());
                                            }
                                            if (endpointMatcher.hasMethod("POST") && collectionMatcher.hasMethod("POST")) {
                                                pathItem.setPost(new Operation());
                                            }
                                            if (endpointMatcher.hasMethod("DELETE") && collectionMatcher.hasMethod("DELETE")) {
                                                pathItem.setDelete(new Operation());
                                            }

                                            openApi.path("/" + candidatePath, pathItem);

                                        } else if (Request.RESOURCE_KEY.equalsIgnoreCase(varName)) {

                                            Index pk = collection.getPrimaryIndex();
                                            if (pk.size() == 1) {
                                                String var = pk.getPropertyName(0);
                                                System.out.println(var);
                                                candidatePath.add("{" + var + "}");
                                                System.out.println(var);
                                            } else
                                                candidatePath.add("{key}");


                                            //consume any additional static and non collection vars
                                            for (int j = i + 1; j < epColPath.size(); j++) {
                                                if (epColPath.isWildcard(j)) {
                                                    break;
                                                } else if (epColPath.isStatic(j)) {
                                                    i++;
                                                    candidatePath.add(epColPath.get(j));
                                                } else if (epColPath.isVar(j)) {
                                                    String nextVar = epColPath.getVarName(j);
                                                    if (Utils.in(nextVar, Request.COLLECTION_KEY, Request.RESOURCE_KEY, Request.RELATIONSHIP_KEY))
                                                        break;

                                                    i++;
                                                    candidatePath.add(nextVar);
                                                }
                                            }

                                            PathItem pathItem = new PathItem();

                                            if (endpointMatcher.hasMethod("PUT") && collectionMatcher.hasMethod("PUT")) {
                                                pathItem.setPut(new Operation());
                                            }
                                            if (endpointMatcher.hasMethod("PUT") && collectionMatcher.hasMethod("PUT")) {
                                                pathItem.setPut(new Operation());
                                            }
                                            if (endpointMatcher.hasMethod("PATCH") && collectionMatcher.hasMethod("PATCH")) {
                                                pathItem.setPatch(new Operation());
                                            }
                                            openApi.path("/" + candidatePath, pathItem);

                                        } else if (regex != null) {

                                        } else if (varName != null) {
                                            candidatePath.add("{" + varName + "}");
                                        }

                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        res.withText(Json.pretty(openApi));
    }
}
