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
package io.inversion.cloud.action.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;
import org.junit.jupiter.api.Test;

import io.inversion.cloud.model.JSArray;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.service.Engine;
import io.inversion.cloud.utils.Utils;

public abstract class AbstractRestGetActionIntegTest extends AbstractRestActionIntegTest
{

   public AbstractRestGetActionIntegTest(String dbType)
   {
      super(dbType);
   }

   @Test
   public void testLimit01() throws Exception
   {
      Engine engine = engine();
      Response res = null;
      JSNode json = null;

      res = engine.get(url("orders?limit=5")).assertOk();
      json = res.getJson();

      assertEquals(5, json.find("meta.pageSize"));
      assertEquals(5, res.data().length());
   }

   @Test
   public void testSort01() throws Exception
   {
      Engine engine = engine();
      Response res = null;

      String url = url("orders?limit=2&sort=orderid");
      res = engine.get(url);
      assertEquals(2, res.data().length());

      String href = res.findString("data.0.href");
      assertTrue(href.indexOf("/orders/10248") > 0);

      res = engine.get(url("orders?limit=2&sort=-orderid&type=ORDER"));
      assertEquals(2, res.data().length());

      href = res.findString("data.0.href");
      assertTrue(href.indexOf("/orders/10272") > 0);
   }

   @Test
   public void testPagination01() throws Exception
   {
      Engine engine = engine();
      Response res = null;

      //--
      //-- gets a sorted list of all hrefs
      res = engine.get(url("orders?type=ORDER&sort=orderId&includes=href&limit=1000"));

      res.dump();

      List<String> hrefs = new ArrayList();
      res.data().forEach(o -> hrefs.add(((JSNode) o).getString("href")));

      //makes sure they are all unqique
      Set uniqueHrefs = new HashSet(hrefs);
      assertEquals(hrefs.size(), uniqueHrefs.size());

      //makes sure they are sorted
      List sortedHrefs = new ArrayList(hrefs);
      Collections.sort(hrefs);
      assertEquals(hrefs.toString(), sortedHrefs.toString());

      int total = 0;
      int pages = 0;

      String start = url("orders?limit=3&sort=orderId");
      String next = start;

      Set alreadyFound = new HashSet();
      do
      {
         res = engine.get(next);

         next = next.toLowerCase();
         assertTrue(next.indexOf("pagenum") == next.lastIndexOf("pagenum"), "There should be only one pagenum param");
         assertTrue(next.indexOf("page") == next.lastIndexOf("page"), "There should be only one page param");
         assertTrue(next.indexOf("offset") == next.lastIndexOf("offset"), "There should be only one offset param");
         assertTrue(next.indexOf("limit") == next.lastIndexOf("limit"), "There should be only one limit param");
         assertTrue(next.indexOf("after") == next.lastIndexOf("after"), "There should be only one after param");

         if (res.data().size() == 0)
            break;

         //makes sure the indexing is correct
         int idx = pages * 3;
         String found = res.findString("data.0.href");
         String shouldBe = hrefs.get(idx);
         if (!shouldBe.equals(found)// 
               || alreadyFound.contains(found))
         {
            //http://localhost/northwind/dynamodb/orders?limit=127&sort=orderId&after(type,ORDER,orderId,10374)
            //GET: http://localhost/northwind/dynamodb/orders?limit=127&sort=orderId&after(type,ORDER,orderId,10374)&after(type,ORDER,orderId,10501)&after(type,ORDER,orderId,10628)&after(type,ORDER,orderId,10755)

            System.out.println("Request  : " + next);
            System.out.println("Index    : " + idx);
            System.out.println("Expected : " + shouldBe);
            System.out.println("Found    : " + found);
            System.out.println("The Found Href is in the sorted list at index: " + sortedHrefs.indexOf(found));

            res.dump();
            fail();
            //assertEquals(shouldBe, found);
         }
         alreadyFound.add(found);

         total += res.data().length();
         pages += 1;

         next = res.next();

         if (next != null)
         {
            assertEquals(3, res.data().length());
            assertEquals(3, res.find("meta.pageSize"));
         }
      }
      while (pages < 200 && next != null);

      assertEquals(9, pages);
      assertEquals(25, total);
   }

