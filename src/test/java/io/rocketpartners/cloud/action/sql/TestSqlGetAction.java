package io.rocketpartners.cloud.action.sql;

import org.junit.Test;

import io.rocketpartners.cloud.service.Response;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.JSObject;
import io.rocketpartners.cloud.utils.Utils;

public class TestSqlGetAction extends TestSqlActions
{

   @Test
   public void test1() throws Exception
   {

      Response res = null;
      JSObject json = null;

      Service service = service("northwind", "northwind");

      res = service.service("GET", "http://localhost/northwind/sql/orders");
      json = res.getJson();
      System.out.println(json);
      assertEquals(res.getStatusCode(), 200);
      assertEquals(json.find("meta.rowCount"), TABLE_ORDERS_ROWS);
      assertEquals(json.find("meta.pageSize"), DEFAULT_MAX_ROWS);
      assertEquals(json.find("meta.pageCount"), Utils.roundUp((int) json.find("meta.rowCount"), (int) json.find("meta.pageSize")));

      res = service.service("GET", "northwind/sql/orders?order=-orderid&page=2&pageSize=17");
      json = res.getJson();
      System.out.println(json);
      assertEquals(res.getStatusCode(), 200);
      assertEquals(json.find("meta.rowCount"), TABLE_ORDERS_ROWS);
      assertEquals(json.find("meta.pageSize"), 17);
      assertEquals(json.find("meta.pageNum"), 2);
      assertEquals(json.find("data.0.orderid"), 11060);
      
      res = service.service("GET", "northwind/sql/orders?eq(shipname, 'Blauer See Delikatessen')&pageSize=100");
      json = res.getJson();
      System.out.println(json);
      
      
      assertEquals(json.find("meta.pageSize"), 100);
      assertEquals(json.find("meta.rowCount"), 7);
      assertEquals(json.find("data.0.orderid"), 10501);
      
      assertTrue(((JSObject)json.find("data.0")).getProperty("ORDERID").getName().equals("orderid"));//test case insensativity of the JSObject but that the prop is actually lower cased
      
   }

}
