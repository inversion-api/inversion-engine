package io.inversion.cloud.action.sql;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import io.inversion.cloud.model.JSArray;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.service.Engine;
import io.inversion.cloud.utils.Rows;
import io.inversion.cloud.utils.Rows.Row;
import io.inversion.cloud.utils.SqlUtils;
import io.inversion.cloud.utils.Utils;
import junit.framework.TestCase;

@RunWith(Parameterized.class)
public class TestSqlPostAction extends TestCase
{
   @Parameterized.Parameters
   public static Collection input()
   {
      return SqlEngineFactory.CONFIG_DBS_TO_TEST;
   }

   String db = null;

   protected String url(String path)
   {
      if (path.startsWith("http"))
         return path;

      String cp = collectionPath();

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

   protected String collectionPath()
   {
      return "northwind/" + db + "/";
   }

   Engine engine = null;

   protected Engine service() throws Exception
   {
      if (engine == null)
      {
         engine = SqlEngineFactory.service(true, false);
      }
      return engine;
   }

   public TestSqlPostAction(String db) throws Exception
   {
      this.db = db;
   }

   @Before
   public void before() throws Exception
   {
      SqlEngineFactory.prepData(db);
   }

   @Test
   public void testUpsert() throws Exception
   {
      Engine engine = service();
      Response res = null;

      SqlDb mysql = (SqlDb) engine.getApi("northwind").getDb(db);

      if (mysql.isType("mysql"))
      {
         Connection conn = mysql.getConnection();
         try
         {
            Rows rows = SqlUtils.selectRows(conn, "SELECT * FROM Orders WHERE OrderID in(10257, 10395, 10476, 10486)");

            for (Row row : rows)
            {
               row.put("shipaddress", "testing_upsert");
            }

            Map clone1 = new HashMap(rows.get(0));
            clone1.remove("OrderID");

            Map clone2 = new HashMap(rows.get(0));
            clone2.put("OrderID", 1);

            List<Map<String, Object>> toUpsert = new ArrayList(rows);
            toUpsert.add(clone1);
            toUpsert.add(clone2);
            List generatedKeys = SqlUtils.mysqlUpsert(conn, "Orders", toUpsert);

            //[10257, 10395, 10476, 10486, 222001, 1]

            assertEquals("11078", generatedKeys.get(4));//should be next auto increment key
            assertEquals("1", generatedKeys.get(5));
         }
         finally
         {
            conn.close();
         }

      }

   }

   @Test
   public void testAddOneRecord() throws Exception
   {
      Response res = null;
      Engine engine = service();

      //the bootstrap process copies 25 orders into the orders table, they are not sequential
      res = engine.get(url("orders?limit=100&sort=orderid"));
      System.out.println(res.getDebug());
      assertEquals(25, res.find("meta.foundRows")); //25 rows are copied by the bootstrap process, 11058 is last one

      //post one new bogus order
      res = engine.post(url("orders"), new JSNode("orderid", 100, "shipaddress", "somewhere in atlanta", "shipcity", "atlanta").toString());
      res.dump();
      assertEquals(res.find("data.0.href"), url("orders/100"));

      //check the values we sent are the values we got back
      res = engine.get(res.findString("data.0.href"));
      assertEquals("somewhere in atlanta", res.find("data.0.shipaddress"));
      assertEquals("atlanta", res.find("data.0.shipcity"));

      //check total records
      res = engine.get(url("orders?limit=25&sort=orderid"));
      assertEquals(26, res.find("meta.foundRows"));
      assertEquals(res.find("data.0.href"), url("orders/100"));
   }

   @Test
   public void testNestedPost1() throws Exception
   {
      Response res = null;
      Engine engine = service();

      JSNode john = JSNode.parseJsonNode(Utils.read(getClass().getResourceAsStream("upsert001/upsert001-1.json")));

      res = engine.get(url("employees?employeeId=5&expands=employees"));
      JSNode steve = res.findNode("data.0");

      assertEquals(3, res.findArray("data.0.employees").size());

      //steve.findArray("employees").clear();
      steve.findArray("employees").add(john);

      System.out.println(steve);
      res = engine.put(steve.getString("href"), steve);
      res.dump();

      res = engine.get(url("employees?employeeId=5&expands=employees"));
      res.dump();

      assertEquals("the new employee was not related to its parent", 4, res.findArray("data.0.employees").size());

      //-- make sure the new employee was POSTED
      res = engine.get(url("employees/99999991?expands=reportsTo,territories"));
      assertEquals(1, res.data().size());
      assertTrue(res.findString("data.0.href").contains("/99999991"));
      assertTrue(res.findString("data.0.reportsTo.href").contains("employees/5"));
      assertTrue(res.findString("data.0.territories.0.TerritoryID").equals("30346"));

      res = engine.get(res.findString("data.0.territories.0.href") + "?expands=region");

      //-- confirms that a the new region was created and assigned to territory 30346
      assertEquals("http://localhost/northwind/h2/regions/5", res.findString("data.0.region.href"));
      assertEquals("HotLanta", res.findString("data.0.region.regiondescription"));

      //--now go back to steve and unhook several many-to-one reportsTo relationships
      res = engine.get(url("employees?employeeId=5&expands=employees"));
      steve = res.findNode("data.0");

      JSArray employees = steve.findArray("employees");

      for (int i = 0; i < employees.length(); i++)
      {
         if (!"99999991".equals(employees.getNode(i).getString("employeeId")))
         {
            employees.remove(i);
            i--;
         }
      }

      res = engine.put(steve.getString("href"), steve);
      res = engine.get(url("employees?employeeId=5&expands=employees"));

      assertEquals(1, res.findArray("data.0.employees").size());
      assertTrue(res.findString("data.0.employees.0.href").contains("/99999991"));
      res.dump();

      //-- now unhook all many-to-one employees...this a different case than unhooking some but not all  
      res = engine.get(url("employees?employeeId=5&expands=employees"));
      steve = res.findNode("data.0");

      employees = steve.findArray("employees");
      employees.clear();

      res = engine.put(steve.getString("href"), steve);
      res = engine.get(url("employees?employeeId=5&expands=employees"));

      assertEquals(0, res.findArray("data.0.employees").size());

      //-- now unhook all many-to-many employee->territories...this a different case than unhooking some but not all  
      res = engine.get(url("employees?employeeId=5&expands=territories"));
      assertEquals(7, res.findArray("data.0.territories").size());
      JSNode manager = res.findNode("data.0");
      manager.findArray("territories").clear();

      res = engine.put(manager.getString("href"), manager);
      res = engine.get(url("employees?employeeId=5&expands=territories"));
      assertEquals(0, res.findArray("data.0.territories").size());

      res.dump();
   }

}
