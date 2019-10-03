/*
 * Copyright (c) 2015-2019 Inversion.org, LLC
 * http://inversion.io
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package io.inversion.cloud.demo;

import io.inversion.cloud.action.rest.RestAction;
import io.inversion.cloud.action.sql.H2SqlDb;
import io.inversion.cloud.action.sql.SqlDb;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.service.Inversion;

/**
 * This simple demo launches an API that exposes SQL database tables 
 * as REST collection endpoints.  The demo supports full GET,PUT,POST,DELETE
 * operations with an extensive Resource Query Language (RQL) for GET
 * requests.
 * <br>  
 * The demo connects to an in memory H2 sql db that gets initialized from
 * scratch each time this demo is run.  That means you can fully explore
 * modifying operations (PUT,POST,DELETE) and 'break' whatever you want
 * then restart and have a clean demo app again.
 * <br>
 * If you want to explore your own JDBC DB, you can swap the "withDb()"
 * line below with the commented out one and fill in your connection info.
 * Currently, Inversion only ships with MySQL drivers out of the box but
 * has SQL syntax support for MySQL, SqlServer, and PostgreSQL.
 * <br>
 * Northwind is a demo db that has shipped with various Microsoft products
 * for years.  Some of its table designs seem strange or antiquated 
 * compared to modern conventions but it makes a great demo and test
 * specifically because it shows how Inversion can accommodate a broad
 * range of database design patterns.  
 * 
 * @see Demo1SqlDbNorthwind.ddl for more details on the db
 * @see https://github.com/RocketPartners/rocket-inversion for more information 
 *      on building awesome APIs with Inversion
 *  
 * @author wells
 *
 */
public class Demo001SqlDbNorthwind
{
   /**
    * This simple factory method is static so that other  
    * demos can use and extend this api configuration.
    */
   public static Api buildApi()
   {
      return new Api()//
                      .withName("northwind")//
                      .withDb(new H2SqlDb("db", "DemoSqlDbNorthwind1.db", Demo001SqlDbNorthwind.class.getResource("northwind.h2.ddl").toString()))//
                      //.withDb(new SqlDb("db", "YOUR_JDBC_DRIVER", "YOUR_JDBC_URL", "YOUR_JDBC_USERNAME", "YOUR_JDBC_PASSWORD")))//
                      .withEndpoint("GET,PUT,POST,DELETE", "/*", new RestAction());
   }

   public static void main(String[] args) throws Exception
   {
      Inversion.run(buildApi());
      
      System.out.println("\r\n");
      System.out.println("Your API is running at 'http://localhost:8080/northwind'.");
      System.out.println("REST collection endpoints have been created for each db entity");
      System.out.println("");
      System.out.println("You can get started by exploring some of these urls:");
      System.out.println("  - GET http://localhost:8080/northwind/products");
      System.out.println("  - GET http://localhost:8080/northwind/orders?expands=orderDetails&page=2");
      System.out.println("  - GET http://localhost:8080/northwind/customers?in(country,France,Spain)&sort=-customerid&pageSize=10");
      System.out.println("  - GET http://localhost:8080/northwind/customers?orders.shipCity=Mannheim");
      System.out.println("");
      System.out.println("Append '&explain=true' to any query string to see an explanation of what is happening under the covers");
      System.out.println("  - GET http://localhost:8080/northwind/employees?title='Sales Representative'&sort=employeeid&pageSize=2&page=2&explain=true");
      System.out.println("");
      System.out.println("See https://github.com/RocketPartners/rocket-inversion for more information on building awesome APIs with Inversion");

   }

}
