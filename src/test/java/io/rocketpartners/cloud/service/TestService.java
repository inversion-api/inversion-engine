package io.rocketpartners.cloud.service;

import org.junit.Test;

import io.rocketpartners.cloud.model.Node;
import junit.framework.TestCase;

public class TestService extends TestCase
{
//   public static void main(String[] args)
//   {
//      TestService tests = new TestService();
//      tests.test1();
//   }

//   @Test
//   public void test1()
//   {
//      Service service = null;
//
//      service = new Service()//
//                             .withApi((String) null)//
//                             .withEndpoint("get", "*").getApi()//
//                             .withAction(new MockActionA(), "get", "*").getApi()//
//                             .withDb(new MockDb()).getApi().getService();
//
//      Response resp = service.get("users");
//      assertEquals(200, resp.getStatusCode());
//      assertEquals(1, resp.getPageSize());
//
//      //action is placed on the endpoint instead of the api
//      service = new Service()//
//                             .withApi((String) null)//
//                             .withEndpoint("get", "*").withAction(new MockActionA(), "get", "*").getApi()//
//                             .withDb(new MockDb()).getApi().getService();
//
//      resp = service.get("users");
//      assertEquals(200, resp.getStatusCode());
//      assertEquals(1, resp.getPageSize());
//
//      service = new Service()//
//                             .withApi("testApi")//
//                             .withEndpoint("get", "*").getApi()//
//                             .withAction(new MockActionA(), "get", "*").getApi()//
//                             .withDb(new MockDb()).getApi().getService();
//
//      resp = service.get("users");
//      assertEquals(404, resp.getStatusCode());
//
//      resp = service.get("testApi/users");
//      assertEquals(200, resp.getStatusCode());
//      assertEquals(1, resp.getPageSize());
//
//      assertEquals(200, service.get("/testApi/users").getStatusCode());
//      assertEquals(200, service.get("http://localhost/testApi/users").getStatusCode());
//      assertEquals(200, service.get("http://whateverhost:12345/testApi/users").getStatusCode());
//   }

   @Test
   public void test2()
   {
      Service service = null;

      service = new Service()//
                             .withApi((String) null)//
                             .withEndpoint("get", "actionA/*").withAction(new MockActionA(), "get", "*").getApi()//
                             .withEndpoint("get", "actionB/*").withAction(new MockActionB(), "get", "*").getApi()//
                             .getService();

      Response resp = null;
      Node data = null;

      resp = service.get("/actionA/helloworld");
      data = resp.getJson();
      assertEquals("MockActionA", data.find("data.0.className"));

      resp = service.get("/actionB/hellomoon");
      data = resp.getJson();
      assertEquals("MockActionB", data.find("data.0.className"));

   }

}
