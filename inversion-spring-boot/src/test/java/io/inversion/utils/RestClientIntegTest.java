package io.inversion.utils;

import io.inversion.*;
import io.inversion.spring.main.InversionMain;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RestClientIntegTest {

    @Test
    public void doRequest0_gzip_requests_are_encoded_correctly() throws Exception {

        InversionMain.run(new Api().withEndpoint("*", "*", new Action() {

            public void run(Request req, Response res) throws ApiException {

                if (!"yep".equals(req.getJson().find("testing")))
                    throw ApiException.new500InternalServerError();

                res.withJson(req.getJson());
            }
        }));

        RestClient client = new RestClient();
        Response   resp   = client.post("http://localhost:8080/testme", new JSNode("testing", "yep")).get();
        resp.assertOk();
        assertEquals(resp.find("testing"), "yep");
    }

    @Test
    public void doRequest0_gzip_responses_are_decoded_correctly() throws Exception {

        InversionMain.run(new Api().withEndpoint("*", "*", new Action() {

            public void run(Request req, Response res) throws ApiException {

                res.data().add(new JSNode("hello", "world"));

                //this forces the payload response over the 1KB size to trigger gzip compression
                StringBuilder big = new StringBuilder();
                for (int i = 0; i < 2048; i++) {
                    big.append("0");
                }
                res.data().add(new JSNode("big", big.toString()));

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
        JSNode payload = JSNode.parseJsonNode(str);
        assertEquals("world", payload.find("data.0.hello"));

        //-- now do the test again and the result should be the same
        //-- but the HttpClient will auto decode and remove/hide the Content-Encoding header
        RestClient client = new RestClient();
        Response   resp   = client.get("http://localhost:8080/testme").get();
        resp.assertOk();
        assertEquals(resp.find("data.0.hello"), "world");

    }

}
