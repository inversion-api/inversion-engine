package io.rocketpartners.cloud.demo.sql;

import java.io.InputStream;

import io.rocketpartners.cloud.action.rest.RestAction;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.service.spring.SpringBoot;

public class DemoSqlDbNorthwind1
{
   String      h2File    = getClass().getSimpleName() + ".db";
   String      ddlFile   = "DemoSqlDbNorthwind1.ddl";
   InputStream ddlStream = null;

   public static void main(String[] args) throws Exception
   {
      new DemoSqlDbNorthwind1().serve();
   }

   public void serve()
   {
      SpringBoot.run(buildApi());
   }

   public Api buildApi()
   {
      Api api = new Api()//
                         .withName("northwind")//
                         .withDb(new H2SqlDb("db", getH2File(), true, getDdlStream()))//
                         .withEndpoint("GET,PUT,POST,DELETE", "/*", new RestAction());

      return api;
   }

   public String getH2File()
   {
      return h2File;
   }

   public DemoSqlDbNorthwind1 withH2File(String h2File)
   {
      this.h2File = h2File;
      return this;
   }

   public String getDdlFile()
   {
      return ddlFile;
   }

   public DemoSqlDbNorthwind1 withDdlFile(String ddlFile)
   {
      this.ddlFile = ddlFile;
      return this;
   }

   public InputStream getDdlStream()
   {
      if (ddlStream == null)
         return DemoSqlDbNorthwind1.class.getResourceAsStream(ddlFile);

      return ddlStream;
   }

   public DemoSqlDbNorthwind1 withDdlStream(InputStream ddlStream)
   {
      this.ddlStream = ddlStream;
      return this;
   }

}
