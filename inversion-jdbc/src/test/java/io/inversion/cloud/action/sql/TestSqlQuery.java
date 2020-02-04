/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.cloud.action.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.Test;

import io.inversion.cloud.jdbc.db.SqlQuery;
import junit.framework.TestCase;

public class TestSqlQuery extends TestCase
{
   public static void main(String[] args) throws Exception
   {
      new TestSqlQuery().test1();
   }

   @Test
   public void test1() throws Exception
   {
      List<RqlTest> tests = new ArrayList();

      String rql = null;
      String select = null;
      String dynamicSql = null;
      String preprdStmt = null;
      String[] params = null;

      //these tests are run in reverse order so that you can test your most recently added tests
      //first (by adding them to the bottom of the list) and keep the comment lines referencing
      //test numbers correct to easily find the test that failed.  If you add the tests
      //to the top of the list, the comment lines would not be helpful.

      //Test 1
      tests.add(new RqlTest("group(startYear)&includes=startYear,motiveConfirmed,another&startYear=ne=null&as(count(motiveConfirmed),another)", //
                            "SELECT * FROM Person p JOIN Entry e ON p.id = e.personId  JOIN Country c ON e.country = c.country_name", //
                            "SELECT `startYear`, `motiveConfirmed`, COUNT(`motiveConfirmed`) AS 'another' FROM Person p JOIN Entry e ON p.id = e.personId  JOIN Country c ON e.country = c.country_name WHERE (NOT (`startYear` IS NULL )) GROUP BY `startYear` LIMIT 100", //
                            null));

      tests.add(new RqlTest("ne(startYear, null)", //
                            "SELECT * FROM Person", //
                            "SELECT * FROM Person WHERE (NOT (`startYear` IS NULL )) LIMIT 100", //
                            "SELECT * FROM Person WHERE (NOT (`startYear` IS NULL )) LIMIT 100"));

      tests.add(new RqlTest("aggregate(count,country,result)&includes=country,result&&in(status,killed)&limit=1", //
                            "SELECT * FROM Person p JOIN Entry e ON p.id = e.personId JOIN Country c ON e.country = c.country_name", //
                            "SELECT `country`, COUNT(`country`) AS 'result' FROM Person p JOIN Entry e ON p.id = e.personId JOIN Country c ON e.country = c.country_name WHERE `status` IN('killed') GROUP BY `country` LIMIT 1", //
                            "SELECT `country`, COUNT(`country`) AS 'result' FROM Person p JOIN Entry e ON p.id = e.personId JOIN Country c ON e.country = c.country_name WHERE `status` IN(?) GROUP BY `country` LIMIT 1", //
                            "status", "killed"));//));            

      tests.add(new RqlTest("aggregate(count,country,result)&includes=country,result&in(status,killed)", //
                            "SELECT * FROM Person p JOIN Entry e ON p.id = e.personId JOIN Country c ON e.country = c.country_name", //
                            "SELECT `country`, COUNT(`country`) AS 'result' FROM Person p JOIN Entry e ON p.id = e.personId JOIN Country c ON e.country = c.country_name WHERE `status` IN('killed') GROUP BY `country` LIMIT 100", //
                            "SELECT `country`, COUNT(`country`) AS 'result' FROM Person p JOIN Entry e ON p.id = e.personId JOIN Country c ON e.country = c.country_name WHERE `status` IN(?) GROUP BY `country` LIMIT 100", //
                            "status", "killed"));//));

      //Test 5
      tests.add(new RqlTest("aggregate(count,country,result)&includes=country,result&in(status,killed,other)", //
                            "select * from entries", //
                            "select `country`, COUNT(`country`) AS 'result' from entries WHERE `status` IN('killed', 'other') GROUP BY `country` LIMIT 100", //
                            "select `country`, COUNT(`country`) AS 'result' from entries WHERE `status` IN(?, ?) GROUP BY `country` LIMIT 100", //
                            "status", "killed", "status", "other"));

      tests.add(new RqlTest("aggregate(count,country,result)&includes=result&in(status,killed)", //
                            "select * from entries", //
                            "select COUNT(`country`) AS 'result' from entries WHERE `status` IN('killed') GROUP BY `country` LIMIT 100", //
                            null));

      tests.add(new RqlTest("sort=first,-second,+third", //
                            "select * from table1", //
                            "select * from table1 ORDER BY `first` ASC, `second` DESC, `third` ASC LIMIT 100", //
                            null));

      tests.add(new RqlTest("pagenum=10&pagesize=20", //
                            "select * from table1", //
                            "select SQL_CALC_FOUND_ROWS * from table1 LIMIT 180, 20", //
                            null));

      tests.add(new RqlTest("page(10,20)", //
                            "select * from table1", //
                            "select SQL_CALC_FOUND_ROWS * from table1 LIMIT 180, 20", //
                            null));

      //Test 10
      tests.add(new RqlTest("offset(20, 100)", //
                            "select * from table1", //
                            "select * from table1 LIMIT 20, 100", //
                            null));

      tests.add(new RqlTest("limit(20, 100)", //
                            "select * from table1", //
                            "select * from table1 LIMIT 100, 20", //
                            null));

      tests.add(new RqlTest("offset=10&limit(20)", //
                            "select * from table1", //
                            "select * from table1 LIMIT 10, 20", //
                            null));

      tests.add(new RqlTest("firstName=Wells&eq(lastName,Burke)&includes=firstName,lastName&group(firstName,lastName)&sum(age)&max(height, tallest)&as(min(height), shortest)", //
                            "select * from table1", //
                            "select `firstName`, `lastName`, SUM(`age`), MAX(`height`) AS 'tallest', MIN(`height`) AS 'shortest' from table1 WHERE `firstName` = 'Wells' AND `lastName` = 'Burke' GROUP BY `firstName`, `lastName` LIMIT 100", //
                            null));

      tests.add(new RqlTest("w(city,andl)", //
                            "select * from table1", //
                            null, //
                            "select * from table1 WHERE `city` LIKE ? LIMIT 100", //
                            "city", "%andl%"));

      //Test 15
      tests.add(new RqlTest("sw(city,andl)", //
                            "select * from table1", //
                            null, //
                            "select * from table1 WHERE `city` LIKE ? LIMIT 100", //
                            "city", "andl%"));

      tests.add(new RqlTest("ew(city,andl)", //
                            "select * from table1", //
                            null, //
                            "select * from table1 WHERE `city` LIKE ? LIMIT 100", //
                            "city", "%andl"));

      tests.add(new RqlTest("in(firstName,wells,joe,karl)", //
                            "select * from table1", //
                            "select * from table1 WHERE `firstName` IN('wells', 'joe', 'karl') LIMIT 100", //
                            null));

      tests.add(new RqlTest("out(firstName,wells,joe,karl)", //
                            "select * from table1", //
                            "select * from table1 WHERE `firstName` NOT IN('wells', 'joe', 'karl') LIMIT 100", //
                            null));

      tests.add(new RqlTest("firstName=in=wells,joe,karl", //
                            "select * from table1", //
                            "select * from table1 WHERE `firstName` IN('wells', 'joe', 'karl') LIMIT 100", //
                            null));

      //Test 20
      tests.add(new RqlTest("firstName=Wells&eq(lastName,Burke)&includes=firstName,lastName", //
                            "select * from table1", //
                            "select `firstName`, `lastName` from table1 WHERE `firstName` = 'Wells' AND `lastName` = 'Burke' LIMIT 100", //
                            null));

      tests.add(new RqlTest("firstName=Wells&eq(lastName,Burke)&includes(firstName,lastName)", //
                            "select * from table1", //
                            "select `firstName`, `lastName` from table1 WHERE `firstName` = 'Wells' AND `lastName` = 'Burke' LIMIT 100", //
                            null));

      tests.add(new RqlTest("distinct&firstName=Wells&eq(lastName,Burke)&includes(firstName,lastName)", //
                            "select * from table1", //
                            "select DISTINCT `firstName`, `lastName` from table1 WHERE `firstName` = 'Wells' AND `lastName` = 'Burke' LIMIT 100", //
                            null));

      tests.add(new RqlTest("firstName=Wells&distinct&eq(lastName,Burke)", //
                            "select * from table1", //
                            "select DISTINCT * from table1 WHERE `firstName` = 'Wells' AND `lastName` = 'Burke' LIMIT 100", //
                            null));

      tests.add(new RqlTest("includes=lastName&firstName=wells&lastName=Burke&state=ne=CA&age=ge=38", //
                            "select * from table1", //
                            "select `lastName` from table1 WHERE `firstName` = 'wells' AND `lastName` = 'Burke' AND (NOT (`state` = 'CA')) AND `age` >= 38 LIMIT 100", //
                            "select `lastName` from table1 WHERE `firstName` = ? AND `lastName` = ? AND (NOT (`state` = ?)) AND `age` >= ? LIMIT 100", //
                            "firstName", "wells", "lastName", "Burke", "state", "CA", "age", "38"));

      //Test 25
      tests.add(new RqlTest("sum(if(eq(`col1`,'value'),1,'something blah'))", //
                            "SELECT * from table1", //
                            "SELECT *, SUM(IF(`col1` = 'value', 1, 'something blah')) from table1 LIMIT 100", //
                            "SELECT *, SUM(IF(`col1` = ?, 1, ?)) from table1 LIMIT 100", //
                            "col1", "value", null, "something blah"));

      tests.add(new RqlTest("sum(if(eq(\"col1\",if(ne(`col2`,'val1'),`col3`,'val2')),1,'something blah'))", //
                            "SELECT * from table1", //
                            "SELECT *, SUM(IF(`col1` = IF((NOT (`col2` = 'val1')), `col3`, 'val2'), 1, 'something blah')) from table1 LIMIT 100", //
                            "SELECT *, SUM(IF(`col1` = IF((NOT (`col2` = ?)), `col3`, ?), 1, ?)) from table1 LIMIT 100", //
                            "col2", "val1", "col3", "val2", null, "something blah"));

      tests.add(new RqlTest("sum(if(eq(`col1`,if(ne(`col2`,'val1'),`col3`,'val2')),1,'something blah'))", //
                            "SELECT * from table1", //
                            "SELECT *, SUM(IF(`col1` = IF((NOT (`col2` = 'val1')), `col3`, 'val2'), 1, 'something blah')) from table1 LIMIT 100", //
                            "SELECT *, SUM(IF(`col1` = IF((NOT (`col2` = ?)), `col3`, ?), 1, ?)) from table1 LIMIT 100", //
                            "col2", "val1", "col3", "val2", null, "something blah"));

      tests.add(new RqlTest("includes=firstName,lastName", //
                            "select * from table1", //
                            "select `firstName`, `lastName` from table1 LIMIT 100", //
                            "select `firstName`, `lastName` from table1 LIMIT 100"));

      tests.add(new RqlTest("firstName=wells&lastName=Burke&state=ne=CA&age=ge=38&includes=lastName", //
                            "select * from table1", //
                            "select `lastName` from table1 WHERE `firstName` = 'wells' AND `lastName` = 'Burke' AND (NOT (`state` = 'CA')) AND `age` >= 38 LIMIT 100", //
                            "select `lastName` from table1 WHERE `firstName` = ? AND `lastName` = ? AND (NOT (`state` = ?)) AND `age` >= ? LIMIT 100", //
                            "firstName", "wells", "lastName", "Burke", "state", "CA", "age", "38"));

      //TEST 30
      tests.add(new RqlTest("group(startYear)&includes=startYear&startYear=ne=null&as(count(motiveConfirmed), 'Motive Spaces Confirmed')", //
                            "SELECT * FROM Person p JOIN Entry e ON p.id = e.personId  JOIN Country c ON e.country = c.country_name", //
                            "SELECT `startYear`, COUNT(`motiveConfirmed`) AS 'Motive Spaces Confirmed' FROM Person p JOIN Entry e ON p.id = e.personId  JOIN Country c ON e.country = c.country_name WHERE (NOT (`startYear` IS NULL ))  GROUP BY `startYear` LIMIT 100", //
                            null));

      tests.add(new RqlTest("group(startYear)&includes=startYear,'Motive Spaces Confirmed'&startYear=ne=null&as(sum(if(eq(motiveConfirmed,true),1,0)), 'Motive Spaces Confirmed')", //
                            "SELECT * FROM Person p JOIN Entry e ON p.id = e.personId  JOIN Country c ON e.country = c.country_name", //
                            "SELECT `startYear`, SUM(IF(`motiveConfirmed` = 1, 1, 0)) AS 'Motive Spaces Confirmed' FROM Person p JOIN Entry e ON p.id = e.personId  JOIN Country c ON e.country = c.country_name WHERE (NOT (`startYear` IS NULL )) GROUP BY `startYear` LIMIT 100", //
                            null));

      tests.add(new RqlTest("motiveConfirmed=someValue", //
                            "SELECT * FROM Person", //
                            "SELECT * FROM Person WHERE `motiveConfirmed` = 'someValue' LIMIT 100", //
                            null));

      tests.add(new RqlTest("includes='A Col With Caps and Spaces'&as(max(someCol), 'A Col With Caps and Spaces') LIMIT 100", //
                            "SELECT * FROM Person", //
                            "SELECT MAX(`someCol`) AS 'A Col With Caps and Spaces' FROM Person LIMIT 100", //
                            null));

      tests.add(new RqlTest("includes=y,a,b,z&as(max(col1),b)&as(max(col2),a)", //
                            "SELECT * FROM Person", //
                            "SELECT `y`, MAX(`col2`) AS 'a', MAX(`col1`) AS 'b', `z` FROM Person LIMIT 100", //
                            null));

      //TEST 35
      tests.add(new RqlTest("includes=a,b&as(max(col1),b)&as(max(col2),a)", //
                            "SELECT * FROM Person", //
                            "SELECT MAX(`col2`) AS 'a', MAX(`col1`) AS 'b' FROM Person LIMIT 100", //
                            null));

      tests.add(new RqlTest("as(sum(if(eq(type,'Media Worker'),1,0)),'Media Worker')&group(startYear)&includes=startYear,'Motive Confirmed','Media Worker','Motive Unconfirmed'&startYear=ne=null&as(sum(if(eq(motiveConfirmed,Confirmed),1,0)),'Motive Confirmed')&as(sum(if(eq(motiveConfirmed,Unconfirmed),1,0)),'Motive Unconfirmed')", //
                            "SELECT * FROM Entry", //
                            "SELECT `startYear`, SUM(IF(`motiveConfirmed` = 'Confirmed', 1, 0)) AS 'Motive Confirmed', SUM(IF(`type` = 'Media Worker', 1, 0)) AS 'Media Worker', SUM(IF(`motiveConfirmed` = 'Unconfirmed', 1, 0)) AS 'Motive Unconfirmed'  FROM Entry WHERE (NOT (`startYear` IS NULL )) GROUP BY `startYear` LIMIT 100", //
                            "SELECT `startYear`, SUM(IF(`motiveConfirmed` = ?, 1, 0)) AS 'Motive Confirmed', SUM(IF(`type` = ?, 1, 0)) AS 'Media Worker', SUM(IF(`motiveConfirmed` = ?, 1, 0)) AS 'Motive Unconfirmed'  FROM Entry WHERE (NOT (`startYear` IS NULL )) GROUP BY `startYear` LIMIT 100", //
                            "motiveConfirmed", "Confirmed", "type", "Media Worker", "motiveConfirmed", "Unconfirmed"));
      //      
      tests.add(new RqlTest("as(sum(if(eq(type,'Media Worker'),1,0)),'Media Worker')&group(startYear)&includes=startYear,'Motive Confirmed','Media Worker','Motive Unconfirmed'&startYear=ne=null&as(sum(if(eq(motiveConfirmed,Confirmed),1,0)),'Motive Confirmed')&as(sum(if(eq(motiveConfirmed,Unconfirmed),1,0)),'Motive Unconfirmed')&or(eq(`type`,'Media Worker'),ne(`motiveConfirmed`,NULL))", //
                            "SELECT * FROM Entry", //
                            "SELECT `startYear`, SUM(IF(`motiveConfirmed` = 'Confirmed', 1, 0)) AS 'Motive Confirmed', SUM(IF(`type` = 'Media Worker', 1, 0)) AS 'Media Worker', SUM(IF(`motiveConfirmed` = 'Unconfirmed', 1, 0)) AS 'Motive Unconfirmed'  FROM Entry WHERE (NOT (`startYear` IS NULL )) AND (`type` = 'Media Worker' OR (NOT (`motiveConfirmed` IS NULL ))) GROUP BY `startYear` LIMIT 100", //
                            "SELECT `startYear`, SUM(IF(`motiveConfirmed` = ?, 1, 0)) AS 'Motive Confirmed', SUM(IF(`type` = ?, 1, 0)) AS 'Media Worker', SUM(IF(`motiveConfirmed` = ?, 1, 0)) AS 'Motive Unconfirmed'  FROM Entry WHERE (NOT (`startYear` IS NULL )) AND (`type` = ? OR (NOT (`motiveConfirmed` IS NULL ))) GROUP BY `startYear` LIMIT 100", //
                            "motiveConfirmed", "Confirmed", "type", "Media Worker", "motiveConfirmed", "Unconfirmed", "type", "Media Worker"));

      tests.add(new RqlTest("countascol(impunity, 'Full Justice', 'Partial Impunity', 'Complete Impunity')", //
                            "SELECT * FROM Person", //
                            "SELECT *, SUM(IF(`impunity` = 'Full Justice', 1, 0)) AS 'Full Justice', SUM(IF(`impunity` = 'Partial Impunity', 1, 0)) AS 'Partial Impunity', SUM(IF(`impunity` = 'Complete Impunity', 1, 0)) AS 'Complete Impunity' FROM Person WHERE `impunity` IN('Full Justice', 'Partial Impunity', 'Complete Impunity') LIMIT 100", //
                            "SELECT *, SUM(IF(`impunity` = ?, 1, 0)) AS 'Full Justice', SUM(IF(`impunity` = ?, 1, 0)) AS 'Partial Impunity', SUM(IF(`impunity` = ?, 1, 0)) AS 'Complete Impunity' FROM Person WHERE `impunity` IN(?, ?, ?) LIMIT 100", //
                            "impunity", "Full Justice", "impunity", "Partial Impunity", "impunity", "Complete Impunity", "impunity", "Full Justice", "impunity", "Partial Impunity", "impunity", "Complete Impunity"));

      tests.add(new RqlTest("in(status, killed)&group(victim)&count(victim, number)", //
                            "SELECT * FROM (SELECT *, 'Threatened' as 'Victim' FROM Entry WHERE threatened = true UNION SELECT *, 'Tortured' as 'Victim' FROM Entry WHERE tortured = true  UNION SELECT *, 'Taken Captive' as 'Victim' FROM Entry WHERE captive = true )as t", //
                            "SELECT *, COUNT(`victim`) AS 'number' FROM (SELECT *, 'Threatened' as 'Victim' FROM Entry WHERE threatened = true UNION SELECT *, 'Tortured' as 'Victim' FROM Entry WHERE tortured = true  UNION SELECT *, 'Taken Captive' as 'Victim' FROM Entry WHERE captive = true )as t WHERE `status` IN('killed') GROUP BY `victim` LIMIT 100", //
                            null));

      //40
      tests.add(new RqlTest("group(victim)&count(victim, number)", //
                            "SELECT * FROM (SELECT *, 'Threatened' as 'Victim' FROM Entry WHERE threatened = true UNION SELECT *, 'Tortured' as 'Victim' FROM Entry WHERE tortured = true  UNION SELECT *, 'Taken Captive' as 'Victim' FROM Entry WHERE captive = true )as t", //
                            "SELECT *, COUNT(`victim`) AS 'number' FROM (SELECT *, 'Threatened' as 'Victim' FROM Entry WHERE threatened = true UNION SELECT *, 'Tortured' as 'Victim' FROM Entry WHERE tortured = true  UNION SELECT *, 'Taken Captive' as 'Victim' FROM Entry WHERE captive = true )as t GROUP BY `victim` LIMIT 100", //
                            "SELECT *, COUNT(`victim`) AS 'number' FROM (SELECT *, 'Threatened' as 'Victim' FROM Entry WHERE threatened = true UNION SELECT *, 'Tortured' as 'Victim' FROM Entry WHERE tortured = true  UNION SELECT *, 'Taken Captive' as 'Victim' FROM Entry WHERE captive = true )as t GROUP BY `victim` LIMIT 100"));

      tests.add(new RqlTest("as(sum(if(and(eq(motiveConfirmed,Confirmed),eq(type,Journalist)),1,0)),'Motive Confirmed')", //
                            "SELECT * FROM Test", //
                            "SELECT *, SUM(IF((`motiveConfirmed` = 'Confirmed' AND `type` = 'Journalist'), 1, 0)) AS 'Motive Confirmed' FROM Test LIMIT 100", //
                            null));

      rql = "includes=Year,'Motive Confirmed','Media Worker','Motive Unconfirmed'&as(year, Year)&as(sum(if(and(eq(motiveConfirmed,Confirmed),eq(type,Journalist)),1,0)),'Motive Confirmed')&as(sum(if(eq(type,'Media Worker'),1,0)),'Media Worker')&as(sum(if(eq(motiveConfirmed,Unconfirmed),1,0)),'Motive Unconfirmed')&or(eq(type, 'Media Worker'), ne(motiveConfirmed, null))&status=Killed&order=-Year";

      select = "";
      select += " SELECT y.year, e.*, p.*  ";
      select += " FROM Entry e  ";
      select += " JOIN Year y ON y.year < YEAR(CURDATE()) AND (((e.startYear <= year) AND (e.endYear is NULL OR e.endYear >= year) AND status != 'Killed') OR (status = 'Killed' AND e.startYear = year)) ";
      select += " JOIN Person p ON e.personId = p.id ";
      select += " LEFT JOIN Country c ON e.country = c.country_name";

      dynamicSql = "";
      dynamicSql += " SELECT `year` AS 'Year', SUM(IF((`motiveConfirmed` = 'Confirmed' AND `type` = 'Journalist'), 1, 0)) AS 'Motive Confirmed', SUM(IF(`type` = 'Media Worker', 1, 0)) AS 'Media Worker', SUM(IF(`motiveConfirmed` = 'Unconfirmed', 1, 0)) AS 'Motive Unconfirmed'";
      dynamicSql += " FROM Entry e  ";
      dynamicSql += " JOIN Year y ON y.year < YEAR(CURDATE()) AND (((e.startYear <= year) AND (e.endYear is NULL OR e.endYear >= year) AND status != 'Killed') OR (status = 'Killed' AND e.startYear = year)) ";
      dynamicSql += " JOIN Person p ON e.personId = p.id ";
      dynamicSql += " LEFT JOIN Country c ON e.country = c.country_name";
      dynamicSql += " WHERE (`type` = 'Media Worker' OR (NOT (`motiveConfirmed` IS NULL ))) ";
      dynamicSql += " AND `status` = 'Killed' ";
      dynamicSql += " ORDER BY `Year` DESC LIMIT 100";

      tests.add(new RqlTest(rql, //
                            select, //
                            dynamicSql, //
                            null));

      rql = "includes=Year,'Motive Confirmed','Media Worker','Motive Unconfirmed'&as(year, Year)&as(sum(if(and(eq(motiveConfirmed,Confirmed),eq(type,Journalist)),1,0)),'Motive Confirmed')&as(sum(if(eq(type,'Media Worker'),1,0)),'Media Worker')&as(sum(if(eq(motiveConfirmed,Unconfirmed),1,0)),'Motive Unconfirmed')&or(eq(type, 'Media Worker'), ne(motiveConfirmed, null))&status=Killed&order=-Year";

      select = "";
      select += " SELECT * ";
      select += " FROM Entry e  ";
      select += " JOIN Year y ON y.year < YEAR(CURDATE()) AND (((e.startYear <= year) AND (e.endYear is NULL OR e.endYear >= year) AND status != 'Killed') OR (status = 'Killed' AND e.startYear = year)) ";
      select += " JOIN Person p ON e.personId = p.id ";
      select += " LEFT JOIN Country c ON e.country = c.country_name";

      dynamicSql = "";
      dynamicSql += " SELECT `year` AS 'Year', SUM(IF((`motiveConfirmed` = 'Confirmed' AND `type` = 'Journalist'), 1, 0)) AS 'Motive Confirmed', SUM(IF(`type` = 'Media Worker', 1, 0)) AS 'Media Worker', SUM(IF(`motiveConfirmed` = 'Unconfirmed', 1, 0)) AS 'Motive Unconfirmed'";
      dynamicSql += " FROM Entry e  ";
      dynamicSql += " JOIN Year y ON y.year < YEAR(CURDATE()) AND (((e.startYear <= year) AND (e.endYear is NULL OR e.endYear >= year) AND status != 'Killed') OR (status = 'Killed' AND e.startYear = year)) ";
      dynamicSql += " JOIN Person p ON e.personId = p.id ";
      dynamicSql += " LEFT JOIN Country c ON e.country = c.country_name";
      dynamicSql += " WHERE (`type` = 'Media Worker' OR (NOT (`motiveConfirmed` IS NULL ))) ";
      dynamicSql += " AND `status` = 'Killed' ";
      dynamicSql += " ORDER BY `Year` DESC LIMIT 100";

      tests.add(new RqlTest(rql, //
                            select, //
                            dynamicSql, //
                            null));

      tests.add(new RqlTest("firstName=*w*", //
                            "select * from table1", //
                            "select * from table1 WHERE `firstName` LIKE '%w%' LIMIT 100", //
                            "select * from table1 WHERE `firstName` LIKE ?  LIMIT 100", //
                            "firstName", "%w%"));

      //45

      //q= is depricatd???
      //      tests.add(new RqlTest("q=firstName=*w*", //
      //                            "select * from table1", //
      //                            "select SQL_CALC_FOUND_ROWS * from table1 WHERE `firstName` LIKE '%w%' LIMIT 100", //
      //                            "select SQL_CALC_FOUND_ROWS * from table1 WHERE `firstName` LIKE ? LIMIT 100",//
      //                            "firstName", "%w%"));

      tests.add(new RqlTest("pagenum=1", //
                            "select * from table1", //
                            "select SQL_CALC_FOUND_ROWS * from table1 LIMIT 100", //
                            null));

      tests.add(new RqlTest("as(if(captive, 'Taken Captive', 'Not Taken Captive'), 'Captive')", //
                            "select * from table1", //
                            "select SQL_CALC_FOUND_ROWS *, IF(`captive`, 'Taken Captive', 'Not Taken Captive') AS 'Captive' from table1 LIMIT 100", //
                            null));

      tests.add(new RqlTest("name='John Doe'", //
                            "select * from table1", //
                            "select * from table1 WHERE `name` = 'John Doe' LIMIT 100", //
                            "select * from table1 WHERE `name` = ? LIMIT 100", //
                            "name", "John Doe"));

      tests.add(new RqlTest("name='John Doe'", //
                            "select * from table1", //
                            "select * from table1 WHERE `name` = 'John Doe' LIMIT 100", //
                            "select * from table1 WHERE `name` = ? LIMIT 100", //
                            "name", "John Doe"));

      //50
      tests.add(new RqlTest("name='John\\' Doe'", //
                            "select * from table1", //
                            "select * from table1 WHERE `name` = 'John' Doe' LIMIT 100", //
                            "select * from table1 WHERE `name` = ? LIMIT 100", //
                            "name", "John' Doe"));

      tests.add(new RqlTest("name='John\" Doe'", //
                            "select * from table1", //
                            "select * from table1 WHERE `name` = 'John\" Doe' LIMIT 100", //
                            "select * from table1 WHERE `name` = ? LIMIT 100", //
                            "name", "John\" Doe"));

      tests.add(new RqlTest("nn(startYear)", //
                            "SELECT * FROM Person", //
                            "SELECT * FROM Person WHERE `startYear` IS NOT NULL LIMIT 100", //
                            "SELECT * FROM Person WHERE `startYear` IS NOT NULL LIMIT 100"));

      tests.add(new RqlTest("n(startYear)", //
                            "SELECT * FROM Person", //
                            "SELECT * FROM Person WHERE `startYear` IS NULL LIMIT 100", //
                            "SELECT * FROM Person WHERE `startYear` IS NULL LIMIT 100"));

      tests.add(new RqlTest("like(name,asdf*)", //
                            "SELECT * FROM Person", //
                            "SELECT * FROM Person WHERE `name` LIKE 'asdf%' LIMIT 100", //
                            "SELECT * FROM Person WHERE `name` LIKE ? LIMIT 100", //
                            "name", "asdf%"));

      tests.add(new RqlTest("as(firstName, name)", //
                            "select * from table1", //
                            "select *, `firstName` AS 'name' from table1 LIMIT 100", // 
                            "select *, `firstName` AS 'name' from table1 LIMIT 100"));//the dynamicsql and preparedsql should match without fields

      tests.add(new RqlTest("wo(city,andl)", //
                            "select * from table1", //
                            null, //
                            "select * from table1 WHERE (NOT (`city` LIKE ?)) LIMIT 100", //
                            "city", "%andl%"));

      tests.add(new RqlTest("lt(freight,2)", //
                            "select * from table1", //
                            "select * from table1 WHERE `freight` < 2 LIMIT 100", //
                            "select * from table1 WHERE `freight` < ? LIMIT 100", // 
                            "freight", "2"));

      tests.add(new RqlTest("le(freight,2)", //
                            "select * from table1", //
                            "select * from table1 WHERE `freight` <= 2 LIMIT 100", //
                            "select * from table1 WHERE `freight` <= ? LIMIT 100", //
                            "freight", "2"));

      tests.add(new RqlTest("emp(startYear)", //
                            "SELECT * FROM Person", //
                            "SELECT * FROM Person WHERE (`startYear` IS NULL OR `startYear` = '') LIMIT 100", //
                            "SELECT * FROM Person WHERE (`startYear` IS NULL OR `startYear` = '') LIMIT 100"));

      tests.add(new RqlTest("nemp(startYear)", //
                            "SELECT * FROM Person", //
                            "SELECT * FROM Person WHERE (`startYear` IS NOT NULL AND `startYear` != '') LIMIT 100", //
                            "SELECT * FROM Person WHERE (`startYear` IS NOT NULL AND `startYear` != '') LIMIT 100"));

      tests.add(new RqlTest("w(code,'070847030973','070847030911')", //
                            "SELECT * FROM Item", //
                            "SELECT * FROM Item WHERE (`code` LIKE '%070847030973%' OR `code` LIKE '%070847030911%') LIMIT 100", //
                            "SELECT * FROM Item WHERE (`code` LIKE ? OR `code` LIKE ?) LIMIT 100", //
                            "code", "%070847030973%", "code", "%070847030911%"));

      tests.add(new RqlTest("wo(code,'070847030973','070847030911')", //
                            "SELECT * FROM Item", //
                            "SELECT * FROM Item WHERE (NOT (`code` LIKE '%070847030973%' OR `code` LIKE '%070847030911%')) LIMIT 100", //
                            "SELECT * FROM Item WHERE (NOT (`code` LIKE ? OR `code` LIKE ?)) LIMIT 100", //
                            "code", "%070847030973%", "code", "%070847030911%"));

      tests.add(new RqlTest("ne(code,'070847030973','070847030911')", //
                            "SELECT * FROM Item", //
                            "SELECT * FROM Item WHERE (NOT (`code` = '070847030973' OR `code` = '070847030911')) LIMIT 100", //
                            "SELECT * FROM Item WHERE (NOT (`code` = ? OR `code` = ?)) LIMIT 100", //
                            "code", "070847030973", "code", "070847030911"));

      boolean passed = true;
      int running = 0;
      RqlTest test = null;
      //for (int j = tests.size() - 1; j >= 0; j--)
      for (int j = 0; j < tests.size(); j++)
      {
         running = j + 1;
         test = tests.get(j);
         try
         {
            SqlQuery query = new SqlQuery(null, null);//test.rql, test.select);
            query.withTerm(test.rql);
            query.withColumnQuote('`');
            query.withSelectSql(test.select);

            String output = query.getDynamicStmt();

            if (test.dynamicSql != null && !compare(test.dynamicSql, output))
            {
               System.out.println("FAILED RQL    : " + test.rql);
               System.out.println("FAILED SELECT : " + test.select);

               passed = false;
               break;
            }

            if (test.preparedSql != null)
            {
               output = query.getPreparedStmt();

               if (!compare(test.preparedSql, output))
               {
                  passed = false;
                  break;
               }

               for (int i = 0; i < query.getNumValues(); i++)
               {
                  try
                  {
                     String col = (String)query.getColValue(i).getKey();
                     String val = (String)query.getColValue(i).getValue();

                     if (!(col + "").equals(test.fields[i * 2] + ""))
                        passed = false;

                     if (!(val + "").equals(test.fields[(i * 2) + 1] + ""))
                        passed = false;
                  }
                  catch (Exception ex)
                  {
                     passed = false;
                     break;
                  }
               }
               if (!passed)
               {
                  System.out.println(output.replace("\r", "").replace("\n", ""));
                  System.out.println("replaced parameters do not match up: " + query.getValues() + " - " + Arrays.asList(test.fields));
                  break;
               }
            }
         }
         catch (Exception ex)
         {
            ex.printStackTrace();
            passed = false;
            break;
         }

         System.out.println("PASSED: " + (j + 1));

      }

      if (passed)
      {
         System.out.println("SUCCESS!!!! ALL TESTS PASSED");
      }
      else
      {
         System.out.println("FAILED RQL:" + test.rql);
         System.out.println("FAILED: " + running);
         fail();
      }
   }

