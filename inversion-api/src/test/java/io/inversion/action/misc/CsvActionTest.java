package io.inversion.action.misc;

import io.inversion.Request;
import io.inversion.Response;
import io.inversion.json.JSList;
import io.inversion.json.JSMap;
import io.inversion.json.JSNode;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CsvActionTest {
    final CsvAction action = new CsvAction();

    //
    @Test
    public void run_shouldReturnCsv_when_singleObject_in_response() throws Exception {
        Response res          = new Response();
        JSNode   singleObject = new JSMap();
        singleObject.put("key1", "value1");
        singleObject.put("key2", "value2");
        res.withJson(singleObject);
        action.run(buildSuccessfulRequest(), res);
        assertNull(res.getJson());
        assertEquals("key1,key2\r\nvalue1,value2\r\n", res.getText());
    }

    @Test
    public void run_shouldReturnCsv_when_ArrayObject_in_response() throws Exception {
        Response res          = new Response();
        JSNode   singleObject = new JSMap();
        singleObject.put("key1", "value1");
        singleObject.put("key2", "value2");
        JSList array = new JSList();
        array.put(0, singleObject);
        array.put(1, singleObject.copy());
        res.withJson(array);
        action.run(buildSuccessfulRequest(), res);
        System.out.println(res.getJson());
        assertNull(res.getJson());
        assertEquals("key1,key2\r\nvalue1,value2\r\nvalue1,value2\r\n", res.getText());
    }

    @Test
    public void run_shouldReturnCsv_when_InversionWrapper_in_response() throws Exception {
        Response res          = new Response();
        JSNode   singleObject = new JSMap();
        singleObject.put("key1", "value1");
        singleObject.put("key2", "value2");
        JSList array = new JSList();
        array.put(0, singleObject);
        array.put(1, singleObject.copy());
        res.withRecords(array.asList());
        res.withMeta("blah", "blahvalue");
        action.run(buildSuccessfulRequest(), res);
        assertNull(res.getJson());
        String text = res.getText();
        System.out.println(text);
        assertEquals("key1,key2\r\nvalue1,value2\r\nvalue1,value2\r\n", res.getText());
    }

    @Test
    public void run_should_doNothing_when_notGetRequest() throws Exception {
        Response res          = new Response();
        JSNode   singleObject = new JSMap();
        singleObject.put("key1", "value1");
        singleObject.put("key2", "value2");
        res.withJson(singleObject);
        Map<String, String> params = new HashMap<>();
        params.put("format", "csv");

        action.run(new Request("POST", "/test", null, params, null), res);
        assertEquals(singleObject, res.getJson());
    }

    @Test
    public void run_should_doNothing_when_noCsvParam() throws Exception {
//      try
//      {
//
//         Response res = new Response();
//         JSNode singleObject = new JSMap();
//         singleObject.put("key1", "value1");
//         singleObject.put("key2", "value2");
//         res.withJson(singleObject);
//         Map<String, String> params = new HashMap<>();
//         Request req = new Request("GET", "/", null, params, null);
//         req.withEndpoint(new Endpoint());
//
//         Chain.push(null, req, res);
//         action.run(req, res);
//         assertEquals(singleObject, res.getJson());
//      }
//      finally
//      {
//         Chain.pop();
//      }
    }

    private Request buildSuccessfulRequest() {
        Map<String, String> params = new HashMap<>();
        params.put("format", "csv");
        return new Request("GET", "/test", null, params, null);
    }
}
