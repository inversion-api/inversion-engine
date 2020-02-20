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
package io.inversion.cloud.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.inversion.cloud.action.rest.RestAction;
import io.inversion.cloud.jdbc.db.JdbcDb;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.service.Engine;

public class TestMultipleJoins
{
   @Test
   public void testRelatedCollectionJoinSelect2() throws Exception
   {
      String crmDdlUrl = JdbcDb.class.getResource("crm-h2.ddl").toString();

      //SqlDb db = new H2SqlDb("db", "crm.db", crmDdlUrl);

      Engine e = new Engine(new Api()//
                                     .withName("crm")//
                                     .withApiCode("crm")//
                                     //.withDb(db)//

                                     .withDb(new JdbcDb("db", //the database name used as the properties key prefix when 
                                                        "org.h2.Driver", //-- jdbc driver
                                                        "jdbc:h2:mem:" + System.currentTimeMillis() + ";DB_CLOSE_DELAY=-1", //-- jdbc url 
                                                        "sa", //-- jdbc user
                                                        "", //jdbc password
                                                        //OPTIONAL: the demo db is an in-memory db that gets
                                                        //reinitialized each time with the data in "northwind-h2.ddl"
                                                        crmDdlUrl))

                                     .withEndpoint("GET,PUT,POST,DELETE", "/*", new RestAction()));

      e.startup();

      //      String sql = "";
      //      sql += "SELECT DISTINCT \"CUSTOMER\".*";
      //      sql += "FROM \"CUSTOMER\", \"IDENTIFIER\"";
      //      sql += "WHERE (\"CUSTOMER\".\"ID\" = \"IDENTIFIER\".\"CUSTOMERID\")";
      //      sql += "AND \"IDENTIFIER\".\"IDENTIFIER\" = 'SHARED'";
      //      sql += "AND \"IDENTIFIER\".\"PROVIDERCODE\" = 'vendorD_1'";
      //      System.out.println(SqlUtils.selectRows(db.getConnection(), sql));
      //

      Response res = e.get("crm/customers?identifiers.providerCode=loyalty_1");
      //      res.dump();
      //      assertTrue(res.data().size() == 1);
      //      assertEquals("http://localhost/crm/customers/1", res.find("data.0.href"));
      //
      //      res = e.get("crm/customers?identifiers.providerCode=vendorA_1");
      //      res.dump();
      //      assertTrue(res.data().size() == 2);

      res = e.get("crm/customers?identifiers.providerCode=vendorD_1&identifiers.identifier=SHARED");
      res.dump();
      assertTrue(res.data().size() == 1);
      assertEquals("http://localhost/crm/customers/1", res.find("data.0.href"));

   }
}
