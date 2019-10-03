package io.inversion.cloud.action.misc;

import io.inversion.cloud.model.Action;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.ApiException;
import io.inversion.cloud.model.JSArray;
import io.inversion.cloud.model.Endpoint;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.model.SC;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.service.Engine;

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
   public void run(Engine engine, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
   {
      try
      {
         JSArray arr = (JSArray) req.getJson().getArray("data");
         for (int i = 0; i < arr.length(); i++)
         {
            JSNode json = arr.getObject(i);
            
            //TODO use streaming parsers to avoid extra encoding/decoding of the json bodies
            Response batchResponse = engine.service(json.getString("method"), json.getString("url"), json.getString("body"));
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
