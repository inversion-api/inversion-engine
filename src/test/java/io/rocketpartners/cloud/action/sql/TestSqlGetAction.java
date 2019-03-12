package io.rocketpartners.cloud.action.sql;

import io.rocketpartners.cloud.action.rest.TestRestGetActions;
import io.rocketpartners.cloud.model.Response;
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

}
