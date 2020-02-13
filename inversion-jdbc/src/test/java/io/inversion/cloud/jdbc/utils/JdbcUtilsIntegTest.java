/*
 * Copyright (c) 2015-2020 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.cloud.jdbc.utils;

import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import io.inversion.cloud.jdbc.db.JdbcDb;
import junit.framework.TestCase;

public class JdbcUtilsIntegTest extends TestCase
{

   @Test
   public void test_h2Upsert() throws Exception
   {
      Class.forName("org.h2.Driver").newInstance();
      Connection conn = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID().toString() + ";IGNORECASE=TRUE", "sa", "");

      runTests(conn, JdbcDb.class.getResource("northwind-h2.ddl").toString());

      assertEquals("Maria Anders", JdbcUtils.selectValue(conn, "SELECT ContactName FROM customers WHERE CustomerID = 'ALFKI'"));
      assertEquals("UPDATED Alfreds Futterkiste", JdbcUtils.selectValue(conn, "SELECT CompanyName FROM customers WHERE CustomerID = 'ALFKI'"));
      assertEquals("UPDATED Ana Trujillo Emparedados", JdbcUtils.selectValue(conn, "SELECT CompanyName FROM customers WHERE CustomerID = 'ANATR'"));
      assertEquals("UPDATED Ana", JdbcUtils.selectValue(conn, "SELECT ContactName FROM customers WHERE CustomerID = 'ANATR'"));
      assertEquals("John Doe Co ZZZZ5", JdbcUtils.selectValue(conn, "SELECT CompanyName FROM customers WHERE CustomerID = 'ZZZZ5'"));

   }

   /**
    * Integration Environment Setup...
    * 
    * docker rm mysql57
    * docker run --name mysql57 -p 3307:3306 -e MYSQL_ROOT_PASSWORD=password -d mysql/mysql-server:5.7
    * docker exec -it mysql57 bash
    * mysql -h localhost -u root -p
    * GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' IDENTIFIED BY 'password' WITH GRANT OPTION;
    * FLUSH PRIVILEGES;
    * 
    * @throws Exception
    */
   @Test
   public void test_mysqlUpsert() throws Exception
   {
      Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
      Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3307/", "root", "password");
      JdbcUtils.execute(conn, "DROP DATABASE IF EXISTS jdbcutilsintegtest");
      JdbcUtils.execute(conn, "CREATE DATABASE jdbcutilsintegtest");
      conn.close();

      conn = DriverManager.getConnection("jdbc:mysql://localhost:3307/jdbcutilsintegtest?sessionVariables=sql_mode=ANSI_QUOTES", "root", "password");
      runTests(conn, JdbcDb.class.getResource("northwind-mysql.ddl").toString());

      assertEquals("Maria Anders", JdbcUtils.selectValue(conn, "SELECT \"ContactName\" FROM customers WHERE \"CustomerID\" = 'ALFKI'"));
      assertEquals("UPDATED Alfreds Futterkiste", JdbcUtils.selectValue(conn, "SELECT \"CompanyName\" FROM customers WHERE \"CustomerID\" = 'ALFKI'"));
      assertEquals("UPDATED Ana Trujillo Emparedados", JdbcUtils.selectValue(conn, "SELECT \"CompanyName\" FROM customers WHERE \"CustomerID\" = 'ANATR'"));
      assertEquals("UPDATED Ana", JdbcUtils.selectValue(conn, "SELECT \"ContactName\" FROM customers WHERE \"CustomerID\" = 'ANATR'"));
      assertEquals("John Doe Co ZZZZ5", JdbcUtils.selectValue(conn, "SELECT \"CompanyName\" FROM customers WHERE \"CustomerID\" = 'ZZZZ5'"));

   }

   /**
    * Postgres 9.5+ required for upsert
    * https://stackoverflow.com/questions/40327449/postgres-syntax-error-at-or-near-on
    * 
    * docker run --name postgres95 -p 5433:5432 -e POSTGRES_PASSWORD=password -d postgres:9.5
    * @throws Exception
    */
   @Test
   public void test_postgresUpsert() throws Exception
   {
      Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5433/", "postgres", "password");
      JdbcUtils.execute(conn, "DROP DATABASE IF EXISTS jdbcutilsintegtest");
      JdbcUtils.execute(conn, "CREATE DATABASE jdbcutilsintegtest");
      conn.close();

      conn = DriverManager.getConnection("jdbc:postgresql://localhost:5433/jdbcutilsintegtest", "postgres", "password");
      runTests(conn, JdbcDb.class.getResource("northwind-postgres.ddl").toString());

      assertEquals("Maria Anders", JdbcUtils.selectValue(conn, "SELECT \"ContactName\" FROM customers WHERE \"CustomerID\" = 'ALFKI'"));
      assertEquals("UPDATED Alfreds Futterkiste", JdbcUtils.selectValue(conn, "SELECT \"CompanyName\" FROM customers WHERE \"CustomerID\" = 'ALFKI'"));
      assertEquals("UPDATED Ana Trujillo Emparedados", JdbcUtils.selectValue(conn, "SELECT \"CompanyName\" FROM customers WHERE \"CustomerID\" = 'ANATR'"));
      assertEquals("UPDATED Ana", JdbcUtils.selectValue(conn, "SELECT \"ContactName\" FROM customers WHERE \"CustomerID\" = 'ANATR'"));
      assertEquals("John Doe Co ZZZZ5", JdbcUtils.selectValue(conn, "SELECT \"CompanyName\" FROM customers WHERE \"CustomerID\" = 'ZZZZ5'"));
   }

   /**
    * docker run -e 'ACCEPT_EULA=Y' -e 'SA_PASSWORD=Jmk38zZVn' -p 1434:1433 -d mcr.microsoft.com/mssql/server:2017-latest
    */
   @Test
   public void test_sqlserverlUpsert() throws Exception
   {
      Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

      Connection conn = DriverManager.getConnection("jdbc:sqlserver://localhost:1433", "sa", "Jmk38zZVn");
      JdbcUtils.execute(conn, "DROP DATABASE IF EXISTS jdbcutilsintegtest");
      JdbcUtils.execute(conn, "CREATE DATABASE jdbcutilsintegtest");
      conn.close();

      conn = DriverManager.getConnection("jdbc:sqlserver://localhost:1433;databaseName=jdbcutilsintegtest", "sa", "Jmk38zZVn");
      runTests(conn, JdbcDb.class.getResource("northwind-sqlserver.ddl").toString());
      
      
      assertEquals("Maria Anders", JdbcUtils.selectValue(conn, "SELECT \"ContactName\" FROM customers WHERE \"CustomerID\" = 'ALFKI'"));
      assertEquals("UPDATED Alfreds Futterkiste", JdbcUtils.selectValue(conn, "SELECT \"CompanyName\" FROM customers WHERE \"CustomerID\" = 'ALFKI'"));
      assertEquals("UPDATED Ana Trujillo Emparedados", JdbcUtils.selectValue(conn, "SELECT \"CompanyName\" FROM customers WHERE \"CustomerID\" = 'ANATR'"));
      assertEquals("UPDATED Ana", JdbcUtils.selectValue(conn, "SELECT \"ContactName\" FROM customers WHERE \"CustomerID\" = 'ANATR'"));
      assertEquals("John Doe Co ZZZZ5", JdbcUtils.selectValue(conn, "SELECT \"CompanyName\" FROM customers WHERE \"CustomerID\" = 'ZZZZ5'"));
   }

   public void runTests(Connection conn, String ddlUrl) throws Exception
   {
      if (ddlUrl != null)
      {
         JdbcUtils.runDdl(conn, new URL(ddlUrl).openStream());
      }

      try
      {
         runTests(conn);
      }
      finally
      {
         //JdbcUtils.close(conn);
      }

   }

   public void runTests(Connection conn) throws Exception
   {
      System.out.println();

      List returnedKeys = null;

      returnedKeys = JdbcUtils.upsert(conn, //
            "orders", //
            list("OrderID"), //
            rows(//
                  row("OrderID", 10248, "ShipCountry", "USA") //existing record w/ ShipCountry = 'France'
                  , row("OrderID", 10249, "ShipPostalCode", "00000") //existing record w/ ShipCountry = 'Germany'
                  //, row("OrderID", 12000, "CustomerID", "RATTC", "ShipCity", "Atlanta", "ShipCountry", "USA")//this is new
                  , row("CustomerID", "RATTC", "ShipCity", "Atlanta", "ShipCountry", "USA")//this is new
                  , row("CustomerID", "RATTC", "ShipCity", "Atlanta", "ShipCountry", "USA")//this is new

            )); //ShipCountry was 'Brazil

      System.out.println(returnedKeys);

      assertEquals("10248", returnedKeys.get(0) + "");
      assertEquals("10249", returnedKeys.get(1) + "");
      //assertEquals("12000", returnedKeys.get(2) + "");
      assertTrue("11078".equals(returnedKeys.get(2) + "") ||"10273".equals(returnedKeys.get(2) + "") || "1".equals(returnedKeys.get(2) + ""));
      assertTrue("11079".equals(returnedKeys.get(3) + "") ||"10274".equals(returnedKeys.get(3) + "") || "2".equals(returnedKeys.get(3) + ""));

      
      
      //      assertEquals("USA", JdbcUtils.selectValue(conn, "SELECT ShipCountry FROM Orders WHERE OrderId = 10248"));
      //      assertEquals("Germany", JdbcUtils.selectValue(conn, "SELECT ShipCountry FROM Orders WHERE OrderId = 10249"));
      //      assertEquals("00000", JdbcUtils.selectValue(conn, "SELECT ShipPostalCode FROM Orders WHERE OrderId = 10249"));
      //      assertEquals("Atlanta", JdbcUtils.selectValue(conn, "SELECT ShipCity FROM Orders WHERE OrderId = 12000"));

      returnedKeys = JdbcUtils.upsert(conn, //
            "customers", //
            list("CustomerID"), //
            rows(//
                  row("CustomerID", "ZZZZ1", "CompanyName", "John Doe Co ZZZZ1")//
                  , row("CustomerID", "ZZZZ2", "CompanyName", "John Doe Co ZZZZ2")//
                  , row("CustomerID", "ZZZZ3", "CompanyName", "John Doe Co ZZZZ3")//
                  , row("CustomerID", "ALFKI", "CompanyName", "UPDATED Alfreds Futterkiste")//
                  , row("CustomerID", "ZZZZ4", "CompanyName", "John Doe Co ZZZZ4")//
                  , row("CustomerID", "ANATR", "CompanyName", "UPDATED Ana Trujillo Emparedados", "ContactName", "UPDATED Ana")//
                  , row("CustomerID", "ZZZZ5", "CompanyName", "John Doe Co ZZZZ5")//
            ));

      assertEquals("ZZZZ1", returnedKeys.get(0) + "");
      assertEquals("ZZZZ2", returnedKeys.get(1) + "");
      assertEquals("ZZZZ3", returnedKeys.get(2) + "");
      assertEquals("ALFKI", returnedKeys.get(3) + "");
      assertEquals("ZZZZ4", returnedKeys.get(4) + "");
      assertEquals("ANATR", returnedKeys.get(5) + "");
      assertEquals("ZZZZ5", returnedKeys.get(6) + "");

      //      assertEquals("Maria Anders", JdbcUtils.selectValue(conn, "SELECT \"ContactName\" FROM customers WHERE \"CustomerID\" = 'ALFKI'"));
      //      assertEquals("UPDATED Alfreds Futterkiste", JdbcUtils.selectValue(conn, "SELECT \"CompanyName\" FROM customers WHERE \"CustomerID\" = 'ALFKI'"));
      //      assertEquals("UPDATED Ana Trujillo Emparedados", JdbcUtils.selectValue(conn, "SELECT \"CompanyName\" FROM customers WHERE \"CustomerID\" = 'ANATR'"));
      //      assertEquals("UPDATED Ana", JdbcUtils.selectValue(conn, "SELECT \"ContactName\" FROM customers WHERE \"CustomerID\" = 'ANATR'"));
      //      assertEquals("John Doe Co ZZZZ5", JdbcUtils.selectValue(conn, "SELECT \"CompanyName\" FROM customers WHERE \"CustomerID\" = 'ZZZZ5'"));

   }

   Map<String, Object> row(Object... keyValues)
   {
      Map row = new LinkedHashMap();
      for (int i = 0; i < keyValues.length - 1; i += 2)
      {
         row.put(keyValues[i], keyValues[i + 1]);
      }
      return row;
   }

   List<Map<String, Object>> rows(Map... maps)
   {
      ArrayList rows = new ArrayList();
      for (Map row : maps)
      {
         rows.add(row);
      }
      return rows;
   }

   List list(String... values)
   {
      ArrayList list = new ArrayList();
      for (Object value : values)
      {
         list.add(value);
      }
      return list;
   }
}
