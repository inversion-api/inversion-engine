/*
 * Copyright (c) 2015-2020 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.cosmosdb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.microsoft.azure.documentdb.ConnectionPolicy;
import com.microsoft.azure.documentdb.ConsistencyLevel;
import com.microsoft.azure.documentdb.Document;
import com.microsoft.azure.documentdb.DocumentClient;
import com.microsoft.azure.documentdb.DocumentClientException;
import com.microsoft.azure.documentdb.PartitionKey;
import com.microsoft.azure.documentdb.RequestOptions;
import com.microsoft.azure.documentdb.ResourceResponse;

import io.inversion.ApiException;
import io.inversion.Chain;
import io.inversion.Collection;
import io.inversion.Db;
import io.inversion.Results;
import io.inversion.rql.Term;
import io.inversion.utils.JSNode;
import io.inversion.utils.Utils;

public class CosmosDb extends Db<CosmosDb>
{
   protected String                   uri                        = null;
   protected String                   db                         = "";
   protected String                   key                        = null;

   boolean                            allowCrossPartitionQueries = false;

   transient protected DocumentClient documentClient             = null;

   public CosmosDb()
   {
      this.withType("cosmosdb");
   }

   public CosmosDb(String name)
   {
      this();
      withName(name);
   }

   /**
    * Finds the resource keys on the other side of the relationship
    * @param relationship
    * @param sourceResourceKeys
    * @return Map<sourceResourceKey, relatedResourceKey>
    * @throws ApiException
    */
   @Override
   public Results doSelect(Collection table, List<Term> columnMappedTerms) throws ApiException
   {
      CosmosSqlQuery query = new CosmosSqlQuery(this, table, columnMappedTerms);
      return query.doSelect();
   }

   @Override
   public List<String> doUpsert(Collection table, List<Map<String, Object>> rows) throws ApiException
   {
      List keys = new ArrayList();
      for (Map<String, Object> row : rows)
      {
         keys.add(upsertRow(table, row));
      }
      return keys;
   }

   public String upsertRow(Collection table, Map<String, Object> columnMappedTermsRow) throws ApiException
   {
      try
      {
         JSNode doc = new JSNode(columnMappedTermsRow);

         String id = doc.getString("id");
         if (id == null)
         {
            id = table.encodeResourceKey(columnMappedTermsRow);
            if (id == null)
               ApiException.throw400BadRequest("Your record does not contain the required key fields.");
            doc.putFirst("id", id);
         }

         //-- the only way to achieve a PATCH is to query for the document first.
         Results existing = doSelect(table, Arrays.asList(Term.term(null, "_key", table.getPrimaryIndex().getName(), id)));
         if (existing.size() == 1)
         {
            Map<String, Object> row = existing.getRow(0);
            for (String key : row.keySet())
            {
               if (!doc.containsKey(key))
                  doc.put(key, row.get(key));
            }
         }

         //-- https://docs.microsoft.com/en-us/rest/api/cosmos-db/cosmosdb-resource-uri-syntax-for-rest
         String cosmosCollectionUri = "/dbs/" + db + "/colls/" + table.getTableName();

         String json = doc.toString();
         Document document = new Document(json);

         String debug = "CosmosDb: Insert " + json;
         Chain.debug(debug);

         ResourceResponse<Document> response = getDocumentClient().upsertDocument(cosmosCollectionUri, document, new RequestOptions(), true);

         int statusCode = response.getStatusCode();
         if (statusCode > 299)
         {
            ApiException.throw400BadRequest("Unexpected http status code returned from database: '{}'", statusCode);
         }

         String returnedId = response.getResource().getId();
         if (!Utils.equal(id, returnedId))
            ApiException.throw500InternalServerError("The supplied 'id' field does not match the returned 'id' field: '{}' vs. '{}'", id, returnedId);

         return id;
      }
      catch (Exception ex)
      {
         ApiException.throw500InternalServerError(ex);
      }
      return null;
   }

   @Override
   public void delete(Collection table, List<Map<String, Object>> indexValues) throws ApiException
   {
      for (Map<String, Object> row : indexValues)
      {
         deleteRow(table, row);
      }
   }

   /**
    * Deletes a single specific resource.
    * 
    * Rest url format for Cosmos deletions -  https://{databaseaccount}.documents.azure.com/dbs/{db}/colls/{coll}/docs/{doc}
    * 
    * @see https://docs.microsoft.com/en-us/rest/api/cosmos-db/cosmosdb-resource-uri-syntax-for-rest
    * @see 
    * 
    * @param table
    * @param indexValues
    * @throws ApiException
    */
   protected void deleteRow(Collection table, Map<String, Object> indexValues) throws ApiException
   {
      Object id = table.encodeResourceKey(indexValues);
      Object partitionKeyValue = indexValues.get(table.getIndex("PartitionKey").getProperty(0).getColumnName());
      String documentUri = "/dbs/" + db + "/colls/" + table.getTableName() + "/docs/" + id;

      RequestOptions options = new RequestOptions();
      options.setPartitionKey(new PartitionKey(partitionKeyValue));

      ResourceResponse<Document> response = null;
      try
      {
         Chain.debug("CosmosDb: Delete documentUri=" + documentUri + "partitionKeyValue=" + partitionKeyValue);
         response = getDocumentClient().deleteDocument(documentUri, options);

         int statusCode = response.getStatusCode();
         if (statusCode >= 400)
         {
            ApiException.throw500InternalServerError("Unexpected http status code returned from database: {}", statusCode);
         }
      }
      catch (DocumentClientException ex)
      {
         ex.printStackTrace();
         int statusCode = ex.getStatusCode();
         if (statusCode == 404)
         {
            //ignore attempts to delete things that don't exist
         }
         else
         {
            ApiException.throw500InternalServerError(ex);
         }
      }
   }

   protected String getCollectionUri(Collection table)
   {
      String documentUri = "/dbs/" + db + "/colls/" + table.getTableName();
      return documentUri;
   }

   public String getUri()
   {
      return uri;
   }

   public CosmosDb withUri(String uri)
   {
      this.uri = uri;
      return this;
   }

   public String getDb()
   {
      return db;
   }

   public CosmosDb withDb(String db)
   {
      this.db = db;
      return this;
   }

   public String getKey()
   {
      return key;
   }

   public CosmosDb withKey(String key)
   {
      this.key = key;
      return this;
   }

   public boolean isAllowCrossPartitionQueries()
   {
      return allowCrossPartitionQueries;
   }

   public CosmosDb withAllowCrossPartitionQueries(boolean allowCrossPartitionQueries)
   {
      this.allowCrossPartitionQueries = allowCrossPartitionQueries;
      return this;
   }

   public synchronized CosmosDb withDocumentClient(DocumentClient documentClient)
   {
      this.documentClient = documentClient;
      return this;
   }

   public DocumentClient getDocumentClient()
   {
      if (this.documentClient == null)
      {
         synchronized (this)
         {
            if (this.documentClient == null)
            {
               this.documentClient = buildDocumentClient(name, uri, key);
            }
         }
      }

      return documentClient;
   }

   public static DocumentClient buildDocumentClient(String prefix, String uri, String key)
   {
      uri = Utils.getSysEnvPropStr(prefix + ".uri", uri);
      key = Utils.getSysEnvPropStr(prefix + ".key", key);

      if (Utils.empty(uri) || Utils.empty(key))
      {
         String error = "";
         error += "Unable to connect to Cosmos DB because conf values for '" + prefix + ".uri' or '" + prefix + ".key' can not be found. ";
         error += "If this is a development environment, you should probably add these key/value pairs to a '.env' properties file in your working directory. ";
         error += "If this is a production deployment, you should probably set these as environment variables on your container.";
         error += "You could call CosmosDocumentDb.withUri() and CosmosDocumentDb.withKey() directly in your code but compiling these ";
         error += "values into your code is strongly discouraged as a poor security practice.";

         ApiException.throw500InternalServerError(error);
      }

      DocumentClient client = new DocumentClient(uri, key, ConnectionPolicy.GetDefault(), ConsistencyLevel.Session);
      return client;
   }

}
