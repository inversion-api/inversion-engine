package io.rocketpartners.cloud.action.sql;

import io.rocketpartners.cloud.model.ArrayNode;
import io.rocketpartners.cloud.model.ObjectNode;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.service.Service;
import junit.framework.TestCase;

public class TestSqlDeleteAction extends TestCase
{

   protected String collectionPath()
   {
      return "northwind/sql/";
   }

   protected Service service() throws Exception
   {
      return SqlServiceFactory.service();
   }

   public void testSingleDelete() throws Exception
   {
      Response res = null;
      Service service = service();

      String href = "http://localhost/northwind/source/orderdetails/10248~11";
      res = service.get(href).statusOk();
      res.dump();

      assertEquals(1, res.getFoundRows());

      res = service.delete(href);
      res = service.get(href).statusEq(404);
      res.dump();
   }

   public void testBatchHrefDelete() throws Exception
   {
      Response res = null;
      Service service = service();

      res = service.get("http://localhost/northwind/source/orderdetails?limit=10").statusOk();

      ArrayNode hrefs = new ArrayNode();

      res.data().forEach(o -> hrefs.add(((ObjectNode) o).getString("href")));

      assertEquals(10, hrefs.size());

      res = service.delete("http://localhost/northwind/source/orderdetails", hrefs);

      for (int i = 0; i < hrefs.size(); i++)
         service.get(hrefs.getString(i)).hasStatus(404);

   }

   public void testBatchQueryDelete() throws Exception
   {
      Response res = null;
      Service service = service();

      res = service.get("http://localhost/northwind/source/orderdetails?limit=100").statusOk();
      res.dump();

      ArrayNode hrefs = new ArrayNode("http://localhost/northwind/source/orderdetails/10255~36", "http://localhost/northwind/source/orderdetails?orderid=10254", "http://localhost/northwind/source/orderdetails?orderid=10284");

      for (int i = 0; i < hrefs.size(); i++)
         assertTrue(service.get(hrefs.getString(i)).statusOk().getFoundRows() > 0);

      service.delete("http://localhost/northwind/source/orderdetails", hrefs).isSuccess();

      for (int i = 0; i < hrefs.size(); i++)
      {
         res = service.get(hrefs.getString(i));
         assertTrue(res.hasStatus(404) || (res.hasStatus(200) && res.getFoundRows() == 0));
      }

//      res = service.get("http://localhost/northwind/source/orderdetails?limit=100").statusOk();
//      res.dump();
   }

}
