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
package io.inversion.cloud.service;

import org.junit.Test;

import io.inversion.cloud.action.misc.MockAction;
import io.inversion.cloud.model.Action;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.Endpoint;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Path;
import io.inversion.cloud.model.Response;
import junit.framework.TestCase;

public class TestEngine extends TestCase
{

   @Test
   public void testEndpointMatching1()
   {
      Endpoint ep = null;

      ep = new Endpoint("GET", "actionA*", new MockActionA("GET", "*"));
      assertEquals("actionA*", ep.getIncludePaths().get(0).toString());
      assertTrue(ep.matches("GET", "actionA"));
      assertFalse(ep.matches("PUT", "actionA"));
      assertFalse(ep.matches("GET", "actionB"));
      assertTrue(ep.matches("GET", "actionAasdf"));
      assertTrue(ep.matches("GET", "actionA/asdasdf"));

      ep = new Endpoint("GET", "actionA", new MockActionA("GET", "*"));
      assertEquals("actionA", ep.getPath().toString());
      assertEquals(0, ep.getIncludePaths().size());
      assertTrue(ep.matches("GET", "actionA"));
      assertTrue(ep.matches("GET", "actionA/asdf"));

      ep = new Endpoint("GET", "actionA/*", new MockActionA("GET", "*"));
      assertEquals("actionA", ep.getPath().toString());
      assertEquals("*", ep.getIncludePaths().get(0).toString());
      assertTrue(ep.matches("GET", "actionA/asdf"));

      ep = new Endpoint("GET", "actionA/asdf*", new MockActionA("GET", "*"));
      assertEquals("actionA", ep.getPath().toString());
      assertEquals("asdf*", ep.getIncludePaths().get(0).toString());
      assertTrue(ep.matches("GET", "actionA/asdf"));
      assertFalse(ep.matches("GET", "actionA/zxcvasdf"));

      ep = new Endpoint("GET", "actionA", new MockActionA("GET", "*")).withExcludePaths("*");
      assertEquals("actionA", ep.getPath().toString());
      assertTrue(ep.matches("GET", "actionA/"));
      assertTrue(ep.matches("GET", "actionA"));
      assertFalse(ep.matches("GET", "actionA/zxcvasdf"));

      ep = new Endpoint("GET", "actionA", new MockActionA("GET", "*")).withExcludePaths("asdf*");
      assertEquals("actionA", ep.getPath().toString());
      assertEquals("asdf*", ep.getExcludePaths().get(0).toString());
      assertFalse(ep.matches("GET", "actionA/asdf"));
      assertTrue(ep.matches("GET", "actionA/zxcvasdf"));

      ep = new Endpoint("GET", "actionA/*", new MockActionA("GET", "*")).withExcludePaths("bbb*,ccc*,ddd,aaa");
      assertEquals("actionA", ep.getPath().toString());
      assertEquals("*", ep.getIncludePaths().get(0).toString());
      assertEquals("bbb*", ep.getExcludePaths().get(0).toString());
      assertEquals("ccc*", ep.getExcludePaths().get(1).toString());
      assertEquals("ddd", ep.getExcludePaths().get(2).toString());
      assertEquals("aaa", ep.getExcludePaths().get(3).toString());
      assertFalse(ep.matches("GET", "actionA/aaa"));
      assertTrue(ep.matches("GET", "actionA/aaaxyz"));
      assertFalse(ep.matches("GET", "actionA/ddd"));
      assertTrue(ep.matches("GET", "actionA/dddxyz"));

      assertFalse(ep.matches("GET", "actionA/bbb"));
      assertFalse(ep.matches("GET", "actionA/bbbsdfbshtgzdsfg"));
      assertFalse(ep.matches("GET", "actionA/ccc"));
      assertFalse(ep.matches("GET", "actionA/cccccccasdfasdf"));
      assertTrue(ep.matches("GET", "actionA/dddddd"));

      ep = new Endpoint("GET", "actionA/aaa*", new MockActionA("GET", "*")).withExcludePaths("bbb*,ccc*,ddd,aaa");
      assertEquals("actionA", ep.getPath().toString());
      assertEquals("aaa*", ep.getIncludePaths().get(0).toString());
      assertEquals("bbb*", ep.getExcludePaths().get(0).toString());
      assertEquals("ccc*", ep.getExcludePaths().get(1).toString());
      assertEquals("ddd", ep.getExcludePaths().get(2).toString());
      assertEquals("aaa", ep.getExcludePaths().get(3).toString());
      assertFalse(ep.matches("GET", "actionA/aaa"));
      assertTrue(ep.matches("GET", "actionA/aaaxyz"));
      assertFalse(ep.matches("GET", "actionA/"));//not allowed or denied so denied
      assertFalse(ep.matches("GET", "actionA/abcd"));//not allowed or denied so denied
      assertFalse(ep.matches("GET", "actionA/bb"));//not allowed or denied so denied
      assertFalse(ep.matches("GET", "actionA/ccc"));//specifically denied
   }

