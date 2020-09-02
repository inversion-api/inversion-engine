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

import io.inversion.*;
import io.inversion.action.db.DbAction;
import io.inversion.jdbc.JdbcDbFactory;
import io.inversion.utils.JSNode;
import io.inversion.utils.Path;
import io.inversion.utils.Utils;

import java.util.HashSet;
import java.util.Set;

public class CosmosDbFactory {

    protected static CosmosDb buildDb() {
        return new NorthwindCosmosDb();
    }

    public static Engine buildEngine() {
        return buildEngine(new NorthwindCosmosDb());
    }

    public static Engine buildEngine(CosmosDb cosmosdb) {
        final Api api = new Api("northwind");

        api.withDb(cosmosdb);
        api.withEndpoint("GET,PUT,POST,DELETE", "cosmosdb/*", new Action() {

                    public void run(Request req, Response res) throws ApiException {
                        String collectionKey = req.getCollectionKey().toLowerCase();

                        if (req.isGet()) {
                            req.getUrl().withParam("Type", collectionKey.toLowerCase());
                        }

                        JSNode json = req.getJson();
                        if (json != null) {
                            json.asNodeList().forEach(node -> node.put("type", collectionKey.toLowerCase()));
                        }
                    }
                }//
                , new DbAction());

        Engine dstEngine = new Engine(api);

        Engine srcEngine = new Engine().withApi(new Api("northwind") //
                .withEndpoint("*", "source" + "/*", new DbAction())//
                .withDb(JdbcDbFactory.bootstrapH2("cosmos_source")));

        //if (rebuildCosmos)
        {
            deleteAll(dstEngine, "/northwind/cosmosdb/orders");
            //deleteAll(e, "/northwind/cosmosdb/orderDetails");
            deleteAll(dstEngine, "/northwind/cosmosdb/customers");
            deleteAll(dstEngine, "/northwind/cosmosdb/employees");

            //-- reload cosmos

            Response res = srcEngine.get("/northwind/source/orders?limit=25").assertOk();

            res.dump();
            //Set<Object> orderIds = new HashSet<>();
            Set<Object> customerIds = new HashSet<>();

            for (JSNode order : res.getData().asNodeList()) {
                cleanSourceNode("orders", order);

                //orderIds.add(order.get("orderid"));
                customerIds.add(order.get("customerid"));

                res = srcEngine.get("/northwind/source/orderDetails?orderId=" + order.get("orderid"));

                for (JSNode details : res.getData().asNodeList()) {
                    cleanSourceNode("orderDetails", details);
                    details.remove("employees");
                    details.remove("order");
                    details.remove("orderid");
                }
                order.put("orderDetails", res.getData());
                dstEngine.post("/northwind/cosmosdb/orders", order).assertOk();
            }

            //            String getOrderDetails = "/northwind/source/orderdetails?in(orderid," + Utils.implode(",", orderIds) + ")";
            //            res = e.get(getOrderDetails).assertOk();
            //            for (JSNode node : res.data().asNodeList())
            //            {
            //               cleanSourceNode("orderDetails", node);
            //               e.post("/northwind/cosmosdb/orderdetails", node).assertOk();
            //            }

            String getCustomers = "/northwind/source/customers?in(customerid," + Utils.implode(",", customerIds) + ")";
            res = srcEngine.get(getCustomers).assertOk();
            for (JSNode customer : res.getData().asNodeList()) {
                cleanSourceNode("customers", customer);
                dstEngine.post("/northwind/cosmosdb/customers", customer).assertOk();
            }

            res = srcEngine.get("/northwind/source/employees").assertOk();
            for (JSNode employee : res.getData().asNodeList()) {
                employee.remove("employees");
                cleanSourceNode("employees", employee);
                dstEngine.post("/northwind/cosmosdb/employees", employee).assertOk();
            }
        }

        return dstEngine;
    }

    /**
     * Removes 'href' and turns relationship hrefs back into their key value
     *
     * @param collection the collection being operated on
     * @param node       the node to clean
     */
    public static void cleanSourceNode(String collection, JSNode node) {
        node.remove("href");
        node.remove("employee");

        if ("employees".equalsIgnoreCase(collection)) {
            String reportsTo = node.getString("reportsTo");
            if (!Utils.empty(reportsTo)) {
                //List parts = Arrays.asList("employees", "employee-" + reportsTo.substring(reportsTo.lastIndexOf("/") + 1));
                reportsTo = reportsTo.substring(0, reportsTo.lastIndexOf("/") + 1) + "employees~" + reportsTo.substring(reportsTo.lastIndexOf("/") + 1);

                node.put("reportsTo", reportsTo);
            }
        }

        for (String key : node.keySet()) {
            Object value = node.get(key);
            if (value instanceof String) {
                String str = (String) value;
                if (str.startsWith("http://"))
                    node.put(key, Utils.last(Utils.explode("/", str)));
            }
        }
    }

    protected static void deleteAll(Engine e, String url) {
        int      safetyCounter = 0;
        Response res;
        do {
            safetyCounter += 1;

            if (safetyCounter > 2000)
                throw new RuntimeException("Something is not right, your delete seems to be stuck in an infinite loop.");

            res = e.get(url).assertOk();

            if (res.getData().size() == 0)
                break;

            for (JSNode order : res.getData().asNodeList()) {
                res = e.delete(order.getString("href"));
                res.assertOk();
            }
        } while (res.findString("meta.next") != null);
    }

    static class NorthwindCosmosDb extends CosmosDb {

        public NorthwindCosmosDb() {
            withName("cosmos");
        }

