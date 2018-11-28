/**
 * 
 */
package io.rcktapp.api.handler.dynamo;

import java.util.ArrayList;
import java.util.List;

import io.forty11.j.J;
import io.rcktapp.api.Api;
import io.rcktapp.api.ApiException;
import io.rcktapp.api.Chain;
import io.rcktapp.api.Collection;
import io.rcktapp.api.Handler;
import io.rcktapp.api.Request;
import io.rcktapp.api.SC;

/**
 * @author tc-rocket
 *
 */
public abstract class DynamoDbHandler implements Handler
{

   //   public DynamoDb findDbOrThrow404(Api api, Chain chain, Request req) throws Exception
   //   {
   //      String dbName = (String) chain.get("db");
   //
   //      DynamoDb db = null;
   //      if (!J.empty(dbName))
   //      {
   //         db = (DynamoDb) api.getDb(dbName);
   //      }
   //      else
   //      {
   //         db = (DynamoDb) api.findDb(req.getCollectionKey());
   //         if (db == null)
   //         {
   //            db = (DynamoDb) api.findDb(req.getEntityKey());
   //         }
   //      }
   //
   //      if (db == null)
   //      {
   //         throw new ApiException(SC.SC_404_NOT_FOUND, "Unable to map request to a dynamodb table. Please check your endpoint.");
   //      }
   //
   //      return db;
   //
   //   }

   public Collection findCollectionOrThrow404(Api api, Chain chain, Request req) throws Exception
   {
      Collection collection = api.getCollection(req.getCollectionKey());
      if (collection == null)
      {
         collection = (Collection) api.getCollection(req.getEntityKey());
      }

      if (collection == null)
      {
         throw new ApiException(SC.SC_404_NOT_FOUND, "A dynamo table is not configured for this collection key, please edit your query or your config and try again.");
      }

      if (!(collection.getEntity().getTable().getDb() instanceof DynamoDb))
      {
         throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Bad server configuration. The endpoint is hitting the dynamo handler, but this collection is not related to a dynamodb");
      }

      return collection;

   }
   
   protected List splitToList(String csv)
   {
      List<String> l = new ArrayList<>();
      if (csv != null)
      {
         String[] arr = csv.split(",");
         for (String e : arr)
         {
            e = e.trim();
            if (!J.empty(e))
               l.add(e);
         }
      }
      return l;
   }

}
