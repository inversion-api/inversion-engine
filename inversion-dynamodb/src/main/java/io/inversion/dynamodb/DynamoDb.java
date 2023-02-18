/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
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
package io.inversion.dynamodb;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.dynamodbv2.model.Projection;
import io.inversion.Collection;
import io.inversion.*;
import io.inversion.rql.Term;
import io.inversion.utils.Utils;

import java.util.*;
import java.util.concurrent.Callable;

public class DynamoDb<T extends DynamoDb> extends Db<T> {

    public static final String PRIMARY_INDEX_NAME = "Primary Index";

    public static final String PRIMARY_INDEX_TYPE          = "primary";
    public static final String LOCAL_SECONDARY_INDEX_TYPE  = "localsecondary";
    public static final String GLOBAL_SECONDARY_INDEX_TYPE = "globalsecondary";
    protected final     int    batchMax                    = 20;
    protected           String awsAccessKey                = null;
    protected           String awsSecretKey                = null;
    protected           String awsRegion                   = "us-east-1";
    protected           String awsEndpoint                 = null;

    transient protected AmazonDynamoDB dynamoClient = null;

    public DynamoDb() {
        this.withType("dynamodb");
    }

    public DynamoDb(String name, String includeTables) {
        this();
        withName(name);
        withIncludeTables(includeTables);
    }

    public static Index findIndexByName(Collection coll, String name) {
        if (coll != null && coll.getIndexes() != null) {
            for (Index index : coll.getIndexes()) {
                if (index.getName().equals(name)) {
                    return index;
                }
            }
        }
        return null;
    }

    /*
     * These match the string that dynamo uses for these types.
     * https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBMapper.DataTypes.html
     */
    protected static String getTypeStringFromObject(Object obj) {
        if (obj instanceof Number) {
            return "N";
        } else if (obj instanceof Boolean) {
            return "BOOL";
        } else {
            return "S";
        }
    }

    public static AmazonDynamoDB buildDynamoClient(String awsRegion, String awsAccessKey, String awsSecretKey, String awsEndpoint) {
        AmazonDynamoDBClientBuilder builder = AmazonDynamoDBClientBuilder.standard();
        if (!Utils.empty(awsRegion)) {
            if (!Utils.empty(awsEndpoint)) {
                AwsClientBuilder.EndpointConfiguration endpointConfig = new AwsClientBuilder.EndpointConfiguration(awsEndpoint, awsRegion);
                builder.withEndpointConfiguration(endpointConfig);
            } else {
                builder.withRegion(awsRegion);
            }
        }
        if (!Utils.empty(awsAccessKey) && !Utils.empty(awsSecretKey)) {
            BasicAWSCredentials creds = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
            builder.withCredentials(new AWSStaticCredentialsProvider(creds));
        }

        AmazonDynamoDB dynamoClient = builder.build();

        return dynamoClient;
    }

    @Override
    public Results doSelect(Collection table, List<Term> columnMappedTerms) throws ApiException {

        return (Results) run(() -> {
            return doSelect0(table, columnMappedTerms);
        });
    }

    public Results doSelect0(Collection table, List<Term> columnMappedTerms) throws ApiException{
        DynamoDbQuery query  = new DynamoDbQuery(this , table, columnMappedTerms).withDynamoTable(getDynamoTable(table));
        Results       result = query.doSelect();
        return result;
    }

    @Override
    public List<String> doUpsert(Collection collection, List<Map<String, Object>> rows) throws ApiException {
        List<String>   keys         = new ArrayList<>();
        AmazonDynamoDB dynamoClient = getDynamoClient();
        DynamoDB       dynamoDb     = new DynamoDB(dynamoClient);
        Table          table        = dynamoDb.getTable(collection.getTableName());

        List<UpdateItemSpec> updates = new ArrayList();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            String              key = collection.encodeKeyFromColumnNames(row);
            keys.add(key);

            UpdateItemSpec spec = buildUpdateItemSpec(collection, row);
            //TODO: wrap these in a transaction
            //-- @see https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Programming.Errors.html
            table.updateItem(spec);
        }