   @Test
   public void testEq01() throws Exception
   {
      Engine engine = engine();
      Response res = null;
      JSNode json = null;
      res = engine.get(url("orders?eq(orderid,10257)"));
      res.dump();
      json = res.getJson();
      assertEquals(1, res.data().size());
      assertTrue(res.findString("data.0.orderid").equals("10257"));
   }

   @Test
   public void testLike01() throws Exception
   {
      Engine engine = engine();
      assertEquals(2, engine.get(url("orders?like(customerId,*VI*)")).getFoundRows());
   }

   @Test
   public void testLike02() throws Exception
   {
      Engine engine = engine();
      Response res = null;
      JSNode json = null;

      res = engine.get(url("orders?limit=5&like(customerId,*ZZ*)")).assertOk();
      json = res.getJson();
      assertTrue(json.getArray("data").length() == 0);
   }

   @Test
   public void testW01() throws Exception
   {
      Engine engine = engine();
      Response res = null;
      res = engine.get(url("employees?w(city,ondo)")).assertOk();

      assertEquals(4, res.data().length());
      for (Object obj : res.data())
      {
         assertTrue(((JSNode) obj).getString("city").contains("ondo"));
      }

      //--the *s should not be necessary
      res = engine.get(url("employees?w(city,*ondo*)")).assertOk();
      assertEquals(4, res.data().length());
      for (Object obj : res.data())
      {
         assertTrue(((JSNode) obj).getString("city").contains("ondo"));
      }

   }

   @Test
   public void testWo01() throws Exception
   {
      Engine engine = engine();
      Response res = null;
      JSNode json = null;
      JSArray data = null;

      res = engine.get(url("orders?eq(employeeid,5)"));
      json = res.getJson();
      assertEquals(3, json.find("meta.foundRows"));

      res = engine.get(url("orders?eq(employeeid,5)&w(shipcountry,witze)"));
      assertEquals(1, res.getFoundRows());

      res = engine.get(url("orders?eq(employeeid,5)&wo(shipcountry,witze)"));
      assertEquals(2, res.getFoundRows());
   }

   @Test
   public void testSw01() throws Exception
   {
      Engine engine = engine();
      Response res = null;
      res = engine.get(url("orders?limit=5&sw(customerId,VI)")).assertOk();

      JSArray data = res.data();
      assertTrue(data.length() > 0);
      for (Object o : data)
      {
         assertTrue(((JSNode) o).getString("customerid").startsWith("VI"));
      }

      //check that the trailing * is not doubled
      assertEquals(res.getFoundRows(), engine.get(url("orders?limit=5&sw(customerId,VI*)")).assertOk().getFoundRows());
   }

   @Test
   public void testSw_noRecordsShouldMatch() throws Exception
   {
      Engine engine = engine();
      assertEquals(0, engine.get(url("orders?limit=5&sw(customerId,Z)")).assertOk().getFoundRows());
   }

   @Test
   public void testEw01() throws Exception
   {
      Engine engine = engine();
      Response res = null;
      res = engine.get(url("orders?ew(shipname,Chevalier)"));

      JSArray data = res.data();
      assertTrue(data.size() == 1);
      for (Object o : data)
      {
         assertTrue(((JSNode) o).getString("shipname").endsWith("Chevalier"));
      }

      //check that the leading * is not doubled
      res = engine.get(url("orders?ew(shipname,*Chevalier)"));
      data = res.data();
      assertTrue(data.size() == 1);
      for (Object o : data)
      {
         assertTrue(((JSNode) o).getString("shipname").endsWith("Chevalier"));
      }

   }

