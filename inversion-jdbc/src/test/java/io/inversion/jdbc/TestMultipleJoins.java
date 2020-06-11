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
package io.inversion.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import io.inversion.Api;
import io.inversion.Chain;
import io.inversion.Db;
import io.inversion.Engine;
import io.inversion.Response;
import io.inversion.action.db.DbAction;

@TestInstance(Lifecycle.PER_CLASS)
public class TestMultipleJoins
{
   Engine engine = null;
   Api    api    = null;
   Db     db     = null;

   @BeforeAll
   public void beforeAll_initializeEngine()
   {
      Chain.resetAll();
      JdbcConnectionLocal.closeAll();
      
      String crmDdlUrl = JdbcDb.class.getResource("crm-h2.ddl").toString();
      db = JdbcDbFactory.bootstrapH2(getClass().getName(), crmDdlUrl);

      api = new Api()//
                     .withName("crm")//
                     .withDb(db)//
                     .withEndpoint("GET,PUT,POST,DELETE", "/*", new DbAction());

      engine = new Engine(api);
      engine.startup();
   }

   @AfterAll
   public void afterAll_finalizeEngine()
   {
      if (db != null)
      {
         db.shutdown();
      }
   }

   @Test
   public void testRelatedCollectionJoinSelect2() throws Exception
   {

      //      String sql = "";
      //      sql += "SELECT DISTINCT \"CUSTOMER\".*";
      //      sql += "FROM \"CUSTOMER\", \"IDENTIFIER\"";
      //      sql += "WHERE (\"CUSTOMER\".\"ID\" = \"IDENTIFIER\".\"CUSTOMERID\")";
      //      sql += "AND \"IDENTIFIER\".\"IDENTIFIER\" = 'SHARED'";
      //      sql += "AND \"IDENTIFIER\".\"PROVIDERCODE\" = 'vendorD_1'";
      //      System.out.println(SqlUtils.selectRows(db.getConnection(), sql));
      //

      Response res = engine.get("crm/customers?identifiers.providerCode=loyalty_1");
      //      res.dump();
      //      assertTrue(res.data().size() == 1);
      //      assertEquals("http://localhost/crm/customers/1", res.find("data.0.href"));
      //
      //      res = e.get("crm/customers?identifiers.providerCode=vendorA_1");
      //      res.dump();
      //      assertTrue(res.data().size() == 2);

      res = engine.get("crm/customers?identifiers.providerCode=vendorD_1&identifiers.identifier=SHARED");
      res.dump();
      assertTrue(res.getData().size() == 1);
      assertEquals("http://localhost/crm/customers/1", res.find("data.0.href"));

   }
}
