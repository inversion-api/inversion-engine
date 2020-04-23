package io.inversion.cloud.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.inversion.cloud.action.rest.RestAction;
import io.inversion.cloud.jdbc.db.JdbcDb;
import io.inversion.cloud.model.Action;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.ApiException;
import io.inversion.cloud.model.Collection;
import io.inversion.cloud.model.Db;
import io.inversion.cloud.model.Index;
import io.inversion.cloud.model.JSArray;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Property;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.model.Rows.Row;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.service.Engine;
import io.inversion.cloud.utils.Utils;

public class TestOverloadedDynamicTables
{
   //TODO restore me
   //   @Test
   //   public void overloadedPartitionedDynamicTable()
   //   {
   //      Db db = new JdbcDb("db", "org.h2.Driver", //
   //                         "jdbc:h2:mem:" + "overloadedPartitionedDynamicTable" + ";IGNORECASE=TRUE;DB_CLOSE_DELAY=-1", //
   //                         "sa", //
   //                         "", //
   //                         JdbcDb.class.getResource("dynamic-h2.ddl").toString())
   //         {
   //
   //            @Override
   //            public void configDb() throws Exception
   //            {
   //               super.configDb();
   //               Collection object = getCollectionByTableName("Object");
   //               removeCollection(object);
   //
   //               Collection addresses = object.copy().withName("addresses");
   //               addresses.getProperty("value1").withJsonName("customer");
   //               addresses.getProperty("value2").withJsonName("alias");
   //               addresses.getProperty("value3").withJsonName("address1");
   //               addresses.getProperty("value4").withJsonName("address2");
   //               addresses.getProperty("value5").withJsonName("city");
   //               addresses.getProperty("value6").withJsonName("state");
   //               addresses.getProperty("value7").withJsonName("zip");
   //               addresses.withIndex("PK", "primary", true, "partition", "id");
   //               addresses.withIndex("PartitionKey", "PartitionKey", false, "partition");
   //
   //               Collection customers = object.copy().withName("customers");
   //               customers.getProperty("value1").withJsonName("firstName");
   //               customers.getProperty("value2").withJsonName("lastName");
   //               customers.withIndex("PK", "primary", true, "partition", "id");
   //               customers.withRelationship("addresses", addresses, "customer", "customer");
   //               customers.withIndex("PartitionKey", "PartitionKey", false, "partition");
   //
   //               withCollection(addresses);
   //               withCollection(customers);
   //
   //            }
   //         };
   //
   //      Api api = new Api("crm").withIncludePaths("crm/:tenant/[:type]/*") //
   //                              .withEndpoint("*", "*", new Action()
   //                                 {
   //                                    public void run(Request req, Response res) throws Exception
   //                                    {
   //                                       //-- requires "type" param and forces to lower case
   //                                       String tenant = req.getUrl().getParam("tenant").toLowerCase();
   //                                       req.getUrl().withParam("tenant", tenant);
   //
   //                                       String type = req.getUrl().getParam("type").toLowerCase();
   //                                       req.getUrl().withParam("type", type);
   //
   //                                       Index partitionIdx = req.getCollection().getIndex("PartitionKey");
   //                                       String partitionProp = partitionIdx.getProperty(0).getJsonName();
   //
   //                                       Object partitionKey = null;
   //
   //                                       if (req.getUrl().findKey(partitionProp) == null)
   //                                       {
   //                                          String entityKey = req.getEntityKey();
   //                                          if (entityKey == null && req.getChain().getParent() != null)
   //                                          {
   //                                             //-- handles /customers/${customerKey}?expands=addresses
   //                                             entityKey = req.getChain().getParent().getRequest().getEntityKey();
   //                                          }
   //
   //                                          if (entityKey != null)
   //                                          {
   //                                             //-- handles /addresses/$entityKey1,$entityKey2,$entityKey3
   //                                             List<String> entityKeys = Utils.explode(",",  entityKey);
   //                                             Row row = req.getCollection().decodeKey(entityKeys.get(0));
   //                                             partitionKey = row.get(partitionProp);
   //                                          }
   //
   //                                          if (partitionKey != null)
   //                                             req.getUrl().withParam(partitionProp, partitionKey.toString());
   //                                       }
   //
   //                                       JSNode json = req.getJson();
   //
   //
   //                                       if (json != null)
   //                                       {
   //                                          for (JSNode node : json.asNodeList())
   //                                          {
   //                                             //-- not necessary for RDBMS stores but prevents
   //                                             //-- unintended props in document stores
   //                                             node.removeAll("collection", "entity", "relationship");
   //
   //                                             //-- forces correct case
   //                                             node.put("tenant", tenant);
   //                                             node.put("type", type);
   //
   //                                             //-- creates a unique id for each record
   //                                             if (Utils.empty(node.get("id")))
   //                                                node.put("id", UUID.randomUUID().toString());
   //
   //                                             switch (req.getCollection().getName())
   //                                             {
   //                                                case "customers":
   //                                                   node.put("partition", node.get("id"));
   //                                                   break;
   //                                                case "addresses":
   //
   //                                                   Object customerId = null;
   //                                                   Object customerNode = node.get("customer");
   //
   //                                                   if (customerNode instanceof JSNode)
   //                                                      customerId = ((JSNode) customerNode).getString("id");
   //
   //                                                   if (customerId == null && customerNode instanceof String)
   //                                                   {
   //                                                      String customerKey = (String) customerNode;
   //                                                      customerKey = customerKey.substring(customerKey.lastIndexOf("/") + 1);
   //
   //                                                      Row row = req.getApi().getCollection("customers").decodeKey(customerKey);
   //                                                      customerId = row.get("id");
   //                                                   }
   //
   //                                                   if (customerId == null)
   //                                                      ApiException.throw400BadRequest("An address must have an associated customer");
   //
   //                                                   node.put("partition", customerId);
   //
   //                                                   break;
   //                                                default :
   //                                                   ApiException.throw400BadRequest("Collection '{}' is unsupported: '{}'", req.getCollectionKey(), req.getUrl());
   //                                             }
   //                                          }
   //                                       }
   //
   //                                       if (req.isGet() || req.isDelete())
   //                                       {
   //                                          if (req.getUrl().findKey("type") == null || (req.getEntityKey() == null && req.getUrl().findKey("partition") == null))
   //                                             ApiException.throw400BadRequest("Unable to GET/DELTE collection '{}' without a 'type' and 'partition' param: '{}'", req.getCollectionKey(), req.getUrl());
   //                                       }
   //
   //                                       req.getChain().go();
   //
   //                                       json = res.getJson();
   //                                       if (json != null)
   //                                       {
   //                                          for (JSNode node : res.data().asNodeList())
   //                                          {
   //                                             node.removeAll("tenant", "type", "partition");
   //                                          }
   //                                       }
   //
   //                                    }
   //                                 }, new RestAction())//
   //                              .withDb(db);
   //
   //      Engine e = new Engine(api);
   //
   //      Response res = null;
   //
   //      res = e.post("crm/acmeco/customers", new JSNode("firstName", "myFirstName", "lastName", "myLastName", "addresses", new JSArray(new JSNode("alias", "home", "address1", "1234 hometown rd."), new JSNode("alias", "office", "address1", "6789 workville st.")))).dump().assertOk();
   //
   //      String customerHref = res.findString("data.0.href");
   //      String customerKey = customerHref.substring(customerHref.lastIndexOf("/") + 1);
   //
   //      res = e.get(customerHref + "?expands=addresses").dump().assertOk();
   //
   //      String addressHref = res.findString("data.0.addresses.0.href");
   //
   //      //-- these two are functionally the same thing
   //      res = e.get(customerHref + "/addresses").dump().assertOk();
   //      assertEquals(2, res.data().size());
   //
   //      res = e.get(customerHref + "/addresses?alias=office").dump().assertOk();
   //      assertEquals(1, res.data().size());
   //
   //      res = e.get(addressHref + "?expands=customer").dump().assertOk();
   //      res = e.get(addressHref + "?expands=customer&customer=" + customerKey).dump().assertOk();
   //
   //      //-- this will fail because there is no customerKey to establish the partition
   //      res = e.get("crm/acmeco/customers").dump().assertStatus(400);
   //            
   //
   //      JSNode apartment = new JSNode("alias", "apartment", "address1", "1234 downtown rd.");
   //      res = e.post("crm/acmeco/addresses", apartment).assertStatus(400);
   //
   //      apartment.put("customer", customerKey);
   //      res = e.post("crm/acmeco/addresses", apartment).assertOk();
   //
   //      res = e.get(customerHref + "/addresses").dump().assertOk();
   //      assertEquals(3, res.data().size());
   //
   //      for (JSNode address : res.data().asNodeList())
   //      {
   //         e.delete(address.getString("href")).dump().assertOk();
   //      }
   //
   //      res = e.get(customerHref + "/addresses").dump().assertOk();
   //      assertEquals(0, res.data().size());
   //      
   //      res = e.delete(customerHref).dump().assertOk();
   //
   //   }
}
