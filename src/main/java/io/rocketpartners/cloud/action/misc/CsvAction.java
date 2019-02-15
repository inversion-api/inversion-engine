/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
 * http://rocketpartners.io
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package io.rocketpartners.cloud.action.misc;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.service.Request;
import io.rocketpartners.cloud.service.Response;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.JSArray;
import io.rocketpartners.cloud.utils.JSObject;

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
   public void run(Service service, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
   {
      if (!"GET".equals(req.getMethod()) || 200 != res.getStatusCode() || res.getJson() == null || res.getText() != null)
      {
         return;
      }

      if (!"csv".equalsIgnoreCase(req.getParam("format")) && !"csv".equalsIgnoreCase(chain.getConfig("format", null)))
         return;

      JSObject arr = res.getJson();
      if (!(arr instanceof JSArray))
      {
         arr = new JSArray(arr);
      }

      byte[] bytes = toCsv((JSArray) arr).getBytes();

      res.withHeader("Content-Length", bytes.length + "");
      res.debug("Content-Length " + bytes.length + "");
      //res.setContentType("text/csv");

      res.withText(new String(bytes));
      res.withJson(null);
   }

   public String toCsv(JSArray arr) throws Exception
   {
      StringBuffer buff = new StringBuffer();

      LinkedHashSet<String> keys = new LinkedHashSet();

      for (int i = 0; i < arr.length(); i++)
      {
         JSObject obj = (JSObject) arr.get(i);
         if (obj != null)
         {
            for (String key : obj.keySet())
            {
               Object val = obj.get(key);
               if (!(val instanceof JSArray) && !(val instanceof JSObject))
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
            Object val = ((JSObject) arr.get(i)).get(key);
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
