package io.inversion.cloud.jdbc.h2;

import java.util.UUID;

import io.inversion.cloud.jdbc.db.JdbcDb;
import io.inversion.cloud.jdbc.utils.JdbcUtils;
import io.inversion.cloud.utils.Utils;

public class H2Utils
{
   public static JdbcDb bootstrapH2(String database) throws Exception
   {
      database = database.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();

      JdbcDb db = new JdbcDb("h2", //
                             "org.h2.Driver", //
                             "jdbc:h2:mem:" + database + ";IGNORECASE=TRUE;DB_CLOSE_DELAY=-1", //
                             "sa", //
                             "", //
                             JdbcDb.class.getResource("northwind-h2.ddl").toString())
         {
            protected void doShutdown()
            {
               try
               {
                  JdbcUtils.execute(getConnection(), "SHUTDOWN");
                  super.doShutdown();
               }
               catch (Exception ex)
               {
                  Utils.rethrow(ex);
               }
            }
         };
      return db;

      //      Class.forName("org.h2.Driver").newInstance();
      //      Connection conn = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID().toString() + ";IGNORECASE=TRUE", "sa", "");
      //
      //      runTests(conn, JdbcDb.class.getResource("northwind-h2.ddl").toString());

   }

}
