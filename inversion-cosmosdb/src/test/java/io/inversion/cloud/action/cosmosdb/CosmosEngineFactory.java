package io.inversion.cloud.action.cosmosdb;

import java.util.HashSet;
import java.util.Set;

import io.inversion.cloud.action.rest.RestAction;
import io.inversion.cloud.action.sql.SqlEngineFactory;
import io.inversion.cloud.model.Action;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.ApiException;
import io.inversion.cloud.model.Collection;
import io.inversion.cloud.model.Endpoint;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.model.SC;
import io.inversion.cloud.model.Table;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.service.Engine;
import io.inversion.cloud.utils.Utils;

public class CosmosEngineFactory
{
   static Engine  engine        = null;

   static boolean rebuildCosmos = false;

   protected static Engine engine() throws Exception
   {
      if (engine == null)
      {
         engine = SqlEngineFactory.service(false, true);

         CosmosDocumentDb cosmosdb = new CosmosDocumentDb("cosmos");

         //this is false because we are going to manually create 
         //the tables and collections in this configuration
         cosmosdb.withBootstrap(false);
         cosmosdb.withDb("inversion-testing-cosmos1");
         cosmosdb.withCollectionPath("cosmosdb/");

         cosmosdb.withTable(new Table("People")//

                                               .withColumn("id", "string", false)//
                                               .withColumn("pk", "string", false)//
                                               .withColumn("type", "string", false)//
                                               //-- the 'href' value will be composed off of the primary index
                                               //-- the partionKey columns need to be encoded into the entityKey on the 
                                               //-- href...meaning make sure the partionKey field is part of the 
                                               //-- primary index.
                                               .withIndex("primaryIndex", "primary", true, "id", "pk")//
                                               .withIndex("PartitionKey", "PartitionKey", false, "pk")//

                                               //these are customer fields
                                               .withColumn("CustomerId", "string")//
                                               .withColumn("CompanyName", "string")//
                                               .withColumn("ContactName", "string")//
                                               .withColumn("ContactTitle", "string")//
                                               .withColumn("Address", "string")//
                                               .withColumn("City", "string")//
                                               .withColumn("Region", "string")//
                                               .withColumn("PostalCode", "string")//
                                               .withColumn("Country", "string")//
                                               .withColumn("Phone", "string")//
                                               .withColumn("Fax", "string")//

                                               //these are employee fields
                                               .withColumn("EmployeeID", "number")//
                                               .withColumn("LastName", "string")//
                                               .withColumn("FirstName", "string")//
                                               .withColumn("Title", "string")//
                                               .withColumn("TitleOfCourtesy", "string")//
                                               .withColumn("BirthDate", "string")//
                                               .withColumn("HireDate", "string")//
                                               .withColumn("HomePhone", "string")//
                                               .withColumn("Extension", "string")//
                                               .withColumn("Notes", "string")//
                                               .withColumn("ReportsTo", "number")//
                                               .withColumn("Salary", "number")//
         );

         cosmosdb.withTable(new Table("Orders")//

                                               .withColumn("id", "string", false)//
                                               .withColumn("type", "string", false)//
                                               .withColumn("pk", "string", false)//
                                               .withIndex("primaryIndex", "primary", true, "id", "pk")//
                                               .withIndex("PartitionKey", "PartitionKey", false, "pk")//

                                               //these are order fields
                                               .withColumn("OrderID", "number")//
                                               .withColumn("EmployeeID", "number")//
                                               .withColumn("OrderDate", "string")//
                                               .withColumn("RequiredDate", "string")//
                                               .withColumn("ShippedDate", "string")//
                                               .withColumn("ShipVia", "number")//
                                               .withColumn("Freight", "number")//
                                               .withColumn("ShipName", "string")//
                                               .withColumn("ShipAddress", "string")//
                                               .withColumn("ShipCity", "string")//
                                               .withColumn("ShipRegion", "string")//
                                               .withColumn("ShipPostalCode", "string")//
                                               .withColumn("ShipCountry", "string")//

                                               //these are orderdetails fields

                                               .withColumn("ProductID", "number")//
                                               .withColumn("UnitPrice", "number")//
                                               .withColumn("Quantity", "number")//
                                               .withColumn("Discount", "number")//
         );

         final Api api = engine.getApi("northwind");

         api.withDb(cosmosdb);
         api.withCollection(new Collection(api.findTable("Orders")).withName("orders"));
         api.withCollection(new Collection(api.findTable("Orders")).withName("orderDetails"));

         api.withCollection(new Collection(api.findTable("People")).withName("customers"));
         api.withCollection(new Collection(api.findTable("People")).withName("employees"));

         api.withEndpoint("GET,PUT,POST,DELETE", "cosmosdb/*", new Action()
            {
               public void run(Engine engine, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
               {
                  String collectionKey = req.getCollectionKey().toLowerCase();

                  if (req.isGet())
                     req.withParam("type", collectionKey);

                  JSNode json = req.getJson();
                  if (json != null)
                  {
                     json.asNodeList().forEach(node -> node.put("type", collectionKey));

                     if ("orders".equals(collectionKey))
                        json.asNodeList().forEach(node -> {
                           node.put("id", "order-" + node.get("orderid"));
                           node.put("pk", node.getString("orderid"));
                        });

                     else if ("orderdetails".equals(collectionKey))
                        json.asNodeList().forEach(node -> {
                           node.put("id", "orderdetails-" + node.get("orderid"));
                           node.put("pk", node.getString("orderid"));
                        });

                     else if ("customers".equals(collectionKey))
                        json.asNodeList().forEach(node -> {
                           node.put("id", "customer-" + node.get("customerid"));
                           node.put("pk", "customers");
                        });

                     else if ("employees".equals(collectionKey))
                        json.asNodeList().forEach(node -> {
                           node.put("id", "employee-" + node.get("employeeid"));
                           node.put("pk", "employees");
                        });

                     else
                        throw new ApiException(SC.SC_400_BAD_REQUEST, "Unsupported collection: " + collectionKey);

                     System.out.println(json);

                  }
               }
            }//
               , new RestAction());

         engine.startup();

         if (rebuildCosmos)
         {
            Engine e = engine;

            deleteAll(e, "/northwind/cosmosdb/orders");
            deleteAll(e, "/northwind/cosmosdb/orderDetails");
            deleteAll(e, "/northwind/cosmosdb/customers");
            deleteAll(e, "/northwind/cosmosdb/employees");

            Response res = null;

            //-- reload cosmos
            res = e.get("/northwind/source/orders?limit=25").statusOk();

            Set orderIds = new HashSet();
            Set customerIds = new HashSet();

            for (JSNode node : res.data().asNodeList())
            {
               cleanSourceNode(node);

               orderIds.add(node.get("orderid"));
               customerIds.add(node.get("customerid"));

               e.post("/northwind/cosmosdb/orders", node).statusOk();
            }

            String getOrderDetails = "/northwind/source/orderdetails?in(orderid," + Utils.implode(",", orderIds) + ")";
            res = e.get(getOrderDetails).statusOk();
            for (JSNode node : res.data().asNodeList())
            {
               cleanSourceNode(node);
               e.post("/northwind/cosmosdb/orderdetails", node).statusOk();
            }

            String getCustomers = "/northwind/source/customers?in(customerid," + Utils.implode(",", customerIds) + ")";
            res = e.get(getCustomers).statusOk();
            for (JSNode node : res.data().asNodeList())
            {
               cleanSourceNode(node);
               e.post("/northwind/cosmosdb/customers", node).statusOk();
            }

            res = e.get("/northwind/source/employees").statusOk();
            for (JSNode node : res.data().asNodeList())
            {
               cleanSourceNode(node);
               e.post("/northwind/cosmosdb/employees", node).statusOk();
            }
         }

      }
      return engine;
   }

   /**
    * Removes 'href' and turns relationship hrefs back into their key value
    * @param node
    */
   public static void cleanSourceNode(JSNode node)
   {
      node.remove("href");

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

         res = e.get(url).statusOk();
         for (JSNode order : res.data().asNodeList())
         {
            res = engine.delete(order.getString("href")).statusOk();
            res.dump();
         }
         break;

      }
      while (res.findString("meta.next") != null);
   }

}
