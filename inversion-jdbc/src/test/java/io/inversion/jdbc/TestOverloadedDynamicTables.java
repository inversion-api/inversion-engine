package io.inversion.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import io.inversion.Action;
import io.inversion.Api;
import io.inversion.ApiException;
import io.inversion.Chain;
import io.inversion.Collection;
import io.inversion.Db;
import io.inversion.Engine;
import io.inversion.Index;
import io.inversion.Request;
import io.inversion.Response;
import io.inversion.action.rest.RestAction;
import io.inversion.jdbc.JdbcDb;
import io.inversion.jdbc.JdbcUtils;
import io.inversion.jdbc.JdbcDb.JdbcConnectionLocal;
import io.inversion.utils.JSArray;
import io.inversion.utils.JSNode;
import io.inversion.utils.Rows.Row;
import io.inversion.utils.Utils;

@TestInstance(Lifecycle.PER_CLASS)
public class TestOverloadedDynamicTables
{
   Engine engine = null;
   Api    api    = null;
   Db     db     = null;

   @BeforeAll
   public void beforeAll_initializeEngine()
   {
      Chain.resetAll();
      JdbcConnectionLocal.closeAll();
      
      
      db = new JdbcDb("db", "org.h2.Driver", //
                      "jdbc:h2:mem:" + System.currentTimeMillis() + ";IGNORECASE=TRUE;DB_CLOSE_DELAY=-1", //
                      "sa", //
                      "", //
                      JdbcDb.class.getResource("dynamic-h2.ddl").toString())
         {

            @Override
            public void configDb() throws Exception
            {
               super.configDb();
               Collection object = getCollectionByTableName("Object");
               removeCollection(object);

               Collection addresses = object.copy().withName("addresses");
               addresses.getProperty("value1").withJsonName("customer");
               addresses.getProperty("value2").withJsonName("alias");
               addresses.getProperty("value3").withJsonName("address1");
               addresses.getProperty("value4").withJsonName("address2");
               addresses.getProperty("value5").withJsonName("city");
               addresses.getProperty("value6").withJsonName("state");
               addresses.getProperty("value7").withJsonName("zip");
               addresses.withIndex("PK", "primary", true, "partition", "id");
               addresses.withIndex("PartitionKey", "PartitionKey", false, "partition");

               Collection customers = object.copy().withName("customers");
               customers.getProperty("value1").withJsonName("firstName");
               customers.getProperty("value2").withJsonName("lastName");
               customers.withIndex("PK", "primary", true, "partition", "id");
               customers.withRelationship("addresses", addresses, "customer", "customer");
               customers.withIndex("PartitionKey", "PartitionKey", false, "partition");

               withCollection(addresses);
               withCollection(customers);

            }

            protected void doShutdown()
            {
               try
               {
                  JdbcUtils.execute(getConnection(), "SHUTDOWN");
               }
               catch (Exception ex)
               {

               }
               super.doShutdown();
            }
         };

      api = new Api("crm").withIncludeOn(null, "crm/:tenant/[:type]/*") //
                          .withEndpoint("*", "*", new Action()
                             {
                                public void run(Request req, Response res) throws Exception
                                {
                                   //-- requires "type" param and forces to lower case
                                   String tenant = req.getUrl().getParam("tenant").toLowerCase();
                                   req.getUrl().withParam("tenant", tenant);

                                   String type = req.getUrl().getParam("type").toLowerCase();
                                   req.getUrl().withParam("type", type);

                                   Index partitionIdx = req.getCollection().getIndex("PartitionKey");
                                   String partitionProp = partitionIdx.getProperty(0).getJsonName();

                                   Object partitionKey = null;

                                   if (req.getUrl().findKey(partitionProp) == null)
                                   {
                                      String resourceKey = req.getResourceKey();
                                      if (resourceKey == null && req.getChain().getParent() != null)
                                      {
                                         //-- handles /customers/${customerKey}?expands=addresses
                                         resourceKey = req.getChain().getParent().getRequest().getResourceKey();
                                      }

                                      if (resourceKey != null)
                                      {
                                         //-- handles /addresses/$resourceKey1,$resourceKey2,$resourceKey3
                                         List<String> resourceKeys = Utils.explode(",", resourceKey);
                                         Row row = req.getCollection().decodeKey(resourceKeys.get(0));
                                         partitionKey = row.get(partitionProp);
                                      }

                                      if (partitionKey != null)
                                         req.getUrl().withParam(partitionProp, partitionKey.toString());
                                   }

                                   JSNode json = req.getJson();

                                   if (json != null)
                                   {
                                      for (JSNode node : json.asNodeList())
                                      {
                                         //-- not necessary for RDBMS stores but prevents
                                         //-- unintended props in document stores
                                         node.remove("collection", "resource", "relationship");

                                         //-- forces correct case
                                         node.put("tenant", tenant);
                                         node.put("type", type);

                                         //-- creates a unique id for each record
                                         if (Utils.empty(node.get("id")))
                                            node.put("id", UUID.randomUUID().toString());

                                         switch (req.getCollection().getName())
                                         {
                                            case "customers":
                                               node.put("partition", node.get("id"));
                                               break;
                                            case "addresses":

                                               Object customerId = null;
                                               Object customerNode = node.get("customer");

                                               if (customerNode instanceof JSNode)
                                                  customerId = ((JSNode) customerNode).getString("id");

                                               if (customerId == null && customerNode instanceof String)
                                               {
                                                  String customerKey = (String) customerNode;
                                                  customerKey = customerKey.substring(customerKey.lastIndexOf("/") + 1);

                                                  Row row = req.getApi().getCollection("customers").decodeKey(customerKey);
                                                  customerId = row.get("id");
                                               }

                                               if (customerId == null)
                                                  ApiException.throw400BadRequest("An address must have an associated customer");

                                               node.put("partition", customerId);

                                               break;
                                            default :
                                               ApiException.throw400BadRequest("Collection '{}' is unsupported: '{}'", req.getCollectionKey(), req.getUrl());
                                         }
                                      }
                                   }

                                   if (req.isGet() || req.isDelete())
                                   {
                                      if (req.getUrl().findKey("type") == null || (req.getResourceKey() == null && req.getUrl().findKey("partition") == null))
                                         ApiException.throw400BadRequest("Unable to GET/DELTE collection '{}' without a 'type' and 'partition' param: '{}'", req.getCollectionKey(), req.getUrl());
                                   }

                                   req.getChain().go();

                                   json = res.getJson();
                                   if (json != null)
                                   {
                                      //                             if(Chain.getDepth() == 1)
                                      //                             {
                                      //                                //todo: recursive remove
                                      //                                for (JSNode node : res.data().asNodeList())
                                      //                                {
                                      //                                   node.remove("tenant", "type", "partition");
                                      //                                }   
                                      //                             }
                                   }

                                }
                             }, new RestAction())//
                          .withDb(db);

      engine = new Engine(api);
   }

