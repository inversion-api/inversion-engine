package io.rocketpartners.cloud.demo;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;

import org.h2.tools.Server;

import io.rocketpartners.cloud.action.rest.RestAction;
import io.rocketpartners.cloud.action.sql.SqlDb;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.service.spring.SpringBoot;
import io.rocketpartners.cloud.utils.SqlUtils;
import io.rocketpartners.cloud.utils.Utils;

public class DemoSqlDb
{
   static final String DEMO_DB_DRIVER = "org.h2.Driver";
   static final String DEMO_DB_URL    = "jdbc:h2:tcp://localhost:9092/nio:~/.inversion_demo/northwind.h2.db;AUTO_SERVER=TRUE";
   static final String DEMO_DB_USER   = "sa";
   static final String DEMO_DB_PASS   = "";
   static final String DEMO_DDL       = "northwind-h2-demo.ddl";

   public static void main(String[] args) throws Exception
   {
      Service service = new Service()
         {
            @Override
            public void startup0()
            {
               try
               {
                  //--
                  //-- This first part is simply initializing an H2 in memory DB 
                  //-- that will  be used by the demo.  This rebuilds the DB every
                  //-- time you restart because the service configured below 
                  //-- allows you to read and write to the db.  If you delete a 
                  //-- bunch of demo data on run run while experimenting, it 
                  //-- will be restored the next time you run...so try to break
                  //-- stuff and have fun.
                  File f = new File(System.getProperty("user.home"), ".inversion_demo");
                  System.out.println("Deleting old demo db dir: " + f.getCanonicalPath());
                  Utils.delete(f);

                  //-- starts an H2 TCP server on port 9092
                  Server.createTcpServer().start();

                  //-- runs the DDL that initializes the DB
                  Class.forName(DEMO_DB_DRIVER);
                  Connection conn = DriverManager.getConnection(DEMO_DB_URL, DEMO_DB_USER, DEMO_DB_PASS);
                  SqlUtils.runDdl(conn, DemoSqlDb.class.getResourceAsStream(DEMO_DDL));
                  conn.commit();
                  conn.close();
                  //--
                  //-- END traditional JDBC / H2 initialization.

                  //construct the SqlDb that will connect to the H2 db and 
                  //reflectively construct the REST collection endpoints
                  //that are mapped to tables
                  SqlDb db = new SqlDb("db");
                  db.withDriver(DEMO_DB_DRIVER);
                  db.withUrl(DEMO_DB_URL);
                  db.withUser(DEMO_DB_USER);
                  db.withPass(DEMO_DB_PASS);

                  //now wire up the service
                  withApi(new Api("demo")//
                                         .withDb(db)//
                                         .withEndpoint("GET,PUT,POST,DELETE", "/*", new RestAction()));

                  String msg = "\n\n";
                  msg += "\n CONGRATULATIONS YOUR DEMO IS RUNNING!";
                  msg += "\n";
                  msg += "\n Inversion inspected the database meta data and automatically created ";
                  msg += "\n REST collections mapped to the H2 tables. ";
                  msg += "\n";
                  msg += "\n Simply point your web browser to one of the following urls to start exploring! ";
                  msg += "\n   - http://localhost:8080/demo/categories ";
                  msg += "\n   - http://localhost:8080/demo/customers ";
                  msg += "\n   - http://localhost:8080/demo/employees";
                  msg += "\n   - http://localhost:8080/demo/products";
                  msg += "\n   - http://localhost:8080/demo/regions";
                  msg += "\n   - http://localhost:8080/demo/shippers";
                  msg += "\n   - http://localhost:8080/demo/suppliers";
                  msg += "\n   - http://localhost:8080/demo/territories";
                  msg += "\n   - http://localhost:8080/demo/orders";
                  msg += "\n   - http://localhost:8080/demo/orderDetails";
                  msg += "\n";
                  msg += "\n";
                  msg += "\n";

               }
               catch (Exception ex)
               {
                  Utils.rethrow(ex);
               }
            }
         };

      SpringBoot.run(service);
   }

}
