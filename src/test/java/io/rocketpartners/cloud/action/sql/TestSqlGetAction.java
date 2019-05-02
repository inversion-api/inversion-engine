package io.rocketpartners.cloud.action.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import io.rocketpartners.cloud.action.rest.TestRestGetActions;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.model.Table;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.Utils;

@RunWith(Parameterized.class)
public class TestSqlGetAction extends TestRestGetActions
{
   @Parameterized.Parameters
   public static Collection input()
   {
      return SqlServiceFactory.CONFIG_DBS_TO_TEST;
   }

   String db = null;

   public TestSqlGetAction(String db)
   {
      this.db = db;
   }

   protected String collectionPath()
   {
      return "northwind/" + db + "/";
      //return "northwind/h2/";
   }

   @Override
   protected Service service() throws Exception
   {
      return SqlServiceFactory.service();
   }

   /**
    * Makes sure that MTM link tables are not recognized as entities
    *  
    * @throws Exception
    */
   @Test
   public void testCollections() throws Exception
   {
      List c1 = new ArrayList(Arrays.asList("Categories", "CustomerDemographics", "Customers", "Employees", "IndexLogs", "Orders", "OrderDetails", "Products", "Regions", "Shippers", "Suppliers", "Territories"));

      List<String> c2 = new ArrayList();

      Api api = service().getApi("northwind");
      List<Table> tables = api.getDb(db).getTables();
      for (Table t : tables)
      {
         io.rocketpartners.cloud.model.Collection c = api.getCollection(t);
         if (c != null)
            c2.add(c.getName());
      }

      Collections.sort(c1);
      Collections.sort(c2);

      assertEquals(c1.toString().toLowerCase(), c2.toString().toLowerCase());

   }

   @Test
   public void testExcludes() throws Exception
   {
      Response res = null;
      Service service = service();

      //res = service.get("http://localhost/northwind/source/orders?limit=5&sort=orderid&excludes=href,shipname,orderdetails,customer,employee,shipvia");
      res = service.get(url("orders?limit=5&sort=orderid&excludes=href,shipname,orderdetails,customer,employee,shipvia")).statusOk();
      System.out.println(res.getDebug());
      assertNull(res.find("data.0.href"));
      assertNull(res.find("data.0.shipname"));
      assertNull(res.find("data.0.orderdetails"));
      assertNull(res.find("data.0.customer"));
      assertNull(res.find("data.0.employee"));
      assertNull(res.find("data.0.shipvia"));
   }

   @Test
   public void testRelationships1() throws Exception
   {
      Response res = null;
      Service service = service();

      //res = service.get("http://localhost/northwind/source/orders?limit=5&sort=orderid");
      res = service.get(url("orders?limit=5&sort=orderid"));
      assertEquals(5, res.data().size());
      assertTrue(res.findString("data.0.customer").endsWith("/customers/VINET"));
      assertTrue(res.findString("data.0.orderdetails").toLowerCase().endsWith("/orders/10248/orderdetails"));
      assertTrue(res.findString("data.0.employee").endsWith("/employees/5"));

      res = service.get(url("employees/1/territories?limit=5&order=-territoryid"));
      assertEquals(2, res.data().size());
      assertTrue(res.findString("data.0.href").endsWith("/territories/19713"));
      assertTrue(res.findString("data.0.employees").endsWith("/territories/19713/employees"));
   }

   @Test
   public void testExpandsOneToMany11() throws Exception
   {
      Service service = service();
      Response res = null;

      res = service.get(url("orders/10395?expands=customer,employee,employee.reportsto"));
      assertTrue(res.findString("data.0.customer.href").endsWith("/customers/HILAA"));
      assertTrue(res.findString("data.0.employee.href").endsWith("/employees/6"));
      assertTrue(res.findString("data.0.employee.reportsto.href").endsWith("/employees/5"));
   }

   @Test
   public void testExpandsOneToMany12() throws Exception
   {
      Service service = service();
      Response res = null;

      res = service.get(url("orders/10395?expands=employee.reportsto.employees"));
      assertTrue(res.findString("data.0.employee.href").endsWith("/employees/6"));
      assertTrue(res.findString("data.0.employee.reportsto.href").endsWith("/employees/5"));
      assertTrue(res.findString("data.0.employee.reportsto.employees.0.href").endsWith("/employees/6"));
      assertTrue(res.getJson().toString().indexOf("\"@link\" : \"" + url("employees/6") + "\"") > 0);
   }

