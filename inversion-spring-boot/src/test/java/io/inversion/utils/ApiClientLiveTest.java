package io.inversion.utils;

import io.inversion.*;
import io.inversion.json.JSMap;
import io.inversion.json.JSNode;
import io.inversion.json.JSParser;
import io.inversion.spring.main.InversionMain;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ApiClientLiveTest {

    @Test
    public void testConcurrentConnections() throws Exception {

        final int[] connections = new int[]{0};
        final int[] attempts = new int[]{0};

        InversionMain.run(new Api("testme").withEndpoint(new Action() {

            public void run(Request req, Response res) throws ApiException {

                synchronized (connections){
                    connections[0] +=1;
                    System.out.println("Connected: " + connections[0]);
                }

                Utils.sleep(30000);
                }
        }));

        ApiClient client = new ApiClient();

        List<Future<Response>> futures = new ArrayList<>();
        for(int i=0; i<100; i++){
            new Thread(() -> {
                synchronized (attempts){
                    attempts[0] += 1;
                    System.out.println("Calling: " + (attempts[0]));
                }

                futures.add(client.get("http://localhost:8080/testme"));
            }).start();
        }

        Utils.sleep(3000);
        Utils.sleep(100000);
    }

    @Test
    public void testGzip() throws Exception {

        final JSNode  json = new JSMap();
        StringBuilder buff = new StringBuilder();
        for(int i=0; i<5000; i++){
            buff.append("x");
        }
        json.put("x", buff.toString());

        InversionMain.run(new Api("testme").withEndpoint(new Action() {

            public void run(Request req, Response res) throws ApiException {
                res.withJson(json);
            }
        }));

        ApiClient client = new ApiClient();
        Response  res    = client.get("http://localhost:8080/testme").get();

        assertTrue(json.toString().equals(res.getJson().toString()));
    }


    @Test
    public void testConnectionReuse() throws Exception {

        final int[] connections = new int[]{0};
        final int[] attempts = new int[]{0};

        InversionMain.run(new Api("testme").withEndpoint(new Action() {

            public void run(Request req, Response res) throws ApiException {

                synchronized (connections){
                    connections[0] +=1;
                    System.out.println("Connected: " + connections[0]);
                }

                Utils.sleep(2000);
            }
        }));

        ApiClient client = new ApiClient();

        List<Future<Response>> futures = new ArrayList<>();
        for(int i=0; i<2; i++){
            client.get("http://localhost:8080/testme");
            client.get("http://localhost:8080/testme");
            Thread.sleep(5000);
        }

        Thread.sleep(10000);
    }


    @Test
    public void testHeaders() throws Exception {

        final int[] connections = new int[]{0};
        final int[] attempts = new int[]{0};

        InversionMain.run(new Api("testme").withEndpoint(new Action() {

            public void run(Request req, Response res) throws ApiException {

                synchronized (connections){
                    connections[0] +=1;
                    System.out.println("Connected: " + connections[0]);
                }

                String sleep = req.getUrl().getParam("sleep");
                if(sleep != null)
                    Utils.sleep(Integer.parseInt(sleep));
            }
        }));

        ApiClient client = new ApiClient();

        client.get("http://localhost:8080/testme").get();

        Utils.sleep(5);
    }

    @Test
    public void testEvictIdleConnections() throws Exception {

        final int[] connections = new int[]{0};
        final int[] attempts = new int[]{0};

        InversionMain.run(new Api("testme").withEndpoint(new Action() {

            public void run(Request req, Response res) throws ApiException {

                synchronized (connections){
                    connections[0] +=1;
                    System.out.println("Connected: " + connections[0]);
                }

                String sleep = req.getUrl().getParam("sleep");
                if(sleep != null)
                    Utils.sleep(Integer.parseInt(sleep));
            }
        }));

        ApiClient client = new ApiClient();

        client.get("http://localhost:8080/testme");
        Utils.sleep(5000);
        client.get("http://localhost:8080/testme");
        Utils.sleep(5000);
        client.get("http://localhost:8080/testme");
        Utils.sleep(5000);
        client.get("http://localhost:8080/testme");

        Utils.sleep(500000);
    }



    @Test
    public void doRequest0_gzip_requests_are_encoded_correctly() throws Exception {

        InversionMain.run(new Api().withEndpoint(new Action() {

            public void run(Request req, Response res) throws ApiException {

                if (!"yep".equals(req.getJson().find("testing")))
                    throw ApiException.new500InternalServerError();

                res.withJson(req.getJson());
            }
        }));

        ApiClient client = new ApiClient();
        Response  resp   = client.post("http://localhost:8080/testme", new JSMap("testing", "yep")).get();
        resp.assertOk();
        assertEquals(resp.find("testing"), "yep");
    }

    @Test
    public void doRequest0_gzip_responses_are_decoded_correctly() throws Exception {

        InversionMain.run(new Api().withEndpoint(new Action() {

            public void run(Request req, Response res) throws ApiException {

                res.data().add(new JSMap("hello", "world"));

                //this forces the payload response over the 1KB size to trigger gzip compression
                StringBuilder big = new StringBuilder();
                for (int i = 0; i < 2048; i++) {
                    big.append("0");
                }
                res.data().add(new JSMap("big", big.toString()));

            }
        }));

        //-- the HttpClient will transparently handle gzip decoding
        //-- this is basic URLConnection test is done to make sure the server
        //-- is actually sending gzipped content
        URLConnection conn = new URL("http://localhost:8080/testme").openConnection();
        conn.setRequestProperty("Accept-Encoding", "gzip");
        assertEquals("gzip", conn.getHeaderField("Content-Encoding"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Utils.pipe(new GZIPInputStream(conn.getInputStream()), out);
        String str     = new String(out.toByteArray());
        JSNode payload = JSParser.asJSNode(str);
        assertEquals("world", payload.find("data.0.hello"));

        //-- now do the test again and the result should be the same
        //-- but the HttpClient will auto decode and remove/hide the Content-Encoding header
        ApiClient client = new ApiClient();
        Response  resp   = client.get("http://localhost:8080/testme").get();
        resp.assertOk();
        assertEquals(resp.find("data.0.hello"), "world");

    }

}
