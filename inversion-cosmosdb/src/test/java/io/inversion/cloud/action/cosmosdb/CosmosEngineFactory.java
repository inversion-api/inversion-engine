package io.inversion.cloud.action.cosmosdb;

import io.inversion.cloud.action.rest.RestAction;
import io.inversion.cloud.action.sql.SqlEngineFactory;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.Table;
import io.inversion.cloud.service.Engine;

public class CosmosEngineFactory
{
   protected static Engine engine = null;

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
                  super.configDb();
                  //                  withTable(new Table(this, "person").withColumn("personId", "string")//
                  //                                                     .withColumn("type", "string")//                               
                  //                                                     .withColumn("title", "string")//
                  //                                                     .withColumn("firstName", "string")//
                  //                                                     .withColumn("lastName", "string")//
                  //                                                     .withColumn("suffix", "string")//
                  //
                  //                                                     .withColumn("dateOfBirth", "string")//
                  //                  );//
                  //
                  //                  withTable(new Table(this, "person.identifier")//
                  //                                                                .withColumn("identifierId", "string")//
                  //                                                                .withColumn("customerId", "string")//
                  //                                                                .withColumn("title", "string")//
                  //                                                                .withColumn("providerCode", "string")//
                  //                                                                .withColumn("type", "string")//
                  //                                                                .withColumn("identifier", "string")//
                  //                  );//
                  //
                  //                  withTable(new Table(this, "order")//
                  //                                                    .withColumn("CustomerID", "string")//
                  //                                                    .withColumn("EmployeeID", "string")//
                  //                                                    .withColumn("OrderDate", "string")//
                  //                                                    .withColumn("RequiredDate", "string")//
                  //                                                    .withColumn("ShippedDate", "string")//
                  //                                                    .withColumn("ShipVia", "string")//
                  //                                                    .withColumn("Freight", "string")//
                  //                                                    .withColumn("ShipName", "string")//
                  //                                                    .withColumn("ShipAddress", "string")//
                  //                                                    .withColumn("ShipCity", "string")//
                  //                                                    .withColumn("ShipRegion", "string")//
                  //                                                    .withColumn("ShipPostalCode", "string")//
                  //                                                    .withColumn("ShipCountry", "string")//
                  //                  );//
               }

               @Override
               public void configApi() throws Exception
               {
                  super.configApi();
               }
            };

         cosmosdb.withDb("inversion-testing-cosmos1");
         cosmosdb.withIncludeTables("People|persons,Orders");
         cosmosdb.withCollectionPath("cosmosdb/");
         cosmosdb.withTable(new Table("Orders")//
                                               .withColumns("id|string,orderid|number||,shipcountry||false|true")//
                                               .withIndex("pk", "primary", true, "id")
         //.withColumn("ShipCountry", "string", false, false)//
         ///.withColumn)

         );

         //         cosmosdb.withTable("Order", "orderid|number,ShipCountry|string,"))
         //         cosmosdb.withTable("Order", "orderid,shipCountry,somethingelse|type|other"))

         //         Table orders = cosmosdb.getTable("Orders");
         //         Column orderId = orders.getColumn("orderId");
         //         Index pk = new Index(orders, orderId, "pk_orders_orderid", "primary", true);
         //         orders.withIndex(pk);

         //cosmosdb.withUri("https://inversion-testing-account.documents.azure.com:443/");
         //cosmosdb.withKey("Agf752hPuJ7SXFJdIQdBVdDQFBoIzbPyqA7ZM6TvOaaYbBgRnNYLAfd1Orsq7dMDRDSoElWuA9fGHWAXdaeqMw==");

         final Api api = engine.getApi("northwind");

         api.withDb(cosmosdb);
         api.withEndpoint("GET,PUT,POST,DELETE", "cosmosdb/*", new RestAction());

         engine.startup();
      }
      return engine;

   }
}
