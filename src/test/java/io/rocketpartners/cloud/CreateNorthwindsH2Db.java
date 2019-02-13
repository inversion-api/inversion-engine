package io.rocketpartners.cloud;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;

import io.rocketpartners.cloud.utils.Rows;
import io.rocketpartners.cloud.utils.Sql;
import io.rocketpartners.cloud.utils.Rows.Row;

/**
 * http://optique-project.eu/training-programme/module-ontop/
 * http://www.optique-project.eu/optique-files/training-programme/Northwind.H2.sql
 * https://github.com/cjlee/northwind
 */
public class CreateNorthwindsH2Db
{
   public static void main(String[] args) throws Exception
   {
      new File("./northwind.db").delete();
      new File("./northwind.mv.db").delete();
      new File("./northwind.trace.db").delete();
      new File("./northwind.lock.db").delete();

      Class.forName("org.h2.Driver").newInstance();
      Connection conn = DriverManager.getConnection("jdbc:h2:./northwind", "sa", "");

      Sql.runDdl(conn, CreateNorthwindsH2Db.class.getResourceAsStream("Northwind.H2.sql"));
      
      DatabaseMetaData dbmd = conn.getMetaData();
      ResultSet rs = dbmd.getTables(null, null, "%", new String[]{"TABLE", "VIEW"});
      while (rs.next())
      {
         String tableCat = rs.getString("TABLE_CAT");
         String tableSchem = rs.getString("TABLE_SCHEM");
         String tableName = rs.getString("TABLE_NAME");

         System.out.println(tableName);
      }

      Rows rows = Sql.selectRows(conn, "SELECT * FROM PRODUCTS");
      for(Row row : rows)
      {
         System.out.println(row);
      }
      
      
      conn.close();

   }
}
