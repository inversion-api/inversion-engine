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
package io.inversion.jdbc.sqlserver;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import io.inversion.jdbc.AbstractSqlQueryRqlTest;

@TestInstance(Lifecycle.PER_CLASS)
public class SqlServerRqlUnitTest extends AbstractSqlQueryRqlTest {

    public SqlServerRqlUnitTest() throws Exception {
        super("sqlserver");

        withExpectedResult("eq", "SELECT \"orders\".* FROM \"orders\" WHERE \"orders\".\"orderID\" = ? AND \"orders\".\"shipCountry\" = ? ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[10248, France]");
        withExpectedResult("ne", "SELECT \"orders\".* FROM \"orders\" WHERE (NOT (\"orders\".\"shipCountry\" = ?)) ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[France]");
        withExpectedResult("n", "SELECT \"orders\".* FROM \"orders\" WHERE \"orders\".\"shipRegion\" IS NULL ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[]");
        withExpectedResult("nn", "SELECT \"orders\".* FROM \"orders\" WHERE \"orders\".\"shipRegion\" IS NOT NULL ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[]");
        withExpectedResult("emp", "SELECT \"orders\".* FROM \"orders\" WHERE (\"orders\".\"shipRegion\" IS NULL OR \"orders\".\"shipRegion\" = '') ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[]");
        withExpectedResult("nemp", "SELECT \"orders\".* FROM \"orders\" WHERE (\"orders\".\"shipRegion\" IS NOT NULL AND \"orders\".\"shipRegion\" != '') ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[]");
        withExpectedResult("likeMiddle", "SELECT \"orders\".* FROM \"orders\" WHERE \"orders\".\"shipCountry\" LIKE ? ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[F%ance]");
        withExpectedResult("likeStartsWith", "SELECT \"orders\".* FROM \"orders\" WHERE \"orders\".\"shipCountry\" LIKE ? ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[Franc%]");
        withExpectedResult("likeEndsWith", "SELECT \"orders\".* FROM \"orders\" WHERE \"orders\".\"shipCountry\" LIKE ? ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[%ance]");
        withExpectedResult("sw", "SELECT \"orders\".* FROM \"orders\" WHERE \"orders\".\"shipCountry\" LIKE ? ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[Franc%]");
        withExpectedResult("ew", "SELECT \"orders\".* FROM \"orders\" WHERE \"orders\".\"shipCountry\" LIKE ? ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[%nce]");
        withExpectedResult("w", "SELECT \"orders\".* FROM \"orders\" WHERE \"orders\".\"shipCountry\" LIKE ? ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[%ance%]");
        withExpectedResult("wo", "SELECT \"orders\".* FROM \"orders\" WHERE (NOT (\"orders\".\"shipCountry\" LIKE ?)) ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[%ance%]");
        withExpectedResult("lt", "SELECT \"orders\".* FROM \"orders\" WHERE \"orders\".\"freight\" < ? ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[10]");
        withExpectedResult("le", "SELECT \"orders\".* FROM \"orders\" WHERE \"orders\".\"freight\" <= ? ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[10]");
        withExpectedResult("gt", "SELECT \"orders\".* FROM \"orders\" WHERE \"orders\".\"freight\" > ? ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[3.67]");
        withExpectedResult("ge", "SELECT \"orders\".* FROM \"orders\" WHERE \"orders\".\"freight\" >= ? ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[3.67]");
        withExpectedResult("in", "SELECT \"orders\".* FROM \"orders\" WHERE \"orders\".\"shipCity\" IN(?, ?) ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[Reims, Charleroi]");
        withExpectedResult("out", "SELECT \"orders\".* FROM \"orders\" WHERE \"orders\".\"shipCity\" NOT IN(?, ?) ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[Reims, Charleroi]");
        withExpectedResult("and", "SELECT \"orders\".* FROM \"orders\" WHERE \"orders\".\"shipCity\" = ? AND \"orders\".\"shipCountry\" = ? ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[Lyon, France]");
        withExpectedResult("or", "SELECT \"orders\".* FROM \"orders\" WHERE (\"orders\".\"shipCity\" = ? OR \"orders\".\"shipCity\" = ?) ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[Reims, Charleroi]");
        withExpectedResult("not", "SELECT \"orders\".* FROM \"orders\" WHERE NOT ((\"orders\".\"shipCity\" = ? OR \"orders\".\"shipCity\" = ?)) ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[Reims, Charleroi]");
        withExpectedResult("as", "SELECT \"orders\".*, \"orders\".\"orderid\" AS \"order_identifier\" FROM \"orders\" ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[]");
        withExpectedResult("includes", "SELECT \"orders\".\"shipCountry\", \"orders\".\"shipCity\", \"orders\".\"orderId\" FROM \"orders\" ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[]");
        withExpectedResult("distinct", "SELECT DISTINCT \"orders\".\"shipCountry\" FROM \"orders\" ORDER BY \"orders\".\"shipCountry\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[]");
        //      .withResult("count1", "");
        //      .withResult("count2", "");
        //      .withResult("count3", "");
        //      .withResult("countAs", "");
        //      .withResult("sum", "");
        //      .withResult("sumAs", "");
        //      .withResult("sumIf", "");
        //      .withResult("min", "");
        //      .withResult("max", "");
        withExpectedResult("groupCount", "SELECT \"orders\".\"shipCountry\", COUNT(*) AS \"countryCount\" FROM \"orders\" GROUP BY \"orders\".\"shipCountry\" ORDER BY \"orders\".\"shipCountry\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[]");
        withExpectedResult("offset", "SELECT \"orders\".* FROM \"orders\" ORDER BY \"orders\".\"orderId\" ASC OFFSET 3 ROWS FETCH NEXT 100 ROWS ONLY args=[]");
        withExpectedResult("limit", "SELECT \"orders\".* FROM \"orders\" ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 7 ROWS ONLY args=[]");
        withExpectedResult("page", "SELECT \"orders\".* FROM \"orders\" ORDER BY \"orders\".\"orderId\" ASC OFFSET 14 ROWS FETCH NEXT 7 ROWS ONLY args=[]");
        withExpectedResult("pageNum", "SELECT \"orders\".* FROM \"orders\" ORDER BY \"orders\".\"orderId\" ASC OFFSET 14 ROWS FETCH NEXT 7 ROWS ONLY args=[]");
        //      .withResult("after", "");
        withExpectedResult("sort", "SELECT \"orders\".* FROM \"orders\" ORDER BY \"orders\".\"shipCountry\" DESC, \"orders\".\"shipCity\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[]");
        withExpectedResult("order", "SELECT \"orders\".* FROM \"orders\" ORDER BY \"orders\".\"shipCountry\" ASC, \"orders\".\"shipCity\" DESC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[]");

        withExpectedResult("onToManyExistsEq", "SELECT \"employees\".* FROM \"employees\" WHERE EXISTS (SELECT 1 FROM \"employees\" \"~~relTbl_employees\" WHERE \"employees\".\"reportsTo\" = \"~~relTbl_employees\".\"employeeId\" AND \"~~relTbl_employees\".\"firstName\" = ?) ORDER BY \"employees\".\"employeeId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[Andrew]");
        withExpectedResult("onToManyNotExistsNe", "SELECT \"employees\".* FROM \"employees\" WHERE NOT EXISTS (SELECT 1 FROM \"employees\" \"~~relTbl_employees\" WHERE \"employees\".\"reportsTo\" = \"~~relTbl_employees\".\"employeeId\" AND \"~~relTbl_employees\".\"firstName\" = ?) ORDER BY \"employees\".\"employeeId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[Andrew]");
        withExpectedResult("manyToOneExistsEq", "SELECT \"employees\".* FROM \"employees\" WHERE EXISTS (SELECT 1 FROM \"employees\" \"~~relTbl_employees\" WHERE \"employees\".\"employeeId\" = \"~~relTbl_employees\".\"reportsTo\" AND \"~~relTbl_employees\".\"firstName\" = ?) ORDER BY \"employees\".\"employeeId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[Nancy]");
        withExpectedResult("manyToOneNotExistsNe", "SELECT \"employees\".* FROM \"employees\" WHERE NOT EXISTS (SELECT 1 FROM \"employees\" \"~~relTbl_employees\" WHERE \"employees\".\"employeeId\" = \"~~relTbl_employees\".\"reportsTo\" AND \"~~relTbl_employees\".\"firstName\" = ?) ORDER BY \"employees\".\"employeeId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[Nancy]");
        withExpectedResult("manyTManyNotExistsNe",
                "SELECT \"employees\".* FROM \"employees\" WHERE NOT EXISTS (SELECT 1 FROM \"orderDetails\" \"~~relTbl_orderDetails\", \"employeeOrderDetails\" \"~~lnkTbl_employeeOrderDetails\" WHERE \"employees\".\"employeeId\" = \"~~lnkTbl_employeeOrderDetails\".\"employeeId\" AND \"~~lnkTbl_employeeOrderDetails\".\"orderId\" = \"~~relTbl_orderDetails\".\"orderId\" AND \"~~lnkTbl_employeeOrderDetails\".\"productId\" = \"~~relTbl_orderDetails\".\"productId\" AND \"~~relTbl_orderDetails\".\"quantity\" = ?) ORDER BY \"employees\".\"employeeId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[12]");
        withExpectedResult("eqNonexistantColumn", "SELECT \"orders\".* FROM \"orders\" WHERE \"orders\".\"orderId\" >= ? ORDER BY \"orders\".\"orderId\" ASC OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY args=[1000]");
    }
}