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

import io.inversion.cloud.jdbc.db.JdbcDb;
import io.inversion.cloud.rql.RqlValidationSuite;

public class MySqlRqlUnitTest extends AbstractSqlRqlTest
{
   public MySqlRqlUnitTest()
   {
      dbType = "mysql";
   }

   @Override
   protected RqlValidationSuite buildUnitTestSuite()
   {
      RqlValidationSuite suite = new RqlValidationSuite(SqlQuery.class.getName(), new JdbcDb().withType(dbType));

      suite//
           .withResult("eq", "SELECT `orders`.* FROM `orders` WHERE `orders`.`orderID` = ? AND `orders`.`shipCountry` = ? ORDER BY `orders`.`orderId` ASC LIMIT 100 args=[10248, France]")//
           .withResult("ne", "")//
           .withResult("n", "")//
           .withResult("nn", "")//
           .withResult("emp", "")//
           .withResult("nemp", "")//
           .withResult("likeMiddle", "")//
           .withResult("likeStartsWith", "")//
           .withResult("likeEndsWith", "")//
           .withResult("sw", "")//
           .withResult("ew", "")//
           .withResult("w", "")//
           .withResult("wo", "")//
           .withResult("lt", "")//
           .withResult("le", "")//
           .withResult("gt", "")//
           .withResult("ge", "")//
           .withResult("in", "")//
           .withResult("out", "")//
           .withResult("and", "")//
           .withResult("or", "")//
           .withResult("not", "")//
           .withResult("as", "")//
           .withResult("includes", "")//
           .withResult("distinct", "")//
           .withResult("count1", "")//
           .withResult("count2", "")//
           .withResult("count3", "")//
           .withResult("countAs", "")//
           .withResult("sum", "")//
           .withResult("sumAs", "")//
           .withResult("sumIf", "")//
           .withResult("min", "")//
           .withResult("max", "")//
           .withResult("groupCount", "")//
           .withResult("offset", "")//
           .withResult("limit", "")//
           .withResult("page", "")//
           .withResult("pageNum", "")//
           .withResult("after", "")//
           .withResult("sort", "")//
           .withResult("order", "")//
      //.withResult("", "")//
      //.withResult("", "")//
      ;

      return suite;
   }

}
