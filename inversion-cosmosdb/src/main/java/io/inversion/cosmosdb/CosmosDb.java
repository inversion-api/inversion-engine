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

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.PartitionKey;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.inversion.*;
import io.inversion.rql.Term;
import io.inversion.utils.JSNode;
import io.inversion.utils.Rows;
import io.inversion.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CosmosDb extends Db<CosmosDb> {

    public static final String INDEX_TYPE_PARTITION_KEY = "PartitionKey";

    protected           String         uri            = null;
    protected           String         db             = "";
    protected           String         key            = null;
    transient protected CosmosClient cosmosClient = null;
    boolean allowCrossPartitionQueries = false;

    public CosmosDb() {
        this.withType("cosmosdb");
    }

    public CosmosDb(String name) {
        this();
        withName(name);
    }

    public static CosmosClient buildCosmosClient(String uri, String key) {
        if (Utils.empty(uri) || Utils.empty(key)) {
            String error = "";
            error += "Unable to connect to Cosmos DB because conf values for 'uri' or 'key' can not be found. ";
            error += "If this is a development environment, you should probably add these key/value pairs to a '.env' properties file in your working directory. ";
            error += "If this is a production deployment, you should probably set these as environment variables on your container.";
            error += "You could call CosmosDocumentDb.withUri() and CosmosDocumentDb.withKey() directly in your code but compiling these ";
            error += "values into your code is strongly discouraged as a poor security practice.";

            throw ApiException.new500InternalServerError(error);
        }

        return new CosmosClientBuilder()
                .endpoint(uri)
                .key(key)
                .contentResponseOnWriteEnabled(true)
                .consistencyLevel(ConsistencyLevel.SESSION)
                .buildClient();
    }

    /**
     * Finds the resource keys on the other side of the relationship
     *
     * @param collection        the collection to query
     * @param columnMappedTerms the query terms
     * @return Map key=sourceResourceKey, value=relatedResourceKey
     * @throws ApiException if selection fails for any reason
     */
    @Override
    public Results doSelect(Collection collection, List<Term> columnMappedTerms) throws ApiException {

        Index partitionIdx = collection.getIndexByType(INDEX_TYPE_PARTITION_KEY);
        if (partitionIdx != null) {

            Map<String, Object> values = new HashMap<>();
            for (Property prop : partitionIdx.getProperties()) {
                String colName = prop.getColumnName();
                for (Term term : columnMappedTerms) {
                    if (term.hasChildLeafToken("eq") && colName.equals(term.getToken(0))) {
                        values.put(colName, term.getToken(1));
                        break;
                    }
                }
            }

            for (Term term : columnMappedTerms) {
                if (term.hasToken("_key")) {
                    String indexName = term.getToken(0);
                    Index idx = collection.getIndex(indexName);
                    Rows.Row key = collection.decodeResourceKey(idx, term.getToken(1));
//                    Rows.Row key = collection.decodeResourceKey(term.getToken(0));
                    for (Property prop : partitionIdx.getProperties()) {
                        String colName = prop.getColumnName();
                        if (key.containsKey(colName))
                            values.put(colName, key.get(colName));
                    }
                }
            }


            //if the query supplied the parts necessary to construct the
            if (values.size() == partitionIdx.size()) {

                //-- remove any explicit partitionKey query params supplied by the users
                columnMappedTerms.removeIf(term -> term.hasToken("eq") && partitionIdx.getName().equals(term.getToken(0)));

                String partitionKey = io.inversion.Collection.encodeResourceKey(values, partitionIdx);
                columnMappedTerms.add(Term.term(null, "eq", partitionIdx.getName(), partitionKey));
            }
        }


        CosmosSqlQuery query = new CosmosSqlQuery(this, collection, columnMappedTerms);
        return query.doSelect();
    }

    @Override
    public List<String> doUpsert(Collection table, List<Map<String, Object>> rows) throws ApiException {
        List<String> keys = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            keys.add(upsertRow(table, row));
        }
        return keys;
    }

    void normalizePartitionKey(Collection collection, Map<String, Object> row) {
        //-- makes sure the partition key is set correctly on the document if there is one.
        Index partitionIdx = collection.getIndexByType(INDEX_TYPE_PARTITION_KEY);
        if (partitionIdx != null) {
            String partitionKey = io.inversion.Collection.encodeResourceKey(row, partitionIdx);
            if (partitionKey == null)
                throw ApiException.new400BadRequest("Unable to determine the CosmosDb partition key from the supplied fields");

            row.put(partitionIdx.getName(), partitionKey);
        }
    }

    public String upsertRow(Collection collection, Map<String, Object> row) throws ApiException {
        try {

            normalizePartitionKey(collection, row);

            JSNode doc = new JSNode(row);
            String id  = doc.getString("id");
            if (id == null) {
                id = collection.encodeResourceKey(row);
                if (id == null)
                {
                    // this is the first potential problem, where we couldn't get the primary index for some reason
                    Index index = collection.getPrimaryIndex();
                    if(index == null) {
                        log.warn("INVERSION UNABLE TO DETERMINE PRIMARY INDEX");
                    }
                    else {
                        // this is the second potential problem, where we found the index but couldn't construct it
                        // either it's the wrong index being chosen, or the row, by the time it gets here, doesn't
                        // have the data required to construct the thing
                        log.info("INVERSION USING INDEX [{}]", index);
                        if(row == null)
                            log.warn("BUT THE ROW IS NULL!");
                        else if(row.size() == 0)
                            log.warn("BUT THE ROW IS EMPTY!");
                        else
                            index.getColumnNames().forEach(colName -> {
                                if(Utils.empty(row.get(colName)))
                                    log.warn("INVERSION ROW MAP KEY [{}] is EMPTY", colName);
                            });
                    }
                    throw ApiException.new400BadRequest("Your record does not contain the required key fields.");
                }
                doc.putFirst("id", id);
            }

            //-- the only way to achieve a PATCH is to query for the document first.
            Results existing = doSelect(collection, Utils.asList(Term.term(null, "_key", collection.getPrimaryIndex().getName(), id)));
            if (existing.size() == 1) {
                Map<String, Object> existingRow = existing.getRow(0);
                for (String key : existingRow.keySet()) {
                    if (!doc.containsKey(key))
                        doc.put(key, existingRow.get(key));
                }
            }

            String json = doc.toString();
            JsonNode itemJsonObject = new ObjectMapper().readTree(json);

            String debug = "CosmosDb: Insert " + json;
            Chain.debug(debug);

            CosmosItemResponse<JsonNode> response = getCosmosClient().getDatabase(db).getContainer(collection.getTableName())
                    .upsertItem(itemJsonObject, new CosmosItemRequestOptions());

            int statusCode = response.getStatusCode();
            if (statusCode > 299) {
                throw ApiException.new400BadRequest("Unexpected http status code returned from database: '{}'", statusCode);
            }

            String returnedId = response.getItem().get("id").asText();
            if (!Utils.equal(id, returnedId))
                throw ApiException.new500InternalServerError("The supplied 'id' field does not match the returned 'id' field: '{}' vs. '{}'", id, returnedId);

            return id;
        } catch (Exception ex) {
            throw ApiException.new500InternalServerError(ex);
        }
    }

    @Override
    public void delete(Collection table, List<Map<String, Object>> indexValues) throws ApiException {
        for (Map<String, Object> row : indexValues) {
            deleteRow(table, row);
        }
    }

    /**
     * Deletes a single specific resource.
     * <p>
     * Rest url format for Cosmos deletions -  https://{databaseaccount}.documents.azure.com/dbs/{db}/colls/{coll}/docs/{doc}
     *
     * @param collection  the collection to delete
     * @param indexValues identifiers for the documents to delete
     * @see <a href="https://docs.microsoft.com/en-us/rest/api/cosmos-db/cosmosdb-resource-uri-syntax-for-rest">CosmosDb Resource URI Syntax</a>
     */
    protected void deleteRow(Collection collection, Map<String, Object> indexValues) throws ApiException {

        normalizePartitionKey(collection, indexValues);

        Object id                = collection.encodeResourceKey(indexValues);
        Object partitionKeyValue = indexValues.get(collection.getIndex(CosmosDb.INDEX_TYPE_PARTITION_KEY).getProperty(0).getColumnName());
        String documentUri       = "/dbs/" + db + "/colls/" + collection.getTableName() + "/docs/" + id;

        CosmosItemResponse<Object> response;
        try {
            Chain.debug("CosmosDb: Delete documentUri=" + documentUri + "partitionKeyValue=" + partitionKeyValue);
            response = getCosmosClient().getDatabase(db).getContainer(collection.getTableName())
                    .deleteItem(String.valueOf(id), new PartitionKey(partitionKeyValue), new CosmosItemRequestOptions());

            int statusCode = response.getStatusCode();
            if (statusCode >= 400) {
                throw ApiException.new500InternalServerError("Unexpected http status code returned from database: {}", statusCode);
            }
        } catch (CosmosException ex) {
            ex.printStackTrace();
            int statusCode = ex.getStatusCode();
            if (statusCode == 404) {
                //ignore attempts to delete things that don't exist
            } else {
                throw ApiException.new500InternalServerError(ex);
            }
        }
    }

    protected String getCollectionUri(Collection table) {
        String documentUri = "/dbs/" + db + "/colls/" + table.getTableName();
        return documentUri;
    }

    public String getUri() {
        return uri;
    }

    public CosmosDb withUri(String uri) {
        this.uri = uri;
        return this;
    }

    public String getDb() {
        return db;
    }

    public CosmosDb withDb(String db) {
        this.db = db;
        return this;
    }

    public String getKey() {
        return key;
    }

    public CosmosDb withKey(String key) {
        this.key = key;
        return this;
    }

    public boolean isAllowCrossPartitionQueries() {
        return allowCrossPartitionQueries;
    }

    public CosmosDb withAllowCrossPartitionQueries(boolean allowCrossPartitionQueries) {
        this.allowCrossPartitionQueries = allowCrossPartitionQueries;
        return this;
    }

    public synchronized CosmosDb withCosmosClient(CosmosClient cosmosClient) {
        this.cosmosClient = cosmosClient;
        return this;
    }

    public CosmosClient getCosmosClient() {
        if (this.cosmosClient == null) {
            synchronized (this) {
                if (this.cosmosClient == null) {
                    this.cosmosClient = buildCosmosClient(uri, key);
                }
            }
        }

        return cosmosClient;
    }

}
