
package io.inversion.cloud.utils;

import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.junit.Test;

import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.service.Engine;
import junit.framework.TestCase;

public class TestRestClient extends TestCase
{
   @Test
   public void testBuildFuture_includeHeaders_applied()
   {
      RestClient client = new RestClient()
         {
            protected void doCall(FutureResponse future)
            {
               //intentional do nothing
            }
         }.withUrl("http://somehost")//
          .withForwardedHeaders(true);

      Engine engine = new Engine();
      Request inboundRequest = new Request("GET", "http://localhost:8080/path?param1=a&param2=b", null, RestClient.asHeaderMap("header1", "header1Val", "header2", "header2Val", "header3", "header3Val"), -1);
      Response response = new Response();

      Chain.push(engine, inboundRequest, new Response());
      try
      {
         client.withForcedHeader("header3", "forcedHeader3Val");
         FutureResponse resp = client.call("GET", "somepath", null, null, -1, RestClient.asHeaderMap("header2", "header2ChildRequestVal", "header3", "header3ChildRequestVal", "header4", "header4ChildRequestVal"));

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

      }
      finally
      {
         Chain.pop();
      }
   }

}
