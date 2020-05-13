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
package io.inversion.dynamodb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import io.inversion.Db;
import io.inversion.action.db.AbstractDbGetActionIntegTest;
import io.inversion.utils.Utils;

/**
 * @see README.md
 */
@TestInstance(Lifecycle.PER_CLASS)
public class DynamoDbGetActionIntegTest extends AbstractDbGetActionIntegTest
{

   public DynamoDbGetActionIntegTest()
   {
      super("dynamo");
   }

   @Override
   public void initializeDb()
   {
      Db db = getDb();
      if (db == null)
      {
         try
         {
            db = DynamoDbFactory.buildNorthwindDynamoDb();
         }
         catch (Exception e)
         {
            Utils.rethrow(e);
         }
         setDb(db);
      }
   }

   protected String collectionPath()
   {
      return "northwind/dynamodb/";
   }

   protected String url(String path)
   {
      String url = super.url(path);
      if (path.indexOf("/orders") > 0 && path.indexOf("type") < 0)
      {
         if (url.indexOf("?") < 0)
            url += "?";
         else
            url += "&";

         url += "type=ORDER";
      }
      return url;
   }

   @Test
   @Override
   public void testOut01() throws Exception
   {
      System.err.println("IMPLEMENT ME TestDynamoDbGetActions.testOut01()");
      //TODO: implement me
   }

   @Test
   @Override
   public void testLike01() throws Exception
   {
      System.err.println("DynamoDb implementation does not support the like() operator...skipping test.");
   }

   @Test
   @Override
   public void testLike02() throws Exception
   {
      System.err.println("DynamoDb implementation does not support the like() operator...skipping test.");
   }

   @Test
   @Override
   public void testW01() throws Exception
   {
      System.err.println("DynamoDb implementation does not support the w() operator....skipping test.");
   }

   @Test
   @Override
   public void testWo01() throws Exception
   {
      System.err.println("DynamoDb implementation does not support the wo() operator....skipping test.");
   }

   @Test
   @Override
   public void testEw01() throws Exception
   {
      System.err.println("DynamoDb implementation does not support the ew() operator....skipping test.");
   }

   //   @Test
   //   public void testSort01() throws Exception
   //   {
   //      Engine engine = engine();
   //      Response res = null;
   //      JSNode json = null;
   //      String href = null;
   //
   //      String url = url("orders?limit=2&type=ORDER");
   //
   //      res = engine.get(url);
   //
   //      //res.assertDebug("DynamoDb  QuerySpec:'gs3' maxPageSize=2 scanIndexForward=true nameMap={#var1=sk} valueMap={:val1=ORDER} keyConditionExpression='(#var1 = :val1)' filterExpression='' projectionExpression=''");
   //      assertEquals(2, res.findArray("data").length());
   //      assertTrue(res.findString("data.0.href").endsWith("northwind/dynamodb/orders/10248~ORDER"));
   //
   //      res = engine.get(url("orders?limit=2&type=ORDER&sort=-orderid"));
   //      //res.assertDebug("DynamoDb  QuerySpec:'gs3' maxPageSize=2 scanIndexForward=false nameMap={#var1=sk} valueMap={:val1=ORDER} keyConditionExpression='(#var1 = :val1)' filterExpression='' projectionExpression=''");
   //      assertEquals(2, res.findArray("data").length());
   //      assertTrue(res.findString("data.0.href").endsWith("northwind/dynamodb/orders/11077~ORDER"));
   //   }
   //
   //   public void testSort02DescendingSortRequiresPartitionKey() throws Exception
   //   {
   //      assertEquals(400, engine().get(url("orders?limit=2&sort=-orderid")).getStatusCode());
   //   }
   //
   //   @Test
   //   public void test0() throws Exception
   //   {
   //      Engine engine = engine();
   //      Response res = null;
   //      JSNode json = null;
   //
   //      res = engine.get("northwind/dynamodb/orders?limit=5").assertOk();
   //      json = res.getJson();
   //
   //      assertEquals(5, json.getArray("data").length());
   //      //res.assertDebug("DynamoDb  ScanSpec maxPageSize=5 scanIndexForward=true nameMap={} valueMap={} keyConditionExpression='' filterExpression='' projectionExpression=''");
   //
   //      res = engine.get("northwind/dynamodb/orders?limit=1000&sort=orderid&type=ORDER");
   //      //res.assertDebug("DynamoDb  QuerySpec:'gs3' maxPageSize=1000 scanIndexForward=true nameMap={#var1=sk} valueMap={:val1=ORDER} keyConditionExpression='(#var1 = :val1)' filterExpression='' projectionExpression=''");
   //
   //      json = res.getJson();
   //      assertEquals(830, json.getArray("data").length());
   //      String first = json.findString("data.0.href");
   //      //assertTrue(first.endsWith("/dynamodb/orders/10641~ORDER"));
   //      assertTrue(first.endsWith("/dynamodb/orders/10248~ORDER"));
   //
   //      res = engine.get("northwind/dynamodb/orders/10248~ORDER");
   //      assertEquals(1, res.getData().length());
   //
   //   }

}
