/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
 * http://rocketpartners.io
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.rcktapp.api.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import io.rcktapp.api.service.RQL;
import io.rcktapp.api.service.RQL.Replacer;

public class TestRql
{
   public static void main(String[] args) throws Exception
   {
      List<RqlTest> tests = new ArrayList();

      String rql = null;
      String select = null;
      String dynamicSql = null;
      String preprdStmt = null;
      String[] params = null;

      //these tests are run in reverse order so that you can test your most recently added tests
      //frist (by adding them to the bottom of the list) and keep the comment lines referencing
      //test numbers correct to easily find the test that failed.  If you add the tests
      //to the top of the list, the comment lines would not be helpful.

      //Test 1
      tests.add(new RqlTest("group(startYear)&includes=startYear,motiveConfirmed,another&startYear=ne=null&as(count(motiveConfirmed),another)", //
                            "SELECT * FROM Person p JOIN Entry e ON p.id = e.personId  JOIN Country c ON e.country = c.country_name", //
                            "SELECT `startYear`, `motiveConfirmed`, COUNT(`motiveConfirmed`) AS 'another' FROM Person p JOIN Entry e ON p.id = e.personId  JOIN Country c ON e.country = c.country_name WHERE `startYear` IS NOT NULL GROUP BY `startYear`", //
                            null));

      tests.add(new RqlTest("ne(startYear, null)", //
                            "SELECT * FROM Person", //
                            "SELECT * FROM Person WHERE `startYear` IS NOT NULL", //
                            "SELECT * FROM Person WHERE `startYear` IS NOT NULL"));

      tests.add(new RqlTest("aggregate(count,country,result)&includes=country,result&&in(status,killed)&limit=1", //
                            "SELECT * FROM Person p JOIN Entry e ON p.id = e.personId JOIN Country c ON e.country = c.country_name", //
                            "SELECT `country`, COUNT(`country`) AS 'result' FROM Person p JOIN Entry e ON p.id = e.personId JOIN Country c ON e.country = c.country_name WHERE `status` IN('killed') GROUP BY `country` LIMIT 1", //
                            "SELECT `country`, COUNT(`country`) AS 'result' FROM Person p JOIN Entry e ON p.id = e.personId JOIN Country c ON e.country = c.country_name WHERE `status` IN(?) GROUP BY `country` LIMIT 1", //
                            "`status`", "'killed'"));//));            

      tests.add(new RqlTest("aggregate(count,country,result)&includes=country,result&in(status,killed)", //
                            "SELECT * FROM Person p JOIN Entry e ON p.id = e.personId JOIN Country c ON e.country = c.country_name", //
                            "SELECT `country`, COUNT(`country`) AS 'result' FROM Person p JOIN Entry e ON p.id = e.personId JOIN Country c ON e.country = c.country_name WHERE `status` IN('killed') GROUP BY `country`", //
                            "SELECT `country`, COUNT(`country`) AS 'result' FROM Person p JOIN Entry e ON p.id = e.personId JOIN Country c ON e.country = c.country_name WHERE `status` IN(?) GROUP BY `country`", "`status`", "'killed'"));//));

      //Test 5
      tests.add(new RqlTest("aggregate(count,country,result)&includes=country,result&in(status,killed,other)", //
                            "select * from entries", //
                            "select `country`, COUNT(`country`) AS 'result' from entries WHERE `status` IN('killed', 'other') GROUP BY `country`", //
                            "select `country`, COUNT(`country`) AS 'result' from entries WHERE `status` IN(?, ?) GROUP BY `country`", //
                            "`status`", "'killed'", "`status`", "'other'"));

      tests.add(new RqlTest("aggregate(count,country,result)&includes=result&in(status,killed)", //
                            "select * from entries", //
                            "select COUNT(`country`) AS 'result' from entries WHERE `status` IN('killed') GROUP BY `country`", //
                            null));

      tests.add(new RqlTest("sort=first,-second,+third", //
                            "select * from table1", //
                            "select * from table1 ORDER BY `first` ASC, `second` DESC, `third` ASC", //
                            null));

      tests.add(new RqlTest("pagenum=10&pagesize=20)", //
                            "select * from table1", //
                            "select * from table1 LIMIT 180, 20", //
                            null));

      tests.add(new RqlTest("page(10,20)", //
                            "select * from table1", //
                            "select * from table1 LIMIT 180, 20", //
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
                            "select `firstName`, `lastName`, SUM(`age`), MAX(`height`) AS 'tallest', MIN(`height`) AS 'shortest' from table1 WHERE `firstName` = 'Wells' AND `lastName` = 'Burke' GROUP BY `firstName`, `lastName`", //
                            null));

      tests.add(new RqlTest("in(firstName,wells,joe,karl)", //
                            "select * from table1", //
                            "select * from table1 WHERE `firstName` IN('wells', 'joe', 'karl')", //
                            null));

      //Test 15
      tests.add(new RqlTest("out(firstName,wells,joe,karl)", //
                            "select * from table1", //
                            "select * from table1 WHERE `firstName` NOT IN('wells', 'joe', 'karl')", //
                            null));

      tests.add(new RqlTest("firstName=in=wells,joe,karl", //
                            "select * from table1", //
                            "select * from table1 WHERE `firstName` IN('wells', 'joe', 'karl')", //
                            null));

      tests.add(new RqlTest("firstName=Wells&eq(lastName,Burke)&includes=firstName,lastName", //
                            "select * from table1", //
                            "select `firstName`, `lastName` from table1 WHERE `firstName` = 'Wells' AND `lastName` = 'Burke'", //
                            null));

      tests.add(new RqlTest("firstName=Wells&eq(lastName,Burke)&includes(firstName,lastName)", //
                            "select * from table1", //
                            "select `firstName`, `lastName` from table1 WHERE `firstName` = 'Wells' AND `lastName` = 'Burke'", //
                            null));

      tests.add(new RqlTest("distinct&firstName=Wells&eq(lastName,Burke)&includes(firstName,lastName)", //
                            "select * from table1", //
                            "select DISTINCT `firstName`, `lastName` from table1 WHERE `firstName` = 'Wells' AND `lastName` = 'Burke'", //
                            null));

      //Test 20
      tests.add(new RqlTest("firstName=Wells&distinct&eq(lastName,Burke)", //
                            "select * from table1", //
                            "select DISTINCT * from table1 WHERE `firstName` = 'Wells' AND `lastName` = 'Burke'", //
                            null));

      tests.add(new RqlTest("includes=lastName&firstName=wells&lastName=Burke&state=ne=CA&age=ge=38", //
                            "select * from table1", //
                            "select `lastName` from table1 WHERE `firstName` = 'wells' AND `lastName` = 'Burke' AND `state` <> 'CA' AND `age` >= 38", //
                            "select `lastName` from table1 WHERE `firstName` = ? AND `lastName` = ? AND `state` <> ? AND `age` >= ?", //
                            "`firstName`", "'wells'", "`lastName`", "'Burke'", "`state`", "'CA'", "`age`", "38"));

      tests.add(new RqlTest("sum(if(eq(`col1`,'value'),1,'something blah'))", //
                            "SELECT * from table1", //
                            "SELECT *, SUM(IF(`col1` = 'value', 1, 'something blah')) from table1", //
                            "SELECT *, SUM(IF(`col1` = ?, 1, 'something blah')) from table1", //
                            "`col1`", "'value'"));

      tests.add(new RqlTest("sum(if(eq(`col1`,if(ne(`col2`,'val1'),`col3`,'val2')),1,'something blah'))", //
                            "SELECT * from table1", //
                            "SELECT *, SUM(IF(`col1` = IF(`col2` <> 'val1', `col3`, 'val2'), 1, 'something blah')) from table1", //
                            "SELECT *, SUM(IF(`col1` = IF(`col2` <> ?, `col3`, ?), 1, 'something blah')) from table1", //
                            "`col2`", "'val1'", "`col1`", "'val2'"));

      tests.add(new RqlTest("includes=firstName,lastName", //
                            "select * from table1", //
                            "select `firstName`, `lastName` from table1", //
                            "select `firstName`, `lastName` from table1"));

      //Test 25
      tests.add(new RqlTest("as(firstName, name)", //
                            "select * from table1", //
                            "select *, `firstName` AS 'name' from table1", // 
                            "select *, `firstName` AS 'name' from table1"));//the dynamicsql and preparedsql should match without fields

      tests.add(new RqlTest("firstName=wells&lastName=Burke&state=ne=CA&age=ge=38&includes=lastName", //
                            "select * from table1", //
                            "select `lastName` from table1 WHERE `firstName` = 'wells' AND `lastName` = 'Burke' AND `state` <> 'CA' AND `age` >= 38", //
                            "select `lastName` from table1 WHERE `firstName` = ? AND `lastName` = ? AND `state` <> ? AND `age` >= ?", //
                            "`firstName`", "'wells'", "`lastName`", "'Burke'", "`state`", "'CA'", "`age`", "38"));

      tests.add(new RqlTest("group(startYear)&includes=startYear&startYear=ne=null&as(count(motiveConfirmed), 'Motive Spaces Confirmed')", //
                            "SELECT * FROM Person p JOIN Entry e ON p.id = e.personId  JOIN Country c ON e.country = c.country_name", //
                            "SELECT `startYear`, COUNT(`motiveConfirmed`) AS 'Motive Spaces Confirmed' FROM Person p JOIN Entry e ON p.id = e.personId  JOIN Country c ON e.country = c.country_name WHERE `startYear` IS NOT NULL  GROUP BY `startYear`", //
                            null));

      tests.add(new RqlTest("group(startYear)&includes=startYear,'Motive Spaces Confirmed'&startYear=ne=null&as(sum(if(eq(motiveConfirmed,true),1,0)), 'Motive Spaces Confirmed')", //
                            "SELECT * FROM Person p JOIN Entry e ON p.id = e.personId  JOIN Country c ON e.country = c.country_name", //
                            "SELECT `startYear`, SUM(IF(`motiveConfirmed` = TRUE, 1, 0)) AS 'Motive Spaces Confirmed' FROM Person p JOIN Entry e ON p.id = e.personId  JOIN Country c ON e.country = c.country_name WHERE `startYear` IS NOT NULL GROUP BY `startYear`", //
                            null));

      tests.add(new RqlTest("motiveConfirmed=someValue", //
                            "SELECT * FROM Person", //
                            "SELECT * FROM Person WHERE `motiveConfirmed` = 'someValue'", //
                            null));

      //TEST 30
      tests.add(new RqlTest("includes='A Col With Caps and Spaces'&as(max(someCol), 'A Col With Caps and Spaces')", //
                            "SELECT * FROM Person", //
                            "SELECT MAX(`someCol`) AS 'A Col With Caps and Spaces' FROM Person", //
                            null));

      tests.add(new RqlTest("includes=y,a,b,z&as(max(col1),b)&as(max(col2),a)", //
                            "SELECT * FROM Person", //
                            "SELECT `y`, MAX(`col2`) AS 'a', MAX(`col1`) AS 'b', `z` FROM Person", //
                            null));

      tests.add(new RqlTest("includes=a,b&as(max(col1),b)&as(max(col2),a)", //
                            "SELECT * FROM Person", //
                            "SELECT MAX(`col2`) AS 'a', MAX(`col1`) AS 'b' FROM Person", //
                            null));

      tests.add(
            new RqlTest("as(sum(if(eq(type,'Media Worker'),1,0)),'Media Worker')&group(startYear)&includes=startYear,'Motive Confirmed','Media Worker','Motive Unconfirmed'&startYear=ne=null&as(sum(if(eq(motiveConfirmed,Confirmed),1,0)),'Motive Confirmed')&as(sum(if(eq(motiveConfirmed,Unconfirmed),1,0)),'Motive Unconfirmed')", //
                        "SELECT * FROM Entry", //
                        "SELECT `startYear`, SUM(IF(`motiveConfirmed` = 'Confirmed', 1, 0)) AS 'Motive Confirmed', SUM(IF(`type` = 'Media Worker', 1, 0)) AS 'Media Worker', SUM(IF(`motiveConfirmed` = 'Unconfirmed', 1, 0)) AS 'Motive Unconfirmed'  FROM Entry WHERE `startYear` IS NOT NULL GROUP BY `startYear`", //
                        "SELECT `startYear`, SUM(IF(`motiveConfirmed` = ?, 1, 0)) AS 'Motive Confirmed', SUM(IF(`type` = ?, 1, 0)) AS 'Media Worker', SUM(IF(`motiveConfirmed` = ?, 1, 0)) AS 'Motive Unconfirmed'  FROM Entry WHERE `startYear` IS NOT NULL GROUP BY `startYear`", //
                        "`motiveConfirmed`", "'Confirmed'", "`type`", "'Media Worker'", "`motiveConfirmed`", "'Unconfirmed'"));

      tests.add(
            new RqlTest("as(sum(if(eq(type,'Media Worker'),1,0)),'Media Worker')&group(startYear)&includes=startYear,'Motive Confirmed','Media Worker','Motive Unconfirmed'&startYear=ne=null&as(sum(if(eq(motiveConfirmed,Confirmed),1,0)),'Motive Confirmed')&as(sum(if(eq(motiveConfirmed,Unconfirmed),1,0)),'Motive Unconfirmed')&or(eq(`type`,'Media Worker'),ne(`motiveConfirmed`,NULL))", //
                        "SELECT * FROM Entry", //
                        "SELECT `startYear`, SUM(IF(`motiveConfirmed` = 'Confirmed', 1, 0)) AS 'Motive Confirmed', SUM(IF(`type` = 'Media Worker', 1, 0)) AS 'Media Worker', SUM(IF(`motiveConfirmed` = 'Unconfirmed', 1, 0)) AS 'Motive Unconfirmed'  FROM Entry WHERE `startYear` IS NOT NULL AND (`type` = 'Media Worker' OR `motiveConfirmed` IS NOT NULL) GROUP BY `startYear`", //
                        "SELECT `startYear`, SUM(IF(`motiveConfirmed` = ?, 1, 0)) AS 'Motive Confirmed', SUM(IF(`type` = ?, 1, 0)) AS 'Media Worker', SUM(IF(`motiveConfirmed` = ?, 1, 0)) AS 'Motive Unconfirmed'  FROM Entry WHERE `startYear` IS NOT NULL AND (`type` = ? OR `motiveConfirmed` IS NOT NULL) GROUP BY `startYear`", //
                        "`motiveConfirmed`", "'Confirmed'", "`type`", "'Media Worker'", "`motiveConfirmed`", "'Unconfirmed'", "`type`", "'Media Worker'"));

      //TEST 35
      tests.add(
            new RqlTest("countascol(impunity, 'Full Justice', 'Partial Impunity', 'Complete Impunity')", //
                        "SELECT * FROM Person", //
                        "SELECT *, SUM(IF(`impunity` = 'Full Justice', 1, 0)) AS 'Full Justice', SUM(IF(`impunity` = 'Partial Impunity', 1, 0)) AS 'Partial Impunity', SUM(IF(`impunity` = 'Complete Impunity', 1, 0)) AS 'Complete Impunity' FROM Person WHERE `impunity` IN('Full Justice', 'Partial Impunity', 'Complete Impunity')", //
                        "SELECT *, SUM(IF(`impunity` = ?, 1, 0)) AS 'Full Justice', SUM(IF(`impunity` = ?, 1, 0)) AS 'Partial Impunity', SUM(IF(`impunity` = ?, 1, 0)) AS 'Complete Impunity' FROM Person WHERE `impunity` IN(?, ?, ?)", //
                        "`impunity`", "'Full Justice'", "`impunity`", "'Partial Impunity'", "`impunity`", "'Complete Impunity'", "`impunity`", "'Full Justice'", "`impunity`", "'Partial Impunity'", "`impunity`", "'Complete Impunity'"));

      tests.add(
            new RqlTest("in(status, killed)&group(victim)&count(victim, number)", //
                        "SELECT * FROM (SELECT *, 'Threatened' as 'Victim' FROM Entry WHERE threatened = true UNION SELECT *, 'Tortured' as 'Victim' FROM Entry WHERE tortured = true  UNION SELECT *, 'Taken Captive' as 'Victim' FROM Entry WHERE captive = true )as t", //
                        "SELECT *, COUNT(`victim`) AS 'number' FROM (SELECT *, 'Threatened' as 'Victim' FROM Entry WHERE threatened = true UNION SELECT *, 'Tortured' as 'Victim' FROM Entry WHERE tortured = true  UNION SELECT *, 'Taken Captive' as 'Victim' FROM Entry WHERE captive = true )as t WHERE `status` IN('killed') GROUP BY `victim`", //
                        null));

      tests.add(
            new RqlTest("group(victim)&count(victim, number)", //
                        "SELECT * FROM (SELECT *, 'Threatened' as 'Victim' FROM Entry WHERE threatened = true UNION SELECT *, 'Tortured' as 'Victim' FROM Entry WHERE tortured = true  UNION SELECT *, 'Taken Captive' as 'Victim' FROM Entry WHERE captive = true )as t", //
                        "SELECT *, COUNT(`victim`) AS 'number' FROM (SELECT *, 'Threatened' as 'Victim' FROM Entry WHERE threatened = true UNION SELECT *, 'Tortured' as 'Victim' FROM Entry WHERE tortured = true  UNION SELECT *, 'Taken Captive' as 'Victim' FROM Entry WHERE captive = true )as t GROUP BY `victim`", //
                        "SELECT *, COUNT(`victim`) AS 'number' FROM (SELECT *, 'Threatened' as 'Victim' FROM Entry WHERE threatened = true UNION SELECT *, 'Tortured' as 'Victim' FROM Entry WHERE tortured = true  UNION SELECT *, 'Taken Captive' as 'Victim' FROM Entry WHERE captive = true )as t GROUP BY `victim`"));

      tests.add(new RqlTest("as(sum(if(and(eq(motiveConfirmed,Confirmed),eq(type,Journalist)),1,0)),'Motive Confirmed')", //
                            "SELECT * FROM Test", //
                            "SELECT *, SUM(IF((`motiveConfirmed` = 'Confirmed' AND `type` = 'Journalist'), 1, 0)) AS 'Motive Confirmed' FROM Test", //
                            null));

      rql = "includes=Year,'Motive Confirmed','Media Worker','Motive Unconfirmed'&as(year, Year)&as(sum(if(and(eq(motiveConfirmed,Confirmed),eq(type,Journalist)),1,0)),'Motive Confirmed')&as(sum(if(eq(type,'Media Worker'),1,0)),'Media Worker')&as(sum(if(eq(motiveConfirmed,Unconfirmed),1,0)),'Motive Unconfirmed')&or(eq(type, 'Media Worker'), ne(motiveConfirmed, null))&status=Killed&order=-Year";

      select = "";
      select += " SELECT y.year, e.*, p.*  ";
      select += " FROM Entry e  ";
      select += " JOIN Year y ON y.year < YEAR(CURDATE()) AND (((e.startYear <= year) AND (e.endYear is NULL OR e.endYear >= year) AND status != 'Killed') OR (status = 'Killed' AND e.startYear = year)) ";
      select += " JOIN Person p ON e.personId = p.id ";
      select += " LEFT JOIN Country c ON e.country = c.country_name";

      dynamicSql = "";
      dynamicSql += " SELECT y.year, e.*, p.*,  ";
      dynamicSql += " `year` AS 'Year', SUM(IF((`motiveConfirmed` = 'Confirmed' AND `type` = 'Journalist'), 1, 0)) AS 'Motive Confirmed', SUM(IF(`type` = 'Media Worker', 1, 0)) AS 'Media Worker', SUM(IF(`motiveConfirmed` = 'Unconfirmed', 1, 0)) AS 'Motive Unconfirmed'";
      dynamicSql += " FROM Entry e  ";
      dynamicSql += " JOIN Year y ON y.year < YEAR(CURDATE()) AND (((e.startYear <= year) AND (e.endYear is NULL OR e.endYear >= year) AND status != 'Killed') OR (status = 'Killed' AND e.startYear = year)) ";
      dynamicSql += " JOIN Person p ON e.personId = p.id ";
      dynamicSql += " LEFT JOIN Country c ON e.country = c.country_name";
      dynamicSql += " WHERE (`type` = 'Media Worker' OR `motiveConfirmed` IS NOT NULL) ";
      dynamicSql += " AND `status` = 'Killed' ";
      dynamicSql += " ORDER BY `Year` DESC";

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
      dynamicSql += " WHERE (`type` = 'Media Worker' OR `motiveConfirmed` IS NOT NULL) ";
      dynamicSql += " AND `status` = 'Killed' ";
      dynamicSql += " ORDER BY `Year` DESC";

      //40
      tests.add(new RqlTest(rql, //
                            select, //
                            dynamicSql, //
                            null));

      //      rql = "group(year)&includes=Year&as(year,Year)&countascol(impunity,'Full Justice','Partial Impunity','Complete Impunity')&status='Killed'&typeOfDeath=Murder&order=-year";
      //
      //      tests.add(new RqlTest(rql, //
      //                            select, //
      //                            dynamicSql, //
      //                            null));

      tests.add(new RqlTest("firstName=*w*", //
                            "select * from table1", //
                            "select * from table1 WHERE `firstName` LIKE '%w%'", //
                            "select * from table1 WHERE `firstName` LIKE ?", "`firstName`", "'%w%'"));

      tests.add(new RqlTest("q=firstName=*w*", //
                            "select * from table1", //
                            "select * from table1 WHERE `firstName` LIKE '%w%'", //
                            "select * from table1 WHERE `firstName` LIKE ?", "`firstName`", "'%w%'"));

      tests.add(new RqlTest("pagenum=1", //
                            "select * from table1", //
                            "select SQL_CALC_FOUND_ROWS * from table1", //
                            null));

      tests.add(new RqlTest("as(if(captive, 'Taken Captive', 'Not Taken Captive'), 'Captive')", //
                            "select * from table1", //
                            "select *, IF(`captive`, 'Taken Captive', 'Not Taken Captive') AS 'Captive' from table1", //
                            null));

      boolean passed = true;
      int running = 0;
      for (int j = tests.size() - 1; j >= 0; j--)
      {
         running = j + 1;
         RqlTest test = tests.get(j);
         try
         {
            String output = RQL.toSql(test.select, split(test.rql)).sql;
            if (!compare(test.dynamicSql, output))
            {
               passed = false;
               break;
            }

            if (test.preparedSql != null)
            {
               Replacer r = new Replacer();
               output = RQL.toSql(test.select, split(test.rql), r).sql;
               if (!compare(test.preparedSql, output))
               {
                  passed = false;
                  break;
               }

               for (int i = 0; i < r.cols.size(); i++)
               {
                  try
                  {
                     if (!r.cols.get(i).equals(test.fields[i * 2]))
                        passed = false;

                     if (!r.vals.get(i).equals(test.fields[(i * 2) + 1]))
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
                  System.out.println(output);
                  System.out.println("replaced parameters do not match up: " + r.cols + " - " + r.vals + " - " + Arrays.asList(test.fields));
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
         System.out.println("FAILED: " + running);
      }
   }

   public static boolean compare(String str1, String str2)
   {
      str1 = str1.replaceAll("\\s+", " ").trim();
      str2 = str2.replaceAll("\\s+", " ").trim();

      if (!str1.equals(str2))
      {
         System.out.println("\r\n");
         System.out.println("\r\n");
         System.out.println(str1);
         System.out.println(str2);

         for (int i = 0; i < str1.length() && i < str2.length(); i++)
         {
            if (str1.charAt(i) == str2.charAt(i))
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

         String err = "failed test: " + str1 + " != " + str2;
         return false;
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
