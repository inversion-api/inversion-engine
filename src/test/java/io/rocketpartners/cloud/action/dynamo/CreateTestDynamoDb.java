package io.rocketpartners.cloud.action.dynamo;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.LocalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.Projection;
import com.amazonaws.services.dynamodbv2.model.ProjectionType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;

public class CreateTestDynamoDb
{
   public static void main(String[] args) throws Exception
   {
      //deleteTable("test-northwind");
      createTable("test-northwind");
   }

   public static void deleteTable(String tableName) throws Exception
   {
      AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
      DeleteTableRequest dtr = new DeleteTableRequest().withTableName(tableName);
      client.deleteTable(dtr);
   }

   public static void createTable(String tableName) throws Exception
   {
      List<AttributeDefinition> attrs = new ArrayList<>();

      attrs.add(new AttributeDefinition().withAttributeName("hk").withAttributeType("N"));
      attrs.add(new AttributeDefinition().withAttributeName("sk").withAttributeType("S"));

      attrs.add(new AttributeDefinition().withAttributeName("gs1hk").withAttributeType("N"));
      attrs.add(new AttributeDefinition().withAttributeName("gs1sk").withAttributeType("S"));

      attrs.add(new AttributeDefinition().withAttributeName("gs2hk").withAttributeType("S"));
      attrs.add(new AttributeDefinition().withAttributeName("gs2sk").withAttributeType("S"));

      attrs.add(new AttributeDefinition().withAttributeName("ls1").withAttributeType("S"));
      attrs.add(new AttributeDefinition().withAttributeName("ls2").withAttributeType("S"));
      attrs.add(new AttributeDefinition().withAttributeName("ls3").withAttributeType("S"));

      List<KeySchemaElement> keys = new ArrayList<>();
      keys.add(new KeySchemaElement().withAttributeName("hk").withKeyType(KeyType.HASH));
      keys.add(new KeySchemaElement().withAttributeName("sk").withKeyType(KeyType.RANGE));

      List<LocalSecondaryIndex> lsxs = new ArrayList();
      lsxs.add(new LocalSecondaryIndex().withIndexName("ls1").withKeySchema(new KeySchemaElement().withAttributeName("hk").withKeyType(KeyType.HASH)//
            , new KeySchemaElement().withAttributeName("ls1").withKeyType(KeyType.RANGE)));

      lsxs.add(new LocalSecondaryIndex().withIndexName("ls2").withKeySchema(new KeySchemaElement().withAttributeName("hk").withKeyType(KeyType.HASH)//
            , new KeySchemaElement().withAttributeName("ls2").withKeyType(KeyType.RANGE)));

      lsxs.add(new LocalSecondaryIndex().withIndexName("ls3").withKeySchema(new KeySchemaElement().withAttributeName("hk").withKeyType(KeyType.HASH)//
            , new KeySchemaElement().withAttributeName("ls3").withKeyType(KeyType.RANGE)));

      List<GlobalSecondaryIndex> gsxs = new ArrayList();
      gsxs.add(new GlobalSecondaryIndex().withIndexName("gs1").withKeySchema(new KeySchemaElement().withAttributeName("gs1hk").withKeyType(KeyType.HASH), new KeySchemaElement().withAttributeName("gs1sk").withKeyType(KeyType.RANGE)));
      gsxs.add(new GlobalSecondaryIndex().withIndexName("gs2").withKeySchema(new KeySchemaElement().withAttributeName("gs2hk").withKeyType(KeyType.HASH), new KeySchemaElement().withAttributeName("gs2sk").withKeyType(KeyType.RANGE)));
      gsxs.add(new GlobalSecondaryIndex().withIndexName("gs3").withKeySchema(new KeySchemaElement().withAttributeName("sk").withKeyType(KeyType.HASH), new KeySchemaElement().withAttributeName("hk").withKeyType(KeyType.RANGE)));

      for (LocalSecondaryIndex lsx : lsxs)
      {
         lsx.setProjection(new Projection().withProjectionType(ProjectionType.ALL));
      }

      for (GlobalSecondaryIndex gsx : gsxs)
      {
         gsx.setProjection(new Projection().withProjectionType(ProjectionType.ALL));
         gsx.withProvisionedThroughput(new ProvisionedThroughput()//
                                                                  .withReadCapacityUnits(5L)//
                                                                  .withWriteCapacityUnits(5L));
      }

      AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
      DynamoDB dynamoDB = new DynamoDB(client);

      CreateTableRequest request = new CreateTableRequest()//
                                                           .withGlobalSecondaryIndexes(gsxs)//
                                                           .withLocalSecondaryIndexes(lsxs).withTableName(tableName)//
                                                           .withKeySchema(keys)//
                                                           .withAttributeDefinitions(attrs)//
                                                           .withProvisionedThroughput(new ProvisionedThroughput()//
                                                                                                                 .withReadCapacityUnits(100L)//
                                                                                                                 .withWriteCapacityUnits(100L));

      com.amazonaws.services.dynamodbv2.document.Table table = dynamoDB.createTable(request);

      table.waitForActive();
   }
}
