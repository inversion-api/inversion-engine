/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
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
package io.inversion.action.security;

import io.inversion.*;


import java.util.ArrayList;
import java.util.List;

public class AuthFilter extends Filter<AuthFilter> {
    public static final int AUTH_ACTION_DEFAULT_ORDER_IS_100 = 100;

    protected List<AuthScheme> schemes = new ArrayList();

    public AuthFilter() {
        withOrder(AUTH_ACTION_DEFAULT_ORDER_IS_100);
    }

    @Override
    public void run(Request req, Response resp) throws ApiException {
        User user = Chain.getUser();

        if (user == null) {
            for (AuthScheme scheme : schemes) {
                user = scheme.getUser(req, resp);
                if (user != null)
                    break;
            }
        }

        if (user == null)
            throw ApiException.new401Unauthroized();

        Chain.peek().withUser(user);

    }

    public List<AuthScheme> getAuthSchemes() {
        return schemes;
    }

    public AuthFilter withAuthSchemes(List<AuthScheme> schemes) {
        schemes.forEach(this::withAuthScheme);
        return this;
    }

    public AuthFilter withAuthScheme(AuthScheme scheme) {
        if (scheme != null && !schemes.contains(scheme))
            schemes.add(scheme);
        return this;
    }

//    @Override
//    public Operation hook_documentOp(Task docChain, OpenAPI openApi, List<Op> ops, Op op, Map<Object, Schema> schemas) {
//        Operation operation = super.hook_documentOp(docChain, openApi, ops, op, schemas);
//
//        if (op.isEpAction(this)) {
//            //System.out.println("asdf");
//        } else {
//            documentSecurity(openApi, null);
//        }
//
//
//        return operation;
//    }
//
//    protected void documentSecurity(OpenAPI openApi, Op op) {
//
//        if (openApi.getSecurity() != null)
//            return;
//
//        Components                  components      = openApi.getComponents();
//        Map<String, SecurityScheme> securitySchemes = new LinkedHashMap();
//        List<AuthScheme> schemes = getAuthSchemes();
//
//        //List<SecurityRequirement> securityRequirements = new ArrayList<>();
//        ListMap<String, String> securityRequirementsNames = new ListMap<>();
//
//        for (int i = 0; i < schemes.size(); i++) {
//            AuthScheme scheme = schemes.get(i);
//
//            String name = scheme.getName();
//            if (name == null)
//                name = scheme.getClass().getSimpleName() + (i + 1);
//            String description = scheme.getDescription();
//
//            if(scheme instanceof BasicScheme){
//                SecurityScheme oasSS = new SecurityScheme();
//                securitySchemes.put(name, oasSS);
//                oasSS.setName(name);
//                oasSS.setDescription(description);
//                oasSS.setType(SecurityScheme.Type.HTTP);
//                oasSS.setScheme("basic");
//
//                securityRequirementsNames.put(name, name);
//            }
//            else if (scheme instanceof BearerScheme) {
//
//                SecurityScheme oasSS = new SecurityScheme();
//                securitySchemes.put(name, oasSS);
//                oasSS.setName(name);
//                oasSS.setDescription(description);
//                oasSS.setType(SecurityScheme.Type.HTTP);
//                oasSS.setScheme("bearer");
//                oasSS.setBearerFormat(((BearerScheme) scheme).getBarerFormat());
//                securityRequirementsNames.put(name, name);
//
//
//            } else if (scheme instanceof ApiKeyScheme) {
//
//                for (Param param : scheme.getParams()) {
//
//                    String apiKeyName = name + "_" + param.getKey() + "_" + param.getIn();
//
//                    SecurityScheme oasSS = new SecurityScheme();
//                    securitySchemes.put(apiKeyName, oasSS);
//                    oasSS.setDescription(description);
//                    oasSS.setType(SecurityScheme.Type.APIKEY);
//                    oasSS.setName(param.getKey());
//
//                    securityRequirementsNames.put(name, apiKeyName);
//
//                    switch (param.getIn()) {
//                        case HEADER:
//                            oasSS.setIn(SecurityScheme.In.HEADER);
//                            break;
//                        case QUERY:
//                            oasSS.setIn(SecurityScheme.In.QUERY);
//                            break;
//                        case COOKIE:
//                            oasSS.setIn(SecurityScheme.In.COOKIE);
//                            break;
//                    }
//                }
//            }
//
//        }
//
//        components.setSecuritySchemes(securitySchemes);
//
//        List<SecurityRequirement> securityRequirements = new ArrayList<>();
//        for(String name : securityRequirementsNames.keySet()){
//            SecurityRequirement req = new SecurityRequirement();
//            req.put(name, securityRequirementsNames.get(name));
//            securityRequirements.add(req);
//        }
//
//        openApi.setSecurity(securityRequirements);
//
//
//    }
}
