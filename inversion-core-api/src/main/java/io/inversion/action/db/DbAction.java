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
import io.inversion.utils.Task;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;


public class DbAction extends Action<DbAction> {

    private DbGetAction    getAction    = new DbGetAction();
    private DbPostAction   postAction   = new DbPostAction();
    private DbPutAction    putAction    = new DbPutAction();
    private DbPatchAction  patchAction  = new DbPatchAction();
    private DbDeleteAction deleteAction = new DbDeleteAction();

    @Override
    protected List<RuleMatcher> getDefaultIncludeMatchers() {

        List<RuleMatcher> matchers = new ArrayList<>();
        if (getAction != null)
            matchers.addAll(getAction.getIncludeMatchers());

        if (postAction != null)
            matchers.addAll(postAction.getIncludeMatchers());

        if (putAction != null)
            matchers.addAll(putAction.getIncludeMatchers());

        if (patchAction != null)
            matchers.addAll(patchAction.getIncludeMatchers());

        if (deleteAction != null)
            matchers.addAll(deleteAction.getIncludeMatchers());

        return matchers;
    }


    @Override
    protected LinkedHashSet<Path> getIncludePaths(Api api, Db db, String method) {
        LinkedHashSet<Path> includePaths = new LinkedHashSet<>();
        LinkedHashSet<Path> paths = super.getIncludePaths(api, db, method);
        for (Path actionPath : paths) {
            int collectionIdx   = actionPath.getVarIndex("_collection");
            int resourceIdx     = actionPath.getVarIndex("_resource");
            int relationshipIdx = actionPath.getVarIndex("_relationship");
            if (collectionIdx > -1) {
                for (Collection c : (List<Collection>) db.getCollections()) {
                    Path collPath = actionPath.copy();
                    collPath.set(collectionIdx, c.getName());

                    if (resourceIdx > -1) {
                        String resourceKey = "{" + getResourceKeyParamName(c) + "}";
                        collPath.set(resourceIdx, resourceKey);
                        if (relationshipIdx > -1) {
                            for (Relationship relationship : c.getRelationships()) {
                                Path relPath = collPath.copy();
                                relPath.set(relationshipIdx, relationship.getName());
                                includePaths.add(relPath);
                            }
                        } else {
                            includePaths.add(collPath);
                        }
                    } else {
                        includePaths.add(collPath);
                    }
                }
            } else if (collectionIdx < 0 && resourceIdx < 0 && relationshipIdx < 0) {
                includePaths.add(actionPath);
            }
        }
        return includePaths;
    }


    @Override
    public void configureOp(Task task, Op op) {
        super.configureOp(task, op);
        String method = op.getMethod();
        Path   path   = op.getPath();
        Db     db     = op.getApi().matchDb(method, path);
        if (db == null)
            return;

        String collName = op.getPathParamValue("_collection");
        if (collName != null) {
            Collection coll = db.getCollection(collName);
            if (coll != null) {
                op.withCollection(coll);
                String relName = op.getPathParamValue("_relationship");
                if (relName != null) {
                    op.withRelationship(coll.getRelationship(relName));
                }
            }
        }

        switch (op.getMethod().toUpperCase()) {
            case "GET":
                getAction.configureOp(task, op);
                break;
            case "POST":
                postAction.configureOp(task, op);
                break;
            case "PUT":
                putAction.configureOp(task, op);
                break;
            case "PATCH":
                patchAction.configureOp(task, op);
                break;
            case "DELETE":
                deleteAction.configureOp(task, op);
                break;
        }
    }

//    @Override
//    public Operation hook_documentOp(Task docChain, OpenAPI openApi, List<Op> ops, Op op, Map<Object, Schema> schemas) {
//        switch (op.getMethod().toUpperCase()) {
//            case "GET":
//                return getAction.hook_documentOp(docChain, openApi, ops, op, schemas);
//            case "POST":
//                return postAction.hook_documentOp(docChain, openApi, ops, op, schemas);
//            case "PUT":
//                return putAction.hook_documentOp(docChain, openApi, ops, op, schemas);
//            case "PATCH":
//                return patchAction.hook_documentOp(docChain, openApi, ops, op, schemas);
//            case "DELETE":
//                return deleteAction.hook_documentOp(docChain, openApi, ops, op, schemas);
//        }
//        return null;
//    }

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

    protected String getResourceKeyParamName(Collection c) {
        String name = null;
        Index  idx  = c.getResourceIndex();
        if (idx != null && idx.size() == 1) {
            name = idx.getJsonNames().get(0);
        } else {
            name = c.getSingularDisplayName() + "Id";
            name = Character.toLowerCase(name.charAt(0)) + name.substring(1);

            if (c.getProperty(name) != null) {
                name = name.substring(0, name.length() - 2) + "Key";
            }
            if (c.getProperty(name) != null) {
                name = "id";
            }

            if (c.getProperty(name) != null) {
                name = "key";
            }
            while (c.getProperty(name) != null) {
                name += "Id";
            }
        }
        return name;
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
