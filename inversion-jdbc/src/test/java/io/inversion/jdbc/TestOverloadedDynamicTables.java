package io.inversion.jdbc;

import io.inversion.*;
import io.inversion.action.db.DbAction;
import io.inversion.json.JSList;
import io.inversion.json.JSMap;
import io.inversion.json.JSNode;
import io.inversion.utils.Utils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(Lifecycle.PER_CLASS)
public class TestOverloadedDynamicTables {
    Engine engine = null;
    Api    api    = null;
    Db     db     = null;

    @BeforeAll
    public void beforeAll_initializeEngine() {
        Chain.resetAll();
        JdbcConnectionLocal.closeAll();

        db = new JdbcDb("db", "org.h2.Driver", //
                "jdbc:h2:mem:" + System.currentTimeMillis() + ";IGNORECASE=TRUE;DB_CLOSE_DELAY=-1", //
                "sa", //
                "", //
                JdbcDb.class.getResource("dynamic-h2.ddl").toString()) {

            @Override
            public void configDb() throws ApiException {
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
                customers.withOneToManyRelationship("addresses", addresses, "customer", "customer");
                customers.withIndex("PartitionKey", "PartitionKey", false, "partition");

                withCollection(addresses);
                withCollection(customers);

            }

        };

        api = new Api("crm").withServer(new Server().withIncludeOn("crm/{tenant}/[{type}]/*")) //
                .withEndpoint(new Action() {
                    public void run(Request req, Response res) throws ApiException {
                        //-- requires "type" param and forces to lower case
                        String tenant = req.getUrl().getParam("tenant").toLowerCase();
                        req.getUrl().withParam("tenant", tenant);

                        String type = req.getUrl().getParam("type").toLowerCase();
                        req.getUrl().withParam("type", type);

                        Index  partitionIdx  = req.getCollection().getIndex("PartitionKey");
                        String partitionProp = partitionIdx.getProperty(0).getJsonName();

                        Object partitionKey = null;

                        if (req.getUrl().findKey(partitionProp) == null) {
                            String resourceKey = req.getResourceKey();
                            if (resourceKey == null && req.getChain().getParent() != null) {
                                //-- handles /customers/${customerKey}?expands=addresses
                                resourceKey = req.getChain().getParent().getRequest().getResourceKey();
                            }

                            if (resourceKey != null) {
                                //-- handles /addresses/$resourceKey1,$resourceKey2,$resourceKey3
                                List<String>        resourceKeys = Utils.explode(",", resourceKey);
                                Map<String, Object> row          = req.getCollection().decodeKeyToJsonNames(resourceKeys.get(0));
                                partitionKey = row.get(partitionProp);
                            }

                            if (partitionKey != null)
                                req.getUrl().withParam(partitionProp, partitionKey.toString());
                        }

                        JSNode json = req.getJson();

                        if (json != null) {
                            for (JSNode node : json.asMapList()) {
                                //-- not necessary for RDBMS stores but prevents
                                //-- unintended props in document stores
                                node.removeValues("_collection", "_resource", "_relationship");

                                //-- forces correct case
                                node.putValue("tenant", tenant);
                                node.putValue("type", type);

                                //-- creates a unique id for each record
                                if (Utils.empty(node.getValue("id")))
                                    node.putValue("id", UUID.randomUUID().toString());

                                switch (req.getCollection().getName()) {
                                    case "customers":
                                        node.putValue("partition", node.getValue("id"));
                                        break;
                                    case "addresses":

                                        Object customerId = null;
                                        Object customerNode = node.getValue("customer");

                                        if (customerNode instanceof JSNode)
                                            customerId = ((JSNode) customerNode).getString("id");

                                        if (customerId == null && customerNode instanceof String) {
                                            String customerKey = (String) customerNode;
                                            customerKey = customerKey.substring(customerKey.lastIndexOf("/") + 1);

                                            Map<String, Object> row = req.getApi().getCollection("customers").decodeKeyToJsonNames(customerKey);
                                            customerId = row.get("id");
                                        }

                                        if (customerId == null)
                                            throw ApiException.new400BadRequest("An address must have an associated customer");

                                        node.putValue("partition", customerId);

                                        break;
                                    default:
                                        throw ApiException.new400BadRequest("Collection '{}' is unsupported: '{}'", req.getCollectionKey(), req.getUrl());
                                }
                            }
                        }

                        if (req.isGet() || req.isDelete()) {
                            if (req.getUrl().findKey("type") == null || (req.getResourceKey() == null && req.getUrl().findKey("partition") == null))
                                throw ApiException.new400BadRequest("Unable to GET/DELTE collection '{}' without a 'type' and 'partition' param: '{}'", req.getCollectionKey(), req.getUrl());
                        }

                        req.getChain().go();

                        json = res.getJson();
                        if (json != null) {
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
                }, new DbAction())//
                .withDb(db);

        engine = new Engine(api);
    }

    @AfterAll
    public void afterAll_finalizeEngine() {
        if (db != null) {
            db.shutdown();
        }
    }

    @Test
    public void overloadedPartitionedDynamicTable() {
        Engine   e   = engine;
        Response res = null;

        res = e.post("crm/acmeco/customers", new JSMap("firstName", "myFirstName", "lastName", "myLastName", "addresses", //
                new JSList(new JSMap("alias", "home", "address1", "1234 hometown rd."), //
                        new JSMap("alias", "office", "address1", "6789 workville st."))));

        res.dump();
        res.assertOk();

        String customerHref = res.findString("data.0.href");
        String customerKey  = customerHref.substring(customerHref.lastIndexOf("/") + 1);

        res = e.get(customerHref + "?expands=addresses").assertOk();

        String addressHref = res.findString("data.0.addresses.0.href");

        //-- these two are functionally the same thing
        res = e.get(customerHref + "/addresses").assertOk();
        assertEquals(2, res.data().size());

        res = e.get(customerHref + "/addresses?alias=office").assertOk();
        assertEquals(1, res.data().size());

        e.get(addressHref + "?expands=customer").assertOk();
        e.get(addressHref + "?expands=customer&customer=" + customerKey).assertOk();

        //-- this will fail because there is no customerKey to establish the partition
        e.get("crm/acmeco/customers").assertStatus(400);

        JSNode apartment = new JSMap("alias", "apartment", "address1", "1234 downtown rd.");
        e.post("crm/acmeco/addresses", apartment).assertStatus(400);

        apartment.putValue("customer", customerKey);
        e.post("crm/acmeco/addresses", apartment).assertOk();

        res = e.get(customerHref + "/addresses").assertOk();
        assertEquals(3, res.data().size());

        for (JSNode address : res.data().asMapList()) {
            e.delete(address.getString("href")).assertOk();
        }

        res = e.get(customerHref + "/addresses").assertOk();
        assertEquals(0, res.data().size());

        e.delete(customerHref).assertOk();
    }
}
