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
   /**
    * Used when "appendTenantIdToPk" is enabled for a table
    * The default value for this field is "::" so the primary key will look like this..
    * {tenant_id}::{pk_value}
    * 
    * Example:  1::4045551212
    */
   protected String tenantIdDelimiter = "::";

   public Collection findCollectionOrThrow404(Api api, Chain chain, Request req) throws Exception
   {
      Collection collection = api.getCollection(req.getCollectionKey());

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

   protected boolean isAppendTenantIdToPk(Chain chain, String collectionName)
   {
      return chain.getConfigSet("appendTenantIdToPk").contains(collectionName);
   }

   String addTenantIdToKey(Object tenantIdOrCode, String key)
   {
      return tenantIdOrCode + tenantIdDelimiter + key;
   }

   String removeTenantIdFromKey(Object tenantIdOrCode, String key)
   {
      if (key != null)
      {
         int preLength = (tenantIdOrCode + tenantIdDelimiter).length();
         return key.substring(preLength);
      }
      return key;
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

   public void setTenantIdDelimiter(String tenantIdDelimiter)
   {
      this.tenantIdDelimiter = tenantIdDelimiter;
   }

}
