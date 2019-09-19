package io.rocketpartners.cloud.service.config;

import org.junit.Test;

import io.rocketpartners.cloud.action.security.AuthAction;
import io.rocketpartners.cloud.action.sql.SqlDb;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.service.Service;
import junit.framework.TestCase;

public class TestConfig extends TestCase
{

   @Test
   public void testConfigSimple()
   {
      Service dev = new Service();
      dev.setProfile("dev");
      dev.setConfigPath("io/rocketpartners/cloud/service/config/");
      dev.startup();

      Api devApi = dev.getApi("northwind");
      assertEquals(10, ((SqlDb) devApi.getDb("db")).getPoolMax());
      assertEquals(0, devApi.getActions().size());

      Service prod = new Service();
      prod.setProfile("prod");
      prod.setConfigPath("io/rocketpartners/cloud/service/config/");
      prod.startup();

      Api prodApi = prod.getApi("northwind");

      assertEquals(50, ((SqlDb) prodApi.getDb("db")).getPoolMax());
      assertEquals(1, prodApi.getActions().size());
      assertTrue(prodApi.getActions().get(0) instanceof AuthAction);
   }
}