   @Test
   public void testN01() throws Exception
   {
      Engine engine = engine();
      Response res = null;
      JSNode json = null;
      JSArray data = null;
      res = engine.get(url("orders?n(shipregion)"));
      res.assertOk();
      json = res.getJson();
      data = json.getArray("data");
      assertEquals(15, data.length());
      assertEquals(15, res.getFoundRows());
      for (Object o : data)
      {
         String shipRegion = ((JSNode) o).getString("shipRegion");
         if (!"null".equalsIgnoreCase(shipRegion + ""))
         {
            System.out.println("should be null: '" + StringEscapeUtils.escapeJava(shipRegion) + "'");
            fail("should be null: '" + StringEscapeUtils.escapeJava(shipRegion) + "'");
         }
      }
   }

   @Test
   public void testN02() throws Exception
   {
      Engine engine = engine();
      assertTrue(engine.get(url("orders?limit=5&nn(shipcountry)")).assertOk().data().size() > 0);
      assertTrue(engine.get(url("orders?limit=5&n(shipcountry)")).assertOk().data().size() == 0);
   }

   @Test
   public void testEmp01() throws Exception
   {
      Engine engine = engine();
      Response res = null;

      res = engine.get(url("orders?limit=1&emp(shipregion)"));
      assertEquals(15, res.getFoundRows());

      for (JSNode result : res.data().asNodeList())
      {
         assertTrue(Utils.empty(result.getString("shipregion")));
      }
   }

   @Test
   public void testNn01() throws Exception
   {
      Engine engine = engine();
      Response res = null;
      JSNode json = null;
      JSArray data = null;

      res = engine.get(url("orders?limit=500&nn(shipregion)")).assertOk();

      json = res.getJson();
      data = json.getArray("data");
      assertTrue(data.length() > 0);
      for (Object o : data)
      {
         assertNotNull(((JSNode) o).getString("shipregion"));
      }
   }

   @Test
   public void testNemp01() throws Exception
   {
      Engine engine = engine();
      Response res = null;

      res = engine.get(url("orders?limit=1&nemp(shipregion)"));
      assertEquals(10, res.getFoundRows());

      for (JSNode result : res.data().asNodeList())
      {
         assertTrue(!Utils.empty(result.getString("shipregion")));
      }
   }

   @Test
   public void testIn01() throws Exception
   {
      Engine engine = engine();
      Response res = null;
      res = engine.get(url("orders?in(orderid,10249,10258,10252)"));

      JSArray data = res.data();
      List<String> list = Arrays.asList("10249", "10258", "10252");
      assertEquals(3, data.length());
      for (Object obj : data)
      {
         assertTrue(list.contains(((JSNode) obj).getString("orderId")));
      }
   }

   @Test
   public void testOut01() throws Exception
   {
      Engine engine = engine();
      Response res = engine.get(url("orders?out(orderid,10249,10258,10252)"));

      Set ids = new HashSet(Utils.explode(",", "10249,10258,10252"));
      assertTrue(res.getFoundRows() == 22);
      for (Object obj : res.data())
      {
         assertFalse(ids.contains(((JSNode) obj).find("orderId").toString()));
      }
   }

   @Test
   public void testLt01() throws Exception
   {
      Engine engine = engine();
      Response res = null;

      res = engine.get(url("orders?lt(freight,3.25)"));

      res.dump();
      assertEquals(1, res.getFoundRows());

      for (JSNode node : res.data().asNodeList())
      {
         float val = Float.parseFloat(node.getString("freight"));
         assertTrue(val < 3.25);
      }
   }

   @Test
   public void testLe01() throws Exception
   {
      Engine engine = engine();
      Response res = null;

      res = engine.get(url("orders?le(freight,3.2500)"));
      assertEquals(2, res.getFoundRows());

      JSArray data = res.data();
      for (Object o : data)
      {
         float val = Float.parseFloat(((JSNode) o).getString("freight"));
         if (val > 3.25f)
         {
            fail("Value is greater than threshold: " + val);
         }
      }
   }

}
