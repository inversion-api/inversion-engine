package io.inversion.cloud.action.sql;

import org.junit.Test;

import io.inversion.cloud.action.rest.RestAction;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.service.Engine;
import io.inversion.cloud.utils.SqlUtils;
import junit.framework.TestCase;

public class TestMultipleJoins extends TestCase
{
   @Test
   public void testRelatedCollectionJoinSelect2() throws Exception
   {
      String crmDdlUrl = TestMultipleJoins.class.getResource("crm-h2.ddl").toString();

      SqlDb db = new H2SqlDb("db", "crm.db", crmDdlUrl);

      Engine e = new Engine(new Api()//
                                     .withName("crm")//
                                     .withApiCode("crm")//
                                     .withDb(db).withEndpoint("GET,PUT,POST,DELETE", "/*", new RestAction()));

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
