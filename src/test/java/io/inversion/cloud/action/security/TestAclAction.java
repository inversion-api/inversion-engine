/**
 * 
 */
package io.inversion.cloud.action.security;

import org.junit.Test;

import io.inversion.cloud.demo.Demo002AclRules;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.service.Engine;
import io.inversion.cloud.service.MockActionA;
import junit.framework.TestCase;

/**
 * @author tc-rocket
 */
public class TestAclAction extends TestCase
{
   @Test
   public void testAclAction1()
   {
      Engine engine = new Engine()//
                                     .withApi("test")//
                                     .withEndpoint("GET", "blocked/*", new MockActionA())//
                                     .withEndpoint("GET", "open/*", new MockActionA())//
                                     .withAction(new AclAction().withAclRules(new AclRule().withMethods("GET").withIncludePaths("open/*")))//
                                     .getEngine();

      assertEquals(403, engine.get("/test/blocked/blah").getStatusCode());
      assertEquals(200, engine.get("/test/open/blah").getStatusCode());

   }

   /**
    * @see io.inversion.cloud.demo.security.DemoAclRules1
    */
   @Test
   public void testAclAction2()
   {
      Engine engine = new Engine(Demo002AclRules.buildApi());
      Response res = null;

      res = engine.get("northwind/products?limit=1");
      res.dump();
      String href = res.statusOk().findString("data.0.href");
      assertTrue(href != null);
      res.assertDebug("AclAction:", "MATCH_ALLOW rule_allow_public_read");

      //anon users should not be able to delete from the product table
      res = engine.delete(href);
      assertEquals("should be forbidden by the acl rules", 403, res.getStatusCode());
      res.assertDebug("AclAction:", "NO_MATCH_DENY");

      //anon users don't have read access to the employee table
      res = engine.get("northwind/employees?limit=1");
      assertEquals("should be forbidden by the acl rules", 403, res.getStatusCode());
      res.assertDebug("AclAction:", "NO_MATCH_DENY");

      res = engine.get("northwind/employees?limit=1&username=Jack");
      res.dump();
      res.assertDebug("User: 'Jack' - perms=human_resources,manager");
      res.assertDebug("AclAction: MATCH_ALLOW human_resources_all");

   }



}
