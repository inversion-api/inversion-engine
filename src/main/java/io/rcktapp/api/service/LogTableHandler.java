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
import java.sql.SQLClientInfoException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.rcktapp.api.Api;
import io.rcktapp.api.ApiException;
import io.rcktapp.api.Attribute;
import io.rcktapp.api.Chain;
import io.rcktapp.api.Col;
import io.rcktapp.api.Collection;
import io.rcktapp.api.Entity;
import io.rcktapp.api.Handler;
import io.rcktapp.api.Relationship;
import io.rcktapp.api.Request;
import io.rcktapp.api.Response;
import io.rcktapp.api.Rule;
import io.rcktapp.api.SC;
import io.forty11.j.J;
import io.forty11.js.JSArray;
import io.forty11.js.JSObject;
import io.forty11.js.JSObject.Property;
import io.forty11.sql.Sql;
import io.forty11.utils.ListMap;

public class LogTableHandler implements Handler
{

   @Override
   public void service(Service service, Chain chain, Rule rule, Request req, Response res) throws Exception
   {
      if(true)
         return;
      
      if (req.isGet())
      {
         res.setStatus(SC.SC_400_BAD_REQUEST);
         return;
      }

      String method;
      String collection;
      int entityId;
      String body = null;

      if (req.isPost())
      {
         ListMap<String, String> headerMap = res.getHeaders();
         String location = headerMap.get("Location").remove(0);
         String[] locationArr = location.split("/");
         entityId = Integer.parseInt(locationArr[locationArr.length - 1]);
      }
      else
      {
         String url = req.getUrl();
         String[] urlParts = url.split("/");
         entityId = Integer.parseInt(urlParts[urlParts.length - 1]);
      }
      if (!req.isDelete())
      {
         JSObject bodyObj = req.getJson();
         body = bodyObj.toString();
      }
      method = req.getMethod();
      collection = req.getCollectionKey();

      Connection conn = null;
      try
      {
         conn = ((Snooze) service).getConnection(req.getApi(), req.getCollectionKey());
         String sql = "INSERT INTO Log (method, collection, entityId, body) VALUES (?, ?, ?, ?)";
         Sql.execute(conn, sql, method, collection, entityId, body);
      }
      finally
      {
         Sql.close(conn);
      }
   }
}
