package io.rocketpartners.cloud.action.sql;

import org.junit.Test;

import io.rocketpartners.cloud.service.Chain.ChainLocal;
import io.rocketpartners.cloud.model.Node;
import io.rocketpartners.cloud.service.Response;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.Utils;

public class TestSqlGetAction extends TestSqlActions
{

   @Test
   public void test1() throws Exception
   {

      Response res = null;
      Node json = null;

      Service service = service("northwind", "northwind");

      res = service.service("GET", "http://localhost/northwind/sql/orders");
      json = res.getJson();
      System.out.println(json);
      assertEquals(res.getStatusCode(), 200);
      assertEquals(json.find("meta.rowCount"), TABLE_ORDERS_ROWS);
      assertEquals(json.find("meta.pageSize"), DEFAULT_MAX_ROWS);
      assertEquals(json.find("meta.pageCount"), Utils.roundUp((int) json.find("meta.rowCount"), (int) json.find("meta.pageSize")));

      res = service.get("northwind/sql/orders?order=-orderid&page=2&pageSize=17");
      json = res.getJson();

      assertTrue(ChainLocal.peek() == null);

      //System.out.println(json);
      assertEquals(200, res.getStatusCode());
      assertEquals(TABLE_ORDERS_ROWS, json.find("meta.rowCount"));
      assertEquals(17, json.find("meta.pageSize"));
      assertEquals(2, json.find("meta.pageNum"));
      assertEquals(11060, json.find("data.0.orderid"));

      res = service.get("northwind/sql/orders?eq(shipname, 'Blauer See Delikatessen')&pageSize=100");
      json = res.getJson();
      System.out.println(json);

      assertEquals(100, json.find("meta.pageSize"));
      assertEquals(7, json.find("meta.rowCount"));
      assertEquals(10501, json.find("data.0.orderid"));

      assertTrue(((Node) json.find("data.0")).getProperty("ORDERID").getName().equals("orderid"));//test case insensativity of the JSObject but that the prop is actually lower cased

   }

}
