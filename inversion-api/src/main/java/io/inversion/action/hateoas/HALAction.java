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
package io.inversion.action.hateoas;

import io.inversion.*;
import io.inversion.json.JSList;
import io.inversion.json.JSMap;
import io.inversion.json.JSNode;
import io.inversion.utils.Task;
import io.inversion.utils.Utils;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HALAction extends HATEOASAction<HALAction> {

//
//
//    public Operation hook_documentOp(Task docChain, OpenAPI openApi, List<Op> ops, Op op, Map<Object, Schema> schemas) {
//        docChain.go();
//        HALAction.super.hook_documentOp(docChain, openApi, ops, op, schemas);
//        updateResponseSchema(docChain, openApi, ops, op, schemas);
//    }
//
//
//    public String updateResponseSchema(Task docChain, OpenAPI openApi, List<Op> ops, Op op, Map<Object, Schema> schemas) {
//
//        String schemaName = super.documentResponseSchema(docChain, openApi, ops, op, schemas);
//
//        Op.OpFunction function = op.getFunction();
//        if (!Utils.in(function, Op.OpFunction.GET, Op.OpFunction.FIND, Op.OpFunction.RELATED))
//            return schemaName;
//
//        Schema              schema     = openApi.getComponents().getSchemas().get(schemaName);
//
//        if(schema == null)
//            return null;
//
//        Map<String, Schema> properties = schema.getProperties();
//        Schema              links      = properties.get("_links");
//        if (links == null) {
//            links = new Schema();
//            properties.put("_links", links);
//            links.addProperties("self", newHrefSchema());
//
//            Collection coll = op.getRelationship() != null ? op.getRelationship().getRelated() : op.getCollection();
//            if (Utils.in(function, Op.OpFunction.GET)) {
//
//                if (coll != null) {
//                    for (Relationship rels : coll.getRelationships()) {
//                        String name = rels.getName();
//                        links.addProperties(name, newHrefSchema());
//                    }
//                }
//            } else {
//                links.addProperties("first", newHrefSchema());
//                links.addProperties("prev", newHrefSchema());
//                links.addProperties("next", newHrefSchema());
//                links.addProperties("last", newHrefSchema());
//                links.addProperties("after", newHrefSchema());
//
//                Op getOp = findOp(ops, Op.OpFunction.GET, coll);
//                if(getOp != null){
//                    for(String key : new ArrayList<>(properties.keySet())){
//                        if(!key.equals("_links"))
//                            properties.remove(key);
//                    }
//
//                    String schemaRefName = getOp.getName() + "Response";
//                    Schema refSchema = newComponentRefSchema(schemaRefName);
//
//                    ArraySchema arr = new ArraySchema();
//                    arr.setItems(refSchema);
//                    properties.put("_embedded", arr);
//                }
//
//                schema.addProperties("page", newTypeSchema("number"));
//                schema.addProperties("size", newTypeSchema("number"));
//                schema.addProperties("pages", newTypeSchema("number"));
//                schema.addProperties("total", newTypeSchema("number"));
//            }
//        }
//        return schemaName;
//    }
//
//    public void run(Request req, Response res) throws ApiException {
//
//        req.getChain().go();
//
//        if (res.isSuccess()) {
//            JSNode json = res.getJson();
//
//            if (json != null) {
//
//                if (json.hasProperty("_links"))
//                    return; //-- already in HAL format, could be from a custom action or internal recursion
//
//                if (req.getCollection() != null) {
//
//                    JSNode meta = res.findObject("meta");
//
//                    if (meta != null) {
//
//                        int page  = res.getPageNum();
//                        int size  = res.getPageSize();
//                        int pages = res.getPageCount();
//                        int total = res.getFoundRows();
//
//                        json.putValue("page", page);
//                        json.putValue("size", size);
//                        //json.put("sort", sort);
//                        json.putValue("pages", pages);
//                        json.putValue("total", total);
//
//                        meta.removeAll("pageSize", "pageNum", "foundRows", "pageCount");
//
//                        JSNode links = new JSMap();
//                        for (String key : meta.keySet()) {
//                            String value = meta.getValue(key) + "";
//                            if (value != null) {
//                                if (value.startsWith("http://") || value.startsWith("https://")) {
//                                    links.putValue(key, new JSMap("href", value));
//                                } else {
//                                    json.putValue(key, value);
//                                }
//                            }
//                        }
//                        json.remove("meta");
//                        if (links.size() > 0)
//                            json.putFirst("_links", links);
//                    }
//
//                    //-- move ".data" to "._embedded" and update resource links
//                    if (json.find("data") instanceof JSList) {
//                        JSList data = (JSList) json.remove("data");
//                        json.putValue("_embedded", data);
//                        data.stream().filter(node -> node instanceof JSNode && !(node instanceof JSList)).forEach(node -> updateResourceLinks(req, (JSNode) node));
//                    }
//
//                    //-- make sure the root is "self" linked
//                    if (Chain.getDepth() == 1) {
//
//                        //-- unwrap a root response for a single resource
//                        if (req.getResourceKey() != null && req.getRelationshipKey() == null) {
//                            JSList array = res.data();
//                            if (array != null && array.size() == 1 && array.get(0) instanceof JSNode) {
//                                json = array.getNode(0);
//                                res.withJson(json);
//                            }
//                        }
//
//                        JSNode links = json.getNode("_links");
//                        if (links == null) {
//                            links = new JSMap();
//                            json.putFirst("_links", links);
//                        }
//
//                        if (!links.containsKey("self"))
//                            links.putFirst("self", new JSMap("href", req.getUrl().getOriginal()));
//
//                    }
//                }
//
//                //-- simply makes sure "_links" is the top property
//                json.streamAll().filter(node -> node instanceof JSNode && !(node instanceof JSList) && ((JSNode) node).hasProperty("_links")).forEach(node -> {
//                    Object links = ((JSNode) node).remove("_links");
//                    ((JSNode) node).putFirst("_links", links);
//                });
//
//            }
//        }
//    }
//
//    protected void updateResourceLinks(Request req, JSNode node) {
//
//        if (node.hasProperty("_links"))
//            return;
//
//        Collection coll = req.getCollection();
//        if (coll != null) {
//            String resourceKey = coll.encodeKeyFromJsonNames(node);
//            if (resourceKey != null) {
//                JSMap links = new JSMap();
//                node.putFirst("_links", links);
//                for (Relationship rel : req.getCollection().getRelationships()) {
//                    String link = Chain.buildLink(coll, resourceKey, rel.getName());
//                    links.put(rel.getName(), new JSMap("href", link));
//                }
//                Object href = node.getValue("href");
//                if (href instanceof String)
//                    links.putFirst("href", href);
//                else
//                    links.putFirst("self", new JSMap("href", Chain.buildLink(coll, resourceKey, null)));
//            }
//        }
//    }

}
