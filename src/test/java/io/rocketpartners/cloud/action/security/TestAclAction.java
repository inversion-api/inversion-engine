/**
 * 
 */
package io.rocketpartners.cloud.action.security;

import org.junit.Test;

import io.rocketpartners.cloud.demo.Demo002AclRules;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.service.MockActionA;
import io.rocketpartners.cloud.service.Service;
import junit.framework.TestCase;

/**
 * @author tc-rocket
 */
public class TestAclAction extends TestCase
{
   @Test
   public void testAclAction1()
   {
      Service service = new Service()//
                                     .withApi("test")//
                                     .withEndpoint("GET", "blocked/*", new MockActionA())//
                                     .withEndpoint("GET", "open/*", new MockActionA())//
                                     .withAction(new AclAction().withAclRules(new AclRule().withMethods("GET").withIncludePaths("open/*")))//
                                     .getService();

      assertEquals(403, service.get("/test/blocked/blah").getStatusCode());
      assertEquals(200, service.get("/test/open/blah").getStatusCode());

   }

   /**
    * @see io.rocketpartners.cloud.demo.security.DemoAclRules1
    */
   @Test
   public void testAclAction2()
   {
      Service service = new Service(Demo002AclRules.buildApi());
      Response res = null;

      res = service.get("northwind/products?limit=1");
      res.dump();
      String href = res.statusOk().findString("data.0.href");
      assertTrue(href != null);
      res.assertDebug("AclAction:", "MATCH_ALLOW rule_allow_public_read");

      //anon users should not be able to delete from the product table
      res = service.delete(href);
      assertEquals("should be forbidden by the acl rules", 403, res.getStatusCode());
      res.assertDebug("AclAction:", "NO_MATCH_DENY");

      //anon users don't have read access to the employee table
      res = service.get("northwind/employees?limit=1");
      assertEquals("should be forbidden by the acl rules", 403, res.getStatusCode());
      res.assertDebug("AclAction:", "NO_MATCH_DENY");

      res = service.get("northwind/employees?limit=1&username=Jack");
      res.dump();
      res.assertDebug("User: 'Jack' - perms=human_resources,manager");
      res.assertDebug("AclAction: MATCH_ALLOW human_resources_all");

   }



}
