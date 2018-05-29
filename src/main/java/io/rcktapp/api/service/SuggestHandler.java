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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.rcktapp.api.service;

import java.sql.Connection;
import java.util.List;

import io.forty11.j.J;
import io.forty11.js.JSArray;
import io.forty11.sql.Sql;
import io.rcktapp.api.Action;
import io.rcktapp.api.Api;
import io.rcktapp.api.Chain;
import io.rcktapp.api.Collection;
import io.rcktapp.api.Endpoint;
import io.rcktapp.api.Handler;
import io.rcktapp.api.Request;
import io.rcktapp.api.Response;
import io.rcktapp.api.Rule;
import io.rcktapp.api.service.Service;
import io.rcktapp.api.service.Snooze;

public class SuggestHandler implements Handler
{
   public void service(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res) throws Exception
   {
      String field = req.getParam("field"); //change this to be a comma separated list of collection.property,collection.property...
      String value = req.getParam("value");
      
      if(J.empty(field))
         return;
      
      if (J.empty(value))
         value = "";

      Connection conn = null;
      try
      {

         Api api = req.getApi();

         String table = field.substring(0, field.indexOf('.')); //change this to collection not table
         String column = field.substring(field.indexOf('.') + 1, field.length());

         //don't hard code table capitalization.  Use api.findTable or db.find...something like that is 
         //case insensative.  
         table = Character.toUpperCase(table.charAt(0)) + table.substring(1, table.length());

         try
         {
            value = Sql.check(value);
         }
         catch (Exception ex)
         {
            value = "";
         }

         conn = ((Snooze) service).getConnection(req.getApi(), null);

         Collection col = null;
         
         
         //make the sql do a distinct union of all the collection.property values passed in.
         //if the api is multi tenant, and the collection has a tenant property that include tenant=? in the query
         List<String> list = Sql.selectList(conn, "SELECT DISTINCT " + column + " FROM " + table + " WHERE " + column + " LIKE '%" + value + "%' AND " + column + " != '' ORDER BY " + column);

         if (list.size() > 1)
         {
            int next = 0;
            for (int i = 1; i < list.size(); i++)
            {
               if(list.get(i).toLowerCase().startsWith(value.toLowerCase()))
               {
                  String val = list.remove(i);
                  list.add(next++, val);
               }
            }
         }

         JSArray arr = new JSArray(list.toArray());
         res.setJson(arr);
      }
      finally
      {
         Sql.close(conn);
      }

   }
}
