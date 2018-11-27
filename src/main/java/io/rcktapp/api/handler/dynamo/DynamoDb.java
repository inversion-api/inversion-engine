package io.rcktapp.api.handler.dynamo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.atteo.evo.inflector.English;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.RangeKeyCondition;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.TableDescription;

import io.rcktapp.api.Api;
import io.rcktapp.api.Attribute;
import io.rcktapp.api.Collection;
import io.rcktapp.api.Column;
import io.rcktapp.api.Db;
import io.rcktapp.api.Entity;
import io.rcktapp.api.Index;
import io.rcktapp.api.Table;

public class DynamoDb extends Db
{
   public static final String PARTITION_KEY_INDEX = "Partition Key Index";
   public static final String SORT_KEY_INDEX      = "Sort Key Index";

   public static final String PARTITION_TYPE      = "partition";
   public static final String SORT_TYPE           = "sort";

   String                     awsRegion           = "us-east-1";

   /**
    * A CSV of pipe delimited collection name to table name pairs.
    * 
    * Example: dynamodb.tables=promo|promo-dev,loyalty-punchcard|loyalty-punchcard-dev
    */
   String                     tableMappings;

   /**
    * Use to config which row is used to build the column/attribute model  (otherwise first row of scan will be used)
    * 
    * FORMAT: collection name | primaryKey | sortKey (optional)
    */
   String                     blueprintRow;

   @Override
   public void bootstrapApi(Api api) throws Exception
   {
      AmazonDynamoDB dynamoClient = AmazonDynamoDBClientBuilder.standard().withRegion(awsRegion).build();

      this.setType("dynamo");

      if (tableMappings != null)
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

         String[] parts = tableMappings.split(",");
         for (String part : parts)
         {
            String[] arr = part.split("\\|");
            String collectionName = arr[0];
            String tableName = arr[1];

            Table table = buildTable(tableName, blueprintRowMap.get(collectionName), dynamoClient);
            Entity entity = buildEntity(collectionName, table);

            addTable(table);
            api.addCollection(entity.getCollection());
            entity.getCollection().setApi(api);

         }

         api.addDb(this, 0);
      }
      else
      {
         log.warn("DynamoDb must have 'tableMappings' configured to be used");
      }

   }

   Table buildTable(String tableName, String[] bluePrintArr, AmazonDynamoDB dynamoClient)
   {

      Table table = new Table(this, tableName);

      DynamoDB dynamoDB = new DynamoDB(dynamoClient);
      com.amazonaws.services.dynamodbv2.document.Table dynamoTable = dynamoDB.getTable(tableName);
      TableDescription tableDescription = dynamoTable.describe();
      String pk = null;
      String sk = null;

      List<KeySchemaElement> keySchema = tableDescription.getKeySchema();
      for (KeySchemaElement keyInfo : keySchema)
      {
         if (keyInfo.getKeyType().equalsIgnoreCase("HASH"))
         {
            pk = keyInfo.getAttributeName();
         }
         else if (keyInfo.getKeyType().equalsIgnoreCase("RANGE"))
         {
            sk = keyInfo.getAttributeName();
         }
      }

      // Lookup the blueprint row (the row to use to determine all the columns)

      Map<String, Object> bluePrintMap = null;

      if (bluePrintArr != null && bluePrintArr.length > 0)
      {
         String bluePrintPK = bluePrintArr[0];

         QuerySpec querySpec = new QuerySpec()//
                                              .withHashKey(pk, bluePrintPK)//
                                              .withMaxPageSize(1)//
                                              .withMaxResultSize(1);
         if (sk != null && bluePrintArr.length > 1)
         {
            String bluePrintSK = bluePrintArr[1];
            querySpec = querySpec.withRangeKeyCondition(new RangeKeyCondition(sk).eq(bluePrintSK));
         }

         ItemCollection<QueryOutcome> queryResults = dynamoTable.query(querySpec);

         for (Item item : queryResults)
         {
            bluePrintMap = item.asMap();
         }

      }
      else
      {
         ScanSpec scanSpec = new ScanSpec()//
                                           .withMaxPageSize(1)//
                                           .withMaxResultSize(1);

         ItemCollection<ScanOutcome> scanResults = dynamoTable.scan(scanSpec);
         for (Item item : scanResults)
         {
            bluePrintMap = item.asMap();
         }
      }

      if (bluePrintMap != null)
      {
         for (String k : bluePrintMap.keySet())
         {
            Object obj = bluePrintMap.get(k);
            boolean nullable = true;
            if (pk.equals(k) || (sk != null && sk.equals(k)))
            {
               nullable = false; // keys are not nullable
            }

            Column column = new Column(table, k, getTypeStringFromObject(obj), nullable);

            if (pk.equals(k))
            {
               // pk column
               Index index = new Index(table, PARTITION_KEY_INDEX, PARTITION_TYPE);
               index.addColumn(column);
               table.addIndex(index);
            }
            if (sk != null && sk.equals(k))
            {
               // sk column
               Index index = new Index(table, SORT_KEY_INDEX, SORT_TYPE);
               index.addColumn(column);
               table.addIndex(index);
            }

            table.addColumn(column);
         }
      }

      return table;

   }

   Entity buildEntity(String collectionName, Table table)
   {
      Entity entity = new Entity();
      Collection collection = new Collection();

      entity.setTbl(table);
      entity.setHint(table.getName());
      entity.setCollection(collection);

      collection.setEntity(entity);

      if (!collectionName.endsWith("s"))
         collectionName = English.plural(collectionName);

      collection.setName(collectionName);

      String pk = findPartitionKeyName(table);
      String sk = findSortKeyName(table);

      for (Column col : table.getColumns())
      {
         Attribute attr = new Attribute();
         attr.setEntity(entity);
         attr.setName(col.getName());
         attr.setColumn(col);
         attr.setHint(col.getTable().getName() + "." + col.getName());
         attr.setType(col.getType());

         if (col.getName().equals(pk) || col.getName().equals(sk))
         {
            entity.addKey(attr);
         }
         else
         {
            entity.addAttribute(attr);
         }
      }

      return entity;
   }

   public static String findPartitionKeyName(Table table)
   {
      Index index = findIndexByName(table, PARTITION_KEY_INDEX);
      if (index != null && index.getColumns() != null)
      {
         return index.getColumns().get(0).getName();
      }
      return null;
   }

   public static String findSortKeyName(Table table)
   {
      Index index = findIndexByName(table, SORT_KEY_INDEX);
      if (index != null && index.getColumns() != null)
      {
         return index.getColumns().get(0).getName();
      }
      return null;
   }

   public static Index findIndexByName(Table table, String name)
   {
      if (table != null && table.getIndexes() != null)
      {
         for (Index index : table.getIndexes())
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
   public static String getTypeStringFromObject(Object obj)
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

   public void setTableMappings(String tableMappings)
   {
      this.tableMappings = tableMappings;
   }

   public void setAwsRegion(String awsRegion)
   {
      this.awsRegion = awsRegion;
   }

   public void setBlueprintRow(String blueprintRow)
   {
      this.blueprintRow = blueprintRow;
   }

   public static void main(String[] args)
   {
      try
      {
         DynamoDb dynamoDb = new DynamoDb();
         dynamoDb.setTableMappings("promo|promo-dev,loyalty-punchcard|loyalty-punchcard-dev");

         Api api = new Api();
         dynamoDb.bootstrapApi(api);
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

}
