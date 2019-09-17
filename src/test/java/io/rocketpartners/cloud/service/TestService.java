package io.rocketpartners.cloud.service;

import org.junit.Test;

import io.rocketpartners.cloud.action.misc.StatusAction;
import io.rocketpartners.cloud.model.Action;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.model.ObjectNode;
import io.rocketpartners.cloud.model.Path;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.utils.Utils;
import junit.framework.TestCase;

public class TestService extends TestCase
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
      Service service = null;

      service = new Service()//
                             .withApi("northwind")//
                             .withEndpoint(null, "source/*", new MockAction("sourceAction"))//
                             .withEndpoint(null, "h2/*", new MockAction("h2Action"))//
                             .withEndpoint(null, "mysql/*", new MockAction("mysqlAction"))//
                             .withEndpoint(null, "dynamo/*", new MockAction("dynamoAction"))//
                             .getService();

      Utils.assertDebug(service.get("northwind/source/collection"), "Action", "sourceAction");
      Utils.assertDebug(service.get("northwind/source"), "Action", "sourceAction");
      Utils.assertDebug(service.get("northwind/h2/collection"), "Action", "h2Action");
      Utils.assertDebug(service.get("northwind/mysql/collection/entity/subcollection"), "Action", "mysqlAction");
      Utils.assertDebug(service.get("northwind/dynamo/collection"), "Action", "dynamoAction");
   }

   @Test
   public void test1()
   {
      Service service = null;

      service = new Service()//
                             .withApi((String) null)//
                             .withEndpoint("get", "/*")//
                             .withAction(new MockActionA())//
                             .withDb(new MockDb()).getService();

      Response resp = service.get("users");
      resp.dump();
      assertEquals(200, resp.getStatusCode());
      assertEquals("tester1", resp.find("data.0.firstName"));

      //action is placed on the endpoint instead of the api
      service = new Service()//
                             .withApi((String) null)//
                             .withEndpoint("get", "/*", new MockActionA())//
                             .withDb(new MockDb()).getService();

      resp = service.get("users");
      assertEquals("tester1", resp.find("data.0.firstName"));

      service = new Service()//
                             .withApi("testApi")//
                             .withEndpoint("get", "*", new MockActionA())//
                             .withDb(new MockDb()).getService();

      resp = service.get("users");
      assertEquals(404, resp.getStatusCode());

      resp = service.get("testApi/users");
      assertEquals(200, resp.getStatusCode());
      assertEquals("tester1", resp.find("data.0.firstName"));

      assertEquals(200, service.get("/testApi/users").getStatusCode());
      assertEquals(200, service.get("http://localhost/testApi/users").getStatusCode());
      assertEquals(200, service.get("http://whateverhost:12345/testApi/users").getStatusCode());
   }

   @Test
   public void test2()
   {
      Service service = null;

      service = new Service()//
                             .withApi((String) null)//
                             .withEndpoint("get", "actionA/*", new MockActionA("get", "*"))//
                             .withEndpoint("get", "actionB/*", new MockActionB("get", "*"))//
                             .getService();

      Response resp = null;
      ObjectNode data = null;

      resp = service.get("/actionA/helloworld");
      data = resp.getJson();
      assertEquals("MockActionA", data.find("data.0.className"));

      resp = service.get("/actionB/hellomoon");
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
      Service service = null;

      service = new Service()//
                             .withApi((String) null)//
                             .withEndpoint("GET", "actionA/*", new MockActionA("GET", "*"))//
                             .getService();

      Response resp = null;
      resp = service.get("/actionA");
      assertEquals(200, resp.getStatusCode());

      resp = service.get("actionA");
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
      Action actionA = new StatusAction()
         {
            public void run(Service service, io.rocketpartners.cloud.model.Api api, Endpoint endpoint, io.rocketpartners.cloud.service.Chain chain, io.rocketpartners.cloud.model.Request req, Response res) throws Exception
            {
               Chain.debug("Endpoint_actionA_overriddenParam " + chain.getConfig("overriddenParam"));
               Chain.debug("Endpoint_actionA_endpointParam " + chain.getConfig("endpointParam"));
               Chain.debug("Endpoint_actionA_actionAParam " + chain.getConfig("actionAParam"));
               Chain.debug("Endpoint_actionA_actionBParam " + chain.getConfig("actionBParam"));
               Chain.debug("Endpoint_actionA_actionCParam " + chain.getConfig("actionCParam"));
            }
         }.withConfig("overriddenParam=actionAOverride&actionAParam=actionAValue");

      Action actionB = new StatusAction()
         {
            public void run(Service service, io.rocketpartners.cloud.model.Api api, Endpoint endpoint, io.rocketpartners.cloud.service.Chain chain, io.rocketpartners.cloud.model.Request req, Response res) throws Exception
            {
               Chain.debug("Endpoint_actionB_overriddenParam " + chain.getConfig("overriddenParam"));
               Chain.debug("Endpoint_actionB_endpointParam " + chain.getConfig("endpointParam"));
               Chain.debug("Endpoint_actionB_actionAParam " + chain.getConfig("actionAParam"));
               Chain.debug("Endpoint_actionB_actionBParam " + chain.getConfig("actionBParam"));
               Chain.debug("Endpoint_actionB_actionCParam " + chain.getConfig("actionCParam"));

            }
         }.withConfig("overriddenParam=actionBOverride&actionBParam=actionBValue");

      Action actionC = new StatusAction()
         {
            public void run(Service service, io.rocketpartners.cloud.model.Api api, Endpoint endpoint, io.rocketpartners.cloud.service.Chain chain, io.rocketpartners.cloud.model.Request req, Response res) throws Exception
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

      Service service = new Service()//
                                     .withApi("test")//
                                     .withEndpoint(ep)//
                                     .getService();

      Response res = service.get("test/test");
      res.dump();

      Utils.assertDebug(res, "Endpoint_actionA_overriddenParam", "actionAOverride");
      Utils.assertDebug(res, "Endpoint_actionA_endpointParam", "endpointValue");
      Utils.assertDebug(res, "Endpoint_actionA_actionAParam", "actionAValue");
      Utils.assertDebug(res, "Endpoint_actionA_actionBParam", "null");
      Utils.assertDebug(res, "Endpoint_actionA_actionCParam", "null");

      Utils.assertDebug(res, "Endpoint_actionB_overriddenParam", "actionBOverride");
      Utils.assertDebug(res, "Endpoint_actionB_endpointParam", "endpointValue");
      Utils.assertDebug(res, "Endpoint_actionB_actionAParam", "actionAValue");
      Utils.assertDebug(res, "Endpoint_actionB_actionBParam", "actionBValue");
      Utils.assertDebug(res, "Endpoint_actionB_actionCParam", "null");

      Utils.assertDebug(res, "Endpoint_actionC_overriddenParam", "actionCOverride");
      Utils.assertDebug(res, "Endpoint_actionC_endpointParam", "endpointValue");
      Utils.assertDebug(res, "Endpoint_actionC_actionAParam", "actionAValue");
      Utils.assertDebug(res, "Endpoint_actionC_actionBParam", "actionBValue");
      Utils.assertDebug(res, "Endpoint_actionC_actionCParam", "actionCValue");

   }

}
