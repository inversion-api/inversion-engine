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
package io.inversion.cloud.action.dynamo;

import org.junit.Test;

import io.inversion.cloud.action.rest.TestRestGetActions;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.service.Engine;

/**
 * @see README.md
 */
public class DynamoDbGetActionsIntegTest extends TestRestGetActions
{

   protected String collectionPath()
   {
      return "northwind/dynamodb/";
   }

   @Override
   protected Engine service() throws Exception
   {
      return DynamoDbEngineFactory.service();
   }

   @Test
   public void testOut01() throws Exception
   {
      System.err.println("IMPLEMENT ME TestDynamoDbGetActions.testOut01()");
      //TODO: implement me
   }

   public void testLike01() throws Exception
   {
      System.err.println("DynamoDb implementation does not support the like() operator...skipping test.");
   }

   public void testLike02() throws Exception
   {
      System.err.println("DynamoDb implementation does not support the like() operator...skipping test.");
   }

   @Test
   public void testW01() throws Exception
   {
      System.err.println("DynamoDb implementation does not support the w() operator....skipping test.");
   }

   public void testWo01() throws Exception
   {
      System.err.println("DynamoDb implementation does not support the wo() operator....skipping test.");
   }

   @Test
   public void testEw01() throws Exception
   {
      System.err.println("DynamoDb implementation does not support the ew() operator....skipping test.");
   }

   @Test
   public void testSort01() throws Exception
   {
      Engine engine = service();
      Response res = null;
      JSNode json = null;
      String href = null;

      String url = url("orders?limit=2&type=ORDER");

      res = engine.get(url);

      res.assertDebug("DynamoDb  QuerySpec:'gs3' maxPageSize=2 scanIndexForward=true nameMap={#var1=sk} valueMap={:val1=ORDER} keyConditionExpression='(#var1 = :val1)' filterExpression='' projectionExpression=''");
      assertEquals(2, res.findArray("data").length());
      assertTrue(res.findString("data.0.href").endsWith("northwind/dynamodb/orders/10248~ORDER"));

      res = engine.get(url("orders?limit=2&type=ORDER&sort=-orderid"));
      res.assertDebug("DynamoDb  QuerySpec:'gs3' maxPageSize=2 scanIndexForward=false nameMap={#var1=sk} valueMap={:val1=ORDER} keyConditionExpression='(#var1 = :val1)' filterExpression='' projectionExpression=''");
      assertEquals(2, res.findArray("data").length());
      assertTrue(res.findString("data.0.href").endsWith("northwind/dynamodb/orders/11077~ORDER"));
   }

   public void testSort02DescendingSortRequiresPartitionKey() throws Exception
   {
      assertEquals(400, service().get(url("orders?limit=2&sort=-orderid")).getStatusCode());
   }

   @Test
   public void test0() throws Exception
   {
      Engine engine = service();
      Response res = null;
      JSNode json = null;

      res = engine.get("northwind/dynamodb/orders?limit=5").assertOk();
      json = res.getJson();

      assertEquals(5, json.getArray("data").length());
      res.assertDebug("DynamoDb  ScanSpec maxPageSize=5 scanIndexForward=true nameMap={} valueMap={} keyConditionExpression='' filterExpression='' projectionExpression=''");

      res = engine.get("northwind/dynamodb/orders?limit=1000&sort=orderid&type=ORDER");
      res.assertDebug("DynamoDb  QuerySpec:'gs3' maxPageSize=1000 scanIndexForward=true nameMap={#var1=sk} valueMap={:val1=ORDER} keyConditionExpression='(#var1 = :val1)' filterExpression='' projectionExpression=''");

      json = res.getJson();
      assertEquals(830, json.getArray("data").length());
      String first = json.findString("data.0.href");
      //assertTrue(first.endsWith("/dynamodb/orders/10641~ORDER"));
      assertTrue(first.endsWith("/dynamodb/orders/10248~ORDER"));

      res = engine.get("northwind/dynamodb/orders/10248~ORDER");
      assertEquals(1, res.data().length());

   }

