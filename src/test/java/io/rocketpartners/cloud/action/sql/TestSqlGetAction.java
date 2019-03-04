package io.rocketpartners.cloud.action.sql;

import io.rocketpartners.cloud.action.rest.TestRestGetActions;
import io.rocketpartners.cloud.model.ObjectNode;
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
      ObjectNode json = null;

      Service service = service();

      res = service.get("http://localhost/northwind/source/orders?limit=5&sort=orderid");
      System.out.println(res.getDebug());
      assertEquals(5, res.data().size());
      assertTrue(res.findString("data.0.customer").endsWith("http://localhost/northwind/source/customers/VINET"));
      assertTrue(res.findString("data.0.orderdetails").endsWith("northwind/source/orders/10248/orderdetails"));
      assertTrue(res.findString("data.0.employee").endsWith("northwind/source/employees/5"));

      res = service.get("http://localhost/northwind/source/customers/SUPRD/customerdemographics?limit=5");
      System.out.println(res.getDebug());

      res = service.get("http://localhost/northwind/source/employees?limit=5");
      System.out.println(res.getDebug());

      res = service.get("http://localhost/northwind/source/employees/1/territories?limit=5&order=-territoryid");
      System.out.println(res.getDebug());
      assertEquals(2, res.data().size());
      assertTrue(res.findString("data.0.href").endsWith("northwind/source/territories/19713"));
      assertTrue(res.findString("data.0.employees").endsWith("northwind/source/territories/19713/employees"));
      

      //      res = service.get("http://localhost/northwind/source/orders/10252/orderdetails?limit=5");
      //      System.out.println(res.getDebug());

   }

   //   @Test
   //   public void test1() throws Exception
   //   {
   //
   //      Response res = null;
   //      ObjectNode json = null;
   //
   //      Service service = service("northwind", "northwind");
   //
   //      res = service.service("GET", "http://localhost/northwind/sql/orders");
   //      json = res.getJson();
   //      System.out.println(json);
   //      assertEquals(res.getStatusCode(), 200);
   //      assertEquals(json.find("meta.rowCount"), TABLE_ORDERS_ROWS);
   //      assertEquals(json.find("meta.pageSize"), DEFAULT_MAX_ROWS);
   //      assertEquals(json.find("meta.pageCount"), Utils.roundUp((int) json.find("meta.rowCount"), (int) json.find("meta.pageSize")));
   //
   //      res = service.get("northwind/sql/orders?order=-orderid&page=2&pageSize=17");
   //      json = res.getJson();
   //
   //      assertTrue(Chain.peek() == null);
   //
   //      //System.out.println(json);
   //      assertEquals(200, res.getStatusCode());
   //      assertEquals(TABLE_ORDERS_ROWS, json.find("meta.rowCount"));
   //      assertEquals(17, json.find("meta.pageSize"));
   //      assertEquals(2, json.find("meta.pageNum"));
   //      assertEquals(11060, json.find("data.0.orderid"));
   //
   //      res = service.get("northwind/sql/orders?eq(shipname, 'Blauer See Delikatessen')&pageSize=100");
   //      json = res.getJson();
   //      System.out.println(json);
   //
   //      assertEquals(100, json.find("meta.pageSize"));
   //      assertEquals(7, json.find("meta.rowCount"));
   //      assertEquals(10501, json.find("data.0.orderid"));
   //
   //      assertTrue(((ObjectNode) json.find("data.0")).getProperty("ORDERID").getName().equals("orderid"));//test case insensativity of the JSObject but that the prop is actually lower cased
   //
   //   }

}