   @Test
   public void testEndpointMatching2()
   {
      Engine engine = null;

      engine = new Engine()//
                           .withApi(new Api("northwind")//
                           .withEndpoint(null, "source/*", new MockAction("sourceAction"))//
                           .withEndpoint(null, "h2/*", new MockAction("h2Action"))//
                           .withEndpoint(null, "mysql/*", new MockAction("mysqlAction"))//
                           .withEndpoint(null, "dynamo/*", new MockAction("dynamoAction")));
                           

      engine.get("northwind/source/collection").assertDebug("Action:", "sourceAction");
      engine.get("northwind/source").assertDebug("Action:", "sourceAction");
      engine.get("northwind/h2/collection").assertDebug("Actio:n", "h2Action");
      engine.get("northwind/mysql/collection/entity/subcollection").assertDebug("Action:", "mysqlAction");
      engine.get("northwind/dynamo/collection").assertDebug("Action:", "dynamoAction");
   }

   @Test
   public void test1()
   {
      Engine engine = null;

      engine = new Engine()//
                           .withApi(new Api()//
                                             .withEndpoint("get", "/*")//
                                             .withAction(new MockActionA())//
                                             .withDb(new MockDb()));

      Response resp = engine.get("users");
      resp.dump();
      assertEquals(200, resp.getStatusCode());
      assertEquals("tester1", resp.find("data.0.firstName"));

      //action is placed on the endpoint instead of the api
      engine = new Engine()//
                           .withApi(new Api()//
                                             .withEndpoint("get", "/*", new MockActionA())//
                                             .withDb(new MockDb()));

      resp = engine.get("users");
      assertEquals("tester1", resp.find("data.0.firstName"));

      engine = new Engine()//
                           .withApi(new Api("testApi")//
                                                      .withEndpoint("get", "*", new MockActionA())//
                                                      .withDb(new MockDb()));

      resp = engine.get("users");
      assertEquals(404, resp.getStatusCode());

      resp = engine.get("testApi/users");
      assertEquals(200, resp.getStatusCode());
      assertEquals("tester1", resp.find("data.0.firstName"));

      assertEquals(200, engine.get("/testApi/users").getStatusCode());
      assertEquals(200, engine.get("http://localhost/testApi/users").getStatusCode());
      assertEquals(200, engine.get("http://whateverhost:12345/testApi/users").getStatusCode());
   }

   @Test
   public void test2()
   {
      Engine engine = null;

      engine = new Engine()//
                           .withApi(new Api()//
                                             .withEndpoint("get", "actionA/*", new MockActionA("get", "*"))//
                                             .withEndpoint("get", "actionB/*", new MockActionB("get", "*")));

      Response resp = null;
      JSNode data = null;

      resp = engine.get("/actionA/helloworld");
      data = resp.getJson();
      assertEquals("MockActionA", data.find("data.0.className"));

      resp = engine.get("/actionB/hellomoon");
      data = resp.getJson();
      assertEquals("MockActionB", data.find("data.0.className"));

   }

   @Test
   public void testSlashCorrection()
   {
      assertEquals("a/b", new Path("/a////b/////").toString());
   }

   @Test
   public void testSimpleEndpoint2()
   {
      Engine engine = null;

      engine = new Engine()//
                           .withApi(new Api()//
                                             .withEndpoint("GET", "actionA/*", new MockActionA("GET", "*")));

      Response resp = null;
      resp = engine.get("/actionA");
      assertEquals(200, resp.getStatusCode());

      resp = engine.get("actionA");
      assertEquals(200, resp.getStatusCode());

   }

