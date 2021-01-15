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
import io.inversion.utils.JSArray;
import io.inversion.utils.JSNode;

public class HALAction extends Action<HALAction> {

    public void run(Request req, Response res) throws ApiException {

        req.getChain().go();

        if (res.isSuccess()) {
            JSNode json = res.getJson();

            if (json != null) {

                if (json.hasProperty("_links"))
                    return; //-- already in HAL format, could be from a custom action or internal recursion

                if (req.getCollection() != null) {

                    JSNode meta = res.findNode("meta");

                    if (meta != null) {

                        int page  = res.getPageNum();
                        int size  = res.getPageSize();
                        int pages = res.getPageNum();
                        int total = res.getFoundRows();

                        json.put("page", page);
                        json.put("size", size);
                        //json.put("sort", sort);
                        json.put("pages", pages);
                        json.put("total", total);

                        meta.removeAll("pageSize", "pageNum", "foundRows", "pageCount");

                        JSNode links = new JSNode();
                        for (String key : meta.keySet()) {
                            String value = meta.get(key) + "";
                            if (value != null) {
                                if (value.startsWith("http://") || value.startsWith("https://")) {
                                    links.put(key, new JSNode("href", value));
                                } else {
                                    json.put(key, value);
                                }
                            }
                        }
                        json.remove("meta");
                        if (links.size() > 0)
                            json.putFirst("_links", links);
                    }

                    //-- move ".data" to "._embedded" and update resource links
                    if (json.find("data") instanceof JSArray) {
                        JSArray data = (JSArray) json.remove("data");
                        json.put("_embedded", data);
                        data.stream().filter(node -> node instanceof JSNode && !(node instanceof JSArray)).forEach(node -> updateResourceLinks(req, (JSNode) node));
                    }

                    //-- make sure the root is "self" linked
                    if (Chain.getDepth() == 1) {

                        //-- unwrap a root response for a single resource
                        if (req.getResourceKey() != null && req.getRelationshipKey() == null) {
                            JSArray array = res.getData();
                            if (array != null && array.size() == 1 && array.get(0) instanceof JSNode) {
                                json = array.getNode(0);
                                res.withJson(json);
                            }
                        }

                        JSNode links = json.getNode("_links");
                        if (links == null) {
                            links = new JSNode();
                            json.putFirst("_links", links);
                        }

                        if (!links.containsKey("self"))
                            links.putFirst("self", new JSNode("href", req.getUrl().getOriginal()));

                    }
                }

                //-- simply makes sure "_links" is the top property
                json.streamAll().filter(node -> node instanceof JSNode && !(node instanceof JSArray) && ((JSNode) node).hasProperty("_links")).forEach(node -> {
                    Object links = ((JSNode) node).remove("_links");
                    ((JSNode) node).putFirst("_links", links);
                });

            }
        }
    }

    protected void updateResourceLinks(Request req, JSNode node) {

        if (node.hasProperty("_links"))
            return;

        Collection coll        = req.getCollection();
        if(coll != null) {
            String resourceKey = coll.encodeJsonKey(node);
            if (resourceKey != null) {
                JSNode links = new JSNode();
                node.putFirst("_links", links);
                for (Relationship rel : req.getCollection().getRelationships()) {
                    String link = Chain.buildLink(req.getCollection(), resourceKey, rel.getName());
                    links.put(rel.getName(), new JSNode("href", link));
                }
                Object href = node.get("href");
                if (href instanceof String)
                    links.putFirst("href", href);
                else
                    links.putFirst("self", new JSNode("href", Chain.buildLink(coll, resourceKey, null)));
            }
        }
    }

}
