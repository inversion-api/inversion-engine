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
package io.inversion.cloud.action.dynamo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest;
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndexDescription;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.LocalSecondaryIndexDescription;
import com.amazonaws.services.dynamodbv2.model.PutRequest;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;

import io.inversion.cloud.model.ApiException;
import io.inversion.cloud.model.Collection;
import io.inversion.cloud.model.Db;
import io.inversion.cloud.model.Index;
import io.inversion.cloud.model.Property;
import io.inversion.cloud.model.Results;
import io.inversion.cloud.model.Rows.Row;
import io.inversion.cloud.rql.Term;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.utils.Utils;

public class DynamoDb extends Db<DynamoDb>
{

   public static final String         PRIMARY_INDEX_NAME          = "Primary Index";

   public static final String         PRIMARY_INDEX_TYPE          = "primary";
   public static final String         LOCAL_SECONDARY_INDEX_TYPE  = "localsecondary";
   public static final String         GLOBAL_SECONDARY_INDEX_TYPE = "globalsecondary";

   protected String                   awsAccessKey                = null;
   protected String                   awsSecretKey                = null;
   protected String                   awsRegion                   = "us-east-1";
   protected String                   awsEndpoint                 = null;

   /**
    * Use to config which row is used to build the column/attribute model  (otherwise first row of scan will be used)
    *
    * FORMAT: collection name | primaryKey | sortKey (optional)
    */
   protected String                   blueprintRow;

   protected int                      batchMax                    = 20;

   transient protected AmazonDynamoDB dynamoClient                = null;

   public DynamoDb()
   {
      this.withType("dynamodb");
   }

   public DynamoDb(String name, String includeTables)
   {
      this();
      withName(name);
      withIncludeTables(includeTables);
   }

   @Override
   public Results<Row> select(Collection table, List<Term> columnMappedTerms) throws Exception
   {
      DynamoDbQuery query = new DynamoDbQuery(table, columnMappedTerms).withDynamoTable(getDynamoTable(table));
      return query.doSelect();
   }

   @Override
   public List<String> upsert(Collection table, List<Map<String, Object>> rows) throws Exception
   {
      AmazonDynamoDB dynamoClient = getDynamoClient();
      List keys = new ArrayList();
      List<WriteRequest> writeRequests = new LinkedList<WriteRequest>();
      BatchWriteItemRequest batch = new BatchWriteItemRequest();
      for (int i = 0; i < rows.size(); i++)
      {
         Map<String, Object> row = rows.get(i);

         String key = table.encodeKey(row);
         keys.add(key);

         for (String attr : (List<String>) new ArrayList(row.keySet()))
         {
            if (Utils.empty(row.get(attr)))
               row.remove(attr);
         }

         if (i > 0 && i % batchMax == 0)
         {
            //write a batch to dynamo
            batch.addRequestItemsEntry(table.getTableName(), writeRequests);
            dynamoClient.batchWriteItem(batch);
            batch.clearRequestItemsEntries();
            writeRequests.clear();
         }
         //add to the current row to batch 
         Map<String, AttributeValue> item = ItemUtils.fromSimpleMap(row);
         Chain.debug("DynamoDb", "PutRequest", item);
         PutRequest put = new PutRequest().withItem(item);
         writeRequests.add(new WriteRequest(put));
      }

      if (writeRequests.size() > 0)
      {
         batch.addRequestItemsEntry(table.getTableName(), writeRequests);
         getDynamoClient().batchWriteItem(batch);
         batch.clearRequestItemsEntries();
         writeRequests.clear();
      }

      return keys;
   }

   @Override
   public void delete(Collection table, List<Map<String, Object>> indexValues) throws Exception
   {
      for (Map<String, Object> row : indexValues)
      {
         deleteRow(table, row);
      }

   }

   public void deleteRow(Collection table, Map<String, Object> row) throws Exception
   {
      com.amazonaws.services.dynamodbv2.document.Table dynamo = getDynamoTable(table);

      Index pk = table.getPrimaryIndex();

      if (pk.size() == 1)
      {
         dynamo.deleteItem(pk.getProperty(0).getColumnName(), row.get(pk.getProperty(0).getColumnName()));
      }
      else if (pk.size() == 2)
      {
         dynamo.deleteItem(pk.getProperty(0).getColumnName(), row.get(pk.getProperty(0).getColumnName()), pk.getProperty(1).getColumnName(), row.get(pk.getProperty(1).getColumnName()));
      }
      else
      {
         ApiException.throw400BadRequest("A dynamo delete must have a hash key and an optional sortKey and that is it: '{}'", row);
      }
   }

