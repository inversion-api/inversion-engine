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

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.rocketpartners.cloud.action.sql.SqlDb;
import io.rocketpartners.cloud.model.Action;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.ArrayNode;
import io.rocketpartners.cloud.model.Change;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.model.ObjectNode;
import io.rocketpartners.cloud.model.Request;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.SqlUtils;
import io.rocketpartners.cloud.utils.Utils;

public class LogAction extends Action<LogAction>
{
   Logger                               log            = LoggerFactory.getLogger(getClass());

   protected String                     logMask        = "* * * * * * * * * *";
   protected String                     logTable       = null;
   protected String                     logChangeTable = null;;

   protected Set<String> logMaskFields  = new HashSet<>();

   @Override
   public void run(Service service, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
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
         String logMask = chain.getConfig("logMask", this.logMask);
         String logTable = chain.getConfig("logTable", this.logTable);
         String logChangeTable = chain.getConfig("logChangeTable", this.logChangeTable);

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
                  Connection conn = ((SqlDb)req.getCollection().getDb()).getConnection();
                  Map<String, Object> logParams = new HashMap<>();
                  logParams.put("method", req.getMethod());
                  logParams.put("userId", req.getUser() == null ? null : req.getUser().getId());
                  logParams.put("username", req.getUser() == null ? null : req.getUser().getUsername());

                  ObjectNode bodyJson = maskFields(req.getJson(), logMask);
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
                  Long logId = (Long) SqlUtils.insertMap(conn, logTable, logParams);

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
                  SqlUtils.insertMaps(conn, logChangeTable, changeMap);
               }
            }
         }
         catch (Exception e)
         {
            log.error("Unexpected exception while adding row to audit log. " + req.getMethod() + " " + req.getUrl(), e);
         }
      }
   }

   ObjectNode maskFields(ObjectNode json, String mask)
   {
      if (json != null)
      {
         if (json instanceof ArrayNode)
         {
            for (Object o : (ArrayNode) json)
            {
               if (o instanceof ObjectNode)
               {
                  maskFields((ObjectNode) o, mask);
               }
            }
         }
         else
         {
            for (String key : json.keySet())
            {
               if (logMaskFields.contains(key))
               {
                  json.put(key, mask);
               }
            }
         }
      }

      return json;
   }

   public List<String> getLogMaskFields()
   {
      return new ArrayList(logMaskFields);
   }

   public void setLogMaskFields(java.util.Collection<String> logMaskFields)
   {
      this.logMaskFields.clear();
      this.logMaskFields.addAll(logMaskFields);
   }
   
   public LogAction withLogMaskFields(java.util.Collection<String> logMaskFields)
   {
      setLogMaskFields(logMaskFields);
      return this;
   }
   
   public LogAction withLogMaskFields(String... logMaskFields)
   {
      for (String maskField : Utils.explode(",", logMaskFields))
         withLogMaskField(maskField);

      return this;
   }
   
   public LogAction withLogMaskField(String logMaskField)
   {
      if (!logMaskFields.contains(logMaskField))
         logMaskFields.add(logMaskField);
      return this;
   }
   
   public String getLogTable()
   {
      return logTable;
   }
   
   public void setLogTable(String logTable)
   {
      this.logTable = logTable;
   }
   
   public LogAction withLogTable(String logTable)
   {
      setLogTable(logTable);
      return this;
   }
   
   public String getLogChangeTable()
   {
      return logChangeTable;
   }
   
   public void setLogChangeTable(String logChangeTable)
   {
      this.logChangeTable = logChangeTable;
   }
   
   public LogAction withLogChangeTable(String logChangeTable)
   {
      setLogChangeTable(logChangeTable);
      return this;
   }
}