   @Test
   public void testExpandsManyToOne11() throws Exception
   {
      Service service = service();
      Response res = null;

      res = service.get(url("employees/5?expands=employees"));
      assertEquals(3, res.findArray("data.0.employees").length());
      assertNotNull(res.find("data.0.employees.0.lastname"));
   }

   @Test
   public void testExpandsManyToMany11() throws Exception
   {
      Service service = service();
      Response res = null;

      res = service.get(url("employees/6?expands=territories"));
      assertEquals(5, res.findArray("data.0.territories").length());
      assertNotNull(res.find("data.0.territories.0.territorydescription"));
   }

   @Test
   public void testIncludes11() throws Exception
   {
      Service service = service();
      Response res = null;

      res = service.get(url("orders/10395?includes=shipname")).statusOk();
      assertEquals("HILARION-Abastos", res.findString("data.0.shipname"));
      assertEquals(1, res.findNode("data.0").size());
   }

   @Test
   public void testExcludes11() throws Exception
   {
      Service service = service();
      Response res = null;

      res = service.get(url("orders/10395?expands=customer,employee.reportsto&excludes=customer,employee.firstname,employee.reportsto.territories"));
      assertNull(res.findString("data.0.customer"));
      assertTrue(res.findString("data.0.employee.href").endsWith("/employees/6"));
      assertNull(res.findString("data.0.employee.firstname"));
      assertTrue(res.findString("data.0.employee.reportsto.href").endsWith("/employees/5"));
      assertNull(res.findString("data.0.employee.reportsto.territories"));
   }

   @Test
   public void testIncludes12() throws Exception
   {
      Service service = service();
      Response res = null;

      res = service.get(url("orders/10395?expands=customer,employee.reportsto&includes=employee.reportsto.territories"));

      String toMatch = Utils.parseJson("[ {\"employee\" : {\"reportsto\" : {\"territories\" : \"" + url("employees/5/territories") + "\"}}} ]").toString();
      assertEquals(toMatch.toLowerCase(), res.data().toString().toLowerCase());
   }

   @Test
   public void testTwoPartEntityKey11() throws Exception
   {
      Service service = service();
      Response res = null;

      res = service.get(url("orderdetails/10395~46"));
      res.dump();
      assertTrue(res.findString("data.0.href").toLowerCase().endsWith("/orderdetails/10395~46"));
      assertTrue(res.findString("data.0.product").endsWith("/products/46"));
      assertTrue(res.findString("data.0.order").endsWith("/orders/10395"));

      res = service.get(url("orders/10395/orderdetails"));
      assertTrue(res.find("meta.foundRows") != null);
      assertTrue(res.findString("data.0.href").toLowerCase().endsWith("/orderdetails/10395~46"));
      assertTrue(res.findString("data.1.href").toLowerCase().endsWith("/orderdetails/10395~53"));
      assertTrue(res.findString("data.2.href").toLowerCase().endsWith("/orderdetails/10395~69"));

   }

   @Test
   public void testTwoPartForeignKey11() throws Exception
   {
      Service service = service();
      Response res = null;

      res = service.get(url("orderdetails/10395~46")).statusOk();
      assertEquals(1, res.getFoundRows());

      res = service.get(url("orders/10395?expands=orderdetails")).statusOk();
      assertTrue(res.findString("data.0.orderdetails.0.href").toLowerCase().endsWith("orderdetails/10395~46"));

      res = service.get(url("orderdetails/10395~46?expands=order")).statusOk();
      assertTrue(res.findString("data.0.order.href").endsWith("/orders/10395"));
   }

   @Test
   public void testMtmRelationshipWithCompoundForeignKey11() throws Exception
   {
      Service service = service();
      Response res = null;

      res = service.get(url("employees/5?expands=orderdetails"));
      res.dump();
      assertTrue(res.findString("data.0.orderdetails.0.href").toLowerCase().endsWith("/orderdetails/10248~11"));

      res = service.get(url("employees/5/orderdetails"));
      assertTrue(res.findString("data.0.employees").toLowerCase().endsWith("/orderdetails/10248~11/employees"));
      assertTrue(res.findString("data.0.order").toLowerCase().endsWith("/orders/10248"));

      res = service.get(url("orderdetails/10248~11/employees"));
      assertTrue(res.findString("data.0.href").toLowerCase().endsWith("/employees/5"));
   }

}