   @AfterAll
   public void afterAll_finalizeEngine()
   {
      if (db != null)
      {
         db.shutdown();
      }
   }

   @Test
   public void overloadedPartitionedDynamicTable()
   {
      Engine e = engine;

      Response res = null;

      res = e.post("crm/acmeco/customers", new JSNode("firstName", "myFirstName", "lastName", "myLastName", "addresses", //
                                                      new JSArray(new JSNode("alias", "home", "address1", "1234 hometown rd."), //
                                                                  new JSNode("alias", "office", "address1", "6789 workville st.")))).dump().assertOk();

      String customerHref = res.findString("data.0.href");
      String customerKey = customerHref.substring(customerHref.lastIndexOf("/") + 1);

      res = e.get(customerHref + "?expands=addresses").dump().assertOk();

      String addressHref = res.findString("data.0.addresses.0.href");

      //-- these two are functionally the same thing
      res = e.get(customerHref + "/addresses").dump().assertOk();
      assertEquals(2, res.data().size());

      res = e.get(customerHref + "/addresses?alias=office").dump().assertOk();
      assertEquals(1, res.data().size());

      res = e.get(addressHref + "?expands=customer").dump().assertOk();
      res = e.get(addressHref + "?expands=customer&customer=" + customerKey).dump().assertOk();

      //-- this will fail because there is no customerKey to establish the partition
      res = e.get("crm/acmeco/customers").dump().assertStatus(400);

      JSNode apartment = new JSNode("alias", "apartment", "address1", "1234 downtown rd.");
      res = e.post("crm/acmeco/addresses", apartment).assertStatus(400);

      apartment.put("customer", customerKey);
      res = e.post("crm/acmeco/addresses", apartment).assertOk();

      res = e.get(customerHref + "/addresses").dump().assertOk();
      assertEquals(3, res.data().size());

      for (JSNode address : res.data().asNodeList())
      {
         e.delete(address.getString("href")).dump().assertOk();
      }

      res = e.get(customerHref + "/addresses").dump().assertOk();
      assertEquals(0, res.data().size());

      res = e.delete(customerHref).dump().assertOk();

   }
}
