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
package io.inversion.action.db;

import io.inversion.*;
import io.inversion.Collection;
import io.inversion.action.openapi.OpenAPIWriter;
import io.inversion.json.JSMap;
import io.inversion.Url;
import io.inversion.utils.Utils;

import java.util.*;

public class DbDeleteAction<A extends DbDeleteAction> extends Action<A> implements OpenAPIWriter<A> {

    @Override
    protected List<RuleMatcher> getDefaultIncludeMatchers(){
        return Utils.asList(new RuleMatcher("DELETE", "{" + Request.COLLECTION_KEY + "}/[{" + Request.RESOURCE_KEY + "}]"));
    }

    @Override
    public void run(Request req, Response res) throws ApiException {
        String resourceKey     = req.getResourceKey();
        String relationshipKey = req.getRelationshipKey();

        if (Utils.empty(resourceKey))
            throw ApiException.new400BadRequest("An resource key must be included in the url path for a DELETE request.");

        if (!Utils.empty(relationshipKey))
            throw ApiException.new400BadRequest("A relationship key in the url path is not valid for a DELETE request");

        if (req.getJson() != null)
            throw ApiException.new501NotImplemented("A JSON body can not be included with a DELETE.  Batch delete is not supported.");

        int deleted = delete(req.getEngine(), req.getCollection(), req.getUrl());

        if (deleted < 1)
            throw ApiException.new404NotFound("The requested resource '{}' could not be found.", resourceKey);
        else
            res.withStatus(Status.SC_204_NO_CONTENT);
    }

    protected int delete(Engine engine, Collection collection, Url url) throws ApiException {
        int deleted = 0;

        Set<String> alreadyDeleted = new HashSet<>();

        for (int i = 0; i < 1000; i++) {
            //-- regardless of the query string passed in, this should resolve the keys 
            //-- that need to be deleted and make sure the user has read access to the key

            Url query = new Url(url.getOriginal());
            query.withParam("include", Utils.implode(",", collection.getResourceIndex().getJsonNames()));
            String urlStr = query.toString();
            Response res = engine.get(urlStr).assertStatus(200, 404);

            if (res.hasStatus(404))
                break;

            List<Map<String, Object>> rows = new ArrayList();

            for (JSMap node : res.data().asMapList()) {

                String key = collection.encodeKeyFromJsonNames(node);

                if (alreadyDeleted.contains(key))
                    throw ApiException.new500InternalServerError("Deletion of '{}' was not successful.", key);
                else
                    alreadyDeleted.add(key);

                rows.add(node);
            }
            collection.getDb().delete(collection, rows);
            deleted += res.data().size();
        }

        return deleted;
    }
}
