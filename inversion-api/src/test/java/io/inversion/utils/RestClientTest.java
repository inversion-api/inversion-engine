package io.inversion.utils;

import io.inversion.*;
import io.inversion.utils.RestClient.FutureResponse;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RestClientTest {

    @Test
    public void testFluency()
    {
        RestClient client = new RestClient() {
            protected Response doRequest(Request request) {
                String url = request.getUrl().toString();
                Response response = new Response().withUrl(url);
                if(url.contains("error"))
                    response.withStatus(Status.SC_500_INTERNAL_SERVER_ERROR);
                return response;
            }
        };

        client.onRequest(request -> {System.out.println("client.onRequest"); return null;});
        client.onRequest(response -> {System.out.println("client.onResponse"); return null;});

        client.get("/something/error")//
                .onSuccess(response -> System.out.println("response.onSuccess1"))
                .onFailure(response -> System.out.println("response.onFailure1"))
                .onResponse(response -> System.out.println("response.onResponse1"));


        client.get("/something/success")//
                .onSuccess(response -> System.out.println("response.onSuccess2"))
                .onFailure(response -> System.out.println("response.onFailure2"))
                .onResponse(response -> System.out.println("response.onResponse2"));
    }


    @Test
    public void buildUrl_variable_replaced() {

        RestClient client = new RestClient().withUrl("http://somehost/:_collection/[:_resource]/[:_relationship]");

//        try {
//            Chain.push(null, new Request("GET", "http://myservice?tenant=12345&_collection=books"), null);
//            String url = client.buildUrl(null);
//            System.out.println(url);
//            assertEquals("http://somehost/books", url);
//        } finally {
//            Chain.pop();
//        }

        try {
            Chain.push(null, new Request("GET", "http://myservice?tenant=12345&_collection=books&_resource=12345"), null);
            String url = client.buildUrl(null);
            System.out.println(url);
            assertEquals("http://somehost/books/12345", url);
        } finally {
            Chain.pop();
        }
    }

    //   @Test
    //   public void test_retries_dont_retry_on_404()
    //   {
    //      final int[] statusCode = new int[]{404};
    //      RestClient client = new RestClient()
    //         {
    //            @Override
    //            protected Response doRequest(Request request)
    //            {
    //               Response resp = new Response();
    //               resp.withStatusCode(statusCode[0]);
    //
    //               return resp;
    //            }
    //         };
    //      FutureResponse resp = client.call("GET", "url", null, null, 5, null);
    //      resp.get();
    //      assertEquals(0, resp.request.getRetryCount());
    //
    //      statusCode[0] = 500;
    //      resp = client.call("GET", "url", null, null, 5, null);
    //      resp.get();
    //      assertEquals(5, resp.request.getRetryCount());
    //
    //   }

    @Test
    public void testBuildFuture_includeHeaders_applied() {
        RestClient client = new RestClient() {
            protected Response getResponse(FutureResponse future) {
                return new Response(null);
                //intentional do nothing
            }
        }.withUrl("http://somehost")//
                .withForwardedHeaders(true)//
                .withIncludeForwardHeaders("header1", "header2", "HEADER3", "Header4");

        Engine  engine         = new Engine();
        Request inboundRequest = new Request("GET", "http://localhost:8080/path?param1=a&param2=b", null, Utils.addToMap(new ArrayListValuedHashMap(), "header1", "header1Val", "header2", "header2Val", "header3", "header3Val", "headerX", "headerXVal"));
        Chain.push(engine, inboundRequest, new Response());
        try {
            client.withForcedHeader("header3", "forcedHeader3Val");
            FutureResponse resp = client.call("GET", "somepath", null, null, Utils.addToMap(new ArrayListValuedHashMap(), "header2", "header2ChildRequestVal", "header3", "header3ChildRequestVal", "header4", "header4ChildRequestVal"));

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

        } finally {
            Chain.pop();
        }
    }

    @Test
    public void testBuildFuture_includeParams_applied() {
        RestClient client = new RestClient() {
            protected void doCall(FutureResponse future) {
                //intentional do nothing
            }
        }.withUrl("http://somehost")//
                .withForwardedParams(true)//
                .withIncludeParams("param0", "param1", "param2", "param3", "param4");

        Engine  engine         = new Engine();
        Request inboundRequest = new Request("GET", "http://localhost:8080/path?param1=param1val&param2=param2Val");

        Chain.push(engine, inboundRequest, new Response());
        try {
            FutureResponse      resp        = client.get("somepath?param0=param0Val", "param1", "param1Override", "param3", "param3Val");
            Map<String, String> finalParams = resp.getRequest().getUrl().getParams();

            assertEquals("param0Val", finalParams.get("param0"));
            assertEquals("param1Override", finalParams.get("param1"));
            assertEquals("param2Val", finalParams.get("param2"));
            assertEquals("param3Val", finalParams.get("param3"));

            resp = client.get("somepath?param0=param0Val&param1=param1Override&param3=param3Val");
            finalParams = resp.getRequest().getUrl().getParams();

            assertEquals("param0Val", finalParams.get("param0"));
            assertEquals("param1Override", finalParams.get("param1"));
            assertEquals("param2Val", finalParams.get("param2"));
            assertEquals("param3Val", finalParams.get("param3"));

        } finally {
            Chain.pop();
        }
    }

}
