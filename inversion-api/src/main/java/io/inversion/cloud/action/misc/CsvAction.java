/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.cloud.action.misc;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.JSArray;
import io.inversion.cloud.model.Endpoint;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.service.Engine;

/**
 * Converts a JSON object/array response value into CSV format.  
 * 
 * Works for a status code 200 GET request when 'format=csv' is passed in on the query string 
 * or the Endpoint or an action has 'format=csv' as part of its config.
 * 
 * @author wells
 */
public class CsvAction extends BatchAction<CsvAction>
{
   @Override
   public void run(Engine engine, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
   {
      if (!"GET".equals(req.getMethod()) || 200 != res.getStatusCode() || res.getJson() == null || res.getText() != null)
      {
         return;
      }

      if (!"csv".equalsIgnoreCase(req.getParam("format")) && !"csv".equalsIgnoreCase(chain.getConfig("format", null)))
      {
         return;
      }

      //support result being an array, a single object, or an inversion state object where we will pull results from the data field.
      JSNode arr = res.getJson();
      if (!(arr instanceof JSArray))
      {
         if(res.getJson().hasProperty("data")){
            arr = res.getJson().getArray("data");
         } else {
            arr = new JSArray(arr);
         }
      }

      byte[] bytes = toCsv((JSArray) arr).getBytes();

      res.withHeader("Content-Length", bytes.length + "");
      res.debug("Content-Length " + bytes.length + "");
      //res.setContentType("text/csv");

      res.withText(new String(bytes));
   }

   public String toCsv(JSArray arr) throws Exception
   {
      StringBuffer buff = new StringBuffer();

      LinkedHashSet<String> keys = new LinkedHashSet();

      for (int i = 0; i < arr.length(); i++)
      {
         JSNode obj = (JSNode) arr.get(i);
         if (obj != null)
         {
            for (String key : obj.keySet())
            {
               Object val = obj.get(key);
               if (!(val instanceof JSArray) && !(val instanceof JSNode))
                  keys.add(key);
            }
         }
      }

      CSVPrinter printer = new CSVPrinter(buff, CSVFormat.DEFAULT);

      List<String> keysList = new ArrayList(keys);
      for (String key : keysList)
      {
         printer.print(key);
      }
      printer.println();

      for (int i = 0; i < arr.length(); i++)
      {
         for (String key : keysList)
         {
            Object val = ((JSNode) arr.get(i)).get(key);
            if (val != null)
            {
               printer.print(val);
            }
            else
            {
               printer.print("");
            }
         }
         printer.println();
      }
      printer.flush();
      printer.close();

      return buff.toString();
   }
}
