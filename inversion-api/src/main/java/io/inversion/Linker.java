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

import java.util.ArrayList;
import java.util.List;

public class Linker {

    protected String name = null;


    public Linker() {
    }

    public String buildLink(Request req, Collection collection, String resourceKey, String relationshipKey) {

        List<Op> found = new ArrayList();
        for (Op op : req.getApi().getOps()) {
            if (op.getCollection() != collection)
                continue;

            if (resourceKey != null) {
                if (relationshipKey != null) {
                    if (op.getFunction() != Op.OpFunction.RELATED)
                        continue;
                } else {
                    if (op.getFunction() != Op.OpFunction.GET)
                        continue;
                }
            } else {
                if (op.getFunction() != Op.OpFunction.FIND)
                    continue;
            }

            Path opPath = new Path(op.getPath());
            for (Param p : op.getParams()) {
                if (p.getIn() == Param.In.PATH) {
                    if (p.getKey().equalsIgnoreCase("_collection")) {
                        opPath.set(p.getIndex(), collection.getName());
                    } else if (p.getKey().equalsIgnoreCase("_resource")) {
                        opPath.set(p.getIndex(), resourceKey);
                    } else if (p.getKey().equalsIgnoreCase("_relationship")) {
                        opPath.set(p.getIndex(), relationshipKey);
                    }
                }
            }


            for (int i = 0; i < opPath.size(); i++) {
                if (opPath.isVar(i)) {
                    opPath = null;
                    break;
                }
            }
            if (opPath != null) {
                Path serverPath = new Path(req.getServerPath());
                Url  url        = req.getUrl();

                String link = req.getUrl().getProtocol() + "://" + req.getUrl().getHost() + (req.getUrl().getPort() > 0 ? (":" + req.getUrl().getPort()) : "");
                Path   path = new Path(req.getServerPath().toString(), opPath.toString());
                link += "/" + path;

                return link;
            }
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

}