   public static boolean compare(String expected, String received)
   {
      expected = expected.replaceAll("\\s+", " ").trim();
      received = received.replaceAll("\\s+", " ").trim();

      if (!expected.equals(received))
      {
         //TODO: TAKE THESE "CORRECTIONS" OUT
         received = received.replaceAll("SQL_CALC_FOUND_ROWS ", "");
         //str2 = str2.replaceAll(" LIMIT 0, 100", "");

         expected = expected.replaceAll("SQL_CALC_FOUND_ROWS ", "");
         //str1 = str1.replaceAll(" LIMIT 0, 100", "");

         if (!expected.equals(received))
         {
            //System.out.println("\r\n");
            //System.out.println("\r\n");
            System.out.println("EXPECTED: " + expected);
            System.out.println("ACTUAL  : " + received);
            System.out.print("          " );

            for (int i = 0; i < expected.length() && i < received.length(); i++)
            {
               if (expected.charAt(i) == received.charAt(i))
               {
                  System.out.print(" ");
               }
               else
               {
                  System.out.println("X");
                  break;
               }
            }
            System.out.println(" ");

            String err = "failed test: " + expected + " != " + received;
            return false;
         }
      }
      return true;
   }

   static LinkedHashMap split(String queryString)
   {
      LinkedHashMap map = new LinkedHashMap();

      String[] terms = queryString.split("&");
      for (String term : terms)
      {
         int eqIdx = term.indexOf('=');
         if (eqIdx < 0)
         {
            map.put(term, null);
         }
         else
         {
            String value = term.substring(eqIdx + 1, term.length());
            term = term.substring(0, eqIdx);
            map.put(term, value);
         }
      }
      return map;
   }

   static class RqlTest
   {
      String   rql         = null;
      String   select      = null;
      String   dynamicSql  = null;
      String   preparedSql = null;
      String[] fields      = null;

      public RqlTest(String rql, String select, String dynamicSql, String preparedSql, String... fields)
      {
         super();
         this.rql = rql;
         this.select = select;
         this.dynamicSql = dynamicSql;
         this.preparedSql = preparedSql;
         this.fields = fields;
      }
   }

}
