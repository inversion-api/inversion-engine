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
package io.rcktapp.api.handler.dynamo;

import java.util.ArrayList;
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
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndexDescription;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.LocalSecondaryIndexDescription;
import com.amazonaws.services.dynamodbv2.model.TableDescription;

import io.rcktapp.api.Api;
import io.rcktapp.api.ApiException;
import io.rcktapp.api.Attribute;
import io.rcktapp.api.Collection;
import io.rcktapp.api.Column;
import io.rcktapp.api.Db;
import io.rcktapp.api.Entity;
import io.rcktapp.api.Index;
import io.rcktapp.api.SC;
import io.rcktapp.api.Table;
import io.rcktapp.rql.Parser;
import io.rcktapp.rql.Predicate;
import io.rcktapp.rql.dynamo.DynamoRql;

public class DynamoDb extends Db
{
   public static final String PARTITION_KEY_INDEX   = "Partition Key Index";
   public static final String SORT_KEY_INDEX        = "Sort Key Index";

   public static final String PARTITION_TYPE        = "partition";
   public static final String SORT_TYPE             = "sort";
   public static final String LOCAL_SECONDARY_TYPE  = "localsecondary";
   public static final String GLOBAL_SECONDARY_TYPE = "globalsecondary";

   static
   {
      try
      {
         //bootstraps the DynamoRql type
         Class.forName(DynamoRql.class.getName());
      }
      catch (Exception ex)
      {
         ex.printStackTrace();
      }
   }

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

   private AmazonDynamoDB dynamoClient = null;

   @Override
   public void bootstrapApi() throws Exception
   {
      this.dynamoClient = getDynamoClient();

      this.setType("dynamo");

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

            Table table = buildTable(tableName, blueprintRowMap.get(collectionName), dynamoClient);
            Entity entity = buildEntity(collectionName, table);

            addTable(table);
            api.addCollection(entity.getCollection());

         }

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

      if (tableDescription.getGlobalSecondaryIndexes() != null)
      {
         for (GlobalSecondaryIndexDescription indexDesc : tableDescription.getGlobalSecondaryIndexes())
         {
            DynamoIndex index = new DynamoIndex(table, indexDesc.getIndexName(), GLOBAL_SECONDARY_TYPE);

            for (KeySchemaElement keyInfo : indexDesc.getKeySchema())
            {
               Column column = table.getColumns().stream()//
                                    .filter(c -> c.getName().equals(keyInfo.getAttributeName()))//
                                    .findFirst().orElse(null);
               index.addColumn(column);

               if (keyInfo.getKeyType().equalsIgnoreCase("HASH"))
               {
                  index.setPartitionKey(keyInfo.getAttributeName());
               }

               else if (keyInfo.getKeyType().equalsIgnoreCase("RANGE"))
               {
                  index.setSortKey(keyInfo.getAttributeName());
               }
            }

            table.addIndex(index);
         }
      }

