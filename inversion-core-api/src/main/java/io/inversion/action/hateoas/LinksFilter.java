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

import io.inversion.Collection;
import io.inversion.*;
import io.inversion.json.JSMap;
import io.inversion.json.JSNode;

import java.util.*;

public class LinksFilter extends HATEOASFilter<LinksFilter> {

    public void run(Request req, Response res) throws ApiException {

        if (Chain.isRoot() && req.getCollection() != null){
            if(req.getJson() != null){
                req.getData().asMapList().forEach(node -> removeLinks(req.getCollection(), node));
            }

            req.getChain().go();

            if (res.isSuccess() && res.getJson() != null){
                Collection coll = req.getRelationship() != null ? req.getRelationship().getRelated() : req.getCollection();
                res.data().stream().filter(node -> node instanceof JSMap).forEach(node -> addLinks(coll, (JSMap) node));
            }
        }
    }

    public void addSelfLink(JSMap entityNode, String link){
        addLink(entityNode, "href", link);
    }


    protected void removeLinks(Collection coll, final JSMap node) {


        if (node.get("href") instanceof String){
            String href = (String)node.remove("href");

            if(href.startsWith("http://") || href.startsWith("https://") || href.startsWith("/")){
                href = href.substring(href.lastIndexOf("/") + 1);
            }

            Map<String, Object> row = coll.decodeKeyToJsonNames((String) href);
            for (String key : row.keySet()) {
                node.put(key, row.get(key));
            }
        }

        for (Relationship rel : coll.getRelationships()) {
            if (node.containsKey(rel.getName())) {
                Object value = node.get(rel.getName());

                if (value instanceof JSNode) {
                    ((JSNode)value).asMapList().forEach(child -> removeLinks(rel.getRelated(), child));
                }else {
                    if (rel.isManyToOne()) {
                        if (value != null && value instanceof String) {
                            String href = (String)value;
                            if(href.startsWith("http://") || href.startsWith("https://") || href.startsWith("/")) {
                                node.remove(rel.getName());
                                href = href.substring(href.lastIndexOf("/") + 1);
                                Map<String, Object> primaryKey = rel.getRelated().decodeKeyToJsonNames(href);
                                Map<String, Object> foreignKey = rel.buildForeignKeyFromPrimaryKey(primaryKey);
                                for(String key : foreignKey.keySet()){
                                    if(!node.containsKey(key)){
                                        node.put(key, foreignKey.get(key));
                                    }
                                }
                            }
                        }
                    } else {
                        node.remove(rel.getName());
                    }
                }
            }
        }
    }
}
