package io.inversion.cloud.jdbc.db;

import java.sql.Connection;
import java.sql.DriverManager;

import io.inversion.cloud.jdbc.db.JdbcDb;
import io.inversion.cloud.jdbc.utils.JdbcUtils;
import io.inversion.cloud.utils.Utils;

public class PostgresUtils
{

   /**
    * By default this util intentionally tries to connect postgres on port 5433 instead of the default
    * 5432 expecting that you will run a test postgres in a docker container and we don't want the docker
    * port to potentially conflict with a native install of postgres on your dev machine.
    *  
    * You can easily run an integ ready postgres docker container with the following one liner:
    * 
    * <code>
    * docker run --name postgres95 -p 5433:5432 -e POSTGRES_PASSWORD=password -d postgres:9.5
    * <code>
    * 
    * Postgres 9.5+ is required for upsert support.
    * @see https://stackoverflow.com/questions/40327449/postgres-syntax-error-at-or-near-on
    * 
    *
    * @throws Exception
    */
   public static JdbcDb bootstrapPostgres(String database) throws Exception
   {
      database = database.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
      
      String driver = Utils.getSysEnvPropStr("postgres.driver", "org.postgresql.Driver");
      String url = Utils.getSysEnvPropStr("postgres.url", "jdbc:postgresql://localhost:5433/");
      String user = Utils.getSysEnvPropStr("postgres.user", "postgres");
      String pass = Utils.getSysEnvPropStr("postgres.pass", "password");

      Class.forName(driver).newInstance();

      Connection conn = null;
      try
      {
         conn = DriverManager.getConnection(url, user, pass);
      }
      catch (Exception ex)
      {
         String message = "Looks like we could not connect to Postgress at: " + url + ".  You can start a free dev/test Postgres server with the following Docker one liner \"docker run --name postgres95 -p 5433:5432 -e POSTGRES_PASSWORD=password -d postgres:9.5\".";
         throw new Exception(message, ex);
      }

      JdbcUtils.execute(conn, "DROP DATABASE IF EXISTS " + database);
      JdbcUtils.execute(conn, "CREATE DATABASE " + database);
      conn.close();

      JdbcDb db = new JdbcDb("postgres", driver, url + database, user, pass, JdbcDb.class.getResource("northwind-postgres.ddl").toString());
      return db;
   }
}
