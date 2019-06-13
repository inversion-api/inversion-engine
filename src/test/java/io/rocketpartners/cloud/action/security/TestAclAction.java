/**
 * 
 */
package io.rocketpartners.cloud.action.security;

import org.junit.Assert;
import org.junit.Test;

import io.rocketpartners.cloud.model.AclRule;
import io.rocketpartners.cloud.service.MockActionA;
import io.rocketpartners.cloud.service.Service;

/**
 * @author tc-rocket
 *
 */
public class TestAclAction
{
   @Test
   public void testAclAction()
   {
      Service service = new Service()//
                                     .withApi("test")//
                                     .withAction(new AclAction()).getApi()//
                                     .withEndpoint("GET", "blocked/*").withAction(new MockActionA()).getApi()//
                                     .withEndpoint("GET", "open/*").withAction(new MockActionA()).getApi()//
                                     .withAclRule(new AclRule("GET").withIncludePaths("open/*"))//
                                     .getService();

      Assert.assertEquals(403, service.get("/test/blocked/blah").getStatusCode());
      Assert.assertEquals(200, service.get("/test/open/blah").getStatusCode());

   }
}
