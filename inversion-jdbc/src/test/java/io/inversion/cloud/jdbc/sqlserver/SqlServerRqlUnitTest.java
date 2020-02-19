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
package io.inversion.cloud.jdbc.sqlserver;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import io.inversion.cloud.jdbc.AbstractSqlRqlTest;
import io.inversion.cloud.rql.RqlValidationSuite;

@TestInstance(Lifecycle.PER_CLASS)
public class SqlServerRqlUnitTest extends AbstractSqlRqlTest
{
   public SqlServerRqlUnitTest() throws Exception
   {
      super("sqlserver");
   }

   /**
    * The majority of these should be postgres/h2 compatible.  Mysql and MsSQL 
    * will probably have to customize most of these.
    */
   @Override
   protected void customizeUnitTestSuite(RqlValidationSuite suite)
   {
      super.customizeUnitTestSuite(suite);

      suite//
           .withResult("eq", "SELECT \"orders\".* FROM \"orders\" WHERE \"orders\".\"orderID\" = ? AND \"orders\".\"shipCountry\" = ? ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[10248, France]")//
           .withResult("ne", "SELECT \"orders\".* FROM \"orders\" WHERE (NOT (\"orders\".\"shipCountry\" = ?)) ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[France]")//
           .withResult("n", "SELECT \"orders\".* FROM \"orders\" WHERE \"orders\".\"shipRegion\" IS NULL ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[]")//
           .withResult("nn", "SELECT \"orders\".* FROM \"orders\" WHERE \"orders\".\"shipRegion\" IS NOT NULL ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[]")//
           .withResult("emp", "SELECT \"orders\".* FROM \"orders\" WHERE (\"orders\".\"shipRegion\" IS NULL OR \"orders\".\"shipRegion\" = '') ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[]")//
           .withResult("nemp", "SELECT \"orders\".* FROM \"orders\" WHERE (\"orders\".\"shipRegion\" IS NOT NULL AND \"orders\".\"shipRegion\" != '') ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[]")//
           .withResult("likeMiddle", "SELECT \"orders\".* FROM \"orders\" WHERE \"orders\".\"shipCountry\" LIKE ? ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[F%ance]")//
           .withResult("likeStartsWith", "SELECT \"orders\".* FROM \"orders\" WHERE \"orders\".\"shipCountry\" LIKE ? ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[Franc%]")//
           .withResult("likeEndsWith", "SELECT \"orders\".* FROM \"orders\" WHERE \"orders\".\"shipCountry\" LIKE ? ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[%ance]")//
           .withResult("sw", "SELECT \"orders\".* FROM \"orders\" WHERE \"orders\".\"shipCountry\" LIKE ? ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[Franc%]")//
           .withResult("ew", "SELECT \"orders\".* FROM \"orders\" WHERE \"orders\".\"shipCountry\" LIKE ? ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[%nce]")//
           .withResult("w", "SELECT \"orders\".* FROM \"orders\" WHERE \"orders\".\"shipCountry\" LIKE ? ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[%ance%]")//
           .withResult("wo", "SELECT \"orders\".* FROM \"orders\" WHERE (NOT (\"orders\".\"shipCountry\" LIKE ?)) ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[%ance%]")//
           .withResult("lt", "SELECT \"orders\".* FROM \"orders\" WHERE \"orders\".\"freight\" < ? ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[10]")//
           .withResult("le", "SELECT \"orders\".* FROM \"orders\" WHERE \"orders\".\"freight\" <= ? ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[10]")//
           .withResult("gt", "SELECT \"orders\".* FROM \"orders\" WHERE \"orders\".\"freight\" > ? ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[3.67]")//
           .withResult("ge", "SELECT \"orders\".* FROM \"orders\" WHERE \"orders\".\"freight\" >= ? ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[3.67]")//
           .withResult("in", "SELECT \"orders\".* FROM \"orders\" WHERE \"orders\".\"shipCity\" IN(?, ?) ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[Reims, Charleroi]")//
           .withResult("out", "SELECT \"orders\".* FROM \"orders\" WHERE \"orders\".\"shipCity\" NOT IN(?, ?) ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[Reims, Charleroi]")//
           .withResult("and", "SELECT \"orders\".* FROM \"orders\" WHERE \"orders\".\"orderID\" = ? AND \"orders\".\"shipCountry\" = ? ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[10248, France]")//
           .withResult("or", "SELECT \"orders\".* FROM \"orders\" WHERE (\"orders\".\"shipCity\" = ? OR \"orders\".\"shipCity\" = ?) ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[Reims, Charleroi]")//
           .withResult("not", "SELECT \"orders\".* FROM \"orders\" WHERE NOT ((\"orders\".\"shipCity\" = ? OR \"orders\".\"shipCity\" = ?)) ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[Reims, Charleroi]")//
           .withResult("as", "SELECT \"orders\".*, \"orders\".\"orderid\" AS \"order_identifier\" FROM \"orders\" ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[]")//
           .withResult("includes", "SELECT \"orders\".\"shipCountry\", \"orders\".\"shipCity\", \"orders\".\"orderId\" FROM \"orders\" ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[]")//
           .withResult("distinct", "SELECT DISTINCT \"orders\".\"shipCountry\" FROM \"orders\" ORDER BY \"orders\".\"shipCountry\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[]")//
           //      .withResult("count1", "")//
           //      .withResult("count2", "")//
           //      .withResult("count3", "")//
           //      .withResult("countAs", "")//
           //      .withResult("sum", "")//
           //      .withResult("sumAs", "")//
           //      .withResult("sumIf", "")//
           //      .withResult("min", "")//
           //      .withResult("max", "")//
           .withResult("groupCount", "SELECT \"orders\".\"shipCountry\", COUNT(*) AS \"countryCount\" FROM \"orders\" GROUP BY \"orders\".\"shipCountry\" ORDER BY \"orders\".\"shipCountry\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[]")//
           .withResult("offset", "SELECT \"orders\".* FROM \"orders\" ORDER BY \"orders\".\"orderId\" ASC OFFSET 3 ROWS FETCH NEXT 100 ROWS ONLY args=[]")//
           .withResult("limit", "SELECT \"orders\".* FROM \"orders\" ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 7 ROWS ONLY args=[]")//
           .withResult("page", "SELECT \"orders\".* FROM \"orders\" ORDER BY \"orders\".\"orderId\" ASC OFFSET 14 ROWS FETCH NEXT 7 ROWS ONLY args=[]")//
           .withResult("pageNum", "SELECT \"orders\".* FROM \"orders\" ORDER BY \"orders\".\"orderId\" ASC OFFSET 14 ROWS FETCH NEXT 7 ROWS ONLY args=[]")//
           //      .withResult("after", "")//
           .withResult("sort", "SELECT \"orders\".* FROM \"orders\" ORDER BY \"orders\".\"shipCountry\" DESC, \"orders\".\"shipCity\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[]")//
           .withResult("order", "SELECT \"orders\".* FROM \"orders\" ORDER BY \"orders\".\"shipCountry\" ASC, \"orders\".\"shipCity\" DESC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[]")//

      ;

   }
}