package io.rocketpartners.cloud;

import java.io.File;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import io.rocketpartners.cloud.handler.sql.SqlDb;
import io.rocketpartners.cloud.handler.sql.SqlGetAction;
import io.rocketpartners.cloud.service.Response;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.service.spring.SpringBoot;
import io.rocketpartners.cloud.utils.JSObject;
import io.rocketpartners.cloud.utils.Sql;
import io.rocketpartners.cloud.utils.Utils;
import io.rocketpartners.cloud.utils.Sql.SqlListener;
import junit.framework.TestCase;

public class TestConfigs extends TestCase
{
   static
   {
      try
      {
         initDb();
      }
      catch (Exception ex)
      {
         ex.printStackTrace();
         Utils.rethrow(ex);
      }
   }

   public static void initDb() throws Exception
   {
      new File("./northwind.db").delete();
      new File("./northwind.mv.db").delete();
      new File("./northwind.trace.db").delete();
      new File("./northwind.lock.db").delete();

      SqlDb db = new SqlDb();
      db.withDriver("org.h2.Driver");
      db.withUrl("jdbc:h2:./northwind");
      db.withUser("sa");
      db.withPass("");

      Connection conn = db.getConnection();
      Sql.runDdl(conn, TestConfigs.class.getResourceAsStream("Northwind.H2.sql"));

      Sql.addSqlListener(new SqlListener()
         {
            @Override
            public void beforeStmt(String method, String sql, Object... vals)
            {
               System.out.println("SQL " + method + " - " + sql.replaceAll("\r\n", " ") + " - " + new ArrayList(Arrays.asList(vals)));
            }
         });

   }

   public static void main(String[] args) throws Exception
   {
      TestConfigs tests = new TestConfigs();
      //tests.test1();

      tests.test2();
   }

   @Test
   public void test1() throws Exception
   {
      Service service = new Service()//
                                     .withApi("test")//
                                     .withEndpoint("GET", "*").withAction(new SqlGetAction()).withMaxRows(100).getApi()//
                                     .withDb(new SqlDb()).withConfig("org.h2.Driver", "jdbc:h2:./northwind", "sa", "").getApi().getService();

      Response res = service.service("GET", "http://localhost/demo/helloworld/categories");
      JSObject json = res.getJson();
      System.out.println(json);
   }

   public void test2() throws Exception
   {
      //initDb();

      SpringBoot.run(new Service()//
                                  .withApi("test")//
                                  .withEndpoint("GET", "*").withAction(new SqlGetAction()).withMaxRows(100).getApi()//
                                  .withDb(new SqlDb()).withConfig("org.h2.Driver", "jdbc:h2:./northwind", "sa", "").getApi().getService());

      initDb();

   }

}
