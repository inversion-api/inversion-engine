package io.inversion.cloud.action.security;

import org.junit.Test;

import io.inversion.cloud.action.rest.RestAction;
import io.inversion.cloud.action.sql.H2SqlDb;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.service.Engine;
import junit.framework.TestCase;

public class TestAuthAction extends TestCase
{
   
   /**
    * This simple factory method is static so that other  
    * demos can use and extend this api configuration.
    */
   public static Api buildApi()
   {
      return new Api()//
                      .withName("users")//
                      .withDb(new H2SqlDb("db", "users.db", TestAuthAction.class.getResource("users-h2.ddl").toString()))//
                      .withEndpoint("GET,PUT,POST,DELETE", "/*", new RestAction());
   }


   @Test
   public void testAuthAction001()
   {
      Engine e = new Engine(buildApi());
      e.startup();
      
      System.out.println("done");
   }
}
