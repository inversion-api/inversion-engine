package io.rocketpartners.cloud.service.config;

import org.junit.Assert;
import org.junit.Test;

import io.rocketpartners.cloud.action.rest.RestAction;
import io.rocketpartners.cloud.action.sql.SqlDb;
import io.rocketpartners.cloud.action.sql.SqlServiceFactory;
import io.rocketpartners.cloud.service.Service;
import junit.framework.TestCase;

public class TestConfig extends TestCase
{

   //   @Test
   //   public void testConfigSimple()
   //   {
   //      Service service = new Service();
   //
   //      service.setProfile("simple");
   //      service.setConfigPath("io/rocketpartners/cloud/service/config/");
   //
   //      try
   //      {
   //         service.startup();
   //         Assert.assertEquals(true, service.isConfigDebug());
   //      }
   //      catch (Exception e)
   //      {
   //         Assert.fail("service.startup() threw an exception - " + e.getMessage());
   //      }
   //
   //   }

   @Test
   public void testConfigDB()
   {
      //TODO: fix me / implemented mixed code and props config

      // Do this because I needed a static jdbc url that could be configured in the properties file
      //SqlDb h2Db = SqlServiceFactory.createDb("db", "northwind-h2.ddl", "org.h2.Driver", "jdbc:h2:./.h2/northwind-testconfig", "sa", "", "test/");

      //         Service service = new Service()//
      //                                        .withApi("test")//
      //                                        .withDb(new SqlDb().withName("db").withType("mysql"))//
      //                                        .makeEndpoint("GET,PUT,POST,DELETE", "test/", "*").withAction(new RestAction()).getApi()//
      //                                        .getService();
      //   
      //         service.setProfile("someprofile");
      //         service.setConfigPath("io/rocketpartners/cloud/service/config/");
      //   
      //         service.setConfigDebug(true);
      //         service.setConfigOut("inversion.debug.text");
      //   
      //         try
      //         {
      //            service.startup();
      //   
      //            SqlDb sqlDb = (SqlDb) service.getApi("test").getDb("db");
      //            Assert.assertEquals(40, sqlDb.getPoolMax());
      //            Assert.assertEquals(3600, sqlDb.getIdleConnectionTestPeriod());
      //   
      //         }
      //         catch (Exception e)
      //         {
      //            e.printStackTrace();
      //            Assert.fail("service.startup() threw an exception - " + e.getMessage());
      //         }

   }
}
