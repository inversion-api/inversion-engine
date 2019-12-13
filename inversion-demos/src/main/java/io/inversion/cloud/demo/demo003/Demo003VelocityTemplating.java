/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.cloud.demo.demo003;

import io.inversion.cloud.action.rest.RestAction;
import io.inversion.cloud.action.script.ScriptAction;
import io.inversion.cloud.action.sql.SqlDb;
import io.inversion.cloud.demo.demo001.Demo001SqlDbNorthwind;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.service.spring.InversionApp;
import io.inversion.cloud.utils.Utils;

public class Demo003VelocityTemplating
{
   /**
    * This simple factory method is static so that other  
    * demos can use and extend this api configuration.
    */
   public static Api buildApi()
   {
      return new Api()//
                      .withName("northwind")//

                      //-- DATABASE CONFIGURATION OPTION #1.
                      //-- you can set your database connection information explicitly in the code here... 
                      .withDb(new SqlDb("northwind", //the database name used as the properties key prefix when 
                                        "org.h2.Driver", //-- jdbc driver
                                        "jdbc:h2:mem:northwind;DB_CLOSE_DELAY=-1", //-- jdbc url 
                                        "sa", //-- jdbc user
                                        "", //jdbc password
                                        //OPTIONAL: the demo db is an in-memory db that gets
                                        //reinitialized each time with the data in "northwind-h2.ddl"
                                        SqlDb.class.getResource("northwind-h2.ddl").toString()))

                      .withEndpoint("GET", "/db", new RestAction())//
                      .withEndpoint("GET", "/script", new ScriptAction().withScriptsDir("io/inversion/cloud/demo/demo003"));

   }

   public static void main(String[] args) throws Exception
   {
      //Launches the API on port 8080
      InversionApp.run(buildApi());

      //this simply prints out the README to the console  
      System.out.println(Utils.read(Demo001SqlDbNorthwind.class.getResourceAsStream("README.md")));
   }

}
