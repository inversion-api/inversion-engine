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
import io.inversion.action.openapi.OpenAPIWriter;
import io.inversion.utils.Path;
import io.inversion.utils.Task;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class DbAction extends Action<DbAction> implements OpenAPIWriter<DbAction> {
    private DbGetAction    getAction    = new DbGetAction();
    private DbPostAction   postAction   = new DbPostAction();
    private DbPutAction    putAction    = new DbPutAction();
    private DbPatchAction  patchAction  = new DbPatchAction();
    private DbDeleteAction deleteAction = new DbDeleteAction();

    @Override
    public void hook_enumerateOps(Task taskChain, List<Op> ops) {
        List<Op> fixed = new ArrayList();
        for (Op op : ops) {
            fixed.addAll(enumerateOps(op));
        }
        ops.clear();
        ops.addAll(fixed);
    }


    public List<Op> enumerateOps(Op template) {
        if(template.getParam(Param.In.PATH, Request.COLLECTION_KEY) == null)
            return Collections.emptyList();

        if (template.getDb() == null || template.getDb().getCollections().size() == 0)
            return Collections.emptyList();

        List<Op> workingOps = new ArrayList();
        workingOps.add(template);

        for (int i = 0; i < workingOps.size(); i++) {
            Op op = workingOps.get(i);
            if (op.getCollection() != null)
                continue;
            else {
                workingOps.remove(i);
                i--;
                for (Collection coll : (List<Collection>) template.getDb().getCollections()) {

                    String colName = coll.getName();
                    Param colP = op.getParam(Param.In.PATH, Request.COLLECTION_KEY);
                    boolean matched = true;
                    if(colP != null){
                        String colNameMatch = op.getPathParamValue(Request.COLLECTION_KEY);
                        colNameMatch = Path.unwrapOptional(colNameMatch);
                        if(Path.isVar(colNameMatch)){
                            String regex = Path.getRegex(colNameMatch);
                            if (regex != null) {
                                Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                                if (!pattern.matcher(colName).matches()){
                                    matched = false;
                                }
                            }
                        }
                        else{
                            matched = coll.getName().equalsIgnoreCase(colNameMatch);
                        }
                        if(!matched)
                            continue;
                    }

                    Op clone = op.copy();
                    clone.withCollection(coll);
                    workingOps.add(clone);
                }
            }
        }

        for (int i = 0; i < workingOps.size(); i++) {
            Op         op   = workingOps.get(i);
            Collection c    = op.getCollection();
            Path       path = op.getPath();
            if (path.hasAllVars(Request.COLLECTION_KEY)) {
                for (int j = 0; j < path.size(); j++) {
                    if (path.isVar(j) && Request.COLLECTION_KEY.equalsIgnoreCase(path.getVarName(j))) {
                        String name = op.getCollection().getName();
                        path.set(j, name);
                    }
                }
            }
            if (path.hasAllVars(Request.RESOURCE_KEY)) {
                String name = null;
                Index  idx  = c.getResourceIndex();
                if (idx != null && idx.size() == 1) {
                    name = idx.getJsonName(0);
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

                for (int j = 0; j < path.size(); j++) {
                    if (path.isVar(j) && Request.RESOURCE_KEY.equals(path.getVarName(j))) {
                        path.set(j, "{" + name + "}");
                    }
                }
            }


            op.withPath(path);
        }

        //-- add a new copy of the op for each relationship
        for (int i = 0; i < workingOps.size(); i++) {
            Op op = workingOps.get(i);
            if (op.getRelationship() != null)
                continue;

            Param relP = op.getParam(Param.In.PATH, Request.RELATIONSHIP_KEY);
            if (relP == null)
                continue;

            workingOps.remove(i);
            i--;
            for (Relationship rel : op.getCollection().getRelationships()) {
                Op clone = op.copy();
                clone.withRelationship(rel);
                workingOps.add(clone);
            }
        }

        for (int i = 0; i < workingOps.size(); i++) {
            Op op = workingOps.get(i);
            Relationship rel = op.getRelationship();
            if (rel == null)
                continue;
            Path path = op.getPath();
            if (path.hasAllVars(Request.RELATIONSHIP_KEY)) {
                for (int j = 0; j < path.size(); j++) {
                    if (path.isVar(j) && Request.RELATIONSHIP_KEY.equals(path.getVarName(j))) {
                        path.set(j, rel.getName());
                    }
                }
            }
            op.withPath(path);
        }

        for (int i = 0; i < workingOps.size(); i++) {
            Op op = workingOps.get(i);
            if (op.getPath().hasAnyVars(Request.COLLECTION_KEY, Request.RESOURCE_KEY, Request.RELATIONSHIP_KEY)) {
                workingOps.remove(i);
                i--;
            }
        }

        //TODO validate that the action paths match
//        for (int i = 0; i < workingOps.size(); i++) {
//            Op op = workingOps.get(i);
//            Path epPath = op.getEndpointPathMatch();
//            Path opPath = op.getPath();
//            if(!epPath.matches(opPath)){
//                workingOps.remove(i);
//                i--;
//            }
//        }

        return workingOps;
    }


    @Override
    public void hook_documentOp(Task docChain, OpenAPI openApi, List<Op> ops, Op op, Map<Object, Schema> schemas) {
        switch (op.getMethod().toUpperCase()) {
            case "GET":
                getAction.hook_documentOp(docChain, openApi, ops, op, schemas);
                break;
            case "POST":
                postAction.hook_documentOp(docChain, openApi, ops, op, schemas);
                break;
            case "PUT":
                putAction.hook_documentOp(docChain, openApi, ops, op, schemas);
                break;
            case "PATCH":
                patchAction.hook_documentOp(docChain, openApi, ops, op, schemas);
                break;
            case "DELETE":
                deleteAction.hook_documentOp(docChain, openApi, ops, op, schemas);
                break;
        }
    }

    @Override
    public DbAction hook_configureOp(Task task, Op op) {
        switch (op.getMethod().toUpperCase()) {
            case "GET":
                getAction.hook_configureOp(task, op);
                break;
            case "POST":
                postAction.hook_configureOp(task, op);
                break;
            case "PUT":
                putAction.hook_configureOp(task, op);
                break;
            case "PATCH":
                patchAction.hook_configureOp(task, op);
                break;
            case "DELETE":
                deleteAction.hook_configureOp(task, op);
                break;
        }
        return this;
    }

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
