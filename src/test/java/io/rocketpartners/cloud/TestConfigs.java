package io.rocketpartners.cloud;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.Test;

import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.sql.CreateNorthwindsH2Db;
import io.rocketpartners.utils.Rows;
import io.rocketpartners.utils.Sql;
import junit.framework.TestCase;

public class TestConfigs extends TestCase
{
   public static void main(String[] args) throws Exception
   {
      new TestConfigs().test();
   }

   @Test
   public void test() throws Exception
   {
      new File("./northwind.db").delete();
      new File("./northwind.mv.db").delete();
      new File("./northwind.trace.db").delete();
      new File("./northwind.lock.db").delete();

      Class.forName("org.h2.Driver").newInstance();

      DataSource pool = JdbcConnectionPool.create("jdbc:h2:./northwind", "sa", "");

      //Connection conn = DriverManager.getConnection("jdbc:h2:./northwind;MVCC=FALSE", "sa", "");
      Connection conn = pool.getConnection();

//      Sql.runDdl(conn, CreateNorthwindsH2Db.class.getResourceAsStream("Northwind.H2.sql"));
//
//
//      conn.commit();
      
      DatabaseMetaData dbmd = conn.getMetaData();
      ResultSet rs = dbmd.getTables(null, null, "%", new String[]{"TABLE", "VIEW"});
      while (rs.next())
      {
         String tableCat = rs.getString("TABLE_CAT");
         String tableSchem = rs.getString("TABLE_SCHEM");
         String tableName = rs.getString("TABLE_NAME");

         System.out.println(tableName);
      }

      //conn.close();

      conn = pool.getConnection();//DriverManager.getConnection("jdbc:h2:./northwind;MVCC=FALSE", "sa", "");

      Rows rows = Sql.selectRows(conn, "SELECT * FROM CATEGORIES");

      //      conn.close();
      //
      //      

      //
      //      conn = DriverManager.getConnection("jdbc:h2:./northwind", "sa", "");

      rows = Sql.selectRows(conn, "SELECT * FROM CATEGORIES");
      System.out.println(rows);

      ///conn.close();

      //      
      //      Connection conn = pool.getConnection();//
      //
      //      Sql.runDdl(conn, CreateNorthwindsH2Db.class.getResourceAsStream("Northwind.H2.sql"));
      //
      //      conn.close();

      //new File("./northwind.trace.db").delete();

      //conn = DriverManager.getConnection("jdbc:h2:./northwind", "sa", "");
      //      Class.forName("org.h2.Driver");
      //      Connection conn = DriverManager.getConnection("jdbc:h2:./northwind", "sa", "");

      //    pool =  JdbcConnectionPool.create("jdbc:h2:./northwind", "sa", "");
      //    
      //    conn = pool.getConnection();

      System.out.println(Sql.selectRows(conn, "SELECT * FROM CATEGORIES"));

      conn.close();

      //      Service service = startService("io/rocketpartners/cloud/configs/test1000/");
      //
      //      Request req = new Request(new Url("http://localhost/demo/helloworld/CATEGORIES"), "GET", new HashMap(), new HashMap(), null);
      //      Response res = new Response();
      //
      //      Chain chain = service.service(req, res);
      //
      //      JSObject json = res.getJson();
      //      System.out.println(json);
   }

   public static Service startService(String configPath)
   {
      Service service = new Service();
      service.setConfigPath(configPath);
      service.init();
      return service;
   }

}
