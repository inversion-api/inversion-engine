package io.inversion.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;

import io.inversion.Api;
import io.inversion.action.rest.RestAction;
import io.inversion.spring.InversionApp;
import io.inversion.utils.Utils;

public class JdbcDbFactory
{
   public static void main(String[] args)
   {
      InversionApp.run(new Api("northwind").withEndpoint("*", "*/", new RestAction()).withDb(buildDb("mysql", "northwind-running")));
   }

   public static JdbcDb buildDb(String type, String schemaName)
   {
      try
      {
         type = type.toLowerCase();
         switch (type)
         {
            case "h2":
               return bootstrapH2(schemaName);
            case "mysql":
               return bootstrapMySql(schemaName);
            case "postgres":
            case "redshift":
               return bootstrapPostgres(schemaName);
            case "sqlserver":
               return bootstrapSqlServer(schemaName);
         }
         throw new RuntimeException("Unsupported db type: " + type);
      }
      catch (Exception ex)
      {
         Utils.rethrow(ex);
      }
      return null;
   }

   public static JdbcDb bootstrapH2(String database) throws Exception
   {
      return bootstrapH2(database, JdbcDb.class.getResource("northwind-h2.ddl").toString());
   }

   public static JdbcDb bootstrapH2(String database, String ddlUrl)
   {
      try
      {

         database = database.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();

         JdbcDb db = new JdbcDb("h2", //
                                "org.h2.Driver", //
                                "jdbc:h2:mem:" + database + ";IGNORECASE=TRUE;DB_CLOSE_DELAY=-1", //
                                "sa", //
                                "", //
                                ddlUrl)
            {

               @Override
               protected void doShutdown()
               {
                  try
                  {
                     Connection conn = getConnection();
                     JdbcUtils.execute(conn, "SHUTDOWN");
                     JdbcConnectionLocal.closeAll();
                  }
                  catch (Exception ex)
                  {
                     //ex.printStackTrace();
                  }
                  finally
                  {
                     super.shutdown();
                  }

               }
            };
         return db;
      }
      catch (Exception ex)
      {
         Utils.rethrow(ex);
      }
      return null;

   }

   /**
    * By default this util intentionally tries to connect mysql on port 3307 instead of the default
    * 3306 expecting that you will run a test mysql in a docker container and we don't want the docker
    * port to potentially conflict with a native install of mysql on your dev machine.
    * <p>
    * You can easily run an integ ready mysql docker container with the following docker config:
    * 
    * <pre>
    * docker rm mysql57
    * docker run --name mysql57 -p 3307:3306 -e MYSQL_ROOT_PASSWORD=password -d mysql/mysql-server:5.7
    * docker exec -it mysql57 bash
    * mysql -h localhost -u root -p
    * GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' IDENTIFIED BY 'password' WITH GRANT OPTION;
    * FLUSH PRIVILEGES;
    * <pre>
    * 
    * <p>
    * Unlike Postgres and SqlServer we can't use a simple Docker one-liner because the default
    * 'root' account can not make network connections to MySql out of the box and you have to 
    * add the remote connect privilege to MySql.
    * 
    * @throws ApiException
    */
   public static JdbcDb bootstrapMySql(String database) throws Exception
   {
      database = database.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();

      String driver = Utils.getSysEnvPropStr("mysql.driver", "com.mysql.cj.jdbc.Driver");
      String url = Utils.getSysEnvPropStr("mysql.url", "jdbc:mysql://localhost:3307/");
      String user = Utils.getSysEnvPropStr("mysql.user", "root");
      String pass = Utils.getSysEnvPropStr("mysql.pass", "password");

      Class.forName(driver).newInstance();

      Connection conn = null;
      try
      {
         conn = DriverManager.getConnection(url, user, pass);
      }
      catch (Exception ex)
      {
         String message = "Looks like we could not connect to the MySql at: " + url + ".  You can start a free dev/test MySql with a few lines of Docker config.  See MySqlUtils.bootstrapMySql JavaDoc for Docker setup info.";
         throw new Exception(message, ex);
      }

      JdbcUtils.execute(conn, "DROP DATABASE IF EXISTS " + database);
      JdbcUtils.execute(conn, "CREATE DATABASE " + database);
      conn.close();

      conn = DriverManager.getConnection(url + database + "?sessionVariables=sql_mode=ANSI_QUOTES", user, pass);
      JdbcUtils.runSql(conn, JdbcDb.class.getResourceAsStream("northwind-mysql.ddl"));
      conn.close();

      //sessionVariables=sql_mode='STRICT_TRANS_TABLES,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION,PIPES_AS_CONCAT'

      JdbcDb db = new JdbcDb("mysql", driver, url + database, user, pass);
      //JdbcDb db = new JdbcDb("mysql", driver, url + database + "?sessionVariables=sql_mode=NO_ENGINE_SUBSTITUTION", user, pass, JdbcDb.class.getResource("northwind-mysql.ddl").toString());
      return db;
   }

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
    * @throws ApiException
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
