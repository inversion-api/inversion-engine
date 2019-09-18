/**
 * 
 */
package io.rocketpartners.cloud.action.security;

import junit.framework.TestCase;

/**
 * @author tc-rocket
 */
public class TestAclAction extends TestCase
{
//   @Test
//   public void testAclAction1()
//   {
//      Service service = new Service()//
//                                     .withApi("test")//
//                                     .withEndpoint("GET", "blocked/*", new MockActionA())//
//                                     .withEndpoint("GET", "open/*", new MockActionA())//
//                                     .withAction(new AclAction().withAclRules(new AclRule().withMethods("GET").withIncludePaths("open/*")))//
//                                     .getService();
//
//      assertEquals(403, service.get("/test/blocked/blah").getStatusCode());
//      assertEquals(200, service.get("/test/open/blah").getStatusCode());
//
//   }

//   /**
//    * @see io.rocketpartners.cloud.demo.security.DemoAclRules1
//    */
//   @Test
//   public void testAclAction2()
//   {
//      Service service = new Service(new DemoAclRules1().buildApi());
//      Response res = null;
//      
////      Response res = service.get("northwind/products?limit=1");
////      res.dump();
////      String href = res.statusOk().findString("data.0.href");
////      assertTrue(href != null);
////      Utils.assertDebug(res, "ACL:", "MATCH_ALLOW public_read");
////
////      //anon users should not be able to delete from the product table
////      res = service.delete(href);
////      assertEquals("should be forbidden by the acl rules", 403, res.getStatusCode());
////      Utils.assertDebug(res, "ACL:", "NO_MATCH_DENY");
////      
////      //anon users don't have read access to the employee table
////      res = service.get("northwind/employees?limit=1");
////      assertEquals("should be forbidden by the acl rules", 403, res.getStatusCode());
////      Utils.assertDebug(res, "ACL:", "NO_MATCH_DENY");
//
//      
//
//      res = service.get("northwind/employees?limit=1&username=Jack");
//      res.dump();
//      Utils.assertDebug(res, "ACL:", "MATCH_ALLOW public_read");
//      
//
//   }
   
   
   
   
//   @Test
//   public void testAclAction3()
//   {
//      Service service = new Service(new DemoAclRules1().buildApi());
//
//      Response res = service.get("northwind/orders?limit=1");
//      String href = res.statusOk().findString("data.0.href");
//      res.dump();
//      assertTrue(href != null);
//      Utils.assertDebug(res, "ACL:", "MATCH_ALLOW public_read");
//
//      res = service.delete(href);
//      res.dump();
//
//      assertEquals("should be forbidden by the acl rules", 403, res.getStatusCode());
//      Utils.assertDebug(res, "ACL:", "NO_MATCH_DENY");
//   }
   

}