   public void configDb() throws Exception
   {
      for (String tableName : includeTables.keySet())
      {
         withCollection(buildCollection(tableName));
      }
   }

   protected Collection buildCollection(String tableName)
   {
      AmazonDynamoDB dynamoClient = getDynamoClient();

      Collection coll = new Collection(tableName);
      withCollection(coll);

      DynamoDB dynamoDB = new DynamoDB(dynamoClient);
      com.amazonaws.services.dynamodbv2.document.Table dynamoTable = dynamoDB.getTable(tableName);
      TableDescription tableDescription = dynamoTable.describe();

      for (AttributeDefinition attr : tableDescription.getAttributeDefinitions())
      {
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

      addTableIndex(PRIMARY_INDEX_TYPE, PRIMARY_INDEX_NAME, keySchema, coll);

      if (tableDescription.getGlobalSecondaryIndexes() != null)
      {
         for (GlobalSecondaryIndexDescription indexDesc : tableDescription.getGlobalSecondaryIndexes())
         {
            addTableIndex(GLOBAL_SECONDARY_INDEX_TYPE, indexDesc.getIndexName(), indexDesc.getKeySchema(), coll);
         }
      }

      if (tableDescription.getLocalSecondaryIndexes() != null)
      {
         for (LocalSecondaryIndexDescription indexDesc : tableDescription.getLocalSecondaryIndexes())
         {
            addTableIndex(LOCAL_SECONDARY_INDEX_TYPE, indexDesc.getIndexName(), indexDesc.getKeySchema(), coll);
         }
      }

      return coll;
   }

   //   protected Collection buildCollection(String collectionName, Table table)
   //   {
   //      Collection collection = new Collection();
   //      collection.withName(beautifyCollectionName(collectionName));
   //      collection.withTable(table);
   //
   //      Entity entity = collection.getEntity();
   //
   //      for (Column col : table.getColumns())
   //      {
   //         entity.getAttribute(col.getColumnName()).withName(beautifyAttributeName(col.getColumnName()));
   //      }
   //
   //      if (getCollectionPath() != null)
   //         collection.withIncludePaths(getCollectionPath());
   //
   //      return collection;
   //   }

   protected void addTableIndex(String type, String indexName, List<KeySchemaElement> keySchemaList, Collection table)
   {
      Index index = new Index(indexName, type, false);

      for (KeySchemaElement keyInfo : keySchemaList)
      {
         Property property = table.getProperty(keyInfo.getAttributeName());

         index.withProperties(property);
         property.withColumnName(keyInfo.getAttributeName());
         
         //TODO: was this refactor correct
//         index.withColumnNames(column.getColumnName());
//
//         if (keyInfo.getKeyType().equalsIgnoreCase("HASH"))
//         {
//            index.setColumnName(0, keyInfo.getAttributeName());
//         }
//
//         else if (keyInfo.getKeyType().equalsIgnoreCase("RANGE"))
//         {
//            index.setColumnName(1, keyInfo.getAttributeName());
//         }
      }

      table.withIndexes(index);
   }

   public com.amazonaws.services.dynamodbv2.document.Table getDynamoTable(Collection table)
   {
      return getDynamoTable(table.getTableName());
   }

   public com.amazonaws.services.dynamodbv2.document.Table getDynamoTable(String tableName)
   {
      return new DynamoDB(getDynamoClient()).getTable(tableName);
   }

   public DynamoDb withBlueprintRow(String blueprintRow)
   {
      this.blueprintRow = blueprintRow;
      return this;
   }

   public DynamoDb withAwsRegion(String awsRegion)
   {
      this.awsRegion = awsRegion;
      return this;
   }

   public DynamoDb withAwsAccessKey(String awsAccessKey)
   {
      this.awsAccessKey = awsAccessKey;
      return this;
   }

   public DynamoDb withAwsSecretKey(String awsSecretKey)
   {
      this.awsSecretKey = awsSecretKey;
      return this;
   }

   public DynamoDb withAwsEndpoint(String awsEndpoint)
   {
      this.awsEndpoint = awsEndpoint;
      return this;
   }

   @Override
   public String toString()
   {
      return this.getClass().getSimpleName() + " - " + this.getName() + " - " + this.getCollections();
   }

   //   /**
   //    * Used to keep track of Hash and Sort keys for a dynamo index.
   //    * 
   //    * @author kfrankic
   //    *
   //    */
   //   public static class DynamoDbIndex extends Index
   //   {
   //      public static final String PRIMARY_INDEX         = "Primary Index";
   //
   //      public static final String PRIMARY_TYPE          = "primary";
   //      public static final String LOCAL_SECONDARY_TYPE  = "localsecondary";
   //      public static final String GLOBAL_SECONDARY_TYPE = "globalsecondary";
   //
   //      protected Property         hashKey               = null;
   //      protected Property         sortKey               = null;
   //
   //      public DynamoDbIndex()
   //      {
   //         super();
   //      }
   //
   //      public DynamoDbIndex(String name, String type)
   //      {
   //         this(name, type, null, null);
   //      }
   //
   //      public DynamoDbIndex(String name, String type, Property pk, Property sk)
   //      {
   //         super(name, type, "primary".equalsIgnoreCase(type) ? true : false);
   //         this.hashKey = pk;
   //         this.sortKey = sk;
   //      }
   //
   //      public boolean isLocalIndex()
   //      {
   //         return LOCAL_SECONDARY_TYPE.equalsIgnoreCase(type);
   //      }
   //
   //      public boolean isPrimaryIndex()
   //      {
   //         return PRIMARY_TYPE.equalsIgnoreCase(type);
   //      }
   //
   //      public boolean isGlobalSecondary()
   //      {
   //         return !isLocalIndex() && !isPrimaryIndex();
   //      }
   //
   //      public Property getHashKey()
   //      {
   //         return hashKey;
   //      }
   //
   //      public String getHashKeyName()
   //      {
   //         return hashKey != null ? hashKey.getColumnName() : null;
   //      }
   //
   //      public DynamoDbIndex witHashKey(Property hashKey)
   //      {
   //         this.hashKey = hashKey;
   //         withColumnNames(hashKey.getColumnName());
   //         return this;
   //      }
   //
   //      public Property getSortKey()
   //      {
   //         return sortKey;
   //      }
   //
   //      public String getSortKeyName()
   //      {
   //         return sortKey != null ? sortKey.getColumnName() : null;
   //      }
   //
   //      public DynamoDbIndex withSortKey(Property sortKey)
   //      {
   //         this.sortKey = sortKey;
   //         withColumnNames(sortKey.getColumnName());
   //         return null;
   //      }
   //
   //   }

   public static Index findIndexByName(Collection coll, String name)
   {
      if (coll != null && coll.getIndexes() != null)
      {
         for (Index index : coll.getIndexes())
         {
            if (index.getName().equals(name))
            {
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
   protected static String getTypeStringFromObject(Object obj)
   {
      if (obj instanceof Number)
      {
         return "N";
      }
      else if (obj instanceof Boolean)
      {
         return "BOOL";
      }
      else
      {
         return "S";
      }
   }

   public AmazonDynamoDB getDynamoClient()
   {
      if (this.dynamoClient == null)
      {
         synchronized (this)
         {
            if (this.dynamoClient == null)
            {
               this.dynamoClient = buildDynamoClient(name, awsRegion, awsAccessKey, awsSecretKey, awsEndpoint);
            }
         }
      }

      return dynamoClient;
   }

   public static AmazonDynamoDB buildDynamoClient(String prefix)
   {
      return buildDynamoClient(prefix, null, null, null, null);
   }

   public static AmazonDynamoDB buildDynamoClient(String prefix, String awsRegion, String awsAccessKey, String awsSecretKey, String awsEndpoint)
   {
      awsRegion = Utils.getSysEnvPropStr(prefix + ".awsRegion", awsRegion);
      awsAccessKey = Utils.getSysEnvPropStr(prefix + ".awsAccessKey", awsAccessKey);
      awsSecretKey = Utils.getSysEnvPropStr(prefix + ".awsSecretKey", awsSecretKey);
      awsEndpoint = Utils.getSysEnvPropStr(prefix + ".awsEndpoint", awsEndpoint);

      AmazonDynamoDBClientBuilder builder = AmazonDynamoDBClientBuilder.standard();
      if (!Utils.empty(awsRegion))
      {
         if (!Utils.empty(awsEndpoint))
         {
            AwsClientBuilder.EndpointConfiguration endpointConfig = new AwsClientBuilder.EndpointConfiguration(awsEndpoint, awsRegion);
            builder.withEndpointConfiguration(endpointConfig);
         }
         else
         {
            builder.withRegion(awsRegion);
         }
      }
      if (!Utils.empty(awsAccessKey) && !Utils.empty(awsSecretKey))
      {
         BasicAWSCredentials creds = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
         builder.withCredentials(new AWSStaticCredentialsProvider(creds));
      }

      AmazonDynamoDB dynamoClient = builder.build();

      return dynamoClient;
   }

}
