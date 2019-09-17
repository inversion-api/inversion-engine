package io.rocketpartners.cloud.demo.security;

import io.rocketpartners.cloud.action.rest.RestAction;
import io.rocketpartners.cloud.action.security.AclAction;
import io.rocketpartners.cloud.action.security.AclRule;
import io.rocketpartners.cloud.demo.sql.DemoSqlDbNorthwind1;
import io.rocketpartners.cloud.demo.sql.H2SqlDb;
import io.rocketpartners.cloud.model.Api;

public class DemoAclRules1 extends DemoSqlDbNorthwind1
{
   public static void main(String[] args) throws Exception
   {
      new DemoAclRules1().serve();
   }

   public Api buildApi()
   {
      Api api = new Api()//
                         .withName("northwind")//
                         .withDb(new H2SqlDb("db", getH2File(), true, getDdlStream()))//
                         .withEndpoint("GET,PUT,POST,DELETE", "/*", new RestAction());

      AclAction acl = new AclAction("/*");

      acl.withAclRules(AclRule.allowIfUserHasAllPermissions("require_write", "PUT,POST,DELETE", "*", null, "write"));
      acl.withAclRules(AclRule.allowAny("public_read", "GET",  "*", null));
      
      api.withAction(acl);
      return api;
   }

}
