package io.rocketpartners.cloud.action.rest;

import java.util.ArrayList;
import java.util.List;

import io.rocketpartners.cloud.model.Action;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.ApiException;
import io.rocketpartners.cloud.model.ArrayNode;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.model.ObjectNode;
import io.rocketpartners.cloud.model.Request;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.model.Table;
import io.rocketpartners.cloud.model.Url;
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

      int count = Utils.empty(entityKey) ? 0 : 1;
      count += Utils.empty(req.getQuery()) ? 0 : 1;
      count += json == null ? 0 : 1;

      if (count != 1)
         throw new ApiException(SC.SC_400_BAD_REQUEST, "DELETE expects an entity url, OR a query string OR a JSON array of entity urls, but only one at a time.");

      if (!Utils.empty(subcollectionKey))
         throw new ApiException(SC.SC_400_BAD_REQUEST, "A subcollection key is not valid for a DELETE request");

      List<String> toDelete = new ArrayList();

      if (req.getJson() != null)
      {
         if (!(json instanceof ArrayNode))
         {
            throw new ApiException(SC.SC_400_BAD_REQUEST, "The JSON body to a DELETE must be an array that contains string urls.");
         }

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
            toDelete.add((String) o);
         }
      }
      else if (entityKey != null)
      {
         toDelete.add(req.getUrl().toString());
      }

      for (String url : toDelete)
      {
         //don't do "next" pagination because we are deleting and the offsets would be wrong
         //the 1000 needs to be replaced with some other configurable 'sanity' guard
         for (int i = 0; i < 1000; i++)
         {
            String next = url;
            if (next.indexOf("?") > 0)
               next += "&includes=href";
            else
               next += "?includes=href";

            Response getRes = service.get(next);
            if (getRes.hasStatus(404) || getRes.data().size() == 0)
            {
               break;//everything has been deleted
            }
            else if (!getRes.isSuccess())
            {
               throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Error retrieving entity keys to delete: " + getRes.getErrorContent());
            }

            Table table = req.getCollection().getEntity().getTable();

            ArrayNode deleteArr = getRes.data();
            List<String> entityKeys = new ArrayList();

            for (int j = 0; j < deleteArr.length(); j++)
            {
               ObjectNode deleteObj = deleteArr.getObject(j);
               Url u = new Url(deleteObj.getString("href"));
               List<String> path = Utils.explode("/", u.getPath());
               String key = path.get(path.size() - 1);

               entityKeys.add(key);
            }
            req.getCollection().getDb().delete(table, entityKeys);
         }
      }
   }

}
