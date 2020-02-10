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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
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
import io.inversion.cloud.model.Column;
import io.inversion.cloud.model.Db;
import io.inversion.cloud.model.Entity;
import io.inversion.cloud.model.Index;
import io.inversion.cloud.model.Results;
import io.inversion.cloud.model.SC;
import io.inversion.cloud.model.Table;
import io.inversion.cloud.rql.Term;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.utils.Rows.Row;
import io.inversion.cloud.utils.Utils;

public class DynamoDb extends Db<DynamoDb>
{
   protected String                   awsAccessKey = null;
   protected String                   awsSecretKey = null;
   protected String                   awsRegion    = "us-east-1";

   /**
    * Use to config which row is used to build the column/attribute model  (otherwise first row of scan will be used)
    * 
    * FORMAT: collection name | primaryKey | sortKey (optional)
    */
   protected String                   blueprintRow;

   protected int                      batchMax     = 20;

   transient protected AmazonDynamoDB dynamoClient = null;

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
   public Results<Row> select(Table table, List<Term> columnMappedTerms) throws Exception
   {
      DynamoDbQuery query = new DynamoDbQuery(table, columnMappedTerms).withDynamoTable(getDynamoTable(table));
      return query.doSelect();
   }


   @Override
   public List<String> upsert(Table table, List<Map<String, Object>> rows) throws Exception
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
            batch.addRequestItemsEntry(table.getName(), writeRequests);
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
         batch.addRequestItemsEntry(table.getName(), writeRequests);
         getDynamoClient().batchWriteItem(batch);
         batch.clearRequestItemsEntries();
         writeRequests.clear();
      }

      return keys;
   }

   @Override
   public void delete(Table table, List<Map<String, Object>> indexValues) throws Exception
   {
      for (Map<String, Object> row : indexValues)
      {
         deleteRow(table, row);
      }

   }

   public void deleteRow(Table table, Map<String, Object> row) throws Exception
   {
      com.amazonaws.services.dynamodbv2.document.Table dynamo = getDynamoTable(table);

      Index pk = table.getPrimaryIndex();

      if (pk.size() == 1)
      {
         dynamo.deleteItem(pk.getColumn(0).getName(), row.get(pk.getColumn(0).getName()));
      }
      else if (pk.size() == 2)
      {
         dynamo.deleteItem(pk.getColumn(0).getName(), row.get(pk.getColumn(0).getName()), pk.getColumn(1).getName(), row.get(pk.getColumn(1).getName()));
      }
      else
      {
         throw new ApiException(SC.SC_400_BAD_REQUEST, "A dynamo delete must have a hash key and an optional sortKey and that is it: '" + row + "'");
      }
   }

   //   @Override
   //   protected void startup0()
   //   {
   //      if (includeTables != null)
   //      {
   //         Map<String, String[]> blueprintRowMap = new HashMap<>();
   //         if (blueprintRow != null)
   //         {
   //            String[] parts = blueprintRow.split(",");
   //            for (String part : parts)
   //            {
   //               String[] arr = part.split("\\|");
   //               String collection = arr[0];
   //               blueprintRowMap.put(collection, arr);
   //            }
   //         }
   //
   //         String[] parts = includeTables.split(",");
   //         for (String part : parts)
   //         {
   //            String[] arr = part.split("\\|");
   //            String collectionName = arr[0];
   //            String tableName = collectionName;
   //            if (arr.length > 1)
   //            {
   //               tableName = arr[1];
   //            }
   //            else
   //            {
   //               collectionName = beautifyCollectionName(collectionName);
   //            }
   //
   //            Table table = buildTable(tableName, blueprintRowMap.get(collectionName));
   //            withTable(table);
   //
   //            Collection collection = buildCollection(collectionName, table);
   //            api.withCollection(collection);
   //         }
   //
   //      }
   //      else
   //      {
   //         log.warn("DynamoDb must have 'tableMappings' configured to be used");
   //      }
   //
   //   }

   Table buildTable(String tableName, String[] bluePrintArr)
   {
      AmazonDynamoDB dynamoClient = getDynamoClient();

      Table table = new Table(tableName);
      withTable(table);

      DynamoDB dynamoDB = new DynamoDB(dynamoClient);
      com.amazonaws.services.dynamodbv2.document.Table dynamoTable = dynamoDB.getTable(tableName);
      TableDescription tableDescription = dynamoTable.describe();

      for (AttributeDefinition attr : tableDescription.getAttributeDefinitions())
      {
         table.withColumn(attr.getAttributeName(), attr.getAttributeType(), true);
      }

      DynamoDbIndex index = new DynamoDbIndex(DynamoDbIndex.PRIMARY_INDEX, DynamoDbIndex.PRIMARY_TYPE);

      List<KeySchemaElement> keySchema = tableDescription.getKeySchema();
      for (KeySchemaElement keyInfo : keySchema)
      {
         if (keyInfo.getKeyType().equalsIgnoreCase("HASH"))
         {
            index.witHashKey(table.getColumn(keyInfo.getAttributeName()));
         }
         else if (keyInfo.getKeyType().equalsIgnoreCase("RANGE"))
         {
            index.withSortKey(table.getColumn(keyInfo.getAttributeName()));
         }
      }

      if (tableDescription.getGlobalSecondaryIndexes() != null)
      {
         for (GlobalSecondaryIndexDescription indexDesc : tableDescription.getGlobalSecondaryIndexes())
         {
            addTableIndex(DynamoDbIndex.GLOBAL_SECONDARY_TYPE, indexDesc.getIndexName(), indexDesc.getKeySchema(), table);
         }
      }

      if (tableDescription.getLocalSecondaryIndexes() != null)
      {
         for (LocalSecondaryIndexDescription indexDesc : tableDescription.getLocalSecondaryIndexes())
         {
            addTableIndex(DynamoDbIndex.LOCAL_SECONDARY_TYPE, indexDesc.getIndexName(), indexDesc.getKeySchema(), table);
         }
      }

      return table;
   }

   protected Collection buildCollection(String collectionName, Table table)
   {
      Collection collection = new Collection();
      collection.withName(beautifyCollectionName(collectionName));
      collection.withTable(table);

      Entity entity = collection.getEntity();

      for (Column col : table.getColumns())
      {
         entity.getAttribute(col.getName()).withName(beautifyAttributeName(col.getName()));
      }

      if (getCollectionPath() != null)
         collection.withIncludePaths(getCollectionPath());

      return collection;
   }

   protected void addTableIndex(String type, String indexName, List<KeySchemaElement> keySchemaList, Table table)
   {
      DynamoDbIndex index = new DynamoDbIndex(indexName, type);

      for (KeySchemaElement keyInfo : keySchemaList)
      {
         Column column = table.getColumn(keyInfo.getAttributeName());

         index.withColumnNames(column.getName());

         if (keyInfo.getKeyType().equalsIgnoreCase("HASH"))
         {
            index.witHashKey(table.getColumn(keyInfo.getAttributeName()));
         }

         else if (keyInfo.getKeyType().equalsIgnoreCase("RANGE"))
         {
            index.withSortKey(table.getColumn(keyInfo.getAttributeName()));
         }
      }

      table.withIndexes(index);
   }

   public com.amazonaws.services.dynamodbv2.document.Table getDynamoTable(Collection collection)
   {
      return getDynamoTable(collection.getTable().getName());
   }

   public com.amazonaws.services.dynamodbv2.document.Table getDynamoTable(Table table)
   {
      return getDynamoTable(table.getName());
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

   @Override
   public String toString()
   {
      return this.getClass().getSimpleName() + " - " + this.getName() + " - " + this.getTables();
   }

   /**
    * Used to keep track of Hash and Sort keys for a dynamo index.
    * 
    * @author kfrankic
    *
    */
   public static class DynamoDbIndex extends Index
   {
      public static final String PRIMARY_INDEX         = "Primary Index";

      public static final String PRIMARY_TYPE          = "primary";
      public static final String LOCAL_SECONDARY_TYPE  = "localsecondary";
      public static final String GLOBAL_SECONDARY_TYPE = "globalsecondary";

      protected Column           hashKey               = null;
      protected Column           sortKey               = null;

      public DynamoDbIndex()
      {
         super();
      }

      public DynamoDbIndex(String name, String type)
      {
         this(name, type, null, null);
      }

      public DynamoDbIndex(String name, String type, Column pk, Column sk)
      {
         super(name, type, "primary".equalsIgnoreCase(type) ? true : false, pk.getName(), sk.getName());
         this.hashKey = pk;
         this.sortKey = sk;
      }

      public boolean isLocalIndex()
      {
         return LOCAL_SECONDARY_TYPE.equalsIgnoreCase(type);
      }

      public boolean isPrimaryIndex()
      {
         return PRIMARY_TYPE.equalsIgnoreCase(type);
      }

      public boolean isGlobalSecondary()
      {
         return !isLocalIndex() && !isPrimaryIndex();
      }

      public Column getHashKey()
      {
         return hashKey;
      }

      public String getHashKeyName()
      {
         return hashKey != null ? hashKey.getName() : null;
      }

      public DynamoDbIndex witHashKey(Column hashKey)
      {
         this.hashKey = hashKey;
         withColumnNames(hashKey.getName());
         return this;
      }

      public Column getSortKey()
      {
         return sortKey;
      }

      public String getSortKeyName()
      {
         return sortKey != null ? sortKey.getName() : null;
      }

      public DynamoDbIndex withSortKey(Column sortKey)
      {
         this.sortKey = sortKey;
         withColumnNames(sortKey.getName());
         return null;
      }

   }

   public static DynamoDbIndex findIndexByName(Table table, String name)
   {
      if (table != null && table.getIndexes() != null)
      {
         for (DynamoDbIndex index : (List<DynamoDbIndex>) (List<?>) table.getIndexes())
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

   @Override
   public Object cast(String type, Object value)
   {
      try
      {
         if (value == null)
            return null;

         if (type == null)
         {
            try
            {
               if (value.toString().indexOf(".") < 0)
               {
                  value = Long.parseLong(value.toString());
               }
               else
               {
                  value = Double.parseDouble(value.toString());
               }

               return value;
            }
            catch (Exception ex)
            {

            }
            return value.toString();
         }

         switch (type)
         {
            case "S":
               return value.toString();

            case "N":
               if (value.toString().indexOf(".") < 0)
                  return Long.parseLong(value.toString());
               else
                  Double.parseDouble(value.toString());
            case "BOOL":
               return Boolean.parseBoolean(value.toString());

            default :
               return value.toString();
         }
      }
      catch (Exception ex)
      {
         throw new RuntimeException("Error casting '" + value + "' to type '" + type + "'", ex);
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
               this.dynamoClient = buildDynamoClient(name + ".", awsRegion, awsAccessKey, awsSecretKey);
            }
         }
      }

      return dynamoClient;
   }

   public static AmazonDynamoDB buildDynamoClient(String prefix)
   {
      return buildDynamoClient(prefix, null, null, null);
   }

   public static AmazonDynamoDB buildDynamoClient(String prefix, String awsRegion, String awsAccessKey, String awsSecretKey)
   {
      awsRegion = Utils.findSysEnvPropStr(prefix + ".awsRegion", awsRegion);
      awsAccessKey = Utils.findSysEnvPropStr(prefix + ".awsAccessKey", awsAccessKey);
      awsSecretKey = Utils.findSysEnvPropStr(prefix + ".awsSecretKey", awsSecretKey);

      AmazonDynamoDBClientBuilder builder = AmazonDynamoDBClientBuilder.standard();
      if (!Utils.empty(awsRegion))
      {
         builder.withRegion(awsRegion);
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
