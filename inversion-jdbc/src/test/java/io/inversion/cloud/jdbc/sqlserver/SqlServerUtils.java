package io.inversion.cloud.jdbc.sqlserver;

import java.sql.Connection;
import java.sql.DriverManager;

import io.inversion.cloud.jdbc.db.JdbcDb;
import io.inversion.cloud.jdbc.utils.JdbcUtils;
import io.inversion.cloud.utils.Utils;

public class SqlServerUtils
{
   /**
    * By default this util intentionally tries to connect to SqlServer on port 1434 instead of the default
    * 1433 expecting that you will run a test sqlserver in a docker container and we don't want the docker
    * port to potentially conflict with a native install of sqlserver on your dev machine.
    *  
    * You can easily run an integ ready sqlserver docker container with the following one liner:
    * <pre>
    *   docker run --name sqlserver2017 -e 'ACCEPT_EULA=Y' -e 'SA_PASSWORD=Jmk38zZVn' -p 1434:1433 -d mcr.microsoft.com/mssql/server:2017-latest
    * <pre>
    */
   public static JdbcDb bootstrapSqlServer(String database) throws Exception
   {
      database = database.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();

      String driver = Utils.getSysEnvPropStr("sqlserver.driver", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
      String url = Utils.getSysEnvPropStr("sqlserver.url", "jdbc:sqlserver://localhost:1434");
      String user = Utils.getSysEnvPropStr("sqlserver.user", "sa");
      String pass = Utils.getSysEnvPropStr("sqlserver.pass", "Jmk38zZVn");

      Class.forName(driver).newInstance();

      Connection conn = null;
      try
      {
         conn = DriverManager.getConnection(url, user, pass);
      }
      catch (Exception ex)
      {
         String message = "Looks like we could not connect to the SqlServer at: " + url + ".  You can start a free dev/test SqlServer with the following Docker one liner \"docker run --name sqlserver2017 -e 'ACCEPT_EULA=Y' -e 'SA_PASSWORD=Jmk38zZVn' -p 1434:1433 -d mcr.microsoft.com/mssql/server:2017-latest\".";
         throw new Exception(message, ex);
      }

      JdbcUtils.execute(conn, "DROP DATABASE IF EXISTS " + database);
      JdbcUtils.execute(conn, "CREATE DATABASE " + database);
      conn.close();

      JdbcDb db = new JdbcDb("sqlserver", driver, url + ";databaseName=" + database, user, pass, JdbcDb.class.getResource("northwind-sqlserver.ddl").toString());
      return db;

   }
}
