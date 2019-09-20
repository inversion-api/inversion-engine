/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * http://rocketpartners.io
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package io.rocketpartners.cloud.demo;

import java.util.HashMap;
import java.util.Map;

import io.rocketpartners.cloud.action.security.AclAction;
import io.rocketpartners.cloud.model.Action;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.ApiException;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.model.Request;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.model.User;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.service.Inversion;
import io.rocketpartners.cloud.service.Service;

public class Demo002AclRules
{
   public static void main(String[] args) throws Exception
   {
      Inversion.run(buildApi());
   }

   public static Api buildApi()
   {
      Api api = Demo001SqlDbNorthwind.buildApi();

      Action authAction = new Action()
         {
            //generally you would use AuthAction or a custom subclass to do authentication and authorization
            //and pull credentials from a dirctory or db etc.  This is here as a trivial example showing how
            //a user with permissions is attached to the request
            public void run(Service service, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
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
                  req.withUser(user);
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
