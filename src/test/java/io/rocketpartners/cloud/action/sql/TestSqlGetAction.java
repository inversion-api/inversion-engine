package io.rocketpartners.cloud.action.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.rocketpartners.cloud.action.rest.TestRestGetActions;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.Collection;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.model.Table;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.Utils;

public class TestSqlGetAction extends TestRestGetActions
{
   
   protected String collectionPath()
   {
      return "northwind/sql/";
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
   public void testCollections() throws Exception
   {
      List c1 = new ArrayList(Arrays.asList("Categories", "CustomerDemographics", "Customers", "Employees", "Orders", "OrderDetails", "Products", "Regions", "Shippers", "Suppliers", "Territories"));

      List<String> c2 = new ArrayList();

      Api api = service().getApi("northwind");
      List<Table> tables = api.getDb("northwind-source").getTables();
      for (Table t : tables)
      {
         Collection c = api.getCollection(t);
         if (c != null)
            c2.add(c.getName());
      }

      Collections.sort(c1);
      Collections.sort(c2);

      assertEquals(c1.toString().toLowerCase(), c2.toString().toLowerCase());

   }

   public void testExcludes() throws Exception
   {
      Response res = null;
      Service service = service();

      res = service.get("http://localhost/northwind/source/orders?limit=5&sort=orderid&excludes=href,shipname,orderdetails,customer,employee,shipvia");
      System.out.println(res.getDebug());
      assertNull(res.find("data.0.href"));
      assertNull(res.find("data.0.shipname"));
      assertNull(res.find("data.0.orderdetails"));
      assertNull(res.find("data.0.customer"));
      assertNull(res.find("data.0.employee"));
      assertNull(res.find("data.0.shipvia"));
   }
   
   
   
   public void testRelationships1() throws Exception
   {
      Response res = null;
      Service service = service();

      res = service.get("http://localhost/northwind/source/orders?limit=5&sort=orderid");
      assertEquals(5, res.data().size());
      assertTrue(res.findString("data.0.customer").endsWith("http://localhost/northwind/source/customers/VINET"));
      assertTrue(res.findString("data.0.orderdetails").endsWith("northwind/source/orders/10248/orderdetails"));
      assertTrue(res.findString("data.0.employee").endsWith("northwind/source/employees/5"));

      res = service.get("http://localhost/northwind/source/employees/1/territories?limit=5&order=-territoryid");
      assertEquals(2, res.data().size());
      assertTrue(res.findString("data.0.href").endsWith("northwind/source/territories/19713"));
      assertTrue(res.findString("data.0.employees").endsWith("northwind/source/territories/19713/employees"));
   }

   public void testExpandsOneToMany11() throws Exception
   {
      Service service = service();
      Response res = null;

      res = service.get(url("orders/10395?expands=customer,employee,employee.reportsto"));
      assertTrue(res.findString("data.0.customer.href").endsWith("/customers/HILAA"));
      assertTrue(res.findString("data.0.employee.href").endsWith("/employees/6"));
      assertTrue(res.findString("data.0.employee.reportsto.href").endsWith("/employees/5"));
   }

   public void testExpandsOneToMany12() throws Exception
   {
      Service service = service();
      Response res = null;

      res = service.get(url("orders/10395?expands=employee.reportsto.employees"));
      assertTrue(res.findString("data.0.employee.href").endsWith("/employees/6"));
      assertTrue(res.findString("data.0.employee.reportsto.href").endsWith("/employees/5"));
      assertTrue(res.findString("data.0.employee.reportsto.employees.0.href").endsWith("/employees/6"));
      assertTrue(res.getJson().toString().indexOf("\"@link\" : \"http://localhost/northwind/sql/employees/6\"") > 0);
   }

   public void testExpandsManyToOne11() throws Exception
   {
      Service service = service();
      Response res = null;

      res = service.get("http://localhost/northwind/source/employees/5?expands=employees");
      assertEquals(3, res.findArray("data.0.employees").length());
      assertNotNull(res.find("data.0.employees.0.lastname"));
   }

   public void testExpandsManyToMany11() throws Exception
   {
      Service service = service();
      Response res = null;

      res = service.get("http://localhost/northwind/source/employees/6?expands=territories");
      assertEquals(5, res.findArray("data.0.territories").length());
      assertNotNull(res.find("data.0.territories.0.territorydescription"));
   }

   public void testIncludes11() throws Exception
   {
      Service service = service();
      Response res = null;

      res = service.get("http://localhost/northwind/source/orders/10395?includes=shipname");
      assertEquals("HILARION-Abastos", res.findString("data.0.shipname"));
      assertEquals(1, res.findNode("data.0").size());
   }

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

   public void testIncludes12() throws Exception
   {
      Service service = service();
      Response res = null;

      res = service.get(url("orders/10395?expands=customer,employee.reportsto&includes=employee.reportsto.territories"));

      String toMatch = Utils.parseJson("[ {\"employee\" : {\"reportsto\" : {\"territories\" : \"http://localhost/northwind/sql/employees/5/territories\"}}} ]").toString();
      assertEquals(toMatch, res.data().toString());
   }

   public void testTwoPartEntityKey11() throws Exception
   {
      Service service = service();
      Response res = null;

      res = service.get(url("http://localhost/northwind/source/orderdetails/10395~46"));
      assertTrue(res.findString("data.0.href").endsWith("/orderdetails/10395~46"));
      assertTrue(res.findString("data.0.product").endsWith("/products/46"));
      assertTrue(res.findString("data.0.order").endsWith("/orders/10395"));

      res = service.get(url("http://localhost/northwind/source/orders/10395/orderdetails"));
      assertTrue(res.find("meta.foundRows") != null);
      assertTrue(res.findString("data.0.href").endsWith("/orderdetails/10395~46"));
      assertTrue(res.findString("data.1.href").endsWith("/orderdetails/10395~53"));
      assertTrue(res.findString("data.2.href").endsWith("/orderdetails/10395~69"));

   }

   public void testTwoPartForeignKey11() throws Exception
   {
      Service service = service();
      Response res = null;

      res = service.get(url("http://localhost/northwind/source/orderdetails/10395~46"));
      System.out.println(res.getDebug());

      res = service.get(url("http://localhost/northwind/source/orders/10395?expands=orderdetails"));
      System.out.println(res.getDebug());

      res = service.get(url("http://localhost/northwind/source/orderdetails/10395~46?expands=orders"));
      System.out.println(res.getDebug());
   }

   public void testMtmRelationshipWithCompoundForeignKey11() throws Exception
   {
      Service service = service();
      Response res = null;

      res = service.get(url("http://localhost/northwind/source/employees/5?expands=orderdetails"));
      assertTrue(res.findString("data.0.orderdetails.0.href").endsWith("/orderdetails/10248~11"));

      res = service.get(url("http://localhost/northwind/source/employees/5/orderdetails"));
      assertTrue(res.findString("data.0.employees").endsWith("/orderdetails/10248~11/employees"));
      assertTrue(res.findString("data.0.order").endsWith("/orders/10248"));

      res = service.get(url("http://localhost/northwind/source/orderdetails/10248~11/employees"));
      assertTrue(res.findString("data.0.href").endsWith("/employees/5"));

      //TODO: what about missing record count...!!!
   }

}
