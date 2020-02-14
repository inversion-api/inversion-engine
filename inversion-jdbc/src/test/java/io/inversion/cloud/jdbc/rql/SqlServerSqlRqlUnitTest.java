/*
 * Copyright (c) 2015-2020 Rocket Partners, LLC
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
package io.inversion.cloud.jdbc.rql;

import io.inversion.cloud.jdbc.db.SqlServerUtils;
import io.inversion.cloud.rql.RqlValidationSuite;

public class SqlServerSqlRqlUnitTest extends AbstractSqlRqlTest
{
   public SqlServerSqlRqlUnitTest() throws Exception
   {
      db = SqlServerUtils.bootstrapSqlServer(SqlServerSqlRqlUnitTest.class.getName());
   }

   /**
    * The majority of these should be postgres/h2 compatible.  Mysql and MsSQL 
    * will probably have to customize most of these.
    */
   @Override
   protected void customizeUnitTestSuite(RqlValidationSuite suite)
   {

      suite//
           .withResult("eq", "SELECT \"orders\".* FROM \"orders\" WHERE \"orders\".\"orderID\" = ? AND \"orders\".\"shipCountry\" = ? ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY args=[10248, France]");//
   }
}