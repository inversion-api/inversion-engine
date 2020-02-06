package io.inversion.cloud.jdbc.rql;

import java.sql.Connection;

import org.junit.Test;

import io.inversion.cloud.jdbc.db.JdbcDb;
import io.inversion.cloud.jdbc.utils.JdbcUtils;
import junit.framework.TestCase;

public class H2SqlTest extends TestCase
{
   JdbcDb     db   = new JdbcDb("db",                                                                          //
                                "org.h2.Driver",                                                               //
                                "jdbc:h2:mem:northwind" + System.currentTimeMillis() + ";DB_CLOSE_DELAY=-1",   //
                                "sa",                                                                          //
                                "",                                                                            //
                                JdbcDb.class.getResource("northwind-h2.ddl").toString());

   Connection conn = db.getConnection();

   @Test
   public void testH2Sql() throws Exception
   {
      run("SELECT * FROM orders LIMIT 100 OFFSET 0");
      run("SELECT * FROM \"ORDERS\" LIMIT 100 OFFSET 0");
      run("SELECT count(orderId) FROM \"ORDERS\"");
      run("SELECT count(\"ORDERS\".\"ORDERID\") FROM \"ORDERS\"");
      run("SELECT count(\"ORDERS\".*) FROM \"ORDERS\"");
   }

   public void run(String sql) throws Exception
   {
      try
      {
         JdbcUtils.execute(conn, sql);
      }
      catch (Exception ex)
      {
         ex.printStackTrace();
         fail(ex.getMessage());
      }
   }
}
