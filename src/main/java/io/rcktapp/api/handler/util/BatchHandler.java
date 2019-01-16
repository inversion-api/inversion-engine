package io.rcktapp.api.handler.util;

import io.forty11.web.js.JSArray;
import io.forty11.web.js.JSObject;
import io.rcktapp.api.Action;
import io.rcktapp.api.Api;
import io.rcktapp.api.ApiException;
import io.rcktapp.api.Chain;
import io.rcktapp.api.Endpoint;
import io.rcktapp.api.Handler;
import io.rcktapp.api.Request;
import io.rcktapp.api.Response;
import io.rcktapp.api.SC;
import io.rcktapp.api.service.Service;

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
