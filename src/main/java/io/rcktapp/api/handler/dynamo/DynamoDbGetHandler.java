/**
 * 
 */
package io.rcktapp.api.handler.dynamo;

import io.rcktapp.api.Action;
import io.rcktapp.api.Api;
import io.rcktapp.api.Chain;
import io.rcktapp.api.Collection;
import io.rcktapp.api.Endpoint;
import io.rcktapp.api.Request;
import io.rcktapp.api.Response;
import io.rcktapp.api.Table;
import io.rcktapp.api.service.Service;

/**
 * @author tc-rocket
 *
 */
public class DynamoDbGetHandler extends DynamoDbHandler
{

   @Override
   public void service(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res) throws Exception
   {
      Collection collection = findCollectionOrThrow404(api, chain, req);
      Table table = collection.getEntity().getTable();
      String pk = DynamoDb.findPartitionKeyName(table);
      String sk = DynamoDb.findSortKeyName(table);

      if (chain.getRequest().isDebug())
      {
         res.debug("Dynamo Table: " + table.getName() + ", PK: " + pk + ", SK: " + sk);
      }

   }

}
