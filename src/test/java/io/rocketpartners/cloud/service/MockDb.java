package io.rocketpartners.cloud.service;

import io.rocketpartners.cloud.model.Db;
import io.rocketpartners.cloud.model.Table;

public class MockDb extends Db
{

   @Override
   public void bootstrapApi() throws Exception
   {
      Table users = withTable("users")//
                                      .withColumn("primaryKey", "int")//
                                      .withColumn("firstName", "varchar")//
                                      .withColumn("lastName", "varchar");

      api.withCollection(users, "users");

   }

}
