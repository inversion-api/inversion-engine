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
import io.inversion.cloud.action.sql.SqlEngineFactory;
import io.inversion.cloud.model.Action;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.Collection;
import io.inversion.cloud.model.Endpoint;
import io.inversion.cloud.model.Entity;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Relationship;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.model.Table;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.service.Engine;
import io.inversion.cloud.utils.Utils;

public class CosmosEngineFactory
{
   static Engine  engine        = null;

   static boolean rebuildCosmos = true;

   protected static Engine engine() throws Exception
   {
      if (engine == null)
      {
         engine = SqlEngineFactory.service(false, true);

         CosmosDocumentDb cosmosdb = new CosmosDocumentDb("cosmos")
            {
               @Override
               public void configDb() throws Exception
               {
                  withDb("inversion-testing-cosmos1");
                  withCollectionPath("cosmosdb/");

                  Table customersTbl = new Table("customers").withActualName("Northwind")//

                                                             .withColumn("type", "string", false)//
                                                             .withColumn("customerId", "string")//
                                                             .withIndex("primaryIndex", "primary", true, "type", "customerId")//
                                                             .withIndex("PartitionKey", "PartitionKey", false, "type")//

                                                             .withColumn("companyName", "string")//
                                                             .withColumn("contactName", "string")//
                                                             .withColumn("contactTitle", "string")//
                                                             .withColumn("address", "string")//
                                                             .withColumn("city", "string")//
                                                             .withColumn("region", "string")//
                                                             .withColumn("postalCode", "string")//
                                                             .withColumn("country", "string")//
                                                             .withColumn("phone", "string")//
                                                             .withColumn("fax", "string");

                  Table employeesTbl = new Table("employees").withActualName("Northwind")//

                                                             .withColumn("type", "string", false)//
                                                             .withColumn("employeeId", "number")//
                                                             .withIndex("primaryIndex", "primary", true, "type", "employeeId")//
                                                             .withIndex("PartitionKey", "PartitionKey", false, "type")//

                                                             .withColumn("lastName", "string")//
                                                             .withColumn("firstName", "string")//
                                                             .withColumn("title", "string")//
                                                             .withColumn("titleOfCourtesy", "string")//
                                                             .withColumn("birthDate", "string")//
                                                             .withColumn("hireDate", "string")//
                                                             .withColumn("homePhone", "string")//
                                                             .withColumn("extension", "string")//
                                                             .withColumn("notes", "string")//
                                                             .withColumn("reportsTo", "number")//
                                                             .withColumn("salary", "number");

                  employeesTbl.withIndex("fkIdx_Employees_reportsTo", "foreignKey", false, "type", "reportsTo");
                  employeesTbl.getColumn("type").withPk(employeesTbl.getColumn("type"));
                  employeesTbl.getColumn("reportsTo").withPk(employeesTbl.getColumn("employeeId"));
                  

                  Table ordersTbl = new Table("orders").withActualName("Northwind")//

                                                       .withColumn("type", "string", false)//
                                                       .withColumn("orderId", "number")//
                                                       .withIndex("primaryIndex", "primary", true, "type", "orderId")//
                                                       .withIndex("PartitionKey", "PartitionKey", false, "type")//

                                                       //these are order fields
                                                       .withColumn("customerId", "string")//
                                                       .withColumn("employeeId", "number")//
                                                       .withColumn("orderDate", "string")//
                                                       .withColumn("requiredDate", "string")//
                                                       .withColumn("shippedDate", "string")//
                                                       //.withColumn("ShipVia", "number")//
                                                       .withColumn("freight", "number")//
                                                       .withColumn("shipName", "string")//
                                                       .withColumn("shipAddress", "string")//
                                                       .withColumn("shipCity", "string")//
                                                       .withColumn("shipRegion", "string")//
                                                       .withColumn("shipPostalCode", "string")//
                                                       .withColumn("shipCountry", "string");

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

                  withTable(customersTbl);
                  withTable(employeesTbl);
                  withTable(ordersTbl);
                  //withTable(orderDetailsTbl);

               }

               @Override
               public void configApi() throws Exception
               {
                  super.configApi();

                  Entity employeeEntity = getApi().getCollection("employees").getEntity();
                  employeeEntity.withRelationship(new Relationship("reportsTo", Relationship.REL_ONE_TO_MANY, employeeEntity, employeeEntity, getTable("employees").getIndex("fkIdx_Employees_reportsTo"), null));
                  employeeEntity.withRelationship(new Relationship("employees", Relationship.REL_MANY_TO_ONE, employeeEntity, employeeEntity, getTable("employees").getIndex("fkIdx_Employees_reportsTo"), null));
               }
            };

         final Api api = engine.getApi("northwind");
         api.withDb(cosmosdb);

         api.withEndpoint("GET,PUT,POST,DELETE", "cosmosdb/*", new Action()
            {
               public void run(Engine engine, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
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

         engine.startup();

         if (rebuildCosmos)
         {
            Engine e = engine;

            deleteAll(e, "/northwind/cosmosdb/orders");
            //deleteAll(e, "/northwind/cosmosdb/orderDetails");
            deleteAll(e, "/northwind/cosmosdb/customers");
            deleteAll(e, "/northwind/cosmosdb/employees");

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
               e.post("/northwind/cosmosdb/orders", order).assertOk();
            }

//            String getOrderDetails = "/northwind/source/orderdetails?in(orderid," + Utils.implode(",", orderIds) + ")";
//            res = e.get(getOrderDetails).assertOk();
//            for (JSNode node : res.data().asNodeList())
//            {
//               cleanSourceNode("orderDetails", node);
//               e.post("/northwind/cosmosdb/orderdetails", node).assertOk();
//            }

            String getCustomers = "/northwind/source/customers?in(customerid," + Utils.implode(",", customerIds) + ")";
            res = e.get(getCustomers).assertOk();
            for (JSNode customer : res.data().asNodeList())
            {
               cleanSourceNode("customers", customer);
               e.post("/northwind/cosmosdb/customers", customer).assertOk();
            }

            res = e.get("/northwind/source/employees").assertOk();
            for (JSNode employee : res.data().asNodeList())
            {
               employee.remove("employees");
               cleanSourceNode("employees", employee);
               e.post("/northwind/cosmosdb/employees", employee).assertOk().dump();
            }
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
            res = engine.delete(order.getString("href"));
            res.assertOk();
         }
         break;

      }
      while (res.findString("meta.next") != null);
   }

}
