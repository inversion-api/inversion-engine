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
package io.inversion.dynamodb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.*;
import io.inversion.Api;
import io.inversion.Collection;
import io.inversion.Engine;
import io.inversion.Response;
import io.inversion.action.db.DbAction;
import io.inversion.jdbc.JdbcDbFactory;
import io.inversion.json.JSList;
import io.inversion.json.JSNode;
import io.inversion.utils.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DynamoDbFactory {

    public static void main(String[] args) throws Exception {
        buildNorthwindDynamoDb();
    }

    protected static DynamoDb buildNorthwindDynamoDb() {
        rebuildNorthwind();
        return new NorthwindDynamoDb();
    }

    protected static void rebuildNorthwind() {
        try {
            DynamoDB dynamoDB = new DynamoDB(DynamoDb.buildDynamoClient("dynamo"));
            Table    table    = dynamoDB.getTable("northwind");
            if (table != null) {
                table.delete();
                table.waitForDelete();
            }
            //DeleteTableRequest dtr = new DeleteTableRequest().withTableName("northwind");
            //DeleteTableResult dtres = client.deleteTable(dtr);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {

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

            List<LocalSecondaryIndex> lsxs = new ArrayList<>();
            lsxs.add(new LocalSecondaryIndex().withIndexName("ls1").withKeySchema(new KeySchemaElement().withAttributeName("hk").withKeyType(KeyType.HASH)//
                    , new KeySchemaElement().withAttributeName("ls1").withKeyType(KeyType.RANGE)));

            lsxs.add(new LocalSecondaryIndex().withIndexName("ls2").withKeySchema(new KeySchemaElement().withAttributeName("hk").withKeyType(KeyType.HASH)//
                    , new KeySchemaElement().withAttributeName("ls2").withKeyType(KeyType.RANGE)));

            lsxs.add(new LocalSecondaryIndex().withIndexName("ls3").withKeySchema(new KeySchemaElement().withAttributeName("hk").withKeyType(KeyType.HASH)//
                    , new KeySchemaElement().withAttributeName("ls3").withKeyType(KeyType.RANGE)));

            List<GlobalSecondaryIndex> gsxs = new ArrayList<>();
            gsxs.add(new GlobalSecondaryIndex().withIndexName("gs1").withKeySchema(new KeySchemaElement().withAttributeName("gs1hk").withKeyType(KeyType.HASH), new KeySchemaElement().withAttributeName("gs1sk").withKeyType(KeyType.RANGE)));
            gsxs.add(new GlobalSecondaryIndex().withIndexName("gs2").withKeySchema(new KeySchemaElement().withAttributeName("gs2hk").withKeyType(KeyType.HASH), new KeySchemaElement().withAttributeName("ls3").withKeyType(KeyType.RANGE)));
            gsxs.add(new GlobalSecondaryIndex().withIndexName("gs3").withKeySchema(new KeySchemaElement().withAttributeName("sk").withKeyType(KeyType.HASH), new KeySchemaElement().withAttributeName("hk").withKeyType(KeyType.RANGE)));

            for (LocalSecondaryIndex lsx : lsxs) {
                lsx.setProjection(new Projection().withProjectionType(ProjectionType.ALL));
            }

            for (GlobalSecondaryIndex gsx : gsxs) {
                gsx.setProjection(new Projection().withProjectionType(ProjectionType.ALL));
                gsx.withProvisionedThroughput(new ProvisionedThroughput()//
                        .withReadCapacityUnits(50L)//
                        .withWriteCapacityUnits(50L));
            }

            AmazonDynamoDB client   = DynamoDb.buildDynamoClient("dynamo");
            DynamoDB       dynamoDB = new DynamoDB(client);

            CreateTableRequest request = new CreateTableRequest()//
                    .withTableName("northwind")//
                    .withGlobalSecondaryIndexes(gsxs)//
                    .withLocalSecondaryIndexes(lsxs).withKeySchema(keys)//
                    .withAttributeDefinitions(attrs)//
                    .withProvisionedThroughput(new ProvisionedThroughput()//
                            .withReadCapacityUnits(50L)//
                            .withWriteCapacityUnits(50L));

            Table table = dynamoDB.createTable(request);

            try {
                table.waitForActive();
            } catch (Exception ex) {
                table.waitForActive();
            }

            Api h2Api = new Api("northwind");
            h2Api.withDb(JdbcDbFactory.bootstrapH2("dynamodbtesting" + System.currentTimeMillis()));
            h2Api.withEndpoint(new DbAction());
            Engine h2Engine = new Engine().withApi(h2Api);

            Engine dynamoEngine = new Engine().withApi(new Api("northwind")//
                    .withDb(new NorthwindDynamoDb())//
                    .withEndpoint(new DbAction()));

            System.out.println();
            System.out.println("RELOADING DYNAMO...");

            int empShipRegion = 0;

            Response res;
            int      pages = 0;
            int      total = 0;
            String   start = "northwind/orders?pageSize=100&sort=orderid";
            String   next  = start;
            do {
                JSList toPost = new JSList();

                res = h2Engine.get(next);
                res.assertOk();
                if (res.data().size() == 0)
                    break;

                pages += 1;
                next = res.getNext();

                //-- now post to DynamoDb
                for (Object o : res.data()) {
                    total += 1;
                    JSNode js = (JSNode) o;

                    js.remove("href");
                    js.put("type", "ORDER");

                    if (Utils.empty(js.findString("shipregion"))) {
                        empShipRegion += 1;
                    }

                    for (String key : js.keySet()) {
                        String value = js.getString(key);
                        if (value != null && (value.startsWith("http://") || value.startsWith("https://"))) {
                            value = value.substring(value.lastIndexOf("/") + 1);
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
            } while (pages < 200 && next != null);

            h2Engine.shutdown();

            System.out.println("EMPTY SHIP REGION = " + empShipRegion + " out of " + total);
            System.out.println("...FINISHED RELOADING");
            System.out.println("...FINISHED RELOADING");
            System.out.println("...FINISHED RELOADING");
            System.out.println("...FINISHED RELOADING");
            System.out.println("...FINISHED RELOADING");
            System.out.println("...FINISHED RELOADING");

        } catch (Exception ex) {
            Utils.rethrow(ex);
        }

    }

    public static class NorthwindDynamoDb extends DynamoDb implements Serializable {

        public NorthwindDynamoDb() {
            super("dynamo", "northwind");
        }

        @Override
        public void configApi(Api api) {
            Collection northwind = getCollectionByTableName("northwind");
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

}
