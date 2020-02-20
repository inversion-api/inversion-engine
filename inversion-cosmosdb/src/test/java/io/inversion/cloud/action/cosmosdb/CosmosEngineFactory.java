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
package io.inversion.cloud.action.cosmosdb;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.inversion.cloud.action.rest.RestAction;
import io.inversion.cloud.model.Action;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.Collection;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Relationship;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.service.Engine;
import io.inversion.cloud.utils.Utils;

public class CosmosEngineFactory
{
   protected static Engine buildEngine() throws Exception
   {
      CosmosDocumentDb cosmosdb = new CosmosDocumentDb("cosmos")
         {
            @Override
            public void configDb() throws Exception
            {
               withDb("inversion-testing-cosmos1");
               withCollectionPath("cosmos/");

               Collection customersTbl = new Collection("customers").withTableName("Northwind")//

                                                                    .withProperty("type", "string", false)//
                                                                    .withProperty("customerId", "string")//
                                                                    .withIndex("primaryIndex", "primary", true, "type", "customerId")//
                                                                    .withIndex("PartitionKey", "PartitionKey", false, "type")//

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

               Collection employeesTbl = new Collection("employees").withName("Northwind")//

                                                                    .withProperty("type", "string", false)//
                                                                    .withProperty("employeeId", "number")//
                                                                    .withIndex("primaryIndex", "primary", true, "type", "employeeId")//
                                                                    .withIndex("PartitionKey", "PartitionKey", false, "type")//

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

               employeesTbl.withIndex("fkIdx_Employees_reportsTo", "foreignKey", false, "type", "reportsTo");
               employeesTbl.getProperty("type").withPk(employeesTbl.getProperty("type"));
               employeesTbl.getProperty("reportsTo").withPk(employeesTbl.getProperty("employeeId"));

               Collection ordersTbl = new Collection("orders").withTableName("Northwind")//

                                                              .withProperty("type", "string", false)//
                                                              .withProperty("orderId", "number")//
                                                              .withIndex("primaryIndex", "primary", true, "type", "orderId")//
                                                              .withIndex("PartitionKey", "PartitionKey", false, "type")//

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
               //                                                                   .withIndex("PartitionKey", "PartitionKey", false, "type")//
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
            public void configApi() throws Exception
            {
               super.configApi();

               Collection employeesTbl = getApi().getCollection("employees");
               employeesTbl.withRelationship(new Relationship("reportsTo", Relationship.REL_ONE_TO_MANY, employeesTbl, employeesTbl, getCollection("employees").getIndex("fkIdx_Employees_reportsTo"), null));
               employeesTbl.withRelationship(new Relationship("employees", Relationship.REL_MANY_TO_ONE, employeesTbl, employeesTbl, getCollection("employees").getIndex("fkIdx_Employees_reportsTo"), null));
            }
         };

      final Api api = new Api("northwind");

      api.withDb(cosmosdb);
      api.withEndpoint("GET,PUT,POST,DELETE", "cosmos/*", new Action()
         {
            public void run(Request req, Response res) throws Exception
            {
               String collectionKey = req.getCollectionKey().toLowerCase();

               if (req.isGet())
               {
                  req.withParam("Type", collectionKey.toLowerCase());
               }

               JSNode json = req.getJson();
               if (json != null)
               {
                  json.asNodeList().forEach(node -> node.put("type", collectionKey.toLowerCase()));
               }
            }
         }//
            , new RestAction());

      Engine engine = new Engine(api);

      //if (rebuildCosmos)
      {
         Engine e = engine;

         deleteAll(e, "/northwind/cosmos/orders");
         //deleteAll(e, "/northwind/cosmos/orderDetails");
         deleteAll(e, "/northwind/cosmos/customers");
         deleteAll(e, "/northwind/cosmos/employees");

         Response res = null;

         //-- reload cosmos

         res = e.get("/northwind/source/orders?limit=25").assertOk();

         res.dump();
         Set orderIds = new HashSet();
         Set customerIds = new HashSet();

         for (JSNode order : res.data().asNodeList())
         {
            cleanSourceNode("orders", order);

            orderIds.add(order.get("orderid"));
            customerIds.add(order.get("customerid"));

            res = e.get("/northwind/source/orderDetails?orderId=" + order.get("orderid"));

            for (JSNode details : res.data().asNodeList())
            {
               cleanSourceNode("orderDetails", details);
               details.remove("employees");
               details.remove("order");
               details.remove("orderid");
            }
            order.put("orderDetails", res.data());
            e.post("/northwind/cosmos/orders", order).assertOk();
         }

         //            String getOrderDetails = "/northwind/source/orderdetails?in(orderid," + Utils.implode(",", orderIds) + ")";
         //            res = e.get(getOrderDetails).assertOk();
         //            for (JSNode node : res.data().asNodeList())
         //            {
         //               cleanSourceNode("orderDetails", node);
         //               e.post("/northwind/cosmos/orderdetails", node).assertOk();
         //            }

         String getCustomers = "/northwind/source/customers?in(customerid," + Utils.implode(",", customerIds) + ")";
         res = e.get(getCustomers).assertOk();
         for (JSNode customer : res.data().asNodeList())
         {
            cleanSourceNode("customers", customer);
            e.post("/northwind/cosmos/customers", customer).assertOk();
         }

         res = e.get("/northwind/source/employees").assertOk();
         for (JSNode employee : res.data().asNodeList())
         {
            employee.remove("employees");
            cleanSourceNode("employees", employee);
            e.post("/northwind/cosmos/employees", employee).assertOk().dump();
         }
      }

      return engine;
   }

   /**
    * Removes 'href' and turns relationship hrefs back into their key value
    * @param node
    */
   public static void cleanSourceNode(String collection, JSNode node)
   {
      node.remove("href");
      node.remove("employee");

      if ("employees".equalsIgnoreCase(collection))
      {
         String reportsTo = node.getString("reportsTo");
         if (!Utils.empty(reportsTo))
         {
            List parts = Arrays.asList("employees", "employee-" + reportsTo.substring(reportsTo.lastIndexOf("/") + 1));
            reportsTo = reportsTo.substring(0, reportsTo.lastIndexOf("/") + 1) + "employees~" + reportsTo.substring(reportsTo.lastIndexOf("/") + 1);

            node.put("reportsTo", reportsTo);
         }
      }

      for (String key : node.keySet())
      {
         Object value = node.get(key);
         if (value instanceof String)
         {
            String str = (String) value;
            if (str.startsWith("http://"))
               node.put(key, Utils.last(Utils.explode("/", str)));
         }
      }
   }

   protected static void deleteAll(Engine e, String url)
   {
      int safetyCounter = 0;
      Response res = null;
      do
      {
         safetyCounter += 1;

         if (safetyCounter > 2000)
            throw new RuntimeException("Something is not right, your delete seems to be stuck in an infinate loop.");

         res = e.get(url).assertOk();
         for (JSNode order : res.data().asNodeList())
         {
            res = e.delete(order.getString("href"));
            res.assertOk();
         }
         break;

      }
      while (res.findString("meta.next") != null);
   }

}
