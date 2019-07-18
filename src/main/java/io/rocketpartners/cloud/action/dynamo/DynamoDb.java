/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
 * http://rocketpartners.io
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package io.rocketpartners.cloud.action.dynamo;

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
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest;
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndexDescription;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.LocalSecondaryIndexDescription;
import com.amazonaws.services.dynamodbv2.model.PutRequest;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;

import io.rocketpartners.cloud.model.ApiException;
import io.rocketpartners.cloud.model.Collection;
import io.rocketpartners.cloud.model.Column;
import io.rocketpartners.cloud.model.Db;
import io.rocketpartners.cloud.model.Entity;
import io.rocketpartners.cloud.model.Index;
import io.rocketpartners.cloud.model.Results;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.model.Table;
import io.rocketpartners.cloud.rql.Term;
import io.rocketpartners.cloud.utils.Rows.Row;
import io.rocketpartners.cloud.utils.Utils;

public class DynamoDb extends Db<DynamoDb>
{
   protected String       awsAccessKey = null;
   protected String       awsSecretKey = null;
   protected String       awsRegion    = "us-east-1";

   /**
    * A CSV of pipe delimited collection name to table name pairs.
    * 
    * Example: dynamodb.tables=promo|promo-dev,loyalty-punchcard|loyalty-punchcard-dev
    * 
    * Or if the collection name is the name as the table name you can just send a the name
    * 
    * Example: dynamodb.includeTables=orders,users,events
    */
   protected String       includeTables;

   /**
    * Use to config which row is used to build the column/attribute model  (otherwise first row of scan will be used)
    * 
    * FORMAT: collection name | primaryKey | sortKey (optional)
    */
   protected String       blueprintRow;

   protected int          batchMax     = 20;

   private AmazonDynamoDB dynamoClient = null;

   public DynamoDb()
   {
      this.withType("dynamodb");
   }

   public DynamoDb(String name, String includeTables)
   {
      this();
      this.name = name;
      this.includeTables = includeTables;
   }

   @Override
   public Results<Row> select(Table table, List<Term> columnMappedTerms) throws Exception
   {
      DynamoDbQuery query = new DynamoDbQuery(table, columnMappedTerms).withDynamoTable(getDynamoTable(table));
      return query.doSelect();
   }

   @Override
   public String upsert(Table table, Map<String, Object> row) throws Exception
   {
      List<String> keys = upsert(table, Arrays.asList(row));
      if (keys != null && keys.size() > 0)
         return keys.get(0);

      return null;
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

         if (i > 0 && i % batchMax == 0)
         {
            //write a batch to dynamo
            batch.addRequestItemsEntry(table.getName(), writeRequests);
            dynamoClient.batchWriteItem(batch);
            batch.clearRequestItemsEntries();
            writeRequests.clear();
         }
         //add to the current row to batch 
         PutRequest put = new PutRequest().withItem(ItemUtils.fromSimpleMap(row));
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
   public void delete(Table table, String entityKey) throws Exception
   {
      Row key = table.decodeKey(entityKey);

      com.amazonaws.services.dynamodbv2.document.Table dynamo = getDynamoTable(table);

      if (key.size() == 1)
      {
         dynamo.deleteItem(key.getKey(0), key.get(0));
      }
      else if (key.size() == 2)
      {
         dynamo.deleteItem(key.getKey(0), key.get(0), key.getKey(1), key.get(1));
      }
      else
      {
         throw new ApiException(SC.SC_400_BAD_REQUEST, "A dynamo delete must have a hash key and an optional sortKey and that is it: '" + entityKey + "'");
      }
   }

   @Override
   protected void startup0()
   {
      if (includeTables != null)
      {
         Map<String, String[]> blueprintRowMap = new HashMap<>();
         if (blueprintRow != null)
         {
            String[] parts = blueprintRow.split(",");
            for (String part : parts)
            {
               String[] arr = part.split("\\|");
               String collection = arr[0];
               blueprintRowMap.put(collection, arr);
            }
         }

         String[] parts = includeTables.split(",");
         for (String part : parts)
         {
            String[] arr = part.split("\\|");
            String collectionName = arr[0];
            String tableName = collectionName;
            if (arr.length > 1)
            {
               tableName = arr[1];
            }
            else
            {
               collectionName = beautifyCollectionName(collectionName);
            }

            Table table = buildTable(tableName, blueprintRowMap.get(collectionName));
            withTable(table);

            Collection collection = buildCollection(collectionName, table);
            api.withCollection(collection);
         }

      }
      else
      {
         log.warn("DynamoDb must have 'tableMappings' configured to be used");
      }

   }

   Table buildTable(String tableName, String[] bluePrintArr)
   {
      AmazonDynamoDB dynamoClient = getDynamoClient();

      Table table = new Table(this, tableName);

      DynamoDB dynamoDB = new DynamoDB(dynamoClient);
      com.amazonaws.services.dynamodbv2.document.Table dynamoTable = dynamoDB.getTable(tableName);
      TableDescription tableDescription = dynamoTable.describe();

      for (AttributeDefinition attr : tableDescription.getAttributeDefinitions())
      {
         table.makeColumn(attr.getAttributeName(), attr.getAttributeType());
      }

      DynamoDbIndex index = new DynamoDbIndex(table, DynamoDbIndex.PRIMARY_INDEX, DynamoDbIndex.PRIMARY_TYPE);

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
      DynamoDbIndex index = new DynamoDbIndex(table, indexName, type);

      for (KeySchemaElement keyInfo : keySchemaList)
      {
         Column column = table.getColumn(keyInfo.getAttributeName());

         index.withColumn(column);

         if (keyInfo.getKeyType().equalsIgnoreCase("HASH"))
         {
            index.witHashKey(table.getColumn(keyInfo.getAttributeName()));
         }

         else if (keyInfo.getKeyType().equalsIgnoreCase("RANGE"))
         {
            index.withSortKey(table.getColumn(keyInfo.getAttributeName()));
         }
      }

      table.withIndex(index);
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

   public DynamoDb withIncludeTables(String includeTables)
   {
      this.includeTables = includeTables;
      return this;
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

      public DynamoDbIndex(Table table, String name, String type)
      {
         super(table, name, type);
      }

      public DynamoDbIndex(Table table, String name, String type, Column pk, Column sk)
      {
         super(table, name, type);

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
         withColumn(hashKey);
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
         withColumn(sortKey);
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
            return value.toString();

         switch (type)
         {
            case "N":
               return Long.parseLong(value.toString());

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
