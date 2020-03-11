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
package io.inversion.cloud.action.dynamo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
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

import io.inversion.cloud.action.rest.RestAction;
import io.inversion.cloud.jdbc.JdbcDbFactory;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.Collection;
import io.inversion.cloud.model.JSArray;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.service.Engine;

public class DynamoDbFactory
{
   public static void main(String[] args) throws Exception
   {
      buildNorthwindDynamoDb();
   }

   protected static DynamoDb buildNorthwindDynamoDb() throws Exception
   {
      rebuildNorthwind();
      return new NorthwindDynamoDb();
   }

   public static class NorthwindDynamoDb extends DynamoDb implements Serializable
   {
      public NorthwindDynamoDb()
      {
         super("dynamo", "northwind");
      }

      @Override
      public void configApi(Api api)
      {
         Collection northwind = getCollection("northwind");
         removeCollection(northwind);
         
         Collection orders = northwind.copy().withName("orders");
         withCollection(orders);
         
         orders.getPropertyByColumnName("hk").withJsonName("orderId"); //get orders by id 
         orders.getPropertyByColumnName("sk").withJsonName("type");

         orders.getPropertyByColumnName("gs1hk").withJsonName("employeeId"); //get orders by customer sorted by date
         orders.getPropertyByColumnName("gs1sk").withJsonName("orderDate");
         orders.getPropertyByColumnName("gs2hk").withJsonName("customerId");
         
         orders.getPropertyByColumnName("ls1").withJsonName("shipCity");
         orders.getPropertyByColumnName("ls2").withJsonName("shipName");
         orders.getPropertyByColumnName("ls3").withJsonName("requiredDate");
         
         super.configApi(api);
      }
   }

   protected static void rebuildNorthwind() throws Exception
   {
      try
      {
         AmazonDynamoDB client = DynamoDb.buildDynamoClient("dynamo");
         DeleteTableRequest dtr = new DeleteTableRequest().withTableName("northwind");
         client.deleteTable(dtr);
      }
      catch (Exception ex)
      {
         //ex.printStackTrace();
      }

      List<AttributeDefinition> attrs = new ArrayList<>();

      attrs.add(new AttributeDefinition().withAttributeName("hk").withAttributeType("N"));
      attrs.add(new AttributeDefinition().withAttributeName("sk").withAttributeType("S"));

      attrs.add(new AttributeDefinition().withAttributeName("gs1hk").withAttributeType("N"));
      attrs.add(new AttributeDefinition().withAttributeName("gs1sk").withAttributeType("S"));

      attrs.add(new AttributeDefinition().withAttributeName("gs2hk").withAttributeType("S"));
      //attrs.add(new AttributeDefinition().withAttributeName("gs2sk").withAttributeType("S"));  gs2sk is ls3

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
      gsxs.add(new GlobalSecondaryIndex().withIndexName("gs2").withKeySchema(new KeySchemaElement().withAttributeName("gs2hk").withKeyType(KeyType.HASH), new KeySchemaElement().withAttributeName("ls3").withKeyType(KeyType.RANGE)));
      gsxs.add(new GlobalSecondaryIndex().withIndexName("gs3").withKeySchema(new KeySchemaElement().withAttributeName("sk").withKeyType(KeyType.HASH), new KeySchemaElement().withAttributeName("hk").withKeyType(KeyType.RANGE)));

      for (LocalSecondaryIndex lsx : lsxs)
      {
         lsx.setProjection(new Projection().withProjectionType(ProjectionType.ALL));
      }

      for (GlobalSecondaryIndex gsx : gsxs)
      {
         gsx.setProjection(new Projection().withProjectionType(ProjectionType.ALL));
         gsx.withProvisionedThroughput(new ProvisionedThroughput()//
                                                                  .withReadCapacityUnits(50L)//
                                                                  .withWriteCapacityUnits(50L));
      }

      AmazonDynamoDB client = DynamoDb.buildDynamoClient("dynamo");
      DynamoDB dynamoDB = new DynamoDB(client);

      CreateTableRequest request = new CreateTableRequest()//
                                                           .withTableName("northwind")//
                                                           .withGlobalSecondaryIndexes(gsxs)//
                                                           .withLocalSecondaryIndexes(lsxs).withKeySchema(keys)//
                                                           .withAttributeDefinitions(attrs)//
                                                           .withProvisionedThroughput(new ProvisionedThroughput()//
                                                                                                                 .withReadCapacityUnits(50L)//
                                                                                                                 .withWriteCapacityUnits(50L));

      Table table = dynamoDB.createTable(request);

      try
      {
         table.waitForActive();
      }
      catch (Exception ex)
      {
         table.waitForActive();
      }

      Api h2Api = new Api("northwind");
      h2Api.withDb(JdbcDbFactory.bootstrapH2("dynamodbtesting"));
      h2Api.withEndpoint("*", "/*", new RestAction());
      Engine h2Engine = new Engine().withApi(h2Api);

      Engine dynamoEngine = new Engine().withApi(new Api("northwind")//
                                                                     .withDb(new NorthwindDynamoDb())//
                                                                     .withEndpoint("*", "/*", new RestAction()));

      System.out.println("");
      System.out.println("RELOADING DYNAMO...");

      Response res = null;
      int pages = 0;
      int total = 0;
      String start = "northwind/orders?pageSize=100&sort=orderid";
      String next = start;
      do
      {
         JSArray toPost = new JSArray();

         res = h2Engine.get(next);
         res.assertOk();
         if (res.getData().size() == 0)
            break;

         pages += 1;
         next = res.next();

         //-- now post to DynamoDb
         for (Object o : res.getData())
         {
            total += 1;
            JSNode js = (JSNode) o;

            js.remove("href");
            js.put("type", "ORDER");

            for (String key : js.keySet())
            {
               String value = js.getString(key);
               if (value != null && (value.startsWith("http://") || value.startsWith("https://")))
               {
                  value = value.substring(value.lastIndexOf("/") + 1, value.length());
                  js.remove(key);

                  if (!key.toLowerCase().endsWith("id"))
                     key = key + "Id";

                  js.put(key, value);
               }
            }
            toPost.add(js);
         }

         res = dynamoEngine.post("northwind/orders", toPost);
         res.dump();
         assertEquals(201, res.getStatusCode());
         System.out.println("DYNAMO LOADED: " + total);// + " - " + js.getString("orderid"));
      }
      while (pages < 200 && next != null);

   }

}
