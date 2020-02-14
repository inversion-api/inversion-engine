package io.inversion.cloud.jdbc.db;

import java.sql.Connection;
import java.sql.DriverManager;

import io.inversion.cloud.jdbc.utils.JdbcUtils;
import io.inversion.cloud.utils.Utils;

public class MySqlUtils
{

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
    * @throws Exception
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

      JdbcDb db = new JdbcDb("mysql", driver, url + database + "?sessionVariables=sql_mode=ANSI_QUOTES", user, pass, JdbcDb.class.getResource("northwind-mysql.ddl").toString());
      return db;
   }
}
