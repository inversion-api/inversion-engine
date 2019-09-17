/**
 * 
 */
package io.rocketpartners.cloud.action.security;

import org.junit.Test;

import io.rocketpartners.cloud.action.misc.StatusAction;
import io.rocketpartners.cloud.demo.security.DemoAclRules1;
import io.rocketpartners.cloud.model.Action;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.service.MockActionA;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.Utils;
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
                                     .withAction(new AclAction().withAclRules(new AclRule("GET").withIncludePaths("open/*")))//
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
      Service service = new Service(new DemoAclRules1().buildApi());

      Response res = service.get("northwind/orders?limit=1");
      String href = res.statusOk().findString("data.0.href");
      res.dump();
      assertTrue(href != null);
      Utils.assertDebug(res, "ACL:", "MATCH_ALLOW public_read");

      res = service.delete(href);
      res.dump();

      assertEquals("should be forbidden by the acl rules", 403, res.getStatusCode());
      Utils.assertDebug(res, "ACL:", "NO_MATCH_DENY");
   }

}
