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
package io.rcktapp.api.handler.util;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.forty11.sql.Sql;
import io.forty11.utils.CaseInsensitiveSet;
import io.forty11.web.js.JSArray;
import io.forty11.web.js.JSObject;
import io.rcktapp.api.Action;
import io.rcktapp.api.Api;
import io.rcktapp.api.Chain;
import io.rcktapp.api.Change;
import io.rcktapp.api.Endpoint;
import io.rcktapp.api.Handler;
import io.rcktapp.api.Request;
import io.rcktapp.api.Response;
import io.rcktapp.api.handler.sql.SqlDb;
import io.rcktapp.api.service.Service;

public class LogHandler implements Handler
{
   private Logger             log             = LoggerFactory.getLogger(LogHandler.class);

   String                     logTable;
   String                     logChangeTable;

   CaseInsensitiveSet<String> sensitiveFields = new CaseInsensitiveSet<>();

   @Override
   public void service(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res) throws Exception
   {
      if (chain.getParent() != null)
      {
         //this must be a nested call to service.include so the outter call
         //is responsible for logging this change
         return;
      }

      try
      {
         chain.go();
      }
      finally
      {
         try
         {
            if (res.getStatusCode() > 199 && res.getStatusCode() < 300)
            {
               List<Change> changes = res.getChanges();
               int tenantId = 0;
               if (api.isMultiTenant())
               {
                  if (req.getUser() != null)
                  {
                     tenantId = req.getUser().getTenantId();
                  }
                  else if (req.getParams().containsKey("tenantid"))
                  {
                     tenantId = Integer.parseInt(req.getParam("tenantid"));
                  }
               }

               if (changes.size() > 0)
               {
                  Connection conn = ((SqlDb) service.getDb(req.getApi(), req.getCollectionKey())).getConnection();
                  Map<String, Object> logParams = new HashMap<>();
                  logParams.put("method", req.getMethod());
                  logParams.put("userId", req.getUser() == null ? null : req.getUser().getId());
                  logParams.put("username", req.getUser() == null ? null : req.getUser().getUsername());

                  JSObject bodyJson = obfuscateSensitiveFields(req.getJson());
                  if (bodyJson != null)
                  {
                     logParams.put("body", bodyJson.toString());
                  }
                  else
                  {
                     logParams.put("body", "");
                  }

                  logParams.put("url", req.getUrl().toString());
                  logParams.put("collectionKey", req.getCollectionKey());
                  if (api.isMultiTenant())
                  {
                     logParams.put("tenantId", tenantId);
                  }
                  Long logId = (Long) Sql.insertMap(conn, logTable, logParams);

                  List<Map> changeMap = new ArrayList();
                  for (Change c : changes)
                  {
                     Map<String, Object> changeParams = new HashMap<>();
                     changeParams.put("logId", logId);
                     changeParams.put("method", c.getMethod());
                     changeParams.put("collectionKey", c.getCollectionKey());
                     changeParams.put("entityKey", c.getEntityKey());
                     if (api.isMultiTenant())
                     {
                        changeParams.put("tenantId", tenantId);
                     }
                     changeMap.add(changeParams);
                  }
                  Sql.insertMaps(conn, logChangeTable, changeMap);
               }
            }
         }
         catch (Exception e)
         {
            log.error("Unexpected exception while adding row to audit log. " + req.getMethod() + " " + req.getUrl(), e);
         }
      }
   }

   JSObject obfuscateSensitiveFields(JSObject json)
   {
      if (json != null)
      {
         if (json instanceof JSArray)
         {
            for (Object o : ((JSArray) json).getObjects())
            {
               if (o instanceof JSObject)
               {
                  obfuscateSensitiveFields((JSObject) o);
               }
            }
         }
         else
         {
            for (String key : json.keys())
            {
               if (sensitiveFields.contains(key))
               {
                  json.put(key, "* * * * * * * * * *");
               }
            }
         }
      }

      return json;
   }

   public List<String> getSensitiveFields()
   {
      return new ArrayList(sensitiveFields);
   }

   public void setSensitiveFields(java.util.Collection<String> whitelist)
   {
      this.sensitiveFields.clear();
      this.sensitiveFields.addAll(whitelist);
   }

}
