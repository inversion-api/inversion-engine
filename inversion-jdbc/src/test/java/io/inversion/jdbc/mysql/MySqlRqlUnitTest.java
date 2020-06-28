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
package io.inversion.jdbc.mysql;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import io.inversion.jdbc.AbstractSqlQueryRqlTest;
import io.inversion.rql.RqlValidationSuite;

@TestInstance(Lifecycle.PER_CLASS)
public class MySqlRqlUnitTest extends AbstractSqlQueryRqlTest {

   public MySqlRqlUnitTest() throws Exception {
      super("mysql");
   }

   @Override
   protected void customizeUnitTestSuite(RqlValidationSuite suite) {
      super.customizeUnitTestSuite(suite);

      suite//
           .withResult("eq", "SELECT `orders`.* FROM `orders` WHERE `orders`.`orderID` = ? AND `orders`.`shipCountry` = ? ORDER BY `orders`.`orderId` ASC LIMIT 100 args=[10248, France]")//
           .withResult("ne", "SELECT `orders`.* FROM `orders` WHERE (NOT (`orders`.`shipCountry` = ?)) ORDER BY `orders`.`orderId` ASC LIMIT 100 args=[France]")//
           .withResult("n", "SELECT `orders`.* FROM `orders` WHERE `orders`.`shipRegion` IS NULL ORDER BY `orders`.`orderId` ASC LIMIT 100 args=[]")//
           .withResult("nn", "SELECT `orders`.* FROM `orders` WHERE `orders`.`shipRegion` IS NOT NULL ORDER BY `orders`.`orderId` ASC LIMIT 100 args=[]")//
           .withResult("emp", "SELECT `orders`.* FROM `orders` WHERE (`orders`.`shipRegion` IS NULL OR `orders`.`shipRegion` = '') ORDER BY `orders`.`orderId` ASC LIMIT 100 args=[]")//
           .withResult("nemp", "SELECT `orders`.* FROM `orders` WHERE (`orders`.`shipRegion` IS NOT NULL AND `orders`.`shipRegion` != '') ORDER BY `orders`.`orderId` ASC LIMIT 100 args=[]")//
           .withResult("likeMiddle", "SELECT `orders`.* FROM `orders` WHERE `orders`.`shipCountry` LIKE ? ORDER BY `orders`.`orderId` ASC LIMIT 100 args=[F%ance]")//
           .withResult("likeStartsWith", "SELECT `orders`.* FROM `orders` WHERE `orders`.`shipCountry` LIKE ? ORDER BY `orders`.`orderId` ASC LIMIT 100 args=[Franc%]")//
           .withResult("likeEndsWith", "SELECT `orders`.* FROM `orders` WHERE `orders`.`shipCountry` LIKE ? ORDER BY `orders`.`orderId` ASC LIMIT 100 args=[%ance]")//
           .withResult("sw", "SELECT `orders`.* FROM `orders` WHERE `orders`.`shipCountry` LIKE ? ORDER BY `orders`.`orderId` ASC LIMIT 100 args=[Franc%]")//
           .withResult("ew", "SELECT `orders`.* FROM `orders` WHERE `orders`.`shipCountry` LIKE ? ORDER BY `orders`.`orderId` ASC LIMIT 100 args=[%nce]")//
           .withResult("w", "SELECT `orders`.* FROM `orders` WHERE `orders`.`shipCountry` LIKE ? ORDER BY `orders`.`orderId` ASC LIMIT 100 args=[%ance%]")//
           .withResult("wo", "SELECT `orders`.* FROM `orders` WHERE (NOT (`orders`.`shipCountry` LIKE ?)) ORDER BY `orders`.`orderId` ASC LIMIT 100 args=[%ance%]")//
           .withResult("lt", "SELECT `orders`.* FROM `orders` WHERE `orders`.`freight` < ? ORDER BY `orders`.`orderId` ASC LIMIT 100 args=[10]")//
           .withResult("le", "SELECT `orders`.* FROM `orders` WHERE `orders`.`freight` <= ? ORDER BY `orders`.`orderId` ASC LIMIT 100 args=[10]")//
           .withResult("gt", "SELECT `orders`.* FROM `orders` WHERE `orders`.`freight` > ? ORDER BY `orders`.`orderId` ASC LIMIT 100 args=[3.67]")//
           .withResult("ge", "SELECT `orders`.* FROM `orders` WHERE `orders`.`freight` >= ? ORDER BY `orders`.`orderId` ASC LIMIT 100 args=[3.67]")//
           .withResult("in", "SELECT `orders`.* FROM `orders` WHERE `orders`.`shipCity` IN(?, ?) ORDER BY `orders`.`orderId` ASC LIMIT 100 args=[Reims, Charleroi]")//
           .withResult("out", "SELECT `orders`.* FROM `orders` WHERE `orders`.`shipCity` NOT IN(?, ?) ORDER BY `orders`.`orderId` ASC LIMIT 100 args=[Reims, Charleroi]")//
           .withResult("and", "SELECT `orders`.* FROM `orders` WHERE `orders`.`shipCity` = ? AND `orders`.`shipCountry` = ? ORDER BY `orders`.`orderId` ASC LIMIT 100 args=[Lyon, France]")//
           .withResult("or", "SELECT `orders`.* FROM `orders` WHERE (`orders`.`shipCity` = ? OR `orders`.`shipCity` = ?) ORDER BY `orders`.`orderId` ASC LIMIT 100 args=[Reims, Charleroi]")//
           .withResult("not", "SELECT `orders`.* FROM `orders` WHERE NOT ((`orders`.`shipCity` = ? OR `orders`.`shipCity` = ?)) ORDER BY `orders`.`orderId` ASC LIMIT 100 args=[Reims, Charleroi]")//
           .withResult("as", "SELECT `orders`.*, `orders`.`orderid` AS 'order_identifier' FROM `orders` ORDER BY `orders`.`orderId` ASC LIMIT 100 args=[]")//
           .withResult("includes", "SELECT `orders`.`shipCountry`, `orders`.`shipCity`, `orders`.`orderId` FROM `orders` ORDER BY `orders`.`orderId` ASC LIMIT 100 args=[]")//
           .withResult("distinct", "SELECT DISTINCT `orders`.`shipCountry` FROM `orders` ORDER BY `orders`.`shipCountry` ASC LIMIT 100 args=[]")//
           .withResult("count1", "SELECT `orders`.*, COUNT(*) FROM `orders` ORDER BY `orders`.`orderId` ASC LIMIT 100 args=[]")//
           .withResult("count2", "SELECT `orders`.*, COUNT(?) FROM `orders` ORDER BY `orders`.`orderId` ASC LIMIT 100 args=[1]")//
           .withResult("count3", "SELECT `orders`.*, COUNT(`orders`.`shipRegion`) FROM `orders` ORDER BY `orders`.`orderId` ASC LIMIT 100 args=[]")//
           .withResult("countAs", "SELECT `orders`.*, COUNT(*) AS 'countOrders' FROM `orders` ORDER BY `orders`.`orderId` ASC LIMIT 100 args=[]")//
           .withResult("sum", "SELECT `orders`.*, SUM(`orders`.`freight`) FROM `orders` ORDER BY `orders`.`orderId` ASC LIMIT 100 args=[]")//
           .withResult("sumAs", "SELECT `orders`.*, SUM(`orders`.`freight`) AS 'Sum Freight' FROM `orders` ORDER BY `orders`.`orderId` ASC LIMIT 100 args=[]")//
           .withResult("sumIf", "SELECT `orders`.*, SUM(IF(`orders`.`shipCountry` = ?, 1, 0)) AS 'French Orders' FROM `orders` ORDER BY `orders`.`orderId` ASC LIMIT 100 args=[France]")//
           .withResult("min", "SELECT `orders`.*, MIN(`orders`.`freight`) FROM `orders` ORDER BY `orders`.`orderId` ASC LIMIT 100 args=[]")//
           .withResult("max", "SELECT `orders`.*, MAX(`orders`.`freight`) FROM `orders` ORDER BY `orders`.`orderId` ASC LIMIT 100 args=[]")//
           .withResult("groupCount", "SELECT `orders`.`shipCountry`, COUNT(*) AS 'countryCount' FROM `orders` GROUP BY `orders`.`shipCountry` ORDER BY `orders`.`shipCountry` ASC LIMIT 100 args=[]")//
           .withResult("offset", "SELECT `orders`.* FROM `orders` ORDER BY `orders`.`orderId` ASC LIMIT 3, 100 args=[]")//
           .withResult("limit", "SELECT `orders`.* FROM `orders` ORDER BY `orders`.`orderId` ASC LIMIT 7 args=[]")//
           .withResult("page", "SELECT `orders`.* FROM `orders` ORDER BY `orders`.`orderId` ASC LIMIT 14, 7 args=[]")//
           .withResult("pageNum", "SELECT `orders`.* FROM `orders` ORDER BY `orders`.`orderId` ASC LIMIT 14, 7 args=[]")//
           .withResult("after", "SELECT `orders`.* FROM `orders` ORDER BY `orders`.`orderId` ASC LIMIT 100 args=[]")//
           .withResult("sort", "SELECT `orders`.* FROM `orders` ORDER BY `orders`.`shipCountry` DESC, `orders`.`shipCity` ASC LIMIT 100 args=[]")//
           .withResult("order", "SELECT `orders`.* FROM `orders` ORDER BY `orders`.`shipCountry` ASC, `orders`.`shipCity` DESC LIMIT 100 args=[]")//
           .withResult("onToManyExistsEq", "SELECT `employees`.* FROM `employees` WHERE EXISTS (SELECT 1 FROM `employees` `~~relTbl_employees` WHERE `employees`.`reportsTo` = `~~relTbl_employees`.`employeeId` AND `~~relTbl_employees`.`firstName` = ?) ORDER BY `employees`.`employeeId` ASC LIMIT 100 args=[Andrew]")//
           .withResult("onToManyNotExistsNe", "SELECT `employees`.* FROM `employees` WHERE NOT EXISTS (SELECT 1 FROM `employees` `~~relTbl_employees` WHERE `employees`.`reportsTo` = `~~relTbl_employees`.`employeeId` AND `~~relTbl_employees`.`firstName` = ?) ORDER BY `employees`.`employeeId` ASC LIMIT 100 args=[Andrew]")//
           .withResult("manyToOneExistsEq", "SELECT `employees`.* FROM `employees` WHERE EXISTS (SELECT 1 FROM `employees` `~~relTbl_employees` WHERE `employees`.`employeeId` = `~~relTbl_employees`.`reportsTo` AND `~~relTbl_employees`.`firstName` = ?) ORDER BY `employees`.`employeeId` ASC LIMIT 100 args=[Nancy]")//
           .withResult("manyToOneNotExistsNe", "SELECT `employees`.* FROM `employees` WHERE NOT EXISTS (SELECT 1 FROM `employees` `~~relTbl_employees` WHERE `employees`.`employeeId` = `~~relTbl_employees`.`reportsTo` AND `~~relTbl_employees`.`firstName` = ?) ORDER BY `employees`.`employeeId` ASC LIMIT 100 args=[Nancy]")//
           .withResult("manyTManyNotExistsNe",
                 "SELECT `employees`.* FROM `employees` WHERE NOT EXISTS (SELECT 1 FROM `orderDetails` `~~relTbl_orderDetails`, `employeeOrderDetails` `~~lnkTbl_employeeOrderDetails` WHERE `employees`.`employeeId` = `~~lnkTbl_employeeOrderDetails`.`employeeId` AND `~~lnkTbl_employeeOrderDetails`.`orderId` = `~~relTbl_orderDetails`.`orderId` AND `~~lnkTbl_employeeOrderDetails`.`productId` = `~~relTbl_orderDetails`.`productId` AND `~~relTbl_orderDetails`.`quantity` = ?) ORDER BY `employees`.`employeeId` ASC LIMIT 100 args=[12]")//
           .withResult("eqNonexistantColumn", "SELECT `orders`.* FROM `orders` WHERE `orders`.`orderId` >= ? ORDER BY `orders`.`orderId` ASC LIMIT 100 args=[1000]");
      ;
   }

}
