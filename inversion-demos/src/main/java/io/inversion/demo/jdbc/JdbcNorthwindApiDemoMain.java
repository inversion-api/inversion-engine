/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
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
package io.inversion.demo.jdbc;

import io.inversion.Api;
import io.inversion.action.db.DbAction;
import io.inversion.jdbc.JdbcDb;
import io.inversion.spring.main.InversionMain;
import io.inversion.utils.Utils;

/**
 * This demo launches an API that exposes SQL database tables as REST collection endpoints.
 * The demo supports full GET,PUT,POST,DELETE operations with an extensive Resource Query Language
 * (RQL) for GET requests
 *
 * @see <a href="https://github.com/inversion-api/inversion-engine">Inversion on Github</a> for more information on building awesome APIs with Inversion
 */
public class JdbcNorthwindApiDemoMain {
    /**
     * This simple factory method is static so that other
     * demos can use and extend this api configuration.
     *
     * @return the constructed Api
     */
    public static Api buildApi() {
        return new Api()//
                .withName("northwind")//

                //-- DATABASE CONFIGURATION OPTION #1.
                //-- you can set your database connection information explicitly in the code here...
                .withDb(new JdbcDb("northwindDb", //the database name used as the properties key prefix when
                        "org.h2.Driver", //-- jdbc driver
                        "jdbc:h2:mem:northwind;DB_CLOSE_DELAY=-1", //-- jdbc url
                        "sa", //-- jdbc user
                        "", //-- jdbc password
                        //-- OPTIONAL: the demo db is an in-memory db that gets
                        //-- reinitialized each time with the data in "northwind-h2.ddl"
                        JdbcDb.class.getResource("northwind-h2.ddl").toString()))

                //-- DATABASE CONFIGURATION OPTION #2 & #3
                //-- comment out the above  "withDb()" method and uncomment below
                //.withDb(new SqlDb("northwind"))

                //-- then add the following name value pairs to one of the following
                //--   - to an 'inversion.properties' file in the classpath
                //--   - OR as java system properties
                //--   - OR as environment variables
                //--
                //--  northwind.driver=${YOUR_JDBC_DRIVER}
                //--  northwind.url=${YOUR_JDBC_URL}
                //--  northwind.user=${YOUR_JDBC_USERNAME}
                //--  northwind.pass=${YOUR_JDBC_PASSWORD}

                //-- the RestAction performs CRUD operations on Db objects.
                .withEndpoint("GET,POST,PUT,PATCH,DELETE,/*", new DbAction());

    }

    public static void main(String[] args) throws Exception {
        //Launches the API on port 8080
        InversionMain.run(buildApi());

        //this simply prints out the README to the console
        System.out.println(Utils.read(JdbcNorthwindApiDemoMain.class, "README-JdbcNorthwindApiDemoMain.md"));
    }

}
