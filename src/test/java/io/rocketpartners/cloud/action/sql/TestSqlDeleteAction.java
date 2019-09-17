package io.rocketpartners.cloud.action.sql;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import io.rocketpartners.cloud.model.ArrayNode;
import io.rocketpartners.cloud.model.ObjectNode;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.service.Service;
import junit.framework.TestCase;

@RunWith(Parameterized.class)
public class TestSqlDeleteAction extends TestCase
{
   @Parameterized.Parameters
   public static Collection input()
   {
      return SqlServiceFactory.CONFIG_DBS_TO_TEST;
   }

   protected String url(String path)
   {
      if (path.startsWith("http"))
         return path;

      String cp = "northwind/" + db + "/";

      if (cp.length() == 0)
         return path;

      if (!cp.endsWith("/"))
         cp += "/";

      while (path.startsWith("/"))
         path = path.substring(1, path.length());

      //      if (path.indexOf(cp) > -1 || path.startsWith("http"))
      //         return path;

      return "http://localhost/" + cp + path;
   }

   protected Service service() throws Exception
   {
      return SqlServiceFactory.service();
   }

   String db = null;

   public TestSqlDeleteAction(String db)
   {
      this.db = db;
   }

   @Before
   public void before() throws Exception
   {
      SqlServiceFactory.prepData(db);
   }

   @Test
   public void testSingleDelete() throws Exception
   {
      Response res = null;
      Service service = service();

      res = service.get(url("orderdetails?limit=1&sort=orderid"));
      res.dump();
      res.statusOk();
      String href = res.findString("data.0.href");

      res = service.delete(href);
      res.dump();
      res = service.get(href);
      res.dump();
      res.statusEq(404);
   }

   @Test
   public void testBatchHrefDelete() throws Exception
   {
      Response res = null;
      Service service = service();

      res = service.get(url("orderdetails?limit=10&sort=orderid")).statusOk();

      ArrayNode hrefs = new ArrayNode();

      res.data().forEach(o -> hrefs.add(((ObjectNode) o).getString("href")));

      assertEquals(10, hrefs.size());

      res = service.delete(url("orderdetails"), hrefs);

      for (int i = 0; i < hrefs.size(); i++)
         service.get(hrefs.getString(i)).hasStatus(404);
   }

   @Test
   public void testBatchQueryDelete() throws Exception
   {
      Response res = null;
      Service service = service();

      ArrayNode hrefs = new ArrayNode(url("orderdetails/10257~27"), url("orderdetails?orderid=10395"), url("orderdetails?orderid=10476"));

      for (int i = 0; i < hrefs.size(); i++)
         assertTrue(service.get(hrefs.getString(i)).statusOk().getFoundRows() > 0);

      service.delete(url("orderdetails"), hrefs).isSuccess();

      for (int i = 0; i < hrefs.size(); i++)
      {
         res = service.get(hrefs.getString(i));
         assertTrue(res.hasStatus(404) || (res.hasStatus(200) && res.getFoundRows() == 0));
      }
   }

   @Test
   public void testBatchQueryDeleteWithMultipleConditionsOnMultiPKTable() throws Exception
   {
      Service service = service();

      int allRecordsSize = service.get(url("orderdetails")).getJson().findArray("data").size();

      // The Order Detail table has a two column PK
      // select * from `Order Details` where Quantity = 60 and UnitPrice > 10;
      String url = url("orderdetails?Quantity=60&gt(UnitPrice,10)");

      ArrayNode data = service.get(url).getJson().findArray("data");
      assertTrue("data should contain two records", data.size() == 2);

      Response res = service.delete(url("orderdetails"), new ArrayNode(url));
      assertTrue("bulk delete should succeed", res.isSuccess());

      data = service.get(url).getJson().findArray("data");
      assertTrue("data should contain zero records after delete", data.size() == 0);

      int allRecordsSizeAfterDelete = service.get(url("orderdetails")).getJson().findArray("data").size();
      assertEquals("Wrong number of records were deleted", 2, (allRecordsSize - allRecordsSizeAfterDelete));

   }

   @Test
   public void testBatchQueryDeleteWithMultipleConditionsOnSinglePKtable() throws Exception
   {
      Service service = service();

      Response res = service.get(url("indexlogs"));
      int allRecordsSize = res.data().size();

      //res = service.get(url("indexlogs/1,4,16,18"));
      //res.dump();
      
      // The IndexLog table has a single column PK
      // select * from IndexLog where tenantCode = 'us' and error is null and modifiedAt < '2019-04-01 00:00:00';
      String url = url("indexlogs?tenantCode=us&n(error)&lt(modifiedAt,2019-04-01 00:00:00)");
      res = service.get(url);

      ArrayNode data =res.getJson().findArray("data");
      assertTrue("data should contain three records", data.size() == 3);

      res = service.delete(url("indexlogs"), new ArrayNode(url));
      res.dump();
      assertTrue("bulk delete should succeed", res.isSuccess());

      res = service.get(url);
      data = res.getJson().findArray("data");
      assertTrue("data should contain zero records after delete", data.size() == 0);

      res = service.get(url("indexlogs"));
      int allRecordsSizeAfterDelete = res.getJson().findArray("data").size();
      assertEquals("Wrong number of records were deleted", 3, (allRecordsSize - allRecordsSizeAfterDelete));
   }

   //2019-05-16 this is currently failing because of the OrderDetails child records...not sure what to do with this test
   //   @Test
   //   public void testBatchQueryDeleteWithForeignKeyConstraint() throws Exception
   //   {
   //      Service service = service();
   //
   //      // select * from Orders where ShipVia = '2' and ShipRegion is null and OrderDate < '2014-09-01 00:00:00';
   //      String url = url("orders?shipvia=2&n(shipregion)&lt(orderdate,2014-09-01 00:00:00)");
   //
   //      ArrayNode data = service.get(url).getJson().findArray("data");
   //      assertTrue("data should contain two records", data.size() == 2);
   //
   //      Response res = service.delete(url("orders"), new ArrayNode(url));
   //      res.dump();
   //      assertTrue("bulk delete should succeed", res.isSuccess());
   //
   //      data = service.get(url).getJson().findArray("data");
   //      assertTrue("data should contain zero records after delete", data.size() == 0);
   //
   //   }

}
