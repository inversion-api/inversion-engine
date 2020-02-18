package io.inversion.cloud.jdbc.h2;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.inversion.cloud.jdbc.db.JdbcDb;
import io.inversion.cloud.jdbc.utils.JdbcUtils;

public class H2SqlTest
{
   JdbcDb     db   = new JdbcDb("db",                                                                          //
                                "org.h2.Driver",                                                               //
                                "jdbc:h2:mem:northwind" + UUID.randomUUID().toString() + ";DB_CLOSE_DELAY=-1",   //
                                "sa",                                                                          //
                                "",                                                                            //
                                JdbcDb.class.getResource("northwind-h2.ddl").toString());

   

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
         JdbcUtils.execute(db.getConnection(), sql);
      }
      catch (Exception ex)
      {
         ex.printStackTrace();
         fail(ex.getMessage());
      }
   }
}