        return keys;
    }


    protected UpdateItemSpec buildUpdateItemSpec(Collection collection, Map<String, Object> row) {
        Index         idx      = collection.getResourceIndex();
        LinkedHashSet keyProps = new LinkedHashSet(idx.getColumnNames());

        Map<String, Object> keyMap     = new LinkedHashMap<>();
        PrimaryKey          primaryKey = new PrimaryKey();
        for (String col : idx.getColumnNames()) {
            primaryKey.addComponent(col, row.get(col));
            keyMap.put(col, row.get(col));
        }

        Map<String, String> nameMap  = new LinkedHashMap<>();
        Map<String, Object> valueMap = new LinkedHashMap<>();

        StringBuilder addExpression       = null;
        StringBuilder setExpression       = null;
        StringBuilder removeExpression    = null;
        StringBuilder conditionExpression = null;

        for (Property prop : collection.getProperties()) {
            //-- use an ADD clause on the conditionExpression to auto increment the revision on each write
            //-- the rev may not have been passed in with the request so you can't just loop over the row keys here
            if (prop.isRevisionColumn()) {
                if (addExpression == null)
                    addExpression = new StringBuilder("ADD ");
                else
                    addExpression.append(", ");

                String varName = "#var" + nameMap.size();
                nameMap.put(varName, prop.getColumnName());

                String valName = ":val" + valueMap.size();
                valueMap.put(valName, 1);

                addExpression.append(varName).append(" ").append(valName);
            } else if (prop.isTimestampColumn()) {
                //-- ensures that a timestamp property is always updated
                try {
                    long   now = System.currentTimeMillis();
                    Object str = row.get(prop.getColumnName());
                    if (str != null) {
                        long ts = Long.parseLong(str.toString());
                        if (Math.abs(ts - now) > 100)//give the caller a 100ms leeway on whatever they passed in.
                            row.put(prop.getColumnName(), now);
                    } else {
                        row.put(prop.getColumnName(), now);
                    }
                } catch (Exception ex) {
                    throw ApiException.new400BadRequest("Your {} field is setup as a timestamp field but is not a the right format.  It should be epoc time or simply left blank to have it auto updated.");
                }
            }
        }

        for (String prop : row.keySet()) {
            if (!keyProps.contains(prop)) {
                Object   value    = row.get(prop);
                Property property = collection.getPropertyByColumnName(prop);
                if (property != null && property.isRevisionColumn()) {
                    Object rev = row.get(prop);
                    if (rev != null) {
                        //-- use a condition expression to require that the passed
                        //-- in revision number matches the existing revision number
                        if (conditionExpression == null)
                            conditionExpression = new StringBuilder();
                        else
                            conditionExpression.append(" AND ");

                        String varName = "#var" + nameMap.size();
                        nameMap.put(varName, prop);

                        String valName = ":val" + valueMap.size();
                        valueMap.put(valName, value);

                        conditionExpression.append("(");
                        conditionExpression.append("attribute_not_exists(").append(varName).append(") OR ").append(varName).append(" = ").append(valName);
                        conditionExpression.append(")");
                    }
                } else if (value != null) {
                    String varName = "#var" + nameMap.size();
                    nameMap.put(varName, prop);

                    String valName = ":val" + valueMap.size();
                    valueMap.put(valName, value);

                    if (setExpression == null)
                        setExpression = new StringBuilder("SET ");
                    else
                        setExpression.append(", ");

                    setExpression.append(varName).append(" = ").append(valName);

                } else {
                    String varName = "#var" + nameMap.size();
                    nameMap.put(varName, prop);

                    if (removeExpression == null)
                        removeExpression = new StringBuilder("REMOVE ");
                    else
                        removeExpression.append(", ");

                    removeExpression.append(varName);
                }
            }
        }

        String updateExpression = "";
        if (setExpression != null)
            updateExpression = setExpression.toString();

        if (removeExpression != null) {
            //if (updateExpression != null)
            //   updateExpression += " ";

            updateExpression += " " + removeExpression.toString();
        }
        if (addExpression != null) {
            //if (updateExpression != null)
            //    updateExpression += " ";
            updateExpression += " " + addExpression;
        }
        updateExpression = updateExpression.trim();

        UpdateItemSpec update = new UpdateItemSpec();
        update.withPrimaryKey(primaryKey);

        if (!Utils.empty(updateExpression))
            update.withUpdateExpression(updateExpression);

        if (!Utils.empty(conditionExpression))
            update.withConditionExpression(conditionExpression.toString());

        if (nameMap.size() > 0)
            update.withNameMap(nameMap);

        if (valueMap.size() > 0)
            update.withValueMap(valueMap);

        //-- start explain debug
        StringBuilder debug = new StringBuilder("DynamoDb: UpdateItemSpec");
        debug.append(" key = ").append(keyMap);
        debug.append(" updateExpression=").append(updateExpression);
        if (!Utils.empty(conditionExpression))
            debug.append(" conditionExpression=").append(conditionExpression);
        debug.append(" nameMap=").append(nameMap);
        debug.append(" valueMap=").append(valueMap);
        String out = debug.toString();
        out.replace("\r\n", "\n");
        out.replace("\n", " ");
        Chain.debug(out);
        System.out.println(out);
        //-- end explain debug

        return update;

    }


    @Override
    public void doDelete(Collection table, List<Map<String, Object>> indexValues) throws ApiException {
        for (Map<String, Object> row : indexValues) {
            deleteRow(table, row);
        }
    }

    /**
     * TODO: update all calls to DynamoDb to use this method to have consistent error handling.
     *
     * @param statement
     * @return
     */
    public Object run(Callable statement) {
        try {
            return statement.call();
        } catch (ConditionalCheckFailedException ex) {
            throw new ApiException((Exception) null, Status.SC_409_CONFLICT, "A submitted resource is out of date and could not be updated.  Please refresh your copy before submitting again.");
        } catch (ItemCollectionSizeLimitExceededException ex) {
            throw new ApiException((Exception) null, Status.SC_507_INSUFFICIENT_STORAGE, "Collection size exceeded");
        } catch (LimitExceededException ex) {
            throw new ApiException((Exception) null, Status.SC_429_TOO_MANY_REQUESTS, "Too many requests.");
        } catch (ProvisionedThroughputExceededException ex) {
            throw new ApiException((Exception) null, Status.SC_429_TOO_MANY_REQUESTS, "Too many requests.");

        } catch (RequestLimitExceededException ex) {
            throw new ApiException((Exception) null, Status.SC_429_TOO_MANY_REQUESTS, "Too many requests.");
        } catch (AmazonServiceException ase) {
            String errorCode = ase.getErrorCode();
            //ex: "One or more parameter values were invalid:"
            if("ValidationException".equalsIgnoreCase(errorCode)){
                throw ApiException.new400BadRequest(ase.getMessage());
            }
            throw new ApiException((Exception)null, Status.SC_500_INTERNAL_SERVER_ERROR,
                    "Could not complete operation. Error Message: , HTTP Status: {}, AWS Error Code: {}, Error Type: {}, Request ID: {}"
                    /*ase.getMessage(),*/ /*ase.getStatusCode(), ase.getErrorCode(), ase.getErrorType(), ase.getRequestId()*/
            );

        } catch (AmazonClientException ace) {
            throw new ApiException(Status.SC_500_INTERNAL_SERVER_ERROR,
                    "Internal error occurred communicating with DynamoDB. Error Message: {}",
                    ace.getMessage());
        } catch (Exception ex) {
            throw new ApiException(ex);
        }
    }


    public void deleteRow(Collection table, Map<String, Object> row) throws ApiException {
        com.amazonaws.services.dynamodbv2.document.Table dynamo = getDynamoTable(table);

        Index pk = table.getResourceIndex();

        if (pk.size() == 1) {
            dynamo.deleteItem(pk.getProperty(0).getColumnName(), row.get(pk.getProperty(0).getColumnName()));
        } else if (pk.size() == 2) {
            dynamo.deleteItem(pk.getProperty(0).getColumnName(), row.get(pk.getProperty(0).getColumnName()), pk.getProperty(1).getColumnName(), row.get(pk.getProperty(1).getColumnName()));
        } else {
            throw ApiException.new400BadRequest("A dynamo delete must have a hash key and an optional sortKey and that is it: '{}'", row);
        }
    }

    public void configDb() throws ApiException {
        for (String tableName : includeTables.keySet()) {
            List<String> collectionNames = Utils.explode(",", includeTables.get(tableName));
            for (String collectionName : collectionNames) {
                withCollection(buildCollection(tableName, collectionName));
            }
        }
    }

    protected Collection buildCollection(String tableName, String collectionName) {
        AmazonDynamoDB dynamoClient = getDynamoClient();

        Collection coll = new Collection(collectionName);
        coll.withTableName(tableName);

        DynamoDB                                         dynamoDB         = new DynamoDB(dynamoClient);
        com.amazonaws.services.dynamodbv2.document.Table dynamoTable      = dynamoDB.getTable(tableName);
        TableDescription                                 tableDescription = dynamoTable.describe();

        for (AttributeDefinition attr : tableDescription.getAttributeDefinitions()) {
            coll.withProperty(attr.getAttributeName(), attr.getAttributeType(), true);
        }

        //      Index index = new Index(PRIMARY_INDEX_NAME, PRIMARY_INDEX_TYPE);
        //
        List<KeySchemaElement> keySchema = tableDescription.getKeySchema();
        //      for (KeySchemaElement keyInfo : keySchema)
        //      {
        //         if (keyInfo.getKeyType().equalsIgnoreCase("HASH"))
        //         {
        //            index.witHashKey(coll.getProperty(keyInfo.getAttributeName()));
        //         }
        //         else if (keyInfo.getKeyType().equalsIgnoreCase("RANGE"))
        //         {
        //            index.withSortKey(coll.getProperty(keyInfo.getAttributeName()));
        //         }
        //      }

        Index primaryIndex = addTableIndex(PRIMARY_INDEX_TYPE, PRIMARY_INDEX_NAME, keySchema, coll, true, null, null);

        if (tableDescription.getGlobalSecondaryIndexes() != null) {
            for (GlobalSecondaryIndexDescription indexDesc : tableDescription.getGlobalSecondaryIndexes()) {
                Projection projection = indexDesc.getProjection();
                addTableIndex(GLOBAL_SECONDARY_INDEX_TYPE, indexDesc.getIndexName(), indexDesc.getKeySchema(), coll, false, projection, primaryIndex);
            }
        }

        if (tableDescription.getLocalSecondaryIndexes() != null) {
            for (LocalSecondaryIndexDescription indexDesc : tableDescription.getLocalSecondaryIndexes()) {
                Projection projection = indexDesc.getProjection();
                addTableIndex(LOCAL_SECONDARY_INDEX_TYPE, indexDesc.getIndexName(), indexDesc.getKeySchema(), coll, false, projection, primaryIndex);
            }
        }

        return coll;
    }

    protected Index addTableIndex(String type, String indexName, List<KeySchemaElement> keySchemaList, Collection collection, boolean unique, Projection projection, Index primaryIndex) {
        Index index = new Index(indexName, type, unique);

        for (KeySchemaElement keyInfo : keySchemaList) {
            Property property = collection.getProperty(keyInfo.getAttributeName());
            index.withProperties(property);
            property.withColumnName(keyInfo.getAttributeName());
        }

        if(projection != null){
            io.inversion.Projection invProj = new io.inversion.Projection();
            invProj.withType(projection.getProjectionType());

            for(Property prop : primaryIndex.getProperties()){
                invProj.withProperty(prop);
            }

            for (KeySchemaElement keyInfo : keySchemaList) {
                Property property = collection.getProperty(keyInfo.getAttributeName());
                invProj.withProperty(property);
            }

            List<String> nonKeys = projection.getNonKeyAttributes();
            if(nonKeys != null){
                for(String name : nonKeys){
                    Property property = collection.getProperty(name);
                    invProj.withProperty(property);
                }
            }

            //--TODO add columns
            index.withProjection(invProj);
        }

        collection.withIndexes(index);
        return index;
    }

    public com.amazonaws.services.dynamodbv2.document.Table getDynamoTable(Collection table) {
        return getDynamoTable(table.getTableName());
    }

    public com.amazonaws.services.dynamodbv2.document.Table getDynamoTable(String tableName) {
        return new DynamoDB(getDynamoClient()).getTable(tableName);
    }

    public DynamoDb withAwsRegion(String awsRegion) {
        this.awsRegion = awsRegion;
        return this;
    }

    public DynamoDb withAwsAccessKey(String awsAccessKey) {
        this.awsAccessKey = awsAccessKey;
        return this;
    }

    public DynamoDb withAwsSecretKey(String awsSecretKey) {
        this.awsSecretKey = awsSecretKey;
        return this;
    }

    public DynamoDb withAwsEndpoint(String awsEndpoint) {
        this.awsEndpoint = awsEndpoint;
        return this;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " - " + this.getName() + " - " + this.getCollections();
    }

    public AmazonDynamoDB getDynamoClient() {
        if (this.dynamoClient == null) {
            synchronized (this) {
                if (this.dynamoClient == null) {
                    this.dynamoClient = buildDynamoClient(awsRegion, awsAccessKey, awsSecretKey, awsEndpoint);
                }
            }
        }

        return dynamoClient;
    }

}
