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
      int maxRows = 7;
      Response res = null;
      JSObject json = null;

      Service service = new Service()//
                                     .withApi("northwind")//
                                     .withEndpoint("GET", "*").withAction(new SqlGetAction())//
                                     .withMaxRows(maxRows).getApi()//
                                     .withDb(new SqlDb()).withConfig("org.h2.Driver", "jdbc:h2:./northwind", "sa", "").getApi().getService();

//      res = service.service("GET", "http://localhost/northwind/orders");
//      json = res.getJson();
//      System.out.println(json);
//
//      assertEquals(json.find("meta.rowCount"), TABLE_ORDERS_ROWS);
//      assertEquals(json.find("meta.pageSize"), maxRows);
//      assertEquals(json.find("meta.pageCount"), Utils.roundUp((int) json.find("meta.rowCount"), (int) json.find("meta.pageSize")));

      res = service.service("GET", "http://localhost/northwind/orders?order=-orderid");
      json = res.getJson();
      System.out.println(json);

      assertEquals(json.find("meta.rowCount"), TABLE_ORDERS_ROWS);
   }

   //   @Test
   //   public void test2() throws Exception
   //   {
   //      //initDb();
   //
   //      SpringBoot.run(new Service()//
   //                                  .withApi("northwind")//
   //                                  .withEndpoint("GET", "*").withAction(new SqlGetAction()).withMaxRows(100).getApi()//
   //                                  .withDb(new SqlDb()).withConfig("org.h2.Driver", "jdbc:h2:./northwind", "sa", "").getApi().getService());
   //
   //      initDb();
   //
   //   }
}
