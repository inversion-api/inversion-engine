/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.cloud.action.security;

import org.junit.Test;

import junit.framework.TestCase;

/**
 * @author tc-rocket
 */
public class TestAclAction extends TestCase
{

   @Test
   public void test() throws Exception
   {
   }

   //   @Test
   //   public void testAclAction1()
   //   {
   //      Engine engine = new Engine()//
   //                                     .withApi("test")//
   //                                     .withEndpoint("GET", "blocked/*", new MockActionA())//
   //                                     .withEndpoint("GET", "open/*", new MockActionA())//
   //                                     .withAction(new AclAction().withAclRules(new AclRule().withMethods("GET").withIncludePaths("open/*")))//
   //                                     .getEngine();
   //
   //      assertEquals(403, engine.get("/test/blocked/blah").getStatusCode());
   //      assertEquals(200, engine.get("/test/open/blah").getStatusCode());
   //
   //   }
   //
   //   /**
   //    * @see io.inversion.cloud.demo.security.DemoAclRules1
   //    */
   //   @Test
   //   public void testAclAction2()
   //   {
   //      Engine engine = new Engine(Demo002AclRules.buildApi());
   //      Response res = null;
   //
   //      res = engine.get("northwind/products?limit=1");
   //      res.dump();
   //      String href = res.assertOk().findString("data.0.href");
   //      assertTrue(href != null);
   //      res.assertDebug("AclAction:", "MATCH_ALLOW rule_allow_public_read");
   //
   //      //anon users should not be able to delete from the product table
   //      res = engine.delete(href);
   //      assertEquals("should be forbidden by the acl rules", 403, res.getStatusCode());
   //      res.assertDebug("AclAction:", "NO_MATCH_DENY");
   //
   //      //anon users don't have read access to the employee table
   //      res = engine.get("northwind/employees?limit=1");
   //      assertEquals("should be forbidden by the acl rules", 403, res.getStatusCode());
   //      res.assertDebug("AclAction:", "NO_MATCH_DENY");
   //
   //      res = engine.get("northwind/employees?limit=1&username=Jack");
   //      res.dump();
   //      res.assertDebug("User: 'Jack' - perms=human_resources,manager");
   //      res.assertDebug("AclAction: MATCH_ALLOW human_resources_all");
   //
   //   }

}
