package io.rocketpartners.cloud.handler.misc;

import io.rocketpartners.cloud.model.Action;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.ApiException;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.service.Handler;
import io.rocketpartners.cloud.service.Request;
import io.rocketpartners.cloud.service.Response;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.utils.JSArray;
import io.rocketpartners.utils.JSObject;

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
public class BatchHandler implements Handler
{

   @Override
   public void service(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res) throws Exception
   {
      try
      {
         JSArray arr = (JSArray) req.getJson().getArray("data");
         for (int i = 0; i < arr.length(); i++)
         {
            JSObject json = arr.getObject(i);
            
            //TODO use streaming parsers to avoid extra encoding/decoding of the json bodies
            Response batchResponse = service.include(chain, json.getString("method"), json.getString("url"), json.getString("body"));
            if(batchResponse.getStatusCode() > 299)
            {
               res.setStatus(batchResponse.getStatus());
               res.setOutput(batchResponse.getOutput());
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
