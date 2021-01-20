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
import io.inversion.Collection;
import io.inversion.utils.JSArray;
import io.inversion.utils.JSNode;
import io.inversion.utils.Rows;

import java.util.*;

public class LinksAction extends Action<LinksAction> {

    public void run(Request req, Response res) throws ApiException {
        if (Chain.isRoot() && req.getCollection() != null){
            if(req.getJson() != null){
                req.getData().asNodeList().forEach(node -> removeLinks(req.getCollection(), node));
            }

            req.getChain().go();

            if (res.isSuccess() && res.getJson() != null){
                Collection coll = req.getRelationship() != null ? req.getRelationship().getRelated() : req.getCollection();
                res.getData().stream().filter(node -> node instanceof JSNode && !(node instanceof JSArray)).forEach(node -> addLinks(coll, (JSNode) node));
            }
        }
    }

    protected void addLinks(Collection coll, JSNode node){

        String resourceKey = coll.encodeKeyFromJsonNames(node);
        LinkedHashMap<String, String> toAdd = new LinkedHashMap<>();

        if(coll != null && resourceKey != null){
            for(Relationship rel : coll.getRelationships()){

                Object value = node.get(rel.getName());
                if(value instanceof JSNode){
                    ((JSNode)value).asNodeList().forEach(child -> addLinks(rel.getRelated(), child));
                }
                else{
                    String link = null;
                    if(rel.isManyToOne()){

                        Map<String, Object> primaryKey = rel.buildPrimaryKeyFromForeignKey(node);
                        String key = primaryKey == null ? null : Collection.encodeKey(primaryKey, rel.getRelated().getPrimaryIndex(), true);

                        if(key != null)
                            link = Chain.buildLink(rel.getRelated(), key, null);
                        else
                            continue;
                    }

                    if(link == null)
                        link = Chain.buildLink(coll, resourceKey, rel.getName());

                    toAdd.put(rel.getName(), link);
                }
            }
        }

        //-- you can't change the properties in the above loop because you may be
        //-- modifying data that you need for the creation of additional MANY_TO_ONE
        //-- optimized links...meaning you may be overwriting data fields that are still important.
        if(toAdd.size() > 0){
            List<String> keys = new ArrayList(toAdd.keySet());
            Collections.reverse(keys);
            for(String key : keys){
                node.putFirst(key, toAdd.get(key));
            }
        }

        if(node.get("href") == null){
            String href = Chain.buildLink(coll, resourceKey, null);
            node.putFirst("href", href);
        }
    }


    protected void removeLinks(Collection coll, final JSNode node) {

        if (node.get("href") instanceof String){
            String href = (String)node.remove("href");

            if(href.startsWith("http://") || href.startsWith("https://")){
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
                    ((JSNode)value).asNodeList().forEach(child -> removeLinks(rel.getRelated(), child));
                }else {
                    if (rel.isManyToOne()) {
                        if (value != null && value instanceof String) {
                            String href = (String)value;
                            if(href.startsWith("http://") || href.startsWith("https://")) {
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
