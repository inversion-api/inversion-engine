package io.inversion.cloud.jdbc.db;

import java.util.UUID;

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
                             JdbcDb.class.getResource("northwind-h2.ddl").toString());
      return db;

      //      Class.forName("org.h2.Driver").newInstance();
      //      Connection conn = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID().toString() + ";IGNORECASE=TRUE", "sa", "");
      //
      //      runTests(conn, JdbcDb.class.getResource("northwind-h2.ddl").toString());

   }

}