   /**
    * ...are configed params set to query?  How
    */
   @Test
   public void testConfigParamOverrides()
   {
      //includes/excludes, expands, requires/restricts
      Endpoint ep = new Endpoint("GET", "/", "*").withConfig("endpointParam=endpointValue&overriddenParam=endpointValue");
      Action actionA = new MockAction()
         {
            public void run(Engine engine, io.inversion.cloud.model.Api api, Endpoint endpoint, Chain chain, io.inversion.cloud.model.Request req, Response res) throws Exception
            {
               Chain.debug("Endpoint_actionA_overriddenParam " + chain.getConfig("overriddenParam"));
               Chain.debug("Endpoint_actionA_endpointParam " + chain.getConfig("endpointParam"));
               Chain.debug("Endpoint_actionA_actionAParam " + chain.getConfig("actionAParam"));
               Chain.debug("Endpoint_actionA_actionBParam " + chain.getConfig("actionBParam"));
               Chain.debug("Endpoint_actionA_actionCParam " + chain.getConfig("actionCParam"));
            }
         }.withConfig("overriddenParam=actionAOverride&actionAParam=actionAValue");

      Action actionB = new MockAction()
         {
            public void run(Engine engine, io.inversion.cloud.model.Api api, Endpoint endpoint, Chain chain, io.inversion.cloud.model.Request req, Response res) throws Exception
            {
               Chain.debug("Endpoint_actionB_overriddenParam " + chain.getConfig("overriddenParam"));
               Chain.debug("Endpoint_actionB_endpointParam " + chain.getConfig("endpointParam"));
               Chain.debug("Endpoint_actionB_actionAParam " + chain.getConfig("actionAParam"));
               Chain.debug("Endpoint_actionB_actionBParam " + chain.getConfig("actionBParam"));
               Chain.debug("Endpoint_actionB_actionCParam " + chain.getConfig("actionCParam"));

            }
         }.withConfig("overriddenParam=actionBOverride&actionBParam=actionBValue");

      Action actionC = new MockAction()
         {
            public void run(Engine engine, io.inversion.cloud.model.Api api, Endpoint endpoint, Chain chain, io.inversion.cloud.model.Request req, Response res) throws Exception
            {
               Chain.debug("Endpoint_actionC_overriddenParam " + chain.getConfig("overriddenParam"));
               Chain.debug("Endpoint_actionC_endpointParam " + chain.getConfig("endpointParam"));
               Chain.debug("Endpoint_actionC_actionAParam " + chain.getConfig("actionAParam"));
               Chain.debug("Endpoint_actionC_actionBParam " + chain.getConfig("actionBParam"));
               Chain.debug("Endpoint_actionC_actionCParam " + chain.getConfig("actionCParam"));

            }
         }.withConfig("overriddenParam=actionCOverride&actionCParam=actionCValue");

      ep.withAction(actionA);
      ep.withAction(actionB);
      ep.withAction(actionC);

      Engine engine = new Engine()//
                                  .withApi(new Api("test")//
                                                          .withEndpoint(ep));

      Response res = engine.get("test/test");
      res.dump();

      res.assertDebug("Endpoint_actionA_overriddenParam", "actionAOverride");
      res.assertDebug("Endpoint_actionA_endpointParam", "endpointValue");
      res.assertDebug("Endpoint_actionA_actionAParam", "actionAValue");
      res.assertDebug("Endpoint_actionA_actionBParam", "null");
      res.assertDebug("Endpoint_actionA_actionCParam", "null");

      res.assertDebug("Endpoint_actionB_overriddenParam", "actionBOverride");
      res.assertDebug("Endpoint_actionB_endpointParam", "endpointValue");
      res.assertDebug("Endpoint_actionB_actionAParam", "actionAValue");
      res.assertDebug("Endpoint_actionB_actionBParam", "actionBValue");
      res.assertDebug("Endpoint_actionB_actionCParam", "null");

      res.assertDebug("Endpoint_actionC_overriddenParam", "actionCOverride");
      res.assertDebug("Endpoint_actionC_endpointParam", "endpointValue");
      res.assertDebug("Endpoint_actionC_actionAParam", "actionAValue");
      res.assertDebug("Endpoint_actionC_actionBParam", "actionBValue");
      res.assertDebug("Endpoint_actionC_actionCParam", "actionCValue");

   }

}
