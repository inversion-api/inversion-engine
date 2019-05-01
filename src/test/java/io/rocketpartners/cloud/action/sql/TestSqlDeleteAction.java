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
      //return Arrays.asList(new Object[][]{{"h2"}, {"mysql"}});
      return Arrays.asList(new Object[][]{{"h2"}});
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
      SqlServiceFactory.prepData(db, url("orders"));
   }

   @Test
   public void testSingleDelete() throws Exception
   {
      Response res = null;
      Service service = service();

      res = service.get(url("orderdetails?limit=1&sort=orderid")).statusOk();
      String href = res.findString("data.0.href");

      res = service.delete(href);
      res = service.get(href).statusEq(404);
      res.dump();
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

}
