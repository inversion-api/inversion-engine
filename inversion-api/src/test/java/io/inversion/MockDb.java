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
package io.inversion;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.inversion.rql.Term;

public class MockDb extends Db<MockDb>
{
   String property1 = null;

   //   @Override
   //   protected void doStartup()
   //   {
   //      Table users = new Table("users")//
   //                                      .withColumn("primaryKey", "int", false)//
   //                                      .withColumn("firstName", "varchar", true)//
   //                                      .withColumn("lastName", "varchar", true);
   //      withTable(users);
   //      api.makeCollection(users, "users");
   //   }
   //
   @Override
   public Results doSelect(Collection table, List<Term> columnMappedTerms) throws ApiException
   {
      return new Results(null);
   }

   @Override
   public List<String> doUpsert(Collection table, List<Map<String, Object>> rows) throws ApiException
   {
      return Collections.EMPTY_LIST;
   }

   @Override
   public void delete(Collection table, List<Map<String, Object>> indexValues) throws ApiException
   {

   }

   public String getProperty1()
   {
      return property1;
   }

   public MockDb withProperty1(String property1)
   {
      this.property1 = property1;
      return this;
   }

}