   @Test
   public void testA() throws Exception
   {
      Engine engine = service();
      Response res = null;
      JSNode json = null;

      res = engine.get("northwind/dynamodb/orders?shipname=Blauer See Delikatessen").assertOk();
      json = res.getJson();
      assertEquals(7, json.getArray("data").length());
      res.assertDebug("DynamoDb  ScanSpec maxPageSize=100 scanIndexForward=true nameMap={#var1=ls2} valueMap={:val1=Blauer See Delikatessen} keyConditionExpression='' filterExpression='(#var1 = :val1)' projectionExpression=''");
   }

   @Test
   public void testC() throws Exception
   {
      Engine engine = service();
      Response res = null;
      JSNode json = null;

      res = engine.service("GET", "northwind/dynamodb/orders?orderid=11058").assertOk();
      json = res.getJson();

      assertEquals(json.getArray("data").length(), 1);
      res.assertDebug("DynamoDb  QuerySpec:'Primary Index' maxPageSize=100 scanIndexForward=true nameMap={#var1=hk} valueMap={:val1=11058} keyConditionExpression='(#var1 = :val1)' filterExpression='' projectionExpression=''");
   }

   @Test
   public void testD() throws Exception
   {
      Engine engine = service();
      Response res = null;
      JSNode json = null;

      res = engine.get("northwind/dynamodb/orders?orderid=11058&type=ORDER").assertOk();
      json = res.getJson();

      assertEquals(json.getArray("data").length(), 1);
      res.assertDebug("DynamoDb  GetItemSpec partKeyCol=hk partKeyVal=11058 sortKeyCol=sk sortKeyVal=ORDER");

      res = engine.get("northwind/dynamodb/orders/11058~ORDER");
      json = res.getJson();

      res.dump();
      assertEquals(json.getArray("data").length(), 1);
      res.assertDebug("DynamoDb  GetItemSpec partKeyCol=hk partKeyVal=11058 sortKeyCol=sk sortKeyVal=ORDER");

      //[1]: DynamoDb  ScanSpec maxPageSize=100 scanIndexForward=true nameMap={#var1=hk, #var2=sk} valueMap={:val1=11058, :val2=ORDER} keyConditionExpression='' filterExpression='((#var1 = :val1) and (#var2 = :val2))' projectionExpression=''

   }

   @Test
   public void testE() throws Exception
   {
      Engine engine = service();
      Response res = null;
      JSNode json = null;

      res = engine.get("northwind/dynamodb/orders?eq(OrderId, 11058)&gt(type, 'AAAAA')").assertOk();
      json = res.getJson();

      assertEquals(json.getArray("data").length(), 1);
      res.assertDebug("DynamoDb  QuerySpec:'Primary Index' maxPageSize=100 scanIndexForward=true nameMap={#var1=hk, #var2=sk} valueMap={:val1=11058, :val2=AAAAA} keyConditionExpression='(#var1 = :val1) and (#var2 > :val2)' filterExpression='' projectionExpression=''");
   }

   @Test
   public void testF() throws Exception
   {
      Engine engine = service();
      Response res = null;
      JSNode json = null;

      res = engine.get("northwind/dynamodb/orders?eq(OrderId, 12345)&gt(type, 'AAAAA')&gt(ShipCity,A)").assertOk();
      json = res.getJson();

      res.assertDebug("DynamoDb  QuerySpec:'Primary Index' maxPageSize=100 scanIndexForward=true nameMap={#var1=hk, #var2=sk, #var3=ls1} valueMap={:val1=12345, :val2=AAAAA, :val3=A} keyConditionExpression='(#var1 = :val1) and (#var2 > :val2)' filterExpression='(#var3 > :val3)' projectionExpression=''");
   }