        @Override
        public void configDb() throws ApiException {
            withDb("inversion-testing-cosmos1");
            withEndpointPath(new Path("cosmosdb/[:type]/*"));

            Collection customersTbl = new Collection("customers").withTableName("Northwind")//

                    .withProperty("type", "string", false)//
                    .withProperty("customerId", "string")//
                    .withIndex("primaryIndex", "primary", true, "type", "customerId")//
                    .withIndex(CosmosDb.INDEX_TYPE_PARTITION_KEY, CosmosDb.INDEX_TYPE_PARTITION_KEY, false, "type")//

                    .withProperty("companyName", "string")//
                    .withProperty("contactName", "string")//
                    .withProperty("contactTitle", "string")//
                    .withProperty("address", "string")//
                    .withProperty("city", "string")//
                    .withProperty("region", "string")//
                    .withProperty("postalCode", "string")//
                    .withProperty("country", "string")//
                    .withProperty("phone", "string")//
                    .withProperty("fax", "string");

            Collection employeesTbl = new Collection("employees").withTableName("Northwind")//

                    .withProperty("type", "string", false)//
                    .withProperty("employeeId", "number")//
                    .withIndex("primaryIndex", "primary", true, "type", "employeeId")//
                    .withIndex(CosmosDb.INDEX_TYPE_PARTITION_KEY, CosmosDb.INDEX_TYPE_PARTITION_KEY, false, "type")//

                    .withProperty("lastName", "string")//
                    .withProperty("firstName", "string")//
                    .withProperty("title", "string")//
                    .withProperty("titleOfCourtesy", "string")//
                    .withProperty("birthDate", "string")//
                    .withProperty("hireDate", "string")//
                    .withProperty("homePhone", "string")//
                    .withProperty("extension", "string")//
                    .withProperty("notes", "string")//
                    .withProperty("reportsTo", "number")//
                    .withProperty("salary", "number");

            //         employeesTbl.withIndex("fkIdx_Employees_reportsTo", "FOREIGN_KEY", false, "type", "reportsTo");
            //         employeesTbl.getProperty("type").withPk(employeesTbl.getProperty("type"));
            //         employeesTbl.getProperty("reportsTo").withPk(employeesTbl.getProperty("employeeId"));

            Collection ordersTbl = new Collection("orders").withTableName("Northwind")//

                    .withProperty("type", "string", false)//
                    .withProperty("orderId", "number")//
                    .withIndex("primaryIndex", "primary", true, "type", "orderId")//
                    .withIndex(CosmosDb.INDEX_TYPE_PARTITION_KEY, CosmosDb.INDEX_TYPE_PARTITION_KEY, false, "type")//

                    //these are order fields
                    .withProperty("customerId", "string")//
                    .withProperty("employeeId", "number")//
                    .withProperty("orderDate", "string")//
                    .withProperty("requiredDate", "string")//
                    .withProperty("shippedDate", "string")//
                    //.withColumn("ShipVia", "number")//
                    .withProperty("freight", "number")//
                    .withProperty("shipName", "string")//
                    .withProperty("shipAddress", "string")//
                    .withProperty("shipCity", "string")//
                    .withProperty("shipRegion", "string")//
                    .withProperty("shipPostalCode", "string")//
                    .withProperty("shipCountry", "string");

            //                  Table orderDetailsTbl = new Table("orderDetails").withActualName("Northwind")//
            //
            //                                                                   .withColumn("type", "string", false)//
            //                                                                   .withColumn("orderId", "number")//
            //                                                                   .withIndex("primaryIndex", "primary", true, "type", "orderId")//
            //                                                                   .withIndex(CosmosDb.INDEX_TYPE_PARTITION_KEY, CosmosDb.INDEX_TYPE_PARTITION_KEY, false, "type")//
            //
            //                                                                   .withColumn("productId", "number")//
            //                                                                   .withColumn("unitPrice", "number")//
            //                                                                   .withColumn("quantity", "number")//
            //                                                                   .withColumn("discount", "number");

            withCollection(customersTbl);
            withCollection(employeesTbl);
            withCollection(ordersTbl);
            //withTable(orderDetailsTbl);

        }

        @Override
        public void configApi(Api api) {
            super.configApi(api);

            //               Collection employeesTbl = api.getCollection("employees");
            //               employeesTbl.withRelationship(new Relationship("reportsTo", Relationship.REL_MANY_TO_ONE, employeesTbl, employeesTbl, getCollection("employees").getIndex("fkIdx_Employees_reportsTo"), null));
            //               employeesTbl.withRelationship(new Relationship("employees", Relationship.ONE_TO_MANY, employeesTbl, employeesTbl, getCollection("employees").getIndex("fkIdx_Employees_reportsTo"), null));

            api.withRelationship("employees", "employees", "employees", "reportsTo", "reportsTo");
        }

        //      collection.withRelationship(name, field1, field2, field3, targetCollection, field1, field2, field3)

        //      collection.withRelationship("relationshipName")

        //      //many-to-one
        //      childCollection.withRelationship("mayParent", "/parent/${childForeignKeyField}")
        //
        //      //one-to-many
        //      parentCollection.withRelationship("myChildren", "/child?childForeignKeyField=${parentPrimaryKeyField}")
        //
        //      //many-to-many
        //      peerCollection.withRelationship("myPeers", "/peerA?peerAToPeerB.peerBId={peerBId}")

        //      employeesTbl.withRelationship(new Relationship("reportsTo", Relationship.REL_MANY_TO_ONE, employeesTbl, employeesTbl, getCollection("employees").getIndex("fkIdx_Employees_reportsTo"), null));
    }

}