      if (tableDescription.getLocalSecondaryIndexes() != null)
      {
         for (LocalSecondaryIndexDescription indexDesc : tableDescription.getLocalSecondaryIndexes())
         {
            for (KeySchemaElement keyInfo : indexDesc.getKeySchema())
            {
               if (keyInfo.getKeyType().equalsIgnoreCase("RANGE"))
               {
                  Index index = new Index(table, indexDesc.getIndexName(), LOCAL_SECONDARY_TYPE);

                  Column column = table.getColumns().stream()//
                                       .filter(c -> c.getName().equals(keyInfo.getAttributeName()))//
                                       .findFirst().orElse(null);

                  index.addColumn(column);
                  table.addIndex(index);
                  break;
               }
            }
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

   public AmazonDynamoDB getDynamoClient()
   {
      if (this.dynamoClient == null)
      {
         this.dynamoClient = AmazonDynamoDBClientBuilder.standard().withRegion(awsRegion).build();
      }

      return dynamoClient;
   }

   public com.amazonaws.services.dynamodbv2.document.Table getDynamoTable(String tableName)
   {
      return new DynamoDB(getDynamoClient()).getTable(tableName);
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

   public static List<Index> findIndexesByType(Table table, String type)
   {
      List<Index> l = new ArrayList<Index>();

      if (table != null && table.getIndexes() != null)
      {
         for (Index index : table.getIndexes())
         {
            if (index.getType().equals(type))
            {
               l.add(index);
            }
         }
      }
      return l;
   }

   public static Index findIndexByColumnName(Table table, String colName)
   {
      if (table != null && table.getIndexes() != null)
      {
         for (Index index : table.getIndexes())
         {
            if (!index.getColumns().isEmpty())
            {
               for (Column column : index.getColumns())
               {
                  if (column.getName().equalsIgnoreCase(colName))
                  {
                     return index;
                  }
               }
            }
         }
      }
      return null;
   }

   public static Index findIndexByTypeAndColumnName(Table table, String typeName, String colName)
   {
      if (table != null)
      {
         List<Index> list = findIndexesByType(table, typeName);
         for (Index index : list)
         {
            if (!index.getColumns().isEmpty())
            {
               for (Column column : index.getColumns())
               {
                  if (column.getName().equalsIgnoreCase(colName))
                  {
                     return index;
                  }
               }
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

   public static Object cast(String value, String colName, Table table)
   {
      Column col = table.getColumn(colName);

      if (col != null)
      {
         switch (col.getType())
         {
            case "N":
               return Long.parseLong(value);

            case "BOOL":
               return Boolean.parseBoolean(value);

         }
      }

      return Parser.dequote(value);
   }

   public static String attributeValueAsString(AttributeValue attr, String colName, Table table)
   {
      Column col = table.getColumn(colName);

      if (col != null)
      {
         switch (col.getType())
         {
            case "N":
               return attr.getN();

            case "BOOL":
               return attr.getBOOL().toString();

         }
      }

      return attr.getS();
   }

   public static RangeKeyCondition predicateToRangeKeyCondition(Predicate pred, Table table)
   {
      String name = pred.getTerms().get(0).getToken();
      Object val = DynamoDb.cast((String) pred.getTerms().get(1).getToken(), name, table);

      RangeKeyCondition rkc = new RangeKeyCondition(name);
      switch (pred.getToken())
      {
         case "eq":
            rkc.eq(val);
            break;

         case "gt":
            rkc.gt(val);
            break;

         case "ge":
            rkc.ge(val);
            break;

         case "lt":
            rkc.lt(val);
            break;

         case "le":
            rkc.le(val);
            break;

         case "sw":
            rkc.beginsWith((String) val);
            break;

         default :
            throw new ApiException(SC.SC_400_BAD_REQUEST, "Operator '" + pred.getToken() + "' is not supported for a dynamo range key condition");
      }

      return rkc;
   }

   public void setIncludeTables(String includeTables)
   {
      this.includeTables = includeTables;
   }

   public void setAwsRegion(String awsRegion)
   {
      this.awsRegion = awsRegion;
   }

   public void setBlueprintRow(String blueprintRow)
   {
      this.blueprintRow = blueprintRow;
   }

   @Override
   public String toString()
   {
      return this.getClass().getSimpleName() + " - " + this.getName() + " - " + this.getTables();
   }

   public String verboseToString()
   {
      String s = "";
      s = s + this.getName() + "\n";
      s = s + this.getApi().getAccountCode() + "/" + this.getApi().getApiCode() + "\n";
      for (Table table : this.getTables())
      {
         s = s + "TABLE:     " + table.getName() + "\n";
         for (Column column : table.getColumns())
         {
            s = s + " > COLUMN: " + column.getName() + " (" + column.getType() + ")\n";
         }
         for (Index index : table.getIndexes())
         {
            s = s + " > INDEX:  " + index.getName() + " (" + index.getType() + " / " + index.getColumns() + ")\n";
         }
      }

      return s;
   }

   public static void main(String[] args)
   {
      try
      {
         DynamoDb dynamoDb = new DynamoDb();
         dynamoDb.setIncludeTables("promo|promo-dev,loyalty-punchcard|loyalty-punchcard-dev,tim-test");

         Api api = new Api();
         dynamoDb.setApi(api);
         dynamoDb.bootstrapApi();

         System.out.println(dynamoDb.verboseToString());
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

}
