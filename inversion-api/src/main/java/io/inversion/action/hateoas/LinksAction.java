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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

public class LinksAction extends Action<LinksAction> {

    public void run(Request req, Response res) throws ApiException {

        req.getChain().go();

        if(res.isSuccess() && req.getCollection() != null && res.getJson() != null) {
            JSNode data = res.getJson().getNode("data");
            if (data != null) {
                data.stream().filter(node -> node instanceof JSNode && !(node instanceof JSArray)).forEach(node -> updateResourceLinks(req, (JSNode) node));
            }
        }
    }

    protected void updateResourceLinks(Request req, JSNode node){

        String link = null;
        String resourceKey = req.getCollection().encodeJsonKey(node);
        Collection coll = req.getCollection();

        LinkedHashMap<String, String> toAdd = new LinkedHashMap<>();

        if(coll != null && resourceKey != null){
            for(Relationship rel : req.getCollection().getRelationships()){

                if(node.get(rel.getName()) instanceof JSNode)//-- means this property was expanded.
                    continue;

                if(rel.isManyToOne()){
                    //-- this is an optimization that prevents potentially unnecessary db queries on return access
                    String key = rel.getCollection().encodeJsonKey(node, rel.getFkIndex1());
                    if(key != null)
                        link = Chain.buildLink(rel.getRelated(), key, null);
                }

                if(link == null)
                    link = Chain.buildLink(req.getCollection(), resourceKey, rel.getName());

                toAdd.put(rel.getName(), link);
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

        link = node.getString("href");
        if(link == null){
            node.putFirst("href", Chain.buildLink(coll, resourceKey, null));
        }
    }

}
