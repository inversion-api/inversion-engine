package io.rocketpartners.cloud.action.rest;

import java.util.ArrayList;
import java.util.List;

import io.rocketpartners.cloud.model.Action;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.ApiException;
import io.rocketpartners.cloud.model.ArrayNode;
import io.rocketpartners.cloud.model.Collection;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.model.ObjectNode;
import io.rocketpartners.cloud.model.Request;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.Utils;

public class RestDeleteAction extends Action<RestDeleteAction>
{
   @Override
   public void run(Service service, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
   {

      String entityKey = req.getEntityKey();
      String subcollectionKey = req.getSubCollectionKey();
      ObjectNode json = req.getJson();

      if (!Utils.empty(req.getQuery()))
         throw new ApiException(SC.SC_400_BAD_REQUEST, "Query strings are not supported for delete operations at this time.");

      if ((Utils.empty(entityKey) && json == null) || (!Utils.empty(entityKey) && json != null))
         throw new ApiException(SC.SC_400_BAD_REQUEST, "DELETE expects an entity url or a JSON array of entity urls but not both at the same time");

      if (!Utils.empty(subcollectionKey))
         throw new ApiException(SC.SC_400_BAD_REQUEST, "A subcollection key is not valid for a DELETE request");

      if (req.getJson() != null)
      {
         if (!(json instanceof ArrayNode))
         {
            throw new ApiException(SC.SC_400_BAD_REQUEST, "The JSON body to a DELETE must be an array that contains string urls.");
         }

         List<String> urls = new ArrayList();

         for (Object o : (ArrayNode) json)
         {
            if (!(o instanceof String))
               throw new ApiException(SC.SC_400_BAD_REQUEST, "The JSON body to a DELETE must be an array that contains string urls.");

            String url = (String) o;

            String path = req.getUrl().toString();
            if (path.indexOf("?") > 0)
            {
               path = path.substring(0, path.indexOf("?") - 1);
            }

            if (!url.toLowerCase().startsWith(path.toLowerCase()))
            {
               throw new ApiException(SC.SC_400_BAD_REQUEST, "All delete request must be for the collection in the original request: '" + path + "'");
            }
            urls.add((String) o);
         }

         for (String url : urls)
         {
            Response r = service.delete(url);
            if (r.getStatusCode() != 204)
            {
               throw new ApiException("Nested delete url: " + url + " failed!");
            }
         }
      }
      else
      {
         Collection col = req.getCollection();
         col.getDb().delete(req, col.getTable(), req.getEntityKey());

         res.withStatus(SC.SC_204_NO_CONTENT);
      }
   }

}
