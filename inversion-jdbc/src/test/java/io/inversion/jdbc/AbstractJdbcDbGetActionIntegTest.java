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
package io.inversion.jdbc;

import io.inversion.Api;
import io.inversion.Collection;
import io.inversion.Engine;
import io.inversion.Response;
import io.inversion.action.db.AbstractDbGetActionIntegTest;
import io.inversion.utils.JSNode;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public abstract class AbstractJdbcDbGetActionIntegTest extends AbstractDbGetActionIntegTest implements AbstractJdbcDbEngineTest {
    public AbstractJdbcDbGetActionIntegTest(String type) {
        super(type);
    }

    @Test
    public void testNonUrlSafeKeyValues() throws Exception {
        Response res;
        Engine   engine = engine();

        res = engine.get(url("urls"));
        res.dump();

        String str1 = res.find("data.0").toString();

        String href = res.findString("data.0.href");

        res = engine.get(href);
        res.dump();

        String str2 = res.find("data.0").toString();

        assertEquals(str1, str2);

    }

    @Test
    public void testNegativeOneToManyFilters() throws Exception {
        Response resp;

        //find everyone who reports to Andrew
        resp = engine.get(url("employees" + "?eq(reportsTo.firstName,Andrew)"));
        assertEquals(5, resp.getStream().size());

        //find everyone who does not report to Andrew
        resp = engine.get(url("employees" + "?ne(reportsTo.firstName,Andrew)"));
        assertEquals(4, resp.getStream().size());
    }

    @Test
    public void testNegativeManyToOneFilters() throws Exception {
        Response resp;

        //"Andrew Fuller" is Nancy's manager in the Northwind datataset
        resp = engine.get(url("employees" + "?eq(employees.firstName,Nancy)"));
        assertEquals(1, resp.getStream().size());
        assertTrue(resp.getStream().toString().toLowerCase().indexOf("fuller") > 0);

        resp = engine.get(url("employees" + "?ne(employees.firstName,Nancy)"));
        assertEquals(8, resp.getStream().size());
        assertTrue(!resp.getStream().toString().toLowerCase().contains("fuller"));
    }

    @Test
    public void testNegativeManyToManyFilters() throws Exception {
        Response resp;

        //-- find everyone who has never sold something with a quantity of 12
        resp = engine.get(url("employees" + "?ne(orderdetails.quantity,12)"));
        resp.dump();
        assertEquals(6, resp.getStream().size());
    }

    @Test
    public void testRelatedCollectionJoinSelect() throws Exception {
        Response res;
        Engine   engine = engine();

        res = engine.get(url("customers?orders.shipCity=NONE&customerid=VINET"));
        assertEquals(res.getFoundRows(), 0);

        res = engine.get(url("customers?customerid=VINET&orders.freight=32.3800&expands=orders"));
        res.dump();
        assertEquals(res.getFoundRows(), 1);
        assertTrue(res.findString("data.0.href").endsWith("/customers/VINET"));
        assertEquals(res.findArray("data.0.orders").length(), 1);

        res = engine.get(url("customers?customerid=VINET&in(orders.freight,32.3800,11.0800)&expands=orders"));
        assertEquals(1, res.getFoundRows());
        assertTrue(res.findString("data.0.href").endsWith("/customers/VINET"));
        assertEquals(1, res.findArray("data.0.orders").length());

        res = engine.get(url("orders?in(freight,32.3800)&customer.customerid=VINET&expands=customer"));
        assertEquals(1, res.getFoundRows());
        assertTrue(res.findString("data.0.customer.href").endsWith("/customers/VINET"));

        //composite key many-to-many
        res = engine.get(url("employees?orderdetails.orderid=10258&expands=orderdetails"));
        res.dump();
        assertEquals(1, res.getFoundRows());
        assertTrue(res.findString("data.0.href").endsWith("/employees/1"));
        assertEquals(res.findArray("data.0.orderdetails").length(), 5);
        assertEquals(res.findString("data.0.orderdetails.0.orderid"), "10258");

        //joining back to yourself...have to alias table as join still
        res = engine.get(url("employees?reportsto.employeeid=2"));
        res.dump();
        assertEquals(res.getFoundRows(), 5);
        assertTrue(res.findString("data.0.reportsto").endsWith("/employees/2"));

        //joining back to yourself...the other way, finds #3's boss...it would be silly to do this but it needs to work
        res = engine.get(url("employees?employees.employeeid=3"));
        assertEquals(res.getFoundRows(), 1);
        assertTrue(res.findString("data.0.href").endsWith("/employees/2"));

        //three way join on the employee table
        res = engine.get(url("employees?employees.employeeid=6&reportsto.employeeid=2"));
        assertEquals(1, res.getFoundRows());
        assertTrue(res.findString("data.0.href").endsWith("/employees/5"));

        //works
        res = engine.get(url("employees?employees.lastname=Davolio&expands=employees"));
        res.dump();
        assertEquals(1, res.getFoundRows());
        assertTrue(res.findString("data.0.href").endsWith("/employees/2"));
        assertEquals(res.findArray("data.0.employees").length(), 5);
        assertEquals(res.find("data.0.employees.0.lastname"), "Davolio");

        res = engine.get(url("employees?orderdetails.orderid=10258&expands=orderdetails"));
        assertEquals(1, res.getFoundRows());

        //none of the orderdetails from above have a quantity of 90
        res = engine.get(url("employees?orderdetails.orderid=10258&orderdetails.quantity=90"));
        assertEquals(0, res.getFoundRows());
    }

    /**
     * Makes sure that MTM link tables are not recognized as entities
     */
    @Test
    public void testCollections() {
        List<String> c1 = new ArrayList(Arrays.asList("categories", "customercustomerdemoes", "customerdemographics", "customers", "employeeorderdetails", "employees", "employeeterritories", "indexlogs", "orderdetails", "orders", "products", "regions", "shippers", "suppliers", "territories", "urls"));
        List<String> c2 = new ArrayList<>();

        Api api = engine().getApi("northwind");

        for (Collection coll : (List<Collection>) api.getDb(getType()).getCollections()) {
            c2.add(coll.getName().toLowerCase());
        }

        java.util.Collection disjunction = CollectionUtils.disjunction(c1, c2);

        assertEquals(0, disjunction.size());

    }

    @Test
    public void testRelationships1() throws Exception {
        Response res;
        Engine   engine = engine();

        //res = engine.get("http://localhost/northwind/source/orders?limit=5&sort=orderid");
        res = engine.get(url("orders?limit=5&sort=orderid"));
        res.dump();
        assertEquals(5, res.getStream().size());
        assertTrue(res.findString("data.0.customer").endsWith("/customers/VINET"));
        assertTrue(res.findString("data.0.orderdetails").toLowerCase().endsWith("/orders/10248/orderdetails"));
        assertTrue(res.findString("data.0.employee").endsWith("/employees/5"));

        res = engine.get(url("employees/1/territories?limit=5&order=-territoryid")).assertOk();
        assertEquals(2, res.getStream().size());
        assertTrue(res.findString("data.0.href").endsWith("/territories/19713"));
        assertTrue(res.findString("data.0.employees").endsWith("/territories/19713/employees"));
    }

    @Test
    public void testExpandsOneToMany11() throws Exception {
        Engine   engine = engine();
        Response res;

        res = engine.get(url("orders/10248?expands=customer,employee,employee.reportsto"));
        assertTrue(res.findString("data.0.customer.href").endsWith("/customers/VINET"));
        assertTrue(res.findString("data.0.employee.href").endsWith("/employees/5"));
        assertTrue(res.findString("data.0.employee.reportsto.href").endsWith("/employees/2"));
    }

//TODO: put me back in, only commented out becuase a change made this run forever.
//    @Test
//    public void testExpandsOneToMany12() throws Exception {
//        Engine   engine = engine();
//        Response res;
//
//        res = engine.get(url("orders/10248?expands=employee.reportsto.employees"));
//        res.dump();
//        assertTrue(res.findString("data.0.employee.href").endsWith("/employees/5"));
//        assertTrue(res.findString("data.0.employee.reportsto.href").endsWith("/employees/2"));
//        assertTrue(res.findString("data.0.employee.reportsto.employees.0.href").endsWith("/employees/1"));
//        assertTrue(res.getJson().toString().indexOf("\"@link\" : \"" + url("employees/5") + "\"") > 0);
//    }

    @Test
    public void testExpandsManyToOne() throws Exception {
        Engine   engine = engine();
        Response res;

        res = engine.get(url("employees/5?expands=employees"));
        res.dump();
        assertEquals(3, res.findArray("data.0.employees").length());
        assertNotNull(res.find("data.0.employees.0.lastname"));
    }

    @Test
    public void testExpandsManyToMany() throws Exception {
        Engine   engine = engine();
        Response res;

        res = engine.get(url("employees/6?expands=territories"));
        assertEquals(5, res.findArray("data.0.territories").length());
        assertNotNull(res.find("data.0.territories.0.territorydescription"));
    }

    @Test
    public void testIncludes() throws Exception {
        Engine   engine = engine();
        Response res;

        res = engine.get(url("orders/10248?includes=shipname"));
        res.dump();
        res.assertOk();
        assertEquals("Vins et alcools Chevalier", res.findString("data.0.shipname"));
        //we only included 'shipname' but 'href' is always included unless it is
        //specifically excluded
        assertEquals(1, res.findNode("data.0").size());
    }

    @Test
    public void testExcludes11() throws Exception {
        Engine   engine = engine();
        Response res;

        res = engine.get(url("orders/10248?expands=customer,employee.reportsto&excludes=customer,employee.firstname,employee.reportsto.territories"));

        assertFalse(res.findNode("data.0").containsKey("customer"));
        assertTrue(res.findString("data.0.employee.href").endsWith("/employees/5"));
        assertFalse(res.findNode("data.0.employee").containsKey("firstname"));
        assertTrue(res.findString("data.0.employee.reportsto.href").endsWith("/employees/2"));
        assertFalse(res.findNode("data.0.employee.reportsto").hasProperty("territories"));
    }

    @Test
    public void testIncludes12() throws Exception {
        Engine   engine = engine();
        Response res;

        res = engine.get(url("orders/10248?expands=customer,employee.reportsto&includes=employee.reportsto.territories"));
        //res = engine.get(url("orders/10248?expands=customer,employee.reportsto"));
        res.dump();

        String collectionPath = collectionPath();

        String toMatch = JSNode.parseJson("[{\"employee\":{\"reportsto\":{\"territories\":\"http://127.0.0.1/COLLECTION_PATHemployees/2/territories\"}}}]").toString();
        toMatch = toMatch.replace("COLLECTION_PATH", collectionPath);

        assertEquals(toMatch.toLowerCase(), res.getStream().toString().toLowerCase());
    }

    @Test
    public void testTwoPartResourceKey11() throws Exception {
        Engine   engine = engine();
        Response res;

        res = engine.get(url("orderdetails/10248~11"));
        res.dump();
        assertTrue(res.findString("data.0.href").toLowerCase().endsWith("/orderdetails/10248~11"));
        assertTrue(res.findString("data.0.product").endsWith("/products/11"));
        assertTrue(res.findString("data.0.order").endsWith("/orders/10248"));

        res = engine.get(url("orders/10248/orderdetails"));
        res.dump();
        assertNotNull(res.find("meta.foundRows"));
        assertTrue(res.findString("data.0.href").toLowerCase().endsWith("/orderdetails/10248~11"));
        assertTrue(res.findString("data.1.href").toLowerCase().endsWith("/orderdetails/10248~42"));
        assertTrue(res.findString("data.2.href").toLowerCase().endsWith("/orderdetails/10248~72"));

    }

    @Test
    public void testTwoPartForeignKey11() throws Exception {
        Engine   engine = engine();
        Response res;

        res = engine.get(url("orderdetails/10248~11")).assertOk();
        assertEquals(1, res.getFoundRows());

        res = engine.get(url("orders/10248?expands=orderdetails"));
        res.dump();
        res.assertOk();
        assertTrue(res.findString("data.0.orderdetails.0.href").toLowerCase().endsWith("orderdetails/10248~11"));
        assertTrue(res.findString("data.0.orderdetails.1.href").toLowerCase().endsWith("orderdetails/10248~42"));
        assertTrue(res.findString("data.0.orderdetails.2.href").toLowerCase().endsWith("orderdetails/10248~72"));

        res = engine.get(url("orderdetails/10248~11?expands=order")).assertOk();
        assertTrue(res.findString("data.0.order.href").endsWith("/orders/10248"));
    }

    //   @Test
    //   public void testMtmRelationshipWithCompoundForeignKey11() throws Exception
    //   {
    //      Engine engine = service();
    //      Response res = null;
    //
    //      res = engine.get(url("employees/5?expands=orderdetails"));
    //      res.dump();
    //      assertTrue(res.findString("data.0.orderdetails.0.href").toLowerCase().endsWith("/orderdetails/10248~11"));
    //
    //      res = engine.get(url("employees/5/orderdetails"));
    //      assertTrue(res.findString("data.0.employees").toLowerCase().endsWith("/orderdetails/10248~11/employees"));
    //      assertTrue(res.findString("data.0.order").toLowerCase().endsWith("/orders/10248"));
    //
    //      res = engine.get(url("orderdetails/10248~11/employees"));
    //      assertTrue(res.findString("data.0.href").toLowerCase().endsWith("/employees/5"));
    //   }

    @Test
    public void testWildcardAndUnderscores() throws Exception {
        Engine   engine = engine();
        Response res;
        res = engine.get(url("indexlogs?w(error,ERROR_MSG)")).assertOk();
        res.dump();
        assertEquals((int) (Integer) res.getJson().getNode("meta").get("foundRows"), 1);

        String debug = res.getDebug().toLowerCase();
        if (debug.contains("[1]: sql ->"))//this is checking sql statements
        {
            assertTrue(debug.indexOf("args=[%error\\_msg%]") > 0);
        }

        res = engine.get(url("indexlogs?w(error,ERROR MSG)")).assertOk();

        assertEquals((int) (Integer) res.getJson().getNode("meta").get("foundRows"), 1);

        debug = res.getDebug().toLowerCase();
        if (debug.contains("[1]: sql ->"))//this is checking sql statements
        {
            assertTrue(debug.indexOf("args=[%error msg%]") > 0);
        }

        res = engine.get(url("indexlogs?eq(error,ERROR_MSG foo)")).assertOk();

        assertEquals((int) (Integer) res.getJson().getNode("meta").get("foundRows"), 1);

        debug = res.getDebug().toLowerCase();
        if (debug.contains("[1]: sql ->"))//this is checking sql statements
        {
            assertTrue(debug.indexOf("args=[error_msg foo]") > 0);
        }

    }

}
