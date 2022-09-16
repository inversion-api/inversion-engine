/*
 * Copyright (c) 2015-2022 Rocket Partners, LLC
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
import io.inversion.action.db.DbAction;
import io.inversion.json.JSList;
import io.inversion.json.JSMap;
import io.inversion.json.JSNode;
import io.inversion.utils.Task;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;

import java.util.List;
import java.util.Map;

public class SdkAction extends HATEOASAction<SdkAction>{

    public void run(Request req, Response res) throws ApiException {
        if (Chain.isRoot()
                && req.getCollection() != null
        ){
            req.getChain().go();

            if (res.isSuccess() && res.getJson() != null){
                if(req.getOp().getFunction() == Op.OpFunction.FIND){
                    updateFindResponse(req, res);
                }
                else{
                    removeMetaAndCollapseData(req, res);
                }
            }
        }
    }

    void removeMetaAndCollapseData(Request req, Response res){
        res.getJson().remove("meta");
        JSNode json = res.getJson();
        JSList data = res.data();
        if(data != json){
            if(data.size() == 1 && data.get(0) instanceof JSMap)
                res.withJson(data.getNode(0));
            else{
                res.withJson(new JSMap("items", data));
            }
        }
    }

    void updateFindResponse(Request req, Response res){
        JSNode data = res.data();
        System.out.println(res.getMeta());
        String next = res.getNext();
        int totalCount = res.getFoundRows();
        int itemCount = res.data() != null ? res.data().size() : 0;
        String lastKey = res.getLastKey();
        int pageSize = res.getPageSize();
        int pageCount = res.getPageCount();
        int pageNumber = res.getPageNum();

        JSMap page = res.findMap("page");
        if(page == null){
            page = new JSMap();
        }

        if(next != null)
            page.put("next", next);

        if(totalCount > -1)
            page.put("totalCount",totalCount );

        if(itemCount > -1)
            page.put("itemCount",itemCount );

        if(lastKey != null)
            page.put("lastKey", lastKey);

        if(pageSize > -1)
            page.put("pageSize",pageSize );

        if(pageCount > -1)
            page.put("pageCount", pageCount);

        if(pageNumber > -1)
            page.put("pageNumber", pageNumber);


        JSMap newJson = new JSMap();
        newJson.put("page", page);
        newJson.put("items", data);
        res.withJson(newJson);
    }


    @Override
    public Operation documentOpFind(Task docChain, OpenAPI openApi, List<Op> ops, Op op, Map<Object, Schema> schemas){
        docChain.go();
        boolean hasDbAction = false;
        for(Action a : op.getActions()){
            if(a instanceof DbAction){
                hasDbAction = true;
                break;
            }
        }
        if(hasDbAction){
            Operation operation = openApi.getPaths().get(op.getOperationPath()).getGet();
            Schema schema = schemas.get(op);
            if(operation != null && schema instanceof ArraySchema){
                ArraySchema as = (ArraySchema)schema;

                Schema paginationInfo = openApi.getComponents().getSchemas().get("PaginationInfo");
                if(paginationInfo == null){
                    paginationInfo = newTypeSchema("object", "Pagination and Sort Information");
                    openApi.getComponents().addSchemas("PaginationInfo", paginationInfo);
                    paginationInfo.addProperties("next", newTypeSchema("string", "The GET URL to return the next page of results."));
                    paginationInfo.addProperties("totalCount", newTypeSchema("number", "The number of items returned in this paginated response."));
                    paginationInfo.addProperties("itemCount", newTypeSchema("number", "The number of items identified as matching the query."));
                    paginationInfo.addProperties("lastKey", newTypeSchema("string", "The primary key of the last item returned in this page.  This can be returned as the 'after' query parameter to most efficiently continue pagination."));
                    paginationInfo.addProperties("pageNumber", newTypeSchema("number", "The page number returned by this response being between 1 and pageCount."));
                    paginationInfo.addProperties("pageSize", newTypeSchema("number", "The number of items that were requested to be returned."));
                    paginationInfo.addProperties("pageCount", newTypeSchema("number", "The total number of pages available meaning  (itemCount / pageSize) rounded up."));
                }

                Schema listWrapper = newTypeSchema("object", null);
                String wrapperName = op.getName() + "Result";
                openApi.getComponents().addSchemas(wrapperName, listWrapper);

                listWrapper.addProperties("page", newComponentRefSchema("PaginationInfo"));

                ArraySchema arr = new ArraySchema();
                arr.setItems(as.getItems());
                listWrapper.addProperties("items", arr);

                addResponse(operation, op, "200", null, wrapperName);
            }
        }
        return null;
    }

}
