package io.rocketpartners.cloud.action.sql;

import io.rocketpartners.cloud.action.rest.TestRestGetActions;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.service.Service;

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
      System.out.println(res.getDebug());
      assertEquals(5, res.data().size());
      assertTrue(res.findString("data.0.customer").endsWith("http://localhost/northwind/source/customers/VINET"));
      assertTrue(res.findString("data.0.orderdetails").endsWith("northwind/source/orders/10248/orderdetails"));
      assertTrue(res.findString("data.0.employee").endsWith("northwind/source/employees/5"));

      res = service.get("http://localhost/northwind/source/employees/1/territories?limit=5&order=-territoryid");
      System.out.println(res.getDebug());
      assertEquals(2, res.data().size());
      assertTrue(res.findString("data.0.href").endsWith("northwind/source/territories/19713"));
      assertTrue(res.findString("data.0.employees").endsWith("northwind/source/territories/19713/employees"));
   }

   public void testExpandsOneToMany11() throws Exception
   {
      Service service = service();
      Response res = null;

      res = service.get(url("orders/10395?expands=customer,employee,employee.reportsto"));
      System.out.println(res.getDebug());
      assertTrue(res.findString("data.0.customer.href").endsWith("/customers/HILAA"));
      assertTrue(res.findString("data.0.employee.href").endsWith("/employees/6"));
      assertTrue(res.findString("data.0.employee.reportsto.href").endsWith("/employees/5"));
   }

   public void testExpandsOneToMany12() throws Exception
   {
      Service service = service();
      Response res = null;

      res = service.get(url("orders/10395?expands=employee.reportsto.employees"));
      System.out.println(res.getDebug());

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
      System.out.println(res.getDebug());

      assertEquals(3, res.findArray("data.0.employees").length());
      assertNotNull(res.find("data.0.employees.0.lastname"));
   }

   public void testExpandsManyToMany11() throws Exception
   {
      Service service = service();
      Response res = null;

      res = service.get("http://localhost/northwind/source/employees/6?expands=territories");
      System.out.println(res.getDebug());

      assertEquals(5, res.findArray("data.0.territories").length());
      assertNotNull(res.find("data.0.territories.0.territorydescription"));
   }

}
