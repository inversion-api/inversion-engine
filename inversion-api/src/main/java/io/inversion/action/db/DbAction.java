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
import io.inversion.utils.Path;

import java.util.ArrayList;
import java.util.List;

public class DbAction extends Action<DbAction> {
    protected DbGetAction    getAction    = new DbGetAction();
    protected DbPostAction   postAction   = new DbPostAction();
    protected DbPutAction    putAction    = new DbPutAction();
    protected DbPatchAction  patchAction  = new DbPatchAction();
    protected DbDeleteAction deleteAction = new DbDeleteAction();

    protected List<Path> getOperationPaths(Engine engine, Api api, Endpoint ep, String method, Path enginePath, Path apiPath, Path epPath) {
        List<Path> paths = new ArrayList();

        if(!matches(method, epPath)){
            return paths;
        }

        for(Collection collection : api.getCollections()){
            if(collection.matches(method, epPath)){

                String collectionKey = collection.getName();
                String resourceKey = null;

                Index pk = collection.getPrimaryIndex();
                if (pk != null && pk.size() == 1) {
                    resourceKey = pk.getJsonName(0);
                } else {
                    resourceKey = collection.getName() + "Id";
                    for (int j = 0; j < 10; j++) {
                        if (collection.getProperty(resourceKey) == null)
                            break;
                        resourceKey = "_" + resourceKey;
                    }
                }
                resourceKey = "{" + resourceKey + "}";


                Path copy = epPath.copy();
                for(int i=0; i< copy.size(); i++){
                    if(Request.COLLECTION_KEY.equalsIgnoreCase(copy.get(i))){
                        copy.set(i, collection.getName());
                    }
                    if(Request.RESOURCE_KEY.equalsIgnoreCase(copy.get(i))){
                        copy.set(i, resourceKey);
                    }
                }

                if(!epPath.hasAllVars(Request.RELATIONSHIP_KEY)){
                    paths.add(copy);
                }
                else{
                    for(Relationship rel : collection.getRelationships()){
                        Path relCopy = copy.copy();
                        for(int i=0; i<relCopy.size(); i++){
                            if(Request.RELATIONSHIP_KEY.equalsIgnoreCase(relCopy.get(i))){
                                relCopy.set(i, rel.getName());
                            }
                        }
                        paths.add(relCopy);
                    }
                }
            }
        }
        return paths;
    }


    @Override
    public void run(Request req, Response res) throws ApiException {
        if (req.isMethod("GET")) {
            getAction.run(req, res);
        } else if (req.isMethod("POST")) {
            postAction.run(req, res);
        } else if (req.isMethod("PUT")) {
            putAction.run(req, res);
        } else if (req.isMethod("PATCH")) {
            patchAction.run(req, res);
        } else if (req.isMethod("DELETE")) {
            deleteAction.run(req, res);
        }
    }

    public DbGetAction getGetAction() {
        return getAction;
    }

    public DbAction withGetAction(DbGetAction getAction) {
        this.getAction = getAction;
        return this;
    }

    public DbPostAction getPostAction() {
        return postAction;
    }

    public DbAction withPostAction(DbPostAction postAction) {
        this.postAction = postAction;
        return this;
    }

    public DbPutAction getPutAction() {
        return putAction;
    }

    public DbAction withPutAction(DbPutAction putAction) {
        this.putAction = putAction;
        return this;
    }

    public DbPatchAction getPatchAction() {
        return patchAction;
    }

    public DbAction withPatchAction(DbPatchAction patchAction) {
        this.patchAction = patchAction;
        return this;
    }

    public DbDeleteAction getDeleteAction() {
        return deleteAction;
    }

    public DbAction withDeleteAction(DbDeleteAction deleteAction) {
        this.deleteAction = deleteAction;
        return this;
    }

}
