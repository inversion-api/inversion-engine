/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.cloud.demo.demo002;

import java.util.HashMap;
import java.util.Map;

import io.inversion.cloud.action.security.AclAction;
import io.inversion.cloud.demo.demo001.Demo001SqlDbNorthwind;
import io.inversion.cloud.model.Action;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.ApiException;
import io.inversion.cloud.model.Endpoint;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.model.SC;
import io.inversion.cloud.model.User;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.service.Engine;
import io.inversion.cloud.service.spring.InversionApp;

public class Demo002AclRules
{
   public static void main(String[] args) throws Exception
   {
      InversionApp.run(buildApi());

      System.out.println("\r\n");
      System.out.println("Your API is running at 'http://localhost:8080/northwind'.");
   }

   public static Api buildApi()
   {
      Api api = Demo001SqlDbNorthwind.buildApi();

      Action authAction = new Action()
         {
            //generally you would use AuthAction or a custom subclass to do authentication and authorization
            //and pull credentials from a dirctory or db etc.  This is here as a trivial example showing how
            //a user with permissions is attached to the request
            public void run(Engine engine, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
            {
               String username = req.getParam("username");

               Map<String, String> perms = new HashMap();
               perms.put("Jack", "human_resources,manager");
               perms.put("Diane", "sales_perms,sales_perms");
               perms.put("Bill", "sales_perms");
               perms.put("Ted", "supplychain_perms");
               perms.put("Alice", "supplychain_perms,manager");
               perms.put("Jessica", "admin_perms");

               String userPerms = perms.get(username);

               if (username != null && userPerms == null)
                  throw new ApiException(SC.SC_401_UNAUTHORIZED, "Unknown user '" + username + "'");

               if (userPerms != null)
               {
                  Chain.debug("User: '" + username + "' - perms=" + userPerms);
                  User user = new User(username, "user", userPerms);
                  Chain.peek().withUser(user);
               }
            }
         };

      authAction.withOrder(100);
      api.withAction(authAction);

      AclAction acl = new AclAction("/*");
      acl.withOrder(500);

      api.withAction(acl);

      acl.withAclRule("rule_allow_public_read", "GET", "categories/*, products/*");
      acl.withAclRule("rule_allow_human_resources_all", "GET,PUT,POST,DELETE", "territories/*,employees/*", null, "human_resources", "manager");
      acl.withAclRule("rule_allow_sales_fulfillment_all", "GET,PUT,POST,DELETE", "customers/*,customerdemographics/*,orders/*,orderdetails/*", null, "sales_perms");
      acl.withAclRule("rule_allow_sales_manager_read", "GET", "territories/*,employees/*", null, "sales_perms", "manager");
      acl.withAclRule("rule_allow_supply_chain_read", "GET", "shippers/*,suppliers/*,regions/*", null, "supplychain_perms");
      acl.withAclRule("rule_allow_supply_chain_write", "PUT,POST,DELETE", "categories/*,products/*,shippers/*,suppliers/*,regions/*", null, "supplychain_perms", "manager");
      acl.withAclRule("rule_allow_admin_all", "GET,PUT,POST,DELETE", "*", null, "admin_perms");

      return api;
   }

}
