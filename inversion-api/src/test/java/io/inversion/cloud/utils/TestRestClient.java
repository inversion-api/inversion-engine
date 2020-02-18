
package io.inversion.cloud.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.ConnectException;
import java.util.Map;

import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.junit.jupiter.api.Test;

import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.service.Engine;

public class TestRestClient
{

   @Test
   public void test_retriesMax()
   {
      final int[] tries = new int[]{0};
      RestClient client = new RestClient()
         {
            protected Response getResponse(FutureResponse future)
            {
               tries[0] += 1;
               Response resp = new Response();
               if (future.getRetryCount() >= 4)
               {
                  //simulates an HTTP response that was initially successful
                  //but then threw an exception on content download
                  resp.withStatusCode(200);
                  resp.withError(new ConnectException());
               }
               else
               {
                  resp.withStatusCode(500);
               }
               return resp;
            }
         }.withTotalRetryMax(20);

      FutureResponse fut = client.call("GET", "url", null, null, 5, null);
      fut.get();
      assertEquals(21, tries[0]);
      assertEquals(20, fut.getTotalRetries());
   }

   @Test
   public void test_retries_dont_retry_on_404()
   {
      final int[] statusCode = new int[]{404};
      RestClient client = new RestClient()
         {
            protected Response getResponse(FutureResponse future)
            {
               Response resp = new Response();
               resp.withStatusCode(statusCode[0]);

               return resp;
            }
         };
      FutureResponse resp = client.call("GET", "url", null, null, 5, null);
      resp.get();
      assertEquals(0, resp.getRetryCount());

      statusCode[0] = 500;
      resp = client.call("GET", "url", null, null, 5, null);
      resp.get();
      assertEquals(5, resp.getRetryCount());

   }

   @Test
   public void test_withRemoteAsync()
   {
      final Thread[] thread = new Thread[1];

      RestClient client = new RestClient()
         {
            protected Response getResponse(FutureResponse future)
            {
               thread[0] = Thread.currentThread();
               return new Response(null);
            }
         }.withUrl("http://somehost")//
          .withRemoteAsync(false);

      client.withRemoteAsync(false);
      client.get("subpath").get();
      assertTrue(Thread.currentThread() == thread[0]);

      client.withRemoteAsync(true);
      client.get("subpath").get();
      assertTrue(Thread.currentThread() != thread[0]);
   }

   @Test
   public void testBuildFuture_includeHeaders_applied()
   {
      RestClient client = new RestClient()
         {
            protected Response getResponse(FutureResponse future)
            {
               return new Response(null);
               //intentional do nothing
            }
         }.withUrl("http://somehost")//
          .withForwardedHeaders(true)//
          .withWhitelistedHeaders("header1", "header2", "HEADER3", "Header4");

      Engine engine = new Engine();
      Request inboundRequest = new Request("GET", "http://localhost:8080/path?param1=a&param2=b", null, Utils.addToMap(new ArrayListValuedHashMap(), "header1", "header1Val", "header2", "header2Val", "header3", "header3Val", "headerX", "headerXVal"), -1);
      Response response = new Response();

      Chain.push(engine, inboundRequest, new Response());
      try
      {
         client.withForcedHeader("header3", "forcedHeader3Val");
         FutureResponse resp = client.call("GET", "somepath", null, null, -1, Utils.addToMap(new ArrayListValuedHashMap(), "header2", "header2ChildRequestVal", "header3", "header3ChildRequestVal", "header4", "header4ChildRequestVal"));

         //resp.get

         ArrayListValuedHashMap<String, String> finalHeaders = resp.getRequest().getHeaders();

         assertEquals(1, finalHeaders.get("header1").size());
         assertEquals("header1Val", finalHeaders.get("header1").get(0));

         assertEquals(1, finalHeaders.get("header2").size());
         assertEquals("header2ChildRequestVal", finalHeaders.get("header2").get(0));

         assertEquals(1, finalHeaders.get("header3").size());
         assertEquals("forcedHeader3Val", finalHeaders.get("header3").get(0));

         assertEquals(1, finalHeaders.get("header4").size());
         assertEquals("header4ChildRequestVal", finalHeaders.get("header4").get(0));
         
         assertEquals(0, finalHeaders.get("headerX").size());

      }
      finally
      {
         Chain.pop();
      }
   }

   @Test
   public void testBuildFuture_includeParams_applied()
   {
      RestClient client = new RestClient()
         {
            protected void doCall(FutureResponse future)
            {
               //intentional do nothing
            }
         }.withUrl("http://somehost")//
          .withForwardedParams(true)//
          .withWhitelistedParams("param0", "param1", "param2", "param3", "param4");

      Engine engine = new Engine();
      Request inboundRequest = new Request("GET", "http://localhost:8080/path?param1=param1val&param2=param2Val");
      Response response = new Response();

      Chain.push(engine, inboundRequest, new Response());
      try
      {
         FutureResponse resp = client.get("somepath?param0=param0Val", "param1", "param1Override", "param3", "param3Val");
         Map<String, String> finalParams = resp.getRequest().getParams();

         assertEquals("param0Val", finalParams.get("param0"));
         assertEquals("param1Override", finalParams.get("param1"));
         assertEquals("param2Val", finalParams.get("param2"));
         assertEquals("param3Val", finalParams.get("param3"));

         resp = client.get("somepath?param0=param0Val&param1=param1Override&param3=param3Val");
         finalParams = resp.getRequest().getParams();

         assertEquals("param0Val", finalParams.get("param0"));
         assertEquals("param1Override", finalParams.get("param1"));
         assertEquals("param2Val", finalParams.get("param2"));
         assertEquals("param3Val", finalParams.get("param3"));

      }
      finally
      {
         Chain.pop();
      }
   }

}
