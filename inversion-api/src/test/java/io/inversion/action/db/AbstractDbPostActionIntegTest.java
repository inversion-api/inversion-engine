/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
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
package io.inversion.action.db;

import io.inversion.Engine;
import io.inversion.Response;
import io.inversion.utils.JSArray;
import io.inversion.utils.JSNode;
import io.inversion.utils.Utils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public abstract class AbstractDbPostActionIntegTest extends AbstractDbActionIntegTest {

    public AbstractDbPostActionIntegTest(String dbType) {
        super(dbType);
    }

    @Test
    public void testCompressDups() throws Exception {
        Response res;
        Engine   engine = engine();

        res = engine.get(url("employees?employeeId=5&expands=employees,territories,territories.region"));

        JSNode employee5 = res.findNode("data.0");
        JSNode employee5Copy = JSNode.parseJsonNode(employee5.toString());

        //-- this adds a logical duplicate
        JSArray territories = res.findArray("data.0.territories");
        territories.add(territories.get(territories.length()-1));                                   //-- this adds a referential duplicate
        territories.add(JSNode.parseJson(territories.get(territories.length()-1).toString()));      //--this adds a copy duplicate
        territories.getNode(1).put("region", new JSNode("regionId", 1)); //-- this is another duplicate

        res = engine.put(employee5.getString("href"), employee5);

        if(res.getError() != null)
            res.getError().printStackTrace();

        res.assertOk();
        res = engine.get(url("employees?employeeId=5&expands=employees,territories,territories.region"));
        JSNode updated5 = res.findNode("data.0");

        System.out.println(employee5);
        System.out.println(updated5);

        assertEquals(employee5Copy.toString(), updated5.toString());
    }


    @Test
    public void testAddOneRecord() throws Exception {
        Response res;
        Engine   engine = engine();

        //the bootstrap process copies 25 orders into the orders table, they are not sequential
        res = engine.get(url("orders?limit=100&sort=orderid"));
        assertEquals(25, res.find("meta.foundRows")); //25 rows are copied by the bootstrap process, 11058 is last one

        //post one new bogus order
        res = engine.post(url("orders"), new JSNode("shipaddress", "somewhere in atlanta", "shipcity", "atlanta")).assertOk();

        res.dump();
        //check the values we sent are the values we got back
        res = engine.get(res.findString("data.0.href"));
        assertEquals("somewhere in atlanta", res.find("data.0.shipaddress"));
        assertEquals("atlanta", res.find("data.0.shipcity"));

        //check total records
        res = engine.get(url("orders?page=1&sort=orderid"));
        res.dump();
        assertEquals(26, res.find("meta.foundRows"));

        assertEquals(10273, res.getStream().getNode(25).getInt("orderId"));
    }

    @Test
    public void testDuplicate1() throws Exception {
        Response res;
        Engine   engine = engine();

        res = engine.get(url("employees?employeeId=5&expands=employees,territories,territories.region"));

        res.dump();
        JSNode employee5 = res.findNode("data.0");

        String comp1 = res.getJson().toString();
        String comp2 = JSNode.parseJson(comp1).toString();
        assertEquals(comp1, comp2);

        System.out.println(employee5);

        res = engine.put(employee5.getString("href"), employee5);
        res.dump();


        if(res.getError() != null)
            res.getError().printStackTrace();

        res.assertOk();
        res = engine.get(url("employees?employeeId=5&expands=employees,territories,territories.region"));
        JSNode updated5 = res.findNode("data.0");

        assertEquals(employee5.toString(), updated5.toString());
    }

    @Test
    public void test_put_with_nested_one_to_many_new_child(){
        Response res;
        Engine   engine = engine();

        res = engine.get(url("territories/30346?expands=region"));
        JSNode origional30346 = res.data();
        res.dump();

        assertEquals(4, res.findInt("data.0.region.regionId"));
        assertEquals("Southern", res.findString("data.0.region.regionDescription"));

        JSNode updateTo30346 = JSNode.parseJsonNode(Utils.read(AbstractDbPostActionIntegTest.class.getResourceAsStream("upsert001/put_with_nested_one_to_many_post.json")));
        res = engine.put(url("territories"), updateTo30346);
        res.dump();

        res = engine.get(url("territories/30346?expands=region"));
        res.dump();
        JSNode updated30346 = res.getStream();

        assertEquals(5, res.findInt("data.0.regionId"));
        assertEquals(5, res.findInt("data.0.region.regionId"));
        assertEquals("HotLanta", res.findString("data.0.region.regionDescription"));
    }



    @Test
    public void test_post_remove_many_to_many_relationships() {

        Response res;
        Engine   engine = engine();

        res = engine.get(url("employees?employeeId=5&expands=territories"));
        assertEquals(7, res.findArray("data.0.territories").size());

        JSArray territories = res.findArray("data.0.territories");
        territories.remove(6);
        territories.remove(0);

        res = engine.put(res.findString("data.0.href"), res.getJson().findNode("data.0"));
        res.dump();
        res.assertOk();
        res = engine.get(url("employees?employeeId=5&expands=territories")).dump();
        res.dump();
        assertEquals(territories.toString(), res.find("data.0.territories").toString());
    }

    @Test
    public void test_post_remove_one_to_many_relationships(){

        Response res;
        Engine   engine = engine();

        res = engine.get(url("employees?employeeId=5&expands=employees"));
        JSArray employees = res.findArray("data.0.employees");
        System.out.println(employees.size());

        assertEquals(3, employees.length());
        employees.remove(2);
        employees.remove(0);

        res = engine.put(res.findString("data.0.href"), res.getJson());
        res.dump();
        res.assertOk();

        res = engine.get(url("employees?employeeId=5&expands=employees"));
        JSArray newEmployees = res.findArray("data.0.employees");
        assertEquals(employees.toString(), newEmployees.toString());
    }

    @Test
    public void test_post_remove_many_to_one_relationship(){

        Response res;
        Engine   engine = engine();

        res = engine.get(url("employees/1"));
        res.dump();
        assertTrue(res.findString("data.0.reportsTo").endsWith("/2"));

        res.findNode("data.0").put("reportsTo", null);
        res = engine.put(res.findString("data.0.href"), res.getJson());
        res.dump();
        res.assertOk();

        res = engine.get(url("employees/1"));
        res.dump();
        assertNull(res.find("data.0.reportsTo"));
    }



    @Test
    public void testNestedPost1() throws Exception {
        Response res;
        Engine   engine = engine();

        JSNode john = JSNode.parseJsonNode(Utils.read(AbstractDbPostActionIntegTest.class.getResourceAsStream("upsert001/upsert001-1.json")));

        res = engine.get(url("employees?employeeId=5&expands=employees"));
        JSNode steve = res.findNode("data.0");

        assertEquals(3, res.findArray("data.0.employees").size());

        //steve.findArray("employees").clear();
        steve.findArray("employees").add(john);

        System.out.println(steve);
        res = engine.put(steve.getString("href"), steve);
        res.dump();
        res.assertOk();

        res = engine.get(url("employees?employeeId=5&expands=employees"));
        res.dump();

        assertEquals(4, res.findArray("data.0.employees").size(), "the new employee was not related to its parent");

        //-- make sure the new employee was POSTED
        res = engine.get(url("employees/99999991?expands=reportsTo,territories,territories.region"));
        res.dump();
        assertEquals(1, res.getStream().size());
        assertTrue(res.findString("data.0.href").contains("/99999991"));
        assertTrue(res.findString("data.0.reportsTo.href").contains("employees/5"));
        assertEquals(res.findString("data.0.territories.0.TerritoryID"), "30346");

        res = engine.get(res.findString("data.0.territories.0.href") + "?expands=region");
        res.dump();

        //-- confirms that a the new region was created and assigned to territory 30346
        assertEquals(url("regions/5"), res.findString("data.0.region.href"));
        assertEquals("HotLanta", res.findString("data.0.region.regiondescription"));

        //--now go back to steve and unhook several many-to-one reportsTo relationships
        res = engine.get(url("employees?employeeId=5&expands=employees"));
        steve = res.findNode("data.0");

        JSArray employees = steve.findArray("employees");

        for (int i = 0; i < employees.length(); i++) {
            if (!"99999991".equals(employees.getNode(i).getString("employeeId"))) {
                employees.remove(i);
                i--;
            }
        }

        engine.put(steve.getString("href"), steve).dump();
        res = engine.get(url("employees?employeeId=5&expands=employees")).dump();

        assertEquals(1, res.findArray("data.0.employees").size());
        assertTrue(res.findString("data.0.employees.0.href").contains("/99999991"));
        res.dump();

        //-- now unhook all many-to-one employees...this a different case than unhooking some but not all  
        res = engine.get(url("employees?employeeId=5&expands=employees"));
        steve = res.findNode("data.0");

        employees = steve.findArray("employees");
        employees.clear();

        engine.put(steve.getString("href"), steve);
        res = engine.get(url("employees?employeeId=5&expands=employees"));
        res.dump();
        assertEquals(0, res.findArray("data.0.employees").size());

        //-- now unhook all many-to-many employee->territories...this a different case than unhooking some but not all  
        res = engine.get(url("employees?employeeId=5&expands=territories"));
        assertEquals(7, res.findArray("data.0.territories").size());
        JSNode manager = res.findNode("data.0");
        manager.findArray("territories").clear();

        engine.put(manager.getString("href"), manager);
        res = engine.get(url("employees?employeeId=5&expands=territories")).dump();
        assertEquals(0, res.findArray("data.0.territories").size());

        res.dump();
    }

    //   @Test
    //   public void testNestedPutPost_oneToMany() throws Exception
    //   {
    //      Engine engine = new Engine(new Api()//
    //                                          .withName("crm")//
    //                                          .withDb(new JdbcDb("crm", //the database name used as the properties key prefix when
    //                                                             "org.h2.Driver", //-- jdbc driver
    //                                                             "jdbc:h2:mem:crm;DB_CLOSE_DELAY=-1", //-- jdbc url 
    //                                                             "sa", //-- jdbc user
    //                                                             "", //jdbc password
    //                                                             JdbcDb.class.getResource("crm-h2.ddl").toString()))//
    //                                          .withEndpoint("GET,PUT,POST,DELETE", "/*", new DbAction()));
    //      engine.startup();
    //      Response res = null;
    //
    //      JSNode cust3 = JSNode.parseJsonNode(Utils.read(getClass().getResourceAsStream("upsert002/01-POST-customers.json")));
    //
    //      res = engine.post("crm/customers", cust3);
    //
    //      res = engine.get("crm/customers?lastName=Tester1&expands=identifiers");
    //      res.dump();
    //
    //      assertEquals("Tester1", (res.find("data.0.lastName")));
    //      assertEquals("new_one_1", (res.find("data.0.identifiers.0.providerCode")));
    //      assertEquals("customerId", (res.find("data.0.identifiers.0.type")));
    //      assertEquals("new_one_val_1", (res.find("data.0.identifiers.0.identifier")));
    //
    //      //creates identifier 11
    //      cust3 = JSNode.parseJsonNode(Utils.read(getClass().getResourceAsStream("upsert002/02-PUT-customers.json")));
    //      engine.put(cust3.getString("href"), cust3).isSuccess();
    //
    //      //verify new identifier 11 values
    //      res = engine.get("crm/customers?lastName=Tester1&expands=identifiers");
    //
    //      res.dump();
    //      assertEquals(1, res.data().size());
    //      assertEquals("Tester1", (res.find("data.0.lastName")));
    //      assertEquals("11", (res.find("data.0.identifiers.0.id") + ""));
    //      assertEquals("new_one_2", (res.find("data.0.identifiers.0.providerCode")));
    //      assertEquals("customerId", (res.find("data.0.identifiers.0.type")));
    //      assertEquals("new_one_val_2", (res.find("data.0.identifiers.0.identifier")));
    //
    //      res = engine.get("crm/identifiers/11");
    //      assertEquals("3", res.find("data.0.customerid") + "");
    //
    //      //make sure old identifier 11 was unhooked
    //      res = engine.get("crm/identifiers/10");
    //      assertEquals("null", res.find("data.0.customerid") + "");
    //
    //      //puts identifier 10 back on 
    //      cust3 = JSNode.parseJsonNode(Utils.read(getClass().getResourceAsStream("upsert002/03-PUT-customers.json")));
    //      engine.put(cust3.getString("href"), cust3).isSuccess();
    //
    //      res = engine.get("crm/identifiers/10");
    //      assertEquals("3", res.find("data.0.customerid") + "");
    //      assertEquals("new_one_val_1_updated", res.find("data.0.identifier"));
    //
    //      res = engine.get("crm/identifiers");
    //      res.dump();
    //
    //      res = engine.get("crm/customers?lastName=Tester1&expands=identifiers");
    //      res.dump();
    //
    //      assertEquals(1, res.data().size());
    //      assertEquals("Tester1", (res.find("data.0.lastName")));
    //      assertEquals("new_one_1", (res.find("data.0.identifiers.0.providerCode")));
    //      assertEquals("customerId", (res.find("data.0.identifiers.0.type")));
    //      assertEquals("new_one_val_1_updated", (res.find("data.0.identifiers.0.identifier")));
    //
    //      //res = engine.get("crm/customers?expands=identifiers&properties");
    //      res = engine.get(cust3.getString("href") + "?expands=identifiers&properties");
    //
    //      res.dump();
    //
    //      res = engine.get("crm/identifiers");
    //      res.dump();
    //   }
    //
    //   @Test
    //   public void testNestedPutPost_manyToOne() throws Exception
    //   {
    //      Engine engine = new Engine(new Api()//
    //                                          .withName("crm")//
    //                                          .withDb(new JdbcDb("crm2", //the database name used as the properties key prefix when
    //                                                             "org.h2.Driver", //-- jdbc driver
    //                                                             "jdbc:h2:mem:crm2;DB_CLOSE_DELAY=-1", //-- jdbc url 
    //                                                             "sa", //-- jdbc user
    //                                                             "", //jdbc password
    //                                                             JdbcDb.class.getResource("crm-h2.ddl").toString()))//
    //                                          .withEndpoint("GET,PUT,POST,DELETE", "/*", new DbAction()));
    //      engine.startup();
    //      Response res = null;
    //
    //      InputStream stream = getClass().getResourceAsStream("upsert003/01-POST-identifiers.json");
    //      JSNode id10 = JSNode.parseJsonNode(Utils.read(stream));
    //      res = engine.post("crm/identifiers", id10);
    //      res = engine.get("crm/identifiers?id=10&expands=customer");
    //      res = engine.get("crm/customers?lastName=Tester1&expands=identifiers");
    //
    //      assertEquals("Tester1", (res.find("data.0.lastName")));
    //      assertEquals("new_one_1", (res.find("data.0.identifiers.0.providerCode")));
    //      assertEquals("customerId", (res.find("data.0.identifiers.0.type")));
    //      assertEquals("new_one_val_1", (res.find("data.0.identifiers.0.identifier")));
    //
    //      JSNode id11 = JSNode.parseJsonNode(Utils.read(getClass().getResourceAsStream("upsert003/02-PUT-identifiers.json")));
    //      res = engine.post("crm/identifiers", id11);
    //      res = engine.get("crm/customers?lastName=Tester2&expands=identifiers");
    //
    //      assertEquals(1, res.findArray("data.0.identifiers").size());
    //      assertEquals("Tester2", (res.find("data.0.lastName")));
    //      assertEquals("new_one_1", (res.find("data.0.identifiers.0.providerCode")));
    //      assertEquals("customerId", (res.find("data.0.identifiers.0.type")));
    //      assertEquals("new_one_val_1_updated", (res.find("data.0.identifiers.0.identifier")));
    //
    //      res = engine.get("crm/customers?lastName=Tester1&expands=identifiers");
    //      assertEquals(0, res.findArray("data.0.identifiers").size());
    //   }

}
