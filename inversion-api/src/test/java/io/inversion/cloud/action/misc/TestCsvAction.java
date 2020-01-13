package io.inversion.cloud.action.misc;

import io.inversion.cloud.model.*;
import io.inversion.cloud.service.Chain;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class TestCsvAction
{
   CsvAction action = new CsvAction();

   //
   @Test
   public void run_shouldReturnCsv_when_singleObject_in_response() throws Exception
   {
      Response res = new Response();
      JSNode singleObject = new JSNode();
      singleObject.put("key1", "value1");
      singleObject.put("key2", "value2");
      res.withJson(singleObject);
      action.run(null, null, null, null, buildSuccessfulRequest(), res);
      assertNull(res.getJson());
      assertEquals("key1,key2\r\nvalue1,value2\r\n", res.getText());
   }

   @Test
   public void run_shouldReturnCsv_when_ArrayObject_in_response() throws Exception
   {
      Response res = new Response();
      JSNode singleObject = new JSNode();
      singleObject.put("key1", "value1");
      singleObject.put("key2", "value2");
      JSArray array = new JSArray();
      array.put(0, singleObject);
      array.put(1, singleObject);
      res.withJson(array);
      action.run(null, null, null, null, buildSuccessfulRequest(), res);
      assertNull(res.getJson());
      assertEquals("key1,key2\r\nvalue1,value2\r\nvalue1,value2\r\n", res.getText());
   }

   @Test
   public void run_shouldReturnCsv_when_InversionWrapper_in_response() throws Exception
   {
      Response res = new Response();
      JSNode singleObject = new JSNode();
      singleObject.put("key1", "value1");
      singleObject.put("key2", "value2");
      JSArray array = new JSArray();
      array.put(0, singleObject);
      array.put(1, singleObject);
      res.withData(array);
      res.withMeta("blah", "blahvalue");
      action.run(null, null, null, null, buildSuccessfulRequest(), res);
      assertNull(res.getJson());
      assertEquals("key1,key2\r\nvalue1,value2\r\nvalue1,value2\r\n", res.getText());
   }

   @Test
   public void run_should_doNothing_when_notGetRequest() throws Exception
   {
      Response res = new Response();
      JSNode singleObject = new JSNode();
      singleObject.put("key1", "value1");
      singleObject.put("key2", "value2");
      res.withJson(singleObject);
      Map<String, String> params = new HashMap<>();
      params.put("format", "csv");

      action.run(null, null, null, null, new Request("POST", "/", null, params, null), res);
      assertEquals(singleObject, res.getJson());
   }

   @Test
   public void run_should_doNothing_when_noCsvParam() throws Exception
   {
      try
      {
         Response res = new Response();
         JSNode singleObject = new JSNode();
         singleObject.put("key1", "value1");
         singleObject.put("key2", "value2");
         res.withJson(singleObject);
         Map<String, String> params = new HashMap<>();
         Request req = new Request("GET", "/", null, params, null);
         req.withEndpoint(new Endpoint());
         action.run(null, null, null, Chain.push(null, req, res), req, res);
         assertEquals(singleObject, res.getJson());
      }
      finally
      {
         Chain.pop();
      }
   }

   private Request buildSuccessfulRequest()
   {
      Map<String, String> params = new HashMap<>();
      params.put("format", "csv");
      return new Request("GET", "/", null, params, null);
   }
}
