package io.rocketpartners.cloud.demo.sql;

import io.rocketpartners.cloud.action.rest.RestAction;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.service.spring.SpringBoot;

public class DemoSqlDbNorthwind1
{
   String h2File  = "northwind-h2-demo.db";
   String ddlFile = "northwind-h2-demo.ddl";

   public static void main(String[] args) throws Exception
   {
      SpringBoot.run(new DemoSqlDbNorthwind1().buildService());
   }

   public Service buildService()
   {
      Api api = new Api()//
                         .withName("demo")//
                         .withDb(new H2SqlDb("db", h2File, true, DemoSqlDbNorthwind1.class.getResourceAsStream(ddlFile)))//
                         .withEndpoint("GET,PUT,POST,DELETE", "/*", new RestAction());

      Service service = new Service().withApi(api);
      return service;
   }

}
