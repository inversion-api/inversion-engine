/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.cloud.jdbc.action;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inversion.cloud.jdbc.db.JdbcDb;
import io.inversion.cloud.jdbc.utils.JdbcUtils;
import io.inversion.cloud.model.Action;
import io.inversion.cloud.model.Change;
import io.inversion.cloud.model.JSArray;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.utils.Utils;

public class JdbcLogAction extends Action<JdbcLogAction>
{
   Logger                log            = LoggerFactory.getLogger(getClass());

   protected String      logMask        = "* * * * * * * * * *";
   protected String      logTable       = null;
   protected String      logChangeTable = null;;

   protected Set<String> logMaskFields  = new HashSet<>();

   @Override
   public void run(Request req, Response res) throws Exception
   {
      if (req.getChain().getParent() != null)
      {
         //this must be a nested call to service.include so the outter call
         //is responsible for logging this change
         return;
      }

      try
      {
         req.getChain().go();
      }
      finally
      {
         String logMask = req.getChain().getConfig("logMask", this.logMask);
         String logTable = req.getChain().getConfig("logTable", this.logTable);
         String logChangeTable = req.getChain().getConfig("logChangeTable", this.logChangeTable);

         try
         {
            if (res.getStatusCode() > 199 && res.getStatusCode() < 300)
            {
               List<Change> changes = res.getChanges();
               int tenantId = 0;
               if (req.getApi().isMultiTenant())
               {
                  if (Chain.getUser() != null)
                  {
                     tenantId = Chain.getUser().getTenantId();
                  }
                  else if (req.getParams().containsKey("tenantid"))
                  {
                     tenantId = Integer.parseInt(req.getParam("tenantid"));
                  }
               }

               if (changes.size() > 0)
               {
                  Connection conn = ((JdbcDb) req.getCollection().getDb()).getConnection();
                  Map<String, Object> logParams = new HashMap<>();
                  logParams.put("method", req.getMethod());
                  logParams.put("userId", Chain.getUser() == null ? null : Chain.getUser().getId());
                  logParams.put("username", Chain.getUser() == null ? null : Chain.getUser().getUsername());

                  JSNode bodyJson = maskFields(req.getJson(), logMask);
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
                  if (req.getApi().isMultiTenant())
                  {
                     logParams.put("tenantId", tenantId);
                  }
                  Long logId = (Long) JdbcUtils.insertMap(conn, logTable, logParams);

                  List<Map> changeMap = new ArrayList();
                  for (Change c : changes)
                  {
                     Map<String, Object> changeParams = new HashMap<>();
                     changeParams.put("logId", logId);
                     changeParams.put("method", c.getMethod());
                     changeParams.put("collectionKey", c.getCollectionKey());
                     changeParams.put("entityKey", c.getEntityKey());
                     if (req.getApi().isMultiTenant())
                     {
                        changeParams.put("tenantId", tenantId);
                     }
                     changeMap.add(changeParams);
                  }
                  JdbcUtils.insertMaps(conn, logChangeTable, changeMap);
               }
            }
         }
         catch (Exception e)
         {
            log.error("Unexpected exception while adding row to audit log. " + req.getMethod() + " " + req.getUrl(), e);
         }
      }
   }

   JSNode maskFields(JSNode json, String mask)
   {
      if (json != null)
      {
         if (json instanceof JSArray)
         {
            for (Object o : (JSArray) json)
            {
               if (o instanceof JSNode)
               {
                  maskFields((JSNode) o, mask);
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

   public JdbcLogAction withLogMaskFields(java.util.Collection<String> logMaskFields)
   {
      setLogMaskFields(logMaskFields);
      return this;
   }

   public JdbcLogAction withLogMaskFields(String... logMaskFields)
   {
      for (String maskField : Utils.explode(",", logMaskFields))
         withLogMaskField(maskField);

      return this;
   }

   public JdbcLogAction withLogMaskField(String logMaskField)
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

   public JdbcLogAction withLogTable(String logTable)
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

   public JdbcLogAction withLogChangeTable(String logChangeTable)
   {
      setLogChangeTable(logChangeTable);
      return this;
   }
}
