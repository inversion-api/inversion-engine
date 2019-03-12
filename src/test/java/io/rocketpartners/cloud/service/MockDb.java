package io.rocketpartners.cloud.service;

import java.util.List;
import java.util.Map;

import io.rocketpartners.cloud.model.Db;
import io.rocketpartners.cloud.model.Request;
import io.rocketpartners.cloud.model.Results;
import io.rocketpartners.cloud.model.Table;
import io.rocketpartners.cloud.rql.Term;
import io.rocketpartners.cloud.utils.Rows.Row;

public class MockDb extends Db<MockDb>
{

   @Override
   public void bootstrapApi()
   {
      Table users = withTable("users")//
                                      .withColumn("primaryKey", "int")//
                                      .withColumn("firstName", "varchar")//
                                      .withColumn("lastName", "varchar");

      api.withCollection(users, "users");

   }

   public Results<Row> select(Request request, Table table, List<Term> columnMappedTerms) throws Exception
   {
      return null;
   }

   public String upsert(Request request, Table table, Map<String, Object> values) throws Exception
   {
      return null;
   }

   public void delete(Request request, Table table, String entityKey) throws Exception
   {

   }

}
