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
   protected void startup0()
   {
      Table users = makeTable("users")//
                                      .makeColumn("primaryKey", "int").getTable()//
                                      .makeColumn("firstName", "varchar").getTable()//
                                      .makeColumn("lastName", "varchar").getTable();

      api.makeCollection(users, "users");

   }

   @Override
   public Results<Row> select(Table table, List<Term> columnMappedTerms) throws Exception
   {
      return null;
   }

   @Override
   public String upsert(Table table, Map<String, Object> values) throws Exception
   {
      return null;
   }

   @Override
   public void delete(Table table, String entityKey) throws Exception
   {

   }

}
