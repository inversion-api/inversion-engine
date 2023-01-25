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
import io.inversion.action.openapi.OpenAPIWriter;
import io.inversion.json.JSMap;
import io.inversion.json.JSNode;

import java.util.*;

public class HATEOASFilter<T extends HATEOASFilter>  extends Filter<T> implements OpenAPIWriter<T> {

    public void addLinks(Collection coll, JSMap entityNode){
        String                        resourceKey = coll.encodeKeyFromJsonNames(entityNode);
        LinkedHashMap<String, String> toAdd       = new LinkedHashMap<>();
        if (coll != null && resourceKey != null) {
            for (Relationship rel : coll.getRelationships()) {
                Object value = entityNode.get(rel.getName());
                if (value instanceof JSNode) {
                    ((JSNode) value).asMapList().forEach(child -> addLinks(rel.getRelated(), child));
                } else {
                    String link = null;
                    if (rel.isManyToOne()) {

                        if(rel.getName().equalsIgnoreCase("reportsto"))
                            System.out.println("asdf");

                        Map<String, Object> primaryKey = rel.buildPrimaryKeyFromForeignKey(entityNode);
                        String              key        = primaryKey == null ? null : Collection.encodeKey(primaryKey, rel.getRelated().getResourceIndex(), true);
                        if (key != null)
                            link = Chain.buildLink(rel.getRelated(), key);
                        else
                            continue;
                    }
                    else{
                        link = Chain.buildLink(coll, resourceKey, rel.getName());
                    }
                    toAdd.put(rel.getName(), link);
                }
            }
        }

        //-- you can't change the properties in the above loop because you may be
        //-- modifying data that you need for the creation of additional MANY_TO_ONE
        //-- optimized links...meaning you may be overwriting data fields that are still important.
        if (toAdd.size() > 0) {
            List<String> keys = new ArrayList(toAdd.keySet());
            Collections.reverse(keys);
            for (String key : keys) {
                addLink(entityNode, key, toAdd.get(key));
            }
        }

        String link = Chain.buildLink(coll, resourceKey, null);
        addSelfLink(entityNode, link);
    }

    public void addSelfLink(JSMap entityNode, String link){
        addLink(entityNode, "self", link);
    }

    public void addLink(JSMap entityNode, String name, String link){
        System.out.println("-----------------");
        System.out.println(entityNode);
        entityNode.putFirst(name, link);
        System.out.println(name);
        System.out.println(entityNode);
        System.out.println("<<<-----------------");
    }
}