   @Test
   public void testG() throws Exception
   {
      Engine engine = service();
      Response res = null;
      JSNode json = null;

      res = engine.get("northwind/dynamodb/orders?eq(OrderId, 11058)&sw(type, 'ORD')");
      json = res.getJson();
      //System.out.println(res.getDebug());

      assertEquals(json.getArray("data").length(), 1);
      res.assertDebug("DynamoDb  QuerySpec:'Primary Index' maxPageSize=100 scanIndexForward=true nameMap={#var1=hk, #var2=sk} valueMap={:val1=11058, :val2=ORD} keyConditionExpression='(#var1 = :val1) and begins_with(#var2,:val2)' filterExpression='' projectionExpression=''");
   }

   @Test
   public void testH() throws Exception
   {
      Engine engine = service();
      Response res = null;

      res = engine.get("northwind/dynamodb/orders?eq(OrderId, 11058)&sw(type, 'ORD')&eq(shipcity,Mannheim)").assertOk();
      res.dump();

      assertEquals(res.data().length(), 1);
      res.assertDebug("DynamoDb  QuerySpec:'ls1' maxPageSize=100 scanIndexForward=true nameMap={#var1=hk, #var2=ls1, #var3=sk} valueMap={:val1=11058, :val2=Mannheim, :val3=ORD} keyConditionExpression='(#var1 = :val1) and (#var2 = :val2)' filterExpression='begins_with(#var3,:val3)' projectionExpression=''");
   }

   @Test
   public void testI() throws Exception
   {
      Engine engine = service();
      Response res = null;
      JSNode json = null;

      res = engine.get("northwind/source/orders?eq(OrderId, 11058)").assertOk();
      res = engine.get("northwind/dynamodb/orders?eq(OrderId, 11058)").assertOk();
      res = engine.get("northwind/dynamodb/orders?eq(OrderId, 11058)&eq(employeeId,9)").assertOk();
      res = engine.get("northwind/dynamodb/orders?eq(OrderId, 11058)&sw(type, 'ORD')&eq(employeeId,9)&eq(OrderDate,'2014-10-29T00:00-0400')").assertOk();
      res.assertDebug("DynamoDb  QuerySpec:'gs1' maxPageSize=100 scanIndexForward=true nameMap={#var4=hk, #var1=gs1hk, #var2=gs1sk, #var3=sk} valueMap={:val1=9, :val2=2014-10-29T00:00-0400, :val3=ORD, :val4=11058} keyConditionExpression='(#var1 = :val1) and (#var2 = :val2)' filterExpression='begins_with(#var3,:val3) and (#var4 = :val4)' projectionExpression=''");
      assertEquals(res.data().length(), 1);
   }

   @Test
   public void testK() throws Exception
   {
      Engine engine = service();
      Response res = null;

      res = engine.get("northwind/dynamodb/orders?gt(OrderId, 1)&eq(type, ORDER)").assertOk();
      res.assertDebug("DynamoDb  QuerySpec:'gs3' maxPageSize=100 scanIndexForward=true nameMap={#var1=sk, #var2=hk} valueMap={:val1=ORDER, :val2=1} keyConditionExpression='(#var1 = :val1) and (#var2 > :val2)' filterExpression='' projectionExpression=''");
   }

   @Test
   public void testAA() throws Exception
   {
      Engine engine = service();
      Response res = null;

      res = engine.get("northwind/dynamodb/orders?eq(type, ORDER)&or(eq(shipname, 'Blauer See Delikatessen'),eq(customerid,HILAA))");
      res.assertDebug("DynamoDb", "QuerySpec:'gs3' maxPageSize=100 scanIndexForward=true nameMap={#var1=sk, #var2=ls2, #var3=customerid} valueMap={:val1=ORDER, :val2=Blauer See Delikatessen, :val3=HILAA} keyConditionExpression='(#var1 = :val1)' filterExpression='((#var2 = :val2) or (#var3 = :val3))' projectionExpression=''");
      res.assertOk();
   }
}
