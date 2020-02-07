/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.cloud.action.misc;

import io.inversion.cloud.model.Action;
import io.inversion.cloud.model.ApiException;
import io.inversion.cloud.model.JSArray;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.model.SC;

/**
 * 
 * {
 *   meta: {},
 *   data: [
 *           {
 *              method: '',
 *              url:    '',
 *              body:   ''
 *            },
 *            ...
 *          ]
 * }
 *
 */
public class BatchAction<T extends BatchAction> extends Action<T>
{

   @Override
   public void run(Request req, Response res) throws Exception
   {
      try
      {
         JSArray arr = (JSArray) req.getJson().getArray("data");
         for (int i = 0; i < arr.length(); i++)
         {
            JSNode json = arr.getObject(i);
            
            //TODO use streaming parsers to avoid extra encoding/decoding of the json bodies
            Response batchResponse = req.getEngine().service(json.getString("method"), json.getString("url"), json.getString("body"));
            if(batchResponse.getStatusCode() > 299)
            {
               res.withStatus(batchResponse.getStatus());
               res.withOutput(batchResponse.getOutput());
               break;
            }
         }
      }
      catch (ClassCastException ex)
      {
         throw new ApiException(SC.SC_400_BAD_REQUEST, "Batch requests must be of the form { meta: '', data: [{method: '', url: '', body: ''},{method: '', url: '', body: ''}...]}");
      }

   }

}
