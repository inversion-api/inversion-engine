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
package io.inversion.cloud.action.cosmosdb;

import java.util.List;
import java.util.Map;

import com.microsoft.azure.documentdb.ConnectionPolicy;
import com.microsoft.azure.documentdb.ConsistencyLevel;
import com.microsoft.azure.documentdb.Document;
import com.microsoft.azure.documentdb.DocumentClient;
import com.microsoft.azure.documentdb.RequestOptions;
import com.microsoft.azure.documentdb.ResourceResponse;

import io.inversion.cloud.model.ApiException;
import io.inversion.cloud.model.Column;
import io.inversion.cloud.model.Db;
import io.inversion.cloud.model.Index;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Results;
import io.inversion.cloud.model.SC;
import io.inversion.cloud.model.Table;
import io.inversion.cloud.rql.Term;
import io.inversion.cloud.utils.Rows.Row;
import io.inversion.cloud.utils.Utils;

public class CosmosDocumentDb extends Db<CosmosDocumentDb>
{
   protected String                   uri            = null;
   protected String                   db             = "";
   protected String                   key            = null;

   transient protected DocumentClient documentClient = null;

   public CosmosDocumentDb()
   {
      this.withType("cosmosdb");
   }

   public CosmosDocumentDb(String name)
   {
      this();
      withName(name);
   }

   public String getCollectionUri(Table table)
   {
      String documentUri = "/dbs/" + db + "/colls/" + table.getName();
      return documentUri;
   }
   
   /**
    * Finds the entity keys on the other side of the relationship
    * @param relationship
    * @param sourceEntityKeys
    * @return Map<sourceEntityKey, relatedEntityKey>
    * @throws Exception
    */
   @Override
   public Results<Row> select(Table table, List<Term> columnMappedTerms) throws Exception
   {
      CosmosDocumentDb db = null;
      if (table == null)
      {
         db = this;
      }
      else
      {
         db = (CosmosDocumentDb) table.getDb();
      }

      CosmosSqlQuery query = new CosmosSqlQuery(this, table, columnMappedTerms);

      return query.doSelect();
   }

   public void delete(Table table, Index index, List<Map<String, Object>> columnMappedIndexValues) throws Exception
   {
      throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Unsupported Operation.  Implement " + getClass().getName() + ".delete()");
   }

   public void delete(Table table, String entityKey) throws Exception
   {
      //-- https://docs.microsoft.com/en-us/rest/api/cosmos-db/cosmosdb-resource-uri-syntax-for-rest
      //-- https://{databaseaccount}.documents.azure.com/dbs/{db}/colls/{coll}/docs/{doc}

      String documentUri = "/dbs/" + db + "/colls/" + table.getName() + "/docs/" + entityKey;

      ResourceResponse<Document> response = getDocumentClient().deleteDocument(documentUri, new RequestOptions());

      int statusCode = response.getStatusCode();
      if (statusCode > 200)
      {
         throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Unexpected http status code returned from database: '" + statusCode + "'");
      }
   }

   public String upsert(Table table, Map<String, Object> columnMappedTermsRow) throws Exception
   {
      JSNode doc = new JSNode(columnMappedTermsRow);

      //-- this simply makes the pk fields appear at the top of the inserted document
      Index pk = table.getPrimaryIndex();
      if (pk != null)
      {
         for (int i = pk.size() - 1; i >= 0; i--)
         {
            Column key = pk.getColumn(i);
            Object val = doc.remove(key.getName());
            if (val != null)
            {
               doc.putFirst(key.getName(), val);
            }
         }
      }

      //-- https://docs.microsoft.com/en-us/rest/api/cosmos-db/cosmosdb-resource-uri-syntax-for-rest
      String cosmosCollectionUri = "/dbs/" + db + "/colls/" + table.getName();
      Document document = new Document(doc.toString());

      ResourceResponse<Document> response = getDocumentClient().upsertDocument(cosmosCollectionUri, document, new RequestOptions(), true);

      int statusCode = response.getStatusCode();
      if (statusCode > 200)
      {
         throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Unexpected http status code returned from database: '" + statusCode + "'");
      }

      String id = response.getResource().getId();
      return id;
   }

   public synchronized CosmosDocumentDb withDocumentClient(DocumentClient documentClient)
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
               this.documentClient = buildDocumentClient(name + ".", uri, key);
            }
         }
      }

      return documentClient;
   }

   public static DocumentClient buildDocumentClient(String prefix, String uri, String key)
   {
      uri = Utils.findSysEnvPropStr(prefix + ".uri", uri);
      key = Utils.findSysEnvPropStr(prefix + ".key", key);

      return new DocumentClient(uri, key, ConnectionPolicy.GetDefault(), ConsistencyLevel.Session);
   }

   public String getUri()
   {
      return uri;
   }

   public CosmosDocumentDb withUri(String uri)
   {
      this.uri = uri;
      return this;
   }

   public String getDb()
   {
      return db;
   }

   public CosmosDocumentDb withDb(String db)
   {
      this.db = db;
      return this;
   }

   public String getKey()
   {
      return key;
   }

   public CosmosDocumentDb withKey(String key)
   {
      this.key = key;
      return this;
   }

}
