package io.rocketpartners.cloud.action.sql;

import java.io.File;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;

import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.Sql;
import io.rocketpartners.cloud.utils.Sql.SqlListener;
import io.rocketpartners.cloud.utils.Utils;
import junit.framework.TestCase;

public class TestSqlActions extends TestCase
{
   public static final int TABLE_ORDERS_ROWS = 830;

   static boolean          initedDb          = false;

   public Service service()
   {
      return new Service()
         {
            public void init()
            {
               initDb();
               super.init();
            }
         };
   }

   /**
    * IMPORTANT - if you are running test cases in springboot/tomcat 
    * this must run AFTER tomcat is bootstrapped.
    * 
    * Seems to be a tomcat side effect dependency on being about 
    * to set java.net.URL.setURLStreamHandlerFactory
    * 
    * @throws Exception
    */
   public static synchronized void initDb()
   {
      try
      {
         if (initedDb)
            return;

         initedDb = true;

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
         Sql.runDdl(conn, TestSqlActions.class.getResourceAsStream("Northwind.H2.sql"));

         Sql.addSqlListener(new SqlListener()
            {
               @Override
               public void beforeStmt(String method, String sql, Object... vals)
               {
                  System.out.println("SQL " + method + " - " + sql.replaceAll("\r\n", " ") + " - " + new ArrayList(Arrays.asList(vals)));
               }
            });
      }
      catch (Exception ex)
      {
         ex.printStackTrace();
         Utils.rethrow(ex);
      }
   }

}
