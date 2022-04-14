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

import io.inversion.Action;
import io.inversion.Op;
import io.inversion.action.db.DbAction;
import io.inversion.utils.Task;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;

import java.util.List;
import java.util.Map;

public class SdkAction extends HATEOASAction<SdkAction>{


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

                Schema listWrapper = new Schema();
                String wrapperName = op.getName() + "Result";
                openApi.getComponents().addSchemas(wrapperName, listWrapper);

                schema.setType("object");

                ArraySchema arr = new ArraySchema();
                arr.setItems(as.getItems());

                listWrapper.addProperties("itemCount", newTypeSchema("number"));
                listWrapper.addProperties("totalCount", newTypeSchema("number"));
                listWrapper.addProperties("lastKey", newTypeSchema("number"));
                listWrapper.addProperties("pageNumber", newTypeSchema("number"));
                listWrapper.addProperties("pageSize", newTypeSchema("number"));
                listWrapper.addProperties("pageCount", newTypeSchema("number"));

                listWrapper.addProperties("items", arr);

                //addResponse(Operation operation, Op op, String status, String description, String schemaName)
                addResponse(operation, op, "200", null, wrapperName);
            }
        }
        return null;
    }

}
