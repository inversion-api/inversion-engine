package io.rocketpartners.cloud.api.handler.util;

import io.rocketpartners.rest.JSArray;
import io.rocketpartners.rest.JSObject;
import io.rocketpartners.cloud.api.Action;
import io.rocketpartners.cloud.api.Api;
import io.rocketpartners.cloud.api.ApiException;
import io.rocketpartners.cloud.api.Chain;
import io.rocketpartners.cloud.api.Endpoint;
import io.rocketpartners.cloud.api.Handler;
import io.rocketpartners.cloud.api.Request;
import io.rocketpartners.cloud.api.Response;
import io.rocketpartners.cloud.api.SC;
import io.rocketpartners.cloud.api.service.Service;

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
